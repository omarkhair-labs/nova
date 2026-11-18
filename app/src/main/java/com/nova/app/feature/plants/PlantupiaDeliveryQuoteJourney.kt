package com.nova.app.feature.plants

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONObject

/**
 * Chapter 05: one authored route through recipient/address/map -> review ->
 * acknowledged customer request. The native map remains the real picker.
 */
@Composable
internal fun PlantupiaDeliveryQuoteJourney(
    draft: ShopDeliveryDraft,
    cart: JSONObject?,
    step: String,
    receipt: ShopDeliveryQuoteReceipt?,
    busy: Boolean,
    error: String?,
    showErrors: Boolean,
    onRecipient: (String) -> Unit,
    onPhone: (String) -> Unit,
    onCity: (String) -> Unit,
    onStreet: (String) -> Unit,
    onBuilding: (String) -> Unit,
    onLandmark: (String) -> Unit,
    onNote: (String) -> Unit,
    onPin: (Double, Double) -> Unit,
    onPayment: (String) -> Unit,
    onReview: () -> Unit,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onCart: () -> Unit,
    onOrders: () -> Unit,
    onCollection: () -> Unit,
    exactPrice: ShopDeliveryExactPrice? = null,
    priceChecked: Boolean = false,
    priceLoading: Boolean = false,
    priceError: String? = null,
    onReloadPrice: () -> Unit = {},
) {
    val issues = shopDeliveryFieldIssues(draft)
    val ready = shopDeliveryReady(draft, cart)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        if (step == "received" && receipt != null) {
            DeliveryQuoteReceived(receipt, onOrders, onCollection)
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("DELIVERY / " + if (step == "review") "REVIEW" else "ADDRESS",
                style = PlantupiaText.sectionLabel)
            Text(
                if (step == "review") "Review your total"
                else "Where should we deliver?",
                style = PlantupiaText.editorialHeading,
            )
            if (step == "details") {
                Text("See the delivery fee and full price before placing an order.",
                    style = PlantupiaText.supporting)
            }
        }

        // A compact progress rail, not a tab strip with hidden form state.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            DeliveryPhase(
                number = "01", name = "Address & contact",
                active = step == "details", modifier = Modifier.weight(1f),
            )
            DeliveryPhase(
                number = "02", name = "Review & order",
                active = step == "review", modifier = Modifier.weight(1f),
            )
        }

        if (step == "details") {
            DeliverySection("01 / ADDRESS", "Recipient and delivery point") {
                PlantupiaDeliveryAddressFields(
                    name = draft.recipient, onNameChange = onRecipient,
                    phone = draft.phone, onPhoneChange = onPhone,
                    city = draft.city, onCityChange = onCity,
                    street = draft.street, onStreetChange = onStreet,
                    building = draft.building, onBuildingChange = onBuilding,
                    landmark = draft.landmark, onLandmarkChange = onLandmark,
                    latitude = draft.latitude, longitude = draft.longitude,
                    pinConfirmed = draft.pinConfirmed, onPinConfirmed = onPin,
                    showErrors = showErrors,
                )
                Text("Your map pin is chosen by you, not independently verified.",
                    style = PlantupiaText.supporting)
            }
            DeliverySection("02 / PAYMENT", "Choose a payment preference") {
                Column(
                    Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    DeliveryPaymentOption(
                        selected = draft.paymentPreference == "cod",
                        title = "Cash on delivery",
                        explanation = "A preference only. No payment is collected now.",
                        onSelect = { onPayment("cod") },
                    )
                    DeliveryPaymentOption(
                        selected = draft.paymentPreference == "manual_transfer",
                        title = "Manual bank transfer",
                        explanation = "Payment details and verification are separate later steps.",
                        onSelect = { onPayment("manual_transfer") },
                    )
                }
            }
            DeliverySection("03 / OPTIONAL", "Delivery instructions") {
                OutlinedTextField(
                    value = draft.note,
                    onValueChange = { onNote(it.take(500)) },
                    label = { Text("Delivery note (optional)") },
                    placeholder = { Text("Access instructions, if needed") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2, maxLines = 5,
                )
                Text("${draft.note.length} / 500", style = PlantupiaText.sectionLabel)
            }
            DeliverySection("COST", "Items and delivery") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saved items subtotal", style = PlantupiaText.body)
                    Text("EGP " + (cart?.optString("subtotal") ?: "—"),
                        style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    when {
                        exactPrice != null -> "Delivery · EGP " + exactPrice.shippingFee +
                            " · Total EGP " + exactPrice.total
                        priceLoading || !priceChecked -> "Checking the delivery price for this city…"
                        priceError != null -> "Delivery price unavailable; retry before continuing."
                        else -> "No set delivery fee for this city. The team can review a request."
                    },
                    style = PlantupiaText.supporting,
                )
                if (showErrors && issues.isNotEmpty()) {
                    Text(
                        "Please check: " + issues.joinToString(", "),
                        color = PlantupiaColors.errorText,
                        style = PlantupiaText.supporting,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
                if (cart?.optBoolean("can_request_shipping_quote") != true) {
                    Text("Review your current saved cart before continuing.",
                        color = PlantupiaColors.errorText, style = PlantupiaText.supporting)
                }
                Button(
                    onClick = onReview, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                ) { Text("Review total  ↗") }
                TextButton(onClick = onCart, enabled = !busy) {
                    Text("← Back to saved cart", color = PlantupiaColors.forest)
                }
            }
        } else if (step == "review") {
            DeliverySection("01 / ITEMS", "Your cart") {
                val lines = shopCartObjects(cart)
                lines.forEach { item ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(item.name, style = PlantupiaText.body,
                                fontWeight = FontWeight.SemiBold)
                            Text(item.variant + " · " + item.quantity + " unit(s)",
                                style = PlantupiaText.supporting)
                        }
                        Text("EGP " + item.lineTotal, style = PlantupiaText.body)
                    }
                }
                HorizontalDivider(color = PlantupiaColors.border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Items only", style = PlantupiaText.body)
                    Text("EGP " + (cart?.optString("subtotal") ?: "—"),
                        style = PlantupiaText.editorialHeading)
                }
                if (exactPrice != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Delivery", style = PlantupiaText.body)
                        Text("EGP " + exactPrice.shippingFee, style = PlantupiaText.body)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total to confirm", style = PlantupiaText.body)
                        Text("EGP " + exactPrice.total, style = PlantupiaText.editorialHeading)
                    }
                } else {
                    Text("Delivery · To be confirmed. No final total yet.",
                        style = PlantupiaText.supporting)
                }
            }
            DeliverySection("02 / DELIVERY", "Send to") {
                Text(draft.recipient.trim(), style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                Text(normalizedDeliveryPhone(draft.phone), style = PlantupiaText.supporting)
                Text(draft.street.trim() + " · " + draft.city.trim(), style = PlantupiaText.body)
                if (draft.building.isNotBlank()) {
                    Text(draft.building.trim(), style = PlantupiaText.supporting)
                }
                if (draft.landmark.isNotBlank()) {
                    Text("Nearby: " + draft.landmark.trim(), style = PlantupiaText.supporting)
                }
                Text(
                    if (draft.pinConfirmed) "Exact pin selected by you" else "Exact map pin not confirmed",
                    style = PlantupiaText.supporting,
                    color = if (draft.pinConfirmed) PlantupiaColors.forest
                    else PlantupiaColors.errorText,
                )
                Text(
                    if (draft.paymentPreference == "cod") "Preference · Cash on delivery"
                    else "Preference · Manual bank transfer",
                    style = PlantupiaText.supporting,
                )
                if (draft.note.isNotBlank()) {
                    Text("Note: " + draft.note.trim(), style = PlantupiaText.supporting)
                }
                OutlinedButton(
                    onClick = onEdit, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text("Edit address or preference") }
            }

            DeliverySection("03 / CONFIRMATION",
                if (exactPrice != null) "Place your order" else "Delivery needs review") {
                Text(
                    if (exactPrice != null)
                        "This confirms the total shown above. Payment is handled separately."
                    else
                        "No approved fee for this city yet. The team must confirm a final total before you can place an order.",
                    style = PlantupiaText.body,
                )
                if (priceLoading || !priceChecked) {
                    Text("Checking the latest delivery price…", style = PlantupiaText.supporting)
                }
                if (priceError != null) {
                    Text(priceError, color = PlantupiaColors.errorText,
                        style = PlantupiaText.supporting)
                    TextButton(onClick = onReloadPrice, enabled = !busy) {
                        Text("Retry delivery price check", color = PlantupiaColors.forest)
                    }
                }
                if (error != null) {
                    Text(
                        error, color = PlantupiaColors.errorText,
                        style = PlantupiaText.supporting,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text("If the connection was interrupted, check Your requests before resubmitting.",
                        style = PlantupiaText.supporting)
                    OutlinedButton(
                        onClick = onOrders, enabled = !busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
                    ) { Text("Check your existing requests  ↗") }
                }
                if (!ready) {
                    Text(
                        "Your saved items or delivery details changed. Edit them or refresh the cart before sending.",
                        color = PlantupiaColors.errorText, style = PlantupiaText.supporting,
                    )
                }
                Button(
                    onClick = onSubmit,
                    enabled = ready && !busy && error == null && priceChecked &&
                        !priceLoading && priceError == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp,
                            color = PlantupiaColors.paper)
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(
                        when {
                            busy -> if (exactPrice != null) "Placing your order…" else "Sending your request…"
                            exactPrice != null -> "Place Order"
                            else -> "Request delivery confirmation"
                        },
                    )
                }
                TextButton(onClick = onCart, enabled = !busy) {
                    Text("← Review saved cart", color = PlantupiaColors.forest)
                }
            }
        }
    }
}

@Composable
private fun DeliveryPhase(number: String, name: String, active: Boolean, modifier: Modifier) {
    val phaseColor by animateColorAsState(
        targetValue = if (active) PlantupiaColors.forest else PlantupiaColors.softSage,
        animationSpec = tween(190),
        label = "delivery-phase-change",
    )
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = phaseColor,
        border = BorderStroke(1.dp, if (active) PlantupiaColors.forest else PlantupiaColors.border),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(number, style = PlantupiaText.sectionLabel,
                color = if (active) PlantupiaColors.paper else PlantupiaColors.secondaryText)
            Text(name, style = PlantupiaText.supporting,
                color = if (active) PlantupiaColors.paper else PlantupiaColors.forest)
        }
    }
}

@Composable
private fun DeliverySection(
    eyebrow: String, heading: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(eyebrow, style = PlantupiaText.sectionLabel)
        Text(heading, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
        content()
        HorizontalDivider(color = PlantupiaColors.border)
    }
}

@Composable
private fun DeliveryPaymentOption(
    selected: Boolean, title: String, explanation: String, onSelect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().selectable(
            selected = selected, role = Role.RadioButton, onClick = onSelect,
        ),
        color = if (selected) PlantupiaColors.softSage else PlantupiaColors.paper,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) PlantupiaColors.forest else PlantupiaColors.border,
        ),
    ) {
        Row(Modifier.fillMaxWidth().padding(13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            RadioButton(selected = selected, onClick = null)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                Text(explanation, style = PlantupiaText.supporting)
            }
        }
    }
}

@Composable
private fun DeliveryQuoteReceived(
    receipt: ShopDeliveryQuoteReceipt,
    onOrders: () -> Unit,
    onCollection: () -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(19.dp),
    ) {
        Text(if (receipt.confirmed) "PLANTUPIA  /  ORDER CONFIRMED"
             else "PLANTUPIA  /  YOUR REQUEST", style = PlantupiaText.sectionLabel)
        Text(if (receipt.confirmed) "Your order is confirmed."
             else "We've received your request.", style = PlantupiaText.editorialHeading)
        Text(
            if (receipt.confirmed)
                "Your items have been ordered at the displayed total. The Plantupia " +
                    "team can now prepare them. Follow the order for real updates."
            else
                "Your selection and delivery details are saved. Plantupia will review " +
                    "delivery cost before you decide whether to confirm the final total.",
            style = PlantupiaText.body,
        )

        Surface(
            color = PlantupiaColors.softSage,
            shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            Column(
                Modifier.fillMaxWidth().padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("YOUR REFERENCE", style = PlantupiaText.sectionLabel)
                Text(receipt.reference, style = PlantupiaText.body,
                    fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = PlantupiaColors.border)
                Text("THE OBJECTS", style = PlantupiaText.sectionLabel)
                receipt.lines.forEach { line ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(line.name, style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                            Text(
                                listOfNotNull(
                                    line.variant.takeIf { it.isNotBlank() },
                                    line.quantity.toString() + " unit(s)",
                                ).joinToString(" · "),
                                style = PlantupiaText.supporting,
                            )
                        }
                        Text("EGP " + line.subtotal, style = PlantupiaText.body)
                    }
                }
                HorizontalDivider(color = PlantupiaColors.border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Items subtotal", style = PlantupiaText.body)
                    Text("EGP " + receipt.productSubtotal, style = PlantupiaText.body,
                        fontWeight = FontWeight.SemiBold)
                }
                Text(
                    if (receipt.confirmed) "Delivery · EGP " + receipt.shippingFee
                    else "Delivery · To be confirmed",
                    style = PlantupiaText.supporting,
                )
                if (receipt.confirmed) {
                    Text("Total · EGP " + receipt.finalTotal, style = PlantupiaText.body,
                        fontWeight = FontWeight.SemiBold)
                }
                Text("To · " + receipt.recipient + ", " + receipt.city,
                    style = PlantupiaText.supporting)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("WHAT HAPPENS NEXT", style = PlantupiaText.sectionLabel)
            if (receipt.confirmed) {
                Text("01  Your order is confirmed.", style = PlantupiaText.body)
                Text("02  Plantupia prepares the items.", style = PlantupiaText.body)
                Text("03  Follow the recorded delivery updates.", style = PlantupiaText.body)
                Text("No payment or shipment has been recorded yet.",
                    style = PlantupiaText.supporting)
            } else {
                Text("01  Your request has been recorded.", style = PlantupiaText.body)
                Text("02  The team checks delivery and availability.", style = PlantupiaText.body)
                Text("03  You review any final price before confirming.", style = PlantupiaText.body)
                Text("No payment, stock reservation or shipment has been recorded.",
                    style = PlantupiaText.supporting)
            }
        }
        Button(
            onClick = onOrders,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
        ) { Text(if (receipt.confirmed) "Track your order  ↗"
             else "See your request & updates  ↗") }
        OutlinedButton(
            onClick = onCollection,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Continue exploring") }
    }
}
