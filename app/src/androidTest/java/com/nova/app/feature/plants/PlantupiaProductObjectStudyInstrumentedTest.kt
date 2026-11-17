package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Real Compose semantics on an isolated in-memory catalog, no merchant mutation. */
class PlantupiaProductObjectStudyInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    private fun product(id: String, option: String, price: String, units: Int): JSONObject =
        JSONObject()
            .put("id", id)
            .put("product_id", "example-product")
            .put("name", "Example vessel")
            .put("description", "Review-only product data")
            .put("kind", "planter")
            .put("sku", "QA-" + id)
            .put("variant_label", option)
            .put("price", price)
            .put("in_stock", units > 0)
            .put("available_units", units)
            .put("has_image", false)
            .put("photos", JSONArray())

    @Test fun selectedRealSkuDeterminesAddAndUnavailableSkuCannotBeAdded() {
        val repo = PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val variants = listOf(
            product("sku-a", "Small", "250.00", 3),
            product("sku-b", "Large", "480.00", 2),
            product("sku-c", "Unavailable", "620.00", 0),
        )
        var submitted: Pair<String, Int>? = null
        compose.setContent {
            StoreProductDetails(
                productId = "example-product", variants = variants,
                cart = JSONObject().put("results", JSONArray()),
                repository = repo, busy = false,
                onAdd = { sku, quantity -> submitted = sku to quantity },
            )
        }
        compose.onNodeWithText("Product photo pending").assertExists()
        compose.onNodeWithText("CHOOSE YOUR OPTION").assertExists()
        compose.onNodeWithContentDescription("Selected price EGP 250.00").assertExists()
        compose.onNodeWithText("Large").performClick()
        compose.onNodeWithContentDescription("Selected price EGP 480.00").assertExists()
        compose.onNodeWithText("Add to cart").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals("sku-b" to 1, submitted) }
        compose.onNodeWithText("Unavailable").performClick()
        compose.onNodeWithText("Add to cart").assertIsNotEnabled()
        compose.onAllNodesWithText("Inspect photo").assertCountEquals(0)
    }

    @Test fun cartNotLoadedBlocksAddRatherThanPretendingSuccess() {
        val repo = PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        compose.setContent {
            StoreProductDetails(
                productId = "example-product",
                variants = listOf(product("sku-a", "Small", "250.00", 5)),
                cart = null, repository = repo, busy = false,
                onAdd = { _, _ -> throw AssertionError("Must not add without cart") },
            )
        }
        compose.onNodeWithText("Add to cart").assertIsNotEnabled()
        compose.onNodeWithText(
            "The cart is unavailable right now. Return to Collection to retry."
        ).assertExists()
    }
    @Test fun pendingAddIsNotSuccessAndAmbiguousFailureOffersCartCheck() {
        val repo = PlantupiaRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        var pending by mutableStateOf(true)
        var failure by mutableStateOf<Pair<String, String>?>(null)
        var cartChecks = 0
        compose.setContent {
            StoreProductDetails(
                productId = "example-product",
                variants = listOf(product("sku-a", "Small", "250.00", 4)),
                cart = JSONObject().put("results", JSONArray()),
                repository = repo,
                busy = pending,
                adding = pending,
                addErrorForSku = failure,
                onAdd = { _, _ -> throw AssertionError("Pending action must not re-submit") },
                onViewCart = { cartChecks++ },
            )
        }
        compose.onNodeWithText("Saving your selection…").assertIsNotEnabled()
        compose.onAllNodesWithText("Saved to your cart").assertCountEquals(0)
        compose.runOnIdle {
            pending = false
            failure = "sku-a" to "We couldn't verify the cart change. Check the saved cart."
        }
        compose.onNodeWithText(
            "We couldn't verify the cart change. Check the saved cart."
        ).assertExists()
        compose.onNodeWithText("Check saved cart  ↗").performClick()
        compose.runOnIdle { assertEquals(1, cartChecks) }
    }

}
