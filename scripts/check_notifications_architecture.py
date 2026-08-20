#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/notifications/domain/model/NotificationModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/notifications/data/NotificationsRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/notifications/data/remote/CoreNotificationsRepositoryAdapter.kt"
MAPPING_TEST = ROOT / "app/src/test/java/com/nova/app/feature/notifications/CoreNotificationsRepositoryAdapterTest.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/notifications/NotificationsStateOwner.kt"
OWNER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/notifications/NotificationsStateOwnerTest.kt"
PRIVACY_MODEL = ROOT / "app/src/main/java/com/nova/app/feature/privacy/domain/model/FollowRequestModels.kt"
PRIVACY_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/FollowRequestRepository.kt"
PRIVACY_ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/remote/CoreFollowRequestRepositoryAdapter.kt"
PRIVACY_MAPPING_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/CoreFollowRequestRepositoryAdapterTest.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/notifications/NovaNotificationRepository.kt"
CORE_PRIVACY = ROOT / "app/src/main/java/com/nova/app/core/privacy/NovaPrivacyRepository.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/notifications/NotificationsScreen.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Notifications architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
adapter = read(ADAPTER)
mapping_test = read(MAPPING_TEST)
owner = read(OWNER)
owner_test = read(OWNER_TEST)
privacy_model = read(PRIVACY_MODEL)
privacy_contract = read(PRIVACY_CONTRACT)
privacy_adapter = read(PRIVACY_ADAPTER)
privacy_mapping_test = read(PRIVACY_MAPPING_TEST)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
core_privacy = read(CORE_PRIVACY)
screen = read(SCREEN)

for required in (
    "data class NovaNotification(",
    "val id: Long",
    "val kind: String",
    "val actor: NovaPostAuthor",
    "val postId: Long?",
    "val reelId: Long?",
    "val reelAuthorUsername: String",
    "val commentPreview: String",
    "val createdAt: String",
    "val isRead: Boolean",
    "data class NovaNotificationPage(",
    "val notifications: List<NovaNotification>",
    "val nextCursor: String?",
    "val unreadCount: Int",
):
    if required not in models:
        errors.append(f"stable Notifications models are missing field/seam: {required}")

for forbidden in (
    "NovaFollowRequest",
    "NovaPrivacyRepository",
    "FollowRequestRepository",
):
    if forbidden in models or forbidden in contract:
        errors.append(f"Notifications data boundary must not absorb Privacy ownership: {forbidden}")

if "interface NotificationsRepository" not in contract:
    errors.append("stable NotificationsRepository contract is missing")
for operation in (
    "suspend fun notifications(cursor: String? = null): ApiResult<NovaNotificationPage>",
    "suspend fun markAllRead(): ApiResult<Int>",
):
    if operation not in contract:
        errors.append(f"stable NotificationsRepository is missing operation: {operation}")

if "class CoreNotificationsRepositoryAdapter(context: Context) : NotificationsRepository" not in adapter:
    errors.append("Notifications production adapter must implement NotificationsRepository")
if "private val delegate = NovaNotificationRepository(context.applicationContext)" not in adapter:
    errors.append("Notifications adapter must delegate to the existing production repository")
for required in (
    "delegate.notifications(cursor)",
    "result.value.toStableNotificationPage()",
    "delegate.markAllRead()",
    "internal fun CoreNotification.toStableNotification()",
    "internal fun CoreNotificationPage.toStableNotificationPage()",
    "notifications = notifications.map(CoreNotification::toStableNotification)",
):
    if required not in adapter:
        errors.append(f"Notifications adapter is missing stable mapping/delegation seam: {required}")

if "val notificationsRepository: NotificationsRepository = CoreNotificationsRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer is missing the stable Notifications repository seam")

for test_name in (
    "notification mapping preserves every live field",
    "page mapping preserves order cursor unread count and nullable targets",
):
    if test_name not in mapping_test:
        errors.append(f"Notifications mapping characterization is missing test: {test_name}")

# Follow requests are a narrow Privacy-owned dependency of the Notifications owner.
for required in (
    "data class NovaFollowRequest(",
    "val requester: NovaPerson",
    "val createdAt: String",
):
    if required not in privacy_model:
        errors.append(f"stable Privacy follow-request model is missing: {required}")
if "import com.nova.app.feature.people.domain.model.NovaPerson" not in privacy_model:
    errors.append("stable follow-request model must use the feature-owned People model")
if "interface FollowRequestRepository" not in privacy_contract:
    errors.append("Privacy-owned FollowRequestRepository contract is missing")
for operation in (
    "suspend fun followRequests(): ApiResult<List<NovaFollowRequest>>",
    "suspend fun acceptFollowRequest(requestId: Long): ApiResult<Unit>",
    "suspend fun declineFollowRequest(requestId: Long): ApiResult<Unit>",
):
    if operation not in privacy_contract:
        errors.append(f"FollowRequestRepository is missing operation: {operation}")
if "class CoreFollowRequestRepositoryAdapter(context: Context) : FollowRequestRepository" not in privacy_adapter:
    errors.append("Privacy follow-request adapter must implement FollowRequestRepository")
for required in (
    "private val delegate = NovaPrivacyRepository(context.applicationContext)",
    "delegate.followRequests()",
    "delegate.acceptFollowRequest(requestId)",
    "delegate.declineFollowRequest(requestId)",
    "internal fun CoreFollowRequest.toStableFollowRequest()",
):
    if required not in privacy_adapter:
        errors.append(f"Privacy follow-request adapter is missing seam: {required}")
if "val followRequestRepository: FollowRequestRepository = CoreFollowRequestRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer is missing the Privacy-owned follow-request seam")
if "follow request mapping preserves id requester and created at" not in privacy_mapping_test:
    errors.append("follow-request field mapping characterization is missing")

# Characterize the full async Activity state before switching the live Compose screen.
for required in (
    "data class NotificationsUiState(",
    "val notifications: List<NovaNotification>",
    "val followRequests: List<NovaFollowRequest>",
    "val nextCursor: String?",
    "val isLoading: Boolean = true",
    "val isLoadingMore: Boolean = false",
    "val requestsLoading: Boolean = true",
    "val requestBusyId: Long?",
    "val openingPostId: Long?",
    "val errorMessage: String?",
    "val requestError: String?",
    "sealed interface NotificationOpenTarget",
    "private val notificationsRepository: NotificationsRepository",
    "private val followRequestRepository: FollowRequestRepository",
    "private val postRepository: PostRepository",
    "private val onUnreadCountChanged: (Int) -> Unit",
    "private val onSessionExpired: () -> Unit",
    "private val onPostOpened: (NovaPost) -> Unit",
    "if (state.isLoadingMore || (reset && state.isLoading && state.notifications.isNotEmpty())) return",
    "if (reset) loadFollowRequests()",
    "val existingIds = state.notifications.mapTo(mutableSetOf()) { it.id }",
    "state.notifications + page.notifications.filterNot { it.id in existingIds }",
    "onUnreadCountChanged(page.unreadCount)",
    "if (reset && page.unreadCount > 0)",
    "notificationsRepository.markAllRead()",
    "if (readResult.statusCode == 401) onSessionExpired()",
    "if (state.requestBusyId != null) return",
    "if (state.openingPostId != null) return",
    "state = state.copy(openingPostId = null)",
    "onPostOpened(result.value)",
    "lastVisible >= totalItems - LOAD_MORE_THRESHOLD",
    "internal const val LOAD_MORE_THRESHOLD = 4",
    '"follow" -> NotificationOpenTarget.Person(notification.actor.username)',
    '"like", "comment", "comment_reply"',
    '"reel_like", "reel_comment", "reel_repost", "reel_reply"',
    "username = notification.reelAuthorUsername.trim().lowercase()",
):
    if required not in owner:
        errors.append(f"Notifications state owner is missing characterized seam: {required}")

for forbidden in (
    "NovaNotificationRepository",
    "NovaPrivacyRepository",
    "NovaFeedRepository",
    "NovaReelsNavigator",
    "LocalContext",
):
    if forbidden in owner:
        errors.append(f"NotificationsStateOwner must depend only on stable/data seams: {forbidden}")

for test_name in (
    "reset loads follow requests then activity and marks unread notifications read",
    "load more filters preexisting ids only and preserves duplicates inside incoming page",
    "activity failures keep non401 inline and report 401 as terminal",
    "mark all read ignores non401 failure but reports 401 terminal",
    "follow request failures preserve inline versus terminal 401 semantics",
    "follow request decision uses one global busy id and removes success",
    "open post clears busy before success callback and keeps failure semantics",
    "load more threshold stays four items from the end",
    "notification click routing preserves post reel and actor fallbacks",
):
    if test_name not in owner_test:
        errors.append(f"Notifications state characterization is missing test: {test_name}")

# Preserve the current production notification routes/parser/auth/error behavior.
for required in (
    "data class NovaNotification(",
    "data class NovaNotificationPage(",
    "class NovaNotificationRepository(",
    "class NovaNotificationApiClient(",
    '"notifications/"',
    '"notifications/?cursor=${encode(cursor)}"',
    'path = "notifications/read/"',
    'method = "POST"',
    'json.optString("kind")',
    'json.opt("post_id")',
    'json.opt("reel_id")',
    'json.optString("reel_author_username")',
    'json.optString("comment_preview")',
    'json.optString("created_at")',
    'json.optBoolean("is_read", false)',
    "connectTimeout = 10_000",
    "readTimeout = 10_000",
):
    if required not in core_repository:
        errors.append(f"Notifications transport-sensitive seam changed or disappeared: {required}")

# Preserve the current Privacy follow-request transport while exposing the narrow adapter.
for required in (
    'requestJson("follow-requests/", bearerToken = token)',
    'requestDecision(requestId, "accept")',
    'requestDecision(requestId, "decline")',
    'path = "follow-requests/$requestId/$action/"',
    'method = "POST"',
):
    if required not in core_privacy:
        errors.append(f"Privacy follow-request transport-sensitive seam changed or disappeared: {required}")

# Characterization PR intentionally leaves the live screen on concrete/local-state wiring.
for required in (
    "import com.nova.app.core.notifications.NovaNotification",
    "import com.nova.app.core.notifications.NovaNotificationRepository",
    "NovaNotificationRepository(context.applicationContext)",
    "NovaPrivacyRepository(context.applicationContext)",
    "NovaFeedRepository(context.applicationContext)",
    "mutableStateOf<List<NovaNotification>>(emptyList())",
    "repository.notifications(cursor)",
    "repository.markAllRead()",
    "privacyRepository.followRequests()",
    "feedRepository.post(postId)",
):
    if required not in screen:
        errors.append(f"Notifications characterization PR must preserve current live-screen wiring: {required}")

for forbidden in (
    "NotificationsStateOwner",
    "NotificationsRepository",
    "FollowRequestRepository",
    "context.appContainer.notificationsRepository",
    "context.appContainer.followRequestRepository",
):
    if forbidden in screen:
        errors.append(f"Notifications characterization PR must not switch the live screen yet: {forbidden}")

if errors:
    print("Notifications architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Notifications architecture check passed.")
