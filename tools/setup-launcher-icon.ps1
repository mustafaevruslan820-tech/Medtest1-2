# Копирует логотип из assets в mipmap-* (перекодированный PNG для иконки приложения).
# Запуск: powershell -ExecutionPolicy Bypass -File .\tools\setup-launcher-icon.ps1

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path $PSScriptRoot -Parent
$src = Join-Path $root "app\src\main\assets\images\umnoe_zdorove_logo.png"
if (-not (Test-Path $src)) {
    Write-Error "Not found: $src (run move-drawables-to-assets.ps1 first)"
}

$sizes = @{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

$img = [System.Drawing.Image]::FromFile($src)
try {
    foreach ($folder in $sizes.Keys) {
        $px = $sizes[$folder]
        $dir = Join-Path $root "app\src\main\res\$folder"
        New-Item -ItemType Directory -Force -Path $dir | Out-Null
        $out = Join-Path $dir "umnoe_zdorove_logo.png"

        $bmp = New-Object System.Drawing.Bitmap $px, $px, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($img, 0, 0, $px, $px)
        $g.Dispose()
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()
        Write-Host "Wrote $out (${px}px)"
    }
}
finally {
    $img.Dispose()
}

Write-Host "Done. Sync Gradle and rebuild release APK."
