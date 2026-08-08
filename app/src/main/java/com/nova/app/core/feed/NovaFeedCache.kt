package com.nova.app.core.feed

import android.content.Context
import com.nova.app.core.network.NovaPost
import com.nova.app.core.network.NovaPostAuthor
import com.nova.app.core.network.NovaPostPage
import org.json.JSONArray
import org.json.JSONObject


class NovaFeedCache(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(userId: Long, page: NovaPostPage) {
        if (userId <= 0L) return

        val payload = JSONObject()
            .put("saved_at", System.currentTimeMillis())
            .put("next_cursor", page.nextCursor ?: JSONObject.NULL)
            .put(
                "posts",
                JSONArray().apply {
                    page.posts.forEach { put(postToJson(it)) }
                },
            )

        prefs.edit()
            .putString(key(userId), payload.toString())
            .apply()
    }

    fun load(userId: Long): NovaPostPage? {
        if (userId <= 0L) return null
        val cacheKey = key(userId)
        val raw = prefs.getString(cacheKey, null) ?: return null

        return runCatching {
            val payload = JSONObject(raw)
            val savedAt = payload.optLong("saved_at", 0L)
            if (savedAt <= 0L || System.currentTimeMillis() - savedAt > MAX_AGE_MS) {
                prefs.edit().remove(cacheKey).apply()
                return@runCatching null
            }

            val array = payload.optJSONArray("posts") ?: JSONArray()
            val posts = buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let { add(jsonToPost(it)) }
                }
            }

            NovaPostPage(
                posts = posts,
                nextCursor = payload.optString("next_cursor")
                    .takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrNull()
    }

    private fun postToJson(post: NovaPost): JSONObject {
        return JSONObject()
            .put("id", post.id)
            .put(
                "author",
                JSONObject()
                    .put("id", post.author.id)
                    .put("username", post.author.username)
                    .put("name", post.author.name)
                    .put("avatar_url", post.author.avatarUrl),
            )
            .put("image_url", post.imageUrl)
            .put("caption", post.caption)
            .put("created_at", post.createdAt)
            .put("is_mine", post.isMine)
            .put("likes_count", post.likesCount)
            .put("comments_count", post.commentsCount)
            .put("is_liked", post.isLiked)
    }

    private fun jsonToPost(json: JSONObject): NovaPost {
        val author = json.optJSONObject("author") ?: JSONObject()
        return NovaPost(
            id = json.optLong("id"),
            author = NovaPostAuthor(
                id = author.optLong("id"),
                username = author.optString("username"),
                name = author.optString("name"),
                avatarUrl = author.optString("avatar_url"),
            ),
            imageUrl = json.optString("image_url"),
            caption = json.optString("caption"),
            createdAt = json.optString("created_at"),
            isMine = json.optBoolean("is_mine", false),
            likesCount = json.optInt("likes_count", 0),
            commentsCount = json.optInt("comments_count", 0),
            isLiked = json.optBoolean("is_liked", false),
        )
    }

    private fun key(userId: Long) = "feed_$userId"

    private companion object {
        const val PREFS_NAME = "nova_feed_cache"
        const val MAX_AGE_MS = 48L * 60L * 60L * 1000L
    }
}
