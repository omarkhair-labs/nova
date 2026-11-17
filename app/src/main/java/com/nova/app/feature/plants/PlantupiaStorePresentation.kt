package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.feature.plants.data.PlantupiaRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.json.JSONArray
import org.json.JSONObject

@Composable
internal fun StorePanel(
    modifier: Modifier = Modifier.fillMaxWidth(),
    accent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = PlantupiaShapes.card,
        color = if (accent) PlantupiaColors.softSage else PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content,
        )
    }
}

@Composable
internal fun StoreCatalog(
    families: Map<String, List<JSONObject>>,
    visibleCount: Int,
    repository: PlantupiaRepository,
    onProduct: (String) -> Unit,
) {
    val entries = families.entries.take(visibleCount)
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val columns = plantupiaShopProductColumns(maxWidth.value, fontScale)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            entries.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { (id, variants) ->
                        key(id) {
                            val product = variants.first()
                            val available = variants.filter { it.optBoolean("in_stock") }
                            val priced = available.ifEmpty { variants }
                            val price = priced.minByOrNull {
                                it.optString("price").toBigDecimalOrNull() ?: java.math.BigDecimal.ZERO
                            }?.optString("price").orEmpty()
                            val gallery = product.optJSONArray("photos")
                            val cover = product.optBoolean("has_image")
                            val firstGallery = gallery?.optJSONObject(0)
                            val hasPhoto = cover || firstGallery?.optString("id").orEmpty().isNotBlank()
                            val kind = product.optString("kind")
                            val interaction = remember { MutableInteractionSource() }
                            val pressed by interaction.collectIsPressedAsState()
                            val pressScale by animateFloatAsState(
                                targetValue = if (pressed) .985f else 1f,
                                animationSpec = spring(dampingRatio = .88f, stiffness = 600f),
                                label = "shop-object-press",
                            )
                            Surface(
                                modifier = Modifier.weight(1f).graphicsLayer {
                                    scaleX = pressScale
                                    scaleY = pressScale
                                },
                                interactionSource = interaction,
                                onClick = { onProduct(id) },
                                shape = RoundedCornerShape(13.dp),
                                color = PlantupiaColors.paper,
                                border = BorderStroke(1.dp, PlantupiaColors.border),
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                                    if (hasPhoto) {
                                        Box(
                                            Modifier.fillMaxWidth().height(174.dp)
                                                .background(PlantupiaColors.softSage),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            PlantupiaCatalogPhoto(
                                                productId = id, repository = repository,
                                                compact = true, catalogPath = "commerce/catalog",
                                                photoId = if (cover) null else firstGallery?.optString("id"),
                                                photoAlt = if (cover) product.optString("image_description")
                                                    else firstGallery?.optString("alt_text").orEmpty(),
                                                fullObject = true,
                                            )
                                        }
                                    } else {
                                        PlantupiaShopObjectAwaitingPhoto(kind)
                                    }
                                    Column(
                                        Modifier.fillMaxWidth().padding(13.dp),
                                        verticalArrangement = Arrangement.spacedBy(7.dp),
                                    ) {
                                        Text(
                                            storeCategoryLabel(kind).uppercase(),
                                            style = PlantupiaText.sectionLabel,
                                        )
                                        Text(
                                            product.optString("name"),
                                            style = PlantupiaText.body.copy(fontWeight = FontWeight.SemiBold),
                                            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            if (priced.size > 1) "From EGP $price" else "EGP $price",
                                            style = PlantupiaText.body,
                                        )
                                        Text(
                                            if (available.isEmpty()) "CURRENTLY UNAVAILABLE"
                                            else "EXPLORE THE OBJECT  ↗",
                                            style = PlantupiaText.sectionLabel.copy(
                                                color = if (available.isEmpty())
                                                    PlantupiaColors.secondaryText
                                                else PlantupiaColors.forest,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun storeCategoryLabel(kind: String): String = when (kind) {
    "plant" -> "Plants"
    "planter" -> "Pots"
    "care" -> "Care"
    else -> "Shop"
}

internal fun storeVariantName(product: JSONObject): String =
    product.optString("variant_label").ifBlank { product.optString("sku") }

internal fun storeOrderStatus(status: String): String = when (status) {
    "quote_pending" -> "Delivery quote requested"
    "quoted" -> "Ready for your approval"
    "confirmed" -> "Order confirmed"
    "preparing" -> "Being prepared"
    "dispatched" -> "With the delivery team"
    "delivered" -> "Delivered"
    "cancelled" -> "Cancelled"
    "declined" -> "Unable to fulfill"
    "return_requested" -> "Return requested"
    "returned" -> "Returned"
    else -> "Check for an update"
}

internal fun storePaymentStatus(status: String): String = when (status) {
    "unpaid" -> "Payment pending"
    "verified" -> "Payment verified"
    "refunded" -> "Refund verified"
    else -> "Payment status unavailable"
}

internal fun storeDateTime(value: String): Instant? =
    runCatching { Instant.parse(value) }.getOrNull()

internal fun storeDateLabel(time: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withZone(ZoneId.systemDefault()).format(time)

@Composable
internal fun StoreOrderHistory(timeline: JSONArray) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (index in timeline.length() - 1 downTo 0) {
            val event = timeline.optJSONObject(index) ?: continue
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("•", color = PlantupiaColors.forest)
                Column {
                    Text(event.optString("label"), style = PlantupiaText.body)
                    storeDateTime(event.optString("at"))?.let {
                        Text(storeDateLabel(it), style = PlantupiaText.supporting)
                    }
                }
            }
        }
    }
}


@Composable
internal fun StoreOrderCard(
    order: JSONObject,
    focused: Boolean,
    busy: Boolean,
    onReviewQuote: () -> Unit,
    onCancel: () -> Unit,
    onOpenPlant: (String) -> Unit,
    onTransfer: (String) -> Unit,
    onReturn: (String, String) -> Unit,
) {
    val id = order.optString("id")
    val status = order.optString("status")
    StorePanel(accent = status == "quoted" || focused) {
        Text("Order " + id.take(8), style = PlantupiaText.sectionLabel)
        Text(storeOrderStatus(status), style = PlantupiaText.editorialHeading)
        Text("Products · " + displayedEGP(order.optString("subtotal")))
        val shippingFee = order.optString("shipping_fee")
        if (order.isNull("shipping_fee")) {
            if (status == "quote_pending") {
                Text("Plantupia is checking the delivery fee. Come back here to review the total.", style = PlantupiaText.body)
            }
        } else {
            Text("Shipping · " + displayedEGP(shippingFee))
            Text("Exact total · " + displayedEGP(order.optString("final_total")),
                fontWeight = FontWeight.SemiBold)
        }
        val expires = storeDateTime(order.optString("shipping_quote_expires_at"))
        if (status == "quoted" && expires != null && expires <= Instant.now()) {
            Text("This quote expired. Contact Plantupia for an updated delivery fee.", style = PlantupiaText.body)
        } else if (status == "quoted" && !order.isNull("final_total")) {
            expires?.let { Text("Quote valid until " + storeDateLabel(it), style = PlantupiaText.supporting) }
            Button(
                modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                onClick = onReviewQuote,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
            ) { Text("Review total & confirm") }
        }
        HorizontalDivider(color = PlantupiaColors.border)
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
                fontWeight = FontWeight.SemiBold, color = PlantupiaColors.forest,
            )
            Text(
                "Add your own photo and placement. A shop species label is not a verified identity.",
                style = PlantupiaText.supporting,
            )
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenPlant(deliveredPlants.optString(0)) },
            ) { Text("Open delivered plants") }
        }
        val timeline = order.optJSONArray("timeline")
        if (timeline != null && timeline.length() > 0) {
            var expanded by rememberSaveable(id) { mutableStateOf(false) }
            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Hide order updates" else "Show order updates")
            }
            if (expanded) StoreOrderHistory(timeline)
        }
        Text(storePaymentStatus(order.optString("payment_status")),
            style = PlantupiaText.sectionLabel)
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
                    onSubmit = onTransfer,
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
                onSubmit = onReturn,
            )
        }
        if (order.optBoolean("refunded")) {
            Text("Plantupia recorded a verified external refund.",
                style = PlantupiaText.supporting)
        }
        if (status in listOf("quote_pending", "quoted", "confirmed")) {
            OutlinedButton(
                enabled = !busy,
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Cancel order") }
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
            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
        ) { Text("Submit request for company review") }
    }
}
