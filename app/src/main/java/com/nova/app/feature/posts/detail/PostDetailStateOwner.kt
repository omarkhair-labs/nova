package com.nova.app.feature.posts.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPost
import com.nova.app.feature.posts.data.PostRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PostDetailUiState(
    val post: NovaPost? = null,
    val isLoading: Boolean = true,
    val isLiking: Boolean = false,
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
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PostDetailUiState())
        private set

    fun load() {
        scope.launch { loadNow() }
    }

    internal suspend fun loadNow() {
        state = state.copy(isLoading = true, errorMessage = null)
        when (val result = repository.post(postId)) {
            is ApiResult.Success -> {
                state = state.copy(
                    post = result.value,
                    isLoading = false,
                    errorMessage = null,
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

    fun toggleLike(post: NovaPost) {
        if (state.isLiking) return
        scope.launch { toggleLikeNow(post) }
    }

    internal suspend fun toggleLikeNow(post: NovaPost) {
        if (state.isLiking) return
        state = state.copy(isLiking = true, errorMessage = null)
        when (
            val result = repository.setLiked(
                postId = post.id,
                liked = !post.isLiked,
            )
        ) {
            is ApiResult.Success -> {
                state = state.copy(
                    post = result.value,
                    isLiking = false,
                    contentMutationVersion = state.contentMutationVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        isLiking = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(isLiking = false, errorMessage = result.message)
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
