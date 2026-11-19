package com.nova.app.feature.auth

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.AccountSecurityActivity
import com.nova.app.feature.plants.PlantupiaBrandGlyph
import com.nova.app.feature.plants.PlantupiaColors
import com.nova.app.feature.plants.PlantupiaShapes
import com.nova.app.feature.plants.PlantupiaText
import com.nova.app.ui.components.NovaPrimaryButton
import com.nova.app.ui.components.NovaTextField

@Composable
fun CreateAccountScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onCreate: (String, String, String, String) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthPage(
        eyebrow = "NEW SPACE / ACCOUNT",
        title = "Start your living space",
        subtitle = "Your plants and care records stay private. Your name and username matter only when you choose to be visible to people.",
        email = email,
        onEmailChange = { email = it.trim() },
        password = password,
        onPasswordChange = { password = it },
        buttonText = if (isLoading) "Creating account…" else "Create account",
        buttonEnabled = name.trim().length >= 2 && username.trim().length >= 3 &&
            email.contains('@') && password.length >= 8,
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onSubmit = { onCreate(email, password, username, name) },
        helperText = "Use at least 8 characters for your password.",
        identityFields = {
            Text("HOW PEOPLE CAN RECOGNIZE YOU", style = PlantupiaText.sectionLabel)
            Spacer(modifier = Modifier.height(10.dp))
            NovaTextField(
                value = name,
                onValueChange = { name = it.take(80) },
                label = "Name",
                placeholder = "Maya",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(modifier = Modifier.height(14.dp))
            NovaTextField(
                value = username,
                onValueChange = { raw ->
                    username = raw.lowercase()
                        .filter { it.isLetterOrDigit() || it == '_' || it == '.' }
                        .take(30)
                },
                label = "Username",
                placeholder = "maya.green",
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text("PRIVATE ACCOUNT", style = PlantupiaText.sectionLabel)
            Spacer(modifier = Modifier.height(10.dp))
        },
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
        eyebrow = "RETURN / YOUR PLANTUPIA",
        title = "Come back to your space",
        subtitle = "Your plants, care history, orders and conversations continue from the account you left.",
        email = email,
        onEmailChange = { email = it.trim() },
        password = password,
        onPasswordChange = { password = it },
        buttonText = if (isLoading) "Opening your space…" else "Log in",
        buttonEnabled = email.contains('@') && password.isNotBlank(),
        isLoading = isLoading,
        errorMessage = errorMessage,
        onBack = onBack,
        onSubmit = { onLogin(email, password) },
        helperText = "Signing in does not make any private plant or care record public.",
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
    eyebrow: String,
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
    identityFields: (@Composable () -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PlantupiaColors.cream)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack, enabled = !isLoading) {
                    Text("← Back", color = PlantupiaColors.forest, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                PlantupiaBrandGlyph(modifier = Modifier.size(28.dp))
                Text(
                    "PLANTUPIA",
                    modifier = Modifier.padding(start = 8.dp),
                    color = PlantupiaColors.forestText,
                    fontFamily = FontFamily.Serif,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(modifier = Modifier.height(34.dp))
            Text(eyebrow, style = PlantupiaText.sectionLabel)
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                title,
                style = PlantupiaText.pageHeading.copy(fontSize = 35.sp, lineHeight = 41.sp),
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                subtitle,
                style = PlantupiaText.body.copy(fontSize = 14.5.sp, lineHeight = 22.sp),
                modifier = Modifier.fillMaxWidth(0.94f),
            )

            Spacer(modifier = Modifier.height(24.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.paper,
                shape = PlantupiaShapes.card,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                ) {
                    identityFields?.invoke()

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

                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            color = PlantupiaColors.cream,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    if (!secondaryActionText.isNullOrBlank() && onSecondaryAction != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = onSecondaryAction, enabled = !isLoading) {
                            Text(
                                text = secondaryActionText,
                                color = PlantupiaColors.forest,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = helperText, style = PlantupiaText.supporting)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PlantupiaColors.cream)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 18.dp),
        ) {
            NovaPrimaryButton(
                text = buttonText,
                onClick = onSubmit,
                enabled = buttonEnabled && !isLoading,
            )
        }
    }
}
