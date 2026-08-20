package com.nova.app.feature.stories

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.feature.stories.domain.model.NovaStory
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import com.nova.app.feature.stories.domain.model.NovaStoryViewer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class StoryViewerUiState(
    val stories: List<NovaStory>,
    val index: Int,
    val replyBody: String = "",
    val mutationBusy: Boolean = false,
    val message: String? = null,
    val viewersVisible: Boolean = false,
    val viewersLoading: Boolean = false,
    val viewers: List<NovaStoryViewer> = emptyList(),
    val viewersError: String? = null,
    val sessionExpiryVersion: Int = 0,
    val finishedVersion: Int = 0,
    val deletedVersion: Int = 0,
) {
    val currentStory: NovaStory? get() = stories.getOrNull(index)
}


/** Owns Story viewer mutation/viewer-list state; playback and frame progress stay in UI. */
class StoryViewerStateOwner(
    initialGroup: NovaStoryGroup,
    private val repository: StoriesRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(initialState(initialGroup))
        private set

    fun enterCurrentStory() {
        val story = state.currentStory ?: return
        state = state.copy(message = null)
        if (!story.isMine && !story.isViewed) {
            scope.launch { markViewedNow(story.id) }
        }
    }

    internal suspend fun markViewedNow(storyId: Long) {
        when (val result = repository.markViewed(storyId)) {
            is ApiResult.Success -> {
                state = state.copy(
                    stories = state.stories.map { story ->
                        if (story.id == storyId) story.copy(isViewed = true) else story
                    },
                )
            }

            is ApiResult.Failure -> {
                // Preserve legacy behavior: only terminal auth failure was surfaced here.
                if (result.statusCode == 401) recordSessionExpiry()
            }
        }
    }

    fun advance() {
        state = if (state.index < state.stories.lastIndex) {
            state.copy(index = state.index + 1)
        } else {
            state.copy(finishedVersion = state.finishedVersion + 1)
        }
    }

    fun previous() {
        if (state.index > 0) {
            state = state.copy(index = state.index - 1)
        }
    }

    fun setReplyBody(value: String) {
        state = state.copy(replyBody = value.take(1000))
    }

    fun toggleReaction(emoji: String) {
        if (state.mutationBusy) return
        val story = state.currentStory ?: return
        scope.launch { toggleReactionNow(story.id, story.myReaction, emoji) }
    }

    internal suspend fun toggleReactionNow(
        storyId: Long,
        currentReaction: String,
        emoji: String,
    ) {
        if (state.mutationBusy) return
        state = state.copy(mutationBusy = true)
        val result = if (currentReaction == emoji) {
            repository.removeReaction(storyId)
        } else {
            repository.react(storyId, emoji)
        }

        when (result) {
            is ApiResult.Success -> {
                val nextReaction = if (currentReaction == emoji) "" else emoji
                state = state.copy(
                    stories = state.stories.map { story ->
                        if (story.id == storyId) story.copy(myReaction = nextReaction) else story
                    },
                )
            }

            is ApiResult.Failure -> recordMutationFailure(result)
        }
        state = state.copy(mutationBusy = false)
    }

    fun sendReply() {
        if (state.mutationBusy || state.replyBody.isBlank()) return
        val story = state.currentStory ?: return
        val body = state.replyBody
        scope.launch { sendReplyNow(story.id, body) }
    }

    internal suspend fun sendReplyNow(storyId: Long, body: String) {
        if (state.mutationBusy || body.isBlank()) return
        state = state.copy(mutationBusy = true)
        when (val result = repository.reply(storyId, body)) {
            is ApiResult.Success -> {
                state = state.copy(
                    replyBody = "",
                    message = "Reply sent to Messages.",
                )
            }

            is ApiResult.Failure -> recordMutationFailure(result)
        }
        state = state.copy(mutationBusy = false)
    }

    fun deleteCurrentStory() {
        if (state.mutationBusy) return
        val story = state.currentStory ?: return
        scope.launch { deleteStoryNow(story.id) }
    }

    internal suspend fun deleteStoryNow(storyId: Long) {
        if (state.mutationBusy) return
        state = state.copy(mutationBusy = true)
        when (val result = repository.deleteStory(storyId)) {
            is ApiResult.Success -> {
                state = state.copy(deletedVersion = state.deletedVersion + 1)
            }

            is ApiResult.Failure -> recordMutationFailure(result)
        }
        state = state.copy(mutationBusy = false)
    }

    fun openViewers() {
        val story = state.currentStory ?: return
        state = state.copy(
            viewersVisible = true,
            viewersLoading = true,
            viewers = emptyList(),
            viewersError = null,
        )
        scope.launch { loadViewersNow(story.id) }
    }

    fun closeViewers() {
        state = state.copy(viewersVisible = false)
    }

    internal suspend fun loadViewersNow(storyId: Long) {
        when (val result = repository.viewers(storyId)) {
            is ApiResult.Success -> {
                state = state.copy(
                    viewers = result.value,
                    viewersLoading = false,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        viewersLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        viewersLoading = false,
                        viewersError = result.message,
                    )
                }
            }
        }
    }

    private fun recordMutationFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(message = result.message)
        }
    }

    private fun recordSessionExpiry() {
        state = state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
    }
}


private fun initialState(group: NovaStoryGroup): StoryViewerUiState {
    val firstUnseen = group.stories.indexOfFirst { !it.isViewed }.let { if (it < 0) 0 else it }
    return StoryViewerUiState(
        stories = group.stories,
        index = firstUnseen,
    )
}
