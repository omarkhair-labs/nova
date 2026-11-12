package com.nova.app.feature.plants.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlantupiaSpaceFocusTest {
    private val first = PlantupiaSpace("allowed-home", "Home", "", "")
    private val second = PlantupiaSpace("allowed-office", "Office", "", "")

    @Test fun keepsAnExplicitlySelectedAuthorizedSpace() {
        assertEquals(second, resolveFocusedSpace(listOf(first, second), second.id))
    }

    @Test fun revokedOrForeignSpaceCannotRemainSelected() {
        assertEquals(first, resolveFocusedSpace(listOf(first), second.id))
        assertEquals(first, resolveFocusedSpace(listOf(first), "guessed-other-customer"))
    }

    @Test fun emptyAccountNeverInventsASpace() {
        assertNull(resolveFocusedSpace(emptyList(), "guessed-other-customer"))
    }

    @Test fun scopedInventoryNeverMixesIdenticallyNamedPlantsAcrossSpaces() {
        fun plant(id: String, site: String) = PlantupiaPlant(
            id = id, spaceId = site, label = "same name", name = "same name",
            species = "", placement = "", coverPhoto = "", active = true,
            canRecord = true, lastEventKind = "", lastEventAt = "",
        )
        val entries = listOf(plant("p-home", first.id), plant("p-office", second.id))
        assertEquals(listOf("p-home"), findSpacePlants(entries, first.id, "").map { it.id })
        assertEquals(listOf("p-office"), findSpacePlants(entries, second.id, "").map { it.id })
    }
    @Test fun newPlantDefaultsToCurrentlyFocusedAuthorizedSpace() {
        assertEquals(second.id, chooseWritableSpaceId(listOf(first, second), second.id, first.id))
    }

    @Test fun deniedOrStalePreferredSpaceCannotBeAssignedToNewPlant() {
        assertEquals(first.id, chooseWritableSpaceId(listOf(first), second.id, first.id))
        assertNull(chooseWritableSpaceId(emptyList(), second.id, null))
    }

    @Test fun justCreatedServerSpaceIsValidUntilRefresh() {
        assertEquals("new-id", chooseWritableSpaceId(emptyList(), second.id, "new-id", "new-id"))
        assertNull(chooseWritableSpaceId(emptyList(), second.id, "revoked", "new-id"))
    }

    @Test fun spaceHeroDoesNotInventPlantActivity() {
        assertEquals("No plants added yet", spacePlantCountSummary(0))
        assertEquals("1 plant in this space", spacePlantCountSummary(1))
        assertEquals("7 plants in this space", spacePlantCountSummary(7))
        assertEquals("100 plants in this space", spacePlantCountSummary(100))
    }

    @Test fun homeHeroNeverLabelsPlantPhotoAsSitePhotography() {
        assertEquals("PLANT PHOTO · IN THIS SPACE", spaceHeroMediaEyebrow(true))
        assertEquals("LIVING SPACE", spaceHeroMediaEyebrow(false))
    }

}
