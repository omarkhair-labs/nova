#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

CLIENT = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
PERSON_COMPAT = ROOT / "app/src/main/java/com/nova/app/core/network/PersonModelCompatibility.kt"
AUTH_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/auth/domain/model/AuthModels.kt"
AUTH_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/auth/data/AuthJsonParser.kt"
AUTH_REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/auth/data/remote/AuthRemoteDataSource.kt"
PEOPLE_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleJsonParser.kt"
POST_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/posts/domain/model/PostModels.kt"
POST_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/posts/data/PostJsonParser.kt"
AUTH_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/auth/NovaAuthRepository.kt"
FEED_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/feed/NovaFeedRepository.kt"
SOCIAL_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialRepository.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required network characterization file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


def require_all(text: str, label: str, seams: tuple[str, ...]) -> None:
    for seam in seams:
        if seam not in text:
            errors.append(f"{label} changed: {seam}")


def forbid_all(text: str, label: str, seams: tuple[str, ...]) -> None:
    for seam in seams:
        if seam in text:
            errors.append(f"{label} returned: {seam}")


client = read(CLIENT)
person_compat = read(PERSON_COMPAT)
auth_models = read(AUTH_MODELS)
auth_parser = read(AUTH_PARSER)
auth_remote = read(AUTH_REMOTE)
people_parser = read(PEOPLE_PARSER)
post_models = read(POST_MODELS)
post_parser = read(POST_PARSER)
auth_repository = read(AUTH_REPOSITORY)
feed_repository = read(FEED_REPOSITORY)
social_repository = read(SOCIAL_REPOSITORY)

# Shared client: Auth feature ownership is gone; People/Posts endpoints remain
# characterized until their dedicated Phase 5 slices.
require_all(client, "NovaApiClient shared boundary", (
    'data class NovaPostAuthor(',
    'data class UploadFile(',
    'sealed interface ApiResult<out T>',
    'class NovaApiClient(',
    'private val baseUrl: String = "http://127.0.0.1:8000/api/v1/"',
    'import com.nova.app.feature.people.data.parseNovaPerson',
    'import com.nova.app.feature.posts.data.parseNovaComment',
    'import com.nova.app.feature.posts.data.parseNovaPost',
    'import com.nova.app.feature.posts.data.parseNovaPostPage',
    'import com.nova.app.feature.posts.data.parseNovaPosts',
))
forbid_all(client, "feature-owned core network seam", (
    'data class NovaUser(',
    'data class AuthSession(',
    'data class NovaPost(',
    'data class NovaPostPage(',
    'data class NovaComment(',
    'data class NovaCommentMutation(',
    'suspend fun register(',
    'suspend fun login(',
    'suspend fun me(',
    'suspend fun updateProfile(',
    'suspend fun refresh(',
    'import com.nova.app.feature.auth.data.parseAuthSession',
    'import com.nova.app.feature.auth.data.parseNovaUser',
    'private fun parsePerson(',
    'private fun parsePostAuthor(',
    'private fun parsePosts(',
    'private fun parsePostPage(',
    'private fun parsePost(',
    'private fun parseComment(',
))

require_all(person_compat, "temporary NovaPerson compatibility alias", (
    'typealias NovaPerson = com.nova.app.feature.people.domain.model.NovaPerson',
))

# Auth models/parser/remote are authoritative.
require_all(auth_models, "feature Auth models", (
    'package com.nova.app.feature.auth.domain.model',
    'data class NovaUser(',
    'val id: Long',
    'val email: String',
    'val username: String',
    'val name: String',
    'val avatarUrl: String',
    'val followersCount: Int = 0',
    'val followingCount: Int = 0',
    'val postsCount: Int = 0',
    'data class AuthSession(',
    'val accessToken: String',
    'val refreshToken: String',
    'val user: NovaUser',
))
require_all(auth_parser, "feature Auth parser", (
    'internal fun parseAuthSession(',
    'internal fun parseNovaUser(',
    'val access = json.optString("access")',
    'val refresh = json.optString("refresh")',
    'val userJson = json.optJSONObject("user")',
    'if (access.isBlank() || refresh.isBlank() || userJson == null)',
    'ApiResult.Failure("Nova returned an invalid authentication response.")',
    'user = parseNovaUser(userJson, resolveMediaUrl)',
    'id = json.optLong("id")',
    'email = json.optString("email")',
    'username = json.optString("username")',
    'name = json.optString("name")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'followersCount = json.optInt("followers_count", 0)',
    'followingCount = json.optInt("following_count", 0)',
    'postsCount = json.optInt("posts_count", 0)',
))
require_all(auth_remote, "feature Auth remote", (
    'class AuthRemoteDataSource(',
    'private val api: NovaApiClient',
    'api.requestJson("auth/register/", "POST", body)',
    '.put("email", email)',
    '.put("password", password)',
    '.put("username", username)',
    '.put("name", name)',
    'api.requestJson("auth/login/", "POST", body)',
    'api.requestJson("me/", bearerToken = accessToken)',
    'path = "me/"',
    'method = "PUT"',
    '"name" to name',
    '"username" to username',
    'fileField = "avatar"',
    'api.requestJson("auth/refresh/", "POST", body)',
    'JSONObject().put("refresh", refreshToken)',
    'ApiResult.Failure("Nova returned an invalid session response.")',
    'parseAuthSession(response.value, api::resolveMediaUrl)',
    'ApiResult.Success(parseNovaUser(response.value, api::resolveMediaUrl))',
))
require_all(auth_repository, "NovaAuthRepository feature remote ownership", (
    'private val remote: AuthRemoteDataSource',
    'remote.register(',
    'remote.login(',
    'remote.me(',
    'remote.updateProfile(',
    'remote.refresh(',
))
forbid_all(auth_repository, "NovaAuthRepository superseded core Auth seam", (
    'api.register(',
    'api.login(',
    'api.me(',
    'api.updateProfile(',
    'api.refresh(',
    'import com.nova.app.core.network.NovaUser',
    'import com.nova.app.core.network.AuthSession',
))

# Existing session-refresh orchestration in Feed/Social remains above the HTTP
# engine but now consumes the feature-owned Auth refresh endpoint.
for text, label in (
    (feed_repository, "NovaFeedRepository Auth refresh seam"),
    (social_repository, "NovaSocialRepository Auth refresh seam"),
):
    require_all(text, label, (
        'import com.nova.app.feature.auth.data.remote.AuthRemoteDataSource',
        'private val authRemote = AuthRemoteDataSource(api)',
        'authRemote.refresh(stored.refreshToken)',
    ))
    forbid_all(text, f"{label} legacy", ('api.refresh(stored.refreshToken)',))

# People parser + endpoint behavior is frozen for #169.
require_all(people_parser, "feature People parser", (
    'internal fun parseNovaPerson(',
    'id = json.optLong("id")',
    'username = json.optString("username")',
    'name = json.optString("name")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'followersCount = json.optInt("followers_count", 0)',
    'followingCount = json.optInt("following_count", 0)',
    'postsCount = json.optInt("posts_count", 0)',
    'isFollowing = json.optBoolean("is_following", false)',
))
require_all(client, "People/social network contract", (
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
    'add(parseNovaPerson(it, ::resolveMediaUrl))',
    'ApiResult.Success(parseNovaPerson(response.value, ::resolveMediaUrl))',
))

# Posts models/parser + endpoint behavior is frozen for #170.
require_all(post_models, "feature Posts models", (
    'data class NovaPost(',
    'val author: NovaPostAuthor',
    'data class NovaPostPage(',
    'data class NovaComment(',
    'data class NovaCommentMutation(',
))
require_all(post_parser, "feature Posts parser", (
    'internal fun parseNovaPostAuthor(',
    'internal fun parseNovaPosts(',
    'internal fun parseNovaPostPage(',
    'internal fun parseNovaPost(',
    'internal fun parseNovaComment(',
    'val nextCursor = json.optString("next_cursor")',
    '.takeIf { it.isNotBlank() && it != "null" }',
    'imageUrl = resolveMediaUrl(json.optString("image_url"))',
    'val rawParentId = json.opt("parent_id")',
    'null, JSONObject.NULL -> null',
    'is Number -> rawParentId.toLong().takeIf { it > 0L }',
    'val replyRows = json.optJSONArray("replies") ?: JSONArray()',
    'repliesCount = json.optInt("replies_count", replies.size)',
))
require_all(client, "feed/posts/comments network contract", (
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
    'parseNovaPosts(response.value, ::resolveMediaUrl)',
    'parseNovaPostPage(response.value, ::resolveMediaUrl)',
    'parseNovaPost(response.value, ::resolveMediaUrl)',
    'add(parseNovaComment(it, ::resolveMediaUrl))',
))

# Shared HTTP/media/error behavior must remain byte-for-byte semantically stable.
require_all(client, "network URL/media behavior", (
    'internal fun resolveMediaUrl(raw: String): String',
    'if (raw.isBlank() || raw == "null") return ""',
    'if (raw.startsWith("http://") || raw.startsWith("https://")) return raw',
    'val apiUrl = URL(baseUrl)',
    'URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()',
    '.getOrDefault(raw)',
    'URLEncoder.encode(value, Charsets.UTF_8.name())',
))
require_all(client, "JSON transport", (
    'internal suspend fun requestJson(',
    'withContext(Dispatchers.IO)',
    'connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply',
    'requestMethod = method',
    'connectTimeout = 10_000',
    'readTimeout = 10_000',
    'setRequestProperty("Accept", "application/json")',
    'bearerToken?.let { setRequestProperty("Authorization", "Bearer $it") }',
    'setRequestProperty("Content-Type", "application/json; charset=utf-8")',
    'writer.write(body.toString())',
    'message = "Can\'t reach Nova right now. Check your connection and try again."',
    'connection?.disconnect()',
))
require_all(client, "multipart transport", (
    'internal suspend fun requestMultipart(',
    'val boundary = "Nova-${UUID.randomUUID()}"',
    'val lineEnd = "\\r\\n"',
    'connectTimeout = 20_000',
    'readTimeout = 20_000',
    'setRequestProperty("Authorization", "Bearer $bearerToken")',
    'setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")',
    'DataOutputStream(connection.outputStream).use',
    'Content-Type: text/plain; charset=UTF-8',
    'output.write(value.toByteArray(Charsets.UTF_8))',
    'Content-Type: ${file.mimeType}',
    'output.write(file.bytes)',
    'output.writeBytes("--$boundary--$lineEnd")',
    'message = "Nova couldn\'t upload that right now. Check your connection and try again."',
))
require_all(client, "network response/error behavior", (
    'val status = connection.responseCode',
    'val stream = if (status in 200..299) connection.inputStream else connection.errorStream',
    'val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()',
    '.getOrElse { JSONObject().put("detail", raw) }',
    'message = parseError(json, status)',
    'statusCode = status',
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
))

production_ctor = 'NovaApiClient("https://zpjunyusgmug0hgsm8ebwhkn.158.101.254.30.sslip.io/api/v1/")'
for text, name in (
    (auth_repository, "NovaAuthRepository.kt"),
    (feed_repository, "NovaFeedRepository.kt"),
    (social_repository, "NovaSocialRepository.kt"),
):
    require_all(text, f"{name} production API base URL", (production_ctor,))

require_all(feed_repository, "NovaFeedRepository Posts client seam", (
    'api.feed(',
    'api.personPosts(',
    'api.post(',
    'api.createPost(',
    'api.deletePost(',
    'api.setLiked(',
    'api.comments(',
    'api.addComment(',
    'api.deleteComment(',
    'api.deleteCommentReply(',
))
require_all(social_repository, "NovaSocialRepository People client seam", (
    'api.people(',
    'api.person(',
    'api.setFollowing(',
    'api.setBlocked(',
    'api.reportPerson(',
))

if errors:
    print("Network architecture characterization failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Network architecture characterization passed.")
