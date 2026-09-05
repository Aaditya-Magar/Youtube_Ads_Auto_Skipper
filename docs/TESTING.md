# Verification — 2026-09-05

## Completed

- Clean debug APK and instrumentation APK build with Gradle 9.4.1 / AGP 9.2.1.
- Android lint: zero errors. One advisory recommends a newer Gradle version;
  the project intentionally pins the version supported by the selected AGP.
- Twelve executable Java assertions for accepted IDs and rejected package/ID
  combinations, null inputs, misleading text and countdown controls.
- Pixel 6 emulator, Android 15 / API 35: instrumentation checks accept an
  available button and reject hidden, disabled, non-clickable and unrelated nodes.
- Same emulator: first-launch disclosure, Not now, saved onboarding choice,
  settings return, Ready status, pause, reopening, and access revocation.
- Portrait onboarding and Ready screenshots inspected for readability and
  controls clear of system bars.
- APK signature verified; APK declares minimum API 26 and target API 37.
- Runtime dependency report contains no third-party dependencies.

The emulator smoke script uses shell privileges to grant/revoke access solely
for testing. The production app always sends users to Android Settings.

## YouTube compatibility remains unverified

The emulator's preinstalled YouTube version is **19.17.42**. Its APK contains the
six exact resource IDs currently used by the detector, and the legacy/modern
Skip layouts reference the Skip container and label IDs. This verifies that
the identifiers exist, not that each is exposed or clickable during a live ad.

The attempted test video opened in the mobile browser after launching through
YouTube. No live ad skip was observed in the official app. Browser playback is
outside this app's scope and does not count as a successful integration test.

## Next device checks

- Use a current official YouTube app on an emulator or physical phone to observe
  a skippable ad and confirm exactly one intended click when it becomes available.
- Check consecutive ads, unskippable ads, portrait/landscape, mini-player,
  fullscreen, different languages, and returning to other apps.
- Verify Android 8.0 on-device behavior; lint checks API use but does not replace
  testing an API 26 device.
- Test newer Android releases, tablet/foldable layouts, larger font sizes, and
  manufacturer-specific accessibility/background restrictions.
- Confirm manual accessibility setup for a downloaded APK, including restricted
  settings where applicable. Force-stopping an app may require re-enabling its
  accessibility service; ordinary reopening is what the smoke script tests.

Do not list a device/YouTube combination as supported until a live Skip action
has been observed. Report unknown UI layouts instead of adding coordinate taps
or broad text matching.
