package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Real creation controls with synthetic text; no account login or server writes. */
class PlantupiaCreationDraftInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun isolatedSession() { NovaSessionStore(context).logout() }
    @After fun clearSession() { NovaSessionStore(context).logout() }

    @Test fun sameLoginRestoresTheOpenSpaceFormAndUnsubmittedText() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val repository = remember { PlantupiaRepository(context) }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantupiaSelfServiceForms(
                        spaces = emptyList(), repository = repository,
                        onCreated = { error("Restoration must not create a space") },
                        onRefreshSpaces = {},
                        onPlantCreated = { _, _ -> error("Restoration must not create a plant") },
                    )
                }
            }
        }
        compose.onNodeWithText("+ Create my space").performClick()
        compose.onNodeWithText("Space name *").performScrollTo().performTextInput("FICTIONAL QA window desk")
        compose.onNodeWithText("Continue").performScrollTo().performClick()
        compose.onNodeWithText("Location (optional)").performScrollTo().performTextInput("Draft room")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("A little context").assertExists()
        compose.onNodeWithText("FICTIONAL QA window desk").assertExists()
        compose.onNodeWithText("Location (optional)").assertTextContains("Draft room")
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Space name *").assertTextContains("FICTIONAL QA window desk")
    }

    @Test fun freshLoginDiscardsThePreviousUnsubmittedSpaceDraft() {
        val repositoryVersion = mutableIntStateOf(0)
        compose.setContent {
            val repository = remember(repositoryVersion.intValue) { PlantupiaRepository(context) }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantupiaSelfServiceForms(
                        spaces = emptyList(), repository = repository,
                        onCreated = {}, onRefreshSpaces = {}, onPlantCreated = { _, _ -> },
                    )
                }
            }
        }
        compose.onNodeWithText("+ Create my space").performClick()
        compose.onNodeWithText("Space name *").performScrollTo().performTextInput("Earlier login draft")
        compose.runOnIdle {
            NovaSessionStore(context).logout()
            repositoryVersion.intValue++
        }
        compose.onNodeWithText("+ Create my space").assertExists()
        compose.onAllNodesWithText("Earlier login draft").assertCountEquals(0)
        compose.onNodeWithText("+ Create my space").performClick()
        compose.onAllNodesWithText("Earlier login draft").assertCountEquals(0)
        compose.onNodeWithText("Space name *").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
    }

    @Test fun plantDraftWaitsForReloadAndCannotRestoreRevokedWriteAccess() {
        val restoration = StateRestorationTester(compose)
        val ready = mutableStateOf(true)
        val spaces = mutableStateOf(listOf(
            PlantupiaSpace("fictional-a", "First desk", "", "", canAddPlant = true),
            PlantupiaSpace("fictional-b", "Window desk", "", "", canAddPlant = true),
        ))
        restoration.setContent {
            val repository = remember { PlantupiaRepository(context) }
            MaterialTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantupiaSelfServiceForms(
                        spaces = spaces.value, repository = repository,
                        onCreated = {}, onRefreshSpaces = {}, onPlantCreated = { _, _ -> },
                        preferredSpaceId = "fictional-b", enabled = ready.value,
                    )
                }
            }
        }
        compose.onNodeWithText("+ Add plant").performClick()
        compose.onNodeWithText("I don't know the type — add my plant anyway")
            .performScrollTo().performClick()
        compose.onNodeWithText("Plant name *").performScrollTo().performTextInput("FICTIONAL QA fern")
        compose.runOnIdle { ready.value = false }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { ready.value = true }
        compose.onNodeWithText("Plant name *").assertTextContains("FICTIONAL QA fern")
        compose.onNodeWithText("Window desk").assertExists()
        compose.runOnIdle {
            spaces.value = spaces.value.map { it.copy(canAddPlant = false) }
        }
        compose.onNodeWithText("Choose a space").assertExists()
        compose.onNodeWithText("Continue", substring = false).assertIsNotEnabled()
    }
}
