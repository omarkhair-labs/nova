package com.nova.app.feature.plants

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaTab

/**
 * Real primary AI root, not a link to the old Circle feed and not a fake chatbot.
 * Uses the exact existing authorized API, entitlement checks and per-question
 * consent from PlantupiaAssistantSheet, rendered as a full native screen.
 * Circle remains reachable from the contextual conversation and Create.
 */
@Composable
internal fun PlantupiaAiRoute(
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    Scaffold(
        containerColor = PlantupiaColors.paper,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.AI,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 17.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("ASK · WITH CONTEXT, NOT ASSUMPTIONS", style = PlantupiaText.sectionLabel)
                TextButton(onClick = onProfile) { Text("My profile") }
            }
            PlantupiaAssistantSheet(
                plant = null,
                repository = repository,
                onDismiss = onToday,
                fullPage = true,
                onCircle = onCircle,
            )
        }
    }
}
