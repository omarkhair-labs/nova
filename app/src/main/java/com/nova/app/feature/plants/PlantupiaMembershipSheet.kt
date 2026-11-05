package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nova.app.core.network.ApiResult
import com.nova.app.feature.plants.data.PlantupiaRepository
import org.json.JSONObject
import kotlinx.coroutines.launch

/** A discoverable membership entry. Uses the SAME existing QA entitlement API. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantupiaMembershipSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val repository = remember(context) { PlantupiaRepository(context.applicationContext) }
    var access by remember { mutableStateOf<JSONObject?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var agreed by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository, refresh) {
        loading = true
        error = null
        when (val result = repository.demoGet("demo/access/")) {
            is ApiResult.Success -> if (repository.isCurrentSession()) access = result.value
            is ApiResult.Failure -> if (repository.isCurrentSession()) {
                access = null
                error = "Plantupia Care plans aren't available for this account right now."
            }
        }
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = PlantupiaColors.cream,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("PLANTUPIA CARE", style = PlantupiaText.sectionLabel)
            Text("Care that grows with you.", style = PlantupiaText.editorialHeading)
            Text("Your plants, private care records, history and self-chosen check-ins remain available without a plan.",
                style = PlantupiaText.supporting)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = PlantupiaColors.softSage,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, PlantupiaColors.border),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Free care", fontWeight = FontWeight.SemiBold)
                    Text("My Plants · Record care · Living History · optional local check-ins",
                        style = PlantupiaText.supporting)
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { refresh++ }) { Text("Retry access check") }
            }
            access?.let { current ->
                val active = current.optBoolean("entitled")
                val currentOffer = current.optJSONObject("offer")
                if (active && currentOffer != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.softSage,
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Current preview access", style = PlantupiaText.sectionLabel)
                            Text(currentOffer.optString("name"), fontWeight = FontWeight.SemiBold)
                            val expiry = current.optString("expires_at")
                            if (expiry.isNotBlank() && expiry != "null") {
                                Text("Expires: ${expiry.take(16).replace('T', ' ')}", style = PlantupiaText.supporting)
                            }
                            Text("AI limit: ${currentOffer.optInt("ai_daily_limit")} questions per day",
                                style = PlantupiaText.supporting)
                            if (current.optBoolean("human_access")) Text(
                                "Team requests: ${currentOffer.optInt("human_limit")} per 30 days",
                                style = PlantupiaText.supporting,
                            )
                            if (!current.optBoolean("ai_configured")) Text(
                                "AI replies are not available on this preview server.",
                                style = PlantupiaText.supporting,
                            )
                        }
                    }
                }
                Text("Optional care support", fontWeight = FontWeight.SemiBold)
                if (current.optJSONArray("offers")?.length() == 0) {
                    Text("Care plans are not available yet.", style = PlantupiaText.supporting)
                }
                current.demoRows("offers").forEach { offer ->
                    val human = offer.optInt("human_limit") > 0
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = PlantupiaColors.cream,
                        border = BorderStroke(1.dp, PlantupiaColors.border),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(offer.optString("name"), modifier = Modifier.weight(1f),
                                    fontWeight = FontWeight.SemiBold)
                                Text("EGP ${offer.optString("price")}", fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (human) "AI + private human team requests" else "AI plant-care questions",
                                style = PlantupiaText.supporting)
                            Text("${offer.optInt("ai_daily_limit")} AI questions/day" +
                                if (human) " · ${offer.optInt("human_limit")} team requests/30 days" else "",
                                style = PlantupiaText.supporting)
                            Text("Concept monthly price only · ${offer.optInt("qa_hours")} hours of no-charge QA access",
                                style = PlantupiaText.supporting)
                            if (!active) Button(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                                enabled = agreed && !busy,
                                colors = ButtonDefaults.buttonColors(containerColor = PlantupiaColors.forest),
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        try {
                                            val result = repository.demoMutation(
                                                "access", "demo/access/",
                                                JSONObject().put("offer", offer.getString("key"))
                                                    .put("confirm_no_payment", true),
                                            )
                                            if (repository.isCurrentSession()) when (result) {
                                                is ApiResult.Success -> {
                                                    agreed = false
                                                    refresh++
                                                }
                                                is ApiResult.Failure -> error = result.message
                                            }
                                        } finally {
                                            busy = false
                                        }
                                    }
                                },
                            ) { Text(if (busy) "Activating…" else "Try this plan — no charge") }
                        }
                    }
                }
                if (!active && current.demoRows("offers").isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = agreed, onCheckedChange = { agreed = it }, enabled = !busy)
                        Text("I understand this is temporary QA access, not a paid subscription.",
                            modifier = Modifier.weight(1f), style = PlantupiaText.supporting)
                    }
                }
                Text("No recurring billing, payment method or company support response time is offered here.",
                    style = PlantupiaText.supporting)
            }
            TextButton(onClick = onDismiss) { Text("Back to profile") }
            Spacer(Modifier.height(20.dp))
        }
    }
}
