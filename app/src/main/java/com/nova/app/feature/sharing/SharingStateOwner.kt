package com.nova.app.feature.sharing

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.sharing.data.SharingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


sealed interface SharingTarget {
    data class Post(val id: Long) : SharingTarget
    data class Profile(val username: String) : SharingTarget
    data class Reel(val id: Long) : SharingTarget
}


data class SharingUiState(
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val conversations: List<NovaConversation> = emptyList(),
    val loadingPeople: Boolean = true,
    val loadingConversations: Boolean = true,
    val busyUsername: String? = null,
    val busyConversationId: Long? = null,
    val addingToStory: Boolean = false,
    val sentUsernames: Set<String> = emptySet(),
    val sentConversationIds: Set<Long> = emptySet(),
    val message: String? = null,
    val error: String? = null,
) {
    val busy: Boolean
        get() = busyUsername != null || busyConversationId != null || addingToStory
}


/** Owns share-dialog async/search/send state; rendering and Android external share stay in UI. */
class SharingStateOwner(
    val target: SharingTarget,
    private val messagesRepository: MessagesRepository,
    private val peopleRepository: PeopleRepository,
    private val sharingRepository: SharingRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(SharingUiState())
        private set

    private var searchJob: Job? = null

    val canAddToStory: Boolean
        get() = target is SharingTarget.Post || target is SharingTarget.Reel

    fun start() {
        scheduleSearch()
    }

    fun setQuery(value: String) {
        state = state.copy(
            query = value.take(QUERY_MAX_LENGTH),
            error = null,
        )
        scheduleSearch()
    }

    fun clearError() {
        state = state.copy(error = null)
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            searchNow()
        }
    }

    internal suspend fun searchNow() {
        val query = state.query.trim()
        state = state.copy(
            loadingPeople = true,
            loadingConversations = true,
            error = null,
        )

        when (val result = messagesRepository.conversations(query)) {
            is ApiResult.Success -> state = state.copy(conversations = result.value.conversations)
            is ApiResult.Failure -> state = state.copy(
                conversations = emptyList(),
                error = result.message,
            )
        }
        state = state.copy(loadingConversations = false)

        when (val result = peopleRepository.people(query)) {
            is ApiResult.Success -> {
                val directUsernames = state.conversations
                    .filterNot { it.isGroup }
                    .mapTo(mutableSetOf()) { it.otherUser.username.lowercase() }
                state = state.copy(
                    people = result.value.filterNot { it.username.lowercase() in directUsernames },
                )
            }

            is ApiResult.Failure -> state = state.copy(
                people = emptyList(),
                error = state.error ?: result.message,
            )
        }
        state = state.copy(loadingPeople = false)
    }

    fun sendToPerson(person: NovaPerson) {
        scope.launch { sendToPersonNow(person) }
    }

    internal suspend fun sendToPersonNow(person: NovaPerson) {
        if (state.busy) return
        state = state.copy(
            busyUsername = person.username,
            error = null,
            message = null,
        )
        when (val result = shareToPerson(person.username)) {
            is ApiResult.Success -> state = state.copy(
                message = "Sent to @${person.username}",
                sentUsernames = state.sentUsernames + person.username.lowercase(),
            )
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(busyUsername = null)
    }

    fun sendToConversation(conversation: NovaConversation) {
        scope.launch { sendToConversationNow(conversation) }
    }

    internal suspend fun sendToConversationNow(conversation: NovaConversation) {
        if (state.busy) return
        state = state.copy(
            busyConversationId = conversation.id,
            error = null,
            message = null,
        )
        val result = if (conversation.isGroup) {
            shareToGroup(conversation.id)
        } else {
            shareToPerson(conversation.otherUser.username)
        }
        when (result) {
            is ApiResult.Success -> state = state.copy(
                message = "Sent to ${conversation.displayName}",
                sentConversationIds = state.sentConversationIds + conversation.id,
            )
            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(busyConversationId = null)
    }

    fun addToStory(audience: String) {
        scope.launch { addToStoryNow(audience) }
    }

    internal suspend fun addToStoryNow(audience: String) {
        if (!canAddToStory || state.busy) return
        state = state.copy(
            addingToStory = true,
            error = null,
            message = null,
        )
        val result = when (val currentTarget = target) {
            is SharingTarget.Post -> sharingRepository.addPostToStory(currentTarget.id, audience = audience)
            is SharingTarget.Reel -> sharingRepository.addReelToStory(currentTarget.id, audience = audience)
            is SharingTarget.Profile -> ApiResult.Failure("That content can't be added to a Story.")
        }
        when (result) {
            is ApiResult.Success -> state = state.copy(
                message = if (audience == "close_friends") {
                    "Added to your Close Friends Story"
                } else {
                    "Added to your Story"
                },
            )

            is ApiResult.Failure -> state = state.copy(error = result.message)
        }
        state = state.copy(addingToStory = false)
    }

    private suspend fun shareToPerson(username: String): ApiResult<Unit> = when (val currentTarget = target) {
        is SharingTarget.Post -> sharingRepository.sharePost(username, currentTarget.id)
        is SharingTarget.Reel -> sharingRepository.shareReel(username, currentTarget.id)
        is SharingTarget.Profile -> sharingRepository.shareProfile(username, currentTarget.username)
    }

    private suspend fun shareToGroup(conversationId: Long): ApiResult<Unit> = when (val currentTarget = target) {
        is SharingTarget.Post -> sharingRepository.sharePostToConversation(conversationId, currentTarget.id)
        is SharingTarget.Reel -> sharingRepository.shareReelToConversation(conversationId, currentTarget.id)
        is SharingTarget.Profile -> sharingRepository.shareProfileToConversation(conversationId, currentTarget.username)
    }

    companion object {
        internal const val SEARCH_DEBOUNCE_MS = 220L
        internal const val QUERY_MAX_LENGTH = 60
    }
}
