package com.nova.app.feature.plants.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaSpaceBrowserTest {
    private fun plant(
        id: String,
        space: String = "office",
        name: String,
        species: String = "",
        placement: String = "",
        active: Boolean = true,
        lastEventAt: String = "",
    ) = PlantupiaPlant(
        id = id,
        spaceId = space,
        label = name,
        name = name,
        species = species,
        placement = placement,
        coverPhoto = "",
        active = active,
        canRecord = true,
        lastEventKind = "",
        lastEventAt = lastEventAt,
    )

    @Test fun filtersBySiteAndOnlyActiveUnits() {
        val plants = listOf(
            plant("a", name = "Ficus", space = "office"),
            plant("b", name = "Fern", space = "home"),
            plant("c", name = "Retired", space = "office", active = false),
        )
        assertEquals(listOf("a"), findSpacePlants(plants, "office", "").map { it.id })
    }

    @Test fun searchesAliasSpeciesAndPlacementWithoutCaseSensitivity() {
        val plants = listOf(
            plant("a", name = "Maya", species = "Monstera", placement = "Reception"),
            plant("b", name = "Nile", species = "Ficus", placement = "Atrium"),
        )
        assertEquals(listOf("a"), findSpacePlants(plants, "office", "  MONSTera  ").map { it.id })
        assertEquals(listOf("a"), findSpacePlants(plants, "office", "reception").map { it.id })
        assertEquals(listOf("b"), findSpacePlants(plants, "office", "nile").map { it.id })
        assertTrue(findSpacePlants(plants, "office", "bedroom").isEmpty())
    }

    @Test fun ordersByLatestRealEventNotGuesswork() {
        val plants = listOf(
            plant("a", name = "Newer", lastEventAt = "2026-10-09T10:00:00Z"),
            plant("b", name = "Old", lastEventAt = "2026-02-01T10:00:00Z"),
            plant("c", name = "No events"),
        )
        assertEquals(listOf("a", "b", "c"), findSpacePlants(plants, "office", "").map { it.id })
    }
}
