package com.nova.app.feature.plants

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import org.junit.Rule
import org.junit.Test

/** The date controls genuinely swap the selected private evidence frame. */
class PlantupiaPhotoTimeLensInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun saved(id: String, date: String): PlantupiaCareEvent =
        PlantupiaCareEvent(
            id = id,
            kind = "observation", note = "A real user report in this isolated UI test",
            evidence = "observed", observedAt = date,
            photoUrl = "https://example.invalid/" + id + ".jpg",
            followUpOn = "", followUpCompletedAt = "",
        )

    @Test fun datesSwitchRealLoadedFramesWithoutInventingHealthResult() {
        val pair = PlantupiaLoadedPhotoPair(
            earlier = saved("older", "2026-10-02T12:00:00Z"),
            later = saved("newer", "2026-11-18T12:00:00Z"),
        )
        compose.setContent {
            PlantupiaPhotoTimeLens(
                pair = pair,
                hasOlderPages = true,
                onOpenPhoto = {},
            )
        }
        compose.onNodeWithText("See it at another point in time").assertExists()
        compose.onNodeWithContentDescription(
            "Later recorded photo, 2026-11-18",
        ).assertIsSelected()
        compose.onNodeWithContentDescription(
            "Earlier recorded photo, 2026-10-02",
        ).performClick()
        compose.onNodeWithContentDescription(
            "Earlier recorded photo, 2026-10-02",
        ).assertIsSelected()
        compose.onNodeWithText("Compare photos").performClick()
        compose.onNodeWithText("Single photo").assertExists()
        compose.onNodeWithContentDescription("Earlier recorded photo, " + pair.earlier.observedAt.take(10)).assertIsNotSelected()
        compose.onNodeWithContentDescription("Later recorded photo, " + pair.later.observedAt.take(10)).assertIsNotSelected()
        compose.onNodeWithText("Single photo").performClick()
        compose.onNodeWithText("Only photos loaded so far.", substring = true).assertExists()
        compose.onAllNodesWithText("Healthy", substring = true).assertCountEquals(0)
    }
}
