package com.nova.app.feature.auth

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted

@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onContinue: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthPage(
        title = "Create your account",
        subtitle = "Start with the basics. You can shape everything else later.",
        email = email,
        onEmailChange = { email = it.trim() },
        password = password,
        onPasswordChange = { password = it },
        buttonText = "Continue",
        buttonEnabled = email.contains('@') && password.length >= 8,
        onBack = onBack,
        onSubmit = { onContinue(email) },
        helperText = "Use at least 8 characters for your password.",
    )
}

@Composable
fun LoginScreen(
    onBack: () -> Unit,
    onLogin: (String) -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthPage(
        title = "Welcome back",
        subtitle = "Log in and pick up exactly where you left off.",
        email = email,
        onEmailChange = { email = it.trim() },
        password = password,
        onPasswordChange = { password = it },
        buttonText = "Log in",
        buttonEnabled = email.contains('@') && password.isNotBlank(),
        onBack = onBack,
        onSubmit = { onLogin(email) },
        helperText = "Forgot password will be connected when the backend lands.",
    )
}

@Composable
private fun AuthPage(
    title: String,
    subtitle: String,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    buttonText: String,
    buttonEnabled: Boolean,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    helperText: String,
) {
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
            title = title,
            subtitle = subtitle,
            onBack = onBack,
        )

        Spacer(modifier = Modifier.height(36.dp))

        NovaTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            placeholder = "you@example.com",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        NovaTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            placeholder = "••••••••",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = helperText,
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )

        Spacer(modifier = Modifier.weight(1f))

        NovaPrimaryButton(
            text = buttonText,
            onClick = onSubmit,
            enabled = buttonEnabled,
        )
    }
}
