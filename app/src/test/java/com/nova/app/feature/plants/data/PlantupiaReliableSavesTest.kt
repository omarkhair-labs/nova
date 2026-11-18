package com.nova.app.feature.plants.data

import android.content.ContentResolver
import android.net.Uri
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.auth.withRealSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.io.ByteArrayInputStream
import java.net.InetSocketAddress
import java.nio.file.Files
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

class PlantupiaReliableSavesTest {
    /** Real TCP close after the complete multipart body, before any acknowledgement. */
    private class DroppedUploadServer : AutoCloseable {
        private val socket = java.net.ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"))
        val bodies = CopyOnWriteArrayList<String>()
        val committed = java.util.concurrent.ConcurrentHashMap<String, String>()
        @Volatile var acknowledge = false
        @Volatile var error: Throwable? = null
        val api get() = NovaApiClient("http://127.0.0.1:${socket.localPort}/")
        private val worker = Thread {
            while (!socket.isClosed) {
                try {
                    socket.accept().use { client ->
                        client.soTimeout = 5_000
                        val input = client.getInputStream()
                        val header = java.io.ByteArrayOutputStream()
                        while (!header.toString(Charsets.ISO_8859_1).endsWith("\r\n\r\n")) {
                            val byte = input.read()
                            check(byte >= 0 && header.size() < 16_384)
                            header.write(byte)
                        }
                        val length = Regex("(?i)Content-Length: (\\d+)").find(header.toString(Charsets.ISO_8859_1))!!.groupValues[1].toInt()
                        val body = input.readNBytes(length).toString(Charsets.ISO_8859_1)
                        bodies += body
                        val id = Regex("name=\"client_submission_id\".*?\\r\\n\\r\\n([^\\r]+)", RegexOption.DOT_MATCHES_ALL).find(body)!!.groupValues[1]
                        val result = committed.getOrPut(id) { UUID.randomUUID().toString() }
                        if (acknowledge) {
                            val bytes = JSONObject().put("id", result).put("client_submission_id", id).toString().toByteArray()
                            client.getOutputStream().apply {
                                write("HTTP/1.1 200 OK\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\n\r\n".toByteArray())
                                write(bytes); flush()
                            }
                        } // Closing the actual socket guarantees an immediate lost acknowledgement.
                    }
                } catch (failure: Throwable) { if (!socket.isClosed) error = failure }
            }
        }.apply { isDaemon = true; start() }
        override fun close() { socket.close(); worker.join(2_000) }
    }

    @Test(timeout = 20_000) fun actualMultipartDisconnectAfterCommitReusesOneIdentityAndPhoto() = runBlocking {
        withRealSessionStore { store, context ->
            val root = Files.createTempDirectory("plantupia-dropped-upload").toFile()
            Mockito.`when`(context.noBackupFilesDir).thenReturn(root)
            val resolver = Mockito.mock(ContentResolver::class.java)
            val uri = Mockito.mock(Uri::class.java)
            Mockito.`when`(context.contentResolver).thenReturn(resolver)
            Mockito.`when`(resolver.getType(uri)).thenReturn("image/png")
            Mockito.`when`(resolver.openInputStream(uri)).thenAnswer { ByteArrayInputStream("owned-photo".toByteArray()) }
            try { DroppedUploadServer().use { server ->
                store.save(store.beginLogin(), login(1))
                val repository = PlantupiaRepository(context, server.api)
                assertTrue(repository.recordWithPhoto("plant-a", "care", "Original", uri) is ApiResult.Failure)
                val pending = (repository.pendingSubmissions("plant-a") as ApiResult.Success).value.single()
                server.acknowledge = true
                assertTrue(PlantupiaRepository(context, server.api).retrySubmission(pending) is ApiResult.Success)
                assertNull(server.error)
                assertEquals(1, server.committed.size)
                assertTrue(server.bodies.size >= 2)
                assertTrue(server.bodies.all { it.contains(pending.id) && it.contains("owned-photo") })
            } } finally { root.deleteRecursively() }
        }
    }

    @Test fun queuedCreationReminderCannotDeliverIntoAnotherLoginOfTheSameUser() {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), login(1))
            val original = store.load()!!.identity.generation
            store.save(store.beginLogin(), login(1))
            context.getSharedPreferences("plantupia_local_checkins", 0).edit().putLong("event-a", 1).commit()
            assertFalse(com.nova.app.feature.plants.PlantupiaCareReminderScheduler.consumeIfAuthorized(context, "event-a", 1, original))
            assertEquals(1L, context.getSharedPreferences("plantupia_local_checkins", 0).getLong("event-a", -1))
        }
    }

    private class Server : AutoCloseable {
        val requests = CopyOnWriteArrayList<Triple<String, String, String>>()
        val committed = mutableMapOf<String, String>()
        @Volatile var acknowledge = false
        @Volatile var rejection: String? = null
        private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
            createContext("/") { exchange ->
                val body = exchange.requestBody.readBytes().toString(Charsets.ISO_8859_1)
                val json = if (body.startsWith("{")) JSONObject(body) else null
                val id = json?.getString("client_submission_id") ?: Regex("name=\"client_submission_id\".*?\\r\\n\\r\\n([^\\r]+)", RegexOption.DOT_MATCHES_ALL).find(body)!!.groupValues[1]
                val actor = exchange.requestHeaders.getFirst("Authorization")
                requests += Triple(actor, id, body)
                val result = if (rejection == null) synchronized(committed) { committed.getOrPut("$actor:$id") { UUID.randomUUID().toString() } } else "rejected"
                val response = if (rejection != null) JSONObject().put("detail", "Rejected").put("code", rejection).put("request_id", id).toString()
                    else if (acknowledge) JSONObject().put("id", result).put("post_id", 42).put("client_submission_id", id).toString()
                    else "{\"id\":" // Commit accepted; acknowledgement cannot be decoded.
                exchange.responseHeaders.add("Connection", "close")
                // HttpURLConnection observes an incomplete stream immediately; OkHttp
                // waits for HttpServer's keep-alive timeout despite exchange.close().
                // Multipart therefore receives a complete but unusable acknowledgement.
                exchange.sendResponseHeaders(if (rejection != null) 400 else 200, response.length.toLong() + if (json != null && !acknowledge && rejection == null) 64 else 0)
                try { exchange.responseBody.write(response.toByteArray()) }
                finally { runCatching { exchange.responseBody.close() }; exchange.close() }
            }
            start()
        }
        val api get() = NovaApiClient("http://127.0.0.1:${server.address.port}/")
        override fun close() { server.stop(0) }
    }

    @Test(timeout = 20_000) fun lostJsonAckAndRecreatedRepositoryRetryOneLogicalRecord() = runBlocking {
        withRealSessionStore { store, context ->
            val root = Files.createTempDirectory("plantupia-reliable-save").toFile()
            Mockito.`when`(context.noBackupFilesDir).thenReturn(root)
            try { Server().use { server ->
                store.save(store.beginLogin(), login(1))
                val repository = PlantupiaRepository(context, server.api)
                assertTrue(repository.record("plant-a", "care", "Original care") is ApiResult.Failure)
                val before = (repository.pendingSubmissions("plant-a") as ApiResult.Success).value.single()
                server.acknowledge = true
                val recreated = PlantupiaRepository(context, server.api)
                assertTrue(recreated.record("plant-a", "care", "Original care") is ApiResult.Success)
                assertEquals(1, server.committed.size)
                assertEquals(1, server.requests.map { it.second }.distinct().size)
                val wire = server.requests.map { JSONObject(it.third) }
                assertEquals(1, wire.map { it.getString("observed_at") }.distinct().size)
                val pending = (recreated.pendingSubmissions("plant-a") as ApiResult.Success).value.single()
                assertEquals(before.id, pending.id)
                assertNotNull(pending.acknowledgedResult)
                recreated.confirmSubmission("plant-a", pending.slot, pending.acknowledgedResult!!)
                assertTrue((recreated.pendingSubmissions("plant-a") as ApiResult.Success).value.isEmpty())
            } } finally { root.deleteRecursively() }
        }
    }

    @Test(timeout = 20_000) fun multipartRetryUsesOwnedPhotoAfterOriginalUriExpiresAndRequiresPublicReview() = runBlocking {
        withRealSessionStore { store, context ->
            val root = Files.createTempDirectory("plantupia-reliable-save").toFile()
            Mockito.`when`(context.noBackupFilesDir).thenReturn(root)
            val resolver = Mockito.mock(ContentResolver::class.java)
            val uri = Mockito.mock(Uri::class.java)
            Mockito.`when`(context.contentResolver).thenReturn(resolver)
            Mockito.`when`(resolver.getType(uri)).thenReturn("image/png")
            val photo = "exact-photo-owned-by-A".toByteArray()
            Mockito.`when`(resolver.openInputStream(uri)).thenAnswer { ByteArrayInputStream(photo) }
            try { Server().use { server ->
                store.save(store.beginLogin(), login(1))
                val repository = PlantupiaRepository(context, server.api)
                assertTrue(repository.askWithPhoto("plant-a", "Topic", "Original question", uri) is ApiResult.Failure)
                val pending = (repository.pendingSubmissions("plant-a") as ApiResult.Success).value.single()
                assertArrayEquals(photo, pending.photo!!.readBytes())
                Mockito.`when`(resolver.openInputStream(uri)).thenThrow(java.io.FileNotFoundException("Expired picker permission"))
                server.acknowledge = true
                val recreated = PlantupiaRepository(context, server.api)
                val calls = server.requests.size
                assertTrue(recreated.retrySubmission(pending) is ApiResult.Failure)
                assertEquals(calls, server.requests.size) // No public retry without renewed explicit review.
                assertEquals(ApiResult.Success("42"), recreated.retrySubmission(pending, publicConsentConfirmed = true))
                assertEquals(1, server.committed.size)
                assertEquals(1, server.requests.map { it.second }.distinct().size)
                assertTrue(server.requests.all { it.third.contains(photo.toString(Charsets.ISO_8859_1)) })
            } } finally { root.deleteRecursively() }
        }
    }

    @Test(timeout = 20_000) fun accountSwitchCannotReadOrSendAsAnotherAccountsPendingSubmission() = runBlocking {
        withRealSessionStore { store, context ->
            val root = Files.createTempDirectory("plantupia-reliable-save").toFile()
            Mockito.`when`(context.noBackupFilesDir).thenReturn(root)
            try { Server().use { server ->
                store.save(store.beginLogin(), login(1))
                val a = PlantupiaRepository(context, server.api)
                a.record("plant-a", "care", "Private A")
                val pending = (a.pendingSubmissions("plant-a") as ApiResult.Success).value.single()
                store.logout()
                store.save(store.beginLogin(), login(2))
                val b = PlantupiaRepository(context, server.api)
                val calls = server.requests.size
                assertTrue((b.pendingSubmissions("plant-a") as ApiResult.Success).value.isEmpty())
                assertEquals(409, (a.retrySubmission(pending) as ApiResult.Failure).statusCode)
                assertEquals(409, (b.retrySubmission(pending) as ApiResult.Failure).statusCode)
                assertEquals(calls, server.requests.size)
                store.save(store.beginLogin(), login(1))
                val newA = PlantupiaRepository(context, server.api)
                server.acknowledge = true
                assertTrue(newA.retrySubmission((newA.pendingSubmissions("plant-a") as ApiResult.Success).value.single()) is ApiResult.Success)
                assertEquals(1, server.committed.size)
            } } finally { root.deleteRecursively() }
        }
    }

    @Test(timeout = 20_000) fun onlyBoundDefinitiveRejectionUnlocksTheFrozenDraft() = runBlocking {
        withRealSessionStore { store, context ->
            val root = Files.createTempDirectory("plantupia-reliable-save").toFile()
            Mockito.`when`(context.noBackupFilesDir).thenReturn(root)
            try { Server().use { server ->
                store.save(store.beginLogin(), login(1))
                val repository = PlantupiaRepository(context, server.api)
                server.rejection = "unknown_proxy_error"
                assertTrue(repository.record("plant-a", "care", "Original") is ApiResult.Failure)
                assertEquals(1, (repository.pendingSubmissions("plant-a") as ApiResult.Success).value.size)
                server.rejection = "submission_rejected"
                assertTrue(repository.record("plant-a", "care", "Original") is ApiResult.Failure)
                assertTrue((repository.pendingSubmissions("plant-a") as ApiResult.Success).value.isEmpty())
            } } finally { root.deleteRecursively() }
        }
    }

    private fun login(id: Long) = AuthSession("access-$id", "refresh-$id", NovaUser(id, "u$id@example.test", "u$id", "User $id", ""))
}
