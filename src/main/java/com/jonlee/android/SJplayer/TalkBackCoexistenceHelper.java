package com.jonlee.android.SJplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityManager;
import androidx.preference.PreferenceManager;

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

    public static boolean isTalkBackActive(Context context) {
        if (context == null) return false;
        try {
            AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
            return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean shouldSuppressSettingsSelfVoicing(Context context) {
        if (context == null) return false;
        try {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            String talkbackMode = sp.getString("pref_talkback_mode", "auto");
            boolean isTtsGloballyEnabled = sp.getBoolean("isTTSEnabled", true);
            boolean isTalkBackActive = isTalkBackActive(context);
            return shouldSuppressSettingsSelfVoicing(talkbackMode, isTtsGloballyEnabled, isTalkBackActive);
        } catch (Exception e) {
            return isTalkBackActive(context);
        }
    }

    public static boolean shouldSuppressSettingsSelfVoicing(String talkbackMode, boolean isTtsGloballyEnabled, boolean isTalkBackActive) {
        SpeechDecision decision = decide(talkbackMode, isTtsGloballyEnabled, isTalkBackActive);
        return !decision.useInternalTts;
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

