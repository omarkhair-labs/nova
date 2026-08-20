#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODEL = ROOT / "app/src/main/java/com/nova/app/feature/people/domain/model/PeopleModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleRepository.kt"
API = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
SOCIAL = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialRepository.kt"
PAGING = ROOT / "app/src/main/java/com/nova/app/core/social/NovaSocialPagingRepository.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
APP = ROOT / "app/src/main/java/com/nova/app/NovaApp.kt"
PEOPLE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/PeopleStateOwner.kt"
PERSON_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/PersonStateOwner.kt"
CONNECTIONS_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/people/SocialConnectionsStateOwner.kt"
PEOPLE_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/PeopleScreen.kt"
CONNECTIONS_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/people/SocialConnectionsScreen.kt"
SOCIAL_GRAPH_ACTIVITY = ROOT / "app/src/main/java/com/nova/app/SocialGraphActivity.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required people architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


model = read(MODEL)
contract = read(CONTRACT)
api = read(API)
social = read(SOCIAL)
paging = read(PAGING)
container = read(CONTAINER)
app = read(APP)
people_owner = read(PEOPLE_OWNER)
person_owner = read(PERSON_OWNER)
connections_owner = read(CONNECTIONS_OWNER)
people_screen = read(PEOPLE_SCREEN)
connections_screen = read(CONNECTIONS_SCREEN)
social_graph_activity = read(SOCIAL_GRAPH_ACTIVITY)

for declaration in ("data class NovaPerson(", "data class NovaPersonPage(", "data class NovaProfilePostPage("):
    if declaration not in model:
        errors.append(f"stable people model owner is missing {declaration}")

if "data class NovaPerson(" in api:
    errors.append("NovaApiClient must not own NovaPerson")

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
):
    if f"class {owner_name}(" not in owner_text:
        errors.append(f"missing stable People state owner {owner_name}")

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
):
    if forbidden in app:
        errors.append(f"NovaApp must not retain legacy People orchestration: {forbidden}")

for required in (
    "PeopleStateOwner(",
    "PersonStateOwner(",
    "state = peopleState",
    "onFollowToggle = personOwner::toggleFollow",
):
    if required not in app:
        errors.append(f"NovaApp is missing stable People wiring: {required}")

if "SocialConnectionsStateOwner(" not in social_graph_activity:
    errors.append("SocialGraphActivity must host SocialConnectionsStateOwner")

if errors:
    print("People architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("People architecture check passed.")
