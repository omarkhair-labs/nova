package com.nova.app.feature.plants

import org.junit.Assert.*
import org.junit.Test

class PlantupiaCareEntryModeTest {
    @Test fun explicitModeMapsToDistinctRealEventKinds() {
        assertEquals("care", PlantupiaCareEntryMode.CARE.eventKind)
        assertEquals("observation", PlantupiaCareEntryMode.NOTE.eventKind)
        assertEquals("issue", PlantupiaCareEntryMode.CONCERN.eventKind)
    }

    @Test fun noteAndConcernRequireHumanEvidenceButNotARecordedWatering() {
        assertTrue(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.NOTE, false, true, true))
        assertTrue(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.CONCERN, false, true, true))
        assertFalse(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.NOTE, true, false, true))
        assertFalse(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.CONCERN, false, false, true))
    }

    @Test fun careChoiceIsStackedOnNarrowScreenOrLargeSystemText() {
        assertTrue(careEntryModesNeedStack(290f, 1f))
        assertTrue(careEntryModesNeedStack(390f, 2f))
        assertFalse(careEntryModesNeedStack(390f, 1f))
        assertTrue(careEntryModesNeedStack(330f, 1.3f))
    }

    @Test fun careRequiresAffirmativeActionAndNoInvalidCheckDate() {
        assertFalse(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.CARE, false, true, true))
        assertTrue(plantupiaCanSaveCareEntry(PlantupiaCareEntryMode.CARE, true, false, true))
        for (mode in PlantupiaCareEntryMode.entries) {
            assertFalse(plantupiaCanSaveCareEntry(mode, true, true, false))
        }
    }
}
