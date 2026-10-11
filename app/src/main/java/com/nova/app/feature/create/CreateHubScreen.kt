package com.nova.app.feature.create

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nova.app.feature.memories.MemoriesRail
import com.nova.app.feature.pulse.PulseRail
import com.nova.app.feature.rooms.RoomsRail
import com.nova.app.feature.stories.StoriesRail
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaBottomBar
import com.nova.app.ui.components.NovaCard
import com.nova.app.ui.components.NovaTab
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaType
import kotlinx.coroutines.launch


private data class CreateHubAction(
    val title: String,
    val subtitle: String,
    val icon: NovaIconAsset,
    val onClick: () -> Unit,
)


/** Truthful central Create destination backed only by existing Nova flows. */
@Composable
fun CreateHubScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    onCreatePost: () -> Unit,
    onOpenReels: () -> Unit,
    onPersonClick: (String) -> Unit,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    // Private care and public media require different intentional entry points.
    // Nova engines remain functional; secondary formats are opened on demand.
    var activeFormat by remember { mutableStateOf<String?>(null) }
    var showOtherFormats by remember { mutableStateOf(false) }

    val primary = listOf(
        CreateHubAction(
            "لحظة جديدة لنباتك",
            "سجل ملاحظة أو رعاية في تاريخ نباتك الخاص",
            NovaIconAsset.Home,
            onHomeClick,
        ),
        CreateHubAction(
            "سؤال عن نباتك",
            "ابدأ من النبات؛ مشاركة الصورة للعامة باختيارك",
            NovaIconAsset.Orbit,
            onHomeClick,
        ),
        CreateHubAction(
            "لحظة عامة",
            "منشور للناس — لا يربط تلقائيًا بمساحة خاصة",
            NovaIconAsset.Create,
            onCreatePost,
        ),
        CreateHubAction(
            "فيديو",
            "شارك تجربة أو لحظة بالفيديو",
            NovaIconAsset.Reels,
            onOpenReels,
        ),
    )
    val otherFormats = listOf(
        CreateHubAction("Story", "لحظة مؤقتة لمدة 24 ساعة", NovaIconAsset.Story) { activeFormat = "story" },
        CreateHubAction("Pulse", "لحظة قصيرة مع الناس", NovaIconAsset.Orbit) { activeFormat = "pulse" },
        CreateHubAction("Gathering", "تجمّع اجتماعي حول اهتمام مشترك", NovaIconAsset.Room) { activeFormat = "gathering" },
        CreateHubAction("Memory", "ارجع للحظات عبر الزمن", NovaIconAsset.Memory) { activeFormat = "memory" },
    )

    Scaffold(
        containerColor = NovaBackground,
        bottomBar = {
            NovaBottomBar(
                selected = NovaTab.Create,
                onHomeClick = onHomeClick,
                onOrbitClick = onOrbitClick,
                onCreateClick = {},
                onProfileClick = onProfileClick,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding(),
            contentPadding = PaddingValues(
                start = NovaSpacing.lg,
                end = NovaSpacing.lg,
                top = NovaSpacing.lg,
                bottom = NovaSpacing.xxl,
            ),
            verticalArrangement = Arrangement.spacedBy(NovaSpacing.lg),
        ) {
            item(key = "living-create-heading") {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    Text("إيه اللي حصل في مساحتك؟", color = NovaInk, style = NovaType.pageTitle)
                    Text(
                        "احفظ اللي حصل للنبات عندك، أو شارك لحظة مع الدائرة. القرار قرارك.",
                        color = NovaMuted,
                        style = NovaType.bodyCompact,
                    )
                }
            }
            item(key = "living-create-actions") {
                Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                    primary.forEach { action -> CreateActionCard(action) }
                }
            }
            item(key = "living-create-other") {
                TextButton(onClick = {
                    showOtherFormats = !showOtherFormats
                    if (!showOtherFormats) activeFormat = null
                }) {
                    Text(
                        if (showOtherFormats) "إخفاء وسائل المشاركة الأخرى ↑" else "وسائل مشاركة أخرى ↓",
                        color = NovaAccent,
                    )
                }
                if (showOtherFormats) {
                    Column(verticalArrangement = Arrangement.spacedBy(NovaSpacing.sm)) {
                        otherFormats.forEach { action -> CreateActionCard(action) }
                    }
                }
            }
            if (showOtherFormats && activeFormat != null) {
                item(key = "living-create-format") {
                    when (activeFormat) {
                        "story" -> {
                            CreateSectionLabel("Story", "صورة أو فيديو أو نص — لمدة 24 ساعة")
                            StoriesRail(
                                displayName = displayName,
                                username = username,
                                avatarUrl = avatarUrl,
                                onSessionExpired = onSessionExpired,
                            )
                        }
                        "pulse" -> {
                            CreateSectionLabel("Pulse", "لحظات قصيرة")
                            PulseRail(
                                displayName = displayName,
                                username = username,
                                avatarUrl = avatarUrl,
                                onSessionExpired = onSessionExpired,
                            )
                        }
                        "gathering" -> {
                            CreateSectionLabel("Gathering", "تجمعات المجتمع")
                            RoomsRail(onPersonClick = onPersonClick, onSessionExpired = onSessionExpired)
                        }
                        "memory" -> {
                            CreateSectionLabel("Memory", "لحظاتك القديمة")
                            MemoriesRail(onPersonClick = onPersonClick, onSessionExpired = onSessionExpired)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun CreateActionCard(action: CreateHubAction) {
    NovaCard(onClick = action.onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.md),
        ) {
            Surface(
                shape = CircleShape,
                color = NovaAccentSoft,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                NovaIcon(
                    asset = action.icon,
                    contentDescription = null,
                    tint = NovaAccent,
                    modifier = Modifier.padding(NovaSpacing.sm).size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(action.title, color = NovaInk, style = NovaType.label)
                Text(action.subtitle, color = NovaMuted, style = NovaType.meta)
            }
            NovaIcon(
                asset = NovaIconAsset.Back,
                contentDescription = null,
                tint = NovaMuted,
                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = -1f },
            )
        }
    }
}


@Composable
private fun CreateSectionLabel(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = NovaSpacing.md)) {
        Text(title, color = NovaInk, style = NovaType.sectionTitle)
        Text(subtitle, color = NovaMuted, style = NovaType.meta)
    }
}
