package com.nova.app.feature.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.nova.app.core.auth.NovaPendingRegistrationPhoto
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaKeyboardAwareFormPage
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMuted

@Composable
fun ProfileSetupScreen(
    email: String,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onFinish: (String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var selectedPhoto by remember { mutableStateOf<Uri?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            selectedPhoto = uri
            NovaPendingRegistrationPhoto.set(uri)
        }
    }

    fun back() {
        NovaPendingRegistrationPhoto.clear()
        onBack()
    }

    NovaKeyboardAwareFormPage(
        title = "Make it yours",
        subtitle = "This is how people will recognize you around Nova.",
        onBack = ::back,
        action = {
            NovaPrimaryButton(
                text = if (isLoading) "Creating account…" else "Enter Nova",
                onClick = { onFinish(name.trim(), username.trim()) },
                enabled = !isLoading && name.trim().length >= 2 && username.trim().length >= 3,
            )
        },
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        NovaAvatar(
            source = selectedPhoto?.toString().orEmpty(),
            fallbackText = name.ifBlank { username.ifBlank { "N" } },
            size = 96.dp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Row(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { photoPicker.launch("image/*") },
                enabled = !isLoading,
            ) {
                Text(
                    text = if (selectedPhoto == null) "Add profile photo" else "Choose another",
                    color = NovaAccent,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (selectedPhoto != null) {
                TextButton(
                    onClick = {
                        selectedPhoto = null
                        NovaPendingRegistrationPhoto.clear()
                    },
                    enabled = !isLoading,
                ) {
                    Text(
                        text = "Remove",
                        color = NovaMuted,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }

        Text(
            text = "Optional · JPG, PNG or another image up to 5 MB.",
            color = NovaMuted,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(24.dp))

        NovaTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = "Name",
            placeholder = "Your name",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        NovaTextField(
            value = username,
            onValueChange = { raw ->
                username = raw
                    .lowercase()
                    .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                    .take(24)
            },
            label = "Username",
            placeholder = "yourname",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = if (email.isBlank()) "You can change these later." else "Account: $email",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
    }
}
