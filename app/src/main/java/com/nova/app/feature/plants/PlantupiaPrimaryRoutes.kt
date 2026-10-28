package com.nova.app.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun PlantupiaPlantsRoute(
    accountId: Long,
    onCreatePost: () -> Unit,
    onOpenLinkedPost: (Long, () -> Boolean) -> Unit,
    onToday: () -> Unit,
    onCircle: () -> Unit,
    onCreate: () -> Unit,
    onProfile: () -> Unit,
) {
    Scaffold(
        containerColor = PlantupiaColors.cream,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Plants,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .background(PlantupiaColors.cream)
                .padding(horizontal = 17.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("PLANTUPIA", style = PlantupiaText.sectionLabel)
                TextButton(onClick = onProfile) { Text("Me", color = PlantupiaColors.forest) }
            }
            PlantupiaClientSection(
                accountId = accountId,
                onCommunityCreateClick = onCreatePost,
                onOpenLinkedPost = onOpenLinkedPost,
                section = PlantupiaPrimarySection.PLANTS,
            )
        }
    }
}

@Composable
fun PlantupiaShopRoute(
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
                selected = NovaTab.Shop,
                onHomeClick = onToday,
                onOrbitClick = onCircle,
                onCreateClick = onCreate,
                onProfileClick = onProfile,
            )
        },
    ) { padding ->
        PlantupiaQaShop(
            repository = repository,
            onDismiss = onToday,
            fullPage = true,
            modifier = Modifier.padding(padding).statusBarsPadding(),
        )
    }
}
