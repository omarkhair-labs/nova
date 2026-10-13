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
}
