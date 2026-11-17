package com.nova.app.feature.plants

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** No account credentials, merchant data changes or actual network actions. */
class PlantupiaCartExperienceInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private val repo by lazy {
        PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
    }

    private fun savedCart() = JSONObject()
        .put("results", JSONArray().put(JSONObject()
            .put("product", JSONObject()
                .put("id", "qa-sku-one")
                .put("product_id", "qa-object")
                .put("kind", "plant")
                .put("name", "FICTIONAL QA plant")
                .put("variant_label", "Pot 16 cm")
                .put("sku", "QA-PLANT-16")
                .put("price", "380.00")
                .put("available_units", 4)
                .put("in_stock", true)
                .put("has_image", false)
                .put("photos", JSONArray()))
            .put("quantity", 2)
            .put("line_total", "760.00")
            .put("available", true)))
        .put("subtotal", "760.00")
        .put("all_items_available", true)
        .put("can_request_shipping_quote", true)

    @Test fun cartShowsObjectPhotoPlaceholderExactOptionAndQuantityControls() {
        val changes = mutableListOf<Int>()
        var opened = ""
        var removed = ""
        compose.setContent {
            MaterialTheme {
                PlantupiaCartExperience(
                    cart = savedCart(), repository = repo, busy = false, pendingSku = null,
                    removed = null, error = null, notice = null,
                    onQuantityChange = { _, next -> changes += next },
                    onRemove = { removed = it },
                    onRestore = {},
                    onRefresh = {},
                    onOpenProduct = { opened = it },
                    onContinueShopping = {},
                    onDeliveryDetails = {},
                )
            }
        }
        compose.onNodeWithText("Product photo pending").assertExists()
        compose.onNodeWithText("FICTIONAL QA plant").performClick()
        compose.runOnIdle { assertEquals("qa-object", opened) }
        compose.onNodeWithText("Pot 16 cm").assertExists()
        compose.onNodeWithText("EGP 380.00 each").assertExists()
        compose.onNodeWithText("Items subtotal").assertExists()
        compose.onAllNodesWithText("EGP 760.00").assertCountEquals(2)
        compose.onNodeWithContentDescription("Increase quantity of FICTIONAL QA plant").performClick()
        compose.runOnIdle { assertEquals(listOf(3), changes) }
        compose.onNodeWithText("Remove").performClick()
        compose.runOnIdle { assertEquals("qa-sku-one", removed) }
    }

    @Test fun savingAndCheckoutDoesNotClaimPaymentOrReservation() {
        var delivery = 0
        compose.setContent {
            MaterialTheme {
                PlantupiaCartExperience(
                    cart = savedCart(), repository = repo, busy = false,
                    pendingSku = "qa-sku-one", removed = null,
                    error = null, notice = null,
                    onQuantityChange = { _, _ -> throw AssertionError("Busy cart must not update") },
                    onRemove = { throw AssertionError("Busy cart must not remove") },
                    onRestore = {}, onRefresh = {}, onOpenProduct = {},
                    onContinueShopping = {},
                    onDeliveryDetails = { delivery++ },
                )
            }
        }
        compose.onNodeWithText("Saving…").assertExists()
        compose.onNodeWithText("Continue to delivery details  ↗").assertIsNotEnabled()
        compose.onNodeWithText(
            "Delivery is not included. Plantupia will quote its cost before you decide whether to confirm. No payment or stock reservation happens here."
        ).assertExists()
        compose.runOnIdle { assertEquals(0, delivery) }
    }

    @Test fun removedSavedObjectCanBeRestoredAsASeparateBackendAction() {
        var restore = 0
        compose.setContent {
            MaterialTheme {
                PlantupiaCartExperience(
                    cart = JSONObject().put("results", JSONArray())
                        .put("subtotal", "0.00").put("can_request_shipping_quote", false),
                    repository = repo, busy = false, pendingSku = null,
                    removed = ShopRemovedCartObject("qa-sku-one", 2, "FICTIONAL QA plant"),
                    error = null, notice = null, onQuantityChange = { _, _ -> },
                    onRemove = {}, onRestore = { restore++ },
                    onRefresh = {}, onOpenProduct = {},
                    onContinueShopping = {}, onDeliveryDetails = {},
                )
            }
        }
        compose.onNodeWithText("Removed from your saved cart").assertExists()
        compose.onNodeWithText("Restore saved selection  ↗").performClick()
        compose.runOnIdle { assertEquals(1, restore) }
        compose.onAllNodesWithText("Continue to delivery details  ↗").assertCountEquals(0)
    }

    @Test fun unavailableCartLineBlocksDeliveryAndIncrease() {
        val cart = savedCart()
        cart.getJSONArray("results").getJSONObject(0)
            .put("available", false)
            .put("notice", "Item not available")
        cart.put("can_request_shipping_quote", false)
        cart.put("all_items_available", false)
        compose.setContent {
            MaterialTheme {
                PlantupiaCartExperience(cart, repo, busy = false, pendingSku = null,
                    removed = null, error = null, notice = null,
                    onQuantityChange = { _, _ -> throw AssertionError("Stock must block increase") },
                    onRemove = {}, onRestore = {}, onRefresh = {},
                    onOpenProduct = {}, onContinueShopping = {},
                    onDeliveryDetails = { throw AssertionError("Not allowed") })
            }
        }
        compose.onNodeWithText("Item not available").assertExists()
        compose.onNodeWithText("Continue to delivery details  ↗").assertIsNotEnabled()
    }
}
