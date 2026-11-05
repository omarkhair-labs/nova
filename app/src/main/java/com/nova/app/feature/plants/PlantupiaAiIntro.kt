package com.nova.app.feature.plants

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Quiet generative-looking original symbol, NOT an AI diagnosis or real botanical evidence. */
@Composable
internal fun PlantupiaAiIntro() {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val emerge by animateFloatAsState(
        targetValue = if (appeared) 1f else .90f,
        animationSpec = spring(dampingRatio = .86f, stiffness = 330f),
        label = "ai-quiet-arrival",
    )
    Surface(color = Color(0xFFE6EDE3), shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Canvas(
                modifier = Modifier.size(92.dp)
                    .graphicsLayer { scaleX = emerge; scaleY = emerge }
                    .clearAndSetSemantics { },
            ) {
                val s = size.minDimension
                val c = Offset(s * .5f, s * .5f)
                drawCircle(Color(0xFF678E71).copy(alpha = .34f), radius = s*.43f,
                    center = c, style = Stroke(width = s*.018f))
                drawCircle(Color(0xFF123B2E).copy(alpha = .45f), radius = s*.28f,
                    center = c, style = Stroke(width = s*.022f))
                drawLine(Color(0xFF123B2E),
                    Offset(s*.5f,s*.80f), Offset(s*.5f,s*.34f), strokeWidth=s*.032f,
                    cap=StrokeCap.Round)
                drawOval(Color(0xFF678E71),
                    topLeft=Offset(s*.24f,s*.26f),
                    size=androidx.compose.ui.geometry.Size(s*.25f,s*.15f))
                drawOval(Color(0xFF678E71),
                    topLeft=Offset(s*.50f,s*.19f),
                    size=androidx.compose.ui.geometry.Size(s*.23f,s*.15f))
                drawCircle(Color(0xFFD9A64E),radius=s*.05f,center=Offset(s*.86f,s*.20f))
                drawCircle(Color(0xFF123B2E),radius=s*.023f,center=Offset(s*.12f,s*.63f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                Text("A QUESTION, NOT A DIAGNOSIS", style = PlantupiaText.sectionLabel)
                Text("Grow your understanding", style = PlantupiaText.editorialHeading.copy(fontSize = 22.sp))
                Text("Ask about conditions, changes and what to inspect next.",
                    style = PlantupiaText.supporting)
            }
        }
    }
}
