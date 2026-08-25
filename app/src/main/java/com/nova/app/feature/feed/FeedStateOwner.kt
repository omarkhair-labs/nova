package com.nova.app.feature.feed

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class FeedUiState(
    val posts: List<NovaPost> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: String? = null,
    val errorMessage: String? = null,
    val deletingPostId: Long? = null,
    val likingPostIds: Set<Long> = emptySet(),
    val repostingPostIds: Set<Long> = emptySet(),
    val isUploadingPost: Boolean = false,
    val postErrorMessage: String? = null,
    val actionErrorPostId: Long? = null,
    val actionErrorMessage: String? = null,
    val contentVersion: Int = 0,
    val sessionExpiryVersion: Int = 0,
    val profileRefreshVersion: Int = 0,
    val postCreatedVersion: Int = 0,
) {
    val hasMore: Boolean
        get() = nextCursor != null
}


/** Lifecycle-scoped owner for feed paging and mutations shared across social-content routes. */
class FeedStateOwner(
    private val feedRepository: FeedRepository,
    private val postRepository: PostRepository,
    private val repostRepository: PostRepostRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(FeedUiState())
        private set

    fun reset() {
        state = FeedUiState(
            contentVersion = state.contentVersion + 1,
            sessionExpiryVersion = state.sessionExpiryVersion,
            profileRefreshVersion = state.profileRefreshVersion,
            postCreatedVersion = state.postCreatedVersion,
        )
    }

    fun clearPostError() {
        state = state.copy(postErrorMessage = null)
    }

    fun loadFeed() {
        if (state.isLoading || state.isLoadingMore) return
        scope.launch { loadFeedNow() }
    }

    internal suspend fun loadFeedNow() {
        if (state.isLoading || state.isLoadingMore) return
        state = state.copy(
            isLoading = true,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        when (val result = feedRepository.feed()) {
            is ApiResult.Success -> {
                state = state.copy(
                    posts = result.value.posts,
                    nextCursor = result.value.nextCursor,
                    isLoading = false,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        isLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isLoading = false, errorMessage = result.message)
                }
            }
        }
    }

    fun loadMore() {
        val cursor = state.nextCursor ?: return
        if (state.isLoading || state.isLoadingMore) return
        scope.launch { loadMoreNow(cursor) }
    }

    internal suspend fun loadMoreNow(cursor: String) {
        if (state.isLoading || state.isLoadingMore) return
        state = state.copy(isLoadingMore = true, errorMessage = null)
        when (val result = feedRepository.feed(cursor)) {
            is ApiResult.Success -> {
                state = state.copy(
                    posts = mergeFeedPage(state.posts, result.value.posts),
                    nextCursor = result.value.nextCursor,
                    isLoadingMore = false,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        isLoadingMore = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isLoadingMore = false, errorMessage = result.message)
                }
            }
        }
    }

    fun deletePost(post: NovaPost) {
        if (state.deletingPostId != null || !post.isMine) return
        scope.launch { deletePostNow(post) }
    }

    internal suspend fun deletePostNow(post: NovaPost) {
        if (state.deletingPostId != null || !post.isMine) return
        state = state.copy(deletingPostId = post.id, errorMessage = null)
        when (val result = postRepository.deletePost(post.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    posts = state.posts.filterNot { it.id == post.id },
                    deletingPostId = null,
                    contentVersion = state.contentVersion + 1,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        deletingPostId = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(deletingPostId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun toggleLike(post: NovaPost) {
        if (post.id in state.likingPostIds) return
        scope.launch { toggleLikeNow(post) }
    }

    internal suspend fun toggleLikeNow(post: NovaPost) {
        if (post.id in state.likingPostIds) return
        val optimistic = post.copy(
            isLiked = !post.isLiked,
            likesCount = (post.likesCount + if (post.isLiked) -1 else 1).coerceAtLeast(0),
        )
        state = state.copy(
            posts = replacePost(state.posts, optimistic),
            likingPostIds = state.likingPostIds + post.id,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        when (
            val result = postRepository.setLiked(
                postId = post.id,
                liked = optimistic.isLiked,
            )
        ) {
            is ApiResult.Success -> {
                state = state.copy(
                    posts = replacePost(state.posts, result.value),
                    likingPostIds = state.likingPostIds - post.id,
                    contentVersion = state.contentVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                val rolledBack = replacePost(state.posts, post)
                state = if (result.statusCode == 401) {
                    state.copy(
                        posts = rolledBack,
                        likingPostIds = state.likingPostIds - post.id,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        posts = rolledBack,
                        likingPostIds = state.likingPostIds - post.id,
                        actionErrorPostId = post.id,
                        actionErrorMessage = "Like wasn't saved. ${result.message}",
                    )
                }
            }
        }
    }

    fun toggleRepost(post: NovaPost) {
        if (post.id in state.repostingPostIds) return
        scope.launch { toggleRepostNow(post) }
    }

    internal suspend fun toggleRepostNow(post: NovaPost) {
        if (post.id in state.repostingPostIds) return
        val optimistic = post.copy(
            isReposted = !post.isReposted,
            repostsCount = (post.repostsCount + if (post.isReposted) -1 else 1).coerceAtLeast(0),
        )
        state = state.copy(
            posts = replacePost(state.posts, optimistic),
            repostingPostIds = state.repostingPostIds + post.id,
            errorMessage = null,
            actionErrorPostId = null,
            actionErrorMessage = null,
        )
        when (val result = repostRepository.setPostReposted(post.id, optimistic.isReposted)) {
            is ApiResult.Success -> {
                val updated = optimistic.copy(
                    repostsCount = result.value.repostsCount,
                    isReposted = result.value.isReposted,
                    repostedBy = result.value.repostedBy,
                )
                state = state.copy(
                    posts = if (result.value.stillInFeed) {
                        replacePost(state.posts, updated)
                    } else {
                        state.posts.filterNot { it.id == post.id }
                    },
                    repostingPostIds = state.repostingPostIds - post.id,
                    contentVersion = state.contentVersion + 1,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                )
            }
            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        posts = replacePost(state.posts, post),
                        repostingPostIds = state.repostingPostIds - post.id,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        posts = replacePost(state.posts, post),
                        repostingPostIds = state.repostingPostIds - post.id,
                        actionErrorPostId = post.id,
                        actionErrorMessage = "Repost wasn't saved. ${result.message}",
                    )
                }
            }
        }
    }

    fun createPost(caption: String, imageUri: Uri) {
        if (state.isUploadingPost) return
        scope.launch { createPostNow(caption, imageUri) }
    }

    internal suspend fun createPostNow(caption: String, imageUri: Uri) {
        if (state.isUploadingPost) return
        state = state.copy(isUploadingPost = true, postErrorMessage = null)
        when (val result = postRepository.createPost(caption, imageUri)) {
            is ApiResult.Success -> {
                state = state.copy(
                    posts = listOf(result.value) + state.posts.filterNot { it.id == result.value.id },
                    isUploadingPost = false,
                    contentVersion = state.contentVersion + 1,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                    postCreatedVersion = state.postCreatedVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        isUploadingPost = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isUploadingPost = false, postErrorMessage = result.message)
                }
            }
        }
    }

    fun synchronizePost(updated: NovaPost) {
        state = state.copy(posts = replacePost(state.posts, updated))
    }

    fun removePost(postId: Long) {
        state = state.copy(
            posts = state.posts.filterNot { it.id == postId },
            contentVersion = state.contentVersion + 1,
        )
    }

    fun removePostsByAuthor(authorId: Long) {
        state = state.copy(
            posts = state.posts.filterNot { it.author.id == authorId },
            contentVersion = state.contentVersion + 1,
        )
    }

    fun markContentChanged() {
        state = state.copy(contentVersion = state.contentVersion + 1)
    }
}


internal fun replacePost(posts: List<NovaPost>, updated: NovaPost): List<NovaPost> =
    posts.map { existing -> if (existing.id == updated.id) updated else existing }
