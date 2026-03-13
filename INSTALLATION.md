# Quick Installation Guide

Complete installation in ~10 minutes.

## Prerequisites

- Android device (Android 9.0+)
- Spotify app installed
- Computer with ADB (for Shizuku setup)

## Step 1: Install Shizuku (5 min)

**Download**: [Shizuku GitHub Releases](https://github.com/RikkaApps/Shizuku/releases)

**Start Service** (choose one):

### Option A: Wireless ADB (No USB Cable)
1. Settings → About Phone → Tap "Build Number" 7 times
2. Settings → Developer Options → Wireless Debugging → ON
3. Open Shizuku app → Tap "Pair"
4. Follow pairing instructions
5. Service starts automatically ✓

### Option B: USB ADB (Requires Computer)
1. Settings → Developer Options → USB Debugging → ON
2. Connect device to computer via USB
3. On computer: `adb devices`
4. Accept "Allow USB debugging" on device
5. Open Shizuku app - service starts automatically ✓

## Step 2: Install Spotify Ad Skipper (1 min)

**Download APK** from [GitHub Releases](../../releases)

**Install**:
```bash
# Via ADB
adb install spotify-ad-skipper-v1.0.apk

# Or manually: Transfer to device and tap to install
```

## Step 3: Grant Permissions (2 min)

Launch the app and follow the checklist:

### ✓ Notification Access
Tap button → Enable in settings → Return to app

### ✓ Shizuku Permission
Tap button → Allow in dialog → Done

### ✓ Battery Optimization
Tap button → Select "Don't optimize" → Return to app

## Step 4: Samsung Devices Only (1 min)

```
Settings → Device Care → Battery → Background usage limits
→ Remove app from "Sleeping apps"

Settings → Apps → Spotify Ad Skipper → Battery
→ Select "Unrestricted"
```

## Testing (2 min)

1. **Verify**: All three checkmarks are green ✓
2. **Test**: Open Spotify and play music
3. **Wait**: For an ad to play
4. **Confirm**: Ad is skipped within 3-4 seconds

## Troubleshooting

### Ads Not Skipping?
- Check all three checkmarks are green
- Verify Shizuku service is running
- Restart both apps

### Service Stops After Sleep?
- Remove from "Sleeping apps" (Samsung)
- Set battery to "Unrestricted"

## Total Time: ~10 minutes

For detailed troubleshooting, see [README.md](README.md)
