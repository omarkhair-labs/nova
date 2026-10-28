package com.nova.app.core.push

import org.junit.Assert.*
import org.junit.Test

class PlantupiaPushPolicyTest {
    @Test fun aPrivateAlertNeverTargetsASignedOutOrDifferentAccount() {
        assertTrue(authorizedPlantupiaPushRecipient(12, 12))
        assertFalse(authorizedPlantupiaPushRecipient(12, 13))
        assertFalse(authorizedPlantupiaPushRecipient(12, null))
        assertFalse(authorizedPlantupiaPushRecipient(null, null))
        assertFalse(authorizedPlantupiaPushRecipient(null, 12))
        assertFalse(authorizedPlantupiaPushRecipient(0, 0))
    }
}
