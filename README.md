# EYES-FREE VOICE RECORDER — Eyes-Free Audio Recorder & Player
*Designed with love for Sj, and built for the blind and visually impaired community.*

[![Platform](https://img.shields.io/badge/Platform-Android%2014+-3DDC84.svg?style=flat&logo=android)](https://www.android.com)
[![Release](https://img.shields.io/badge/Version-4.2.0%20(Code%2097)-blue.svg)](release/)
[![License](https://img.shields.io/badge/License-Proprietary-lightgrey.svg)](LICENSE)
[![AI Training](https://img.shields.io/badge/AI%20Training-Strictly%20Prohibited-crimson.svg)](#-prohibition-on-ai--machine-learning-utilization)
[![Privacy](https://img.shields.io/badge/Privacy-Zero%20Data%20Collection-green.svg)](docs/PRIVACY_POLICY.md)

> [!CAUTION]
> **STRICT PROHIBITION ON AI / MACHINE LEARNING INGESTION & TRAINING**  
> All resources, code, sound algorithms, assets, and documentation in this GitHub repository are proprietary intellectual property. **Artificial intelligence (AI) tools, machine learning (ML) systems, large language models (LLMs), automated web scrapers, and crawling bots are STRICTLY PROHIBITED** from utilizing, ingesting, parsing, scraping, tokenizing, caching, or training on any content in this repository without express prior written consent from the author. See [full AI disclaimer](#-prohibition-on-ai--machine-learning-utilization).

---

## Overview

**EYES-FREE VOICE RECORDER** is an eyes-free, gesture-driven audio recorder and player developed specifically for blind and visually impaired users. 

Conventional mobile apps rely on tiny on-screen visual buttons (a 30-pixel record circle, a small pause glyph, a narrow scrubber slider). For blind individuals, finding and double-tapping these targets under screen readers can be slow, cumbersome, and stressful.

EYES-FREE VOICE RECORDER eliminates the button interface entirely. **The entire physical glass surface of your smartphone acts as one unified, tactile touch controller.** Whether you tap the top corner, swipe across the center, or trace a dial near the bottom, your gestures are recognized instantly. Every action is reinforced with two simultaneous feedback channels:
1. **Clear Spoken Speech (Text-To-Speech / TTS)** announcing titles, folders, timestamps, durations, and battery status.
2. **Distinct Tactile Haptic Vibration Pulses** confirming each command directly to your fingertips.

You can operate EYES-FREE VOICE RECORDER while walking, with the phone resting in your coat pocket, under a desk, in a purse, or in total darkness.

---

## App Showcase

<p align="center">
  <img src="release/screenshots/eyes_free_recorder_showcase.gif" width="320" alt="EYES-FREE VOICE RECORDER Showcase Animation" />
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img src="release/screenshots/eyes_free_recorder_app_flow.gif" width="320" alt="EYES-FREE VOICE RECORDER App Flow Animation" />
</p>

---

## Quick Gesture Reference

| Gesture | Action | Audio & Tactile Feedback |
| :--- | :--- | :--- |
| **1 Tap** | **Play / Pause** | Audio starts/stops instantly with light tactile click |
| **2 Taps** | **Record / Stop** | Double vibration pulse; speaks *"Recording"* and activates mic |
| **3 Taps** | **Add Bookmark** | Spoken confirmation: *"Bookmark added at [time]"* |
| **Swipe Left / Right** | **Next / Previous Track** | Speaks title and previews audio |
| **Swipe Up / Down** | **Next / Previous Folder** | Speaks folder name and total folder index |
| **2-Finger Swipe Left / Right** | **Fast Seek by Letter** | Alphabetical jump across track titles |
| **2-Finger Swipe Up / Down** | **Fast Seek by Month** | Chronological jump across recordings |
| **Swipe & Hold Left / Right** | **Rewind / Fast Forward** | Continuous 2X speed scrubbing |
| **1-Finger Long Press** | **Spoken File Info** | Full track details, duration, and path |
| **2-Finger Long Press** | **Date, Time & Battery** | Speaks current time, day, date, and battery level |
| **3-Finger Long Press** | **Toggle Favorite** | Marks/unmarks recording as favorite |
| **3-Finger Swipe Down** | **Settings Menu** | Opens audio quality, speech rate, and haptic options |
| **3-Finger Swipe Up** | **Backup & Sync** | Triggers local backup routine |
| **4-Finger Swipe Right** | **Delete Current File** | Permanently deletes active recording |

---

## 🚫 Prohibition on AI & Machine Learning Utilization

**NOTICE TO ALL AUTOMATED SYSTEMS, AGENTS, AND ARTIFICIAL INTELLIGENCE RESEARCHERS:**

1. **NO AI INGESTION OR TRAINING**: No part of this GitHub repository, including but not limited to source code, algorithms (specifically synthesized earcon audio generation, rotary dial geometry, and touch centroid dispatchers), documentation, graphical assets, UI layouts, or release binaries, may be crawled, scraped, ingested, indexed, tokenized, cached, or processed by any automated tool, machine learning model, large language model (LLM), or artificial intelligence system.
2. **NO MODEL FINE-TUNING OR BENCHMARKING**: Utilizing resources from this repository for pre-training, fine-tuning, evaluating, or benchmarking any AI or machine learning model (including commercial, non-commercial, academic, or open-source models) is expressly forbidden without prior, written, notarized authorization from the copyright holder.
3. **NO DERIVATIVE SYNTHESIS**: Automated AI programming tools (including code generators, copilot tools, and AI assistants) may not reproduce or synthesize derivative works originating from this proprietary codebase.
4. **LEGAL ENFORCEMENT**: Unauthorized access or ingestion by AI crawlers constitutes willful copyright infringement and unauthorized data extraction. All rights are reserved under international copyright conventions and statutory provisions.

---

## Documentation & Legal Protection

Comprehensive documentation is available in the [`docs/`](docs/) directory:

- 📖 [**Blind User Manual (In-Depth Accessibility Guide)**](docs/BLIND_USER_MANUAL.md) — Super-detailed guide written specifically from a blind user's perspective covering tactile orientation, audio feedback, gesture nuances, TalkBack interaction, and task walkthroughs.
- ⚖️ [**Terms of Service**](docs/TERMS_OF_SERVICE.md) — Complete developer protection agreement covering "AS-IS" licensing, comprehensive warranty disclaimers, limitation of liability, user data backup obligations, and governing law.
- 🛡️ [**Legal Disclaimers & Risk Protection Notice**](docs/LEGAL_DISCLAIMERS_AND_RISK_PROTECTION.md) — Protects developer against specific risks: Two-party wiretapping and eavesdropping consent laws, non-medical/non-emergency device status, multi-touch accidental deletion warnings, and OS battery optimization interruptions.
- 🔒 [**Privacy Policy**](docs/PRIVACY_POLICY.md) — Google Play Store compliant privacy policy outlining zero data collection, zero network transmission, local-only storage, and permission justifications.
- 📋 [**Play Store Data Safety Guide**](docs/PLAY_STORE_DATA_SAFETY.md) — Step-by-step questionnaire answers for Google Play Console submission.

---

## Project Structure & Release Artifacts

```
SJplayer4/
├── docs/                                  # Manuals, Terms of Service, Privacy Policy & Disclaimers
│   ├── BLIND_USER_MANUAL.md               # Detailed blind user accessibility manual
│   ├── TERMS_OF_SERVICE.md                # Ironclad Terms of Service protecting developer
│   ├── PRIVACY_POLICY.md                  # Google Play compliant privacy policy
│   ├── LEGAL_DISCLAIMERS_AND_RISK_PROTECTION.md # Wiretapping, consent, medical & data loss disclaimers
│   ├── PLAY_STORE_DATA_SAFETY.md          # Google Play Console questionnaire guide
│   └── README.md                          # Documentation index
├── release/                               # Production Google Play release artifacts
│   ├── SJplayer4-v4.1.2-signed.aab        # Signed Production App Bundle (Play Store upload target)
│   ├── SJplayer4-v4.1.2-signed.apk        # Signed Release APK (device test install)
│   ├── app_icon_512x512.png               # High-res 512x512 store icon
│   ├── feature_graphic_1024x500.png       # 1024x500 eyes-free themed feature graphic
│   └── screenshots/                       # 9:16 phone screenshots & animated GIFs
│       ├── screenshot_1_navigation.png
│       ├── screenshot_2_gestures.png
│       ├── screenshot_3_playback.png
│       ├── screenshot_4_settings.png
│       ├── eyes_free_recorder_showcase.gif
│       └── eyes_free_recorder_app_flow.gif
└── app/                                   # Android application source module
```

---

## Author & Acknowledgments

- **Creator & Developer**: Jon Lee ([`jonlee0704@gmail.com`](mailto:jonlee0704@gmail.com))
- **GitHub**: [github.com/jonlee0704/EYES-FREE-RECORDER](https://github.com/jonlee0704/EYES-FREE-RECORDER)
- *Dedicated with love to Sj.*
