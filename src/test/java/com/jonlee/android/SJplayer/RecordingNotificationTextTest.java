package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecordingNotificationTextTest {

    @Test
    public void underAMinute() {
        assertEquals("Recording, less than a minute", RecordingNotificationText.title(0, false));
        assertEquals("Recording, less than a minute", RecordingNotificationText.title(59_999, false));
        assertEquals("Recording, less than a minute", RecordingNotificationText.title(-5, false));
    }

    @Test
    public void minutesAndHours() {
        assertEquals("1 minute", RecordingNotificationText.duration(60_000));
        assertEquals("12 minutes", RecordingNotificationText.duration(12 * 60_000 + 30_000));
        assertEquals("1 hour", RecordingNotificationText.duration(60 * 60_000));
        assertEquals("1 hour 1 minute", RecordingNotificationText.duration(61 * 60_000));
        assertEquals("2 hours 5 minutes", RecordingNotificationText.duration(125 * 60_000));
    }

    @Test
    public void proPrefix() {
        assertEquals("Pro recording, 3 minutes", RecordingNotificationText.title(3 * 60_000, true));
    }

    @Test
    public void nextMinuteDelay() {
        assertEquals(60_000, RecordingNotificationText.millisUntilNextMinute(0));
        assertEquals(1, RecordingNotificationText.millisUntilNextMinute(59_999));
        assertEquals(30_000, RecordingNotificationText.millisUntilNextMinute(90_000));
    }
}
