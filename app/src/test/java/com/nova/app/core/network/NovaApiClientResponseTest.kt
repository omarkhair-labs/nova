package com.nova.app.core.network

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.net.InetSocketAddress

class NovaApiClientResponseTest {
    private fun withResponse(status: Int, body: String, test: (NovaApiClient) -> Unit) {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            exchange.requestBody.use { it.readBytes() }
            val bytes = body.toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(status, if (status == 204 || status == 205) -1 else bytes.size.toLong())
            exchange.responseBody.use { if (bytes.isNotEmpty()) it.write(bytes) }
        }
        server.start()
        try { test(NovaApiClient("http://127.0.0.1:${server.address.port}/")) }
        finally { server.stop(0) }
    }

    @Test fun malformedSuccessIsFailureForJsonAndMultipart() {
        for (body in listOf("", " ", "null", "[]", "42", "private server trace", "{broken", "{} trailing", "{}{}")) {
            withResponse(200, body) { client ->
                val json = runBlocking { client.requestJson("records/") }
                assertTrue("JSON accepted: $body", json is ApiResult.Failure)
                val multipart = runBlocking {
                    client.requestMultipart("records/", "POST", emptyMap(), "photo", null, "test-only-token")
                }
                assertTrue("multipart accepted: $body", multipart is ApiResult.Failure)
                assertFalse((json as ApiResult.Failure).message.contains("private server trace"))
            }
        }
    }

    @Test fun validObjectRemainsAvailableToFeatureContractValidation() {
        withResponse(200, "{\"results\":[]}") { client ->
            val result = runBlocking { client.requestJson("records/") }
            assertTrue(result is ApiResult.Success)
            assertEquals(0, (result as ApiResult.Success).value.getJSONArray("results").length())
        }
        // Transport cannot impose one feature's required fields on every Nova API.
        withResponse(200, "{}") { client -> assertTrue(runBlocking { client.requestJson("object/") } is ApiResult.Success) }
    }

    @Test fun noContentDeleteResponsesRemainCompatible() {
        for (status in listOf(204, 205)) withResponse(status, "") { client ->
            val result = runBlocking { client.requestJson("resource/", method = "DELETE") }
            assertTrue(result is ApiResult.Success)
            assertEquals(0, (result as ApiResult.Success).value.length())
        }
    }

    @Test fun existingJsonErrorStatusAndMessageArePreserved() {
        withResponse(403, "{\"detail\":\"Read-only space membership.\"}") { client ->
            val result = runBlocking { client.requestJson("records/") } as ApiResult.Failure
            assertEquals(403, result.statusCode)
            assertEquals("Read-only space membership.", result.message)
        }
    }
}
