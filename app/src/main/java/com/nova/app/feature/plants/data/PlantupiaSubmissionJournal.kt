package com.nova.app.feature.plants.data

import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** App-private, no-backup snapshots; no background sending and no persisted public consent UI. */
internal data class PlantupiaSubmission(
    val id: String,
    val account: Long,
    val plant: String,
    val slot: String,
    val fields: Map<String, String>,
    val photo: File?,
    val mime: String?,
    val reminder: Boolean,
    val acknowledgedResult: String? = null,
) {
    val operation: String get() = if (slot == "question") "question" else "event"
}

internal class PlantupiaSubmissionJournal(private val root: File, private val environment: String) {
    fun prepare(
        account: Long, plant: String, slot: String, fields: Map<String, String>,
        photo: ByteArray? = null, mime: String? = null, reminder: Boolean = false,
    ): PlantupiaSubmission = synchronized(lock) {
        require(account > 0 && photo?.size?.let { it <= 10 * 1024 * 1024 } != false)
        val existing = load(account, plant, slot)
        if (existing != null) {
            check(existing.fields.filterKeys { it != "observed_at" } == fields && existing.mime == mime &&
                existing.photo?.let { digest(it.readBytes()) } == photo?.let(::digest)) {
                "An earlier save is unconfirmed. Retry the original saved submission before starting a different one."
            }
            // Notification opt-in is local, independent of the immutable server save.
            // Honor a changed choice without changing its identity or observation time.
            return existing.copy(reminder = reminder).also { if (it != existing) write(it) }
        }
        val directory = directory(account).apply { check(mkdirs() || isDirectory) }
        val file = photo?.let {
            val suffix = when (mime) { "image/png" -> "png"; "image/webp" -> "webp"; "image/jpeg" -> "jpg"; else -> error("Unsupported photo") }
            File(directory, "${digest(it)}.$suffix").also { target -> if (!target.exists()) atomicWrite(target, it) }
        }
        val frozen = if (slot == "question") fields else fields + ("observed_at" to Instant.now().toString())
        val submission = PlantupiaSubmission(UUID.randomUUID().toString(), account, plant, slot, frozen, file, mime, reminder)
        write(submission)
        submission
    }

    fun load(account: Long, plant: String, slot: String): PlantupiaSubmission? = synchronized(lock) {
        val path = manifest(account, plant, slot)
        if (!path.exists()) return null
        // A corrupt/unreadable snapshot must never silently generate another public identity.
        val json = JSONObject(path.readText())
        check(json.getString("environment") == environment && json.getLong("account") == account &&
            json.getString("plant") == plant && json.getString("slot") == slot)
        val id = json.getString("id").also { UUID.fromString(it) }
        val fields = json.getJSONObject("fields").let { obj -> obj.keys().asSequence().associateWith { obj.getString(it) } }
        val photo = json.optString("photo").takeIf { it.isNotEmpty() }?.let {
            check(it.matches(Regex("[a-f0-9]{64}\\.(png|jpg|webp)")))
            File(directory(account), it).also { file -> check(file.isFile && digest(file.readBytes()) == it.substringBefore('.')) }
        }
        PlantupiaSubmission(id, account, plant, slot, fields, photo,
            json.optString("mime").takeIf(String::isNotEmpty), json.getBoolean("reminder"),
            json.optString("result").takeIf(String::isNotEmpty))
    }

    fun pending(account: Long, plant: String): List<PlantupiaSubmission> = synchronized(lock) {
        directory(account).listFiles().orEmpty().filter { it.extension == "json" }.mapNotNull {
            val obj = JSONObject(it.readText())
            if (obj.getString("environment") != environment || obj.getString("plant") != plant) null
            else load(account, plant, obj.getString("slot"))
        }
    }

    fun acknowledge(submission: PlantupiaSubmission, result: String) = synchronized(lock) {
        val current = load(submission.account, submission.plant, submission.slot)
        check(current?.id == submission.id)
        write(requireNotNull(current).copy(acknowledgedResult = result))
    }

    fun confirm(account: Long, plant: String, slot: String, result: String): Boolean = synchronized(lock) {
        val current = load(account, plant, slot) ?: return false
        if (current.acknowledgedResult != result) return false
        remove(current)
        true
    }

    /** A definitive validation rejection of the new protocol never committed this snapshot. */
    fun reject(submission: PlantupiaSubmission) = synchronized(lock) {
        if (load(submission.account, submission.plant, submission.slot)?.id == submission.id) remove(submission)
    }

    private fun remove(submission: PlantupiaSubmission) {
        check(manifest(submission.account, submission.plant, submission.slot).delete())
        val photo = submission.photo ?: return
        val referenced = directory(submission.account).listFiles().orEmpty().filter { it.extension == "json" }.any {
            // Unreadable manifests retain their media until they can be recovered.
            runCatching { JSONObject(it.readText()).optString("photo") == photo.name }.getOrDefault(true)
        }
        if (!referenced) photo.delete()
    }

    private fun write(submission: PlantupiaSubmission) {
        val json = JSONObject().put("environment", environment).put("account", submission.account)
            .put("plant", submission.plant).put("slot", submission.slot).put("id", submission.id)
            .put("fields", JSONObject(submission.fields)).put("photo", submission.photo?.name.orEmpty())
            .put("mime", submission.mime.orEmpty()).put("reminder", submission.reminder)
            .put("result", submission.acknowledgedResult.orEmpty())
        atomicWrite(manifest(submission.account, submission.plant, submission.slot), json.toString().toByteArray())
    }

    private fun directory(account: Long) = File(root, "u$account")
    private fun manifest(account: Long, plant: String, slot: String) =
        File(directory(account), "${digest("$environment\u0000$plant\u0000$slot".toByteArray())}.json")

    private companion object {
        val lock = Any()
        fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        fun atomicWrite(target: File, bytes: ByteArray) {
            val temp = File.createTempFile("submission-", ".tmp", target.parentFile)
            try {
                FileOutputStream(temp).use { it.write(bytes); it.fd.sync() }
                Files.move(temp.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } finally { temp.delete() }
        }
    }
}
