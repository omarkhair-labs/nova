package com.nova.app.feature.plants

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MainActivity
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Opt-in real local integration test, never a mock service or production test.
 * A private fixture must be placed in the QA app's files/ceo-local-qa.json.
 * No credentials, raw code or private fixture is committed or printed.
 * Run the two stages separately so a second staff account can actually reply.
 */
class PlantupiaCeoJourneyInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var fixture: JSONObject
    private lateinit var repository: PlantupiaRepository
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun localAccountOnly() {
        val uri = URI(NovaMessagingRepository.PRODUCTION_API_URL)
        assumeTrue(context.packageName.endsWith(".qa") && uri.host in listOf("127.0.0.1", "10.0.2.2", "localhost"))
        val file = File(context.filesDir, "ceo-local-qa.json")
        assumeTrue("Requires a private disposable local fixture", file.exists())
        fixture = JSONObject(file.readText())
        require(fixture.getString("scope") == "LOCAL_CEO_QA_ONLY")
        val customer = fixture.getJSONObject("customer")
        val result = runBlocking { AuthRemoteDataSource(NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL))
            .login(customer.getString("email"), customer.getString("password")) }
        assertTrue("The real disposable customer login must succeed", result is ApiResult.Success)
        val store = NovaSessionStore(context)
        assertTrue(store.save(store.beginLogin(), (result as ApiResult.Success).value))
        repository = PlantupiaRepository(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @After fun closeOwnedActivity() { scenario?.close() }

    private fun node(text: String, substring: Boolean = false): SemanticsNodeInteraction {
        compose.waitUntil(45_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }
        return compose.onAllNodes(hasText(text, substring = substring)).onFirst()
    }

    private fun click(text: String, substring: Boolean = false) {
        node(text, substring)
        // A recent-observation heading can repeat a plant's name. Select the
        // actual interactive inventory row/button instead of that static label.
        val target = compose.onAllNodes(hasText(text, substring = substring) and hasClickAction()).onFirst()
        runCatching { target.performScrollTo() }
        target.performClick()
    }

    private fun type(label: String, value: String) {
        val target = node(label)
        runCatching { target.performScrollTo() }
        target.performTextReplacement(value)
        closeSoftKeyboard()
    }

    private fun openPlant(name: String) {
        val description = "Open plant: $name"
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
        val row = compose.onNodeWithContentDescription(description)
        row.performScrollTo().assertIsDisplayed().performClick()
    }

    private fun capture(name: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
        requireNotNull(bitmap)
        val directory = File(context.getExternalFilesDir(null), "ceo-evidence").apply { mkdirs() }
        File(directory, "$name.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    }

    @Test fun stage1_claimInspectPaywallAndCreateActualSupportRequest() {
        val plantsBefore = runBlocking { repository.plants() }
        assertTrue(plantsBefore is ApiResult.Success)
        if ((plantsBefore as ApiResult.Success).value.isEmpty()) {
            click("Connect your plants")
            type("Delivery code", fixture.getString("invitation"))
            click("Review my delivery")
            node("3 plants prepared").assertExists()
            click("Confirm and connect my plants")
            click("Open My Space")
        }
        val plants = runBlocking { repository.plants() }
        assertTrue(plants is ApiResult.Success)
        val actual = (plants as ApiResult.Success).value
        val expected = fixture.getJSONArray("plant_ids").let { array -> (0 until array.length()).map { array.getString(it) }.toSet() }
        assertEquals(expected, actual.map { it.id }.toSet())
        actual.forEach {
            val identity = node(it.name)
            runCatching { identity.performScrollTo() }
            identity.assertIsDisplayed()
        }
        capture("01-original-plants")
        openPlant("FICTIONAL QA supplied plant 01")
        click("Care guide")
        node("Reviewed guidance · version 1").assertExists()
        capture("02-sourced-guide")
        val plans = runBlocking { repository.demoGet("care-plans/") }
        assertTrue(plans is ApiResult.Success)
        val plan = (plans as ApiResult.Success).value.demoRows().first { it.getString("plant_name") == "FICTIONAL QA supplied plant 01" }
        if (!plan.getBoolean("enabled")) click("Opt into this inspection plan")
        click("Inspect or change the next check")
        type("What did you actually observe?", "Software QA tester records an inspection-flow check; no physical plant was measured or watered.")
        click("Record inspection in Living History")
        // Re-read after the acknowledged native action rather than inventing a local event.
        compose.waitUntil(30_000) {
            runBlocking { repository.historyPage(plan.getString("plant_id")) }.let { response ->
                response is ApiResult.Success && response.value.events.any { it.note.startsWith("Software QA tester records") }
            }
        }
        click("Ask Plantupia AI")
        node("Preview access · no payment").assertExists()
        capture("03-ai-paywall")
        val activate = compose.onAllNodes(hasText("Try this plan · no charge") and isEnabled()).onFirst()
        runCatching { activate.performScrollTo() }
        activate.performClick()
        node("The AI provider is not configured on this QA server.", true).assertExists()
        capture("04-honest-ai-unavailable")
        click("Close")
        click("Ask the team")
        node("Care, when you need it.").assertExists()
        val expert = compose.onAllNodes(hasText("Try this plan · no charge") and isEnabled()).onLast()
        runCatching { expert.performScrollTo() }
        expert.performClick()
        type("Request subject", "CEO QA inspection question")
        type("What do you want the team to know?", "Software QA customer asks the real operator to confirm this request is visible. No botanical diagnosis requested.")
        val consent = compose.onAllNodes(isToggleable()).onFirst()
        runCatching { consent.performScrollTo() }
        consent.performClick()
        click("Send request")
        node("CEO QA inspection question", true).assertExists()
        capture("05-awaiting-actual-staff")
    }

    @Test fun stage2_reopenActualStaffReplyAndPersistNoChargeOrder() {
        click("Plants")
        openPlant("FICTIONAL QA supplied plant 01")
        click("Ask the team")
        click("CEO QA inspection question", true)
        node("QA staff reply: I can see your explicitly shared request. This is an actual typed QA response, not botanical advice.").assertExists()
        capture("06-actual-staff-reply")
        click("Close")
        // Returning through the existing selected-plant Back action preserves My Space.
        click("← Back to space")
        click("Shop")
        val catalog = runBlocking { repository.demoGet("catalog/") }
        assertTrue(catalog is ApiResult.Success)
        val plantProduct = (catalog as ApiResult.Success).value.demoRows().first { it.getString("kind") == "plant" }
        val description = "View QA product: " + plantProduct.getString("name")
        try {
            compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
        } catch (failure: Throwable) {
            capture("diagnostic-qa-shop")
            compose.onAllNodes(isRoot()).printToLog("CeoQaShop")
            throw failure
        }
        val product = compose.onNodeWithContentDescription(description)
        runCatching { product.performScrollTo() }
        product.performClick()
        click("Add to cart")
        node("Preview order only · no charge or delivery").assertExists()
        val consent = compose.onAllNodes(isToggleable()).onFirst()
        runCatching { consent.performScrollTo() }
        consent.performClick()
        click("Place preview order")
        node("Preview order received", true).assertExists()
        capture("07-persisted-no-charge-order")
        val orders = runBlocking { repository.demoGet("demo/orders/") }
        assertTrue(orders is ApiResult.Success && orders.value.demoRows().isNotEmpty())
        scenario!!.recreate()
        click("Shop")
        click("My orders")
        node("charged: no", true).assertExists()
        capture("08-order-after-relaunch")
    }

    @Test fun stage3_readOnlyReopenAndRevokedPaywall() {
        click("Plants")
        val plants = runBlocking { repository.plants() }
        assertTrue(plants is ApiResult.Success)
        val actual = (plants as ApiResult.Success).value
        val expected = fixture.getJSONArray("plant_ids").let { array -> (0 until array.length()).map { array.getString(it) }.toSet() }
        assertEquals(expected, actual.map { it.id }.toSet())
        actual.forEach { node(it.name).let { row -> runCatching { row.performScrollTo() }; row.assertIsDisplayed() } }
        val width = context.resources.configuration.screenWidthDp
        capture("09-plant-identities-${width}dp")
        openPlant("FICTIONAL QA supplied plant 01")
        click("Care guide")
        node("Reviewed guidance · version 1").assertExists()
        capture("10-care-plan-${width}dp")
        click("Ask Plantupia AI")
        node("Care, when you need it.").assertExists()
        capture("11-revoked-paywall-${width}dp")
        click("Close")
        click("← Back to space")
        click("Shop")
        click("My orders")
        node("charged: no", true).assertExists()
        capture("12-orders-${width}dp")
    }
}
