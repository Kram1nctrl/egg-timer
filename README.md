# Egg Timer

[![Android CI](https://github.com/Kram1nctrl/egg-timer/actions/workflows/android.yml/badge.svg)](https://github.com/Kram1nctrl/egg-timer/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/Kram1nctrl/egg-timer?display_name=tag)](https://github.com/Kram1nctrl/egg-timer/releases/latest)

A small Android app for boiling eggs. Pick soft, medium, hard, or a custom time, then watch
the countdown ring and get an alarm when the eggs are ready.

Built with Kotlin and Jetpack Compose as a first Android project.

## Download and install

1. Open the [latest release](https://github.com/Kram1nctrl/egg-timer/releases/latest).
2. Download `egg-timer-vX.Y.Z.apk` from the **Assets** section.
3. Open the file on your phone and allow installing from this source when Android asks.

The APK is signed with a release key, so it installs like any normal app. Android will still
warn you because the app did not come from the Play Store — that warning is expected for any
APK downloaded outside Google Play.

Requires Android 8.0 (API 26) or newer.

## Features

- Presets: soft (4 min, runny yolk), medium (6 min, jammy yolk), hard (9 min, firm yolk)
- Custom timer from 1 to 60 minutes
- Circular countdown ring with `m:ss` remaining
- Alarm sound plus vibration when the timer finishes
- Foreground service keeps counting while the app is backgrounded or the screen is locked
- Ongoing notification with the remaining time, and a heads-up notification when done
- Keeps the screen awake while the app is open
- Adaptive egg launcher icon, including a themed (monochrome) variant on Android 13+
- Light and dark theme, warm egg-coloured palette

## Build from source

Requires JDK 17+ and the Android SDK (compileSdk 35, build-tools 35.0.0).

```sh
export JAVA_HOME="$HOME/Android/jdk"
export ANDROID_HOME="$HOME/Android/sdk"
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Signed release builds

Release signing is configured in `app/build.gradle.kts` and reads credentials from
environment variables first, then from `~/.android-keys/keystore.properties`:

```properties
eggTimer.keystore=/home/you/.android-keys/eggtimer-release.jks
eggTimer.keystorePassword=...
eggTimer.keyAlias=eggtimer
eggTimer.keyPassword=...
```

The keystore and the properties file live outside the repository and are never committed.
Pushing a tag such as `v1.0.1` runs `.github/workflows/release.yml`, which builds the signed
APK and attaches it to a new GitHub release.

```sh
git tag v1.0.1
git push origin v1.0.1
```

## Install on a phone over USB

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
.github/workflows/
├── android.yml                             CI: build debug APK on every push and PR
└── release.yml                             release: signed APK attached to a tag
app/src/main/
├── AndroidManifest.xml                     permissions, activity, service
├── java/com/kram1nctrl/eggtimer/
│   ├── MainActivity.kt                     Compose UI and screen layout
│   ├── TimerEngine.kt                      countdown state, presets, alarm, vibration
│   ├── TimerService.kt                     foreground service, notifications, ticking
│   └── ui/theme/Theme.kt                   colour scheme
└── res/
    ├── drawable/ic_launcher_foreground.xml egg and yolk
    ├── drawable/ic_launcher_monochrome.xml themed-icon silhouette
    ├── drawable/ic_stat_egg.xml            notification icon
    ├── mipmap-anydpi-v26/ic_launcher.xml   adaptive launcher icon
    └── values/{strings,themes,colors}.xml  text, app theme, icon background
```

## How the countdown works

`TimerEngine` stores a deadline using `SystemClock.elapsedRealtime()` rather than counting
ticks. The foreground service compares that deadline with the current clock every 200 ms,
so the timer cannot drift when Android delays or skips callbacks. The UI reads the same
state object, which is why the time stays correct after rotation or backgrounding.

## Roadmap

- [ ] Multiple simultaneous timers
- [ ] Saved custom presets
- [ ] Cook history
- [ ] Instrumented UI tests

## License

No license yet. Add one (for example MIT) before reusing this code elsewhere.
