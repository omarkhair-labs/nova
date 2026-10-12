package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaCreateIntentTest {
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
