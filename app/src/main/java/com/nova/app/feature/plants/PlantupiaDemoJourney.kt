package com.nova.app.feature.plants

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
        if (!human && access?.optBoolean("ai_configured") != true) Text("The AI provider is not configured on this QA server. No model response is available. Your free care guide and history still work.", fontWeight = FontWeight.SemiBold)
        content { access = null; refresh++ }
    } else {
        Text("Care, when you need it.", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = demoForest)
        Text("Your plants and care schedule are always free.", color = demoForest)
        Text("Preview only · no payment will be collected", style = PlantupiaText.supporting)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlantupiaDemoHome(repository: PlantupiaRepository, plants: List<PlantupiaPlant>, onOpenPlant: (PlantupiaPlant) -> Unit) {
    var plans by remember { mutableStateOf(emptyList<JSONObject>()) }
    var available by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf(emptyList<JSONObject>()) }
    var message by remember { mutableStateOf<String?>(null) }
    var shop by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh) {
        pending = repository.pendingDemoMutations()
        when (val result = repository.demoGet("care-plans/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) { available = true; plans = result.value.demoRows() }
            is ApiResult.Failure -> if (repository.isCurrentSession() && result.statusCode != 404) message = "Inspection plans couldn't load. Retry when connected."
        }
    }
    if (available) {
        DemoCard {
            Text("YOUR CARE & SHOP", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            val optedIn = plans.filter { it.optBoolean("enabled") && it.optBoolean("valid") }
            if (optedIn.isEmpty()) Text("Explore plants & essentials.", fontSize = 13.sp)
            optedIn.sortedBy { it.getString("next_due_on") }.take(4).forEach { plan ->
                TextButton(onClick = { plants.firstOrNull { it.id == plan.getString("plant_id") }?.let(onOpenPlant) }) {
                    Text("${if (plan.optBoolean("due")) "Due · " else "Next · "}${plan.getString("plant_name")} · ${plan.getString("action")} · ${plan.getString("next_due_on")}")
                }
            }
            OutlinedButton(onClick = { shop = true }, modifier = Modifier.fillMaxWidth()) { Text("Shop plants & essentials") }
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
    if (shop) PlantupiaQaShop(repository, onDismiss = { shop = false; refresh++ })
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
internal fun PlantupiaTeamSheet(plant: PlantupiaPlant, repository: PlantupiaRepository, onDismiss: () -> Unit) {
    var tickets by remember { mutableStateOf(emptyList<JSONObject>()) }
    var thread by remember { mutableStateOf<JSONObject?>(null) }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var reply by remember { mutableStateOf("") }
    var share by remember { mutableStateOf(false) }
    var placement by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh) {
        when (val result = repository.demoGet("service-requests/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) tickets = result.value.demoRows().filter { it.getString("plant_id") == plant.id }
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
        val ticket = thread
        if (ticket != null) when (val result = repository.demoGet("service-requests/${ticket.getString("id")}/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) thread = result.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = demoPaper) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PlantupiaConversationHeader(category = "HUMAN SUPPORT", title = "Plantupia team",
                subtitle = plant.name + " · Private messages", onClose = onDismiss)
            Text("A team member replies when available.", style = PlantupiaText.supporting)
            // Existing shared threads remain readable after expiry; new writes require access.
            if (thread == null) tickets.forEach { ticket ->
                Surface(onClick = { thread = ticket; refresh++ }, modifier = Modifier.fillMaxWidth(),
                    color = PlantupiaColors.softSage, shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PlantupiaColors.border)) {
                    Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(ticket.getString("subject"), style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold))
                            Text(ticket.getString("status"), style = PlantupiaText.supporting)
                        }
                        Text("Open →", style = PlantupiaText.sectionLabel)
                    }
                }
            }
            thread?.let { ticket ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(ticket.getString("subject"), style = PlantupiaText.editorialHeading)
                    Text(if (ticket.getString("status") == "closed") "Closed · read-only"
                        else "Open · waiting for a team reply", style = PlantupiaText.supporting)
                    ticket.optJSONArray("messages")?.let { messages ->
                        Column(
                            modifier = Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                        for (i in 0 until messages.length()) {
                            val message = messages.getJSONObject(i)
                            PlantupiaConversationMessage(
                                body = message.getString("body"),
                                mine = !message.getBoolean("from_staff"),
                                sender = if (message.getBoolean("from_staff")) "Plantupia team" else "You",
                                detail = message.getString("created_at").take(16).replace("T", " · "),
                            )
                        }
                        }
                    }
                    TextButton(onClick = { refresh++ }) { Text("Refresh replies") }
                    TextButton(onClick = { thread = null; reply = "" }) { Text("All requests") }
                }
            }
            PlantupiaDemoAccessGate(repository, human = true) { accessLost ->
                if (thread == null) {
                    OutlinedTextField(subject, { subject = it.take(140) }, label = { Text("Request subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it.take(2000) }, label = { Text("What do you want the team to know?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row { Checkbox(share, { share = it }, modifier = Modifier.semantics { contentDescription = "Share this request and plant label with authorized support staff" }); Text("Share this message and plant name with the team.", modifier = Modifier.weight(1f), style = PlantupiaText.supporting) }
                    Row { Checkbox(placement, { placement = it }, modifier = Modifier.semantics { contentDescription = "Also share recorded placement" }); Text("Include placement (optional). No photos or history.", modifier = Modifier.weight(1f), style = PlantupiaText.supporting) }
                    Button(enabled = !busy && share && subject.isNotBlank() && body.isNotBlank(), onClick = {
                        busy = true; error = null
                        scope.launch {
                            val result = repository.demoMutation("support-new-${plant.id}", "service-requests/", JSONObject()
                                .put("plant_id", plant.id).put("subject", subject.trim()).put("body", body.trim())
                                .put("share_context", share).put("share_placement", placement))
                            if (repository.isCurrentSession()) when (result) {
                                is ApiResult.Success -> { subject = ""; body = ""; share = false; placement = false; refresh++ }
                                is ApiResult.Failure -> { error = result.message; if (result.statusCode == 402) accessLost() }
                            }
                            busy = false
                        }
                    }) { Text("Send request") }
                } else if (thread?.getString("status") != "closed") {
                    PlantupiaConversationComposer(
                        value = reply,
                        onChange = { reply = it.take(2000) },
                        enabled = !busy,
                        placeholder = "Reply to the team…",
                        onSend = {
                        val id = thread!!.getString("id")
                        busy = true
                        scope.launch {
                            when (val result = repository.demoMutation("support-reply-$id", "service-requests/$id/", JSONObject().put("body", reply.trim()))) {
                                is ApiResult.Success -> if (repository.isCurrentSession()) { reply = ""; refresh++ }
                                is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 402) accessLost() }
                            }
                            busy = false
                        }
                    })
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            
            Spacer(Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlantupiaQaShop(repository: PlantupiaRepository, onDismiss: () -> Unit) {
    var products by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var quantity by remember { mutableIntStateOf(1) }
    var cart by remember { mutableStateOf<JSONObject?>(null) }
    var orders by remember { mutableStateOf(emptyList<JSONObject>()) }
    var screen by remember { mutableStateOf("catalog") }
    var confirmed by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(refresh, screen) {
        error = null
        val path = when (screen) { "cart" -> "cart/"; "orders" -> "demo/orders/"; else -> "catalog/" }
        when (val result = repository.demoGet(path)) {
            is ApiResult.Success -> if (repository.isCurrentSession()) when (screen) {
                "cart" -> { cart = result.value; confirmed = false }
                "orders" -> orders = result.value.demoRows()
                else -> products = result.value.demoRows()
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
        }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = demoPaper) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (selected == null) "Shop" else "Product", fontFamily = FontFamily.Serif, fontSize = 30.sp, color = demoForest)
            Text("Preview · no payment or delivery", style = PlantupiaText.supporting)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("catalog" to "Shop", "cart" to "Cart", "orders" to "My orders").forEach { (destination, label) ->
                    Surface(
                        onClick = { screen = destination; selected = null },
                        modifier = Modifier.weight(1f),
                        color = if (screen == destination) demoForest else demoSage,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            label,
                            modifier = Modifier.padding(vertical = 11.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color = if (screen == destination) demoPaper else demoForest,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
            when (screen) {
                "cart" -> cart?.let { quote ->
                    if (quote.demoRows().isEmpty()) Text("Your cart is empty.", style = PlantupiaText.supporting)
                    quote.demoRows().forEach { line ->
                        val product = line.getJSONObject("product")
                        DemoCard {
                            Text(product.getString("name").removePrefix("FICTIONAL QA · "), fontWeight = FontWeight.SemiBold)
                            Text("${line.getInt("quantity")} × ${product.getString("currency")} ${product.getString("price")}")
                            TextButton(enabled = !busy, onClick = { busy = true; scope.launch {
                                when (val result = repository.demoPost("cart/", JSONObject().put("product_id", product.getString("id")).put("quantity", 0))) {
                                    is ApiResult.Success -> if (repository.isCurrentSession()) { cart = result.value; confirmed = false }
                                    is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
                                }; busy = false
                            } }) { Text("Remove from cart") }
                        }
                    }
                    Text("Total · EGP ${quote.getString("total_provisional")}", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
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
                }
                "orders" -> {
                    receipt?.let { Text("Preview order received · " + it.take(8), fontWeight = FontWeight.SemiBold, color = demoForest) }
                    if (orders.isEmpty()) Text("No orders yet.", style = PlantupiaText.supporting)
                    orders.forEach { order -> DemoCard {
                        Text("Order " + order.getString("id").take(8), fontWeight = FontWeight.SemiBold)
                        Text("${order.getString("status")} · ${order.getString("created_at").take(10)}", style = PlantupiaText.supporting)
                        order.demoRows("lines").forEach { line -> Text("${line.getInt("quantity")} × ${line.getString("name")}") }
                        Text("EGP ${order.getString("total_provisional")} · charged: no", color = demoForest)
                        Text("No payment or delivery was initiated.", style = PlantupiaText.supporting)
                    } }
                }
                else -> {
                    val product = selected
                    if (product == null) {
                        if (products.isEmpty()) Text("Products coming soon.", style = PlantupiaText.supporting)
                        products.chunked(2).forEach { group ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                group.forEach { item ->
                                    PlantupiaCatalogTile(
                                        item = item,
                                        repository = repository,
                                        modifier = Modifier.weight(1f),
                                        onOpen = { selected = item; quantity = 1 },
                                    )
                                }
                                if (group.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (product.optBoolean("has_image")) {
                                PlantupiaCatalogPhoto(product.getString("id"), repository)
                            } else {
                                PlantupiaCatalogMissingPhoto(compact = false)
                            }
                            Text(
                                product.getString("name").removePrefix("FICTIONAL QA · "),
                                fontFamily = FontFamily.Serif, fontSize = 27.sp, color = demoForest,
                            )
                            if (!product.getString("description").startsWith("Fictional Plantupia software QA product")) {
                                Text(product.getString("description"), style = PlantupiaText.body)
                            }
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
                                                selected = null
                                                screen = "cart"
                                            }
                                            is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
                                        }
                                        busy = false
                                    }
                                },
                            ) { Text("Add to cart") }
                            TextButton(onClick = { selected = null }) { Text("← Back to shop") }
                        }
                    }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(if (it.contains("nonstaff customer account", ignoreCase = true)) "Switch to a customer account to shop." else it, color = MaterialTheme.colorScheme.error); TextButton(onClick = { refresh++ }) { Text("Retry") } }
            TextButton(onClick = onDismiss) { Text("Back to My Space") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlantupiaCatalogMissingPhoto(compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(if (compact) 148.dp else 245.dp),
        color = demoSage,
        shape = RoundedCornerShape(13.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text("Photo coming soon", style = PlantupiaText.supporting)
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
        }
    }
}

@Composable
private fun PlantupiaCatalogPhoto(productId: String, repository: PlantupiaRepository, compact: Boolean = false) {
    var bitmap by remember(productId) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(productId) { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    LaunchedEffect(productId, revision) {
        failed = false
        when (val result = repository.demoGet("catalog/$productId/image/")) {
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
    bitmap?.let { Image(it, "Uploaded catalog photograph", modifier = Modifier.fillMaxWidth().height(if (compact) 148.dp else 245.dp).clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.Crop) }
    if (failed) TextButton(onClick = { revision++ }) { Text("Retry photo") }
    else if (bitmap == null) LinearProgressIndicator(Modifier.fillMaxWidth())
}
