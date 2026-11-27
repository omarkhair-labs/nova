#!/usr/bin/env bash
# Read-only journeys on a new CI AVD and a fresh, runner-private SQLite fixture.
# The app can reach only the owned localhost Django process through adb reverse.
set -euo pipefail
umask 077

case "${ANDROID_SERIAL:-}" in
  emulator-*) ;;
  *) echo '::error::Core-root checks require the disposable CI emulator.' >&2; exit 1 ;;
esac
: "${RUNNER_TEMP:?A private runner temporary directory is required.}"

repository="$(pwd -P)"
readonly repository
readonly package='com.omarkhair70.nova.qa'
readonly runner="$package.test/androidx.test.runner.AndroidJUnitRunner"
readonly test_class='com.nova.app.feature.plants.PlantupiaCoreRootsInstrumentedTest'
readonly remote_images='files/craft-evidence'
readonly output="$repository/artifacts/core-roots"
qa_root="$(mktemp -d "$RUNNER_TEMP/plantupia-core-roots.XXXXXX")"
readonly qa_root
readonly credentials="$qa_root/core-roots-local-qa.json"
backend_pid=''
stage='initialization'
mkdir -p "$output/test-results" "$output/craft-evidence" "$output/diagnostics" "$qa_root/media"

adb_bounded() { timeout --signal=TERM --kill-after=5s 30s adb "$@"; }

# Raw backend/instrumentation/logcat output stays private until known fixture
# secrets and token-shaped values have been removed. Raw files are never uploaded.
sanitize() {
  python3 - "$1" "$2" "$credentials" <<'PY'
import json
import os
import re
import sys
from pathlib import Path

source, destination, fixture = map(Path, sys.argv[1:])
text = source.read_text(errors="replace") if source.is_file() else ""
secrets = [os.environ.get("DJANGO_SECRET_KEY", "")]
if fixture.is_file():
    data = json.loads(fixture.read_text())
    secrets += [entry["password"] for entry in data.get("accounts", {}).values()]
    secrets.append(data.get("invitation", ""))
for secret in sorted(filter(None, secrets), key=len, reverse=True):
    text = text.replace(secret, "[REDACTED]")
text = re.sub(r"\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b", "[REDACTED JWT]", text)
text = re.sub(
    r"(?im)^.*(?:authorization\s*[:=]|[\"']?(?:access_token|refresh_token|password)[\"']?\s*[:=]).*$",
    "[Sensitive diagnostic line removed]", text,
)
destination.parent.mkdir(parents=True, exist_ok=True)
destination.write_text(text)
PY
}

diagnostics() {
  local label="$1" directory="$output/diagnostics/$1"
  mkdir -p "$directory"
  adb_bounded shell wm size > "$directory/display-size.txt" 2>&1 || true
  adb_bounded shell wm density > "$directory/display-density.txt" 2>&1 || true
  adb_bounded shell settings get system font_scale > "$directory/font-scale.txt" 2>&1 || true
  adb_bounded logcat -d -t 3000 > "$qa_root/logcat-$label.txt" 2>&1 || true
  sanitize "$qa_root/logcat-$label.txt" "$directory/logcat-redacted.txt" || true
  if ! adb_bounded exec-out screencap -p > "$directory/device.png" 2> "$directory/screencap.txt"; then
    rm -f "$directory/device.png"
  fi
}

finish() {
  local status=$?
  trap - EXIT
  set +e
  printf 'Last stage: %s\nScript exit: %s\n' "$stage" "$status" > "$output/diagnostics/execution.txt"
  diagnostics exit
  sanitize "$qa_root/backend.log" "$output/diagnostics/backend-redacted.txt"
  if [[ -n "$backend_pid" ]]; then
    kill "$backend_pid" 2>/dev/null
    wait "$backend_pid" 2>/dev/null
  fi
  adb_bounded shell am force-stop "$package" > /dev/null 2>&1
  adb_bounded shell pm clear "$package" > /dev/null 2>&1
  adb_bounded reverse --remove tcp:8765 > /dev/null 2>&1
  # The only removed host path is this process's own mktemp directory.
  if [[ "$qa_root" == "$RUNNER_TEMP"/plantupia-core-roots.* ]]; then
    rm -rf -- "$qa_root"
  fi
  if (( status != 0 )); then
    echo "::error::Core-root execution failed at $stage. Review core-roots-test-results."
  fi
  exit "$status"
}
trap finish EXIT
trap 'exit 143' TERM
trap 'exit 130' INT

# Explicitly replace inherited service configuration. This process cannot use a
# hosted database, provider, bucket, mail server, or company/publication gate.
export DJANGO_ENV=development DJANGO_DEBUG=1
export DJANGO_ALLOWED_HOSTS=localhost,127.0.0.1
export DATABASE_URL="sqlite:///$qa_root/qa.sqlite3"
export PLANTUPIA_LOCAL_QA_MEDIA_ROOT="$qa_root/media"
export PLANTUPIA_HANDOVER_QA_ENABLED=1 PLANTUPIA_LIVING_SYSTEMS_ENABLED=1
export PLANTUPIA_COMPANY_DASHBOARD_ENABLED=0 PLANTUPIA_CEO_DEMO_ENABLED=0
export PLANTUPIA_PREVIEW_HOSTED_QA_ENABLED=0 PLANTUPIA_STOREFRONT_PUBLICATION_ENABLED=0
export REDIS_URL='' RAILWAY_PUBLIC_DOMAIN=''
export AWS_ENDPOINT_URL='' AWS_ACCESS_KEY_ID='' AWS_SECRET_ACCESS_KEY='' AWS_S3_BUCKET_NAME=''
export FIREBASE_SERVICE_ACCOUNT_JSON='' GOOGLE_APPLICATION_CREDENTIALS=''
export PLANTUPIA_AI_ENABLED=0 PLANTUPIA_AI_API_KEY=''
export EMAIL_HOST='' EMAIL_HOST_USER='' EMAIL_HOST_PASSWORD='' DEFAULT_FROM_EMAIL=''
export PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1
export PYTHONPATH="$repository/backend"
export DJANGO_SETTINGS_MODULE=nova_backend.settings
DJANGO_SECRET_KEY="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
export DJANGO_SECRET_KEY

stage='validate isolated settings'
python3 - "$repository" "$qa_root" <<'PY'
import sys
from pathlib import Path

import django
django.setup()
from django.conf import settings

repository, private = map(lambda p: Path(p).resolve(), sys.argv[1:])
assert not private.is_relative_to(repository), "Private fixture must be outside the repository."
db = settings.DATABASES["default"]
assert settings.ENVIRONMENT == "development" and not settings.IS_PRODUCTION
assert db["ENGINE"] == "django.db.backends.sqlite3"
database = Path(db["NAME"]).resolve()
assert database.parent == private and not database.exists(), "A fresh private SQLite database is required."
assert Path(settings.MEDIA_ROOT).resolve() == private / "media"
assert set(settings.ALLOWED_HOSTS) == {"localhost", "127.0.0.1"}
assert settings.STORAGES["default"]["BACKEND"] == "django.core.files.storage.FileSystemStorage"
print("Validated fresh local SQLite, private filesystem media and loopback hosts.")
PY

stage='migrate disposable SQLite only'
if ! timeout --signal=TERM --kill-after=5s 3m python3 "$repository/backend/manage.py" migrate --noinput > "$qa_root/migrate.txt" 2>&1; then
  sanitize "$qa_root/migrate.txt" "$output/diagnostics/migrate-redacted.txt"
  exit 1
fi
stage='create guarded synthetic fixture'
if ! timeout --signal=TERM --kill-after=5s 2m python3 "$repository/backend/manage.py" seed_local_plantupia_qa \
    --credentials-file "$credentials" --with-sample-care-guides > "$qa_root/seed.txt" 2>&1; then
  sanitize "$qa_root/seed.txt" "$output/diagnostics/seed-redacted.txt"
  exit 1
fi
python3 - "$credentials" <<'PY'
import json
import stat
import sys
from pathlib import Path

path = Path(sys.argv[1])
fixture = json.loads(path.read_text())
assert fixture["synthetic"] is True and len(fixture["plant_ids"]) == 7
assert {"staff", "customer", "stranger", "independent"} == set(fixture["accounts"])
assert stat.S_IMODE(path.stat().st_mode) == 0o600
print("Created seven synthetic plants and three QA-only guide examples; no delivery was claimed or real botanical approval implied.")
PY

stage='start owned loopback backend'
# Refuse another local process before starting the server owned by this script.
python3 - <<'PY'
import socket
with socket.socket() as probe:
    probe.bind(("127.0.0.1", 8765))
PY
python3 "$repository/backend/manage.py" runserver 127.0.0.1:8765 --noreload > "$qa_root/backend.log" 2>&1 &
backend_pid=$!
backend_ready=0
for attempt in {1..60}; do
  if ! kill -0 "$backend_pid" 2>/dev/null; then break; fi
  if curl -fsS --max-time 2 http://127.0.0.1:8765/api/v1/health/ -o "$qa_root/health.json" 2>/dev/null; then
    if python3 - "$qa_root/health.json" <<'PY'
import json
import sys
with open(sys.argv[1]) as stream:
    assert json.load(stream).get("status") == "ok"
PY
    then backend_ready=1; break; fi
  fi
  sleep 1
done
if (( backend_ready != 1 )); then exit 1; fi
printf '%s\n' 'Fresh local SQLite migrated; guarded seed has seven synthetic plants and three QA-only guide examples.' \
  'Owned server healthy at http://127.0.0.1:8765; no hosted service configured.' \
  > "$output/test-results/backend-setup.txt"

ensure_offline() {
  adb_bounded shell svc wifi disable
  adb_bounded shell svc data disable
  # No protected airplane-mode broadcast. Validate the actual API30 network
  # state before installing or launching anything; adb reverse is still local.
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

stage='install isolated QA packages'
adb_bounded wait-for-device
ensure_offline
timeout --signal=TERM --kill-after=10s 3m adb install -r -t app/build/outputs/apk/qa/app-qa.apk
timeout --signal=TERM --kill-after=10s 3m adb install -r -t app/build/outputs/apk/androidTest/qa/app-qa-androidTest.apk
adb_bounded reverse tcp:8765 tcp:8765

overall_status=0
run_pass() {
  local label="$1" width="$2" font="$3"
  local directory="$output/test-results/$label" images="$output/craft-evidence/$label"
  stage="$label"
  mkdir -p "$directory"
  ensure_offline
  adb_bounded shell am force-stop "$package"
  adb_bounded shell pm clear "$package" > "$directory/clear-data.txt"
  if ! grep -Eq '^Success[[:space:]]*$' "$directory/clear-data.txt"; then exit 1; fi
  adb_bounded shell run-as "$package" mkdir -p files
  # Non-PTY shell waits for the private write to finish and propagates errors.
  # Fixture values are sent over stdin and never enter the CI log.
  adb_bounded shell -T "run-as $package sh -c 'umask 077; cat > files/core-roots-local-qa.json && test -s files/core-roots-local-qa.json'" \
    < "$credentials"
  adb_bounded shell wm size "$(( width * 2 ))x1600"
  adb_bounded shell wm density 320
  adb_bounded shell settings put system font_scale "$font"
  adb_bounded shell settings put secure show_ime_with_hard_keyboard 1
  adb_bounded shell input keyevent KEYCODE_WAKEUP
  adb_bounded shell wm dismiss-keyguard
  adb_bounded logcat -c
  if ! kill -0 "$backend_pid" 2>/dev/null; then exit 1; fi

  local instrument_status=0
  timeout --signal=TERM --kill-after=10s 12m adb shell am instrument -w -r \
    -e class "$test_class" -e plantupiaCapture true "$runner" \
    > "$qa_root/instrumentation-$label.txt" 2>&1 || instrument_status=$?
  sanitize "$qa_root/instrumentation-$label.txt" "$directory/instrumentation-redacted.txt"
  if [[ -e "$images" ]]; then
    echo "::error::Refusing stale screenshot evidence for $label." >&2
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
  diagnostics "$label"

  if ! python3 - "$directory" "$images" "$width" "$font" "$instrument_status" "$pull_status" <<'PY'
import json
import os
import re
import sys
from pathlib import Path
from PIL import Image

directory, images = map(Path, sys.argv[1:3])
width, font, process_status, pull_status = sys.argv[3:]
process_status, pull_status = map(int, (process_status, pull_status))
raw = (directory / "instrumentation-redacted.txt").read_text()
lines = [line.strip() for line in raw.splitlines()]
errors = []
if process_status:
    errors.append(f"Instrumentation process exited {process_status}.")
if lines.count("OK (4 tests)") != 1 or "INSTRUMENTATION_CODE: -1" not in lines:
    errors.append("Expected exactly four completed, successful instrumentation tests.")
if re.search(r"^INSTRUMENTATION_STATUS_CODE: -\d+\s*$", raw, re.MULTILINE):
    errors.append("The runner reported a failed, ignored or assumption-skipped test.")
if pull_status:
    errors.append(f"Required app-private screenshot collection exited {pull_status}.")
names = [
    "01-customer-today", "02-customer-plants", "03-create", "04-create-public",
    "05-circle", "06-circle-moments", "07-profile", "08-profile-public",
    "09-settings", "10-settings-about", "11-shop-empty", "12-shop-cart",
    "13-ask", "14-today-return", "15-owner-today", "16-owner-plants", "17-owner-profile",
    "18-owner-plant-detail", "19-owner-plant-care", "20-owner-plant-history", "21-owner-plant-guide",
    "22-place-without-survey", "23-plain-private-history", "24-capture-permission", "25-calendar-visit-return",
]
captures = []
for name in names:
    filename = f"{name}-{width}dp-font{font}.png"
    path = images / filename
    try:
        with Image.open(path) as bitmap:
            assert bitmap.format == "PNG" and min(bitmap.size) > 0
            size = list(bitmap.size)
            bitmap.verify()
        captures.append({"file": filename, "pixels": size})
    except (OSError, ValueError, SyntaxError, AssertionError):
        errors.append(f"Missing, incomplete or invalid required screenshot: {filename}.")
result = {
    "pass": directory.name, "logical_width_dp": int(width), "font_scale": float(font),
    "expected_tests": 4, "process_exit": process_status, "capture_exit": pull_status,
    "required_images": len(names), "captures": captures, "passed": not errors, "errors": errors,
}
(directory / "result.json").write_text(json.dumps(result, indent=2) + "\n")
status = "PASS" if not errors else "FAIL"
summary = (
    f"### {directory.name}: {status}\n\n"
    f"Four journeys against disposable QA at {width} dp, font scale {font}; "
    f"collected {len(captures)}/{len(names)} required native screenshots.\n\n"
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

run_pass nominal-390dp 390 1.0
run_pass large-text-320dp 320 2.0
stage='complete'
exit "$overall_status"
