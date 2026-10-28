package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab

internal object PlantupiaShopOpenSignal {
    var target by mutableStateOf<Pair<Long, String>?>(null)
        private set
    fun open(accountId: Long, orderId: String) {
        if (accountId <= 0 || runCatching { java.util.UUID.fromString(orderId) }.isFailure) return
        target = accountId to orderId
        NovaRootNavigationSignal.request(NovaRootTab.Shop)
    }
    fun consume() { target = null }
}
