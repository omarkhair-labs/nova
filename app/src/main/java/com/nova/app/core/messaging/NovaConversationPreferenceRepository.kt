package com.nova.app.core.messaging

import com.nova.app.feature.messages.appearance.data.remote.ConversationAppearanceRemoteRepository
import com.nova.app.feature.messages.appearance.model.ConversationPreference


@Deprecated("Use ConversationAppearanceRepository through AppContainer.")
typealias NovaConversationPreferenceRepository = ConversationAppearanceRemoteRepository

@Deprecated("Use ConversationPreference.")
typealias NovaConversationPreference = ConversationPreference
