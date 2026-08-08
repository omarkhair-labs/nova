package com.nova.app.feature.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted


@Composable
fun EditProfileScreen(
    displayName: String,
    username: String,
    avatarUrl: String,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSave: (String, String, Uri?) -> Unit,
) {
    var name by remember(displayName) { mutableStateOf(displayName) }
    var handle by remember(username) { mutableStateOf(username) }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) selectedPhoto = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        NovaHeader(
            title = "Edit profile",
            subtitle = "Keep your Nova identity feeling like you.",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(26.dp))

        NovaAvatar(
            source = selectedPhoto?.toString() ?: avatarUrl,
            fallbackText = name.ifBlank { handle },
            size = 104.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
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

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
        } else {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Your username has to stay unique across Nova.",
                color = NovaMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        NovaPrimaryButton(
            text = if (isLoading) "Saving…" else "Save changes",
            onClick = { onSave(name.trim(), handle.trim(), selectedPhoto) },
            enabled = !isLoading && name.trim().length >= 2 && handle.trim().length >= 3,
        )
    }
}
