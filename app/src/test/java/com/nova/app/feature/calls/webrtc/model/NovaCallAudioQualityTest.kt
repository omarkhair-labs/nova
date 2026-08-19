package com.nova.app.feature.calls.webrtc.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.webrtc.RTCStats
import org.webrtc.RTCStatsReport


class NovaCallAudioQualityTest {
    @Test
    fun parsesAudioRtpAndSelectedCandidatePair() {
        val report = RTCStatsReport(
            2_500_000L,
            mapOf(
                "in" to stat(
                    type = "inbound-rtp",
                    id = "in",
                    members = mapOf(
                        "kind" to "audio",
                        "bytesReceived" to 12_000L,
                        "packetsReceived" to 120L,
                        "packetsLost" to 4L,
                        "concealedSamples" to 800L,
                        "jitter" to 0.035,
                    ),
                ),
                "out" to stat(
                    type = "outbound-rtp",
                    id = "out",
                    members = mapOf(
                        "mediaType" to "audio",
                        "bytesSent" to 18_000L,
                        "packetsSent" to 160L,
                    ),
                ),
                "pair" to stat(
                    type = "candidate-pair",
                    id = "pair",
                    members = mapOf(
                        "state" to "succeeded",
                        "nominated" to true,
                        "currentRoundTripTime" to 0.120,
                        "availableOutgoingBitrate" to 96_000.0,
                    ),
                ),
            ),
        )

        val snapshot = NovaCallAudioQualitySnapshot.from(report)

        assertEquals(2_500_000L, snapshot.timestampUs)
        assertEquals(12_000L, snapshot.inboundBytes)
        assertEquals(120L, snapshot.inboundPackets)
        assertEquals(4L, snapshot.inboundPacketsLost)
        assertEquals(800L, snapshot.concealedSamples)
        assertEquals(35.0, snapshot.jitterMs!!, 0.001)
        assertEquals(18_000L, snapshot.outboundBytes)
        assertEquals(160L, snapshot.outboundPackets)
        assertEquals(120.0, snapshot.roundTripTimeMs!!, 0.001)
        assertEquals(96.0, snapshot.availableOutgoingBitrateKbps!!, 0.001)
    }

    @Test
    fun calculatesBitrateLossAndPacketProgressAcrossSamples() {
        val previous = NovaCallAudioQualitySnapshot(
            timestampUs = 1_000_000L,
            inboundBytes = 10_000L,
            inboundPackets = 100L,
            inboundPacketsLost = 10L,
            concealedSamples = 1_000L,
            jitterMs = 20.0,
            outboundBytes = 20_000L,
            outboundPackets = 200L,
            roundTripTimeMs = 80.0,
            availableOutgoingBitrateKbps = 128.0,
        )
        val current = previous.copy(
            timestampUs = 3_500_000L,
            inboundBytes = 20_000L,
            inboundPackets = 200L,
            inboundPacketsLost = 20L,
            concealedSamples = 1_250L,
            jitterMs = 45.0,
            outboundBytes = 40_000L,
            outboundPackets = 350L,
            roundTripTimeMs = 140.0,
        )

        val delta = NovaCallAudioQualityDelta.between(previous, current)

        assertEquals(32.0, delta.inboundKbps!!, 0.001)
        assertEquals(64.0, delta.outboundKbps!!, 0.001)
        assertEquals(100L, delta.inboundPacketsDelta)
        assertEquals(150L, delta.outboundPacketsDelta)
        assertEquals(250L, delta.concealedSamplesDelta)
        assertEquals(10.0 / 110.0 * 100.0, delta.packetLossPercent!!, 0.001)
        assertEquals(true, delta.inboundProgressed)
        assertEquals(true, delta.outboundProgressed)
    }

    @Test
    fun counterResetDoesNotProduceFakeNegativeQualityRates() {
        val previous = NovaCallAudioQualitySnapshot(
            timestampUs = 5_000_000L,
            inboundBytes = 50_000L,
            inboundPackets = 500L,
            inboundPacketsLost = 30L,
            concealedSamples = 2_000L,
            jitterMs = 20.0,
            outboundBytes = 60_000L,
            outboundPackets = 600L,
            roundTripTimeMs = 50.0,
            availableOutgoingBitrateKbps = 256.0,
        )
        val current = previous.copy(
            timestampUs = 7_500_000L,
            inboundBytes = 500L,
            inboundPackets = 5L,
            inboundPacketsLost = 0L,
            concealedSamples = 0L,
            outboundBytes = 700L,
            outboundPackets = 7L,
        )

        val delta = NovaCallAudioQualityDelta.between(previous, current)

        assertNull(delta.inboundKbps)
        assertNull(delta.outboundKbps)
        assertNull(delta.inboundPacketsDelta)
        assertNull(delta.outboundPacketsDelta)
        assertNull(delta.packetLossPercent)
    }

    private fun stat(type: String, id: String, members: Map<String, Any>): RTCStats {
        return RTCStats(1_000_000L, type, id, members)
    }
}
