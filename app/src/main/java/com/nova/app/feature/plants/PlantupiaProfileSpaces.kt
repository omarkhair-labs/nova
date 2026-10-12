package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset

private sealed interface PrivateSpacesSummary {
    data object Loading : PrivateSpacesSummary
    data object Unavailable : PrivateSpacesSummary
    data class Loaded(
        val spaces: List<PlantupiaSpace>,
        val plants: List<PlantupiaPlant>,
    ) : PrivateSpacesSummary
}

/**
 * Only used on the authenticated user's own ProfileScreen, NEVER PersonScreen.
 * The existing Nova social profile remains public; this separate section
 * fetches authorized private Plantupia records and never posts their photos.
 */
@Composable
fun PlantupiaProfileSpaces(onOpenSpaces: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context.applicationContext) {
        PlantupiaRepository(context.applicationContext)
    }
    var state by remember { mutableStateOf<PrivateSpacesSummary>(PrivateSpacesSummary.Loading) }
    var retry by remember { mutableIntStateOf(0) }

    LaunchedEffect(repository, retry) {
        state = PrivateSpacesSummary.Loading
        val spaceResponse = repository.spaces()
        if (spaceResponse !is ApiResult.Success) {
            state = PrivateSpacesSummary.Unavailable
        } else {
            when (val plantResponse = repository.plants()) {
                is ApiResult.Success -> {
                    state = PrivateSpacesSummary.Loaded(spaceResponse.value, plantResponse.value)
                }
                is ApiResult.Failure -> state = PrivateSpacesSummary.Unavailable
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "PRIVATE / مساحاتك الخاصة",
            style = PlantupiaText.sectionLabel,
        )
        when (val current = state) {
            PrivateSpacesSummary.Loading -> {
                Surface(
                    color = PlantupiaColors.softSage,
                    shape = PlantupiaShapes.card,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        CircularProgressIndicator(
                            color = PlantupiaColors.forest,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                        Text("بنحمّل مساحاتك الخاصة…", style = PlantupiaText.body)
                    }
                }
            }
            PrivateSpacesSummary.Unavailable -> {
                Surface(
                    color = PlantupiaColors.softSage,
                    shape = PlantupiaShapes.card,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("مساحاتك مش ظاهرة دلوقتي", style = PlantupiaText.editorialHeading)
                        Text(
                            "مش هنعرِض أرقام أو صور تجريبية مكان بياناتك. جرّب تحميلها تاني لما الخدمة تبقى متاحة.",
                            style = PlantupiaText.supporting,
                        )
                        TextButton(onClick = { retry++ }) {
                            Text("إعادة المحاولة", color = PlantupiaColors.forest)
                        }
                    }
                }
            }
            is PrivateSpacesSummary.Loaded -> {
                val activePlants = current.plants.filter { it.active }
                // Authorized server media only: no invented installations, client
                // photography, inferred plant health or public profile publishing.
                val photos = activePlants.filter { it.coverPhoto.isNotBlank() }
                    .distinctBy { it.coverPhoto }
                    .take(6)

                Surface(
                    onClick = onOpenSpaces,
                    color = PlantupiaColors.softSage,
                    shape = PlantupiaShapes.card,
                    border = BorderStroke(1.dp, PlantupiaColors.border),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        Text(
                            if (current.spaces.isEmpty()) "مكان للحياة اللي هتوثّقها"
                            else "المكان اللي بتشارك في رعايته",
                            style = PlantupiaText.editorialHeading,
                        )
                        if (current.spaces.isEmpty()) {
                            Text(
                                "لسه مفيش مساحات مسجلة. ابدأ بمساحة خاصة، وبعدها ضيف نباتاتك وصورها وسجلّها.",
                                style = PlantupiaText.body,
                            )
                            EmptySpacesIllustration()
                        } else {
                            Text(
                                "${current.spaces.size} مساحة · ${activePlants.size} وحدة نباتية",
                                style = PlantupiaText.supporting,
                            )
                            if (photos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    items(photos, key = { it.id }) { plant ->
                                        Surface(
                                            shape = PlantupiaShapes.inset,
                                            color = PlantupiaColors.cream,
                                            modifier = Modifier.width(118.dp).height(144.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.BottomStart) {
                                                AsyncImage(
                                                    model = plant.coverPhoto,
                                                    contentDescription = "صورة خاصة للنبات ${plant.name}",
                                                    modifier = Modifier.fillMaxWidth().height(144.dp),
                                                    contentScale = ContentScale.Crop,
                                                )
                                                Surface(
                                                    color = PlantupiaColors.paper.copy(alpha = 0.92f),
                                                    shape = PlantupiaShapes.badge,
                                                    modifier = Modifier.padding(7.dp),
                                                ) {
                                                    Text(
                                                        text = plant.name,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                        color = PlantupiaColors.forestText,
                                                        style = PlantupiaText.sectionLabel,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                EmptySpacesIllustration()
                                Text(
                                    "أضف صورة حقيقية لنباتك علشان تبدأ حكاية مساحتك البصرية.",
                                    style = PlantupiaText.supporting,
                                )
                            }
                            Text(
                                current.spaces.take(3).joinToString(" · ") { it.name } +
                                    if (current.spaces.size > 3) " · +${current.spaces.size - 3}" else "",
                                style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (current.spaces.isEmpty()) "أنشئ أول مساحة" else "استكشف مساحاتك",
                                color = PlantupiaColors.forest,
                                style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                            )
                            NovaIcon(
                                asset = NovaIconAsset.Back,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = PlantupiaColors.forest,
                            )
                        }
                    }
                }
            }
        }
        Text(
            "المساحات والصور وسجل الرعاية خاصين بالأشخاص المصرّح لهم، ومش جزء من منشورات بروفايلك العام.",
            style = PlantupiaText.supporting,
        )
    }
}

/** Deliberate drawn empty media state; never substitutes a fake plant photograph. */
@Composable
private fun EmptySpacesIllustration() {
    Surface(
        color = PlantupiaColors.cream,
        shape = PlantupiaShapes.inset,
        modifier = Modifier.fillMaxWidth().height(112.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            PlantupiaBrandGlyph(modifier = Modifier.size(56.dp))
        }
    }
}
