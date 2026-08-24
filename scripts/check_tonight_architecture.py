#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/tonight/domain/model/TonightModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/tonight/data/TonightRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/tonight/data/TonightParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/tonight/data/remote/TonightRemoteRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/tonight/TonightStateOwner.kt"
SURFACE = ROOT / "app/src/main/java/com/nova/app/feature/tonight/TonightSurface.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/tonight/TonightScreen.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Tonight architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
parser = read(PARSER)
remote = read(REMOTE)
owner = read(OWNER)
surface = read(SURFACE)
screen = read(SCREEN)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)

for declaration in (
    "data class TonightPerson(",
    "data class TonightPulse(",
    "data class TonightPersonRow(",
    "data class TonightSnapshot(",
):
    if declaration not in models:
        errors.append(f"Tonight domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Tonight model: {declaration}")

if "interface TonightRepository" not in contract:
    errors.append("stable TonightRepository contract is missing")
if "suspend fun tonight(utcOffsetMinutes: Int)" not in contract:
    errors.append("TonightRepository must own local-offset Tonight reads")

if "internal fun parseTonightSnapshot(" not in parser:
    errors.append("Tonight wire parsing must remain feature-owned")

if ") : TonightRepository" not in remote:
    errors.append("TonightRemoteRepository must implement TonightRepository directly")
for required in (
    'path = "tonight/?utc_offset_minutes=$utcOffsetMinutes"',
    "NovaSessionStore",
    "parseTonightSnapshot(",
):
    if required not in remote:
        errors.append(f"Tonight remote implementation is missing protected seam: {required}")

for forbidden in ("tonight/", "TonightSnapshot", "TonightRepository"):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Tonight concern: {forbidden}")

if "class TonightStateOwner(" not in owner:
    errors.append("TonightStateOwner must own Tonight async state")
if "private val repository: TonightRepository" not in owner:
    errors.append("TonightStateOwner must depend on TonightRepository")
for required in (
    "sessionExpiryVersion",
    "loadNow(",
    "utcOffsetMinutes",
):
    if required not in owner:
        errors.append(f"TonightStateOwner is missing state seam: {required}")

for required in (
    "context.appContainer.tonightRepository",
    "TonightStateOwner(repository, scope)",
    "TimeZone.getDefault()",
    "millisUntilTonightBoundary()",
    "delay(millisUntilTonightBoundary())",
    "TonightLiveCard(",
    "TonightSleepingCard(",
    "TonightPersonCard(",
):
    if required not in surface:
        errors.append(f"Tonight Home surface is missing stable wiring: {required}")
for forbidden in (
    "NovaApiClient",
    "ApiResult",
    "repository.tonight(",
    "presence_store",
    "is_online(",
):
    if forbidden in surface:
        errors.append(f"Tonight UI must not own transport or raw presence concern: {forbidden}")

for required in (
    "fun TonightScreen(",
    "TonightSurface(",
    "RoomTonightSection(",
    "NovaBottomBar(",
):
    if required not in screen:
        errors.append(f"full Tonight destination is missing stable visual/navigation seam: {required}")

if "val tonightRepository: TonightRepository = TonightRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct TonightRemoteRepository behind TonightRepository")

if "import com.nova.app.feature.tonight.TonightSurface" not in home:
    errors.append("Home must import Tonight surface")
if "TonightSurface(" not in home:
    errors.append("Home must render Tonight")
if home.find("TonightSurface(") > home.find("PulseRail("):
    errors.append("Home live hierarchy must keep Tonight before Pulse")
if home.find("PulseRail(") > home.find("OrbitRail("):
    errors.append("Home live hierarchy must keep Pulse before Orbit")

if errors:
    print("Tonight architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Tonight architecture check passed.")
