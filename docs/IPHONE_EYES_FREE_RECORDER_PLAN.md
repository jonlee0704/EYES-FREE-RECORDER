# EYES-FREE VOICE RECORDER for iPhone (iOS)
## Master Product Strategy, Architecture & Engineering Plan

*A purpose-built, eyes-free audio recording and playback application engineered specifically for the blind and visually impaired community on Apple iOS.*

---

## 1. Executive Vision & Problem Statement

### 1.1 The Problem on iPhone Today
Apple’s iOS platform is universally praised in the blindness community for **VoiceOver**, but native audio recording on iPhone remains frustratingly visual-first:
1. **Button Hunting**: Native Apple Voice Memos relies on tiny touch targets (a small record circle, microscopic scrubber bars, and nested menus). Blind users must swipe sequentially through dozens of items or drag a finger across the glass to find controls.
2. **The "Magic Tap" Collision**: The 2-finger double-tap shortcut on iOS frequently misfires, resuming paused Apple Music, podcasts, or phone calls instead of triggering recording at critical moments.
3. **Complex Folder Management**: Navigating nested folders requires dismissing modal sheets, tapping back buttons, and deciphering deep hierarchies.
4. **Lack of In-App Screen Curtain**: iPhone's Screen Curtain is locked behind system-wide VoiceOver settings, leaving low-vision or non-VoiceOver users without display privacy or OLED battery savings.

### 1.2 The EYES-FREE Solution on iOS
**EYES-FREE VOICE RECORDER for iPhone** transforms the entire physical glass surface into one unified, tactile instrument:
- **Zero Button Hunting**: 100% of the screen responds anywhere.
- **Dual-Pane Spatial Rotary Engine**: Folders on the left, files on the right. Instant spatial orientation.
- **Multi-Sensory Feedback**: Combines Apple’s class-leading **Taptic Engine (CoreHaptics)**, instant spoken text-to-speech (**AVSpeechSynthesizer**), and zero-latency synthesized earcons.
- **Hardware Superpowers**: Action Button pocket trigger, Dynamic Island live telemetry, AirPods remote control, and on-device offline voice-transcription auto-naming.

---

## 2. Core Architectural Philosophy: The Dual-Layer Accessibility Engine

To ensure maximum versatility and zero conflict with Apple guidelines:

```
+--------------------------------------------------------------------------+
|                       EYES-FREE iOS ARCHITECTURE                         |
+--------------------------------------------------------------------------+
|  LAYER 1: AUTONOMOUS EYES-FREE CANVAS (Default / Active Mode)             |
|  • Custom full-surface touch responder (direct centroid tracking)         |
|  • Self-voicing speech pipeline (AVSpeechSynthesizer, interruptible)      |
|  • Real-time binaural earcons (AVAudioEngine)                             |
|  • Works with screen blanked, in pockets, or in total darkness            |
+--------------------------------------------------------------------------+
|  LAYER 2: ACCESSIBILITY PARITY & VOICEOVER HARMONY                        |
|  • Implements UIAccessibilityContainer & UIAccessibilityCustomAction      |
|  • Magic Tap (2-finger double tap) cleanly mapped to Record Start/Stop    |
|  • VoiceOver Escape (2-finger scrub) mapped to Cancel/Undo                |
|  • Accessible Rotor actions for Section Seeking                           |
+--------------------------------------------------------------------------+
|  LAYER 3: HARDWARE & OS INTEGRATIONS                                      |
|  • Action Button (App Intent instant trigger from locked pocket)          |
|  • Dynamic Island & Live Activities (Audio metering & recording status)   |
|  • CoreHaptics (Subtle transient ticks, boundaries, and confirmation)    |
|  • CloudKit / iCloud Drive (Private, zero-setup multi-device sync)        |
+--------------------------------------------------------------------------+
```

---

## 3. Master Gesture & Interaction System

We adopt universal accessibility standards so iPhone switchers experience **zero friction**, while maintaining full-surface speed:

| Gesture | Action | Audio Cue (Earcon) | Haptic Feedback (Taptic Engine) | Spoken Announcement |
| :--- | :--- | :--- | :--- | :--- |
| **1x Tap** | **Play / Pause** | Sine chime (440Hz ➔ 880Hz) | Crisp light transient | Speaks filename / playback state |
| **2x Tap** *(1 finger)* **OR**<br>**2-Finger Double Tap** *(Magic Tap)* | **Record / Stop** | Rising 2-tone alert / Falling release tone | Heavy double pulse | *"Recording started"* / *"Recording saved"* |
| **3x Tap** | **Add Bookmark** | High resonant ping (1760Hz) | Distinctive sharp click | *"Bookmark added at [time]"* |
| **Swipe Left / Right** | **Next / Prev Track** | Lateral stereo blip | Rotary wheel click | Speaks track title & index |
| **Swipe Up / Down** | **Next / Prev Folder** | Deep resonant tick | Firm wheel notch | Speaks folder name & count |
| **2-Finger Swipe Left / Right** | **Alphabet File Seek** | Ascending high blip | Double tick | Speaks active letter (e.g. *"[A]"*) |
| **2-Finger Swipe Up / Down** | **Month Folder Seek** | Low-frequency whoosh | Heavy notch | Speaks active month (e.g. *"[October]"*) |
| **2-Finger Swipe L/R (Playback)** | **Bookmark Jump** | Chapter chime | Spring transient | Jumps directly between bookmarks |
| **3-Finger Triple Tap** | **Screen Curtain** | Soft visual power-down sweep | Continuous soft buzz | *"Screen curtain on / off"* |
| **4-Finger Swipe Right** | **Delete Current File** | Warning buzz | Triple sharp warning pulse | *"File deleted. 2-finger scrub to undo."* |
| **1-Finger Long Press** | **Audio Details Readout** | Radar aura tone | Subtle expanding pulse | Speaks duration, format, and path |
| **2-Finger Long Press** | **Time & Battery Readout** | Status ping | Quick double tap | Speaks time, date, and battery % |
| **2-Finger Scrub (Z-Gesture)** | **Undo / Cancel** | Reverse harmonic tone | Double soft tick | *"Undo complete"* |

---

## 4. Hardware Integrations (Beating Apple Voice Memos)

### 4.1 Action Button Instant Pocket Record (iPhone 15 Pro, iPhone 16)
- **The Workflow**: User presses the physical Action Button on the side of their iPhone while it remains in their jacket or trouser pocket.
- **The Execution**: Uses iOS **App Intents** framework. Immediately triggers background audio recording, pulses the Taptic Engine twice, and sounds a spoken earcon through connected AirPods or the phone speaker. No screen unlock or viewing required.

### 4.2 Dynamic Island & Live Activities
- When recording or playing in the background:
  - Dynamic Island expands to show real-time decibel audio waveforms.
  - Tapping or long-pressing opens tactile controls.
  - Blind users using VoiceOver hear instant spoken elapsed time: *"Recording, 4 minutes 12 seconds, audio levels optimal"*.

### 4.3 EarPods / AirPods Remote Control
- Double-squeeze on AirPods Pro stem or double-click on wired EarPods inline remote triggers Start / Stop recording with immediate earcon confirmation.

---

## 5. Intelligent Audio Superpowers

### 5.1 On-Device Speech-to-Text Auto Naming (`SFSpeechRecognizer`)
- **Blind User Problem**: Files named `New Recording 153.m4a` require tedious playback to identify content.
- **The Solution**: 
  - Using Apple’s neural on-device speech engine (`SFSpeechRecognizer`, 100% offline, zero internet required), the app transcribes the first 10–15 seconds of speech.
  - Generates intelligent, human-readable file titles automatically:
    - *Example*: User says: *"Notes on chemistry lecture chapter 4 regarding covalent bonds..."*
    - *Auto-title*: `Chemistry Lecture - Covalent Bonds (Oct 2).m4a`

### 5.2 Dynamic Noise Suppression & Voice Isolation
- Utilizes `AVAudioSession` Voice Processing modes (`.voiceChat` and `.measurement`) to filter ambient street noise, air conditioning rumble, and room reverberation.

### 5.3 Smart Playback: Skip Silence & Pitch-Corrected Speed
- **Skip Silence**: Detects pauses and dead air in meetings or lectures, speeding through silence seamlessly.
- **High-Speed Playback (1.0x to 3.0x)**: Utilizes `AVAudioUnitTimePitch` to preserve natural vocal pitch, allowing blind power-users to review long lectures at 2.5x speed without audio degradation.

### 5.4 Private iCloud Drive Sync (CloudKit)
- Audio recordings sync seamlessly across iPhone, iPad, and Mac via the user's private iCloud container (`FileManager.default.url(forUbiquityContainerIdentifier:)`).
- Zero third-party cloud servers; completely private, compliant with Apple’s strictest privacy guidelines.

---

## 6. Technical Stack & iOS Architecture

| Component | Technology | Rationale |
| :--- | :--- | :--- |
| **Language** | **Swift 6** | Memory-safe, concurrency-safe (async/await), modern Apple standard. |
| **UI Framework** | **SwiftUI + UIKit Touch Interceptor** | Declarative state management paired with a custom `UIViewRepresentable` touch canvas for sub-millisecond centroid tracking. |
| **Audio Engine** | **AVFoundation (`AVAudioEngine`, `AVAudioRecorder`)** | Low-latency audio processing, hardware microphone steering, uncompressed PCM or high-efficiency AAC/ALAC. |
| **Haptics** | **CoreHaptics (`CHHapticEngine`)** | Granular, continuous haptic patterns tailored for non-visual spatial orientation. |
| **Speech (TTS)** | **`AVSpeechSynthesizer`** | Native high-quality Siri/Compact neural voices, interruptible, configurable pitch/speed. |
| **Speech (STT)** | **`Speech` (`SFSpeechRecognizer`)** | 100% on-device offline transcription for automatic naming. |
| **Data Persistence**| **SwiftData / CoreData + FileManager** | Fast metadata querying for thousands of recordings and folders. |
| **App Intents** | **AppIntents framework** | Action Button, Siri Shortcuts, and Control Center widget actions. |

---

## 7. Phased Implementation Roadmap

```
PHASE 1: Core Foundation & Tactile Canvas
├── Project setup (Swift 6, iOS 17+ target)
├── Custom Touch Interceptor View (Centroid tracking & multi-touch dispatcher)
├── Dual-Pane Rotary Navigation Engine (Folders & Files)
├── CoreHaptics Engine implementation (Rotary ticks, boundaries, tap pulses)
└── AVSpeechSynthesizer Integration (Self-voicing audio announcements)

PHASE 2: Recording Engine & Earcon Soundscapes
├── High-Fidelity Audio Recording (AAC 64-256kbps & Lossless ALAC)
├── Synthesized Earcon Generator (Low-latency audio feedback)
├── Screen Curtain implementation (True black OLED canvas + system brightness management)
└── Audio Bookmarking (3x Tap timestamp insertion)

PHASE 3: Navigation Superpowers & Parity
├── 2-Finger Alphabetical & Month Section Seeking
├── 2-Finger Bookmark Skip during playback
├── 4-Finger Safe Deletion with 2-Finger Undo
└── VoiceOver Harmony (UIAccessibilityCustomAction & Magic Tap recognition)

PHASE 4: Hardware & Smart Ecosystem
├── Action Button App Intent (Instant pocket recording)
├── Live Activities & Dynamic Island widgets
├── On-device SFSpeechRecognizer first-15s auto-naming
└── Skip Silence & Pitch-Preserved Speed Playback (1.0x - 3.0x)

PHASE 5: Testing, AppleVis Community Beta & App Store Release
├── TestFlight beta rollout with blind accessibility testers
├── AppleVis directory submission and feature review
└── App Store submission with full Accessibility declarations
```

---

## 8. Open Design Decisions for Discussion

Before coding begins, we will align on these key design choices:

1. **Storage Structure & Existing Files**:
   - Should the iOS app import existing voice memos from iCloud Drive / Files app, or maintain an isolated folder structure (`Documents/EyesFreeRecorder/`)?
2. **Action Button Behavior**:
   - Should pressing the Action Button immediately start recording in the background, or open the app first with spoken confirmation?
3. **Screen Curtain Implementation**:
   - On iOS, third-party apps cannot turn off the physical screen hardware directly. Should we use an opaque pitch-black OLED view coupled with system idle timer control, or guide users to the native triple-tap Screen Curtain when VoiceOver is active?
4. **VoiceOver Detection**:
   - Should the app automatically suppress its internal TTS when `UIAccessibility.isVoiceOverRunning` is detected, or offer a dedicated toggle in settings?

---

*Document Status: Draft v1.0 — Ready for iterative refinement.*  
*Maintained under: `docs/IPHONE_EYES_FREE_RECORDER_PLAN.md`*
