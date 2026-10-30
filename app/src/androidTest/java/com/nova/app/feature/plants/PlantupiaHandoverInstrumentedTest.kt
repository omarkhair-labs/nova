package com.nova.app.feature.plants

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.plants.data.PlantupiaHandoverStore
import com.nova.app.ui.theme.NovaTheme
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

class PlantupiaHandoverInstrumentedTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val code = "synthetic_" + "x".repeat(43)
    @After fun clearIntent() { PlantupiaHandoverStore(context).clear() }

    @Test fun realKeystorePersistsEncryptedIntentAndSeparatesAccounts() {
        val first = PlantupiaHandoverStore(context)
        assertTrue(first.save(code, null))
        val restored = PlantupiaHandoverStore(context)
        assertEquals(code, restored.read(null)?.token)
        assertTrue(restored.save(code, 7))
        assertNull(PlantupiaHandoverStore(context).read(8))
        assertNull(PlantupiaHandoverStore(context).read(null))
        assertEquals(code, PlantupiaHandoverStore(context).read(7)?.token)
        val bytes = File(context.noBackupFilesDir, "plantupia-handover").readBytes()
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains(code))
    }

    @Test fun registrationContinuationPreservesCodeWithoutClaimingOrConfirming() {
        var registered = false
        var connected = false
        PlantupiaHandoverStore(context).clear()
        compose.setContent {
            NovaTheme {
                PlantupiaHandoverScreen(null, "", onLogin = {}, onRegister = { registered = true },
                    onClose = {}, onConnected = { connected = true })
            }
        }
        compose.onNodeWithText("Delivery code").performTextInput(code)
        compose.runOnIdle { assertEquals(code, PlantupiaHandoverStore(context).read(null)?.token) }
        compose.onNodeWithText("Create account and continue").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(registered)
            assertFalse(connected)
            assertEquals(code, PlantupiaHandoverStore(context).read(null)?.token)
        }
    }

    @Test fun notMyDeliveryRemovesPendingSecret() {
        PlantupiaHandoverStore(context).save(code, null)
        var closed = false
        compose.setContent {
            NovaTheme {
                PlantupiaHandoverScreen(null, "", onLogin = {}, onRegister = {},
                    onClose = { closed = true }, onConnected = {})
            }
        }
        compose.onNodeWithText("Not my delivery? Remove code").performScrollTo().performClick()
        compose.runOnIdle {
            assertTrue(closed)
            assertNull(PlantupiaHandoverStore(context).read(null))
        }
    }
}
