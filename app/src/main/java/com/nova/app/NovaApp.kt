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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nova.app.app.AppContainer
import com.nova.app.app.AppViewModel
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.feed.FeedStateOwner
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.legal.PrivacyScreen
import com.nova.app.feature.legal.TermsScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.people.PeopleScreen
import com.nova.app.feature.people.PersonScreen
import com.nova.app.feature.post.CreatePostScreen
import com.nova.app.feature.post.PostCommentsScreen
import com.nova.app.feature.post.PostDetailScreen
import com.nova.app.feature.posts.comments.PostCommentsStateOwner
import com.nova.app.feature.posts.detail.PostDetailStateOwner
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.profile.EditProfileScreen
import com.nova.app.feature.profile.ProfileScreen
import com.nova.app.feature.welcome.WelcomeScreen
import com.nova.app.navigation.NovaRoute
import com.nova.app.ui.theme.NovaAccent
import com.nova.app.ui.theme.NovaBackground
import com.nova.app.ui.theme.NovaMuted
import kotlinx.coroutines.launch

@Composable
fun NovaApp(
    appContainer: AppContainer,
    appViewModel: AppViewModel,
) {
    val authRepository = appContainer.authRepository
    val socialRepository = appContainer.socialRepository
    val feedRepository = appContainer.feedDataRepository
    val postRepository = appContainer.postDataRepository
    val appState = appViewModel.state
    val scope = rememberCoroutineScope()
    val feedOwner = remember(feedRepository, postRepository, scope) {
        FeedStateOwner(feedRepository, postRepository, scope)
    }
    val feedState = feedOwner.state

    val backStack = remember {
        mutableStateListOf<NovaRoute>(NovaRoute.Welcome)
    }

    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var people by remember { mutableStateOf<List<NovaPerson>>(emptyList()) }
    var peopleLoading by remember { mutableStateOf(false) }
    var peopleError by remember { mutableStateOf<String?>(null) }
    var followingUsername by remember { mutableStateOf<String?>(null) }
    var peopleRequestVersion by remember { mutableStateOf(0) }

    var handledFeedSessionExpiry by remember { mutableStateOf(0) }
    var handledProfileRefresh by remember { mutableStateOf(0) }
    var handledPostCreated by remember { mutableStateOf(0) }

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
        feedOwner.reset()
    }

    fun resetToWelcome() {
        resetSocialState()
        backStack.clear()
        backStack.add(NovaRoute.Welcome)
    }

    fun clearSessionUi() {
        pendingEmail = ""
        pendingPassword = ""
        authError = null
        resetToWelcome()
    }

    fun expireSession() {
        appViewModel.expireSession()
        clearSessionUi()
    }

    fun refreshCurrentUser() {
        scope.launch {
            if (!appViewModel.refreshSession()) {
                clearSessionUi()
            }
        }
    }

    LaunchedEffect(feedState.sessionExpiryVersion) {
        if (feedState.sessionExpiryVersion > handledFeedSessionExpiry) {
            handledFeedSessionExpiry = feedState.sessionExpiryVersion
            expireSession()
        }
    }

    LaunchedEffect(feedState.profileRefreshVersion) {
        if (feedState.profileRefreshVersion > handledProfileRefresh) {
            handledProfileRefresh = feedState.profileRefreshVersion
            refreshCurrentUser()
        }
    }

    LaunchedEffect(feedState.postCreatedVersion) {
        if (feedState.postCreatedVersion > handledPostCreated) {
            handledPostCreated = feedState.postCreatedVersion
            backStack.removeLastOrNull()
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
                    feedOwner.loadFeed()
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
        appViewModel.bootstrapSession()
        if (appViewModel.state.currentUser != null) {
            openHome()
        } else {
            resetToWelcome()
        }
    }

    if (appState.isBootstrapping) {
        NovaStartupScreen()
        return
    }

    NavDisplay(
        backStack = backStack,
        onBack = {
            if (!authLoading && !feedState.isUploadingPost && backStack.size > 1) {
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
                        onTerms = { backStack.add(NovaRoute.Terms) },
                        onPrivacy = { backStack.add(NovaRoute.Privacy) },
                    )
                }

                NovaRoute.Terms -> NavEntry(route) {
                    TermsScreen(onBack = { backStack.removeLastOrNull() })
                }

                NovaRoute.Privacy -> NavEntry(route) {
                    PrivacyScreen(onBack = { backStack.removeLastOrNull() })
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
                                            appViewModel.onAuthenticated(result.value)
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
                                            appViewModel.onAuthenticated(result.value)
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
                    val user = appState.currentUser
                    LaunchedEffect(user?.id) {
                        if (user != null && feedState.posts.isEmpty() && !feedState.isLoading) {
                            feedOwner.loadFeed()
                        }
                    }

                    HomeScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        posts = feedState.posts,
                        isLoading = feedState.isLoading,
                        isLoadingMore = feedState.isLoadingMore,
                        hasMore = feedState.hasMore,
                        errorMessage = feedState.errorMessage,
                        deletingPostId = feedState.deletingPostId,
                        likingPostId = feedState.likingPostId,
                        onCreatePost = {
                            feedOwner.clearPostError()
                            backStack.add(NovaRoute.CreatePost)
                        },
                        onRefresh = feedOwner::loadFeed,
                        onLoadMore = feedOwner::loadMore,
                        onRetry = feedOwner::loadFeed,
                        onDeletePost = feedOwner::deletePost,
                        onLikeToggle = feedOwner::toggleLike,
                        onCommentsClick = { post ->
                            backStack.add(NovaRoute.PostComments(post.id))
                        },
                        onResolvePost = postRepository::post,
                        onPersonClick = { username ->
                            backStack.add(NovaRoute.Person(username))
                        },
                        onPeopleClick = { backStack.add(NovaRoute.People) },
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.CreatePost -> NavEntry(route) {
                    CreatePostScreen(
                        isLoading = feedState.isUploadingPost,
                        errorMessage = feedState.postErrorMessage,
                        onBack = {
                            if (!feedState.isUploadingPost) {
                                feedOwner.clearPostError()
                                backStack.removeLastOrNull()
                            }
                        },
                        onShare = { imageUri, caption ->
                            feedOwner.createPost(caption = caption, imageUri = imageUri)
                        },
                    )
                }

                is NovaRoute.PostDetail -> NavEntry(route) {
                    val detailScope = rememberCoroutineScope()
                    val detailOwner = remember(route.postId, postRepository, detailScope) {
                        PostDetailStateOwner(route.postId, postRepository, detailScope)
                    }
                    val detailState = detailOwner.state
                    var handledSessionExpiry by remember(detailOwner) { mutableStateOf(0) }
                    var handledMutation by remember(detailOwner) { mutableStateOf(0) }
                    var handledDelete by remember(detailOwner) { mutableStateOf(0) }

                    LaunchedEffect(route.postId, feedState.contentVersion) {
                        detailOwner.load()
                    }
                    LaunchedEffect(detailState.post) {
                        detailState.post?.let(feedOwner::synchronizePost)
                    }
                    LaunchedEffect(detailState.sessionExpiryVersion) {
                        if (detailState.sessionExpiryVersion > handledSessionExpiry) {
                            handledSessionExpiry = detailState.sessionExpiryVersion
                            expireSession()
                        }
                    }
                    LaunchedEffect(detailState.contentMutationVersion) {
                        if (detailState.contentMutationVersion > handledMutation) {
                            handledMutation = detailState.contentMutationVersion
                            feedOwner.markContentChanged()
                        }
                    }
                    LaunchedEffect(detailState.deletedVersion) {
                        if (detailState.deletedVersion > handledDelete) {
                            handledDelete = detailState.deletedVersion
                            feedOwner.removePost(route.postId)
                            refreshCurrentUser()
                            backStack.removeLastOrNull()
                        }
                    }

                    PostDetailScreen(
                        post = detailState.post,
                        isLoading = detailState.isLoading,
                        isLiking = detailState.isLiking,
                        isDeleting = detailState.isDeleting,
                        errorMessage = detailState.errorMessage,
                        onBack = { backStack.removeLastOrNull() },
                        onRetry = detailOwner::load,
                        onAuthorClick = { username ->
                            if (username == appState.currentUser?.username) {
                                backStack.add(NovaRoute.Profile)
                            } else {
                                backStack.add(NovaRoute.Person(username))
                            }
                        },
                        onLikeToggle = detailOwner::toggleLike,
                        onCommentsClick = { selectedPost ->
                            backStack.add(NovaRoute.PostComments(selectedPost.id))
                        },
                        onDelete = detailOwner::delete,
                    )
                }

                is NovaRoute.PostComments -> NavEntry(route) {
                    val commentsScope = rememberCoroutineScope()
                    val commentsOwner = remember(route.postId, postRepository, commentsScope) {
                        PostCommentsStateOwner(
                            postId = route.postId,
                            initialPost = feedState.posts.firstOrNull { it.id == route.postId },
                            repository = postRepository,
                            scope = commentsScope,
                        )
                    }
                    val commentsState = commentsOwner.state
                    var handledSessionExpiry by remember(commentsOwner) { mutableStateOf(0) }
                    var handledMutation by remember(commentsOwner) { mutableStateOf(0) }

                    LaunchedEffect(route.postId) {
                        commentsOwner.load()
                    }
                    LaunchedEffect(commentsState.post) {
                        commentsState.post?.let(feedOwner::synchronizePost)
                    }
                    LaunchedEffect(commentsState.sessionExpiryVersion) {
                        if (commentsState.sessionExpiryVersion > handledSessionExpiry) {
                            handledSessionExpiry = commentsState.sessionExpiryVersion
                            expireSession()
                        }
                    }
                    LaunchedEffect(commentsState.contentMutationVersion) {
                        if (commentsState.contentMutationVersion > handledMutation) {
                            handledMutation = commentsState.contentMutationVersion
                            feedOwner.markContentChanged()
                        }
                    }

                    PostCommentsScreen(
                        post = commentsState.post,
                        comments = commentsState.comments,
                        isLoading = commentsState.isLoading,
                        isSending = commentsState.isSending,
                        deletingCommentId = commentsState.deletingCommentId,
                        isReplySending = commentsState.isReplySending,
                        deletingReplyId = commentsState.deletingReplyId,
                        errorMessage = commentsState.errorMessage,
                        replyErrorMessage = commentsState.replyErrorMessage,
                        onBack = { backStack.removeLastOrNull() },
                        onRetry = commentsOwner::load,
                        onSend = commentsOwner::sendComment,
                        onDelete = commentsOwner::deleteComment,
                        onSendReply = commentsOwner::sendReply,
                        onDeleteReply = commentsOwner::deleteReply,
                        onClearReplyError = commentsOwner::clearReplyError,
                        onAuthorClick = { username ->
                            if (username == appState.currentUser?.username) {
                                backStack.add(NovaRoute.Profile)
                            } else {
                                backStack.add(NovaRoute.Person(username))
                            }
                        },
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
                    var profilePosts by remember(route.username) { mutableStateOf<List<NovaPost>>(emptyList()) }
                    var profilePostsLoading by remember(route.username) { mutableStateOf(true) }
                    var profilePostsError by remember(route.username) { mutableStateOf<String?>(null) }

                    fun loadPersonPosts() {
                        scope.launch {
                            profilePostsLoading = true
                            profilePostsError = null
                            when (val result = postRepository.personPosts(route.username)) {
                                is ApiResult.Success -> {
                                    profilePosts = result.value
                                    profilePostsLoading = false
                                }

                                is ApiResult.Failure -> {
                                    profilePostsLoading = false
                                    if (result.statusCode == 401) {
                                        expireSession()
                                    } else {
                                        profilePostsError = result.message
                                    }
                                }
                            }
                        }
                    }

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

                    LaunchedEffect(route.username, feedState.contentVersion) {
                        profilePostsLoading = true
                        profilePostsError = null
                        when (val result = postRepository.personPosts(route.username)) {
                            is ApiResult.Success -> {
                                profilePosts = result.value
                                profilePostsLoading = false
                            }

                            is ApiResult.Failure -> {
                                profilePostsLoading = false
                                if (result.statusCode == 401) {
                                    expireSession()
                                } else {
                                    profilePostsError = result.message
                                }
                            }
                        }
                    }

                    PersonScreen(
                        person = person,
                        isLoading = personLoading,
                        errorMessage = personError,
                        profilePosts = profilePosts,
                        postsLoading = profilePostsLoading,
                        postsError = profilePostsError,
                        onRetryPosts = ::loadPersonPosts,
                        onPostClick = { post ->
                            backStack.add(NovaRoute.PostDetail(post.id))
                        },
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
                                            feedOwner.loadFeed()
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
                        onBlocked = { blockedPerson ->
                            people = people.filterNot { it.id == blockedPerson.id }
                            feedOwner.removePostsByAuthor(blockedPerson.id)
                            refreshCurrentUser()
                            backStack.removeLastOrNull()
                        },
                    )
                }

                NovaRoute.Profile -> NavEntry(route) {
                    val user = appState.currentUser
                    val profileUsername = user?.username.orEmpty()
                    var profilePosts by remember(profileUsername) { mutableStateOf<List<NovaPost>>(emptyList()) }
                    var profilePostsLoading by remember(profileUsername) { mutableStateOf(true) }
                    var profilePostsError by remember(profileUsername) { mutableStateOf<String?>(null) }

                    fun loadProfilePosts() {
                        if (profileUsername.isBlank()) return
                        scope.launch {
                            profilePostsLoading = true
                            profilePostsError = null
                            when (val result = postRepository.personPosts(profileUsername)) {
                                is ApiResult.Success -> {
                                    profilePosts = result.value
                                    profilePostsLoading = false
                                }

                                is ApiResult.Failure -> {
                                    profilePostsLoading = false
                                    if (result.statusCode == 401) {
                                        expireSession()
                                    } else {
                                        profilePostsError = result.message
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(profileUsername, feedState.contentVersion) {
                        if (profileUsername.isNotBlank()) {
                            profilePostsLoading = true
                            profilePostsError = null
                            when (val result = postRepository.personPosts(profileUsername)) {
                                is ApiResult.Success -> {
                                    profilePosts = result.value
                                    profilePostsLoading = false
                                }

                                is ApiResult.Failure -> {
                                    profilePostsLoading = false
                                    if (result.statusCode == 401) {
                                        expireSession()
                                    } else {
                                        profilePostsError = result.message
                                    }
                                }
                            }
                        }
                    }

                    ProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        email = user?.email.orEmpty(),
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        postsCount = user?.postsCount ?: 0,
                        followersCount = user?.followersCount ?: 0,
                        followingCount = user?.followingCount ?: 0,
                        profilePosts = profilePosts,
                        postsLoading = profilePostsLoading,
                        postsError = profilePostsError,
                        onRetryPosts = ::loadProfilePosts,
                        onPostClick = { post ->
                            backStack.add(NovaRoute.PostDetail(post.id))
                        },
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
                    val user = appState.currentUser
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
                                            appViewModel.onAuthenticated(result.value)
                                            authLoading = false
                                            feedOwner.markContentChanged()
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