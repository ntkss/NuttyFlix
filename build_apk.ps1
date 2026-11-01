$ErrorActionPreference = "Stop"

$sdk = "C:\Users\ntkss\AppData\Local\Android\Sdk"
$buildTools = "$sdk\build-tools\30.0.3"
$androidJar = "$sdk\platforms\android-30\android.jar"
$projectRoot = "c:\work\jobs\tv"
$appRoot = "$projectRoot\app"
$srcMain = "$appRoot\src\main"
$buildDir = "$projectRoot\build"
$jdkBin = "C:\Program Files\Java\jdk-15\bin"

Write-Host "=== STARTING FAST APK BUILD ==="

# 1. Clean build directory
if (Test-Path $buildDir) { Remove-Item $buildDir -Recurse -Force }
New-Item -ItemType Directory -Path "$buildDir\compiled" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\gen" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\classes" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\dex" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\out" | Out-Null

# 2. Compile resources with aapt2
Write-Host "1/7 Compiling resources (aapt2)..."
& "$buildTools\aapt2.exe" compile --dir "$srcMain\res" -o "$buildDir\compiled\resources.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

# 3. Link resources with aapt2
Write-Host "2/7 Linking resources (aapt2)..."
& "$buildTools\aapt2.exe" link -o "$buildDir\base.apk" `
    -I $androidJar `
    --manifest "$srcMain\AndroidManifest.xml" `
    --java "$buildDir\gen" `
    --auto-add-overlay `
    "$buildDir\compiled\resources.zip"
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

# 4. Compile Java source files
Write-Host "3/7 Compiling Java code (javac)..."
$javaFiles = Get-ChildItem -Path "$srcMain\java", "$buildDir\gen" -Filter "*.java" -Recurse | Select-Object -ExpandProperty FullName
& "$jdkBin\javac.exe" -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath $androidJar -cp $androidJar -d "$buildDir\classes" $javaFiles
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

# 5. Compile bytecode to Dalvik Executable (d8)
Write-Host "4/7 Converting to Dalvik DEX (d8)..."
$classFiles = Get-ChildItem -Path "$buildDir\classes" -Filter "*.class" -Recurse | Select-Object -ExpandProperty FullName
& "$jdkBin\java.exe" -cp "$buildTools\lib\d8.jar" com.android.tools.r8.D8 --release --min-api 26 --lib $androidJar --output "$buildDir\dex" $classFiles
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

# 6. Add classes.dex into APK
Write-Host "5/7 Packaging APK..."
Push-Location "$buildDir\dex"
& "$jdkBin\jar.exe" uf "$buildDir\base.apk" classes.dex
Pop-Location
if ($LASTEXITCODE -ne 0) { throw "jar failed" }

# 7. Zipalign
Write-Host "6/7 Aligning APK (zipalign)..."
& "$buildTools\zipalign.exe" -f -p 4 "$buildDir\base.apk" "$buildDir\out\NuttyLauncher-unaligned.apk"
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

# 8. Sign APK
Write-Host "7/7 Signing APK (apksigner)..."
$keystore = "$projectRoot\debug.keystore"
if (-not (Test-Path $keystore)) {
    & "$jdkBin\keytool.exe" -genkey -v -keystore $keystore -alias androiddebugkey -storepass android -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
}

$finalApk = "$buildDir\out\NuttyLauncher.apk"
& "$jdkBin\java.exe" -jar "$buildTools\lib\apksigner.jar" sign --ks $keystore --ks-pass pass:android --key-pass pass:android --out $finalApk "$buildDir\out\NuttyLauncher-unaligned.apk"
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

# 9. Verify signature
& "$jdkBin\java.exe" -jar "$buildTools\lib\apksigner.jar" verify --verbose $finalApk

Write-Host "=== BUILD SUCCESSFUL ==="
Write-Host "Final APK generated at: $finalApk"
Write-Host "Size: " (Get-Item $finalApk).Length "bytes"
