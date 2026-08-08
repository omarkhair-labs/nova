package com.nova.app.feature.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
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
            title = "Make it yours",
            subtitle = "This is how people will recognize you around Nova.",
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(28.dp))

        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = CircleShape,
                color = NovaAccentSoft,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = name.firstOrNull()?.uppercase() ?: "N",
                        color = NovaAccent,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

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

        Spacer(modifier = Modifier.weight(1f))

        NovaPrimaryButton(
            text = if (isLoading) "Creating account…" else "Enter Nova",
            onClick = { onFinish(name.trim(), username.trim()) },
            enabled = !isLoading && name.trim().length >= 2 && username.trim().length >= 3,
        )
    }
}
