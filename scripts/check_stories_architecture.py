#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/stories/domain/model/StoryModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/stories/data/StoriesRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/stories/data/remote/CoreStoriesRepositoryAdapter.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
RAIL_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/stories/StoriesStateOwner.kt"
VIEWER_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/stories/StoryViewerStateOwner.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/stories/StoriesRail.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/stories/NovaStoriesRepository.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Stories architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
adapter = read(ADAPTER)
container = read(CONTAINER)
rail_owner = read(RAIL_OWNER)
viewer_owner = read(VIEWER_OWNER)
rail = read(RAIL)
core_repository = read(CORE_REPOSITORY)

for declaration in (
    "data class NovaStoryAuthor(",
    "data class NovaStorySharedPost(",
    "data class NovaStorySharedReel(",
    "data class NovaStory(",
    "data class NovaStoryGroup(",
    "data class NovaStoryViewer(",
):
    if declaration not in models:
        errors.append(f"stable Stories domain owner is missing {declaration}")

if "interface StoriesRepository" not in contract:
    errors.append("stable StoriesRepository contract is missing")

for method in (
    "suspend fun stories()",
    "suspend fun createStory(",
    "suspend fun createTextStory(",
    "suspend fun markViewed(",
    "suspend fun react(",
    "suspend fun removeReaction(",
    "suspend fun reply(",
    "suspend fun viewers(",
    "suspend fun deleteStory(",
):
    if method not in contract:
        errors.append(f"StoriesRepository is missing current operation: {method}")

if ") : StoriesRepository" not in adapter:
    errors.append("production Stories adapter must implement StoriesRepository")

if "private val delegate: NovaStoriesRepository" not in adapter:
    errors.append("Stories adapter must delegate to the existing production transport")

if "val storiesRepository: StoriesRepository = CoreStoriesRepositoryAdapter(NovaStoriesRepository(appContext))" not in container:
    errors.append("AppContainer must own the stable StoriesRepository")

if "class StoriesStateOwner(" not in rail_owner:
    errors.append("StoriesStateOwner must own rail/create async state")
if "private val repository: StoriesRepository" not in rail_owner:
    errors.append("StoriesStateOwner must depend on the stable StoriesRepository")
for required in (
    "sessionExpiryVersion",
    "mediaCreatedVersion",
    "textCreatedVersion",
    "createMediaStoryNow(",
    "createTextStoryNow(",
):
    if required not in rail_owner:
        errors.append(f"StoriesStateOwner is missing protected seam: {required}")
if "NovaStoriesRepository" in rail_owner:
    errors.append("StoriesStateOwner must not depend on the core Stories transport")

if "class StoryViewerStateOwner(" not in viewer_owner:
    errors.append("StoryViewerStateOwner must own viewer mutation state")
if "private val repository: StoriesRepository" not in viewer_owner:
    errors.append("StoryViewerStateOwner must depend on the stable StoriesRepository")
for required in (
    "markViewedNow(",
    "toggleReactionNow(",
    "sendReplyNow(",
    "deleteStoryNow(",
    "loadViewersNow(",
    "viewersVisible",
    "mutationBusy",
    "sessionExpiryVersion",
):
    if required not in viewer_owner:
        errors.append(f"StoryViewerStateOwner is missing protected seam: {required}")
if "NovaStoriesRepository" in viewer_owner:
    errors.append("StoryViewerStateOwner must not depend on the core Stories transport")

for required in (
    "context.appContainer.storiesRepository",
    "StoriesStateOwner(repository, scope)",
    "StoryViewerStateOwner(initialGroup, repository, scope)",
    "StoryViewersDialogV2(owner = owner)",
):
    if required not in rail:
        errors.append(f"live Stories UI is missing stable owner wiring: {required}")

for forbidden in (
    "com.nova.app.core.stories",
    "NovaStoriesRepository",
    "ApiResult",
    "repository.stories()",
    "repository.createStory(",
    "repository.createTextStory(",
    "repository.markViewed(",
    "repository.react(",
    "repository.removeReaction(",
    "repository.reply(",
    "repository.viewers(",
    "repository.deleteStory(",
):
    if forbidden in rail:
        errors.append(f"live Stories UI must not own repository/network orchestration: {forbidden}")

# The live consumer now uses stable owners. The old core transport still exists
# temporarily behind the adapter; its passive records/helper naming are audited
# in the next cleanup slice before declaring the Stories feature exit gate.
if "class NovaStoriesRepository(" not in core_repository:
    errors.append("production Stories transport disappeared before the cleanup slice")

if errors:
    print("Stories architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Stories architecture check passed.")
