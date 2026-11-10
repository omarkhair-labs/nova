package com.nova.app.feature.plants

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val storeInk = Color(0xFF123B2E)
private val storePaper = Color(0xFFF8F8F2)

internal fun realCheckoutAddressValid(
    name: String, phone: String, city: String, address: String,
    latitude: Double?, longitude: Double?, confirmed: Boolean,
): Boolean =
    plantupiaDeliveryAddressReady(name, phone, city, address, latitude, longitude, confirmed)

private fun displayedEGP(value: String): String = "EGP " + value

/**
 * The ONLY normal customer Shop.
 * Commercial SKU -> saved Cart -> customer-selected map pin -> real shipping
 * quote -> customer's explicit acceptance -> exact stock reservation -> Orders.
 *
 * There is deliberately no link to DemoOrder, QA cart, or QA-only checkout.
 * Payment and delivery status always come from genuine backend evidence.
 */
@Composable
internal fun PlantupiaStoreShop(
    repository: PlantupiaRepository,
    accountId: Long,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf("catalog") }
    var products by remember { mutableStateOf(emptyList<JSONObject>()) }
    var cart by remember { mutableStateOf<JSONObject?>(null) }
    var orders by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }

    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf("") }
    var building by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var latitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var longitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var pinConfirmed by rememberSaveable { mutableStateOf(false) }
    var paymentMethod by rememberSaveable { mutableStateOf("cod") }

    BackHandler(enabled = screen == "checkout") { screen = "cart" }
    val externalOrdersAccount = PlantupiaShopOpenSignal.ordersAccount
    LaunchedEffect(externalOrdersAccount, accountId) {
        if (externalOrdersAccount == accountId) {
            PlantupiaShopOpenSignal.consumeOrders()
            screen = "orders"
            refresh++
        }
    }
    // Legacy order push links must never reopen the fictional purchase flow.
    val oldOrderTarget = PlantupiaShopOpenSignal.target
    LaunchedEffect(oldOrderTarget, accountId) {
        if (oldOrderTarget?.first == accountId) {
            PlantupiaShopOpenSignal.consume()
            screen = "orders"
            feedback = "This earlier test-order link is not a commercial purchase."
        }
    }
    LaunchedEffect(screen, refresh) {
        loaded = false
        when (screen) {
            "catalog" -> when (val result = repository.demoGet("commerce/catalog/")) {
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    products = result.value.demoRows()
                    // Reuse this ONE cart while browsing; repeated Add increments
                    // the exact SKU instead of silently resetting the quantity to 1.
                    when (val current = repository.demoGet("commerce/cart/")) {
                        is ApiResult.Success -> if (repository.isCurrentSession()) cart = current.value
                        is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = current.message
                    }
                    loaded = true
                }
                is ApiResult.Failure -> if (repository.isCurrentSession()) {
                    products = emptyList()
                    loaded = true
                    feedback = result.message
                }
            }
            "cart", "checkout" -> when (val result = repository.demoGet("commerce/cart/")) {
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    cart = result.value
                    loaded = true
                }
                is ApiResult.Failure -> if (repository.isCurrentSession()) {
                    cart = null
                    loaded = true
                    feedback = result.message
                }
            }
            "business" -> { loaded = true }
            "orders" -> when (val result = repository.demoGet("commerce/orders/")) {
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    orders = result.value.demoRows()
                    loaded = true
                }
                is ApiResult.Failure -> if (repository.isCurrentSession()) {
                    orders = emptyList()
                    loaded = true
                    feedback = result.message
                }
            }
        }
    }

    fun updateQuantity(skuId: String, next: Int) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                when (val result = repository.demoPost("commerce/cart/",
                    JSONObject().put("sku_id", skuId).put("quantity", next))) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        cart = result.value
                        feedback = "Cart updated."
                        if (screen == "catalog") refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                }
            } finally { busy = false }
        }
    }

    fun updateOrder(orderId: String, action: String, version: Int = 0) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                val path = "commerce/orders/" + orderId + "/" +
                    if (action == "accept") "accept-quote/" else "cancel/"
                val body = if (action == "accept")
                    JSONObject().put("quote_version", version) else JSONObject()
                when (val result = repository.demoPost(path, body)) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        feedback = if (action == "accept") {
                            "Exact shipping quote accepted. Stock reserved; payment not yet confirmed."
                        } else "Order cancelled. Active stock holds released."
                        refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                }
            } finally { busy = false }
        }
    }

    fun sendTransferReference(orderId: String, reference: String) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                when (val result = repository.demoPost(
                    "commerce/orders/" + orderId + "/transfer-reference/",
                    JSONObject().put("reference", reference.trim()),
                )) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        feedback = "Reference received. Company finance must still verify the actual transfer."
                        refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                        feedback = result.message
                    }
                }
            } finally { busy = false }
        }
    }

    fun submitReturnRequest(orderId: String, reason: String, note: String) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                when (val result = repository.demoPost(
                    "commerce/orders/" + orderId + "/return/",
                    JSONObject().put("reason", reason).put("note", note.trim()),
                )) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        feedback = "Return request sent for company review. No refund or restock has occurred."
                        refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                        feedback = result.message
                    }
                }
            } finally { busy = false }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .imePadding().padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("PLANTUPIA · SHOP", style = PlantupiaText.sectionLabel)
                Text(
                    when (screen) {
                        "cart" -> "Your cart"
                        "checkout" -> "Delivery & checkout"
                        "orders" -> "My orders"
                        "business" -> "For businesses"
                        else -> "Plants & essentials"
                    },
                    style = PlantupiaText.editorialHeading,
                )
            }
            TextButton(onClick = onDismiss) { Text("Home", color = storeInk) }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (destination in listOf("catalog", "cart", "orders")) {
                val label = when (destination) {
                    "catalog" -> "Products"
                    "cart" -> "Cart"
                    else -> "My orders"
                }
                val selected = screen == destination ||
                    (destination == "cart" && screen == "checkout")
                OutlinedButton(
                    onClick = { selectedProductId = null; screen = destination; feedback = null },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = storeInk,
                        containerColor = if (selected) Color(0xFFE7EEE2) else storePaper,
                    ),
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 9.dp),
                ) {
                    Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth(), color = storeInk)
        feedback?.let { Text(it, style = PlantupiaText.supporting, color = storeInk) }

        when (screen) {
            "catalog" -> {
                OutlinedButton(
                    onClick = { screen = "business"; feedback = null },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("For businesses · wholesale & installation projects") }
                OutlinedTextField(
                    search, { search = it.take(100) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search products") },
                )
                val visible = products.filter { product ->
                    listOf(product.optString("name"), product.optString("sku"),
                        product.optString("variant_label")).any {
                            it.contains(search.trim(), ignoreCase = true)
                        }
                }
                if (loaded && products.isEmpty()) {
                    Text("No published Plantupia products are available yet. Company staff can import and approve actual products before they go on sale.",
                        style = PlantupiaText.supporting)
                } else if (loaded && visible.isEmpty()) {
                    Text("No products match this search.", style = PlantupiaText.supporting)
                }
                visible.groupBy { it.optString("product_id") }.forEach { (productId, variants) ->
                    val product = variants.first()
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (product.optBoolean("has_image")) {
                                PlantupiaCatalogPhoto(
                                    productId = productId, repository = repository,
                                    compact = true, photoAlt = product.optString("image_description"),
                                    catalogPath = "commerce/catalog",
                                )
                            }
                            Text(product.optString("name"), style = PlantupiaText.editorialHeading)
                            if (selectedProductId == productId) {
                                Text(product.optString("description"), style = PlantupiaText.supporting)
                            }
                            TextButton(onClick = {
                                selectedProductId = if (selectedProductId == productId) null else productId
                            }) {
                                Text(if (selectedProductId == productId) "Less about product" else "Product details",
                                    color = storeInk)
                            }
                            variants.forEach { variant ->
                                val selectedSku = variant.optString("id")
                                val inCart = cart?.demoRows()?.firstOrNull { line ->
                                    line.optJSONObject("product")?.optString("id") == selectedSku
                                }?.optInt("quantity") ?: 0
                                val available = variant.optInt("available_units")
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Column(Modifier.weight(1f)) {
                                        Text(variant.optString("variant_label").ifBlank {
                                            variant.optString("sku")
                                        }, fontWeight = FontWeight.SemiBold)
                                        Text(displayedEGP(variant.optString("price")) + " · " +
                                            variant.optString("available_units") + " available",
                                            style = PlantupiaText.supporting)
                                    }
                                    Button(
                                        onClick = { updateQuantity(selectedSku, inCart + 1) },
                                        enabled = !busy && variant.optBoolean("in_stock") &&
                                            inCart < 10 && inCart < available,
                                        colors = ButtonDefaults.buttonColors(containerColor = storeInk),
                                    ) { Text(if (inCart > 0) "+1 · " + inCart + " in cart" else "Add") }
                                }
                            }
                        }
                    }
                }
            }
            "cart" -> {
                val rows = cart?.demoRows().orEmpty()
                if (loaded && rows.isEmpty()) Text("Your cart is empty.", style = PlantupiaText.supporting)
                rows.forEach { line ->
                    val product = line.optJSONObject("product") ?: JSONObject()
                    val qty = line.optInt("quantity")
                    Surface(
                        color = Color.White,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                    ) {
                        Column(Modifier.padding(14.dp).fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(product.optString("name"), fontWeight = FontWeight.SemiBold)
                            Text(product.optString("sku") + " · " +
                                product.optString("variant_label"), style = PlantupiaText.supporting)
                            Text("Quantity " + qty + " · " + displayedEGP(line.optString("line_total")),
                                color = storeInk)
                            if (!line.optBoolean("available")) {
                                Text(line.optString("notice"), style = PlantupiaText.supporting,
                                    color = MaterialTheme.colorScheme.error)
                            }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedButton(onClick = {
                                    updateQuantity(product.optString("id"), qty - 1)
                                }, enabled = !busy) { Text("−") }
                                Spacer(Modifier.width(8.dp))
                                Text(qty.toString())
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = {
                                    updateQuantity(product.optString("id"), qty + 1)
                                }, enabled = !busy && qty < 10 && qty < product.optInt("available_units")) {
                                    Text("+")
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { updateQuantity(product.optString("id"), 0) },
                                    enabled = !busy) { Text("Remove") }
                            }
                        }
                    }
                }
                cart?.let {
                    Text("Products subtotal · " + displayedEGP(it.optString("subtotal")),
                        style = PlantupiaText.editorialHeading)
                    Text("Shipping is calculated and confirmed separately before you accept the final order total. Cart quantities do not reserve stock.",
                        style = PlantupiaText.supporting)
                    Button(
                        enabled = !busy && it.optBoolean("can_request_shipping_quote") && rows.isNotEmpty(),
                        onClick = { screen = "checkout"; feedback = null },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = storeInk),
                    ) { Text("Continue to delivery & checkout") }
                }
            }
            "checkout" -> {
                val hasCart = cart?.optBoolean("can_request_shipping_quote") == true
                Text("Confirm your address and entrance on the map. We will ask Plantupia for the actual shipping fee before you accept the final total.",
                    style = PlantupiaText.supporting)
                PlantupiaDeliveryAddressFields(
                    name = name, onNameChange = { name = it },
                    phone = phone, onPhoneChange = { phone = it },
                    city = city, onCityChange = { city = it },
                    street = address, onStreetChange = { address = it },
                    building = building, onBuildingChange = { building = it },
                    landmark = landmark, onLandmarkChange = { landmark = it },
                    latitude = latitude, longitude = longitude, pinConfirmed = pinConfirmed,
                    onPinConfirmed = { newLat, newLon ->
                        latitude = newLat; longitude = newLon; pinConfirmed = true
                    },
                )
                Text("Payment preference (no money collected yet)", style = PlantupiaText.sectionLabel)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { paymentMethod = "cod" },
                    ) { Text((if (paymentMethod == "cod") "✓ " else "") + "Cash") }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = { paymentMethod = "manual_transfer" },
                    ) { Text((if (paymentMethod == "manual_transfer") "✓ " else "") + "Transfer") }
                }
                Text("Subtotal · " + displayedEGP(cart?.optString("subtotal") ?: "0.00"),
                    style = PlantupiaText.editorialHeading)
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    enabled = !busy && hasCart && realCheckoutAddressValid(
                        name, phone, city, address, latitude, longitude, pinConfirmed,
                    ),
                    colors = ButtonDefaults.buttonColors(containerColor = storeInk),
                    onClick = {
                        val fingerprint = cart?.optString("fingerprint").orEmpty()
                        if (fingerprint.isBlank()) return@Button
                        busy = true
                        feedback = null
                        scope.launch {
                            try {
                                val payload = JSONObject()
                                    .put("cart_fingerprint", fingerprint)
                                    .put("contact_name", name.trim())
                                    .put("contact_phone", phone.trim())
                                    .put("city", city.trim())
                                    .put("delivery_address", address.trim())
                                    .put("building_details", building.trim())
                                    .put("landmark", landmark.trim())
                                    .put("delivery_latitude", latitude)
                                    .put("delivery_longitude", longitude)
                                    .put("delivery_pin_confirmed", true)
                                    .put("payment_preference", paymentMethod)
                                when (val result = repository.demoMutation(
                                    "single-shop-shipping-quote", "commerce/orders/", payload,
                                )) {
                                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                                        feedback = "Your delivery quote was requested. Plantupia will confirm shipping; no payment or stock reservation yet."
                                        screen = "orders"
                                        refresh++
                                    }
                                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                                        feedback = result.message
                                        if (result.statusCode == 409) refresh++
                                    }
                                }
                            } finally { busy = false }
                        }
                    },
                ) { Text(if (busy) "Submitting…" else "Request exact shipping quote") }
                TextButton(onClick = { screen = "cart" }) { Text("Back to cart") }
            }
            "business" -> {
                PlantupiaBusinessInquiryScreen(
                    repository = repository,
                    onBack = { screen = "catalog"; refresh++ },
                )
            }
            "orders" -> {
                OutlinedButton(onClick = { refresh++ }, modifier = Modifier.fillMaxWidth()) {
                    Text("Refresh orders")
                }
                if (loaded && orders.isEmpty()) Text("You have no commercial orders yet.",
                    style = PlantupiaText.supporting)
                orders.forEach { order ->
                    val id = order.optString("id")
                    val status = order.optString("status")
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp),
                        color = Color.White,
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Order " + id.take(8), fontWeight = FontWeight.SemiBold, color = storeInk)
                            Text(status.replace('_', ' '), style = PlantupiaText.body)
                            Text("Products · " + displayedEGP(order.optString("subtotal")))
                            val shippingFee = order.optString("shipping_fee")
                            if (order.isNull("shipping_fee")) {
                                Text("Plantupia is confirming your shipping cost.",
                                    style = PlantupiaText.supporting)
                            } else {
                                Text("Shipping · " + displayedEGP(shippingFee))
                                Text("Exact total · " + displayedEGP(order.optString("final_total")),
                                    fontWeight = FontWeight.SemiBold)
                            }
                            val lines = order.optJSONArray("lines")
                            if (lines != null) for (i in 0 until lines.length()) {
                                val line = lines.optJSONObject(i) ?: continue
                                Text(line.optString("name") + " · " + line.optString("sku") +
                                    " × " + line.optString("quantity"),
                                    style = PlantupiaText.supporting)
                            }
                            // Only plants physically delivered with staff-retained proof
                            // appear here; a quote, reservation or dispatch does not.
                            val deliveredPlants = order.optJSONArray("my_plant_ids")
                            if (deliveredPlants != null && deliveredPlants.length() > 0) {
                                Text(
                                    "${deliveredPlants.length()} delivered plant(s) added to My Plants.",
                                    fontWeight = FontWeight.SemiBold, color = storeInk,
                                )
                                Text(
                                    "Add your own photo and placement. A shop species label is not a verified identity.",
                                    style = PlantupiaText.supporting,
                                )
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        PlantupiaPlantOpenSignal.open(
                                            accountId, deliveredPlants.optString(0),
                                        )
                                    },
                                ) { Text("Open delivered plants") }
                            }
                            val timeline = order.optJSONArray("timeline")
                            if (timeline != null && timeline.length() > 0) {
                                Text("Order updates", fontWeight = FontWeight.SemiBold, color = storeInk)
                                for (i in 0 until timeline.length()) {
                                    val event = timeline.optJSONObject(i) ?: continue
                                    Text(event.optString("label"), style = PlantupiaText.supporting)
                                }
                            }
                            Text("Payment · " + order.optString("payment_status"),
                                style = PlantupiaText.supporting)
                            val fulfillment = order.optJSONObject("fulfillment")
                            if (fulfillment != null) {
                                Text("Delivery team · " + fulfillment.optString("carrier_name"),
                                    style = PlantupiaText.supporting)
                                Text("Tracking / handover · " + fulfillment.optString("tracking_reference"),
                                    style = PlantupiaText.supporting)
                                if (fulfillment.optBoolean("delivery_proof_recorded")) {
                                    Text("Plantupia recorded delivery evidence.",
                                        style = PlantupiaText.supporting)
                                } else {
                                    Text("Handover recorded; delivery is not yet confirmed.",
                                        style = PlantupiaText.supporting)
                                }
                            } else {
                                Text("No carrier handover has been recorded.",
                                    style = PlantupiaText.supporting)
                            }
                            if (order.optString("payment_preference") == "manual_transfer" &&
                                order.optString("payment_status") == "unpaid" &&
                                status in listOf("confirmed", "preparing", "dispatched", "delivered")) {
                                if (order.optBoolean("transfer_reference_submitted")) {
                                    Text(
                                        "Transfer reference submitted: " + order.optString("transfer_reference") +
                                            ". Finance has not verified payment.",
                                        style = PlantupiaText.supporting,
                                    )
                                } else {
                                    PlantupiaTransferReferenceForm(
                                        orderId = id,
                                        busy = busy,
                                        onSubmit = { reference -> sendTransferReference(id, reference) },
                                    )
                                }
                            }
                            val returnCase = order.optJSONObject("return_case")
                            if (returnCase != null) {
                                Text("Return case · " + returnCase.optString("decision").replace('_', ' '),
                                    style = PlantupiaText.supporting)
                                Text("Physical return and refund are reviewed separately.",
                                    style = PlantupiaText.supporting)
                            } else if (status == "delivered") {
                                PlantupiaReturnRequestForm(
                                    orderId = id,
                                    busy = busy,
                                    onSubmit = { reason, note -> submitReturnRequest(id, reason, note) },
                                )
                            }
                            if (order.optBoolean("refunded")) {
                                Text("Plantupia recorded a verified external refund.",
                                    style = PlantupiaText.supporting)
                            }
                            if (status == "quoted" && !busy && !order.isNull("final_total")) {
                                Text("Accept only if you agree with this exact products + shipping total.",
                                    style = PlantupiaText.supporting)
                                Button(
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                                    onClick = { updateOrder(id, "accept", order.optInt("shipping_quote_version")) },
                                    enabled = !busy,
                                    colors = ButtonDefaults.buttonColors(containerColor = storeInk),
                                ) { Text("Accept exact total & reserve stock") }
                            }
                            if (status in listOf("quote_pending", "quoted", "confirmed")) {
                                OutlinedButton(
                                    enabled = !busy,
                                    onClick = { updateOrder(id, "cancel") },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("Cancel order") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantupiaTransferReferenceForm(
    orderId: String,
    busy: Boolean,
    onSubmit: (String) -> Unit,
) {
    var reference by rememberSaveable(orderId) { mutableStateOf("") }
    Text(
        "Already transferred using Plantupia's official payment instructions? " +
            "Enter the exact reference. This does not confirm payment.",
        style = PlantupiaText.supporting,
    )
    OutlinedTextField(
        value = reference,
        onValueChange = { reference = it.take(120) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("Bank / InstaPay transfer reference") },
        singleLine = true,
    )
    OutlinedButton(
        modifier = Modifier.fillMaxWidth(),
        enabled = !busy && reference.trim().length in 4..120,
        onClick = { onSubmit(reference) },
    ) { Text("Submit reference for company verification") }
}

@Composable
private fun PlantupiaReturnRequestForm(
    orderId: String,
    busy: Boolean,
    onSubmit: (String, String) -> Unit,
) {
    var expanded by rememberSaveable(orderId) { mutableStateOf(false) }
    var reason by rememberSaveable(orderId) { mutableStateOf("damaged") }
    var note by rememberSaveable(orderId) { mutableStateOf("") }
    OutlinedButton(onClick = { expanded = !expanded }, enabled = !busy) {
        Text(if (expanded) "Close return request" else "Request a return")
    }
    if (expanded) {
        Text("The company will review this request before accepting returned items.",
            style = PlantupiaText.supporting)
        for ((value, label) in listOf(
            "damaged" to "Damaged",
            "incorrect_item" to "Incorrect item",
            "quality_issue" to "Quality issue",
            "other" to "Other reason",
        )) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                onClick = { reason = value },
            ) { Text((if (reason == value) "✓ " else "") + label) }
        }
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(500) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Explain the return request") },
            minLines = 2,
        )
        Button(
            enabled = !busy && note.trim().length in 10..500,
            modifier = Modifier.fillMaxWidth(),
            onClick = { onSubmit(reason, note) },
            colors = ButtonDefaults.buttonColors(containerColor = storeInk),
        ) { Text("Submit request for company review") }
    }
}
