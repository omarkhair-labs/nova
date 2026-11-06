package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val shopInk = Color(0xFF123B2E)

internal fun qaDeliveryFormReady(name: String, phone: String, city: String, street: String,
    latitude: Double?, longitude: Double?, pinConfirmed: Boolean): Boolean =
    name.trim().length >= 2 && phone.count(Char::isDigit) >= 7 &&
    city.trim().length >= 2 && street.trim().length >= 5 &&
    latitude != null && longitude != null && pinConfirmed && validDeliveryPin(latitude, longitude)

/** Isolated company SKU request, NOT the existing fictional cart or a payment checkout. */
@Composable
internal fun PlantupiaCommerceQaJourney(repository: PlantupiaRepository) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf(emptyList<JSONObject>()) }
    var requests by remember { mutableStateOf(emptyList<JSONObject>()) }
    var selected by rememberSaveable { mutableStateOf("") }
    var qty by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var street by rememberSaveable { mutableStateOf("") }
    var building by rememberSaveable { mutableStateOf("") }
    var landmark by rememberSaveable { mutableStateOf("") }
    var lat by rememberSaveable { mutableStateOf<Double?>(null) }
    var lon by rememberSaveable { mutableStateOf<Double?>(null) }
    var pinConfirmed by rememberSaveable { mutableStateOf(false) }
    var payment by rememberSaveable { mutableStateOf("cod") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var showMap by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        when (val r = repository.demoGet("commerce/qa/catalog/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                items = r.value.demoRows()
                if (selected.isBlank() && items.isNotEmpty()) selected = items.first().optString("id")
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = r.message
        }
        when (val r = repository.demoGet("commerce/qa/requests/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) requests = r.value.demoRows()
            is ApiResult.Failure -> Unit
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Delivery request · QA", style = PlantupiaText.editorialHeading)
        Text("Testing only. No payment, delivery, or stock reservation.", style = PlantupiaText.supporting)
        Text("01 / Exact product SKU", style = PlantupiaText.sectionLabel)
        if (items.isEmpty()) Text("No counted internal QA products are available.", style = PlantupiaText.supporting)
        items.forEach { item ->
            val current = selected == item.optString("id")
            OutlinedButton(onClick = { selected = item.optString("id") }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth()) {
                    Text((if (current) "✓ " else "") + item.optString("product_name"), color = shopInk)
                    Text(item.optString("sku") + " · " + item.optString("option") +
                        " · EGP " + item.optString("price"), style = PlantupiaText.supporting)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { if (qty > 1) qty-- }) { Text("−") }
            Text("Qty: " + qty, modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = { if (qty < 10) qty++ }) { Text("+") }
        }
        HorizontalDivider()
        Text("02 / Your exact delivery address", style = PlantupiaText.sectionLabel)
        OutlinedTextField(name, { name = it.take(160) }, label = { Text("Recipient name") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(phone, { phone = it.take(32) }, label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(city, { city = it.take(120) }, label = { Text("City / governorate") },
            modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(street, { street = it.take(320) }, label = { Text("Street / address") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(building, { building = it.take(180) }, label = { Text("Building / floor / flat") },
            modifier = Modifier.fillMaxWidth())
        OutlinedTextField(landmark, { landmark = it.take(180) }, label = { Text("Nearby landmark") },
            modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { showMap = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (pinConfirmed) "✓ Exact pin selected · Edit" else "Set exact delivery pin on map")
        }
        if (pinConfirmed) Text("Confirmed pin: " +
            "%.6f, %.6f".format(java.util.Locale.US, lat, lon), style = PlantupiaText.supporting)
        Text("The selected pin is not proof of address accuracy. Confirm the entrance and building.",
            style = PlantupiaText.supporting)
        HorizontalDivider()
        Text("03 / Payment preference (not collected)", style = PlantupiaText.sectionLabel)
        OutlinedButton(onClick = { payment = "cod" }) {
            Text((if (payment == "cod") "✓ " else "") + "Cash on delivery")
        }
        OutlinedButton(onClick = { payment = "manual_transfer" }) {
            Text((if (payment == "manual_transfer") "✓ " else "") + "InstaPay / bank transfer")
        }
        Row {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("I confirm this is a no-charge QA request, not a real purchase.",
                style = PlantupiaText.supporting, modifier = Modifier.padding(top = 12.dp))
        }
        Button(
            enabled = !busy && consent && selected.isNotBlank() &&
                qaDeliveryFormReady(name, phone, city, street, lat, lon, pinConfirmed),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = shopInk),
            onClick = {
                busy = true
                feedback = null
                scope.launch {
                    val payload = JSONObject()
                        .put("lines", JSONArray().put(JSONObject().put("sku_id", selected).put("quantity", qty)))
                        .put("contact_name", name.trim()).put("contact_phone", phone.trim())
                        .put("city", city.trim()).put("delivery_address", street.trim())
                        .put("building_details", building.trim()).put("landmark", landmark.trim())
                        .put("delivery_latitude", lat).put("delivery_longitude", lon)
                        .put("delivery_pin_confirmed", true)
                        .put("payment_preference", payment).put("confirm_qa_no_payment", true)
                    when (val r = repository.demoMutation(
                        "commerce-qa-pin", "commerce/qa/requests/", payload)) {
                        is ApiResult.Success -> if (repository.isCurrentSession()) {
                            feedback = "QA request saved: " + r.value.optString("id").take(8) +
                                ". No charge or delivery."
                            refresh++
                        }
                        is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = r.message
                    }
                    busy = false
                }
            },
        ) { Text(if (busy) "Saving…" else "Submit QA request · no charge") }
        feedback?.let { Text(it, color = shopInk) }
        HorizontalDivider()
        Text("My QA requests", style = PlantupiaText.sectionLabel)
        TextButton(onClick = { refresh++ }) { Text("Refresh") }
        requests.take(10).forEach { order ->
            Text(order.optString("id").take(8) + " · " + order.optString("status") +
                " · EGP " + order.optString("provisional_subtotal") + " provisional",
                style = PlantupiaText.supporting)
        }
    }
    if (showMap) PlantupiaDeliveryPinPicker(lat, lon,
        onDismiss = { showMap = false },
        onConfirm = { latitude, longitude ->
            lat = latitude; lon = longitude; pinConfirmed = true; showMap = false
        },
    )
}
