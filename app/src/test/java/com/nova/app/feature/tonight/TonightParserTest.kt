package com.nova.app.feature.tonight

import com.nova.app.feature.tonight.data.resolveTonightPulseMediaUrls
import com.nova.app.feature.tonight.domain.model.TonightPerson
import com.nova.app.feature.tonight.domain.model.TonightPulse
import org.junit.Assert.assertEquals
import org.junit.Test


class TonightParserTest {
    @Test
    fun `video moment keeps the existing Pulse thumbnail contract`() {
        val media = resolveTonightPulseMediaUrls(
            rawMediaUrl = "/media/pulse.mp4",
            rawThumbnailUrl = "/media/pulse.jpg",
            resolveMediaUrl = { "https://nova.example$it" },
        )
        val pulse = TonightPulse(
            id = 9,
            author = TonightPerson(
                id = 7,
                username = "friend",
                name = "Friend",
                avatarUrl = "",
            ),
            mediaUrl = media.mediaUrl,
            thumbnailUrl = media.thumbnailUrl,
            mediaType = "video",
            audience = "followers",
            note = "Tonight",
            createdAt = "2026-08-27T18:00:00Z",
            expiresAt = "2026-08-28T06:00:00Z",
            isMine = false,
        )

        assertEquals("https://nova.example/media/pulse.mp4", pulse.mediaUrl)
        assertEquals("https://nova.example/media/pulse.jpg", pulse.thumbnailUrl)
        assertEquals(pulse.thumbnailUrl, pulse.previewUrl)
    }
}
