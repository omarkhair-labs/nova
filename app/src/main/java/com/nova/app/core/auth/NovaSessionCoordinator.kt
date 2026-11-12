package com.nova.app.core.auth

import com.nova.app.core.network.ApiResult
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/** One process-wide owner, shared by every repository/store instance. No network I/O under lock. */
internal object NovaSessionCoordinator {
    val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private data class Delivery(val result: ApiResult<String>, val identity: NovaSessionStore.LoginIdentity)
    private class Flight(val result: CompletableDeferred<Delivery> = CompletableDeferred()) {
        var job: Job? = null
    }
    private val flights = mutableMapOf<NovaSessionStore.LoginIdentity, Flight>()

    /** Caller holds [lock]. Wake old waiters immediately; a late transport cannot commit. */
    fun invalidate(owner: NovaSessionStore.LoginIdentity) {
        flights.remove(owner)?.let {
            it.result.complete(Delivery(changedSession(), owner))
            it.job?.cancel()
        }
    }

    suspend fun refresh(
        store: NovaSessionStore,
        stored: NovaSessionStore.StoredSession,
        request: suspend () -> ApiResult<String>,
    ): ApiResult<String> {
        currentCoroutineContext().ensureActive()
        val owner = stored.identity
        val flight = synchronized(lock) {
            val current = store.current(owner) ?: return changedSession()
            // Another caller already refreshed the access token rejected by this request.
            if (current.accessToken != stored.accessToken) return ApiResult.Success(current.accessToken)
            flights[owner] ?: Flight().also { created ->
                flights[owner] = created
                created.job = scope.launch(start = CoroutineStart.LAZY) {
                    try {
                        val response = request()
                        currentCoroutineContext().ensureActive()
                        synchronized(lock) {
                            if (flights[owner] !== created) return@synchronized
                            // Remove before an authentication rejection invalidates this login.
                            flights.remove(owner)
                            val result = when {
                                store.current(owner) == null -> changedSession()
                                response is ApiResult.Success -> {
                                    if (store.updateAccessToken(owner, response.value)) response
                                    else ApiResult.Failure("Nova couldn't save the refreshed session. Please retry.")
                                }
                                response is ApiResult.Failure && response.statusCode in listOf(400, 401) -> {
                                    store.clear(owner)
                                    expiredSession()
                                }
                                else -> response
                            }
                            created.result.complete(Delivery(result, store.identity()))
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Return the bounded failure below; never crash the shared worker scope.
                    } finally {
                        synchronized(lock) {
                            if (flights[owner] === created) flights.remove(owner)
                            // Cancellation/throwing transports cannot strand other waiters.
                            created.result.complete(Delivery(
                                ApiResult.Failure("Nova couldn't refresh this session. Please retry."), owner,
                            ))
                        }
                    }
                }
            }
        }
        flight.job?.start()
        val delivery = flight.result.await()
        currentCoroutineContext().ensureActive()
        return if (store.isCurrent(delivery.identity)) delivery.result else changedSession()
    }
}

internal fun changedSession() = ApiResult.Failure("Your session changed. Please try again.", 409)
internal fun expiredSession() = ApiResult.Failure("Your session expired. Please log in again.", 401)

internal suspend fun NovaSessionStore.refreshAccessToken(
    stored: NovaSessionStore.StoredSession,
    refresh: suspend () -> ApiResult<String>,
): ApiResult<String> = NovaSessionCoordinator.refresh(this, stored, refresh)

/** Origin is immutable across initial request, refresh and retry, including successful replies. */
internal suspend fun <T> NovaSessionStore.authenticatedRequest(
    stored: NovaSessionStore.StoredSession,
    refresh: suspend () -> ApiResult<String>,
    request: suspend (String) -> ApiResult<T>,
): ApiResult<T> {
    currentCoroutineContext().ensureActive()
    val current = current(stored.identity) ?: return changedSession()
    val first = request(current.accessToken)
    currentCoroutineContext().ensureActive()
    if (!isCurrent(stored.identity)) return changedSession()
    if (first !is ApiResult.Failure || first.statusCode != 401) return first
    return when (val token = refreshAccessToken(current, refresh)) {
        is ApiResult.Failure -> token
        is ApiResult.Success -> {
            if (!isCurrent(stored.identity)) return changedSession()
            val retried = request(token.value)
            currentCoroutineContext().ensureActive()
            synchronized(NovaSessionCoordinator.lock) {
                val latest = current(stored.identity) ?: return changedSession()
                if (retried is ApiResult.Failure && retried.statusCode == 401 && latest.accessToken == token.value) {
                    clear(stored.identity)
                }
                retried
            }
        }
    }
}

/** Realtime keeps the current token on a transient refresh failure, never another login's token. */
internal suspend fun NovaSessionStore.realtimeToken(
    stored: NovaSessionStore.StoredSession,
    refresh: suspend () -> ApiResult<String>,
): ApiResult<String> = when (val result = refreshAccessToken(stored, refresh)) {
    is ApiResult.Success -> result
    is ApiResult.Failure -> {
        if (result.statusCode == 401 || result.statusCode == 409) result
        else current(stored.identity)?.let { ApiResult.Success(it.accessToken) } ?: changedSession()
    }
}
