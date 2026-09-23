# YaNaviPeb

**English** | [Русский](README.ru.md)

Turn-by-turn navigation from Yandex Maps on a Pebble Time 2 watch.

While Yandex Maps is guiding you along a route, the watch shows the maneuver arrow, the distance to
it, the maneuver text ("Поверните направо", "Turn right"), the arrival time, the remaining distance
and the remaining time. The watchapp opens by itself when navigation starts and closes after it ends.

```
        [ 64×64 arrow ]
             150 м
       Поверните направо
  ─────────────────────────
  23:57    1,61 км    15 мин
```

The project is not affiliated with Yandex or Core Devices and does not use their APIs: the data comes
from the navigation notification that Yandex Maps shows on the phone. Text is shown exactly as
Yandex Maps provides it, so it is in Russian.

## How it works

The project has two parts:

- **Android app** (`android/`) reads the Yandex Maps notification with a `NotificationListenerService`,
  extracts the distance, maneuver, arrival time, remaining distance and time, and the maneuver icon,
  and sends only the changed fields to the watch, at most once per second. The fields are read
  from the notification's custom layout: the app inflates its `RemoteViews` and finds the fields by
  their view resource names (`titleView`, `remainingDistanceView`, `timeOfArrivalView`,
  `primaryIconTinted` and so on). If the distance or the maneuver view is missing, the notification's
  standard title and text are used instead.
- **Watchapp** (`watchapp/`, C, Pebble SDK) receives the data over AppMessage and draws the screen.

Communication goes through the official Pebble phone app (`coredevices.coreapp`) and the
PebbleKit 2 library (`io.rebble.pebblekit2`). The arrow is not matched against a table: the icon
itself is taken from the notification and sent to the watch (64×64, 1 bit), so every maneuver that
Yandex Maps shows is supported.

## Requirements

- Pebble Time 2 (`emery` platform). Other Pebble models are not supported.
- Android 11 or newer with the Pebble app (`coredevices.coreapp`).
- Yandex Maps (`ru.yandex.yandexmaps`). Yandex Navigator does not work: its notification has no
  maneuver data.
- A language pack with Cyrillic on the watch, e.g. "Кириллица (для уведомлений)" from the Pebble app.

## Installation

There are no prebuilt releases yet; both apps are built from source.

### Android app

Requires the Android SDK and JDK 21.

```bash
cd android
./gradlew :app:installDebug
```

`installDebug` installs the app on a phone connected over adb. Alternatively, build the APK with
`./gradlew :app:assembleDebug` and install `app/build/outputs/apk/debug/app-debug.apk` manually.

### Watchapp

Requires Pebble SDK 4.33.1 (the `pebble` tool) and Python 3.

```bash
cd watchapp
pebble build
tools/patch_appinfo.py
```

`tools/patch_appinfo.py` is required after every build. It adds the `companionApp` block to
`build/watchapp.pbw`, which SDK 4.33.1 silently drops from `package.json`. Without that block the
Pebble app does not allow the Android app to send data to the watch.

Install `build/watchapp.pbw` on the watch: open the file on the phone with the Pebble app, or run
`pebble install --phone <phone IP>` with Developer Connection enabled.

### Phone setup

1. Open YaNaviPeb and tap "Open notification access settings". Grant access.
2. Disable battery optimization for YaNaviPeb and for the Pebble app, otherwise the system may stop
   them in the background.
3. Test the connection without driving: "Start watch demo" plays a recorded route
   on the watch. Do not run the demo during real navigation.

After that, just start a route in Yandex Maps.

## Limitations

- Street names and traffic lights are not shown: the Yandex Maps notification does not contain them.
- The Yandex Maps notification format is undocumented and may change with a Maps update. If the watch
  stops receiving data, check this first.
- Only one phone–watch pair and only the `emery` platform are supported.

## Development

```
android/                  Android app (Kotlin, View Binding)
  app/src/main/.../nav/     Yandex Maps notification → NavState
  app/src/main/.../watch/   watch protocol and sync
  app/src/main/.../demo/    recorded route for the demo mode
watchapp/                 watchapp (C, Pebble SDK)
  tools/                    appinfo patch, sample messages for the emulator
```

Android unit tests:

```bash
cd android
./gradlew :app:testDebugUnitTest
```

The watchapp can be tried in the emulator with a sample maneuver
(`right`, `left`, `straight` or `idle`; requires Python 3 with Pillow):

```bash
cd watchapp
pebble install --emulator emery
tools/send_sample.sh right
```

The emulator has no language pack, so Cyrillic text shows up as boxes there.

### Protocol

AppMessage keys are declared in `watchapp/package.json` (`pebble.messageKeys`) and numbered from 10000
in array order. The same numbers live in `Protocol.kt` on the Kotlin side; `ProtocolTest` checks that
they match.

| Key | ID | Type | Max bytes | Example |
|---|---|---|---|---|
| `STATE` | 10000 | uint8 | 1 | `0` — no navigation, `1` — navigating |
| `DISTANCE` | 10001 | string | 15 | `150 м` |
| `MANEUVER` | 10002 | string | 48 | `Поверните направо` |
| `REMAINING` | 10003 | string | 15 | `1,61 км` |
| `ETA` | 10004 | string | 7 | `23:57` |
| `DURATION` | 10005 | string | 19 | `1 ч 17 мин` |
| `ICON` | 10006 | bytes | 512 | 64×64, 1 bit per pixel |

String limits are UTF-8 bytes without the terminating NUL; the phone truncates strings on a character
boundary. Icon layout: rows of 8 bytes, most significant bit first, 1 is a white pixel.

## License

[0BSD](LICENSE): use, modify and distribute the code however you like, no attribution required.
