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
KOTLIN_ROOTS = (
    ROOT / "app/src/main/java",
    ROOT / "app/src/test/java",
)

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Stories architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
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
    if declaration in core_repository:
        errors.append(f"core Stories transport must not redeclare stable model: {declaration}")

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

if ADAPTER.exists():
    errors.append("obsolete CoreStoriesRepositoryAdapter must be removed after the live switch")

if ") : StoriesRepository" not in core_repository:
    errors.append("production NovaStoriesRepository must implement StoriesRepository directly")

for stable_import in (
    "import com.nova.app.feature.stories.domain.model.NovaStory\n",
    "import com.nova.app.feature.stories.domain.model.NovaStoryAuthor\n",
    "import com.nova.app.feature.stories.domain.model.NovaStoryGroup\n",
    "import com.nova.app.feature.stories.domain.model.NovaStorySharedPost\n",
    "import com.nova.app.feature.stories.domain.model.NovaStorySharedReel\n",
    "import com.nova.app.feature.stories.domain.model.NovaStoryViewer\n",
):
    if stable_import not in core_repository:
        errors.append(f"production Stories transport must parse into stable model: {stable_import.strip()}")

if "val storiesRepository: StoriesRepository = NovaStoriesRepository(appContext)" not in container:
    errors.append("AppContainer must expose NovaStoriesRepository through the stable StoriesRepository contract")
if "CoreStoriesRepositoryAdapter" in container:
    errors.append("AppContainer must not restore the removed Stories adapter")

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
    "StoryViewersDialog(owner = owner)",
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

for obsolete_name in (
    "StoryV2Background",
    "StoryV2Ink",
    "StoryV2Muted",
    "MediaStoryComposerV2",
    "TextStoryComposerV2",
    "StoryAudienceChooserV2",
    "StoryAudienceChipV2",
    "StoryViewerV2",
    "StoryVisualV2",
    "StoryViewersDialogV2",
):
    if obsolete_name in rail:
        errors.append(f"live Stories implementation must not retain superseded helper name: {obsolete_name}")

legacy_model_imports = (
    "import com.nova.app.core.stories.NovaStory\n",
    "import com.nova.app.core.stories.NovaStoryAuthor\n",
    "import com.nova.app.core.stories.NovaStoryGroup\n",
    "import com.nova.app.core.stories.NovaStorySharedPost\n",
    "import com.nova.app.core.stories.NovaStorySharedReel\n",
    "import com.nova.app.core.stories.NovaStoryViewer\n",
)
for kotlin_root in KOTLIN_ROOTS:
    if not kotlin_root.exists():
        continue
    for path in kotlin_root.rglob("*.kt"):
        text = path.read_text(encoding="utf-8")
        relative = path.relative_to(ROOT)
        for legacy_import in legacy_model_imports:
            if legacy_import in text:
                errors.append(
                    f"legacy core Story model import remains in {relative}: {legacy_import.strip()}"
                )
        if "CoreStoriesRepositoryAdapter" in text:
            errors.append(f"removed Stories adapter is still referenced by {relative}")

if errors:
    print("Stories architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Stories architecture check passed.")
