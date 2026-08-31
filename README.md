# imanage-android — Privacy-First Native File Explorer

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="I Manage Logo"/>
</p>

<p align="center">
  <b>A modern, ultra-secure, 100% offline Android File Explorer built with Kotlin & Jetpack Compose (Material 3).</b>
</p>

<p align="center">
  <a href="https://github.com/PrabinCode/imanage-android/releases/latest">
    <img src="https://img.shields.io/badge/Download-Latest%20APK-006874?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Language-Kotlin%202.0-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose%20(Material%203)-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" />
  <img src="https://img.shields.io/badge/Internet%20Permission-NONE%20(100%25%20Offline)-red?style=flat-square" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" />
</p>

---

## 📥 Direct APK Download

You can download the ready-to-install APK directly from GitHub:

* **[Download Latest APK from Releases](https://github.com/PrabinCode/imanage-android/releases)**
* Or download the compiled APK file directly from this repository: [`IManage-v1.3.0.apk`](IManage-v1.3.0.apk)

---

## 🌟 Key Features

* **🛡️ 100% Private, Zero AI & Zero Telemetry:** No remote AI training/scraping, zero 3rd-party analytics SDKs, and zero internet permissions.
* **✨ Feature Onboarding Tour:** Interactive first-install feature intro slider with instant permission setup.
* **🔒 Hardware-Backed Safe Vault:** AES-256 GCM encrypted storage via Android KeyStore.
* **🔥 DoD 5220.22-M File Shredder:** Multi-pass cryptographic data overwriting.
* **🌐 Offline Wi-Fi PC Transfer:** Bidirectional file transfer with real-time progress bar.
* **🗜️ Advanced Archive Suite:** Password-protected encrypted ZIP creation and multi-archive extraction (`.zip`, `.tar.gz`, `.tgz`).
* **🧹 Smart Storage Duplicate Cleaner:** Two-stage SHA-256 hash comparison to reclaim storage.
* **🎵 Built-in Offline Media Players:** Persistent background Audio Player bar and native Video Player.
* **🏷️ Custom Color Tags:** Tag files with color badges (🔴 *Important*, 🟢 *Personal*, 🔵 *Work*, 🟠 *Finance*).
* **📄 Native Offline PDF & Image Viewers:** Zoomable sandboxed viewers.
* **🔐 Master PIN & Auto-Lock:** PBKDF2 (10,000 rounds) gate with configurable inactivity auto-lock. hardware-encrypted vault (AES-256 via Android KeyStore) protected by Biometrics/PIN.
* 🌪️ **DoD Secure File Shredder:** Multi-pass overwrite algorithm (`0x00`, `0xFF`, and `SecureRandom` bytes) before unlinking files to guarantee permanent unrecoverability.
* 🚫 **Anti-Snooping (`FLAG_SECURE`):** Blocks screenshots, screen recording, and obscures app previews in recent tasks.
* 📊 **Storage Analyzer:** Visual disk breakdown, large files finder (>100MB), empty folder cleaner, and duplicate file detector.
* 🗄️ **Smart Recycle Bin:** Soft-delete with 1-tap instant restoration or permanent shredding.
* 📝 **In-App Text / Code Editor:** View and edit `.txt`, `.json`, `.xml`, `.md`, `.kt`, `.py`, `.sh`, `.log`, and config files directly.
* 🧭 **Full Storage & System Root Browsing:** Explore internal storage, SD cards, and the system root partition (`/`) with dotfiles/hidden files support.

---

## 🏗️ System Architecture

```
com.imanage.fileexplorer/
├── data/
│   ├── crypto/            # Android KeyStore + AES-256 streaming engine + DoD shredder
│   ├── local/             # Room SQLite Database (Bookmarks, Trash, Vault index)
│   ├── model/             # FileItem, FileType, StorageVolumeInfo, SortOption
│   └── repository/        # FileSystemRepository, VaultRepository, StorageAnalyzerRepository, TrashRepository
└── ui/
    ├── components/        # BreadcrumbBar, FileListItem, FileGridItem, StorageIndicator, CategoryGrid
    ├── navigation/        # Compose Navigation Graph & Destinations
    ├── screens/           # Home, Explorer, Vault, Analyzer, Trash, Search, Settings, TextEditor
    └── theme/             # Material 3 Dynamic Colors & OLED Dark Theme
```

---

## 🚀 Building from Source

```bash
# Clone the repository
git clone https://github.com/PrabinCode/imanage-android.git
cd imanage-android

# Build Debug APK
./gradlew assembleDebug
```
The APK output is generated at: `app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License
This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.
