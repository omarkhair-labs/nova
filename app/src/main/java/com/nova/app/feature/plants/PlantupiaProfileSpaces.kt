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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.plants.data.PlantupiaSpace

private sealed interface PrivateSpacesSummary {
    data object Loading : PrivateSpacesSummary
    data object Unavailable : PrivateSpacesSummary
    data class Loaded(
        val spaces: List<PlantupiaSpace>,
        val plants: List<PlantupiaPlant>,
    ) : PrivateSpacesSummary
}

internal fun mayFetchPrivateSpaces(accountId: Long): Boolean = accountId > 0L

/**
 * Only used on the authenticated user's own ProfileScreen, NEVER PersonScreen.
 * The existing Nova social profile remains public; this separate section
 * fetches authorized private Plantupia records and never posts their photos.
 */
@Composable
fun PlantupiaProfileSpaces(accountId: Long, onOpenSpaces: () -> Unit) {
    // A public/guest/unknown identity must never request or render private data.
    if (!mayFetchPrivateSpaces(accountId)) return

    val context = LocalContext.current
    // Each authenticated account owns a distinct repository and remembered UI state.
    // Rapid logout/account switching invalidates the prior LaunchedEffect request.
    val repository = remember(context.applicationContext, accountId) {
        PlantupiaRepository(context.applicationContext)
    }
    var state by remember(accountId) { mutableStateOf<PrivateSpacesSummary>(PrivateSpacesSummary.Loading) }
    var retry by remember(accountId) { mutableIntStateOf(0) }

    LaunchedEffect(repository, accountId, retry) {
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

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("YOUR LIVING PLACES · PRIVATE", style = PlantupiaText.sectionLabel)
            if (state is PrivateSpacesSummary.Loaded) {
                TextButton(onClick = { retry += 1 }) {
                    Text("Refresh", color = PlantupiaColors.forest)
                }
            }
        }
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
                        Text("Loading your private spaces…", style = PlantupiaText.body)
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
                        Text("Your spaces are unavailable", style = PlantupiaText.editorialHeading)
                        Text(
                            "We won't substitute sample photos or counts for your real data. Try again when the service is available.",
                            style = PlantupiaText.supporting,
                        )
                        TextButton(onClick = { retry++ }) {
                            Text("Retry", color = PlantupiaColors.forest)
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
                    .take(3)

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
                            "Your private places",
                            style = PlantupiaText.editorialHeading,
                        )
                        if (current.spaces.isEmpty()) {
                            Text(
                                "No spaces yet. Add a place to begin recording its plants and changes.",
                                style = PlantupiaText.body,
                            )
                            EmptySpacesIllustration()
                        } else {
                            Text(
                                "${current.spaces.size} spaces · ${activePlants.size} plants",
                                style = PlantupiaText.supporting,
                            )
                            if (photos.isNotEmpty()) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                                    items(photos, key = { it.id }) { plant ->
                                        Surface(
                                            shape = PlantupiaShapes.inset,
                                            color = PlantupiaColors.cream,
                                            modifier = Modifier.width(92.dp).height(110.dp),
                                        ) {
                                            Box(contentAlignment = Alignment.BottomStart) {
                                                PlantupiaEvidencePhoto(
                                                    photoUrl = plant.coverPhoto,
                                                    photoDescription = "Private uploaded photo of ${plant.name}",
                                                    modifier = Modifier.fillMaxWidth().height(110.dp),
                                                    cornerRadius = 14.dp,
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
                                    "Add a real photo of your plant to begin its visual history.",
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
                                if (current.spaces.isEmpty()) "Create first space" else "Explore your spaces",
                                color = PlantupiaColors.forest,
                                style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                "→",
                                color = PlantupiaColors.forest,
                                style = PlantupiaText.editorialHeading,
                            )
                        }
                    }
                }
            }
        }
        Text(
            "Your place and care records stay separate from what you share publicly.",
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
            PlantupiaEmptyPlaceArt(modifier = Modifier.fillMaxWidth().height(112.dp))
        }
    }
}
