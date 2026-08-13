package com.nova.app.feature.messages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.nova.app.ui.theme.NovaBackground


internal object ConversationChromeTags {
    const val Root = "conversation_chrome"
    const val Header = "conversation_header"
    const val Composer = "conversation_composer"
}


/**
 * Shared layout contract for the live conversation header, message viewport,
 * and composer. MainActivity/MessagesActivity own window resizing; the route
 * supplies the resized viewport to this scaffold.
 */
@Composable
internal fun ConversationChromeScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = modifier.testTag(ConversationChromeTags.Root),
        containerColor = NovaBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ConversationChromeTags.Header),
            ) {
                topBar()
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(ConversationChromeTags.Composer),
            ) {
                bottomBar()
            }
        },
        content = content,
    )
}
