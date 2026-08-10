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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.social.NovaBlockedAccountsRepository
import com.nova.app.ui.components.NovaAvatar
import com.nova.app.ui.components.NovaHeader
import com.nova.app.ui.components.NovaSecondaryButton
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaAccentSoft
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaBorder
import com.nova.app.ui.theme.NovaInk
import com.nova.app.ui.theme.NovaMuted
import com.nova.app.ui.theme.NovaSurface
import kotlinx.coroutines.launch


@Composable
fun BlockedAccountsScreen(
    onBack: () -> Unit,
    onSessionExpired: () -> Unit,
) {
    val context = LocalContext.current
    val repository = remember(context) {
        NovaBlockedAccountsRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    var blocked by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var unblockingUsername by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun load() {
        scope.launch {
            isLoading = true
            errorMessage = null
            when (val result = repository.blockedAccounts()) {
                is ApiResult.Success -> blocked = result.value
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) {
                        onSessionExpired()
                        return@launch
                    }
                    errorMessage = result.message
                }
            }
            isLoading = false
        }
    }

    fun unblock(person: NovaPerson) {
        if (unblockingUsername != null) return
        scope.launch {
            unblockingUsername = person.username
            errorMessage = null
            when (val result = repository.unblock(person.username)) {
                is ApiResult.Success -> blocked = blocked.filterNot { it.id == person.id }
                is ApiResult.Failure -> {
                    if (result.statusCode == 401) {
                        onSessionExpired()
                        return@launch
                    }
                    errorMessage = result.message
                }
            }
            unblockingUsername = null
        }
    }

    LaunchedEffect(Unit) { load() }

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

        if (!errorMessage.isNullOrBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = NovaSurface,
                border = BorderStroke(1.dp, NovaBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = NovaMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    NovaSecondaryButton(text = "Try again", onClick = ::load)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        when {
            isLoading && blocked.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = NovaAccent)
                }
            }

            blocked.isEmpty() -> {
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
                                Text(
                                    text = "✓",
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                    color = NovaAccent,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                )
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
                    items(blocked, key = { it.id }) { person ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = NovaSurface,
                            border = BorderStroke(1.dp, NovaBorder),
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
                                        if (unblockingUsername == null) unblock(person)
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = NovaAccentSoft,
                                ) {
                                    Text(
                                        text = if (unblockingUsername == person.username) "…" else "Unblock",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        color = NovaAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }
            }
        }
    }
}
