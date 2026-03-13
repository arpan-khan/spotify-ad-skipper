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

# Build and install debug APK
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

### Debug Build (Development)

**Purpose**: Testing and development

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
- No optimization (~2-3 MB)
- Fast build (~20s)
- Includes debug logs

### Release Build (Production)

**Purpose**: Distribution to users

```bash
# Build (requires keystore setup)
./gradlew assembleRelease

# Output
app/build/outputs/apk/release/app-release.apk
```

**Characteristics**:
- Requires release keystore
- ProGuard optimized (~1-2 MB)
- Slower build (~40s)
- Debug logs removed

**For release build setup**, see `.kiro/steering/build-and-deploy.md`

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

## Release Process

For creating production releases, see:
- `.kiro/steering/build-and-deploy.md` - Complete build/deploy guide
- `.kiro/steering/deployment-checklist.md` - Release checklist

**Quick summary**:
1. Update version in `app/build.gradle.kts`
2. Build release: `./gradlew assembleRelease`
3. Test release build
4. Create GitHub release with APK

## Contributing

### Spec-Driven Development

This project follows spec-driven development:
- `.kiro/specs/spotify-ad-skipper/requirements.md` - Functional requirements
- `.kiro/specs/spotify-ad-skipper/design.md` - Technical design
- `.kiro/specs/spotify-ad-skipper/tasks.md` - Implementation tasks

### Before Submitting

1. Run tests: `./gradlew test`
2. Build successfully: `./gradlew assembleDebug`
3. Follow Kotlin conventions
4. Update documentation if needed
5. Test on physical device

## Additional Resources

- [Shizuku Documentation](https://github.com/RikkaApps/Shizuku)
- [Android Developer Guide](https://developer.android.com/guide)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)

## Development Guidelines

See `.kiro/steering/` for detailed guidelines:
- `build-and-deploy.md` - APK build and deployment process
- `deployment-checklist.md` - Release checklist
- `shizuku-integration.md` - Shizuku API integration
- `kotlin-coding-standards.md` - Kotlin conventions
- `android-platform-guidelines.md` - Android best practices
