# Removes default .webp launcher files that conflict with .png from setup-brand-icon.ps1
$res = Join-Path (Split-Path $PSScriptRoot -Parent) "app\src\main\res"
$patterns = @("ic_launcher*.webp", "ic_launcher_round*.webp", "ic_launcher_foreground*.webp")
$removed = 0
foreach ($pat in $patterns) {
    Get-ChildItem -Path $res -Recurse -Filter $pat -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Force $_.FullName
        Write-Host "Removed $($_.FullName)"
        $removed++
    }
}
if ($removed -eq 0) { Write-Host "No .webp launcher files found." }
else { Write-Host "Removed $removed file(s). Rebuild release APK." }
