package com.nova.app.core.messaging

import com.nova.app.feature.messages.group.data.remote.GroupMembershipRemoteRepository


@Deprecated("Use GroupMembershipRepository through AppContainer.")
typealias NovaGroupMessagingRepository = GroupMembershipRemoteRepository
