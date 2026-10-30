package com.nova.app.feature.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nova.app.ui.components.NovaKeyboardAwareFormPage
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.components.PlantupiaAvatar
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMuted

@Composable
fun EditProfileScreen(
    displayName: String,
    userId: Long = 0L,
    avatarVariant: Int = -1,
    username: String,
    avatarUrl: String,
    bio: String,
    location: String,
    link: String,
    interests: List<String>,
    profileTheme: String,
    showOrbit: Boolean,
    isVerified: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (String, String, Uri?, String, String, String, List<String>, String, Boolean, Int) -> Unit,
) {
    var name by remember(displayName) { mutableStateOf(displayName) }
    var handle by remember(username) { mutableStateOf(username) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var profileBio by remember(bio) { mutableStateOf(bio) }
    var profileLocation by remember(location) { mutableStateOf(location) }
    var profileLink by remember(link) { mutableStateOf(link) }
    var interestsText by remember(interests) { mutableStateOf(interests.joinToString(", ")) }
    var selectedAvatarVariant by remember(userId, avatarVariant) { mutableStateOf(avatarVariant) }
    var showPublicPreview by remember(userId) { mutableStateOf(false) }
    val linkError = if (
        profileLink.isNotBlank() && normalizedProfileExternalUrl(profileLink) == null
    ) {
        "Enter a valid http or https profile link."
    } else {
        null
    }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) selectedPhoto = uri
    }

    BackHandler(enabled = isLoading) { }

    NovaKeyboardAwareFormPage(
        title = "Edit profile",
        subtitle = "Your identity in Plantupia. Only the details you add here are public.",
        onBack = onBack,
        action = {
            val visibleError = errorMessage ?: linkError
            if (visibleError != null) {
                Text(
                    text = visibleError,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            NovaPrimaryButton(
                text = "Save changes",
                busy = isLoading,
                busyText = "Saving…",
                onClick = {
                    onSave(
                        name.trim(),
                        handle.trim(),
                        selectedPhoto,
                        profileBio.trim(),
                        profileLocation.trim(),
                        profileLink.trim(),
                        interestsText.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct().take(8),
                        profileTheme,
                        showOrbit,
                        selectedAvatarVariant,
                    )
                },
                enabled = linkError == null &&
                    name.trim().length >= 2 &&
                    handle.trim().length >= 3,
            )
        },
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Text("YOUR PHOTO", color = PlantupiaColors.forest, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PlantupiaAvatar(
                userId = userId,
                displayName = name,
                photoUrl = selectedPhoto?.toString() ?: avatarUrl,
                size = 88.dp,
                chosenVariant = selectedAvatarVariant,
            )
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text("How people recognize you", color = PlantupiaColors.forestText, fontWeight = FontWeight.SemiBold)
                Text(
                    "Use a photograph or keep an illustrated identity.",
                    color = NovaMuted, fontSize = 12.sp, lineHeight = 18.sp,
                )
                TextButton(
                    onClick = { photoPicker.launch("image/*") },
                    enabled = !isLoading,
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text(if (selectedPhoto == null) "Choose photo" else "Change selected photo", color = PlantupiaColors.forest)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text(
            "CHOOSE AN ILLUSTRATION",
            color = PlantupiaColors.forest,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
        )
        Text(
            "Use any of these identities when you do not upload a photo. No illustration assumes your gender or personality.",
            color = NovaMuted,
            fontSize = 12.sp,
        )
        // Only compose the visible portraits. The previous eager Row
        // inflated all nine VectorDrawable previews on every edit-screen
        // entry, before someone even chose to scroll the portrait picker.
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(items = (-1..7).toList(), key = { it }) { variant ->
                val chosen = selectedAvatarVariant == variant
                Surface(
                    onClick = { selectedAvatarVariant = variant },
                    enabled = !isLoading,
                    modifier = Modifier.semantics {
                        selected = chosen
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = if (chosen) PlantupiaColors.softSage else PlantupiaColors.paper,
                    border = BorderStroke(
                        if (chosen) 2.dp else 1.dp,
                        if (chosen) PlantupiaColors.forest else PlantupiaColors.border,
                    ),
                ) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        PlantupiaAvatar(
                            userId = userId,
                            displayName = name,
                            photoUrl = "",
                            size = 58.dp,
                            chosenVariant = variant,
                        )
                        Text(
                            if (variant == -1) "Default" else "Style ${variant + 1}",
                            color = PlantupiaColors.forestText,
                            fontSize = 11.sp,
                        )
                    }
                }
            }
        }
        if (selectedPhoto != null || avatarUrl.isNotBlank()) {
            Text(
                "Your uploaded photo stays visible. The illustration is the fallback.",
                color = NovaMuted,
                fontSize = 12.sp,
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Text(
            "ABOUT YOU",
            color = PlantupiaColors.forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        NovaTextField(
            value = name,
            onValueChange = { name = it.take(80) },
            label = "Name",
            placeholder = "Your name",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))
        NovaTextField(
            value = handle,
            onValueChange = { raw ->
                handle = raw
                    .lowercase()
                    .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                    .take(30)
            },
            label = "Username",
            placeholder = "yourname",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Your username has to be unique within Plantupia.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )

        Spacer(modifier = Modifier.height(14.dp))
        NovaTextField(
            value = profileBio,
            onValueChange = { profileBio = it.take(160) },
            label = "Bio",
            placeholder = "A little about you and what interests you.",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "OPTIONAL PUBLIC DETAILS",
            color = PlantupiaColors.forest,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Location, links and interests can be seen by other people. Leave them empty if you prefer.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(modifier = Modifier.height(10.dp))
        NovaTextField(
            value = profileLocation,
            onValueChange = { profileLocation = it.take(80) },
            label = "Location",
            placeholder = "Seoul, KR",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(14.dp))
        NovaTextField(
            value = profileLink,
            onValueChange = { profileLink = it.take(300) },
            label = "Link",
            placeholder = "https://your-site.example",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(14.dp))
        NovaTextField(
            value = interestsText,
            onValueChange = { interestsText = it.take(200) },
            label = "Interests",
            placeholder = "Gardens, architecture, photography",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        )
        Text(
            text = "Separate up to 8 interests with commas.",
            color = NovaMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        // Preserve the legacy Nova theme/orbit fields on save, without presenting
        // them as Plantupia product settings until their UX and privacy are reviewed.
        // Reuse the same public identity view visitors see, but keep it
        // optional so the edit form isn't buried underneath a tall preview.
        Spacer(modifier = Modifier.height(18.dp))
        TextButton(
            onClick = { showPublicPreview = !showPublicPreview },
            modifier = Modifier.align(Alignment.Start),
        ) {
            Text(
                if (showPublicPreview) "Hide public preview" else "Preview public profile",
                color = PlantupiaColors.forest,
                fontWeight = FontWeight.SemiBold,
            )
        }
        if (showPublicPreview) {
            NovaProfileIdentity(
                displayName = name,
                username = handle,
                avatarUrl = selectedPhoto?.toString() ?: avatarUrl,
                bio = profileBio,
                location = profileLocation,
                link = profileLink,
                interests = interestsText.split(',').map(String::trim).filter(String::isNotBlank).take(8),
                profileTheme = profileTheme,
                showOrbit = showOrbit,
                isVerified = isVerified,
                plantupiaIdentityId = userId,
                plantupiaAvatarVariant = selectedAvatarVariant,
            )
        }

    }
}
