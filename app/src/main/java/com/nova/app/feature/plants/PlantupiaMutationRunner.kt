package com.nova.app.feature.plants

import com.nova.app.core.network.ApiResult

/** Server work may finish after Back. Only same-account side effects survive that navigation. */
internal suspend fun <T> runPlantupiaMutation(
    isAccountCurrent: () -> Boolean,
    isDetailCurrent: () -> Boolean,
    request: suspend () -> ApiResult<T>,
    onStarted: () -> Unit,
    onCommitted: (T) -> Unit = {},
    onSuccess: suspend (T) -> Unit,
    onFailure: (ApiResult.Failure) -> Unit,
    onFinished: () -> Unit,
) {
    if (!isAccountCurrent() || !isDetailCurrent()) return
    onStarted()
    try {
        when (val result = request()) {
            is ApiResult.Success -> {
                if (!isAccountCurrent()) return
                onCommitted(result.value)
                if (isDetailCurrent()) onSuccess(result.value)
            }
            is ApiResult.Failure -> {
                if (isAccountCurrent() && isDetailCurrent()) onFailure(result)
            }
        }
    } finally {
        // A stale completion must not clear a newer plant's busy state.
        if (isAccountCurrent() && isDetailCurrent()) onFinished()
    }
}

internal class PlantupiaCallbackLifetime(private val accountCurrent: () -> Boolean) {
    private var active = true
    fun isCurrent(): Boolean = active && accountCurrent()
    fun dispose() { active = false }
}

/** Reload records without reopening/resetting an unrelated care composer. */
internal suspend fun <T> reloadPlantupiaMutationRecords(
    isCurrent: () -> Boolean,
    refreshInventory: suspend () -> Unit,
    loadHistory: suspend () -> T,
    applyHistory: (T) -> Unit,
    loadPending: suspend () -> Unit,
    onFinished: () -> Unit,
) {
    if (!isCurrent()) return
    try {
        refreshInventory()
        if (!isCurrent()) return
        val history = loadHistory()
        if (!isCurrent()) return
        applyHistory(history)
        loadPending()
    } finally {
        if (isCurrent()) onFinished()
    }
}
