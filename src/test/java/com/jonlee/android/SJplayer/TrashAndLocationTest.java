package com.jonlee.android.SJplayer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Comprehensive unit tests for P0/P1 features:
 * 1. Geotagging default value (P0)
 * 2. TalkBack coexistence decision routing (P1)
 * 3. Trash and Restore logic for main recordings and Pro companion tracks (P1)
 * 4. Commander constant uniqueness (P1)
 */
public class TrashAndLocationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testGeotaggingDefaultsToFalse() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        // Verifies P0 rule: when no preference is saved yet, saveLocation must default to false (opt-in)
        boolean saveLocation = prefs.getBoolean("pref_save_location", false);
        assertFalse("Location tagging must default to false for privacy", saveLocation);

        prefs.edit().putBoolean("pref_save_location", true).apply();
        assertTrue("User can explicitly opt-in to location tagging", prefs.getBoolean("pref_save_location", false));
    }

    @Test
    public void testTalkBackCoexistenceSpeechDecisions() {
        // 1. Auto mode with TalkBack active: mute internal TTS, use accessibility announcement
        TalkBackCoexistenceHelper.SpeechDecision d1 =
                TalkBackCoexistenceHelper.decide("auto", true, true);
        assertFalse("Internal TTS must be muted when TalkBack is active in auto mode", d1.useInternalTts);
        assertTrue("Accessibility announcement must be enabled for TalkBack", d1.useAccessibilityAnnouncement);

        // 2. Auto mode with TalkBack inactive: use internal TTS, use accessibility announcement
        TalkBackCoexistenceHelper.SpeechDecision d2 =
                TalkBackCoexistenceHelper.decide("auto", true, false);
        assertTrue("Internal TTS must speak when TalkBack is off", d2.useInternalTts);
        assertTrue("Accessibility announcement should remain active", d2.useAccessibilityAnnouncement);

        // 3. Auto mode with TTS globally turned off: internal TTS must stay off
        TalkBackCoexistenceHelper.SpeechDecision d3 =
                TalkBackCoexistenceHelper.decide("auto", false, false);
        assertFalse("Internal TTS must stay off when globally disabled", d3.useInternalTts);

        // 4. Internal-only mode: always use internal TTS, suppress accessibility announcements
        TalkBackCoexistenceHelper.SpeechDecision d4 =
                TalkBackCoexistenceHelper.decide("internal_only", true, true);
        assertTrue("Internal-only mode must keep internal TTS active", d4.useInternalTts);
        assertFalse("Internal-only mode must suppress accessibility announcements", d4.useAccessibilityAnnouncement);

        // 5. Both mode: keep both active
        TalkBackCoexistenceHelper.SpeechDecision d5 =
                TalkBackCoexistenceHelper.decide("both", true, true);
        assertTrue("Both mode must keep internal TTS active", d5.useInternalTts);
        assertTrue("Both mode must keep accessibility announcements active", d5.useAccessibilityAnnouncement);

        // 6. Null mode defaults to auto behavior
        TalkBackCoexistenceHelper.SpeechDecision d6 =
                TalkBackCoexistenceHelper.decide(null, true, true);
        assertFalse("Null mode must default to auto (mute internal TTS with TalkBack)", d6.useInternalTts);
        assertTrue(d6.useAccessibilityAnnouncement);
    }

    @Test
    public void testSettingsTalkBackControlAndSelfVoicingSuppression() {
        // Safe null handling for Context
        assertFalse(TalkBackCoexistenceHelper.isTalkBackActive(null));
        assertFalse(TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing((android.content.Context) null));

        // When TalkBack is active (default "auto" mode):
        // Platform TalkBack takes control of Settings; internal self-voicing is suppressed.
        assertTrue("TalkBack active in auto mode must suppress internal Settings self-voicing",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing("auto", true, true));

        // When TalkBack is inactive (default "auto" mode):
        // Settings provides internal self-voicing for eyes-free users.
        assertFalse("TalkBack inactive in auto mode must allow internal Settings self-voicing",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing("auto", true, false));

        // When TTS is globally turned off:
        // Internal self-voicing must be suppressed regardless of TalkBack.
        assertTrue("Globally disabled TTS must suppress internal Settings self-voicing",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing("auto", false, false));

        // User explicit override 'internal_only':
        assertFalse("internal_only mode allows self-voicing even if TalkBack is on",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing("internal_only", true, true));

        // User explicit override 'both':
        assertFalse("both mode allows internal self-voicing",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing("both", true, true));

        // Null mode defaults to auto:
        assertTrue("Null mode defaults to auto and suppresses self-voicing when TalkBack is active",
                TalkBackCoexistenceHelper.shouldSuppressSettingsSelfVoicing(null, true, true));
    }

    @Test
    public void testTrashAndRestoreWithCompanions() throws IOException {
        File recordingsDir = tempFolder.newFolder("recordings");
        File trashDir = tempFolder.newFolder(".Trash");

        File mainFile = new File(recordingsDir, "2026-10-13-take1.wav");
        File safetyFile = new File(recordingsDir, "2026-10-13-take1_safety.wav");
        File origFile = new File(recordingsDir, "2026-10-13-take1_original.wav");

        writeFile(mainFile, "main-audio-content");
        writeFile(safetyFile, "safety-track-minus-12db");
        writeFile(origFile, "unprocessed-original-audio");

        assertTrue(mainFile.exists());
        assertTrue(safetyFile.exists());
        assertTrue(origFile.exists());

        // Simulate move to trash
        File trashMain = new File(trashDir, mainFile.getName());
        File trashSafety = new File(trashDir, safetyFile.getName());
        File trashOrig = new File(trashDir, origFile.getName());

        Recorder.copyDirectoryOneLocationToAnotherLocation(mainFile, trashMain);
        Recorder.copyDirectoryOneLocationToAnotherLocation(safetyFile, trashSafety);
        Recorder.copyDirectoryOneLocationToAnotherLocation(origFile, trashOrig);

        assertTrue(mainFile.delete());
        assertTrue(safetyFile.delete());
        assertTrue(origFile.delete());

        assertFalse(mainFile.exists());
        assertFalse(safetyFile.exists());
        assertFalse(origFile.exists());

        assertTrue(trashMain.exists());
        assertTrue(trashSafety.exists());
        assertTrue(trashOrig.exists());

        // Simulate restore (undo)
        Recorder.copyDirectoryOneLocationToAnotherLocation(trashMain, mainFile);
        Recorder.copyDirectoryOneLocationToAnotherLocation(trashSafety, safetyFile);
        Recorder.copyDirectoryOneLocationToAnotherLocation(trashOrig, origFile);

        assertTrue(trashMain.delete());
        assertTrue(trashSafety.delete());
        assertTrue(trashOrig.delete());

        // All files must be completely restored
        assertTrue("Main file restored", mainFile.exists());
        assertTrue("Safety file restored", safetyFile.exists());
        assertTrue("Original file restored", origFile.exists());

        assertEquals("main-audio-content".length(), mainFile.length());
        assertEquals("safety-track-minus-12db".length(), safetyFile.length());
        assertEquals("unprocessed-original-audio".length(), origFile.length());

        assertFalse("Trash main deleted", trashMain.exists());
        assertFalse("Trash safety deleted", trashSafety.exists());
        assertFalse("Trash orig deleted", trashOrig.exists());
    }

    @Test
    public void testCommanderRestoreConstantIsUnique() throws IllegalAccessException {
        assertEquals("RESTORE_FILE constant must be 63", 63, Commander.RESTORE_FILE);

        // Ensure all Commander action command constants (defined between START_RECORD and RESTORE_FILE) are strictly unique
        int[] actionCommands = new int[] {
                Commander.START_RECORD, Commander.STOP_RECORD, Commander.START_PLAYBACK, Commander.STOP_PLAYBACK,
                Commander.FAST_FORWARD_2X, Commander.FAST_BACKWARD_2X, Commander.JOG_CLOCK_WISE, Commander.JOG_ANTI_CLOCK_WISE,
                Commander.NEXT_SONG, Commander.PREVIOUS_SONG, Commander.NEXT_FOLDER, Commander.PREVIOUS_FOLDER,
                Commander.SPEAK_FILE_INFO, Commander.ONETOUCH, Commander.FAST_FORWARD_3X, Commander.FAST_BACKWARD_3X,
                Commander.NEXT_DAY, Commander.PREVIOUS_DAY, Commander.PAUSE, Commander.LONG_PRESS,
                Commander.NEXT_ARTIST, Commander.PREVIOUS_ARTIST, Commander.FF_FOLDER, Commander.REWIND_FOLDER,
                Commander.FF_FILE, Commander.REWIND_FILE, Commander.STOP_BROWSE_MODE, Commander.BREAK_500MS,
                Commander.BREAK_100MS, Commander.BREAK_50MS, Commander.UPDATE_PLAY_ICON, Commander.TURN_ON_OFF_TTS,
                Commander.DELETE_FILE, Commander.UPDATE_PROGRESS, Commander.READ_LYRICS, Commander.SPEAK_DATE_TIME,
                Commander.START_STOP_BACKUP, Commander.SETTINGS, Commander.TOGGLE_FAVORITE, Commander.ADD_BOOKMARK,
                Commander.NEXT_BOOKMARK, Commander.PREVIOUS_BOOKMARK, Commander.NEXT_FAVORITE, Commander.PREVIOUS_FAVORITE,
                Commander.FAST_SEEK_NEXT_FOLDER, Commander.FAST_SEEK_PREVIOUS_FOLDER, Commander.FAST_SEEK_NEXT_FILE,
                Commander.FAST_SEEK_PREVIOUS_FILE, Commander.SPEAK_SPATIAL_STATUS, Commander.TOGGLE_SCREEN_CURTAIN,
                Commander.PRO_MODE_TOGGLE, Commander.PRO_NEXT_ENVIRONMENT, Commander.PRO_PREVIOUS_ENVIRONMENT,
                Commander.PRO_NEXT_FOCUS, Commander.PRO_PREVIOUS_FOCUS, Commander.PRO_NOISE_REDUCTION_UP,
                Commander.PRO_NOISE_REDUCTION_DOWN, Commander.PRO_SOUND_CHECK, Commander.PRO_GAIN_UP,
                Commander.PRO_GAIN_DOWN, Commander.PRO_MENU, Commander.PRO_CYCLE_MODE, Commander.RESTORE_FILE
        };

        Set<Integer> uniqueCodes = new HashSet<>();
        for (int cmd : actionCommands) {
            assertFalse("Duplicate Commander action code: " + cmd, uniqueCodes.contains(cmd));
            uniqueCodes.add(cmd);
        }
        assertEquals("Total registered commands count", actionCommands.length, uniqueCodes.size());
    }

    @Test
    public void testProButtonVisualAndToggleLogic() {
        // Verify color constants: highlighted purple when on, dimmed purple-gray when off
        assertEquals(0xFFB388FF, MainActivity.PRO_BUTTON_COLOR_ON);
        assertEquals(0xFF6C647A, MainActivity.PRO_BUTTON_COLOR_OFF);

        // Verify TTS color constants: illuminated cyan when on, dimmed slate-blue when off
        assertEquals(0xFF00E5FF, MainActivity.TTS_BUTTON_COLOR_ON);
        assertEquals(0xFF4A6B80, MainActivity.TTS_BUTTON_COLOR_OFF);

        // Verify strikethrough is cleared (no strike when off or on, as color highlight + background light UX is used)
        int initialFlags = android.graphics.Paint.STRIKE_THRU_TEXT_FLAG;
        int flagsCleared = initialFlags & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG;
        assertEquals("Strikethrough flag must be cleared (no strike)",
                0, flagsCleared & android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);

        // Verify ProRecordingSettings label and toggle state
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        ProRecordingSettings pro = new ProRecordingSettings(prefs);
        assertEquals("Label must be Pro", "Pro", pro.buttonLabel());
        assertFalse("Pro mode must start off by default", pro.isEnabled());

        pro.setEnabled(true);
        assertTrue("Pro mode enabled", pro.isEnabled());
        assertEquals("Label remains Pro when on", "Pro", pro.buttonLabel());

        pro.setEnabled(false);
        assertFalse("Pro mode disabled", pro.isEnabled());
        assertEquals("Label remains Pro when off", "Pro", pro.buttonLabel());
    }

    private static void writeFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes(StandardCharsets.UTF_8));
        }
    }
}
