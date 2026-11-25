package com.nova.app.feature.plants

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ActivityScenario
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
 * Read-only navigation through the real app against the guarded disposable seed.
 * The seed's customer has no claimed delivery; the existing staff owner holds
 * the seven synthetic plants. No claim, order, publication or account is created.
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

        awaitText("Start with a real place.")
        capture("01-customer-today")
        tab("Plants")
        awaitText("My plants")
        awaitText("Start with a real place.")
        capture("02-customer-plants")

        tab("Create")
        awaitText("What are you doing?")
        capture("03-create")
        scrollToText("Explore the Circle")
        capture("04-create-public")
        clickText("Explore the Circle")
        awaitText("Better together")
        scrollToText("No questions yet")
        capture("05-circle")

        clickText("Shared moments")
        awaitText("Back to Circle")
        scrollToText("Your circle is quiet")
        capture("06-circle-moments")
        clickText("Back to Circle")
        awaitText("Better together")
        openProfile()
        awaitText("Your private places")
        compose.onNodeWithContentDescription("Settings").assertIsDisplayed()
        compose.onNodeWithContentDescription("Messages").assertIsDisplayed()
        capture("07-profile")
        scrollToText("Edit public profile")
        capture("08-profile-public")

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
        awaitText("The living collection")
        scrollToText("The collection is growing.")
        compose.onNodeWithText("No products are currently published for this account.").assertExists()
        capture("11-shop-empty")
        clickText("Cart", substring = true)
        awaitText("Your cart")
        scrollToText("Nothing saved yet")
        capture("12-shop-cart")

        tab("Ask")
        awaitText("Start with a plant, or don't.")
        // This seed has no paid/preview entitlement or configured AI provider.
        // Capture its real unavailable/access surface, without sending a question.
        awaitNoProgress()
        revealGeneralContext()
        capture("13-ask")
        tab("Today")
        awaitText("Start with a real place.")
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

        awaitText("One day, one living space")
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
        scrollToText("${spaces.size} spaces · ${plants.count { it.active }} plants")
        capture("17-owner-profile")
        val after = runBlocking { repository.plants() }
        assertTrue("Read-only navigation must retain the original inventory.",
            after is ApiResult.Success && after.value.map { it.id }.toSet() == expectedIds)
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
        compose.waitUntil(30_000) { compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty() }
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
                return compose.onAllNodes(matcher).onFirst().performScrollTo().assertIsDisplayed()
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
        val label = hasText("No plant context")
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
        val directory = File(context.getExternalFilesDir(null), "craft-evidence").apply { mkdirs() }
        val screenshot = requireNotNull(instrumentation.uiAutomation.takeScreenshot()) {
            "The actual emulator frame must be capturable."
        }
        File(directory, "$name-${config.screenWidthDp}dp-font${config.fontScale}.png").outputStream().use {
            assertTrue("The native screenshot must be written completely.",
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
}
