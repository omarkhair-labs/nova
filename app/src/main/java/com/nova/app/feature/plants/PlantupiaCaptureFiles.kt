package com.nova.app.feature.plants

import android.net.Uri
import java.io.File

/** File-URI support is restricted to this application's own temporary camera directory. */
internal fun ownedCaptureFile(cacheDir: File, uri: Uri): File? =
    if (uri.scheme != "file" || !uri.authority.isNullOrEmpty()) null
    else runCatching {
        val root = File(cacheDir, "plantupia-captures").canonicalFile
        val path = uri.path ?: return null
        val file = File(path).canonicalFile
        file.takeIf { it.parentFile == root && it.name.startsWith("care-") && it.extension == "jpg" }
    }.getOrNull()
