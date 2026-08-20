package com.nova.app.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.security.AccountSecurityStateOwner
import com.nova.app.feature.security.PasswordRecoveryStage
import com.nova.app.feature.security.PasswordRecoveryStateOwner
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted


@Composable
fun PasswordRecoveryScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.applicationContext.appContainer
    val scope = rememberCoroutineScope()
    val owner = remember(container, scope) {
        PasswordRecoveryStateOwner(
            repository = container.securityRepository,
            scope = scope,
        )
    }
    val state = owner.state

    SecurityPage(
        title = when (state.stage) {
            PasswordRecoveryStage.Email -> "Reset your password"
            PasswordRecoveryStage.Code -> "Check your email"
            PasswordRecoveryStage.Done -> "Password changed"
        },
        subtitle = when (state.stage) {
            PasswordRecoveryStage.Email -> "Enter the email on your Nova account. We'll send a 6-digit code."
            PasswordRecoveryStage.Code -> "Enter the code sent to ${state.email} and choose a new password."
            PasswordRecoveryStage.Done -> "Your old password and previous Nova sessions can no longer be used."
        },
        onBack = onBack,
    ) {
        when (state.stage) {
            PasswordRecoveryStage.Email -> {
                NovaTextField(
                    value = state.email,
                    onValueChange = owner::setEmail,
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                StatusText(error = state.error, info = state.info)
                Spacer(Modifier.weight(1f))
                NovaPrimaryButton(
                    text = if (state.loading) "Sending…" else "Send reset code",
                    enabled = !state.loading && state.email.contains('@'),
                    onClick = owner::requestResetCode,
                )
            }

            PasswordRecoveryStage.Code -> {
                NovaTextField(
                    value = state.code,
                    onValueChange = owner::setCode,
                    label = "6-digit code",
                    placeholder = "000000",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                NovaTextField(
                    value = state.newPassword,
                    onValueChange = owner::setNewPassword,
                    label = "New password",
                    placeholder = "At least 8 characters",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(12.dp))
                NovaTextField(
                    value = state.confirmPassword,
                    onValueChange = owner::setConfirmPassword,
                    label = "Confirm new password",
                    placeholder = "Repeat your password",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(12.dp))
                StatusText(error = state.error, info = state.info)
                Spacer(Modifier.weight(1f))
                NovaSecondaryButton(
                    text = "Send a new code",
                    onClick = owner::requestResetCode,
                )
                Spacer(Modifier.height(10.dp))
                NovaPrimaryButton(
                    text = if (state.loading) "Changing…" else "Change password",
                    enabled = !state.loading && state.code.length == 6 &&
                        state.newPassword.length >= 8 && state.newPassword == state.confirmPassword,
                    onClick = owner::resetPassword,
                )
            }

            PasswordRecoveryStage.Done -> {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = NovaAccentSoft,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Password reset complete",
                            color = NovaInk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            state.info ?: "Log in again with your new password.",
                            color = NovaMuted,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                NovaPrimaryButton(text = "Back to log in", onClick = onBack)
            }
        }
    }
}


@Composable
fun AccountSecurityScreen(
    onBack: () -> Unit,
    onAccountDeleted: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.applicationContext.appContainer
    val scope = rememberCoroutineScope()
    val currentOnAccountDeleted by rememberUpdatedState(onAccountDeleted)
    val owner = remember(container, scope) {
        AccountSecurityStateOwner(
            repository = container.securityRepository,
            scope = scope,
            onAccountDeleted = { currentOnAccountDeleted() },
        )
    }
    val state = owner.state

    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = owner::dismissDeleteConfirmation,
            title = { Text("Delete your Nova account?") },
            text = {
                Text(
                    "Your profile, posts and social connections will be removed. Shared direct-message history stays with the other participant under Deleted user. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = owner::confirmDelete,
                    enabled = state.loadingAction == null,
                ) {
                    Text(
                        if (state.loadingAction == "delete") "Deleting…" else "Delete account",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = owner::dismissDeleteConfirmation,
                    enabled = state.loadingAction == null,
                ) { Text("Cancel") }
            },
        )
    }

    SecurityScrollablePage(
        title = "Security",
        subtitle = "Change your password or remove every other signed-in Nova session.",
        onBack = onBack,
    ) {
        Text("Password", color = NovaInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        NovaTextField(
            value = state.currentPassword,
            onValueChange = owner::setCurrentPassword,
            label = "Current password",
            placeholder = "Your current password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
        NovaTextField(
            value = state.newPassword,
            onValueChange = owner::setNewPassword,
            label = "New password",
            placeholder = "At least 8 characters",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
        NovaTextField(
            value = state.confirmPassword,
            onValueChange = owner::setConfirmPassword,
            label = "Confirm new password",
            placeholder = "Repeat new password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
        StatusText(error = state.error, info = state.info)
        Spacer(Modifier.height(18.dp))
        NovaPrimaryButton(
            text = if (state.loadingAction == "change") "Changing…" else "Change password",
            enabled = state.loadingAction == null && state.currentPassword.isNotBlank() &&
                state.newPassword.length >= 8 && state.newPassword == state.confirmPassword,
            onClick = owner::changePassword,
        )

        Spacer(Modifier.height(30.dp))
        Text("Signed-in devices", color = NovaInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "If you don't recognize another device, sign every other session out. Your password stays the same.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(14.dp))
        NovaSecondaryButton(
            text = if (state.loadingAction == "revoke") "Signing out devices…" else "Log out other devices",
            onClick = owner::revokeOtherSessions,
        )

        Spacer(Modifier.height(24.dp))
        Text(
            "Danger zone",
            color = MaterialTheme.colorScheme.error,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Deleting your account removes your public Nova identity and can't be undone.",
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(12.dp))
        NovaSecondaryButton(
            text = "Delete account",
            onClick = owner::requestDeleteConfirmation,
        )
        Spacer(Modifier.height(14.dp))
    }
}


@Composable
private fun SecurityPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
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
        NovaHeader(title = title, subtitle = subtitle, onBack = onBack)
        Spacer(Modifier.height(30.dp))
        content()
    }
}


@Composable
private fun SecurityScrollablePage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 18.dp),
    ) {
        NovaHeader(title = title, subtitle = subtitle, onBack = onBack)
        Spacer(Modifier.height(30.dp))
        content()
    }
}


@Composable
private fun StatusText(
    error: String?,
    info: String?,
) {
    if (!error.isNullOrBlank()) {
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    } else if (!info.isNullOrBlank()) {
        Text(
            text = info,
            color = NovaMuted,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
