package com.nova.app.feature.plants.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Short-lived AES-GCM intent, excluded from backup; never SavedState, URL, log or analytics. */
internal class PlantupiaHandoverStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "plantupia-handover"))

    fun read(account: Long?): PendingPlantupiaHandover? = synchronized(lock) {
        try {
            if (!file.baseFile.exists()) return@synchronized null
            val bytes = file.readFully()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
            val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(12, bytes.size)), Charsets.UTF_8))
            val pending = PendingPlantupiaHandover(json.getString("token"),
                if (json.isNull("owner")) null else json.getLong("owner"), json.getLong("created"))
            if (deliveryCode(pending.token) == null || !canResumeHandover(pending, pending.owner, System.currentTimeMillis())) {
                file.delete()
                null
            } else pending.takeIf { canResumeHandover(it, account, System.currentTimeMillis()) }
        } catch (_: Exception) {
            file.delete() // Corrupt/unavailable key fails closed; ask for the original code.
            null
        }
    }

    fun save(token: String, account: Long?): Boolean = synchronized(lock) {
        if (deliveryCode(token) == null) return@synchronized false
        try {
            val previous = read(account)?.takeIf { it.token == token }
            val json = JSONObject().put("token", token).put("owner", account ?: JSONObject.NULL)
                .put("created", previous?.createdAt ?: System.currentTimeMillis())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.iv + cipher.doFinal(json.toString().toByteArray(Charsets.UTF_8))
            val stream = file.startWrite()
            try { stream.write(encrypted); file.finishWrite(stream) }
            catch (failure: Exception) { file.failWrite(stream); throw failure }
            true
        } catch (_: Exception) { false }
    }

    fun clear() = synchronized(lock) { file.delete() }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("plantupia_handover_key", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("plantupia_handover_key", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    companion object { private val lock = Any() }
}
