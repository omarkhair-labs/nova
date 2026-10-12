package com.nova.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.feature.plants.PlantupiaCreateIntent
import com.nova.app.feature.plants.PlantupiaCreateMode
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaElevation
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType


/** Five native destinations: living space / actual care / create / circle / self.
 * The + is a visible Create sheet, with the original full CreateHub retained.
 * Inbox remains the existing real messaging destination, not AI chat.
 */
@Composable
fun NovaBottomBar(
    selected: NovaTab?,
    onHomeClick: () -> Unit,
    onOrbitClick: () -> Unit,
    onCreateClick: () -> Unit,
    onProfileClick: () -> Unit,
    onInboxClick: (() -> Unit)? = null,
    messagesUnreadCount: Int? = null,
    containerColor: Color = NovaSurface,
    inactiveContentColor: Color = NovaMuted,
) {
    var showQuickCreate by remember { mutableStateOf(false) }
    if (showQuickCreate) {
        ModalBottomSheet(
            onDismissRequest = { showQuickCreate = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = NovaSurface,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("إنشاء", color = NovaAccent, style = NovaType.screenTitle)
                Text("اختار الفعل، والخاص هيفضل خاص لحد ما تختار تنشر.", color = NovaMuted, style = NovaType.bodyCompact)
                QuickCreateAction("سجّل رعاية أو ملاحظة", "في سجل نباتك الخاص", NovaIconAsset.Edit) {
                    showQuickCreate = false
                    PlantupiaCreateIntent.request(PlantupiaCreateMode.OBSERVATION)
                    onHomeClick()
                }
                QuickCreateAction("أضف نبات", "اسم، مكان، صورة", NovaIconAsset.Photo) {
                    showQuickCreate = false
                    PlantupiaCreateIntent.requestAddPlant()
                    onHomeClick()
                }
                QuickCreateAction("اسأل الناس عن نبات", "سؤال عام بموافقتك", NovaIconAsset.Orbit) {
                    showQuickCreate = false
                    PlantupiaCreateIntent.request(PlantupiaCreateMode.QUESTION)
                    onHomeClick()
                }
                HorizontalDivider(color = NovaMuted.copy(alpha = 0.2f))
                QuickCreateAction("Story، فيديو، أو منشور", "استخدم أدوات Nova الاجتماعية", NovaIconAsset.Story) {
                    showQuickCreate = false
                    onCreateClick()
                }
                TextButton(onClick = { showQuickCreate = false; onCreateClick() }) {
                    Text("كل أدوات الإنشاء ←", color = NovaAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
    Surface(
        color = containerColor,
        shadowElevation = NovaElevation.floating,
        tonalElevation = NovaElevation.flat,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = NovaSpacing.sm, vertical = NovaSpacing.xs),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaTabItem(
                label = "Home",
                displayLabel = "مساحتي",
                icon = NovaIconAsset.Home,
                selected = selected == NovaTab.Home,
                inactiveContentColor = inactiveContentColor,
                onClick = onHomeClick,
            )
            NovaTabItem(
                label = "Care",
                displayLabel = "الرعاية",
                icon = NovaIconAsset.Like,
                selected = selected == NovaTab.Care,
                inactiveContentColor = inactiveContentColor,
                onClick = { NovaRootNavigationSignal.request(NovaRootTab.Care) },
            )
            NovaTabItem(
                label = "Circle",
                displayLabel = "الدائرة",
                icon = NovaIconAsset.Orbit,
                selected = selected == NovaTab.Orbit,
                inactiveContentColor = inactiveContentColor,
                onClick = onOrbitClick,
            )
            NovaCreateTabItem(
                selected = selected == NovaTab.Create,
                onClick = { showQuickCreate = true },
            )
            NovaTabItem(
                label = "Profile",
                displayLabel = "أنا",
                icon = NovaIconAsset.Profile,
                selected = selected == NovaTab.Profile,
                inactiveContentColor = inactiveContentColor,
                onClick = onProfileClick,
            )
        }
    }
}


@Composable
private fun NovaCreateTabItem(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "nova-create-tab-scale",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else NovaAccent,
        label = "nova-create-tab-color",
    )

    Box(
        modifier = Modifier.widthIn(min = 58.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .size(52.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            shape = CircleShape,
            color = containerColor,
            shadowElevation = NovaElevation.raised,
        ) {
            Box(contentAlignment = Alignment.Center) {
                NovaIcon(
                    asset = NovaIconAsset.Create,
                    contentDescription = "وثّق لحظة أو شاركها",
                    tint = Color.White,
                    modifier = Modifier.size(25.dp),
                )
            }
        }
    }
}


@Composable
private fun NovaTabItem(
    label: String,
    displayLabel: String = label,
    icon: NovaIconAsset,
    selected: Boolean,
    onClick: () -> Unit,
    badgeCount: Int = 0,
    inactiveContentColor: Color = NovaMuted,
) {
    val contentColor by animateColorAsState(
        targetValue = if (selected) NovaAccent else inactiveContentColor,
        label = "nova-tab-content",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        label = "nova-tab-icon-scale",
    )

    Surface(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 58.dp),
        color = Color.Transparent,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = NovaSpacing.sm, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                NovaIcon(
                    asset = icon,
                    contentDescription = displayLabel,
                    tint = contentColor,
                    modifier = Modifier
                        .size(21.dp)
                        .graphicsLayer {
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
                if (badgeCount > 0) {
                    Surface(shape = CircleShape, color = NovaAccent) {
                        Text(
                            text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                            modifier = Modifier.padding(horizontal = NovaSpacing.xs, vertical = 1.dp),
                            color = Color.White,
                            style = NovaType.badge,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(NovaSpacing.xxs))
            Text(
                text = displayLabel,
                color = contentColor,
                style = if (selected) NovaType.navigationLabelSelected else NovaType.navigationLabel,
            )
        }
    }
}


@Composable
private fun QuickCreateAction(label: String, subtitle: String, icon: NovaIconAsset, onClick: () -> Unit) {
    Surface(onClick = onClick, color = NovaBackground, shape = RoundedCornerShape(18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NovaIcon(asset = icon, contentDescription = null, tint = NovaAccent, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = NovaAccent, style = NovaType.label)
                Text(subtitle, color = NovaMuted, style = NovaType.meta)
            }
        }
    }
}

enum class NovaTab {
    Home,
    Care,
    Orbit,
    Create,
    Inbox,
    Profile,
}
