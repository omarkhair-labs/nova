package com.nova.app.feature.plants.data

import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaSpacePermissionPolicyTest {
    private fun space(id: String, permissionReported: Boolean, canCreate: Boolean = false) =
        PlantupiaSpace(
            id = id, name = "Home", description = "", location = "",
            canAddPlant = canCreate, canAddPlantPermissionReported = permissionReported,
        )

    @Test fun missingPermissionFieldIsNotConfusedWithServerDenial() {
        val text = addPlantAccessExplanation(listOf(space("s1", permissionReported = false)))
        assertTrue(text.contains("didn't confirm"))
        assertTrue(text.contains("preview API"))
    }

    @Test fun mixedPermissionWithoutWritableSpaceExplainsUnknownBackendState() {
        val text = addPlantAccessExplanation(listOf(
            space("s1", permissionReported = true),
            space("s2", permissionReported = false),
        ))
        assertTrue(text.contains("didn't confirm"))
    }

    @Test fun confirmedReadOnlyPermissionShowsRealNextAction() {
        val text = addPlantAccessExplanation(listOf(space("s1", permissionReported = true)))
        assertTrue(text.contains("manager access"))
    }

    @Test fun missingSpacesExplainsFirstStep() {
        assertTrue(addPlantAccessExplanation(emptyList()).contains("first private space"))
    }
}
