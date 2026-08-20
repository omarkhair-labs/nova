package com.nova.app.feature.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.core.privacy.NovaPersonPrivacyState
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


const val MODE_FOLLOWERS = "followers"
const val MODE_FOLLOWING = "following"


data class SocialConnectionsUiState(
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val privacyByUserId: Map<Long, NovaPersonPrivacyState> = emptyMap(),
    val nextCursor: String? = null,
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val updatingUsername: String? = null,
    val errorMessage: String? = null,
    val sessionExpiryVersion: Int = 0,
    val currentUserId: Long? = null,
)


/** Owns followers/following search, paging, privacy-aware follow state, and terminal-session effects. */
class SocialConnectionsStateOwner(
    private val username: String,
    mode: String,
    currentUserId: Long?,
    private val pagingRepository: PeoplePagingRepository,
    private val peopleRepository: PeopleRepository,
    private val scope: CoroutineScope,
) {
    val mode: String = if (mode == MODE_FOLLOWING) MODE_FOLLOWING else MODE_FOLLOWERS

    var state by mutableStateOf(SocialConnectionsUiState(currentUserId = currentUserId))
        private set

    private var requestVersion = 0
    private var queryJob: Job? = null

    fun enter() {
        scheduleQueryLoad()
    }

    fun setQuery(raw: String) {
        val query = raw.take(40)
        if (query == state.query) return
        state = state.copy(query = query)
        scheduleQueryLoad()
    }

    private fun scheduleQueryLoad() {
        queryJob?.cancel()
        queryJob = scope.launch {
            delay(240)
            loadNow(reset = true)
        }
    }

    fun retry() {
        scope.launch { loadNow(reset = true) }
    }

    fun loadMore() {
        if (state.isLoadingMore || state.nextCursor == null) return
        scope.launch { loadNow(reset = false) }
    }

    internal suspend fun loadNow(reset: Boolean) {
        if (!reset && (state.isLoadingMore || state.nextCursor == null)) return
        requestVersion += 1
        val version = requestVersion
        val cursor = if (reset) null else state.nextCursor

        state = if (reset) {
            state.copy(isLoading = true, errorMessage = null)
        } else {
            state.copy(isLoadingMore = true, errorMessage = null)
        }

        val result = if (mode == MODE_FOLLOWING) {
            pagingRepository.following(username, state.query, cursor)
        } else {
            pagingRepository.followers(username, state.query, cursor)
        }

        when (result) {
            is ApiResult.Success -> {
                if (version != requestVersion) return
                state = state.copy(
                    people = if (reset) {
                        result.value.people
                    } else {
                        mergePeoplePage(state.people, result.value.people)
                    },
                    privacyByUserId = if (reset) {
                        result.value.privacyByUserId
                    } else {
                        state.privacyByUserId + result.value.privacyByUserId
                    },
                    nextCursor = result.value.nextCursor,
                    isLoading = false,
                    isLoadingMore = false,
                )
            }

            is ApiResult.Failure -> {
                if (version != requestVersion) return
                state = if (result.statusCode == 401) {
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

    fun toggleFollow(person: NovaPerson) {
        if (state.updatingUsername != null || person.id == state.currentUserId) return
        scope.launch { toggleFollowNow(person) }
    }

    internal suspend fun toggleFollowNow(person: NovaPerson) {
        if (state.updatingUsername != null || person.id == state.currentUserId) return
        val privacy = state.privacyByUserId[person.id]
            ?: NovaPersonPrivacyState(false, false, true)
        val shouldFollow = !person.isFollowing && !privacy.followRequested
        state = state.copy(updatingUsername = person.username, errorMessage = null)

        when (val result = peopleRepository.setFollowing(person.username, shouldFollow)) {
            is ApiResult.Success -> {
                state = when {
                    privacy.followRequested -> {
                        state.copy(
                            privacyByUserId = state.privacyByUserId + (
                                person.id to privacy.copy(followRequested = false)
                            ),
                            updatingUsername = null,
                        )
                    }

                    privacy.isPrivate && !person.isFollowing -> {
                        state.copy(
                            privacyByUserId = state.privacyByUserId + (
                                person.id to privacy.copy(
                                    followRequested = true,
                                    canViewContent = false,
                                )
                            ),
                            updatingUsername = null,
                        )
                    }

                    else -> {
                        state.copy(
                            people = state.people.map { existing ->
                                if (existing.id == result.value.id) result.value else existing
                            },
                            privacyByUserId = if (privacy.isPrivate && person.isFollowing) {
                                state.privacyByUserId + (
                                    person.id to privacy.copy(canViewContent = false)
                                )
                            } else {
                                state.privacyByUserId
                            },
                            updatingUsername = null,
                        )
                    }
                }
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        updatingUsername = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(
                        updatingUsername = null,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }
}
