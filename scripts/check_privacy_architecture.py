#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/privacy/domain/model/PrivacyModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/PrivacyRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/remote/CorePrivacyRepositoryAdapter.kt"
MAPPING_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/CorePrivacyRepositoryAdapterTest.kt"
FOLLOW_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/FollowRequestRepository.kt"
FOLLOW_ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/remote/CoreFollowRequestRepositoryAdapter.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/PrivacyStateOwner.kt"
OWNER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/PrivacyStateOwnerTest.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/privacy/NovaPrivacyRepository.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/privacy/PrivacyScreen.kt"
PERSON_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/PersonScreen.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Privacy architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
adapter = read(ADAPTER)
mapping_test = read(MAPPING_TEST)
follow_contract = read(FOLLOW_CONTRACT)
follow_adapter = read(FOLLOW_ADAPTER)
owner = read(OWNER)
owner_test = read(OWNER_TEST)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
screen = read(SCREEN)
person_screen = read(PERSON_SCREEN)

# Stable Privacy-owned models exclude the already-separated follow-request record.
for required in (
    "data class NovaPersonPrivacyState(",
    "val isPrivate: Boolean",
    "val followRequested: Boolean",
    "val canViewContent: Boolean",
    "data class NovaPrivacySummary(",
    "val pendingFollowRequests: Int",
    "val closeFriendsCount: Int",
    "val acceptedPendingRequests: Int = 0",
):
    if required not in models:
        errors.append(f"stable Privacy models are missing field/seam: {required}")
if "NovaFollowRequest" in models:
    errors.append("PrivacyModels.kt must not duplicate the existing follow-request model")

if "interface PrivacyRepository" not in contract:
    errors.append("stable PrivacyRepository contract is missing")
for operation in (
    "suspend fun summary(): ApiResult<NovaPrivacySummary>",
    "suspend fun setPrivate(isPrivate: Boolean): ApiResult<NovaPrivacySummary>",
    "suspend fun personState(username: String): ApiResult<NovaPersonPrivacyState>",
    "suspend fun closeFriends(): ApiResult<List<NovaPerson>>",
    "suspend fun setCloseFriend(username: String, enabled: Boolean): ApiResult<Unit>",
):
    if operation not in contract:
        errors.append(f"stable PrivacyRepository is missing operation: {operation}")
for forbidden in ("followRequests", "acceptFollowRequest", "declineFollowRequest"):
    if forbidden in contract:
        errors.append(f"PrivacyRepository must not absorb the separate FollowRequestRepository operation: {forbidden}")

# Production remains adapter-backed until all Privacy consumers switch.
if "class CorePrivacyRepositoryAdapter(context: Context) : PrivacyRepository" not in adapter:
    errors.append("Privacy production adapter must implement PrivacyRepository")
for required in (
    "private val delegate = NovaPrivacyRepository(context.applicationContext)",
    "delegate.summary()",
    "delegate.setPrivate(isPrivate)",
    "delegate.personState(username)",
    "delegate.closeFriends()",
    "delegate.setCloseFriend(username, enabled)",
    "internal fun CorePrivacySummary.toStablePrivacySummary()",
    "internal fun CorePersonPrivacyState.toStablePersonPrivacyState()",
):
    if required not in adapter:
        errors.append(f"Privacy adapter is missing mapping/delegation seam: {required}")

if "val privacyRepository: PrivacyRepository = CorePrivacyRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer is missing the stable Privacy repository seam")
if "fun currentCachedUsername(): String = sessionStore.load()?.cachedUser?.username.orEmpty()" not in container:
    errors.append("AppContainer is missing the explicit cached-username seam for the later live switch")

for test_name in (
    "privacy summary mapping preserves every live field",
    "person privacy mapping preserves private request and content state",
):
    if test_name not in mapping_test:
        errors.append(f"Privacy mapping characterization is missing test: {test_name}")

# Follow requests remain their own Privacy-owned contract, shared with Notifications.
if "interface FollowRequestRepository" not in follow_contract:
    errors.append("existing Privacy-owned FollowRequestRepository must remain")
for operation in ("followRequests", "acceptFollowRequest", "declineFollowRequest"):
    if operation not in follow_contract:
        errors.append(f"FollowRequestRepository lost operation: {operation}")
if "class CoreFollowRequestRepositoryAdapter(context: Context) : FollowRequestRepository" not in follow_adapter:
    errors.append("existing follow-request production adapter must remain during Privacy characterization")
if "val followRequestRepository: FollowRequestRepository = CoreFollowRequestRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer must keep the independent follow-request seam")

# Characterize the PrivacyScreen state/orchestration before switching the live UI.
for required in (
    "data class PrivacyUiState(",
    "val summary: NovaPrivacySummary? = null",
    "val requests: List<NovaFollowRequest> = emptyList()",
    "val closeFriends: List<NovaPerson> = emptyList()",
    "val followers: List<NovaPerson> = emptyList()",
    "val followerCursor: String? = null",
    "val followerQuery: String = \"\"",
    "val loading: Boolean = true",
    "val loadingFollowers: Boolean = false",
    "val loadingMore: Boolean = false",
    "val privacyBusy: Boolean = false",
    "val requestBusyId: Long? = null",
    "val closeFriendBusyId: Long? = null",
    "val error: String? = null",
    "val feedback: String? = null",
    "class PrivacyStateOwner(",
    "private val username: String",
    "private val privacyRepository: PrivacyRepository",
    "private val followRequestRepository: FollowRequestRepository",
    "private val peoplePagingRepository: PeoplePagingRepository",
    "private val scope: CoroutineScope",
    "private val onSessionExpired: () -> Unit",
    "fun start()",
    "loadSummaryBundle()",
    "loadFollowers(reset = true)",
    "scheduleFollowerQueryLoad()",
    "privacyRepository.summary()",
    "followRequestRepository.followRequests()",
    "privacyRepository.closeFriends()",
    "raw.take(FOLLOWER_QUERY_MAX_LENGTH)",
    "delay(FOLLOWER_SEARCH_DEBOUNCE_MS)",
    "query = state.followerQuery.trim()",
    "val existingIds = state.followers.mapTo(mutableSetOf()) { it.id }",
    "state.followers + page.people.filterNot { it.id in existingIds }",
    "if (state.privacyBusy) return",
    "if (summary.acceptedPendingRequests > 0)",
    "pending follow requests were accepted.",
    "if (state.requestBusyId != null) return",
    "if (accept) loadFollowers(reset = true)",
    "Follow request declined.",
    "if (state.closeFriendBusyId != null) return",
    "Added @${person.username} to Close Friends.",
    "Removed @${person.username} from Close Friends.",
    "if (result.statusCode == 401)",
    "onSessionExpired()",
    "internal const val FOLLOWER_SEARCH_DEBOUNCE_MS = 280L",
    "internal const val FOLLOWER_QUERY_MAX_LENGTH = 50",
):
    if required not in owner:
        errors.append(f"Privacy state characterization is missing seam: {required}")

for test_name in (
    "summary bundle keeps summary requests close friends order",
    "summary bundle reports 401 terminal but continues remaining bundle calls",
    "followers paging trims query and preserves duplicates inside incoming page",
    "followers failures keep non401 inline and report 401 terminal",
    "private toggle accepted pending clears requests and refreshes followers",
    "follow request decision keeps one global busy id and refreshes only accept",
    "close friend toggle keeps one global busy id and updates summary count",
    "follower query keeps 50 character cap and 280 ms debounce contract",
    "load more without cursor stays a no op",
):
    if test_name not in owner_test:
        errors.append(f"Privacy state-owner characterization is missing test: {test_name}")

# Preserve the exact current Privacy transport/auth/parser behavior.
for required in (
    "data class NovaPersonPrivacyState(",
    "data class NovaPrivacySummary(",
    "data class NovaFollowRequest(",
    "class NovaPrivacyRepository(",
    'requestJson("privacy/", bearerToken = token)',
    'path = "privacy/"',
    'JSONObject().put("is_private", isPrivate)',
    'path = "people/${encode(username.trim().lowercase())}/"',
    'requestJson("follow-requests/", bearerToken = token)',
    'requestDecision(requestId, "accept")',
    'requestDecision(requestId, "decline")',
    'requestJson("close-friends/", bearerToken = token)',
    'path = "close-friends/"',
    'body = JSONObject().put("username", clean)',
    'path = "close-friends/${encode(clean)}/"',
    'method = "DELETE"',
    'json.optBoolean("is_private", false)',
    'json.optInt("pending_follow_requests", 0)',
    'json.optInt("close_friends_count", 0)',
    'json.optInt("accepted_pending_requests", 0)',
    'json.optBoolean("follow_requested", false)',
    'json.optBoolean("can_view_content", true)',
    "connectTimeout = 12_000",
    "readTimeout = 20_000",
    "authApi.refresh(stored.refreshToken)",
    "if (retried.statusCode == 401) sessionStore.clear()",
):
    if required not in core_repository:
        errors.append(f"Privacy transport-sensitive seam changed or disappeared: {required}")

# Characterization PR intentionally leaves live PrivacyScreen behavior untouched.
for required in (
    "import com.nova.app.core.auth.NovaSessionStore",
    "import com.nova.app.core.network.ApiResult",
    "import com.nova.app.core.privacy.NovaFollowRequest",
    "import com.nova.app.core.privacy.NovaPrivacyRepository",
    "import com.nova.app.core.privacy.NovaPrivacySummary",
    "import com.nova.app.core.social.NovaSocialPagingRepository",
    "NovaPrivacyRepository(context.applicationContext)",
    "NovaSocialPagingRepository(context.applicationContext)",
    "NovaSessionStore(context.applicationContext).load()?.cachedUser?.username.orEmpty()",
    "mutableStateOf<NovaPrivacySummary?>(null)",
    "mutableStateOf<List<NovaFollowRequest>>(emptyList())",
    "privacyRepository.summary()",
    "privacyRepository.followRequests()",
    "privacyRepository.closeFriends()",
    "privacyRepository.setPrivate(enabled)",
    "privacyRepository.setCloseFriend(person.username, !currentlyClose)",
    "delay(280)",
    "followerQuery = it.take(50)",
    'text = if (loadingMore) "Loading more…" else "Load more followers"',
):
    if required not in screen:
        errors.append(f"Privacy characterization PR must preserve current live-screen wiring/behavior: {required}")
for forbidden in (
    "PrivacyStateOwner(",
    "context.appContainer.privacyRepository",
    "import com.nova.app.feature.privacy.data.PrivacyRepository",
    "CorePrivacyRepositoryAdapter",
):
    if forbidden in screen:
        errors.append(f"Privacy characterization PR must not switch live PrivacyScreen yet: {forbidden}")

# PersonScreen is another live consumer of personState and remains untouched until its focused Privacy residual switch.
for required in (
    "import com.nova.app.core.privacy.NovaPersonPrivacyState",
    "import com.nova.app.core.privacy.NovaPrivacyRepository",
    "NovaPrivacyRepository(context.applicationContext)",
    "privacyRepository.personState(selected.username)",
):
    if required not in person_screen:
        errors.append(f"Privacy characterization PR must preserve current PersonScreen privacy wiring: {required}")
for forbidden in (
    "context.appContainer.privacyRepository",
    "import com.nova.app.feature.privacy.data.PrivacyRepository",
    "CorePrivacyRepositoryAdapter",
):
    if forbidden in person_screen:
        errors.append(f"Privacy characterization PR must not switch PersonScreen yet: {forbidden}")

if errors:
    print("Privacy architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Privacy architecture check passed.")
