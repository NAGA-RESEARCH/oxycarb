# 🎵 Oxycarb - Universal Android Music Application

<p align="center">
  <b>Modern, Feature-Rich, Open-Source Android Music Streaming & Player App built with Jetpack Compose, Media3 ExoPlayer, and NewPipeExtractor.</b>
</p>

<p align="center">
  <a href="https://github.com/NAGA-RESEARCH/oxycarb/releases/tag/v1.2.0">
    <img src="https://img.shields.io/badge/Release-v1.2.0-E83D7A?style=for-the-badge&logo=android" alt="Version 1.2.0" />
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-2.2.10-purple?style=for-the-badge&logo=kotlin" alt="Kotlin" />
  </a>
  <a href="https://developer.android.com/jetpack/compose">
    <img src="https://img.shields.io/badge/Jetpack_Compose-120Hz_Smooth-4285F4?style=for-the-badge&logo=jetpackcompose" alt="Jetpack Compose" />
  </a>
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-green?style=for-the-badge" alt="License" />
  </a>
</p>

---

## 🚀 Download Latest APK

📥 **[Download Oxycarb v1.2.0 APK](https://github.com/NAGA-RESEARCH/oxycarb/releases/download/v1.2.0/Oxycarb-v1.2.0-debug.apk)**

---

## ✨ Features

- 🎧 **Smart Auto-Play Radio Engine & Genre Radio Chain**: Continuously queues and auto-plays related songs or genre tracks.
- 🎤 **Real-Time Synced Lyrics (LRCLIB API)**: Auto-scrolling, beat-synced LRC lyrics with tap-to-seek timestamp controls.
- ⬇️ **Offline Downloads & Priority Mode**: Download audio tracks (`.m4a`) for instant 0ms offline playback with live progress rings.
- 🛡️ **Uninstall-Safe Data Vault**: Opt-in Scoped Storage MediaStore backup in `Documents/Oxycarb` ensuring favorites & custom playlists survive app uninstalls and reinstalls.
- 📉 **YouTube-Style Swipe-Down Player Collapse**: Swipe down from anywhere in the top 70% of the player to smoothly collapse into the Mini-Player (with bottom bar auto-hiding).
- 📶 **Network Monitor & Offline Priority**: Real-time connectivity monitoring with automatic offline mode prioritization.
- 🔍 **Voice Search & Hum to Search**: Built-in speech recognition for voice and melody searches.
- 🗄️ **Visual Library 2x2 Vector Cards Grid**: Sleek interactive cards for History, Favorites, Playlists, and Downloads with live item counts.
- 🎨 **Dynamic App Themes System (5 Themes)**: Instant switching between:
  - 🌙 **Deep Dark**
  - ☀️ **Clean Light**
  - 🌌 **Midnight Blue**
  - ⚡ **Cyberpunk Gold**
  - 🌴 **GTA VI (Vice Sunset)**
- 🎚️ **Audio Equalizer Presets & Sleep Timer**: Choose equalizer presets and graphical sleep timer up to 120 minutes.
- ⚡ **120Hz Ultra-Smooth UI**: Zero frame-drop list rendering with Compose keys and OkHttp HTTP/2 connection pooling.

---

## 📸 Tech Stack & Libraries

- **UI Framework**: Jetpack Compose, Material 3
- **Audio Engine**: AndroidX Media3 ExoPlayer & MediaSession
- **Extraction Engine**: NewPipeExtractor
- **Database**: Room Database
- **Network & Cache**: OkHttp 4.12.0 with 100MB Disk Cache
- **Language**: Kotlin 2.2.10
- **Min SDK**: API 26 (Android 8.0) | **Target SDK**: API 37 (Android 15)

---

## 🛠️ How to Build & Run

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/NAGA-RESEARCH/oxycarb.git
   cd oxycarb
   ```

2. **Open in Android Studio**:
   Open Android Studio Ladybug / Meerkat or later and let Gradle sync.

3. **Build APK**:
   ```bash
   ./gradlew assembleDebug
   ```

---

## 👨‍💻 Author & Maintainer

Crafted with ❤️ by **Aman (AK Bisariya)**

- 🔴 **YouTube**: [@AK_Bisariya](https://youtube.com/@AK_Bisariya)
- 🐦 **X (Twitter)**: [@AK_Bisariya](https://x.com/AK_Bisariya)
- 💻 **GitHub**: [NAGA-RESEARCH](https://github.com/NAGA-RESEARCH)

---

## 📜 License

This project is open-source and licensed under the [MIT License](LICENSE). Free for developers to customize and distribute!
