package com.nova.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.R

/**
 * REVIEW-ONLY design tool — no public route, account mutation, default picker
 * change or production profile identity switch. The two original illustrations
 * are vector assets converted from the original SVG studies in docs/art-lab.
 *
 * Open in Android Studio Compose Preview to compare an actual native-rendered
 * 96dp portrait and its 40dp miniature, rather than judging the SVG board alone.
 */
@Composable
private fun ArtLabVectorSample(
    name: String,
    resource: Int,
    resourceMicro: Int,
) {
    Column(
        modifier = Modifier.width(112.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = "$name vector portrait study, 96 dp",
            modifier = Modifier.size(96.dp).clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(resourceMicro),
            contentDescription = "$name vector portrait study, 40 dp",
            modifier = Modifier.size(40.dp).clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
        Image(
            painter = painterResource(resourceMicro),
            contentDescription = "$name simplified micro portrait study, 24 dp",
            modifier = Modifier.size(24.dp).clip(CircleShape),
            contentScale = ContentScale.Fit,
        )
        Text(name, fontSize = 12.sp, color = Color(0xFF173B31))
    }
}

@Preview(name = "Plantupia / Portrait Lab A-B", widthDp = 430, heightDp = 360, showBackground = true)
@Composable
private fun PlantupiaPortraitArtLabPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3EEDE))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Text(
            "PORTRAIT LAB / NATIVE COMPARISON",
            fontSize = 12.sp,
            color = Color(0xFF173B31),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.width(112.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PlantupiaAvatar(
                    userId = 100L,
                    displayName = "Existing portrait",
                    photoUrl = "",
                    size = 96.dp,
                    chosenVariant = 0,
                )
                PlantupiaAvatar(
                    userId = 100L,
                    displayName = "Existing portrait",
                    photoUrl = "",
                    size = 40.dp,
                    chosenVariant = 0,
                )
                Text("Current", fontSize = 12.sp, color = Color(0xFF173B31))
            }
            ArtLabVectorSample(
                "The Window",
                R.drawable.plantupia_artlab_window,
                R.drawable.plantupia_artlab_window_micro,
            )
            ArtLabVectorSample(
                "The Courtyard",
                R.drawable.plantupia_artlab_courtyard,
                R.drawable.plantupia_artlab_courtyard_micro,
            )
        }
        Text(
            "Art prototype only · existing profile selection and photos stay unchanged",
            fontSize = 11.sp,
            color = Color(0xFF526859),
        )
    }
}

/**
 * Review the whole persisted eight-variant family before approving a single
 * character. Sizes reveal different problems: 24/40 for Circle and comments,
 * 96 for a selected person's identity. No fake photos or user data.
 */
@Preview(name = "Plantupia / Full Eight Portraits", widthDp = 430, heightDp = 580, showBackground = true)
@Composable
private fun PlantupiaFullPortraitFamilyPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF3EEDE))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("ALL EIGHT / SAME ART FAMILY", fontSize = 13.sp, color = Color(0xFF173B31))
        for (rowIndex in 0..1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                for (columnIndex in 0..3) {
                    val variant = rowIndex * 4 + columnIndex
                    Column(
                        modifier = Modifier.width(94.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        PlantupiaAvatar(
                            userId = (variant + 1).toLong(),
                            displayName = "Person ${variant + 1}",
                            photoUrl = "",
                            size = 92.dp,
                            chosenVariant = variant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                            PlantupiaAvatar(
                                userId = (variant + 1).toLong(),
                                displayName = "Person ${variant + 1}",
                                photoUrl = "",
                                size = 40.dp,
                                chosenVariant = variant,
                            )
                            PlantupiaAvatar(
                                userId = (variant + 1).toLong(),
                                displayName = "Person ${variant + 1}",
                                photoUrl = "",
                                size = 24.dp,
                                chosenVariant = variant,
                            )
                        }
                        Text("STYLE 0${variant + 1}", fontSize = 10.sp, color = Color(0xFF526859))
                    }
                }
            }
        }
        Text(
            "92 hero / 40 micro / 24 micro · technical preview, not art approval",
            fontSize = 11.sp,
            color = Color(0xFF526859),
        )
    }
}
