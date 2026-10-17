package com.nova.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.io.InputStream
import java.io.OutputStream
import java.security.Key
import java.security.KeyStoreSpi
import java.security.Provider
import java.security.Security
import java.security.cert.Certificate
import java.util.Collections
import java.util.Date
import javax.crypto.KeyGenerator

/** Characterization of F01, not a claim that shared authentication is fixed. */
class NovaSessionStoreRefreshRaceTest {
    @Test fun delayedARefreshCanMixItsAccessTokenWithBsSession() = runBlocking {
        withRealSessionStore { store, _ ->
            store.save(session(1, "access-A", "refresh-A"))
            val original = requireNotNull(store.load())
            val refreshReply = CompletableDeferred<String>()
            val callback = async(start = CoroutineStart.UNDISPATCHED) {
                // The real unconditional write used by current Nova refresh callers.
                store.updateAccessToken(refreshReply.await())
            }
            assertEquals("refresh-A", original.refreshToken)
            store.save(session(2, "access-B", "refresh-B"))
            refreshReply.complete("refreshed-access-A")
            callback.await()
            val mixed = requireNotNull(store.load())
            assertEquals(2L, mixed.cachedUser?.id)
            assertEquals("refresh-B", mixed.refreshToken)
            assertEquals("refreshed-access-A", mixed.accessToken)
            assertNotEquals("access-B", mixed.accessToken)
        }
    }

    @Test fun plantupiaRepositoryRejectsBAndSameAccountNewLogin() {
        for (replacement in listOf(session(2, "access-B", "refresh-B"), session(1, "new-access-A", "new-refresh-A"))) {
            withRealSessionStore { store, context ->
                store.save(session(1, "access-A", "refresh-A"))
                val repository = PlantupiaRepository(context)
                assertTrue(repository.isCurrentSession())
                store.save(replacement)
                assertFalse(repository.isCurrentSession())
                assertEquals(replacement.accessToken, store.load()?.accessToken)
                assertEquals(replacement.refreshToken, store.load()?.refreshToken)
            }
        }
    }

    private fun session(id: Long, access: String, refresh: String) = AuthSession(
        access, refresh, NovaUser(id, "user$id@example.test", "user$id", "User $id", ""),
    )

    /** Only Android platform dependencies are substituted; encryption and store code are real. */
    private inline fun withRealSessionStore(test: (NovaSessionStore, Context) -> Unit) {
        val provider = object : Provider("PlantupiaJvmKeyStore", 1.0, "Test-only Android keystore") {
            init { put("KeyStore.AndroidKeyStore", JvmKeyStore::class.java.name) }
        }
        Security.insertProviderAt(provider, 1)
        val values = mutableMapOf<String, Any?>()
        val edits = mutableMapOf<String, Any?>()
        var clearing = false
        lateinit var editor: SharedPreferences.Editor
        editor = Mockito.mock(SharedPreferences.Editor::class.java) { call ->
            when (call.method.name) {
                "clear" -> { clearing = true; editor }
                "remove" -> { edits[call.getArgument(0)] = null; editor }
                "apply", "commit" -> {
                    if (clearing) values.clear()
                    for ((key, value) in edits) if (value == null) values.remove(key) else values[key] = value
                    edits.clear(); clearing = false
                    if (call.method.name == "commit") true else null
                }
                else -> { edits[call.getArgument(0)] = call.getArgument<Any?>(1); editor }
            }
        }
        val prefs = Mockito.mock(SharedPreferences::class.java) { call ->
            when (call.method.name) {
                "edit" -> editor
                "getAll" -> values.toMap()
                "contains" -> values.containsKey(call.getArgument(0))
                else -> values[call.getArgument<String>(0)] ?: call.getArgument<Any?>(1)
            }
        }
        val context = Mockito.mock(Context::class.java)
        Mockito.`when`(context.applicationContext).thenReturn(context)
        Mockito.`when`(context.getSharedPreferences(Mockito.anyString(), Mockito.anyInt())).thenReturn(prefs)
        val base64 = Mockito.mockStatic(Base64::class.java) { call ->
            when (call.method.name) {
                "encodeToString" -> java.util.Base64.getEncoder().encodeToString(call.getArgument<ByteArray>(0))
                "decode" -> java.util.Base64.getDecoder().decode(call.getArgument<String>(0))
                else -> error("Unexpected Base64 call")
            }
        }
        try { test(NovaSessionStore(context), context) }
        finally { base64.close(); Security.removeProvider(provider.name) }
    }

    class JvmKeyStore : KeyStoreSpi() {
        override fun engineGetKey(alias: String?, password: CharArray?): Key = key
        override fun engineGetCertificateChain(alias: String?): Array<Certificate>? = null
        override fun engineGetCertificate(alias: String?): Certificate? = null
        override fun engineGetCreationDate(alias: String?) = Date(0)
        override fun engineSetKeyEntry(alias: String?, key: Key?, password: CharArray?, chain: Array<out Certificate>?) {}
        override fun engineSetKeyEntry(alias: String?, key: ByteArray?, chain: Array<out Certificate>?) {}
        override fun engineSetCertificateEntry(alias: String?, cert: Certificate?) {}
        override fun engineDeleteEntry(alias: String?) {}
        override fun engineAliases() = Collections.enumeration(listOf("nova_session_key"))
        override fun engineContainsAlias(alias: String?) = true
        override fun engineSize() = 1
        override fun engineIsKeyEntry(alias: String?) = true
        override fun engineIsCertificateEntry(alias: String?) = false
        override fun engineGetCertificateAlias(cert: Certificate?): String? = null
        override fun engineStore(stream: OutputStream?, password: CharArray?) {}
        override fun engineLoad(stream: InputStream?, password: CharArray?) {}
        companion object { private val key = KeyGenerator.getInstance("AES").apply { init(128) }.generateKey() }
    }
}
