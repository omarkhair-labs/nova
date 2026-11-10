package com.nova.app.feature.plants

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import kotlinx.coroutines.launch
import org.json.JSONObject

private val businessInk = Color(0xFF123B2E)

/**
 * Separate B2B journey from the same real customer Shop.
 * A quote accepted here requests follow-up only. It never reserves inventory,
 * produces a purchase order, collects payment or creates an installation.
 */
@Composable
internal fun PlantupiaBusinessInquiryScreen(
    repository: PlantupiaRepository,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var companyName by rememberSaveable { mutableStateOf("") }
    var contact by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("wholesale") }
    var quantity by rememberSaveable { mutableStateOf("1") }
    var description by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var inquiries by remember { mutableStateOf(emptyList<JSONObject>()) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        loaded = false
        when (val result = repository.demoGet("commerce/business/inquiries/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) {
                val rows = result.value.optJSONArray("results")
                inquiries = if (rows == null) emptyList() else
                    (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }
                loaded = true
            }
            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                inquiries = emptyList()
                feedback = result.message
                loaded = true
            }
        }
    }

    fun submitInquiry() {
        if (busy) return
        val number = quantity.toIntOrNull() ?: return
        if (number !in 1..10000) return
        busy = true
        feedback = null
        scope.launch {
            try {
                val payload = JSONObject()
                    .put("kind", kind)
                    .put("company_name", companyName.trim())
                    .put("contact_name", contact.trim())
                    .put("phone", phone.trim())
                    .put("email", email.trim())
                    .put("city", city.trim())
                    .put("quantity", number)
                    .put("scope", description.trim())
                when (val result = repository.demoMutation(
                    "real-business-inquiry", "commerce/business/inquiries/", payload,
                )) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        feedback = "Business request submitted. Plantupia will review the scope; this is not a purchase order."
                        refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                }
            } finally { busy = false }
        }
    }

    fun acceptBusinessQuote(id: String, version: Int) {
        if (busy) return
        busy = true
        feedback = null
        scope.launch {
            try {
                when (val result = repository.demoPost(
                    "commerce/business/inquiries/" + id + "/accept/",
                    JSONObject().put("quote_version", version),
                )) {
                    is ApiResult.Success -> if (repository.isCurrentSession()) {
                        feedback = "Quote accepted for commercial follow-up. No stock, payment or installation was created."
                        refresh++
                    }
                    is ApiResult.Failure -> if (repository.isCurrentSession()) feedback = result.message
                }
            } finally { busy = false }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Wholesale supply or installation project", style = PlantupiaText.editorialHeading)
        Text("Describe what your organization needs. Plantupia can return a nonbinding EGP quotation. " +
            "A signed contract, stock allocation, installation and QR handover are separate steps.",
            style = PlantupiaText.supporting)
        OutlinedButton(onClick = onBack, enabled = !busy) { Text("Back to products") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { kind = "wholesale" },
                enabled = !busy,
            ) { Text((if (kind == "wholesale") "✓ " else "") + "Wholesale") }
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = { kind = "installation" },
                enabled = !busy,
            ) { Text((if (kind == "installation") "✓ " else "") + "Installation") }
        }
        OutlinedTextField(companyName, { companyName = it.take(180) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Organization / company") })
        OutlinedTextField(contact, { contact = it.take(160) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Contact person") })
        OutlinedTextField(phone, { phone = it.take(32) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Contact phone") })
        OutlinedTextField(email, { email = it.take(254) },
            modifier = Modifier.fillMaxWidth(), label = { Text("Email (optional)") })
        OutlinedTextField(city, { city = it.take(120) },
            modifier = Modifier.fillMaxWidth(), label = { Text("City / site area") })
        OutlinedTextField(quantity,
            { input -> quantity = input.filter { it.isDigit() }.take(5) },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Estimated number of items") })
        OutlinedTextField(description, { description = it.take(2000) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What should be supplied or installed?") },
            minLines = 3)
        val amount = quantity.toIntOrNull()
        Button(
            enabled = !busy && companyName.trim().length >= 2 &&
                contact.trim().length >= 2 && phone.trim().length >= 7 &&
                city.trim().length >= 2 && amount != null && amount in 1..10000 &&
                description.trim().length >= 20,
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = businessInk),
            onClick = { submitInquiry() },
        ) { Text(if (busy) "Submitting…" else "Request business quotation") }

        feedback?.let { Text(it, style = PlantupiaText.supporting) }
        HorizontalDivider()
        Text("Your business requests", style = PlantupiaText.editorialHeading)
        if (!loaded) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (loaded && inquiries.isEmpty()) Text("No commercial enquiries yet.",
            style = PlantupiaText.supporting)
        inquiries.forEach { item ->
            val id = item.optString("id")
            val status = item.optString("status")
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.optString("company_name"), fontWeight = FontWeight.SemiBold)
                    Text(item.optString("kind") + " · " + status,
                        style = PlantupiaText.supporting)
                    if (!item.isNull("quoted_amount")) {
                        Text("Company quotation · EGP " + item.optString("quoted_amount"),
                            color = businessInk, fontWeight = FontWeight.SemiBold)
                        Text(item.optString("quote_note"), style = PlantupiaText.supporting)
                    }
                    if (status == "quoted") {
                        Button(
                            enabled = !busy,
                            colors = ButtonDefaults.buttonColors(containerColor = businessInk),
                            onClick = { acceptBusinessQuote(id, item.optInt("quote_version")) },
                        ) { Text("Accept quote for follow-up") }
                    }
                    Text("No purchase order, payment or project was created by this request.",
                        style = PlantupiaText.supporting)
                }
            }
        }
    }
}
