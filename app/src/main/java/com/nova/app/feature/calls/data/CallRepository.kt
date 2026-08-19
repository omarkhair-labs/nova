package com.nova.app.feature.calls.data

import com.nova.app.core.network.ApiResult
import com.nova.app.feature.calls.domain.model.NovaCallKind
import com.nova.app.feature.calls.domain.model.NovaCallSession
import com.nova.app.feature.calls.domain.model.NovaIceConfig


interface CallRepository {
    suspend fun createCall(conversationId: Long, kind: NovaCallKind): ApiResult<NovaCallSession>
    suspend fun call(callId: String): ApiResult<NovaCallSession>
    suspend fun callAction(callId: String, action: String): ApiResult<NovaCallSession>
    suspend fun iceConfig(): ApiResult<NovaIceConfig>
    suspend fun realtimeAccessToken(): ApiResult<String>
}
