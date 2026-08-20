#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/nova/app"

required = [
    MAIN / "feature/posts/domain/model/PostModels.kt",
    MAIN / "feature/posts/data/PostRepository.kt",
    MAIN / "feature/feed/data/FeedRepository.kt",
    MAIN / "feature/feed/FeedStateOwner.kt",
    MAIN / "feature/posts/detail/PostDetailStateOwner.kt",
    MAIN / "feature/posts/comments/PostCommentsStateOwner.kt",
]

errors: list[str] = []
for path in required:
    if not path.exists():
        errors.append(f"missing stable feed/posts owner: {path.relative_to(ROOT)}")

legacy_imports = (
    "import com.nova.app.core.network.NovaPost\n",
    "import com.nova.app.core.network.NovaPostPage\n",
    "import com.nova.app.core.network.NovaComment\n",
    "import com.nova.app.core.network.NovaCommentMutation\n",
)

# These paths are the completed feed/posts/comments slice. Other social features
# may use the explicit compatibility aliases until their own Phase 4 slice moves.
consolidated_paths = [
    MAIN / "NovaApp.kt",
    MAIN / "feature/feed",
    MAIN / "feature/home",
    MAIN / "feature/post",
    MAIN / "feature/posts",
]

for entry in consolidated_paths:
    files = [entry] if entry.is_file() else entry.rglob("*.kt")
    for path in files:
        text = path.read_text(encoding="utf-8")
        for forbidden in legacy_imports:
            if forbidden in text:
                errors.append(
                    f"legacy post model import in consolidated slice: {path.relative_to(ROOT)} -> {forbidden.strip()}"
                )

api = MAIN / "core/network/NovaApiClient.kt"
api_text = api.read_text(encoding="utf-8")
for declaration in (
    "data class NovaPost(",
    "data class NovaPostPage(",
    "data class NovaComment(",
    "data class NovaCommentMutation(",
):
    if declaration in api_text:
        errors.append(f"NovaApiClient still owns post model declaration: {declaration}")

comments_screen = MAIN / "feature/post/PostCommentsScreen.kt"
comments_text = comments_screen.read_text(encoding="utf-8")
for forbidden in ("NovaFeedRepository", "PostRepository", "deleteCommentReply("):
    if forbidden in comments_text:
        errors.append(f"PostCommentsScreen still owns transport: {forbidden}")

home = MAIN / "feature/home/HomeScreen.kt"
home_text = home.read_text(encoding="utf-8")
if "NovaFeedRepository" in home_text:
    errors.append("HomeScreen still constructs/depends on NovaFeedRepository")
if "onResolvePost" not in home_text:
    errors.append("HomeScreen must resolve push-target posts through its callback boundary")

app = MAIN / "NovaApp.kt"
app_text = app.read_text(encoding="utf-8")
for required_text in (
    "appContainer.postDataRepository",
    "PostCommentsStateOwner",
    "onResolvePost = postRepository::post",
    "onSendReply = commentsOwner::sendReply",
    "onDeleteReply = commentsOwner::deleteReply",
):
    if required_text not in app_text:
        errors.append(f"NovaApp missing stable feed/posts wiring: {required_text}")

if errors:
    print("Feed/posts architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Feed/posts architecture check passed.")
