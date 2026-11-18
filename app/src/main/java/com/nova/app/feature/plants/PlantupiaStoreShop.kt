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
 * Commercial SKU -> saved cart -> delivery price for the chosen city ->
 * single direct order when price is known; explicit review only for an exception.
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
    var cartReceipt by remember { mutableStateOf<ShopCartReceipt?>(null) }
    var productAddFailure by remember { mutableStateOf<Pair<String, String>?>(null) }
    var addingSku by remember { mutableStateOf<String?>(null) }
    var cartPendingSku by remember { mutableStateOf<String?>(null) }
    var cartActionError by remember { mutableStateOf<String?>(null) }
    var cartActionNotice by remember { mutableStateOf<String?>(null) }
    var lastRemoved by remember { mutableStateOf<ShopRemovedCartObject?>(null) }
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
    var deliveryNote by rememberSaveable { mutableStateOf("") }
    var deliveryStep by remember { mutableStateOf("details") }
    var deliveryReceipt by remember { mutableStateOf<ShopDeliveryQuoteReceipt?>(null) }
    var deliveryError by remember { mutableStateOf<String?>(null) }
    var showDeliveryIssues by remember { mutableStateOf(false) }
    var deliveryPrice by remember { mutableStateOf<ShopDeliveryExactPrice?>(null) }
    var deliveryPriceBusy by remember { mutableStateOf(false) }
    var deliveryPriceChecked by remember { mutableStateOf(false) }
    var deliveryPriceCity by remember { mutableStateOf("") }
    var deliveryPriceFingerprint by remember { mutableStateOf("") }
    var deliveryPriceError by remember { mutableStateOf<String?>(null) }
    var deliveryPriceRefresh by remember { mutableIntStateOf(0) }

    BackHandler(enabled = screen in setOf("checkout", "product", "business")) {
        when {
            screen == "checkout" && deliveryStep == "review" -> deliveryStep = "details"
            screen == "checkout" && deliveryStep == "received" -> {
                focusedOrderId = deliveryReceipt?.reference
                deliveryReceipt = null
                deliveryStep = "details"
                screen = "orders"
                refresh++
            }
            screen == "checkout" -> screen = "cart"
            else -> screen = "catalog"
        }
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
    // Switching accounts must not show a previous customer's cart or collection
    // while the new account's authenticated data is still loading.
    LaunchedEffect(accountId) {
        products = emptyList()
        cart = null
        orders = emptyList()
        selectedProductId = null
        if (screen == "product" || screen == "checkout" || screen == "business") {
            screen = "catalog"
        }
        category = "all"
        visibleCount = 8
        search = ""
        feedback = null
        quoteToAccept = null
        cartReceipt = null
        productAddFailure = null
        addingSku = null
        cartPendingSku = null
        cartActionError = null
        cartActionNotice = null
        lastRemoved = null
        // Delivery information is private to the signed-in customer. Saved
        // Compose state from another account must never cross this boundary.
        name = ""; phone = ""; city = ""; address = ""
        building = ""; landmark = ""; latitude = null; longitude = null
        pinConfirmed = false; paymentMethod = "cod"; deliveryNote = ""
        deliveryStep = "details"; deliveryReceipt = null
        deliveryError = null; showDeliveryIssues = false
        deliveryPrice = null; deliveryPriceBusy = false
        deliveryPriceChecked = false; deliveryPriceError = null
        deliveryPriceCity = ""; deliveryPriceFingerprint = ""
        focusedOrderId = null
        busy = false
    }
    val dataPage = if (screen == "product") "catalog" else screen
    LaunchedEffect(accountId, dataPage, refresh) {
        loaded = false
        loadError = null
        when (dataPage) {
            "catalog" -> {
                when (val result = repository.demoGet("commerce/catalog/")) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        products = result.value.demoRows()
                        // Product discovery should not wait for a second cart request.
                        loaded = true
                        // Reuse this ONE cart while browsing; repeated Add increments
                        // the exact SKU instead of silently resetting the quantity to 1.
                        when (val current = repository.demoGet("commerce/cart/")) {
                            is ApiResult.Success -> if (repository.isCurrentSession()) cart = current.value
                            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                                // Browsing still works when the user's cart cannot load.
                                // The cart opens its own retry/error state on demand.
                                cart = null
                            }
                        }
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

    // Merchant-authored city price is rechecked for each city and exact saved cart.
    // An unavailable response is NOT the same as a temporary network error.
    val deliveryCartFingerprint = cart?.optString("fingerprint").orEmpty()
    LaunchedEffect(
        accountId, screen, city.trim(), deliveryCartFingerprint,
        loaded, loadError, deliveryPriceRefresh,
    ) {
        deliveryPrice = null
        deliveryPriceChecked = false
        deliveryPriceError = null
        deliveryPriceBusy = false
        deliveryPriceCity = ""
        deliveryPriceFingerprint = ""
        if (screen != "checkout" || !loaded || loadError != null ||
            city.trim().length < 2 || deliveryCartFingerprint.length != 64) return@LaunchedEffect
        deliveryPriceBusy = true
        val requestedCity = city.trim()
        try {
            val path = "commerce/delivery-price/?city=" +
                java.net.URLEncoder.encode(requestedCity, "UTF-8") +
                "&cart_fingerprint=" + deliveryCartFingerprint
            when (val result = repository.demoGet(path)) {
                is ApiResult.Success -> if (repository.isCurrentSession()) {
                    val currentDraft = ShopDeliveryDraft(city = requestedCity)
                    val exact = shopDeliveryExactPrice(result.value, currentDraft, cart)
                    if (result.value.optBoolean("available") && exact == null) {
                        deliveryPriceError = "Could not verify the displayed delivery total. Refresh to retry."
                    }
                    deliveryPrice = exact
                    deliveryPriceChecked = true
                    deliveryPriceCity = requestedCity
                    deliveryPriceFingerprint = deliveryCartFingerprint
                }
                is ApiResult.Failure -> if (repository.isCurrentSession()) {
                    deliveryPriceError = "Couldn't check this city's delivery price. " + result.message
                    deliveryPriceChecked = true
                    deliveryPriceCity = requestedCity
                    deliveryPriceFingerprint = deliveryCartFingerprint
                }
            }
        } finally {
            if (repository.isCurrentSession()) deliveryPriceBusy = false
        }
    }

    fun updateQuantity(
        skuId: String, next: Int, fromProduct: Boolean = false, restoreRemoval: Boolean = false,
    ) {
        // The backend mutation sets an absolute desired quantity, not an increment.
        // Repeating the same request after an ambiguous timeout cannot double-add.
        if (busy) return
        val removedCandidate = if (!fromProduct && next == 0)
            shopCartRemovalCandidate(cart, skuId) else null
        busy = true
        feedback = null
        if (!fromProduct) {
            cartPendingSku = skuId
            cartActionError = null
            cartActionNotice = null
        }
        if (fromProduct) {
            addingSku = skuId
            productAddFailure = null
            cartReceipt = null
        }
        scope.launch {
            try {
                when (val result = repository.demoPost("commerce/cart/",
                    JSONObject().put("sku_id", skuId).put("quantity", next))) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        cart = result.value
                        if (fromProduct) {
                            val saved = shopCartReceiptFromResponse(result.value, skuId, next)
                            if (saved != null && screen == "product" && selectedProductId == saved.productId) {
                                cartReceipt = saved
                            } else if (saved == null) {
                                productAddFailure = skuId to
                                    "We couldn't confirm the exact item and quantity. Open your cart to check before trying again."
                            } else {
                                feedback = "Your saved cart has been updated."
                            }
                        } else {
                            if (shopCartMutationConfirmed(result.value, skuId, next)) {
                                if (next == 0 && removedCandidate != null) {
                                    lastRemoved = removedCandidate
                                } else {
                                    if (restoreRemoval) lastRemoved = null
                                    cartActionNotice = if (restoreRemoval)
                                        "Your selection was restored to the saved cart."
                                    else "Quantity saved to your cart."
                                }
                            } else {
                                cartActionError =
                                    "The returned cart did not confirm that exact change. Refresh it before trying again."
                            }
                        }
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) {
                        if (fromProduct) {
                            productAddFailure = skuId to
                                ("We couldn't confirm the update. Check your cart before retrying. " + result.message)
                        } else {
                            cartActionError =
                                "We couldn't confirm the cart change. Refresh your saved cart before retrying. " +
                                    result.message
                        }
                    }
                }
            } catch (error: Exception) {
                if (error is java.util.concurrent.CancellationException) throw error
                if (repository.isCurrentSession()) {
                    if (fromProduct) {
                        productAddFailure = skuId to
                            "We couldn't verify the cart change. Check your saved cart before trying again."
                    } else {
                        cartActionError =
                            "We couldn't confirm the cart change. Refresh the saved cart before retrying."
                    }
                }
            } finally {
                // An obsolete account's request must not unlock a new user's
                // still-pending operation or expose its original selection.
                if (repository.isCurrentSession()) {
                    busy = false
                    if (fromProduct) addingSku = null
                    else cartPendingSku = null
                }
            }
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

    cartReceipt?.let { saved ->
        PlantupiaCartReceiptSheet(
            receipt = saved,
            repository = repository,
            onContinue = { cartReceipt = null },
            onViewCart = {
                cartReceipt = null
                productAddFailure = null
                screen = "cart"
                selectedProductId = null
                feedback = null
            },
        )
    }

    Column(
        modifier = modifier.fillMaxWidth().imePadding()
            .verticalScroll(if (screen == "catalog") catalogScroll else screenScroll)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val cartUnits = cart?.optJSONArray("results")?.let { lines ->
            (0 until lines.length()).sumOf { index ->
                lines.optJSONObject(index)?.optInt("quantity") ?: 0
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PLANTUPIA  /  THE SHOP", style = PlantupiaText.sectionLabel)
                    Text(
                        when (screen) {
                            "cart" -> "Your selection"
                            "checkout" -> "Delivery details"
                            "orders" -> "Your orders"
                            "business" -> "For the places we share"
                            "product" -> "Object study"
                            else -> "The living collection"
                        },
                        style = PlantupiaText.pageHeading,
                    )
                }
            }
            PlantupiaShopNavigation(
                screen = screen,
                cartQuantity = cartUnits,
                enabled = !busy,
                onCollection = {
                    screen = "catalog"
                    selectedProductId = null
                    feedback = null
                },
                onOrders = {
                    screen = "orders"
                    selectedProductId = null
                    feedback = null
                },
                onCart = {
                    screen = "cart"
                    selectedProductId = null
                    feedback = null
                },
            )
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
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "OBJECTS / PLACES / CONTINUITY",
                        style = PlantupiaText.sectionLabel,
                    )
                    Text(
                        "Plants, vessels and the things that help us care for them.",
                        style = PlantupiaText.body,
                    )
                }
                OutlinedTextField(
                    search, { search = it.take(100); visibleCount = 8 },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Find something for your space") },
                    placeholder = { Text("Name, type or product code") },
                )
                if (loaded && loadError == null) {
                    val allFamilies = remember(products) {
                        products.groupBy { it.optString("product_id") }
                    }
                    val actualKinds = remember(allFamilies) {
                        allFamilies.values.map { variants -> variants.first().optString("kind") }
                    }
                    PlantupiaShopCategoryRail(
                        current = if (category == "accessory") "care" else category,
                        productKinds = actualKinds,
                        enabled = !busy,
                        onChoose = { category = it; visibleCount = 8 },
                    )
                    val selectedKind = if (category == "accessory") "care" else category
                    val needle = search.trim()
                    val families = remember(allFamilies, selectedKind, needle) {
                        allFamilies.filterValues { variants ->
                            val product = variants.first()
                            (selectedKind == "all" || selectedKind == product.optString("kind")) &&
                                variants.any { variant ->
                                    listOf(
                                        variant.optString("name"),
                                        variant.optString("description"),
                                        variant.optString("sku"),
                                        variant.optString("variant_label"),
                                    ).any { it.contains(needle, ignoreCase = true) }
                                }
                        }
                    }
                    val count = families.size
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "$count ${if (count == 1) "OBJECT" else "OBJECTS"} IN VIEW",
                            style = PlantupiaText.sectionLabel,
                        )
                        if (needle.isNotEmpty() || selectedKind != "all") {
                            TextButton(onClick = {
                                search = ""; category = "all"; visibleCount = 8
                            }) { Text("Clear selection", color = storeInk) }
                        }
                    }
                    if (families.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text(
                                if (allFamilies.isEmpty()) "The collection is growing."
                                else "Nothing matching that selection.",
                                style = PlantupiaText.editorialHeading,
                            )
                            Text(
                                if (allFamilies.isEmpty())
                                    "No products are currently published for this account."
                                else "Try another category or a shorter product name.",
                                style = PlantupiaText.body,
                            )
                            if (allFamilies.isNotEmpty()) {
                                TextButton(onClick = {
                                    category = "all"; search = ""; visibleCount = 8
                                }) { Text("See the whole collection  ↗", color = storeInk) }
                            }
                        }
                    } else {
                        StoreCatalog(
                            families, plantupiaVisibleFamilies(count, visibleCount),
                            repository,
                        ) {
                            selectedProductId = it
                            screen = "product"
                            feedback = null
                            productAddFailure = null
                        }
                        if (count > visibleCount) {
                            OutlinedButton(
                                onClick = { visibleCount += 8 },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                            ) {
                                Text("Explore ${minOf(8, count - visibleCount)} more objects  ↓")
                            }
                        }
                    }
                } else if (loadError == null) {
                    Text("Opening the collection…", style = PlantupiaText.supporting)
                }
                TextButton(
                    onClick = { screen = "business"; feedback = null },
                    enabled = !busy,
                ) { Text("For spaces, projects and wholesale  ↗", color = storeInk) }
            }
            "product" -> {
                val id = selectedProductId.orEmpty()
                val variants = products.filter { it.optString("product_id") == id }
                if (variants.isNotEmpty()) {
                    StoreProductDetails(
                        productId = id,
                        variants = variants,
                        cart = cart,
                        repository = repository,
                        busy = busy || !loaded,
                        onAdd = { sku, quantity -> updateQuantity(sku, quantity, fromProduct = true) },
                        adding = addingSku != null,
                        addErrorForSku = productAddFailure,
                        onViewCart = {
                            screen = "cart"
                            selectedProductId = null
                            productAddFailure = null
                        },
                    )
                } else if (loaded && loadError == null) {
                    Text("This product is no longer available.", style = PlantupiaText.body)
                }
            }
            "cart" -> {
                if (loaded && loadError == null && cart != null) {
                    PlantupiaCartExperience(
                        cart = cart!!,
                        repository = repository,
                        busy = busy,
                        pendingSku = cartPendingSku,
                        removed = lastRemoved,
                        error = cartActionError,
                        notice = cartActionNotice,
                        onQuantityChange = { sku, quantity ->
                            updateQuantity(sku, quantity)
                        },
                        onRemove = { sku -> updateQuantity(sku, 0) },
                        onRestore = {
                            lastRemoved?.let { removed ->
                                updateQuantity(
                                    removed.skuId, removed.previousQuantity, restoreRemoval = true,
                                )
                            }
                        },
                        onRefresh = {
                            cartActionError = null
                            cartActionNotice = null
                            refresh++
                        },
                        onOpenProduct = { productId ->
                            selectedProductId = productId
                            screen = "product"
                            feedback = null
                            productAddFailure = null
                        },
                        onContinueShopping = {
                            screen = "catalog"
                            selectedProductId = null
                            feedback = null
                        },
                        onDeliveryDetails = {
                            if (!busy && cart?.optBoolean("can_request_shipping_quote") == true) {
                                screen = "checkout"
                                deliveryStep = "details"
                                deliveryReceipt = null
                                deliveryError = null
                                showDeliveryIssues = false
                                feedback = null
                            }
                        },
                    )
                } else if (loaded && loadError == null) {
                    Text(
                        "Your saved cart is unavailable. Refresh it before continuing.",
                        style = PlantupiaText.supporting,
                    )
                    TextButton(onClick = { refresh++ }) {
                        Text("Refresh saved cart  ↗", color = storeInk)
                    }
                }
            }
            "checkout" -> {
                val draft = ShopDeliveryDraft(
                    recipient = name, phone = phone, city = city, street = address,
                    building = building, landmark = landmark, note = deliveryNote,
                    latitude = latitude, longitude = longitude,
                    pinConfirmed = pinConfirmed, paymentPreference = paymentMethod,
                )
                val checkedPrice = deliveryPriceChecked &&
                    deliveryPriceCity == draft.city.trim() &&
                    deliveryPriceFingerprint == cart?.optString("fingerprint")
                val exactPrice = deliveryPrice?.takeIf {
                    checkedPrice && it.city == draft.city.trim() &&
                        it.cartFingerprint == cart?.optString("fingerprint")
                }
                PlantupiaDeliveryQuoteJourney(
                    draft = draft, cart = if (loaded && loadError == null) cart else null,
                    step = deliveryStep, receipt = deliveryReceipt, busy = busy,
                    error = deliveryError, showErrors = showDeliveryIssues,
                    exactPrice = exactPrice,
                    priceChecked = checkedPrice, priceLoading = deliveryPriceBusy,
                    priceError = deliveryPriceError,
                    onReloadPrice = { deliveryPriceRefresh++ },
                    onRecipient = { name = it; deliveryError = null },
                    onPhone = { phone = it; deliveryError = null },
                    onCity = {
                        if (city != it) pinConfirmed = false
                        city = it; deliveryError = null
                    },
                    onStreet = {
                        if (address != it) pinConfirmed = false
                        address = it; deliveryError = null
                    },
                    onBuilding = { building = it; deliveryError = null },
                    onLandmark = { landmark = it; deliveryError = null },
                    onNote = { deliveryNote = it; deliveryError = null },
                    onPin = { lat, lon ->
                        latitude = lat; longitude = lon
                        pinConfirmed = true; deliveryError = null
                    },
                    onPayment = { paymentMethod = it; deliveryError = null },
                    onReview = {
                        showDeliveryIssues = true
                        if (loaded && loadError == null && shopDeliveryReady(draft, cart)) {
                            deliveryStep = "review"
                            deliveryError = null
                            showDeliveryIssues = false
                        }
                    },
                    onEdit = { deliveryStep = "details"; deliveryError = null },
                    onSubmit = {
                        val exactCart = cart
                        if (!loaded || loadError != null ||
                            !shopDeliveryReady(draft, exactCart) || exactCart == null || busy ||
                            !checkedPrice || deliveryPriceBusy || deliveryPriceError != null
                        ) {
                            deliveryError = "Your cart, delivery price or address needs checking before you proceed."
                        } else {
                            val finalPrice = exactPrice
                            val payload = shopDeliveryQuotePayload(draft, exactCart)
                            val direct = finalPrice != null
                            if (direct) {
                                payload.put("rate_revision", finalPrice!!.rateRevision)
                                payload.put("expected_total", finalPrice.total)
                            }
                            busy = true
                            deliveryError = null
                            scope.launch {
                                try {
                                    val result = repository.demoMutation(
                                        if (direct) "single-shop-direct-order" else "single-shop-shipping-quote",
                                        if (direct) "commerce/orders/direct/" else "commerce/orders/",
                                        payload,
                                    )
                                    when (result) {
                                        is ApiResult.Success -> if (repository.isCurrentSession()) {
                                            val acknowledged = shopDeliveryReceiptFromResponse(
                                                result.value, draft, exactCart, finalPrice)
                                            if (acknowledged != null) {
                                                focusedOrderId = acknowledged.reference
                                                deliveryReceipt = acknowledged
                                                deliveryStep = "received"
                                            } else {
                                                deliveryError =
                                                    "We couldn't verify the saved order. Check Your orders before retrying."
                                            }
                                        }
                                        is ApiResult.Failure -> if (repository.isCurrentSession()) {
                                            deliveryError =
                                                "Your order was not confirmed. Check Your orders before retrying. " +
                                                    result.message
                                            deliveryPriceRefresh++
                                            if (result.statusCode == 409) refresh++
                                        }
                                    }
                                } catch (failure: Exception) {
                                    if (failure is java.util.concurrent.CancellationException) throw failure
                                    if (repository.isCurrentSession()) {
                                        deliveryError =
                                            "The connection ended without a verified receipt. Check Your orders before retrying."
                                    }
                                } finally {
                                    if (repository.isCurrentSession()) busy = false
                                }
                            }
                        }
                    },
                    onCart = {
                        screen = "cart"; deliveryError = null; feedback = null
                    },
                    onOrders = {
                        focusedOrderId = deliveryReceipt?.reference
                        deliveryReceipt = null
                        deliveryStep = "details"
                        screen = "orders"; refresh++; feedback = null
                    },
                    onCollection = {
                        deliveryReceipt = null
                        deliveryStep = "details"
                        screen = "catalog"; feedback = null
                    },
                )
            }
            "business" -> {
                PlantupiaBusinessInquiryScreen(
                    repository = repository,
                    onBack = { screen = "catalog"; refresh++ },
                    onAcknowledged = { scope.launch { screenScroll.animateScrollTo(0) } },
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
