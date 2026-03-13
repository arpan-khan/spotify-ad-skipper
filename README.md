# Spotify Ad Skipper

A native Android application that automatically detects and bypasses Spotify advertisements using Shizuku API for reliable, invisible, and instant ad skipping.

## Overview

Spotify Ad Skipper monitors Spotify notifications in the background. When an advertisement is detected, it automatically force-stops the Spotify process, relaunches the app, and skips to the next track - all within 3.5 seconds.

## Features

- **Automatic Ad Detection**: Monitors Spotify notifications for title "Advertisement"
- **Reliable Ad Skipping**: Uses Shizuku API for 100% reliable process termination
- **Quick Recovery**: Spotify closes and reopens within ~3 seconds
- **Battery Efficient**: Passive notification listener with zero CPU usage when idle
- **Privacy Focused**: No data collection, storage, or transmission
- **Samsung Optimized**: Tested on Samsung One UI 7.0

## How It Works

```
1. Detection    → Monitors Spotify notifications (~2s after ad starts)
2. Force Stop   → Terminates Spotify via Shizuku IActivityManager (app closes)
3. Relaunch     → Relaunches Spotify via Shizuku with elevated privileges (1000ms delay)
4. Skip         → Sends "Next Track" intent (2000ms delay)
                  Total interruption: ~3 seconds (visible close/reopen)
```

**Note**: This is NOT like Spotify Premium. The app will visibly close and reopen when an ad is detected. Ads play for ~2 seconds before detection, then Spotify restarts within ~3 seconds.

**Android 15 Fix**: Uses Shizuku to bypass background activity launch restrictions. Both force-stop and relaunch operations use IActivityManager with elevated privileges.

## Requirements

- **Android 9.0 (API 28)** or higher
- **Spotify App**: Official Spotify app installed
- **Shizuku App**: Required for privileged operations ([Download](https://github.com/RikkaApps/Shizuku/releases))

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

## Architecture

### Components
- **MainActivity**: Permission management UI with checklist interface
- **ShizukuController**: Force-stop AND activity launch via IActivityManager (AndroidHiddenApiBypass + reflection)
- **SpotifyController**: Spotify lifecycle management (relaunch + skip intents)
- **SpotifyAdListener**: NotificationListenerService for passive ad detection

### Key Implementation Details
- **Android 15 Background Launch Fix**: Uses Shizuku's IActivityManager.startActivityAsUser() to bypass background activity launch restrictions
- **Hidden API Bypass**: AndroidHiddenApiBypass library exempts IActivityManager from runtime restrictions
- **Calling Package**: Uses "com.android.shell" as callingPackage to match Shizuku's uid=2000

### Technologies
- **Kotlin 2.1.0**: Primary programming language
- **Shizuku API 13.1.5**: Privileged process management
- **Kotlin Coroutines**: Async ad skip sequence execution
- **AndroidX**: Modern Android framework components

## Security & Privacy

- **No Data Collection**: The app does not collect, store, or transmit any user data
- **Minimal Permissions**: Only requests essential permissions for functionality
- **Scoped Operations**: All operations are scoped to Spotify package only
- **No Network Access**: No internet permission requested or used
- **Open Source**: Code is available for review and audit

## Limitations

- **Visible Interruption**: Spotify will close and reopen when an ad is detected (~3 seconds)
- **Detection Delay**: Ads play for ~2 seconds before detection
- **Not Like Premium**: This is not seamless ad-free listening
- **Spotify Only**: Only works with official Spotify app (com.spotify.music)
- **Requires Shizuku**: Shizuku must be installed and running with ADB or root
- **Android 9.0+**: Does not support older Android versions
- **Playlist Context**: After ad skip, playback resumes on the playlist that was playing when the ad started (not the playlist you switched to during the ad)

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

This application is not affiliated with, endorsed by, or connected to Spotify AB or Shizuku. All trademarks are the property of their respective owners.

**Legal Notice**: This app modifies the normal operation of the Spotify application. Use may violate Spotify's Terms of Service. The developers assume no liability for any consequences of using this software.

## Acknowledgments

- **Shizuku** by RikkaApps - Provides the privileged API access
- **Spotify** - For the music streaming service
- **Android Open Source Project** - For the platform and documentation
