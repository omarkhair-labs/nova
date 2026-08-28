#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/privacy/domain/model/PrivacyModels.kt"
FOLLOW_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/privacy/domain/model/FollowRequestModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/PrivacyRepository.kt"
FOLLOW_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/FollowRequestRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/PrivacyStateOwner.kt"
OWNER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/PrivacyStateOwnerTest.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/privacy/NovaPrivacyRepository.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/privacy/PrivacyScreen.kt"
PERSON_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/PersonScreen.kt"
PRIVATE_BADGE = ROOT / "app/src/main/java/com/nova/app/feature/people/PrivateProfileBadge.kt"
PRIVATE_BADGE_TEST = ROOT / "app/src/test/java/com/nova/app/feature/people/PrivateProfileBadgeTest.kt"
PEOPLE_MODELS = ROOT / "app/src/main/java/com/nova/app/feature/people/domain/model/PeopleModels.kt"
PEOPLE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/PeopleStateOwner.kt"
CONNECTIONS_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/SocialConnectionsStateOwner.kt"
PEOPLE_PAGING = ROOT / "app/src/main/java/com/nova/app/feature/people/data/remote/PeoplePagingRemoteRepository.kt"
PEOPLE_OWNER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/people/PeopleStateOwnersTest.kt"
PRIVACY_ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/remote/CorePrivacyRepositoryAdapter.kt"
FOLLOW_ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/privacy/data/remote/CoreFollowRequestRepositoryAdapter.kt"
PRIVACY_MAPPING_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/CorePrivacyRepositoryAdapterTest.kt"
FOLLOW_MAPPING_TEST = ROOT / "app/src/test/java/com/nova/app/feature/privacy/CoreFollowRequestRepositoryAdapterTest.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Privacy architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
follow_models = read(FOLLOW_MODELS)
contract = read(CONTRACT)
follow_contract = read(FOLLOW_CONTRACT)
owner = read(OWNER)
owner_test = read(OWNER_TEST)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
screen = read(SCREEN)
person_screen = read(PERSON_SCREEN)
private_badge = read(PRIVATE_BADGE)
private_badge_test = read(PRIVATE_BADGE_TEST)
people_models = read(PEOPLE_MODELS)
people_owner = read(PEOPLE_OWNER)
connections_owner = read(CONNECTIONS_OWNER)
people_paging = read(PEOPLE_PAGING)
people_owner_test = read(PEOPLE_OWNER_TEST)

# Stable Privacy owns all Privacy records. Follow requests stay a separate narrow contract.
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
    errors.append("PrivacyModels.kt must not duplicate the separate follow-request record")
if "data class NovaFollowRequest(" not in follow_models:
    errors.append("FollowRequestModels.kt must own NovaFollowRequest")

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
        errors.append(f"PrivacyRepository must not absorb FollowRequestRepository operation: {forbidden}")

if "interface FollowRequestRepository" not in follow_contract:
    errors.append("stable FollowRequestRepository contract is missing")
for operation in ("followRequests", "acceptFollowRequest", "declineFollowRequest"):
    if operation not in follow_contract:
        errors.append(f"FollowRequestRepository lost operation: {operation}")

# Privacy exit: no temporary adapters or adapter-only mapping tests may return.
for legacy_path in (PRIVACY_ADAPTER, FOLLOW_ADAPTER, PRIVACY_MAPPING_TEST, FOLLOW_MAPPING_TEST):
    if legacy_path.exists():
        errors.append(f"Privacy exit file must stay deleted: {legacy_path.relative_to(ROOT)}")

# Production transport implements both stable contracts directly and emits feature-owned records.
for required in (
    "import com.nova.app.feature.people.domain.model.NovaPerson",
    "import com.nova.app.feature.privacy.data.FollowRequestRepository",
    "import com.nova.app.feature.privacy.data.PrivacyRepository",
    "import com.nova.app.feature.privacy.domain.model.NovaFollowRequest",
    "import com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState",
    "import com.nova.app.feature.privacy.domain.model.NovaPrivacySummary",
    "class NovaPrivacyRepository(",
    ") : PrivacyRepository, FollowRequestRepository",
    "override suspend fun summary()",
    "override suspend fun setPrivate(isPrivate: Boolean)",
    "override suspend fun personState(username: String)",
    "override suspend fun followRequests()",
    "override suspend fun acceptFollowRequest(requestId: Long)",
    "override suspend fun declineFollowRequest(requestId: Long)",
    "override suspend fun closeFriends()",
    "override suspend fun setCloseFriend(username: String, enabled: Boolean)",
):
    if required not in core_repository:
        errors.append(f"direct Privacy production implementation is missing seam: {required}")
for forbidden in (
    "data class NovaPersonPrivacyState(",
    "data class NovaPrivacySummary(",
    "data class NovaFollowRequest(",
    "import com.nova.app.core.network.NovaPerson",
):
    if forbidden in core_repository:
        errors.append(f"core Privacy repository must not own duplicate model seam: {forbidden}")

# Preserve the exact Privacy transport/auth/parser behavior.
for required in (
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

# AppContainer exposes both narrow interfaces from one direct production transport instance.
for required in (
    "import com.nova.app.core.privacy.NovaPrivacyRepository",
    "private val privacyTransport = NovaPrivacyRepository(appContext)",
    "val privacyRepository: PrivacyRepository = privacyTransport",
    "val followRequestRepository: FollowRequestRepository = privacyTransport",
    "fun currentCachedUsername(): String = sessionStore.load()?.cachedUser?.username.orEmpty()",
):
    if required not in container:
        errors.append(f"AppContainer direct Privacy wiring is missing seam: {required}")
for forbidden in ("CorePrivacyRepositoryAdapter", "CoreFollowRequestRepositoryAdapter"):
    if forbidden in container:
        errors.append(f"AppContainer must not restore Privacy adapter: {forbidden}")

# People paging/state is part of the Privacy model exit edge and must use the stable model directly.
for text, name in (
    (people_models, "PeopleModels.kt"),
    (people_owner, "PeopleStateOwner.kt"),
    (connections_owner, "SocialConnectionsStateOwner.kt"),
    (people_paging, "PeoplePagingRemoteRepository.kt"),
    (people_owner_test, "PeopleStateOwnersTest.kt"),
    (person_screen, "PersonScreen.kt"),
    (private_badge, "PrivateProfileBadge.kt"),
    (private_badge_test, "PrivateProfileBadgeTest.kt"),
):
    if "com.nova.app.core.privacy.NovaPersonPrivacyState" in text:
        errors.append(f"{name} must not import the core Privacy state record")
for text, name in (
    (people_models, "PeopleModels.kt"),
    (people_owner, "PeopleStateOwner.kt"),
    (connections_owner, "SocialConnectionsStateOwner.kt"),
    (people_paging, "PeoplePagingRemoteRepository.kt"),
    (people_owner_test, "PeopleStateOwnersTest.kt"),
    (person_screen, "PersonScreen.kt"),
    (private_badge, "PrivateProfileBadge.kt"),
    (private_badge_test, "PrivateProfileBadgeTest.kt"),
):
    if "com.nova.app.feature.privacy.domain.model.NovaPersonPrivacyState" not in text:
        errors.append(f"{name} must consume the stable Privacy state record")
if "val privacyByUserId: Map<Long, NovaPersonPrivacyState> = emptyMap()" not in people_models:
    errors.append("NovaPersonPage must retain the privacyByUserId stable seam")
if "val privacy = mutableMapOf<Long, NovaPersonPrivacyState>()" not in people_paging:
    errors.append("People paging transport must parse privacy state into the stable model")

# Characterized Privacy state owner remains the sole live async/domain owner for PrivacyScreen.
for required in (
    "data class PrivacyUiState(",
    "class PrivacyStateOwner(",
    "private val privacyRepository: PrivacyRepository",
    "private val followRequestRepository: FollowRequestRepository",
    "private val peoplePagingRepository: PeoplePagingRepository",
    "fun start()",
    "loadSummaryBundle()",
    "loadFollowers(reset = true)",
    "private var followerRequestVersion = 0L",
    "if (requestVersion != followerRequestVersion) return",
    "raw.take(FOLLOWER_QUERY_MAX_LENGTH)",
    "delay(FOLLOWER_SEARCH_DEBOUNCE_MS)",
    "val requestQuery = state.followerQuery.trim()",
    "query = requestQuery",
    "val existingIds = state.followers.mapTo(mutableSetOf()) { it.id }",
    "state.followers + page.people.filterNot { it.id in existingIds }",
    "if (state.privacyBusy) return",
    "if (summary.acceptedPendingRequests > 0)",
    "if (state.requestBusyId != null) return",
    "if (accept) loadFollowers(reset = true)",
    "if (state.closeFriendBusyId != null) return",
    "if (result.statusCode == 401)",
    "onSessionExpired()",
    "internal const val FOLLOWER_SEARCH_DEBOUNCE_MS = 280L",
    "internal const val FOLLOWER_QUERY_MAX_LENGTH = 50",
):
    if required not in owner:
        errors.append(f"Privacy state characterization lost seam: {required}")
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
    "new follower search ignores an older in flight response",
):
    if test_name not in owner_test:
        errors.append(f"Privacy state-owner characterization is missing test: {test_name}")

# Live PrivacyScreen remains owner-backed with unchanged visible seams.
for required in (
    "val appContainer = context.appContainer",
    "PrivacyStateOwner(",
    "username = appContainer.currentCachedUsername()",
    "privacyRepository = appContainer.privacyRepository",
    "followRequestRepository = appContainer.followRequestRepository",
    "peoplePagingRepository = appContainer.peoplePagingRepository",
    "onSessionExpired = { sessionExpiredCallback.value() }",
    "LaunchedEffect(owner)",
    "owner.start()",
    "onCheckedChange = owner::togglePrivate",
    "owner.decideFollowRequest(item, true)",
    "owner.decideFollowRequest(item, false)",
    "onValueChange = owner::setFollowerQuery",
    "owner.toggleCloseFriend(person)",
    "owner.loadFollowers(reset = false)",
    'title = "Privacy"',
    'text = "Private account"',
    'title = "Follow requests"',
    'title = "Close Friends"',
):
    if required not in screen:
        errors.append(f"live PrivacyScreen is missing owner/UI seam: {required}")
for forbidden in (
    "NovaPrivacyRepository(context.applicationContext)",
    "NovaSocialPagingRepository(context.applicationContext)",
    "NovaSessionStore(context.applicationContext)",
    "mutableStateOf<",
):
    if forbidden in screen:
        errors.append(f"PrivacyScreen must not restore route-local orchestration: {forbidden}")

# PersonScreen keeps its characterized local safety/message/share behavior but reads Privacy via AppContainer.
for required in (
    "val privacyRepository = context.appContainer.privacyRepository",
    "privacyRepository.personState(selected.username)",
    "is ApiResult.Success -> privacyState = result.value",
    "is ApiResult.Failure -> messageError = result.message",
    "socialRepository.setFollowing(selectedPerson.username, false)",
    'safetyMessage = "Follow request canceled."',
    'safetyMessage = "Follow request sent."',
    "onFollowToggle(selectedPerson)",
    "socialRepository.setBlocked(selectedPerson.username, true)",
    "socialRepository.report(",
    "NovaMessagingRepository(context.applicationContext)",
    "NovaShareDialog(",
):
    if required not in person_screen:
        errors.append(f"PersonScreen Privacy residual lost protected seam: {required}")

# Reject any reintroduction of the deleted core Privacy record imports or adapters across production/test Kotlin.
for kotlin_file in (ROOT / "app/src").rglob("*.kt"):
    text = kotlin_file.read_text(encoding="utf-8")
    relative = kotlin_file.relative_to(ROOT)
    for forbidden in (
        "import com.nova.app.core.privacy.NovaPersonPrivacyState",
        "import com.nova.app.core.privacy.NovaPrivacySummary",
        "import com.nova.app.core.privacy.NovaFollowRequest",
        "CorePrivacyRepositoryAdapter",
        "CoreFollowRequestRepositoryAdapter",
    ):
        if forbidden in text:
            errors.append(f"Privacy exit boundary restored {forbidden} in {relative}")

if errors:
    print("Privacy architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Privacy architecture check passed.")
