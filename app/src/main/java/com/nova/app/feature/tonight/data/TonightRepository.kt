package com.nova.app.feature.tonight.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.tonight.domain.model.TonightSnapshot


interface TonightRepository {
    suspend fun tonight(utcOffsetMinutes: Int): ApiResult<TonightSnapshot>
}
