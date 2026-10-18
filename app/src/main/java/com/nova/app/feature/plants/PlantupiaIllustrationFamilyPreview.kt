package com.nova.app.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.feature.plants.data.PlantupiaCareAction

/**
 * An actual Compose family contact sheet. Render at 360 and 430dp in Android
 * Studio; original art is visible in the live Plant/Space/Circle/History screens.
 * This preview itself is never a user-facing route or a substitute for device QA.
 */
@Composable
private fun IllustrationSample(title: String, art: @Composable () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        art()
        Text(title, color = PlantupiaColors.forestText, fontSize = 12.sp)
    }
}

@Preview(name = "Plantupia / Illustration Family 360", widthDp = 360, heightDp = 820, showBackground = true)
@Preview(name = "Plantupia / Illustration Family 430", widthDp = 430, heightDp = 820, showBackground = true)
@Composable
private fun PlantupiaIllustrationFamilyPreview() {
    Column(
        modifier = Modifier.fillMaxWidth().background(PlantupiaColors.cream).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("PLANTUPIA / ORIGINAL ILLUSTRATION KIT", color = PlantupiaColors.forest, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                IllustrationSample("Empty private place") { PlantupiaEmptyPlaceArt() }
                IllustrationSample("First plant") {
                    PlantupiaEmptyPlantArt(Modifier.fillMaxWidth().height(112.dp))
                }
            }
            Column(Modifier.weight(1f)) {
                IllustrationSample("Field notes") {
                    PlantupiaEmptyFieldNotesArt(Modifier.fillMaxWidth().height(112.dp))
                }
                IllustrationSample("Public question") {
                    PlantupiaEmptyCircleArt(Modifier.fillMaxWidth().height(112.dp))
                }
            }
        }
        Text("AUTHORED CARE PICTOGRAMS", color = PlantupiaColors.forest, fontSize = 12.sp)
        // Use the exact production care-grid column policy, not a misleading
        // fixed three-column design-only arrangement at 360dp.
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val columns = plantupiaCareActionColumns(
                availableWidthDp = maxWidth.value,
                fontScale = LocalDensity.current.fontScale,
            )
            val actions = PlantupiaCareAction.entries.toList()
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                actions.chunked(columns).forEach { group ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        group.forEachIndexed { index, action ->
                            PlantupiaCareActionTile(
                                action = action,
                                selected = index == 0 && action == actions.first(),
                                enabled = true,
                                onClick = {},
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Artwork is illustrative, never evidence of a real plant or public post",
            color = Color(0xFF526859),
            fontSize = 11.sp,
        )
    }
}
