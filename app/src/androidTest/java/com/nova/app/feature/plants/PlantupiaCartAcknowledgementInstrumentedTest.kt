package com.nova.app.feature.plants

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Pure visual/interaction fixture; never submits to a real merchant account. */
class PlantupiaCartAcknowledgementInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun receipt() = ShopCartReceipt(
        productId = "faux-id", skuId = "fake-sku",
        productName = "FICTIONAL QA plant", optionName = "Medium",
        skuCode = "QA-123", quantity = 2,
        price = "350.00", lineTotal = "700.00",
        cartSubtotal = "950.00", photoId = null,
        hasPhoto = false, photoAlt = "",
    )

    @Test fun acknowledgedSheetHasTwoRealChoicesAndNoFalseReservation() {
        val repository = PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        var continued = 0
        var viewed = 0
        compose.setContent {
            MaterialTheme {
                PlantupiaCartReceiptSheet(
                    receipt = receipt(), repository = repository,
                    onContinue = { continued++ },
                    onViewCart = { viewed++ },
                )
            }
        }
        compose.onNodeWithText("Saved to your cart").assertExists()
        compose.onNodeWithText("FICTIONAL QA plant").assertExists()
        compose.onNodeWithText("Medium").assertExists()
        compose.onNodeWithText("Quantity in cart · 2").assertExists()
        compose.onNodeWithText("Saved cart subtotal · EGP 950.00").assertExists()
        compose.onNodeWithText(
            "No stock is reserved yet. Delivery is quoted and confirmed separately."
        ).assertExists()
        compose.onNodeWithText("View your cart  ↗").performClick()
        compose.runOnIdle {
            assertEquals(1, viewed)
            assertEquals(0, continued)
        }
    }

    @Test fun continueExploringIsASeparateDismissChoice() {
        val repository = PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        var continued = 0
        compose.setContent {
            MaterialTheme {
                PlantupiaCartReceiptSheet(receipt(), repository, onContinue = { continued++ },
                    onViewCart = { throw AssertionError("Must not open cart") })
            }
        }
        compose.onNodeWithText("Continue exploring").performClick()
        compose.runOnIdle { assertEquals(1, continued) }
    }
}
