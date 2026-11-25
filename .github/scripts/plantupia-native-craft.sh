#!/usr/bin/env bash
# Run only callback-based entrance and signed-out draft tests on a fresh CI AVD.
# Direct instrumentation retains app-private screenshots until they are collected;
# Gradle's connected-test cleanup can uninstall the app before that evidence is read.
set -euo pipefail

case "${ANDROID_SERIAL:-}" in
  emulator-*) ;;
  *) echo '::error::Native craft checks require the disposable CI emulator.' >&2; exit 1 ;;
esac

readonly package='com.omarkhair70.nova'
readonly runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
readonly remote_images='files/craft-evidence'
readonly output='artifacts/native-craft'
readonly entrance='com.nova.app.feature.auth.PlantupiaEntranceInstrumentedTest'
readonly drafts='com.nova.app.feature.plants.PlantupiaCreationDraftInstrumentedTest'
readonly keyboard='com.nova.app.feature.auth.PlantupiaEntranceKeyboardInstrumentedTest'
mkdir -p "$output/test-results" "$output/craft-evidence" "$output/diagnostics"

adb_bounded() { timeout --signal=TERM --kill-after=5s 30s adb "$@"; }

diagnostics() {
  local label="$1"
  local directory="$output/diagnostics/$label"
  mkdir -p "$directory"
  adb_bounded logcat -d -t 5000 > "$directory/logcat.txt" 2>&1 || true
  adb_bounded shell wm size > "$directory/display-size.txt" 2>&1 || true
  adb_bounded shell wm density > "$directory/display-density.txt" 2>&1 || true
  adb_bounded shell settings get system font_scale > "$directory/font-scale.txt" 2>&1 || true
  adb_bounded shell dumpsys input_method > "$directory/input-method.txt" 2>&1 || true
  if ! adb_bounded exec-out screencap -p > "$directory/device.png" 2> "$directory/screencap.txt"; then
    echo "::warning::Diagnostic device screenshot failed for $label; see screencap.txt."
    rm -f "$directory/device.png"
  fi
}

finish() {
  local status=$?
  trap - EXIT
  # The emulator action tears down immediately after this process returns.
  diagnostics exit
  if (( status != 0 )); then
    echo '::error::Native craft execution or required screenshot capture failed. Review native-craft-test-results.'
  fi
  exit "$status"
}
trap finish EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

ensure_offline() {
  adb_bounded shell svc wifi disable
  adb_bounded shell svc data disable
  # API 30's ConnectivityService reports this exact line when no default network
  # exists. Use the supported radio controls; shell cannot send the protected
  # AIRPLANE_MODE broadcast on this image. Do not launch before verifying offline.
  for attempt in {1..10}; do
    adb_bounded shell dumpsys connectivity > "$output/diagnostics/connectivity.txt"
    if grep -Eq '^[[:space:]]*Active default network: none[[:space:]]*$' "$output/diagnostics/connectivity.txt"; then
      return 0
    fi
    sleep 1
  done
  echo '::error::Could not verify an offline emulator; no app tests will be launched.' >&2
  return 1
}

adb_bounded wait-for-device
ensure_offline
timeout --signal=TERM --kill-after=10s 3m adb install -r -t app/build/outputs/apk/debug/app-debug.apk
timeout --signal=TERM --kill-after=10s 3m adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

overall_status=0
run_pass() {
  local label="$1" width="$2" font="$3" classes="$4" expected_tests="$5" limit="$6"
  local directory="$output/test-results/$label"
  local images="$output/craft-evidence/$label"
  mkdir -p "$directory"

  ensure_offline
  adb_bounded shell am force-stop "$package"
  adb_bounded shell pm clear "$package" | tee "$directory/clear-data.txt"
  if ! grep -Eq '^Success[[:space:]]*$' "$directory/clear-data.txt"; then
    echo "::error::Could not reset the disposable app for $label." >&2
    exit 1
  fi
  adb_bounded shell wm size "$(( width * 2 ))x1600"
  adb_bounded shell wm density 320
  adb_bounded shell settings put system font_scale "$font"
  adb_bounded shell settings put secure show_ime_with_hard_keyboard 1
  adb_bounded shell input keyevent KEYCODE_WAKEUP
  adb_bounded shell wm dismiss-keyguard
  adb_bounded logcat -c

  # AndroidJUnitRunner can return an adb exit status of zero after test failure.
  # Keep raw output and validate the exact result summary independently below.
  set +e
  timeout --signal=TERM --kill-after=10s "$limit" adb shell am instrument -w -r \
    -e class "$classes" -e plantupiaCapture true "$runner" 2>&1 | tee "$directory/instrumentation.txt"
  local pipeline_status=("${PIPESTATUS[@]}")
  set -e

  # Collect before another pass clears app data, including after test failure.
  # A collection failure or missing/invalid expected image is a failing result.
  if [[ -e "$images" ]]; then
    echo "::error::Refusing to reuse existing screenshot evidence for $label." >&2
    exit 1
  fi
  local archive
  archive="$(mktemp "${RUNNER_TEMP:?}/plantupia-native-captures.XXXXXX.tar")"
  local pull_status=0
  # API30 scoped storage blocks shell reads of Android/data. run-as accesses
  # only this owned debuggable app's private capture directory.
  timeout --signal=TERM --kill-after=5s 90s adb exec-out run-as "$package" \
    tar -C "$remote_images" -cf - . > "$archive" 2> "$directory/capture.txt" || pull_status=$?
  if (( pull_status == 0 )); then
    python3 - "$archive" "$images" >> "$directory/capture.txt" 2>&1 <<'PY' || pull_status=$?
import re
import shutil
import sys
import tarfile
from pathlib import Path, PurePosixPath

archive, destination = map(Path, sys.argv[1:])
if destination.exists():
    raise ValueError("Refusing an existing capture destination.")
with tarfile.open(archive, "r:") as stream:
    files = []
    seen = set()
    total = 0
    for member in stream.getmembers():
        if member.isdir() and member.name in (".", "./"):
            continue
        path = PurePosixPath(member.name)
        if (not member.isfile() or path.is_absolute() or len(path.parts) != 1
                or not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9_.-]*\.png", path.name)
                or path.name in seen):
            raise ValueError("Capture archive contains an unsafe or duplicate non-PNG entry.")
        total += member.size
        if member.size <= 0 or member.size > 64 * 1024 * 1024 or total > 256 * 1024 * 1024:
            raise ValueError("Capture archive exceeds image size limits.")
        seen.add(path.name)
        files.append((member, path.name))
    if not files or len(files) > 128:
        raise ValueError("Capture archive must contain 1 to 128 PNG files.")
    destination.mkdir()
    for member, name in files:
        with stream.extractfile(member) as source, (destination / name).open("xb") as target:
            shutil.copyfileobj(source, target)
print(f"Collected {len(files)} app-private PNG captures.")
PY
  fi
  rm -f -- "$archive"
  if (( pull_status != 0 )); then
    echo "::error::ADB could not collect $label craft images (exit $pull_status)."
    cat "$directory/capture.txt"
  fi
  diagnostics "$label"

  if ! python3 - "$directory" "$images" "$width" "$font" "$expected_tests" \
    "${pipeline_status[0]}" "${pipeline_status[1]}" "$pull_status" <<'PY'
import json
import os
import re
import struct
import sys
from pathlib import Path

directory, images = map(Path, sys.argv[1:3])
width, font, expected_tests, adb_status, tee_status, pull_status = sys.argv[3:]
expected_tests, adb_status, tee_status, pull_status = map(
    int, (expected_tests, adb_status, tee_status, pull_status)
)
raw = (directory / "instrumentation.txt").read_text(errors="replace")
expected_summary = f"OK ({expected_tests} tests)"
lines = [line.strip() for line in raw.splitlines()]
observed = re.findall(r"^OK \((\d+) tests?\)\s*$", raw, re.MULTILINE)
errors = []
if adb_status != 0 or tee_status != 0:
    errors.append(f"Instrumentation process/logging exits: adb={adb_status}, tee={tee_status}.")
if lines.count(expected_summary) != 1:
    errors.append(f"Expected exactly one runner summary: {expected_summary!r}.")
if "INSTRUMENTATION_CODE: -1" not in lines:
    errors.append("AndroidJUnitRunner did not report a completed instrumentation result.")
if pull_status != 0:
    errors.append(f"Required app-private screenshot collection failed with exit {pull_status}.")

names = ["welcome", "create-account", "create-profile", "login", "login-error"]
if expected_tests == 9:
    names.append("login-keyboard")
captures = []
for name in names:
    filename = f"{name}-{width}dp-font{font}.png"
    path = images / filename
    if not path.is_file():
        errors.append(f"Missing required screenshot: {filename}.")
        continue
    data = path.read_bytes()
    if (len(data) < 57 or data[:8] != b"\x89PNG\r\n\x1a\n"
            or data[12:16] != b"IHDR"
            or data[-12:] != b"\x00\x00\x00\x00IEND\xaeB`\x82"):
        errors.append(f"Invalid or incomplete PNG: {filename}.")
        continue
    pixel_width, pixel_height = struct.unpack(">II", data[16:24])
    if not pixel_width or not pixel_height:
        errors.append(f"Empty PNG dimensions: {filename}.")
        continue
    captures.append({"file": filename, "pixels": [pixel_width, pixel_height]})

result = {
    "pass": directory.name,
    "logical_width_dp": int(width),
    "display_pixels": [int(width) * 2, 1600],
    "density_dpi": 320,
    "font_scale": float(font),
    "expected_tests": expected_tests,
    "observed_success_counts": [int(count) for count in observed],
    "process_exit": adb_status,
    "capture_exit": pull_status,
    "required_images": len(names),
    "captures": captures,
    "passed": not errors,
    "errors": errors,
}
(directory / "result.json").write_text(json.dumps(result, indent=2) + "\n")
status = "PASS" if not errors else "FAIL"
summary = (
    f"### {directory.name}: {status}\n\n"
    f"Requested {expected_tests} tests at {width} dp, font scale {font}; "
    f"runner success counts: {observed or 'none'}. "
    f"Collected {len(captures)}/{len(names)} required images.\n\n"
)
if errors:
    summary += "\n".join(f"- {error}" for error in errors) + "\n"
(directory / "summary.md").write_text(summary)
if os.environ.get("GITHUB_STEP_SUMMARY"):
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as stream:
        stream.write(summary + "\n")
for error in errors:
    print(f"::error::{error}")
print(summary)
sys.exit(bool(errors))
PY
  then
    overall_status=1
  fi
}

# No backend-fixture journeys or actual account actions. The first pass also
# exercises the real Android software keyboard with a stub login callback.
run_pass nominal-390dp 390 1.0 "$entrance,$drafts,$keyboard" 9 9m
run_pass large-text-320dp 320 2.0 "$entrance" 5 5m
exit "$overall_status"
