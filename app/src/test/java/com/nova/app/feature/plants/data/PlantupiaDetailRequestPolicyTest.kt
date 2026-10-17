package com.nova.app.feature.plants.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaDetailRequestPolicyTest {
    @Test fun onlyLatestResponseMayUpdateTheSelectedPlant() {
        assertTrue(isCurrentPlantDetailRequest("office-plant", 4, "office-plant", 4))
        assertFalse(isCurrentPlantDetailRequest("office-plant", 4, "home-plant", 5))
    }

    @Test fun backNavigationInvalidatesOutstandingHistory() {
        assertFalse(isCurrentPlantDetailRequest("office-plant", 4, null, 5))
    }

    @Test fun reopeningSamePlantDoesNotAcceptPriorHistoryPage() {
        assertFalse(isCurrentPlantDetailRequest("office-plant", 4, "office-plant", 6))
        assertTrue(isCurrentPlantDetailRequest("office-plant", 6, "office-plant", 6))
    }

    @Test fun emptyOrMissingPlantNeverAuthorizesAResponse() {
        assertFalse(isCurrentPlantDetailRequest("", 1, "", 1))
        assertFalse(isCurrentPlantDetailRequest("plant", 1, null, 1))
    }
}
