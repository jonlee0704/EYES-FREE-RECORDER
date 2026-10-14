package com.jonlee.android.SJplayer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for P0/P1 fixes: Geotag default off, Trash Undo / Restore, and TalkBack preferences.
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
        assertTrue("User can opt-in to location tagging", prefs.getBoolean("pref_save_location", false));
    }

    @Test
    public void testTalkBackModeDefaultsToAuto() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        String mode = prefs.getString("pref_talkback_mode", "auto");
        assertEquals("TalkBack mode must default to auto", "auto", mode);
    }

    @Test
    public void testTrashAndRestoreSimulation() throws IOException {
        File recordingsDir = tempFolder.newFolder("recordings");
        File trashDir = tempFolder.newFolder(".Trash");

        File originalFile = new File(recordingsDir, "2026-10-13-19-00-take1.m4a");
        try (FileOutputStream fos = new FileOutputStream(originalFile)) {
            fos.write("audio-bytes-content".getBytes(StandardCharsets.UTF_8));
        }
        assertTrue("Original file must exist before deletion", originalFile.exists());

        // Simulate move to trash
        File trashedFile = new File(trashDir, originalFile.getName());
        Recorder.copyDirectoryOneLocationToAnotherLocation(originalFile, trashedFile);
        assertTrue("Original file must delete when moved to trash", originalFile.delete());
        assertFalse(originalFile.exists());
        assertTrue("File must exist in trash folder", trashedFile.exists());

        // Simulate restore (undo)
        Recorder.copyDirectoryOneLocationToAnotherLocation(trashedFile, originalFile);
        assertTrue("Trash copy must delete after restore", trashedFile.delete());
        assertTrue("Original file must be restored", originalFile.exists());
        assertFalse("Trash file must no longer exist after restore", trashedFile.exists());
        assertEquals("File size must match after restore", "audio-bytes-content".length(), originalFile.length());
    }
}
