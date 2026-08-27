#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]

MODELS = ROOT / "app/src/main/java/com/nova/app/feature/orbit/domain/model/OrbitModels.kt"
CONTRACT = ROOT / "app/src/main/java/com/nova/app/feature/orbit/data/OrbitRepository.kt"
PARSER = ROOT / "app/src/main/java/com/nova/app/feature/orbit/data/OrbitParser.kt"
REMOTE = ROOT / "app/src/main/java/com/nova/app/feature/orbit/data/remote/OrbitRemoteRepository.kt"
OWNER = ROOT / "app/src/main/java/com/nova/app/feature/orbit/OrbitStateOwner.kt"
RAIL = ROOT / "app/src/main/java/com/nova/app/feature/orbit/OrbitRail.kt"
SCREEN = ROOT / "app/src/main/java/com/nova/app/feature/orbit/OrbitScreen.kt"
CONTAINER = ROOT / "app/src/main/java/com/nova/app/app/AppContainer.kt"
HOME = ROOT / "app/src/main/java/com/nova/app/feature/home/HomeScreen.kt"
NETWORK = ROOT / "app/src/main/java/com/nova/app/core/network/NovaApiClient.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.exists():
        errors.append(f"missing required Orbit architecture file: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


models = read(MODELS)
contract = read(CONTRACT)
parser = read(PARSER)
remote = read(REMOTE)
owner = read(OWNER)
rail = read(RAIL)
screen = read(SCREEN)
container = read(CONTAINER)
home = read(HOME)
network = read(NETWORK)

for declaration in (
    "data class OrbitPerson(",
    "data class OrbitPost(",
    "data class OrbitPulse(",
    "data class OrbitEvent(",
    "data class OrbitPage(",
):
    if declaration not in models:
        errors.append(f"Orbit domain owner is missing {declaration}")
    if declaration in network:
        errors.append(f"shared network core must not own Orbit model: {declaration}")

if "interface OrbitRepository" not in contract:
    errors.append("stable OrbitRepository contract is missing")
if "suspend fun orbit(" not in contract:
    errors.append("OrbitRepository must own paged Orbit reads")

if "internal fun parseOrbitPage(" not in parser:
    errors.append("Orbit wire parsing must remain feature-owned")

if ") : OrbitRepository" not in remote:
    errors.append("OrbitRemoteRepository must implement OrbitRepository directly")
for required in (
    '"orbit/?cursor=',
    '?: "orbit/"',
    "NovaSessionStore",
    "parseOrbitPage(",
):
    if required not in remote:
        errors.append(f"Orbit remote implementation is missing protected seam: {required}")

for forbidden in ("orbit/", "OrbitEvent", "OrbitRepository"):
    if forbidden in network:
        errors.append(f"NovaApiClient must remain generic and must not own Orbit concern: {forbidden}")

if "class OrbitStateOwner(" not in owner:
    errors.append("OrbitStateOwner must own Orbit async state")
if "private val repository: OrbitRepository" not in owner:
    errors.append("OrbitStateOwner must depend on OrbitRepository")
for required in (
    "sessionExpiryVersion",
    "loadNow(",
    "loadMoreNow(",
    "nextCursor",
    "loadingMore",
):
    if required not in owner:
        errors.append(f"OrbitStateOwner is missing state seam: {required}")

for required in (
    "context.appContainer.orbitRepository",
    "OrbitStateOwner(repository, scope)",
    "OrbitPersonRailItem(",
    "OrbitMoreCard(",
    "showLivePoint = event.pulse != null",
    "orbitIcon(",
):
    if required not in rail:
        errors.append(f"Orbit live UI is missing stable wiring: {required}")
for forbidden in (
    "NovaApiClient",
    "ApiResult",
    "repository.orbit(",
):
    if forbidden in rail:
        errors.append(f"Orbit UI must not own network/repository orchestration: {forbidden}")

for required in (
    "fun OrbitScreen(",
    "OrbitStateOwner(repository, scope)",
    "NovaBottomBar(",
    "NovaTab.Orbit",
    "OrbitConstellation(",
    "OrbitActivityRow(",
    "onPostClick: (Long) -> Unit",
    "showLivePoint = events.any { it.pulse != null }",
):
    if required not in screen:
        errors.append(f"full Orbit destination is missing stable visual/navigation seam: {required}")
for forbidden in ("NovaApiClient", "ApiResult", "repository.orbit("):
    if forbidden in screen:
        errors.append(f"full Orbit destination must not own transport orchestration: {forbidden}")

if "val orbitRepository: OrbitRepository = OrbitRemoteRepository(appContext, api)" not in container:
    errors.append("AppContainer must construct OrbitRemoteRepository behind OrbitRepository")

if "import com.nova.app.feature.orbit.OrbitRail" not in home:
    errors.append("Home must import the Orbit surface")
if "OrbitRail(" not in home:
    errors.append("Home must render Orbit")
if home.find("OrbitRail(") < home.find("PulseRail("):
    errors.append("Home hierarchy must keep Pulse before Orbit")
if home.find("OrbitRail(") > home.find("onClick = onCreatePost"):
    errors.append("Home hierarchy must keep Orbit before permanent post creation")

if errors:
    print("Orbit architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Orbit architecture check passed.")
