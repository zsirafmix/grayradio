# ZsirafGrayRadio

Grayscale Android internet radio player — large tile grid, Room persistence, Media3 ExoPlayer, and Radio Browser search.

**App name:** ZsirafGrayRadio  
**Package:** `com.grayradio.app`  
**Version:** 1.2.0  
**Repo:** https://github.com/zsirafmix/grayradio

**Brand logo:** Custom grayscale giraffe/radio mark used as the launcher icon (adaptive + density mipmaps) and as a small circular brand mark in the TopAppBar.

## Features

- 2-column large grayscale station tiles
- Tabs: **Mentett** | **Kedvencek** | **Böngésző**
- Országválasztó + stílus/tag szűrő (Radio Browser)
- Kedvencek (csillag a csempén / hosszú nyomás); Room `isFavorite` + migráció v1→v2
- Play / pause with bottom now-playing bar
- Add stations manually (name + stream URL)
- Search / add from [Radio Browser API](https://api.radio-browser.info/) (`https://de1.api.radio-browser.info`, User-Agent `ZsirafGrayRadio/1.1`)
- Edit / delete via long-press or tile menu
- Stations persisted with Room; seeded on first launch
- Strict black / white / gray Material 3 theme (logos desaturated)

## Seed stations

| Station | Stream |
|---------|--------|
| Retro Rádió | `https://icast.connectmedia.hu/5001/live.mp3` |
| ROCK FM | `https://icast.connectmedia.hu/5301/live.mp3/` |
| BDPST ROCK (320) | `http://s2.audiostream.hu/bdpstrock_320k` |
| Petőfi Rádió | `https://icast.connectmedia.hu/4738/mr2.mp3` |
| Klubrádió | `https://a7.asurahosting.com:8160/radio.mp3` |

## Tech stack

- Kotlin, Jetpack Compose, Material 3
- Media3 ExoPlayer
- Room + Coroutines / Flow + ViewModel
- OkHttp + Coil (grayscale logos)
- minSdk 26 · targetSdk / compileSdk 35 · Gradle Kotlin DSL

## Build instructions

### Requirements

- Android Studio Ladybug (2024.2+) or newer, **or** JDK 17 + Android SDK 35
- Accept Android SDK licenses

### Open in Android Studio

1. `File → Open` → select this repository root
2. Let Gradle sync
3. Run the `app` configuration on a device / emulator (API 26+)

### Command line

```bash
# Optional: point to your SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

chmod +x gradlew
./gradlew :app:assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`

```bash
./gradlew :app:installDebug
```

## Project layout

```
app/src/main/java/com/grayradio/app/
  data/          Room entity/DAO/DB, Radio Browser API, repository
  player/        ExoPlayer wrapper
  ui/            Compose screens, theme, ViewModel
  GrayRadioApp.kt
  MainActivity.kt
```

## License

MIT — personal / open use.
