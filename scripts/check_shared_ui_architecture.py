#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java"
SHADOW_ANDROIDX = MAIN / "androidx"
CALL_ACTIVITY = MAIN / "com/nova/app/CallActivity.kt"
ICON_ALIASES = MAIN / "com/nova/app/ui/icons/NovaMaterialIconAliases.kt"
SHARED_COMPONENTS = MAIN / "com/nova/app/ui/components"
LEGACY_COMPONENTS = SHARED_COMPONENTS / "NovaComponents.kt"
COMMUNICATION_ICON_ALIASES = ("CallEnd", "Mic", "Videocam", "VolumeUp")
NARROW_COMPONENT_SEAMS = {
    "NovaButtons.kt": ("fun NovaPrimaryButton(", "fun NovaSecondaryButton("),
    "NovaTextField.kt": ("fun NovaTextField(",),
    "NovaHeader.kt": ("fun NovaHeader(",),
    "NovaBottomBar.kt": ("fun NovaBottomBar(", "enum class NovaTab"),
}

errors: list[str] = []

shadow_sources = list(SHADOW_ANDROIDX.rglob("*.kt")) if SHADOW_ANDROIDX.exists() else []
for path in shadow_sources:
    errors.append(
        "application source must not shadow the androidx namespace: "
        f"{path.relative_to(ROOT)}"
    )

for source in MAIN.rglob("*.kt"):
    text = source.read_text(encoding="utf-8")
    for import_name in COMMUNICATION_ICON_ALIASES:
        forbidden = f"import androidx.compose.material.icons.filled.{import_name}"
        if forbidden in text:
            errors.append(
                "communication icon alias must be imported from app-owned ui.icons: "
                f"{source.relative_to(ROOT)} -> {forbidden}"
            )

if LEGACY_COMPONENTS.exists():
    errors.append(
        "shared UI must stay split by responsibility; legacy dumping-ground file exists: "
        f"{LEGACY_COMPONENTS.relative_to(ROOT)}"
    )

for file_name, seams in NARROW_COMPONENT_SEAMS.items():
    component_file = SHARED_COMPONENTS / file_name
    if not component_file.is_file():
        errors.append(f"missing narrow shared UI owner: {component_file.relative_to(ROOT)}")
        continue
    component_text = component_file.read_text(encoding="utf-8")
    for seam in seams:
        if seam not in component_text:
            errors.append(f"shared UI public seam moved unexpectedly: {file_name} -> {seam}")

if not ICON_ALIASES.is_file():
    errors.append("missing app-owned communication icon aliases")
else:
    aliases = ICON_ALIASES.read_text(encoding="utf-8")
    if "package com.nova.app.ui.icons" not in aliases:
        errors.append("communication icon aliases must stay in the app-owned ui.icons namespace")
    for seam in (
        "val Icons.Filled.CallEnd: ImageVector",
        "val Icons.Filled.Mic: ImageVector",
        "val Icons.Filled.Videocam: ImageVector",
        "val Icons.Filled.VolumeUp: ImageVector",
        "NovaCommunicationIcons.CallEnd",
        "NovaCommunicationIcons.Mic",
        "NovaCommunicationIcons.Video",
        "NovaCommunicationIcons.VolumeUp",
    ):
        if seam not in aliases:
            errors.append(f"app-owned communication icon alias seam changed: {seam}")

activity = CALL_ACTIVITY.read_text(encoding="utf-8")
for import_name in COMMUNICATION_ICON_ALIASES:
    required = f"import com.nova.app.ui.icons.{import_name}"
    if required not in activity:
        errors.append(f"CallActivity must import app-owned icon alias: {required}")

for usage in (
    "Icons.Filled.CallEnd",
    "Icons.Filled.Mic",
    "Icons.Filled.Videocam",
    "Icons.Filled.VolumeUp",
):
    if usage not in activity:
        errors.append(f"CallActivity icon usage changed unexpectedly: {usage}")

if errors:
    print("Shared UI architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Shared UI architecture check passed.")
