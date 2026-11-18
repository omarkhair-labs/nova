# Plantupia: user-invoked, disposable Windows/USB phone acceptance.
# Never uses the hosted/production API, database or object storage.
param(
    [Parameter(Mandatory = $true)]
    [string]$ApkPath,
    [switch]$CreateLocalQr
)
$ErrorActionPreference = "Stop"

function Check-NativeExit([string]$label) {
    if ($LASTEXITCODE -ne 0) { throw "$label failed (exit code $LASTEXITCODE)." }
}
$repo = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$apk = (Resolve-Path $ApkPath -ErrorAction Stop).Path
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "ADB not found. Install Android SDK Platform Tools, enable USB debugging and authorize this computer."
}
$runRoot = Join-Path $env:LOCALAPPDATA "PlantupiaQA"
if (-not $env:LOCALAPPDATA) { throw "Windows LOCALAPPDATA is required for isolated QA." }
New-Item -ItemType Directory -Force -Path $runRoot | Out-Null
$venv = Join-Path $runRoot "python-venv"
$venvPython = Join-Path $venv "Scripts\python.exe"
if (-not (Test-Path $venvPython)) {
    if (Get-Command py -ErrorAction SilentlyContinue) {
        & py -3.12 -m venv $venv
        Check-NativeExit "Python 3.12 venv creation"
    } elseif (Get-Command python -ErrorAction SilentlyContinue) {
        & python -m venv $venv
        Check-NativeExit "Python venv creation"
    } else {
        throw "Python 3.12 is required and was not found."
    }
}
& $venvPython -m pip install -r (Join-Path $repo "backend\requirements.txt")
Check-NativeExit "Installing isolated Python dependencies"
if ($CreateLocalQr) {
    & $venvPython -m pip install "qrcode[pil]"
    Check-NativeExit "Installing optional offline QR encoder"
}
$runId = (Get-Date -Format "yyyyMMdd-HHmmss") + "-" + [guid]::NewGuid().ToString("N").Substring(0, 8)
$dataDir = Join-Path $runRoot $runId
$mediaDir = Join-Path $dataDir "media"
New-Item -ItemType Directory -Force -Path $mediaDir | Out-Null
$credentials = Join-Path $dataDir "credentials-private.json"
$env:DJANGO_ENV = "development"
$env:DJANGO_DEBUG = "1"
$env:DJANGO_SECRET_KEY = (& $venvPython -c "import secrets; print(secrets.token_urlsafe(48))").Trim()
$env:DJANGO_ALLOWED_HOSTS = "127.0.0.1,localhost"
# sqlite:/// relative filename lives under $dataDir, not the repository.
$env:DATABASE_URL = "sqlite:///plantupia-demo.sqlite3"
$env:PLANTUPIA_HANDOVER_QA_ENABLED = "1"
$env:PLANTUPIA_LOCAL_QA_MEDIA_ROOT = $mediaDir
$env:REDIS_URL = ""
foreach ($key in @("AWS_ENDPOINT_URL", "AWS_ACCESS_KEY_ID", "AWS_SECRET_ACCESS_KEY", "AWS_S3_BUCKET_NAME", "RAILWAY_PUBLIC_DOMAIN")) {
    Remove-Item ("Env:" + $key) -ErrorAction SilentlyContinue
}
Push-Location $dataDir
try {
    & $venvPython (Join-Path $repo "backend\manage.py") migrate --noinput
    Check-NativeExit "Local SQLite schema migration"
    & $venvPython (Join-Path $repo "backend\manage.py") seed_local_plantupia_qa --credentials-file $credentials --with-sample-care-guides
    Check-NativeExit "Local synthetic fixture setup"
    if ($CreateLocalQr) {
        $qrPath = Join-Path $dataDir "invitation-private-qr.png"
        & $venvPython -c "import json,sys,qrcode; from pathlib import Path; payload=json.loads(Path(sys.argv[1]).read_text(encoding='utf-8')); qrcode.make(payload['invitation']).save(sys.argv[2])" $credentials $qrPath
        Check-NativeExit "Offline private QR generation"
        Write-Host "Private local QR: $qrPath"
    }
    & adb get-state
    Check-NativeExit "ADB authorized phone"
    & adb reverse tcp:8765 tcp:8765
    Check-NativeExit "USB-to-PC reverse tunnel"
    & adb install -r $apk
    Check-NativeExit "Installing the QA APK on the phone"
    Write-Host ""
    Write-Host "Local protected staff Admin: http://127.0.0.1:8765/admin/"
    Write-Host "Private QA accounts + code are in: $credentials"
    Write-Host "This server is LOCAL ONLY. Keep the USB cable connected."
    Write-Host "Press Ctrl+C to stop the QA server. Delete $dataDir after testing."
    & $venvPython (Join-Path $repo "backend\manage.py") runserver 127.0.0.1:8765
} finally {
    Pop-Location
}
