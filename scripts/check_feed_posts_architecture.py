#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/nova/app"

required = [
    MAIN / "feature/posts/domain/model/PostModels.kt",
    MAIN / "feature/posts/data/PostRepository.kt",
    MAIN / "feature/posts/data/PostJsonParser.kt",
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
post_json_parser = MAIN / "feature/posts/data/PostJsonParser.kt"
post_json_parser_text = post_json_parser.read_text(encoding="utf-8")
people_remote = MAIN / "feature/people/data/remote/PeopleRemoteDataSource.kt"
people_remote_text = people_remote.read_text(encoding="utf-8") if people_remote.exists() else ""

for declaration in (
    "data class NovaPost(",
    "data class NovaPostPage(",
    "data class NovaComment(",
    "data class NovaCommentMutation(",
):
    if declaration in api_text:
        errors.append(f"NovaApiClient still owns post model declaration: {declaration}")

for required_text in (
    "import com.nova.app.core.network.NovaPostAuthor",
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

# Shared client only needs the parsers used by feed/post/comment endpoints.
for required_import in (
    "import com.nova.app.feature.posts.data.parseNovaComment",
    "import com.nova.app.feature.posts.data.parseNovaPost",
    "import com.nova.app.feature.posts.data.parseNovaPostPage",
):
    if required_import not in api_text:
        errors.append(f"NovaApiClient must import feature-owned Posts parser: {required_import}")
if "import com.nova.app.feature.posts.data.parseNovaPosts" in api_text:
    errors.append("NovaApiClient must not retain parseNovaPosts after People profile-post transport moved out")

for required_call in (
    "parseNovaPostPage(response.value, ::resolveMediaUrl)",
    "parseNovaPost(response.value, ::resolveMediaUrl)",
    "add(parseNovaComment(it, ::resolveMediaUrl))",
    "comment = parseNovaComment(comment, ::resolveMediaUrl)",
    "post = parseNovaPost(post, ::resolveMediaUrl)",
    "ApiResult.Success(parseNovaPost(post, ::resolveMediaUrl))",
):
    if required_call not in api_text:
        errors.append(f"NovaApiClient live Posts decode path must use feature parser: {required_call}")

# Profile-post list decoding now lives in the People feature but still reuses the authoritative Posts parser.
for required_text in (
    "import com.nova.app.feature.posts.data.parseNovaPosts",
    "parseNovaPosts(response.value, api::resolveMediaUrl)",
):
    if required_text not in people_remote_text:
        errors.append(f"People profile-post transport must reuse Posts parser: {required_text}")

for forbidden_call in (
    "ApiResult.Success(parsePosts(response.value))",
    "ApiResult.Success(parsePostPage(response.value))",
    "ApiResult.Success(parsePost(response.value))",
    "comment = parseComment(comment)",
    "post = parsePost(post)",
    "ApiResult.Success(parsePost(post))",
):
    if forbidden_call in api_text:
        errors.append(f"NovaApiClient must not route live Posts responses through core parser: {forbidden_call}")

comments_start = api_text.find("    suspend fun comments(")
comments_end = api_text.find("    suspend fun addComment(", comments_start)
if comments_start == -1 or comments_end == -1:
    errors.append("NovaApiClient comments endpoint boundary is missing")
else:
    live_comments_body = api_text[comments_start:comments_end]
    if "add(parseComment(it))" in live_comments_body:
        errors.append(
            "NovaApiClient live comments endpoint must not route responses through core parseComment"
        )
    if "add(parseNovaComment(it, ::resolveMediaUrl))" not in live_comments_body:
        errors.append(
            "NovaApiClient live comments endpoint must use feature-owned parseNovaComment"
        )

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

if errors:
    print("Feed/posts architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Feed/posts architecture check passed.")
