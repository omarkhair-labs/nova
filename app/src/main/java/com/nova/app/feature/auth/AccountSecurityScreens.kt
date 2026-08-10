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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.auth.NovaAccountSecurityRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.components.NovaTextField
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import kotlinx.coroutines.launch


private enum class RecoveryStage { Email, Code, Done }


@Composable
fun PasswordRecoveryScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaAccountSecurityRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var stage by remember { mutableStateOf(RecoveryStage.Email) }
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    SecurityPage(
        title = when (stage) {
            RecoveryStage.Email -> "Reset your password"
            RecoveryStage.Code -> "Check your email"
            RecoveryStage.Done -> "Password changed"
        },
        subtitle = when (stage) {
            RecoveryStage.Email -> "Enter the email on your Nova account. We'll send a 6-digit code."
            RecoveryStage.Code -> "Enter the code sent to $email and choose a new password."
            RecoveryStage.Done -> "Your old password and previous Nova sessions can no longer be used."
        },
        onBack = onBack,
    ) {
        when (stage) {
            RecoveryStage.Email -> {
                NovaTextField(
                    value = email,
                    onValueChange = { email = it.trim() },
                    label = "Email",
                    placeholder = "you@example.com",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                )
                Spacer(Modifier.height(14.dp))
                StatusText(error = error, info = info)
                Spacer(Modifier.weight(1f))
                NovaPrimaryButton(
                    text = if (loading) "Sending…" else "Send reset code",
                    enabled = !loading && email.contains('@'),
                    onClick = {
                        if (loading) return@NovaPrimaryButton
                        scope.launch {
                            loading = true
                            error = null
                            info = null
                            when (val result = repository.requestPasswordReset(email)) {
                                is ApiResult.Success -> {
                                    info = result.value
                                    stage = RecoveryStage.Code
                                }
                                is ApiResult.Failure -> error = result.message
                            }
                            loading = false
                        }
                    },
                )
            }

            RecoveryStage.Code -> {
                NovaTextField(
                    value = code,
                    onValueChange = { value -> code = value.filter(Char::isDigit).take(6) },
                    label = "6-digit code",
                    placeholder = "000000",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next,
                    ),
                )
                Spacer(Modifier.height(12.dp))
                NovaTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
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
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = "Confirm new password",
                    placeholder = "Repeat your password",
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                )
                Spacer(Modifier.height(12.dp))
                StatusText(error = error, info = info)
                Spacer(Modifier.weight(1f))
                NovaSecondaryButton(
                    text = "Send a new code",
                    onClick = {
                        if (loading) return@NovaSecondaryButton
                        scope.launch {
                            loading = true
                            error = null
                            when (val result = repository.requestPasswordReset(email)) {
                                is ApiResult.Success -> info = result.value
                                is ApiResult.Failure -> error = result.message
                            }
                            loading = false
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                NovaPrimaryButton(
                    text = if (loading) "Changing…" else "Change password",
                    enabled = !loading && code.length == 6 &&
                        newPassword.length >= 8 && newPassword == confirmPassword,
                    onClick = {
                        if (loading) return@NovaPrimaryButton
                        scope.launch {
                            loading = true
                            error = null
                            info = null
                            when (
                                val result = repository.resetPassword(
                                    email = email,
                                    code = code,
                                    newPassword = newPassword,
                                )
                            ) {
                                is ApiResult.Success -> {
                                    info = result.value
                                    stage = RecoveryStage.Done
                                }
                                is ApiResult.Failure -> error = result.message
                            }
                            loading = false
                        }
                    },
                )
            }

            RecoveryStage.Done -> {
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
                            info ?: "Log in again with your new password.",
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
    val repository = remember(context) {
        NovaAccountSecurityRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var loadingAction by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = {
                if (loadingAction == null) showDeleteConfirm = false
            },
            title = { Text("Delete your Nova account?") },
            text = {
                Text(
                    "Your profile, posts and social connections will be removed. Shared direct-message history stays with the other participant under Deleted user. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (loadingAction != null) return@TextButton
                        scope.launch {
                            loadingAction = "delete"
                            error = null
                            info = null
                            when (val result = repository.deleteAccount(currentPassword)) {
                                is ApiResult.Success -> onAccountDeleted()
                                is ApiResult.Failure -> {
                                    error = result.message
                                    showDeleteConfirm = false
                                    loadingAction = null
                                }
                            }
                        }
                    },
                    enabled = loadingAction == null,
                ) {
                    Text(
                        if (loadingAction == "delete") "Deleting…" else "Delete account",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false },
                    enabled = loadingAction == null,
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
            value = currentPassword,
            onValueChange = { currentPassword = it },
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
            value = newPassword,
            onValueChange = { newPassword = it },
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
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm new password",
            placeholder = "Repeat new password",
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            visualTransformation = PasswordVisualTransformation(),
        )
        Spacer(Modifier.height(12.dp))
        StatusText(error = error, info = info)
        Spacer(Modifier.height(18.dp))
        NovaPrimaryButton(
            text = if (loadingAction == "change") "Changing…" else "Change password",
            enabled = loadingAction == null && currentPassword.isNotBlank() &&
                newPassword.length >= 8 && newPassword == confirmPassword,
            onClick = {
                if (loadingAction != null) return@NovaPrimaryButton
                scope.launch {
                    loadingAction = "change"
                    error = null
                    info = null
                    when (
                        val result = repository.changePassword(
                            currentPassword = currentPassword,
                            newPassword = newPassword,
                        )
                    ) {
                        is ApiResult.Success -> {
                            info = "Password changed. Other sessions were signed out."
                            currentPassword = newPassword
                            newPassword = ""
                            confirmPassword = ""
                        }
                        is ApiResult.Failure -> error = result.message
                    }
                    loadingAction = null
                }
            },
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
            text = if (loadingAction == "revoke") "Signing out devices…" else "Log out other devices",
            onClick = {
                if (loadingAction != null || currentPassword.isBlank()) return@NovaSecondaryButton
                scope.launch {
                    loadingAction = "revoke"
                    error = null
                    info = null
                    when (val result = repository.revokeOtherSessions(currentPassword)) {
                        is ApiResult.Success -> info = "Other Nova sessions were signed out."
                        is ApiResult.Failure -> error = result.message
                    }
                    loadingAction = null
                }
            },
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
            onClick = {
                if (loadingAction != null) return@NovaSecondaryButton
                if (currentPassword.isBlank()) {
                    error = "Enter your current password first."
                } else {
                    error = null
                    showDeleteConfirm = true
                }
            },
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
