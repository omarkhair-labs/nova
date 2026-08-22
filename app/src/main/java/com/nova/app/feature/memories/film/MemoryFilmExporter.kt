package com.nova.app.feature.memories.film

import com.nova.app.feature.memories.domain.model.MemoryFilmPlan


data class MemoryFilmExport(
    val filePath: String,
    val durationMs: Long,
)


interface MemoryFilmExporter {
    suspend fun export(
        plan: MemoryFilmPlan,
        onProgress: (Int) -> Unit = {},
    ): Result<MemoryFilmExport>

    fun cancel()
}
