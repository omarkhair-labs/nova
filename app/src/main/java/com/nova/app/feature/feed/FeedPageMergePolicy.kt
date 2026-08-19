package com.nova.app.feature.feed

import com.nova.app.core.network.NovaPost


fun mergeFeedPage(
    existing: List<NovaPost>,
    incoming: List<NovaPost>,
): List<NovaPost> {
    val existingIds = existing.mapTo(mutableSetOf()) { it.id }
    return existing + incoming.filterNot { it.id in existingIds }
}
