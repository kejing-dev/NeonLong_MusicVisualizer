# Neon Lóng

A volumetric 3D music visualizer for PICO OS 6: a cyberpunk Chinese dragon, built like a floating neon city, that turns whatever you listen to into light and motion in your room.

It's a native Kotlin app on the PICO Spatial SDK (ECS) and Spatial UI (Compose). It isn't a web app: no WebView, WebXR or JavaScript. A build check enforces this.

## Features
- A dragon of 27 body segments carrying 81 towers that work as a live spectrum analyzer. Kicks run head to tail, and the jaw, whiskers and tail answer the bass, mids and highs.
- It lives in a Volumetric window in your Shared Space. You can resize the window, turn the dragon on its turntable ring, and touch its segments to send ripples.
- The 龍 badge opens a control panel with transport, palette (Kowloon, Imperial, Jade), sensitivity and settings (reduce motion, reduce flashing, seated, spectrum panel, interface sounds).
- A safety notice is shown before any audio plays. All dragon-wide flashes go through a flash limiter.
- English and Simplified Chinese.
- No internet permission. Audio is analyzed on the device and never stored.

## Requirements
| | Version |
|---|---|
| Android Studio | 2025.1.4, with the PICO Spatial Plugin |
| JDK | Android Studio's bundled JetBrains Runtime 21 |
| Gradle / AGP / Kotlin | 8.13 / 8.13.2 / 2.0.0 |
| PICO Spatial SDK | BOM `com.pico.spatial:bom` 6.1.9; Spatial Tools plugin 6.1.0; Spatial Editor 6.1.9 |
| Android SDK | compileSdk, minSdk and targetSdk 35 |
| Device | PICO OS 6 headset or PICO Emulator 6.1.0 |

Keep the SDK, Spatial Editor and Emulator on the same major.minor version.

## Build and run
From this folder. On Windows with Git Bash, point `JAVA_HOME` at Android Studio's JDK first:

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
```

| Task | Command |
|---|---|
| Unit tests | `./gradlew :game-logic:test :tools:assetgen:test :tools:demosynth:test` |
| Debug APK | `./gradlew :app:assembleDebug` |
| Install | `adb install -r app/build/outputs/apk/debug/app-debug.apk` |
| Logs | `adb logcat -s NeonLong:V` |
| No-web check | `./gradlew :app:verifyNoWeb` (also runs before every build) |
| SDK API index | `./gradlew :app:dumpSdkApi` |

If the Spatial Tools plugin can't build from the command line, build in Android Studio.

## Project layout
| Module | Contents |
|---|---|
| `app` | The Android app: containers (`dragon` volume; `controls`, `safety`, `spectrum` windows), ECS Systems, rendering, audio, input, settings and UI. |
| `game-logic` | Pure Kotlin with no Android or PICO imports: the dragon simulation, audio analysis, beat detection, flash limiter, palettes, settings model and configs. Tested with JUnit 5. |
| `tools/assetgen` | Generates every `GEN_` model (glTF) and texture, plus a triangle report. |
| `tools/demosynth` | Generates the demo track, its kick timings and the interface sounds. |
| `editor-asset` | The Spatial Editor project from the template. Don't hand-edit it. |

Generated assets go to `app/build/generated/neonlong/assets` during the build and are never committed. They're deterministic, so every build produces the same files.

## Configuration
Gameplay and UI numbers live in config classes in `game-logic` (`logic/config`). The JSON files in `app/src/main/assets/config/` override them. Unknown keys are ignored, and missing keys keep their defaults.

## Conventions
- Systems are registered by their container and unregistered when it closes. No allocations in `System.update`.
- `ServiceRegistry` is the only singleton.
- Placeholder entities use the `PH_` prefix and generated assets use `GEN_`.
- All UI text lives in `res/values/strings.xml` and `res/values-zh-rCN/strings.xml`. The two files must have the same keys (checked by a test).

## Status
In development. Done: the full dragon driven by the demo track, the control panel, settings and persistence. Next: music sources (files, other apps, microphone), the immersive Stage, performance and store readiness.
