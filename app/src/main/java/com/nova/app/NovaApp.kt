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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import com.nova.app.app.AppContainer
import com.nova.app.app.AppViewModel
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.create.CreateHubScreen
import com.nova.app.feature.feed.FeedStateOwner
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.legal.PrivacyScreen
import com.nova.app.feature.legal.TermsScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.orbit.OrbitScreen
import com.nova.app.feature.people.PeopleScreen
import com.nova.app.feature.people.PeopleStateOwner
import com.nova.app.feature.people.PersonScreen
import com.nova.app.feature.people.PersonStateOwner
import com.nova.app.feature.post.CreatePostScreen
import com.nova.app.feature.post.PostCommentsScreen
import com.nova.app.feature.post.PostDetailScreen
import com.nova.app.feature.posts.comments.PostCommentsStateOwner
import com.nova.app.feature.posts.detail.PostDetailStateOwner
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import com.nova.app.feature.profile.EditProfileScreen
import com.nova.app.feature.profile.ProfileContentStateOwner
import com.nova.app.feature.profile.ProfileScreen
import com.nova.app.feature.pulse.PulseScreen
import com.nova.app.feature.publishing.MediaPublishTarget
import com.nova.app.feature.publishing.MediaPublishWorker
import com.nova.app.feature.publishing.MediaPublishingStateOwner
import com.nova.app.feature.tonight.TonightScreen
import com.nova.app.feature.welcome.WelcomeScreen
import com.nova.app.navigation.AppDestination
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
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
    val peopleRepository = appContainer.peopleRepository
    val peoplePagingRepository = appContainer.peoplePagingRepository
    val feedRepository = appContainer.feedDataRepository
    val postRepository = appContainer.postDataRepository
    val postRepostRepository = appContainer.postRepostRepository
    val appState = appViewModel.state
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val feedOwner = remember(feedRepository, postRepository, postRepostRepository, scope) {
        FeedStateOwner(feedRepository, postRepository, postRepostRepository, scope)
    }
    val feedState = feedOwner.state
    val publishingOwner = remember(context, scope) { MediaPublishingStateOwner(context, scope) }
    val publishingState = publishingOwner.state

    val backStack = remember {
        mutableStateListOf<NovaRoute>(NovaRoute.Welcome)
    }
    val rootRequestVersion = NovaRootNavigationSignal.requestVersion
    val tonightRequestVersion = NovaRootNavigationSignal.tonightRequestVersion
    val personRequestVersion = NovaRootNavigationSignal.personRequestVersion

    var pendingEmail by remember { mutableStateOf("") }
    var pendingPassword by remember { mutableStateOf("") }
    var authLoading by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    var handledFeedSessionExpiry by remember { mutableStateOf(0) }
    var handledProfileRefresh by remember { mutableStateOf(0) }
    var handledPostCreated by remember { mutableStateOf(0) }
    var handledBackgroundPostPublished by remember { mutableStateOf(0) }

    fun openRoot(tab: NovaRootTab) {
        backStack.clear()
        backStack.add(
            when (tab) {
                NovaRootTab.Home -> NovaRoute.Home
                NovaRootTab.Orbit -> NovaRoute.Orbit
                NovaRootTab.Create -> NovaRoute.Create
                NovaRootTab.Profile -> NovaRoute.Profile
            }
        )
    }

    fun openHome() = openRoot(NovaRootTab.Home)

    fun resetSocialState() {
        feedOwner.reset()
        publishingOwner.reset()
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

    LaunchedEffect(rootRequestVersion, appState.currentUser?.id) {
        val requested = NovaRootNavigationSignal.pendingTab ?: return@LaunchedEffect
        if (appState.currentUser == null) return@LaunchedEffect
        openRoot(requested)
        NovaRootNavigationSignal.consume(requested)
    }

    LaunchedEffect(tonightRequestVersion, appState.currentUser?.id) {
        if (
            NovaRootNavigationSignal.pendingTonight &&
            appState.currentUser != null
        ) {
            backStack.clear()
            backStack.add(NovaRoute.Tonight)
            NovaRootNavigationSignal.consumeTonight()
        }
    }

    LaunchedEffect(personRequestVersion, appState.currentUser?.id) {
        val requested = NovaRootNavigationSignal.pendingPersonUsername ?: return@LaunchedEffect
        val currentUser = appState.currentUser ?: return@LaunchedEffect
        if (requested == currentUser.username) {
            openRoot(NovaRootTab.Profile)
        } else if (backStack.lastOrNull() != NovaRoute.Person(requested)) {
            backStack.add(NovaRoute.Person(requested))
        }
        NovaRootNavigationSignal.consumePerson(requested)
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

    LaunchedEffect(appState.currentUser?.id) {
        appState.currentUser?.id?.let(publishingOwner::enter) ?: publishingOwner.reset()
    }

    LaunchedEffect(publishingState.postPublishedVersion) {
        if (publishingState.postPublishedVersion > handledBackgroundPostPublished) {
            handledBackgroundPostPublished = publishingState.postPublishedVersion
            feedOwner.loadFeed()
            refreshCurrentUser()
        }
    }

    LaunchedEffect(Unit) {
        val hydratedFromCache = appViewModel.hydrateCachedSession()
        if (hydratedFromCache) {
            openHome()
        }
        appViewModel.bootstrapSession()
        if (appViewModel.state.currentUser != null) {
            if (!hydratedFromCache) openHome()
        } else {
            resetToWelcome()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, appState.currentUser?.id) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                appState.currentUser?.id?.let(feedOwner::onForeground)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
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
                        isLoading = authLoading,
                        errorMessage = authError,
                        onBack = { backStack.removeLastOrNull() },
                        onCreate = { email, password, handle, name ->
                            if (!authLoading) {
                                scope.launch {
                                    authLoading = true
                                    authError = null
                                    when (
                                        val result = authRepository.register(
                                            email = email,
                                            password = password,
                                            username = handle,
                                            name = name,
                                        )
                                    ) {
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
                    val homeFeedState = feedOwner.state
                    val homePublishingState = publishingOwner.state
                    LaunchedEffect(user?.id) {
                        if (user == null) feedOwner.reset() else feedOwner.enter(user.id)
                    }

                    val feedBelongsToUser = user != null && homeFeedState.userId == user.id

                    HomeScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        posts = homeFeedState.posts.takeIf { feedBelongsToUser }.orEmpty(),
                        isLoading = !feedBelongsToUser || homeFeedState.isLoading,
                        isRefreshing = feedBelongsToUser && homeFeedState.isRefreshing,
                        isLoadingMore = feedBelongsToUser && homeFeedState.isLoadingMore,
                        hasMore = feedBelongsToUser && homeFeedState.hasMore,
                        errorMessage = homeFeedState.errorMessage.takeIf { feedBelongsToUser },
                        deletingPostId = homeFeedState.deletingPostId.takeIf { feedBelongsToUser },
                        likingPostIds = homeFeedState.likingPostIds.takeIf { feedBelongsToUser }.orEmpty(),
                        repostingPostIds = homeFeedState.repostingPostIds.takeIf { feedBelongsToUser }.orEmpty(),
                        actionErrorPostId = homeFeedState.actionErrorPostId.takeIf { feedBelongsToUser },
                        actionErrorMessage = homeFeedState.actionErrorMessage.takeIf { feedBelongsToUser },
                        publishingItems = homePublishingState.items.takeIf {
                            homePublishingState.userId == user?.id
                        }.orEmpty(),
                        onCreatePost = {
                            feedOwner.clearPostError()
                            backStack.add(NovaRoute.CreatePost)
                        },
                        onRetryPublish = publishingOwner::retry,
                        onCancelPublish = publishingOwner::cancel,
                        onRefresh = feedOwner::refreshFeed,
                        onLoadMore = feedOwner::loadMore,
                        onRetry = feedOwner::loadFeed,
                        onDeletePost = feedOwner::deletePost,
                        onLikeToggle = feedOwner::toggleLike,
                        onRepostToggle = feedOwner::toggleRepost,
                        onPostClick = { post -> backStack.add(NovaRoute.PostDetail(post.id)) },
                        onCommentsClick = { post ->
                            backStack.add(NovaRoute.PostComments(post.id))
                        },
                        onResolvePost = postRepository::post,
                        onPersonClick = { username ->
                            backStack.add(NovaRoute.Person(username))
                        },
                        onPeopleClick = { backStack.add(NovaRoute.People) },
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onPulseClick = { backStack.add(NovaRoute.Pulse) },
                        onTonightClick = { backStack.add(NovaRoute.Tonight) },
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.Orbit -> NavEntry(route) {
                    val user = appState.currentUser
                    OrbitScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        onPersonClick = { username -> backStack.add(NovaRoute.Person(username)) },
                        onPostClick = { postId -> backStack.add(NovaRoute.PostDetail(postId)) },
                        onDiscoveryClick = { backStack.add(NovaRoute.People) },
                        onHomeClick = { openRoot(NovaRootTab.Home) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onProfileClick = { openRoot(NovaRootTab.Profile) },
                        onSessionExpired = ::expireSession,
                    )
                }

                NovaRoute.Create -> NavEntry(route) {
                    val user = appState.currentUser
                    CreateHubScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        onCreatePost = {
                            feedOwner.clearPostError()
                            backStack.add(NovaRoute.CreatePost)
                        },
                        onOpenReels = { appViewModel.navigate(AppDestination.Reels) },
                        onPersonClick = { username -> backStack.add(NovaRoute.Person(username)) },
                        onHomeClick = { openRoot(NovaRootTab.Home) },
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onProfileClick = { openRoot(NovaRootTab.Profile) },
                        onSessionExpired = ::expireSession,
                    )
                }

                NovaRoute.Pulse -> NavEntry(route) {
                    PulseScreen(
                        onHomeClick = { openRoot(NovaRootTab.Home) },
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onProfileClick = { openRoot(NovaRootTab.Profile) },
                        onSessionExpired = ::expireSession,
                    )
                }

                NovaRoute.Tonight -> NavEntry(route) {
                    TonightScreen(
                        onPersonClick = { username -> backStack.add(NovaRoute.Person(username)) },
                        onHomeClick = { openRoot(NovaRootTab.Home) },
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onProfileClick = { openRoot(NovaRootTab.Profile) },
                        onSessionExpired = ::expireSession,
                    )
                }

                NovaRoute.CreatePost -> NavEntry(route) {
                    CreatePostScreen(
                        isLoading = false,
                        errorMessage = null,
                        onBack = {
                            feedOwner.clearPostError()
                            backStack.removeLastOrNull()
                        },
                        onShare = { mediaUri, caption ->
                            appState.currentUser?.id?.let { userId ->
                                MediaPublishWorker.enqueue(
                                    context = context,
                                    target = MediaPublishTarget.POST,
                                    userId = userId,
                                    sourceUri = mediaUri,
                                    caption = caption,
                                )
                                publishingOwner.enter(userId)
                                backStack.removeLastOrNull()
                            }
                        },
                    )
                }

                is NovaRoute.PostDetail -> NavEntry(route) {
                    val detailScope = rememberCoroutineScope()
                    val detailOwner = remember(route.postId, postRepository, postRepostRepository, detailScope) {
                        PostDetailStateOwner(route.postId, postRepository, postRepostRepository, detailScope)
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
                        isReposting = detailState.isReposting,
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
                        onRepostToggle = detailOwner::toggleRepost,
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
                            currentUser = appState.currentUser?.let {
                                NovaPostAuthor(it.id, it.username, it.name, it.avatarUrl)
                            } ?: NovaPostAuthor(0L, "nova", "Nova user", ""),
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
                        likingCommentIds = commentsState.likingCommentIds,
                        errorMessage = commentsState.errorMessage,
                        replyErrorMessage = commentsState.replyErrorMessage,
                        onBack = { backStack.removeLastOrNull() },
                        onRetry = commentsOwner::load,
                        onSend = commentsOwner::sendComment,
                        onDelete = commentsOwner::deleteComment,
                        onSendReply = commentsOwner::sendReply,
                        onDeleteReply = commentsOwner::deleteReply,
                        onLike = commentsOwner::toggleLike,
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
                    val peopleScope = rememberCoroutineScope()
                    val peopleOwner = remember(peopleRepository, peoplePagingRepository, peopleScope) {
                        PeopleStateOwner(peopleRepository, peoplePagingRepository, peopleScope)
                    }
                    val peopleState = peopleOwner.state
                    var handledSessionExpiry by remember(peopleOwner) { mutableStateOf(0) }
                    var handledProfileRefresh by remember(peopleOwner) { mutableStateOf(0) }
                    var handledFeedRefresh by remember(peopleOwner) { mutableStateOf(0) }

                    LaunchedEffect(peopleOwner) {
                        peopleOwner.enter()
                    }
                    LaunchedEffect(peopleState.sessionExpiryVersion) {
                        if (peopleState.sessionExpiryVersion > handledSessionExpiry) {
                            handledSessionExpiry = peopleState.sessionExpiryVersion
                            expireSession()
                        }
                    }
                    LaunchedEffect(peopleState.profileRefreshVersion) {
                        if (peopleState.profileRefreshVersion > handledProfileRefresh) {
                            handledProfileRefresh = peopleState.profileRefreshVersion
                            refreshCurrentUser()
                        }
                    }
                    LaunchedEffect(peopleState.feedRefreshVersion) {
                        if (peopleState.feedRefreshVersion > handledFeedRefresh) {
                            handledFeedRefresh = peopleState.feedRefreshVersion
                            feedOwner.loadFeed()
                        }
                    }

                    PeopleScreen(
                        state = peopleState,
                        onQueryChange = peopleOwner::setQuery,
                        onFilterChange = peopleOwner::setFilter,
                        onPersonClick = { username ->
                            backStack.add(NovaRoute.Person(username))
                        },
                        onFollowToggle = peopleOwner::toggleFollow,
                        onRetry = peopleOwner::retry,
                        onLoadMore = peopleOwner::loadMore,
                        onHomeClick = ::openHome,
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                is NovaRoute.Person -> NavEntry(route) {
                    val personScope = rememberCoroutineScope()
                    val personOwner = remember(route.username, peopleRepository, postRepository, personScope) {
                        PersonStateOwner(
                            username = route.username,
                            peopleRepository = peopleRepository,
                            postRepository = postRepository,
                            scope = personScope,
                        )
                    }
                    val personState = personOwner.state
                    var handledSessionExpiry by remember(personOwner) { mutableStateOf(0) }
                    var handledProfileRefresh by remember(personOwner) { mutableStateOf(0) }
                    var handledFeedRefresh by remember(personOwner) { mutableStateOf(0) }

                    LaunchedEffect(route.username) {
                        personOwner.loadPerson()
                    }
                    LaunchedEffect(route.username, feedState.contentVersion) {
                        personOwner.loadPosts()
                    }
                    LaunchedEffect(personState.sessionExpiryVersion) {
                        if (personState.sessionExpiryVersion > handledSessionExpiry) {
                            handledSessionExpiry = personState.sessionExpiryVersion
                            expireSession()
                        }
                    }
                    LaunchedEffect(personState.profileRefreshVersion) {
                        if (personState.profileRefreshVersion > handledProfileRefresh) {
                            handledProfileRefresh = personState.profileRefreshVersion
                            refreshCurrentUser()
                        }
                    }
                    LaunchedEffect(personState.feedRefreshVersion) {
                        if (personState.feedRefreshVersion > handledFeedRefresh) {
                            handledFeedRefresh = personState.feedRefreshVersion
                            feedOwner.loadFeed()
                        }
                    }

                    PersonScreen(
                        person = personState.person,
                        isLoading = personState.isLoading,
                        errorMessage = personState.errorMessage,
                        profilePosts = personState.profilePosts,
                        postsLoading = personState.postsLoading,
                        postsError = personState.postsError,
                        onRetryProfile = personOwner::loadPerson,
                        onRetryPosts = personOwner::loadPosts,
                        onPostClick = { post ->
                            backStack.add(NovaRoute.PostDetail(post.id))
                        },
                        onBack = { backStack.removeLastOrNull() },
                        onFollowToggle = personOwner::toggleFollow,
                        onBlocked = { blockedPerson ->
                            feedOwner.removePostsByAuthor(blockedPerson.id)
                            refreshCurrentUser()
                            backStack.removeLastOrNull()
                        },
                    )
                }

                NovaRoute.Profile -> NavEntry(route) {
                    val user = appState.currentUser
                    val profileUsername = user?.username.orEmpty()
                    val profileScope = rememberCoroutineScope()
                    val profileContentOwner = remember(
                        profileUsername,
                        postRepository,
                        peoplePagingRepository,
                        profileScope,
                    ) {
                        ProfileContentStateOwner(
                            username = profileUsername,
                            postRepository = postRepository,
                            pagingRepository = peoplePagingRepository,
                            scope = profileScope,
                        )
                    }
                    val profileContentState = profileContentOwner.state
                    var handledContentSessionExpiry by remember(profileContentOwner) { mutableStateOf(0) }

                    LaunchedEffect(profileUsername, feedState.contentVersion) {
                        profileContentOwner.loadPosts()
                    }
                    LaunchedEffect(profileContentState.sessionExpiryVersion) {
                        if (profileContentState.sessionExpiryVersion > handledContentSessionExpiry) {
                            handledContentSessionExpiry = profileContentState.sessionExpiryVersion
                            expireSession()
                        }
                    }

                    ProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        bio = user?.bio.orEmpty(),
                        location = user?.location.orEmpty(),
                        link = user?.link.orEmpty(),
                        interests = user?.interests.orEmpty(),
                        profileTheme = user?.profileTheme ?: "violet",
                        showOrbit = user?.showOrbit ?: true,
                        isVerified = user?.isVerified ?: false,
                        postsCount = user?.postsCount ?: 0,
                        followersCount = user?.followersCount ?: 0,
                        followingCount = user?.followingCount ?: 0,
                        profileContentOwner = profileContentOwner,
                        onPostClick = { post ->
                            backStack.add(NovaRoute.PostDetail(post.id))
                        },
                        onHomeClick = ::openHome,
                        onOrbitClick = { openRoot(NovaRootTab.Orbit) },
                        onCreateClick = { openRoot(NovaRootTab.Create) },
                        onEditProfile = {
                            authError = null
                            backStack.add(NovaRoute.EditProfile)
                        },
                    )
                }

                NovaRoute.EditProfile -> NavEntry(route) {
                    val user = appState.currentUser
                    EditProfileScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        bio = user?.bio.orEmpty(),
                        location = user?.location.orEmpty(),
                        link = user?.link.orEmpty(),
                        interests = user?.interests.orEmpty(),
                        profileTheme = user?.profileTheme ?: "violet",
                        showOrbit = user?.showOrbit ?: true,
                        isVerified = user?.isVerified ?: false,
                        isLoading = authLoading,
                        errorMessage = authError,
                        onBack = {
                            if (!authLoading) {
                                authError = null
                                backStack.removeLastOrNull()
                            }
                        },
                        onSave = { name, handle, avatarUri, bio, location, link, interests, theme, showOrbit ->
                            if (!authLoading) {
                                scope.launch {
                                    authLoading = true
                                    authError = null

                                    when (
                                        val result = authRepository.updateProfile(
                                            name = name,
                                            username = handle,
                                            avatarUri = avatarUri,
                                            bio = bio,
                                            location = location,
                                            link = link,
                                            interests = interests,
                                            profileTheme = theme,
                                            showOrbit = showOrbit,
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
