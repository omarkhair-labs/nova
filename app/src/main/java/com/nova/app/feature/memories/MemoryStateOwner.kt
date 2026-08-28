package com.nova.app.feature.memories

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.data.MemoryRepository
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import com.nova.app.feature.memories.domain.model.MemoryDraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


data class MemoryUiState(
    val memory: WeeklyMemory? = null,
    val weeksAgo: Int = 0,
    val loading: Boolean = true,
    val error: String? = null,
    val sessionExpiryVersion: Int = 0,
    val drafts: List<MemoryDraft> = emptyList(),
    val savingDraft: Boolean = false,
    val deletingDraftId: Long? = null,
)


class MemoryStateOwner(
    private val repository: MemoryRepository,
    private val scope: CoroutineScope,
) {
    var state by mutableStateOf(MemoryUiState())
        private set
    private var requestVersion = 0

    fun load(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        scope.launch { loadNow(utcOffsetMinutes, weeksAgo, showSpinner) }
    }

    internal suspend fun loadNow(
        utcOffsetMinutes: Int,
        weeksAgo: Int = state.weeksAgo,
        showSpinner: Boolean = false,
    ) {
        requestVersion += 1
        val version = requestVersion
        val boundedWeek = weeksAgo.coerceIn(0, 51)
        if (showSpinner) state = state.copy(loading = true)
        val result = repository.week(utcOffsetMinutes, boundedWeek)
        if (version != requestVersion) return
        when (result) {
            is ApiResult.Success -> state = state.copy(
                memory = result.value,
                weeksAgo = boundedWeek,
                loading = false,
                error = null,
            )
            is ApiResult.Failure -> {
                state = if (result.statusCode == 401) {
                    state.copy(
                        loading = false,
                        error = null,
                        sessionExpiryVersion = state.sessionExpiryVersion + 1,
                    )
                } else {
                    state.copy(loading = false, error = result.message)
                }
            }
        }
        val draftsResult = repository.drafts()
        if (version != requestVersion) return
        when (draftsResult) {
            is ApiResult.Success -> state = state.copy(drafts = draftsResult.value)
            is ApiResult.Failure -> if (draftsResult.statusCode == 401) {
                state = state.copy(sessionExpiryVersion = state.sessionExpiryVersion + 1)
            } else if (state.error == null) {
                state = state.copy(error = draftsResult.message)
            }
        }
    }

    fun createDraft(kind: String, title: String, note: String, mediaUri: Uri?) {
        if (state.savingDraft) return
        scope.launch {
            state = state.copy(savingDraft = true, error = null)
            when (val result = repository.createDraft(kind, title, note, mediaUri)) {
                is ApiResult.Success -> state = state.copy(
                    drafts = listOf(result.value) + state.drafts.filterNot { it.id == result.value.id },
                    savingDraft = false,
                )
                is ApiResult.Failure -> state = state.copy(
                    savingDraft = false,
                    error = result.message,
                    sessionExpiryVersion = state.sessionExpiryVersion + if (result.statusCode == 401) 1 else 0,
                )
            }
        }
    }

    fun deleteDraft(draftId: Long) {
        if (state.deletingDraftId != null) return
        scope.launch {
            state = state.copy(deletingDraftId = draftId, error = null)
            when (val result = repository.deleteDraft(draftId)) {
                is ApiResult.Success -> state = state.copy(
                    drafts = state.drafts.filterNot { it.id == draftId },
                    deletingDraftId = null,
                )
                is ApiResult.Failure -> state = state.copy(
                    deletingDraftId = null,
                    error = result.message,
                    sessionExpiryVersion = state.sessionExpiryVersion + if (result.statusCode == 401) 1 else 0,
                )
            }
        }
    }

    fun updateDraft(draftId: Long, kind: String, title: String, note: String, mediaUri: Uri?) {
        if (state.savingDraft) return
        scope.launch {
            state = state.copy(savingDraft = true, error = null)
            when (val result = repository.updateDraft(draftId, kind, title, note, mediaUri)) {
                is ApiResult.Success -> state = state.copy(
                    drafts = listOf(result.value) + state.drafts.filterNot { it.id == result.value.id },
                    savingDraft = false,
                )
                is ApiResult.Failure -> state = state.copy(
                    savingDraft = false,
                    error = result.message,
                    sessionExpiryVersion = state.sessionExpiryVersion + if (result.statusCode == 401) 1 else 0,
                )
            }
        }
    }
}
