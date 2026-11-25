package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp

/** A private specimen passport. Decorative marks never stand in for evidence photos. */
@Composable
internal fun PlantupiaSignatureIdentity(
    name: String,
    context: String,
    identity: String,
    qa: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(name, style = PlantupiaText.pageHeading)
        if (context.isNotBlank()) Text(context, style = PlantupiaText.body)
        Surface(
            color = PlantupiaColors.softSage,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, PlantupiaColors.border),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlantupiaEditorialMark("plant", Modifier.size(30.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("PLANT IDENTITY", style = PlantupiaText.sectionLabel)
                    Text(signatureIdentityLabel(identity, qa), style = PlantupiaText.supporting)
                }
            }
        }
    }
}

/** Topic-specific original pictogram on a ruled field plate; never a scientific measurement. */
@Composable
internal fun PlantupiaSignatureFieldPlate(topic: String) {
    val palette = careChapterPalette(topic)
    Surface(
        color = palette.background,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, palette.ink.copy(alpha = .25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text("FIELD PLATE / " + palette.label.uppercase(),
                style = PlantupiaText.sectionLabel.copy(color = palette.ink))
            Box(
                Modifier.fillMaxWidth().height(110.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxWidth().height(110.dp).clearAndSetSemantics { }) {
                    val r = size.height * .45f
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(palette.ink.copy(alpha = .12f), r, c, style = Stroke(1.dp.toPx()))
                    drawCircle(palette.ink.copy(alpha = .08f), r * .75f, c, style = Stroke(1.dp.toPx()))
                    for (i in 0..4) {
                        val y = size.height * (i + 1) / 6f
                        drawLine(palette.ink.copy(alpha = .15f), Offset(0f, y), Offset(size.width * .1f, y), 1.dp.toPx())
                        drawLine(palette.ink.copy(alpha = .15f), Offset(size.width * .9f, y), Offset(size.width, y), 1.dp.toPx())
                    }
                }
                CareChapterIllustration(topic, palette.ink, Modifier.size(88.dp).clearAndSetSemantics { })
            }
            Text("Topic illustration · read the reviewed instructions below",
                style = PlantupiaText.supporting.copy(color = palette.ink))
        }
    }
}
