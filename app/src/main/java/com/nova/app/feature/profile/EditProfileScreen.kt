package com.nova.app.feature.profile

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.nova.app.ui.components.NovaKeyboardAwareFormPage
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder

@Composable
fun EditProfileScreen(
    displayName: String,
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
    onSave: (String, String, Uri?, String, String, String, List<String>, String, Boolean) -> Unit,
) {
    var name by remember(displayName) { mutableStateOf(displayName) }
    var handle by remember(username) { mutableStateOf(username) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }
    var profileBio by remember(bio) { mutableStateOf(bio) }
    var profileLocation by remember(location) { mutableStateOf(location) }
    var profileLink by remember(link) { mutableStateOf(link) }
    var interestsText by remember(interests) { mutableStateOf(interests.joinToString(", ")) }
    var selectedTheme by remember(profileTheme) { mutableStateOf(profileTheme) }
    var orbitVisible by remember(showOrbit) { mutableStateOf(showOrbit) }
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
        title = "Edit Profile",
        subtitle = "Curate your identity and how your orbit appears.",
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
                text = if (isLoading) "Saving…" else "Save changes",
                onClick = {
                    onSave(
                        name.trim(),
                        handle.trim(),
                        selectedPhoto,
                        profileBio.trim(),
                        profileLocation.trim(),
                        profileLink.trim(),
                        interestsText.split(',').map { it.trim() }.filter { it.isNotBlank() }.distinct().take(8),
                        selectedTheme,
                        orbitVisible,
                    )
                },
                enabled = !isLoading &&
                    linkError == null &&
                    name.trim().length >= 2 &&
                    handle.trim().length >= 3,
            )
        },
    ) {
        Spacer(modifier = Modifier.height(26.dp))

        NovaProfileIdentity(
            displayName = name,
            username = handle,
            avatarUrl = selectedPhoto?.toString() ?: avatarUrl,
            bio = profileBio,
            location = profileLocation,
            link = profileLink,
            interests = interestsText.split(',').map(String::trim).filter(String::isNotBlank).take(8),
            profileTheme = selectedTheme,
            showOrbit = orbitVisible,
            isVerified = isVerified,
        )

        TextButton(
            onClick = { photoPicker.launch("image/*") },
            enabled = !isLoading,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Text(
                text = if (selectedPhoto == null) "Change photo" else "Choose another photo",
                color = NovaAccent,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

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
            value = profileBio,
            onValueChange = { profileBio = it.take(160) },
            label = "Bio",
            placeholder = "Collecting moments, not things.",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
        )

        Spacer(modifier = Modifier.height(14.dp))
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
            placeholder = "Photography, Travel, Design",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done),
        )
        Text(
            text = "Separate up to 8 interests with commas.",
            color = NovaMuted,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            androidx.compose.foundation.layout.Column {
                Text("Show my orbit", color = NovaInk, fontWeight = FontWeight.SemiBold)
                Text("Let people see your orbit treatment.", color = NovaMuted, fontSize = 12.sp)
            }
            Switch(
                checked = orbitVisible,
                onCheckedChange = { orbitVisible = it },
                colors = SwitchDefaults.colors(checkedTrackColor = NovaAccent),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text("Profile theme", color = NovaInk, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("violet", "cyan", "orange", "pink", "slate", "ink", "black").forEach { theme ->
                Surface(
                    onClick = { selectedTheme = theme },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selectedTheme == theme) NovaAccentSoft else androidx.compose.ui.graphics.Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (selectedTheme == theme) NovaAccent else NovaBorder,
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Surface(
                            modifier = Modifier.size(10.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = novaProfileThemePalette(theme).accent,
                        ) { }
                        Text(
                            text = theme.replaceFirstChar { it.uppercase() },
                            color = if (selectedTheme == theme) NovaAccent else NovaInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }

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
            text = "Your username has to stay unique across Nova.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}
