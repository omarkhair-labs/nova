package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import com.nova.app.feature.plants.data.PlantupiaPlant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaCreateIntentTest {
    private fun plant(id: String, active: Boolean, canRecord: Boolean) = PlantupiaPlant(
        id = id,
        spaceId = "space-$id",
        label = "My plant",
        name = "Plant $id",
        species = "",
        placement = "",
        coverPhoto = "",
        active = active,
        canRecord = canRecord,
        lastEventKind = "",
        lastEventAt = "",
    )

    @Test fun quickPlantPickerFiltersOnlyWritableActiveAndMatchesMultilingualNames() {
        val plants = listOf(
            plant("editable", true, true).copy(name = "Monstera", species = "Deliciosa"),
            plant("arabic", true, true).copy(name = "نعناع", placement = "الشرفة"),
            plant("viewer", true, false).copy(name = "Monstera read-only"),
            plant("archived", false, true).copy(name = "Monstera archived"),
        )
        assertEquals(listOf("editable"), filterIntentPlants(plants, "MONSTER").map { it.id })
        assertEquals(listOf("arabic"), filterIntentPlants(plants, "نعناع").map { it.id })
        assertEquals(listOf("arabic"), filterIntentPlants(plants, "الشرفة").map { it.id })
        assertEquals(listOf("editable", "arabic"), filterIntentPlants(plants, "").map { it.id })
        assertTrue(filterIntentPlants(plants, "does not exist").isEmpty())
    }

    @Test fun careAndQuestionTargetOnlyAuthorizedActivePlants() {
        val result = actionablePlantChoices(
            listOf(
                plant("owner", active = true, canRecord = true),
                plant("view-only", active = true, canRecord = false),
                plant("archived", active = false, canRecord = true),
            ),
        )
        assertEquals(listOf("owner"), result.map { it.id })
    }

    @Test fun readOnlyAccountCannotAutoOpenCareEditor() {
        assertTrue(actionablePlantChoices(listOf(plant("viewer", true, false))).isEmpty())
    }

    @Test fun newPlantInEmptyAccountStartsWithPrivateSpace() {
        assertEquals(
            PlantupiaCreationForm.SPACE,
            PlantupiaCreateMode.ADD_PLANT.creationForm(hasSpaces = false),
        )
    }

    @Test fun newPlantWithSpaceOpensRealPlantForm() {
        assertEquals(
            PlantupiaCreationForm.PLANT,
            PlantupiaCreateMode.ADD_PLANT.creationForm(hasSpaces = true),
        )
    }

    @Test fun newSpaceAlwaysOpensSpaceForm() {
        assertEquals(
            PlantupiaCreationForm.SPACE,
            PlantupiaCreateMode.ADD_SPACE.creationForm(hasSpaces = true),
        )
    }

    @Test fun careAndPublicQuestionsMustSelectAnExistingPlant() {
        assertTrue(PlantupiaCreateMode.OBSERVATION.needsPlantSelection())
        assertTrue(PlantupiaCreateMode.QUESTION.needsPlantSelection())
        assertEquals(
            PlantupiaCreationForm.NONE,
            PlantupiaCreateMode.QUESTION.creationForm(hasSpaces = false),
        )
        assertFalse(PlantupiaCreateMode.ADD_PLANT.needsPlantSelection())
        assertFalse(PlantupiaCreateMode.ADD_SPACE.needsPlantSelection())
        assertFalse((null as PlantupiaCreateMode?).needsPlantSelection())
    }
}
