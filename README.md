> **Disclaimer**: This project is an independent fork of the original [sihooney/spotify-ad-skipper](https://github.com/sihooney/spotify-ad-skipper) with custom enhancements and fixes. This project is provided as-is without any promise of future updates or ongoing maintenance.

---

### What's Changed in This Fork

#### Behavior fix
- **No longer kicks you to the home screen** when it skips an ad — it kills/relaunches Spotify silently in the background and snaps focus back to whatever app you were actually in (game, browser, etc.), using Shizuku to capture and restore the foreground task.
- **Removed the unnecessary "send Spotify home" step** before killing it — wasn't needed.

#### New feature: on/off toggle
- **A master switch** inside the app to turn ad-skipping on/off without revoking permissions.
- **State is saved** (`SharedPreferences`) and persists across app restarts.
- **When off**, the notification listener ignores ads entirely.
- **Toggle is disabled** until Notification + Shizuku permissions are actually granted, so it can't show "on" when it can't work.

#### UI overhaul
- **Full redesign** from the plain permission-checklist screen to a modern dark, Spotify-themed UI: hero status banner, pill-style status badges, rounded cards, custom icons, ripple buttons.
- **Smarter Shizuku button** — now context-aware (Install / Open / Grant, depending on actual Shizuku state) instead of one static button.
- **Original functionality** (grant notification access, grant Shizuku, battery optimization exemption) all preserved underneath the new visuals.

#### Per-app exclusions & skip stats

* Exclude specific apps so ads play normally in them instead of triggering skip — manage from a new screen with search, sorted with excluded apps on top.
* Tracks total/today/this week ads skipped plus an estimated time saved, shown on the main screen.

---

# Spotify Ad Skipper

A native Android application that automatically detects and bypasses Spotify advertisements using Shizuku API.

## Overview

Spotify Ad Skipper monitors Spotify notifications in the background. When an advertisement is detected, it closes Spotify (emulating manual swipe-to-close), relaunches it, and sends a play intent - all automatically.

## Features

- **Automatic Ad Detection**: Monitors Spotify notifications for "Advertisement"
- **Queue Preservation**: Emulates manual close to preserve Spotify's queue state
- **Reliable Relaunch**: Uses Shizuku to bypass Android 15 background launch restrictions
- **Auto-Play**: Sends play intent after relaunch to resume playback
- **Battery Efficient**: Passive notification listener with zero CPU usage when idle
- **Privacy Focused**: No data collection, storage, or transmission
- **Samsung Optimized**: Tested on Samsung One UI 7.0

## Requirements

- **Android 9.0 (API 28)** or higher
- **Spotify App**: Official Spotify app installed
- **Shizuku App**: Required for relaunching from background ([Download](https://github.com/RikkaApps/Shizuku/releases))

## Quick Start

### For Users

See [INSTALLATION.md](INSTALLATION.md) for complete installation instructions.

**Quick summary**:
1. Install Shizuku and start the service
2. Install Spotify Ad Skipper APK
3. Grant three permissions (Notification, Shizuku, Battery)
4. Done! Ads will be skipped automatically

### For Developers

See [PROJECT_SETUP.md](PROJECT_SETUP.md) for build instructions.

**Quick summary**:
```bash
# Build and install APK
./gradlew installDebug

# Output APK location
# app/build/outputs/apk/debug/app-debug.apk
```

## Getting the APK

**Option A: Download Pre-built** (Recommended)
- Download from [GitHub Releases](../../releases)
- Look for `spotify-ad-skipper-v1.0.apk`

**Option B: Build from Source**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

- **[INSTALLATION.md](INSTALLATION.md)** - User installation guide (~10 minutes)
- **[PROJECT_SETUP.md](PROJECT_SETUP.md)** - Developer build and setup guide

## Project Structure

```
spotify-ad-skipper/
├── app/src/main/java/com/spotify/adskipper/
│   ├── MainActivity.kt              # Permission setup UI
│   ├── SpotifyAdListener.kt         # Ad detection service
│   ├── ShizukuController.kt         # Shizuku API wrapper
│   ├── SpotifyController.kt         # Spotify lifecycle manager
│   ├── Result.kt                    # Type-safe error handling
│   ├── ShizukuStatus.kt             # Shizuku status enum
│   └── AdSkipConfig.kt              # Configuration data class
├── app/src/test/                    # Unit tests
└── app/build.gradle.kts             # Build configuration
```

## Troubleshooting

### Ads Not Being Skipped?

1. Open Spotify Ad Skipper - verify all three checkmarks are green
2. Open Shizuku app - verify service is running
3. Check battery optimization is disabled
4. Restart both apps if needed

### Service Stops After Sleep?

**Samsung devices**: 
- Settings → Device Care → Battery → Remove app from "Sleeping apps"
- Settings → Apps → Spotify Ad Skipper → Battery → "Unrestricted"

See [INSTALLATION.md](INSTALLATION.md) for detailed troubleshooting.

## License

This project is provided as-is for educational purposes. Use at your own risk.

## Disclaimer

This application is not affiliated with, endorsed by, or connected to Spotify AB or Shizuku. All trademarks are the property of their respective owners. The developers assume no liability for any consequences of using this software.

## Acknowledgments

- **Shizuku** by RikkaApps - Provides the privileged API access
- **Spotify** - For the music streaming service
- **Android Open Source Project** - For the platform and documentation
