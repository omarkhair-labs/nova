package com.nova.app.feature.pulse.data

import android.net.Uri
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.pulse.domain.model.NovaPulse


interface PulseRepository {
    suspend fun pulses(): ApiResult<List<NovaPulse>>

    suspend fun createTextPulse(
        note: String,
        audience: String = "followers",
    ): ApiResult<NovaPulse>

    suspend fun createTextPulse(
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createTextPulse(note, audience)

    suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String = "",
        audience: String = "followers",
    ): ApiResult<NovaPulse>

    suspend fun createMediaPulse(
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = createMediaPulse(mediaUri, note, audience)

    suspend fun pulseChain(pulseId: Long): ApiResult<List<NovaPulse>>

    suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String = "followers",
    ): ApiResult<NovaPulse>

    suspend fun replyTextPulse(
        pulseId: Long,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = replyTextPulse(pulseId, note, audience)

    suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String = "",
        audience: String = "followers",
    ): ApiResult<NovaPulse>

    suspend fun replyMediaPulse(
        pulseId: Long,
        mediaUri: Uri,
        note: String,
        audience: String,
        category: String,
    ): ApiResult<NovaPulse> = replyMediaPulse(pulseId, mediaUri, note, audience)

    suspend fun deletePulse(pulseId: Long): ApiResult<Unit>

    suspend fun recordView(pulseId: Long): ApiResult<NovaPulse> =
        ApiResult.Failure("Pulse viewing is unavailable.")

    suspend fun setReaction(pulseId: Long, enabled: Boolean): ApiResult<NovaPulse> =
        ApiResult.Failure("Pulse reactions are unavailable.")
}
