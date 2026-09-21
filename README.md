<div align="center">
  <h1>🎬 VidoPRO</h1>
  <p><b>A Professional, Open-Source Android Video Editor</b></p>
  <p>Inspired by CapCut & Alight Motion, built entirely with Jetpack Compose & AndroidX Media3.</p>
  
  [![Build and Release APK](https://github.com/Vikash922/VidoPRO/actions/workflows/build-apk.yml/badge.svg)](https://github.com/Vikash922/VidoPRO/actions/workflows/build-apk.yml)
  [![Kotlin](https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
  [![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
</div>

---

## 🌟 Key Features

* **Advanced Timeline Engine:** Multi-track editing (Video, Audio, Text) with seamless gapless ripple logic.
* **Live Video Preview:** Powered by `androidx.media3.exoplayer` for smooth, accurate frame-by-frame seeking.
* **Creative Tools:** Split, Speed Control, Text Overlays, Volume mixing, and live Color Filters (Brightness, Contrast, Saturation).
* **Robust Undo/Redo:** Command-based history stack with coalescing for rapid gestures.
* **Auto-Saving:** Reliable Room Database integration ensures you never lose project progress.
* **Studio-Grade Export:** Render directly to MP4 using `androidx.media3.transformer` (up to 1080p, 60fps).
* **OTA Auto-Updater:** Built-in seamless AppUpdater that fetches updates via GitHub Releases!

## 🛠️ Tech Stack

* **UI:** 100% Jetpack Compose (Material 3) with buttery-smooth `AnimatedVisibility` and `Crossfade` transitions.
* **Media Processing:** AndroidX Media3 (`ExoPlayer`, `Transformer`, `Effect`).
* **Architecture:** Clean Architecture (MVI pattern) with robust ViewModel state management.
* **Local Storage:** Room Database (Coroutines/Flow).
* **CI/CD:** Automated GitHub Actions pipeline for Keystore signing and APK releases.

## 🚀 Getting Started

### Prerequisites
* Android Studio (Latest Stable)
* JDK 17+
* Android device or emulator (API 24+)

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/Vikash922/VidoPRO.git
   ```
2. Open the project in Android Studio.
3. (Optional) Configure Firebase by adding your `google-services.json` in `app/`.
4. Click **Run** to install the debug build on your device.

## 📦 Automated Release & Updates
VidoPRO uses a fully automated CI/CD pipeline!
* Push to `main` to trigger the **GitHub Action**.
* It automatically builds `assembleRelease`, signs it, and attaches `app-release.apk` to a GitHub Release.
* The built-in `AppUpdater.kt` checks `update.json` on app launch and prompts users to seamlessly download the latest version via Android's `DownloadManager`.

## 📄 License
This project is open-source and intended for educational and professional development purposes. UI assets and styling do not infringe on proprietary software.

