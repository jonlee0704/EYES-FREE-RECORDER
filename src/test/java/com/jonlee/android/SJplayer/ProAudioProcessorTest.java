package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Test;

/** JVM tests for the Pro Recording Mode signal chain (PRD A2). */
public class ProAudioProcessorTest {

    private static final int SR = 48000;

    private static float[] run(ProAudioProcessor p, float[] in, int block) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i += block) {
            int n = Math.min(block, in.length - i);
            p.process(in, i, out, i, n);
        }
        return out;
    }

    private static ProAudioProcessor transparent() {
        ProAudioProcessor p = new ProAudioProcessor(SR);
        p.setNoiseReductionPercent(0);
        p.setLimiterEnabled(false);
        p.setGainDb(0);
        return p;
    }

    private static float rms(float[] x, int from, int to) {
        double s = 0;
        for (int i = from; i < to; i++) s += x[i] * x[i];
        return (float) Math.sqrt(s / (to - from));
    }

    private static float[] sine(float hz, float amp, int n) {
        float[] x = new float[n];
        for (int i = 0; i < n; i++) x[i] = (float) (amp * Math.sin(2 * Math.PI * hz * i / SR));
        return x;
    }

    @Test
    public void noProcessingIsTransparentWithConstantLatency() {
        Random r = new Random(1);
        float[] in = new float[SR];
        for (int i = 0; i < in.length; i++) in[i] = (r.nextFloat() * 2 - 1) * 0.5f;
        ProAudioProcessor p = transparent();
        float[] out = run(p, in, 441);   // odd block size on purpose
        int latency = p.getLatencySamples();
        for (int i = latency + 1024; i < in.length; i++) {
            assertEquals("sample " + i, in[i - latency], out[i], 1e-4f);
        }
    }

    @Test
    public void lowCutRemovesRumble() {
        ProAudioProcessor p = transparent();
        p.setLowCutHz(100);
        float[] out = run(p, sine(20, 0.5f, SR), 256);
        float[] voice = run(transparentWithLowCut(100), sine(1000, 0.5f, SR), 256);
        assertTrue("20 Hz should be cut", rms(out, SR / 2, SR) < 0.5f * 0.707f * 0.1f);
        assertTrue("1 kHz should pass", rms(voice, SR / 2, SR) > 0.5f * 0.707f * 0.9f);
    }

    private static ProAudioProcessor transparentWithLowCut(float hz) {
        ProAudioProcessor p = transparent();
        p.setLowCutHz(hz);
        return p;
    }

    @Test
    public void musicFocusRemovesSteadyNoiseButKeepsNotes() {
        Random r = new Random(2);
        int n = SR * 6;
        float[] noise = new float[n];
        for (int i = 0; i < n; i++) noise[i] = (float) (r.nextGaussian() * 0.02);
        float[] in = noise.clone();
        // A note from 4 s to 5 s, well above the hiss
        float[] note = sine(440, 0.3f, n);
        for (int i = 4 * SR; i < 5 * SR; i++) in[i] += note[i];

        ProAudioProcessor p = transparent();
        p.setFocus(ProAudioProcessor.NoiseFocus.MUSIC);
        p.setNoiseReductionPercent(100);
        float[] out = run(p, in, 256);
        int lat = p.getLatencySamples();

        float noiseBefore = rms(in, 3 * SR, 4 * SR - 4096);
        float noiseAfter = rms(out, 3 * SR + lat, 4 * SR - 4096 + lat);
        double reductionDb = 20 * Math.log10(noiseBefore / noiseAfter);
        assertTrue("steady noise reduced by " + reductionDb + " dB", reductionDb > 10);

        float noteBefore = rms(in, 4 * SR + 4096, 5 * SR - 4096);
        float noteAfter = rms(out, 4 * SR + 4096 + lat, 5 * SR - 4096 + lat);
        double noteLossDb = 20 * Math.log10(noteBefore / noteAfter);
        assertTrue("note kept (loss " + noteLossDb + " dB)", noteLossDb < 1.5);
    }

    @Test
    public void noiseReductionAmountIsWetDryMix() {
        Random r = new Random(3);
        float[] in = new float[SR * 4];
        for (int i = 0; i < in.length; i++) in[i] = (float) (r.nextGaussian() * 0.05);

        float[] full = run(processorWithNr(100), in, 256);
        float[] half = run(processorWithNr(50), in, 256);
        float fullRms = rms(full, 3 * SR, 4 * SR);
        float halfRms = rms(half, 3 * SR, 4 * SR);
        float inRms = rms(in, 3 * SR, 4 * SR);
        assertTrue(fullRms < halfRms);
        assertTrue(halfRms < inRms);
        // 50% mix keeps at least half of the original amplitude
        assertTrue(halfRms >= 0.45f * inRms);
    }

    private static ProAudioProcessor processorWithNr(int percent) {
        ProAudioProcessor p = transparent();
        p.setFocus(ProAudioProcessor.NoiseFocus.NATURAL);
        p.setNoiseReductionPercent(percent);
        return p;
    }

    @Test
    public void liveNoiseReductionChangeSettlesWithin200msWithoutClicks() {
        Random r = new Random(4);
        float[] in = new float[SR * 4];
        for (int i = 0; i < in.length; i++) in[i] = (float) (r.nextGaussian() * 0.05);
        ProAudioProcessor p = transparent();
        p.setFocus(ProAudioProcessor.NoiseFocus.VOICE);
        float[] out = new float[in.length];
        int change = 2 * SR;
        p.process(in, 0, out, 0, change);
        p.setNoiseReductionPercent(100);
        p.process(in, change, out, change, in.length - change);

        int settle = change + p.getLatencySamples() + SR / 5;   // 200 ms
        float target = rms(out, 3 * SR, 4 * SR);
        float settled = rms(out, settle, settle + SR / 10);
        assertEquals(target, settled, target * 0.5f + 1e-4f);
        // No sample jumps out of the signal's range (no click)
        for (int i = change; i < settle; i++) {
            assertTrue(Math.abs(out[i]) < 0.4f);
        }
    }

    @Test
    public void gainTrimAndLimiter() {
        ProAudioProcessor p = transparent();
        p.setGainDb(6);
        float[] out = run(p, sine(1000, 0.1f, SR), 256);
        assertEquals(0.2f * 0.707f, rms(out, SR / 2, SR), 0.01f);

        ProAudioProcessor limited = transparent();
        limited.setLimiterEnabled(true);
        limited.setGainDb(12);
        float[] loud = run(limited, sine(1000, 0.5f, SR), 256);
        assertTrue(ProAudioProcessor.peak(loud, SR / 2, SR / 2) <= 0.892f);
    }

    @Test
    public void soundCheckRecommendation() {
        // PRD example: "Peak minus 2 dB. Recommend lowering input 6 dB"
        assertEquals(-6, ProAudioProcessor.recommendGainChangeDb(-2f));
        assertEquals(0, ProAudioProcessor.recommendGainChangeDb(-8f));
        assertEquals(12, ProAudioProcessor.recommendGainChangeDb(-20f));
    }

    @Test
    public void settingsStepping() {
        assertEquals(-24, ProRecordingSettings.clampGain(-40));
        assertEquals(12, ProRecordingSettings.clampGain(15));
        assertEquals(-9, ProRecordingSettings.clampGain(-10));
        assertEquals(75, ProRecordingSettings.snapNoiseReduction(70));
        assertEquals("minus 9 dB", ProRecordingSettings.formatDb(-9));
    }
}
