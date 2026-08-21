package com.nova.app.core.feed

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.nova.app.core.auth.NovaSessionStore
import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource
import com.nova.app.feature.feed.data.FeedRepository
import com.nova.app.feature.people.data.remote.PeopleRemoteDataSource
import com.nova.app.feature.posts.data.PostRepository
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostPage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class NovaFeedRepository(
    context: Context,
    private val api: NovaApiClient = NovaApiClient("https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/"),
) : FeedRepository, PostRepository {
    private val appContext = context.applicationContext
    private val sessionStore = NovaSessionStore(appContext)
    private val feedCache = NovaFeedCache(appContext)
    private val authRemote = AuthRemoteDataSource(api)
    private val peopleRemote = PeopleRemoteDataSource(api)

    override suspend fun feed(cursor: String?): ApiResult<NovaPostPage> {
        val cachedUserId = sessionStore.load()?.cachedUser?.id?.takeIf { it > 0L }
        val result = authenticatedCall { accessToken ->
            api.feed(accessToken, cursor)
        }

        if (cursor.isNullOrBlank()) {
            when (result) {
                is ApiResult.Success -> {
                    cachedUserId?.let { userId -> feedCache.save(userId, result.value) }
                }

                is ApiResult.Failure -> {
                    if (result.statusCode != 401) {
                        val cached = cachedUserId?.let(feedCache::load)
                        if (cached != null) return ApiResult.Success(cached)
                    }
                }
            }
        }

        return result
    }

    override fun cachedFeed(userId: Long): NovaPostPage? {
        return feedCache.load(userId)
    }

    override suspend fun personPosts(username: String): ApiResult<List<NovaPost>> {
        return authenticatedCall { accessToken ->
            peopleRemote.personPosts(accessToken, username)
        }
    }

    override suspend fun post(postId: Long): ApiResult<NovaPost> {
        return authenticatedCall { accessToken ->
            api.post(accessToken, postId)
        }
    }

    override suspend fun createPost(
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

    override suspend fun deletePost(postId: Long): ApiResult<Unit> {
        return authenticatedCall { accessToken ->
            api.deletePost(accessToken, postId)
        }
    }

    override suspend fun setLiked(
        postId: Long,
        liked: Boolean,
    ): ApiResult<NovaPost> {
        return authenticatedCall { accessToken ->
            api.setLiked(accessToken, postId, liked)
        }
    }

    override suspend fun comments(postId: Long): ApiResult<List<NovaComment>> {
        return authenticatedCall { accessToken ->
            api.comments(accessToken, postId)
        }
    }

    override suspend fun addComment(
        postId: Long,
        body: String,
        parentId: Long?,
    ): ApiResult<NovaCommentMutation> {
        return authenticatedCall { accessToken ->
            api.addComment(accessToken, postId, body.trim(), parentId)
        }
    }

    override suspend fun deleteComment(commentId: Long): ApiResult<NovaPost> {
        return authenticatedCall { accessToken ->
            api.deleteComment(accessToken, commentId)
        }
    }

    override suspend fun deleteCommentReply(replyId: Long): ApiResult<NovaPost> {
        return authenticatedCall { accessToken ->
            api.deleteCommentReply(accessToken, replyId)
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

        return when (val refreshed = authRemote.refresh(stored.refreshToken)) {
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
