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
import org.json.JSONObject

private val businessInk = Color(0xFF123B2E)

/** One short nonbinding business quote enquiry inside the existing QA shop. */
@Composable
internal fun PlantupiaBusinessQaJourney(repository: PlantupiaRepository) {
    val scope = rememberCoroutineScope()
    var company by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var product by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("10") }
    var details by rememberSaveable { mutableStateOf("") }
    var consent by rememberSaveable { mutableStateOf(false) }
    var items by remember { mutableStateOf(emptyList<JSONObject>()) }
    var quotes by remember { mutableStateOf(emptyList<JSONObject>()) }
    var busy by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        when (val response = repository.demoGet("commerce/qa/catalog/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) items = response.value.demoRows()
            is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = response.message
        }
        when (val response = repository.demoGet("commerce/qa/business/rfqs/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) quotes = response.value.demoRows()
            is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = response.message
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("For business", style = PlantupiaText.editorialHeading)
        Text("Need plants, pots or an installation for a workplace? Request a simple price estimate.",
            style = PlantupiaText.supporting)
        Text("01 / Business contact", style = PlantupiaText.sectionLabel)
        OutlinedTextField(value = company, onValueChange = { company = it.take(180) },
            label = { Text("Company / organization") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = contact, onValueChange = { contact = it.take(160) },
            label = { Text("Contact name") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = phone, onValueChange = { phone = it.take(32) },
            label = { Text("Contact phone") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = city, onValueChange = { city = it.take(120) },
            label = { Text("Project city") }, modifier = Modifier.fillMaxWidth())
        HorizontalDivider()
        Text("02 / What do you need?", style = PlantupiaText.sectionLabel)
        OutlinedButton(onClick = { product = "" }, modifier = Modifier.fillMaxWidth()) {
            Text(if (product.isEmpty()) "✓ Custom project / product request" else "Custom request")
        }
        items.take(20).forEach { item ->
            OutlinedButton(onClick = { product = item.optString("id") },
                modifier = Modifier.fillMaxWidth()) {
                Text((if (product == item.optString("id")) "✓ " else "") +
                    item.optString("product_name") + " · " + item.optString("sku"),
                    color = businessInk)
            }
        }
        OutlinedTextField(value = quantity, onValueChange = {
            quantity = it.filter(Char::isDigit).take(5)
        }, label = { Text("Approximate units") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = details, onValueChange = { details = it.take(1500) },
            label = { Text("Plants, quantities, sites, installation or other needs") },
            modifier = Modifier.fillMaxWidth(), minLines = 3)
        Row {
            Checkbox(checked = consent, onCheckedChange = { consent = it })
            Text("Send a QA quote request — not a binding purchase or payment.",
                style = PlantupiaText.supporting, modifier = Modifier.padding(top = 10.dp))
        }
        val count = quantity.toIntOrNull() ?: 0
        val valid = company.trim().length >= 2 && contact.trim().length >= 2 &&
            phone.count(Char::isDigit) >= 7 && city.trim().length >= 2 &&
            details.trim().length >= 10 && count in 1..10000
        Button(
            enabled = !busy && consent && valid,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = businessInk),
            onClick = {
                busy = true
                scope.launch {
                    val request = JSONObject()
                        .put("company_name", company.trim())
                        .put("contact_name", contact.trim()).put("phone", phone.trim())
                        .put("city", city.trim()).put("quantity", count)
                        .put("scope", details.trim()).put("confirm_qa_no_contract", true)
                    if (product.isNotBlank()) request.put("sku_id", product)
                    when (val result = repository.demoMutation(
                        "b2b-qa-rfq", "commerce/qa/business/rfqs/", request,
                    )) {
                        is ApiResult.Success -> if (repository.isCurrentSession()) {
                            feedback = "RFQ sent · " + result.value.optString("id").take(8)
                            refresh++
                        }
                        is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                    }
                    busy = false
                }
            },
        ) { Text(if (busy) "Sending…" else "Request a quote") }
        feedback?.let { Text(it, color = businessInk) }
        HorizontalDivider()
        Text("Your business enquiries", style = PlantupiaText.sectionLabel)
        TextButton(onClick = { refresh++ }) { Text("Refresh quote status") }
        quotes.take(20).forEach { quote ->
            Surface(color = Color(0xFFEEF1E8), modifier = Modifier.fillMaxWidth(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(quote.optString("company") + " · " + quote.optString("status"),
                        color = businessInk)
                    if (!quote.isNull("quoted_amount")) {
                        Text("QA estimate · EGP " + quote.optString("quoted_amount"))
                        if (quote.optString("quote_note").isNotBlank()) {
                            Text(quote.optString("quote_note"), style = PlantupiaText.supporting)
                        }
                    }
                    if (quote.optString("status") == "qa_quoted") {
                        OutlinedButton(enabled = !busy, onClick = {
                            busy = true
                            scope.launch {
                                when (val result = repository.demoMutation(
                                    "b2b-qa-accept-" + quote.optString("id"),
                                    "commerce/qa/business/rfqs/" + quote.optString("id") + "/accept/",
                                    JSONObject(),
                                )) {
                                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                                        feedback = "Quote acknowledged. Company can follow up."
                                        refresh++
                                    }
                                    is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                                }
                                busy = false
                            }
                        }) { Text("Interested · continue discussion") }
                    }
                }
            }
        }
        Text("Business quotes are nonbinding QA responses. Installation and QR handover require separately reviewed actual plants.",
            style = PlantupiaText.supporting)
    }
}
