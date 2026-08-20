package com.nova.app.feature.people

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.people.data.PeopleRepository
import com.nova.app.feature.people.domain.model.NovaPerson
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class PersonUiState(
    val person: NovaPerson? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profilePosts: List<NovaPost> = emptyList(),
    val postsLoading: Boolean = true,
    val postsError: String? = null,
    val sessionExpiryVersion: Int = 0,
    val profileRefreshVersion: Int = 0,
    val feedRefreshVersion: Int = 0,
)


/** Route-scoped owner for person loading, profile posts, and normal follow mutations. */
class PersonStateOwner(
    private val username: String,
    private val peopleRepository: PeopleRepository,
    private val postRepository: PostRepository,
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

    fun loadPosts() {
        scope.launch { loadPostsNow() }
    }

    internal suspend fun loadPostsNow() {
        state = state.copy(postsLoading = true, postsError = null)
        when (val result = postRepository.personPosts(username)) {
            is ApiResult.Success -> {
                state = state.copy(
                    profilePosts = result.value,
                    postsLoading = false,
                )
            }

            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        postsLoading = false,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(postsLoading = false, postsError = result.message)
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
