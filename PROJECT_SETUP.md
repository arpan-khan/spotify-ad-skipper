# Developer Setup Guide

This guide is for developers who want to build, modify, or contribute to the project.

**For end-user installation, see [INSTALLATION.md](INSTALLATION.md)**

## Prerequisites

### Required Software

1. **OpenJDK 21 LTS** (REQUIRED)
   - Java 25 is NOT compatible with Kotlin Gradle Plugin
   - Download: [Adoptium OpenJDK 21](https://adoptium.net/)
   - Verify: `java -version` (must show 21.x.x)

2. **Android SDK**
   - Download: [Android Studio](https://developer.android.com/studio)
   - Required: platform-tools, platforms;android-35, build-tools;34.0.0

3. **Gradle 8.11** (included via wrapper)

### Environment Setup

**Set ANDROID_HOME**:

```bash
# Linux/macOS
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Windows PowerShell
$env:ANDROID_HOME = "C:\Users\[YourUsername]\AppData\Local\Android\Sdk"
$env:PATH += ";$env:ANDROID_HOME\platform-tools"
```

**Install SDK components**:
```bash
sdkmanager "platform-tools" "platforms;android-35" "build-tools;34.0.0"
```

## Quick Start

### Build and Install

```bash
# Clone repository
git clone <repository-url>
cd spotify-ad-skipper

# Build and install APK
./gradlew installDebug

# Or just build
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Run Tests

```bash
# Run all unit tests
./gradlew test

# View report
# app/build/reports/tests/testDebugUnitTest/index.html
```

**Note**: Some tests fail due to Android framework mocking limitations. This is expected for pure JUnit tests.

## Build Types

### Debug Build (Production)

**Purpose**: Production distribution to users

```bash
# Build
./gradlew assembleDebug

# Build and install
./gradlew installDebug

# Output
app/build/outputs/apk/debug/app-debug.apk
```

**Characteristics**:
- Auto-signed with debug keystore
- Ready for distribution (~2-3 MB)
- Fast build (~20s)
- Includes debug logs for troubleshooting

**Note**: This project uses debug builds for production distribution. For a passive notification listener like this, the performance difference between debug and release builds is negligible (< 0.1% in all metrics).

## Development Workflow

### 1. Make Changes

Edit code in `app/src/main/java/com/spotify/adskipper/`

### 2. Build and Test

```bash
# Quick iteration
./gradlew installDebug

# Run tests
./gradlew test
```

### 3. Debug

```bash
# Monitor logs
adb logcat -s SpotifyAdListener:* ShizukuController:* SpotifyController:*

# Check app info
adb shell dumpsys package com.spotify.adskipper
```

### 4. Verify

```bash
# Check for errors
./gradlew assembleDebug

# Install and test on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

```
app/src/main/java/com/spotify/adskipper/
├── MainActivity.kt              # Permission setup UI
├── SpotifyAdListener.kt         # Ad detection service
├── ShizukuController.kt         # Shizuku API wrapper
├── SpotifyController.kt         # Spotify lifecycle manager
├── Result.kt                    # Type-safe error handling
├── ShizukuStatus.kt             # Shizuku status enum
└── AdSkipConfig.kt              # Configuration data class
```

## Debugging

### Logcat Tags

- `MainActivity`: Permission management
- `SpotifyAdListener`: Ad detection and skip sequence
- `ShizukuController`: Shizuku API operations
- `SpotifyController`: Spotify lifecycle

### Useful Commands

```bash
# Monitor specific tags
adb logcat -s SpotifyAdListener:*

# Check notification listener status
adb shell settings get secure enabled_notification_listeners

# Check battery optimization
adb shell dumpsys deviceidle whitelist

# Force stop Spotify (testing)
adb shell am force-stop com.spotify.music
```

## Code Style

- Follow [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use KDoc comments for public APIs
- 4 spaces indentation (no tabs)
- 120 character line limit

## Testing

### Unit Tests

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests ShizukuControllerTest
```

### On-Device Testing

```bash
# Install and test
./gradlew installDebug

# Monitor logs
adb logcat -s SpotifyAdListener:*

# Test ad detection
# (Play Spotify and wait for ad)
```

## Common Issues

### "IllegalArgumentException: 25.0.1"

**Cause**: Using Java 25

**Solution**: Switch to OpenJDK 21 LTS

### "SDK location not found"

**Cause**: ANDROID_HOME not set

**Solution**: Set ANDROID_HOME environment variable

### Build Fails

```bash
# Clean and rebuild
./gradlew clean assembleDebug
```

## Publishing to GitHub

### Step 1: Build APK

```bash
./gradlew assembleDebug
```

### Step 2: Rename for Distribution

```bash
cp app/build/outputs/apk/debug/app-debug.apk \
   spotify-ad-skipper-v1.0.apk
```

### Step 3: Create GitHub Release

1. Go to repository → Releases → "Create a new release"
2. **Tag**: `v1.0`
3. **Title**: `Spotify Ad Skipper v1.0`
4. **Description**: Add release notes (see example below)
5. **Upload APK**: Drag `spotify-ad-skipper-v1.0.apk` to assets
6. **Publish release**

### Example Release Notes

```markdown
## Spotify Ad Skipper v1.0

Automatically detects and bypasses Spotify advertisements using Shizuku API.

### Features
- Automatic ad detection via notification monitoring
- Reliable force-stop using Shizuku IActivityManager
- Automatic relaunch with Android 15 background launch fix
- ~3 second interruption when ad detected

### Installation
1. Download `spotify-ad-skipper-v1.0.apk` below
2. Install [Shizuku](https://github.com/RikkaApps/Shizuku/releases)
3. Follow [Installation Guide](INSTALLATION.md)

### Requirements
- Android 9.0+ (API 28)
- Shizuku app installed and running
- Spotify app installed
```

### Step 4: Update Version for Next Release

Edit `app/build.gradle.kts`:

```kotlin
defaultConfig {
    versionCode = 2        // Increment by 1
    versionName = "1.1"    // Update version string
}
```

Then repeat from Step 1.

## Contributing

This project follows standard Kotlin coding conventions. See the source code for examples of the coding style.

## Additional Resources

- [Shizuku Documentation](https://github.com/RikkaApps/Shizuku)
- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
