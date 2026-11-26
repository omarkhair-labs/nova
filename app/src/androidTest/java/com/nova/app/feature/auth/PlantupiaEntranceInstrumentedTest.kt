package com.nova.app.feature.auth

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.welcome.WelcomeScreen
import com.nova.app.ui.theme.NovaTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Exercises real entrance composables using fictional input and callbacks; never contacts an account API. */
class PlantupiaEntranceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun welcomeKeepsAllFiveRealEntryActionsReachable() {
        val actions = mutableListOf<String>()
        compose.setContent {
            NovaTheme {
                WelcomeScreen(
                    onCreateAccount = { actions += "create" }, onLogin = { actions += "login" },
                    onTerms = { actions += "terms" }, onPrivacy = { actions += "privacy" },
                    onConnectPlants = { actions += "delivery" },
                )
            }
        }
        capture("welcome")
        compose.onNodeWithText("Create account").performScrollTo().performClick()
        compose.onNodeWithText("Log in").performScrollTo().performClick()
        compose.onNodeWithText("Connect delivery").performScrollTo().performClick()
        compose.onNodeWithText("Terms").performScrollTo().performClick()
        compose.onNodeWithText("Privacy").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("create", "login", "delivery", "terms", "privacy"), actions) }
    }

    @Test fun registrationValidatesBeforeAdvancingAndSubmitsTheCompleteExistingContract() {
        val created = mutableListOf<List<String>>()
        val loading = mutableStateOf(false)
        compose.setContent {
            NovaTheme {
                CreateAccountScreen(loading.value, null, {}, { email, password, username, name ->
                    created += listOf(email, password, username, name)
                    loading.value = true
                })
            }
        }
        compose.onNodeWithText("Continue").performClick()
        compose.onNodeWithText("Enter a complete email address.").assertExists()
        compose.onAllNodesWithText("Name", substring = false).assertCountEquals(0)
        field("Email").performScrollTo().performTextInput("fictional@example.com")
        field("Password").performScrollTo().performTextInput("test-password-2026")
        capture("create-account")
        compose.onNodeWithText("Continue").performClick()
        field("Name").performScrollTo().performTextInput("Fictional Test")
        field("Username").performScrollTo().performTextInput("fictional.test")
        compose.onNodeWithText("Edit account details").performScrollTo().performClick()
        field("Email").assertTextContains("fictional@example.com")
        compose.onNodeWithText("Continue").performClick()
        field("Name").assertTextContains("Fictional Test")
        capture("create-profile")
        field("Username").performScrollTo().performImeAction()
        compose.onNodeWithText("Creating account…").assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(listOf(listOf("fictional@example.com", "test-password-2026", "fictional.test", "Fictional Test")), created)
        }
    }

    @Test fun restorationKeepsEmailAndNameButNeverRestoresThePassword() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            NovaTheme { CreateAccountScreen(false, null, {}, { _, _, _, _ -> error("Must not submit on restore") }) }
        }
        field("Email").performTextInput("fictional@example.com")
        field("Password").performScrollTo().performTextInput("temporary-password")
        compose.onNodeWithText("Continue").performClick()
        field("Name").performScrollTo().performTextInput("Fictional Test")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("1 of 2").assertExists()
        field("Email").assertTextContains("fictional@example.com")
        field("Password").assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText,
            androidx.compose.ui.text.AnnotatedString("")))
        field("Password").performScrollTo().performTextInput("replacement-password")
        compose.onNodeWithText("Continue").performClick()
        field("Name").assertTextContains("Fictional Test")
    }

    @Test fun loginSupportsRevealImeSubmissionAndBusyLockWithoutLosingTheIdentifier() {
        val submitted = mutableListOf<Pair<String, String>>()
        val loading = mutableStateOf(false)
        compose.setContent {
            NovaTheme {
                LoginScreen(loading.value, null, {}, { email, password ->
                    submitted += email to password
                    loading.value = true
                }, initialEmail = "fictional@example.com")
            }
        }
        field("Email").assertTextContains("fictional@example.com")
        capture("login")
        field("Password").performScrollTo().performTextInput("test-password")
        compose.onNodeWithContentDescription("Show Password").performClick()
        compose.onNodeWithContentDescription("Hide Password").assertExists().performClick()
        field("Password").performImeAction()
        compose.onNodeWithText("Opening your space…").assertIsNotEnabled()
        compose.onNodeWithText("Email").assertIsNotEnabled()
        compose.runOnIdle { assertEquals(listOf("fictional@example.com" to "test-password"), submitted) }
    }

    @Test fun recoverableLoginErrorIsAnnouncedAndSwitchingKeepsTypedEmail() {
        val switched = mutableListOf<String>()
        compose.setContent {
            NovaTheme {
                LoginScreen(false, "Connection interrupted. Try again.", {}, { _, _ -> },
                    onCreateAccount = { switched += it })
            }
        }
        field("Email").performScrollTo().performTextInput("fictional@example.com")
        compose.onNodeWithText("Connection interrupted. Try again.")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        capture("login-error")
        compose.onNodeWithText("New to Plantupia? Create account").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(listOf("fictional@example.com"), switched) }
    }

    private fun field(label: String) = compose.onNode(hasText(label) and hasSetTextAction())

    private fun capture(name: String) {
        if (InstrumentationRegistry.getArguments().getString("plantupiaCapture") != "true") return
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val config = context.resources.configuration
        val directory = File(context.filesDir, "craft-evidence").apply { mkdirs() }
        val file = File(directory, "$name-${config.screenWidthDp}dp-font${config.fontScale}.png")
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    }
}
