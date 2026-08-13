package com.nova.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class DeepLinkRouterTest {
    @Test
    fun directMessageUsesTheExistingConversationContract() {
        assertEquals(
            NovaDeepLinkDecision.Conversation(
                conversationId = 42L,
                username = "maya",
                displayName = "Maya",
                avatarUrl = "maya.jpg",
                kind = "direct",
                membersCount = 2,
            ),
            DeepLinkRouter.decide(
                NovaPushIntentData(
                    kind = "message",
                    conversationId = "42",
                    actorUsername = "maya",
                    actorName = "Maya",
                    actorAvatarUrl = "maya.jpg",
                )
            ),
        )
    }

    @Test
    fun groupMessageUsesTheExistingGroupDefaults() {
        assertEquals(
            NovaDeepLinkDecision.Conversation(
                conversationId = 73L,
                username = "group",
                displayName = "Nova group",
                avatarUrl = "",
                kind = "group",
                membersCount = 0,
            ),
            DeepLinkRouter.decide(
                NovaPushIntentData(
                    kind = "message",
                    conversationId = "73",
                    conversationKind = "group",
                )
            ),
        )
    }

    @Test
    fun everyReelActivityKindRoutesToTheNormalizedProfileReel() {
        listOf("reel_like", "reel_comment", "reel_repost", "reel_reply").forEach { kind ->
            assertEquals(
                NovaDeepLinkDecision.ProfileReel(
                    username = "maya",
                    initialReelId = 91L,
                ),
                DeepLinkRouter.decide(
                    NovaPushIntentData(
                        kind = kind,
                        reelId = "91",
                        reelAuthorUsername = "  MAYA  ",
                    )
                ),
            )
        }
    }

    @Test
    fun incompleteSpecialRoutesPreserveTheInAppFallback() {
        val incomplete = listOf(
            NovaPushIntentData(kind = "message", conversationId = "0", actorUsername = "maya"),
            NovaPushIntentData(kind = "message", conversationId = "42"),
            NovaPushIntentData(kind = "reel_like", reelId = "91"),
            NovaPushIntentData(kind = "reel_reply", reelId = "not-a-number", reelAuthorUsername = "maya"),
            NovaPushIntentData(kind = "follow", actorUsername = "maya"),
            NovaPushIntentData(),
        )

        incomplete.forEach { data ->
            assertEquals(NovaDeepLinkDecision.InAppSignal, DeepLinkRouter.decide(data))
        }
    }
}
