package com.nova.app.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class HttpErrorSanitizationTest {
    @Test fun hidesHtml404PageInsteadOfDisplayingIt() {
        val html = "<!doctype html><html><head><title>Not Found</title></head></html>"
        val message = htmlErrorMessage(404, html)!!
        assertEquals("This API isn't available on the connected server yet.", message)
        assertFalse(message.contains("<html"))
    }

    @Test fun hidesProxyErrorsButRetainsStructuredApiErrors() {
        assertEquals(
            "The server returned a page instead of API data. Please try again later.",
            htmlErrorMessage(502, "  <HTML>Bad gateway</HTML>"),
        )
        assertNull(htmlErrorMessage(404, "{\"detail\":\"Plant not found.\"}"))
    }
}
