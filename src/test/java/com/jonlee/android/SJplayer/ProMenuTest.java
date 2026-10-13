package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/** JVM tests for the spoken Pro menu and Pro presets (PRD A2, PR13). */
public class ProMenuTest {

    private ProRecordingSettings settings;
    private ProMenu menu;

    @Before
    public void setUp() {
        settings = new ProRecordingSettings(new FakeSharedPreferences());
        menu = new ProMenu(settings);
    }

    private void moveTo(ProMenu.Item item) {
        while (menu.current() != item) menu.move(1);
    }

    @Test
    public void opensOnProModeAndStopsAtBothEnds() {
        ProMenu.Result r = menu.open();
        assertTrue(menu.isOpen());
        assertEquals(ProMenu.Item.PRO_MODE, menu.current());
        assertTrue(r.speech.contains("Pro mode, off. 1 of 11"));
        assertEquals(ProMenu.Feedback.BOUNDARY, menu.move(-1).feedback);
        moveTo(ProMenu.Item.CLOSE);
        assertEquals(ProMenu.Feedback.BOUNDARY, menu.move(1).feedback);
        assertEquals(ProMenu.Item.CLOSE, menu.current());
    }

    @Test
    public void tapOnProModeTurnsItOnAndSpeaksTheSettings() {
        menu.open();
        ProMenu.Result r = menu.select();
        assertTrue(settings.isEnabled());
        assertEquals(ProMenu.Feedback.TOGGLE_ON, r.feedback);
        assertEquals("Pro mode on. Meeting room, Voice focus, noise reduction 75 percent, gain auto.", r.speech);
    }

    @Test
    public void environmentChangeLoadsItsDefaults() {
        menu.open();
        moveTo(ProMenu.Item.ENVIRONMENT);
        menu.change(1);
        assertEquals(ProRecordingSettings.Environment.SMALL_VENUE, settings.getEnvironment());
        assertEquals(ProRecordingSettings.Focus.MUSIC, settings.getFocus());
        assertEquals(ProRecordingSettings.GainMode.MANUAL_SAFETY, settings.getGainMode());
        assertEquals(25, settings.getEffectiveNoiseReduction());
    }

    @Test
    public void inputGainNeedsAManualMode() {
        menu.open();
        moveTo(ProMenu.Item.INPUT_GAIN);
        ProMenu.Result r = menu.change(1);
        assertEquals(ProMenu.Feedback.BOUNDARY, r.feedback);
        assertEquals(0, settings.getGainDb());

        settings.setGainMode(ProRecordingSettings.GainMode.MANUAL);
        menu.change(-1);
        assertEquals(-3, settings.getGainDb());
    }

    @Test
    public void naturalFocusIsCappedAtFiftyPercent() {
        settings.setFocus(ProRecordingSettings.Focus.NATURAL);
        menu.open();
        moveTo(ProMenu.Item.NOISE_REDUCTION);
        menu.change(1);   // 25 -> 50
        ProMenu.Result r = menu.change(1);
        assertEquals(ProMenu.Feedback.BOUNDARY, r.feedback);
        assertTrue(r.speech.contains("capped at 50 percent"));
        assertEquals(50, settings.getEffectiveNoiseReduction());
    }

    @Test
    public void presetsSaveAndLoadEverySetting() {
        settings.setEnvironment(ProRecordingSettings.Environment.STADIUM);
        settings.setGainDb(-9);
        menu.open();
        moveTo(ProMenu.Item.PRESETS);
        menu.change(1);   // slot 2
        assertFalse(settings.hasPreset(2));
        menu.select();    // empty slot: save
        assertTrue(settings.hasPreset(2));

        settings.setEnvironment(ProRecordingSettings.Environment.MEETING_ROOM);
        settings.setGainDb(6);
        ProMenu.Result r = menu.select();   // filled slot: load
        assertTrue(r.speech.startsWith("Loaded. Preset 2: Stadium, Natural focus"));
        assertEquals(ProRecordingSettings.Environment.STADIUM, settings.getEnvironment());
        assertEquals(ProRecordingSettings.Focus.NATURAL, settings.getFocus());
        assertEquals(ProRecordingSettings.GainMode.MANUAL_SAFETY, settings.getGainMode());
        assertEquals(-9, settings.getGainDb());
    }

    @Test
    public void soundCheckAndCloseAreActions() {
        menu.open();
        moveTo(ProMenu.Item.SOUND_CHECK);
        assertEquals(ProMenu.Action.RUN_SOUND_CHECK, menu.select().action);
        moveTo(ProMenu.Item.CLOSE);
        assertEquals(ProMenu.Action.CLOSE, menu.select().action);
        assertFalse(menu.isOpen());
    }
}
