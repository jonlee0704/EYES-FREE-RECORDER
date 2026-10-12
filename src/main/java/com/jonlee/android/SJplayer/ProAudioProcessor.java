package com.jonlee.android.SJplayer;

/**
 * Pro Recording Mode signal chain for one audio channel (PRD A2, "Technical approach").
 *
 *   low-cut filter -> focus-specific noise reduction (wet/dry mix) -> wind reduction
 *   -> echo / reverb control -> input gain trim -> limiter
 *
 * AGC is not done here: in Auto gain mode the phone's own AGC stays on upstream of the app.
 *
 * Plain Java with no Android dependencies, so it runs (and is unit tested) on the JVM.
 * Processing is streaming: any block size can be pushed through {@link #process}, and the
 * output has a constant latency of {@link #getLatencySamples()} samples (511 samples, about
 * 10.6 ms at 48 kHz) from the STFT stage.
 *
 * Noise reduction methods per focus:
 *   - Voice:   speech-band Wiener suppression with a fast-adapting noise floor and strong
 *              attenuation outside the speech band. This is the on-device stand-in for a
 *              speech-enhancement model (RNNoise); see {@link #setFocus}.
 *   - Music:   stationary-noise reduction. The noise profile is learned with minimum statistics
 *              over a long (3 s) window, so only steady noise (HVAC, fans, hiss, hum) is removed
 *              and sustained notes or rhythm are left alone.
 *   - Natural: the same light stationary reduction with a high gain floor; the amount is capped at 50%.
 *
 * The noise-reduction amount is a wet/dry mix applied per frequency bin
 * (gain' = 1 - mix * (1 - gain)), smoothed over about 100 ms so live changes have no clicks.
 */
public class ProAudioProcessor {

    public static final int FFT_SIZE = 512;
    public static final int HOP_SIZE = FFT_SIZE / 2;
    private static final int BINS = FFT_SIZE / 2 + 1;

    public enum NoiseFocus { VOICE, MUSIC, NATURAL }

    private final int sampleRate;

    // --- Settings (written from any thread, read by the audio thread) ---
    private volatile float lowCutHz = 0f;
    private volatile boolean echoControl = false;
    private volatile boolean windReduction = false;
    private volatile NoiseFocus focus = NoiseFocus.VOICE;
    private volatile float targetMix = 0f;          // 0..1 noise-reduction amount
    private volatile float targetGainLinear = 1f;   // input trim
    private volatile boolean limiterEnabled = true;

    // --- Low-cut biquad (2nd-order Butterworth high-pass) ---
    private float hpB0 = 1, hpB1 = 0, hpB2 = 0, hpA1 = 0, hpA2 = 0;
    private float hpX1, hpX2, hpY1, hpY2;
    private float appliedLowCutHz = -1f;

    // --- STFT state ---
    private final float[] window = new float[FFT_SIZE];
    private final float[] inputFrame = new float[FFT_SIZE];
    private final float[] overlap = new float[FFT_SIZE];
    private final float[] re = new float[FFT_SIZE];
    private final float[] im = new float[FFT_SIZE];
    private final float[] pendingOut = new float[HOP_SIZE];
    private int inputFill = FFT_SIZE - HOP_SIZE;   // prime with zeros so the first hop completes a frame
    private int pendingOutRead = HOP_SIZE;         // nothing produced yet

    // Per-bin analysis
    private final float[] binFreq = new float[BINS];
    private final float[] smoothedPower = new float[BINS];
    private final float[] noisePower = new float[BINS];
    private final float[] prevCleanPower = new float[BINS];
    private final float[] gains = new float[BINS];
    private boolean firstFrame = true;

    // Minimum-statistics noise tracking: V sub-windows of U frames each
    private static final int MIN_SUBWINDOWS = 6;
    private final float[][] subWindowMin = new float[MIN_SUBWINDOWS][BINS];
    private final float[] currentMin = new float[BINS];
    private int subWindowFrames = 0;
    private int subWindowIndex = 0;
    private int framesPerSubWindow = 48;

    // Late-reverberation estimate history (power spectra of past frames)
    private static final int REVERB_DELAY_FRAMES = 9;   // ~50 ms at 48 kHz / 256 hop
    private final float[][] powerHistory = new float[REVERB_DELAY_FRAMES + 1][BINS];
    private int historyIndex = 0;
    private float reverbDecay;                            // energy decay over the delay, T60 = 0.5 s

    // Wind state
    private float windAmount = 0f;

    // Smoothed controls
    private float currentMix = 0f;
    private float currentGain = 1f;
    private final float gainSmoothing;

    // Limiter
    private static final float LIMITER_CEILING = 0.891f;  // -1 dBFS
    private float limiterGain = 1f;
    private final float limiterRelease;

    public ProAudioProcessor(int sampleRate) {
        this.sampleRate = sampleRate;
        for (int i = 0; i < FFT_SIZE; i++) {
            // sqrt-Hann for analysis and synthesis: overlap-adds to unity at 50% overlap
            window[i] = (float) Math.sqrt(0.5 - 0.5 * Math.cos(2 * Math.PI * i / FFT_SIZE));
        }
        for (int k = 0; k < BINS; k++) {
            binFreq[k] = (float) k * sampleRate / FFT_SIZE;
            gains[k] = 1f;
        }
        float framesPerSecond = (float) sampleRate / HOP_SIZE;
        float t60 = 0.5f;
        float delaySeconds = REVERB_DELAY_FRAMES / framesPerSecond;
        reverbDecay = (float) Math.exp(-2.0 * (3.0 * Math.log(10.0) / t60) * delaySeconds);
        gainSmoothing = (float) (1.0 - Math.exp(-1.0 / (0.010 * sampleRate)));   // ~10 ms
        limiterRelease = (float) (1.0 - Math.exp(-1.0 / (0.100 * sampleRate)));  // ~100 ms
        setFocus(NoiseFocus.VOICE);
        resetNoiseTracking();
    }

    // ---------------------------------------------------------------------------------------
    // Settings
    // ---------------------------------------------------------------------------------------

    public void setLowCutHz(float hz) {
        this.lowCutHz = Math.max(0f, hz);
    }

    public void setEchoControl(boolean enabled) {
        this.echoControl = enabled;
    }

    public void setWindReduction(boolean enabled) {
        this.windReduction = enabled;
    }

    /**
     * Voice uses a short noise window (about 0.8 s) so it follows changing backgrounds;
     * Music and Natural use a long window (about 3 s) so only steady noise is learned.
     * A trained speech model (e.g. RNNoise) can replace the Voice branch of {@link #computeNoiseGain}.
     */
    public void setFocus(NoiseFocus focus) {
        this.focus = focus;
        float framesPerSecond = (float) sampleRate / HOP_SIZE;
        float windowSeconds = (focus == NoiseFocus.VOICE) ? 0.8f : 3.0f;
        this.framesPerSubWindow = Math.max(4, Math.round(windowSeconds * framesPerSecond / MIN_SUBWINDOWS));
    }

    /** Noise-reduction amount in percent (0..100). */
    public void setNoiseReductionPercent(int percent) {
        this.targetMix = Math.max(0, Math.min(100, percent)) / 100f;
    }

    public void setGainDb(float db) {
        this.targetGainLinear = (float) Math.pow(10.0, db / 20.0);
    }

    public void setLimiterEnabled(boolean enabled) {
        this.limiterEnabled = enabled;
    }

    public int getLatencySamples() {
        // A frame completes on the last sample of each hop, and that same call emits the first
        // sample of the overlap-added hop, so the delay is one sample less than the FFT size
        return FFT_SIZE - 1;
    }

    // ---------------------------------------------------------------------------------------
    // Processing
    // ---------------------------------------------------------------------------------------

    /**
     * Processes {@code length} samples from {@code in} into {@code out} (may be the same array).
     * Samples are floats in -1..1.
     */
    public void process(float[] in, int offset, float[] out, int outOffset, int length) {
        process(in, offset, out, outOffset, length, null);
    }

    /**
     * Same as {@link #process(float[], int, float[], int, int)}, and also writes the processed
     * signal before the gain trim and limiter into {@code preGainOut} (if not null), at the same
     * offset as {@code out}. The recorder uses it to build the safety track.
     */
    public void process(float[] in, int offset, float[] out, int outOffset, int length, float[] preGainOut) {
        updateLowCut();
        for (int i = 0; i < length; i++) {
            float x = lowCut(in[offset + i]);

            inputFrame[inputFill++] = x;
            if (inputFill == FFT_SIZE) {
                processFrame();
                // Slide the analysis buffer by one hop
                System.arraycopy(inputFrame, HOP_SIZE, inputFrame, 0, FFT_SIZE - HOP_SIZE);
                inputFill = FFT_SIZE - HOP_SIZE;
            }

            float y = 0f;
            if (pendingOutRead < HOP_SIZE) {
                y = pendingOut[pendingOutRead++];
            }

            if (preGainOut != null) preGainOut[outOffset + i] = y;

            currentGain += (targetGainLinear - currentGain) * gainSmoothing;
            y *= currentGain;

            if (limiterEnabled) {
                float a = Math.abs(y) * limiterGain;
                if (a > LIMITER_CEILING) {
                    limiterGain = LIMITER_CEILING / Math.abs(y);
                } else {
                    limiterGain += (1f - limiterGain) * limiterRelease;
                }
                y *= limiterGain;
            } else {
                limiterGain = 1f;
            }
            out[outOffset + i] = y;
        }
    }

    private void processFrame() {
        for (int i = 0; i < FFT_SIZE; i++) {
            re[i] = inputFrame[i] * window[i];
            im[i] = 0f;
        }
        fft(re, im, false);

        // Smooth the mix target per frame: ~100 ms to settle, no clicks on live changes
        currentMix += (targetMix - currentMix) * 0.2f;
        if (Math.abs(targetMix - currentMix) < 1e-4f) currentMix = targetMix;

        float lowEnergy = 0f, totalEnergy = 1e-12f;
        float[] history = powerHistory[historyIndex];

        for (int k = 0; k < BINS; k++) {
            float p = re[k] * re[k] + im[k] * im[k];
            history[k] = p;
            totalEnergy += p;
            if (binFreq[k] < 250f) lowEnergy += p;

            if (firstFrame) {
                smoothedPower[k] = p;
                noisePower[k] = p;
                prevCleanPower[k] = p;
            } else {
                smoothedPower[k] = 0.85f * smoothedPower[k] + 0.15f * p;
            }
        }
        firstFrame = false;
        updateNoiseEstimate();

        // Wind: low-frequency energy dominating the frame means gusts on the mic
        if (windReduction) {
            float ratio = lowEnergy / totalEnergy;
            float target = Math.max(0f, Math.min(1f, (ratio - 0.4f) / 0.4f));
            windAmount += (target - windAmount) * (target > windAmount ? 0.5f : 0.1f);
        } else {
            windAmount = 0f;
        }

        int delayed = Math.floorMod(historyIndex - REVERB_DELAY_FRAMES, REVERB_DELAY_FRAMES + 1);
        float[] pastPower = powerHistory[delayed];

        for (int k = 0; k < BINS; k++) {
            float p = history[k];
            float g = computeNoiseGain(k, p);

            // Wet/dry mix of the noise-reduced and original signal
            g = 1f - currentMix * (1f - g);

            if (windAmount > 0f && binFreq[k] < 300f) {
                g *= 1f - 0.85f * windAmount;
            }

            if (echoControl && p > 1e-12f) {
                float lateReverb = reverbDecay * pastPower[k];
                float rg = 1f - lateReverb / p;
                g *= Math.max(0.3f, Math.min(1f, rg));
            }

            gains[k] = g;
            prevCleanPower[k] = g * g * p;
        }
        historyIndex = (historyIndex + 1) % (REVERB_DELAY_FRAMES + 1);

        // Apply gains to the conjugate-symmetric spectrum
        for (int k = 0; k < BINS; k++) {
            re[k] *= gains[k];
            im[k] *= gains[k];
            if (k > 0 && k < FFT_SIZE / 2) {
                re[FFT_SIZE - k] *= gains[k];
                im[FFT_SIZE - k] *= gains[k];
            }
        }
        fft(re, im, true);

        for (int i = 0; i < FFT_SIZE; i++) {
            overlap[i] += re[i] * window[i];
        }
        System.arraycopy(overlap, 0, pendingOut, 0, HOP_SIZE);
        System.arraycopy(overlap, HOP_SIZE, overlap, 0, FFT_SIZE - HOP_SIZE);
        java.util.Arrays.fill(overlap, FFT_SIZE - HOP_SIZE, FFT_SIZE, 0f);
        pendingOutRead = 0;
    }

    /** Full-strength (100%) noise-reduction gain for one bin; the mix is applied by the caller. */
    private float computeNoiseGain(int k, float power) {
        NoiseFocus f = focus;
        float noise = noisePower[k] + 1e-12f;
        float overSubtraction;
        float floor;
        switch (f) {
            case VOICE:
                // Outside the speech band, everything counts as noise
                if (binFreq[k] < 80f || binFreq[k] > 8000f) return 0.05f;
                overSubtraction = 2.0f;
                floor = 0.03f;
                break;
            case MUSIC:
                overSubtraction = 1.2f;
                floor = 0.12f;
                break;
            case NATURAL:
            default:
                overSubtraction = 1.0f;
                floor = 0.3f;
                break;
        }
        // Decision-directed a-priori SNR (Ephraim-Malah) limits musical noise
        float posterior = power / (overSubtraction * noise);
        float priori = 0.98f * prevCleanPower[k] / (overSubtraction * noise)
                + 0.02f * Math.max(posterior - 1f, 0f);
        float g = priori / (1f + priori);
        return Math.max(floor, Math.min(1f, g));
    }

    private void updateNoiseEstimate() {
        for (int k = 0; k < BINS; k++) {
            if (smoothedPower[k] < currentMin[k]) currentMin[k] = smoothedPower[k];
        }
        subWindowFrames++;
        if (subWindowFrames >= framesPerSubWindow) {
            System.arraycopy(currentMin, 0, subWindowMin[subWindowIndex], 0, BINS);
            subWindowIndex = (subWindowIndex + 1) % MIN_SUBWINDOWS;
            java.util.Arrays.fill(currentMin, Float.MAX_VALUE);
            subWindowFrames = 0;
        }
        final float bias = 1.5f;   // minimum statistics under-estimates the mean noise power
        for (int k = 0; k < BINS; k++) {
            float m = currentMin[k];
            for (int w = 0; w < MIN_SUBWINDOWS; w++) {
                if (subWindowMin[w][k] < m) m = subWindowMin[w][k];
            }
            if (m == Float.MAX_VALUE) m = smoothedPower[k];
            noisePower[k] = bias * m;
        }
    }

    private void resetNoiseTracking() {
        java.util.Arrays.fill(currentMin, Float.MAX_VALUE);
        for (float[] row : subWindowMin) java.util.Arrays.fill(row, Float.MAX_VALUE);
    }

    private void updateLowCut() {
        float hz = lowCutHz;
        if (hz == appliedLowCutHz) return;
        appliedLowCutHz = hz;
        if (hz <= 0f) {
            hpB0 = 1; hpB1 = 0; hpB2 = 0; hpA1 = 0; hpA2 = 0;
            return;
        }
        // RBJ cookbook high-pass, Q = 1/sqrt(2)
        double w0 = 2 * Math.PI * hz / sampleRate;
        double cos = Math.cos(w0);
        double alpha = Math.sin(w0) / (2 * Math.sqrt(0.5));
        double a0 = 1 + alpha;
        hpB0 = (float) (((1 + cos) / 2) / a0);
        hpB1 = (float) (-(1 + cos) / a0);
        hpB2 = hpB0;
        hpA1 = (float) ((-2 * cos) / a0);
        hpA2 = (float) ((1 - alpha) / a0);
    }

    private float lowCut(float x) {
        float y = hpB0 * x + hpB1 * hpX1 + hpB2 * hpX2 - hpA1 * hpY1 - hpA2 * hpY2;
        hpX2 = hpX1;
        hpX1 = x;
        hpY2 = hpY1;
        hpY1 = y;
        return y;
    }

    /** In-place iterative radix-2 FFT. Inverse includes the 1/N scale. */
    static void fft(float[] re, float[] im, boolean inverse) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                float t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = 2 * Math.PI / len * (inverse ? 1 : -1);
            float wRe = (float) Math.cos(ang), wIm = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float cRe = 1f, cIm = 0f;
                for (int j = 0; j < len / 2; j++) {
                    int a = i + j, b = i + j + len / 2;
                    float uRe = re[a], uIm = im[a];
                    float vRe = re[b] * cRe - im[b] * cIm;
                    float vIm = re[b] * cIm + im[b] * cRe;
                    re[a] = uRe + vRe; im[a] = uIm + vIm;
                    re[b] = uRe - vRe; im[b] = uIm - vIm;
                    float nRe = cRe * wRe - cIm * wIm;
                    cIm = cRe * wIm + cIm * wRe;
                    cRe = nRe;
                }
            }
        }
        if (inverse) {
            for (int i = 0; i < n; i++) {
                re[i] /= n;
                im[i] /= n;
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // Level helpers
    // ---------------------------------------------------------------------------------------

    public static float peak(float[] buf, int offset, int length) {
        float p = 0f;
        for (int i = offset; i < offset + length; i++) {
            float a = Math.abs(buf[i]);
            if (a > p) p = a;
        }
        return p;
    }

    public static float toDbfs(float linearPeak) {
        if (linearPeak <= 1e-6f) return -120f;
        return (float) (20.0 * Math.log10(linearPeak));
    }

    /**
     * Sound-check recommendation: aim the peak at -8 dBFS (headroom for louder moments), rounded to the 3 dB gain grid,
     * relative to the gain that was in effect while measuring. Returns the change in dB.
     */
    public static int recommendGainChangeDb(float peakDbfs) {
        final float targetPeak = -8f;
        float change = targetPeak - peakDbfs;
        int stepped = Math.round(change / ProRecordingSettings.GAIN_STEP_DB) * ProRecordingSettings.GAIN_STEP_DB;
        // Within 3 dB of the target is fine as is
        if (Math.abs(change) < ProRecordingSettings.GAIN_STEP_DB) return 0;
        return stepped;
    }
}
