# PARMAR AI — Personal Gujarati & English Voice Assistant

**PARMAR AI** is a native Kotlin + Jetpack Compose Android voice assistant built for modern Android smartphones (targeted and optimized for **OPPO A3 5G**, Android 14/15+).

## Key Capabilities
- **Wake & Stop Word (`DIAMOND`)**: Offline-preferred on-device `SpeechRecognizer` standby with foreground microphone service notification and Stop Listening action.
- **PARMAR Personality**: Addresses you as **Sir**, responds in natural **Gujarati** by default (or **English** when requested), and speaks using a **Female TTS** voice with configurable speed and pitch.
- **Gemini AI Brain**: Powered by **Firebase AI Logic (`gemini-3.8-flash`)** with **Firebase App Check** attestation and structured JSON action routing.
- **Offline Mode**: Deterministic local command engine for time/date, flashlight, opening apps, volume, Wi-Fi/Bluetooth settings, local Notes, and local Reminders—with honest offline status reporting.
- **Phone Controls**: Flashlight (`CameraManager`), Launcher App opening (`PackageManager` `<queries>`), System Clock Alarms, Local Scheduled Reminders (`AlarmManager`), Volume (`AudioManager`), Wi-Fi & Bluetooth Settings shortcuts, and explicit **Device Admin** screen lock.
- **Privacy & Security**: Local Room SQLite storage, optional Device PIN/Biometric lock for Private Memory entries, and Storage Access Framework (SAF) JSON Backup & Restore.

## Build & Download APK (No Errors)
1. **In Google AI Studio Build**:
   - Use the **Settings / Export** menu to **Build / Download APK** or **Export ZIP / Push to GitHub**.
2. **Local Android Studio / Gradle Build**:
   - Open the project in Android Studio (JDK 17+, Android SDK 36).
   - Build Debug APK:
     ```bash
     gradle :app:assembleDebug
     ```
     Output: `app/build/outputs/apk/debug/app-debug.apk`
   - Build Signed Release APK (set `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` env vars):
     ```bash
     gradle :app:assembleRelease
     ```

## OPPO A3 5G Installation & Permission Setup
1. Enable **Install from Unknown Sources** for your File Manager / Browser if sideloading the APK.
2. Open **PARMAR AI** → **Permissions & Privacy**:
   - Grant **Microphone** (`RECORD_AUDIO`) for voice & `DIAMOND` wake word.
   - Grant **Notifications** (`POST_NOTIFICATIONS`) for foreground mic indicator and reminders.
   - Enable **Battery → Unrestricted** (ColorOS Battery Optimization) for reliable background wake listening.
   - *(Optional)* Enable **Device Admin** only if you want voice/button screen locking.
