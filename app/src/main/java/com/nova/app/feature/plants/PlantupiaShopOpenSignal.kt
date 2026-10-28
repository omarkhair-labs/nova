package com.nova.app.feature.plants

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nova.app.navigation.NovaRootNavigationSignal
import com.nova.app.navigation.NovaRootTab

internal object PlantupiaShopOpenSignal {
    var target by mutableStateOf<Pair<Long, String>?>(null)
        private set
    var ordersAccount by mutableStateOf<Long?>(null)
        private set
    fun open(accountId: Long, orderId: String) {
        if (accountId <= 0 || runCatching { java.util.UUID.fromString(orderId) }.isFailure) return
        target = accountId to orderId
        NovaRootNavigationSignal.request(NovaRootTab.Shop)
    }
    fun openOrders(accountId: Long) {
        if (accountId <= 0) return
        ordersAccount = accountId
        NovaRootNavigationSignal.request(NovaRootTab.Shop)
    }
    fun consumeOrders() { ordersAccount = null }
    fun consume() { target = null }
}
