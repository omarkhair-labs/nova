package com.nova.app.core.calls

import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport


data class NovaCallAudioQualitySnapshot(
    val timestampUs: Long,
    val inboundBytes: Long?,
    val inboundPackets: Long?,
    val inboundPacketsLost: Long?,
    val concealedSamples: Long?,
    val jitterMs: Double?,
    val outboundBytes: Long?,
    val outboundPackets: Long?,
    val roundTripTimeMs: Double?,
    val availableOutgoingBitrateKbps: Double?,
) {
    companion object {
        fun from(report: RTCStatsReport): NovaCallAudioQualitySnapshot {
            val stats = report.statsMap.values
            val inbound = stats.firstOrNull { it.type == "inbound-rtp" && it.isAudioRtp() }
            val outbound = stats.firstOrNull { it.type == "outbound-rtp" && it.isAudioRtp() }
            val remoteInbound = stats.firstOrNull { it.type == "remote-inbound-rtp" && it.isAudioRtp() }
            val selectedPair = stats.firstOrNull {
                it.type == "candidate-pair" &&
                    it.members.string("state") == "succeeded" &&
                    (it.members.boolean("nominated") == true || it.members.boolean("selected") == true)
            } ?: stats.firstOrNull {
                it.type == "candidate-pair" && it.members.string("state") == "succeeded"
            }

            return NovaCallAudioQualitySnapshot(
                timestampUs = report.timestampUs.toLong(),
                inboundBytes = inbound?.members?.long("bytesReceived"),
                inboundPackets = inbound?.members?.long("packetsReceived"),
                inboundPacketsLost = inbound?.members?.long("packetsLost"),
                concealedSamples = inbound?.members?.long("concealedSamples"),
                jitterMs = inbound?.members?.double("jitter")?.times(1_000.0),
                outboundBytes = outbound?.members?.long("bytesSent"),
                outboundPackets = outbound?.members?.long("packetsSent"),
                roundTripTimeMs = (
                    selectedPair?.members?.double("currentRoundTripTime")
                        ?: remoteInbound?.members?.double("roundTripTime")
                    )?.times(1_000.0),
                availableOutgoingBitrateKbps = selectedPair?.members
                    ?.double("availableOutgoingBitrate")
                    ?.div(1_000.0),
            )
        }
    }
}


data class NovaCallAudioQualityDelta(
    val inboundKbps: Double?,
    val outboundKbps: Double?,
    val packetLossPercent: Double?,
    val inboundPacketsDelta: Long?,
    val outboundPacketsDelta: Long?,
    val concealedSamplesDelta: Long?,
    val jitterMs: Double?,
    val roundTripTimeMs: Double?,
) {
    val inboundProgressed: Boolean?
        get() = inboundPacketsDelta?.let { it > 0L }

    val outboundProgressed: Boolean?
        get() = outboundPacketsDelta?.let { it > 0L }

    companion object {
        fun between(
            previous: NovaCallAudioQualitySnapshot,
            current: NovaCallAudioQualitySnapshot,
        ): NovaCallAudioQualityDelta {
            val elapsedSeconds = (current.timestampUs - previous.timestampUs)
                .takeIf { it > 0L }
                ?.div(1_000_000.0)

            val inboundBytesDelta = delta(previous.inboundBytes, current.inboundBytes)
            val outboundBytesDelta = delta(previous.outboundBytes, current.outboundBytes)
            val inboundPacketsDelta = delta(previous.inboundPackets, current.inboundPackets)
            val outboundPacketsDelta = delta(previous.outboundPackets, current.outboundPackets)
            val lostDelta = delta(previous.inboundPacketsLost, current.inboundPacketsLost)
            val concealedDelta = delta(previous.concealedSamples, current.concealedSamples)

            val delivered = inboundPacketsDelta?.coerceAtLeast(0L)
            val lost = lostDelta?.coerceAtLeast(0L)
            val packetLossPercent = if (delivered != null && lost != null && delivered + lost > 0L) {
                lost * 100.0 / (delivered + lost).toDouble()
            } else {
                null
            }

            return NovaCallAudioQualityDelta(
                inboundKbps = bitrateKbps(inboundBytesDelta, elapsedSeconds),
                outboundKbps = bitrateKbps(outboundBytesDelta, elapsedSeconds),
                packetLossPercent = packetLossPercent,
                inboundPacketsDelta = inboundPacketsDelta,
                outboundPacketsDelta = outboundPacketsDelta,
                concealedSamplesDelta = concealedDelta,
                jitterMs = current.jitterMs,
                roundTripTimeMs = current.roundTripTimeMs,
            )
        }

        private fun delta(previous: Long?, current: Long?): Long? {
            if (previous == null || current == null || current < previous) return null
            return current - previous
        }

        private fun bitrateKbps(bytesDelta: Long?, elapsedSeconds: Double?): Double? {
            if (bytesDelta == null || elapsedSeconds == null || elapsedSeconds <= 0.0) return null
            return bytesDelta * 8.0 / elapsedSeconds / 1_000.0
        }
    }
}


private fun RTCStats.isAudioRtp(): Boolean {
    return members.string("kind") == "audio" || members.string("mediaType") == "audio"
}

private fun Map<String, Any>.long(name: String): Long? = (this[name] as? Number)?.toLong()
private fun Map<String, Any>.double(name: String): Double? = (this[name] as? Number)?.toDouble()
private fun Map<String, Any>.string(name: String): String? = this[name] as? String
private fun Map<String, Any>.boolean(name: String): Boolean? = this[name] as? Boolean
