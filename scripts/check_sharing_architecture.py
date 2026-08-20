#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/sharing/data/SharingRepository.kt"
ADAPTER = ROOT / "app/src/main/java/com/nova/app/feature/sharing/data/remote/CoreSharingRepositoryAdapter.kt"
STATE_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/sharing/SharingStateOwner.kt"
STATE_TEST = ROOT / "app/src/test/java/com/nova/app/feature/sharing/SharingStateOwnerTest.kt"
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
state_owner = read(STATE_OWNER)
state_test = read(STATE_TEST)
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

for required in (
    "sealed interface SharingTarget",
    "data class Post(val id: Long) : SharingTarget",
    "data class Profile(val username: String) : SharingTarget",
    "data class Reel(val id: Long) : SharingTarget",
    "data class SharingUiState(",
    "class SharingStateOwner(",
    "private val messagesRepository: MessagesRepository",
    "private val peopleRepository: PeopleRepository",
    "private val sharingRepository: SharingRepository",
    "private var searchJob: Job? = null",
    "searchJob?.cancel()",
    "delay(SEARCH_DEBOUNCE_MS)",
    "internal const val SEARCH_DEBOUNCE_MS = 220L",
    "internal const val QUERY_MAX_LENGTH = 60",
    "value.take(QUERY_MAX_LENGTH)",
    "messagesRepository.conversations(query)",
    "peopleRepository.people(query)",
    ".filterNot { it.isGroup }",
    ".mapTo(mutableSetOf()) { it.otherUser.username.lowercase() }",
    "error = state.error ?: result.message",
    "if (state.busy) return",
    "if (!canAddToStory || state.busy) return",
    'message = "Sent to @${person.username}"',
    'message = "Sent to ${conversation.displayName}"',
    '"Added to your Close Friends Story"',
    '"Added to your Story"',
):
    if required not in state_owner:
        errors.append(f"Sharing state owner is missing characterized behavior seam: {required}")

# All failures, including 401, remain inline in the share dialog; no terminal-session effect belongs here.
for forbidden in (
    "sessionExpiryVersion",
    "onSessionExpired",
    "statusCode == 401",
):
    if forbidden in state_owner:
        errors.append(f"Sharing state owner must keep failures inline and not add terminal-session handling: {forbidden}")

for test_seam in (
    "search keeps 220ms contract and caps query at sixty characters",
    "search loads conversations first then filters matching direct people",
    "conversation failure remains visible when people search later succeeds",
    "people failure becomes inline error when conversations succeeded",
    "direct conversation shares to person while group shares by conversation id",
    "typed target routes post profile and reel sends to matching operations",
    "story success copy and profile no-op preserve current eligibility semantics",
    "all share failures including 401 stay inline",
    "global busy lock blocks competing story action while send is in flight",
):
    if test_seam not in state_test:
        errors.append(f"Sharing state-owner characterization is missing test: {test_seam}")

# Production transport remains unchanged during the live state-owner switch.
for required in (
    "data class NovaRepostState(",
    "suspend fun repostState(",
    "suspend fun setReposted(",
    'path = "shares/messages/"',
    'path = "stories/"',
    '.put("recipient_username", recipientUsername.trim().lowercase())',
    '.put("conversation_id", conversationId)',
    'targetKey = "shared_post_id"',
    'targetKey = "shared_reel_id"',
    '.put("caption", caption.trim().take(240))',
    'audience.takeIf { it == "followers" || it == "close_friends" }',
):
    if required not in core_repository:
        errors.append(f"Sharing transport-sensitive seam changed or disappeared: {required}")

# Stable dependencies remain the exact implementations that backed the pre-switch dialog.
if "suspend fun people(query: String = \"\")" not in people_contract:
    errors.append("PeopleRepository must expose the existing people(query) search seam")
if "interface MessagesRepository" not in messages_contract or "suspend fun conversations(query: String = \"\")" not in messages_contract:
    errors.append("MessagesRepository must expose the existing conversation-search seam")
if "val messagingRepository: MessagesRepository = NovaMessagingRepository(" not in container:
    errors.append("AppContainer messagingRepository must remain the current NovaMessagingRepository implementation")
if "val peopleRepository: PeopleRepository = socialRepository" not in container:
    errors.append("AppContainer peopleRepository must remain the current NovaSocialRepository implementation")

# The live dialog must now be a thin AppContainer/state-owner renderer while preserving its visual/external-share tree.
for required in (
    "import com.nova.app.app.appContainer",
    "import com.nova.app.feature.people.domain.model.NovaPerson",
    "SharingTarget.Post(postId)",
    "SharingTarget.Reel(reelId)",
    "SharingTarget.Profile(profileUsername.orEmpty())",
    "SharingStateOwner(",
    "messagesRepository = container.messagingRepository",
    "peopleRepository = container.peopleRepository",
    "sharingRepository = container.sharingRepository",
    "LaunchedEffect(owner)",
    "owner.start()",
    "val state = owner.state",
    "val busy = state.busy",
    "val canAddToStory = owner.canAddToStory",
    "value = state.query",
    "onValueChange = owner::setQuery",
    'owner.addToStory("followers")',
    'owner.addToStory("close_friends")',
    "owner.sendToConversation(conversation)",
    "owner.sendToPerson(person)",
    "state.busyConversationId == conversation.id",
    "state.busyUsername == person.username",
    "state.message?.takeIf",
    "state.error?.takeIf",
    'Text("Share outside Nova"',
    'Text("Use Android\'s share menu"',
    "if (!busy) {",
    "onExternalShare()",
):
    if required not in dialog:
        errors.append(f"live Sharing dialog is missing stable owner/rendering seam: {required}")

for forbidden in (
    "NovaSocialRepository",
    "NovaMessagingRepository",
    "NovaSharingRepository",
    "com.nova.app.core.network.ApiResult",
    "com.nova.app.core.network.NovaPerson",
    "mutableStateOf",
    "scope.launch",
    "delay(220)",
    "messagingRepository.conversations(",
    "socialRepository.people(",
    "sharingRepository.sharePost(",
    "sharingRepository.shareReel(",
    "sharingRepository.shareProfile(",
):
    if forbidden in dialog:
        errors.append(f"live Sharing dialog must not retain pre-switch repository/orchestration dependency: {forbidden}")

if errors:
    print("Sharing architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Sharing architecture check passed.")
