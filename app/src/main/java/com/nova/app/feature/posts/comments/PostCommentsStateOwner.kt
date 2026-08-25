package com.nova.app.feature.posts.comments

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PostCommentsUiState(
    val post: NovaPost? = null,
    val comments: List<NovaComment> = emptyList(),
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val deletingCommentId: Long? = null,
    val isReplySending: Boolean = false,
    val deletingReplyId: Long? = null,
    val likingCommentIds: Set<Long> = emptySet(),
    val errorMessage: String? = null,
    val replyErrorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val contentMutationVersion: Int = 0,
)


/** Route-scoped owner for post/comment transport and mutation state. */
class PostCommentsStateOwner(
    private val postId: Long,
    initialPost: NovaPost?,
    private val currentUser: NovaPostAuthor,
    private val repository: PostRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PostCommentsUiState(post = initialPost))
        private set
    private var nextOptimisticId = -1L

    fun load() {
        scope.launch { loadNow() }
    }

    fun clearReplyError() {
        state = state.copy(replyErrorMessage = null)
    }

    internal suspend fun loadNow() {
        state = state.copy(isLoading = true, errorMessage = null)

        when (val postResult = repository.post(postId)) {
            is ApiResult.Success -> state = state.copy(post = postResult.value)
            is ApiResult.Failure -> {
                if (postResult.statusCode == 401) {
                    state = state.copy(
                        isLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                    return
                }
                state = state.copy(errorMessage = postResult.message)
            }
        }

        when (val result = repository.comments(postId)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = result.value,
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

    fun sendComment(body: String) {
        if (state.isSending) return
        scope.launch { sendCommentNow(body) }
    }

    internal suspend fun sendCommentNow(body: String) {
        if (state.isSending) return
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) return
        val originalPost = state.post
        val optimistic = optimisticComment(cleanBody)
        val optimisticPost = state.post?.let {
            it.copy(commentsCount = it.commentsCount + 1)
        }
        state = state.copy(
            comments = state.comments + optimistic,
            post = optimisticPost,
            isSending = true,
            errorMessage = null,
        )
        when (val result = repository.addComment(postId = postId, body = cleanBody)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = state.comments.map {
                        if (it.id == optimistic.id) result.value.comment else it
                    },
                    post = result.value.post,
                    isSending = false,
                    contentMutationVersion = state.contentMutationVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                val rolledBack = state.comments.filterNot { it.id == optimistic.id }
                state = if (result.statusCode == 401) {
                    state.copy(
                        comments = rolledBack,
                        post = originalPost,
                        isSending = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        comments = rolledBack,
                        post = originalPost,
                        isSending = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun deleteComment(comment: NovaComment) {
        if (state.deletingCommentId != null || !comment.isMine) return
        scope.launch { deleteCommentNow(comment) }
    }

    internal suspend fun deleteCommentNow(comment: NovaComment) {
        if (state.deletingCommentId != null || !comment.isMine) return
        state = state.copy(deletingCommentId = comment.id, errorMessage = null)
        when (val result = repository.deleteComment(comment.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = state.comments.filterNot { it.id == comment.id },
                    post = result.value,
                    deletingCommentId = null,
                    contentMutationVersion = state.contentMutationVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        deletingCommentId = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(deletingCommentId = null, errorMessage = result.message)
                }
            }
        }
    }

    fun sendReply(parent: NovaComment, body: String) {
        if (state.isReplySending) return
        scope.launch { sendReplyNow(parent, body) }
    }

    internal suspend fun sendReplyNow(parent: NovaComment, body: String) {
        if (state.isReplySending) return
        val cleanBody = body.trim()
        if (cleanBody.isBlank()) return
        val optimistic = optimisticComment(cleanBody, parent.id)
        state = state.copy(
            comments = appendReply(state.comments, optimistic),
            isReplySending = true,
            replyErrorMessage = null,
        )
        when (
            val result = repository.addComment(
                postId = postId,
                body = cleanBody,
                parentId = parent.id,
            )
        ) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = replaceReply(state.comments, optimistic.id, result.value.comment),
                    isReplySending = false,
                )
            }

            is ApiResult.Failure -> {
                // Preserve legacy reply behavior: surface the error locally instead of expiring the session.
                state = state.copy(
                    comments = removeReply(state.comments, optimistic),
                    isReplySending = false,
                    replyErrorMessage = result.message,
                )
            }
        }
    }

    fun deleteReply(reply: NovaComment) {
        if (state.deletingReplyId != null || !reply.isMine) return
        scope.launch { deleteReplyNow(reply) }
    }

    fun toggleLike(comment: NovaComment) {
        if (comment.id in state.likingCommentIds || comment.id < 0L) return
        scope.launch {
            val optimistic = comment.copy(
                isLiked = !comment.isLiked,
                likesCount = (comment.likesCount + if (comment.isLiked) -1 else 1).coerceAtLeast(0),
            )
            state = state.copy(
                comments = replaceComment(state.comments, optimistic),
                likingCommentIds = state.likingCommentIds + comment.id,
                replyErrorMessage = null,
            )
            when (
                val result = repository.setCommentLiked(
                    commentId = comment.id,
                    liked = optimistic.isLiked,
                    isReply = comment.parentId != null,
                )
            ) {
                is ApiResult.Success -> {
                    val updated = result.value
                    state = state.copy(comments = replaceComment(state.comments, updated))
                }
                is ApiResult.Failure -> state = state.copy(
                    comments = replaceComment(state.comments, comment),
                    replyErrorMessage = "Reaction wasn't saved. ${result.message}",
                )
            }
            state = state.copy(likingCommentIds = state.likingCommentIds - comment.id)
        }
    }

    private fun optimisticComment(body: String, parentId: Long? = null) = NovaComment(
        id = nextOptimisticId--,
        author = currentUser,
        body = body,
        createdAt = "",
        isMine = true,
        parentId = parentId,
    )

    internal suspend fun deleteReplyNow(reply: NovaComment) {
        if (state.deletingReplyId != null || !reply.isMine) return
        state = state.copy(deletingReplyId = reply.id, replyErrorMessage = null)
        when (val result = repository.deleteCommentReply(reply.id)) {
            is ApiResult.Success -> {
                // Preserve legacy reply behavior: ignore the returned post and only update the reply list.
                state = state.copy(
                    comments = removeReply(state.comments, reply),
                    deletingReplyId = null,
                )
            }

            is ApiResult.Failure -> {
                state = state.copy(
                    deletingReplyId = null,
                    replyErrorMessage = result.message,
                )
            }
        }
    }
}


internal fun replaceComment(comments: List<NovaComment>, updated: NovaComment): List<NovaComment> =
    if (updated.parentId == null) {
        comments.map { if (it.id == updated.id) updated.copy(replies = it.replies) else it }
    } else {
        comments.map { parent ->
            parent.copy(replies = parent.replies.map { if (it.id == updated.id) updated else it })
        }
    }


internal fun replaceReply(
    comments: List<NovaComment>,
    optimisticId: Long,
    persisted: NovaComment,
): List<NovaComment> = comments.map { parent ->
    parent.copy(
        replies = parent.replies.map { if (it.id == optimisticId) persisted else it },
    )
}


internal fun appendReply(comments: List<NovaComment>, reply: NovaComment): List<NovaComment> {
    val parentId = reply.parentId ?: return comments
    return comments.map { parent ->
        if (parent.id != parentId) {
            parent
        } else {
            val existing = parent.replies.filterNot { it.id == reply.id }
            parent.copy(
                replies = existing + reply,
                repliesCount = existing.size + 1,
            )
        }
    }
}


internal fun removeReply(comments: List<NovaComment>, reply: NovaComment): List<NovaComment> {
    val parentId = reply.parentId ?: return comments
    return comments.map { parent ->
        if (parent.id != parentId) {
            parent
        } else {
            val remaining = parent.replies.filterNot { it.id == reply.id }
            parent.copy(
                replies = remaining,
                repliesCount = remaining.size,
            )
        }
    }
}
