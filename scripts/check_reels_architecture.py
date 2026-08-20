#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/reels/domain/model/ReelModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/reels/data/ReelsRepository.kt"
ADAPTERS = ROOT / "app/src/main/java/com/nova/app/feature/reels/data/remote/CoreReelsRepositoryAdapters.kt"
ADAPTER_TEST = ROOT / "app/src/test/java/com/nova/app/feature/reels/data/remote/CoreReelsRepositoryAdaptersTest.kt"
ROOT_STATE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/reels/ReelsStateOwner.kt"
PROFILE_STATE_OWNERS = ROOT / "app/src/main/java/com/nova/app/feature/reels/ProfileReelsStateOwners.kt"
COMMENTS_STATE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/reels/ReelCommentsStateOwner.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaReelsRepository.kt"
CORE_PROFILE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaProfileReelsRepository.kt"
CORE_WATCH_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/reels/NovaReelWatchRepository.kt"
REELS_SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/reels/ReelsScreen.kt"
PROFILE_VIEWER = ROOT / "app/src/main/java/com/nova/app/feature/reels/ProfileReelsViewerScreen.kt"
COMMENTS_SHEET = ROOT / "app/src/main/java/com/nova/app/feature/reels/ThreadedReelCommentsSheet.kt"
PLAYBACK = ROOT / "app/src/main/java/com/nova/app/feature/reels/ReelPlaybackCoordinator.kt"
PROFILE_GRID = ROOT / "app/src/main/java/com/nova/app/ui/components/NovaProfileReelsGrid.kt"
REPOSTED_GRID = ROOT / "app/src/main/java/com/nova/app/ui/components/NovaProfileRepostedReelsGrid.kt"
MAIN_KOTLIN = ROOT / "app/src/main/java"
TEST_KOTLIN = ROOT / "app/src/test/java"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Reels architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
root_state_owner = read(ROOT_STATE_OWNER)
profile_state_owners = read(PROFILE_STATE_OWNERS)
comments_state_owner = read(COMMENTS_STATE_OWNER)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
core_profile_repository = read(CORE_PROFILE_REPOSITORY)
core_watch_repository = read(CORE_WATCH_REPOSITORY)
reels_screen = read(REELS_SCREEN)
profile_viewer = read(PROFILE_VIEWER)
comments_sheet = read(COMMENTS_SHEET)
playback = read(PLAYBACK)
profile_grid = read(PROFILE_GRID)
reposted_grid = read(REPOSTED_GRID)

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

# Reels exit gate: production repositories directly implement stable contracts.
for source_name, source, contract_name in (
    ("NovaReelsRepository", core_repository, "ReelsRepository"),
    ("NovaProfileReelsRepository", core_profile_repository, "ProfileReelsRepository"),
    ("NovaReelWatchRepository", core_watch_repository, "ReelWatchRepository"),
):
    if f") : {contract_name} {{" not in source:
        errors.append(f"{source_name} must directly implement {contract_name}")

for stable_import in (
    "import com.nova.app.feature.reels.domain.model.NovaReel\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelAuthor\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelComment\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelCommentMutation\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelPage\n",
):
    if stable_import not in core_repository:
        errors.append(f"NovaReelsRepository must parse directly into stable model: {stable_import.strip()}")

for stable_import in (
    "import com.nova.app.feature.reels.domain.model.NovaReel\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelAuthor\n",
    "import com.nova.app.feature.reels.domain.model.NovaReelPage\n",
):
    if stable_import not in core_profile_repository:
        errors.append(f"NovaProfileReelsRepository must parse directly into stable model: {stable_import.strip()}")

for duplicate in (
    "data class NovaReelAuthor(",
    "data class NovaReel(",
    "data class NovaReelPage(",
    "data class NovaReelComment(",
    "data class NovaReelCommentMutation(",
):
    if duplicate in core_repository:
        errors.append(f"duplicate core Reel record must stay deleted: {duplicate}")

if ADAPTERS.exists():
    errors.append("temporary CoreReelsRepositoryAdapters.kt must stay deleted after Reels exit")
if ADAPTER_TEST.exists():
    errors.append("obsolete CoreReelsRepositoryAdaptersTest.kt must stay deleted after Reels exit")

for seam in (
    "val reelsRepository: ReelsRepository = NovaReelsRepository(appContext)",
    "val profileReelsRepository: ProfileReelsRepository = NovaProfileReelsRepository(appContext)",
    "val reelWatchRepository: ReelWatchRepository = NovaReelWatchRepository(appContext)",
):
    if seam not in container:
        errors.append(f"AppContainer is missing direct Reels construction seam: {seam}")

for forbidden in (
    "CoreReelsRepositoryAdapter",
    "CoreProfileReelsRepositoryAdapter",
    "CoreReelWatchRepositoryAdapter",
):
    if forbidden in container:
        errors.append(f"AppContainer must not restore temporary Reels adapter: {forbidden}")

for owner, source in (
    ("class ReelsStateOwner(", root_state_owner),
    ("class ProfileReelsViewerStateOwner(", profile_state_owners),
    ("class ProfileReelsGridStateOwner(", profile_state_owners),
    ("class ReelCommentsStateOwner(", comments_state_owner),
):
    if owner not in source:
        errors.append(f"Reels state-owner slice is missing: {owner}")

for source_name, source in (
    ("ReelsStateOwner", root_state_owner),
    ("ProfileReelsStateOwners", profile_state_owners),
    ("ReelCommentsStateOwner", comments_state_owner),
):
    for forbidden in (
        "com.nova.app.core.reels.NovaReelsRepository",
        "com.nova.app.core.reels.NovaProfileReelsRepository",
        "com.nova.app.core.reels.NovaReelWatchRepository",
        "com.nova.app.core.reels.NovaReel\n",
        "com.nova.app.core.reels.NovaReelComment",
    ):
        if forbidden in source:
            errors.append(f"{source_name} must depend on stable Reels contracts/models, not {forbidden.strip()}")

for required in (
    "appendPagePreservingIncomingDuplicates",
    "watchedMs < 250L",
    "reel.isMine",
    "sessionExpiryVersion",
):
    if required not in root_state_owner:
        errors.append(f"root Reels state owner is missing characterized behavior seam: {required}")

for required in (
    "MAX_INITIAL_LOOKUP_PAGES = 20",
    "ProfileReelsSource.Authored",
    "ProfileReelsSource.Reposted",
    "sessionExpiryVersion",
):
    if required not in profile_state_owners:
        errors.append(f"profile Reels state owners are missing characterized behavior seam: {required}")

for required in (
    "replies = existing + comment",
    "repliesCount = existing.size + 1",
    "deletingCommentId != null || state.deletingReplyId != null",
    "sessionExpiryVersion",
):
    if required not in comments_state_owner:
        errors.append(f"Reel comments state owner is missing characterized behavior seam: {required}")

# Preserve transport-sensitive behavior while changing only ownership/types.
for required in (
    '"reels/?cursor=${cursor.trim()}"',
    "caption.trim().take(500)",
    "clean.take(300)",
    'path = "reels/$reelId/like/"',
    'path = "reels/$reelId/repost/"',
    'requestJson("reels/$reelId/comments/"',
    'path = "reel-comments/$commentId/"',
    'path = "reel-comment-replies/$replyId/"',
    'path = "reels/$reelId/"',
    "const val MAX_REEL_VIDEO_BYTES = 120L * 1024 * 1024",
):
    if required not in core_repository:
        errors.append(f"NovaReelsRepository lost protected transport behavior seam: {required}")

for required in (
    "username.trim().lowercase()",
    'ApiResult.Failure("Choose a profile first.")',
    'source = "authored"',
    'source = "reposted"',
    'val path = "reels/profile/$encodedUsername/"',
    'add("cursor=${encode(cursor.trim())}")',
):
    if required not in core_profile_repository:
        errors.append(f"NovaProfileReelsRepository lost protected transport behavior seam: {required}")

for required in (
    "reelId <= 0L || sessionId.isBlank() || watchedMs < 250L",
    '.put("watched_ms", watchedMs.coerceAtLeast(0L))',
    '.put("duration_ms", durationMs.coerceAtLeast(0L))',
    '.put("max_position_ms", maxPositionMs.coerceAtLeast(0L))',
    'path = "reels/$reelId/watch/"',
):
    if required not in core_watch_repository:
        errors.append(f"NovaReelWatchRepository lost protected telemetry behavior seam: {required}")

for required in (
    "val appContainer = context.appContainer",
    "val repository = appContainer.reelsRepository",
    "ReelsStateOwner(",
    "watchRepository = appContainer.reelWatchRepository",
    "owner.load(reset = true)",
    "owner.recordWatch(reel, snapshot)",
    "repository = repository",
):
    if required not in reels_screen:
        errors.append(f"root Reels live screen is missing stable-owner wiring: {required}")

for forbidden in (
    "com.nova.app.core.network.ApiResult",
    "com.nova.app.core.reels.NovaReel",
    "com.nova.app.core.reels.NovaReelsRepository",
    "com.nova.app.core.reels.NovaReelWatchRepository",
    "NovaReelsRepository(context.applicationContext)",
    "NovaReelWatchRepository(context.applicationContext)",
):
    if forbidden in reels_screen:
        errors.append(f"root Reels live screen must not retain legacy orchestration dependency: {forbidden}")

for required in (
    "repository: ReelsRepository",
    "ReelCommentsStateOwner(",
    "owner.loadComments()",
    "owner::send",
    "owner.deleteComment(comment)",
    "owner.deleteReply(reply)",
):
    if required not in comments_sheet:
        errors.append(f"threaded Reel comments are missing stable-owner wiring: {required}")

for forbidden in (
    "com.nova.app.core.network.ApiResult",
    "com.nova.app.core.reels.NovaReel",
    "com.nova.app.core.reels.NovaReelComment",
    "com.nova.app.core.reels.NovaReelsRepository",
    "repository: NovaReelsRepository",
):
    if forbidden in comments_sheet:
        errors.append(f"threaded Reel comments must not retain legacy orchestration dependency: {forbidden}")

if "import com.nova.app.feature.reels.domain.model.NovaReel" not in playback:
    errors.append("Reel playback pool must consume the stable Reel model")
if "import com.nova.app.core.reels.NovaReel" in playback:
    errors.append("Reel playback pool must not retain the core Reel record import")

for required in (
    "val appContainer = context.appContainer",
    "val profileRepository = appContainer.profileReelsRepository",
    "val interactionRepository = appContainer.reelsRepository",
    "ProfileReelsViewerStateOwner(",
    "owner.loadInitial()",
    "owner.loadMore()",
    "repository = interactionRepository",
):
    if required not in profile_viewer:
        errors.append(f"profile Reel viewer is missing stable-owner wiring: {required}")

for forbidden in (
    "com.nova.app.core.network.ApiResult",
    "com.nova.app.core.reels.NovaProfileReelsRepository",
    "com.nova.app.core.reels.NovaReel",
    "com.nova.app.core.reels.NovaReelsRepository",
    "NovaProfileReelsRepository(context.applicationContext)",
    "NovaReelsRepository(context.applicationContext)",
):
    if forbidden in profile_viewer:
        errors.append(f"profile Reel viewer must not retain legacy orchestration dependency: {forbidden}")

for grid_name, grid, source in (
    ("authored profile Reel grid", profile_grid, "ProfileReelsSource.Authored"),
    ("reposted profile Reel grid", reposted_grid, "ProfileReelsSource.Reposted"),
):
    for required in (
        "context.appContainer.profileReelsRepository",
        "ProfileReelsGridStateOwner(",
        source,
        "owner.loadFirstPage()",
        "owner.loadMore()",
        "com.nova.app.feature.reels.domain.model.NovaReel",
    ):
        if required not in grid:
            errors.append(f"{grid_name} is missing stable-owner wiring: {required}")
    for forbidden in (
        "com.nova.app.core.network.ApiResult",
        "com.nova.app.core.reels.NovaProfileReelsRepository",
        "com.nova.app.core.reels.NovaReel\n",
        "NovaProfileReelsRepository(context.applicationContext)",
        "mutableStateOf<List<NovaReel>>(emptyList())",
    ):
        if forbidden in grid:
            errors.append(f"{grid_name} must not retain legacy grid orchestration: {forbidden.strip()}")

if "username = username" not in profile_grid:
    errors.append("authored profile Reel grid must continue opening the current profile's authored source")
if "username = reel.author.username" not in reposted_grid:
    errors.append("reposted profile Reel grid must continue opening the original author's authored source")

# No Kotlin source may restore ownership of the removed core Reel record graph.
legacy_model_imports = (
    "import com.nova.app.core.reels.NovaReel\n",
    "import com.nova.app.core.reels.NovaReelAuthor\n",
    "import com.nova.app.core.reels.NovaReelPage\n",
    "import com.nova.app.core.reels.NovaReelComment\n",
    "import com.nova.app.core.reels.NovaReelCommentMutation\n",
    "import com.nova.app.core.reels.*\n",
)
for source_root in (MAIN_KOTLIN, TEST_KOTLIN):
    for path in source_root.rglob("*.kt"):
        source = path.read_text(encoding="utf-8")
        for forbidden in legacy_model_imports:
            if forbidden in source:
                errors.append(
                    f"legacy core Reel model import must stay removed: {path.relative_to(ROOT)} -> {forbidden.strip()}"
                )

if errors:
    print("Reels architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Reels architecture exit gate passed.")
