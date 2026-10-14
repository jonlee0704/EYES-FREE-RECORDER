package com.jonlee.android.SJplayer;

/**
 * Pure helper for resolving speech dispatch rules between app-internal TTS
 * and system screen readers (TalkBack) to prevent double-speaking collision bugs.
 */
public final class TalkBackCoexistenceHelper {

    private TalkBackCoexistenceHelper() {}

    public static final class SpeechDecision {
        public final boolean useInternalTts;
        public final boolean useAccessibilityAnnouncement;

        public SpeechDecision(boolean useInternalTts, boolean useAccessibilityAnnouncement) {
            this.useInternalTts = useInternalTts;
            this.useAccessibilityAnnouncement = useAccessibilityAnnouncement;
        }
    }

    public static SpeechDecision decide(String talkbackMode, boolean isTtsGloballyEnabled, boolean isTalkBackActive) {
        boolean useAccessibilityAnnouncement = true;
        boolean useInternalTts = isTtsGloballyEnabled;

        if ("auto".equals(talkbackMode) || talkbackMode == null) {
            if (isTalkBackActive) {
                // TalkBack is active: let TalkBack handle speech via accessibility announcement; mute internal TTS to avoid double-speaking
                useInternalTts = false;
                useAccessibilityAnnouncement = true;
            }
        } else if ("internal_only".equals(talkbackMode)) {
            useAccessibilityAnnouncement = false;
        } // "both" leaves useInternalTts = isTtsGloballyEnabled and useAccessibilityAnnouncement = true

        return new SpeechDecision(useInternalTts, useAccessibilityAnnouncement);
    }
}
