# NuttyFlix Build & Compilation Guide

NuttyFlix supports two build pipelines:
1. **Ultra-Fast Direct SDK Build (`build_apk.ps1`)** - Compiles in ~5 seconds directly using Android SDK CLI tools.
2. **Standard Gradle Build** - Standard Android Studio / CI build toolchain.

---

## Prerequisites
- **Java JDK:** JDK 11 or higher (Tested on OpenJDK 15 / 17).
- **Android SDK:** Platforms `android-30` or higher, Build-Tools `30.0.3` or higher.
- **Environment Variable:** `ANDROID_HOME` or Android SDK installed in default path (`%LOCALAPPDATA%\Android\Sdk`).

---

## ⚡ Option 1: Ultra-Fast SDK Direct Build (`build_apk.ps1`)

This script bypasses Gradle daemons and builds the APK directly through the Android SDK toolchain:

### Steps executed internally:
1. **`aapt2 compile`**: Compiles XML drawables, layouts, and colors into `.flat` files.
2. **`aapt2 link`**: Packages resources with `android.jar` and generates `R.java`.
3. **`javac`**: Compiles Java source files (`MainActivity.java` and `R.java`).
4. **`d8.jar`**: Converts `.class` bytecode into Dalvik Executable (`classes.dex`).
5. **Zip packaging**: Adds `classes.dex` and compiled resources into unaligned APK.
6. **`zipalign`**: 4-byte aligns APK for optimal memory mapping on Android.
7. **`apksigner`**: Signs APK with APK Signature Scheme v2 & v3 using `debug.keystore`.

### To run:
```powershell
powershell -ExecutionPolicy Bypass -File .\build_apk.ps1
```

**Output:**
```
=== BUILD SUCCESSFUL ===
Final APK generated at: c:\work\jobs\tv\build\out\NuttyLauncher.apk
Size:  518517 bytes (~506 KB)
```

---

## 🐘 Option 2: Standard Gradle Build

If building in Android Studio or standard CI:
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📲 Deploy to TV via ADB

```bash
# 1. Connect to Android TV IP
adb connect 192.168.31.124:5555

# 2. Stream install APK
adb install -r release/NuttyLauncher-v1.0.apk

# 3. Start launcher activity
adb shell am start -n com.nuttyflix.launcher/.MainActivity
```
