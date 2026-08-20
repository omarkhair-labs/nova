#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/stories/domain/model/StoryModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/stories/data/StoriesRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/stories/data/remote/CoreStoriesRepositoryAdapter.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
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

# This first Stories slice intentionally does not alter live UI orchestration yet.
# Keep that explicit so a later PR must remove the legacy construction rather than
# silently claiming the feature is already complete.
if "NovaStoriesRepository" not in rail:
    errors.append("first Stories boundary PR unexpectedly changed live StoriesRail ownership")

if "class NovaStoriesRepository(" not in core_repository:
    errors.append("existing production Stories transport must remain intact in this slice")

if errors:
    print("Stories architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Stories architecture check passed.")
