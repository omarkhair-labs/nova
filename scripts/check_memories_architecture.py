#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/memories/domain/model/MemoryModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/MemoryRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/MemoryParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/remote/MemoryRemoteRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryStateOwner.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoriesRail.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryScreen.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Memories architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
parser = read(PARSER)
remote = read(REMOTE)
owner = read(OWNER)
rail = read(RAIL)
screen = read(SCREEN)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)

for declaration in (
    "data class MemoryPerson(",
    "data class MemoryRoom(",
    "data class MemoryStats(",
    "data class MemoryHighlight(",
    "data class WeeklyMemory(",
):
    if declaration not in models:
        errors.append(f"Memories domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Memories model: {declaration}")

if "interface MemoryRepository" not in contract:
    errors.append("stable MemoryRepository contract is missing")
if "suspend fun week(" not in contract:
    errors.append("MemoryRepository must own weekly recap reads")

if "internal fun parseWeeklyMemory(" not in parser:
    errors.append("Memories wire parsing must remain feature-owned")

if ") : MemoryRepository" not in remote:
    errors.append("MemoryRemoteRepository must implement MemoryRepository directly")
for required in (
    'path = "memories/week/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo"',
    "NovaSessionStore",
    "parseWeeklyMemory(",
):
    if required not in remote:
        errors.append(f"Memories remote implementation is missing protected seam: {required}")

for forbidden in ("memories/week/", "WeeklyMemory", "MemoryRepository"):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Memories concern: {forbidden}")

if "class MemoryStateOwner(" not in owner:
    errors.append("MemoryStateOwner must own Memories async state")
if "private val repository: MemoryRepository" not in owner:
    errors.append("MemoryStateOwner must depend on MemoryRepository")
for required in ("weeksAgo", "sessionExpiryVersion", "loadNow("):
    if required not in owner:
        errors.append(f"MemoryStateOwner is missing state seam: {required}")

for required in (
    "context.appContainer.memoryRepository",
    "MemoryStateOwner(repository, scope)",
    "MemoryScreen(",
    "Your week is ready",
):
    if required not in rail:
        errors.append(f"Memories Home rail is missing stable wiring: {required}")

for required in (
    "context.appContainer.memoryRepository",
    "MemoryStateOwner(repository, scope)",
    "Older week",
    "Newer week",
    "Share",
    "Your people",
    "Rooms you lived in",
):
    if required not in screen:
        errors.append(f"Your Week experience is missing product seam: {required}")
for forbidden in ("NovaApiClient", "requestJson(", "memories/week/"):
    if forbidden in screen or forbidden in rail:
        errors.append(f"Memories UI must not own transport: {forbidden}")

if "val memoryRepository: MemoryRepository = MemoryRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct MemoryRemoteRepository behind MemoryRepository")

if "import com.nova.app.feature.memories.MemoriesRail" not in home:
    errors.append("Home must import Memories surface")
if "MemoriesRail(" not in home:
    errors.append("Home must render Memories")
if home.find("MemoriesRail(") < home.find("RoomsRail("):
    errors.append("Home hierarchy must keep Rooms before Memories")
if home.find("MemoriesRail(") > home.find("onClick = onCreatePost"):
    errors.append("Home hierarchy must keep Memories before permanent post creation")

if errors:
    print("Memories architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Memories architecture check passed.")
