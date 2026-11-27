package com.nova.app.feature.plants

import org.junit.Assert.assertEquals
import org.junit.Test

class PlantupiaCareLayoutPolicyTest {
    @Test fun narrowEditorsUseTwoColumnsInsteadOfCompressingLabels() {
        listOf(268f, 312f, 342f, 379f).forEach { width ->
            assertEquals(2, plantupiaCareActionColumns(width, 1f))
        }
    }

    @Test fun generousWidthMayUseThreeColumnsAtRegularScale() {
        assertEquals(3, plantupiaCareActionColumns(380f, 1f))
        assertEquals(3, plantupiaCareActionColumns(460f, 1.15f))
    }

    @Test fun enlargedSystemTextAlwaysUsesAtLeastHalfWidthCards() {
        listOf(1.3f, 1.5f, 2f).forEach { scale ->
            assertEquals(2, plantupiaCareActionColumns(500f, scale))
            assertEquals(2, plantupiaCareActionColumns(312f, scale))
        }
    }
}
