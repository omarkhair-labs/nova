package com.nova.app.feature.profile

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


enum class ProfileContentTab {
    Posts,
    Reels,
    Reposts,
}


data class ProfileContentUiState(
    val selectedTab: ProfileContentTab = ProfileContentTab.Posts,
    val posts: List<NovaPost> = emptyList(),
    val postsLoading: Boolean = true,
    val postsError: String? = null,
    val postsPagingError: String? = null,
    val postsNextCursor: String? = null,
    val postsLoadingMore: Boolean = false,
    val reposts: List<NovaPost> = emptyList(),
    val repostsLoading: Boolean = true,
    val repostsError: String? = null,
    val repostsNextCursor: String? = null,
    val repostsLoadingMore: Boolean = false,
    val sessionExpiryVersion: Int = 0,
)


/** Owns authored-post and reposted-post state shared by self and person profile surfaces. */
class ProfileContentStateOwner(
    private val username: String,
    private val postRepository: PostRepository,
    private val pagingRepository: PeoplePagingRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(ProfileContentUiState())
        private set

    private var firstPagePosts: List<NovaPost> = emptyList()
    private var hasLoadedOlderPosts = false

    fun loadPosts() {
        if (username.isBlank()) return
        scope.launch { loadPostsNow() }
    }

    internal suspend fun loadPostsNow() {
        if (username.isBlank()) return
        state = state.copy(postsLoading = true, postsError = null)

        when (val result = postRepository.personPosts(username)) {
            is ApiResult.Success -> synchronizePosts(
                posts = result.value,
                isLoading = false,
                errorMessage = null,
            )

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        postsLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(postsLoading = false, postsError = result.message)
                }
            }
        }
    }

    /** Mirrors an externally owned first page, used by PersonStateOwner without duplicating its request. */
    fun synchronizeExternalPosts(
        posts: List<NovaPost>,
        isLoading: Boolean,
        errorMessage: String?,
    ) {
        synchronizePosts(posts, isLoading, errorMessage)
    }

    private fun synchronizePosts(
        posts: List<NovaPost>,
        isLoading: Boolean,
        errorMessage: String?,
    ) {
        firstPagePosts = posts
        state = if (state.selectedTab == ProfileContentTab.Posts) {
            if (!hasLoadedOlderPosts) {
                state.copy(
                    posts = firstPagePosts,
                    postsLoading = isLoading,
                    postsError = errorMessage,
                    postsNextCursor = initialPostsCursor(firstPagePosts),
                )
            } else {
                val freshIds = firstPagePosts.mapTo(mutableSetOf()) { it.id }
                state.copy(
                    posts = firstPagePosts + state.posts.filterNot { it.id in freshIds },
                    postsLoading = isLoading,
                    postsError = errorMessage,
                )
            }
        } else {
            state.copy(postsLoading = isLoading, postsError = errorMessage)
        }
    }

    fun selectTab(tab: ProfileContentTab) {
        if (tab == state.selectedTab) return

        state = state.copy(selectedTab = tab)
        when (tab) {
            ProfileContentTab.Posts -> resetPostsPaging()
            ProfileContentTab.Reels -> Unit
            ProfileContentTab.Reposts -> {
                state = state.copy(
                    reposts = emptyList(),
                    repostsLoading = true,
                    repostsError = null,
                    repostsNextCursor = null,
                    repostsLoadingMore = false,
                )
                scope.launch { loadRepostsNow() }
            }
        }
    }

    private fun resetPostsPaging() {
        hasLoadedOlderPosts = false
        state = state.copy(
            posts = firstPagePosts,
            postsPagingError = null,
            postsNextCursor = initialPostsCursor(firstPagePosts),
            postsLoadingMore = false,
        )
    }

    fun loadMorePosts() {
        val cursor = state.postsNextCursor ?: return
        if (state.postsLoadingMore || username.isBlank()) return
        scope.launch { loadMorePostsNow(cursor) }
    }

    internal suspend fun loadMorePostsNow(cursor: String) {
        if (state.postsLoadingMore || username.isBlank()) return
        state = state.copy(postsLoadingMore = true, postsPagingError = null)

        when (val result = pagingRepository.profilePosts(username, cursor)) {
            is ApiResult.Success -> {
                val existingIds = state.posts.mapTo(mutableSetOf()) { it.id }
                state = state.copy(
                    posts = state.posts + result.value.posts.filterNot { it.id in existingIds },
                    postsNextCursor = result.value.nextCursor,
                    postsLoadingMore = false,
                )
                hasLoadedOlderPosts = true
            }

            is ApiResult.Failure -> {
                // Preserve the old profile-tab behavior: paging errors, including 401, stay inline.
                state = state.copy(
                    postsLoadingMore = false,
                    postsPagingError = result.message,
                )
            }
        }
    }

    fun retryReposts() {
        if (username.isBlank()) return
        scope.launch { loadRepostsNow() }
    }

    internal suspend fun loadRepostsNow() {
        if (username.isBlank()) {
            state = state.copy(repostsLoading = false)
            return
        }

        state = state.copy(repostsLoading = true, repostsError = null)
        when (val result = pagingRepository.profileReposts(username)) {
            is ApiResult.Success -> {
                state = state.copy(
                    reposts = result.value.posts,
                    repostsNextCursor = result.value.nextCursor,
                    repostsLoading = false,
                )
            }

            is ApiResult.Failure -> {
                // Preserve the old profile-tab behavior: even terminal HTTP failures stayed local here.
                state = state.copy(
                    repostsLoading = false,
                    repostsError = result.message,
                )
            }
        }
    }

    fun loadMoreReposts() {
        val cursor = state.repostsNextCursor ?: return
        if (state.repostsLoadingMore || username.isBlank()) return
        scope.launch { loadMoreRepostsNow(cursor) }
    }

    internal suspend fun loadMoreRepostsNow(cursor: String) {
        if (state.repostsLoadingMore || username.isBlank()) return
        state = state.copy(repostsLoadingMore = true, repostsError = null)

        when (val result = pagingRepository.profileReposts(username, cursor)) {
            is ApiResult.Success -> {
                val existingIds = state.reposts.mapTo(mutableSetOf()) { it.id }
                state = state.copy(
                    reposts = state.reposts + result.value.posts.filterNot { it.id in existingIds },
                    repostsNextCursor = result.value.nextCursor,
                    repostsLoadingMore = false,
                )
            }

            is ApiResult.Failure -> {
                state = state.copy(
                    repostsLoadingMore = false,
                    repostsError = result.message,
                )
            }
        }
    }
}


private const val PROFILE_FIRST_PAGE_SIZE = 24

private fun initialPostsCursor(posts: List<NovaPost>): String? =
    posts.takeIf { it.size >= PROFILE_FIRST_PAGE_SIZE }?.lastOrNull()?.id?.toString()
