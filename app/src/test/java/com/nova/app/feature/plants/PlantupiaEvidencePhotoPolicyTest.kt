package com.nova.app.feature.plants

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaEvidencePhotoPolicyTest {
    @Test fun missingOrUnloadedSourceCannotOpenFullScreen() {
        assertFalse(canOpenPlantupiaPhoto("", false))
        assertFalse(canOpenPlantupiaPhoto("", true))
        assertFalse(canOpenPlantupiaPhoto(" ", true))
        assertFalse(canOpenPlantupiaPhoto("https://media.example.test/plant.png", false))
    }

    @Test fun onlyLoadedRealPhotoSourceCanOpenFullScreen() {
        assertTrue(canOpenPlantupiaPhoto("content://media/external/images/media/123", true))
        assertTrue(canOpenPlantupiaPhoto("https://media.example.test/plant.png", true))
    }
}
