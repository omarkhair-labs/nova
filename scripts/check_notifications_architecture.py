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
owner = read(OWNER)
owner_test = read(OWNER_TEST)
privacy_model = read(PRIVACY_MODEL)
privacy_contract = read(PRIVACY_CONTRACT)
privacy_adapter = read(PRIVACY_ADAPTER)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
core_privacy = read(CORE_PRIVACY)
screen = read(SCREEN)

# Stable Notifications models and repository contract are the single feature boundary.
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

if "interface NotificationsRepository" not in contract:
    errors.append("stable NotificationsRepository contract is missing")
for operation in (
    "suspend fun notifications(cursor: String? = null): ApiResult<NovaNotificationPage>",
    "suspend fun markAllRead(): ApiResult<Int>",
):
    if operation not in contract:
        errors.append(f"stable NotificationsRepository is missing operation: {operation}")

for forbidden in ("NovaFollowRequest", "NovaPrivacyRepository", "FollowRequestRepository"):
    if forbidden in models or forbidden in contract:
        errors.append(f"Notifications data boundary must not absorb Privacy ownership: {forbidden}")

# Exit gate: production implements the stable contract directly; temporary bridge is gone.
if ADAPTER.exists():
    errors.append("Notifications exit gate forbids restoring CoreNotificationsRepositoryAdapter.kt")
if MAPPING_TEST.exists():
    errors.append("Notifications exit gate forbids restoring the obsolete adapter mapping test")

for required in (
    "import com.nova.app.feature.notifications.data.NotificationsRepository",
    "import com.nova.app.feature.notifications.domain.model.NovaNotification",
    "import com.nova.app.feature.notifications.domain.model.NovaNotificationPage",
    ") : NotificationsRepository",
    "override suspend fun notifications(cursor: String?): ApiResult<NovaNotificationPage>",
    "override suspend fun markAllRead(): ApiResult<Int>",
):
    if required not in core_repository:
        errors.append(f"production Notifications repository is missing direct stable ownership seam: {required}")

for forbidden in (
    "data class NovaNotification(",
    "data class NovaNotificationPage(",
    "CoreNotificationsRepositoryAdapter",
    "toStableNotification",
    "toStableNotificationPage",
):
    if forbidden in core_repository:
        errors.append(f"core Notifications repository restored duplicate/adapter ownership: {forbidden}")

if "import com.nova.app.core.notifications.NovaNotificationRepository" not in container:
    errors.append("AppContainer must import the direct production Notifications repository")
if "val notificationsRepository: NotificationsRepository = NovaNotificationRepository(appContext)" not in container:
    errors.append("AppContainer must construct NovaNotificationRepository directly behind NotificationsRepository")
if "CoreNotificationsRepositoryAdapter" in container:
    errors.append("AppContainer must not restore the temporary Notifications adapter")

# Preserve exact notification transport/parser/auth behavior while changing only type ownership.
for required in (
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
    "authApi.refresh(stored.refreshToken)",
    "sessionStore.updateAccessToken(refreshed.value)",
    "if (retried.statusCode == 401) sessionStore.clear()",
):
    if required not in core_repository:
        errors.append(f"Notifications transport-sensitive seam changed or disappeared: {required}")

# Follow requests remain a narrow Privacy-owned dependency; do not close Privacy early.
for required in (
    "data class NovaFollowRequest(",
    "val requester: NovaPerson",
    "val createdAt: String",
):
    if required not in privacy_model:
        errors.append(f"stable Privacy follow-request model is missing: {required}")
if "interface FollowRequestRepository" not in privacy_contract:
    errors.append("Privacy-owned FollowRequestRepository contract is missing")
if "class CoreFollowRequestRepositoryAdapter(context: Context) : FollowRequestRepository" not in privacy_adapter:
    errors.append("Privacy follow-request adapter must remain until the Privacy slice")
if "val followRequestRepository: FollowRequestRepository = CoreFollowRequestRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer must retain the Privacy-owned follow-request seam")
for required in (
    'requestJson("follow-requests/", bearerToken = token)',
    'requestDecision(requestId, "accept")',
    'requestDecision(requestId, "decline")',
    'path = "follow-requests/$requestId/$action/"',
    'method = "POST"',
):
    if required not in core_privacy:
        errors.append(f"Privacy follow-request transport-sensitive seam changed or disappeared: {required}")

# Characterized owner remains the sole Notifications async server-state owner.
for required in (
    "data class NotificationsUiState(",
    "sealed interface NotificationOpenTarget",
    "private val notificationsRepository: NotificationsRepository",
    "private val followRequestRepository: FollowRequestRepository",
    "private val postRepository: PostRepository",
    "val existingIds = state.notifications.mapTo(mutableSetOf()) { it.id }",
    "state.notifications + page.notifications.filterNot { it.id in existingIds }",
    "notificationsRepository.markAllRead()",
    "if (readResult.statusCode == 401) onSessionExpired()",
    "if (state.requestBusyId != null) return",
    "if (state.openingPostId != null) return",
    "onPostOpened(result.value)",
    "lastVisible >= totalItems - LOAD_MORE_THRESHOLD",
    "internal const val LOAD_MORE_THRESHOLD = 4",
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

# Live screen stays render/navigation-only over AppContainer-backed state ownership.
for required in (
    "import com.nova.app.app.appContainer",
    "import com.nova.app.feature.notifications.domain.model.NovaNotification",
    "import com.nova.app.feature.posts.domain.model.NovaPost",
    "import com.nova.app.feature.privacy.domain.model.NovaFollowRequest",
    "val appContainer = context.appContainer",
    "NotificationsStateOwner(",
    "notificationsRepository = appContainer.notificationsRepository",
    "followRequestRepository = appContainer.followRequestRepository",
    "postRepository = appContainer.postDataRepository",
    "val state = owner.state",
    "owner.shouldLoadMore(lastVisible, totalItems)",
    "owner.loadActivity(reset = false)",
    "isRefreshing = state.isRefreshing",
    "onRefresh = { owner.loadActivity(reset = true) }",
    "when (val target = owner.openTarget(notification))",
    "is NotificationOpenTarget.Reel -> NovaReelsNavigator.openProfile(",
    "preview.take(90)",
):
    if required not in screen:
        errors.append(f"live Notifications screen is missing stable rendering/navigation seam: {required}")

for forbidden in (
    "import com.nova.app.core.feed.NovaFeedRepository",
    "import com.nova.app.core.network.ApiResult",
    "import com.nova.app.core.notifications.NovaNotification",
    "import com.nova.app.core.notifications.NovaNotificationRepository",
    "import com.nova.app.core.privacy.NovaFollowRequest",
    "import com.nova.app.core.privacy.NovaPrivacyRepository",
    "mutableStateOf",
    "scope.launch",
    "repository.notifications(",
    "privacyRepository.followRequests()",
    "feedRepository.post(",
):
    if forbidden in screen:
        errors.append(f"live Notifications screen restored concrete/manual async ownership: {forbidden}")

if errors:
    print("Notifications architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Notifications architecture check passed.")
