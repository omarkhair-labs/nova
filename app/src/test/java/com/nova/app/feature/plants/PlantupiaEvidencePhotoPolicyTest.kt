package com.nova.app.feature.plants

import androidx.compose.ui.layout.ContentScale
import org.junit.Assert.assertEquals
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

    @Test fun coverCropNeverBecomesTheDocumentaryEvidenceTreatment() {
        assertEquals(ContentScale.Crop, PlantupiaPhotoFraming.COVER.contentScale)
        assertEquals(ContentScale.Fit, PlantupiaPhotoFraming.FULL_RECORD.contentScale)
    }
}
