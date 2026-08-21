#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/nova/app"

required = [
    MAIN / "feature/posts/domain/model/PostModels.kt",
    MAIN / "feature/posts/data/PostRepository.kt",
    MAIN / "feature/posts/data/PostJsonParser.kt",
    MAIN / "feature/posts/data/remote/PostsRemoteDataSource.kt",
    MAIN / "feature/feed/data/FeedRepository.kt",
    MAIN / "feature/feed/FeedStateOwner.kt",
    MAIN / "feature/posts/detail/PostDetailStateOwner.kt",
    MAIN / "feature/posts/comments/PostCommentsStateOwner.kt",
]

errors: list[str] = []
for path in required:
    if not path.exists():
        errors.append(f"missing stable feed/posts owner: {path.relative_to(ROOT)}")

legacy_imports = (
    "import com.nova.app.core.network.NovaPost\n",
    "import com.nova.app.core.network.NovaPostPage\n",
    "import com.nova.app.core.network.NovaComment\n",
    "import com.nova.app.core.network.NovaCommentMutation\n",
    "import com.nova.app.core.network.NovaPostAuthor\n",
)

consolidated_paths = [
    MAIN / "NovaApp.kt",
    MAIN / "feature/feed",
    MAIN / "feature/home",
    MAIN / "feature/post",
    MAIN / "feature/posts",
]

for entry in consolidated_paths:
    files = [entry] if entry.is_file() else entry.rglob("*.kt")
    for path in files:
        text = path.read_text(encoding="utf-8")
        for forbidden in legacy_imports:
            if forbidden in text:
                errors.append(
                    f"legacy post model import in consolidated slice: {path.relative_to(ROOT)} -> {forbidden.strip()}"
                )

api = MAIN / "core/network/NovaApiClient.kt"
api_text = api.read_text(encoding="utf-8")
post_models = MAIN / "feature/posts/domain/model/PostModels.kt"
post_models_text = post_models.read_text(encoding="utf-8")
post_json_parser = MAIN / "feature/posts/data/PostJsonParser.kt"
post_json_parser_text = post_json_parser.read_text(encoding="utf-8")
posts_remote = MAIN / "feature/posts/data/remote/PostsRemoteDataSource.kt"
posts_remote_text = posts_remote.read_text(encoding="utf-8")
people_remote = MAIN / "feature/people/data/remote/PeopleRemoteDataSource.kt"
people_remote_text = people_remote.read_text(encoding="utf-8") if people_remote.exists() else ""
feed_repository = MAIN / "core/feed/NovaFeedRepository.kt"
feed_repository_text = feed_repository.read_text(encoding="utf-8")

for declaration in (
    "data class NovaPostAuthor(",
    "data class NovaPost(",
    "data class NovaPostPage(",
    "data class NovaComment(",
    "data class NovaCommentMutation(",
):
    if declaration in api_text:
        errors.append(f"NovaApiClient still owns Posts declaration: {declaration}")

for required_text in (
    "data class NovaPostAuthor(",
    "val author: NovaPostAuthor",
    "data class NovaPost(",
    "data class NovaPostPage(",
    "data class NovaComment(",
    "data class NovaCommentMutation(",
):
    if required_text not in post_models_text:
        errors.append(f"feature-owned Posts models lost seam: {required_text}")

for required_text in (
    "import com.nova.app.feature.posts.domain.model.NovaPostAuthor",
    "internal fun parseNovaPostAuthor(",
    "internal fun parseNovaPosts(",
    "internal fun parseNovaPostPage(",
    "internal fun parseNovaPost(",
    "internal fun parseNovaComment(",
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'val nextCursor = json.optString("next_cursor")',
    '.takeIf { it.isNotBlank() && it != "null" }',
    'imageUrl = resolveMediaUrl(json.optString("image_url"))',
    'val rawParentId = json.opt("parent_id")',
    'null, JSONObject.NULL -> null',
    'is Number -> rawParentId.toLong().takeIf { it > 0L }',
    'rawParentId.toString().toLongOrNull()?.takeIf { it > 0L }',
    'val replyRows = json.optJSONArray("replies") ?: JSONArray()',
    'replyRows.optJSONObject(index)?.let { add(parseNovaComment(it, resolveMediaUrl)) }',
    'repliesCount = json.optInt("replies_count", replies.size)',
):
    if required_text not in post_json_parser_text:
        errors.append(f"feature-owned Posts JSON parser lost characterized behavior: {required_text}")

for required_text in (
    "class PostsRemoteDataSource(",
    "private val api: NovaApiClient",
    '"feed/"',
    '"feed/?cursor=${encode(cursor)}"',
    'path = "posts/$postId/"',
    'path = "posts/"',
    'fields = mapOf("caption" to caption)',
    'fileField = "image"',
    'path = "posts/$postId/like/"',
    'path = "posts/$postId/comments/"',
    'JSONObject().put("body", body)',
    'payload.put("parent_id", it)',
    'deleteCommentResource(accessToken, "comments/$commentId/")',
    'deleteCommentResource(accessToken, "comment-replies/$replyId/")',
    'ApiResult.Failure("Nova returned an invalid comment response.")',
    'parseNovaPostPage(response.value, api::resolveMediaUrl)',
    'parseNovaPost(response.value, api::resolveMediaUrl)',
    'add(parseNovaComment(it, api::resolveMediaUrl))',
    'comment = parseNovaComment(comment, api::resolveMediaUrl)',
    'post = parseNovaPost(post, api::resolveMediaUrl)',
    'ApiResult.Success(parseNovaPost(post, api::resolveMediaUrl))',
):
    if required_text not in posts_remote_text:
        errors.append(f"feature-owned Posts transport lost characterized behavior: {required_text}")

for forbidden in (
    "import com.nova.app.feature.posts.data.parseNovaComment",
    "import com.nova.app.feature.posts.data.parseNovaPost",
    "import com.nova.app.feature.posts.data.parseNovaPostPage",
    "import com.nova.app.feature.posts.data.parseNovaPosts",
    "suspend fun feed(",
    "suspend fun post(",
    "suspend fun createPost(",
    "suspend fun deletePost(",
    "suspend fun setLiked(",
    "suspend fun comments(",
    "suspend fun addComment(",
    "suspend fun deleteComment(",
    "suspend fun deleteCommentReply(",
):
    if forbidden in api_text:
        errors.append(f"NovaApiClient must stay free of Posts feature seam: {forbidden}")

for required_text in (
    "import com.nova.app.feature.posts.data.remote.PostsRemoteDataSource",
    "private val postsRemote = PostsRemoteDataSource(api)",
    "postsRemote.feed(accessToken, cursor)",
    "postsRemote.post(accessToken, postId)",
    "postsRemote.createPost(",
    "postsRemote.deletePost(accessToken, postId)",
    "postsRemote.setLiked(accessToken, postId, liked)",
    "postsRemote.comments(accessToken, postId)",
    "postsRemote.addComment(accessToken, postId, body.trim(), parentId)",
    "postsRemote.deleteComment(accessToken, commentId)",
    "postsRemote.deleteCommentReply(accessToken, replyId)",
):
    if required_text not in feed_repository_text:
        errors.append(f"NovaFeedRepository must route Posts through feature transport: {required_text}")
for forbidden in (
    "api.feed(",
    "api.post(",
    "api.createPost(",
    "api.deletePost(",
    "api.setLiked(",
    "api.comments(",
    "api.addComment(",
    "api.deleteComment(",
    "api.deleteCommentReply(",
):
    if forbidden in feed_repository_text:
        errors.append(f"NovaFeedRepository restored shared-client Posts endpoint: {forbidden}")

# Profile-post list decoding lives in People but reuses the authoritative Posts parser.
for required_text in (
    "import com.nova.app.feature.posts.data.parseNovaPosts",
    "parseNovaPosts(response.value, api::resolveMediaUrl)",
):
    if required_text not in people_remote_text:
        errors.append(f"People profile-post transport must reuse Posts parser: {required_text}")

comments_start = posts_remote_text.find("    suspend fun comments(")
comments_end = posts_remote_text.find("    suspend fun addComment(", comments_start)
if comments_start == -1 or comments_end == -1:
    errors.append("PostsRemoteDataSource comments endpoint boundary is missing")
else:
    live_comments_body = posts_remote_text[comments_start:comments_end]
    if "add(parseNovaComment(it, api::resolveMediaUrl))" not in live_comments_body:
        errors.append("PostsRemoteDataSource comments endpoint must use feature-owned parseNovaComment")

comments_screen = MAIN / "feature/post/PostCommentsScreen.kt"
comments_text = comments_screen.read_text(encoding="utf-8")
for forbidden in ("NovaFeedRepository", "PostRepository", "deleteCommentReply("):
    if forbidden in comments_text:
        errors.append(f"PostCommentsScreen still owns transport: {forbidden}")

home = MAIN / "feature/home/HomeScreen.kt"
home_text = home.read_text(encoding="utf-8")
if "NovaFeedRepository" in home_text:
    errors.append("HomeScreen still constructs/depends on NovaFeedRepository")
if "onResolvePost" not in home_text:
    errors.append("HomeScreen must resolve push-target posts through its callback boundary")

app = MAIN / "NovaApp.kt"
app_text = app.read_text(encoding="utf-8")
for required_text in (
    "appContainer.postDataRepository",
    "PostCommentsStateOwner",
    "onResolvePost = postRepository::post",
    "onSendReply = commentsOwner::sendReply",
    "onDeleteReply = commentsOwner::deleteReply",
):
    if required_text not in app_text:
        errors.append(f"NovaApp missing stable feed/posts wiring: {required_text}")

# Phase 5 exit: no core post-author alias/import may return anywhere in Android sources.
for kotlin_file in (ROOT / "app/src").rglob("*.kt"):
    text = kotlin_file.read_text(encoding="utf-8")
    if "import com.nova.app.core.network.NovaPostAuthor" in text:
        errors.append(f"core NovaPostAuthor import returned: {kotlin_file.relative_to(ROOT)}")

if errors:
    print("Feed/posts architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Feed/posts architecture Phase 5 exit check passed.")
