#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/pulse/domain/model/PulseModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/pulse/data/PulseRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/pulse/data/PulseParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/pulse/data/remote/PulseRemoteRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/pulse/PulseStateOwner.kt"
VIEWER_OWNER = ROOT / "app/src/main/java/com/nova/app/feature/pulse/PulseViewerStateOwner.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/pulse/PulseRail.kt"
COMPOSER = ROOT / "app/src/main/java/com/nova/app/feature/pulse/PulseComposerDialog.kt"
VIEWER = ROOT / "app/src/main/java/com/nova/app/feature/pulse/PulseViewerDialog.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Pulse architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
parser = read(PARSER)
remote = read(REMOTE)
owner = read(OWNER)
viewer_owner = read(VIEWER_OWNER)
rail = read(RAIL)
composer = read(COMPOSER)
viewer = read(VIEWER)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)

for declaration in ("data class NovaPulseAuthor(", "data class NovaPulse("):
    if declaration not in models:
        errors.append(f"Pulse domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Pulse model: {declaration}")
for field in ("val replyToId: Long?", "val chainRootId: Long?"):
    if field not in models:
        errors.append(f"Pulse domain model is missing moment-chain metadata: {field}")

if "interface PulseRepository" not in contract:
    errors.append("stable PulseRepository contract is missing")
for method in (
    "suspend fun pulses()",
    "suspend fun createTextPulse(",
    "suspend fun createMediaPulse(",
    "suspend fun pulseChain(",
    "suspend fun replyTextPulse(",
    "suspend fun replyMediaPulse(",
    "suspend fun deletePulse(",
):
    if method not in contract:
        errors.append(f"PulseRepository is missing current operation: {method}")

if "internal fun parseNovaPulse(" not in parser:
    errors.append("Pulse wire parsing must remain feature-owned")
for field in ('"reply_to_id"', '"chain_root_id"'):
    if field not in parser:
        errors.append(f"Pulse parser is missing server chain field: {field}")

if ") : PulseRepository" not in remote:
    errors.append("PulseRemoteRepository must implement PulseRepository directly")
for required in (
    'api.requestJson("pulses/"',
    'path = "pulses/"',
    'path = "pulses/$pulseId/chain/"',
    'path = "pulses/$pulseId/reply/"',
    "api.requestMultipart(",
    'path = "pulses/$pulseId/"',
    "NovaSessionStore",
    "createTextAt(",
    "createMediaAt(",
):
    if required not in remote:
        errors.append(f"Pulse remote implementation is missing protected seam: {required}")

for forbidden in ("pulses/", "NovaPulse", "PulseRepository"):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Pulse concern: {forbidden}")

if "class PulseStateOwner(" not in owner:
    errors.append("PulseStateOwner must own Pulse rail async state")
if "private val repository: PulseRepository" not in owner:
    errors.append("PulseStateOwner must depend on PulseRepository")
for required in (
    "sessionExpiryVersion",
    "createdVersion",
    "createTextNow(",
    "createMediaNow(",
    "deleteNow(",
):
    if required not in owner:
        errors.append(f"PulseStateOwner is missing state seam: {required}")

if "class PulseViewerStateOwner(" not in viewer_owner:
    errors.append("PulseViewerStateOwner must own moment-chain async state")
if "private val repository: PulseRepository" not in viewer_owner:
    errors.append("PulseViewerStateOwner must depend on PulseRepository")
for required in (
    "loadChainNow(",
    "replyTextNow(",
    "replyMediaNow(",
    "replyCreatedVersion",
    "sessionExpiryVersion",
):
    if required not in viewer_owner:
        errors.append(f"PulseViewerStateOwner is missing chain state seam: {required}")

for required in (
    "context.appContainer.pulseRepository",
    "PulseStateOwner(repository, scope)",
    "PulseViewerStateOwner(pulse, repository, scope)",
    "PulseComposerDialog(",
    "PulseViewerDialog(",
):
    if required not in rail:
        errors.append(f"Pulse live UI is missing stable wiring: {required}")

if "fun PulseComposerDialog(" not in composer:
    errors.append("Pulse composer must remain a reusable feature-owned surface")
if "fun PulseViewerDialog(" not in viewer:
    errors.append("Pulse chain viewer must remain feature-owned")
for required in (
    "PulseComposerDialog(",
    "Moment chain",
    "Reply with a moment",
):
    if required not in viewer:
        errors.append(f"Pulse viewer is missing moment-chain UX seam: {required}")

for ui_path, text in ((RAIL, rail), (COMPOSER, composer), (VIEWER, viewer)):
    for forbidden in (
        "NovaApiClient",
        "ApiResult",
        "repository.pulses()",
        "repository.createTextPulse(",
        "repository.createMediaPulse(",
        "repository.pulseChain(",
        "repository.replyTextPulse(",
        "repository.replyMediaPulse(",
        "repository.deletePulse(",
    ):
        if forbidden in text:
            errors.append(
                f"Pulse UI must not own network/repository orchestration in {ui_path.relative_to(ROOT)}: {forbidden}"
            )

if "val pulseRepository: PulseRepository = PulseRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct PulseRemoteRepository behind PulseRepository")

if "import com.nova.app.feature.pulse.PulseRail" not in home:
    errors.append("Home must import the Pulse surface")
if "PulseRail(" not in home:
    errors.append("Home must render Pulse")
if home.find("PulseRail(") > home.find("StoriesRail("):
    errors.append("Home live hierarchy must keep Pulse before Stories")

if errors:
    print("Pulse architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Pulse architecture check passed.")
