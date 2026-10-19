package com.nova.app.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText

/**
 * One consistent editorial contract for the owner's Me and a visited Person.
 * The caller, not the component, is responsible for permission checks:
 * private stats and moments must never be passed to a visitor without access.
 */
@Composable
fun PlantupiaProfileSectionHeading(
    eyebrow: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(eyebrow, style = PlantupiaText.sectionLabel)
        if (!description.isNullOrBlank()) {
            Text(description, style = PlantupiaText.supporting)
        }
    }
}

@Composable
fun PlantupiaProfileSocialStats(
    moments: Int,
    followers: Int,
    following: Int,
    modifier: Modifier = Modifier,
    onFollowersClick: (() -> Unit)? = null,
    onFollowingClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PlantupiaColors.paper,
        shape = PlantupiaShapes.card,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 15.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantupiaProfileStat(moments, "Moments", Modifier.weight(1f), null)
            PlantupiaProfileStat(followers, "Followers", Modifier.weight(1f), onFollowersClick)
            PlantupiaProfileStat(following, "Following", Modifier.weight(1f), onFollowingClick)
        }
    }
}

@Composable
private fun PlantupiaProfileStat(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)?,
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable(
                    onClick = onClick,
                    role = Role.Button,
                ) else Modifier
            )
            .semantics(mergeDescendants = true) {
                contentDescription = "${value.coerceAtLeast(0)} $label"
            }
            .padding(vertical = 9.dp, horizontal = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = value.coerceAtLeast(0).toString(),
            style = PlantupiaText.editorialHeading.copy(
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = label,
            style = PlantupiaText.supporting,
            textAlign = TextAlign.Center,
        )
    }
}
