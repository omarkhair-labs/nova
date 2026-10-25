package com.nova.app.feature.plants.data

import org.json.JSONObject

// Deliberately no generated toString containing a delivery secret.
internal class PendingPlantupiaHandover(val token: String, val owner: Long?, val createdAt: Long)

internal fun deliveryCode(value: String): String? = value.trim().takeIf {
    it.matches(Regex("[A-Za-z0-9_-]{40,160}"))
}

internal fun canResumeHandover(pending: PendingPlantupiaHandover, account: Long?, now: Long): Boolean =
    now >= pending.createdAt && now - pending.createdAt < 24 * 60 * 60 * 1000L &&
        (pending.owner == null || pending.owner == account)

data class PlantupiaDelivery(val claimed: Boolean, val plantCount: Int, val spaceLabel: String, val spaceId: String?)

internal fun parseDelivery(json: JSONObject, claim: Boolean): PlantupiaDelivery {
    val state = requiredPlantupiaString(json, "state")
    require(state == "ready" || state == "claimed")
    require(!claim || state == "claimed")
    val count = requiredPlantupiaLong(json, "plant_count")
    require(count <= Int.MAX_VALUE)
    require(requiredPlantupiaString(json, "ownership_mode") in setOf("customer_owned", "managed"))
    return PlantupiaDelivery(state == "claimed", count.toInt(), requiredPlantupiaString(json, "space_label"),
        if (claim) requiredPlantupiaString(json, "space_id") else null)
}

internal fun handoverFailure(status: Int?): String = when (status) {
    401 -> "Please log in again, then reopen your delivery."
    404 -> "This code is invalid, revoked, or belongs to another account. Check your code and signed-in account."
    410 -> "This invitation has expired. Ask the team that supplied it for a new code."
    409 -> "This delivery is no longer available to this account. Ask the team that supplied the code to verify access."
    429 -> "Too many attempts. Wait a minute, then try again."
    503 -> "Plantupia activation is not available on this server yet. Your own plants and care records remain available."
    else -> "We couldn't confirm your delivery. Check your connection and retry the same code. Nothing is shown as connected until the server confirms it."
}

data class PlantupiaCareGuide(
    val verifiedIdentity: Boolean,
    val placement: String,
    val version: Int? = null,
    val species: String = "",
    val instructions: String = "",
    val contextNotes: String = "",
    val sourceUrl: String = "",
    val sourceReference: String = "",
    val reviewer: String = "",
    val reviewedAt: String = "",
)

internal fun parseCareGuide(json: JSONObject, plantId: String): PlantupiaCareGuide {
    require(requiredPlantupiaString(json, "plant_id") == plantId)
    require(json.opt("identity_verified") is Boolean)
    val verified = json.getBoolean("identity_verified")
    val state = requiredPlantupiaString(json, "state")
    require(state in setOf("unverified", "available"))
    val placement = json.getString("placement")
    if (state == "unverified") {
        require(json.has("guide") && json.isNull("guide"))
        return PlantupiaCareGuide(verified, placement)
    }
    require(verified)
    val guide = json.getJSONObject("guide")
    val version = requiredPlantupiaLong(guide, "version")
    require(version <= Int.MAX_VALUE)
    val source = requiredPlantupiaString(guide, "source_url")
    require(source.startsWith("https://"))
    return PlantupiaCareGuide(verified, placement, version.toInt(), requiredPlantupiaString(guide, "species"),
        requiredPlantupiaString(guide, "instructions"), guide.getString("context_notes"), source,
        requiredPlantupiaString(guide, "source_reference"), requiredPlantupiaString(guide, "reviewer"),
        requiredPlantupiaString(guide, "reviewed_at"))
}
