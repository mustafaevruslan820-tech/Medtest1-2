# Install icon from photo 1 (checkmark + pill on blue gradient).
# Save as: tools\launcher_icon_source.png
# Run: powershell -ExecutionPolicy Bypass -File .\tools\setup-launcher-icon-only.ps1

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

function Get-CenteredSquareCrop {
    param([System.Drawing.Image]$Source)
    $side = [Math]::Min($Source.Width, $Source.Height)
    $x = [int][Math]::Floor(($Source.Width - $side) / 2.0)
    $y = [int][Math]::Floor(($Source.Height - $side) / 2.0)
    $crop = New-Object System.Drawing.Bitmap $side, $side
    $g = [System.Drawing.Graphics]::FromImage($crop)
    $g.DrawImage($Source, 0, 0, (New-Object System.Drawing.Rectangle $x, $y, $side, $side), [System.Drawing.GraphicsUnit]::Pixel)
    $g.Dispose()
    return $crop
}

# Fit icon inside canvas (zoom out) so white rounded square and checkmark stay visible.
function Save-FittedSquareIcon {
    param(
        [System.Drawing.Image]$Source,
        [int]$SizePx,
        [string]$OutPath,
        [double]$FillRatio = 0.78
    )
    $bmp = New-Object System.Drawing.Bitmap $SizePx, $SizePx, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::HighQuality
    $g.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $g.Clear([System.Drawing.Color]::Transparent)

    $srcW = [double]$Source.Width
    $srcH = [double]$Source.Height
    $scale = [Math]::Min($SizePx / $srcW, $SizePx / $srcH) * $FillRatio
    $drawW = $srcW * $scale
    $drawH = $srcH * $scale
    $x = ($SizePx - $drawW) / 2.0
    $y = ($SizePx - $drawH) / 2.0
    $g.DrawImage($Source, [single]$x, [single]$y, [single]$drawW, [single]$drawH)
    $g.Dispose()

    $dir = Split-Path $OutPath -Parent
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $bmp.Save($OutPath, [System.Drawing.Imaging.ImageFormat]::Png)
    $bmp.Dispose()
}

$root = Split-Path $PSScriptRoot -Parent
$res = Join-Path $root "app\src\main\res"
$src = Join-Path $PSScriptRoot "launcher_icon_source.png"
if (-not (Test-Path $src)) {
    Write-Error "Put app icon PNG (photo 1) here: $src"
}

$foregroundSizes = @{
    "mipmap-mdpi"    = 108
    "mipmap-hdpi"    = 162
    "mipmap-xhdpi"   = 216
    "mipmap-xxhdpi"  = 324
    "mipmap-xxxhdpi" = 432
}
$legacySizes = @{
    "mipmap-mdpi"    = 48
    "mipmap-hdpi"    = 72
    "mipmap-xhdpi"   = 96
    "mipmap-xxhdpi"  = 144
    "mipmap-xxxhdpi" = 192
}

$raw = [System.Drawing.Image]::FromFile($src)
$img = Get-CenteredSquareCrop -Source $raw
if ($raw -ne $img) { $raw.Dispose() }
try {
    foreach ($folder in $foregroundSizes.Keys) {
        $fgOut = Join-Path $root "app\src\main\res\$folder\ic_launcher_foreground.png"
        Save-FittedSquareIcon -Source $img -SizePx $foregroundSizes[$folder] -OutPath $fgOut
        Write-Host "$folder\ic_launcher_foreground.png"

        $legacyOut = Join-Path $root "app\src\main\res\$folder\ic_launcher.png"
        Save-FittedSquareIcon -Source $img -SizePx $legacySizes[$folder] -OutPath $legacyOut -FillRatio 0.82
        Copy-Item -Force $legacyOut (Join-Path $root "app\src\main\res\$folder\ic_launcher_round.png")
        Write-Host "$folder\ic_launcher.png"
    }
}
finally {
    $img.Dispose()
}

Write-Host "Done. Install icon updated. Welcome logo is NOT changed (use setup-welcome-logo-only.ps1)."
