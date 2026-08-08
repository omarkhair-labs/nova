package com.nova.app.core.feed

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.NovaPost
import com.nova.app.core.network.UploadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NovaFeedRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient(),
) {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)

    suspend fun feed(): ApiResult<List<NovaPost>> {
        return authenticatedCall { accessToken ->
            api.feed(accessToken)
        }
    }

    suspend fun createPost(
        caption: String,
        imageUri: Uri,
    ): ApiResult<NovaPost> {
        val image = when (val prepared = prepareImage(imageUri)) {
            is ApiResult.Success -> prepared.value
            is ApiResult.Failure -> return prepared
        }

        return authenticatedCall { accessToken ->
            api.createPost(
                accessToken = accessToken,
                caption = caption.trim(),
                image = image,
            )
        }
    }

    suspend fun deletePost(postId: Long): ApiResult<Unit> {
        return authenticatedCall { accessToken ->
            api.deletePost(accessToken, postId)
        }
    }

    private suspend fun prepareImage(uri: Uri): ApiResult<UploadFile> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val resolver = appContext.contentResolver
                val mimeType = resolver.getType(uri)?.takeIf { it.startsWith("image/") }
                    ?: "image/jpeg"
                val extension = MimeTypeMap.getSingleton()
                    .getExtensionFromMimeType(mimeType)
                    ?.takeIf { it.isNotBlank() }
                    ?: "jpg"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Couldn't read that photo.")

                if (bytes.size > 10 * 1024 * 1024) {
                    return@withContext ApiResult.Failure("Post photo must be 10 MB or smaller.")
                }

                ApiResult.Success(
                    UploadFile(
                        bytes = bytes,
                        fileName = "nova-post-${System.currentTimeMillis()}.$extension",
                        mimeType = mimeType,
                    ),
                )
            }.getOrElse {
                ApiResult.Failure("Nova couldn't read that photo. Pick another image and try again.")
            }
        }
    }

    private suspend fun <T> authenticatedCall(
        call: suspend (String) -> ApiResult<T>,
    ): ApiResult<T> {
        val stored = sessionStore.load()
            ?: return ApiResult.Failure("Your session expired. Please log in again.", 401)

        when (val first = call(stored.accessToken)) {
            is ApiResult.Success -> return first
            is ApiResult.Failure -> {
                if (first.statusCode != 401) return first
            }
        }

        return when (val refreshed = api.refresh(stored.refreshToken)) {
            is ApiResult.Success -> {
                sessionStore.updateAccessToken(refreshed.value)
                when (val retried = call(refreshed.value)) {
                    is ApiResult.Success -> retried
                    is ApiResult.Failure -> {
                        if (retried.statusCode == 401) sessionStore.clear()
                        retried
                    }
                }
            }

            is ApiResult.Failure -> {
                if (refreshed.statusCode == 400 || refreshed.statusCode == 401) {
                    sessionStore.clear()
                    ApiResult.Failure("Your session expired. Please log in again.", 401)
                } else {
                    refreshed
                }
            }
        }
    }
}
