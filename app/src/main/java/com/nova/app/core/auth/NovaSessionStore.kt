package com.nova.app.core.auth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.nova.app.core.presence.NovaAppPresence
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class NovaSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(session: AuthSession) {
        if (!storeTokens(session.accessToken, session.refreshToken)) {
            clear()
            return
        }

        updateUser(session.user)
        NovaAppPresence.sessionChanged()
    }

    fun updateAccessToken(accessToken: String) {
        val current = readTokens() ?: return
        storeTokens(accessToken, current.second)
    }

    fun updateUser(user: NovaUser) {
        prefs.edit()
            .putLong(KEY_USER_ID, user.id)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_USERNAME, user.username)
            .putString(KEY_NAME, user.name)
            .putString(KEY_AVATAR_URL, user.avatarUrl)
            .putString(KEY_BIO, user.bio)
            .putString(KEY_LOCATION, user.location)
            .putString(KEY_LINK, user.link)
            .putStringSet(KEY_INTERESTS, user.interests.toSet())
            .putString(KEY_PROFILE_THEME, user.profileTheme)
            .putBoolean(KEY_SHOW_ORBIT, user.showOrbit)
            .putBoolean(KEY_IS_VERIFIED, user.isVerified)
            .putInt(KEY_FOLLOWERS_COUNT, user.followersCount)
            .putInt(KEY_FOLLOWING_COUNT, user.followingCount)
            .putInt(KEY_POSTS_COUNT, user.postsCount)
            .apply()
    }

    fun load(): StoredSession? {
        val (access, refresh) = readTokens() ?: return null

        return StoredSession(
            accessToken = access,
            refreshToken = refresh,
            cachedUser = cachedUser(),
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
        NovaAppPresence.sessionChanged()
    }

    private fun storeTokens(accessToken: String, refreshToken: String): Boolean {
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())

            val plainText = "$accessToken$TOKEN_SEPARATOR$refreshToken".toByteArray(Charsets.UTF_8)
            val encrypted = cipher.doFinal(plainText)

            prefs.edit()
                .putString(KEY_TOKEN_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
                .putString(KEY_TOKEN_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .apply()
            true
        }.getOrElse { false }
    }

    private fun readTokens(): Pair<String, String>? {
        val ivText = prefs.getString(KEY_TOKEN_IV, null) ?: return null
        val cipherText = prefs.getString(KEY_TOKEN_CIPHERTEXT, null) ?: return null

        return runCatching {
            val iv = Base64.decode(ivText, Base64.NO_WRAP)
            val encrypted = Base64.decode(cipherText, Base64.NO_WRAP)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv),
            )

            val plainText = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
            val parts = plainText.split(TOKEN_SEPARATOR, limit = 2)
            if (parts.size != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                null
            } else {
                parts[0] to parts[1]
            }
        }.getOrElse {
            prefs.edit()
                .remove(KEY_TOKEN_IV)
                .remove(KEY_TOKEN_CIPHERTEXT)
                .apply()
            null
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
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
            bio = prefs.getString(KEY_BIO, "").orEmpty(),
            location = prefs.getString(KEY_LOCATION, "").orEmpty(),
            link = prefs.getString(KEY_LINK, "").orEmpty(),
            interests = prefs.getStringSet(KEY_INTERESTS, emptySet()).orEmpty().sorted(),
            profileTheme = prefs.getString(KEY_PROFILE_THEME, "violet").orEmpty(),
            showOrbit = prefs.getBoolean(KEY_SHOW_ORBIT, true),
            isVerified = prefs.getBoolean(KEY_IS_VERIFIED, false),
            followersCount = prefs.getInt(KEY_FOLLOWERS_COUNT, 0),
            followingCount = prefs.getInt(KEY_FOLLOWING_COUNT, 0),
            postsCount = prefs.getInt(KEY_POSTS_COUNT, 0),
        )
    }

    data class StoredSession(
        val accessToken: String,
        val refreshToken: String,
        val cachedUser: NovaUser?,
    )

    private companion object {
        const val PREFS_NAME = "nova_session"
        const val KEY_ALIAS = "nova_session_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_LENGTH_BITS = 128
        const val TOKEN_SEPARATOR = "\u0000"

        const val KEY_TOKEN_IV = "token_iv"
        const val KEY_TOKEN_CIPHERTEXT = "token_ciphertext"
        const val KEY_USER_ID = "user_id"
        const val KEY_EMAIL = "email"
        const val KEY_USERNAME = "username"
        const val KEY_NAME = "name"
        const val KEY_AVATAR_URL = "avatar_url"
        const val KEY_BIO = "bio"
        const val KEY_LOCATION = "location"
        const val KEY_LINK = "link"
        const val KEY_INTERESTS = "interests"
        const val KEY_PROFILE_THEME = "profile_theme"
        const val KEY_SHOW_ORBIT = "show_orbit"
        const val KEY_IS_VERIFIED = "is_verified"
        const val KEY_FOLLOWERS_COUNT = "followers_count"
        const val KEY_FOLLOWING_COUNT = "following_count"
        const val KEY_POSTS_COUNT = "posts_count"
    }
}
