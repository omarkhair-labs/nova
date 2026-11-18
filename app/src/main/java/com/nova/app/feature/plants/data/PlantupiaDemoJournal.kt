package com.nova.app.feature.plants.data

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.json.JSONObject

/** Account-bound, app-private immutable retry intents. Never automatic submissions. */
internal object PlantupiaDemoJournal {
    private fun directory(context: Context, account: Long, origin: String): File {
        val server = MessageDigest.getInstance("SHA-256").digest(origin.toByteArray()).joinToString("") { "%02x".format(it) }
        return File(context.noBackupFilesDir, "plantupia-demo-qa/$server/$account").apply { mkdirs() }
    }

    private fun file(context: Context, account: Long, origin: String, slot: String): AtomicFile {
        val key = MessageDigest.getInstance("SHA-256").digest(slot.toByteArray()).joinToString("") { "%02x".format(it) }
        return AtomicFile(File(directory(context, account, origin), "$key.json"))
    }

    @Synchronized fun prepare(context: Context, account: Long, origin: String, slot: String, path: String, body: JSONObject): JSONObject {
        val target = file(context, account, origin, slot)
        if (target.baseFile.exists()) {
            val old = JSONObject(target.readFully().toString(Charsets.UTF_8))
            check(old.getString("path") == path && old.getString("body") == body.toString()) {
                "A previous QA action needs confirmation. Retry it from My Space before changing this action."
            }
            return old
        }
        val value = JSONObject().put("slot", slot).put("path", path).put("body", body.toString())
            .put("client_id", UUID.randomUUID().toString())
        val stream = target.startWrite()
        try {
            stream.write(value.toString().toByteArray())
            target.finishWrite(stream)
        } catch (error: Exception) {
            target.failWrite(stream)
            throw error
        }
        return value
    }

    @Synchronized fun pending(context: Context, account: Long, origin: String): List<JSONObject> =
        directory(context, account, origin).listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull {
            runCatching { JSONObject(AtomicFile(it).readFully().toString(Charsets.UTF_8)) }.getOrNull()
        }

    @Synchronized fun clear(context: Context, account: Long, origin: String, intent: JSONObject) {
        val target = file(context, account, origin, intent.getString("slot"))
        if (target.baseFile.exists() && JSONObject(target.readFully().toString(Charsets.UTF_8)).getString("client_id") == intent.getString("client_id")) target.delete()
    }
}
