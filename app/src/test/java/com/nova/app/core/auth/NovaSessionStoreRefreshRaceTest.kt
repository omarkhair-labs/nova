package com.nova.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import com.nova.app.feature.plants.data.PlantupiaRepository
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.cancelAndJoin
import com.nova.app.core.network.ApiResult
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.core.messaging.NovaMessagingApiClient
import com.nova.app.core.messaging.NovaMessagingRepository
import com.nova.app.core.calls.NovaCallRepository
import com.nova.app.core.push.NovaPushRepository
import java.io.File
import android.net.Uri
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

/** F01 security regressions: delayed work must leave the replacing login untouched. */
class NovaSessionStoreRefreshRaceTest {
    @Test(timeout = 20_000) fun delayedARefreshLeavesBsSessionUnchanged() = runBlocking {
        withRealSessionStore { store, _ ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val original = requireNotNull(store.load())
            val refreshReply = CompletableDeferred<String>()
            val callback = async(start = CoroutineStart.UNDISPATCHED) {
                // Delayed work retains the immutable identity captured before the network request.
                store.updateAccessToken(original.identity, refreshReply.await())
            }
            assertEquals("refresh-A", original.refreshToken)
            store.save(store.beginLogin(), session(2, "access-B", "refresh-B"))
            refreshReply.complete("refreshed-access-A")
            callback.await()
            val current = requireNotNull(store.load())
            assertEquals(2L, current.cachedUser?.id)
            assertEquals("refresh-B", current.refreshToken)
            assertEquals("access-B", current.accessToken)
        }
    }

    @Test(timeout = 20_000) fun plantupiaRepositoryRejectsBAndSameAccountNewLogin() {
        for (replacement in listOf(session(2, "access-B", "refresh-B"), session(1, "new-access-A", "new-refresh-A"))) {
            withRealSessionStore { store, context ->
                store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
                val repository = PlantupiaRepository(context)
                assertTrue(repository.isCurrentSession())
                store.save(store.beginLogin(), replacement)
                assertFalse(repository.isCurrentSession())
                assertEquals(replacement.accessToken, store.load()?.accessToken)
                assertEquals(replacement.refreshToken, store.load()?.refreshToken)
            }
        }
    }

    @Test(timeout = 20_000) fun everyStaleWriteIsRejectedAcrossStoresAfterSwitchLogoutOrSameUserLogin() {
        for (target in listOf("B", "logout", "new-A")) withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val old = requireNotNull(store.load())
            val otherStore = NovaSessionStore(context)
            if (target == "logout") otherStore.logout()
            else otherStore.save(otherStore.beginLogin(), if (target == "B") session(2, "access-B", "refresh-B")
                else session(1, "access-A", "refresh-A")) // Even identical credentials are a new login.
            val expected = otherStore.load()
            val generation = otherStore.identity()
            assertNotEquals(old.identity, generation)
            assertFalse(store.updateAccessToken(old.identity, "late-A"))
            assertFalse(store.updateUser(old.identity, old.cachedUser!!.copy(name = "Late profile")))
            assertFalse(store.clear(old.identity))
            assertFalse(store.save(old.identity, session(1, "restored-A", "refresh-A")))
            assertFalse(store.replaceSession(old.identity, session(1, "replaced-A", "new-refresh-A")))
            assertEquals(expected, otherStore.load())
            assertEquals(generation, otherStore.identity())
        }
    }

    @Test(timeout = 20_000) fun concurrentRefreshAcrossRepositoriesSharesOneRequestAndOneToken() = runBlocking {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val origin = requireNotNull(store.load())
            val other = NovaSessionStore(context)
            val entered = CompletableDeferred<Unit>()
            val reply = CompletableDeferred<ApiResult<String>>()
            val calls = AtomicInteger()
            val refresh: suspend () -> ApiResult<String> = { calls.incrementAndGet(); entered.complete(Unit); reply.await() }
            val first = async(start = CoroutineStart.UNDISPATCHED) { store.refreshAccessToken(origin, refresh) }
            entered.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { other.refreshAccessToken(origin, refresh) }
            reply.complete(ApiResult.Success("fresh-A"))
            assertEquals(ApiResult.Success("fresh-A"), first.await())
            assertEquals(first.await(), second.await())
            assertEquals(1, calls.get())
            assertEquals(origin.identity, store.load()?.identity)
            assertEquals("refresh-A", store.load()?.refreshToken)
            assertEquals("fresh-A", store.load()?.accessToken)
            // A late 401 for the old access token reuses the already refreshed token.
            assertEquals(ApiResult.Success("fresh-A"), other.refreshAccessToken(origin, refresh))
            assertEquals(1, calls.get())
        }
    }

    @Test(timeout = 20_000) fun switchingDuringBackgroundRefreshImmediatelyInvalidatesAllWaiters() = runBlocking {
        for (target in listOf("B", "logout", "new-A")) withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val origin = requireNotNull(store.load())
            val entered = CompletableDeferred<Unit>()
            val reply = CompletableDeferred<ApiResult<String>>()
            val background = async(start = CoroutineStart.UNDISPATCHED) {
                store.realtimeToken(origin) { entered.complete(Unit); reply.await() }
            }
            entered.await()
            val other = NovaSessionStore(context)
            if (target == "logout") other.logout()
            else other.save(other.beginLogin(), if (target == "B") session(2, "access-B", "refresh-B")
                else session(1, "new-access-A", "new-refresh-A"))
            val expected = other.load()
            assertEquals(409, (background.await() as ApiResult.Failure).statusCode)
            reply.complete(ApiResult.Success("late-access-A"))
            assertEquals(expected, other.load())
        }
    }

    @Test(timeout = 20_000) fun failedRefreshIsCoalescedAndTransientFailureRetainsCurrentCredentials() = runBlocking {
        for (status in listOf(503, 401, 400)) withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val original = requireNotNull(store.load())
            val reply = CompletableDeferred<ApiResult<String>>()
            val entered = CompletableDeferred<Unit>()
            val count = AtomicInteger()
            val refresh: suspend () -> ApiResult<String> = { count.incrementAndGet(); entered.complete(Unit); reply.await() }
            val first = async(start = CoroutineStart.UNDISPATCHED) { store.refreshAccessToken(original, refresh) }
            entered.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) { NovaSessionStore(context).refreshAccessToken(original, refresh) }
            reply.complete(ApiResult.Failure("Rejected", status))
            val one = first.await()
            assertEquals(one, second.await())
            assertEquals(1, count.get())
            if (status == 503) assertEquals(original, store.load()) else {
                assertNull(store.load())
                assertEquals(401, (one as ApiResult.Failure).statusCode)
                assertNotEquals(original.identity, store.identity())
            }
        }
    }

    @Test(timeout = 20_000) fun cancellingOneWaiterDoesNotCancelAnotherRepositorysRefresh() = runBlocking {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val origin = requireNotNull(store.load())
            val entered = CompletableDeferred<Unit>()
            val reply = CompletableDeferred<ApiResult<String>>()
            val first = async(start = CoroutineStart.UNDISPATCHED) {
                store.refreshAccessToken(origin) { entered.complete(Unit); reply.await() }
            }
            entered.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) {
                NovaSessionStore(context).refreshAccessToken(origin) { error("Duplicate refresh") }
            }
            first.cancelAndJoin()
            reply.complete(ApiResult.Success("fresh-A"))
            assertEquals(ApiResult.Success("fresh-A"), second.await())
        }
    }

    @Test(timeout = 20_000) fun delayedMeProfileAndFailureResponsesCannotChangeBOrRetryAsB() = runBlocking {
        for (response in listOf(ApiResult.Success(session(1, "", "").user), ApiResult.Failure("Expired", 401))) {
            withRealSessionStore { store, context ->
                store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
                val origin = requireNotNull(store.load())
                val reply = CompletableDeferred<ApiResult<NovaUser>>()
                val tokens = mutableListOf<String>()
                val request = async(start = CoroutineStart.UNDISPATCHED) {
                    store.authenticatedRequest(origin, refresh = { error("Stale call must not refresh") }) { token ->
                        tokens += token; reply.await()
                    }
                }
                val other = NovaSessionStore(context)
                other.save(other.beginLogin(), session(2, "access-B", "refresh-B"))
                val expected = other.load()
                reply.complete(response)
                assertEquals(409, (request.await() as ApiResult.Failure).statusCode)
                assertEquals(listOf("access-A"), tokens)
                assertEquals(expected, other.load())
            }
        }
    }

    @Test(timeout = 20_000) fun credentialReplacementInvalidatesOldWorkAndRejectsDifferentUserProfiles() = runBlocking {
        withRealSessionStore { store, _ ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val before = requireNotNull(store.load())
            assertFalse(store.updateUser(before.identity, session(2, "", "").user))
            assertTrue(store.replaceSession(before.identity, session(1, "password-access", "password-refresh")))
            assertNotEquals(before.identity, store.load()?.identity)
            assertFalse(store.updateAccessToken(before.identity, "late-A"))
            assertFalse(store.clear(before.identity))
            assertEquals("password-access", store.load()?.accessToken)
        }
    }

    @Test(timeout = 20_000) fun legacyEncryptedSessionUpgradesWithoutChangingCredentialsOrProfile() {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val prefs = context.getSharedPreferences("nova_session", Context.MODE_PRIVATE)
            val encrypted = prefs.getString("token_ciphertext", null)
            assertFalse(encrypted!!.contains("access-A"))
            prefs.edit().remove("login_generation").apply()
            val restored = requireNotNull(NovaSessionStore(context).load())
            assertEquals("access-A", restored.accessToken)
            assertEquals("refresh-A", restored.refreshToken)
            assertEquals(1L, restored.cachedUser?.id)
            assertEquals(encrypted, prefs.getString("token_ciphertext", null))
            assertEquals(restored.identity, NovaSessionStore(context).load()?.identity)
        }
    }

    @Test(timeout = 20_000) fun actualRestoreAndProfileRepositoriesRejectDelayedAResponses() = runBlocking {
        for (profile in listOf(false, true)) withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val remote = Mockito.mock(AuthRemoteDataSource::class.java)
            val response = ApiResult.Success(session(1, "", "").user.copy(name = "Late A"))
            if (profile) Mockito.`when`(remote.updateProfile("access-A", "Changed A", "user1"))
                .thenAnswer { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)); response }
            else Mockito.`when`(remote.me("access-A"))
                .thenAnswer { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)); response }
            val repository = NovaAuthRepository(context, remote, activatePush = {})
            val pending = async(Dispatchers.IO) {
                if (profile) repository.updateProfile("Changed A", "user1", null, "", "", "", emptyList(), "violet", true)
                else repository.restoreSession()
            }
            try {
                check(entered.await(10, TimeUnit.SECONDS))
                val other = NovaSessionStore(context)
                other.save(other.beginLogin(), session(2, "access-B", "refresh-B"))
                val expected = other.load()
                release.countDown()
                assertEquals(409, (pending.await() as ApiResult.Failure).statusCode)
                assertEquals(expected, other.load())
            } finally { release.countDown() }
        }
    }

    @Test(timeout = 20_000) fun actualLoginRepositoryRejectsEarlierLoginAndNeverOverwritesB() = runBlocking {
        withRealSessionStore { store, context ->
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val remote = Mockito.mock(AuthRemoteDataSource::class.java)
            Mockito.`when`(remote.login("a@example.test", "pw"))
                .thenAnswer { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)); ApiResult.Success(session(1, "access-A", "refresh-A")) }
            Mockito.`when`(remote.login("b@example.test", "pw"))
                .thenReturn(ApiResult.Success(session(2, "access-B", "refresh-B")))
            val first = async(Dispatchers.IO) { NovaAuthRepository(context, remote, activatePush = {}).login("a@example.test", "pw") }
            try {
                check(entered.await(10, TimeUnit.SECONDS))
                assertTrue(NovaAuthRepository(context, remote, activatePush = {}).login("b@example.test", "pw") is ApiResult.Success)
                val expected = store.load()
                release.countDown()
                assertEquals(409, (first.await() as ApiResult.Failure).statusCode)
                assertEquals(expected, store.load())
            } finally { release.countDown() }
        }
    }

    @Test(timeout = 20_000) fun actualRestoreAfterRefreshUpdatesProfileWithoutCreatingANewLogin() = runBlocking {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val owner = store.identity()
            val remote = Mockito.mock(AuthRemoteDataSource::class.java)
            Mockito.`when`(remote.me("access-A")).thenReturn(ApiResult.Failure("Expired", 401))
            Mockito.`when`(remote.refresh("refresh-A")).thenReturn(ApiResult.Success("fresh-A"))
            val user = session(1, "", "").user.copy(name = "Current profile", plantupiaAvatarVariant = 4)
            Mockito.`when`(remote.me("fresh-A")).thenReturn(ApiResult.Success(user))
            assertEquals(ApiResult.Success(user), NovaAuthRepository(context, remote, activatePush = {}).restoreSession())
            assertEquals(owner, store.identity())
            assertEquals("fresh-A", store.load()?.accessToken)
            assertEquals("refresh-A", store.load()?.refreshToken)
            assertEquals(user, store.load()?.cachedUser)
        }
    }

    @Test(timeout = 20_000) fun delayedRetryFailureAndSuccessCannotClearOrReturnBsData() = runBlocking {
        for (response in listOf(ApiResult.Success("private A reply"), ApiResult.Failure("Old rejection", 401))) {
            withRealSessionStore { store, context ->
                store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
                val origin = requireNotNull(store.load())
                val retried = CompletableDeferred<Unit>()
                val reply = CompletableDeferred<ApiResult<String>>()
                val request = async(start = CoroutineStart.UNDISPATCHED) {
                    store.authenticatedRequest(origin, refresh = { ApiResult.Success("fresh-A") }) { token ->
                        if (token == "access-A") ApiResult.Failure("Expired", 401)
                        else { retried.complete(Unit); reply.await() }
                    }
                }
                retried.await()
                val other = NovaSessionStore(context)
                other.save(other.beginLogin(), session(2, "access-B", "refresh-B"))
                val expected = other.load()
                reply.complete(response)
                assertEquals(409, (request.await() as ApiResult.Failure).statusCode)
                assertEquals(expected, other.load())
            }
        }
    }

    @Test(timeout = 20_000) fun backgroundCallAndPushRepositoriesCannotResumeUnderAnotherLogin() = runBlocking {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val owner = store.identity()
            val call = NovaCallRepository(context, expectedLogin = owner)
            val push = NovaPushRepository(context, expectedLogin = owner)
            store.save(store.beginLogin(), session(2, "access-B", "refresh-B"))
            val expected = store.load()
            assertEquals(409, (call.callAction("old-A-call", "decline") as ApiResult.Failure).statusCode)
            assertEquals(409, (push.register("old-A-installation") as ApiResult.Failure).statusCode)
            assertEquals(expected, store.load())
        }
    }

    @Test(timeout = 20_000) fun messageMediaPreparationCannotSendUsingTheNextAccountsToken() = runBlocking {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val api = Mockito.mock(NovaMessagingApiClient::class.java)
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val temporary = File.createTempFile("nova-session-regression-", ".m4a").apply { writeBytes(byteArrayOf(1, 2, 3)) }
            val audio = object : File(temporary.path) {
                override fun exists(): Boolean {
                    entered.countDown()
                    check(release.await(10, TimeUnit.SECONDS))
                    return super.exists()
                }
            }
            val repository = NovaMessagingRepository(context, api = api)
            val pending = async(Dispatchers.IO) { repository.sendMessage(11, "Private A", "client-A", null, null, audio, 1_000L) }
            try {
                check(entered.await(10, TimeUnit.SECONDS))
                store.save(store.beginLogin(), session(2, "access-B", "refresh-B"))
                val expected = store.load()
                release.countDown()
                assertEquals(409, (pending.await() as ApiResult.Failure).statusCode)
                Mockito.verifyNoInteractions(api)
                assertEquals(expected, store.load())
            } finally { release.countDown(); temporary.delete() }
        }
    }

    @Test(timeout = 20_000) fun plantupiaKeepsItsLoginAcrossAccessRotation() {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val repository = PlantupiaRepository(context)
            val owner = store.identity()
            assertTrue(store.updateAccessToken(owner, "fresh-A"))
            assertTrue(repository.isCurrentSession())
            assertEquals(owner, store.identity())
        }
    }

    @Test(timeout = 20_000) fun throwingRefreshCannotStrandWaitersOrClearTheCurrentLogin() = runBlocking {
        withRealSessionStore { store, _ ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val origin = requireNotNull(store.load())
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            val first = async(start = CoroutineStart.UNDISPATCHED) {
                store.refreshAccessToken(origin) { entered.complete(Unit); release.await(); error("Transport failed") }
            }
            entered.await()
            val second = async(start = CoroutineStart.UNDISPATCHED) {
                store.refreshAccessToken(origin) { error("Duplicate transport") }
            }
            release.complete(Unit)
            assertTrue(first.await() is ApiResult.Failure)
            assertEquals(first.await(), second.await())
            assertEquals(origin, store.load())
        }
    }

    @Test(timeout = 20_000) fun failedRegistrationRetainsItsOptionalPhotoForRetry() = runBlocking {
        withRealSessionStore { store, context ->
            val photo = Mockito.mock(Uri::class.java)
            NovaPendingRegistrationPhoto.set(photo)
            try {
                val remote = Mockito.mock(AuthRemoteDataSource::class.java)
                Mockito.`when`(remote.register("a@example.test", "pw", "user1", "User 1"))
                    .thenReturn(ApiResult.Failure("Try again", 503))
                assertTrue(NovaAuthRepository(context, remote, activatePush = {}).register("a@example.test", "pw", "user1", "User 1") is ApiResult.Failure)
                assertSame(photo, NovaPendingRegistrationPhoto.snapshot().uri)
                assertNull(store.load())
            } finally { NovaPendingRegistrationPhoto.clear() }
        }
    }

    @Test(timeout = 20_000) fun supersededRegistrationCannotConsumeANewerFlowsPhoto() = runBlocking {
        withRealSessionStore { store, context ->
            NovaPendingRegistrationPhoto.set(Mockito.mock(Uri::class.java))
            val entered = CountDownLatch(1)
            val release = CountDownLatch(1)
            val remote = Mockito.mock(AuthRemoteDataSource::class.java)
            Mockito.`when`(remote.register("a@example.test", "pw", "user1", "User 1"))
                .thenAnswer { entered.countDown(); check(release.await(10, TimeUnit.SECONDS)); ApiResult.Success(session(1, "access-A", "refresh-A")) }
            val pending = async(Dispatchers.IO) { NovaAuthRepository(context, remote, activatePush = {}).register("a@example.test", "pw", "user1", "User 1") }
            try {
                check(entered.await(10, TimeUnit.SECONDS))
                val newPhoto = Mockito.mock(Uri::class.java)
                NovaPendingRegistrationPhoto.set(newPhoto)
                store.save(store.beginLogin(), session(2, "access-B", "refresh-B"))
                val expected = store.load()
                release.countDown()
                assertEquals(409, (pending.await() as ApiResult.Failure).statusCode)
                assertEquals(expected, store.load())
                assertSame(newPhoto, NovaPendingRegistrationPhoto.snapshot().uri)
            } finally { release.countDown(); NovaPendingRegistrationPhoto.clear() }
        }
    }

    @Test(timeout = 20_000) fun expiryCleanupCannotRunAgainstANewerStoredLogin() {
        withRealSessionStore { store, context ->
            store.save(store.beginLogin(), session(1, "access-A", "refresh-A"))
            val owner = store.identity()
            assertTrue(store.clear(owner))
            val signedOut = store.identity()
            var cleanups = 0
            assertTrue(store.ifSignedOut { cleanups += 1 })
            assertEquals(signedOut, store.identity()) // Cleanup does not manufacture another logout.
            NovaSessionStore(context).save(store.beginLogin(), session(2, "access-B", "refresh-B"))
            val expected = store.load()
            assertFalse(store.ifSignedOut { cleanups += 1 })
            assertEquals(1, cleanups)
            assertEquals(expected, store.load())
        }
    }

    @Test(timeout = 20_000) fun replacingOrClearingRegistrationPhotoInvalidatesOldConsumption() {
        val photo = Mockito.mock(Uri::class.java)
        try {
            NovaPendingRegistrationPhoto.set(photo)
            val old = NovaPendingRegistrationPhoto.snapshot()
            NovaPendingRegistrationPhoto.set(photo) // Same URI selected in a new flow is still new intent.
            assertNull(NovaPendingRegistrationPhoto.consume(old))
            assertSame(photo, NovaPendingRegistrationPhoto.snapshot().uri)
            val current = NovaPendingRegistrationPhoto.snapshot()
            NovaPendingRegistrationPhoto.clear()
            assertNull(NovaPendingRegistrationPhoto.consume(current))
        } finally { NovaPendingRegistrationPhoto.clear() }
    }

    private fun session(id: Long, access: String, refresh: String) = AuthSession(
        access, refresh, NovaUser(id, "user$id@example.test", "user$id", "User $id", ""),
    )

}
