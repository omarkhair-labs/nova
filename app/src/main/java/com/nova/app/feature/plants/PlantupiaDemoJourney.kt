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
        Text("Preview access · no payment", fontSize = 11.sp, color = demoForest)
        if (access == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        access?.let { state ->
            state.demoRows("offers").forEach { offer ->
                DemoCard {
                    Text(offer.getString("name"), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${offer.getString("currency")} ${offer.getString("price")} / month", fontSize = 18.sp, color = demoForest)
                    Text("${offer.getInt("ai_daily_limit")} AI questions / day", fontSize = 14.sp)
                    Text(if (offer.getInt("human_limit") > 0) "${offer.getInt("human_limit")} team requests / month" else "AI plant guidance", fontSize = 14.sp)
                    Text("${offer.getInt("qa_hours")}-hour trial · no charge", fontSize = 11.sp)
                    Button(enabled = !busy && (!human || offer.getInt("human_limit") > 0), onClick = {
                        busy = true; error = null
                        scope.launch {
                            val result = repository.demoMutation("access", "demo/access/", JSONObject()
                                .put("offer", offer.getString("key")).put("confirm_no_payment", true))
                            if (repository.isCurrentSession()) when (result) {
                                is ApiResult.Success -> { access = null; refresh++ }
                                is ApiResult.Failure -> error = result.message
                            }
                            busy = false
                        }
                    }) { Text(if (busy) "Activating…" else "Try this plan · no charge") }
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
            Text("TODAY · INSPECTION PLANS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            val optedIn = plans.filter { it.optBoolean("enabled") && it.optBoolean("valid") }
            if (optedIn.isEmpty()) Text("Company proposals are optional. Open a verified plant to review and opt in.")
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
            Text("Ask the Plantupia team", fontFamily = FontFamily.Serif, fontSize = 27.sp, color = demoForest)
            Text(plant.name + " · a private service request, separate from Circle and social DMs.")
            Text("A real employee must reply. No staff presence or response time is promised.", fontSize = 12.sp)
            // Existing shared threads remain readable after expiry; new writes require access.
            tickets.forEach { ticket -> OutlinedButton(onClick = { thread = ticket; refresh++ }, modifier = Modifier.fillMaxWidth()) { Text("${ticket.getString("subject")} · ${ticket.getString("status")}") } }
            thread?.let { ticket ->
                DemoCard {
                    Text(ticket.getString("subject"), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text("Status: ${ticket.getString("status")}")
                    ticket.optJSONArray("messages")?.let { messages ->
                        for (i in 0 until messages.length()) {
                            val message = messages.getJSONObject(i)
                            Text("${if (message.getBoolean("from_staff")) "Staff reply" else "You"} · ${message.getString("sender")} · ${message.getString("created_at").take(16)}", fontSize = 11.sp)
                            Text(message.getString("body"))
                        }
                    }
                    TextButton(onClick = { refresh++ }) { Text("Refresh actual replies") }
                    TextButton(onClick = { thread = null; reply = "" }) { Text("Back to request form") }
                }
            }
            PlantupiaDemoAccessGate(repository, human = true) { accessLost ->
                if (thread == null) {
                    OutlinedTextField(subject, { subject = it.take(140) }, label = { Text("Request subject") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(body, { body = it.take(2000) }, label = { Text("What do you want the team to know?") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row { Checkbox(share, { share = it }, modifier = Modifier.semantics { contentDescription = "Share this request and plant label with authorized support staff" }); Text("Share this message and selected plant label with authorized support staff.", modifier = Modifier.weight(1f)) }
                    Row { Checkbox(placement, { placement = it }, modifier = Modifier.semantics { contentDescription = "Also share recorded placement" }); Text("Also share the recorded placement. No history or photos are shared.", modifier = Modifier.weight(1f)) }
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
                    }) { Text("Send real support request") }
                } else if (thread?.getString("status") != "closed") {
                    OutlinedTextField(reply, { reply = it.take(2000) }, label = { Text("Your follow-up message") }, modifier = Modifier.fillMaxWidth())
                    Button(enabled = !busy && reply.isNotBlank(), onClick = {
                        val id = thread!!.getString("id")
                        busy = true
                        scope.launch {
                            when (val result = repository.demoMutation("support-reply-$id", "service-requests/$id/", JSONObject().put("body", reply.trim()))) {
                                is ApiResult.Success -> if (repository.isCurrentSession()) { reply = ""; refresh++ }
                                is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 402) accessLost() }
                            }
                            busy = false
                        }
                    }) { Text("Send follow-up") }
                }
            }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            TextButton(onClick = onDismiss) { Text("Back to " + plant.name) }
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
            Text("Plantupia Shop", fontFamily = FontFamily.Serif, fontSize = 28.sp, color = demoForest)
            Text("Preview catalog · no payment or delivery", fontSize = 11.sp, color = demoForest)
            Row { TextButton(onClick = { screen = "catalog"; selected = null }) { Text("Catalog") }; TextButton(onClick = { screen = "cart"; selected = null }) { Text("Cart") }; TextButton(onClick = { screen = "orders"; selected = null }) { Text("My orders") } }
            when (screen) {
                "cart" -> cart?.let { quote ->
                    if (quote.demoRows().isEmpty()) Text("Your QA cart is empty. Choose an item from the catalog.")
                    quote.demoRows().forEach { line ->
                        val product = line.getJSONObject("product")
                        DemoCard {
                            Text(product.getString("name"), fontWeight = FontWeight.SemiBold)
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
                    Row { Checkbox(confirmed, { confirmed = it }, modifier = Modifier.semantics { contentDescription = "Confirm QA order with no charge or delivery" }); Text("I confirm a QA order only. No charge, delivery or real stock commitment.", modifier = Modifier.weight(1f)) }
                    Button(enabled = !busy && confirmed && quote.demoRows().isNotEmpty(), onClick = { busy = true; scope.launch {
                        when (val result = repository.demoMutation("checkout", "demo/orders/", JSONObject().put("cart_fingerprint", quote.getString("fingerprint")).put("confirm_no_charge", true))) {
                            is ApiResult.Success -> if (repository.isCurrentSession()) { receipt = result.value.getString("id"); screen = "orders"; refresh++ }
                            is ApiResult.Failure -> if (repository.isCurrentSession()) { error = result.message; if (result.statusCode == 409) refresh++ }
                        }; busy = false
                    } }) { Text("Confirm no-charge QA order") }
                }
                "orders" -> {
                    receipt?.let { Text("QA order received · $it. No payment or delivery was initiated.", fontWeight = FontWeight.SemiBold) }
                    if (orders.isEmpty()) Text("No QA orders yet.")
                    orders.forEach { order -> DemoCard {
                        Text("Order " + order.getString("id").take(8), fontWeight = FontWeight.SemiBold)
                        Text("${order.getString("status")} · ${order.getString("created_at").take(16)}")
                        order.demoRows("lines").forEach { line -> Text("${line.getInt("quantity")} × ${line.getString("name")}") }
                        Text("EGP ${order.getString("total_provisional")} provisional · charged: no")
                        Text("An order is not a delivered plant. Staff review and any new DRAFT/handover are separate.", fontSize = 11.sp)
                    } }
                }
                else -> {
                    val product = selected
                    if (product == null) products.forEach { item -> DemoCard {
                        if (item.optBoolean("has_image")) PlantupiaCatalogPhoto(item.getString("id"), repository, compact = true)
                        Text(item.getString("name").removePrefix("FICTIONAL QA · "), fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text("${item.getString("currency")} ${item.getString("price")}", fontSize = 18.sp, color = demoForest)
                        OutlinedButton(onClick = { selected = item; quantity = 1 }, modifier = Modifier.semantics { contentDescription = "View QA product: " + item.getString("name") }) { Text("View product") }
                    } } else DemoCard {
                        if (product.optBoolean("has_image")) PlantupiaCatalogPhoto(product.getString("id"), repository)
                        Text(product.getString("name").removePrefix("FICTIONAL QA · "), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        if (!product.getString("description").startsWith("Fictional Plantupia software QA product")) Text(product.getString("description"), fontSize = 14.sp)
                        Text("${product.getString("currency")} ${product.getString("price")} · ${product.getInt("qa_stock")} available", fontSize = 18.sp, color = demoForest)
                        Row { TextButton(enabled = !busy && quantity > 1, onClick = { quantity-- }) { Text("−") }; Text("Quantity $quantity", Modifier.padding(top = 12.dp)); TextButton(enabled = !busy && quantity < 10, onClick = { quantity++ }) { Text("+") } }
                        Button(enabled = !busy && product.getInt("qa_stock") >= quantity, onClick = { busy = true; scope.launch {
                            when (val result = repository.demoPost("cart/", JSONObject().put("product_id", product.getString("id")).put("quantity", quantity))) {
                                is ApiResult.Success -> if (repository.isCurrentSession()) { cart = result.value; selected = null; screen = "cart" }
                                is ApiResult.Failure -> if (repository.isCurrentSession()) error = result.message
                            }; busy = false
                        } }) { Text("Save quantity to cart") }
                        TextButton(onClick = { selected = null }) { Text("Back to catalog") }
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
    bitmap?.let { Image(it, "Uploaded catalog photograph", modifier = Modifier.fillMaxWidth().height(if (compact) 148.dp else 260.dp), contentScale = ContentScale.Crop) }
    if (failed) TextButton(onClick = { revision++ }) { Text("Photograph unavailable · retry") }
    else if (bitmap == null) LinearProgressIndicator(Modifier.fillMaxWidth())
}
