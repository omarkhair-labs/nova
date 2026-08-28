package com.nova.app.feature.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.app.appContainer
import com.nova.app.feature.security.BlockedAccountsStateOwner
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.icons.NovaIcon
import com.nova.app.ui.icons.NovaIconAsset
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface


@Composable
fun BlockedAccountsScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.applicationContext.appContainer
    val scope = rememberCoroutineScope()
    val currentOnSessionExpired by rememberUpdatedState(onSessionExpired)
    val owner = remember(container, scope) {
        BlockedAccountsStateOwner(
            repository = container.blockedAccountsRepository,
            scope = scope,
            onSessionExpired = { currentOnSessionExpired() },
        )
    }
    val state = owner.state

    LaunchedEffect(Unit) { owner.load() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        NovaHeader(
            title = "Blocked accounts",
            subtitle = "People you've blocked can't find or contact you on Nova.",
            onBack = onBack,
        )
        Spacer(modifier = Modifier.height(22.dp))

        if (!state.errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = state.errorMessage.orEmpty(),
                        color = NovaMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NovaSecondaryButton(text = "Try again", onClick = owner::load)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when {
            state.isLoading && state.blocked.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            }

            state.blocked.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = NovaSurface,
                        border = BorderStroke(1.dp, NovaBorder),
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = NovaAccentSoft,
                            ) {
                                Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                                    NovaIcon(
                                        asset = NovaIconAsset.Check,
                                        contentDescription = null,
                                        tint = NovaAccent,
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No blocked accounts",
                                color = NovaInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Anyone you block will appear here so you can unblock them later.",
                                color = NovaMuted,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            )
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.blocked, key = { it.id }) { person ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = NovaBackground,
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                NovaAvatar(
                                    source = person.avatarUrl,
                                    fallbackText = person.name.ifBlank { person.username },
                                    size = 48.dp,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = person.name.ifBlank { person.username },
                                        color = NovaInk,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    Text(
                                        text = "@${person.username}",
                                        color = NovaMuted,
                                        fontSize = 11.sp,
                                    )
                                }
                                Surface(
                                    onClick = {
                                        if (state.unblockingUsername == null) owner.unblock(person)
                                    },
                                    modifier = Modifier.heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    color = NovaAccentSoft,
                                ) {
                                    if (state.unblockingUsername == person.username) {
                                        Box(
                                            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 20.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = NovaAccent,
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Unblock",
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            color = NovaAccent,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = NovaBorder)
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}
