package com.nova.app.ui.components

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaBackground

/**
 * Shared form shell for screens that need to stay usable while the IME is open.
 *
 * The editable content owns a bounded scroll area, while the primary action stays
 * pinned above the keyboard. Compose text fields can therefore bring the focused
 * field into view instead of letting the IME cover the bottom of a fixed Column.
 */
@Composable
fun NovaKeyboardAwareFormPage(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    action: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Spacer(modifier = Modifier.height(18.dp))
            NovaHeader(
                title = title,
                subtitle = subtitle,
                onBack = onBack,
            )
            content()
            Spacer(modifier = Modifier.height(24.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(NovaBackground)
                .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 18.dp),
            content = action,
        )
    }
}
