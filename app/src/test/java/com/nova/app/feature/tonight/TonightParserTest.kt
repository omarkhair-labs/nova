package com.nova.app.feature.tonight

import com.nova.app.feature.tonight.data.parseTonightSnapshot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test


class TonightParserTest {
    @Test
    fun `video moment keeps the existing Pulse thumbnail contract`() {
        val snapshot = parseTonightSnapshot(
            JSONObject(
                """
                {
                  "is_tonight": true,
                  "people": [{
                    "person": {"id": 7, "username": "friend", "name": "Friend", "avatar_url": ""},
                    "moments_count": 1,
                    "latest_pulse": {
                      "id": 9,
                      "author": {"id": 7, "username": "friend", "name": "Friend", "avatar_url": ""},
                      "media_url": "/media/pulse.mp4",
                      "thumbnail_url": "/media/pulse.jpg",
                      "media_type": "video",
                      "audience": "followers",
                      "note": "Tonight",
                      "created_at": "2026-08-27T18:00:00Z",
                      "expires_at": "2026-08-28T06:00:00Z",
                      "is_mine": false
                    }
                  }]
                }
                """.trimIndent(),
            ),
            resolveMediaUrl = { "https://nova.example$it" },
        )

        val pulse = snapshot.people.single().latestPulse
        assertEquals("https://nova.example/media/pulse.jpg", pulse.thumbnailUrl)
        assertEquals(pulse.thumbnailUrl, pulse.previewUrl)
    }
}
