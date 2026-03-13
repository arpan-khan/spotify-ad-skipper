# Spotify Ad Skipper

A native Android application that automatically detects and bypasses Spotify advertisements using Shizuku API for reliable, invisible, and instant ad skipping.

## Overview

Spotify Ad Skipper monitors Spotify notifications in the background. When an advertisement is detected, it automatically force-stops the Spotify process, relaunches the app, and skips to the next track - all within 3.5 seconds.

## Features

- **Automatic Ad Detection**: Monitors Spotify notifications for title "Advertisement"
- **Reliable Ad Skipping**: Uses Shizuku API for 100% reliable process termination
- **Invisible Operation**: No UI flashing or visible interruption
- **Battery Efficient**: Passive notification listener with zero CPU usage when idle
- **Privacy Focused**: No data collection, storage, or transmission
- **Samsung Optimized**: Tested on Samsung One UI 7.0

## How It Works

```
1. Detection    → Monitors Spotify notifications
2. Force Stop   → Terminates Spotify via Shizuku (1000ms delay)
3. Relaunch     → Automatically relaunches Spotify
4. Skip         → Sends "Next Track" intent (2000ms delay)
                  Total time: ~3.5 seconds
```

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
# Build and install debug APK
./gradlew installDebug

# Build release APK
./gradlew assembleRelease
```

## Getting the APK

**Option A: Download Pre-built** (Recommended)
- Download from [GitHub Releases](../../releases)
- Look for `spotify-ad-skipper-v1.0.apk`

**Option B: Build from Source**
- See [PROJECT_SETUP.md](PROJECT_SETUP.md)
- APK will be at `app/build/outputs/apk/debug/app-debug.apk`

## Architecture

### Components
- **MainActivity**: Permission management UI with checklist interface
- **ShizukuController**: Singleton wrapper for Shizuku API using Remote Binder Call
- **SpotifyController**: Singleton for Spotify lifecycle management
- **SpotifyAdListener**: NotificationListenerService for passive ad detection

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

- **Spotify Only**: Only works with official Spotify app (com.spotify.music)
- **Requires Shizuku**: Shizuku must be installed and running with ADB or root
- **Android 9.0+**: Does not support older Android versions
- **Detection Delay**: Ads play for a few seconds before detection and skip
- **Not a Spotify Mod**: Works with the official app, not a modified version

## Documentation

- **[INSTALLATION.md](INSTALLATION.md)** - User installation guide (~10 minutes)
- **[PROJECT_SETUP.md](PROJECT_SETUP.md)** - Developer build and setup guide
- **[.kiro/steering/](kiro/steering/)** - Development guidelines and best practices

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
├── .kiro/specs/spotify-ad-skipper/  # Specifications
│   ├── requirements.md              # Functional requirements
│   ├── design.md                    # Technical design
│   └── tasks.md                     # Implementation tasks
└── .kiro/steering/                  # Development guidelines
    ├── build-and-deploy.md          # APK build/deploy process
    ├── deployment-checklist.md      # Release checklist
    ├── shizuku-integration.md       # Shizuku integration guide
    ├── kotlin-coding-standards.md   # Kotlin conventions
    └── android-platform-guidelines.md
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
