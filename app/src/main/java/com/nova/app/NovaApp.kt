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
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.network.NovaUser
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.people.PeopleScreen
import com.nova.app.feature.people.PersonScreen
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
    val socialRepository = remember(context) {
        NovaSocialRepository(context.applicationContext)
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

    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var peopleLoading by remember { mutableStateOf(false) }
    var peopleError by remember { mutableStateOf<String?>(null) }
    var followingUsername by remember { mutableStateOf<String?>(null) }
    var peopleRequestVersion by remember { mutableStateOf(0) }

    fun openHome() {
        backStack.clear()
        backStack.add(NovaRoute.Home)
    }

    fun resetSocialState() {
        people = emptyList()
        peopleLoading = false
        peopleError = null
        followingUsername = null
        peopleRequestVersion += 1
    }

    fun resetToWelcome() {
        resetSocialState()
        backStack.clear()
        backStack.add(NovaRoute.Welcome)
    }

    fun expireSession() {
        authRepository.logout()
        currentUser = null
        pendingEmail = ""
        pendingPassword = ""
        authError = null
        resetToWelcome()
    }

    fun refreshCurrentUser() {
        scope.launch {
            when (val refreshed = authRepository.restoreSession()) {
                is ApiResult.Success -> {
                    if (refreshed.value == null) {
                        expireSession()
                    } else {
                        currentUser = refreshed.value
                    }
                }

                is ApiResult.Failure -> {
                    if (refreshed.statusCode == 401) expireSession()
                }
            }
        }
    }

    fun searchPeople(query: String) {
        peopleRequestVersion += 1
        val requestVersion = peopleRequestVersion

        scope.launch {
            peopleLoading = true
            peopleError = null

            when (val result = socialRepository.people(query)) {
                is ApiResult.Success -> {
                    if (requestVersion == peopleRequestVersion) {
                        people = result.value
                        peopleLoading = false
                    }
                }

                is ApiResult.Failure -> {
                    if (requestVersion == peopleRequestVersion) {
                        peopleLoading = false
                        if (result.statusCode == 401) {
                            expireSession()
                        } else {
                            peopleError = result.message
                        }
                    }
                }
            }
        }
    }

    fun toggleFollowFromList(person: NovaPerson) {
        if (followingUsername != null) return

        scope.launch {
            followingUsername = person.username
            peopleError = null

            when (
                val result = socialRepository.setFollowing(
                    username = person.username,
                    follow = !person.isFollowing,
                )
            ) {
                is ApiResult.Success -> {
                    people = people.map { existing ->
                        if (existing.id == result.value.id) result.value else existing
                    }
                    followingUsername = null
                    refreshCurrentUser()
                }

                is ApiResult.Failure -> {
                    followingUsername = null
                    if (result.statusCode == 401) {
                        expireSession()
                    } else {
                        peopleError = result.message
                    }
                }
            }
        }
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
                                            resetSocialState()
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
                                            resetSocialState()
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
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        onPeopleClick = { backStack.add(NovaRoute.People) },
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.People -> NavEntry(route) {
                    PeopleScreen(
                        people = people,
                        isLoading = peopleLoading,
                        errorMessage = peopleError,
                        followingUsername = followingUsername,
                        onSearch = ::searchPeople,
                        onPersonClick = { username ->
                            backStack.add(NovaRoute.Person(username))
                        },
                        onFollowToggle = ::toggleFollowFromList,
                        onHomeClick = ::openHome,
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                is NovaRoute.Person -> NavEntry(route) {
                    var person by remember(route.username) { mutableStateOf<NovaPerson?>(null) }
                    var personLoading by remember(route.username) { mutableStateOf(true) }
                    var personError by remember(route.username) { mutableStateOf<String?>(null) }

                    LaunchedEffect(route.username) {
                        personLoading = true
                        personError = null
                        when (val result = socialRepository.person(route.username)) {
                            is ApiResult.Success -> person = result.value
                            is ApiResult.Failure -> {
                                if (result.statusCode == 401) {
                                    expireSession()
                                } else {
                                    personError = result.message
                                }
                            }
                        }
                        personLoading = false
                    }

                    PersonScreen(
                        person = person,
                        isLoading = personLoading,
                        errorMessage = personError,
                        onBack = { backStack.removeLastOrNull() },
                        onFollowToggle = { selectedPerson ->
                            if (!personLoading) {
                                scope.launch {
                                    personLoading = true
                                    personError = null
                                    when (
                                        val result = socialRepository.setFollowing(
                                            username = selectedPerson.username,
                                            follow = !selectedPerson.isFollowing,
                                        )
                                    ) {
                                        is ApiResult.Success -> {
                                            person = result.value
                                            people = people.map { existing ->
                                                if (existing.id == result.value.id) result.value else existing
                                            }
                                            refreshCurrentUser()
                                        }

                                        is ApiResult.Failure -> {
                                            if (result.statusCode == 401) {
                                                expireSession()
                                            } else {
                                                personError = result.message
                                            }
                                        }
                                    }
                                    personLoading = false
                                }
                            }
                        },
                    )
                }

                NovaRoute.Profile -> NavEntry(route) {
                    val user = currentUser
                    ProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        email = user?.email.orEmpty(),
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        followersCount = user?.followersCount ?: 0,
                        followingCount = user?.followingCount ?: 0,
                        onHomeClick = ::openHome,
                        onPeopleClick = { backStack.add(NovaRoute.People) },
                        onEditProfile = {
                            authError = null
                            backStack.add(NovaRoute.EditProfile)
                        },
                        onLogout = ::expireSession,
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
                                                expireSession()
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
