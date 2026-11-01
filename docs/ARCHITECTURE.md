# Architecture & Technical Design

## Overview

NuttyFlix Launcher is built with a **Zero-Bloat Native First** design philosophy. Unlike many modern Android TV launchers built with heavy third-party toolchains, hybrid webviews, or bloated Leanback UI libraries that consume 100+ MB of RAM, NuttyFlix is written in pure vanilla Android Framework APIs (Android 11 / API 30).

---

## Key Design Principles

### 1. Minimal Footprint & Fast Startup
- **Single Activity Architecture:** The entire launcher runs inside `MainActivity.java`.
- **Zero Third-Party Dependencies:** Zero dependencies in `build.gradle` (no Jetpack Leanback, no Glide/Picasso, no Retrofit).
- **In-Memory App Discovery:** Queries `PackageManager` once on launch and caches package metadata.
- **Resource Optimization:** Uses vector drawables for UI icons (`ic_settings_gear.xml`) and high-efficiency WebP/PNG for wallpaper. Total compiled APK size is only ~518 KB.

### 2. D-Pad TV Navigation & Remote Focus System
TV remotes rely exclusively on directional keyevents (`KEYCODE_DPAD_UP`, `DOWN`, `LEFT`, `RIGHT`, `DPAD_CENTER`/`ENTER`).
- **Focus Scale Animations:** Focused cards scale to `1.08f` with an elevated shadow and `#FFFFFF` high-contrast background outline. Unfocused cards scale back smoothly to `1.0f`.
- **View Hierarchy Isolation:** Containers (`FrameLayout`, `LinearLayout`, `ScrollView`, `GridLayout`) use `clipChildren="false"` and `clipToPadding="false"` to prevent focus enlargement and elevation shadows from being clipped.
- **Focus Preservation:** When rearranging or updating lists, views are manipulated directly in `GridLayout` rather than destroying/re-creating child views. This guarantees focus stability and prevents Android's focus subsystem from jumping to the top-bar Settings icon.

### 3. App State & Reordering Persistence
- App categories (`Favorites` vs `Applications`) and custom order are persisted in `SharedPreferences` as comma-separated package name strings:
  - Key: `key_fav_order`
  - Key: `key_custom_apps`
- Dynamic Fallback: If `key_fav_order` is empty (first launch), the launcher automatically categorizes known streaming services (Netflix, YouTube, Disney+, Prime Video, Viu, TrueID, AIS Play, Monomax, HBO) into Favorites and others into Applications.

### 4. Interactive App Management
- **Long Press OK:** Detected using `setOnLongClickListener` on every app card.
- **Minimal Action Dialog:** A custom `Dialog` styled with dark translucent background (`#E61E293B`), rounded corners (`16dp`), and high-contrast white pill buttons (`btn_dialog_action_selector.xml`).
- **Options Provided:**
  1. `Move`: Enters in-place rearrange mode with amber border highlight (`#F59E0B`).
  2. `Favorite / Unfavorite`: Moves the app between sections instantly.
  3. `Detail`: Launches `android.settings.APPLICATION_DETAILS_SETTINGS` for the target package.
  4. `Uninstall`: Launches `android.intent.action.UNINSTALL_PACKAGE` (with red text `#EF4444`).

---

## Directory Structure

```
tv/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # TV banner & LEANBACK_LAUNCHER category
│       ├── java/com/nuttyflix/launcher/
│       │   └── MainActivity.java        # Core launcher logic, remote events, app management
│       └── res/
│           ├── color/                   # Text color selectors for focus states
│           ├── drawable/                # Selectors, backgrounds, gear vector drawable
│           └── layout/
│               └── activity_main.xml    # Top bar (Clock/Date/Settings) & centered Grids
├── build_apk.ps1                        # 5-second SDK direct compilation script
├── debug.keystore                       # Standard signing keystore
├── docs/                                # Full technical documentation
└── release/
    └── NuttyLauncher-v1.0.apk           # Production ready binary (<550 KB)
```
