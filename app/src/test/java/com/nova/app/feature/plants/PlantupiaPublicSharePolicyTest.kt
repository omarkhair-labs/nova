package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaPublicSharePolicyTest {
    @Test fun requiresAnExplicitlySelectedPhotoForCurrentNovaPostEngine() {
        assertFalse(PlantupiaPublicShareDraft("Leaves", "What changed?", false).canReview)
        assertTrue(PlantupiaPublicShareDraft("Leaves", "What changed?", true).canReview)
    }

    @Test fun rejectsBlankOrOversizedPublicText() {
        assertFalse(PlantupiaPublicShareDraft("  ", "A question", true).canReview)
        assertFalse(PlantupiaPublicShareDraft("Issue", "  ", true).canReview)
        assertFalse(PlantupiaPublicShareDraft("x".repeat(81), "A question", true).canReview)
        assertFalse(PlantupiaPublicShareDraft("Issue", "x".repeat(501), true).canReview)
        assertTrue(PlantupiaPublicShareDraft("x".repeat(80), "x".repeat(500), true).canReview)
    }

    @Test fun trimsOnlyUserAuthoredPublicPayload() {
        val draft = PlantupiaPublicShareDraft("  A leaf  ", "  What changed?  ", true)
        assertTrue(draft.canReview)
        assertEquals("A leaf", draft.cleanTopic)
        assertEquals("What changed?", draft.cleanQuestion)
    }
}
