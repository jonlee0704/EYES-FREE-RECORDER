# Privacy Policy
**Application**: EYES-FREE VOICE RECORDER  
**Package Identifier**: `com.jonlee.android.SJplayer`  
**Effective Date**: October 1, 2026  
**Developer**: Jon Lee ("Developer", "We", "Us", or "Our")  
**Contact**: `jonlee0704@gmail.com`

---

## 1. Introduction
We recognize that your privacy is of the utmost importance, especially when using an application designed to record audio, voice memos, and spoken notes. This Privacy Policy explains our commitment to transparency, data protection, and zero-surveillance operation when you use **EYES-FREE VOICE RECORDER** (the "App").

**THE CORE PRINCIPLE OF EYES-FREE VOICE RECORDER**:  
**EYES-FREE VOICE RECORDER is a local-first, privacy-respecting application. We do NOT collect, harvest, transmit, sell, or analyze your audio recordings, voice data, personal metadata, or device identifiers.**

---

## 2. Information We Do NOT Collect
Unlike many cloud-connected voice recorders, EYES-FREE VOICE RECORDER operates almost entirely offline and on-device:
- **No Personal Identification Information (PII)**: We do not ask for or collect your name, email address, phone number, physical address, or account credentials.
- **No Audio Uploads**: Your voice recordings, imported songs, and audiobooks remain exclusively on your device's internal storage or SD card. We do not operate cloud servers to upload or store your audio.
- **No Audio Biometrics or Voice Printing**: We do not perform biometric voice analysis, voice recognition profiling, or user identification.
- **No Advertising or Analytics Tracking**: The App contains **no third-party advertising SDKs**, no Google AdMob, no Facebook SDK, and no commercial tracking beacons.
- **No Location Tracking**: The App does not track, record, or share your GPS location coordinates.

---

## 3. Permissions Requested & Why They Are Needed
To provide its eyes-free audio recording and playback functions, EYES-FREE VOICE RECORDER requests certain sensitive Android permissions. Each permission is used strictly for its local operational purpose:

| Android Permission | Technical Name | Operational Purpose in EYES-FREE VOICE RECORDER |
| :--- | :--- | :--- |
| **Microphone** | `android.permission.RECORD_AUDIO` | **Mandatory**: Allows you to record voice memos, lectures, and interviews when you double-tap the screen. Audio is saved directly into local `.wav` or `.aac` files on your device. |
| **Read Media Audio / Storage** | `android.permission.READ_MEDIA_AUDIO`<br>`android.permission.READ_EXTERNAL_STORAGE`<br>`android.permission.WRITE_EXTERNAL_STORAGE` | **Mandatory**: Enables the App to browse, play, index, and organize audio files stored on your phone or SD card, and to save newly created recordings. |
| **Notifications** | `android.permission.POST_NOTIFICATIONS` | **Foreground Service**: Displays an ongoing media control notification so that playback and recording continue reliably when the screen is dark or another app is open. |
| **Vibration** | `android.permission.VIBRATE` | **Tactile Feedback**: Generates distinct haptic pulses when you tap, double-tap, or swipe, allowing blind and visually impaired users to confirm actions by touch. |
| **Modify Audio Settings** | `android.permission.MODIFY_AUDIO_SETTINGS` | **Volume & Playback Routing**: Adjusts playback volume, routes audio to connected headphones, and prevents feedback during recording. |
| **Wake Lock** | `android.permission.WAKE_LOCK` | **Recording Continuity**: Prevents the phone's CPU from entering deep sleep during long recording or playback sessions, avoiding interrupted files. |
| **Wi-Fi / Nearby Devices** *(Optional)* | `android.permission.ACCESS_WIFI_STATE`<br>`android.permission.CHANGE_WIFI_STATE` | **Peer-to-Peer Backup**: Only utilized if you manually initiate the local Wi-Fi direct file transfer tool to backup recordings directly between your own devices. |

---

## 4. Text-To-Speech (TTS) Processing
EYES-FREE VOICE RECORDER utilizes the **Android System Text-to-Speech Engine** installed on your device (such as Google Speech Services or Samsung TTS) to announce file names, folders, time, and battery level.
- Spoken text strings are processed locally by the on-device TTS engine configured in your Android System Settings.
- We do not transmit your file names or text queries to any proprietary external servers.

---

## 5. Third-Party Services & Links
The App operates independently and does not integrate third-party ad networks or marketing analytics services. If the App contains links to external documentation or source code (such as GitHub), your interaction with those external sites is governed by their respective privacy policies.

---

## 6. Children’s Privacy (COPPA & GDPR-K Compliance)
EYES-FREE VOICE RECORDER does not knowingly collect, store, or solicit personal information from children under the age of 13 (or under 16 in the European Union). Because no personal information is ever collected from any user, EYES-FREE VOICE RECORDER is compliant with the Children's Online Privacy Protection Act (COPPA) and international child protection regulations.

---

## 7. Data Retention & Deletion
- All recordings, bookmarks, and preferences are stored exclusively on your device.
- You have complete control over data retention:
  - You can delete any recording at any time using the 4-finger swipe right gesture or any standard Android file manager.
  - Uninstalling the App or clearing app data will remove app configuration files and caches. Audio files in your shared media directories remain intact on your device unless manually deleted.

---

## 8. Changes to This Privacy Policy
We may periodically update this Privacy Policy to reflect app updates or regulatory changes. Any updates will be posted to the project repository with a revised "Last Updated" date. We encourage users to review this page periodically.

---

## 9. Contact Us
If you have questions, feedback, or inquiries regarding this Privacy Policy or your data privacy while using EYES-FREE VOICE RECORDER, please contact:
- **Developer**: Jon Lee
- **Email**: `jonlee0704@gmail.com`
- **GitHub**: [https://github.com/jonlee0704/EYES-FREE-RECORDER](https://github.com/jonlee0704/EYES-FREE-RECORDER)
