package com.nova.app.core.auth

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NovaSessionExpiryPolicyTest {
    @Test
    fun unauthorizedIsTheOnlyTerminalRouteFailure() {
        assertTrue(shouldExpireNovaSession(401))

        listOf(null, 0, 400, 403, 404, 409, 429, 500).forEach { statusCode ->
            assertFalse(
                "status $statusCode must remain a reportable non-session failure",
                shouldExpireNovaSession(statusCode),
            )
        }
    }
}
