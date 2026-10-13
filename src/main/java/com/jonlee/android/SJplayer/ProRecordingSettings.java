package com.jonlee.android.SJplayer;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import java.util.Locale;

/**
 * Pro Recording Mode settings (PRD section A2).
 *
 * Three independent settings: Environment (where you are), Focus (what to keep) and Gain
 * (how loud the input is), plus a noise-reduction amount. Every value persists in the
 * default SharedPreferences so Settings, gestures and the recorder all see the same state.
 */
public class ProRecordingSettings {

    public static final String PREF_PRO_MODE = "pref_pro_mode";
    public static final String PREF_ENVIRONMENT = "pref_pro_environment";
    public static final String PREF_FOCUS = "pref_pro_focus";
    public static final String PREF_NOISE_REDUCTION = "pref_pro_noise_reduction";
    public static final String PREF_GAIN_MODE = "pref_pro_gain_mode";
    public static final String PREF_GAIN_DB = "pref_pro_gain_db";
    public static final String PREF_KEEP_ORIGINAL = "pref_pro_keep_original";
    public static final String PREF_MONITORING = "pref_pro_monitoring";
    public static final String PREF_LIMITER_IN_MANUAL = "pref_pro_limiter_manual";
    public static final String PREF_MAX_NR_WARNED = "pref_pro_max_nr_warned";

    public static final int GAIN_MIN_DB = -24;
    public static final int GAIN_MAX_DB = 12;
    public static final int GAIN_STEP_DB = 3;
    public static final int SAFETY_TRACK_OFFSET_DB = -12;

    /** Five noise-reduction levels: Off, Light, Medium, Strong, Maximum. */
    public static final int[] NOISE_REDUCTION_LEVELS = {0, 25, 50, 75, 100};
    private static final String[] NOISE_REDUCTION_NAMES = {"Off", "Light", "Medium", "Strong", "Maximum"};

    public enum Environment {
        MEETING_ROOM("meeting_room", "Meeting room", 100f, true, false, Focus.VOICE, GainMode.AUTO),
        SMALL_VENUE("small_venue", "Small music venue", 30f, false, false, Focus.MUSIC, GainMode.MANUAL_SAFETY),
        STADIUM("stadium", "Stadium", 60f, false, false, Focus.NATURAL, GainMode.MANUAL_SAFETY),
        STREET("street", "Street", 120f, false, true, Focus.VOICE, GainMode.AUTO);

        public final String key;
        public final String spokenName;
        public final float lowCutHz;
        public final boolean echoControl;
        public final boolean windReduction;
        public final Focus defaultFocus;
        public final GainMode defaultGainMode;

        Environment(String key, String spokenName, float lowCutHz, boolean echoControl, boolean windReduction,
                    Focus defaultFocus, GainMode defaultGainMode) {
            this.key = key;
            this.spokenName = spokenName;
            this.lowCutHz = lowCutHz;
            this.echoControl = echoControl;
            this.windReduction = windReduction;
            this.defaultFocus = defaultFocus;
            this.defaultGainMode = defaultGainMode;
        }

        public static Environment fromKey(String key) {
            for (Environment e : values()) {
                if (e.key.equals(key)) return e;
            }
            return MEETING_ROOM;
        }
    }

    public enum Focus {
        VOICE("voice", "Voice", 75, 100),
        MUSIC("music", "Music", 25, 100),
        NATURAL("natural", "Natural", 25, 50);

        public final String key;
        public final String spokenName;
        public final int defaultNoiseReduction;
        /** Natural focus caps noise reduction at 50%. */
        public final int maxNoiseReduction;

        Focus(String key, String spokenName, int defaultNoiseReduction, int maxNoiseReduction) {
            this.key = key;
            this.spokenName = spokenName;
            this.defaultNoiseReduction = defaultNoiseReduction;
            this.maxNoiseReduction = maxNoiseReduction;
        }

        public static Focus fromKey(String key) {
            for (Focus f : values()) {
                if (f.key.equals(key)) return f;
            }
            return VOICE;
        }
    }

    public enum GainMode {
        AUTO("auto", "gain auto"),
        MANUAL("manual", "manual gain"),
        MANUAL_SAFETY("manual_safety", "manual gain with safety track");

        public final String key;
        public final String spokenName;

        GainMode(String key, String spokenName) {
            this.key = key;
            this.spokenName = spokenName;
        }

        public boolean isManual() {
            return this != AUTO;
        }

        public static GainMode fromKey(String key) {
            for (GainMode g : values()) {
                if (g.key.equals(key)) return g;
            }
            return AUTO;
        }
    }

    private final SharedPreferences prefs;

    public ProRecordingSettings(Context context) {
        this.prefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /** For tests: settings backed by any SharedPreferences. */
    ProRecordingSettings(SharedPreferences prefs) {
        this.prefs = prefs;
    }

    public static ProRecordingSettings get(Context context) {
        return new ProRecordingSettings(context);
    }

    public boolean isEnabled() {
        return prefs.getBoolean(PREF_PRO_MODE, false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_PRO_MODE, enabled).apply();
    }

    public Environment getEnvironment() {
        return Environment.fromKey(prefs.getString(PREF_ENVIRONMENT, Environment.MEETING_ROOM.key));
    }

    /** Selects an environment and loads its default focus, noise reduction and gain mode. */
    public void setEnvironment(Environment env) {
        prefs.edit()
                .putString(PREF_ENVIRONMENT, env.key)
                .putString(PREF_FOCUS, env.defaultFocus.key)
                .putString(PREF_NOISE_REDUCTION, String.valueOf(env.defaultFocus.defaultNoiseReduction))
                .putString(PREF_GAIN_MODE, env.defaultGainMode.key)
                .apply();
    }

    public Focus getFocus() {
        return Focus.fromKey(prefs.getString(PREF_FOCUS, getEnvironment().defaultFocus.key));
    }

    /** Selects a focus and resets noise reduction to that focus's default. */
    public void setFocus(Focus focus) {
        prefs.edit()
                .putString(PREF_FOCUS, focus.key)
                .putString(PREF_NOISE_REDUCTION, String.valueOf(focus.defaultNoiseReduction))
                .apply();
    }

    public int getNoiseReduction() {
        int level;
        try {
            level = Integer.parseInt(prefs.getString(PREF_NOISE_REDUCTION, String.valueOf(getFocus().defaultNoiseReduction)));
        } catch (NumberFormatException e) {
            level = getFocus().defaultNoiseReduction;
        }
        return snapNoiseReduction(level);
    }

    public void setNoiseReduction(int level) {
        prefs.edit().putString(PREF_NOISE_REDUCTION, String.valueOf(snapNoiseReduction(level))).apply();
    }

    /** Noise reduction actually applied, after the focus cap (Natural is capped at 50%). */
    public int getEffectiveNoiseReduction() {
        return Math.min(getNoiseReduction(), getFocus().maxNoiseReduction);
    }

    public GainMode getGainMode() {
        return GainMode.fromKey(prefs.getString(PREF_GAIN_MODE, getEnvironment().defaultGainMode.key));
    }

    public void setGainMode(GainMode mode) {
        prefs.edit().putString(PREF_GAIN_MODE, mode.key).apply();
    }

    public int getGainDb() {
        int db;
        try {
            db = Integer.parseInt(prefs.getString(PREF_GAIN_DB, "0"));
        } catch (NumberFormatException e) {
            db = 0;
        }
        return clampGain(db);
    }

    public void setGainDb(int db) {
        prefs.edit().putString(PREF_GAIN_DB, String.valueOf(clampGain(db))).apply();
    }

    public boolean isKeepOriginal() {
        return prefs.getBoolean(PREF_KEEP_ORIGINAL, true);
    }

    public boolean isMonitoringEnabled() {
        return prefs.getBoolean(PREF_MONITORING, true);
    }

    public boolean isLimiterInManual() {
        return prefs.getBoolean(PREF_LIMITER_IN_MANUAL, false);
    }

    /** Returns true the first time Maximum noise reduction is chosen, so the app warns only once. */
    public boolean consumeMaxNoiseReductionWarning() {
        if (prefs.getBoolean(PREF_MAX_NR_WARNED, false)) return false;
        prefs.edit().putBoolean(PREF_MAX_NR_WARNED, true).apply();
        return true;
    }

    // --- Custom presets (PR13) ---

    public static final int PRESET_SLOTS = 3;
    private static final String PREF_PRESET_PREFIX = "pref_pro_preset_";

    /** True if the preset slot (1-based) holds a saved combination. */
    public boolean hasPreset(int slot) {
        return prefs.getString(PREF_PRESET_PREFIX + slot, null) != null;
    }

    /** Saves the current environment, focus, noise reduction and gain into a slot (1-based). */
    public void savePreset(int slot) {
        String value = getEnvironment().key + "|" + getFocus().key + "|" + getNoiseReduction()
                + "|" + getGainMode().key + "|" + getGainDb();
        prefs.edit().putString(PREF_PRESET_PREFIX + slot, value).apply();
    }

    /** Loads a preset slot (1-based). Returns false if the slot is empty or unreadable. */
    public boolean loadPreset(int slot) {
        String value = prefs.getString(PREF_PRESET_PREFIX + slot, null);
        if (value == null) return false;
        String[] parts = value.split("\\|");
        if (parts.length != 5) return false;
        try {
            prefs.edit()
                    .putString(PREF_ENVIRONMENT, Environment.fromKey(parts[0]).key)
                    .putString(PREF_FOCUS, Focus.fromKey(parts[1]).key)
                    .putString(PREF_NOISE_REDUCTION, String.valueOf(snapNoiseReduction(Integer.parseInt(parts[2]))))
                    .putString(PREF_GAIN_MODE, GainMode.fromKey(parts[3]).key)
                    .putString(PREF_GAIN_DB, String.valueOf(clampGain(Integer.parseInt(parts[4]))))
                    .apply();
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /** e.g. "Preset 2: Small music venue, Music focus, noise reduction 25 percent, manual gain minus 6 dB" */
    public String describePreset(int slot) {
        String value = prefs.getString(PREF_PRESET_PREFIX + slot, null);
        if (value == null) return "Preset " + slot + ", empty";
        String[] parts = value.split("\\|");
        if (parts.length != 5) return "Preset " + slot + ", empty";
        GainMode mode = GainMode.fromKey(parts[3]);
        String gain;
        try {
            gain = mode.isManual() ? mode.spokenName + " " + formatDb(clampGain(Integer.parseInt(parts[4]))) : mode.spokenName;
        } catch (NumberFormatException e) {
            gain = mode.spokenName;
        }
        Focus focus = Focus.fromKey(parts[1]);
        int nr;
        try {
            nr = Math.min(snapNoiseReduction(Integer.parseInt(parts[2])), focus.maxNoiseReduction);
        } catch (NumberFormatException e) {
            nr = focus.defaultNoiseReduction;
        }
        return "Preset " + slot + ": " + Environment.fromKey(parts[0]).spokenName + ", " + focus.spokenName
                + " focus, noise reduction " + nr + " percent, " + gain;
    }

    public void setKeepOriginal(boolean keep) {
        prefs.edit().putBoolean(PREF_KEEP_ORIGINAL, keep).apply();
    }

    public void setMonitoringEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_MONITORING, enabled).apply();
    }

    public GainMode cycleGainMode(int step) {
        GainMode[] all = GainMode.values();
        GainMode next = all[Math.floorMod(getGainMode().ordinal() + step, all.length)];
        setGainMode(next);
        return next;
    }

    // --- Cycling helpers used by gestures ---

    public Environment cycleEnvironment(int step) {
        Environment[] all = Environment.values();
        Environment next = all[Math.floorMod(getEnvironment().ordinal() + step, all.length)];
        setEnvironment(next);
        return next;
    }

    public Focus cycleFocus(int step) {
        Focus[] all = Focus.values();
        Focus next = all[Math.floorMod(getFocus().ordinal() + step, all.length)];
        setFocus(next);
        return next;
    }

    /** Moves noise reduction one level up or down, respecting the focus cap. Returns the new level. */
    public int stepNoiseReduction(int step) {
        int idx = indexOfNoiseReduction(getNoiseReduction());
        int newIdx = Math.max(0, Math.min(NOISE_REDUCTION_LEVELS.length - 1, idx + step));
        int level = Math.min(NOISE_REDUCTION_LEVELS[newIdx], getFocus().maxNoiseReduction);
        setNoiseReduction(level);
        return level;
    }

    /** Moves the input gain by one 3 dB step. Returns the new gain. */
    public int stepGain(int step) {
        int db = clampGain(getGainDb() + step * GAIN_STEP_DB);
        setGainDb(db);
        return db;
    }

    // --- Spoken text ---

    /** e.g. "Meeting room, Voice focus, noise reduction 75 percent, gain auto" */
    public String describe() {
        return getEnvironment().spokenName + ", " + getFocus().spokenName + " focus, noise reduction "
                + getEffectiveNoiseReduction() + " percent, " + describeGain();
    }

    public String describeGain() {
        GainMode mode = getGainMode();
        if (!mode.isManual()) return mode.spokenName;
        return mode.spokenName + " " + formatDb(getGainDb());
    }

    /** Speaks an environment change: the environment plus the defaults it loads. */
    public String describeEnvironmentChange() {
        Environment env = getEnvironment();
        return env.spokenName + ". " + getFocus().spokenName + " focus, noise reduction "
                + getEffectiveNoiseReduction() + " percent, " + describeGain()
                + ", low cut " + Math.round(env.lowCutHz) + " hertz"
                + (env.echoControl ? ", echo control on" : "")
                + (env.windReduction ? ", wind reduction on" : "");
    }

    public static String noiseReductionName(int level) {
        return NOISE_REDUCTION_NAMES[indexOfNoiseReduction(level)];
    }

    /** "minus 9 dB", "plus 3 dB", "0 dB" for speech. */
    public static String formatDb(int db) {
        if (db == 0) return "0 dB";
        return (db < 0 ? "minus " : "plus ") + Math.abs(db) + " dB";
    }

    public static String formatDb(float db) {
        int rounded = Math.round(db);
        if (rounded == 0) return "0 dB";
        return String.format(Locale.US, "%s%d dB", rounded < 0 ? "minus " : "plus ", Math.abs(rounded));
    }

    static int snapNoiseReduction(int level) {
        return NOISE_REDUCTION_LEVELS[indexOfNoiseReduction(level)];
    }

    static int indexOfNoiseReduction(int level) {
        int best = 0;
        for (int i = 0; i < NOISE_REDUCTION_LEVELS.length; i++) {
            if (Math.abs(NOISE_REDUCTION_LEVELS[i] - level) < Math.abs(NOISE_REDUCTION_LEVELS[best] - level)) {
                best = i;
            }
        }
        return best;
    }

    static int clampGain(int db) {
        int clamped = Math.max(GAIN_MIN_DB, Math.min(GAIN_MAX_DB, db));
        // Snap to the 3 dB grid
        return Math.round(clamped / (float) GAIN_STEP_DB) * GAIN_STEP_DB;
    }
}
