package com.jonlee.android.SJplayer;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import com.jonlee.android.common.logger.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * SoundscapeHelper provides synthesized low-latency auditory earcons
 * for non-visual navigation (Eyes-Free Assistive UI).
 *
 * All earcon waveforms are generated dynamically in memory and cached locally
 * as 16-bit PCM WAV files, eliminating external media asset dependencies.
 */
public class SoundscapeHelper {

    private static final String TAG = "SoundscapeHelper";
    private static final int SAMPLE_RATE = 44100;

    private SoundPool soundPool;
    private int soundNextTrack = -1;
    private int soundPrevTrack = -1;
    private int soundNextFolder = -1;
    private int soundPrevFolder = -1;
    private int soundCorner = -1;

    private boolean isLoadedNextTrack = false;
    private boolean isLoadedPrevTrack = false;
    private boolean isLoadedNextFolder = false;
    private boolean isLoadedPrevFolder = false;
    private boolean isLoadedCorner = false;

    // Pro Recording Mode earcons: one per environment, one per focus, plus on/off.
    // Each is a short motif of notes so environments are told apart by melody, not just pitch.
    private static final float[][] PRO_ENVIRONMENT_MOTIFS = {
            {523.25f, 659.25f},             // Meeting room: C5 E5 (calm major third)
            {392.00f, 587.33f, 783.99f},    // Small music venue: G4 D5 G5 (open, musical)
            {261.63f, 392.00f, 523.25f},    // Stadium: C4 G4 C5 (wide, low)
            {880.00f, 659.25f},             // Street: A5 E5 (bright, falling)
    };
    private static final float[][] PRO_FOCUS_MOTIFS = {
            {698.46f},                      // Voice: F5 single note
            {587.33f, 739.99f},             // Music: D5 F#5
            {493.88f, 493.88f},             // Natural: B4 repeated
    };
    private static final float[] PRO_ON_MOTIF = {440.00f, 554.37f, 659.25f};   // A major arpeggio up
    private static final float[] PRO_OFF_MOTIF = {659.25f, 554.37f, 440.00f};  // and down
    private final java.util.Map<String, Integer> proSounds = new java.util.HashMap<>();
    private final java.util.Set<Integer> loadedSounds = java.util.Collections.synchronizedSet(new java.util.HashSet<Integer>());

    public SoundscapeHelper(Context context) {
        initSoundPool(context);
    }

    private void initSoundPool(Context context) {
        try {
            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            soundPool = new SoundPool.Builder()
                    .setMaxStreams(5)
                    .setAudioAttributes(attributes)
                    .build();

            soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool sp, int sampleId, int status) {
                    if (status == 0) {
                        loadedSounds.add(sampleId);
                        if (sampleId == soundNextTrack) isLoadedNextTrack = true;
                        else if (sampleId == soundPrevTrack) isLoadedPrevTrack = true;
                        else if (sampleId == soundNextFolder) isLoadedNextFolder = true;
                        else if (sampleId == soundPrevFolder) isLoadedPrevFolder = true;
                        else if (sampleId == soundCorner) isLoadedCorner = true;
                    }
                }
            });

            File earconsDir = new File(context.getCacheDir(), "earcons");
            if (!earconsDir.exists()) {
                earconsDir.mkdirs();
            }

            File fileNextTrack = new File(earconsDir, "next_track.wav");
            File filePrevTrack = new File(earconsDir, "prev_track.wav");
            File fileNextFolder = new File(earconsDir, "next_folder.wav");
            File filePrevFolder = new File(earconsDir, "prev_folder.wav");
            File fileCorner = new File(earconsDir, "corner_anchor.wav");

            if (!fileNextTrack.exists() || fileNextTrack.length() < 100) {
                writeWavFile(fileNextTrack, generateNextTrackChirp(), SAMPLE_RATE);
            }
            if (!filePrevTrack.exists() || filePrevTrack.length() < 100) {
                writeWavFile(filePrevTrack, generatePrevTrackChirp(), SAMPLE_RATE);
            }
            if (!fileNextFolder.exists() || fileNextFolder.length() < 100) {
                writeWavFile(fileNextFolder, generateNextFolderChime(), SAMPLE_RATE);
            }
            if (!filePrevFolder.exists() || filePrevFolder.length() < 100) {
                writeWavFile(filePrevFolder, generatePrevFolderChime(), SAMPLE_RATE);
            }
            if (!fileCorner.exists() || fileCorner.length() < 100) {
                writeWavFile(fileCorner, generateCornerAnchorPing(), SAMPLE_RATE);
            }

            soundNextTrack = soundPool.load(fileNextTrack.getAbsolutePath(), 1);
            soundPrevTrack = soundPool.load(filePrevTrack.getAbsolutePath(), 1);
            soundNextFolder = soundPool.load(fileNextFolder.getAbsolutePath(), 1);
            soundPrevFolder = soundPool.load(filePrevFolder.getAbsolutePath(), 1);
            soundCorner = soundPool.load(fileCorner.getAbsolutePath(), 1);

            for (int i = 0; i < PRO_ENVIRONMENT_MOTIFS.length; i++) {
                loadProEarcon(earconsDir, "pro_env_" + i, PRO_ENVIRONMENT_MOTIFS[i]);
            }
            for (int i = 0; i < PRO_FOCUS_MOTIFS.length; i++) {
                loadProEarcon(earconsDir, "pro_focus_" + i, PRO_FOCUS_MOTIFS[i]);
            }
            loadProEarcon(earconsDir, "pro_on", PRO_ON_MOTIF);
            loadProEarcon(earconsDir, "pro_off", PRO_OFF_MOTIF);

        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize SoundscapeHelper SoundPool: " + e.getMessage());
        }
    }

    /**
     * Next Track (Swipe Right):
     * Rising affirmative chirp panned slightly right (65ms, 520Hz -> 940Hz).
     */
    public void playNextTrack() {
        if (soundPool != null && isLoadedNextTrack) {
            soundPool.play(soundNextTrack, 0.40f, 0.95f, 1, 0, 1.0f);
        }
    }

    /**
     * Previous Track (Swipe Left):
     * Falling gentle chirp panned slightly left (65ms, 940Hz -> 520Hz).
     */
    public void playPrevTrack() {
        if (soundPool != null && isLoadedPrevTrack) {
            soundPool.play(soundPrevTrack, 0.95f, 0.40f, 1, 0, 1.0f);
        }
    }

    /**
     * Next Folder (Swipe Up):
     * Ascending harmonic chime (95ms, C5: 523Hz -> G5: 784Hz, centered).
     */
    public void playNextFolder() {
        if (soundPool != null && isLoadedNextFolder) {
            soundPool.play(soundNextFolder, 0.85f, 0.85f, 1, 0, 1.0f);
        }
    }

    /**
     * Previous Folder (Swipe Down):
     * Descending harmonic chime (95ms, G5: 784Hz -> C5: 523Hz, centered).
     */
    public void playPrevFolder() {
        if (soundPool != null && isLoadedPrevFolder) {
            soundPool.play(soundPrevFolder, 0.85f, 0.85f, 1, 0, 1.0f);
        }
    }

    /**
     * Corner Anchor:
     * High crystal bell ping (50ms, C6: 1046Hz, fast decay).
     */
    public void playCorner() {
        if (soundPool != null && isLoadedCorner) {
            soundPool.play(soundCorner, 0.80f, 0.80f, 1, 0, 1.0f);
        }
    }

    /** Pro Recording Mode environment earcon (index = Environment ordinal). */
    public void playProEnvironment(int index) {
        playPro("pro_env_" + index);
    }

    /** Pro Recording Mode focus earcon (index = Focus ordinal). */
    public void playProFocus(int index) {
        playPro("pro_focus_" + index);
    }

    public void playProToggle(boolean on) {
        playPro(on ? "pro_on" : "pro_off");
    }

    private void playPro(String name) {
        Integer id = proSounds.get(name);
        if (soundPool != null && id != null && loadedSounds.contains(id)) {
            soundPool.play(id, 0.85f, 0.85f, 1, 0, 1.0f);
        }
    }

    private void loadProEarcon(File dir, String name, float[] notes) {
        File f = new File(dir, name + ".wav");
        if (!f.exists() || f.length() < 100) {
            writeWavFile(f, generateMotif(notes), SAMPLE_RATE);
        }
        proSounds.put(name, soundPool.load(f.getAbsolutePath(), 1));
    }

    /** A sequence of short bell-like notes (45 ms each, 10 ms gap). */
    private static short[] generateMotif(float[] notes) {
        int noteLen = (int) (SAMPLE_RATE * 0.045f);
        int gap = (int) (SAMPLE_RATE * 0.010f);
        short[] samples = new short[notes.length * (noteLen + gap)];
        for (int n = 0; n < notes.length; n++) {
            int start = n * (noteLen + gap);
            for (int i = 0; i < noteLen; i++) {
                float t = (float) i / SAMPLE_RATE;
                float env = (float) Math.exp(-4.0 * i / noteLen);
                if (i < SAMPLE_RATE * 0.003f) env *= i / (SAMPLE_RATE * 0.003f);   // 3 ms soft attack
                float s = (float) Math.sin(2.0 * Math.PI * notes[n] * t) * env * 0.75f * 32767.0f;
                samples[start + i] = (short) Math.max(-32768, Math.min(32767, (int) s));
            }
        }
        return samples;
    }

    public void release() {
        if (soundPool != null) {
            try {
                soundPool.release();
            } catch (Exception ignored) {}
            soundPool = null;
        }
    }

    // --- Procedural PCM Waveform Synthesis ---

    private static short[] generateNextTrackChirp() {
        float duration = 0.065f; // 65ms
        int n = (int) (SAMPLE_RATE * duration);
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / SAMPLE_RATE;
            float frac = t / duration;
            float freq = 520.0f + (940.0f - 520.0f) * frac;
            float env = (float) Math.sin(Math.PI * frac); // Smooth raised-sine window
            float s = (float) Math.sin(2.0 * Math.PI * freq * t) * env * 0.85f * 32767.0f;
            samples[i] = (short) Math.max(-32768, Math.min(32767, (int) s));
        }
        return samples;
    }

    private static short[] generatePrevTrackChirp() {
        float duration = 0.065f; // 65ms
        int n = (int) (SAMPLE_RATE * duration);
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / SAMPLE_RATE;
            float frac = t / duration;
            float freq = 940.0f - (940.0f - 520.0f) * frac;
            float env = (float) Math.sin(Math.PI * frac);
            float s = (float) Math.sin(2.0 * Math.PI * freq * t) * env * 0.85f * 32767.0f;
            samples[i] = (short) Math.max(-32768, Math.min(32767, (int) s));
        }
        return samples;
    }

    private static short[] generateNextFolderChime() {
        float duration = 0.095f; // 95ms
        int n = (int) (SAMPLE_RATE * duration);
        int split = (int) (SAMPLE_RATE * 0.035f); // 35ms note 1, 60ms note 2
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / SAMPLE_RATE;
            float freq = (i < split) ? 523.25f : 783.99f;
            float localT = (i < split) ? t : (t - 0.035f);
            float localDur = (i < split) ? 0.035f : (duration - 0.035f);
            float env = (float) Math.sin(Math.PI * (localT / localDur));
            float s = (float) Math.sin(2.0 * Math.PI * freq * localT) * env * 0.80f * 32767.0f;
            samples[i] = (short) Math.max(-32768, Math.min(32767, (int) s));
        }
        return samples;
    }

    private static short[] generatePrevFolderChime() {
        float duration = 0.095f; // 95ms
        int n = (int) (SAMPLE_RATE * duration);
        int split = (int) (SAMPLE_RATE * 0.035f);
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / SAMPLE_RATE;
            float freq = (i < split) ? 783.99f : 523.25f;
            float localT = (i < split) ? t : (t - 0.035f);
            float localDur = (i < split) ? 0.035f : (duration - 0.035f);
            float env = (float) Math.sin(Math.PI * (localT / localDur));
            float s = (float) Math.sin(2.0 * Math.PI * freq * localT) * env * 0.80f * 32767.0f;
            samples[i] = (short) Math.max(-32768, Math.min(32767, (int) s));
        }
        return samples;
    }

    private static short[] generateCornerAnchorPing() {
        float duration = 0.050f; // 50ms
        int n = (int) (SAMPLE_RATE * duration);
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            float t = (float) i / SAMPLE_RATE;
            float freq = 1046.50f; // C6
            float env = (float) Math.exp(-6.0 * (t / duration)); // Exponential ring-down
            if (t < 0.003f) {
                env *= (t / 0.003f); // 3ms soft attack
            }
            float s = (float) Math.sin(2.0 * Math.PI * freq * t) * env * 0.75f * 32767.0f;
            samples[i] = (short) Math.max(-32768, Math.min(32767, (int) s));
        }
        return samples;
    }

    private static void writeWavFile(File file, short[] pcmSamples, int sampleRate) {
        FileOutputStream fos = null;
        try {
            int numSamples = pcmSamples.length;
            int dataSize = numSamples * 2;
            int chunkSize = 36 + dataSize;
            int byteRate = sampleRate * 2;

            fos = new FileOutputStream(file);
            ByteBuffer header = ByteBuffer.allocate(44);
            header.order(ByteOrder.LITTLE_ENDIAN);

            // RIFF header
            header.put((byte) 'R').put((byte) 'I').put((byte) 'F').put((byte) 'F');
            header.putInt(chunkSize);
            header.put((byte) 'W').put((byte) 'A').put((byte) 'V').put((byte) 'E');

            // Format sub-chunk
            header.put((byte) 'f').put((byte) 'm').put((byte) 't').put((byte) ' ');
            header.putInt(16);             // Sub-chunk size
            header.putShort((short) 1);     // Audio format: PCM
            header.putShort((short) 1);     // Channels: Mono
            header.putInt(sampleRate);
            header.putInt(byteRate);
            header.putShort((short) 2);     // Block align
            header.putShort((short) 16);    // Bits per sample

            // Data sub-chunk
            header.put((byte) 'd').put((byte) 'a').put((byte) 't').put((byte) 'a');
            header.putInt(dataSize);

            fos.write(header.array());

            // Write PCM data
            ByteBuffer dataBuffer = ByteBuffer.allocate(dataSize);
            dataBuffer.order(ByteOrder.LITTLE_ENDIAN);
            for (short sample : pcmSamples) {
                dataBuffer.putShort(sample);
            }
            fos.write(dataBuffer.array());
            fos.flush();
        } catch (Exception e) {
            Log.e(TAG, "Error writing WAV: " + file.getName(), e);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
