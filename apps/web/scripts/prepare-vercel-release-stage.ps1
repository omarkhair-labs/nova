param(
  [Parameter(Mandatory=$true)][ValidateSet("preview","production")][string]$Target,
  [Parameter(Mandatory=$true)][string]$AssetRoot,
  [Parameter(Mandatory=$true)][string]$StageRoot,
  [string]$RepoRoot = ""
)

$ErrorActionPreference = "Stop"
if (-not $RepoRoot) {
  $RepoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..\..")).Path
}
$SourceWeb = Join-Path $RepoRoot "apps\web"
if (-not (Test-Path $SourceWeb)) { throw "apps/web not found under $RepoRoot" }
if (-not (Test-Path $AssetRoot)) { throw "AssetRoot not found: $AssetRoot" }

if (Test-Path $StageRoot) { Remove-Item -LiteralPath $StageRoot -Recurse -Force }
New-Item -ItemType Directory -Path (Join-Path $StageRoot "apps\web") -Force | Out-Null
robocopy $SourceWeb (Join-Path $StageRoot "apps\web") /E /XD node_modules .next .vercel /XF *.log | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy web failed $LASTEXITCODE" }

$PublishedAssets = Join-Path $StageRoot "apps\web\public\private-review-assets"
New-Item -ItemType Directory -Path $PublishedAssets -Force | Out-Null
robocopy $AssetRoot $PublishedAssets /E | Out-Null
if ($LASTEXITCODE -ge 8) { throw "robocopy assets failed $LASTEXITCODE" }

New-Item -ItemType Directory -Path (Join-Path $StageRoot ".vercel") -Force | Out-Null
Copy-Item (Join-Path $SourceWeb ".vercel\project.json") (Join-Path $StageRoot ".vercel\project.json")
$ignore = "node_modules"+[Environment]::NewLine+".next"+[Environment]::NewLine+".git"+[Environment]::NewLine+"*.log"+[Environment]::NewLine
[IO.File]::WriteAllText((Join-Path $StageRoot ".vercelignore"),$ignore,[Text.UTF8Encoding]::new($false))

if ($Target -eq "preview") {
  $enc = [Text.UTF8Encoding]::new($false)
  $files = Get-ChildItem (Join-Path $StageRoot "apps\web\app") -Recurse -File -Include *.ts,*.tsx
  foreach ($file in $files) {
    $s = [IO.File]::ReadAllText($file.FullName)
    $s = $s.Replace(
      '(process.env.VERCEL_ENV === "production" || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))',
      '(process.env.VERCEL_ENV === "preview" || process.env.VERCEL_ENV === "production" || (process.env.NODE_ENV === "development" && !!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR))'
    )
    $s = $s.Replace(
      'process.env.VERCEL_ENV !== "production" && (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)',
      '!["preview","production"].includes(process.env.VERCEL_ENV ?? "") && (process.env.NODE_ENV !== "development" || !process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)'
    )
    $s = $s.Replace(
      'process.env.VERCEL_ENV!=="production"&&(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)',
      '!["preview","production"].includes(process.env.VERCEL_ENV??"")&&(process.env.NODE_ENV!=="development"||!process.env.PLANTUPIA_PRIVATE_ARRIVAL_DIR)'
    )
    $s = $s.Replace(
      'const privateReview = process.env.VERCEL_ENV === "production" || (process.env.NODE_ENV === "development" && !!root);',
      'const privateReview = process.env.VERCEL_ENV === "preview" || process.env.VERCEL_ENV === "production" || (process.env.NODE_ENV === "development" && !!root);'
    )
    [IO.File]::WriteAllText($file.FullName,$s,$enc)
  }

  $contact = Join-Path $StageRoot "apps\web\app\start-a-space\page.tsx"
  $s = [IO.File]::ReadAllText($contact)
  $old = '  const available = process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED === "1" &&'
  if ($s.Contains($old)) {
    $new = '  const available = process.env.VERCEL_ENV !== "preview" &&'+[Environment]::NewLine+'    process.env.PLANTUPIA_WEBSITE_INBOX_ENABLED === "1" &&'
    $s = $s.Replace($old,$new)
    [IO.File]::WriteAllText($contact,$s,$enc)
  }
}

Write-Output ("TARGET="+$Target)
Write-Output ("FILES="+(Get-ChildItem $StageRoot -Recurse -File).Count)
Write-Output ("ASSETS="+(Get-ChildItem $PublishedAssets -File).Count)
Write-Output ("PROJECT="+(Get-Content (Join-Path $StageRoot ".vercel\project.json") -Raw))
