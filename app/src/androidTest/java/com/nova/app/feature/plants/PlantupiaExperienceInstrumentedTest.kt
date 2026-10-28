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
        // The owned headless AVD can surface a System UI ANR after cold boot.
        // Recover that OS dialog only; never dismiss or suppress an app ANR.
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        automation.rootInActiveWindow?.let { root ->
            if (root.findAccessibilityNodeInfosByText("System UI isn't responding").isNotEmpty()) {
                root.findAccessibilityNodeInfosByText("Wait").firstOrNull()?.let { button ->
                    (if (button.isClickable) button else button.parent)?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }
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
        compose.waitForIdle()
        // UiAutomation reads the presented GPU frame, after Compose semantics.
        // Let native navigation/IME presentation finish before recording evidence.
        android.os.SystemClock.sleep(1000)
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

    /** Read-only layout matrix: this may be repeated at different owned-AVD sizes. */
    @Test fun responsiveRootsAndRealCircleFeed() {
        root("Today"); node("APPROVED INSPECTIONS").assertExists(); capture("20-responsive-today")
        root("Plants"); openPlant(); capture("21-responsive-plant")
        root("Shop"); node("Plants & essentials").assertExists(); capture("22-responsive-shop")
        root("Create"); node("Choose a private record or an intentional public story.").assertExists(); capture("23-responsive-create")
        root("Circle"); click("Shared moments")
        node("Back to Circle", true).assertExists(); capture("24-real-circle-feed")
        pressBack(); node("Questions, people and shared experience.").assertExists()
        compose.onNodeWithContentDescription("Open your profile").performClick()
        node("Edit profile", true).assertExists(); capture("25-responsive-profile")
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed().performClick()
        // Settings is an existing independent Activity, preserved by this refinement.
        node("Settings").assertIsDisplayed()
        capture("26-settings")
        pressBack()
    }

    @Test fun realUnavailableAiRetainsQuestionAndCreatesNoHistory() {
        root("Plants"); openPlant()
        val plantId = fixture.getJSONArray("plant_ids").getString(0)
        val before = runBlocking { repository.historyPage(plantId) }
        assertTrue(before is ApiResult.Success)
        click("Ask Plantupia AI")
        val question = "Software QA only: what should be checked before watering?"
        compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription("Message")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Message").assertIsDisplayed().performClick().performTextInput(question)
        capture("27-ai-keyboard")
        compose.onNodeWithContentDescription("Message").performImeAction()
        node("Plantupia AI is unavailable right now.", true).assertExists()
        compose.onNodeWithContentDescription("Message").assertTextContains(question)
        closeSoftKeyboard(); capture("28-ai-real-unavailable")
        click("Close")
        val after = runBlocking { repository.historyPage(plantId) }
        assertTrue(after is ApiResult.Success)
        assertEquals((before as ApiResult.Success).value, (after as ApiResult.Success).value)
    }

    @Test fun actualOrderNoticeOpensOwnPersistedOrder() {
        compose.onNodeWithContentDescription("Notifications").performClick()
        click("Orders")
        click("prepared a separate QA order draft", true)
        node("Draft prepared · awaiting review", true).assertExists()
        node("charged: no", true).assertExists(); capture("29-order-notice-open")
    }

    @Test fun accountSwitchRejectsOtherRecipientAndManualFirstPlantPersists() {
        val tickets = runBlocking { repository.demoGet("service-requests/") }
        assertTrue(tickets is ApiResult.Success)
        val ticket = (tickets as ApiResult.Success).value.demoRows().first()
        val originalAccount = NovaSessionStore(context).load()!!.cachedUser!!.id
        login("stranger")
        val before = runBlocking { repository.plants() }
        assertTrue(before is ApiResult.Success)
        val priorPlants = (before as ApiResult.Success).value
        // Preserve this disposable account's own first plant on a repeated QA run.
        assertTrue(priorPlants.isEmpty() || (priorPlants.size == 1 && priorPlants.single().name == "SOFTWARE QA first private plant"))
        root("Plants")
        if (priorPlants.isEmpty()) { node("Your plants start here.").assertExists(); capture("30-empty-account") }
        scenario!!.close()
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java)
            .putExtra("kind", "plantupia_support_reply").putExtra("recipient_id", originalAccount.toString())
            .putExtra("ticket_id", ticket.getString("id")).putExtra("plant_id", ticket.getString("plant_id")))
        root("Plants")
        compose.onAllNodes(hasText("QA staff reply:", substring = true)).assertCountEquals(0)
        val unauthorized = runBlocking { repository.demoGet("service-requests/" + ticket.getString("id") + "/") }
        assertTrue(unauthorized is ApiResult.Failure && unauthorized.statusCode == 404)
        if (priorPlants.isEmpty()) {
            root("Create"); click("New space")
            node("Space name *").performScrollTo().performTextInput("SOFTWARE QA private starter space")
            closeSoftKeyboard(); click("Create and add a plant")
            node("Plant name *").performScrollTo().performTextInput("SOFTWARE QA first private plant")
            closeSoftKeyboard(); click("Add plant")
        } else {
            val description = "Open plant: SOFTWARE QA first private plant"
            compose.waitUntil(30_000) { compose.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }
            compose.onNodeWithContentDescription(description).performScrollTo().performClick()
        }
        node("Record care").assertExists(); capture("31-first-private-plant")
        root("Create"); click("Record care or observation")
        node("What did you observe or do?").assertExists()
        val saved = runBlocking { repository.plants() }
        assertTrue(saved is ApiResult.Success && saved.value.size == 1)
        login("customer"); root("Plants")
        val original = runBlocking { repository.plants() }
        assertTrue(original is ApiResult.Success)
        assertEquals(fixture.getJSONArray("plant_ids").length(), (original as ApiResult.Success).value.size)
    }

    @Test fun revokedAccessKeepsActualThreadButBlocksNewReplies() {
        val access = runBlocking { repository.demoGet("demo/access/") }
        assumeTrue(access is ApiResult.Success && !access.value.optBoolean("entitled"))
        existingTeamReplyAndAuthorizedOpenIntent()
        root("Plants"); click("Ask the team")
        click("CEO QA inspection question", true)
        node("Optional care access for new replies").assertExists()
        compose.onAllNodes(hasContentDescription("Message")).assertCountEquals(0)
        capture("32-revoked-readable-team")
        click("Close"); click("Ask Plantupia AI")
        node("Care, when you need it.").assertExists(); capture("33-revoked-ai-paywall")
    }

    /** Explicit host coordination stops only this fixture's localhost server. */
    @Test fun interruptedPrivateSaveRetriesOriginalExactlyOnce() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("offline") == "owned-local")
        val markers = File(context.filesDir, "experience-network").apply { mkdirs() }
        markers.listFiles()?.forEach { it.delete() }
        fun signal(name: String) { File(markers, name).writeText("owned-local") }
        fun awaitHost(name: String) {
            val deadline = android.os.SystemClock.elapsedRealtime() + 90_000
            while (!File(markers, name).exists() && android.os.SystemClock.elapsedRealtime() < deadline) android.os.SystemClock.sleep(200)
            assertTrue("Local host must provide $name", File(markers, name).exists())
        }
        root("Plants"); openPlant(); click("Record care")
        val note = "Software QA network recovery " + java.util.UUID.randomUUID() + ". No physical plant observation."
        node("What did you observe or do?").performScrollTo().performTextInput(note)
        closeSoftKeyboard(); signal("ready"); awaitHost("offline")
        click("Save observation")
        node("Retry original entry").assertExists(); capture("34-real-network-failure")
        signal("reconnect"); awaitHost("online")
        click("Retry original entry")
        compose.waitUntil(30_000) { compose.onAllNodes(hasText("Retry original entry")).fetchSemanticsNodes().isEmpty() }
        val plantId = fixture.getJSONArray("plant_ids").getString(0)
        val saved = runBlocking { repository.historyPage(plantId) }
        assertTrue(saved is ApiResult.Success)
        assertEquals(1, (saved as ApiResult.Success).value.events.count { it.note == note })
        scenario!!.recreate(); root("Plants")
        val reopened = runBlocking { repository.historyPage(plantId) }
        assertTrue(reopened is ApiResult.Success)
        assertEquals(1, (reopened as ApiResult.Success).value.events.count { it.note == note })
        capture("35-reconnected-original-history")
    }
}
