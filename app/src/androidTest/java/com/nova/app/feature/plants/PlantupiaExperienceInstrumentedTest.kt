package com.nova.app.feature.plants

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.MainActivity
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.network.*
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.io.File
import java.net.URI
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.Assume.assumeTrue

/** Real local integration only. No provider mocks, hosted origin, production package or embedded credentials. */
class PlantupiaExperienceInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var fixture: JSONObject
    private lateinit var repository: PlantupiaRepository
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun requireOwnedLocalFixture() {
        val uri = URI(NovaMessagingRepository.PRODUCTION_API_URL)
        assumeTrue(context.packageName.endsWith(".qa") && uri.host in listOf("127.0.0.1", "10.0.2.2", "localhost"))
        val file = File(context.filesDir, "experience-local-qa.json")
        assumeTrue("Requires the private disposable local fixture", file.exists())
        fixture = JSONObject(file.readText())
        require(fixture.getString("scope") == "LOCAL_CEO_QA_ONLY")
        login("customer")
    }
    private fun login(role: String) {
        scenario?.close()
        val account = fixture.getJSONObject(role)
        val response = runBlocking { AuthRemoteDataSource(NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL)).login(account.getString("email"), account.getString("password")) }
        assertTrue(response is ApiResult.Success)
        val store = NovaSessionStore(context)
        assertTrue(store.save(store.beginLogin(), (response as ApiResult.Success).value))
        repository = PlantupiaRepository(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
    @After fun closeOwnedActivity() { scenario?.close() }
    private fun node(text: String, substring: Boolean = false): SemanticsNodeInteraction {
        compose.waitUntil(30_000) { compose.onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() }
        return compose.onAllNodes(hasText(text, substring = substring)).onFirst()
    }
    private fun click(text: String, substring: Boolean = false) {
        node(text, substring)
        val target = compose.onAllNodes(hasText(text, substring = substring) and hasClickAction()).onFirst()
        runCatching { target.performScrollTo() }
        target.performClick()
    }
    private fun root(label: String) {
        val matcher = hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        compose.waitUntil(30_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(matcher).onFirst().assertIsDisplayed().performClick()
    }
    private fun openPlant() {
        val description = "Open plant: FICTIONAL QA supplied plant 01"
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed().performClick()
        node("Ask the team").assertExists()
    }
    private fun capture(name: String) {
        val bitmap = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val directory = File(context.getExternalFilesDir(null), "experience-evidence").apply { mkdirs() }
        File(directory, "$name-${context.resources.configuration.screenWidthDp}dp.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
    }

    @Test fun navigationRetainsPlantDraftAndAllFiveRoots() {
        node("Today").assertExists(); capture("01-today")
        root("Plants"); openPlant(); capture("02-plant")
        click("Record care")
        val draft = "UNSAVED software QA draft - never an actual plant observation."
        val input = node("What did you observe or do?")
        input.performScrollTo().performTextReplacement(draft); closeSoftKeyboard()
        root("Shop"); node("Plants & essentials").assertExists(); capture("03-shop")
        root("Plants"); node(draft).assertExists()
        scenario!!.recreate()
        node(draft).assertExists()
        click("← Back to space"); node("Leave this draft?").assertExists(); click("Keep editing")
        node(draft).performScrollTo().performTextReplacement(""); closeSoftKeyboard()
        click("← Back to space")
        root("Create"); node("Choose a private record or an intentional public story.").assertExists(); capture("04-create")
        root("Circle"); node("Questions, people and shared experience.").assertExists(); capture("05-circle")
        compose.onNodeWithContentDescription("Open your profile").performClick()
        node("Edit profile", true).assertExists(); capture("06-profile")
        pressBack(); node("Questions, people and shared experience.").assertExists()
        root("Today")
        capture("07-today-return")
    }

    @Test fun existingTeamReplyAndAuthorizedOpenIntent() {
        val requests = runBlocking { repository.demoGet("service-requests/") }
        assertTrue(requests is ApiResult.Success)
        val ticket = (requests as ApiResult.Success).value.demoRows().first()
        val currentId = NovaSessionStore(context).load()!!.cachedUser!!.id
        scenario!!.close()
        val intent = Intent(context, MainActivity::class.java).putExtra("kind", "plantupia_support_reply")
            .putExtra("recipient_id", currentId.toString()).putExtra("ticket_id", ticket.getString("id")).putExtra("plant_id", ticket.getString("plant_id"))
        scenario = ActivityScenario.launch(intent)
        node("Team replied").assertExists()
        node("QA staff reply:", true).assertExists()
        capture("08-authorized-team-open-intent")
        // This is an OS-open intent test. It does not claim Firebase delivery.
        click("Close")
    }

    @Test fun qaCheckoutIsPersistedAndReopenable() {
        root("Shop")
        val catalog = runBlocking { repository.demoGet("catalog/") }
        assertTrue(catalog is ApiResult.Success)
        val item = (catalog as ApiResult.Success).value.demoRows().first { it.getString("kind") == "plant" }
        val description = "View QA product: " + item.getString("name")
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        click("Add to cart")
        node("Preview order only · no charge or delivery").assertExists()
        val consent = compose.onAllNodes(isToggleable()).onFirst()
        consent.performScrollTo().performClick()
        click("Place preview order")
        node("Preview order received", true).assertExists(); capture("09-order")
        val orders = runBlocking { repository.demoGet("demo/orders/") }
        assertTrue(orders is ApiResult.Success && orders.value.demoRows().isNotEmpty())
        scenario!!.recreate(); root("Shop"); click("My orders")
        node("charged: no", true).assertExists(); capture("10-reopened-order")
    }

    @Test fun notificationCategoriesAndCareRemainPrivate() {
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("Notifications")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Notifications").performClick()
        node("Care, team replies, QA orders and your Circle.").assertExists()
        click("Care"); node("Your scheduled checks").assertExists(); capture("11-notifications-care")
        click("Team"); capture("12-notifications-team")
        click("Orders"); capture("13-notifications-orders")
        click("Circle"); capture("14-notifications-circle")
        pressBack(); root("Plants")
        val actual = runBlocking { repository.plants() }
        assertTrue(actual is ApiResult.Success)
        assertEquals(fixture.getJSONArray("plant_ids").length(), (actual as ApiResult.Success).value.size)
    }
}
