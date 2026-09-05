# Auto Skip for Android

![Android CI](https://github.com/Aaditya-Magar/YT_Ads_Auto_Skipper/actions/workflows/android.yml/badge.svg)

A small Android app that automatically taps recognized, available **Skip** buttons
in the official YouTube app. Designed for **Android 8.0+ (API 26)** phones,
tablets and foldables. Android TV is outside the current scope.

<img src="docs/screenshots/onboarding.png" alt="First-launch accessibility setup" width="260"> <img src="docs/screenshots/ready.png" alt="Auto Skip ready screen with on/off control" width="260">

## What it does

- Explains accessibility access on first launch and opens the system settings.
- Lets you defer setup and enable it later.
- Shows setup status and provides a persistent auto-skip switch.
- Watches YouTube accessibility events and clicks identified Skip controls.
- Processes everything locally, without accounts, analytics or network access.

**Compatibility is provisional.** YouTube controls its interface and can change
the resource identifiers this app uses. Unknown controls are ignored. This app
does not block ads, bypass countdowns, or skip ads without an available button.
The English app interface uses resource strings so translations can be added;
the detector uses resource IDs rather than translated button labels.

## Build

Open this folder in Android Studio, or install a full JDK 17 or newer compatible
with Gradle 9.4.1 and configure `ANDROID_HOME` for your Android SDK. Install SDK
Platform 37 and Android SDK Build-Tools 36.0.0. The project uses Android Gradle
Plugin 9.2.1 and the included Gradle wrapper.

```sh
./gradlew :app:assembleDebug :app:lintDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

If your system provides only a Java runtime, set `JAVA_HOME` to Android Studio's
bundled `jbr` folder or another full JDK. Do not commit machine-specific SDK paths.
The first build needs internet access to download build tools and dependencies.
There are no third-party runtime dependencies.

## Install and set up

1. Install the debug APK on your emulator or phone. For a downloaded APK, Android
   may ask you to allow installation from the app opening the file.
2. Open **Auto Skip** and read the accessibility disclosure.
3. Tap **Open Accessibility Settings**, find **Auto Skip**, and enable its service.
4. Return to the app. It should show **Ready to skip** with Auto-skip switched on.
5. Open the official YouTube app and play a video. The app waits for a recognized
   clickable Skip button. Toggle Auto-skip off to pause.

If access is blocked for a downloaded APK, use **Setup help**. Android may require
**App info → menu → Allow restricted settings** before enabling accessibility.
Only enable access for a build you trust. Instructions vary by manufacturer.
You can revoke access at any time in Android's Accessibility Settings.

Debug APKs are for development/testing. Before publishing downloadable release
APKs, create and retain your own release signing key using Android Studio's
**Generate Signed App Bundle / APK** flow. Never commit that key or its passwords.
This project is independent of YouTube and Google.

## Checks

Pure-Java matching checks, requiring `java` and `javac` on `PATH`:

```sh
sh tests/check.sh
```

Build the dependency-free Android instrumentation checks:

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
```

Start a **disposable emulator**, then run:

```sh
python3 tests/emulator_check.py emulator-5554
```

Replace the serial with the one shown by `adb devices`. This script clears Auto
Skip's data, checks node safety and onboarding, temporarily changes emulator
accessibility settings, verifies enabling/pausing/reopening/revocation, and
restores those settings afterward. It refuses physical-device serials. Screenshots
are saved under `app/build/smoke/`. The custom instrumentation runner is invoked
by this script; use the script rather than Gradle's connected test runner.

See [verification notes](docs/TESTING.md) for actual test results and remaining
device checks. Passing synthetic node checks does **not** establish live YouTube
ad compatibility.

## How it works

`MainActivity` owns onboarding and preferences. `SkipService` subscribes only to
`com.google.android.youtube` events, coalesces bursts, checks the active window,
and scans at most 400 accessibility nodes per pass. `SkipRule` accepts six exact
YouTube resource IDs. Only visible, enabled, clickable matches are acted on, with
an 800 ms delay between successful clicks. No coordinate taps or broad text
matching are used. If the UI changes mid-scan, the service waits for another event.

Screen content is never persisted or transmitted. The app stores only setup and
toggle preferences; backup and device transfer of those preferences are disabled.
The service is protected by Android's `BIND_ACCESSIBILITY_SERVICE` permission.

## Reporting compatibility issues

Include the device model, Android version, YouTube version, YouTube language,
portrait/landscape mode, and whether accessibility and Auto-skip were enabled.
Avoid including account details or private screen content. New detector IDs
should be verified against a real Skip control before adding them.
