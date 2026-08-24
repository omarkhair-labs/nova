package com.nova.app.feature.memories.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.WeeklyMemory
import com.nova.app.feature.memories.domain.model.MemoryDraft


interface MemoryRepository {
    suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int = 0,
    ): ApiResult<WeeklyMemory>

    suspend fun filmPlan(
        utcOffsetMinutes: Int,
        weeksAgo: Int = 0,
    ): ApiResult<MemoryFilmPlan>

    suspend fun drafts(): ApiResult<List<MemoryDraft>> = ApiResult.Success(emptyList())

    suspend fun createDraft(
        kind: String,
        title: String,
        note: String,
        mediaUri: Uri?,
    ): ApiResult<MemoryDraft> = ApiResult.Failure("Memory drafts are unavailable.")

    suspend fun deleteDraft(draftId: Long): ApiResult<Unit> =
        ApiResult.Failure("Memory drafts are unavailable.")

    suspend fun updateDraft(
        draftId: Long,
        kind: String,
        title: String,
        note: String,
        mediaUri: Uri?,
    ): ApiResult<MemoryDraft> = ApiResult.Failure("Memory drafts are unavailable.")
}
