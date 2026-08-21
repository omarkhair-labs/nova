package com.nova.app.feature.messages.group

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.messages.group.data.GroupMembershipRepository
import com.nova.app.feature.messages.group.data.GroupPeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class AddGroupMembersUiState(
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val selected: Set<String> = emptySet(),
    val loading: Boolean = true,
    val adding: Boolean = false,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val updatedVersion: Int = 0,
)


/** Dialog-scoped owner for add-member search, selection, submission, and terminal effects. */
class AddGroupMembersViewModel internal constructor(
    private val conversationId: Long,
    private val existingUsernames: Set<String>,
    private val membershipRepository: GroupMembershipRepository,
    private val peopleRepository: GroupPeopleRepository,
    private val debounceMillis: Long = 220L,
    private val workScope: CoroutineScope? = null,
) : ViewModel() {
    var state by mutableStateOf(AddGroupMembersUiState())
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

    fun toggleSelection(username: String) {
        state = state.copy(
            selected = if (username in state.selected) state.selected - username else state.selected + username,
        )
    }

    fun add() {
        if (state.adding || state.selected.isEmpty()) return
        scope.launch {
            state = state.copy(adding = true, errorMessage = null)
            when (val result = membershipRepository.addMembers(conversationId, state.selected.toList())) {
                is ApiResult.Success -> {
                    state = state.copy(updatedVersion = state.updatedVersion + 1)
                }
                is ApiResult.Failure -> {
                    handleFailure(result)
                    state = state.copy(adding = false)
                }
            }
        }
    }

    private fun scheduleSearch() {
        searchJob?.cancel()
        searchJob = scope.launch {
            delay(debounceMillis)
            state = state.copy(loading = true)
            when (val result = peopleRepository.people(state.query.trim())) {
                is ApiResult.Success -> {
                    state = state.copy(
                        people = result.value.filterNot { it.username in existingUsernames },
                        errorMessage = null,
                    )
                }
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(loading = false)
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
            conversationId: Long,
            existingUsernames: Set<String>,
            membershipRepository: GroupMembershipRepository,
            peopleRepository: GroupPeopleRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AddGroupMembersViewModel::class.java))
                return AddGroupMembersViewModel(
                    conversationId = conversationId,
                    existingUsernames = existingUsernames,
                    membershipRepository = membershipRepository,
                    peopleRepository = peopleRepository,
                ) as T
            }
        }
    }
}
