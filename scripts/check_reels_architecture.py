#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/reels/domain/model/ReelModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/reels/data/ReelsRepository.kt"
ADAPTERS = ROOT / "app/src/main/java/com/nova/app/feature/reels/data/remote/CoreReelsRepositoryAdapters.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaReelsRepository.kt"
CORE_PROFILE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaProfileReelsRepository.kt"
CORE_WATCH_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaReelWatchRepository.kt"
REELS_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/reels/ReelsScreen.kt"
PROFILE_VIEWER = ROOT / "app/src/main/java/com/nova/app/feature/reels/ProfileReelsViewerScreen.kt"
COMMENTS_SHEET = ROOT / "app/src/main/java/com/nova/app/feature/reels/ThreadedReelCommentsSheet.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Reels architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
adapters = read(ADAPTERS)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
core_profile_repository = read(CORE_PROFILE_REPOSITORY)
core_watch_repository = read(CORE_WATCH_REPOSITORY)
reels_screen = read(REELS_SCREEN)
profile_viewer = read(PROFILE_VIEWER)
comments_sheet = read(COMMENTS_SHEET)

for declaration in (
    "data class NovaReelAuthor(",
    "data class NovaReel(",
    "data class NovaReelPage(",
    "data class NovaReelComment(",
    "data class NovaReelCommentMutation(",
):
    if declaration not in models:
        errors.append(f"stable Reels domain owner is missing {declaration}")

for interface in (
    "interface ReelsRepository",
    "interface ProfileReelsRepository",
    "interface ReelWatchRepository",
):
    if interface not in contract:
        errors.append(f"stable Reels data contract is missing {interface}")

for operation in (
    "suspend fun reels(cursor: String? = null)",
    "suspend fun createReel(",
    "suspend fun setLiked(",
    "suspend fun setReposted(",
    "suspend fun comments(",
    "suspend fun addComment(",
    "suspend fun deleteComment(",
    "suspend fun deleteCommentReply(",
    "suspend fun deleteReel(",
    "suspend fun repostedReels(",
    "suspend fun record(",
):
    if operation not in contract:
        errors.append(f"stable Reels contracts are missing current operation: {operation}")

for adapter in (
    "class CoreReelsRepositoryAdapter(context: Context) : ReelsRepository",
    "class CoreProfileReelsRepositoryAdapter(context: Context) : ProfileReelsRepository",
    "class CoreReelWatchRepositoryAdapter(context: Context) : ReelWatchRepository",
):
    if adapter not in adapters:
        errors.append(f"Reels production bridge is missing: {adapter}")

for mapper in (
    "internal fun CoreNovaReelAuthor.toStable()",
    "internal fun CoreNovaReel.toStable()",
    "internal fun CoreNovaReelPage.toStable()",
    "internal fun CoreNovaReelComment.toStable()",
    "internal fun CoreNovaReelCommentMutation.toStable()",
):
    if mapper not in adapters:
        errors.append(f"Reels bridge is missing explicit model mapping: {mapper}")

for seam in (
    "val reelsRepository: ReelsRepository = CoreReelsRepositoryAdapter(appContext)",
    "val profileReelsRepository: ProfileReelsRepository = CoreProfileReelsRepositoryAdapter(appContext)",
    "val reelWatchRepository: ReelWatchRepository = CoreReelWatchRepositoryAdapter(appContext)",
):
    if seam not in container:
        errors.append(f"AppContainer is missing stable Reels construction seam: {seam}")

# This first boundary PR intentionally leaves the current transport records and live
# UI orchestration untouched. The next Reels slice must remove these assertions as
# it moves live state to the stable feature boundary.
for legacy_declaration in (
    "data class NovaReelAuthor(",
    "data class NovaReel(",
    "data class NovaReelPage(",
    "data class NovaReelComment(",
    "data class NovaReelCommentMutation(",
):
    if legacy_declaration not in core_repository:
        errors.append(
            f"first Reels boundary must not silently remove legacy transport record yet: {legacy_declaration}"
        )

if "class NovaProfileReelsRepository(" not in core_profile_repository:
    errors.append("first Reels boundary must retain the existing profile transport")
if "class NovaReelWatchRepository(" not in core_watch_repository:
    errors.append("first Reels boundary must retain the existing watch transport")

for required in (
    "NovaReelsRepository(context.applicationContext)",
    "NovaReelWatchRepository(context.applicationContext)",
    "var reels by remember { mutableStateOf<List<NovaReel>>(emptyList()) }",
):
    if required not in reels_screen:
        errors.append(f"first Reels boundary must leave live ReelsScreen ownership unchanged: {required}")

for required in (
    "NovaProfileReelsRepository(context.applicationContext)",
    "NovaReelsRepository(context.applicationContext)",
    "var reels by remember(username) { mutableStateOf<List<NovaReel>>(emptyList()) }",
):
    if required not in profile_viewer:
        errors.append(f"first Reels boundary must leave profile viewer ownership unchanged: {required}")

if "repository: NovaReelsRepository" not in comments_sheet:
    errors.append("first Reels boundary must leave threaded comment transport ownership unchanged")

if errors:
    print("Reels architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Reels architecture check passed.")
