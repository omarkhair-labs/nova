package com.nova.app.core.network

import com.nova.app.BuildConfig

/** Routes the side-by-side QA build to PR #240 without touching Nova production. */
object NovaServerConfig {
    private const val productionHost = "zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"
    private const val qaHost = "240.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"

    private val host = if (BuildConfig.BUILD_TYPE == "qa") qaHost else productionHost

    private val localQaUrl = BuildConfig.PLANTUPIA_QA_API_URL.takeIf { BuildConfig.BUILD_TYPE == "qa" && it.isNotBlank() }
    val API_URL: String = localQaUrl ?: ("https://" + host + "/api/v1/")
    val WS_URL: String = localQaUrl?.replace("http://", "ws://")?.replace("/api/v1/", "/ws/") ?: ("wss://" + host + "/ws/")
}
