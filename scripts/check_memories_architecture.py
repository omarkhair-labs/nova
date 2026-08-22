#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/memories/domain/model/MemoryModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/MemoryRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/MemoryParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/memories/data/remote/MemoryRemoteRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryStateOwner.kt"
FILM_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryFilmStateOwner.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoriesRail.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryScreen.kt"
FILM_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/memories/MemoryFilmScreen.kt"
FILM_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/memories/film/MemoryFilmExporter.kt"
FILM_EXPORTER = ROOT / "app/src/main/java/com/nova/app/feature/memories/film/Media3MemoryFilmExporter.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"
BUILD = ROOT / "app/build.gradle.kts"

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
film_owner = read(FILM_OWNER)
rail = read(RAIL)
screen = read(SCREEN)
film_screen = read(FILM_SCREEN)
film_contract = read(FILM_CONTRACT)
film_exporter = read(FILM_EXPORTER)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)
build = read(BUILD)

for declaration in (
    "data class MemoryPerson(",
    "data class MemoryRoom(",
    "data class MemoryStats(",
    "data class MemoryHighlight(",
    "data class WeeklyMemory(",
    "data class MemoryFilmScene(",
    "data class MemoryFilmPlan(",
):
    if declaration not in models:
        errors.append(f"Memories domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Memories model: {declaration}")

if "interface MemoryRepository" not in contract:
    errors.append("stable MemoryRepository contract is missing")
for required in ("suspend fun week(", "suspend fun filmPlan("):
    if required not in contract:
        errors.append(f"MemoryRepository is missing protected read: {required}")

for required in ("internal fun parseWeeklyMemory(", "internal fun parseMemoryFilmPlan("):
    if required not in parser:
        errors.append(f"Memories wire parsing must remain feature-owned: {required}")

if ") : MemoryRepository" not in remote:
    errors.append("MemoryRemoteRepository must implement MemoryRepository directly")
for required in (
    'path = "memories/week/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo"',
    'path = "memories/film-plan/?utc_offset_minutes=$utcOffsetMinutes&weeks_ago=$weeksAgo"',
    "NovaSessionStore",
    "parseWeeklyMemory(",
    "parseMemoryFilmPlan(",
):
    if required not in remote:
        errors.append(f"Memories remote implementation is missing protected seam: {required}")

for forbidden in (
    "memories/week/",
    "memories/film-plan/",
    "WeeklyMemory",
    "MemoryFilmPlan",
    "MemoryRepository",
):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Memories concern: {forbidden}")

if "class MemoryStateOwner(" not in owner:
    errors.append("MemoryStateOwner must own Memories async state")
if "private val repository: MemoryRepository" not in owner:
    errors.append("MemoryStateOwner must depend on MemoryRepository")
for required in ("weeksAgo", "sessionExpiryVersion", "loadNow("):
    if required not in owner:
        errors.append(f"MemoryStateOwner is missing state seam: {required}")

if "class MemoryFilmStateOwner(" not in film_owner:
    errors.append("MemoryFilmStateOwner must own Film async/export state")
for required in (
    "private val repository: MemoryRepository",
    "private val exporter: MemoryFilmExporter",
    "loadPlanNow(",
    "exportNow(",
    "sessionExpiryVersion",
    "cancelExport()",
):
    if required not in film_owner:
        errors.append(f"MemoryFilmStateOwner is missing protected seam: {required}")

for required in (
    "context.appContainer.memoryRepository",
    "MemoryStateOwner(repository, scope)",
    "MemoryScreen(",
    "Your week is ready",
    "Make your Memory Film",
    "MemoryFilmScreen(",
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

for required in (
    "context.appContainer.memoryRepository",
    "context.appContainer.memoryFilmExporter",
    "MemoryFilmStateOwner(",
    "Make my film",
    "RenderedFilmPreview(",
    "Share your film",
    "Older week",
):
    if required not in film_screen:
        errors.append(f"Memory Film experience is missing product seam: {required}")

for forbidden in (
    "NovaApiClient",
    "requestJson(",
    "memories/week/",
    "memories/film-plan/",
    "Transformer.Builder",
):
    if forbidden in screen or forbidden in rail or forbidden in film_screen:
        errors.append(f"Memories UI must not own transport/export implementation: {forbidden}")

if "interface MemoryFilmExporter" not in film_contract:
    errors.append("Memory Film must expose a stable exporter contract")
for required in ("suspend fun export(", "fun cancel()"):
    if required not in film_contract:
        errors.append(f"Memory Film exporter contract is missing: {required}")

for required in (
    "class Media3MemoryFilmExporter(",
    ") : MemoryFilmExporter",
    "Transformer.Builder(appContext)",
    "EditedMediaItemSequence.withVideoFrom(",
    "Composition.Builder(sequence)",
    "setImageDurationMs(scene.durationMs)",
    "Presentation.createForWidthAndHeight(",
    "setRemoveAudio(true)",
):
    if required not in film_exporter:
        errors.append(f"Media3 Memory Film adapter is missing protected seam: {required}")

if "val memoryRepository: MemoryRepository = MemoryRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct MemoryRemoteRepository behind MemoryRepository")
if "val memoryFilmExporter: MemoryFilmExporter = Media3MemoryFilmExporter(appContext)" not in container:
    errors.append("AppContainer must construct Media3 Memory Film exporter behind its contract")

for dependency in (
    '"androidx.media3:media3-transformer:1.10.1"',
    '"androidx.media3:media3-effect:1.10.1"',
):
    if dependency not in build:
        errors.append(f"Memory Film build is missing aligned Media3 dependency: {dependency}")

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
