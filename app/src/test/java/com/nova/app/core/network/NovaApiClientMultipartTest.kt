package com.nova.app.core.network

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets


class NovaApiClientMultipartTest {
    @Test
    fun `text-only multipart has known length and preserves UTF-8 fields`() = withServer { server ->
        val result = runBlocking {
            server.client.requestMultipart(
                path = "multipart-text/",
                method = "POST",
                fields = mapOf("note" to "مساء جميل", "audience" to "followers"),
                files = emptyMap(),
                bearerToken = "test-token",
            )
        }

        assertTrue(result is ApiResult.Success)
        val request = server.singleRequest()
        val payload = request.parseMultipart()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/multipart-text/", request.path)
        assertEquals("مساء جميل", payload.fields["note"])
        assertEquals("followers", payload.fields["audience"])
        assertEquals("Bearer test-token", request.authorization)
        request.assertKnownLengthBody()
    }

    @Test
    fun `profile PUT sends every field when no avatar is selected`() = withServer { server ->
        runBlocking {
            server.client.requestMultipart(
                path = "me/",
                method = "PUT",
                fields = mapOf(
                    "name" to "Omar",
                    "username" to "omar",
                    "profile_theme" to "sunset",
                    "show_orbit" to "true",
                ),
                files = emptyMap(),
                bearerToken = "test-token",
            )
        }

        val request = server.singleRequest()
        val payload = request.parseMultipart()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/", request.path)
        assertEquals("omar", payload.fields["username"])
        assertEquals("sunset", payload.fields["profile_theme"])
        assertTrue(payload.files.isEmpty())
        request.assertKnownLengthBody()
    }

    @Test
    fun `Post multipart sends one byte-backed image with its field and filename`() = withServer { server ->
        runBlocking {
            server.client.requestMultipart(
                path = "posts/",
                method = "POST",
                fields = mapOf("caption" to "A real moment", "media_type" to "image"),
                files = mapOf(
                    "image" to UploadFile(
                        bytes = byteArrayOf(1, 2, 3, 4),
                        fileName = "post.jpg",
                        mimeType = "image/jpeg",
                    ),
                ),
                bearerToken = "test-token",
            )
        }

        val request = server.singleRequest()
        val payload = request.parseMultipart()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/posts/", request.path)
        assertEquals("A real moment", payload.fields["caption"])
        assertEquals("post.jpg", payload.files.getValue("image").fileName)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), payload.files.getValue("image").bytes)
        request.assertKnownLengthBody()
    }

    @Test
    fun `Story multipart streams video file and includes thumbnail`() = withServer { server ->
        val video = File.createTempFile("nova-story-transport-", ".mp4")
        try {
            video.writeBytes(byteArrayOf(9, 8, 7, 6, 5))
            runBlocking {
                server.client.requestMultipart(
                    path = "stories/",
                    method = "POST",
                    fields = mapOf("caption" to "Story", "audience" to "followers"),
                    files = mapOf(
                        "media" to UploadFile(
                            sourceFile = video,
                            fileName = "story.mp4",
                            mimeType = "video/mp4",
                        ),
                        "thumbnail" to UploadFile(
                            bytes = byteArrayOf(4, 3, 2, 1),
                            fileName = "story.jpg",
                            mimeType = "image/jpeg",
                        ),
                    ),
                    bearerToken = "test-token",
                )
            }

            val request = server.singleRequest()
            val payload = request.parseMultipart()
            assertEquals("/api/v1/stories/", request.path)
            assertEquals(setOf("media", "thumbnail"), payload.files.keys)
            assertEquals("story.mp4", payload.files.getValue("media").fileName)
            assertArrayEquals(video.readBytes(), payload.files.getValue("media").bytes)
            request.assertKnownLengthBody()
        } finally {
            video.delete()
        }
    }

    @Test
    fun `Pulse multipart sends publication fields plus prepared video and thumbnail`() = withServer { server ->
        runBlocking {
            server.client.requestMultipart(
                path = "pulses/",
                method = "POST",
                fields = mapOf(
                    "note" to "Right now",
                    "audience" to "close_friends",
                    "category" to "music",
                    "media_type" to "video",
                    "client_publish_id" to "11111111-1111-1111-1111-111111111111",
                ),
                files = mapOf(
                    "media" to UploadFile(byteArrayOf(1, 3, 5), "pulse.mp4", "video/mp4"),
                    "thumbnail" to UploadFile(byteArrayOf(2, 4, 6), "pulse.jpg", "image/jpeg"),
                ),
                bearerToken = "test-token",
            )
        }

        val request = server.singleRequest()
        val payload = request.parseMultipart()
        assertEquals("/api/v1/pulses/", request.path)
        assertEquals("close_friends", payload.fields["audience"])
        assertEquals("music", payload.fields["category"])
        assertEquals("video", payload.fields["media_type"])
        assertEquals("pulse.mp4", payload.files.getValue("media").fileName)
        assertEquals("pulse.jpg", payload.files.getValue("thumbnail").fileName)
        request.assertKnownLengthBody()
    }
}


private class CapturingServer : AutoCloseable {
    private val requests = mutableListOf<CapturedRequest>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange -> capture(exchange) }
        start()
    }
    val client = NovaApiClient("http://127.0.0.1:${server.address.port}/api/v1/")

    fun singleRequest(): CapturedRequest = requests.single()

    private fun capture(exchange: HttpExchange) {
        val body = exchange.requestBody.use { it.readBytes() }
        synchronized(requests) {
            requests += CapturedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                contentType = exchange.requestHeaders.getFirst("Content-Type").orEmpty(),
                contentLength = exchange.requestHeaders.getFirst("Content-Length"),
                transferEncoding = exchange.requestHeaders.getFirst("Transfer-Encoding"),
                authorization = exchange.requestHeaders.getFirst("Authorization"),
                body = body,
            )
        }
        val response = "{\"ok\":true}".toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(200, response.size.toLong())
        exchange.responseBody.use { it.write(response) }
    }

    override fun close() {
        server.stop(0)
    }
}


private data class CapturedRequest(
    val method: String,
    val path: String,
    val contentType: String,
    val contentLength: String?,
    val transferEncoding: String?,
    val authorization: String?,
    val body: ByteArray,
) {
    fun assertKnownLengthBody() {
        assertEquals(body.size.toLong(), contentLength?.toLong())
        assertNull(transferEncoding)
        assertTrue(body.isNotEmpty())
    }

    fun parseMultipart(): ParsedMultipart {
        val boundary = contentType.substringAfter("boundary=", "").trim().trim('"')
        assertTrue("multipart boundary missing", boundary.isNotBlank())
        val bodyText = body.toString(StandardCharsets.ISO_8859_1)
        val fields = linkedMapOf<String, String>()
        val files = linkedMapOf<String, CapturedFile>()
        bodyText.split("--$boundary").forEach { rawPart ->
            val part = rawPart.trimStart('\r', '\n')
            if (!part.contains("Content-Disposition: form-data")) return@forEach
            val headers = part.substringBefore("\r\n\r\n")
            val rawContent = part.substringAfter("\r\n\r\n").removeSuffix("\r\n")
            val name = Regex("name=\"([^\"]+)\"").find(headers)?.groupValues?.get(1)
                ?: return@forEach
            val fileName = Regex("filename=\"([^\"]+)\"").find(headers)?.groupValues?.get(1)
            val bytes = rawContent.toByteArray(StandardCharsets.ISO_8859_1)
            if (fileName == null) {
                fields[name] = bytes.toString(StandardCharsets.UTF_8)
            } else {
                files[name] = CapturedFile(fileName, bytes)
            }
        }
        return ParsedMultipart(fields, files)
    }
}


private data class ParsedMultipart(
    val fields: Map<String, String>,
    val files: Map<String, CapturedFile>,
)


private data class CapturedFile(
    val fileName: String,
    val bytes: ByteArray,
)


private inline fun withServer(block: (CapturingServer) -> Unit) {
    CapturingServer().use(block)
}
