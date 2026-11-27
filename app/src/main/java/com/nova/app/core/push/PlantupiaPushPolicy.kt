package com.nova.app.core.push

/** A queued private alert belongs to its original account, never the next login. */
internal fun authorizedPlantupiaPushRecipient(recipientId: Long?, currentAccountId: Long?): Boolean =
    recipientId != null && recipientId > 0 && recipientId == currentAccountId
