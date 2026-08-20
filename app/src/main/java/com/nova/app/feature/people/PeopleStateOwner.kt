package com.nova.app.feature.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class PeopleUiState(
    val query: String = "",
    val people: List<NovaPerson> = emptyList(),
    val privacyByUserId: Map<Long, NovaPersonPrivacyState> = emptyMap(),
    val nextCursor: String? = null,
    val firstPageLoading: Boolean = true,
    val loadingMore: Boolean = false,
    val pagingError: String? = null,
    val followError: String? = null,
    val followingUsername: String? = null,
    val cancelingUsername: String? = null,
    val sessionExpiryVersion: Int = 0,
    val profileRefreshVersion: Int = 0,
    val feedRefreshVersion: Int = 0,
)


/** Owns the live People discovery/search/paging/follow state formerly split between NovaApp and PeopleScreen. */
class PeopleStateOwner(
    private val peopleRepository: PeopleRepository,
    private val pagingRepository: PeoplePagingRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(PeopleUiState())
        private set

    private var requestVersion = 0
    private var queryJob: Job? = null

    fun reset() {
        queryJob?.cancel()
        requestVersion += 1
        state = PeopleUiState()
    }

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
        val showSpinner = state.people.isEmpty()
        queryJob = scope.launch {
            delay(280)
            // Match the old LaunchedEffect -> rememberCoroutineScope handoff:
            // cancelling a later debounce must not cancel an already-started request.
            scope.launch {
                loadPageNow(reset = true, showSpinner = showSpinner)
            }
        }
    }

    fun retry() {
        scope.launch { loadPageNow(reset = true, showSpinner = state.people.isEmpty()) }
    }

    fun loadMore() {
        val cursor = state.nextCursor ?: return
        if (state.loadingMore) return
        scope.launch { loadPageNow(reset = false, cursorOverride = cursor) }
    }

    internal suspend fun loadPageNow(
        reset: Boolean,
        showSpinner: Boolean = true,
        cursorOverride: String? = null,
    ) {
        if (!reset && (state.loadingMore || state.nextCursor == null)) return
        requestVersion += 1
        val version = requestVersion
        val cursor = if (reset) null else cursorOverride ?: state.nextCursor

        state = if (reset) {
            state.copy(
                firstPageLoading = if (showSpinner) true else state.firstPageLoading,
                pagingError = null,
            )
        } else {
            state.copy(loadingMore = true, pagingError = null)
        }

        when (val result = pagingRepository.people(query = state.query, cursor = cursor)) {
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
                    firstPageLoading = false,
                    loadingMore = false,
                )
            }

            is ApiResult.Failure -> {
                if (version != requestVersion) return
                state = state.copy(
                    firstPageLoading = false,
                    loadingMore = false,
                    pagingError = result.message,
                    sessionExpiryVersion = if (result.statusCode == 401) {
                        state.sessionExpiryVersion + 1
                    } else {
                        state.sessionExpiryVersion
                    },
                )
            }
        }
    }

    fun toggleFollow(person: NovaPerson) {
        val privacy = state.privacyByUserId[person.id]
            ?: NovaPersonPrivacyState(isPrivate = false, followRequested = false, canViewContent = true)

        if (privacy.followRequested && !person.isFollowing) {
            cancelRequest(person)
            return
        }

        val wasFollowing = person.isFollowing
        state = if (!wasFollowing && privacy.isPrivate) {
            state.copy(
                privacyByUserId = state.privacyByUserId + (
                    person.id to privacy.copy(followRequested = true)
                ),
            )
        } else {
            state.copy(
                people = state.people.map { existing ->
                    if (existing.id == person.id) {
                        existing.copy(
                            isFollowing = !wasFollowing,
                            followersCount = (existing.followersCount + if (wasFollowing) -1 else 1)
                                .coerceAtLeast(0),
                        )
                    } else {
                        existing
                    }
                },
                privacyByUserId = if (wasFollowing && privacy.isPrivate) {
                    state.privacyByUserId + (
                        person.id to privacy.copy(canViewContent = false)
                    )
                } else {
                    state.privacyByUserId
                },
            )
        }

        // Preserve the old split-owner quirk: optimistic UI happened before NovaApp's global follow lock.
        if (state.followingUsername != null) return

        state = state.copy(
            followingUsername = person.username,
            followError = null,
        )
        scope.launch {
            when (
                val result = peopleRepository.setFollowing(
                    username = person.username,
                    follow = !wasFollowing,
                )
            ) {
                is ApiResult.Success -> {
                    state = state.copy(
                        followingUsername = null,
                        profileRefreshVersion = state.profileRefreshVersion + 1,
                        feedRefreshVersion = state.feedRefreshVersion + 1,
                    )
                }

                is ApiResult.Failure -> {
                    state = state.copy(followingUsername = null)
                    if (result.statusCode == 401) {
                        state = state.copy(
                            sessionExpiryVersion = state.sessionExpiryVersion + 1,
                        )
                    } else {
                        state = state.copy(followError = result.message)
                        // The former outer error triggered a no-spinner paging refresh in PeopleScreen.
                        loadPageNow(reset = true, showSpinner = false)
                    }
                }
            }
        }
    }

    private fun cancelRequest(person: NovaPerson) {
        if (state.cancelingUsername != null || state.followingUsername == person.username) return
        state = state.copy(cancelingUsername = person.username)
        scope.launch {
            when (val result = peopleRepository.setFollowing(person.username, false)) {
                is ApiResult.Success -> {
                    val privacy = state.privacyByUserId[person.id]
                        ?: NovaPersonPrivacyState(false, false, true)
                    state = state.copy(
                        privacyByUserId = state.privacyByUserId + (
                            person.id to privacy.copy(followRequested = false)
                        ),
                        cancelingUsername = null,
                    )
                }

                is ApiResult.Failure -> {
                    // Preserve legacy cancellation behavior: even a 401 stayed a local paging error.
                    state = state.copy(
                        cancelingUsername = null,
                        pagingError = result.message,
                    )
                }
            }
        }
    }
}


internal fun mergePeoplePage(
    existing: List<NovaPerson>,
    incoming: List<NovaPerson>,
): List<NovaPerson> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
    return existing + incoming.filterNot { it.id in existingIds }
}
