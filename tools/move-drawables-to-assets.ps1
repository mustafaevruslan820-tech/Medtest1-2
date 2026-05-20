# Перенос PNG из res/drawable в assets/images (обход ошибки AAPT в release).
# Запуск из корня Medtest1:
#   powershell -ExecutionPolicy Bypass -File .\tools\move-drawables-to-assets.ps1

$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent
$src = Join-Path $root "app\src\main\res\drawable"
$dst = Join-Path $root "app\src\main\assets\images"

New-Item -ItemType Directory -Force -Path $dst | Out-Null
Remove-Item -Force (Join-Path $src "*.tmp") -ErrorAction SilentlyContinue

$moved = Get-ChildItem $src -Filter "*.png"
if ($moved.Count -eq 0) {
    Write-Host "No PNG files in drawable."
    exit 0
}

foreach ($f in $moved) {
    $target = Join-Path $dst $f.Name
    if (Test-Path $target) { Remove-Item -Force $target }
    Move-Item -Force $f.FullName $target
    Write-Host "Moved $($f.Name)"
}

Write-Host ""
Write-Host "Done. Sync Gradle -> Clean -> Build release APK."
