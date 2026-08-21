package com.nova.app.feature.posts.data.remote

import com.nova.app.core.network.ApiResult
import com.nova.app.core.network.NovaApiClient
import com.nova.app.core.network.UploadFile
import com.nova.app.feature.posts.data.parseNovaComment
import com.nova.app.feature.posts.data.parseNovaPost
import com.nova.app.feature.posts.data.parseNovaPostPage
import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaCommentMutation
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostPage
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder


class PostsRemoteDataSource(
    private val api: NovaApiClient,
) {
    suspend fun feed(
        accessToken: String,
        cursor: String? = null,
    ): ApiResult<NovaPostPage> {
        val path = if (cursor.isNullOrBlank()) {
            "feed/"
        } else {
            "feed/?cursor=${encode(cursor)}"
        }

        return when (val response = api.requestJson(path, bearerToken = accessToken)) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPostPage(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun post(
        accessToken: String,
        postId: Long,
    ): ApiResult<NovaPost> {
        return when (
            val response = api.requestJson(
                path = "posts/$postId/",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPost(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun createPost(
        accessToken: String,
        caption: String,
        image: UploadFile,
    ): ApiResult<NovaPost> {
        return when (
            val response = api.requestMultipart(
                path = "posts/",
                method = "POST",
                fields = mapOf("caption" to caption),
                fileField = "image",
                file = image,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPost(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun deletePost(
        accessToken: String,
        postId: Long,
    ): ApiResult<Unit> {
        return when (
            val response = api.requestJson(
                path = "posts/$postId/",
                method = "DELETE",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> response
        }
    }

    suspend fun setLiked(
        accessToken: String,
        postId: Long,
        liked: Boolean,
    ): ApiResult<NovaPost> {
        val response = if (liked) {
            api.requestJson(
                path = "posts/$postId/like/",
                method = "POST",
                body = JSONObject(),
                bearerToken = accessToken,
            )
        } else {
            api.requestJson(
                path = "posts/$postId/like/",
                method = "DELETE",
                bearerToken = accessToken,
            )
        }

        return when (response) {
            is ApiResult.Success -> ApiResult.Success(parseNovaPost(response.value, api::resolveMediaUrl))
            is ApiResult.Failure -> response
        }
    }

    suspend fun comments(
        accessToken: String,
        postId: Long,
    ): ApiResult<List<NovaComment>> {
        return when (
            val response = api.requestJson(
                path = "posts/$postId/comments/",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> {
                val array = response.value.optJSONArray("results") ?: JSONArray()
                val comments = buildList {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let {
                            add(parseNovaComment(it, api::resolveMediaUrl))
                        }
                    }
                }
                ApiResult.Success(comments)
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun addComment(
        accessToken: String,
        postId: Long,
        body: String,
        parentId: Long? = null,
    ): ApiResult<NovaCommentMutation> {
        val payload = JSONObject().put("body", body)
        parentId?.takeIf { it > 0L }?.let { payload.put("parent_id", it) }
        return when (
            val response = api.requestJson(
                path = "posts/$postId/comments/",
                method = "POST",
                body = payload,
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> {
                val comment = response.value.optJSONObject("comment")
                val post = response.value.optJSONObject("post")
                if (comment == null || post == null) {
                    ApiResult.Failure("Nova returned an invalid comment response.")
                } else {
                    ApiResult.Success(
                        NovaCommentMutation(
                            comment = parseNovaComment(comment, api::resolveMediaUrl),
                            post = parseNovaPost(post, api::resolveMediaUrl),
                        ),
                    )
                }
            }

            is ApiResult.Failure -> response
        }
    }

    suspend fun deleteComment(
        accessToken: String,
        commentId: Long,
    ): ApiResult<NovaPost> {
        return deleteCommentResource(accessToken, "comments/$commentId/")
    }

    suspend fun deleteCommentReply(
        accessToken: String,
        replyId: Long,
    ): ApiResult<NovaPost> {
        return deleteCommentResource(accessToken, "comment-replies/$replyId/")
    }

    private suspend fun deleteCommentResource(
        accessToken: String,
        path: String,
    ): ApiResult<NovaPost> {
        return when (
            val response = api.requestJson(
                path = path,
                method = "DELETE",
                bearerToken = accessToken,
            )
        ) {
            is ApiResult.Success -> {
                val post = response.value.optJSONObject("post")
                if (post == null) {
                    ApiResult.Failure("Nova returned an invalid comment response.")
                } else {
                    ApiResult.Success(parseNovaPost(post, api::resolveMediaUrl))
                }
            }

            is ApiResult.Failure -> response
        }
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
