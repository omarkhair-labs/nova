package com.nova.app.feature.messages.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.domain.model.NovaConversation
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.data.GroupPeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class NewGroupUiState(
    val title: String = "",
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val selected: Set<String> = emptySet(),
    val loadingPeople: Boolean = true,
    val creating: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val conversationReadyVersion: Int = 0,
    val readyConversation: NovaConversation? = null,
) {
    val canCreate: Boolean
        get() = !creating && title.isNotBlank() && selected.size >= 2
}


/** Dialog-scoped owner for new-group people search, selection, validation, and creation. */
class NewGroupViewModel internal constructor(
    private val membershipRepository: GroupMembershipRepository,
    private val peopleRepository: GroupPeopleRepository,
    private val debounceMillis: Long = 220L,
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    var state by mutableStateOf(NewGroupUiState())
        private set

    private val scope: CoroutineScope
        get() = workScope ?: viewModelScope

    private var searchJob: Job? = null

    init {
        scheduleSearch()
    }

    fun updateTitle(value: String) {
        state = state.copy(title = value.take(80))
    }

    fun updateQuery(value: String) {
        val capped = value.take(40)
        if (capped == state.query) return
        state = state.copy(query = capped)
        scheduleSearch()
    }

    fun toggleSelection(username: String) {
        if (state.creating) return
        state = state.copy(
            selected = if (username in state.selected) state.selected - username else state.selected + username,
        )
    }

    fun createGroup() {
        if (!state.canCreate) return
        val title = state.title
        val selected = state.selected.toList()
        scope.launch {
            state = state.copy(creating = true, errorMessage = null)
            when (val result = membershipRepository.createGroup(title, selected)) {
                is ApiResult.Success -> {
                    state = state.copy(
                        creating = false,
                        readyConversation = result.value,
                        conversationReadyVersion = state.conversationReadyVersion + 1,
                    )
                }
                is ApiResult.Failure -> {
                    handleFailure(result)
                    state = state.copy(creating = false)
                }
            }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(debounceMillis)
            state = state.copy(loadingPeople = true, errorMessage = null)
            when (val result = peopleRepository.people(state.query.trim())) {
                is ApiResult.Success -> state = state.copy(people = result.value)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(loadingPeople = false)
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
            membershipRepository: GroupMembershipRepository,
            peopleRepository: GroupPeopleRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(NewGroupViewModel::class.java))
                return NewGroupViewModel(
                    membershipRepository = membershipRepository,
                    peopleRepository = peopleRepository,
                ) as T
            }
        }
    }
}
