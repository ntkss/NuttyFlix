Add-Type -AssemblyName System.Drawing

$width = 960
$height = 260
$bmp = New-Object System.Drawing.Bitmap $width, $height
$g = [System.Drawing.Graphics]::FromImage($bmp)
$g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
$g.Clear([System.Drawing.Color]::Transparent)

# Netflix Iconic Red
$netflixRed = [System.Drawing.Color]::FromArgb(229, 9, 20)
$brush = New-Object System.Drawing.SolidBrush $netflixRed

# Subtle drop shadow for rich contrast
$shadowBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(90, 0, 0, 0))

$fontFamily = "Impact"
$chars = "NUTTYFLIX".ToCharArray()
$n = $chars.Length

# Pre-calculate heights & fonts for each char to get widths
$baseHeight = 120.0 # Height at center
$extraEdgeHeight = 35.0 # Extra height at edges (N and X)
$charWidths = @()
$charHeights = @()
$charFonts = @()

for ($i = 0; $i -lt $n; $i++) {
    $norm = ($i - (($n - 1) / 2.0)) / (($n - 1) / 2.0) # -1.0 to +1.0
    $h = $baseHeight + ($extraEdgeHeight * ($norm * $norm))
    $charHeights += $h
    
    $fSize = [float]($h * 0.76)
    $f = [System.Drawing.Font]::new($fontFamily, $fSize, [System.Drawing.FontStyle]::Bold)
    $charFonts += $f
    
    # Measure width accurately without padding
    $sfMeasure = New-Object System.Drawing.StringFormat([System.Drawing.StringFormat]::GenericTypographic)
    $sz = $g.MeasureString($chars[$i].ToString(), $f, 1000, $sfMeasure)
    $charWidths += ($sz.Width * 1.08) # slight natural letter width
}

# Total word width with a small negative tracking (-3px) for bold Netflix look
$letterSpacing = 2.0
$totalWidth = 0.0
for ($i = 0; $i -lt $n; $i++) {
    $totalWidth += $charWidths[$i]
    if ($i -lt $n - 1) { $totalWidth += $letterSpacing }
}

$startX = ($width - $totalWidth) / 2.0

# Render each character along the bottom arch
$currentX = $startX
$topBaseline = 50.0

for ($i = 0; $i -lt $n; $i++) {
    $ch = $chars[$i].ToString()
    $w = $charWidths[$i]
    $h = $charHeights[$i]
    $font = $charFonts[$i]
    
    $norm = ($i - (($n - 1) / 2.0)) / (($n - 1) / 2.0)
    
    # Bottom arch: center is at y = 170, edges are at y = 205 (arched up by 35px)
    $bottomY = 170.0 + (35.0 * ($norm * $norm))
    
    # Slight tilt matching the curvature normal
    $tiltAngle = $norm * 7.5 # -7.5 deg to +7.5 deg
    
    $charCenterX = $currentX + ($w / 2.0)
    
    $sf = New-Object System.Drawing.StringFormat([System.Drawing.StringFormat]::GenericTypographic)
    $sf.Alignment = [System.Drawing.StringAlignment]::Center
    $sf.LineAlignment = [System.Drawing.StringAlignment]::Far
    
    $state = $g.Save()
    $g.TranslateTransform($charCenterX, $bottomY)
    $g.RotateTransform($tiltAngle)
    
    # Shadow
    $g.DrawString($ch, $font, $shadowBrush, 2.5, 3.5, $sf)
    # Red character
    $g.DrawString($ch, $font, $brush, 0.0, 0.0, $sf)
    
    $g.Restore($state)
    
    $currentX += $w + $letterSpacing
}

$destBrain = "C:\Users\ntkss\.gemini\antigravity\brain\3086342f-e8ff-4a88-af63-06cbadc73677\logo_nuttyflix.png"
$destApp = "c:\work\jobs\tv\app\src\main\res\drawable-nodpi\logo_nuttyflix.png"

# Crop tight bounding box
$cropTop = 40
$cropHeight = 180
$cropRect = New-Object System.Drawing.Rectangle 0, $cropTop, $width, $cropHeight
$croppedBmp = $bmp.Clone($cropRect, $bmp.PixelFormat)

$croppedBmp.Save($destBrain, [System.Drawing.Imaging.ImageFormat]::Png)
$croppedBmp.Save($destApp, [System.Drawing.Imaging.ImageFormat]::Png)

$croppedBmp.Dispose()
$g.Dispose()
$bmp.Dispose()
Write-Host "NuttyFlix logo regenerated with precision kerning!"
