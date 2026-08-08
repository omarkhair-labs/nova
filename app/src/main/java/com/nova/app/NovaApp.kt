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
import com.nova.app.core.feed.NovaFeedRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaComment
import com.nova.app.core.network.NovaPerson
import com.nova.app.core.network.NovaPost
import com.nova.app.core.network.NovaUser
import com.nova.app.core.social.NovaSocialRepository
import com.nova.app.feature.auth.CreateAccountScreen
import com.nova.app.feature.auth.LoginScreen
import com.nova.app.feature.home.HomeScreen
import com.nova.app.feature.onboarding.ProfileSetupScreen
import com.nova.app.feature.people.PeopleScreen
import com.nova.app.feature.people.PersonScreen
import com.nova.app.feature.post.CreatePostScreen
import com.nova.app.feature.post.PostCommentsScreen
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
    val feedRepository = remember(context) {
        NovaFeedRepository(context.applicationContext)
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

    var posts by remember { mutableStateOf<List<NovaPost>>(emptyList()) }
    var feedLoading by remember { mutableStateOf(false) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var deletingPostId by remember { mutableStateOf<Long?>(null) }
    var likingPostId by remember { mutableStateOf<Long?>(null) }
    var postUploading by remember { mutableStateOf(false) }
    var postError by remember { mutableStateOf<String?>(null) }

    fun openHome() {
        backStack.clear()
        backStack.add(NovaRoute.Home)
    }

    fun replacePost(updated: NovaPost) {
        posts = posts.map { existing ->
            if (existing.id == updated.id) updated else existing
        }
    }

    fun resetSocialState() {
        people = emptyList()
        peopleLoading = false
        peopleError = null
        followingUsername = null
        peopleRequestVersion += 1
        posts = emptyList()
        feedLoading = false
        feedError = null
        deletingPostId = null
        likingPostId = null
        postUploading = false
        postError = null
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

    fun loadFeed() {
        if (feedLoading) return
        scope.launch {
            feedLoading = true
            feedError = null
            when (val result = feedRepository.feed()) {
                is ApiResult.Success -> {
                    posts = result.value
                    feedLoading = false
                }

                is ApiResult.Failure -> {
                    feedLoading = false
                    if (result.statusCode == 401) {
                        expireSession()
                    } else {
                        feedError = result.message
                    }
                }
            }
        }
    }

    fun deletePost(post: NovaPost) {
        if (deletingPostId != null || !post.isMine) return
        scope.launch {
            deletingPostId = post.id
            feedError = null
            when (val result = feedRepository.deletePost(post.id)) {
                is ApiResult.Success -> {
                    posts = posts.filterNot { it.id == post.id }
                    deletingPostId = null
                    refreshCurrentUser()
                }

                is ApiResult.Failure -> {
                    deletingPostId = null
                    if (result.statusCode == 401) {
                        expireSession()
                    } else {
                        feedError = result.message
                    }
                }
            }
        }
    }

    fun toggleLike(post: NovaPost) {
        if (likingPostId != null) return
        scope.launch {
            likingPostId = post.id
            feedError = null
            when (
                val result = feedRepository.setLiked(
                    postId = post.id,
                    liked = !post.isLiked,
                )
            ) {
                is ApiResult.Success -> {
                    replacePost(result.value)
                    likingPostId = null
                }

                is ApiResult.Failure -> {
                    likingPostId = null
                    if (result.statusCode == 401) {
                        expireSession()
                    } else {
                        feedError = result.message
                    }
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
                    loadFeed()
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
            if (!authLoading && !postUploading && backStack.size > 1) {
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
                    LaunchedEffect(user?.id) {
                        if (user != null && posts.isEmpty() && !feedLoading) {
                            loadFeed()
                        }
                    }

                    HomeScreen(
                        displayName = user?.name?.ifBlank { user.username } ?: "Nova user",
                        username = user?.username ?: "nova",
                        avatarUrl = user?.avatarUrl.orEmpty(),
                        posts = posts,
                        isLoading = feedLoading,
                        errorMessage = feedError,
                        deletingPostId = deletingPostId,
                        likingPostId = likingPostId,
                        onCreatePost = {
                            postError = null
                            backStack.add(NovaRoute.CreatePost)
                        },
                        onRetry = ::loadFeed,
                        onDeletePost = ::deletePost,
                        onLikeToggle = ::toggleLike,
                        onCommentsClick = { post ->
                            backStack.add(NovaRoute.PostComments(post.id))
                        },
                        onPersonClick = { username ->
                            backStack.add(NovaRoute.Person(username))
                        },
                        onPeopleClick = { backStack.add(NovaRoute.People) },
                        onProfileClick = { backStack.add(NovaRoute.Profile) },
                    )
                }

                NovaRoute.CreatePost -> NavEntry(route) {
                    CreatePostScreen(
                        isLoading = postUploading,
                        errorMessage = postError,
                        onBack = {
                            if (!postUploading) {
                                postError = null
                                backStack.removeLastOrNull()
                            }
                        },
                        onShare = { imageUri, caption ->
                            if (!postUploading) {
                                scope.launch {
                                    postUploading = true
                                    postError = null
                                    when (
                                        val result = feedRepository.createPost(
                                            caption = caption,
                                            imageUri = imageUri,
                                        )
                                    ) {
                                        is ApiResult.Success -> {
                                            posts = listOf(result.value) + posts.filterNot {
                                                it.id == result.value.id
                                            }
                                            postUploading = false
                                            refreshCurrentUser()
                                            backStack.removeLastOrNull()
                                        }

                                        is ApiResult.Failure -> {
                                            postUploading = false
                                            if (result.statusCode == 401) {
                                                expireSession()
                                            } else {
                                                postError = result.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                    )
                }

                is NovaRoute.PostComments -> NavEntry(route) {
                    val post = posts.firstOrNull { it.id == route.postId }
                    var comments by remember(route.postId) {
                        mutableStateOf<List<NovaComment>>(emptyList())
                    }
                    var commentsLoading by remember(route.postId) { mutableStateOf(true) }
                    var commentSending by remember(route.postId) { mutableStateOf(false) }
                    var deletingCommentId by remember(route.postId) { mutableStateOf<Long?>(null) }
                    var commentsError by remember(route.postId) { mutableStateOf<String?>(null) }

                    fun loadComments() {
                        if (commentsLoading && comments.isNotEmpty()) return
                        scope.launch {
                            commentsLoading = true
                            commentsError = null
                            when (val result = feedRepository.comments(route.postId)) {
                                is ApiResult.Success -> {
                                    comments = result.value
                                    commentsLoading = false
                                }

                                is ApiResult.Failure -> {
                                    commentsLoading = false
                                    if (result.statusCode == 401) {
                                        expireSession()
                                    } else {
                                        commentsError = result.message
                                    }
                                }
                            }
                        }
                    }

                    LaunchedEffect(route.postId) {
                        commentsLoading = true
                        commentsError = null
                        when (val result = feedRepository.comments(route.postId)) {
                            is ApiResult.Success -> {
                                comments = result.value
                                commentsLoading = false
                            }

                            is ApiResult.Failure -> {
                                commentsLoading = false
                                if (result.statusCode == 401) {
                                    expireSession()
                                } else {
                                    commentsError = result.message
                                }
                            }
                        }
                    }

                    PostCommentsScreen(
                        post = post,
                        comments = comments,
                        isLoading = commentsLoading,
                        isSending = commentSending,
                        deletingCommentId = deletingCommentId,
                        errorMessage = commentsError,
                        onBack = { backStack.removeLastOrNull() },
                        onRetry = ::loadComments,
                        onSend = { body ->
                            if (!commentSending) {
                                scope.launch {
                                    commentSending = true
                                    commentsError = null
                                    when (
                                        val result = feedRepository.addComment(
                                            postId = route.postId,
                                            body = body,
                                        )
                                    ) {
                                        is ApiResult.Success -> {
                                            comments = comments + result.value.comment
                                            replacePost(result.value.post)
                                            commentSending = false
                                        }

                                        is ApiResult.Failure -> {
                                            commentSending = false
                                            if (result.statusCode == 401) {
                                                expireSession()
                                            } else {
                                                commentsError = result.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onDelete = { comment ->
                            if (deletingCommentId == null && comment.isMine) {
                                scope.launch {
                                    deletingCommentId = comment.id
                                    commentsError = null
                                    when (val result = feedRepository.deleteComment(comment.id)) {
                                        is ApiResult.Success -> {
                                            comments = comments.filterNot { it.id == comment.id }
                                            replacePost(result.value)
                                            deletingCommentId = null
                                        }

                                        is ApiResult.Failure -> {
                                            deletingCommentId = null
                                            if (result.statusCode == 401) {
                                                expireSession()
                                            } else {
                                                commentsError = result.message
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onAuthorClick = { username ->
                            if (username == currentUser?.username) {
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
                                            loadFeed()
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
                        postsCount = user?.postsCount ?: 0,
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
