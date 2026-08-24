package com.nova.app.feature.messages.inbox

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.auth.shouldExpireNovaSession
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.data.InboxRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/** Lifecycle-aware owner for inbox search, paging, unread state, and load errors. */
class InboxViewModel internal constructor(
    private val repository: InboxRepository,
    private val workScope: CoroutineScope? = null,
    private val searchDebounceMillis: Long = SEARCH_DEBOUNCE_MS,
    autoLoad: Boolean = true,
) : ViewModel() {
    var state by mutableStateOf(InboxUiState())
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope
    private var searchJob: Job? = null
    private var requestVersion: Int = 0

    init {
        if (autoLoad) scheduleSearch()
    }

    fun onQueryChanged(value: String) {
        state = state.copy(query = value.take(MAX_QUERY_LENGTH))
        scheduleSearch()
    }

    fun onFilterChanged(value: InboxFilter) {
        if (state.filter == value) return
        state = state.copy(filter = value)
        scheduleSearch()
    }

    fun retry() {
        launchLoad(reset = true, showSpinner = true)
    }

    fun refresh() {
        launchLoad(reset = true, showSpinner = false)
    }

    fun loadMore() {
        launchLoad(reset = false, showSpinner = false)
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        val search = state.query
        searchJob = scope.launch {
            delay(searchDebounceMillis)
            loadInbox(
                search = search,
                reset = true,
                showSpinner = state.conversations.isEmpty(),
            )
        }
    }

    private fun launchLoad(reset: Boolean, showSpinner: Boolean) {
        scope.launch {
            loadInbox(
                search = state.query,
                reset = reset,
                showSpinner = showSpinner,
            )
        }
    }

    internal suspend fun loadInbox(
        search: String,
        reset: Boolean,
        showSpinner: Boolean,
    ) {
        if (!reset && (state.isLoadingMore || state.nextCursor == null)) return

        requestVersion += 1
        val version = requestVersion
        val cursor = if (reset) null else state.nextCursor
        state = if (reset) {
            state.copy(
                isLoading = if (showSpinner) true else state.isLoading,
                errorMessage = null,
            )
        } else {
            state.copy(isLoadingMore = true, errorMessage = null)
        }

        when (val result = repository.conversations(search, cursor, state.filter.apiValue)) {
            is ApiResult.Success -> {
                if (version != requestVersion) return
                val conversations = if (reset) {
                    result.value.conversations
                } else {
                    val existingIds = state.conversations.mapTo(mutableSetOf()) { it.id }
                    state.conversations + result.value.conversations.filterNot { it.id in existingIds }
                }
                state = state.copy(
                    conversations = conversations,
                    unreadCount = result.value.unreadCount,
                    nextCursor = result.value.nextCursor,
                    isLoading = false,
                    isLoadingMore = false,
                    errorMessage = null,
                    unreadUpdateVersion = state.unreadUpdateVersion + 1,
                )
            }

            is ApiResult.Failure -> {
                if (version != requestVersion) return
                state = if (shouldExpireNovaSession(result.statusCode)) {
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 260L
        private const val MAX_QUERY_LENGTH = 40

        fun factory(repository: InboxRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(InboxViewModel::class.java))
                    return InboxViewModel(repository) as T
                }
            }
    }
}
