package com.nova.app.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Passwords and reveal state stay in memory; callers must not persist them in saved state. */
@Composable
fun NovaPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Password",
    modifier: Modifier = Modifier,
    placeholder: String = "Enter your password",
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: String? = null,
    autofillType: ContentType = ContentType.Password,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var revealed by remember { mutableStateOf(false) }
    NovaTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        modifier = modifier.semantics { contentType = autofillType },
        placeholder = placeholder,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = if (revealed) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            TextButton(
                onClick = { revealed = !revealed },
                enabled = enabled,
                modifier = Modifier.heightIn(min = 48.dp).semantics {
                    contentDescription = if (revealed) "Hide $label" else "Show $label"
                },
            ) { Text(if (revealed) "Hide" else "Show") }
        },
    )
}
