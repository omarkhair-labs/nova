package com.nova.app.feature.plants

import com.nova.app.feature.plants.data.PlantupiaQuestion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Labels show actual public replies, never imply advice or an outcome exists. */
class PlantupiaCirclePresentationTest {
    @Test fun replyCopyIsHonestAtAllInventorySizes() {
        assertEquals("0 replies", circleReplyLabel(0))
        assertEquals("1 reply", circleReplyLabel(1))
        assertEquals("2 replies", circleReplyLabel(2))
        assertEquals("100 replies", circleReplyLabel(100))
    }

    @Test fun malformedNegativeReplyCountNeverAppearsToPeople() {
        assertEquals("0 replies", circleReplyLabel(-1))
    }

    @Test fun publicQuestionAuthorArtworkRequiresAnActiveKnownIdentity() {
        val publicQuestion = PlantupiaQuestion(
            id = "q", postId = 42L, topic = "Care question", caption = "What changed?",
            imageUrl = "", authorName = "Person", comments = 0, resolved = false,
            isMine = false, authorId = 33L, authorAvatarVariant = 2,
        )
        assertTrue(canDisplayQuestionAuthorPortrait(publicQuestion))
        assertFalse(canDisplayQuestionAuthorPortrait(publicQuestion.copy(authorDeleted = true)))
        assertFalse(canDisplayQuestionAuthorPortrait(publicQuestion.copy(authorId = 0L)))
    }
}
