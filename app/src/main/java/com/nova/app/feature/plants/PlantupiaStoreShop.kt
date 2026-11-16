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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val storeInk = PlantupiaColors.forest

internal fun realCheckoutAddressValid(
    name: String, phone: String, city: String, address: String,
    latitude: Double?, longitude: Double?, confirmed: Boolean,
): Boolean =
    plantupiaDeliveryAddressReady(name, phone, city, address, latitude, longitude, confirmed)

internal fun displayedEGP(value: String): String = "EGP " + value

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
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var screen by rememberSaveable { mutableStateOf("catalog") }
    var products by remember { mutableStateOf(emptyList<JSONObject>()) }
    var cart by remember { mutableStateOf<JSONObject?>(null) }
    var orders by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selectedProductId by rememberSaveable { mutableStateOf<String?>(null) }
    var category by rememberSaveable { mutableStateOf("all") }
    var visibleCount by rememberSaveable { mutableIntStateOf(8) }
    var focusedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var quoteToAccept by remember { mutableStateOf<JSONObject?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val catalogScroll = rememberScrollState()
    val screenScroll = key(screen, selectedProductId) { rememberScrollState() }
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

    BackHandler(enabled = screen in setOf("checkout", "product", "business")) {
        screen = if (screen == "checkout") "cart" else "catalog"
        selectedProductId = null
        feedback = null
    }
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
    val dataPage = if (screen == "product") "catalog" else screen
    LaunchedEffect(dataPage, refresh) {
        loaded = false
        loadError = null
        when (dataPage) {
            "catalog" -> {
                when (val result = repository.demoGet("commerce/catalog/")) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        products = result.value.demoRows()
                        // Reuse this ONE cart while browsing; repeated Add increments
                        // the exact SKU instead of silently resetting the quantity to 1.
                        when (val current = repository.demoGet("commerce/cart/")) {
                            is ApiResult.Success -> if (repository.isCurrentSession()) cart = current.value
                            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                                cart = null
                                loadError = current.message
                            }
                        }
                        loaded = true
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                        products = emptyList()
                        loaded = true
                        loadError = result.message
                    }
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
                    loadError = result.message
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
                    loadError = result.message
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
                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                        feedback = result.message
                        refresh++
                    }
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

    quoteToAccept?.let { quote ->
        AlertDialog(
            onDismissRequest = { quoteToAccept = null },
            title = { Text("Confirm your order") },
            text = {
                Text(
                    "Total " + displayedEGP(quote.optString("final_total")) +
                        " including delivery. Confirming reserves the items. Payment is recorded separately.",
                )
            },
            dismissButton = { TextButton(onClick = { quoteToAccept = null }) { Text("Back") } },
            confirmButton = {
                TextButton(onClick = {
                    quoteToAccept = null
                    updateOrder(quote.optString("id"), "accept", quote.optInt("shipping_quote_version"))
                }) { Text("Confirm order") }
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().imePadding()
            .verticalScroll(if (screen == "catalog") catalogScroll else screenScroll)
            .padding(horizontal = 18.dp, vertical = 14.dp),
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
                        "product" -> "Product details"
                        else -> "Plants & essentials"
                    },
                    style = PlantupiaText.editorialHeading,
                )
            }
            if (screen == "product") {
                TextButton(onClick = { screen = "catalog"; selectedProductId = null; feedback = null }) {
                    Text("Back to products", color = storeInk)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (destination in listOf("catalog", "cart", "orders")) {
                val label = when (destination) {
                    "catalog" -> "Products"
                    "cart" -> "Cart"
                    else -> "My orders"
                }
                val isSelected = screen == destination ||
                    (destination == "cart" && screen == "checkout") ||
                    (destination == "catalog" && screen == "product")
                OutlinedButton(
                    onClick = { selectedProductId = null; screen = destination; feedback = null },
                    enabled = !busy,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { selected = isSelected },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = storeInk,
                        containerColor = if (isSelected) PlantupiaColors.softSage else PlantupiaColors.paper,
                    ),
                    contentPadding = PaddingValues(horizontal = 3.dp, vertical = 9.dp),
                ) {
                    Text(label, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth(), color = storeInk)
        loadError?.let { message ->
            StorePanel {
                Text(message, style = PlantupiaText.supporting, color = PlantupiaColors.errorText)
                TextButton(onClick = { refresh++ }) { Text("Try again") }
            }
        }
        if (screen != "checkout") feedback?.let {
            Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }, style = PlantupiaText.supporting)
        }

        when (screen) {
            "catalog" -> {
                OutlinedTextField(
                    search, { search = it.take(100); visibleCount = 8 },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search products") },
                )
                PlantupiaShopCategoryShelf(category) { category = it; visibleCount = 8 }
                val kind = if (category == "accessory") "care" else category
                val families = remember(products, kind, search) {
                    products.groupBy { it.optString("product_id") }.filterValues { variants ->
                        val product = variants.first()
                        (kind == "all" || kind == product.optString("kind")) &&
                            variants.any { variant ->
                                listOf(variant.optString("name"), variant.optString("sku"), variant.optString("variant_label"))
                                    .any { it.contains(search.trim(), ignoreCase = true) }
                            }
                    }
                }
                if (loaded && loadError == null && families.isEmpty()) {
                    StorePanel(accent = true) {
                        Text(
                            if (products.isEmpty()) "The collection is growing" else "No matching products",
                            style = PlantupiaText.editorialHeading,
                        )
                        if (products.isEmpty()) {
                            Text("There are no published products at the moment.", style = PlantupiaText.body)
                        } else {
                            TextButton(onClick = { search = ""; category = "all"; visibleCount = 8 }) {
                                Text("Clear filters")
                            }
                        }
                    }
                }
                StoreCatalog(families, plantupiaVisibleFamilies(families.size, visibleCount), repository) {
                    selectedProductId = it
                    screen = "product"
                    feedback = null
                }
                if (families.size > visibleCount) {
                    OutlinedButton(onClick = { visibleCount += 8 }, modifier = Modifier.fillMaxWidth()) {
                        Text("Show more products")
                    }
                }
                TextButton(onClick = { screen = "business"; feedback = null }) {
                    Text("For businesses · wholesale & installation")
                }
            }
            "product" -> {
                val id = selectedProductId.orEmpty()
                val variants = products.filter { it.optString("product_id") == id }
                if (variants.isNotEmpty()) {
                    StoreProductDetails(id, variants, cart, repository, busy || !loaded, ::updateQuantity)
                } else if (loaded && loadError == null) {
                    Text("This product is no longer available.", style = PlantupiaText.body)
                }
            }
            "cart" -> {
                val rows = cart?.demoRows().orEmpty()
                if (loaded && loadError == null && rows.isEmpty()) StorePanel(accent = true) {
                    Text("Your cart is empty.", style = PlantupiaText.editorialHeading)
                    TextButton(onClick = { screen = "catalog"; feedback = null }) { Text("Browse products") }
                }
                rows.forEach { line ->
                    val product = line.optJSONObject("product") ?: JSONObject()
                    val qty = line.optInt("quantity")
                    StorePanel {
                        Text(product.optString("name"), fontWeight = FontWeight.SemiBold)
                        Text(storeVariantName(product) + " · " + product.optString("sku"), style = PlantupiaText.supporting)
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
                            }, enabled = loaded && !busy && qty < 10 && qty < product.optInt("available_units")) {
                                Text("+")
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = { updateQuantity(product.optString("id"), 0) },
                                enabled = !busy) { Text("Remove") }
                        }
                    }
                }
                cart?.takeIf { rows.isNotEmpty() }?.let {
                    StorePanel(accent = true) {
                        Text("Items subtotal", style = PlantupiaText.sectionLabel)
                        Text(displayedEGP(it.optString("subtotal")), style = PlantupiaText.editorialHeading)
                        Text("The delivery fee is quoted before you confirm the order.", style = PlantupiaText.body)
                        Button(
                            enabled = loaded && !busy && it.optBoolean("can_request_shipping_quote"),
                            onClick = { screen = "checkout"; feedback = null },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = storeInk),
                        ) { Text("Continue to delivery & checkout") }
                    }
                }
            }
            "checkout" -> {
                val hasCart = cart?.optBoolean("can_request_shipping_quote") == true
                StorePanel(accent = true) {
                    Text("Request a delivery quote", style = PlantupiaText.editorialHeading)
                    Text("Add your delivery details. You can review the full price after Plantupia quotes shipping.", style = PlantupiaText.body)
                    cart?.let { Text("Items · " + displayedEGP(it.optString("subtotal")), style = PlantupiaText.sectionLabel) }
                }
                StorePanel {
                    Text("Delivery address", style = PlantupiaText.editorialHeading)
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
                }
                StorePanel {
                    Text("Payment preference", style = PlantupiaText.editorialHeading)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { paymentMethod = "cod" },
                        ) { Text((if (paymentMethod == "cod") "✓ " else "") + "Cash on delivery") }
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = { paymentMethod = "manual_transfer" },
                        ) { Text((if (paymentMethod == "manual_transfer") "✓ " else "") + "Bank transfer") }
                    }
                    Text("No payment is taken when requesting a quote.", style = PlantupiaText.supporting)
                }
                val addressReady = realCheckoutAddressValid(name, phone, city, address, latitude, longitude, pinConfirmed)
                feedback?.let {
                    Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = PlantupiaText.body, color = PlantupiaColors.errorText)
                }
                if (!addressReady) {
                    Text("Add the recipient, phone, city, street and confirmed map pin to continue.", style = PlantupiaText.supporting)
                }
                if (loaded && loadError == null && !hasCart) {
                    Text("Review the current items in your cart before requesting delivery.", style = PlantupiaText.supporting)
                }
                Button(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    enabled = loaded && !busy && hasCart && addressReady,
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
                                    .put("contact_phone", normalizedDeliveryPhone(phone))
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
                                        focusedOrderId = result.value.optString("id")
                                        feedback = null
                                        name = ""; phone = ""; city = ""; address = ""; building = ""; landmark = ""
                                        latitude = null; longitude = null; pinConfirmed = false
                                        paymentMethod = "cod"
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
                if (loaded && loadError == null && orders.isEmpty()) StorePanel(accent = true) {
                    Text("No orders yet", style = PlantupiaText.editorialHeading)
                    TextButton(onClick = { screen = "catalog" }) { Text("Browse products") }
                }
                val ordered = orders.sortedBy { order ->
                    when {
                        order.optString("id") == focusedOrderId -> 0
                        order.optString("status") == "quoted" -> 1
                        else -> 2
                    }
                }
                ordered.forEach { order ->
                    val id = order.optString("id")
                    key(id) {
                        StoreOrderCard(
                            order = order,
                            focused = id == focusedOrderId,
                            busy = busy || !loaded,
                            onReviewQuote = { quoteToAccept = order },
                            onCancel = { updateOrder(id, "cancel") },
                            onOpenPlant = { plantId -> PlantupiaPlantOpenSignal.open(accountId, plantId) },
                            onTransfer = { reference -> sendTransferReference(id, reference) },
                            onReturn = { reason, note -> submitReturnRequest(id, reason, note) },
                        )
                    }
                }
            }
        }
    }
}
