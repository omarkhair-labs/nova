package com.nova.app.core.auth

import android.content.Context
import com.nova.app.core.network.AuthSession
import com.nova.app.core.network.NovaUser


class NovaSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: AuthSession) {
        prefs.edit()
            .putString(KEY_ACCESS, session.accessToken)
            .putString(KEY_REFRESH, session.refreshToken)
            .putLong(KEY_USER_ID, session.user.id)
            .putString(KEY_EMAIL, session.user.email)
            .putString(KEY_USERNAME, session.user.username)
            .putString(KEY_NAME, session.user.name)
            .putString(KEY_AVATAR_URL, session.user.avatarUrl)
            .apply()
    }

    fun updateAccessToken(accessToken: String) {
        prefs.edit()
            .putString(KEY_ACCESS, accessToken)
            .apply()
    }

    fun updateUser(user: NovaUser) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_NAME, user.name)
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .apply()
    }

    fun load(): StoredSession? {
        val access = prefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() } ?: return null
        val refresh = prefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() } ?: return null

        return StoredSession(
            accessToken = access,
            refreshToken = refresh,
            cachedUser = cachedUser(),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun cachedUser(): NovaUser? {
        val email = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() } ?: return null
        val username = prefs.getString(KEY_USERNAME, null)?.takeIf { it.isNotBlank() } ?: return null

        return NovaUser(
            id = prefs.getLong(KEY_USER_ID, 0L),
            email = email,
            username = username,
            name = prefs.getString(KEY_NAME, "").orEmpty(),
            avatarUrl = prefs.getString(KEY_AVATAR_URL, "").orEmpty(),
        )
    }

    data class StoredSession(
        val accessToken: String,
        val refreshToken: String,
        val cachedUser: NovaUser?,
    )

    private companion object {
        const val PREFS_NAME = "nova_session"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_USERNAME = "username"
        const val KEY_NAME = "name"
        const val KEY_AVATAR_URL = "avatar_url"
    }
}
