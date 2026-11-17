package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
 * acknowledged *request for a quote*. The native map remains the real picker.
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
) {
    val issues = shopDeliveryFieldIssues(draft)
    val ready = shopDeliveryReady(draft, cart)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        if (step == "received" && receipt != null) {
            DeliveryQuoteReceived(receipt, onOrders, onCollection)
            return@Column
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("THE DELIVERY READING / 05", style = PlantupiaText.sectionLabel)
            Text(
                if (step == "review") "Before you ask for a quote"
                else "Where should these objects go?",
                style = PlantupiaText.editorialHeading,
            )
            Text(
                "First we learn where to deliver, then you review the details. " +
                    "Plantupia will quote the delivery fee before you decide to confirm anything.",
                style = PlantupiaText.body,
            )
        }

        // A compact progress rail, not a tab strip with hidden form state.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            DeliveryPhase(
                number = "01", name = "Address & contact",
                active = step == "details", modifier = Modifier.weight(1f),
            )
            DeliveryPhase(
                number = "02", name = "Review request",
                active = step == "review", modifier = Modifier.weight(1f),
            )
        }

        if (step == "details") {
            DeliverySection("01 / THE RECIPIENT", "Tell us where the delivery should arrive") {
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
                Text(
                    "The location picker is an exact point you choose, not an automatic " +
                        "verification of the building or your current location.",
                    style = PlantupiaText.supporting,
                )
            }
            DeliverySection("02 / PAYMENT PREFERENCE", "How would you prefer to settle later?") {
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
            DeliverySection("03 / A HUMAN NOTE", "Anything useful about the entrance?") {
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
            DeliverySection("BEFORE REVIEW", "Items, not yet a final order") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Saved items subtotal", style = PlantupiaText.body)
                    Text("EGP " + (cart?.optString("subtotal") ?: "—"),
                        style = PlantupiaText.body, fontWeight = FontWeight.SemiBold)
                }
                Text(
                    "The delivery fee is not known yet. This form will only request a quote, " +
                        "not take payment or reserve any items.",
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
                ) { Text("Review delivery quote request  ↗") }
                TextButton(onClick = onCart, enabled = !busy) {
                    Text("← Back to saved cart", color = PlantupiaColors.forest)
                }
            }
        } else if (step == "review") {
            DeliverySection("01 / WHAT YOU CHOSE", "The saved cart") {
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
                Text("Shipping: awaiting a company quote · No full total yet.",
                    style = PlantupiaText.supporting)
            }
            DeliverySection("02 / WHERE TO GO", "The recipient and chosen pin") {
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

            DeliverySection("03 / WHAT HAPPENS NEXT", "Only a delivery quote request") {
                Text(
                    "Plantupia reviews the route and provides an exact shipping fee. " +
                        "You'll then see the full amount and choose whether to accept. " +
                        "Submitting this request does not reserve stock or take any money.",
                    style = PlantupiaText.body,
                )
                if (error != null) {
                    Text(
                        error, color = PlantupiaColors.errorText,
                        style = PlantupiaText.supporting,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text("If the connection was interrupted, check Your requests before resubmitting.",
                        style = PlantupiaText.supporting)
                }
                if (!ready) {
                    Text(
                        "Your saved items or delivery details changed. Edit them or refresh the cart before sending.",
                        color = PlantupiaColors.errorText, style = PlantupiaText.supporting,
                    )
                }
                Button(
                    onClick = onSubmit, enabled = ready && !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                ) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp,
                            color = PlantupiaColors.paper)
                        Spacer(Modifier.width(9.dp))
                    }
                    Text(if (busy) "Sending quote request…" else "Send delivery quote request")
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
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (active) PlantupiaColors.forest else PlantupiaColors.softSage,
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
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(11.dp)) {
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
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("PLANTUPIA / RECEIVED", style = PlantupiaText.sectionLabel)
        Text("Delivery quote requested", style = PlantupiaText.editorialHeading)
        Text(
            "Your request reached the Plantupia order system. The team can now review the route " +
                "and provide a shipping price for you to approve.",
            style = PlantupiaText.body,
        )
        Surface(
            color = PlantupiaColors.softSage, shape = RoundedCornerShape(13.dp),
            border = BorderStroke(1.dp, PlantupiaColors.border),
        ) {
            Column(Modifier.fillMaxWidth().padding(17.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("REQUEST REFERENCE", style = PlantupiaText.sectionLabel)
                Text(receipt.reference, style = PlantupiaText.body,
                    fontWeight = FontWeight.SemiBold)
                HorizontalDivider(color = PlantupiaColors.border)
                Text("Products · EGP " + receipt.productSubtotal, style = PlantupiaText.body)
                Text("Recipient · " + receipt.recipient + ", " + receipt.city,
                    style = PlantupiaText.supporting)
                Text("Shipping · Not yet quoted", style = PlantupiaText.supporting)
                Text("No payment was taken and no stock was reserved.",
                    style = PlantupiaText.supporting)
            }
        }
        Button(
            onClick = onOrders,
            modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
        ) { Text("View this request  ↗") }
        OutlinedButton(
            onClick = onCollection,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) { Text("Return to the living collection") }
    }
}
