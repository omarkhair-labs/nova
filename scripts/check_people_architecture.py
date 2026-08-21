#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODEL = ROOT / "app/src/main/java/com/nova/app/feature/people/domain/model/PeopleModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleRepository.kt"
PEOPLE_JSON_PARSER = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleJsonParser.kt"
API = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
SOCIAL = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialRepository.kt"
PAGING = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialPagingRepository.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
APP = ROOT / "app/src/main/java/com/nova/app/NovaApp.kt"
PEOPLE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/PeopleStateOwner.kt"
PERSON_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/PersonStateOwner.kt"
CONNECTIONS_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/SocialConnectionsStateOwner.kt"
PROFILE_CONTENT_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/profile/ProfileContentStateOwner.kt"
PEOPLE_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/PeopleScreen.kt"
PERSON_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/PersonScreen.kt"
CONNECTIONS_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/SocialConnectionsScreen.kt"
PROFILE_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/profile/ProfileScreen.kt"
PRIVATE_PROFILE_BADGE = ROOT / "app/src/main/java/com/nova/app/feature/people/PrivateProfileBadge.kt"
PRIVATE_PROFILE_BADGE_V4 = ROOT / "app/src/main/java/com/nova/app/feature/people/PrivateProfileBadgeV4.kt"
PRIVATE_PROFILE_BADGE_V4_TEST = ROOT / "app/src/test/java/com/nova/app/feature/people/PrivateProfileBadgeV4Test.kt"
PROFILE_TABS = ROOT / "app/src/main/java/com/nova/app/ui/components/NovaProfileContentTabs.kt"
PROFILE_TABS_V4 = ROOT / "app/src/main/java/com/nova/app/ui/components/NovaProfileContentTabsV4.kt"
PROFILE_GRID = ROOT / "app/src/main/java/com/nova/app/ui/components/NovaPagedProfilePostsGrid.kt"
SOCIAL_GRAPH_ACTIVITY = ROOT / "app/src/main/java/com/nova/app/SocialGraphActivity.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required people architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


model = read(MODEL)
contract = read(CONTRACT)
people_json_parser = read(PEOPLE_JSON_PARSER)
api = read(API)
social = read(SOCIAL)
paging = read(PAGING)
container = read(CONTAINER)
app = read(APP)
people_owner = read(PEOPLE_OWNER)
person_owner = read(PERSON_OWNER)
connections_owner = read(CONNECTIONS_OWNER)
profile_content_owner = read(PROFILE_CONTENT_OWNER)
people_screen = read(PEOPLE_SCREEN)
person_screen = read(PERSON_SCREEN)
connections_screen = read(CONNECTIONS_SCREEN)
profile_screen = read(PROFILE_SCREEN)
private_profile_badge = read(PRIVATE_PROFILE_BADGE)
profile_tabs = read(PROFILE_TABS)
profile_grid = read(PROFILE_GRID)
social_graph_activity = read(SOCIAL_GRAPH_ACTIVITY)

for declaration in ("data class NovaPerson(", "data class NovaPersonPage(", "data class NovaProfilePostPage("):
    if declaration not in model:
        errors.append(f"stable people model owner is missing {declaration}")

if "data class NovaPerson(" in api:
    errors.append("NovaApiClient must not own NovaPerson")

if "private fun parsePerson(" in api:
    errors.append("NovaApiClient must not retain the superseded core People parser")

for required in (
    "internal fun parseNovaPerson(",
    "json: JSONObject",
    "resolveMediaUrl: (String) -> String",
    'id = json.optLong("id")',
    'username = json.optString("username")',
    'name = json.optString("name")',
    'avatarUrl = resolveMediaUrl(json.optString("avatar_url"))',
    'followersCount = json.optInt("followers_count", 0)',
    'followingCount = json.optInt("following_count", 0)',
    'postsCount = json.optInt("posts_count", 0)',
    'isFollowing = json.optBoolean("is_following", false)',
):
    if required not in people_json_parser:
        errors.append(f"feature-owned People JSON parser lost characterized behavior: {required}")

if "import com.nova.app.feature.people.data.parseNovaPerson" not in api:
    errors.append("NovaApiClient must import the feature-owned People JSON parser")

for required in (
    "add(parseNovaPerson(it, ::resolveMediaUrl))",
    "ApiResult.Success(parseNovaPerson(response.value, ::resolveMediaUrl))",
):
    if required not in api:
        errors.append(f"NovaApiClient live People decode path must use feature parser: {required}")

for forbidden in ("add(parsePerson(it))", "ApiResult.Success(parsePerson(response.value))"):
    if forbidden in api:
        errors.append(f"NovaApiClient must not route live People responses through the core parser: {forbidden}")

for declaration in ("data class NovaPersonPage(", "data class NovaProfilePostPage("):
    if declaration in paging:
        errors.append(f"NovaSocialPagingRepository must not own {declaration}")

for interface_name in ("interface PeopleRepository", "interface PeoplePagingRepository"):
    if interface_name not in contract:
        errors.append(f"stable people contract is missing {interface_name}")

if ") : PeopleRepository" not in social:
    errors.append("NovaSocialRepository must implement PeopleRepository")

if ") : PeoplePagingRepository" not in paging:
    errors.append("NovaSocialPagingRepository must implement PeoplePagingRepository")

required_stable_imports = (
    "com.nova.app.feature.people.domain.model.NovaPerson",
    "com.nova.app.feature.people.domain.model.NovaPersonPage",
    "com.nova.app.feature.people.domain.model.NovaProfilePostPage",
)
for stable_import in required_stable_imports:
    if stable_import not in paging:
        errors.append(f"NovaSocialPagingRepository must import {stable_import}")

if "val peopleRepository: PeopleRepository = socialRepository" not in container:
    errors.append("AppContainer must expose the stable PeopleRepository")

if "val peoplePagingRepository: PeoplePagingRepository = NovaSocialPagingRepository(appContext)" not in container:
    errors.append("AppContainer must expose the stable PeoplePagingRepository")

for owner_text, owner_name in (
    (people_owner, "PeopleStateOwner"),
    (person_owner, "PersonStateOwner"),
    (connections_owner, "SocialConnectionsStateOwner"),
    (profile_content_owner, "ProfileContentStateOwner"),
):
    if f"class {owner_name}(" not in owner_text:
        errors.append(f"missing stable People/Profile state owner {owner_name}")

for forbidden in ("NovaSocialPagingRepository", "NovaSocialRepository", "rememberCoroutineScope", "ApiResult"):
    if forbidden in people_screen:
        errors.append(f"PeopleScreen must render state/callbacks and not own {forbidden}")

for forbidden in (
    "NovaSocialPagingRepository",
    "NovaSocialRepository",
    "NovaSessionStore",
    "rememberCoroutineScope",
    "ApiResult",
):
    if forbidden in connections_screen:
        errors.append(f"SocialConnectionsScreen must render state/callbacks and not own {forbidden}")

for forbidden in (
    "fun searchPeople(",
    "fun toggleFollowFromList(",
    "var peopleLoading by remember",
    "var followingUsername by remember",
    "val socialRepository = appContainer.socialRepository",
    "var profilePosts by remember",
    "fun loadProfilePosts(",
):
    if forbidden in app:
        errors.append(f"NovaApp must not retain legacy People/Profile orchestration: {forbidden}")

for required in (
    "PeopleStateOwner(",
    "PersonStateOwner(",
    "ProfileContentStateOwner(",
    "state = peopleState",
    "onFollowToggle = personOwner::toggleFollow",
    "profileContentOwner = profileContentOwner",
):
    if required not in app:
        errors.append(f"NovaApp is missing stable People/Profile wiring: {required}")

if "SocialConnectionsStateOwner(" not in social_graph_activity:
    errors.append("SocialGraphActivity must host SocialConnectionsStateOwner")

if PROFILE_TABS_V4.exists():
    errors.append("NovaProfileContentTabsV4.kt must stay deleted after stable profile consolidation")

if PRIVATE_PROFILE_BADGE_V4.exists():
    errors.append("PrivateProfileBadgeV4.kt must stay deleted after stable profile cleanup")

if PRIVATE_PROFILE_BADGE_V4_TEST.exists():
    errors.append("PrivateProfileBadgeV4Test.kt must stay deleted after stable profile cleanup")

if "internal fun shouldShowPrivateProfileBadge(state: NovaPersonPrivacyState): Boolean" not in private_profile_badge:
    errors.append("stable private-profile badge helper seam is missing")

if "shouldShowPrivateProfileBadge(privacyState)" not in person_screen:
    errors.append("PersonScreen must use the stable private-profile badge helper")

for path in (ROOT / "app/src").rglob("*.kt"):
    if "shouldShowPrivateProfileBadgeV4" in path.read_text(encoding="utf-8"):
        errors.append(
            f"{path.relative_to(ROOT)} must not reintroduce shouldShowPrivateProfileBadgeV4"
        )

for forbidden in ("NovaSocialPagingRepository", "rememberCoroutineScope", "ApiResult", "NovaProfileContentTabsV4"):
    if forbidden in profile_tabs:
        errors.append(f"NovaProfileContentTabs must render stable state/callbacks and not own {forbidden}")

if "NovaSocialPagingRepository" in profile_grid:
    errors.append("NovaPagedProfilePostsGrid must use AppContainer stable contracts, not construct NovaSocialPagingRepository")

if "com.nova.app.core.network.NovaPost" in profile_screen:
    errors.append("ProfileScreen must import the stable post model directly")

if errors:
    print("People/Profile architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("People/Profile architecture check passed.")
