param(
    [switch]$NoLaunch
)

$ErrorActionPreference = "Stop"
$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$firebase = Join-Path $root "app\src\debug\google-services.json"
$apk = Join-Path $root "app\build\outputs\apk\debug\app-debug.apk"

function Resolve-JavaHome {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        return $env:JAVA_HOME
    }

    $javaCommand = Get-Command java -ErrorAction SilentlyContinue
    if ($javaCommand) {
        $javaExe = (Resolve-Path $javaCommand.Source).Path
        return (Split-Path (Split-Path $javaExe -Parent) -Parent)
    }

    $candidates = @(
        (Join-Path $env:ProgramFiles "Android\Android Studio\jbr"),
        (Join-Path $env:LOCALAPPDATA "Programs\Android Studio\jbr"),
        (Join-Path $env:ProgramFiles "Android\Android Studio\jre")
    )

    foreach ($candidate in $candidates) {
        if ($candidate -and (Test-Path (Join-Path $candidate "bin\java.exe"))) {
            return $candidate
        }
    }

    $roots = @(
        (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
        (Join-Path $env:ProgramFiles "Java"),
        (Join-Path $env:USERPROFILE ".jdks")
    )

    foreach ($searchRoot in $roots) {
        if (-not $searchRoot -or -not (Test-Path $searchRoot)) { continue }
        $jdk = Get-ChildItem $searchRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { Test-Path (Join-Path $_.FullName "bin\java.exe") } |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($jdk) { return $jdk.FullName }
    }

    return $null
}

if (-not (Test-Path $firebase)) {
    throw "Missing app\src\debug\google-services.json. Keep the real Nova Dev Firebase config local; do not commit it."
}

$javaHome = Resolve-JavaHome
if (-not $javaHome) {
    throw "Java/JDK was not found. Install Android Studio/JDK 17, or set JAVA_HOME before running this script."
}
$env:JAVA_HOME = $javaHome
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
Write-Host "Using JAVA_HOME=$env:JAVA_HOME"
& (Join-Path $env:JAVA_HOME "bin\java.exe") -version
if ($LASTEXITCODE -ne 0) {
    throw "Java was found but could not run."
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
