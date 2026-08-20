#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/sharing/data/SharingRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/sharing/data/remote/CoreSharingRepositoryAdapter.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
CORE_REPOSITORY = ROOT / "app/src/main/java/com/nova/app/core/sharing/NovaSharingRepository.kt"
DIALOG = ROOT / "app/src/main/java/com/nova/app/feature/sharing/NovaShareDialog.kt"
PEOPLE_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/people/data/PeopleRepository.kt"
MESSAGES_CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/messages/data/MessagesRepository.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Sharing architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


contract = read(CONTRACT)
adapter = read(ADAPTER)
container = read(CONTAINER)
core_repository = read(CORE_REPOSITORY)
dialog = read(DIALOG)
people_contract = read(PEOPLE_CONTRACT)
messages_contract = read(MESSAGES_CONTRACT)

if "interface SharingRepository" not in contract:
    errors.append("stable SharingRepository contract is missing")

for operation in (
    "suspend fun sharePost(",
    "suspend fun shareReel(",
    "suspend fun shareProfile(",
    "suspend fun sharePostToConversation(",
    "suspend fun shareReelToConversation(",
    "suspend fun shareProfileToConversation(",
    "suspend fun addPostToStory(",
    "suspend fun addReelToStory(",
):
    if operation not in contract:
        errors.append(f"stable Sharing contract is missing dialog operation: {operation}")

for forbidden in (
    "repostState(",
    "setReposted(",
    "NovaRepostState",
):
    if forbidden in contract:
        errors.append(f"Sharing dialog contract must not absorb post-repost ownership: {forbidden}")

if "class CoreSharingRepositoryAdapter(context: Context) : SharingRepository" not in adapter:
    errors.append("Sharing production adapter must implement SharingRepository")
if "private val delegate = NovaSharingRepository(context.applicationContext)" not in adapter:
    errors.append("Sharing adapter must delegate to the existing production transport")
for delegated in (
    "delegate.sharePost(",
    "delegate.shareReel(",
    "delegate.shareProfile(",
    "delegate.sharePostToConversation(",
    "delegate.shareReelToConversation(",
    "delegate.shareProfileToConversation(",
    "delegate.addPostToStory(",
    "delegate.addReelToStory(",
):
    if delegated not in adapter:
        errors.append(f"Sharing adapter is missing delegation: {delegated}")

if "val sharingRepository: SharingRepository = CoreSharingRepositoryAdapter(appContext)" not in container:
    errors.append("AppContainer is missing the stable Sharing construction seam")

# The production transport remains unchanged in this boundary-only PR.
for required in (
    "data class NovaRepostState(",
    "suspend fun repostState(",
    "suspend fun setReposted(",
    'path = "shares/messages/"',
    'path = "stories/"',
    '.put("recipient_username", recipientUsername.trim().lowercase())',
    '.put("conversation_id", conversationId)',
    '.put("shared_post_id"',
    '.put("shared_reel_id"',
    '.put("caption", caption.trim().take(240))',
    'audience.takeIf { it == "followers" || it == "close_friends" }',
):
    if required not in core_repository:
        errors.append(f"Sharing transport-sensitive seam changed or disappeared: {required}")

# Stable dependencies already expose the exact live search implementations needed next.
if "suspend fun people(query: String = \"\")" not in people_contract:
    errors.append("PeopleRepository must expose the existing people(query) search seam")
if "interface MessagesRepository" not in messages_contract or "suspend fun conversations(query: String = \"\")" not in messages_contract:
    errors.append("MessagesRepository must expose the existing conversation-search seam")
if "val messagingRepository: MessagesRepository = NovaMessagingRepository(" not in container:
    errors.append("AppContainer messagingRepository must remain the current NovaMessagingRepository implementation")
if "val peopleRepository: PeopleRepository = socialRepository" not in container:
    errors.append("AppContainer peopleRepository must remain the current NovaSocialRepository implementation")

# Boundary PR intentionally does not change the live dialog yet.
for required in (
    "NovaSocialRepository(context.applicationContext)",
    "NovaMessagingRepository(context.applicationContext)",
    "NovaSharingRepository(context.applicationContext)",
    "delay(220)",
    "messagingRepository.conversations(query.trim())",
    "socialRepository.people(query.trim())",
    "sharingRepository.sharePost(",
    "sharingRepository.shareReel(",
    "sharingRepository.shareProfile(",
):
    if required not in dialog:
        errors.append(f"Sharing boundary PR must preserve current live-dialog wiring: {required}")

if errors:
    print("Sharing architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Sharing architecture check passed.")
