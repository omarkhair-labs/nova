package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantupiaSpaceSwitcherTest {
    @Test fun singlePlantCountIsGrammatical() {
        assertEquals("0 plants", spacePlantCountLabel(0))
        assertEquals("1 plant", spacePlantCountLabel(1))
        assertEquals("2 plants", spacePlantCountLabel(2))
        assertEquals("100 plants", spacePlantCountLabel(100))
    }
}
