package com.nova.app.core.messaging

import com.nova.app.feature.messages.group.model.GroupDetail
import com.nova.app.feature.messages.group.model.GroupMember
import com.nova.app.feature.messages.group.model.ManagedGroupDetail


@Deprecated("Use GroupMember from feature/messages/group/model.")
typealias NovaGroupMember = GroupMember

@Deprecated("Use GroupDetail from feature/messages/group/model.")
typealias NovaGroupDetail = GroupDetail

@Deprecated("Use ManagedGroupDetail from feature/messages/group/model.")
typealias NovaManagedGroupDetail = ManagedGroupDetail
