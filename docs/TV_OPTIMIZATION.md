# Panasonic 4K Android TV Optimization Guide

This guide documents the non-destructive debloating and memory optimization steps performed on the Panasonic 4K Android TV (Platform: `Jupiter_PA`, Model: `4KTV-JUP`, Android 11 / API 30).

---

## 🔒 Safety First (Zero-Risk Principles)
1. **No `/system` or `/vendor` partition modifications:** Safe for OTA system updates and preserves Widevine L1 / PlayReady DRM (Netflix 4K, Disney+ 4K, HDR10/Dolby Vision intact).
2. **Reversible via ADB:** Any disabled package can be re-enabled instantly with `adb shell pm enable <package>`.

---

## 1. Disable Telemetry, Bloatware & Hotel Services

The following packages were disabled for user 0 (`pm disable-user --user 0`):

```bash
# Hotel mode / Demo mode
adb shell pm disable-user --user 0 com.panasonic.hotelapp

# Analytics & OEM Telemetry
adb shell pm disable-user --user 0 com.google.android.feedback
adb shell pm disable-user --user 0 com.android.traceur

# Unused system TV services / Google Partner Bloat
adb shell pm disable-user --user 0 com.google.android.partnersetup
adb shell pm disable-user --user 0 com.google.android.videos
```

---

## 2. Background Process Restrictions

To free up RAM on resource-constrained TV SoCs (typically 1.5 GB to 2.0 GB RAM):

```bash
# Prevent inactive OEM packages from running in background
adb shell cmd appops set com.panasonic.hotelapp RUN_IN_BACKGROUND ignore
adb shell am set-standby-bucket com.panasonic.hotelapp restricted
```

---

## 3. UI Animation Tuning (Zero Stutter / Zero Latency)

Setting animation scale to `0x` eliminates animation rendering lag on TV chipsets:

```bash
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
```

---

## 4. Set NuttyFlix as Default Launcher

```bash
# Verify launcher is installed
adb shell pm list packages | grep nuttyflix

# Set default home intent
adb shell am start -n com.nuttyflix.launcher/.MainActivity
```
Press the `HOME` button on the Panasonic remote and select **NuttyLauncher** -> **Always**.
