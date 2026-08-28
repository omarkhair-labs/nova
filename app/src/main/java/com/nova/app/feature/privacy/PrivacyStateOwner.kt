package com.nova.app.feature.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeoplePagingRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.privacy.data.FollowRequestRepository
import com.nova.app.feature.privacy.data.PrivacyRepository
import com.nova.app.feature.privacy.domain.model.NovaFollowRequest
import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


data class PrivacyUiState(
    val summary: NovaPrivacySummary? = null,
    val requests: List<NovaFollowRequest> = emptyList(),
    val sentRequests: List<NovaFollowRequest> = emptyList(),
    val followRequestTab: String = "received",
    val closeFriends: List<NovaPerson> = emptyList(),
    val followers: List<NovaPerson> = emptyList(),
    val followerCursor: String? = null,
    val followerQuery: String = "",
    val loading: Boolean = true,
    val loadingFollowers: Boolean = false,
    val loadingMore: Boolean = false,
    val privacyBusy: Boolean = false,
    val requestBusyId: Long? = null,
    val closeFriendBusyId: Long? = null,
    val error: String? = null,
    val feedback: String? = null,
)


/** Owns Privacy-screen async/domain state. Rendering and Activity session navigation remain UI-owned. */
class PrivacyStateOwner(
    private val username: String,
    private val privacyRepository: PrivacyRepository,
    private val followRequestRepository: FollowRequestRepository,
    private val peoplePagingRepository: PeoplePagingRepository,
    private val scope: CoroutineScope,
    private val onSessionExpired: () -> Unit,
) {
    var state by mutableStateOf(PrivacyUiState())
        private set

    private var followerQueryJob: Job? = null
    private var followerRequestVersion = 0L

    fun start() {
        loadSummaryBundle()
        loadFollowers(reset = true)
    }

    fun retry() {
        loadSummaryBundle()
        loadFollowers(reset = true)
    }

    fun loadSummaryBundle() {
        scope.launch { loadSummaryBundleNow() }
    }

    internal suspend fun loadSummaryBundleNow() {
        state = state.copy(loading = true, error = null)

        when (val result = privacyRepository.summary()) {
            is ApiResult.Success -> state = state.copy(summary = result.value)
            is ApiResult.Failure -> handleFailure(result)
        }
        when (val result = followRequestRepository.followRequests()) {
            is ApiResult.Success -> state = state.copy(requests = result.value)
            is ApiResult.Failure -> handleFailure(result)
        }
        when (val result = followRequestRepository.sentFollowRequests()) {
            is ApiResult.Success -> state = state.copy(sentRequests = result.value)
            is ApiResult.Failure -> handleFailure(result)
        }
        when (val result = privacyRepository.closeFriends()) {
            is ApiResult.Success -> state = state.copy(closeFriends = result.value)
            is ApiResult.Failure -> handleFailure(result)
        }

        state = state.copy(loading = false)
    }

    fun setFollowerQuery(raw: String) {
        val query = raw.take(FOLLOWER_QUERY_MAX_LENGTH)
        if (query == state.followerQuery) return
        state = state.copy(followerQuery = query)
        scheduleFollowerQueryLoad()
    }

    fun setFollowRequestTab(tab: String) {
        if (tab in setOf("received", "sent")) state = state.copy(followRequestTab = tab)
    }

    private fun scheduleFollowerQueryLoad() {
        followerQueryJob?.cancel()
        followerQueryJob = scope.launch {
            delay(FOLLOWER_SEARCH_DEBOUNCE_MS)
            // Preserve the old LaunchedEffect -> rememberCoroutineScope handoff:
            // a later query cancellation must not cancel an already-started request.
            loadFollowers(reset = true)
        }
    }

    fun loadFollowers(reset: Boolean) {
        if (username.isBlank()) return
        if (!reset && (state.loadingMore || state.followerCursor == null)) return
        scope.launch { loadFollowersNow(reset) }
    }

    internal suspend fun loadFollowersNow(reset: Boolean) {
        if (username.isBlank()) return
        if (!reset && (state.loadingMore || state.followerCursor == null)) return

        val requestVersion = if (reset) ++followerRequestVersion else followerRequestVersion
        val requestQuery = state.followerQuery.trim()
        val requestCursor = if (reset) null else state.followerCursor

        state = if (reset) {
            state.copy(loadingFollowers = true, error = null)
        } else {
            state.copy(loadingMore = true, error = null)
        }

        when (
            val result = peoplePagingRepository.followers(
                username = username,
                query = requestQuery,
                cursor = requestCursor,
            )
        ) {
            is ApiResult.Success -> {
                if (requestVersion != followerRequestVersion) return
                val page = result.value
                val nextFollowers = if (reset) {
                    page.people
                } else {
                    val existingIds = state.followers.mapTo(mutableSetOf()) { it.id }
                    state.followers + page.people.filterNot { it.id in existingIds }
                }
                state = state.copy(
                    followers = nextFollowers,
                    followerCursor = page.nextCursor,
                )
            }

            is ApiResult.Failure -> {
                if (requestVersion != followerRequestVersion) return
                handleFailure(result)
            }
        }

        if (requestVersion == followerRequestVersion) {
            state = state.copy(
                loadingFollowers = false,
                loadingMore = false,
            )
        }
    }

    fun togglePrivate(enabled: Boolean) {
        scope.launch { togglePrivateNow(enabled) }
    }

    fun setActivityStatus(enabled: Boolean) = updatePrivacySetting(
        showActivityStatus = enabled,
        feedback = if (enabled) "Activity status is visible." else "Activity status is hidden.",
    )

    fun setReadReceipts(enabled: Boolean) = updatePrivacySetting(
        sendReadReceipts = enabled,
        feedback = if (enabled) "Read receipts are on." else "Read receipts are off.",
    )

    fun setStoryAudience(audience: String) = updatePrivacySetting(
        storyAudience = audience,
        feedback = if (audience == "close_friends") {
            "New Stories default to Close Friends."
        } else {
            "New Stories default to followers."
        },
    )

    private fun updatePrivacySetting(
        showActivityStatus: Boolean? = null,
        sendReadReceipts: Boolean? = null,
        storyAudience: String? = null,
        feedback: String,
    ) {
        if (state.privacyBusy) return
        scope.launch {
            state = state.copy(privacyBusy = true, error = null, feedback = null)
            when (
                val result = privacyRepository.updateSettings(
                    showActivityStatus = showActivityStatus,
                    sendReadReceipts = sendReadReceipts,
                    storyAudience = storyAudience,
                )
            ) {
                is ApiResult.Success -> state = state.copy(summary = result.value, feedback = feedback)
                is ApiResult.Failure -> handleFailure(result)
            }
            state = state.copy(privacyBusy = false)
        }
    }

    internal suspend fun togglePrivateNow(enabled: Boolean) {
        if (state.privacyBusy) return
        state = state.copy(
            privacyBusy = true,
            error = null,
            feedback = null,
        )

        when (val result = privacyRepository.setPrivate(enabled)) {
            is ApiResult.Success -> {
                val summary = result.value
                state = state.copy(summary = summary)
                if (summary.acceptedPendingRequests > 0) {
                    state = state.copy(
                        feedback = "${summary.acceptedPendingRequests} pending follow requests were accepted.",
                        requests = emptyList(),
                    )
                    loadFollowers(reset = true)
                } else {
                    state = state.copy(
                        feedback = if (enabled) {
                            "Your account is now private."
                        } else {
                            "Your account is now public."
                        },
                    )
                }
            }

            is ApiResult.Failure -> handleFailure(result)
        }

        state = state.copy(privacyBusy = false)
    }

    fun decideFollowRequest(item: NovaFollowRequest, accept: Boolean) {
        scope.launch { decideFollowRequestNow(item, accept) }
    }

    internal suspend fun decideFollowRequestNow(item: NovaFollowRequest, accept: Boolean) {
        if (state.requestBusyId != null) return
        state = state.copy(
            requestBusyId = item.id,
            error = null,
        )

        val result = if (accept) {
            followRequestRepository.acceptFollowRequest(item.id)
        } else {
            followRequestRepository.declineFollowRequest(item.id)
        }

        when (result) {
            is ApiResult.Success -> {
                val summary = state.summary
                state = state.copy(
                    requests = state.requests.filterNot { it.id == item.id },
                    summary = summary?.copy(
                        pendingFollowRequests = (summary.pendingFollowRequests - 1).coerceAtLeast(0),
                    ),
                    feedback = if (accept) {
                        "@${item.requester.username} can now follow you."
                    } else {
                        "Follow request declined."
                    },
                )
                if (accept) loadFollowers(reset = true)
            }

            is ApiResult.Failure -> handleFailure(result)
        }

        state = state.copy(requestBusyId = null)
    }

    fun toggleCloseFriend(person: NovaPerson) {
        scope.launch { toggleCloseFriendNow(person) }
    }

    internal suspend fun toggleCloseFriendNow(person: NovaPerson) {
        if (state.closeFriendBusyId != null) return
        val currentlyClose = state.closeFriends.any { it.id == person.id }
        state = state.copy(
            closeFriendBusyId = person.id,
            error = null,
        )

        when (val result = privacyRepository.setCloseFriend(person.username, !currentlyClose)) {
            is ApiResult.Success -> {
                val closeFriends = if (currentlyClose) {
                    state.closeFriends.filterNot { it.id == person.id }
                } else {
                    state.closeFriends + person
                }
                state = state.copy(
                    closeFriends = closeFriends,
                    summary = state.summary?.copy(closeFriendsCount = closeFriends.size),
                    feedback = if (currentlyClose) {
                        "Removed @${person.username} from Close Friends."
                    } else {
                        "Added @${person.username} to Close Friends."
                    },
                )
            }

            is ApiResult.Failure -> handleFailure(result)
        }

        state = state.copy(closeFriendBusyId = null)
    }

    private fun handleFailure(result: ApiResult.Failure) {
        if (result.statusCode == 401) {
            onSessionExpired()
        } else {
            state = state.copy(error = result.message)
        }
    }

    companion object {
        internal const val FOLLOWER_SEARCH_DEBOUNCE_MS = 280L
        internal const val FOLLOWER_QUERY_MAX_LENGTH = 50
    }
}
