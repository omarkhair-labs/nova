package com.nova.app.feature.plants

import android.graphics.Bitmap
import android.os.SystemClock
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
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.io.File
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Navigation through the real app against the guarded disposable seed.
 * The field journey adds a reported room to the seven-plant fixture. No claim,
 * order, care record, publication or account is created by these tests.
 */
class PlantupiaCoreRootsInstrumentedTest {
    @get:Rule val compose = createEmptyComposeRule()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private lateinit var fixture: JSONObject
    private var scenario: ActivityScenario<MainActivity>? = null

    @Before fun requireDisposableLocalFixture() {
        val origin = URI(NovaMessagingRepository.PRODUCTION_API_URL)
        require(context.packageName == "com.omarkhair70.nova.qa") {
            "Core-root proof requires the isolated QA application."
        }
        require(origin.scheme == "http" && origin.host in setOf("127.0.0.1", "localhost", "10.0.2.2") &&
            origin.path == "/api/v1/" && origin.userInfo == null && origin.query == null && origin.fragment == null) {
            "Core-root proof requires an explicit loopback QA API."
        }
        val file = File(context.filesDir, "core-roots-local-qa.json")
        require(file.isFile) { "The private disposable seed fixture is required." }
        fixture = runCatching {
            JSONObject(file.readText()).also { parsed ->
                require(parsed.optBoolean("synthetic"))
                for (role in listOf("customer", "staff")) {
                    val account = parsed.getJSONObject("accounts").getJSONObject(role)
                    require(account.getString("email").endsWith("@plantupia.example.test") &&
                        account.getString("password").isNotBlank())
                }
                val ids = parsed.getJSONArray("plant_ids")
                require(ids.length() == 7)
                for (index in 0 until ids.length()) UUID.fromString(ids.getString(index))
                UUID.fromString(parsed.getString("space_id"))
            }
        }.getOrElse {
            // JSON parser exceptions may include the input, which contains secrets.
            throw AssertionError("The private fixture must match the guarded synthetic seven-plant seed.")
        }
        NovaSessionStore(context).logout()
    }

    @After fun closeOwnedSession() {
        scenario?.close()
        scenario = null
        if (::fixture.isInitialized) NovaSessionStore(context).logout()
    }

    @Test fun customerCanReachAllCurrentRootsAndReturnWithoutCreatingRecords() {
        val repository = openSeedAccount("customer")
        val initialPlants = runBlocking { repository.plants() }
        assertTrue("The unclaimed seed customer must have an empty inventory.",
            initialPlants is ApiResult.Success && initialPlants.value.isEmpty())

        awaitText("Your first space")
        capture("01-customer-today")
        tab("Plants")
        awaitText("My plants")
        awaitText("Your first space")
        capture("02-customer-plants")

        tab("Create")
        awaitText("Record care or observation")
        capture("03-create")
        scrollToText("Explore the Circle")
        capture("04-create-public")
        clickText("Explore the Circle")
        awaitText("Circle")
        scrollToText("No questions yet")
        capture("05-circle")

        clickText("Shared moments")
        awaitText("Back to Circle")
        scrollToText("Your circle is quiet")
        capture("06-circle-moments")
        clickText("Back to Circle")
        // The restored lazy list may be below its title at 2x text. Prove the
        // Circle title is reachable and displayed, without requiring a reset
        // of the user’s previously selected scroll position.
        scrollToText("Circle").assertIsDisplayed()
        openProfile()
        awaitText("Your private places")
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Messages").assertIsDisplayed()
        capture("07-profile")
        clickText("Circle profile")
        compose.onNodeWithText("Your private places").assertDoesNotExist()
        scrollToText("Edit public profile")
        capture("08-profile-public")
        clickText("Private world")
        awaitText("Your private places")
        compose.onNodeWithText("Edit public profile").assertDoesNotExist()

        clickDescription("Settings")
        awaitText("Settings")
        capture("09-settings")
        clickText("About Plantupia")
        awaitText("NATURE & ARCHITECTURE")
        capture("10-settings-about")
        clickText("Done")
        pressBack()
        awaitText("My Plantupia")

        tab("Shop")
        awaitText("Search products")
        scrollToText("No products are currently published for this account.").assertIsDisplayed()
        capture("11-shop-empty")
        clickText("Cart", substring = true)
        awaitText("Your cart")
        scrollToText("Nothing saved yet")
        capture("12-shop-cart")

        tab("Ask")
        awaitText("Ask Plantupia")
        // This seed has no paid/preview entitlement or configured AI provider.
        // Capture its real unavailable/access surface, without sending a question.
        awaitNoProgress()
        revealGeneralContext()
        capture("13-ask")
        awaitNode(hasText("General care") and hasClickAction()).assertIsDisplayed().performClick()
        awaitText("Choose a plant").assertIsDisplayed()
        capture("13a-ask-picker")
        awaitNode(hasText("Done") and hasClickAction()).assertIsDisplayed().performClick()
        awaitText("Ask Plantupia")
        tab("Today")
        awaitText("Your first space")
        capture("14-today-return")

        val finalPlants = runBlocking { repository.plants() }
        assertTrue("Navigation must not create or claim a plant.",
            finalPlants is ApiResult.Success && finalPlants.value.isEmpty())
        val cart = runBlocking { repository.demoGet("commerce/cart/") }
        assertTrue("The real customer cart must remain empty.",
            cart is ApiResult.Success && cart.value.optJSONArray("results")?.length() == 0)
    }

    @Test fun existingOwnerCanReadPopulatedSpacesAndPrivateProfileWithoutChangingInventory() {
        val repository = openSeedAccount("staff")
        val plantResponse = runBlocking { repository.plants() }
        assertTrue("Seed owner inventory must load from the local API.", plantResponse is ApiResult.Success)
        val plants = (plantResponse as ApiResult.Success).value
        val expectedIds = fixture.getJSONArray("plant_ids").let { ids ->
            (0 until ids.length()).map { ids.getString(it) }.toSet()
        }
        assertTrue("The original seven plant identities must remain intact.",
            plants.map { it.id }.toSet() == expectedIds)
        val spaceResponse = runBlocking { repository.spaces() }
        assertTrue("Seed owner space must load from the local API.", spaceResponse is ApiResult.Success)
        val spaces = (spaceResponse as ApiResult.Success).value
        val ownSpace = spaces.singleOrNull { it.id == fixture.getString("space_id") }
        assertTrue("The original synthetic space must be accessible to its owner.", ownSpace != null)

        awaitText("No check-ins due")
        capture("15-owner-today")
        tab("Plants")
        awaitText("My plants")
        awaitText(requireNotNull(ownSpace).name)
        // The space switcher and hero repeat the actual name; the last match is
        // the content-sized hero, whose long title must stay reachable at 2x text.
        compose.onAllNodesWithText(ownSpace.name).onLast().performScrollTo().assertIsDisplayed()
        capture("16-owner-plants")
        openProfile()
        awaitText("My Plantupia")
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Messages").assertIsDisplayed()
        scrollToText("${spaces.size} ${if (spaces.size == 1) "space" else "spaces"} · ${plants.count { it.active }} plants")
        capture("17-owner-profile")

        // The existing seed attaches its first sample guide to this exact UUID.
        // Read the real specimen and source note; never fabricate a guide topic.
        val specimen = plants.single { it.id == fixture.getJSONArray("plant_ids").getString(0) }
        val guideResponse = runBlocking { repository.careGuide(specimen.id) }
        assertTrue("The disposable sample guide must load from the local API.",
            guideResponse is ApiResult.Success)
        val guide = (guideResponse as ApiResult.Success).value
        assertTrue("The sample source note must remain explicitly synthetic and unapproved.",
            guide.version != null && guide.instructions.startsWith("SYNTHETIC QA DEMO — NOT APPROVED BY PLANTUPIA.") &&
                guide.sourceReference.contains("SYNTHETIC QA"))
        val initialHistory = runBlocking { repository.historyPage(specimen.id) }
        assertTrue("The original sample plant has no saved care events.",
            initialHistory is ApiResult.Success && initialHistory.value.events.isEmpty())

        tab("Plants")
        clickDescription("Open plant: ${specimen.name}")
        val chapterRole = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        awaitNode(hasText("Overview") and chapterRole).assertIsSelected()
        // Keep the actual tab row in the frame: large-text clipping must be
        // reviewed in the original image, not hidden by a lower-body capture.
        scrollToText("Overview", clickable = true).assertIsSelected()
        compose.onNode(hasText("Care") and chapterRole).assertIsDisplayed()
        compose.onNode(hasText("History") and chapterRole).assertIsDisplayed()
        awaitText(specimen.name)
        capture("18-owner-plant-detail")

        clickText("Care")
        compose.onNode(hasText("Care") and chapterRole).assertIsSelected()
        scrollToText("CARE & OBSERVATIONS")
        capture("19-owner-plant-care")

        clickText("History")
        compose.onNode(hasText("History") and chapterRole).assertIsSelected()
        scrollToText("No field notes yet")
        capture("20-owner-plant-history")

        clickText("Care")
        clickText("Care guide")
        awaitText("Reviewed care")
        clickText("Read complete care notes")
        awaitText("Show less")
        scrollToText("SYNTHETIC QA DEMO — NOT APPROVED BY PLANTUPIA.", substring = true)
        capture("21-owner-plant-guide")
        pressBack()
        clickText("Overview")
        clickText("← Back to space")
        awaitNode(hasContentDescription("Open plant: ${specimen.name}"))

        val finalHistory = runBlocking { repository.historyPage(specimen.id) }
        assertTrue("Reading care chapters and the guide must not record care.",
            finalHistory is ApiResult.Success && finalHistory.value.events.isEmpty())
        val after = runBlocking { repository.plants() }
        assertTrue("Read-only navigation must retain the original inventory.",
            after is ApiResult.Success && after.value.map { it.id }.toSet() == expectedIds &&
                after.value.associate { it.id to it.active } == plants.associate { it.id to it.active })
    }

    @Test fun livingRoomDaylightAndCaptureReachCurrentPlant() {
        val repository = openSeedAccount("staff")
        val id = fixture.getString("space_id")
        val specimenId = fixture.getJSONArray("plant_ids").getString(0)
        val payload = JSONObject("""
            {"base_revision":0,"width_m":4,"length_m":3,
             "plants":[{"plant_id":"$specimenId","x_m":3.7,"y_m":1.5}],
             "windows":[{"id":"East window","x_m":4,"y_m":1.5}]}
        """.trimIndent())
        val original = runBlocking { repository.room(id) }
        assertTrue("The disposable room must be available.", original is ApiResult.Success)
        val initialRevision = (original as ApiResult.Success).value.revision
        if (initialRevision == 0) {
            val first = JSONObject(payload.toString()).put("length_m", 2.8)
            val saved = runBlocking { repository.saveRoom(id, first) }
            assertTrue("The first layout must be acknowledged.", saved is ApiResult.Success && saved.value == 1)
            val revised = runBlocking { repository.saveRoom(id, payload.put("base_revision", 1)) }
            assertTrue("The updated layout must be acknowledged.", revised is ApiResult.Success && revised.value == 2)
        } else {
            assertTrue("Only both expected layouts may exist on this repeat pass.", initialRevision == 2)
        }
        tab("Plants")
        clickText("Explore this place")
        scrollToText("Reported room", substring = true)
        compose.onNodeWithContentDescription("Floor plan with 1 recorded plants and 1 windows").assertExists()
        capture("22-room-placements")
        clickText("Choose a plant in this room")
        clickText("Synthetic plant 01 ·", substring = true)
        clickText("Perspective")
        compose.onNodeWithContentDescription("3D view of recorded room geometry").assertExists()
        scrollToText("Open Synthetic plant 01", clickable = true)
        clickText("Floor plan")
        clickText("Browse layout history")
        clickText("Revision 1", substring = true)
        scrollToText("Earlier layout", substring = true)
        clickText("Return to current layout")
        clickText("Perspective")
        clickText("Inspect a reconstruction file")
        scrollToText("Open PLY scene", clickable = true)
        clickText("Return to room")
        clickText("Study sunlight at Synthetic plant 01")
        fillScenario("Opening width (m)", "2")
        fillScenario("Sill above floor (m)", ".4")
        fillScenario("Head above floor (m)", "2.4")
        fillScenario("Top wall outward bearing from true north (°)", "0")
        scrollToText("Recorded position of Synthetic plant 01 · 3.7, 1.5 m")
        fillScenario("Sample height (m)", "1.1")
        fillScenario("Latitude (°)", "0")
        fillScenario("Longitude (°)", "0")
        fillScenario("Date (YYYY-MM-DD)", "2026-03-20")
        fillScenario("Time zone (e.g. Africa/Cairo)", "UTC")
        clickText("Simulate this day")
        scrollToText("Direct sun reaches this point", substring = true)
        capture("23-room-daylight")
        clickText("Return to placements")
        val unchanged = runBlocking { repository.room(id) }
        assertTrue("Exploring light, history and reconstruction must not write a layout.",
            unchanged is ApiResult.Success && unchanged.value.revision == 2 &&
                unchanged.value.plants.single().id == specimenId && unchanged.value.plants.single().x == 3.7)
        clickText("Open Synthetic plant 01")
        clickText("Care")
        clickText("Record care")
        clickText("Add a note, photo or optional next check", substring = true)
        clickText("Add a photo of your observation")
        scrollToText("Allow camera", clickable = true)
        capture("24-capture-permission")
        pressBack()
        assertTrue("Entering and leaving capture creates no private record.",
            (runBlocking { repository.historyPage(specimenId) } as? ApiResult.Success)?.value?.events?.isEmpty() == true)
    }

    @Test fun recordedCalendarDayReturnsFromAskToTheSameVisit() {
        val repository = openSeedAccount("staff")
        val plantId = fixture.getJSONArray("plant_ids").getString(6)
        val today = java.time.LocalDate.now()
        val day = today.withDayOfMonth(1)
        val marker = "Synthetic calendar navigation visit"
        val existing = runBlocking { repository.historyPage(plantId, observedOn = day) }
        assertTrue(existing is ApiResult.Success)
        if ((existing as ApiResult.Success).value.events.none { it.note == marker }) {
            val record = JSONObject().put("kind", "observation").put("note", marker)
                .put("observed_at", day.toString() + "T10:00:00Z")
            assertTrue(runBlocking { repository.demoPost("plants/$plantId/events/", record) } is ApiResult.Success)
        }
        tab("Plants")
        clickDescription("Open care schedule")
        clickText("Month")
        val locale = InstrumentationRegistry.getInstrumentation().targetContext.resources.configuration.locales[0]
        val dayLabel = day.format(java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", locale))
        val chooseDate = hasText("Choose date ·", substring = true) and hasClickAction()
        val compact = compose.onAllNodes(chooseDate).fetchSemanticsNodes().isNotEmpty()
        val shortDay = day.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM yyyy", locale))
        if (compact) {
            awaitNode(chooseDate).performScrollTo().performClick()
            clickText(shortDay)
        } else {
            awaitNode(hasContentDescription("$dayLabel,", substring = true)).performScrollTo().performClick()
        }
        clickText("Observation recorded")
        awaitText("Records from $day")
        scrollToText(marker)
        clickText("Ask about this history")
        awaitText("Here for Synthetic plant 07")
        pressBack()
        scrollToText(marker)
        clickText("← Back to calendar")
        awaitText("My care schedule")
        if (compact) awaitText("Choose date · $shortDay")
        else awaitNode(hasContentDescription("$dayLabel,", substring = true)).assertIsSelected()
        scrollToText("Observation recorded")
        capture("25-calendar-visit-return")
    }

    private fun fillScenario(label: String, value: String) {
        val field = scrollToText(label)
        field.performTextReplacement(value)
        closeSoftKeyboard()
    }

    private fun openSeedAccount(role: String): PlantupiaRepository {
        val account = fixture.getJSONObject("accounts").getJSONObject(role)
        val response = runCatching {
            runBlocking {
                AuthRemoteDataSource(NovaApiClient(NovaMessagingRepository.PRODUCTION_API_URL))
                    .login(account.getString("email"), account.getString("password"))
            }
        }.getOrElse { throw AssertionError("Local seed authentication did not complete.") }
        // Never stringify auth results or fixture contents into public CI output.
        assertTrue("The existing disposable seed account must authenticate.", response is ApiResult.Success)
        val session = (response as ApiResult.Success).value
        assertTrue("Only the synthetic seed identity may be used.", session.user.username.startsWith("qa_"))
        val store = NovaSessionStore(context)
        assertTrue("The local session must belong to this login.", store.save(store.beginLogin(), session))
        val repository = PlantupiaRepository(context)
        scenario = ActivityScenario.launch(MainActivity::class.java)
        tab("Today")
        return repository
    }

    private fun awaitText(text: String): SemanticsNodeInteraction = awaitNode(hasText(text))

    private fun awaitNode(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        // An Activity transition can briefly have no Compose root. Keep polling
        // within this deadline; actual missing content still fails the proof.
        compose.waitUntil(30_000) {
            compose.onAllNodes(matcher).fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        return compose.onAllNodes(matcher).onFirst()
    }

    private fun tab(label: String) {
        val matcher = hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        awaitNode(matcher).assertIsDisplayed().performClick()
        compose.onAllNodes(matcher).onFirst().assertIsSelected()
    }

    private fun scrollToText(text: String, substring: Boolean = false, clickable: Boolean = false): SemanticsNodeInteraction {
        val matcher = hasText(text, substring = substring).let { if (clickable) it and hasClickAction() else it }
        val deadline = SystemClock.elapsedRealtime() + 30_000
        while (SystemClock.elapsedRealtime() < deadline) {
            if (compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()) {
                val node = compose.onAllNodes(matcher).onFirst()
                if (runCatching { node.assertIsDisplayed() }.isSuccess) return node
                return node.performScrollTo().assertIsDisplayed()
            }
            // Lazy roots do not compose off-screen rows until they are requested.
            // Eager vertical forms already expose their children above.
            val lists = compose.onAllNodes(hasScrollToIndexAction())
            if (lists.fetchSemanticsNodes().isNotEmpty()) {
                runCatching { lists.onFirst().performScrollToNode(matcher) }
            }
            SystemClock.sleep(150)
        }
        throw AssertionError("Expected native content was not reachable: $text")
    }

    private fun clickText(text: String, substring: Boolean = false) {
        scrollToText(text, substring, clickable = true).performClick()
    }

    private fun clickDescription(description: String) {
        awaitNode(hasContentDescription(description)).performScrollTo().assertIsDisplayed().performClick()
    }

    private fun openProfile() {
        clickDescription("Open your profile")
        awaitText("My Plantupia")
    }

    private fun awaitNoProgress() {
        val progress = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.waitUntil(30_000) { compose.onAllNodes(progress).fetchSemanticsNodes().isEmpty() }
    }

    private fun revealGeneralContext() {
        val label = hasText("General care")
        val scroller = SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange) and
            hasAnyDescendant(label)
        repeat(4) {
            if (runCatching { compose.onNode(label).assertIsDisplayed() }.isSuccess) return
            compose.onNode(scroller).performTouchInput { swipeUp() }
        }
        compose.onNode(label).assertIsDisplayed()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        // Semantics can settle before the native window/navigation frame does.
        SystemClock.sleep(350)
        val config = context.resources.configuration
        val directory = File(context.filesDir, "craft-evidence").apply { mkdirs() }
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "The actual emulator frame must be capturable."
        }
        File(directory, "$name-${config.screenWidthDp}dp-font${config.fontScale}.png").outputStream().use {
            assertTrue("The native screenshot must be written completely.",
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
