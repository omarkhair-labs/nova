package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Actual Compose chapter controls, independent of a mock plant API. */
class PlantupiaDetailChaptersInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test fun oneSelectedModeAtATimeAndTapsReplaceTheRealBody() {
        var selected = PlantupiaDetailChapter.OVERVIEW
        compose.setContent {
            var state by remember { mutableStateOf(PlantupiaDetailChapter.OVERVIEW) }
            MaterialTheme {
                Column {
                    PlantupiaDetailChapters(
                        selected = state,
                        onSelect = {
                            state = plantupiaNextChapter(state, it)
                            selected = state
                        },
                    )
                    Text("Only " + state.title + " is visible")
                }
            }
        }
        compose.onNodeWithText("Only Overview is visible").assertExists()
        compose.onNodeWithText("Care", substring = false).performClick()
        compose.onNodeWithText("Only Care is visible").assertExists()
        compose.onAllNodesWithText("Only Overview is visible").assertCountEquals(0)
        compose.onNodeWithText("History", substring = false).performClick()
        compose.onNodeWithText("Only History is visible").assertExists()
        compose.onAllNodesWithText("Only Care is visible").assertCountEquals(0)
        compose.runOnIdle { assertEquals(PlantupiaDetailChapter.HISTORY, selected) }
    }
}
