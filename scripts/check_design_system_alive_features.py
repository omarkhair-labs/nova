#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java/com/nova/app"

TONIGHT_THEME = MAIN / "feature/tonight/TonightTheme.kt"
TONIGHT_SURFACE = MAIN / "feature/tonight/TonightSurface.kt"
PULSE_THEME = MAIN / "feature/pulse/PulseTheme.kt"
PULSE_RAIL = MAIN / "feature/pulse/PulseRail.kt"
PULSE_VIEWER = MAIN / "feature/pulse/PulseViewerDialog.kt"
ORBIT_RAIL = MAIN / "feature/orbit/OrbitRail.kt"
ROOMS_RAIL = MAIN / "feature/rooms/RoomsRail.kt"
ROOMS_SCREEN = MAIN / "feature/rooms/RoomsScreen.kt"
MEMORY_THEME = MAIN / "feature/memories/MemoryTheme.kt"
MEMORIES_RAIL = MAIN / "feature/memories/MemoriesRail.kt"

errors: list[str] = []


def read(path: Path) -> str:
    if not path.is_file():
        errors.append(f"missing DS-4 design-system owner: {path.relative_to(ROOT)}")
        return ""
    return path.read_text(encoding="utf-8")


tonight_theme = read(TONIGHT_THEME)
tonight_surface = read(TONIGHT_SURFACE)
pulse_theme = read(PULSE_THEME)
pulse_rail = read(PULSE_RAIL)
pulse_viewer = read(PULSE_VIEWER)
orbit_rail = read(ORBIT_RAIL)
rooms_rail = read(ROOMS_RAIL)
rooms_screen = read(ROOMS_SCREEN)
memory_theme = read(MEMORY_THEME)
memories_rail = read(MEMORIES_RAIL)

for seam in (
    "data class TonightPalette(",
    "object TonightTheme",
    "val live = TonightPalette(",
    "mediaVideoBackground",
    "mediaTextBackground",
):
    if seam not in tonight_theme:
        errors.append(f"Tonight palette owner seam changed: {seam}")

for seam in (
    "TonightTheme.live",
    "NovaMotion.standard",
    "NovaType.",
    "MaterialTheme.shapes",
):
    if seam not in tonight_surface:
        errors.append(f"Tonight surface bypassed DS-4 seam: {seam}")
if "Color(0x" in tonight_surface:
    errors.append("Tonight surface restored raw feature colors outside TonightTheme")

for seam in (
    "data class PulseMediaPalette(",
    "object PulseTheme",
    "val media = PulseMediaPalette(",
    "overlay",
    "panelBorder",
):
    if seam not in pulse_theme:
        errors.append(f"Pulse palette owner seam changed: {seam}")

for path, text in ((PULSE_RAIL, pulse_rail), (PULSE_VIEWER, pulse_viewer)):
    for seam in (
        "PulseTheme.media",
        "NovaType.",
        "MaterialTheme.shapes",
        "NovaMotion.standard",
    ):
        if seam not in text:
            errors.append(f"Pulse surface bypassed DS-4 seam in {path.relative_to(ROOT)}: {seam}")
    if "Color(0x" in text:
        errors.append(f"Pulse surface restored raw media colors outside PulseTheme: {path.relative_to(ROOT)}")

for seam in (
    "Surface(",
    "NovaOrbitRing(",
    "OrbitPersonRailItem(",
    "NovaType.",
    "NovaSpacing.",
    "MaterialTheme.shapes",
    "NovaMotion.standard",
):
    if seam not in orbit_rail:
        errors.append(f"Orbit rail bypassed DS-4 seam: {seam}")

for path, text in ((ROOMS_RAIL, rooms_rail), (ROOMS_SCREEN, rooms_screen)):
    for seam in ("NovaType.", "NovaSpacing.", "MaterialTheme.shapes"):
        if seam not in text:
            errors.append(f"Rooms surface bypassed DS-4 seam in {path.relative_to(ROOT)}: {seam}")
if "NovaCard(" not in rooms_rail or "NovaMotion.standard" not in rooms_rail:
    errors.append("Rooms rail must use NovaCard and NovaMotion")
# Flow 7 deliberately flattens Rooms discovery rows instead of forcing every
# ordinary list item back into NovaCard. Keep the shared Nova navigation/state
# primitives and the icon action seam alive while allowing Surface-backed rows.
for seam in (
    "NovaBackButton(",
    "NovaLoadingState(",
    "NovaEmptyState(",
    "NovaErrorState(",
    "NovaInlineRetry(",
    "NovaIconButton(",
    "Surface(",
):
    if seam not in rooms_screen:
        errors.append(f"Rooms screen bypassed shared ordinary-screen presentation: {seam}")
if 'text = "‹"' in rooms_screen:
    errors.append("Rooms screen restored legacy text back control")

for seam in (
    "data class MemoryPalette(",
    "object MemoryTheme",
    "val ready = MemoryPalette(",
    "videoBackground",
):
    if seam not in memory_theme:
        errors.append(f"Memory palette owner seam changed: {seam}")
for seam in (
    "MemoryTheme.ready",
    "NovaCard(",
    "NovaType.",
    "NovaSpacing.",
    "MaterialTheme.shapes",
    "NovaMotion.standard",
):
    if seam not in memories_rail:
        errors.append(f"Memories rail bypassed DS-4 seam: {seam}")
if "Color(0x" in memories_rail:
    errors.append("Memories rail restored raw reflective colors outside MemoryTheme")

if errors:
    print("Alive feature design-system check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Alive feature design-system check passed.")
