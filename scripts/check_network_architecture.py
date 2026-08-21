#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

CLIENT = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
PERSON_COMPAT = ROOT / "app/src/main/java/com/nova/app/core/network/PersonModelCompatibility.kt"
CORE_PEOPLE_PAGING = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialPagingRepository.kt"
AUTH_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/auth/domain/model/AuthModels.kt"
AUTH_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/auth/data/AuthJsonParser.kt"
AUTH_REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/auth/data/remote/AuthRemoteDataSource.kt"
PEOPLE_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleJsonParser.kt"
PEOPLE_REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/people/data/remote/PeopleRemoteDataSource.kt"
PEOPLE_PAGING = ROOT / "app/src/main/java/com/nova/app/feature/people/data/remote/PeoplePagingRemoteRepository.kt"
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
auth_models = read(AUTH_MODELS)
auth_parser = read(AUTH_PARSER)
auth_remote = read(AUTH_REMOTE)
people_parser = read(PEOPLE_PARSER)
people_remote = read(PEOPLE_REMOTE)
people_paging = read(PEOPLE_PAGING)
post_models = read(POST_MODELS)
post_parser = read(POST_PARSER)
auth_repository = read(AUTH_REPOSITORY)
feed_repository = read(FEED_REPOSITORY)
social_repository = read(SOCIAL_REPOSITORY)

if PERSON_COMPAT.exists():
    errors.append("PersonModelCompatibility.kt must stay deleted after stable People ownership")
if CORE_PEOPLE_PAGING.exists():
    errors.append("NovaSocialPagingRepository.kt must stay deleted after feature People paging ownership")

# Shared client keeps only shared DTO/transport/auth primitives plus Posts until #170.
require_all(client, "NovaApiClient shared boundary", (
    'data class NovaPostAuthor(',
    'data class UploadFile(',
    'sealed interface ApiResult<out T>',
    'class NovaApiClient(',
    'private val baseUrl: String = "http://127.0.0.1:8000/api/v1/"',
    'import com.nova.app.feature.posts.data.parseNovaComment',
    'import com.nova.app.feature.posts.data.parseNovaPost',
    'import com.nova.app.feature.posts.data.parseNovaPostPage',
    'suspend fun refresh(refreshToken: String): ApiResult<String>',
    'JSONObject().put("refresh", refreshToken)',
    'requestJson("auth/refresh/", "POST", body)',
    'ApiResult.Failure("Nova returned an invalid session response.")',
))
forbid_all(client, "feature-owned core network seam", (
    'data class NovaUser(',
    'data class AuthSession(',
    'suspend fun register(',
    'suspend fun login(',
    'suspend fun me(',
    'suspend fun updateProfile(',
    'suspend fun people(',
    'suspend fun person(',
    'suspend fun personPosts(',
    'suspend fun setFollowing(',
    'suspend fun setBlocked(',
    'suspend fun reportPerson(',
    'import com.nova.app.feature.people.data.parseNovaPerson',
    'import com.nova.app.feature.posts.data.parseNovaPosts',
    'private fun parsePerson(',
    'private fun parsePostAuthor(',
    'private fun parsePosts(',
    'private fun parsePostPage(',
    'private fun parsePost(',
    'private fun parseComment(',
))

# Auth models/parser/remote remain authoritative. Core refresh is intentionally an auth primitive.
require_all(auth_models, "feature Auth models", (
    'package com.nova.app.feature.auth.domain.model',
    'data class NovaUser(',
    'data class AuthSession(',
    'val accessToken: String',
    'val refreshToken: String',
    'val user: NovaUser',
))
require_all(auth_parser, "feature Auth parser", (
    'internal fun parseAuthSession(',
    'internal fun parseNovaUser(',
    'if (access.isBlank() || refresh.isBlank() || userJson == null)',
    'ApiResult.Failure("Nova returned an invalid authentication response.")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
))
require_all(auth_remote, "feature Auth remote", (
    'class AuthRemoteDataSource(',
    'api.requestJson("auth/register/", "POST", body)',
    'api.requestJson("auth/login/", "POST", body)',
    'api.requestJson("me/", bearerToken = accessToken)',
    'path = "me/"',
    'method = "PUT"',
    'fileField = "avatar"',
    'api.requestJson("auth/refresh/", "POST", body)',
    'ApiResult.Failure("Nova returned an invalid session response.")',
))
require_all(auth_repository, "NovaAuthRepository feature remote ownership", (
    'private val remote: AuthRemoteDataSource',
    'remote.register(',
    'remote.login(',
    'remote.me(',
    'remote.updateProfile(',
    'remote.refresh(',
))

# People model parsing and all direct People endpoints are feature-owned.
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
require_all(people_remote, "feature People remote", (
    'class PeopleRemoteDataSource(',
    'private val api: NovaApiClient',
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
    'add(parseNovaPerson(it, api::resolveMediaUrl))',
    'parseNovaPerson(response.value, api::resolveMediaUrl)',
    'parseNovaPosts(response.value, api::resolveMediaUrl)',
    'URLEncoder.encode(value, Charsets.UTF_8.name())',
))
require_all(social_repository, "NovaSocialRepository People remote ownership", (
    'private val peopleRemote = PeopleRemoteDataSource(api)',
    'private val authRemote = AuthRemoteDataSource(api)',
    'peopleRemote.people(accessToken, query)',
    'peopleRemote.person(accessToken, username)',
    'peopleRemote.setFollowing(accessToken, username, follow)',
    'peopleRemote.setBlocked(accessToken, username, blocked)',
    'peopleRemote.reportPerson(accessToken, username, reason, details)',
    'authRemote.refresh(stored.refreshToken)',
))
forbid_all(social_repository, "NovaSocialRepository superseded People client seam", (
    'api.people(',
    'api.person(',
    'api.setFollowing(',
    'api.setBlocked(',
    'api.reportPerson(',
))

# Paging stays byte-for-byte compatible in transport semantics while living in feature People.
require_all(people_paging, "feature People paging transport", (
    'class PeoplePagingRemoteRepository(',
    ') : PeoplePagingRepository',
    'path = "people/"',
    'path = "people/${encode(username.trim().lowercase())}/followers/"',
    'path = "people/${encode(username.trim().lowercase())}/following/"',
    'path = "people/${encode(username.trim().lowercase())}/posts/"',
    'path = "people/${encode(username.trim().lowercase())}/reposts/"',
    'connectTimeout = 12_000',
    'readTimeout = 15_000',
    'requestMethod = "GET"',
    'setRequestProperty("Accept", "application/json")',
    'setRequestProperty("Authorization", "Bearer $bearerToken")',
    '401 -> "Your session expired. Please log in again."',
    '403 -> "Follow this private account to see this content."',
    '404 -> "Nova couldn\'t find that profile."',
    'in 500..599 -> "Nova\'s server had a problem. Try again in a moment."',
    'ApiResult.Failure("Can\'t reach Nova right now. Check your connection and try again.")',
    'val person = parseNovaPerson(item, ::resolveMediaUrl)',
    'add(parseNovaPost(it, ::resolveMediaUrl))',
    'nextCursor = optionalString(response.value, "next_cursor")',
    'isPrivate = json.optBoolean("is_private", false)',
    'followRequested = json.optBoolean("follow_requested", false)',
    'canViewContent = json.optBoolean("can_view_content", true)',
    'authRemote.refresh(stored.refreshToken)',
))
forbid_all(people_paging, "feature People paging duplicate parsers", (
    'private fun parsePerson(',
    'private fun parsePost(',
    'import com.nova.app.core.network.NovaPostAuthor',
))

# Posts ownership remains frozen for #170.
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
    'parseNovaPostPage(response.value, ::resolveMediaUrl)',
    'parseNovaPost(response.value, ::resolveMediaUrl)',
    'add(parseNovaComment(it, ::resolveMediaUrl))',
))

# Shared HTTP/media/error behavior remains protected.
require_all(client, "network URL/media behavior", (
    'internal fun resolveMediaUrl(raw: String): String',
    'if (raw.isBlank() || raw == "null") return ""',
    'if (raw.startsWith("http://") || raw.startsWith("https://")) return raw',
    'URL("${apiUrl.protocol}://${apiUrl.authority}$raw").toString()',
    'URLEncoder.encode(value, Charsets.UTF_8.name())',
))
require_all(client, "JSON transport", (
    'internal suspend fun requestJson(',
    'withContext(Dispatchers.IO)',
    'connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply',
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
    'connectTimeout = 20_000',
    'readTimeout = 20_000',
    'setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")',
    'output.write(file.bytes)',
    'output.writeBytes("--$boundary--$lineEnd")',
    'message = "Nova couldn\'t upload that right now. Check your connection and try again."',
))
require_all(client, "network response/error behavior", (
    'val status = connection.responseCode',
    'val stream = if (status in 200..299) connection.inputStream else connection.errorStream',
    '.getOrElse { JSONObject().put("detail", raw) }',
    'message = parseError(json, status)',
    'statusCode = status',
    'return if (statusCode == 401) "Email or password is incorrect." else detail',
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

if errors:
    print("Network architecture characterization failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Network architecture characterization passed.")
