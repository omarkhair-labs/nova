package com.nova.app.core.auth

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.nova.app.core.presence.NovaAppPresence
import com.nova.app.feature.auth.domain.model.AuthSession
import com.nova.app.feature.auth.domain.model.NovaUser
import java.security.KeyStore
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec


class NovaSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Persisted, non-secret login identity; access-token rotation never changes it. */
    @ConsistentCopyVisibility
    data class LoginIdentity internal constructor(val generation: String)

    fun identity(): LoginIdentity = synchronized(NovaSessionCoordinator.lock) { identityLocked() }

    fun isCurrent(owner: LoginIdentity): Boolean = synchronized(NovaSessionCoordinator.lock) {
        identityLocked() == owner
    }

    fun current(owner: LoginIdentity): StoredSession? = synchronized(NovaSessionCoordinator.lock) {
        if (identityLocked() == owner) loadLocked() else null
    }

    /** Explicit user intent starts a new login, including another login by the same user. */
    fun beginLogin(): LoginIdentity = logout()

    /** Only explicit user logout/login intent may invalidate without an originating owner. */
    fun logout(): LoginIdentity {
        return logoutSession().second
    }

    fun logoutSession(): Pair<StoredSession?, LoginIdentity> {
        val result = synchronized(NovaSessionCoordinator.lock) { loadLocked() to invalidateLocked() }
        NovaAppPresence.sessionChanged()
        return result
    }

    fun clear(owner: LoginIdentity): Boolean {
        val cleared = synchronized(NovaSessionCoordinator.lock) {
            if (identityLocked() != owner) false else { invalidateLocked(); true }
        }
        if (cleared) NovaAppPresence.sessionChanged()
        return cleared
    }

    /** Login response and security credential replacement use one atomic preference edit. */
    fun save(owner: LoginIdentity, session: AuthSession): Boolean = synchronized(NovaSessionCoordinator.lock) {
        if (identityLocked() != owner) return false
        if (session.accessToken.isBlank() || session.refreshToken.isBlank() || session.user.id <= 0L) return false
        val existingId = cachedUser()?.id
        if (existingId != null && existingId != session.user.id) return false
        val encrypted = encryptTokens(session.accessToken, session.refreshToken) ?: return false
        putUser(prefs.edit(), session.user)
            .putString(KEY_TOKEN_IV, encrypted.first)
            .putString(KEY_TOKEN_CIPHERTEXT, encrypted.second)
            .putString(KEY_GENERATION, owner.generation)
            .apply()
        NovaAppPresence.sessionChanged()
        true
    }

    /** Security endpoints replace both credentials and invalidate pre-replacement requests. */
    fun replaceSession(owner: LoginIdentity, session: AuthSession): Boolean = synchronized(NovaSessionCoordinator.lock) {
        val current = current(owner) ?: return false
        if (current.cachedUser?.id != session.user.id) return false
        val encrypted = encryptTokens(session.accessToken, session.refreshToken) ?: return false
        if (session.accessToken.isBlank() || session.refreshToken.isBlank()) return false
        val next = LoginIdentity(UUID.randomUUID().toString())
        putUser(prefs.edit(), session.user)
            .putString(KEY_TOKEN_IV, encrypted.first)
            .putString(KEY_TOKEN_CIPHERTEXT, encrypted.second)
            .putString(KEY_GENERATION, next.generation)
            .apply()
        NovaSessionCoordinator.invalidate(owner)
        NovaAppPresence.sessionChanged()
        true
    }

    fun updateAccessToken(owner: LoginIdentity, accessToken: String): Boolean = synchronized(NovaSessionCoordinator.lock) {
        if (identityLocked() != owner || accessToken.isBlank()) return false
        val current = readTokens() ?: return false
        val encrypted = encryptTokens(accessToken, current.second) ?: return false
        prefs.edit().putString(KEY_TOKEN_IV, encrypted.first)
            .putString(KEY_TOKEN_CIPHERTEXT, encrypted.second).apply()
        true
    }

    fun updateUser(owner: LoginIdentity, user: NovaUser): Boolean = synchronized(NovaSessionCoordinator.lock) {
        val current = current(owner) ?: return false
        if (user.id <= 0L || (current.cachedUser != null && current.cachedUser.id != user.id)) return false
        putUser(prefs.edit(), user).apply()
        true
    }

    private fun putUser(editor: SharedPreferences.Editor, user: NovaUser): SharedPreferences.Editor =
        editor
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
            .putInt(KEY_PLANTUPIA_AVATAR_VARIANT, user.plantupiaAvatarVariant)
            .putBoolean(KEY_SHOW_ORBIT, user.showOrbit)
            .putBoolean(KEY_IS_VERIFIED, user.isVerified)
            .putInt(KEY_FOLLOWERS_COUNT, user.followersCount)
            .putInt(KEY_FOLLOWING_COUNT, user.followingCount)
            .putInt(KEY_POSTS_COUNT, user.postsCount)

    fun load(): StoredSession? = synchronized(NovaSessionCoordinator.lock) { loadLocked() }

    private fun loadLocked(): StoredSession? {
        val tokens = readTokens() ?: return null
        return StoredSession(tokens.first, tokens.second, cachedUser(), identityLocked())
    }

    private fun identityLocked(): LoginIdentity {
        val existing = prefs.getString(KEY_GENERATION, null)
        if (!existing.isNullOrBlank()) return LoginIdentity(existing)
        // Upgrade legacy encrypted sessions in place; never create an identity from user ID.
        val owner = LoginIdentity(UUID.randomUUID().toString())
        prefs.edit().putString(KEY_GENERATION, owner.generation).apply()
        return owner
    }

    private fun invalidateLocked(): LoginIdentity {
        val previous = identityLocked()
        val next = LoginIdentity(UUID.randomUUID().toString())
        prefs.edit().clear().putString(KEY_GENERATION, next.generation).apply()
        NovaSessionCoordinator.invalidate(previous)
        return next
    }

    private fun encryptTokens(accessToken: String, refreshToken: String): Pair<String, String>? = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal("$accessToken$TOKEN_SEPARATOR$refreshToken".toByteArray(Charsets.UTF_8))
        Base64.getEncoder().encodeToString(cipher.iv) to Base64.getEncoder().encodeToString(encrypted)
    }.getOrNull()

    private fun readTokens(): Pair<String, String>? {
        val ivText = prefs.getString(KEY_TOKEN_IV, null) ?: return null
        val cipherText = prefs.getString(KEY_TOKEN_CIPHERTEXT, null) ?: return null

        return runCatching {
            val iv = Base64.getDecoder().decode(ivText)
            val encrypted = Base64.getDecoder().decode(cipherText)

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
            invalidateLocked()
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
            plantupiaAvatarVariant = prefs.getInt(KEY_PLANTUPIA_AVATAR_VARIANT, -1),
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
        val identity: LoginIdentity,
    )

    private companion object {
        const val KEY_GENERATION = "login_generation"
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
        const val KEY_PLANTUPIA_AVATAR_VARIANT = "plantupia_avatar_variant"
        const val KEY_SHOW_ORBIT = "show_orbit"
        const val KEY_IS_VERIFIED = "is_verified"
        const val KEY_FOLLOWERS_COUNT = "followers_count"
        const val KEY_FOLLOWING_COUNT = "following_count"
        const val KEY_POSTS_COUNT = "posts_count"
    }
}
