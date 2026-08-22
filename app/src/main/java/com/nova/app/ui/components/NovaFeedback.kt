package com.nova.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSpacing
import com.nova.app.ui.theme.NovaSurface
import com.nova.app.ui.theme.NovaType

/** Shared full-width feedback presentation for ordinary Nova screens. */
@Composable
fun NovaLoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NovaSpacing.xxxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = NovaAccent)
        Spacer(modifier = Modifier.height(NovaSpacing.md))
        Text(
            text = message,
            color = NovaMuted,
            style = NovaType.bodyCompact,
        )
    }
}

/** Compact progress row for secondary loading work inside a populated screen. */
@Composable
fun NovaInlineLoading(
    message: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = NovaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = NovaAccent,
            strokeWidth = 2.dp,
        )
        Text(
            text = message,
            color = NovaMuted,
            style = NovaType.meta,
        )
    }
}

/** Standard empty-state panel. Copy belongs to the feature; presentation belongs here. */
@Composable
fun NovaEmptyState(
    title: String,
    message: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = NovaSpacing.xxl,
                vertical = NovaSpacing.xxxl,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = NovaAccentSoft,
            ) {
                Spacer(modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            Text(
                text = title,
                color = NovaInk,
                style = NovaType.sectionTitle,
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            Text(
                text = message,
                color = NovaMuted,
                style = NovaType.bodyCompact,
            )
        }
    }
}

/** Standard recoverable error panel with one clear retry action. */
@Composable
fun NovaErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Try again",
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Column(modifier = Modifier.padding(NovaSpacing.xl)) {
            Text(
                text = title,
                color = NovaInk,
                style = NovaType.title,
            )
            Spacer(modifier = Modifier.height(NovaSpacing.sm))
            Text(
                text = message,
                color = NovaMuted,
                style = NovaType.bodyCompact,
            )
            Spacer(modifier = Modifier.height(NovaSpacing.lg))
            NovaSecondaryButton(
                text = retryLabel,
                onClick = onRetry,
            )
        }
    }
}

/** Compact recoverable error for a subsection or pagination failure. */
@Composable
fun NovaInlineRetry(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    retryLabel: String = "Try again",
) {
    Surface(
        onClick = onRetry,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = NovaSurface,
        border = BorderStroke(1.dp, NovaBorder),
    ) {
        Row(
            modifier = Modifier.padding(NovaSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NovaSpacing.sm),
        ) {
            Text(
                text = message,
                color = NovaMuted,
                style = NovaType.meta,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = retryLabel,
                color = NovaAccent,
                style = NovaType.label,
            )
        }
    }
}
