# Photo 1 -> launcher_icon_source.png -> install icon
# Photo 2 -> welcome_icon_source.png -> welcome screen circle
# Run from project root:
#   powershell -ExecutionPolicy Bypass -File .\tools\setup-all-icons.ps1

$ErrorActionPreference = "Stop"
$here = $PSScriptRoot

if (-not (Test-Path (Join-Path $here "launcher_icon_source.png"))) {
    Write-Error "Missing tools\launcher_icon_source.png (photo 1 - app icon with checkmark)"
}
if (-not (Test-Path (Join-Path $here "welcome_icon_source.png"))) {
    Write-Error "Missing tools\welcome_icon_source.png (photo 2 - medicine box illustration)"
}

& (Join-Path $here "setup-launcher-icon-only.ps1")
& (Join-Path $here "setup-welcome-logo-only.ps1")
Write-Host ""
Write-Host "All icons ready. Rebuild APK in Android Studio."
