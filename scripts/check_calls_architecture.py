#!/usr/bin/env python3
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app/src/main/java"

REQUIRED = [
    "app/src/main/java/com/nova/app/feature/calls/CallStateOwner.kt",
    "app/src/main/java/com/nova/app/feature/calls/data/CallRepository.kt",
    "app/src/main/java/com/nova/app/feature/calls/domain/model/CallModels.kt",
    "app/src/main/java/com/nova/app/feature/calls/signaling/CallSignaling.kt",
    "app/src/main/java/com/nova/app/feature/calls/webrtc/CallWebRtcEngine.kt",
    "app/src/main/java/com/nova/app/feature/calls/webrtc/model/CallAudioQuality.kt",
]

FORBIDDEN_PATHS = [
    "app/src/main/java/com/nova/app/core/calls/NovaCallController.kt",
    "app/src/main/java/com/nova/app/core/calls/CallModelCompatibility.kt",
    "app/src/main/java/com/nova/app/core/calls/NovaCallAudioQuality.kt",
]

FORBIDDEN_CORE_IMPORTS = [
    "com.nova.app.core.calls.NovaCallController",
    "com.nova.app.core.calls.NovaCallKind",
    "com.nova.app.core.calls.NovaCallStatus",
    "com.nova.app.core.calls.NovaCallPerson",
    "com.nova.app.core.calls.NovaCallSession",
    "com.nova.app.core.calls.NovaIceServer",
    "com.nova.app.core.calls.NovaIceConfig",
    "com.nova.app.core.calls.NovaCallAudioQualitySnapshot",
    "com.nova.app.core.calls.NovaCallAudioQualityDelta",
    "com.nova.app.core.calls.NovaCallSignalEvent",
    "com.nova.app.core.calls.NovaCallSocketStatus",
]

errors: list[str] = []

for rel in REQUIRED:
    if not (ROOT / rel).is_file():
        errors.append(f"missing stable Calls owner: {rel}")

for rel in FORBIDDEN_PATHS:
    if (ROOT / rel).exists():
        errors.append(f"legacy Calls file must stay deleted: {rel}")

activity = ROOT / "app/src/main/java/com/nova/app/CallActivity.kt"
activity_text = activity.read_text(encoding="utf-8")
if "CallStateOwner" not in activity_text:
    errors.append("CallActivity must delegate live call state to CallStateOwner")
if "NovaCallController" in activity_text:
    errors.append("CallActivity must not reference legacy NovaCallController")

signaling = ROOT / "app/src/main/java/com/nova/app/feature/calls/signaling/CallSignaling.kt"
signaling_text = signaling.read_text(encoding="utf-8")
for declaration in ("enum class NovaCallSocketStatus", "sealed interface NovaCallSignalEvent"):
    if declaration not in signaling_text:
        errors.append(f"stable signaling ownership missing: {declaration}")

for path in MAIN.rglob("*.kt"):
    text = path.read_text(encoding="utf-8")
    for target in FORBIDDEN_CORE_IMPORTS:
        if f"import {target}" in text:
            errors.append(f"legacy Calls import in {path.relative_to(ROOT)}: {target}")

if errors:
    print("Calls architecture check failed:")
    for error in errors:
        print(f"- {error}")
    sys.exit(1)

print("Calls architecture check passed.")
