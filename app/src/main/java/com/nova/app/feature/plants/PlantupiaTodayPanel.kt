package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.feature.plants.data.PlantupiaPlant
import java.time.LocalDate

/** Human-chosen check-in dates, never a watering prediction or sensor value. */
internal fun scheduledPlantCheckIns(plants: List<PlantupiaPlant>): List<PlantupiaPlant> =
    plants.filter { plant ->
        plant.active && runCatching { LocalDate.parse(plant.nextFollowUpOn) }.isSuccess
    }.sortedBy { it.nextFollowUpOn }

@Composable
internal fun PlantupiaTodayPanel(
    plants: List<PlantupiaPlant>,
    onOpenPlant: (PlantupiaPlant) -> Unit,
) {
    val today = LocalDate.now()
    val planned = scheduledPlantCheckIns(plants)
    val due = planned.filter { !LocalDate.parse(it.nextFollowUpOn).isAfter(today) }
    val future = planned.filter { LocalDate.parse(it.nextFollowUpOn).isAfter(today) }

    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("TODAY / النهارده مع نباتاتك", style = PlantupiaText.sectionLabel)
        Surface(
            color = PlantupiaColors.softSage,
            border = BorderStroke(1.dp, PlantupiaColors.border),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = PlantupiaColors.paper,
                    ) {
                        Text(
                            text = due.size.toString(),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            style = PlantupiaText.editorialHeading,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            if (due.isNotEmpty()) "متابعات محتاجة نظرة منك"
                            else "مفيش متابعة مستحقة النهارده",
                            style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "افحص التربة والورق بنفسك قبل قرار الري.",
                            style = PlantupiaText.supporting,
                        )
                    }
                }
                if (due.isNotEmpty()) {
                    due.take(3).forEach { plant ->
                        Surface(
                            onClick = { onOpenPlant(plant) },
                            color = PlantupiaColors.paper,
                            shape = PlantupiaShapes.inset,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (plant.coverPhoto.isNotBlank()) {
                                    AsyncImage(
                                        model = plant.coverPhoto,
                                        contentDescription = "صورة نبات " + plant.name,
                                        modifier = Modifier.size(54.dp),
                                    )
                                } else {
                                    PlantupiaBrandGlyph(modifier = Modifier.size(43.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(plant.name, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                                    Text("متابعة اخترتها: " + plant.nextFollowUpOn, style = PlantupiaText.supporting)
                                }
                                Text("افتح ←", style = PlantupiaText.sectionLabel)
                            }
                        }
                    }
                    if (due.size > 3) {
                        Text("وفيه " + (due.size - 3) + " متابعات تانية", style = PlantupiaText.supporting)
                    }
                } else if (future.isNotEmpty()) {
                    val next = future.first()
                    Text(
                        "المراجعة الجاية: " + next.name + " · " + next.nextFollowUpOn,
                        style = PlantupiaText.supporting,
                    )
                    TextButton(onClick = { onOpenPlant(next) }) {
                        Text("افتح النبات", color = PlantupiaColors.forest)
                    }
                } else {
                    Text(
                        "لسه ما اخترتش مواعيد متابعة. افتح أي نبات وسجّل رعاية أو ملاحظة وحدد إمتى تحب تراجعه.",
                        style = PlantupiaText.supporting,
                    )
                    plants.firstOrNull { it.active }?.let { plant ->
                        TextButton(onClick = { onOpenPlant(plant) }) {
                            Text("ابدأ من " + plant.name + " ←", color = PlantupiaColors.forest)
                        }
                    }
                }
            }
        }
    }
}
