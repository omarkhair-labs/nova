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
        assertEquals("رويت النبات", buildPlantupiaCareNote(PlantupiaCareAction.WATERED, "  "))
        assertEquals(
            "نقلت النبات لمكان آخر\nبعيدًا عن التكييف",
            buildPlantupiaCareNote(PlantupiaCareAction.MOVED, " بعيدًا عن التكييف "),
        )
        assertTrue(PlantupiaCareAction.entries.none { it.recordedText.contains("يجب") })
    }
}
