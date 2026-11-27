package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaInspectionChoiceStyleTest {
    @Test fun observedOutcomesHaveAccessibleLabelsAndHonestVisualTopics() {
        listOf("soil", "leaves", "drainage", "rotation").forEach { action ->
            val choices = inspectionFindingChoices(action)
            assertTrue(choices.isNotEmpty())
            assertEquals(choices.size, choices.map { it.first }.distinct().size)
            choices.forEach { (value, label) ->
                assertTrue(label.isNotBlank())
                assertFalse(label.contains("Watered", ignoreCase = true))
                assertFalse(label.contains("Fertilized", ignoreCase = true))
                assertTrue(inspectionChoiceStyle(value).chapter.isNotBlank())
            }
        }
        assertEquals("inspection", inspectionChoiceStyle("unsure").chapter)
        assertEquals("soil", inspectionChoiceStyle("dry").chapter)
        assertEquals("water", inspectionChoiceStyle("moist").chapter)
        assertEquals("warnings", inspectionChoiceStyle("leaves_changed").chapter)
    }
}
