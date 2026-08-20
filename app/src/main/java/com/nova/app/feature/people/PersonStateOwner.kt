package com.nova.app.feature.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PersonUiState(
    val person: NovaPerson? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val profileRefreshVersion: Int = 0,
    val feedRefreshVersion: Int = 0,
)


/** Route-scoped owner for person identity loading and normal follow mutations. */
class PersonStateOwner(
    private val username: String,
    private val peopleRepository: PeopleRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PersonUiState())
        private set

    fun loadPerson() {
        scope.launch { loadPersonNow() }
    }

    internal suspend fun loadPersonNow() {
        state = state.copy(isLoading = true, errorMessage = null)
        when (val result = peopleRepository.person(username)) {
            is ApiResult.Success -> {
                state = state.copy(person = result.value, isLoading = false)
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

    fun toggleFollow(selectedPerson: NovaPerson) {
        if (state.isLoading) return
        scope.launch { toggleFollowNow(selectedPerson) }
    }

    internal suspend fun toggleFollowNow(selectedPerson: NovaPerson) {
        if (state.isLoading) return
        state = state.copy(isLoading = true, errorMessage = null)
        when (
            val result = peopleRepository.setFollowing(
                username = selectedPerson.username,
                follow = !selectedPerson.isFollowing,
            )
        ) {
            is ApiResult.Success -> {
                state = state.copy(
                    person = result.value,
                    isLoading = false,
                    profileRefreshVersion = state.profileRefreshVersion + 1,
                    feedRefreshVersion = state.feedRefreshVersion + 1,
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
}
