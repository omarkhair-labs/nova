package com.nova.app.feature.reels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.reels.data.ReelsRepository
import com.nova.app.feature.reels.domain.model.NovaReel
import com.nova.app.feature.reels.domain.model.NovaReelComment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class ReelCommentsUiState(
    val comments: List<NovaReelComment> = emptyList(),
    val loading: Boolean = true,
    val sending: Boolean = false,
    val deletingCommentId: Long? = null,
    val deletingReplyId: Long? = null,
    val body: String = "",
    val replyingTo: NovaReelComment? = null,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
    val reelUpdatedVersion: Int = 0,
    val updatedReel: NovaReel? = null,
)


/** Owns one Reel comments sheet's async/list/composer state; sheet rendering stays in UI. */
class ReelCommentsStateOwner(
    private val reelId: Long,
    private val repository: ReelsRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(ReelCommentsUiState())
        private set

    fun setBody(value: String) {
        state = state.copy(body = value.take(300))
    }

    fun beginReply(parent: NovaReelComment) {
        state = state.copy(replyingTo = parent, error = null)
    }

    fun cancelReply() {
        state = state.copy(replyingTo = null)
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    fun loadComments() {
        scope.launch { loadCommentsNow() }
    }

    internal suspend fun loadCommentsNow() {
        state = state.copy(loading = true, error = null)
        when (val result = repository.comments(reelId)) {
            is ApiResult.Success -> state = state.copy(comments = result.value)
            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(loading = false)
    }

    fun send() {
        scope.launch { sendNow() }
    }

    internal suspend fun sendNow() {
        if (state.sending || state.body.isBlank()) return

        val submittedBody = state.body
        val parentId = state.replyingTo?.id
        state = state.copy(sending = true, error = null)
        when (val result = repository.addComment(reelId, submittedBody, parentId)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = appendComment(state.comments, result.value.comment),
                    body = "",
                    replyingTo = null,
                )
                recordUpdatedReel(result.value.reel)
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(sending = false)
    }

    fun deleteComment(comment: NovaReelComment) {
        scope.launch { deleteCommentNow(comment) }
    }

    internal suspend fun deleteCommentNow(comment: NovaReelComment) {
        if (state.deletingCommentId != null || state.deletingReplyId != null) return

        state = state.copy(deletingCommentId = comment.id, error = null)
        when (val result = repository.deleteComment(comment.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = state.comments.filterNot { it.id == comment.id },
                    replyingTo = state.replyingTo?.takeUnless { it.id == comment.id },
                )
                recordUpdatedReel(result.value)
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(deletingCommentId = null)
    }

    fun deleteReply(reply: NovaReelComment) {
        scope.launch { deleteReplyNow(reply) }
    }

    internal suspend fun deleteReplyNow(reply: NovaReelComment) {
        if (state.deletingCommentId != null || state.deletingReplyId != null) return
        val parentId = reply.parentId ?: return

        state = state.copy(deletingReplyId = reply.id, error = null)
        when (val result = repository.deleteCommentReply(reply.id)) {
            is ApiResult.Success -> {
                state = state.copy(
                    comments = state.comments.map { parent ->
                        if (parent.id != parentId) {
                            parent
                        } else {
                            val remaining = parent.replies.filterNot { it.id == reply.id }
                            parent.copy(replies = remaining, repliesCount = remaining.size)
                        }
                    },
                )
                recordUpdatedReel(result.value)
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(deletingReplyId = null)
    }

    private fun recordUpdatedReel(reel: NovaReel) {
        state = state.copy(
            updatedReel = reel,
            reelUpdatedVersion = state.reelUpdatedVersion + 1,
        )
    }

    private fun recordFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(error = result.message)
        }
    }
}


internal fun appendComment(
    comments: List<NovaReelComment>,
    comment: NovaReelComment,
): List<NovaReelComment> {
    val parentId = comment.parentId
    return if (parentId == null) {
        comments + comment
    } else {
        comments.map { parent ->
            if (parent.id != parentId) {
                parent
            } else {
                val existing = parent.replies.filterNot { it.id == comment.id }
                parent.copy(
                    replies = existing + comment,
                    repliesCount = existing.size + 1,
                )
            }
        }
    }
}
