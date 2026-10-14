# EYES-FREE Recorder — Product Requirements Document

Last updated Oct 12, 2026 · S&D A11y Studio (SJ & JY)

> Source of truth: the live PRD doc in Claude. This file is a snapshot exported for the repo.

## Executive summary

EYES-FREE VOICE RECORDER turns the whole phone screen into a single touch controller. Blind and low-vision users can record, find and play audio without hunting for buttons. Every action gets spoken, earcon and haptic feedback.

- **Status:** Android v4.2.0 (versionCode 97, minSdk 26, targetSdk 36) is built from [jonlee0704/EYES-FREE-RECORDER](https://github.com/jonlee0704/EYES-FREE-RECORDER). The latest Play review, of versionCode 95, was **rejected on Oct 1, 2026** for crashing on launch. An iOS version is planned, with an architecture draft dated Oct 2.
- **What ships today:** gesture-only recording and playback, folder and file navigation, two-finger fast seek by letter or month, bookmarks, favorites, corner anchors, a screen curtain, a gesture tutorial, Google Drive backup, and a Terms of Service gate on first launch.
- **Top priorities:**
  1. Fix the launch crash and pass Play review.
  2. Make the privacy disclosures match what the code does. It geotags recordings by default and uses the network for Drive backup and place names, but the docs say "zero data collection".
  3. Settle on one gesture map, because the manual and the code disagree.
  4. Start iOS Phase 1.

This PRD covers every feature found in the code (v4.2.0) and the docs. Where the two disagree, it says so in **Open questions**.

## Problem and vision

Phone recorders are built around small visual targets, which makes them slow and stressful for blind users. Examples are a record circle, a pause glyph and a narrow scrubber. Under TalkBack or VoiceOver, every action becomes find, focus, then double-tap. That breaks the moment when you need to capture a lecture, a conversation or a musical idea right away.

**Pain points**

- **Button hunting:** reaching record or pause takes several swipes or slow exploration by touch.
- **No orientation:** the user can't easily tell which folder or file they're in, or how far through a recording they are.
- **Slow navigation of large libraries:** stepping one file at a time through hundreds of date-named recordings is impractical.
- **Recording in context:** recording should work while walking, in a pocket or in the dark, without unlocking or looking.
- **Privacy and battery:** a lit screen is a privacy and battery cost when nobody is looking at it.

**Vision:** the entire glass is the instrument. A tap anywhere plays. A double tap anywhere records. Swipes move through files and folders like a rotary dial. Every action is confirmed by speech, a spatial earcon and a distinct haptic pattern, so the user always knows the state without sight. The app was first built in 2014 for one person ("Sj"). It is now being generalized for the wider blind and low-vision community.

## Goals, non-goals and success metrics

**Goals**

1. Start a recording within 1 second of intent, from anywhere on the screen, with no visual targeting.
2. Keep the user oriented at all times: folder, file, position and state are available by one gesture.
3. Navigate libraries of thousands of recordings in a few gestures, using letter and month sections.
4. Keep all data on the device by default, with no account, no ads and no tracking.
5. Ship a stable Android app on Google Play, then reach parity on iPhone.

**Non-goals (v4.x)**

- A general music player with playlists, streaming or equalizers.
- Cloud transcription or any server-side processing.
- Replacing TalkBack or VoiceOver outside this app.
- Use for medical, legal-evidence or emergency recording. The ToS disclaims these explicitly.

**Success metrics** (targets are proposed; there is no analytics today, so they would come from beta testing and store data)

| Metric | Target | How measured |
| --- | --- | --- |
| Crash-free sessions | ≥ 99.5% | Play Console Android vitals |
| Play review | Approved, no policy rejections | Play Console |
| Time from app open to recording | ≤ 1 s (p90) | Beta test with stopwatch scripts |
| Gesture recognition accuracy | ≥ 95% intended action | Moderated sessions with blind testers |
| Tasks completed without sighted help | 100% of core tasks | Usability study (record, find, play, bookmark, delete) |
| Store rating | ≥ 4.5 | Play / App Store |
| Beta testers from the blind community | ≥ 20 (Android), ≥ 20 (iOS TestFlight) | Tester roster |

## Target users and use cases

The primary user is a blind person who records and replays audio every day. Most blind users run TalkBack or VoiceOver all the time, so the app has to coexist with screen readers.

| Persona | Context | Needs most |
| --- | --- | --- |
| Sj, the original user (blind, daily recorder) | Records many short memos a day; library organized by date; listens back repeatedly | Instant record, month and letter seek, low-battery reminders, large volume safety cap |
| Blind student | Lectures of 45–90 min | One-tap record, bookmarks during recording, fast scrub, file info on demand |
| Blind professional | Meetings and interviews | Reliable capture, stereo or noise suppression, backup, recording-consent awareness |
| Blind musician | Captures musical ideas and rehearsals | High-quality stereo, quick record from a pocket, favorites |
| Low-vision user without a screen reader | Prefers large gestures to small UI | Screen curtain, high-contrast display modes, spoken feedback |

**Core use cases**

1. Record a voice memo from a pocket in under a second, and know it saved.
2. Find yesterday's recording among hundreds by month, then by time.
3. Listen to a long lecture, mark key moments, and jump back to them later.
4. Check time, battery and free storage without leaving the app.
5. Back up recordings and recover from an accidental delete.
6. Play audio imported from a computer (MP3, M4A, FLAC, OGG, WAV).

## Feature inventory and requirements

The app has 57 features in 9 areas. Status is based on the v4.2.0 source: **Shipped** = in the code; **Partial** = in the code with gaps or doc conflicts; **Planned** = only in docs or the iOS plan.

### A. Recording

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| R1 | Record anywhere | 1-finger double tap anywhere starts recording; the same gesture or a single tap stops it | Mic is live ≤ 500 ms after the gesture; "Recording started" spoken plus a double pulse; playback pauses first | Shipped |
| R2 | Corner record | A tap in the bottom-right 72 dp corner toggles recording | Same feedback as R1, plus corner haptic | Shipped |
| R3 | Format and quality | Records MPEG-4 audio (.m4a) in one of three profiles: High Quality (AAC 64 kbps, 44.1 kHz, the default), Storage Saver (HE-AAC 32 kbps) or Studio/Music (AAC 128 kbps, 48 kHz, stereo). Falls back to plain AAC if the encoder fails | Files play in the system player; bitrate and sample rate match the chosen profile | Shipped (docs say WAV: fix the docs) |
| R4 | Stereo recording | When on (the default), uses the MIC source so the top and bottom mics map to left and right | Stereo file on 2-mic devices; mono fallback otherwise | Shipped |
| R5 | Noise suppression | When on, uses the VOICE\_RECOGNITION source (mono only) | Lower background noise in A/B test | Shipped (default off) |
| R6 | Auto naming | File name is a timestamp `YYYY-MM-DD-HH-MM`, plus the place name when location is on | Unique, sortable names; no overwrite on same-minute recordings | Shipped (check same-minute collisions) |
| R7 | Save to active folder | A new file goes to the current folder, and focus moves to it | "Recording saved" spoken; the file is selected | Shipped |
| R8 | Bookmarks while recording | Triple tap during recording adds a bookmark at the current offset | "Bookmark N added" spoken; offsets saved in the companion metadata | Shipped |
| R9 | Location tag | When on (default **on**), embeds GPS in the MP4 and stores a place name | Tag only with permission; off means no location access at all | Partial: conflicts with privacy docs |
| R10 | Recording from the background or lock screen | Recording continues with the screen off or in the background, with a persistent notification | 60-minute recording survives screen-off and app switch | Gap: no foreground service is declared in the manifest, so Android 14+ may stop recording in the background |

### A2. Pro Recording Mode (new — proposed Oct 12, 2026)

Pro Recording Mode gives blind recordists studio-style control without looking at the screen. It has three independent settings: **Environment** (where you are), **Focus** (what to keep) and **Gain** (how loud the input is). A fourth control, **noise reduction**, sets how much of the unwanted sound is removed. Every setting is spoken, has its own haptic feel, and is saved with the recording.

**1. Environment: where you are.** This sets only room-dependent processing: echo control, gain staging, peak limiting, low-cut filter and wind reduction.

| Environment | Low-cut filter | Echo / reverb control | Wind reduction | Default focus | Default gain mode |
| --- | --- | --- | --- | --- | --- |
| Meeting room | 100 Hz | On | Off | Voice | Auto |
| Small music venue | 30 Hz | Off | Off | Music | Manual + safety track |
| Stadium / arena | 60 Hz | Off | Off | Natural | Manual + safety track |
| Street / outdoor | 120 Hz | Off | On | Voice | Auto |

**2. Focus: what you want to keep.** This decides what counts as "noise".

| Focus | Keeps | Noise reduction removes | Method |
| --- | --- | --- | --- |
| Voice | Speech | Everything that is not speech: music, crowd, traffic, hum | On-device speech-enhancement model (e.g. RNNoise, BSD-licensed) |
| Music | Instruments and vocals | Only steady noise: HVAC, fans, hiss, mains hum. Never notes or rhythm | Stationary-noise reduction (spectral gating learned from a noise profile) |
| Natural | Everything | Very little: gentle hum and hiss only | Light stationary-noise reduction, capped at 50% |

Example: recording a band in a meeting room = **Meeting room + Music focus**. You keep the room's echo control and gain settings, and noise reduction removes only the projector fan and AC hum, never the music.

**3. Noise reduction amount.** There are five levels: Off (0%), Light (25%), Medium (50%), Strong (75%) and Maximum (100%). The level is the wet/dry mix between the original and the cleaned signal. 0% keeps the original; 100% keeps only what the chosen focus keeps. Defaults: Voice 75%, Music 25%, Natural 25%. The app warns once at Maximum that voices may sound thinner.

**4. Input gain: protect real dynamics.** Phones normally run automatic gain control (AGC) and compression, which flatten loud music and crowds. Pro mode can switch these off and lock the gain, so the recording keeps its real dynamics for post-production.

| Gain mode | Behavior | Best for |
| --- | --- | --- |
| Auto | Phone's default AGC and compression | Speech in quiet rooms |
| Manual | AGC off, using the UNPROCESSED mic source where the device supports it. Fixed gain from −24 dB to +12 dB in 3 dB steps. Records 24-bit WAV | Music, loud venues, anything to be mixed later |
| Manual + safety track | Manual, plus a second copy recorded 12 dB lower | Unpredictable peaks (drums, crowds, fireworks) |

Limits to state honestly: most phones don't let apps change the analog mic preamp. Manual gain is a digital trim after the converter, so it can't prevent clipping that happens in the phone's hardware; the safety track and sound check cover that case. With an external USB or Lightning interface or mic, its hardware gain knob is used, and the app records exactly what it receives.

**Requirements and acceptance criteria**

| ID | Requirement | Acceptance criteria |
| --- | --- | --- |
| PR1 | Turn Pro mode on or off with the Pro button in the top bar, from Settings, or from the spoken Pro menu. Decided Oct 13 (JY): one tap on the Pro button steps Standard → Meeting room → Small music venue → Stadium → Street → Standard; a long press opens the Pro menu, which holds every Pro setting. A 3-finger double tap also opens the menu. The mode is locked while recording | Spoken: "Pro mode on. Meeting room, Voice focus, noise reduction 75 percent, gain auto"; state persists. In the menu: 1-finger swipe right/left moves between items, up/down changes the value, tap selects, long press explains the item, double tap closes it and records; it closes after 60 s idle |
| PR2 | Pick the environment with a 3-finger swipe left or right while idle in Pro mode | Speaks the environment and the defaults it loads; a distinct earcon per environment |
| PR3 | Pick the focus with a 3-finger swipe up or down while idle in Pro mode. Decided Oct 12 (JY): while Pro mode is idle, this replaces the Settings/Backup swipe. Settings and Backup stay reachable from the Settings menu, and the swipes work as before once Pro mode is off | Cycles Voice → Music → Natural; spoken; resets the noise-reduction default for that focus |
| PR4 | Set the noise-reduction level in Settings → Pro Recording before recording, or live with a 2-finger swipe up or down while recording | Live change takes effect within 200 ms, with no click or dropout; softly announced; time-stamped in metadata. Idle 2-finger swipes stay reserved for month/letter seek |
| PR5 | Gain control: the volume buttons adjust input gain (not playback volume) while Pro mode is armed or recording, in Manual modes | Each press is 3 dB, spoken ("Input minus 9 dB"); range −24 to +12 dB; in Auto mode the buttons behave normally |
| PR6 | Sound check: a 5-second listen before recording, triggered from Settings or from the Pro menu. Decided Oct 13 (JY): not on 1-finger long press, which stays File info in every state | Reports the peak in dBFS and recommends a gain ("Peak minus 2 dB. Recommend lowering input 6 dB"); one tap applies the recommendation |
| PR7 | Clipping and level feedback: haptic intensity rises as peaks approach 0 dBFS; a short tone and a vibration on clipping. Decided Oct 13 (JY): headphone-aware. Without headphones, nothing is spoken or beeped while recording, because the mic would record it; changes and clip counts are read after "Recording stopped" | At most 1 spoken warning every 10 s with headphones; works with the screen curtain on and in a pocket |
| PR8 | Safety track: in Manual + safety track mode, record a second file 12 dB lower | Both files share a name stem (e.g. "\_safety"); deleted together; playable from file info: a second long press within 10 s cycles main → safety → original, and a tap plays the selected version |
| PR9 | "Keep original": whenever noise reduction is above 0%, also save the unprocessed file (default on) | The original is always recoverable; storage warnings count both files |
| PR10 | Headphone monitoring, so the user hears the effect of focus, noise reduction and gain | ≤ 50 ms latency on wired headphones; Bluetooth shows a latency warning |
| PR11 | Metadata: environment, focus, noise-reduction level and gain (both including time-stamped live changes) and gain mode are saved and read in file info | e.g. "Meeting room, Music focus, noise reduction 25 percent, manual gain minus 6 dB" |
| PR12 | Where am I and status announcements include the Pro mode state | The top-left corner report includes all four settings |
| PR13 | Custom presets: save a combination of environment, focus, noise reduction and gain | Three numbered slots in the Pro menu: tap an empty slot to save, a saved slot to load; long press saves over a slot |

**Technical approach (Android)**

- Replace MediaRecorder with an AudioRecord PCM pipeline when Pro mode is on. Processing chain: low-cut filter → focus-specific noise reduction (wet/dry mix) → wind reduction → AGC (Auto mode only) → limiter (off in Manual unless chosen) → encoder.
- Encoders: MediaCodec AAC, or 24-bit WAV in Manual gain modes.
- Mic source per mode: UNPROCESSED (API 24+, where supported) for Manual gain; VOICE\_RECOGNITION or MIC otherwise. Detect support and announce any fallback ("This phone can't turn off auto gain; using lowest fixed level").
- Everything runs on the device, so the privacy promise holds. Targets: ≤ 20 ms of added latency, ≤ 15% CPU on a mid-range phone, battery cost ≤ 1.5× standard recording.
- iOS (later): AVAudioEngine. Voice-processing I/O for Voice focus; measurement mode with AGC off for Music and Natural; input gain set via AVAudioSession where the input allows it, otherwise the same digital-trim fallback.

**Accessibility and UX (Nicole):** check for gesture collisions with TalkBack pass-through and existing 3-finger gestures (triple tap = curtain, long press = favorite). Review:

- the spoken names for environments and focuses
- the earcon set
- whether repurposing the volume buttons in Manual mode is clear enough (announce it when entering Pro mode)
- the spoken Pro menu (built Oct 13): item order, wording and the 60 s timeout

**Out of scope for v1:** applying noise reduction to existing files after recording, multi-mic beamforming beyond what the device provides, and separating individual instruments.

**Open questions:**

- Free feature or part of a paid tier?
- Should 32-bit float WAV be offered for external interfaces?
- Which test devices define "supports UNPROCESSED"? At least 3 Android phones across vendors should be tested before launch.

### B. Playback

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| P1 | Play / stop | A single tap anywhere starts the selected file, or stops it if playing | Audio starts ≤ 300 ms; state change is announced | Shipped |
| P2 | Swipe-and-hold scrub | Swipe and hold right or left fast-forwards or rewinds continuously; release resumes | Scrub audible while held; resumes at the new position | Shipped |
| P3 | Two-finger hold fast-forward | Two-finger swipe and hold right plays at 3x | Rate returns to 1x on release | Shipped (not in the manual) |
| P4 | Jog dial | A circular drag scrubs forward (clockwise) or back, with 5° detents and haptic ticks | Each detent moves a fixed step; ticks are felt | Shipped |
| P5 | Auto-advance | At the end of a file, the next file plays | No gap or crash at folder end | Shipped (verify the folder-end behavior) |
| P6 | Max volume cap | Output volume is capped by the Max Volume setting (30 / 50 / 70 / 90 / 100%, default 90%) | Volume never exceeds the cap | Shipped |
| P7 | Speed and skip silence | Pitch-corrected 1.0–3.0x speed and skip silence | Speech intelligible at 2.5x | Planned (iOS plan) |
| P8 | Headset support | Wired and Bluetooth headsets for playback, TTS and recording mic | Audio routes to the headset; mic switches when present | Shipped (per manual; verify) |

### C. Library and navigation

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| N1 | Next / previous file | 1-finger swipe right or left | Title and "File A of B" spoken; panned chirp; tick | Shipped |
| N2 | Next / previous folder | 1-finger swipe up or down | "Folder: name, X of Y" spoken; chime; heavy tick | Shipped |
| N3 | Fast folder roll | Swipe and hold up or down cycles folders quickly | Rapid ticks; release stops on a folder | Shipped |
| N4 | Library indexing | Indexes MediaStore plus public Music, Recordings, Podcasts, Downloads and SD-card folders; skips hidden, backup and archive folders; prunes deleted ones | New files appear without a restart; scan < 3 s for 5,000 files (target) | Shipped |
| N5 | Imported formats | Plays MP3, M4A, FLAC, OGG and WAV | All five play | Shipped |
| N6 | Artist navigation | In media-scan mode, diagonal swipes (8-direction dial) move to the next or previous artist | Artist name spoken | Shipped (undocumented) |
| N7 | Navigation mode | When on, swiping between files announces titles without auto-playing | Playback only on tap | Shipped (default off) |
| N8 | Welcome guide file | First run creates `01_Welcome_Audio_Guide.wav` in the default folder | The file exists and plays | Shipped |

### D. Fast seek and search

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| S1 | Seek files by section | 2-finger swipe right or left jumps to the next or previous section. A section is a first letter, or an hour for timestamped files | Section letter spoken; overlay shows "File i of n" | Shipped |
| S2 | Seek folders by month | 2-finger swipe up or down jumps by YYYY-MM for date-named folders, otherwise by first letter | Month or letter spoken | Shipped |
| S3 | Bookmark jump | Jump to the next or previous bookmark in the current file | Seeks to the bookmark; announces its index and time | Partial: the code requires a 2-touch swipe that the fast-seek branch catches first |
| S4 | Favorite jump | Jump to the next or previous favorite file | Announces the file | Partial: same gesture conflict as S3 |
| S5 | Single-section fallback | When there is only one section, say so instead of failing silently | "Single section" spoken | Shipped |

### E. Organization

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| O1 | Bookmarks in playback | Triple tap during playback adds a bookmark at the current position | "Bookmark N at mm:ss" spoken; persists across restarts | Shipped |
| O2 | Favorites | 3-finger long press toggles favorite | "Starred as favorite" or "Removed from favorites" spoken | Shipped |
| O3 | Companion metadata | Bookmarks, favorite flag, channel mode and place name are stored per file, and deleted with the file | Metadata survives an app restart and follows renames | Shipped (check renames) |
| O4 | Delete | 4-finger swipe right deletes the current file: it is moved to an in-app trash folder, then removed | "Deleted name" spoken | Partial: docs say permanent; there is no undo gesture or trash restore UI |
| O5 | Undo delete | 2-finger scrub undoes the last delete | "Undo complete" | Planned (iOS plan) |

### F. Status and orientation

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| F1 | Where am I | Top-left corner tap reports folder X of Y, file A of B, playback state and volume | Full report in ≤ 5 s at default rate | Shipped |
| F2 | File info | 1-finger long press reads title, duration, date, location, favorite and bookmark count | All fields that exist are spoken | Shipped |
| F3 | Time and battery | 2-finger long press or bottom-left corner reads time, day, date and battery % | Playback resumes after | Shipped (free storage is in the manual: verify) |
| F4 | Low-battery reminder | Below 30% battery, speaks a reminder to charge | Repeats at a sane interval, not every action | Shipped (check the frequency) |
| F5 | TTS toggle | Top-right corner turns spoken prompts on or off | State spoken; earcons and haptics stay on | Shipped |

### G. Display

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| D1 | Screen curtain | 3-finger triple tap blacks out the display while touch, audio and haptics keep working; the state persists | "Screen curtain on/off" spoken; 0 visible pixels | Shipped (v4.2.0) |
| D2 | Visualizer modes | Minimal split (default), circular dial, frequency spectrum, starburst | Mode persists; no impact on gesture latency | Shipped |
| D3 | Section overlay | A large letter overlay shows the current section during fast seek | Visible in every mode | Shipped |
| D4 | Fullscreen / immersive | Hides the system bars so edge swipes reach the app | Default on | Shipped |
| D5 | Volume indicator | A transient volume pill, with spoken volume changes | Speaks the new level on hardware key press | Shipped |
| D6 | Tablet layouts | sw600dp portrait and landscape layouts | Gestures work in both orientations | Shipped |

### H. Help and settings

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| H1 | Gesture tutorial | Animated tutorial on first run; replay from Settings; text-help shortcut | Can be dismissed with any gesture; replayable | Shipped (v4.2.0) |
| H2 | Spoken help | Reads the full gesture guide aloud in queued chunks at 1.2x | Interruptible by a tap | Shipped |
| H3 | Settings | 3-finger swipe down opens Settings; covers TTS, quality, stereo, noise suppression, location, max volume, navigation mode, eye-poke warning, visualizer, haptic pulse, fullscreen, screen curtain, tutorial, backup and legal | Every setting is labelled for TalkBack | Shipped |
| H4 | Speech rate | TTS speed is adjustable (2.5x is hard-coded at startup today) | The user setting persists and is applied | Partial: no visible rate setting found in `pref_general.xml` |
| H5 | Eye-poke warning | Configurable reminder about eye-poking, a behavior seen in some blind children | Fires on the chosen schedule | Shipped (purpose to document) |
| H6 | Math quiz | An optional spoken arithmetic quiz on launch, with configurable range, mode and count | Off by default | Shipped (personal feature: keep or remove?) |

### I. Backup

| ID | Feature | Requirement | Acceptance criteria | Status |
| --- | --- | --- | --- | --- |
| B1 | Manual backup | 3-finger swipe up, or Settings, starts a backup to Google Drive with Google sign-in | Progress and result spoken; resumable | Shipped (conflicts with "no network" docs) |
| B2 | Auto sync | Optional periodic sync (default 180 min) of chosen target folders | Runs only on Wi-Fi (to confirm) | Shipped (default off) |
| B3 | Import folder | Choose a folder to import audio from | Imported files are indexed | Shipped |
| B4 | Local Wi-Fi peer backup | Peer-to-peer transfer into a `Shared` folder | — | Planned (manual only) |

## Gesture and interaction model

The whole screen is one control surface. The meaning of a gesture depends on four things: how many fingers touch, the gesture type (tap, swipe, hold, swipe and hold, circle), the direction (4-way, or 8-way in media-scan mode), and the current state (idle, playing or recording). Four 72 dp corner zones act as fixed tactile anchors.

### Canonical gesture map (from the v4.2.0 code)

| Fingers | Gesture | Idle | Playing | Recording |
| --- | --- | --- | --- | --- |
| 1 | Single tap (surface) | Play selected file | Stop playback | Stop and save |
| 1 | Double tap | Start recording | Start recording | Stop and save |
| 1 | Triple tap | Add bookmark | Add bookmark at position | Add bookmark at offset |
| 1 | Tap top-left corner | Where am I | Where am I | Where am I |
| 1 | Tap top-right corner | Toggle speech | Toggle speech | Toggle speech |
| 1 | Tap bottom-left corner | Time, date, battery | Same | Same |
| 1 | Tap bottom-right corner | Start recording | Start recording | Stop and save |
| 1 | Swipe right / left | Next / previous file | Next / previous file | Ignored ("on air") |
| 1 | Swipe up / down | Next / previous folder | Same | Ignored |
| 1 | Swipe and hold right / left | — | Fast-forward / rewind | — |
| 1 | Swipe and hold up / down | Fast folder roll | Fast folder roll | — |
| 1 | Long press (> 0.5 s) | File info | File info | — |
| 1 | Circular drag | Jog through files or folders | Jog scrub (clockwise = forward) | — |
| 1 | Diagonal swipe (media-scan mode) | Next / previous artist | Same | — |
| 2 | Swipe right / left | Seek file section (letter or hour) | Same | — |
| 2 | Swipe up / down | Seek folder section (month or letter) | Same | — |
| 2 | Swipe and hold right | — | 3x fast-forward | — |
| 2 | Long press (> 0.45 s) | Time, date, battery | Same, then resume | — |
| 3 | Double tap | Open / close the Pro menu | Same | "On air" |
| 3 | Triple tap | Toggle screen curtain | Same | Same |
| 3 | Long press | Toggle favorite | Toggle favorite | — |
| 3 | Swipe down | Open Settings | Open Settings | — |
| 3 | Swipe up | Start / stop backup | Same | — |
| 4 | Swipe right | Delete current file | Delete current file | — |

### Recognition rules

- **Tap counting:** taps are grouped within a short window (about 450 ms) before dispatch, so a double tap never fires a single tap first.
- **Swipe threshold:** a swipe must travel at least 50 dp. Shorter movement counts as a tap or a hold.
- **Multi-finger lock:** once 3 or more fingers are seen, the gesture stays in multi-finger mode until all fingers lift, so a stray finger can't trigger a 1-finger action.
- **Two-finger tracking:** two-finger swipes use the centroid of both touches, so uneven finger travel still reads as one direction.
- **Recording guard:** while recording, only stop, bookmark and status gestures are accepted. Everything else is announced as "on air".
- **Dial area:** the jog ring occupies 30–82% of the screen radius with 5° steps. A touch that starts outside the ring is treated as a swipe.

**Doc conflicts to resolve:** the manual lists 1-finger double tap as "speak file info" and triple tap as "where am I". Those mappings exist only in a legacy `GestureListener` path; the active `DialView` path uses record and bookmark. Bookmark and favorite jumps (2- and 3-finger swipes) are unreachable because the 2-finger fast-seek branch catches them first. The iOS plan uses 2-finger swipes during playback for bookmark jumps, which conflicts with section seek.

## Speech, earcon and haptic feedback

Every command gets three kinds of feedback: spoken text, a synthesized earcon and a haptic pattern. If any one channel is muted, the other two still identify the action.

### Spoken announcements (TTS)

| Event | Announcement (example) | Priority |
| --- | --- | --- |
| Launch | Current folder and file | Normal |
| File change | "Project Notes Part 1. File 4 of 28" | Interrupts previous |
| Folder change | "Folder: Lectures. 3 of 12" | Interrupts previous |
| Section seek | "\[M\]" or "October", plus "File i of n" | Interrupts previous |
| Record start / stop | "Recording started" / "Recording saved" | Always spoken, even with speech off |
| Bookmark | "Bookmark 3 at 14:22" | Always spoken |
| Favorite | "Starred as favorite" / "Removed from favorites" | Always spoken |
| Delete | "Deleted \[name\]" | Always spoken |
| Screen curtain | "Screen curtain on / off" | Always spoken |
| Where am I | Folder X of Y, name, file A of B, name, state, volume % | Normal |
| File info | Title, duration, date, location, favorite, bookmark count | Normal |
| Time / battery | "It is 3:45 PM, Wednesday. Battery level is 82 percent." | Normal |
| Low battery (< 30%) | "Battery level is very low, connect to power charger" | Normal |
| Volume key | New volume level | Normal |

**TTS rules**

- Navigation speech flushes the queue, so the newest item is always the one spoken. Help text queues in chunks.
- Safety-critical confirmations use an always-speak path that ignores the speech-off toggle.
- The default rate is 2.5x for navigation and 1.2x for long help. This needs to be a user setting (see H4).
- Speech must never be recorded into the mic. Recording starts only after the "Recording started" utterance finishes, or speech is routed away from the mic. This needs verifying.

### Earcons (synthesized, low latency)

| Action | Sound | Spatial cue |
| --- | --- | --- |
| Next file | Rising chirp 520 → 940 Hz | Panned right |
| Previous file | Falling chirp 940 → 520 Hz | Panned left |
| Next folder | Two-tone up, C5 523 Hz → G5 784 Hz | Centre |
| Previous folder | Two-tone down, G5 → C5 | Centre |
| Corner anchor | Bell ping C6 1046 Hz | Centre |

### Haptics

| Action | Pattern |
| --- | --- |
| Next / previous file | Forward-rolling or backward-stepping micro-ticks |
| Folder change | Heavy "gear-shift" latch, stronger than file ticks |
| Play / stop | Rising ignition pulse / solid brake pulse |
| Record start | Double pulse |
| Corner anchor | Crisp double pulse |
| Jog dial | Micro-tick every detent (5° or about 30 dp) |
| Audio pulse (optional) | Haptics follow the audio level during playback (`pref_haptic_audio_pulse`, default on) |

The manual offers vibration intensity (High / Medium / Light / Mute), but no such preference exists in `pref_general.xml`. Add it or remove it from the manual.

## Accessibility requirements

The app speaks for itself, but it has to work with screen readers too. Most blind users keep TalkBack or VoiceOver on all day.

| ID | Requirement | Acceptance criteria |
| --- | --- | --- |
| AX1 | Works with TalkBack on | With TalkBack active, the user can reach the gesture canvas, and the pass-through gesture hands raw touches to the app. The tutorial explains how |
| AX2 | No double speech | When TalkBack or VoiceOver is speaking, the app's own TTS does not talk over it. Options: duck, defer, or let the user choose. Today the app does not detect TalkBack: decide the behavior |
| AX3 | Labelled UI | Every visible control (canvas, folder label, file label, play status, help, Terms & Privacy, settings) has a meaningful `contentDescription` |
| AX4 | Dialogs readable by screen readers | The Terms of Service dialog, help and settings are fully readable and actionable with TalkBack, including Accept and Decline |
| AX5 | No visual-only information | Every state shown on screen (overlay, volume pill, visualizer) is also spoken or felt |
| AX6 | Screen curtain | Display fully black while touch, audio and haptics keep working. The state persists across launches and is always announced |
| AX7 | Low vision | High-contrast dark theme; large section overlay (≥ 72 sp); does not depend on colour alone |
| AX8 | Motor tolerance | Swipe threshold 50 dp; tap window about 450 ms. Both should become adjustable for users with tremor or slower taps |
| AX9 | Hearing-impaired backup | Haptic patterns alone identify the main actions (play, record, file, folder, error) |
| AX10 | Orientation independence | Gestures work the same in portrait and landscape, and on tablets |
| AX11 | Headset parity | Feedback routes to wired or Bluetooth headsets; hardware media buttons map to play and pause |
| AX12 | Testing with blind users | Every release is tested by at least 3 blind users with screen readers on and off before it reaches production |

**Standards:** follow the Android accessibility guidelines and WCAG 2.2 where they apply to native apps (non-text content, sufficient contrast, timing adjustable, no seizure-inducing visualizers). On iOS, follow the Human Interface Guidelines for VoiceOver, including the Magic Tap and Escape conventions.

## Onboarding, legal and compliance

A first launch has to get a blind user from install to their first recording in under 2 minutes, with no sighted help, and capture legally valid consent on the way.

### First-launch flow

1. **Terms of Service gate:** a mandatory dialog that a screen reader can read. Accept stores `pref_tos_accepted`, its version (currently 1) and a timestamp. Decline exits the app immediately and completely. A new ToS version re-prompts.
2. **Permissions:** a spoken explanation, then the system prompts. Microphone and audio files are required. Notifications are needed for background use. Location is optional and only used for geotagging.
3. **Gesture tutorial:** an animated walkthrough of the core gestures, with a text-help shortcut. It can be dismissed with any gesture, and replayed from Settings.
4. **Welcome audio:** `01_Welcome_Audio_Guide.wav` is created in the default folder, so the first swipe has something to play.
5. **Ready:** the current folder and file are announced.

### Legal documents (in `docs/`)

| Document | Covers | Gap |
| --- | --- | --- |
| Terms of Service | As-is licence, warranty disclaimer, liability cap ($10), user owns backups, recording-consent laws, AI-training ban | Mentions no network or cloud, but Drive backup exists |
| Privacy Policy | Zero data collection, local-only storage, permission justifications | Wrong: location is collected by default; INTERNET is used for Drive and geocoding |
| Legal disclaimers | Two-party consent, not a medical or emergency device, multi-touch deletion risk, battery optimization | OK |
| Play Store Data Safety | Questionnaire answers: "No data collected"; location "Not collected" | Must change: location and Drive file access need declaring, and it claims foreground-service permissions the manifest doesn't declare |
| AI-training prohibition | LICENSE, README, ToS, robots.txt | OK |

### Store compliance requirements

- **C1:** Pass the Broken Functionality policy: no crash on launch on the Play pre-launch report devices (Android 8 to 16).
- **C2:** The Data Safety form matches the shipped behaviour exactly: location, Drive and network use.
- **C3:** Declare a foreground service of type `microphone` / `mediaPlayback` if recording or playback continues in the background, with a persistent notification.
- **C4:** Request location only when the user turns on geotagging, not at first launch. Default geotagging to **off**.
- **C5:** Restrict or remove the Google API key flagged as publicly accessible on Sep 30, 2026 (project `pivotal-racer-640`).
- **C6:** Target audience 18+ (or 13+), no ads, no account required.
- **C7 (iOS):** App Privacy labels, a microphone and speech-recognition usage string, and Accessibility Nutrition Labels.

## Technical architecture

The Android app is a single-activity Java app. A full-screen custom view turns raw touches into commands, a central dispatcher runs them, and one engine owns recording, playback and the library.

### Android (current, v4.2.0)

| Component | Responsibility | Size (lines) |
| --- | --- | --- |
| `DialView` | Full-screen touch canvas: tap counting, swipe direction (4 or 8 way), multi-finger lock, jog ring, visualizers, section overlay, screen curtain, tutorial | 4,774 |
| `MainActivity` | Lifecycle, TTS engine, ToS gate, help, 3-finger triple-tap detection, settings load, notifications, math quiz | 2,129 |
| `Commander` | Command dispatcher (about 50 command codes); recording guard; feedback (speech, earcons, haptics) | 1,690 |
| `Recorder` | MediaRecorder and MediaPlayer, library index and scan, section seek, bookmarks, favorites, delete to trash, low-battery check | 3,215 |
| `GestureListener` | Legacy gesture detector and corner anchors (72 dp) | 665 |
| `SoundscapeHelper` | Synthesized, panned earcons | 299 |
| `AudioMetadataHelper` | Companion metadata per file: bookmarks, favorite, channel mode, place | 501 |
| `LocationHelper` | Last-known location, ISO 6709 parsing, reverse geocoding to a place name | — |
| `DriveMainActivity`, `BackupRecorder` | Google sign-in and Drive v3 upload; backup list UI | 2,062 / 1,021 |
| `SettingsActivity` | Preference screens | 442 |

- **Build:** Gradle, compileSdk / targetSdk 36, minSdk 26 (Android 8.0). Release signing is configured, with an AAB for Play.
- **Dependencies:** AndroidX (appcompat, recyclerview, constraintlayout, preference, documentfile), Play Services Auth, Google API Client and Drive v3, and Gson HTTP client.
- **Storage:** public Music and Recordings folders, plus app-specific storage; per-file companion metadata; a trash folder for deleted files.
- **Code health:** the build compiles `src/main` (the `app` module points its sourceSets there). `app/src/main/java` holds stale 2014-era copies of `MainActivity`, `DialView` and `Commander`, which should be deleted to avoid confusion.

### iOS (planned)

| Layer | Technology |
| --- | --- |
| Language / UI | Swift 6, SwiftUI with a UIKit full-screen touch interceptor |
| Audio | AVAudioEngine and AVAudioRecorder; AAC 64–256 kbps or ALAC; voice-processing modes for noise suppression |
| Speech | AVSpeechSynthesizer (TTS); SFSpeechRecognizer on device for auto-naming |
| Haptics | CoreHaptics (CHHapticEngine) |
| Data | SwiftData or Core Data metadata, plus FileManager; optional iCloud Drive / CloudKit sync |
| System | App Intents (Action Button, Siri, Control Center); Live Activities and Dynamic Island; AirPods controls |
| Accessibility | UIAccessibilityContainer, custom actions, Magic Tap = record, Escape = cancel, rotor for section seek |
| Minimum OS | iOS 17 |

## Non-functional requirements

Recording reliability comes before everything else: a lost recording is the worst outcome for this user.

| Area | Requirement | Target |
| --- | --- | --- |
| Reliability | No crash on launch on any supported device | 0 crashes in the Play pre-launch report; ≥ 99.5% crash-free sessions |
| Reliability | A recording survives screen-off, app switch, incoming call and low memory | Saved up to the interruption point; never a 0-byte or corrupt file |
| Reliability | Safe stop on low storage or battery | Auto-stop and save with a spoken warning at < 100 MB free or < 3% battery |
| Latency | Gesture to feedback (haptic or earcon) | ≤ 100 ms |
| Latency | Gesture to mic live | ≤ 500 ms |
| Latency | Navigation announcement starts | ≤ 300 ms |
| Performance | Library index of 5,000 files across 300 folders | Cold scan ≤ 3 s; incremental ≤ 500 ms |
| Performance | Battery while recording with the screen curtain on | ≤ 5% per hour (target; measure on a mid-range device) |
| Storage | File size per hour (High Quality profile) | About 29 MB per hour at 64 kbps |
| Privacy | Local-only by default | No network calls unless Drive backup is turned on; no analytics or ads SDKs |
| Privacy | Location | Off by default; asked for only when the user turns geotagging on |
| Security | Secrets | No API keys in the repo or APK; restrict OAuth client IDs by package and signing SHA |
| Security | Deleted files | Kept in trash for 30 days, then purged; user can empty it (proposed) |
| Compatibility | OS and devices | Android 8.0–16 (API 26–36), phones and tablets; iOS 17+ |
| Compatibility | Audio routing | Built-in mic, wired headset, Bluetooth headset (SCO and A2DP) |
| Localization | Languages | English first; Korean next (the user community and the original user); TTS follows system locale |
| Quality | Automated tests | Unit tests for gesture classification, section seek and file naming; instrumented record/stop test |

## Known issues and risks

Two issues block the store today: a launch crash and privacy disclosures that don't match the app. Both must be fixed before the next Play submission.

| # | Issue / risk | Severity | Evidence | Fix |
| --- | --- | --- | --- | --- |
| 1 | Play rejected versionCode 95 (v4.1.2) for "Broken Functionality": crashes after opening | Blocker | Play email, Oct 1, 2026 | Reproduce on the Play pre-launch devices; check Android vitals; add null-safety around permissions and first-run storage; submit v4.2.x through an internal test track first |
| 2 | Privacy Policy and Data Safety say no location or network, but the code geotags by default and uses the network for Drive and reverse geocoding | Blocker (policy) | `pref_save_location` default true; `ACCESS_FINE_LOCATION` and `INTERNET` in the manifest | Default geotag off; update the Privacy Policy, ToS and Data Safety form; or remove the features |
| 3 | Google API key publicly accessible (SJplayer, `pivotal-racer-640`) | High | Google Cloud alert, Sep 30, 2026 | Rotate or restrict the key; check git history for secrets |
| 4 | No foreground service declared, so background recording may be killed | High | Manifest has no `<service>` | Add a microphone / mediaPlayback foreground service with a notification |
| 5 | Gesture map differs between the manual, the README, the iOS plan and the code | Medium | Section "Gesture and interaction model" | Publish one canonical map; generate the docs from it |
| 6 | Bookmark and favorite jumps unreachable | Medium | 2-finger swipe is caught by the fast-seek branch | Assign distinct gestures (see Open questions) |
| 7 | Delete is described as permanent, but the code moves files to trash with no restore UI | Medium | `Recorder` delete → trash folder | Add "restore last deleted" and trash purge; align the docs |
| 8 | TTS may talk over TalkBack | Medium | No accessibility-service detection | Detect TalkBack and offer a setting |
| 9 | Stale duplicate code in `app/src/main/java` | Low | Second copy of core classes | Delete |
| 10 | Personal features (math quiz, eye-poke warning) in a public product | Low | Settings screen | Keep behind a "family" profile, or remove for the store build |
| 11 | Same-minute file-name collision | Low | Name format `YYYY-MM-DD-HH-MM` | Add seconds or a counter suffix |

## Release history and roadmap

v4.2.0 is the current build, but the last version Play reviewed (v4.1.2) was rejected. The next milestone is a stable, approved Android release, and iOS work starts after that.

| Date | Version | What shipped |
| --- | --- | --- |
| 2026-10-02 | v4.2.0 (code 97) | Animated gesture tutorial with a text-help shortcut; 2-finger section search; screen curtain toggle; AI-training prohibition across docs; iOS master plan |
| 2026-10-01 | — | Play rejected code 95: crash after opening |
| 2026-09-30 | v4.1.2 (code 95) | Play submission build; null-safety in `Recorder`; blind user manual, ToS, Privacy Policy, legal disclaimers; ToS gate on first launch; rebrand to EYES-FREE VOICE RECORDER |
| 2026-09-30 | v4.1.1 (code 94) | Split list UI; spoken gesture guide; 2-finger fast seek by letter and month; centroid tracking; volume pill and spoken volume; release signing |
| 2014-07-02 | SJplayer v1 | Original app: recording, swipe navigation through files and folders, dial UI, virtual folders, immersive mode |

**Roadmap: 5 phases, 4 gates (no dates set yet)**

| Phase | Name | Scope | Gate to exit |
| --- | --- | --- | --- |
| 0 (now) | Stabilize Android | Fix launch crash, add foreground service, restrict API key. Align privacy docs, default geotag off, one gesture map. | Play approval, 0 launch crashes |
| 1 | Android polish | Bookmark and favorite jump gestures, trash restore, TalkBack mode. Speech rate and vibration settings, blind beta on a test track. | 20+ blind beta testers sign off |
| 2 | iOS foundation | Swift 6 full-screen touch canvas, dual-pane rotary navigation. CoreHaptics, AVSpeechSynthesizer, AAC and ALAC recording, earcons. | Record, find and play eyes-free on iPhone |
| 3 | iOS parity + hardware | Section seek, bookmarks, screen curtain, safe delete with undo. Action Button, Live Activities, on-device auto-naming, 1 to 3x speed. | Feature parity with Android |
| 4 | Launch | TestFlight with blind testers, AppleVis submission. App Store release and Play production release. | — |

Phase 0 is the only phase in progress. Each gate must pass before the next phase starts. Dates are not set yet.

## Open questions

- [ ] **Gesture map:** is 1-finger double tap "record" (code, README) or "file info" (manual)? Is triple tap "bookmark" (code) or "where am I" (manual)?
- [ ] **Bookmark and favorite jumps:** which gestures? Options: 2-finger swipe while playing (as in the iOS plan, but that conflicts with section seek), 3-finger swipe left/right, or the jog dial in a bookmark mode.
- [ ] **Location:** keep geotagging (default off, opt-in) or remove it to keep the "zero data" promise?
- [ ] **Backup:** keep Google Drive backup (and disclose it), replace it with system share or export, or add local Wi-Fi backup as the manual describes?
- [ ] **Delete:** permanent, or trash with a restore gesture? How long is the retention?
- [ ] **TalkBack:** when TalkBack is on, should the app mute its own TTS, duck it, or leave it to a setting?
- [ ] **Speech rate and vibration intensity:** add both to Settings (the manual promises them)?
- [ ] **Personal features:** keep the math quiz and eye-poke warning in the public build?
- [ ] **Recording format:** offer WAV / lossless for music (the manual mentions WAV), or stay AAC-only?
- [ ] **Pricing:** free, paid, or free with donations? (The ToS liability cap assumes a paid app is possible.)
- [ ] **iOS:** isolated app folder or Files and iCloud import? Should the Action Button record immediately or open the app? Use an in-app black view as the curtain, or point users to VoiceOver's curtain?
- [ ] **Branding:** the package name is still `com.jonlee.android.SJplayer`. Keep it (it can't change on Play) and rebrand only the display name?
