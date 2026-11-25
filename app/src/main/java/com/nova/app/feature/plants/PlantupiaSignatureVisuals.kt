package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import com.nova.app.R
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
            val resource = when (topic) {
                "water" -> R.drawable.plantupia_signature_plate_water
                "light" -> R.drawable.plantupia_signature_plate_light
                "soil" -> R.drawable.plantupia_signature_plate_soil
                "humidity" -> R.drawable.plantupia_signature_plate_humidity
                "fertilizing" -> R.drawable.plantupia_signature_plate_fertilizing
                "pruning" -> R.drawable.plantupia_signature_plate_pruning
                "repotting" -> R.drawable.plantupia_signature_plate_repotting
                "inspection" -> R.drawable.plantupia_signature_plate_inspection
                "warnings" -> R.drawable.plantupia_signature_plate_warnings
                else -> null
            }
            if (resource != null) {
                Image(
                    painter = painterResource(resource),
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().height(172.dp),
                )
            } else {
                Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) {
                    CareChapterIllustration(topic, palette.ink, Modifier.size(80.dp).clearAndSetSemantics { })
                }
            }
            Text("Topic illustration · read the reviewed instructions below",
                style = PlantupiaText.supporting.copy(color = palette.ink))
        }
    }
}
