# AGENTS.md — EYES-FREE VOICE RECORDER

Shared instructions for every coding agent working in this repo (Antigravity, Claude Code local, Claude Code on the web, Copilot).
`CLAUDE.md` imports this file, and `.agents/rules/` repeats the workflow rules for Antigravity. **Edit this file, not the copies.**

Owner: JY (jonlee0704@gmail.com) · S&D A11y Studio. The owner authorizes the agents he runs to read and edit this repo for his own work. The AI notice in README/LICENSE still applies to everyone else: never copy this code into other projects.

---

## 1. Multi-agent workflow (mandatory)

Several agents work on this repo in parallel. GitHub is the only shared state.

1. **Branch per task, prefixed by who made it.** Never commit straight to `master`.
   - `ag/<topic>` = Antigravity · `claude/<topic>` = Claude Code (local or web) · `jy/<topic>` = JY by hand
   - Never push to another agent's branch. If you need its work, branch from it or wait for the merge.
2. **Sync first.** Before starting, run `git fetch origin && git switch master && git pull --ff-only`, then branch.
3. **Push at the end of every session**, even work in progress. Unpushed work is invisible to the other agents.
4. **Merge only by PR into `master`.** JY reviews and merges. Keep PRs small: one feature or fix, merged within 1–2 days.
5. **After a merge**, the other agents rebase their open branches onto `master` (`git pull --rebase origin master`).
6. **Same machine:** Claude Code local uses a separate worktree (`git worktree add ../eyes-free-claude claude/<topic>`). It never edits the folder Antigravity has open.
7. **Commit messages:** use conventional prefixes (`feat:`, `fix:`, `docs:`, `refactor:`, `test:`, `build:`) in the imperative mood, and keep the subject line to 72 characters or fewer.

## 2. Source of truth for product decisions

- **Spec:** `docs/PRD-ProRecordingMode.md` (full PRD, including A2 Pro Recording Mode). Read it before changing any gesture, spoken text or recording behavior.
- **Gesture changes** must update all of these in the same PR: code, `README.md` gesture table, `docs/BLIND_USER_MANUAL.md`, the in-app gesture guide and tutorial, and the PRD.
- **Privacy or permission changes** must update `docs/PRIVACY_POLICY.md`, `docs/privacy.html` and `docs/PLAY_STORE_DATA_SAFETY.md` in the same PR.

## 3. Build layout — read this first, it is a trap

| What | Active path (used by Gradle) | Stale copy (DO NOT EDIT) |
|---|---|---|
| Java sources | `src/main/java/` (repo root) | `app/src/main/java/` |
| Resources | `src/main/res/` (repo root) | — |
| Manifest | `app/src/main/AndroidManifest.xml` | `src/main/AndroidManifest.xml` (has drifted; missing `launchMode="singleTask"` and a theme) |

`app/build.gradle` sets `sourceSets.main.java.srcDirs = ['../src/main/java']` and `res.srcDirs = ['../src/main/res']`, but it leaves the manifest at the module default. If you edit a stale file, your change silently does nothing.

- Package / applicationId: `com.jonlee.android.SJplayer`. **Never change it**, because Play can't change it.
- Version lives in `app/build.gradle` (`versionCode` / `versionName`). Bump both for every Play upload. The current version is code 97 / 4.2.0. **Code 95 was rejected** by Play (crash on launch).
- Toolchain: AGP 9.1.0, Gradle 9.3.1 wrapper, compileSdk / targetSdk 36, minSdk 26, Java 11.

### Commands
```bash
./gradlew :app:assembleDebug          # debug APK
./gradlew :app:installDebug           # install on connected device or emulator
./gradlew :app:bundleRelease          # Play AAB (needs RELEASE_* signing props in ~/.gradle/gradle.properties)
./gradlew :app:lint                   # lint (abortOnError is off — read the report)
```
Unit tests live in the repo-root `src/test/java/` (mirroring `src/main/java/`). PR #3 (Pro Recording Mode) adds the first ones and sets `sourceSets.test.java.srcDirs = ['../src/test/java']` in `app/build.gradle`; until it merges, add that line yourself if you add tests. Do **not** use `app/src/test/java/`. New logic that isn't UI (gesture classification, file naming, section seek, time formatting, DSP) should go into plain Java classes with JUnit tests, run with `./gradlew :app:testDebugUnitTest`.

## 4. Code map (src/main/java/com/jonlee/android/SJplayer/)

- `MainActivity.java`: the main screen, wiring, TTS and the ToS gate.
- `DialView.java` + `DialOnTouchListener.java`: **the active gesture path**. 1 tap = play/stop, 2 taps = record, 3 taps = bookmark; this also covers multi-finger swipes and centroid tracking.
- `GestureListener.java` / `BasicGestureDetectFragment.java`: older gesture code. Check whether it's actually reached before editing it.
- `Recorder.java`: `MediaRecorder`, AAC `.m4a`. Pro Recording Mode (PRD A2) will extend this.
- `MediaPlayer.java`, `MusicRetriever.java`, `MediaFolder.java`, `SJFile.java`: playback and the library.
- `SoundscapeHelper.java`: synthesized earcons. `SJAssist.java`: helpers.
- `LocationHelper.java`: geotagging. The `pref_save_location` setting **currently defaults to true**, and the PRD wants it off by default.
- `BackupRecorder.java`, `DriveMainActivity.java`, `SJBackupFileAdapter.java`: Google Drive backup.
- `SettingsActivity.java`: preferences.

## 5. Accessibility rules (non-negotiable — this app is for blind users)

- Every user action gets **spoken feedback (TTS) and a haptic**. Silent failures are bugs.
- New speech interrupts stale speech. Never queue a backlog of announcements.
- No action may depend on seeing the screen. No small tap targets and no visual-only state.
- Destructive actions (delete) must be confirmable and recoverable. Delete currently moves the file to trash.
- Known gaps (see the PRD's "Known issues"): there's no TalkBack detection or coexistence yet, and no foreground service, so long recordings can be killed in the background.
- Recording must never stop silently. If it is interrupted, announce it.

## 6. Never commit

`google-services.json`, keystores (`*.jks`, `*.keystore`, `*.pepk`, `*.pem`), `local.properties`, signing passwords, APKs or AABs, or the `/release` folder. All of these are already in `.gitignore`. Do not force-add them. An API key for this project was publicly exposed once (Sep 30), so treat any key that shows up in a diff as an incident.

## 7. Definition of done for a PR

- [ ] `./gradlew :app:assembleDebug` succeeds; any new unit tests pass
- [ ] Tested on a real device with TalkBack **off** (the app's own speech) and noted in the PR what you tested
- [ ] Docs updated per section 2
- [ ] No secrets, no edits to stale paths from section 3
- [ ] `versionCode` bumped only when JY asks for a release build
