package com.nova.app.feature.stories

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.stories.data.StoriesRepository
import com.nova.app.feature.stories.domain.model.NovaStoryGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class StoriesUiState(
    val groups: List<NovaStoryGroup> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val uploading: Boolean = false,
    val sessionExpiryVersion: Int = 0,
    val mediaCreatedVersion: Int = 0,
    val textCreatedVersion: Int = 0,
)


/** Owns Stories rail loading/create transport state; picker/dialog state stays in UI. */
class StoriesStateOwner(
    private val repository: StoriesRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(StoriesUiState())
        private set

    fun clearError() {
        state = state.copy(error = null)
    }

    fun load(showSpinner: Boolean = false) {
        scope.launch { loadNow(showSpinner) }
    }

    internal suspend fun loadNow(showSpinner: Boolean = false) {
        if (showSpinner) {
            state = state.copy(loading = true)
        }

        when (val result = repository.stories()) {
            is ApiResult.Success -> {
                state = state.copy(
                    groups = result.value,
                    loading = false,
                    error = null,
                )
            }

            is ApiResult.Failure -> {
                recordFailure(result)
                state = state.copy(loading = false)
            }
        }
    }

    fun createMediaStory(
        mediaUri: Uri,
        caption: String,
        audience: String,
    ) {
        scope.launch { createMediaStoryNow(mediaUri, caption, audience) }
    }

    internal suspend fun createMediaStoryNow(
        mediaUri: Uri,
        caption: String,
        audience: String,
    ) {
        state = state.copy(uploading = true)
        when (val result = repository.createStory(mediaUri, caption, audience)) {
            is ApiResult.Success -> {
                state = state.copy(mediaCreatedVersion = state.mediaCreatedVersion + 1)
                // Preserve the old sibling reload: create completion does not wait for refresh.
                load()
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(uploading = false)
    }

    fun createTextStory(
        text: String,
        backgroundStyle: String,
        audience: String,
    ) {
        scope.launch { createTextStoryNow(text, backgroundStyle, audience) }
    }

    internal suspend fun createTextStoryNow(
        text: String,
        backgroundStyle: String,
        audience: String,
    ) {
        state = state.copy(uploading = true)
        when (val result = repository.createTextStory(text, backgroundStyle, audience)) {
            is ApiResult.Success -> {
                state = state.copy(textCreatedVersion = state.textCreatedVersion + 1)
                // Preserve the old sibling reload: create completion does not wait for refresh.
                load()
            }

            is ApiResult.Failure -> recordFailure(result)
        }
        state = state.copy(uploading = false)
    }

    private fun recordFailure(result: ApiResult.Failure) {
        state = if (result.statusCode == 401) {
            state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
        } else {
            state.copy(error = result.message)
        }
    }
}
