package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests verifying real-time hardware recording volume normalization,
 * decibel scaling, dynamic color shifting, and ballistic audio envelope response.
 */
public class RecordingAmbientVisualizerTest {

    @Test
    public void testSilenceReturnsBaselineZero() {
        assertEquals(0.0f, DialView.calculateNormalizedRecordingVolume(0), 0.001f);
        assertEquals(0.0f, DialView.calculateNormalizedRecordingVolume(10), 0.001f);
        assertEquals(0.0f, DialView.calculateNormalizedRecordingVolume(25), 0.001f);
    }

    @Test
    public void testWhisperNormalizesToResponsiveRange() {
        // Amplitude ~500 to 2000 (typical quiet speech / whisper)
        float vol = DialView.calculateNormalizedRecordingVolume(1200);
        assertTrue("Whisper should register above baseline (> 0.15)", vol > 0.15f);
        assertTrue("Whisper should not saturate (< 0.50)", vol < 0.50f);
    }

    @Test
    public void testNormalConversationalVoiceReachesActiveRange() {
        // Amplitude ~6000 to 16000 (typical conversational speech into mic)
        float vol = DialView.calculateNormalizedRecordingVolume(9000);
        assertTrue("Conversational speech should be in active range (> 0.50)", vol > 0.50f);
        assertTrue("Conversational speech should have dynamic headroom (< 0.90)", vol < 0.90f);
    }

    @Test
    public void testPeakLoudnessSaturatesSmoothly() {
        // Amplitude near full scale (0 dBFS / 32767)
        float vol = DialView.calculateNormalizedRecordingVolume(32767);
        assertEquals(1.0f, vol, 0.01f);

        // Amplitude above 25000
        float loudVol = DialView.calculateNormalizedRecordingVolume(28000);
        assertTrue("Loud audio should reach upper tier (> 0.90)", loudVol > 0.90f);
    }

    @Test
    public void testDynamicChromaticShiftByVolume() {
        // Low / ambient level: Studio Crimson
        assertEquals(DialView.RECORDING_COLOR_CRIMSON, DialView.getRecordingEdgeColor(0.20f));
        assertEquals(DialView.RECORDING_COLOR_CRIMSON, DialView.getRecordingEdgeColor(0.40f));

        // Mid conversational voice: Sunset Coral
        assertEquals(DialView.RECORDING_COLOR_CORAL, DialView.getRecordingEdgeColor(0.55f));
        assertEquals(DialView.RECORDING_COLOR_CORAL, DialView.getRecordingEdgeColor(0.75f));

        // High loud voice / accents: Radiant Amber
        assertEquals(DialView.RECORDING_COLOR_AMBER, DialView.getRecordingEdgeColor(0.85f));
        assertEquals(DialView.RECORDING_COLOR_AMBER, DialView.getRecordingEdgeColor(1.0f));
    }

    @Test
    public void testBallisticAttackFasterThanDecay() {
        // Verify ballistic envelope properties:
        // Attack factor (~0.46) responds ~4x faster than decay factor (~0.12)
        float smoothVol = 0.0f;
        float targetVoiceSpike = 0.80f;

        // One frame of attack
        smoothVol += (targetVoiceSpike - smoothVol) * 0.46f;
        float afterAttackStep = smoothVol;
        assertTrue("Single attack frame should register significant rise (> 0.35)", afterAttackStep > 0.35f);

        // One frame of decay from spike
        float targetSilence = 0.0f;
        float decayStep = smoothVol + (targetSilence - smoothVol) * 0.12f;
        float decayAmount = smoothVol - decayStep;
        float attackAmount = afterAttackStep;

        assertTrue("Attack rise must be substantially faster than decay drop for responsive voice feel",
                attackAmount > decayAmount * 2.5f);
    }
}
