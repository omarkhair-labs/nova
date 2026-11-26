package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Present server-owned amounts without truncating prices on small screens or at large text sizes. */
@Composable
internal fun PlantupiaAmountRow(label: String, amount: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        if (availableWidth / LocalDensity.current.fontScale < 248.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, style = PlantupiaText.body)
                amount()
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label, style = PlantupiaText.body, modifier = Modifier.weight(1f))
                Box(Modifier.widthIn(max = availableWidth * .55f)) { amount() }
            }
        }
    }
}
