package com.nova.app.feature.auth

import android.content.Intent
import android.util.Patterns
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.AccountSecurityActivity
import com.nova.app.feature.plants.PlantupiaBrandGlyph
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.ui.components.NovaBackButton
import com.nova.app.ui.components.NovaPasswordField
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField

@Composable
fun CreateAccountScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onCreate: (String, String, String, String) -> Unit,
    initialEmail: String = "",
    onEmailChanged: (String) -> Unit = {},
    onLogin: ((String) -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    // Passwords deliberately never enter saved-instance state. Recreation starts at account details.
    var password by remember { mutableStateOf("") }
    var identityStep by remember { mutableStateOf(false) }
    var attempted by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val validEmail = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val validAccount = validEmail && password.length >= 8
    val validIdentity = name.trim().length >= 2 && username.length >= 3

    fun continueAccount() {
        if (isLoading) return
        attempted = true
        if (validAccount) {
            focus.clearFocus()
            attempted = false
            identityStep = true
        }
    }
    fun submit() {
        if (isLoading) return
        attempted = true
        if (validIdentity && validAccount) {
            focus.clearFocus()
            onCreate(email.trim(), password, username, name.trim())
        }
    }
    fun goBack() {
        if (!isLoading) {
            focus.clearFocus()
            if (identityStep) { identityStep = false; attempted = false } else onBack()
        }
    }
    BackHandler(enabled = identityStep || isLoading) { goBack() }

    AuthPage(
        eyebrow = if (identityStep) "2 of 2" else "1 of 2",
        title = if (identityStep) "Your profile" else "Create account",
        subtitle = "",
        buttonText = if (identityStep) "Create account" else "Continue",
        busyText = "Creating account…",
        buttonEnabled = !isLoading,
        isLoading = isLoading,
        errorMessage = if (identityStep) errorMessage else null,
        onBack = ::goBack,
        onSubmit = if (identityStep) ::submit else ::continueAccount,
        footer = {
            if (onLogin != null) TextButton(
                onClick = { onLogin(email.trim()) }, enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("Already have an account? Log in", color = PlantupiaColors.forest) }
        },
    ) {
        if (!identityStep) {
            NovaTextField(
                value = email,
                onValueChange = { email = it; onEmailChanged(it) },
                label = "Email",
                placeholder = "you@example.com",
                modifier = Modifier.semantics { contentType = ContentType.EmailAddress + ContentType.NewUsername },
                enabled = !isLoading,
                isError = attempted && !validEmail,
                supportingText = if (attempted && !validEmail) "Enter a complete email address." else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(16.dp))
            NovaPasswordField(
                value = password,
                onValueChange = { password = it },
                placeholder = "At least 8 characters",
                enabled = !isLoading,
                isError = attempted && password.length < 8,
                supportingText = "Use at least 8 characters.",
                autofillType = ContentType.NewPassword,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { continueAccount() }),
            )
            Spacer(Modifier.height(18.dp))
            Text("Your spaces and care records stay private. You decide what to share.", style = PlantupiaText.supporting)
        } else {
            Column(Modifier.fillMaxWidth().background(PlantupiaColors.softSage, PlantupiaShapes.inset).padding(14.dp)) {
                Text("ACCOUNT EMAIL", style = PlantupiaText.sectionLabel)
                Text(email.trim(), style = PlantupiaText.body, modifier = Modifier.padding(top = 4.dp))
                TextButton(onClick = { identityStep = false; attempted = false }, enabled = !isLoading) {
                    Text("Edit account details", color = PlantupiaColors.forest)
                }
            }
            Spacer(Modifier.height(18.dp))
            NovaTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = "Name",
                placeholder = "Your name",
                modifier = Modifier.semantics { contentType = ContentType.PersonFullName },
                enabled = !isLoading,
                isError = attempted && name.trim().length < 2,
                supportingText = if (attempted && name.trim().length < 2) "Use at least 2 characters." else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
            )
            Spacer(Modifier.height(16.dp))
            NovaTextField(
                value = username,
                onValueChange = { raw ->
                    username = raw.lowercase().filter { it.isLetterOrDigit() || it == '_' || it == '.' }.take(30)
                },
                label = "Username",
                placeholder = "your.name",
                enabled = !isLoading,
                isError = attempted && username.length < 3,
                supportingText = if (attempted && username.length < 3) "Use at least 3 characters." else
                    "Letters, numbers, periods and underscores. This appears on your public profile.",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
            )
        }
    }
}

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onLogin: (String, String) -> Unit,
    initialEmail: String = "",
    onEmailChanged: (String) -> Unit = {},
    onCreateAccount: ((String) -> Unit)? = null,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    var email by rememberSaveable { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var attempted by remember { mutableStateOf(false) }
    val validEmail = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    fun submit() {
        if (isLoading) return
        attempted = true
        if (validEmail && password.isNotBlank()) {
            focus.clearFocus()
            onLogin(email.trim(), password)
        }
    }
    val recovery = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.getStringExtra(AccountSecurityActivity.EXTRA_RECOVERY_EMAIL)?.let {
                email = it
                onEmailChanged(it)
                password = ""
                attempted = false
            }
        }
    }
    AuthPage(
        eyebrow = "",
        title = "Welcome back",
        subtitle = "",
        buttonText = "Log in",
        busyText = "Opening your space…",
        buttonEnabled = !isLoading,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onSubmit = ::submit,
        secondaryActionText = "Forgot password?",
        onSecondaryAction = {
            focus.clearFocus()
            recovery.launch(
                Intent(context, AccountSecurityActivity::class.java)
                    .putExtra(AccountSecurityActivity.EXTRA_MODE, AccountSecurityActivity.MODE_RECOVERY)
                    .putExtra(AccountSecurityActivity.EXTRA_RECOVERY_EMAIL, email.trim())
            )
        },
        footer = {
            if (onCreateAccount != null) TextButton(
                onClick = { onCreateAccount(email.trim()) }, enabled = !isLoading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text("New to Plantupia? Create account", color = PlantupiaColors.forest) }
        },
    ) {
        NovaTextField(
            value = email,
            onValueChange = { email = it; onEmailChanged(it) },
            label = "Email",
            placeholder = "you@example.com",
            modifier = Modifier.semantics { contentType = ContentType.EmailAddress + ContentType.Username },
            enabled = !isLoading,
            isError = attempted && !validEmail,
            supportingText = if (attempted && !validEmail) "Enter a complete email address." else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(16.dp))
        NovaPasswordField(
            value = password,
            onValueChange = { password = it },
            enabled = !isLoading,
            isError = attempted && password.isBlank(),
            supportingText = if (attempted && password.isBlank()) "Enter your password to continue." else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submit() }),
        )
    }
}

@Composable
private fun AuthPage(
    eyebrow: String,
    title: String,
    subtitle: String,
    buttonText: String,
    busyText: String,
    buttonEnabled: Boolean,
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onSubmit: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    footer: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val errorAnchor = remember { BringIntoViewRequester() }
    val scrollState = key(title) { rememberScrollState() }
    LaunchedEffect(errorMessage) {
        if (!errorMessage.isNullOrBlank()) {
            withFrameNanos { }
            errorAnchor.bringIntoView()
        }
    }
    Box(
        Modifier.fillMaxSize().background(PlantupiaColors.cream)
            .statusBarsPadding().navigationBarsPadding().imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(Modifier.widthIn(max = 480.dp).fillMaxSize()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(scrollState)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    NovaBackButton(onClick = onBack, enabled = !isLoading)
                    Spacer(Modifier.weight(1f))
                    PlantupiaBrandGlyph(Modifier.size(28.dp))
                }
                Spacer(Modifier.height(20.dp))
                if (eyebrow.isNotBlank()) {
                    Text(eyebrow, style = PlantupiaText.supporting)
                    Spacer(Modifier.height(8.dp))
                }
                Text(title, style = PlantupiaText.pageHeading, modifier = Modifier.semantics { heading() })
                if (subtitle.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(subtitle, style = PlantupiaText.body)
                }
                Spacer(Modifier.height(24.dp))
                Surface(color = PlantupiaColors.cream, shape = PlantupiaShapes.card) {
                    Column(Modifier.fillMaxWidth()) {
                        content()
                        if (secondaryActionText != null && onSecondaryAction != null) {
                            TextButton(onClick = onSecondaryAction, enabled = !isLoading,
                                modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(secondaryActionText, color = PlantupiaColors.forest)
                            }
                        }
                        if (!errorMessage.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = errorMessage,
                                style = PlantupiaText.body.copy(color = PlantupiaColors.errorText),
                                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(errorAnchor).semantics {
                                    liveRegion = LiveRegionMode.Polite
                                    error(errorMessage)
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                footer()
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                NovaPrimaryButton(text = buttonText, busy = isLoading, busyText = busyText,
                    onClick = onSubmit, enabled = buttonEnabled)
            }
        }
    }
}
