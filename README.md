# Egg Timer

A small Android app for boiling eggs. Pick soft, medium, hard, or a custom time, then watch
the countdown ring and get an alarm when the eggs are ready.

Built with Kotlin and Jetpack Compose as a first Android project.

## Features

- Presets: soft (4 min, runny yolk), medium (6 min, jammy yolk), hard (9 min, firm yolk)
- Custom timer from 1 to 60 minutes
- Circular countdown ring with `m:ss` remaining
- Alarm sound plus vibration when the timer finishes
- Foreground service keeps counting while the app is backgrounded or the screen is locked
- Ongoing notification with the remaining time, and a heads-up notification when done
- Keeps the screen awake while the app is open
- Light and dark theme, warm egg-coloured palette

## Requirements

- Android 8.0 (API 26) or newer
- To build: JDK 17+ and the Android SDK (compileSdk 35, build-tools 35.0.0)

## Build

```sh
export JAVA_HOME="$HOME/Android/jdk"
export ANDROID_HOME="$HOME/Android/sdk"
./gradlew assembleDebug
```

The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

## Install on a phone

1. Copy `app-debug.apk` to the phone (USB cable, cloud drive, or `adb install`).
2. Open it and allow installing from unknown sources when Android asks.
3. Launch **Egg Timer** from the app drawer.

With a USB-connected phone and developer options enabled:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project layout

```
app/src/main/
├── AndroidManifest.xml                     permissions, activity, service
├── java/com/kram1nctrl/eggtimer/
│   ├── MainActivity.kt                     Compose UI and screen layout
│   ├── TimerEngine.kt                      countdown state, presets, alarm, vibration
│   ├── TimerService.kt                     foreground service, notifications, ticking
│   └── ui/theme/Theme.kt                   colour scheme
└── res/
    ├── drawable/ic_launcher_egg.xml        launcher icon
    └── values/{strings,themes}.xml         text and app theme
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
