package com.nova.app.feature.plants

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Bundle
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import androidx.test.core.app.ActivityScenario
import androidx.activity.compose.setContent
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MainActivity
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.theme.NovaTheme
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Focused real-sheet/platform IME check outside Compose's text-input test override.
 * The separate experience suite covers the actual app navigation into this sheet.
 */
class PlantupiaPlatformKeyboardInstrumentedTest {
    @Test fun actualSoftwareKeyboardLeavesAiComposerVisibleAndQuestionRecoverable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        assumeTrue(context.packageName.endsWith(".qa") && URI(NovaMessagingRepository.PRODUCTION_API_URL).host in listOf("127.0.0.1", "localhost", "10.0.2.2"))
        val file = File(context.filesDir, "experience-local-qa.json")
        assumeTrue("Requires private disposable local fixture", file.exists())
        val fixture = JSONObject(file.readText())
        require(fixture.getString("scope") == "LOCAL_CEO_QA_ONLY")
        val customer = fixture.getJSONObject("customer")
        val result = runBlocking { AuthRemoteDataSource(NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL))
            .login(customer.getString("email"), customer.getString("password")) }
        assertTrue(result is ApiResult.Success)
        val store = NovaSessionStore(context)
        assertTrue(store.save(store.beginLogin(), (result as ApiResult.Success).value))
        val repository = PlantupiaRepository(context)
        val inventory = runBlocking { repository.plants() }
        assertTrue(inventory is ApiResult.Success)
        val plant = (inventory as ApiResult.Success).value.first { it.id == fixture.getJSONArray("plant_ids").getString(0) }
        val automation = instrumentation.uiAutomation
        val priorFlags = automation.serviceInfo.flags
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS }
        fun all(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> = if (root == null) emptyList() else
            listOf(root) + (0 until root.childCount).flatMap { all(root.getChild(it)) }
        fun nodes() = all(automation.rootInActiveWindow)
        fun awaitNode(label: String, description: Boolean = false, substring: Boolean = false, scroll: Boolean = false): AccessibilityNodeInfo {
            val limit = SystemClock.elapsedRealtime() + 35_000
            while (SystemClock.elapsedRealtime() < limit) {
                val current = nodes()
                current.firstOrNull {
                    val value = (if (description) it.contentDescription else it.text)?.toString().orEmpty()
                    it.isVisibleToUser && if (substring) value.contains(label) else value == label
                }?.let { return it }
                if (scroll) current.filter { it.isScrollable && it.isVisibleToUser }.maxByOrNull {
                    Rect().also(it::getBoundsInScreen).height()
                }?.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                SystemClock.sleep(500)
            }
            throw AssertionError("Visible native control missing: $label")
        }
        fun click(node: AccessibilityNodeInfo) {
            var target = node
            while (!target.isClickable && target.parent != null) target = target.parent
            assertTrue(target.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        fun capture(name: String) {
            SystemClock.sleep(500)
            val directory = File(context.getExternalFilesDir(null), "experience-evidence").apply { mkdirs() }
            val config = context.resources.configuration
            File(directory, "$name-${config.screenWidthDp}dp-font${config.fontScale}.jpg").outputStream().use {
                requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.JPEG, 88, it)
            }
        }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            try {
                scenario.onActivity { activity -> activity.setContent {
                    NovaTheme { PlantupiaAssistantSheet(plant, repository, onDismiss = { activity.finish() }) }
                } }
                awaitNode("Message", description = true)
                val input = nodes().firstOrNull { it.isEditable && it.isVisibleToUser }
                assertNotNull("Must locate the actual platform editor", input)
                input!!.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                input.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                val question = "Software QA platform keyboard only. What should I inspect before watering?"
                val deadline = SystemClock.elapsedRealtime() + 15_000
                while (automation.windows.none { window -> window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD } && SystemClock.elapsedRealtime() < deadline) SystemClock.sleep(200)
                val ime = automation.windows.firstOrNull { window -> window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
                assertNotNull("Must observe the actual platform keyboard window", ime)
                SystemClock.sleep(600)
                val editor = nodes().firstOrNull { it.isEditable && it.isVisibleToUser }
                assertNotNull("Must use the real editable accessibility node", editor)
                assertTrue("Native text action must be accepted", editor!!.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, question)
                }))
                val inputBounds = Rect().also { awaitNode("Message", description = true).getBoundsInScreen(it) }
                val keyboardBounds = Rect().also { ime!!.getBoundsInScreen(it) }
                assertTrue("Composer must remain above the actual keyboard", inputBounds.bottom <= keyboardBounds.top)
                capture("36-platform-ime-visible")
                click(awaitNode("Send"))
                if (automation.windows.any { window -> window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD })
                    automation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                awaitNode("Plantupia AI is unavailable right now.", substring = true, scroll = true)
                assertEquals(question, nodes().firstOrNull { node -> node.isEditable && node.isVisibleToUser }?.text?.toString())
                capture("37-platform-ime-recoverable-question")
            } finally { automation.serviceInfo = automation.serviceInfo.apply { flags = priorFlags } }
        }
    }
}
