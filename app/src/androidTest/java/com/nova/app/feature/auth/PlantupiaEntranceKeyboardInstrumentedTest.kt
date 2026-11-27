package com.nova.app.feature.auth

import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MainActivity
import com.nova.app.ui.theme.NovaTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.*
import org.junit.Test

/** Real platform IME outside Compose Test's input override, with a stub submission callback. */
class PlantupiaEntranceKeyboardInstrumentedTest {
    @Test fun actualKeyboardLeavesLoginActionVisibleAndSubmitsTheTypedCredentialsOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        val oldFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        val loading = mutableStateOf(false)
        val submissions = AtomicInteger(0)
        val payload = AtomicReference<Pair<String, String>>()
        fun all(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (root == null) emptyList() else
            listOf(root) + (0 until root.childCount).flatMap { all(root.getChild(it)) }
        fun nodes() = all(automation.rootInActiveWindow)
        fun awaitNode(description: String, predicate: (AccessibilityNodeInfo) -> Boolean): AccessibilityNodeInfo {
            val deadline = SystemClock.elapsedRealtime() + 30_000
            while (SystemClock.elapsedRealtime() < deadline) {
                nodes().firstOrNull { it.isVisibleToUser && predicate(it) }?.let { return it }
                SystemClock.sleep(150)
            }
            throw AssertionError("Visible native control missing: $description")
        }
        fun click(node: AccessibilityNodeInfo) {
            var target = node
            while (!target.isClickable && target.parent != null) target = target.parent
            assertTrue("Control must accept a click", target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity ->
                    activity.setContent {
                        NovaTheme {
                            LoginScreen(
                                isLoading = loading.value, errorMessage = null,
                                onBack = {}, initialEmail = "fictional@example.com",
                                onLogin = { email, password ->
                                    payload.set(email to password)
                                    submissions.incrementAndGet()
                                    loading.value = true
                                },
                            )
                        }
                    }
                }
                val password = awaitNode("Password editor") { it.isEditable && it.isPassword }
                password.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                click(password)
                val deadline = SystemClock.elapsedRealtime() + 20_000
                while (automation.windows.none { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } &&
                    SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(150)
                val keyboard = automation.windows.firstOrNull {
                    it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD
                }
                assertNotNull("The actual Android keyboard must be visible", keyboard)
                val editor = awaitNode("Focused password editor") { it.isEditable && it.isPassword }
                assertTrue(editor.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "fictional-password")
                }))
                // Let the real IME inset animation settle, then use actual accessibility bounds.
                SystemClock.sleep(600)
                val login = awaitNode("Log in") { it.text?.toString() == "Log in" }
                var button = login
                while (!button.isClickable && button.parent != null) button = button.parent
                val buttonBounds = Rect().also(button::getBoundsInScreen)
                val settledKeyboard = automation.windows.first { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                val keyboardBounds = Rect().also(settledKeyboard::getBoundsInScreen)
                assertTrue("Login action must remain above the keyboard", buttonBounds.bottom <= keyboardBounds.top)
                assertTrue("Login action must remain inside the visible app", buttonBounds.top >= 0)
                if (InstrumentationRegistry.getArguments().getString("plantupiaCapture") == "true") {
                    val context = instrumentation.targetContext
                    val config = context.resources.configuration
                    val directory = File(context.filesDir, "craft-evidence").apply { mkdirs() }
                    File(directory, "login-keyboard-${config.screenWidthDp}dp-font${config.fontScale}.png")
                        .outputStream().use { requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                click(login)
                awaitNode("Opening your space") { it.text?.toString() == "Opening your space…" }
                assertEquals("Submit exactly once", 1, submissions.get())
                assertEquals("fictional@example.com" to "fictional-password", payload.get())
            } finally {
                automation.serviceInfo = automation.serviceInfo.apply { flags = oldFlags }
            }
        }
    }
}
