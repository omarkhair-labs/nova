package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.nova.app.feature.plants.data.PlantupiaCareEvent
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import java.io.File

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
            Column(Modifier.verticalScroll(rememberScrollState())) {
                PlantupiaPhotoTimeLens(pair = pair, hasOlderPages = true, onOpenPhoto = {}, alignmentEnabled = true)
            }
        }
        compose.onNodeWithText("Across time").assertExists()
        compose.onNodeWithText("47 days apart").assertExists()
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
        compose.onNodeWithText("Slide across photos").performClick()
        compose.onNodeWithContentDescription("Reveal earlier photo")
            .performSemanticsAction(SemanticsActions.SetProgress) { it(0.75f) }
        compose.onNodeWithText("Side by side").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Earlier recorded photo, " + pair.earlier.observedAt.take(10)).assertIsNotSelected()
        compose.onNodeWithContentDescription("Later recorded photo, " + pair.later.observedAt.take(10)).assertIsNotSelected()
        compose.onNodeWithText("Single photo").performClick()
        compose.onNodeWithText("Only photos loaded so far.", substring = true).assertExists()
        compose.onAllNodesWithText("Healthy", substring = true).assertCountEquals(0)
        compose.onNodeWithText("Align using reference marks").performScrollTo().performClick()
        compose.onNodeWithText("Align two visits").assertExists()
        compose.onNodeWithText("2026-10-02 · 0 of 4 marks").assertExists()
        compose.onNodeWithText("Close alignment").performScrollTo().performClick()
        compose.onNodeWithText("Across time").assertExists()
    }

    @Test fun chronologicalScrubAndRepeatUseTheSelectedSourceRecord() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val folder = File(context.cacheDir, "plantupia-chronology-ui-test").apply { mkdirs() }
        // Deliberately abstract local fixtures: no client imagery or network access.
        fun fixture(id: String, date: String, color: Int): PlantupiaCareEvent {
            val file = File(folder, "$id.png")
            val bitmap = Bitmap.createBitmap(180, 240, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.rgb(238, 232, 214))
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawRect(24f, 28f, 156f, 212f, paint)
            file.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            bitmap.recycle()
            return saved(id, date).copy(photoUrl = Uri.fromFile(file).toString(), note = "Synthetic chronology test")
        }
        val first = fixture("first", "2026-10-01T12:00:00Z", Color.rgb(68, 107, 77))
        val middle = fixture("middle", "2026-10-13T12:00:00Z", Color.rgb(124, 153, 106))
        val last = fixture("last", "2026-11-01T12:00:00Z", Color.rgb(35, 75, 62))
        var repeated: String? = null
        try {
            compose.setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PlantupiaChronologyHeader(listOf(last, first, middle, first), {}, { repeated = it.id })
                }
            }
            compose.onNodeWithText("3 / 3").assertExists()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Photograph recorded 2026-11-01").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithContentDescription("Select recorded photograph date").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(0f) }
            compose.onNodeWithText("1 / 3").assertExists()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithContentDescription("Photograph recorded 2026-10-01").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Photograph again from the same view").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(first.id, repeated) }
            compose.onNodeWithContentDescription("Select recorded photograph date").performScrollTo()
                .performSemanticsAction(SemanticsActions.SetProgress) { it(1f) }
            compose.onNodeWithText("2 / 3").assertExists()
            compose.onNodeWithText("Photograph again from the same view").performScrollTo().performClick()
            compose.runOnIdle { assertEquals(middle.id, repeated) }
            compose.onAllNodesWithText("Healthy", substring = true).assertCountEquals(0)
        } finally { folder.deleteRecursively() }
    }
}
