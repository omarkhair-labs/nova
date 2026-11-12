package com.nova.app.feature.plants

import androidx.test.platform.app.InstrumentationRegistry
import com.nova.app.feature.plants.data.PlantupiaDemoJournal
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlantupiaDemoJournalInstrumentedTest {
    @Test fun immutableRetriesSeparateAccountsAndBackendOrigins() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val origin = "http://127.0.0.1:8766/api/v1/"
        val otherOrigin = "http://127.0.0.1:8765/api/v1/"
        val body = JSONObject().put("body", "Explicit synthetic QA support text")
        val first = PlantupiaDemoJournal.prepare(context, 999997, origin, "qa-journal-test", "service-requests/", body)
        try {
            val retry = PlantupiaDemoJournal.prepare(context, 999997, origin, "qa-journal-test", "service-requests/", body)
            assertEquals(first.getString("client_id"), retry.getString("client_id"))
            assertTrue(PlantupiaDemoJournal.pending(context, 999998, origin).isEmpty())
            assertTrue(PlantupiaDemoJournal.pending(context, 999997, otherOrigin).isEmpty())
            var rejected = false
            try { PlantupiaDemoJournal.prepare(context, 999997, origin, "qa-journal-test", "service-requests/", JSONObject().put("body", "Changed")) }
            catch (_: IllegalStateException) { rejected = true }
            assertTrue(rejected)
        } finally { PlantupiaDemoJournal.clear(context, 999997, origin, first) }
    }
}
