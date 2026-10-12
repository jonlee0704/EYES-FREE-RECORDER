# EYES-FREE Project Rules (summary — full version in AGENTS.md)

- BUILD TRAP: Java and resources are compiled from the repo-root `src/main/`. The manifest is `app/src/main/AndroidManifest.xml`. Never edit `app/src/main/java/` or `src/main/AndroidManifest.xml`; both are stale copies.
- Never change the applicationId `com.jonlee.android.SJplayer`.
- Every user action needs spoken feedback (TTS) and a haptic. Nothing may depend on seeing the screen.
- Gesture changes must update the code, the README gesture table, docs/BLIND_USER_MANUAL.md, the in-app guide, and docs/PRD-ProRecordingMode.md in the same PR.
- Privacy or permission changes must update docs/PRIVACY_POLICY.md, docs/privacy.html and docs/PLAY_STORE_DATA_SAFETY.md in the same PR.
- Read docs/PRD-ProRecordingMode.md before changing recording, gesture or speech behavior.
