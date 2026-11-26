package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp

/** Full-screen private evidence workspace. Caller retains data and session authority. */
@Composable
internal fun PlantupiaLivingWorkspace(
    label: String, title: String, context: String, busy: Boolean,
    close: () -> Unit, navigationKey: Any? = null, content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = { if (!busy) close() },
        properties = DialogProperties(usePlatformDefaultWidth = false,
            dismissOnClickOutside = false, dismissOnBackPress = !busy)) {
        Surface(color = PlantupiaColors.paper, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                TextButton(onClick = close, enabled = !busy) { Text("Back", color = PlantupiaColors.forest) }
                val scroll = rememberScrollState()
                LaunchedEffect(navigationKey) { scroll.scrollTo(0) }
                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(label, style = PlantupiaText.sectionLabel)
                    Text(title, style = PlantupiaText.pageHeading)
                    if (context.isNotBlank()) Text(context, style = PlantupiaText.supporting)
                    HorizontalDivider(color = PlantupiaColors.border)
                    content()
                }
            }
        }
    }
}
