package com.nova.app.core.network

import com.nova.app.BuildConfig

/** Unified #253 QA targets the existing isolated Oracle Plantupia QA backend.
 *  Only the QA build target changes. Nova release still uses its original production host.
 */
object NovaServerConfig {
    private const val productionHost = "zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"
    private const val qaHost = "253.zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io"

    private val host = if (BuildConfig.BUILD_TYPE == "qa") qaHost else productionHost

    private val localQaUrl = BuildConfig.PLANTUPIA_QA_API_URL.takeIf { BuildConfig.BUILD_TYPE == "qa" && it.isNotBlank() }
    val API_URL: String = localQaUrl ?: ("https://" + host + "/api/v1/")
    val WS_URL: String = localQaUrl?.replace("http://", "ws://")?.replace("/api/v1/", "/ws/") ?: ("wss://" + host + "/ws/")
}
