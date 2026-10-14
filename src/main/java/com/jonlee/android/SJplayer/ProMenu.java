package com.jonlee.android.SJplayer;

/**
 * Spoken Pro Recording menu (PRD A2). A 3-finger double tap opens it; every Pro setting is
 * reachable from it without looking at the screen:
 *
 *   1-finger swipe right / left : next / previous item
 *   1-finger swipe up / down    : change the item's value
 *   1-finger tap                : select (toggle, run the sound check, load or save a preset, close)
 *   1-finger long press         : item help; on Presets, save the current settings into the slot
 *   1-finger double tap         : close the menu and start recording
 *   3-finger double tap         : close the menu
 *
 * The menu only decides what changes and what to say. {@link Commander} maps gestures onto it
 * and plays the haptics and earcons for each {@link Result}.
 */
public class ProMenu {

    public enum Item {
        PRO_MODE("Pro mode"),
        ENVIRONMENT("Environment"),
        FOCUS("Focus"),
        NOISE_REDUCTION("Noise reduction"),
        GAIN_MODE("Gain mode"),
        INPUT_GAIN("Input gain"),
        SOUND_CHECK("Sound check"),
        PRESETS("Presets"),
        KEEP_ORIGINAL("Keep original"),
        MONITORING("Headphone monitoring"),
        CLOSE("Close menu");

        public final String spokenName;

        Item(String spokenName) {
            this.spokenName = spokenName;
        }
    }

    /** What Commander should do after a menu gesture, besides speaking. */
    public enum Action { NONE, RUN_SOUND_CHECK, CLOSE }

    /** How the gesture should feel. */
    public enum Feedback { MOVE, CHANGE, TOGGLE_ON, TOGGLE_OFF, BOUNDARY, SELECT }

    public static class Result {
        public final String speech;
        public final Feedback feedback;
        public final Action action;

        Result(String speech, Feedback feedback, Action action) {
            this.speech = speech;
            this.feedback = feedback;
            this.action = action;
        }

        static Result say(String speech, Feedback feedback) {
            return new Result(speech, feedback, Action.NONE);
        }
    }

    private final ProRecordingSettings settings;
    private boolean open = false;
    private int index = 0;
    private int presetSlot = 1;

    public ProMenu(ProRecordingSettings settings) {
        this.settings = settings;
    }

    public boolean isOpen() {
        return open;
    }

    public Item current() {
        return Item.values()[index];
    }

    /** Opens on the first item and returns the opening announcement. */
    public Result open() {
        open = true;
        index = 0;
        return Result.say("Pro menu. Swipe right or left for items, up or down to change, tap to select. "
                + describeCurrent(), Feedback.SELECT);
    }

    public Result close() {
        open = false;
        return new Result("Pro menu closed. " + (settings.isEnabled() ? "Pro mode on." : "Pro mode off."),
                Feedback.SELECT, Action.CLOSE);
    }

    /** Closes without an announcement, e.g. when recording starts. */
    public void dismiss() {
        open = false;
    }

    public Result move(int step) {
        int next = index + step;
        if (next < 0 || next >= Item.values().length) {
            return Result.say((step > 0 ? "Last item. " : "First item. ") + describeCurrent(), Feedback.BOUNDARY);
        }
        index = next;
        return Result.say(describeCurrent(), Feedback.MOVE);
    }

    /** "Environment, Meeting room. 2 of 11" */
    public String describeCurrent() {
        Item item = current();
        return item.spokenName + ", " + valueOf(item) + ". " + (index + 1) + " of " + Item.values().length;
    }

    private String valueOf(Item item) {
        switch (item) {
            case PRO_MODE: return settings.isEnabled() ? "on" : "off";
            case ENVIRONMENT: return settings.getEnvironment().spokenName;
            case FOCUS: return settings.getFocus().spokenName;
            case NOISE_REDUCTION:
                int nr = settings.getEffectiveNoiseReduction();
                return ProRecordingSettings.noiseReductionName(nr) + ", " + nr + " percent";
            case GAIN_MODE: return settings.getGainMode().spokenName;
            case INPUT_GAIN:
                return settings.getGainMode().isManual()
                        ? ProRecordingSettings.formatDb(settings.getGainDb())
                        : "automatic";
            case SOUND_CHECK: return "tap to start";
            case PRESETS: return settings.describePreset(presetSlot);
            case KEEP_ORIGINAL: return settings.isKeepOriginal() ? "on" : "off";
            case MONITORING: return settings.isMonitoringEnabled() ? "on" : "off";
            case CLOSE:
            default: return "tap to close";
        }
    }

    /** Swipe up (step = 1) or down (step = -1): change the current item's value. */
    public Result change(int step) {
        Item item = current();
        switch (item) {
            case PRO_MODE:
                return setProMode(step > 0);
            case ENVIRONMENT:
                settings.cycleEnvironment(step);
                return Result.say(settings.describeEnvironmentChange(), Feedback.CHANGE);
            case FOCUS:
                ProRecordingSettings.Focus focus = settings.cycleFocus(step);
                return Result.say(focus.spokenName + " focus, noise reduction "
                        + settings.getEffectiveNoiseReduction() + " percent", Feedback.CHANGE);
            case NOISE_REDUCTION: {
                int before = settings.getEffectiveNoiseReduction();
                int level = settings.stepNoiseReduction(step);
                if (level == before) {
                    String limit = (step > 0 && settings.getFocus().maxNoiseReduction < 100)
                            ? settings.getFocus().spokenName + " focus is capped at " + settings.getFocus().maxNoiseReduction + " percent"
                            : "Noise reduction " + level + " percent";
                    return Result.say(limit, Feedback.BOUNDARY);
                }
                String msg = "Noise reduction " + level + " percent";
                if (level == 100 && settings.consumeMaxNoiseReductionWarning()) {
                    msg += ". At maximum, voices may sound thinner";
                }
                return Result.say(msg, Feedback.CHANGE);
            }
            case GAIN_MODE: {
                ProRecordingSettings.GainMode mode = settings.cycleGainMode(step);
                String msg = mode.spokenName;
                if (mode.isManual()) msg += ". Volume buttons set input gain";
                return Result.say(msg, Feedback.CHANGE);
            }
            case INPUT_GAIN: {
                if (!settings.getGainMode().isManual()) {
                    return Result.say("Gain is automatic. Choose a manual gain mode first.", Feedback.BOUNDARY);
                }
                int before = settings.getGainDb();
                int db = settings.stepGain(step);
                if (db == before) {
                    return Result.say("Input " + ProRecordingSettings.formatDb(db) + (step > 0 ? ", maximum" : ", minimum"),
                            Feedback.BOUNDARY);
                }
                return Result.say("Input " + ProRecordingSettings.formatDb(db), Feedback.CHANGE);
            }
            case PRESETS: {
                int next = presetSlot + (step > 0 ? 1 : -1);
                if (next < 1 || next > ProRecordingSettings.PRESET_SLOTS) {
                    return Result.say(settings.describePreset(presetSlot), Feedback.BOUNDARY);
                }
                presetSlot = next;
                return Result.say(settings.describePreset(presetSlot)
                        + (settings.hasPreset(presetSlot) ? ". Tap to load" : ". Tap to save the current settings here"),
                        Feedback.CHANGE);
            }
            case KEEP_ORIGINAL:
                settings.setKeepOriginal(step > 0);
                return Result.say("Keep original " + (step > 0 ? "on" : "off"), step > 0 ? Feedback.TOGGLE_ON : Feedback.TOGGLE_OFF);
            case MONITORING:
                settings.setMonitoringEnabled(step > 0);
                return Result.say("Headphone monitoring " + (step > 0 ? "on" : "off"), step > 0 ? Feedback.TOGGLE_ON : Feedback.TOGGLE_OFF);
            case SOUND_CHECK:
                return Result.say("Tap to start the sound check.", Feedback.BOUNDARY);
            case CLOSE:
            default:
                return Result.say("Tap to close the menu.", Feedback.BOUNDARY);
        }
    }

    /** Single tap: select the current item. */
    public Result select() {
        Item item = current();
        switch (item) {
            case PRO_MODE:
                return setProMode(!settings.isEnabled());
            case ENVIRONMENT:
            case FOCUS:
            case GAIN_MODE:
                return change(1);
            case NOISE_REDUCTION:
            case INPUT_GAIN:
                return Result.say(describeCurrent() + ". Swipe up or down to change.", Feedback.SELECT);
            case SOUND_CHECK:
                return new Result(null, Feedback.SELECT, Action.RUN_SOUND_CHECK);
            case PRESETS:
                if (settings.hasPreset(presetSlot)) {
                    settings.loadPreset(presetSlot);
                    return Result.say("Loaded. " + settings.describePreset(presetSlot), Feedback.SELECT);
                }
                return savePreset();
            case KEEP_ORIGINAL:
                return change(settings.isKeepOriginal() ? -1 : 1);
            case MONITORING:
                return change(settings.isMonitoringEnabled() ? -1 : 1);
            case CLOSE:
            default:
                return close();
        }
    }

    /** Long press: save on Presets, otherwise explain the current item. */
    public Result hold() {
        Item item = current();
        if (item == Item.PRESETS) return savePreset();
        return Result.say(helpFor(item), Feedback.SELECT);
    }

    private Result savePreset() {
        settings.savePreset(presetSlot);
        return Result.say("Saved. " + settings.describePreset(presetSlot), Feedback.SELECT);
    }

    private Result setProMode(boolean on) {
        settings.setEnabled(on);
        if (!on) return Result.say("Pro mode off", Feedback.TOGGLE_OFF);
        String msg = "Pro mode on. " + settings.describe() + ".";
        if (settings.getGainMode().isManual()) msg += " Volume buttons set input gain.";
        return Result.say(msg, Feedback.TOGGLE_ON);
    }

    static String helpFor(Item item) {
        switch (item) {
            case PRO_MODE: return "Pro mode records with the settings in this menu instead of the standard recorder.";
            case ENVIRONMENT: return "Where you are. Sets the low-cut filter, echo control and wind reduction, and loads a default focus and gain mode.";
            case FOCUS: return "What to keep. Voice keeps speech, Music keeps instruments and removes only steady noise, Natural keeps almost everything.";
            case NOISE_REDUCTION: return "How much of the unwanted sound is removed, from off to maximum. While recording, a two-finger swipe up or down changes it live.";
            case GAIN_MODE: return "Auto lets the phone set the level. Manual locks the level and records 24-bit WAV. Manual with safety track also saves a copy 12 dB lower.";
            case INPUT_GAIN: return "Input level in manual gain modes, in 3 dB steps from minus 24 to plus 12. The volume buttons change it too.";
            case SOUND_CHECK: return "Listens for 5 seconds and recommends an input level. Tap again after the result to apply it.";
            case PRESETS: return "Three slots for your own combinations. Swipe up or down to choose a slot, tap to load it, or long press to save the current settings into it.";
            case KEEP_ORIGINAL: return "Also saves the unprocessed recording whenever noise reduction is on, so nothing is lost.";
            case MONITORING: return "Plays what is being recorded through headphones, so you hear the effect of each setting.";
            case CLOSE:
            default: return "Closes the Pro menu. A three-finger double tap closes it too.";
        }
    }
}
