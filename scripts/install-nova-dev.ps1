param(
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$firebase = Join-Path $root "app\src\debug\google-services.json"
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"

if (-not (Test-Path $firebase)) {
    throw "Missing app\src\debug\google-services.json. Keep the real Nova Dev Firebase config local; do not commit it."
}

$adbCommand = Get-Command adb -ErrorAction SilentlyContinue
if ($adbCommand) {
    $adb = $adbCommand.Source
} else {
    $sdkAdb = Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdkAdb) {
        $adb = $sdkAdb
    } else {
        throw "ADB was not found. Install Android platform-tools or add adb to PATH."
    }
}

Push-Location $root
try {
    $env:NOVA_DEV_VARIANT = "1"

    Write-Host "Building Nova Dev..."
    & .\gradlew.bat --no-daemon :app:assembleDebug
    if ($LASTEXITCODE -ne 0) {
        throw "Nova Dev build failed."
    }

    if (-not (Test-Path $apk)) {
        throw "Debug APK was not produced at $apk"
    }

    Write-Host "Checking connected Android device..."
    & $adb devices
    if ($LASTEXITCODE -ne 0) {
        throw "ADB could not enumerate devices."
    }

    Write-Host "Installing over the existing Nova Dev app..."
    & $adb install -r $apk
    if ($LASTEXITCODE -ne 0) {
        throw "Nova Dev install failed. Keep the existing app installed so its data is preserved; verify the phone is authorized and that this PC owns the same debug signing key used for the installed Nova Dev build."
    }

    if (-not $NoLaunch) {
        Write-Host "Launching Nova Dev..."
        & $adb shell am force-stop com.omarkhair70.nova.dev | Out-Null
        & $adb shell monkey -p com.omarkhair70.nova.dev -c android.intent.category.LAUNCHER 1 | Out-Null
    }

    Write-Host "Nova Dev is installed: com.omarkhair70.nova.dev"
} finally {
    Remove-Item Env:NOVA_DEV_VARIANT -ErrorAction SilentlyContinue
    Pop-Location
}
