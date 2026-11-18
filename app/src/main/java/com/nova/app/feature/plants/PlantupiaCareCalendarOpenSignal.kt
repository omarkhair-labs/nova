package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab

/**
 * Account-scoped navigation intent. Today and My Plants share one real calendar
 * destination; no duplicate Today calendar, invented events, or forced popup.
 */
internal object PlantupiaCareCalendarOpenSignal {
    var targetAccountId by mutableStateOf<Long?>(null)
        private set
    var version by mutableIntStateOf(0)
        private set
    // Origin matters for Back: Today should return to Today; opening a plant
    // from Schedule still deliberately navigates into My Plants detail.
    var returnToToday by mutableStateOf(false)
        private set

    fun open(accountId: Long, fromToday: Boolean = false) {
        if (accountId <= 0) return
        targetAccountId = accountId
        returnToToday = fromToday
        version += 1
        NovaRootNavigationSignal.request(NovaRootTab.Plants)
    }

    fun consume(accountId: Long) {
        if (targetAccountId == accountId) targetAccountId = null
    }
}
