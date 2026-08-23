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
    ):
        if seam not in text:
            errors.append(f"Pulse surface bypassed DS-4 seam in {path.relative_to(ROOT)}: {seam}")
    if "Color(0x" in text:
        errors.append(f"Pulse surface restored raw media colors outside PulseTheme: {path.relative_to(ROOT)}")

for path, text in ((PULSE_RAIL, pulse_rail), (PULSE_VIEWER, pulse_viewer)):
    if "NovaMotion.standard" not in text:
        errors.append(f"Pulse surface must adopt Nova motion role: {path.relative_to(ROOT)}")

if errors:
    print("Alive feature design-system check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Alive feature design-system check passed.")
