package com.nova.app.core.auth


/** Terminal API authentication failure currently surfaced to route owners. */
internal fun shouldExpireNovaSession(statusCode: Int?): Boolean = statusCode == 401
