<div align="center">

  <img src="icon/logo.png" alt="CashQ Logo" width="128" height="128" />

  # CashQ

  **Fast, Accurate & Privacy-Focused Indian Rupee Cash Counter for Android**

  [![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://www.android.com)
  [![Language](https://img.shields.io/badge/Kotlin-2.3.20-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
  [![Toolkit](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
  [![Architecture](https://img.shields.io/badge/Architecture-Offline--First%20%7C%20UDF-009688?style=for-the-badge)]()
  [![Release](https://img.shields.io/badge/Version-1.0.0-blue?style=for-the-badge)](https://github.com/Dr-SuBZeRo/CashQ/releases)

  <p align="center">
    Designed for retail cashiers, pharmacies, clinics, small businesses, and daily personal cash management. Count notes and coins with real-time calculations, zero cloud dependencies, and zero tracking.
  </p>

  <img src="icon/poster.png" alt="CashQ Poster" width="600" />

</div>

---

## 📌 Overview

**CashQ** is an offline-first native Android application specifically engineered for counting physical Indian Rupee (₹ / INR) banknotes and metallic coins. It provides instant visual subtotals, active grand totals with standard Indian numbering formatting (lakhs & crores), notes-only toggle, combined ₹20/₹10 counting modes, local persistent session history, native sharing, and Material 3 personalization.

- **Developer:** SuBZeRo
- **Application ID:** `com.cashq`
- **Current Version:** `1.0.0` (`versionCode 1`)
- **Min SDK:** Android 8.0 (API 26)
- **Target / Compile SDK:** Android 16 (API 36)

---

## ✨ Implemented Features

### 🧮 1. Precision Currency Counting & Calculation Engine
- **Exact Long Arithmetic in Paise:** Zero floating-point drift. All calculations occur in integer paise (1 Rupee = 100 paise) using exact 64-bit integer mathematics with arithmetic overflow protection.
- **Indian Number Grouping:** Instant formatting according to Indian convention (`₹1,23,456` and `₹1,23,45,678`) rather than Western three-digit grouping.
- **Interactive Denomination Rows:** Large touch targets, immediate subtotal recalculation, and active total cash displays.

### 🪙 2. Master Coin Visibility Toggle
- **Notes-Only Mode:** One-tap toggle switch to hide or show coin denominations.
- **State Preservation:** When coins are toggled off, any previously entered coin quantities are safely retained in session memory and restored when coin visibility is toggled back on.

### 🔄 3. ₹20 & ₹10 Denomination Modes
Because ₹20 and ₹10 circulate simultaneously as banknotes and coins in India, CashQ supports two counting modes:
- **Separate Denominations (Default):** Independent rows for ₹20 Note, ₹20 Coin, ₹10 Note, and ₹10 Coin.
- **Combined Denominations:** Merged single row for ₹20 and single row for ₹10.
- **Lossless Conversion:**
  - *Separate to Combined:* Automatically sums notes and coins into single aggregate rows.
  - *Combined to Separate:* Assigns aggregate counts to notes, resets coins to 0, and prompts with an informative confirmation dialog.

### 💾 4. Offline Session History & Persistence
- **Room Database:** Local SQLite database storing saved counts with timestamps, optional custom titles, denomination quantity maps, and active calculation snapshots.
- **Quick Reload & Review:** Inspect past counting sessions or load them directly back into the live counter.
- **Instant Search & Cleanup:** Search sessions by title and safely delete records with confirmation protection.

### 📤 5. Native Sharesheet Integration
- Exports detailed, plain-text counting breakdown summaries through the native Android Sharesheet (compatible with WhatsApp, SMS, Email, Notes, etc.).
- Clearly indicates whether coins were excluded or combined mode was utilized.

### 🎨 6. Material 3 & Theme Personalization
- Supports **System Default**, **Light Theme**, and **Dark Theme**.
- Supports **Material You Dynamic Colors** (extracting color palettes from the user's wallpaper on Android 12+ / API 31+).

### 🔍 7. Manual GitHub Update Checking
- Direct manual query to the official GitHub repository releases API (`Dr-SuBZeRo/CashQ`).
- Performs semantic version comparison (`1.1.0` vs `1.0.0`) and offers a direct link to new releases.
- **100% User-Initiated:** Never runs in the background; zero battery or background network overhead.

### 🛡️ 8. Privacy & Security Commitment
- **Zero Network Requirement for Core Functions:** All calculations, history records, and preferences remain strictly on the device.
- **No Accounts or Logins:** No phone numbers, email addresses, or registrations required.
- **No Ads or Analytics:** No advertising SDKs, trackers, or third-party telemetry.

---

## 🇮🇳 Canonical Indian Currency Denominations

CashQ models the canonical denominations circulating in the Indian economy:

| Category | Denomination | Short Label | Value in Paise | Face Value (₹) |
|---|---|---|---|---|
| **Banknote** | ₹500 Note | ₹500 | 50,000 | ₹500 |
| **Banknote** | ₹200 Note | ₹200 | 20,000 | ₹200 |
| **Banknote** | ₹100 Note | ₹100 | 10,000 | ₹100 |
| **Banknote** | ₹50 Note | ₹50 | 5,000 | ₹50 |
| **Banknote** | ₹20 Note | ₹20 | 2,000 | ₹20 |
| **Banknote** | ₹10 Note | ₹10 | 1,000 | ₹10 |
| **Coin** | ₹20 Coin | ₹20 | 2,000 | ₹20 |
| **Coin** | ₹10 Coin | ₹10 | 1,000 | ₹10 |
| **Coin** | ₹5 Coin | ₹5 | 500 | ₹5 |
| **Coin** | ₹2 Coin | ₹2 | 200 | ₹2 |
| **Coin** | ₹1 Coin | ₹1 | 100 | ₹1 |
| *Combined* | ₹20 (Note + Coin) | ₹20 | 2,000 | ₹20 |
| *Combined* | ₹10 (Note + Coin) | ₹10 | 1,000 | ₹10 |

---

## 🏗️ Technical Architecture

CashQ follows modern Android development practices adhering to Unidirectional Data Flow (UDF) and Layered Clean Architecture:

```
com.cashq
├── core
│   ├── formatting       # Custom Indian numbering regex & formatting engine
│   ├── sharing          # Plain-text Sharesheet summary generator
│   └── updates          # GitHub release API client & semantic comparator
├── data
│   ├── local
│   │   ├── dao          # Room DAOs with Flow queries
│   │   ├── entity       # SavedSessionEntity with Room TypeConverters
│   │   ├── CashQDatabase# Room database implementation
│   │   └── Preferences  # Jetpack DataStore preferences (interface + implementation)
│   └── repository       # SessionRepository implementation
├── domain
│   ├── calculation      # Pure Kotlin CashCalculationEngine (Long arithmetic)
│   └── model            # Denomination, CurrencyType, CoinCountingMode, CalculationResult
└── ui
    ├── counter          # Main interactive counting screen & CounterViewModel
    ├── history          # Session history list, session cards & HistoryViewModel
    ├── navigation       # Bottom navigation bar & top app bar hosts
    ├── settings         # Preferences screen, theme picker, updates & SettingsViewModel
    └── theme            # Material 3 colors, typography, shapes & adaptive color scheme
```

---

## ⚙️ Requirements & Build Instructions

### Prerequisites
- **JDK:** OpenJDK 17 (recommended: Eclipse Adoptium Temurin 17)
- **Android SDK:** API 36 with Build-Tools 36.0.0
- **Gradle:** 9.1.0 (managed via included `./gradlew`)
- **Android Gradle Plugin (AGP):** 9.0.1
- **Kotlin:** 2.3.20

### 🧪 1. Run Unit Tests
To execute all local JVM unit tests (covering calculation engine, Indian currency formatting, preference synchronization, session entities, and GitHub update comparison):
```powershell
Set-Location G:\CashQ
.\gradlew.bat testDebugUnitTest
```
Test results and HTML reports will be generated in `app/build/reports/tests/testDebugUnitTest/`.

### 🔍 2. Run Android Lint
To verify Android code health, resource integrity, accessibility, and manifest correctness:
```powershell
Set-Location G:\CashQ
.\gradlew.bat lintDebug
```
The report is generated in `app/build/reports/lint-results-debug.html`.

### 📦 3. Build Debug APK
To assemble the standalone debug APK:
```powershell
Set-Location G:\CashQ
.\gradlew.bat assembleDebug
```
The built APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 📲 4. Install onto Device via ADB
Connect your Android device with USB debugging enabled, then run:
```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

---

## 🔒 Privacy Statement

CashQ is built on the principle of complete user privacy:
- All data entered into CashQ remains exclusively on your physical device.
- The app requires zero permissions other than standard network access utilized exclusively when you manually check for updates via the Settings screen.
- No analytics, crash reporters, external logging frameworks, or advertising libraries are included in the binary.

---

## 👤 Developer & Copyright

- **Creator & Developer:** [SuBZeRo](https://github.com/Dr-SuBZeRo)
- **GitHub Repository:** [Dr-SuBZeRo/CashQ](https://github.com/Dr-SuBZeRo/CashQ)

*Note: CashQ is currently unlicensed. All rights reserved by the author. Redistribution, modification, or commercial use without explicit permission is prohibited.*
