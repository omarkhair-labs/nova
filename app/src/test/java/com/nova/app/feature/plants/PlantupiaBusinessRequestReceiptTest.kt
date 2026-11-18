package com.nova.app.feature.plants

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaBusinessRequestReceiptTest {
    private fun server() = JSONObject()
        .put("id", "e70b1769-51d3-45b9-84fa-5bc2b217e001")
        .put("kind", "installation")
        .put("company_name", "Green office")
        .put("contact_name", "Test operator")
        .put("phone", "+201000000000")
        .put("city", "Cairo")
        .put("quantity", 12)
        .put("scope", "Installation to be scoped following a site review.")
        .put("status", "received")
        .put("quoted_amount", JSONObject.NULL)

    private fun read(response: JSONObject) = verifiedPlantupiaBusinessReceipt(
        response,
        kind = "installation",
        organization = "Green office",
        contact = "Test operator",
        phone = "+201000000000",
        city = "Cairo",
        quantity = 12,
        scope = "Installation to be scoped following a site review.",
    )

    @Test fun serverReceiptIncludesGenuineReferenceAndIsNotAPurchaseOrder() {
        val receipt = read(server())
        assertNotNull(receipt)
        assertEquals("e70b1769-51d3-45b9-84fa-5bc2b217e001", receipt!!.reference)
        assertEquals("Green office", receipt.organization)
        assertEquals("installation", receipt.kind)
    }

    @Test fun neverShowReceivedIfServerRecordDidNotMatchSubmission() {
        assertNull(read(server().put("status", "quoted")))
        assertNull(read(server().put("kind", "wholesale")))
        assertNull(read(server().put("company_name", "Another company")))
        assertNull(read(server().put("contact_name", "Another contact")))
        assertNull(read(server().put("phone", "+201111111111")))
        assertNull(read(server().put("quantity", 13)))
        assertNull(read(server().put("city", "Giza")))
        assertNull(read(server().put("scope", "Different project")))
        assertNull(read(server().put("quoted_amount", "3500.00")))
        assertNull(read(server().put("id", "not-a-uuid")))
    }
}
