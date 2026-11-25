package com.nova.app.feature.plants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.sp
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
    activeCount == 0 -> "Start with a real plant. Its record begins here."
    dueCount == 1 -> "One personal check-in ready to review."
    dueCount > 1 -> "$dueCount personal check-ins ready to review."
    else -> "See what's next, or record a moment of care."
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
    // Product photography must be visible on the first screen. Categories are
    // discovery controls, NOT four oversized hero cards that hide inventory.
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(plantupiaShopShelfCategories.size) { index ->
            val category = plantupiaShopShelfCategories[index]
            val active = selected == category.key
            val background by animateColorAsState(
                targetValue = if (active) PlantupiaColors.forest else category.tint,
                label = "shop-filter-" + category.key,
            )
            val ink = if (active) PlantupiaColors.paper else PlantupiaColors.forest
            Surface(
                onClick = { onSelect(category.key) },
                modifier = Modifier.heightIn(min = 52.dp)
                    .semantics {
                        this.selected = active
                        contentDescription = "Filter Shop by " + category.label
                    },
                color = background,
                border = BorderStroke(1.dp, ink.copy(alpha = if (active) .75f else .16f)),
                shape = RoundedCornerShape(15.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlantupiaEditorialMark(category.key, Modifier.size(23.dp), ink)
                    Text(
                        category.label,
                        style = PlantupiaText.body.copy(
                            fontWeight = FontWeight.SemiBold, color = ink,
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Today's first fold belongs to the user's day. Schedule is reachable here
 * without scrolling past inspections or repeating a calendar shortcut below.
 */
@Composable
internal fun PlantupiaTodayMasthead(
    activeCount: Int,
    dueCount: Int,
    onOpenCalendar: () -> Unit,
    date: LocalDate = LocalDate.now(),
) {
    val paper = PlantupiaColors.paper
    Surface(color = PlantupiaColors.forest, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(date.format(DateTimeFormatter.ofPattern("EEEE · MMMM yyyy", Locale.ENGLISH)).uppercase(Locale.ENGLISH),
                style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth / LocalDensity.current.fontScale >= 235.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(date.dayOfMonth.toString().padStart(2, '0'),
                                style = PlantupiaText.pageHeading.copy(color = paper,
                                    fontSize = 70.sp, lineHeight = 76.sp))
                            Text("TODAY / YOUR LIVING RECORD",
                                style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
                        }
                        PlantupiaEditionArt("day", Modifier.size(104.dp))
                    }
                } else {
                    Text(date.dayOfMonth.toString(), style = PlantupiaText.pageHeading.copy(color = paper, fontSize = 44.sp, lineHeight = 49.sp))
                }
            }
            androidx.compose.material3.HorizontalDivider(color = PlantupiaColors.sage.copy(alpha = .35f))
            Text(if (dueCount > 0) "A little care is due" else if (activeCount == 0) "It begins with a plant" else "One day, one living space",
                style = PlantupiaText.editorialHeading.copy(color = paper))
            Text(todayCareBrief(activeCount, dueCount), style = PlantupiaText.body.copy(color = PlantupiaColors.cream))
            Surface(onClick = onOpenCalendar, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                color = paper, shape = RoundedCornerShape(7.dp)) {
                Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlantupiaEditorialMark("today", Modifier.size(24.dp))
                    Text("Schedule ↗", modifier = Modifier.weight(1f), style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

@Composable
internal fun PlantupiaCircleEditorialIntro() {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("THE CIRCLE · HUMAN STORIES", style = PlantupiaText.sectionLabel)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Better together", style = PlantupiaText.pageHeading.copy(fontSize = 36.sp, lineHeight = 40.sp))
                Text("Questions and experiences people choose to share. Your private plant record stays yours.",
                    style = PlantupiaText.body)
            }
            if (LocalDensity.current.fontScale < 1.3f) PlantupiaEditionArt("circle", Modifier.size(84.dp))
        }
        androidx.compose.material3.HorizontalDivider(color = PlantupiaColors.forest)
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
        else -> "Review inspection"
    }
    val dateLabel = runCatching {
        LocalDate.parse(dueOn).format(DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH))
    }.getOrDefault(dueOn)
    Surface(
        onClick = onOpenPlant,
        enabled = canOpenPlant,
        modifier = Modifier.fillMaxWidth().heightIn(min = 70.dp),
        color = background,
        border = BorderStroke(1.dp, accent.copy(alpha = .24f)),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaEditorialMark("today", Modifier.size(35.dp), accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(plantName, style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                Text(description, style = PlantupiaText.supporting)
                Text((if (due) "DUE · " else "NEXT · ") + dateLabel,
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

@Composable
internal fun PlantupiaShopPriceBand(currency: String, price: String, qaStock: Int) {
    Surface(
        color = Color(0xFFE9EDE3),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("PREVIEW PRODUCT PRICE", style = PlantupiaText.sectionLabel)
                Text("$currency $price", style = PlantupiaText.editorialHeading)
            }
            Text(
                if (qaStock > 0) "$qaStock in QA stock" else "Unavailable in QA",
                style = PlantupiaText.supporting,
            )
        }
    }
}

/** Label-led product content; never a prescription for the customer's plant. */
@Composable
internal fun PlantupiaNutritionEvidenceCard(
    label: String,
    directions: String,
    cautions: String,
    sourceReference: String,
    hasSourceLink: Boolean,
    onOpenSource: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFF0F0DD),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFFD9DAB8)),
    ) {
        Column(
            Modifier.padding(17.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantupiaEditorialMark("care", Modifier.size(51.dp), Color(0xFF657844))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PLANT NUTRITION · PRODUCT LABEL", style = PlantupiaText.sectionLabel)
                    Text("Read before using", style = PlantupiaText.editorialHeading)
                }
            }
            if (label.isNotBlank()) Text(label, style = PlantupiaText.body)
            if (directions.isNotBlank()) {
                Text("MANUFACTURER'S DIRECTIONS", style = PlantupiaText.sectionLabel)
                Text(directions, style = PlantupiaText.body)
            }
            if (cautions.isNotBlank()) {
                Text("PRECAUTIONS", style = PlantupiaText.sectionLabel.copy(color = Color(0xFF8A5734)))
                Text(cautions, style = PlantupiaText.body)
            }
            if (sourceReference.isNotBlank()) {
                if (hasSourceLink) {
                    androidx.compose.material3.TextButton(
                        onClick = onOpenSource,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) { Text("Manufacturer source · $sourceReference", color = PlantupiaColors.forest) }
                } else Text("Source · $sourceReference", style = PlantupiaText.supporting)
            }
            Text(
                "This is product information, not a recommendation to feed your plant today.",
                style = PlantupiaText.supporting,
            )
        }
    }
}

/**
 * First-visit acknowledgment appears ONLY after the real server create returns
 * a new plant ID. It does not impersonate staff botanical verification.
 */
@Composable
internal fun PlantupiaArrivalCard(onOpenCare: () -> Unit, onDismiss: () -> Unit, plantName: String = "") {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(visible = visible, enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(260))) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = PlantupiaColors.forest,
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PlantupiaEditorialMark("plant", Modifier.size(54.dp), ink = PlantupiaColors.sage)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("A RECORD BEGINS", style = PlantupiaText.sectionLabel.copy(color = PlantupiaColors.sage))
                        Text("A place for this plant", style = PlantupiaText.editorialHeading.copy(color = PlantupiaColors.paper))
                    }
                }
                if (plantName.isNotBlank()) Text(plantName, style = PlantupiaText.body.copy(color = PlantupiaColors.paper))
                Text("Your plant is saved in your private space. You can add a real observation or choose a check-in day.",
                    style = PlantupiaText.body.copy(color = PlantupiaColors.paper))
                androidx.compose.material3.Button(
                    onClick = onOpenCare,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = PlantupiaColors.paper, contentColor = PlantupiaColors.forest),
                ) { Text("Explore care →") }
                androidx.compose.material3.TextButton(
                    onClick = onDismiss, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Not now", color = PlantupiaColors.paper) }
            }
        }
    }
}
