package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

/** One quiet starting point; suggested questions and the composer carry the interaction. */
@Composable
internal fun PlantupiaAiIntro() {
    Column(Modifier.fillMaxWidth().padding(vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NovaIcon(asset = NovaIconAsset.AI, contentDescription = null,
            tint = PlantupiaColors.forest, modifier = Modifier.size(40.dp))
        Text("How can I help?", style = PlantupiaText.editorialHeading)
    }
}
