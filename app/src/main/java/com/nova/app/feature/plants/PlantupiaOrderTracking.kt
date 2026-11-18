package com.nova.app.feature.plants

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

/**
 * The customer's order timeline is evidence-led.
 * There is NO carrier GPS feed, estimated delivery time, or automated carrier
 * scan available in CommerceOrder at this stage. Never manufacture one.
 */
internal data class ShopTrackingStage(
    val key: String,
    val title: String,
    val meaning: String,
    val recordedAt: String? = null,
) {
    val recorded: Boolean get() = !recordedAt.isNullOrBlank()
}

internal data class ShopTrackingView(
    val stageTitle: String,
    val description: String,
    val steps: List<ShopTrackingStage>,
    val carrier: String?,
    val trackingReference: String?,
    val liveLocationAvailable: Boolean = false,
)

private fun recordedOrderEvents(events: JSONArray?): Map<String, String> {
    val recorded = linkedMapOf<String, String>()
    if (events == null) return recorded
    for (index in 0 until events.length()) {
        val event = events.optJSONObject(index) ?: continue
        val kind = event.optString("kind")
        val at = event.optString("at")
        if (kind.isNotBlank() && storeDateTime(at) != null) recorded[kind] = at
    }
    return recorded
}

internal fun shopOrderTracking(order: JSONObject): ShopTrackingView {
    val status = order.optString("status")
    val events = recordedOrderEvents(order.optJSONArray("timeline"))
    val fulfillment = order.optJSONObject("fulfillment")
    val carrier = fulfillment?.optString("carrier_name")?.trim()?.takeIf { it.isNotBlank() }
    val reference = fulfillment?.optString("tracking_reference")?.trim()?.takeIf { it.isNotBlank() }
    val dispatchRecorded = events["dispatched"] != null && reference != null && carrier != null
    val proofRecorded = dispatchRecorded &&
        fulfillment?.optBoolean("delivery_proof_recorded") == true && events["delivered"] != null

    if (status in listOf("quote_pending", "quoted", "declined")) {
        return ShopTrackingView(
            stageTitle = when (status) {
                "quote_pending" -> "Request received"
                "quoted" -> "Delivery price ready to review"
                else -> "This quote request could not proceed"
            },
            description = when (status) {
                "quote_pending" -> "Your items and delivery details are saved. The team will confirm delivery cost before you choose to proceed. No payment or shipment yet."
                "quoted" -> "Review the final total. Only your confirmation allows the company to prepare this order."
                else -> "No confirmed delivery is in progress for this request."
            },
            steps = listOf(
                ShopTrackingStage("quote_requested", "Request received",
                    "Plantupia has your saved request; no purchase is confirmed yet.", events["quote_requested"]),
                ShopTrackingStage("shipping_quoted", "Delivery price confirmed by team",
                    "A delivery amount was recorded for you to review.", events["shipping_quoted"]),
            ),
            carrier = null,
            trackingReference = null,
        )
    }

    val isException = status in listOf("cancelled", "return_requested", "returned")
    val stages = listOf(
        ShopTrackingStage(
            if (events["order_placed"] != null) "order_placed" else "quote_accepted",
            if (events["order_placed"] != null) "Order placed" else "Order confirmed",
            "Your order was accepted at the displayed total; payment and dispatch are separate.",
            events["order_placed"] ?: events["quote_accepted"],
        ),
        ShopTrackingStage(
            "preparing", "Preparing your order",
            "Plantupia's operator recorded the start of preparation.",
            events["preparing"],
        ),
        ShopTrackingStage(
            "dispatched", "Handed to delivery team",
            "A carrier handover and tracking reference have been recorded.",
            events["dispatched"].takeIf { dispatchRecorded },
        ),
        ShopTrackingStage(
            "delivered", "Delivery recorded",
            "The company recorded a retained delivery-proof reference.",
            events["delivered"].takeIf { proofRecorded },
        ),
    )
    val title = when (status) {
        "confirmed" -> "Order confirmed"
        "preparing" -> "Your order is being prepared"
        "dispatched" -> "Handed to the delivery team"
        "delivered" -> "Delivery recorded"
        "cancelled" -> "Order cancelled"
        "return_requested" -> "Return under review"
        "returned" -> "Return recorded"
        else -> "Latest recorded order status"
    }
    return ShopTrackingView(
        stageTitle = title,
        description = if (isException) {
            "The order has a separate cancellation or return workflow. See the recorded events for details."
        } else if (status == "delivered" && !proofRecorded) {
            "The current status says delivered, but complete handover/proof details are not available in this response."
        } else if (status == "dispatched" && !dispatchRecorded) {
            "Dispatch is reported, but a complete carrier handover record is unavailable. No live location is known."
        } else {
            "Updates reflect actual customer and operator actions. No courier GPS or arrival time is currently connected."
        },
        steps = stages,
        carrier = if (dispatchRecorded) carrier else null,
        trackingReference = if (dispatchRecorded) reference else null,
    )
}

@Composable
internal fun PlantupiaOrderTracking(order: JSONObject) {
    val tracking = shopOrderTracking(order)
    var showHistory by rememberSaveable(order.optString("id")) { mutableStateOf(false) }
    var showJourney by rememberSaveable(order.optString("id")) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = PlantupiaColors.paper,
        border = BorderStroke(1.dp, PlantupiaColors.border),
        shape = PlantupiaShapes.inset,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Text("ORDER PROGRESS", style = PlantupiaText.sectionLabel)
            Column(
                Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(tracking.stageTitle, style = PlantupiaText.body,
                    fontWeight = FontWeight.SemiBold)
                Text(tracking.description, style = PlantupiaText.supporting)
            }
            TextButton(
                onClick = { showJourney = !showJourney },
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(
                    if (showJourney) "Hide delivery stages ↑" else "View delivery stages ↓",
                    color = PlantupiaColors.forest,
                )
            }
            if (showJourney) {
            HorizontalDivider(color = PlantupiaColors.border)
            tracking.steps.forEachIndexed { index, step ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Surface(
                        modifier = Modifier.size(28.dp),
                        shape = RoundedCornerShape(50),
                        color = if (step.recorded) PlantupiaColors.forest else PlantupiaColors.softSage,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                if (step.recorded) "✓" else (index + 1).toString(),
                                color = if (step.recorded) PlantupiaColors.paper
                                    else PlantupiaColors.secondaryText,
                                style = PlantupiaText.sectionLabel,
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(step.title, style = PlantupiaText.body,
                            fontWeight = if (step.recorded) FontWeight.SemiBold else FontWeight.Normal)
                        Text(step.meaning, style = PlantupiaText.supporting)
                        if (step.recorded) {
                            storeDateTime(step.recordedAt.orEmpty())?.let {
                                Text("Recorded · " + storeDateLabel(it),
                                    style = PlantupiaText.sectionLabel)
                            }
                        } else {
                            Text("Not yet recorded", style = PlantupiaText.sectionLabel)
                        }
                    }
                }
            }
            } // The summary is always visible; stage details are by request.
            if (tracking.carrier != null && tracking.trackingReference != null) {
                HorizontalDivider(color = PlantupiaColors.border)
                Text("CARRIER HANDOVER", style = PlantupiaText.sectionLabel)
                Text(tracking.carrier, style = PlantupiaText.body)
                Text("Tracking reference · " + tracking.trackingReference,
                    style = PlantupiaText.supporting)
            }
            // No map, moving courier pin, delivery countdown, fake ETA, or
            // automatic "out for delivery" milestone without carrier events.
            val timeline = order.optJSONArray("timeline")
            if (timeline != null && timeline.length() > 0) {
                TextButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(if (showHistory) "Hide recorded activity" else "See recorded activity",
                    color = PlantupiaColors.forest) }
                if (showHistory) StoreOrderHistory(timeline)
            }
        }
    }
}
