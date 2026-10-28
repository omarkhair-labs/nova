package com.nova.app.feature.plants

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaPlant
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val demoForest = Color(0xFF123B2E)
private val demoPaper = Color(0xFFFFFDF7)
private val demoSage = Color(0xFFEEF1E8)

internal fun JSONObject.demoRows(key: String = "results"): List<JSONObject> {
    val rows = getJSONArray(key)
    return (0 until rows.length()).map { rows.getJSONObject(it) }
}

@Composable
private fun DemoCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = demoSage, shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFD4DFD1))) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

@Composable
internal fun PlantupiaDemoAccessGate(
    repository: PlantupiaRepository,
    human: Boolean,
    scrollPaywall: Boolean = false,
    paywallModifier: Modifier = Modifier,
    content: @Composable (onAccessLost: () -> Unit) -> Unit,
) {
    var access by remember { mutableStateOf<JSONObject?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh) {
        error = null
        when (val result = repository.demoGet("demo/access/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) access = result.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
    }
    val allowed = access?.optBoolean(if (human) "human_access" else "entitled") == true
    if (allowed) {
        Text("Preview access active · no payment", fontSize = 11.sp, color = demoForest)
        if (!human && access?.optBoolean("ai_configured") != true) Text("AI is unavailable in this preview.", style = PlantupiaText.supporting)
        content { access = null; refresh++ }
    } else {
        Column(paywallModifier.then(if (scrollPaywall) Modifier.verticalScroll(rememberScrollState()) else Modifier), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Care, when you need it.", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = demoForest)
        Text("Your plants and care schedule are always free.", color = demoForest)
        Text("Provisional concept prices · QA only · no payment", style = PlantupiaText.supporting)
        if (access == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        access?.let { state ->
            val offers = state.demoRows("offers")
            if (offers.isEmpty()) Text("Care plans are not available yet.", style = PlantupiaText.supporting)
            offers.forEach { offer ->
                val teamCare = offer.getInt("human_limit") > 0
                val canChoose = !human || teamCare
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = demoPaper,
                    border = BorderStroke(1.dp, if (teamCare) demoForest else PlantupiaColors.border),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(offer.getString("name"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = demoForest)
                                Text(
                                    if (teamCare) "AI + team support" else "AI plant guidance",
                                    fontSize = 13.sp, color = PlantupiaColors.secondaryText,
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    offer.getString("price"),
                                    fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = demoForest,
                                )
                                Text("EGP / month", fontSize = 11.sp, color = PlantupiaColors.secondaryText)
                            }
                        }
                        HorizontalDivider(color = PlantupiaColors.border)
                        Text("${offer.getInt("ai_daily_limit")} AI questions / day", style = PlantupiaText.body)
                        if (teamCare) Text(
                            "${offer.getInt("human_limit")} team requests / 30 days",
                            style = PlantupiaText.body,
                        )
                        Text(
                            "Preview · ${offer.getInt("qa_hours")} hours · no payment",
                            style = PlantupiaText.supporting,
                        )
                        Button(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            enabled = !busy && canChoose,
                            colors = ButtonDefaults.buttonColors(containerColor = demoForest),
                            onClick = {
                                busy = true
                                error = null
                                scope.launch {
                                    val result = repository.demoMutation(
                                        "access", "demo/access/",
                                        JSONObject().put("offer", offer.getString("key")).put("confirm_no_payment", true),
                                    )
                                    if (repository.isCurrentSession()) when (result) {
                                        is ApiResult.Success -> { access = null; refresh++ }
                                        is ApiResult.Failure -> error = result.message
                                    }
                                    busy = false
                                }
                            },
                        ) { Text(if (busy) "Activating…" else "Try this plan · no charge") }
                    }
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { refresh++ }) { Text("Retry access check") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaDemoHome(repository: PlantupiaRepository, plants: List<PlantupiaPlant>, refreshVersion: Int = 0, onOpenPlant: (PlantupiaPlant) -> Unit) {
    var plans by remember { mutableStateOf(emptyList<JSONObject>()) }
    var available by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(emptyList<JSONObject>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh, refreshVersion) {
        pending = repository.pendingDemoMutations()
        when (val result = repository.demoGet("care-plans/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) { available = true; plans = result.value.demoRows() }
            is ApiResult.Failure -> if (repository.isCurrentSession() && result.statusCode != 404) message = "Inspection plans couldn't load. Retry when connected."
        }
    }
    val optedIn = plans.filter { it.optBoolean("enabled") && it.optBoolean("valid") }
    // No ornamental "no plans" panel when an owner simply wants today's work.
    // Still surface actual approved actions and pending QA mutation retries.
    if (available && (optedIn.isNotEmpty() || pending.isNotEmpty())) {
        DemoCard {
            Text("APPROVED INSPECTIONS", style = PlantupiaText.sectionLabel)
            optedIn.sortedBy { it.getString("next_due_on") }.take(4).forEach { plan ->
                TextButton(onClick = { plants.firstOrNull { it.id == plan.getString("plant_id") }?.let(onOpenPlant) }) {
                    Text("${if (plan.optBoolean("due")) "Due · " else "Next · "}${plan.getString("plant_name")} · ${plan.getString("action")} · ${plan.getString("next_due_on")}")
                }
            }
            pending.forEach { intent ->
                Button(onClick = {
                    scope.launch {
                        when (val result = repository.retryDemoMutation(intent)) {
                            is ApiResult.Success -> message = "Original QA action confirmed. Reopen its plan, team thread or My orders to inspect the persisted result."
                            is ApiResult.Failure -> message = result.message
                        }
                        refresh++
                    }
                }) { Text("Retry pending QA action") }
            }
        }
    }
    message?.let { Text(it, color = demoForest); TextButton(onClick = { refresh++ }) { Text("Refresh inspection plans") } }
}

@Composable
internal fun PlantupiaInspectionPanel(plant: PlantupiaPlant, repository: PlantupiaRepository, onRecorded: () -> Unit) {
    var plans by remember(plant.id) { mutableStateOf(emptyList<JSONObject>()) }
    var error by remember(plant.id) { mutableStateOf<String?>(null) }
    var refresh by remember(plant.id) { mutableIntStateOf(0) }
    var available by remember(plant.id) { mutableStateOf(false) }
    LaunchedEffect(plant.id, refresh) {
        when (val result = repository.demoGet("care-plans/?plant_id=${plant.id}")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) { available = true; plans = result.value.demoRows(); error = null }
            is ApiResult.Failure -> if (repository.isCurrentSession() && result.statusCode != 404) error = result.message
        }
    }
    if (available) {
        Text("Inspection care plans", fontFamily = FontFamily.Serif, fontSize = 24.sp, color = demoForest)
        if (plans.isEmpty()) Text("No approved inspection proposal for this plant. You can still record observations and choose a personal check-in.")
        plans.forEach { plan -> key(plan.getString("id")) {
            var note by remember { mutableStateOf("") }
            var date by remember(plan.getString("next_due_on")) { mutableStateOf(plan.getString("next_due_on")) }
            var busy by remember { mutableStateOf(false) }
            var expanded by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()
            fun act(action: String) {
                busy = true; error = null
                scope.launch {
                    val payload = JSONObject().put("action", action).put("revision", plan.getInt("revision"))
                    if (action == "complete") payload.put("note", note.trim())
                    if (action == "reschedule") payload.put("next_due_on", date.trim())
                    when (val result = repository.demoMutation("inspection-${plan.getString("id")}", "care-plans/${plan.getString("id")}/actions/", payload)) {
                        is ApiResult.Success -> if (repository.isCurrentSession()) { note = ""; refresh++; if (action == "complete") onRecorded() }
                        is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 409) refresh++ }
                    }
                    busy = false
                }
            }
            DemoCard {
                Text(plan.getString("action"), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                if (!plan.optBoolean("valid")) Text("Paused: the source, identity or approval is no longer current. No inspection can be recorded from this proposal.")
                else {
                    Text(plan.getString("instructions"))
                    Text("Every ${plan.getInt("every_days")} day(s) · ${plan.getString("timezone")} · guide v${plan.getInt("guide_version")} · reviewed by ${plan.getString("reviewer")}", fontSize = 11.sp)
                    if (plant.canRecord) {
                        if (!plan.optBoolean("enabled")) Button(enabled = !busy, onClick = { act("opt_in") }) { Text("Opt into this inspection plan") }
                        else {
                            Text("${if (plan.optBoolean("due")) "Due" else "Next inspection"}: ${plan.getString("next_due_on")}", fontWeight = FontWeight.SemiBold)
                            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Close inspection actions" else "Inspect or change the next check") }
                            if (expanded) {
                            OutlinedTextField(note, { note = it.take(2000) }, label = { Text("What did you actually observe?") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            Button(enabled = !busy && note.isNotBlank(), onClick = { act("complete") }, modifier = Modifier.fillMaxWidth()) { Text("Record inspection in Living History") }
                            Row { TextButton(enabled = !busy, onClick = { act("skip") }) { Text("Skip this check") }; TextButton(enabled = !busy, onClick = { act("decline") }) { Text("Stop plan") } }
                            OutlinedTextField(date, { date = it.take(10) }, label = { Text("Move next check · YYYY-MM-DD") }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(enabled = !busy, onClick = { act("reschedule") }) { Text("Confirm new inspection date") }
                            }
                        }
                    } else Text("Read-only access. A carer or manager chooses and records inspections.")
                    Text("This prompts an inspection, never automatic watering. No notification was silently enabled.", fontSize = 11.sp)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        } }
    }
    error?.let { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { refresh++ }) { Text("Retry inspection plans") } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaTeamSheet(plant: PlantupiaPlant, repository: PlantupiaRepository, onDismiss: () -> Unit, initialTicketId: String? = null) {
    var tickets by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selectedId by rememberSaveable(plant.id) { mutableStateOf(initialTicketId) }
    var thread by remember { mutableStateOf<JSONObject?>(null) }
    var subject by rememberSaveable(plant.id) { mutableStateOf("") }
    var body by rememberSaveable(plant.id) { mutableStateOf("") }
    var reply by rememberSaveable(plant.id) { mutableStateOf("") }
    var share by remember { mutableStateOf(false) }
    var placement by remember { mutableStateOf(false) }
    var humanAccess by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    LaunchedEffect(initialTicketId) { if (!initialTicketId.isNullOrBlank()) selectedId = initialTicketId }
    LaunchedEffect(refresh, selectedId) {
        loading = true; error = null
        when (val result = repository.demoGet("service-requests/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) tickets = result.value.demoRows().filter { it.getString("plant_id") == plant.id }
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
        when (val result = repository.demoGet("demo/access/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) humanAccess = result.value.optBoolean("human_access")
            is ApiResult.Failure -> if (repository.isCurrentSession()) humanAccess = false
        }
        val id = selectedId
        if (id != null) when (val result = repository.demoGet("service-requests/$id/")) {
            is ApiResult.Success -> if (repository.isCurrentSession() && result.value.optString("plant_id") == plant.id) thread = result.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) { thread = null; error = result.message }
        } else thread = null
        loading = false
    }
    val messages = thread?.optJSONArray("messages")?.let { array -> (0 until array.length()).map { array.getJSONObject(it) } }.orEmpty()
    LaunchedEffect(selectedId, messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size) }
    PlantupiaConversationSheet(onDismiss) {
        PlantupiaConversationHeader("HUMAN SUPPORT", "Plantupia team", plant.name + " · Private request", onDismiss)
        if (selectedId == null) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Actual staff replies, when available. No response time is promised.", style = PlantupiaText.supporting)
                tickets.forEach { ticket ->
                    OutlinedButton(onClick = { selectedId = ticket.getString("id") }, modifier = Modifier.fillMaxWidth()) {
                        Text(ticket.getString("subject") + " · " + supportStatusLabel(ticket.getString("status")))
                    }
                }
                PlantupiaDemoAccessGate(repository, human = true) { _ ->
                    OutlinedTextField(subject, { subject = it.take(140) }, label = { Text("Request subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it.take(2000) }, label = { Text("What do you want the team to know?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row {
                        Checkbox(share, { share = it }, modifier = Modifier.semantics { contentDescription = "Share this request and plant label with authorized support staff" })
                        Text("Share this message and plant name with authorized staff.", Modifier.weight(1f), style = PlantupiaText.supporting)
                    }
                    Row {
                        Checkbox(placement, { placement = it }, modifier = Modifier.semantics { contentDescription = "Also share recorded placement" })
                        Text("Include placement (optional). No photos or history.", Modifier.weight(1f), style = PlantupiaText.supporting)
                    }
                    Button(enabled = !busy && share && subject.isNotBlank() && body.isNotBlank(), onClick = {
                        busy = true; error = null
                        scope.launch {
                            val result = repository.demoMutation("support-new-${plant.id}", "service-requests/", JSONObject()
                                .put("plant_id", plant.id).put("subject", subject.trim()).put("body", body.trim())
                                .put("share_context", share).put("share_placement", placement))
                            if (repository.isCurrentSession()) when (result) {
                                is ApiResult.Success -> { selectedId = result.value.getString("id"); subject = ""; body = ""; share = false; placement = false; refresh++ }
                                is ApiResult.Failure -> error = result.message
                            }
                            busy = false
                        }
                    }) { Text("Send request") }
                }
                error?.let { Text(it, color = PlantupiaColors.errorText); TextButton(onClick = { refresh++ }) { Text("Retry support") } }
            }
        } else {
            thread?.let { Text(supportStatusLabel(it.getString("status")), style = PlantupiaText.supporting) }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            androidx.compose.foundation.lazy.LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { thread?.let { ticket ->
                    Text(ticket.getString("subject"), style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                } }
                items(messages.size) { index ->
                    val message = messages[index]
                    PlantupiaConversationMessage(message.getString("body"), !message.getBoolean("from_staff"),
                        if (message.getBoolean("from_staff")) "Team · @" + message.getString("sender") else "You",
                        message.getString("created_at").take(16).replace("T", " · ") + " UTC")
                }
            }
            error?.let { Text(it, color = PlantupiaColors.errorText, style = PlantupiaText.supporting) }
            Row {
                TextButton(onClick = { refresh++ }) { Text("Refresh replies") }
                TextButton(onClick = { selectedId = null; thread = null }) { Text("All requests") }
            }
            if (humanAccess && thread?.getString("status") != "closed") {
                PlantupiaConversationComposer(reply, { reply = it.take(2000) }, enabled = !busy && !loading, placeholder = "Reply to the team…", onSend = {
                    val id = selectedId ?: return@PlantupiaConversationComposer
                    busy = true
                    scope.launch {
                        when (val result = repository.demoMutation("support-reply-$id", "service-requests/$id/", JSONObject().put("body", reply.trim()))) {
                            is ApiResult.Success -> if (repository.isCurrentSession()) { reply = ""; refresh++ }
                            is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 402) humanAccess = false }
                        }
                        busy = false
                    }
                })
            } else if (!loading && thread?.getString("status") != "closed") TextButton(onClick = { selectedId = null }) { Text("Optional care access for new replies") }
        }
        Spacer(Modifier.height(8.dp))
    }
}

internal fun supportStatusLabel(status: String): String = when (status) {
    "replied" -> "Team replied"
    "closed" -> "Closed · read-only"
    else -> "Awaiting a team reply"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaQaShop(
    repository: PlantupiaRepository,
    accountId: Long = 0L,
    onDismiss: () -> Unit,
    fullPage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var products by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = products.firstOrNull { it.optString("id") == selectedId }
    var linkedOrder by remember { mutableStateOf<JSONObject?>(null) }
    var detailOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var quantity by rememberSaveable { mutableIntStateOf(1) }
    var cart by remember { mutableStateOf<JSONObject?>(null) }
    var orders by remember { mutableStateOf(emptyList<JSONObject>()) }
    var screen by rememberSaveable { mutableStateOf("catalog") }
    var category by rememberSaveable { mutableStateOf("all") }
    var searchText by rememberSaveable { mutableStateOf("") }
    var confirmed by remember { mutableStateOf(false) }
    var receipt by rememberSaveable { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    fun changeCart(productId: String, nextQuantity: Int) {
        if (busy) return
        busy = true
        scope.launch {
            try {
                when (val result = repository.demoPost("cart/", JSONObject()
                    .put("product_id", productId).put("quantity", nextQuantity))) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        cart = result.value
                        confirmed = false
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
                }
            } finally {
                busy = false
            }
        }
    }
    LaunchedEffect(refresh, screen) {
        error = null
        val path = when (screen) {
            "cart", "review" -> "cart/"
            "orders" -> "demo/orders/"
            "order-detail" -> detailOrderId?.let { "demo/orders/$it/" } ?: "demo/orders/"
            else -> "catalog/"
        }
        when (val result = repository.demoGet(path)) {
            is ApiResult.Success -> if (repository.isCurrentSession()) when (screen) {
                "cart", "review" -> { cart = result.value; confirmed = false }
                "orders" -> orders = result.value.demoRows()
                "order-detail" -> {
                    if (result.value.has("lines")) linkedOrder = result.value
                    else orders = result.value.demoRows()
                }
                else -> products = result.value.demoRows()
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
    }
    androidx.activity.compose.BackHandler(enabled = fullPage && (selectedId != null || screen != "catalog")) {
        if (selectedId != null) selectedId = null
        else if (screen == "review") { confirmed = false; screen = "cart" }
        else if (screen == "order-detail") { detailOrderId = null; screen = "orders" }
        else screen = "catalog"
    }
    val ordersAccount = PlantupiaShopOpenSignal.ordersAccount
    LaunchedEffect(ordersAccount, accountId) {
        if (ordersAccount != null && ordersAccount == accountId) {
            PlantupiaShopOpenSignal.consumeOrders()
            selectedId = null
            screen = "orders"
            refresh++
        }
    }
    val orderTarget = PlantupiaShopOpenSignal.target
    LaunchedEffect(orderTarget) {
        val target = orderTarget ?: return@LaunchedEffect
        if (target.first != accountId) return@LaunchedEffect
        PlantupiaShopOpenSignal.consume()
        scope.launch {
        when (val result = repository.demoGet("demo/orders/${target.second}/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                linkedOrder = result.value; selectedId = null
                detailOrderId = target.second; screen = "order-detail"; refresh++
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = "This order is unavailable for the current account."
        }
        }
    }
    val shopContent: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("PLANTUPIA · SHOP", style = PlantupiaText.sectionLabel)
                    Text(
                        when {
                            selected != null -> "Product details"
                            screen == "cart" -> "Your cart"
                            screen == "review" -> "Review your order"
                            screen == "orders" -> "Your orders"
                            screen == "order-detail" -> "Order details"
                            else -> "Plants & essentials"
                        },
                        style = PlantupiaText.editorialHeading,
                    )
                }
                if (screen != "catalog" || selected != null) {
                    TextButton(onClick = { selectedId = null; screen = "catalog" }) {
                        Text("Shop", color = demoForest)
                    }
                }
            }
            // One Shop destination; cart and orders are contextual routes, not permanent tabs.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { selectedId = null; screen = "cart" }) {
                    Text("Cart", color = demoForest)
                }
                TextButton(onClick = { selectedId = null; screen = "orders" }) {
                    Text("My orders", color = demoForest)
                }
                if (!fullPage) TextButton(onClick = onDismiss) { Text("Close", color = demoForest) }
            }
            if (screen == "review" || screen == "cart") {
                Text("QA checkout only · illustrative prices · no payment or delivery", style = PlantupiaText.supporting)
            }
            when (screen) {
                "cart", "review" -> cart?.let { quote ->
                    if (quote.demoRows().isEmpty()) Text("Your cart is empty.", style = PlantupiaText.supporting)
                    quote.demoRows().forEach { line ->
                        val product = line.getJSONObject("product")
                        val productId = product.getString("id")
                        val qty = line.getInt("quantity")
                        DemoCard {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Box(Modifier.width(84.dp)) {
                                    if (product.optBoolean("has_image")) {
                                        PlantupiaCatalogPhoto(productId, repository, thumbnail = true)
                                    } else {
                                        PlantupiaCatalogMissingPhoto(compact = true, thumbnail = true)
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        product.getString("name").removePrefix("FICTIONAL QA · ").removePrefix("FICTIONAL QA "),
                                        fontWeight = FontWeight.SemiBold,
                                        color = demoForest,
                                    )
                                    if (product.optString("variant_label").isNotBlank()) {
                                        Text(product.optString("variant_label"), style = PlantupiaText.supporting)
                                    }
                                    Text("${product.getString("currency")} ${product.getString("price")}", color = demoForest)
                                    if (screen == "review") {
                                        Text("Quantity: ${qty}", style = PlantupiaText.supporting)
                                    } else {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Qty", modifier = Modifier.weight(1f), style = PlantupiaText.supporting)
                                            TextButton(enabled = !busy && qty > 1,
                                                onClick = { changeCart(productId, qty - 1) }) { Text("−") }
                                            Text(qty.toString(), fontWeight = FontWeight.SemiBold)
                                            TextButton(enabled = !busy && qty < minOf(10, product.optInt("qa_stock", 0)),
                                                onClick = { changeCart(productId, qty + 1) }) { Text("+") }
                                        }
                                    }
                                }
                            }
                            if (screen == "cart") {
                                TextButton(
                                    enabled = !busy,
                                    onClick = { changeCart(productId, 0) },
                                ) { Text("Remove from cart", color = demoForest) }
                            }
                        }
                    }
                    Text("Total · EGP ${quote.getString("total_provisional")}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    if (screen == "cart") {
                        Button(
                            enabled = !busy && quote.demoRows().isNotEmpty(),
                            onClick = { confirmed = false; screen = "review" },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = demoForest),
                        ) { Text("Review preview order") }
                    } else {
                        Text("Nothing is charged or delivered. This confirms only a QA order request.",
                            style = PlantupiaText.supporting)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(confirmed, { confirmed = it }, modifier = Modifier.semantics { contentDescription = "Confirm preview-only order" })
                            Text("Preview order only · no charge or delivery", modifier = Modifier.weight(1f), style = PlantupiaText.supporting)
                        }
                        Button(enabled = !busy && confirmed && quote.demoRows().isNotEmpty(), onClick = { busy = true; scope.launch {
                            when (val result = repository.demoMutation("checkout", "demo/orders/", JSONObject().put("cart_fingerprint", quote.getString("fingerprint")).put("confirm_no_charge", true))) {
                                is ApiResult.Success -> if (repository.isCurrentSession()) { receipt = result.value.getString("id"); screen = "orders"; refresh++ }
                                is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 409) refresh++ }
                            }; busy = false
                        } }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), colors = ButtonDefaults.buttonColors(containerColor = demoForest)) { Text("Place preview order") }
                        TextButton(onClick = { confirmed = false; screen = "cart" }) {
                            Text("Edit cart", color = demoForest)
                        }
                    }
                }
                "order-detail" -> {
                    val detail = (orders + listOfNotNull(linkedOrder))
                        .firstOrNull { it.optString("id") == detailOrderId }
                    if (detail == null) Text("Loading order details…", style = PlantupiaText.supporting)
                    else {
                        Text("Order " + detail.optString("id").take(8),
                            style = PlantupiaText.editorialHeading)
                        Text("Submitted · " + detail.optString("created_at").take(10),
                            style = PlantupiaText.supporting)
                        val prepared = detail.optString("status") == "draft_prepared"
                        DemoCard {
                            Text(if (prepared) "A staff draft has been prepared"
                                else "Your preview request was received",
                                color = demoForest, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (prepared)
                                    "A draft is not a delivered plant. The team still must verify real plants, approve installation and issue a recipient-bound handover."
                                else "The team may review this QA request. No delivery or installation has started.",
                                style = PlantupiaText.supporting,
                            )
                        }
                        Text("Selected items", style = PlantupiaText.sectionLabel)
                        detail.demoRows("lines").forEach { item ->
                            DemoCard {
                                Text(item.optString("name").removePrefix("FICTIONAL QA · "),
                                    fontWeight = FontWeight.SemiBold)
                                if (item.optString("variant_label").isNotBlank()) Text(
                                    "Option: " + item.optString("variant_label"), style = PlantupiaText.supporting)
                                if (item.optString("sku").isNotBlank()) Text(
                                    "SKU: " + item.optString("sku"), style = PlantupiaText.supporting)
                                Text("${item.getInt("quantity")} × EGP ${item.optString("unit_price_provisional")}",
                                    style = PlantupiaText.body)
                            }
                        }
                        Text("Concept total · EGP " + detail.optString("total_provisional"),
                            style = PlantupiaText.editorialHeading)
                        Text("No payment or physical delivery has been initiated.",
                            style = PlantupiaText.supporting)
                        TextButton(onClick = { detailOrderId = null; screen = "orders" }) {
                            Text("Back to all orders", color = demoForest)
                        }
                    }
                }
                "orders" -> {
                    receipt?.let { Text("Preview order received · " + it.take(8), fontWeight = FontWeight.SemiBold, color = demoForest) }
                    if (orders.isEmpty()) Text("No orders yet.", style = PlantupiaText.supporting)
                    (orders + listOfNotNull(linkedOrder)).distinctBy { it.getString("id") }.forEach { order -> DemoCard {
                        Text("Order " + order.getString("id").take(8), fontWeight = FontWeight.SemiBold)
                        val statusLabel = when (order.getString("status")) {
                            "draft_prepared" -> "Draft prepared · awaiting review"
                            "qa_requested" -> "Preview request received"
                            else -> "Status unavailable"
                        }
                        Text("$statusLabel · ${order.getString("created_at").take(10)}", style = PlantupiaText.supporting)
                        order.demoRows("lines").forEach { line ->
                            Text("${line.getInt("quantity")} × ${line.getString("name").removePrefix("FICTIONAL QA · ").removePrefix("FICTIONAL QA ")}")
                        }
                        Text("Total · EGP ${order.getString("total_provisional")}", color = demoForest)
                        Text("No payment or delivery was initiated.", style = PlantupiaText.supporting)
                        TextButton(onClick = {
                            detailOrderId = order.getString("id")
                            screen = "order-detail"
                        }) { Text("View order details →", color = demoForest) }
                    } }
                }
                else -> {
                    val product = selected
                    if (product == null) {
                        Text("Find something for your space.", style = PlantupiaText.body)
                        Text("Preview collection · illustrative prices only", style = PlantupiaText.supporting)
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Search plants, pots and care") },
                            singleLine = true,
                        )
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("all" to "All", "plant" to "Plants", "planter" to "Pots",
                                "accessory" to "Care").forEach { (filter, label) ->
                                Surface(
                                    onClick = { category = filter },
                                    modifier = Modifier.weight(1f),
                                    color = if (category == filter) demoForest else demoSage,
                                    shape = RoundedCornerShape(12.dp),
                                ) {
                                    Text(label, modifier = Modifier.padding(vertical = 11.dp),
                                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        color = if (category == filter) demoPaper else demoForest)
                                }
                            }
                        }
                        val visible = products.filter { item ->
                            (category == "all" || item.optString("kind") == category) &&
                                (searchText.isBlank() || listOf(item.optString("name"),
                                    item.optString("description"), item.optString("sku"),
                                    item.optString("variant_label")).any {
                                    it.contains(searchText.trim(), ignoreCase = true)
                                })
                        }
                        if (visible.isEmpty()) Text(
                            if (products.isEmpty()) "Products coming soon." else "No products match this search.",
                            style = PlantupiaText.supporting,
                        )
                        // One product family in discovery; selection of its exact
                        // priced SKU happens inside the detail page.
                        val familyPreview = visible.distinctBy { item ->
                            item.optString("family_key").takeIf { it.isNotBlank() }?.let {
                                "family:" + item.optString("kind") + ":" + it
                            } ?: "sku:" + item.getString("id")
                        }
                        familyPreview.chunked(2).forEach { group ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                group.forEach { item ->
                                    PlantupiaCatalogTile(
                                        item = item,
                                        repository = repository,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { selectedId = item.getString("id"); quantity = 1 },
                                    )
                                }
                                if (group.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            var selectedPhotoId by remember(selectedId) { mutableStateOf<String?>(null) }
                            val gallery = product.optJSONArray("photos")
                            if (product.optBoolean("has_image") || selectedPhotoId != null) {
                                PlantupiaCatalogPhoto(
                                    product.getString("id"), repository,
                                    photoId = selectedPhotoId,
                                    photoAlt = if (selectedPhotoId == null) product.optString("image_description")
                                    else (0 until (gallery?.length() ?: 0)).map { gallery!!.getJSONObject(it) }
                                        .firstOrNull { it.optString("id") == selectedPhotoId }?.optString("alt_text").orEmpty(),
                                )
                            } else {
                                PlantupiaCatalogMissingPhoto(compact = false)
                            }
                            if (product.optBoolean("has_image") && (gallery?.length() ?: 0) > 0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TextButton(onClick = { selectedPhotoId = null }) {
                                        Text("Cover photo", color = demoForest)
                                    }
                                    (0 until minOf(gallery!!.length(), 8)).forEach { index ->
                                        val photo = gallery.getJSONObject(index)
                                        TextButton(onClick = { selectedPhotoId = photo.getString("id") }) {
                                            Text("Photo ${index + 2}", color = demoForest)
                                        }
                                    }
                                }
                            } else if ((gallery?.length() ?: 0) > 0) {
                                Column {
                                    (0 until minOf(gallery!!.length(), 8)).forEach { index ->
                                        val photo = gallery.getJSONObject(index)
                                        TextButton(onClick = { selectedPhotoId = photo.getString("id") }) {
                                            Text("View photo ${index + 1}", color = demoForest)
                                        }
                                    }
                                }
                            }
                            Text(
                                product.getString("name").removePrefix("FICTIONAL QA · "),
                                fontFamily = FontFamily.Serif, fontSize = 27.sp, color = demoForest,
                            )
                            if (!product.getString("description").startsWith("Fictional Plantupia software QA product")) {
                                Text(product.getString("description"), style = PlantupiaText.body)
                            }
                            val familyKey = product.optString("family_key")
                            val sameFamily = if (familyKey.isNotBlank()) products.filter {
                                it.optString("family_key") == familyKey &&
                                    it.optString("kind") == product.optString("kind")
                            } else listOf(product)
                            if (sameFamily.size > 1) {
                                Text("Choose an option", style = PlantupiaText.sectionLabel)
                                sameFamily.forEach { option ->
                                    val selectedOption = option.getString("id") == product.getString("id")
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                        onClick = { selectedId = option.getString("id"); quantity = 1 },
                                        enabled = !busy,
                                        border = BorderStroke(1.dp,
                                            if (selectedOption) demoForest else PlantupiaColors.border),
                                    ) {
                                        Text(
                                            (option.optString("variant_label").ifBlank {
                                                option.optString("sku").ifBlank { "Option" }
                                            }) + " · EGP " + option.getString("price") +
                                                if (option.optInt("qa_stock") == 0) " · Unavailable" else "",
                                            color = demoForest,
                                        )
                                    }
                                }
                            }
                            if (product.optString("sku").isNotBlank())
                                Text("SKU · " + product.optString("sku"), style = PlantupiaText.supporting)
                            Text(
                                "${product.getString("currency")} ${product.getString("price")}",
                                fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = demoForest,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("Quantity", modifier = Modifier.weight(1f), style = PlantupiaText.body)
                                TextButton(enabled = !busy && quantity > 1, onClick = { quantity-- }) { Text("−") }
                                Text(quantity.toString(), fontWeight = FontWeight.SemiBold)
                                TextButton(
                                    enabled = !busy && quantity < minOf(10, product.getInt("qa_stock")),
                                    onClick = { quantity++ },
                                ) { Text("+") }
                            }
                            Button(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = demoForest),
                                enabled = !busy && product.getInt("qa_stock") >= quantity,
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        when (val result = repository.demoPost(
                                            "cart/",
                                            JSONObject().put("product_id", product.getString("id")).put("quantity", quantity),
                                        )) {
                                            is ApiResult.Success -> if (repository.isCurrentSession()) {
                                                cart = result.value
                                                selectedId = null
                                                screen = "cart"
                                            }
                                            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
                                        }
                                        busy = false
                                    }
                                },
                            ) { Text("Add to cart") }
                            TextButton(onClick = { selectedId = null }) { Text("← Back to shop") }
                        }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(if (it.contains("nonstaff customer account", ignoreCase = true)) "Switch to a customer account to shop." else it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { refresh++ }) { Text("Retry") } }
            if (!fullPage) TextButton(onClick = onDismiss) { Text("Back to Plants") }
            Spacer(Modifier.height(if (fullPage) 112.dp else 24.dp))
        }
    }
    if (fullPage) {
        Surface(modifier = modifier.fillMaxSize(), color = demoPaper) { shopContent() }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = demoPaper,
        ) { shopContent() }
    }
}

@Composable
private fun PlantupiaCatalogMissingPhoto(compact: Boolean, thumbnail: Boolean = false) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (thumbnail) 84.dp else if (compact) 148.dp else 245.dp),
        color = demoSage,
        shape = RoundedCornerShape(13.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("Photograph not supplied", style = PlantupiaText.supporting)
        }
    }
}

@Composable
private fun PlantupiaCatalogTile(
    item: JSONObject,
    repository: PlantupiaRepository,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.semantics { contentDescription = "View QA product: " + item.getString("name") },
        color = demoPaper,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Column(
            modifier = Modifier.padding(9.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (item.optBoolean("has_image")) {
                PlantupiaCatalogPhoto(item.getString("id"), repository, compact = true)
            } else {
                PlantupiaCatalogMissingPhoto(compact = true)
            }
            Text(
                item.getString("kind").uppercase(),
                fontSize = 10.sp, color = PlantupiaColors.secondaryText,
            )
            Text(
                item.getString("name").removePrefix("FICTIONAL QA · "),
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = demoForest,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${item.getString("currency")} ${item.getString("price")}",
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = demoForest,
            )
            Text("Explore product →", fontSize = 11.sp, fontWeight = FontWeight.Medium,
                color = demoForest)
        }
    }
}

@Composable
private fun PlantupiaCatalogPhoto(
    productId: String, repository: PlantupiaRepository, compact: Boolean = false,
    thumbnail: Boolean = false, photoId: String? = null, photoAlt: String = "",
) {
    var bitmap by remember(productId, photoId) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(productId, photoId) { mutableStateOf(false) }
    var revision by remember(productId, photoId) { mutableIntStateOf(0) }
    LaunchedEffect(productId, photoId, revision) {
        failed = false
        val resource = "catalog/$productId/image/" +
            (if (photoId == null) "" else "?photo_id=$photoId")
        when (val result = repository.demoGet(resource)) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                bitmap = runCatching {
                    val bytes = Base64.decode(result.value.getString("image_base64"), Base64.DEFAULT)
                    require(bytes.size <= 2 * 1024 * 1024)
                    requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size)).asImageBitmap()
                }.getOrNull()
                failed = bitmap == null
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) failed = true
        }
    }
    bitmap?.let { Image(it, photoAlt.ifBlank { "Actual uploaded catalogue photograph" },
        modifier = Modifier.fillMaxWidth()
            .height(if (thumbnail) 84.dp else if (compact) 148.dp else 245.dp)
            .clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop) }
    if (failed) TextButton(onClick = { revision++ }) { Text("Retry photo") }
    else if (bitmap == null) LinearProgressIndicator(Modifier.fillMaxWidth())
}
