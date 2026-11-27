package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaSignaturePolicyTest {
    @Test fun selectionDoesNotVerifyASpecimen() {
        assertEquals("Species selected by you · individual plant not verified", signatureIdentityLabel("user_selected", false))
        assertEquals("Species not confirmed", signatureIdentityLabel("unknown", false))
        assertEquals("Species not confirmed", signatureIdentityLabel("unexpected", true))
        assertTrue(signatureIdentityLabel("staff_verified", true).contains("QA specimen"))
        assertEquals("Botanical identity verified by Plantupia", signatureIdentityLabel("staff_verified", false))
    }

    @Test fun unknownTypeIsAnExplicitIndependentRoute() {
        assertFalse(signatureTypeChosen(null, false))
        assertFalse(signatureTypeChosen(" ", false))
        assertTrue(signatureTypeChosen(null, true))
        assertTrue(signatureTypeChosen("reviewed-key", false))
    }

    @Test fun previewAndClaimAreDifferentMoments() {
        assertEquals(SignatureDeliveryStage.SCAN, signatureDeliveryStage(false, false, null))
        assertEquals(SignatureDeliveryStage.REVIEW, signatureDeliveryStage(true, false, null))
        assertEquals(SignatureDeliveryStage.REVIEW, signatureDeliveryStage(true, true, null))
        assertEquals(SignatureDeliveryStage.REVIEW, signatureDeliveryStage(true, true, ""))
        assertEquals(SignatureDeliveryStage.REVIEW, signatureDeliveryStage(true, false, "space"))
        assertEquals(SignatureDeliveryStage.CONNECTED, signatureDeliveryStage(true, true, "space"))
        assertEquals(SignatureDeliveryStage.SCAN, signatureDeliveryStage(false, true, "space"))
    }
}
