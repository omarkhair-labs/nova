package com.nova.app.feature.messages.details

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.details.data.ConversationToolsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/** Lifecycle-aware owner for conversation details/search/media/mute orchestration. */
class ConversationDetailsViewModel internal constructor(
    private val conversationId: Long,
    private val repository: ConversationToolsRepository,
    private val workScope: CoroutineScope? = null,
    private val searchDebounceMillis: Long = SEARCH_DEBOUNCE_MS,
) : ViewModel() {
    var state by mutableStateOf(ConversationDetailsUiState())
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope

    private var searchJob: Job? = null
    private var mediaJob: Job? = null
    private var contextJob: Job? = null
    private var muteLoadJob: Job? = null
    private var muteSaveJob: Job? = null
    private var loadedMediaType: String? = null

    fun onDialogOpened(initialTab: ConversationDetailsTab) {
        cancelWork()
        loadedMediaType = null
        state = ConversationDetailsUiState(tab = initialTab)
        muteLoadJob = scope.launch { loadMuted() }
        if (initialTab == ConversationDetailsTab.Media) launchMediaLoadIfNeeded()
    }

    fun onDialogClosed() {
        cancelWork()
    }

    fun onTabSelected(tab: ConversationDetailsTab) {
        if (state.tab == tab) return
        state = state.copy(tab = tab)
        if (tab == ConversationDetailsTab.Media) {
            launchMediaLoadIfNeeded()
        } else {
            mediaJob?.cancel()
            mediaJob = null
        }
    }

    fun onQueryChanged(value: String) {
        val capped = value.take(MAX_QUERY_LENGTH)
        state = state.copy(query = capped)
        searchJob?.cancel()
        searchJob = null

        val clean = capped.trim()
        if (clean.isBlank()) {
            state = state.copy(
                searchResults = emptyList(),
                searchLoading = false,
                searchError = null,
            )
            return
        }

        searchJob = scope.launch {
            delay(searchDebounceMillis)
            state = state.copy(searchLoading = true, searchError = null)
            when (val result = repository.searchMessages(conversationId, clean)) {
                is ApiResult.Success -> state = state.copy(searchResults = result.value)
                is ApiResult.Failure -> handleFailure(
                    result = result,
                    onInlineError = { state = state.copy(searchError = it) },
                )
            }
            state = state.copy(searchLoading = false)
        }
    }

    fun onMediaTypeChanged(type: String) {
        val changed = type != state.mediaType
        state = state.copy(
            mediaType = type,
            mediaItems = emptyList(),
            mediaCursor = null,
        )
        loadedMediaType = null

        // Preserve the current V9 key-based behavior: selecting the already-active
        // chip clears the list but does not relaunch the unchanged LaunchedEffect.
        if (changed && state.tab == ConversationDetailsTab.Media) launchMediaLoadIfNeeded()
    }

    fun onLoadMore() {
        val cursor = state.mediaCursor ?: return
        if (state.mediaLoading) return

        // Preserve the current V9 observable behavior exactly. The legacy callback
        // enters loading state and invalidates the loaded marker, but because the
        // LaunchedEffect keys do not change it does not issue another request.
        state = state.copy(
            mediaLoading = true,
            mediaError = null,
            mediaCursor = cursor,
        )
        loadedMediaType = null
    }

    fun openContext(messageId: Long) {
        if (state.contextTargetId == messageId) return
        state = state.copy(contextTargetId = messageId)
        contextJob?.cancel()
        contextJob = scope.launch { loadContext(messageId) }
    }

    fun closeContext() {
        if (state.contextTargetId == null) return
        contextJob?.cancel()
        contextJob = null
        state = state.copy(
            contextTargetId = null,
            messageContext = null,
            contextError = null,
        )
    }

    fun toggleMute() {
        if (state.muteSaving) return
        state = state.copy(muteSaving = true, muteError = null)
        val desired = !state.muted
        muteSaveJob?.cancel()
        muteSaveJob = scope.launch {
            when (val result = repository.setMuted(conversationId, desired)) {
                is ApiResult.Success -> state = state.copy(muted = result.value)
                is ApiResult.Failure -> handleFailure(
                    result = result,
                    onInlineError = { state = state.copy(muteError = it) },
                )
            }
            state = state.copy(muteSaving = false)
        }
    }

    internal suspend fun loadMuted() {
        when (val result = repository.isMuted(conversationId)) {
            is ApiResult.Success -> state = state.copy(
                muted = result.value,
                muteLoading = false,
            )
            is ApiResult.Failure -> {
                state = state.copy(muteLoading = false)
                handleFailure(
                    result = result,
                    onInlineError = { state = state.copy(muteError = it) },
                )
            }
        }
    }

    internal suspend fun loadMedia() {
        if (state.tab != ConversationDetailsTab.Media || loadedMediaType == state.mediaType) return
        val requestedType = state.mediaType
        state = state.copy(mediaLoading = true, mediaError = null)
        when (val result = repository.sharedMedia(conversationId, requestedType)) {
            is ApiResult.Success -> {
                state = state.copy(
                    mediaItems = result.value.items,
                    mediaCursor = result.value.nextCursor,
                )
                loadedMediaType = requestedType
            }
            is ApiResult.Failure -> handleFailure(
                result = result,
                onInlineError = { state = state.copy(mediaError = it) },
            )
        }
        state = state.copy(mediaLoading = false)
    }

    internal suspend fun loadContext(messageId: Long) {
        state = state.copy(contextLoading = true, contextError = null)
        when (val result = repository.messageContext(conversationId, messageId)) {
            is ApiResult.Success -> state = state.copy(messageContext = result.value)
            is ApiResult.Failure -> handleFailure(
                result = result,
                onInlineError = { state = state.copy(contextError = it) },
            )
        }
        state = state.copy(contextLoading = false)
    }

    private fun launchMediaLoadIfNeeded() {
        if (state.tab != ConversationDetailsTab.Media || loadedMediaType == state.mediaType) return
        mediaJob?.cancel()
        mediaJob = scope.launch { loadMedia() }
    }

    private fun handleFailure(
        result: ApiResult.Failure,
        onInlineError: (String) -> Unit,
    ) {
        if (result.statusCode == 401) {
            state = state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            onInlineError(result.message)
        }
    }

    private fun cancelWork() {
        searchJob?.cancel()
        mediaJob?.cancel()
        contextJob?.cancel()
        muteLoadJob?.cancel()
        muteSaveJob?.cancel()
        searchJob = null
        mediaJob = null
        contextJob = null
        muteLoadJob = null
        muteSaveJob = null
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 320L
        private const val MAX_QUERY_LENGTH = 200

        fun factory(
            conversationId: Long,
            repository: ConversationToolsRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ConversationDetailsViewModel::class.java))
                return ConversationDetailsViewModel(
                    conversationId = conversationId,
                    repository = repository,
                ) as T
            }
        }
    }
}
