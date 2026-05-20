# Welcome screen circle logo ONLY (does not change install icon).
# Save the medicine-box illustration (photo 2) as:
#   tools\welcome_icon_source.png
# Run:
#   powershell -ExecutionPolicy Bypass -File .\tools\setup-welcome-logo-only.ps1

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Get-CenteredSquareCrop {
    param([System.Drawing.Image]$Source)
    $side = [Math]::Min($Source.Width, $Source.Height)
    $x = [int][Math]::Floor(($Source.Width - $side) / 2.0)
    $y = [int][Math]::Floor(($Source.Height - $side) / 2.0)
    $crop = New-Object System.Drawing.Bitmap $side, $side
    $g = [System.Drawing.Graphics]::FromImage($crop)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.DrawImage($Source, 0, 0, (New-Object System.Drawing.Rectangle $x, $y, $side, $side), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $crop
}

function Save-SquareAsset {
    param(
        [System.Drawing.Image]$Source,
        [int]$SizePx,
        [string]$OutPath
    )
    $bmp = New-Object System.Drawing.Bitmap $SizePx, $SizePx, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)
    $g.DrawImage($Source, 0, 0, $SizePx, $SizePx)
    $g.Dispose()
    $dir = Split-Path $OutPath -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$root = Split-Path $PSScriptRoot -Parent
$src = Join-Path $PSScriptRoot "welcome_icon_source.png"
if (-not (Test-Path $src)) {
    Write-Error "Put welcome illustration PNG here: $src"
}

$raw = [System.Drawing.Image]::FromFile($src)
$img = Get-CenteredSquareCrop -Source $raw
if ($raw -ne $img) { $raw.Dispose() }
try {
    $assetsDir = Join-Path $root "app\src\main\assets\images"
    New-Item -ItemType Directory -Force -Path $assetsDir | Out-Null
    $out = Join-Path $assetsDir "app_brand_logo.png"
    Save-SquareAsset -Source $img -SizePx 1024 -OutPath $out
    Write-Host "Updated $out (1024x1024, for welcome circle)"
}
finally {
    $img.Dispose()
}
