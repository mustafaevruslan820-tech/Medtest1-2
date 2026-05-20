# Перекодирует PNG в res/drawable для успешной сборки release (AAPT2).
# Запуск: powershell -ExecutionPolicy Bypass -File tools\fix-drawable-png.ps1

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$drawable = Join-Path $PSScriptRoot "..\app\src\main\res\drawable" | Resolve-Path
$maxSide = 2048

Get-ChildItem $drawable -Filter "*.png" | ForEach-Object {
    $path = $_.FullName
    Write-Host "Processing $($_.Name) ..."

    $img = [System.Drawing.Image]::FromFile($path)
    try {
        $w = $img.Width
        $h = $img.Height
        if ($w -gt $maxSide -or $h -gt $maxSide) {
            $scale = [Math]::Min($maxSide / $w, $maxSide / $h)
            $w = [int][Math]::Round($w * $scale)
            $h = [int][Math]::Round($h * $scale)
            Write-Host "  resize -> ${w}x${h}"
        }

        $bmp = New-Object System.Drawing.Bitmap $w, $h, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
        $g = [System.Drawing.Graphics]::FromImage($bmp)
        $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g.Clear([System.Drawing.Color]::Transparent)
        $g.DrawImage($img, 0, 0, $w, $h)
        $g.Dispose()

        $tmp = [System.IO.Path]::Combine([System.IO.Path]::GetTempPath(), [Guid]::NewGuid().ToString("N") + ".png")
        $bmp.Save($tmp, [System.Drawing.Imaging.ImageFormat]::Png)
        $bmp.Dispose()

        Remove-Item -Force $path
        Move-Item -Force $tmp $path
        $newLen = (Get-Item $path).Length
        Write-Host "  OK ($newLen bytes)"
    }
    finally {
        $img.Dispose()
    }
}

Write-Host ""
Write-Host "Done. In Android Studio: Sync -> Clean -> Build APK (release)."
