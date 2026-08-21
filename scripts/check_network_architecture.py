#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

CLIENT = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
PERSON_COMPAT = ROOT / "app/src/main/java/com/nova/app/core/network/PersonModelCompatibility.kt"
PEOPLE_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleJsonParser.kt"
POST_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/posts/domain/model/PostModels.kt"
AUTH_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/auth/NovaAuthRepository.kt"
FEED_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/feed/NovaFeedRepository.kt"
SOCIAL_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialRepository.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required network characterization file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


client = read(CLIENT)
person_compat = read(PERSON_COMPAT)
people_parser = read(PEOPLE_PARSER)
post_models = read(POST_MODELS)
auth_repository = read(AUTH_REPOSITORY)
feed_repository = read(FEED_REPOSITORY)
social_repository = read(SOCIAL_REPOSITORY)

# This gate freezes the current shared boundary before Phase 5 starts moving
# feature DTO/parser ownership. Deliberate later extractions must update this
# characterization in the same PR; accidental wire/error drift must fail CI.
for required in (
    'data class NovaUser(',
    'data class NovaPostAuthor(',
    'data class AuthSession(',
    'data class UploadFile(',
    'sealed interface ApiResult<out T>',
    'class NovaApiClient(',
    'private val baseUrl: String = "http://127.0.0.1:8000/api/v1/"',
    'import com.nova.app.feature.people.data.parseNovaPerson',
    'import com.nova.app.feature.posts.domain.model.NovaComment',
    'import com.nova.app.feature.posts.domain.model.NovaCommentMutation',
    'import com.nova.app.feature.posts.domain.model.NovaPost',
    'import com.nova.app.feature.posts.domain.model.NovaPostPage',
):
    if required not in client:
        errors.append(f"NovaApiClient shared boundary lost seam: {required}")

for forbidden in (
    'data class NovaPost(',
    'data class NovaPostPage(',
    'data class NovaComment(',
    'data class NovaCommentMutation(',
):
    if forbidden in client:
        errors.append(f"NovaApiClient must not reclaim feature-owned post model: {forbidden}")

if 'typealias NovaPerson = com.nova.app.feature.people.domain.model.NovaPerson' not in person_compat:
    errors.append("temporary NovaPerson compatibility alias changed before compatibility cleanup")

for required in (
    'internal fun parseNovaPerson(',
    'json: JSONObject',
    'resolveMediaUrl: (String) -> String',
    'id = json.optLong("id")',
    'username = json.optString("username")',
    'name = json.optString("name")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'followersCount = json.optInt("followers_count", 0)',
    'followingCount = json.optInt("following_count", 0)',
    'postsCount = json.optInt("posts_count", 0)',
    'isFollowing = json.optBoolean("is_following", false)',
):
    if required not in people_parser:
        errors.append(f"feature People parser characterization changed: {required}")

for required in (
    'add(parseNovaPerson(it, ::resolveMediaUrl))',
    'ApiResult.Success(parseNovaPerson(response.value, ::resolveMediaUrl))',
):
    if required not in client:
        errors.append(f"NovaApiClient live People decoding changed: {required}")

if 'private fun parsePerson(' in client:
    errors.append("NovaApiClient must not retain the superseded core People parser")

for required in (
    'data class NovaPost(',
    'val author: NovaPostAuthor',
    'data class NovaPostPage(',
    'data class NovaComment(',
    'data class NovaCommentMutation(',
):
    if required not in post_models:
        errors.append(f"feature-owned post model seam changed: {required}")

# Auth/profile wire contract.
for required in (
    'requestJson("auth/register/", "POST", body)',
    'requestJson("auth/login/", "POST", body)',
    'requestJson("me/", bearerToken = accessToken)',
    'path = "me/"',
    'method = "PUT"',
    '"name" to name',
    '"username" to username',
    'fileField = "avatar"',
    'requestJson("auth/refresh/", "POST", body)',
    'JSONObject().put("refresh", refreshToken)',
    'ApiResult.Failure("Nova returned an invalid authentication response.")',
    'ApiResult.Failure("Nova returned an invalid session response.")',
):
    if required not in client:
        errors.append(f"auth/profile network contract changed: {required}")

# People/social wire contract.
for required in (
    '"people/"',
    '"people/?q=${encode(cleanQuery)}"',
    'path = "people/${encode(username.trim().lowercase())}/"',
    'path = "people/${encode(username.trim().lowercase())}/posts/"',
    'val path = "people/${encode(username.trim().lowercase())}/follow/"',
    'val path = "people/${encode(username.trim().lowercase())}/block/"',
    'path = "people/${encode(username.trim().lowercase())}/report/"',
    '.put("reason", reason)',
    '.put("details", details)',
    '"Report submitted for review."',
):
    if required not in client:
        errors.append(f"People/social network contract changed: {required}")

# Feed/posts/comments wire contract.
for required in (
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
):
    if required not in client:
        errors.append(f"feed/posts/comments network contract changed: {required}")

# Core parser semantics that remain in NovaApiClient after the People extraction.
for required in (
    'id = json.optLong("id")',
    'email = json.optString("email")',
    'username = json.optString("username")',
    'name = json.optString("name")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'followersCount = json.optInt("followers_count", 0)',
    'followingCount = json.optInt("following_count", 0)',
    'postsCount = json.optInt("posts_count", 0)',
    'val nextCursor = json.optString("next_cursor")',
    '.takeIf { it.isNotBlank() && it != "null" }',
    'imageUrl = resolveMediaUrl(json.optString("image_url"))',
    'caption = json.optString("caption")',
    'createdAt = json.optString("created_at")',
    'isMine = json.optBoolean("is_mine", false)',
    'likesCount = json.optInt("likes_count", 0)',
    'commentsCount = json.optInt("comments_count", 0)',
    'isLiked = json.optBoolean("is_liked", false)',
    'val rawParentId = json.opt("parent_id")',
    'null, JSONObject.NULL -> null',
    'is Number -> rawParentId.toLong().takeIf { it > 0L }',
    'rawParentId.toString().toLongOrNull()?.takeIf { it > 0L }',
    'val replyRows = json.optJSONArray("replies") ?: JSONArray()',
    'array.optJSONObject(index)?.let { add(parseNovaComment(it, ::resolveMediaUrl)) }',
    'repliesCount = json.optInt("replies_count", replies.size)',
):
    if required not in client:
        errors.append(f"network parser characterization changed: {required}")

# Media URL and query encoding behavior.
for required in (
    'if (raw.isBlank() || raw == "null") return ""',
    'if (raw.startsWith("http://") || raw.startsWith("https://")) return raw',
    'val apiUrl = URL(baseUrl)',
    'URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()',
    '.getOrDefault(raw)',
    'URLEncoder.encode(value, Charsets.UTF_8.name())',
):
    if required not in client:
        errors.append(f"network URL/encoding behavior changed: {required}")

# JSON transport contract.
for required in (
    'private suspend fun requestJson(',
    'withContext(Dispatchers.IO)',
    'connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply',
    'requestMethod = method',
    'connectTimeout = 10_000',
    'readTimeout = 10_000',
    'setRequestProperty("Accept", "application/json")',
    'bearerToken?.let { setRequestProperty("Authorization", "Bearer $it") }',
    'setRequestProperty("Content-Type", "application/json; charset=utf-8")',
    'outputStream.bufferedWriter(Charsets.UTF_8).use',
    'writer.write(body.toString())',
    'readJsonResponse(connection)',
    'message = "Can\'t reach Nova right now. Check your connection and try again."',
    'connection?.disconnect()',
):
    if required not in client:
        errors.append(f"JSON transport characterization changed: {required}")

# Multipart transport contract.
for required in (
    'private suspend fun requestMultipart(',
    'val boundary = "Nova-${UUID.randomUUID()}"',
    'val lineEnd = "\\r\\n"',
    'connectTimeout = 20_000',
    'readTimeout = 20_000',
    'setRequestProperty("Authorization", "Bearer $bearerToken")',
    'setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")',
    'DataOutputStream(connection.outputStream).use',
    'Content-Disposition: form-data; name=\\"$name\\"',
    'Content-Type: text/plain; charset=UTF-8',
    'output.write(value.toByteArray(Charsets.UTF_8))',
    'Content-Disposition: form-data; name=\\"$fileField\\"; filename=\\"${file.fileName}\\"',
    'Content-Type: ${file.mimeType}',
    'output.write(file.bytes)',
    'output.writeBytes("--$boundary--$lineEnd")',
    'message = "Nova couldn\'t upload that right now. Check your connection and try again."',
):
    if required not in client:
        errors.append(f"multipart transport characterization changed: {required}")

# Response/error behavior.
for required in (
    'val status = connection.responseCode',
    'val stream = if (status in 200..299) connection.inputStream else connection.errorStream',
    'val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()',
    'val json = if (raw.isBlank()) JSONObject() else runCatching { JSONObject(raw) }',
    '.getOrElse { JSONObject().put("detail", raw) }',
    'message = parseError(json, status)',
    'statusCode = status',
    'val detail = json.optString("detail")',
    'return if (statusCode == 401) "Email or password is incorrect." else detail',
    '"email" -> "Email: $message"',
    '"username" -> "Username: $message"',
    '"password" -> "Password: $message"',
    '"avatar" -> "Photo: $message"',
    '"image" -> "Photo: $message"',
    '"caption" -> "Caption: $message"',
    '"body" -> "Comment: $message"',
    '400 -> "Check your details and try again."',
    '401 -> "Your session expired. Please log in again."',
    '404 -> "Nova couldn\'t find that resource."',
    '429 -> "Too many requests. Give Nova a moment and try again."',
    'in 500..599 -> "Nova\'s server had a problem. Try again in a moment."',
    'else -> "Something went wrong. Please try again."',
):
    if required not in client:
        errors.append(f"network response/error characterization changed: {required}")

# Current direct consumers all point at the same production API and keep refresh
# orchestration above NovaApiClient. Future extraction may move these calls only
# together with an intentional gate update.
production_ctor = (
    'NovaApiClient("https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/")'
)
for text, name in (
    (auth_repository, "NovaAuthRepository.kt"),
    (feed_repository, "NovaFeedRepository.kt"),
    (social_repository, "NovaSocialRepository.kt"),
):
    if production_ctor not in text:
        errors.append(f"{name} changed the characterized production Nova API base URL")

for required in ("api.register(", "api.login(", "api.me(", "api.updateProfile(", "api.refresh("):
    if required not in auth_repository:
        errors.append(f"NovaAuthRepository lost characterized NovaApiClient call: {required}")
for required in (
    "api.feed(",
    "api.personPosts(",
    "api.post(",
    "api.createPost(",
    "api.deletePost(",
    "api.setLiked(",
    "api.comments(",
    "api.addComment(",
    "api.deleteComment(",
    "api.deleteCommentReply(",
    "api.refresh(",
):
    if required not in feed_repository:
        errors.append(f"NovaFeedRepository lost characterized NovaApiClient call: {required}")
for required in (
    "api.people(",
    "api.person(",
    "api.setFollowing(",
    "api.setBlocked(",
    "api.reportPerson(",
    "api.refresh(",
):
    if required not in social_repository:
        errors.append(f"NovaSocialRepository lost characterized NovaApiClient call: {required}")

if errors:
    print("Network architecture characterization failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Network architecture characterization passed.")