package com.nova.app.navigation

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MessagesActivity
import com.nova.app.ReelsActivity
import com.nova.app.core.messaging.NovaMessagingNavigator
import com.nova.app.core.reels.NovaReelsNavigator
import com.nova.app.feature.messages.MessagesRouteArgs
import com.nova.app.feature.messages.MessagesRouteFactory
import com.nova.app.feature.reels.ReelsRouteArgs
import com.nova.app.feature.reels.ReelsRouteFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SpecialEntryIntentContractInstrumentedTest {
    @Test
    fun directConversationPreservesAllSpecialEntryExtras() {
        val context = CapturingContext(targetContext())

        NovaMessagingNavigator.openConversation(
            context = context,
            conversationId = 91L,
            username = "team",
            displayName = "Nova Team",
            avatarUrl = "https://example.test/team.jpg",
            kind = "group",
            membersCount = 7,
            currentUserRole = "admin",
        )

        val intent = requireNotNull(context.startedIntent)
        assertEquals(MessagesActivity::class.java.name, intent.component?.className)
        assertEquals(91L, intent.getLongExtra(NovaMessagingNavigator.EXTRA_CONVERSATION_ID, -1L))
        assertEquals("team", intent.getStringExtra(NovaMessagingNavigator.EXTRA_USERNAME))
        assertEquals("Nova Team", intent.getStringExtra(NovaMessagingNavigator.EXTRA_DISPLAY_NAME))
        assertEquals("https://example.test/team.jpg", intent.getStringExtra(NovaMessagingNavigator.EXTRA_AVATAR_URL))
        assertEquals("group", intent.getStringExtra(NovaMessagingNavigator.EXTRA_KIND))
        assertEquals(7, intent.getIntExtra(NovaMessagingNavigator.EXTRA_MEMBERS_COUNT, -1))
        assertEquals("admin", intent.getStringExtra(NovaMessagingNavigator.EXTRA_CURRENT_USER_ROLE))
        assertEquals(
            MessagesRouteArgs(
                id = 91L,
                username = "team",
                displayName = "Nova Team",
                avatarUrl = "https://example.test/team.jpg",
                kind = "group",
                membersCount = 7,
                currentUserRole = "admin",
            ),
            MessagesRouteFactory.fromIntent(intent),
        )
    }

    @Test
    fun blankConversationDisplayNameFallsBackToUsername() {
        val context = CapturingContext(targetContext())

        NovaMessagingNavigator.openConversation(
            context = context,
            conversationId = 12L,
            username = "maya",
            displayName = "",
            avatarUrl = "",
        )

        assertEquals(
            "maya",
            context.startedIntent?.getStringExtra(NovaMessagingNavigator.EXTRA_DISPLAY_NAME),
        )
        assertEquals(
            "direct",
            context.startedIntent?.getStringExtra(NovaMessagingNavigator.EXTRA_KIND),
        )
        assertEquals(
            2,
            context.startedIntent?.getIntExtra(NovaMessagingNavigator.EXTRA_MEMBERS_COUNT, -1),
        )
    }

    @Test
    fun profileReelNormalizesUsernameAndPreservesInitialId() {
        val context = CapturingContext(targetContext())

        NovaReelsNavigator.openProfile(
            context = context,
            username = "  MAYA  ",
            initialReelId = 55L,
        )

        val intent = requireNotNull(context.startedIntent)
        assertEquals(ReelsActivity::class.java.name, intent.component?.className)
        assertEquals("maya", intent.getStringExtra(ReelsActivity.EXTRA_PROFILE_USERNAME))
        assertEquals(55L, intent.getLongExtra(ReelsActivity.EXTRA_INITIAL_REEL_ID, -1L))
        assertEquals(
            ReelsRouteArgs.Profile("maya", 55L),
            ReelsRouteFactory.fromIntent(intent),
        )
    }

    @Test
    fun invalidProfileReelDoesNotStartAnActivity() {
        val context = CapturingContext(targetContext())

        NovaReelsNavigator.openProfile(context, " ", 55L)
        assertNull(context.startedIntent)

        NovaReelsNavigator.openProfile(context, "maya", 0L)
        assertNull(context.startedIntent)
    }

    private fun targetContext(): Context =
        InstrumentationRegistry.getInstrumentation().targetContext

    private class CapturingContext(base: Context) : ContextWrapper(base) {
        var startedIntent: Intent? = null

        override fun startActivity(intent: Intent) {
            startedIntent = intent
        }
    }
}
