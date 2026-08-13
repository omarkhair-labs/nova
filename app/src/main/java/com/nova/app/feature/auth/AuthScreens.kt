package com.nova.app.feature.auth

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.AccountSecurityActivity
import com.nova.app.ui.components.NovaKeyboardAwareFormPage
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaMuted

@Composable
fun CreateAccountScreen(
    onBack: () -> Unit,
    onContinue: (String, String) -> Unit,
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
        isLoading = false,
        errorMessage = null,
        onBack = onBack,
        onSubmit = { onContinue(email, password) },
        helperText = "Use at least 8 characters for your password.",
    )
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthPage(
        title = "Welcome back",
        subtitle = "Log in and pick up exactly where you left off.",
        email = email,
        onEmailChange = { email = it.trim() },
        password = password,
        onPasswordChange = { password = it },
        buttonText = if (isLoading) "Logging in…" else "Log in",
        buttonEnabled = email.contains('@') && password.isNotBlank(),
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onSubmit = { onLogin(email, password) },
        helperText = "Your session stays signed in on this device.",
        secondaryActionText = "Forgot password?",
        onSecondaryAction = {
            context.startActivity(
                Intent(context, AccountSecurityActivity::class.java)
                    .putExtra(
                        AccountSecurityActivity.EXTRA_MODE,
                        AccountSecurityActivity.MODE_RECOVERY,
                    )
            )
        },
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
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    helperText: String,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    NovaKeyboardAwareFormPage(
        title = title,
        subtitle = subtitle,
        onBack = onBack,
        action = {
            NovaPrimaryButton(
                text = buttonText,
                onClick = onSubmit,
                enabled = buttonEnabled && !isLoading,
            )
        },
    ) {
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

        if (!secondaryActionText.isNullOrBlank() && onSecondaryAction != null) {
            Text(
                text = secondaryActionText,
                color = NovaAccent,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable(enabled = !isLoading, onClick = onSecondaryAction)
                    .padding(vertical = 4.dp),
            )
            Spacer(modifier = Modifier.height(6.dp))
        }

        Text(
            text = helperText,
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
