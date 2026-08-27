#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/rooms/domain/model/RoomModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/rooms/data/RoomRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/rooms/data/RoomParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/rooms/data/remote/RoomRemoteRepository.kt"
LIST_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomsStateOwner.kt"
ROOM_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomStateOwner.kt"
TONIGHT_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomTonightStateOwner.kt"
COMPOSER = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomItemComposer.kt"
TONIGHT_SECTION = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomTonightSection.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomsRail.kt"
LIST_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomsScreen.kt"
ROOM_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/rooms/RoomScreen.kt"
TONIGHT_SURFACE = ROOT / "app/src/main/java/com/nova/app/feature/tonight/TonightSurface.kt"
TONIGHT_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/tonight/TonightScreen.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Rooms architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
parser = read(PARSER)
remote = read(REMOTE)
list_owner = read(LIST_OWNER)
room_owner = read(ROOM_OWNER)
tonight_owner = read(TONIGHT_OWNER)
composer = read(COMPOSER)
tonight_section = read(TONIGHT_SECTION)
rail = read(RAIL)
list_screen = read(LIST_SCREEN)
room_screen = read(ROOM_SCREEN)
tonight_surface = read(TONIGHT_SURFACE)
tonight_screen = read(TONIGHT_SCREEN)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)

for declaration in (
    "data class RoomPerson(",
    "data class RoomConversation(",
    "data class RoomSummary(",
    "data class RoomDetail(",
    "data class RoomItem(",
    "data class RoomItemPage(",
    "data class RoomTonightRow(",
    "data class RoomTonightSnapshot(",
):
    if declaration not in models:
        errors.append(f"Rooms domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Rooms model: {declaration}")

if "interface RoomRepository" not in contract:
    errors.append("stable RoomRepository contract is missing")
for required in (
    "suspend fun rooms()",
    "suspend fun room(conversationId: Long)",
    "suspend fun items(",
    "suspend fun createItem(",
    "suspend fun roomTonight(utcOffsetMinutes: Int)",
    "suspend fun updateDescription(",
):
    if required not in contract:
        errors.append(f"RoomRepository is missing protected operation: {required}")

for required in (
    "internal fun parseRooms(",
    "internal fun parseRoomDetail(",
    "internal fun parseRoomItemPage(",
    "internal fun parseRoomItem(",
    "internal fun parseRoomTonightSnapshot(",
):
    if required not in parser:
        errors.append(f"Rooms wire parsing must remain feature-owned: {required}")

if ") : RoomRepository" not in remote:
    errors.append("RoomRemoteRepository must implement RoomRepository directly")
for required in (
    'api.requestJson("rooms/"',
    'path = "rooms/$conversationId/"',
    'path = "rooms/$conversationId/items/?$params"',
    'path = "rooms/tonight/?utc_offset_minutes=$utcOffsetMinutes"',
    "api.requestMultipart(",
    "withContext(Dispatchers.IO)",
    "NovaSessionStore",
    "NovaVideoPreparer",
    "sourceFile = preparedVideo.videoFile",
):
    if required not in remote:
        errors.append(f"Rooms remote implementation is missing protected seam: {required}")

for forbidden in ("rooms/", "RoomSummary", "RoomRepository"):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Rooms concern: {forbidden}")

for owner, declaration in (
    (list_owner, "class RoomsStateOwner("),
    (room_owner, "class RoomStateOwner("),
    (tonight_owner, "class RoomTonightStateOwner("),
):
    if declaration not in owner:
        errors.append(f"Rooms async state owner is missing: {declaration}")
    if "RoomRepository" not in owner:
        errors.append(f"{declaration} must depend on RoomRepository")
    if "sessionExpiryVersion" not in owner:
        errors.append(f"{declaration} must surface terminal session expiry")

for required in (
    "creatingItem",
    "itemCreatedVersion",
    "repository.createItem(",
):
    if required not in room_owner:
        errors.append(f"RoomStateOwner must own publishing state: {required}")

for required in (
    "rememberLauncherForActivityResult",
    "ActivityResultContracts.GetContent()",
    'RoomComposerKind("note", "Note")',
    'RoomComposerKind("photo", "Photo")',
    'RoomComposerKind("video", "Video")',
    'RoomComposerKind("music", "Music")',
    'RoomComposerKind("plan", "Plan")',
    'RoomComposerKind("saved", "Saved")',
):
    if required not in composer:
        errors.append(f"Room composer is missing live publishing seam: {required}")

for required in (
    "context.appContainer.roomRepository",
    "RoomTonightStateOwner(repository, scope)",
    "roomUtcOffsetMinutes()",
    "delay(90_000L)",
    "RoomScreen(",
):
    if required not in tonight_section:
        errors.append(f"Room Tonight section is missing stable wiring: {required}")
for forbidden in ("NovaApiClient", "presence_store", "is_online("):
    if forbidden in tonight_section:
        errors.append(f"Room Tonight UI must not own transport or raw presence: {forbidden}")

for required in (
    "context.appContainer.roomRepository",
    "RoomsStateOwner(repository, scope)",
    "RoomsScreen(",
    "RoomScreen(",
):
    if required not in rail:
        errors.append(f"Rooms Home rail is missing stable wiring: {required}")

if "NewGroupDialog(" not in list_screen:
    errors.append("Rooms creation must reuse Messaging-owned NewGroupDialog")
for forbidden in ("groups/create", "GroupMembershipRemoteRepository", "NovaMessagingApiClient"):
    if forbidden in list_screen:
        errors.append(f"Rooms list must not duplicate Messaging group creation: {forbidden}")

for required in (
    "RoomStateOwner(conversationId, repository, scope)",
    "MessagesRouteFactory.conversationIntent(",
    'kind = "group"',
    "RoomItemComposer(",
    "AddToRoomCard(",
    "RoomSectionRail(",
    "MembersRail(",
    "NovaVideoPlayer(",
):
    if required not in room_screen:
        errors.append(f"Room experience is missing stable seam: {required}")
for forbidden in ("NovaMessagingApiClient", "ConversationRealtime", "sendMessage(", 'Text("▶"'):
    if forbidden in room_screen:
        errors.append(f"Room UI must reuse Messaging rather than own chat transport: {forbidden}")

if "import com.nova.app.feature.rooms.RoomTonightSection" not in tonight_screen:
    errors.append("Tonight destination must compose the feature-owned RoomTonightSection")
if "RoomTonightSection(" not in tonight_screen:
    errors.append("Tonight destination must expose live Rooms")
for forbidden in ("RoomTonightSnapshot", "roomRepository", "rooms/tonight/"):
    if forbidden in tonight_screen or forbidden in tonight_surface:
        errors.append(f"Tonight must compose Rooms UI without owning Rooms data: {forbidden}")

if "val roomRepository: RoomRepository = RoomRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct RoomRemoteRepository behind RoomRepository")

if "import com.nova.app.feature.rooms.RoomsRail" not in home:
    errors.append("Home must import Rooms surface")
if "RoomsRail(" not in home:
    errors.append("Home must render Rooms")
if home.find("RoomsRail(") < home.find("OrbitRail("):
    errors.append("Home hierarchy must keep Orbit before Rooms")
if home.find("RoomsRail(") > home.find("onClick = onCreatePost"):
    errors.append("Home hierarchy must keep Rooms before permanent post creation")

if errors:
    print("Rooms architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Rooms architecture check passed.")
