package com.nova.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.profile.ProfileScreen
import com.nova.app.feature.welcome.WelcomeScreen
import com.nova.app.navigation.NovaRoute

@Composable
fun NovaApp() {
    val backStack = remember {
        mutableStateListOf<NovaRoute>(NovaRoute.Welcome)
    }

    var accountEmail by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("Nova user") }
    var username by remember { mutableStateOf("nova") }

    fun openHome() {
        backStack.clear()
        backStack.add(NovaRoute.Home)
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = { route ->
            when (route) {
                NovaRoute.Welcome -> NavEntry(route) {
                    WelcomeScreen(
                        onCreateAccount = { backStack.add(NovaRoute.CreateAccount) },
                        onLogin = { backStack.add(NovaRoute.Login) },
                    )
                }

                NovaRoute.CreateAccount -> NavEntry(route) {
                    CreateAccountScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onContinue = { email ->
                            accountEmail = email
                            backStack.add(NovaRoute.ProfileSetup)
                        },
                    )
                }

                NovaRoute.Login -> NavEntry(route) {
                    LoginScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onLogin = { email ->
                            accountEmail = email
                            displayName = email.substringBefore('@').ifBlank { "Nova user" }
                            username = displayName.lowercase().replace(" ", "")
                            openHome()
                        },
                    )
                }

                NovaRoute.ProfileSetup -> NavEntry(route) {
                    ProfileSetupScreen(
                        email = accountEmail,
                        onBack = { backStack.removeLastOrNull() },
                        onFinish = { name, handle ->
                            displayName = name
                            username = handle
                            openHome()
                        },
                    )
                }

                NovaRoute.Home -> NavEntry(route) {
                    HomeScreen(
                        displayName = displayName,
                        username = username,
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.Profile -> NavEntry(route) {
                    ProfileScreen(
                        displayName = displayName,
                        username = username,
                        email = accountEmail,
                        onHomeClick = { backStack.removeLastOrNull() },
                        onLogout = {
                            accountEmail = ""
                            displayName = "Nova user"
                            username = "nova"
                            backStack.clear()
                            backStack.add(NovaRoute.Welcome)
                        },
                    )
                }
            }
        },
    )
}
