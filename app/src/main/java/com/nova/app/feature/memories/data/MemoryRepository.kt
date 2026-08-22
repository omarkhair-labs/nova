package com.nova.app.feature.memories.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.memories.domain.model.MemoryFilmPlan
import com.nova.app.feature.memories.domain.model.WeeklyMemory


interface MemoryRepository {
    suspend fun week(
        utcOffsetMinutes: Int,
        weeksAgo: Int = 0,
    ): ApiResult<WeeklyMemory>

    suspend fun filmPlan(
        utcOffsetMinutes: Int,
        weeksAgo: Int = 0,
    ): ApiResult<MemoryFilmPlan>
}
