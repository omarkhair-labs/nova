package com.nova.app.feature.orbit.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.orbit.domain.model.OrbitPage


interface OrbitRepository {
    suspend fun orbit(cursor: String? = null): ApiResult<OrbitPage>
}
