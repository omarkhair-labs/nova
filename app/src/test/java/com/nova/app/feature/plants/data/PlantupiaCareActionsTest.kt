package com.nova.app.feature.plants.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlantupiaCareActionsTest {
    @Test fun keepsFreeformCareWithoutInventingAnAction() {
        assertEquals("لاحظت الورق", buildPlantupiaCareNote(null, " لاحظت الورق "))
        assertEquals("", buildPlantupiaCareNote(null, "  "))
    }

    @Test fun recordsOnlyChosenHumanAction() {
        assertEquals("Watered the plant", buildPlantupiaCareNote(PlantupiaCareAction.WATERED, "  "))
        assertEquals(
            "Moved the plant to another spot\nبعيدًا عن التكييف",
            buildPlantupiaCareNote(PlantupiaCareAction.MOVED, " بعيدًا عن التكييف "),
        )
        assertTrue(PlantupiaCareAction.entries.none { it.recordedText.contains("must", ignoreCase = true) })
    }
}
