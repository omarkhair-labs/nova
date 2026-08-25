package com.nova.app.feature.posts.data

import com.nova.app.feature.posts.domain.model.NovaComment
import com.nova.app.feature.posts.domain.model.NovaPost
import com.nova.app.feature.posts.domain.model.NovaPostAuthor
import com.nova.app.feature.posts.domain.model.NovaPostPage
import org.json.JSONArray
import org.json.JSONObject


internal fun parseNovaPostAuthor(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaPostAuthor {
    return NovaPostAuthor(
        id = json.optLong("id"),
        username = json.optString("username"),
        name = json.optString("name"),
        avatarUrl = resolveMediaUrl(json.optString("avatar_url")),
    )
}


internal fun parseNovaPosts(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): List<NovaPost> {
    val array = json.optJSONArray("results") ?: JSONArray()
    return buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.let { add(parseNovaPost(it, resolveMediaUrl)) }
        }
    }
}


internal fun parseNovaPostPage(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaPostPage {
    val nextCursor = json.optString("next_cursor")
        .takeIf { it.isNotBlank() && it != "null" }
    return NovaPostPage(
        posts = parseNovaPosts(json, resolveMediaUrl),
        nextCursor = nextCursor,
    )
}


internal fun parseNovaPost(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaPost {
    val author = json.optJSONObject("author") ?: JSONObject()
    val repostedBy = json.optJSONObject("reposted_by")?.let {
        parseNovaPostAuthor(it, resolveMediaUrl)
    }
    return NovaPost(
        id = json.optLong("id"),
        author = parseNovaPostAuthor(author, resolveMediaUrl),
        imageUrl = resolveMediaUrl(json.optString("image_url")),
        caption = json.optString("caption"),
        createdAt = json.optString("created_at"),
        isMine = json.optBoolean("is_mine", false),
        likesCount = json.optInt("likes_count", 0),
        commentsCount = json.optInt("comments_count", 0),
        isLiked = json.optBoolean("is_liked", false),
        repostsCount = json.optInt("reposts_count", 0),
        isReposted = json.optBoolean("is_reposted", false),
        repostedBy = repostedBy,
    )
}


internal fun parseNovaComment(
    json: JSONObject,
    resolveMediaUrl: (String) -> String,
): NovaComment {
    val author = json.optJSONObject("author") ?: JSONObject()
    val rawParentId = json.opt("parent_id")
    val parentId = when (rawParentId) {
        null, JSONObject.NULL -> null
        is Number -> rawParentId.toLong().takeIf { it > 0L }
        else -> rawParentId.toString().toLongOrNull()?.takeIf { it > 0L }
    }
    val replyRows = json.optJSONArray("replies") ?: JSONArray()
    val replies = buildList {
        for (index in 0 until replyRows.length()) {
            replyRows.optJSONObject(index)?.let { add(parseNovaComment(it, resolveMediaUrl)) }
        }
    }
    return NovaComment(
        id = json.optLong("id"),
        author = parseNovaPostAuthor(author, resolveMediaUrl),
        body = json.optString("body"),
        createdAt = json.optString("created_at"),
        isMine = json.optBoolean("is_mine", false),
        parentId = parentId,
        repliesCount = json.optInt("replies_count", replies.size),
        replies = replies,
        likesCount = json.optInt("likes_count", 0),
        isLiked = json.optBoolean("is_liked", false),
    )
}
