#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/notifications/domain/model/NotificationModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/notifications/data/NotificationsRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/notifications/data/remote/CoreNotificationsRepositoryAdapter.kt"
STATE_TEST = ROOT / "app/src/test/java/com/nova/app/feature/notifications/CoreNotificationsRepositoryAdapterTest.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/notifications/NovaNotificationRepository.kt"
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
state_test = read(STATE_TEST)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
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
    if test_name not in state_test:
        errors.append(f"Notifications mapping characterization is missing test: {test_name}")

# Preserve the current production routes/parser/auth/error behavior in this boundary-only slice.
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

# Boundary PR intentionally leaves the live screen on its current concrete/local-state wiring.
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
        errors.append(f"Notifications boundary PR must preserve current live-screen wiring: {required}")

for forbidden in (
    "NotificationsRepository",
    "CoreNotificationsRepositoryAdapter",
    "context.appContainer.notificationsRepository",
):
    if forbidden in screen:
        errors.append(f"Notifications boundary PR must not switch the live screen yet: {forbidden}")

if errors:
    print("Notifications architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Notifications architecture check passed.")
