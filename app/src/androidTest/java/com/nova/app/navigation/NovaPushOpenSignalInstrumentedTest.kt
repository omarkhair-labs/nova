package com.nova.app.navigation

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nova.app.core.push.NovaPushOpenSignal
import com.nova.app.core.push.NovaPushTarget
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NovaPushOpenSignalInstrumentedTest {
    @Before
    fun clearBeforeTest() {
        NovaPushOpenSignal.consume()
    }

    @After
    fun clearAfterTest() {
        NovaPushOpenSignal.consume()
    }

    @Test
    fun nullAndEmptyIntentsDoNotCreateATarget() {
        NovaPushOpenSignal.offer(null)
        assertNull(NovaPushOpenSignal.pendingTarget)

        NovaPushOpenSignal.offer(Intent())
        assertNull(NovaPushOpenSignal.pendingTarget)
    }

    @Test
    fun socialNotificationExtrasAreParsedWithoutRenaming() {
        NovaPushOpenSignal.offer(
            Intent()
                .putExtra("kind", "comment")
                .putExtra("actor_username", "maya")
                .putExtra("actor_name", "Maya")
                .putExtra("actor_avatar_url", "https://example.test/maya.jpg")
                .putExtra("post_id", "42")
                .putExtra("conversation_id", "77"),
        )

        assertEquals(
            NovaPushTarget(
                kind = "comment",
                actorUsername = "maya",
                actorName = "Maya",
                actorAvatarUrl = "https://example.test/maya.jpg",
                postId = 42L,
                conversationId = 77L,
            ),
            NovaPushOpenSignal.pendingTarget,
        )
    }

    @Test
    fun invalidNumericExtrasRemainAbsentWithoutDroppingAValidKind() {
        NovaPushOpenSignal.offer(
            Intent()
                .putExtra("kind", "follow")
                .putExtra("post_id", "not-a-number")
                .putExtra("conversation_id", ""),
        )

        assertEquals(
            NovaPushTarget(
                kind = "follow",
                actorUsername = "",
                actorName = "",
                actorAvatarUrl = "",
                postId = null,
                conversationId = null,
            ),
            NovaPushOpenSignal.pendingTarget,
        )
    }

    @Test
    fun consumeClearsThePendingTarget() {
        NovaPushOpenSignal.offer(Intent().putExtra("kind", "follow"))

        NovaPushOpenSignal.consume()

        assertNull(NovaPushOpenSignal.pendingTarget)
    }
}
