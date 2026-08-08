package com.nova.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nova.app.core.auth.NovaAuthRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaUser
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.profile.EditProfileScreen
import com.nova.app.feature.profile.ProfileScreen
import com.nova.app.feature.welcome.WelcomeScreen
import com.nova.app.navigation.NovaRoute
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted
import kotlinx.coroutines.launch

@Composable
fun NovaApp() {
    val context = LocalContext.current
    val authRepository = remember(context) {
        NovaAuthRepository(context.applicationContext)
    }
    val scope = rememberCoroutineScope()

    val backStack = remember {
        mutableStateListOf<NovaRoute>(NovaRoute.Welcome)
    }

    var currentUser by remember { mutableStateOf<NovaUser?>(null) }
    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var isBootstrapping by remember { mutableStateOf(true) }

    fun openHome() {
        backStack.clear()
        backStack.add(NovaRoute.Home)
    }

    fun resetToWelcome() {
        backStack.clear()
        backStack.add(NovaRoute.Welcome)
    }

    LaunchedEffect(Unit) {
        when (val restored = authRepository.restoreSession()) {
            is ApiResult.Success -> {
                currentUser = restored.value
                if (restored.value != null) {
                    openHome()
                } else {
                    resetToWelcome()
                }
            }

            is ApiResult.Failure -> {
                resetToWelcome()
            }
        }
        isBootstrapping = false
    }

    if (isBootstrapping) {
        NovaStartupScreen()
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (!authLoading && backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = { route ->
            when (route) {
                NovaRoute.Welcome -> NavEntry(route) {
                    WelcomeScreen(
                        onCreateAccount = {
                            authError = null
                            backStack.add(NovaRoute.CreateAccount)
                        },
                        onLogin = {
                            authError = null
                            backStack.add(NovaRoute.Login)
                        },
                    )
                }

                NovaRoute.CreateAccount -> NavEntry(route) {
                    CreateAccountScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onContinue = { email, password ->
                            pendingEmail = email.trim().lowercase()
                            pendingPassword = password
                            authError = null
                            backStack.add(NovaRoute.ProfileSetup)
                        },
                    )
                }

                NovaRoute.Login -> NavEntry(route) {
                    LoginScreen(
                        isLoading = authLoading,
                        errorMessage = authError,
                        onBack = {
                            if (!authLoading) {
                                authError = null
                                backStack.removeLastOrNull()
                            }
                        },
                        onLogin = { email, password ->
                            if (!authLoading) {
                                scope.launch {
                                    authLoading = true
                                    authError = null

                                    when (val result = authRepository.login(email, password)) {
                                        is ApiResult.Success -> {
                                            currentUser = result.value
                                            authLoading = false
                                            openHome()
                                        }

                                        is ApiResult.Failure -> {
                                            authError = result.message
                                            authLoading = false
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                NovaRoute.ProfileSetup -> NavEntry(route) {
                    ProfileSetupScreen(
                        email = pendingEmail,
                        isLoading = authLoading,
                        errorMessage = authError,
                        onBack = {
                            if (!authLoading) {
                                authError = null
                                backStack.removeLastOrNull()
                            }
                        },
                        onFinish = { name, handle ->
                            if (!authLoading) {
                                scope.launch {
                                    authLoading = true
                                    authError = null

                                    when (
                                        val result = authRepository.register(
                                            email = pendingEmail,
                                            password = pendingPassword,
                                            username = handle,
                                            name = name,
                                        )
                                    ) {
                                        is ApiResult.Success -> {
                                            currentUser = result.value
                                            pendingPassword = ""
                                            authLoading = false
                                            openHome()
                                        }

                                        is ApiResult.Failure -> {
                                            authError = result.message
                                            authLoading = false
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                NovaRoute.Home -> NavEntry(route) {
                    val user = currentUser
                    HomeScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.Profile -> NavEntry(route) {
                    val user = currentUser
                    ProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        email = user?.email.orEmpty(),
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        onHomeClick = { backStack.removeLastOrNull() },
                        onEditProfile = {
                            authError = null
                            backStack.add(NovaRoute.EditProfile)
                        },
                        onLogout = {
                            authRepository.logout()
                            currentUser = null
                            pendingEmail = ""
                            pendingPassword = ""
                            authError = null
                            resetToWelcome()
                        },
                    )
                }

                NovaRoute.EditProfile -> NavEntry(route) {
                    val user = currentUser
                    EditProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        isLoading = authLoading,
                        errorMessage = authError,
                        onBack = {
                            if (!authLoading) {
                                authError = null
                                backStack.removeLastOrNull()
                            }
                        },
                        onSave = { name, handle, avatarUri ->
                            if (!authLoading) {
                                scope.launch {
                                    authLoading = true
                                    authError = null

                                    when (
                                        val result = authRepository.updateProfile(
                                            name = name,
                                            username = handle,
                                            avatarUri = avatarUri,
                                        )
                                    ) {
                                        is ApiResult.Success -> {
                                            currentUser = result.value
                                            authLoading = false
                                            backStack.removeLastOrNull()
                                        }

                                        is ApiResult.Failure -> {
                                            authLoading = false
                                            if (result.statusCode == 401) {
                                                authRepository.logout()
                                                currentUser = null
                                                resetToWelcome()
                                            } else {
                                                authError = result.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    )
                }
            }
        },
    )
}

@Composable
private fun NovaStartupScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "N",
                color = NovaAccent,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(modifier = Modifier.height(18.dp))
            CircularProgressIndicator(color = NovaAccent)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Opening your space…",
                color = NovaMuted,
                fontSize = 13.sp,
            )
        }
    }
}
