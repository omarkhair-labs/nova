package com.nova.app.feature.plants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Original editorial marks, not photos, plant condition measurement, or AI diagnoses.
 * They represent app destinations and content categories only.
 */
@Composable
internal fun PlantupiaEditorialMark(kind: String, modifier: Modifier = Modifier, ink: Color = PlantupiaColors.forest) {
    Canvas(modifier.clearAndSetSemantics { }) {
        val s = size.minDimension
        val stroke = s * .038f
        fun pt(x: Float, y: Float) = Offset(s*x, s*y)
        fun ln(x1:Float,y1:Float,x2:Float,y2:Float) =
            drawLine(ink,pt(x1,y1),pt(x2,y2),strokeWidth=stroke,cap=StrokeCap.Round)
        when(kind) {
            "circle" -> {
                drawRoundRect(ink.copy(alpha=.24f),pt(.05f,.14f),Size(s*.64f,s*.51f),
                    CornerRadius(s*.16f),style=Stroke(stroke))
                drawRoundRect(ink,pt(.27f,.39f),Size(s*.67f,s*.44f),
                    CornerRadius(s*.17f),style=Stroke(stroke))
                ln(.43f,.56f,.76f,.56f)
                ln(.43f,.67f,.66f,.67f)
            }
            "today" -> {
                drawRoundRect(ink,pt(.10f,.21f),Size(s*.78f,s*.66f),
                    CornerRadius(s*.11f),style=Stroke(stroke))
                ln(.10f,.40f,.88f,.40f)
                ln(.30f,.08f,.30f,.29f);ln(.69f,.08f,.69f,.29f)
                drawCircle(ink,s*.08f,pt(.38f,.63f),style=Stroke(stroke))
                ln(.35f,.63f,.39f,.68f);ln(.39f,.68f,.55f,.52f)
            }
            "plant" -> {
                ln(.49f,.83f,.49f,.28f)
                val left=Path().apply{
                    moveTo(s*.49f,s*.53f);cubicTo(s*.17f,s*.55f,s*.10f,s*.27f,s*.20f,s*.17f)
                    cubicTo(s*.36f,s*.11f,s*.52f,s*.31f,s*.49f,s*.53f)
                }
                val right=Path().apply{
                    moveTo(s*.49f,s*.40f);cubicTo(s*.48f,s*.17f,s*.74f,s*.10f,s*.84f,s*.21f)
                    cubicTo(s*.86f,s*.38f,s*.69f,s*.51f,s*.49f,s*.40f)
                }
                drawPath(left,ink,style=Stroke(stroke));drawPath(right,ink,style=Stroke(stroke))
                ln(.26f,.87f,.73f,.87f)
            }
            "planter" -> {
                ln(.15f,.29f,.86f,.29f)
                ln(.23f,.29f,.33f,.83f);ln(.79f,.29f,.67f,.83f)
                ln(.33f,.83f,.67f,.83f)
                drawCircle(ink,s*.035f,pt(.45f,.50f))
            }
            "care" -> {
                drawCircle(ink,s*.29f,pt(.50f,.50f),style=Stroke(stroke))
                ln(.50f,.35f,.50f,.65f)
                ln(.35f,.50f,.65f,.50f)
                drawCircle(ink.copy(alpha=.55f),s*.04f,pt(.87f,.17f))
                drawCircle(ink.copy(alpha=.55f),s*.03f,pt(.13f,.82f))
            }
            else -> {
                ln(.49f,.82f,.49f,.31f);ln(.49f,.54f,.30f,.35f)
                ln(.49f,.44f,.68f,.24f)
                drawCircle(ink,s*.08f,pt(.30f,.35f),style=Stroke(stroke))
                drawCircle(ink,s*.08f,pt(.68f,.24f),style=Stroke(stroke))
            }
        }
    }
}

/** Status text is derived only from real user-chosen check-ins. */
internal fun todayCareBrief(activeCount: Int, dueCount: Int): String = when {
    activeCount == 0 -> "Your living space starts with a real plant."
    dueCount > 0 -> "$dueCount personal check-in${if (dueCount == 1) "" else "s"} due."
    else -> "No personal check-ins due. Your plants are here when you need them."
}

internal data class ShopShelfCategory(val key: String, val label: String, val sublabel: String, val tint: Color)

internal val plantupiaShopShelfCategories = listOf(
    ShopShelfCategory("all", "All", "The collection", Color(0xFFE5EBDE)),
    ShopShelfCategory("plant", "Plants", "Living things", Color(0xFFDEEAD8)),
    ShopShelfCategory("planter", "Pots", "Objects & vessels", Color(0xFFF1E4D8)),
    ShopShelfCategory("accessory", "Care", "Tools & nutrition", Color(0xFFE5EDF0)),
)

@Composable
internal fun PlantupiaShopCategoryShelf(selected: String, onSelect: (String) -> Unit) {
    BoxWithConstraints {
        val columns = if (maxWidth >= 400.dp && LocalDensity.current.fontScale <= 1.2f) 4 else 2
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            plantupiaShopShelfCategories.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { category ->
                        val active = selected == category.key
                        val background by animateColorAsState(
                            targetValue = if (active) PlantupiaColors.forest else category.tint,
                            label = "shop-shelf-color-" + category.key,
                        )
                        val scale by animateFloatAsState(
                            targetValue = if (active) 1.015f else 1f,
                            animationSpec = spring(dampingRatio = .88f, stiffness = 560f),
                            label = "shop-shelf-focus-" + category.key,
                        )
                        val foreground = if (active) PlantupiaColors.paper else PlantupiaColors.forest
                        Surface(
                            onClick = { onSelect(category.key) },
                            modifier = Modifier.weight(1f).heightIn(min = 107.dp)
                                .graphicsLayer { scaleX=scale; scaleY=scale }
                                .semantics { this.selected = active; contentDescription = "Shop " + category.label },
                            color = background,
                            border = BorderStroke(if (active) 2.dp else 1.dp,
                                foreground.copy(alpha = if (active) .8f else .16f)),
                            shape = RoundedCornerShape(19.dp),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                PlantupiaEditorialMark(
                                    category.key, Modifier.size(34.dp), foreground,
                                )
                                Text(category.label, style = PlantupiaText.body.copy(
                                    fontWeight = FontWeight.SemiBold, color = foreground))
                                Text(category.sublabel, style = PlantupiaText.supporting.copy(color = foreground))
                            }
                        }
                    }
                    if (row.size < columns) repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
internal fun PlantupiaTodayMasthead(
    activeCount: Int,
    dueCount: Int,
    date: LocalDate = LocalDate.now(),
) {
    Surface(
        color = PlantupiaColors.forest,
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    date.format(DateTimeFormatter.ofPattern("EEEE · d MMM", Locale.ENGLISH)).uppercase(Locale.ENGLISH),
                    style = PlantupiaText.sectionLabel.copy(color = Color(0xFFD5DEC5)),
                )
                Text("A quieter way to care", style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.paper))
                Text(
                    todayCareBrief(activeCount, dueCount),
                    style = PlantupiaText.supporting.copy(color = PlantupiaColors.cream),
                )
            }
            PlantupiaEditorialMark("today", Modifier.size(65.dp), Color(0xFFD5DEC5))
        }
    }
}

@Composable
internal fun PlantupiaCircleEditorialIntro() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE6EDE3),
        shape = RoundedCornerShape(23.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("THE CIRCLE · HUMAN STORIES", style = PlantupiaText.sectionLabel)
                Text("Better together", style = PlantupiaText.editorialHeading)
                Text(
                    "Questions and experiences people choose to share. Your private plant record stays yours.",
                    style = PlantupiaText.supporting,
                )
            }
            PlantupiaEditorialMark("circle", Modifier.size(68.dp), PlantupiaColors.forest)
        }
    }
}

/**
 * A source-authorized inspection occurrence is NOT the same as a customer's
 * dated freeform check-in, a diagnosis, or an instruction to water.
 */
@Composable
internal fun PlantupiaApprovedInspectionTodayCard(
    plantName: String,
    action: String,
    dueOn: String,
    due: Boolean,
    canOpenPlant: Boolean,
    onOpenPlant: () -> Unit,
) {
    val accent = if (due) Color(0xFF8A5734) else PlantupiaColors.forest
    val background = if (due) Color(0xFFF5E7D8) else Color(0xFFE7EDE4)
    val description = when (action) {
        "soil" -> "Check the soil"
        "leaves" -> "Observe the leaves"
        "drainage" -> "Inspect drainage"
        "rotation" -> "Check placement and light"
        else -> "Review approved inspection"
    }
    Surface(
        onClick = onOpenPlant,
        enabled = canOpenPlant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 82.dp),
        color = background,
        border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaEditorialMark("today", Modifier.size(45.dp), accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plantName, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                Text(description, style = PlantupiaText.supporting)
                Text((if (due) "DUE · " else "NEXT · ") + dueOn,
                    style = PlantupiaText.sectionLabel.copy(color = accent))
            }
            Text(if (canOpenPlant) "Open →" else "Unavailable", style = PlantupiaText.sectionLabel.copy(color = accent))
        }
    }
}

@Composable
internal fun PlantupiaShopEmptyResults(hasAnyProducts: Boolean, searchActive: Boolean) {
    Surface(
        color = PlantupiaColors.softSage,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(17.dp),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaEditorialMark("plant", Modifier.size(56.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(if (!hasAnyProducts) "The collection is growing" else "Nothing in this view",
                    style = PlantupiaText.editorialHeading)
                Text(
                    if (!hasAnyProducts) "No catalogue products have been published in this QA collection yet."
                    else if (searchActive) "Try a different search or clear your category filter."
                    else "Explore another category to see the available products.",
                    style = PlantupiaText.supporting,
                )
            }
        }
    }
}
