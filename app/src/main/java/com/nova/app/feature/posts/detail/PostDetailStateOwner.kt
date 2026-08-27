package com.nova.app.feature.posts.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.data.PostRepostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PostDetailUiState(
    val post: NovaPost? = null,
    val isLoading: Boolean = true,
    val isLiking: Boolean = false,
    val isReposting: Boolean = false,
    val isDeleting: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val contentMutationVersion: Int = 0,
    val deletedVersion: Int = 0,
)


/** Route-scoped owner for post detail loading, likes, and deletion. */
class PostDetailStateOwner(
    private val postId: Long,
    private val repository: PostRepository,
    private val repostRepository: PostRepostRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PostDetailUiState())
        private set

    private var loadGeneration = 0
    private var likeRevision = 0
    private var repostRevision = 0

    fun load() {
        scope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        loadGeneration += 1
        val requestGeneration = loadGeneration
        val requestLikeRevision = likeRevision
        val requestRepostRevision = repostRevision
        state = state.copy(isLoading = true, errorMessage = null)
        when (val result = repository.post(postId)) {
            is ApiResult.Success -> {
                if (requestGeneration != loadGeneration) return
                val localPost = state.post
                var loadedPost = result.value
                if (localPost != null && (state.isLiking || likeRevision > requestLikeRevision)) {
                    loadedPost = loadedPost.copy(
                        isLiked = localPost.isLiked,
                        likesCount = localPost.likesCount,
                    )
                }
                if (localPost != null && (state.isReposting || repostRevision > requestRepostRevision)) {
                    loadedPost = loadedPost.copy(
                        isReposted = localPost.isReposted,
                        repostsCount = localPost.repostsCount,
                        repostedBy = localPost.repostedBy,
                    )
                }
                state = state.copy(
                    post = loadedPost,
                    isLoading = false,
                    errorMessage = null,
                )
            }

            is ApiResult.Failure -> {
                if (requestGeneration != loadGeneration) return
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

    fun toggleLike(post: NovaPost) {
        if (state.isLiking) return
        scope.launch { toggleLikeNow(post) }
    }

    internal suspend fun toggleLikeNow(post: NovaPost) {
        if (state.isLiking) return
        val current = state.post?.takeIf { it.id == post.id } ?: return
        val previousIsLiked = current.isLiked
        val previousLikesCount = current.likesCount
        val optimisticIsLiked = !current.isLiked
        val optimistic = current.copy(
            isLiked = optimisticIsLiked,
            likesCount = (
                current.likesCount + if (current.isLiked) -1 else 1
            ).coerceAtLeast(0),
        )
        state = state.copy(post = optimistic, isLiking = true, errorMessage = null)
        likeRevision += 1
        when (
            val result = repository.setLiked(
                postId = post.id,
                liked = optimisticIsLiked,
            )
        ) {
            is ApiResult.Success -> {
                likeRevision += 1
                state = state.copy(
                    post = state.post?.copy(
                        isLiked = result.value.isLiked,
                        likesCount = result.value.likesCount,
                    ),
                    isLiking = false,
                    contentMutationVersion = state.contentMutationVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                likeRevision += 1
                val rolledBack = state.post?.copy(
                    isLiked = previousIsLiked,
                    likesCount = previousLikesCount,
                )
                state = if (result.statusCode == 401) {
                    state.copy(
                        post = rolledBack,
                        isLiking = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(post = rolledBack, isLiking = false, errorMessage = result.message)
                }
            }
        }
    }

    fun toggleRepost(post: NovaPost) {
        if (state.isReposting) return
        scope.launch { toggleRepostNow(post) }
    }

    internal suspend fun toggleRepostNow(post: NovaPost) {
        if (state.isReposting) return
        val current = state.post?.takeIf { it.id == post.id } ?: return
        val previousIsReposted = current.isReposted
        val previousRepostsCount = current.repostsCount
        val previousRepostedBy = current.repostedBy
        val optimisticIsReposted = !current.isReposted
        val optimistic = current.copy(
            isReposted = optimisticIsReposted,
            repostsCount = (
                current.repostsCount + if (current.isReposted) -1 else 1
            ).coerceAtLeast(0),
        )
        state = state.copy(post = optimistic, isReposting = true, errorMessage = null)
        repostRevision += 1
        when (val result = repostRepository.setPostReposted(post.id, optimisticIsReposted)) {
            is ApiResult.Success -> {
                repostRevision += 1
                state = state.copy(
                    post = state.post?.copy(
                        repostsCount = result.value.repostsCount,
                        isReposted = result.value.isReposted,
                        repostedBy = result.value.repostedBy,
                    ),
                    isReposting = false,
                    contentMutationVersion = state.contentMutationVersion + 1,
                )
            }
            is ApiResult.Failure -> {
                repostRevision += 1
                val rolledBack = state.post?.copy(
                    isReposted = previousIsReposted,
                    repostsCount = previousRepostsCount,
                    repostedBy = previousRepostedBy,
                )
                state = if (result.statusCode == 401) {
                    state.copy(
                        post = rolledBack,
                        isReposting = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(post = rolledBack, isReposting = false, errorMessage = result.message)
                }
            }
        }
    }

    fun delete(post: NovaPost) {
        if (state.isDeleting || !post.isMine) return
        scope.launch { deleteNow(post) }
    }

    internal suspend fun deleteNow(post: NovaPost) {
        if (state.isDeleting || !post.isMine) return
        state = state.copy(isDeleting = true, errorMessage = null)
        when (val result = repository.deletePost(post.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    isDeleting = false,
                    deletedVersion = state.deletedVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        isDeleting = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isDeleting = false, errorMessage = result.message)
                }
            }
        }
    }
}
