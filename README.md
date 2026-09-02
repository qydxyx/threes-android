# Threes! for Android 🎮

A high-fidelity native Android recreation of the iconic indie puzzle masterpiece **Threes!** (originally by Asher Vollmer, Greg Wohlwend, and Jimmyinson), built with modern **Kotlin** and **Jetpack Compose**.

---

## ✨ Features

- **🎯 1:1 Authentic Drag Physics**:
  - Real-time 1:1 finger tracking — tiles only slide by at most 1 grid space.
  - Per-row/per-column obstacle blocking (blocked lines stay still while movable lines slide).
  - Dynamic gaze tracking (character eyes turn and watch in the direction of your swipe).
  - Incoming card edge peeking preview.
  - 42% threshold release detection with smooth spring snap-back physics.
- **🃏 Authentic Deck & Bonus Spawn Algorithm**:
  - 12-card starter deck (`4x 1, 4x 2, 4x 3`) grab-bag system.
  - Bonus high-card spawning (`+` hint) when maximum tile on board reaches $\ge 48$.
  - Exact exponential scoring formula: $3^{1 + \log_2(N/3)}$.
- **🎵 Procedural Audio & Haptics Engine**:
  - Real-time synthesized pentatonic chime scales (C5 ~ E7 harmonic bell tones) scaling with merge value via `AudioTrack`.
  - Card sliding whoosh, tactile click snaps, game over melancholic jingles, and victory fanfares.
  - Integrated multi-stage haptic vibration feedback.
- **📖 The Cast (14 Character Gallery & Vector Faces)**:
  - Custom Compose `Canvas` vector facial expressions and idling eye-blinking for all 14 numbers (1 Uno, 2 Dos, 3 Trey, 6 Todd, 12 Terry, 24 Torval, 48 Twee, 96 Thelma, 192 Thurston, 384 Torrance, 768 Tycho, 1536 Terrence, 3072 Theron, 6144 Trinity).
  - Interactive "The Cast" book with character bios, quotes, and tap-to-voice animations.
- **🌙 Night Threes (Dark Mode)**:
  - Full night theme support matching the official "Night Threes" aesthetic.
- **🎨 Skeuomorphic Adaptive App Icon**:
  - Layered tactile 3D physical cardstock aesthetic with Android Adaptive Icon support (`mipmap-anydpi-v26`).
- **💾 State Persistence & Undo**:
  - Instant state auto-save and seamless resume.
  - Optional Undo move toggle.

---

## 🛠️ Tech Stack

- **Language**: Kotlin 1.9.24 / 2.0
- **UI Framework**: Jetpack Compose + Material 3
- **Graphics**: Compose Canvas Vector Renderers
- **Audio Engine**: Android `AudioTrack` Real-time PCM Synthesis
- **Architecture**: Clean MVVM + Coroutines StateFlow
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 15 (API 35)

---

## 🚀 Building & Running

### Prerequisites
- JDK 21
- Android SDK (API 35)

### Build Debug APK
```bash
./gradlew assembleDebug
```
The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew testDebugUnitTest
```

---

## 📜 License
MIT License. Created for educational and non-commercial portfolio purposes. Threes! is an original game created by Sirvo LLC.
