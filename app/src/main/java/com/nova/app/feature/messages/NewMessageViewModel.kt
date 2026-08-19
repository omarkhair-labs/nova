package com.nova.app.feature.messages

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaPerson
import com.nova.app.feature.messages.data.MessagesRepository
import com.nova.app.feature.messages.domain.model.NovaConversation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class NewMessageUiState(
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val isLoading: Boolean = true,
    val openingUsername: String? = null,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val conversationReadyVersion: Int = 0,
    val readyConversation: NovaConversation? = null,
)


/** Dialog-scoped owner for direct-message people search, opening, and terminal effects. */
class NewMessageViewModel internal constructor(
    private val messagesRepository: MessagesRepository,
    private val peopleSearch: suspend (String) -> ApiResult<List<NovaPerson>>,
    private val debounceMillis: Long = 220L,
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    var state by mutableStateOf(NewMessageUiState())
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope

    private var searchJob: Job? = null

    init {
        scheduleSearch()
    }

    fun updateQuery(value: String) {
        val capped = value.take(40)
        if (capped == state.query) return
        state = state.copy(query = capped)
        scheduleSearch()
    }

    fun openConversation(person: NovaPerson) {
        if (state.openingUsername != null) return
        scope.launch {
            state = state.copy(openingUsername = person.username, errorMessage = null)
            when (val result = messagesRepository.openConversation(person.username)) {
                is ApiResult.Success -> {
                    state = state.copy(
                        openingUsername = null,
                        readyConversation = result.value,
                        conversationReadyVersion = state.conversationReadyVersion + 1,
                    )
                }
                is ApiResult.Failure -> {
                    state = state.copy(openingUsername = null)
                    handleFailure(result)
                }
            }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(debounceMillis)
            state = state.copy(isLoading = true, errorMessage = null)
            when (val result = peopleSearch(state.query.trim())) {
                is ApiResult.Success -> state = state.copy(people = result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(isLoading = false)
        }
    }

    private fun handleFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(errorMessage = result.message)
        }
    }

    override fun onCleared() {
        searchJob?.cancel()
    }

    companion object {
        fun factory(
            messagesRepository: MessagesRepository,
            peopleSearch: suspend (String) -> ApiResult<List<NovaPerson>>,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NewMessageViewModel::class.java))
                return NewMessageViewModel(
                    messagesRepository = messagesRepository,
                    peopleSearch = peopleSearch,
                ) as T
            }
        }
    }
}
