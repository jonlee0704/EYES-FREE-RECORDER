package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AutomaticGainControl;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Pro Recording Mode capture engine (PRD A2). Replaces MediaRecorder with an AudioRecord PCM
 * pipeline while Pro mode is on:
 *
 *   AudioRecord (float) -> {@link ProAudioProcessor} per channel -> encoder(s)
 *
 * Outputs: the main file, an optional "original" (unprocessed input, PR9), and an optional
 * safety track recorded 12 dB lower (PR8). Optional headphone monitoring (PR10) plays the
 * processed signal back with low latency. Level and clipping are reported to a {@link Listener}
 * on the main thread for haptic and spoken feedback (PR7).
 *
 * Everything runs on the device; no audio leaves the phone.
 */
public class ProAudioRecorder {

    private static final String TAG = "ProAudioRecorder";
    public static final int SAMPLE_RATE = 48000;
    private static final int LEVEL_REPORT_INTERVAL_MS = 50;
    private static final float CLIP_THRESHOLD = 0.999f;

    public interface Listener {
        /** Called on the main thread about every 50 ms while recording. */
        void onLevel(float peakDbfs, boolean clipped);

        /** Called on the main thread if recording fails after it started. */
        void onError(String message);
    }

    public interface SoundCheckCallback {
        /** Called on the main thread. peakDbfs includes the current input gain. */
        void onResult(float peakDbfs, int recommendedChangeDb);

        void onError(String message);
    }

    /** Capture configuration chosen for this device and the current settings. */
    public static class Config {
        public int audioSource;
        public String audioSourceName;
        public int channels;
        public boolean wav;              // 24-bit WAV (Manual gain) or AAC
        public int aacBitRate;
        /** Spoken when the device can't do what the settings asked for, else null. */
        public String fallbackMessage;

        public String fileExtension() {
            return wav ? ".wav" : ".m4a";
        }

        public String mimeType() {
            return wav ? "audio/x-wav" : "audio/mp4";
        }
    }

    /** File descriptors for the outputs; safety and original may be null. */
    public static class Outputs {
        public FileDescriptor main;
        public FileDescriptor safety;
        public FileDescriptor original;
    }

    private final Context context;
    private final ProRecordingSettings settings;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Config config;
    private AudioRecord audioRecord;
    private AutomaticGainControl agc;
    private AudioTrack monitorTrack;
    private ProAudioProcessor[] processors;
    private ProAudioFileWriter mainWriter;
    private ProAudioFileWriter safetyWriter;
    private ProAudioFileWriter originalWriter;
    private Thread thread;
    private volatile boolean running = false;
    private volatile int maxAmplitudeSinceLastQuery = 0;
    private volatile float gainDb;
    private Listener listener;
    private String monitoringNotice;

    public ProAudioRecorder(Context context, ProRecordingSettings settings) {
        this.context = context.getApplicationContext();
        this.settings = settings;
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    /** Picks the mic source, channel count and file format. Call before creating output files. */
    public Config prepare(boolean wantStereo) {
        Config c = new Config();
        ProRecordingSettings.GainMode gainMode = settings.getGainMode();
        c.channels = wantStereo ? 2 : 1;
        c.wav = gainMode.isManual();
        c.aacBitRate = c.channels == 2 ? 256000 : 128000;

        if (gainMode.isManual()) {
            if (isUnprocessedSupported(context)) {
                c.audioSource = MediaRecorder.AudioSource.UNPROCESSED;
                c.audioSourceName = "unprocessed microphone";
            } else {
                // VOICE_RECOGNITION is required by the Android CDD to have AGC off
                c.audioSource = MediaRecorder.AudioSource.VOICE_RECOGNITION;
                c.audioSourceName = "voice recognition microphone";
                c.fallbackMessage = "This phone can't fully turn off its audio processing; using the voice recognition microphone with auto gain off.";
            }
        } else {
            c.audioSource = MediaRecorder.AudioSource.MIC;
            c.audioSourceName = "microphone with auto gain";
        }
        this.config = c;
        return c;
    }

    public Config getConfig() {
        return config;
    }

    /** Spoken once at start if monitoring needs a warning (Bluetooth latency), else null. */
    public String getMonitoringNotice() {
        return monitoringNotice;
    }

    @SuppressLint("MissingPermission")
    public void start(Outputs outputs) throws IOException {
        if (config == null) throw new IllegalStateException("prepare() first");

        audioRecord = createAudioRecord(config);
        if (audioRecord == null && config.channels == 2) {
            Log.w(TAG, "Stereo capture failed, falling back to mono");
            config.channels = 1;
            config.aacBitRate = 128000;
            audioRecord = createAudioRecord(config);
        }
        if (audioRecord == null && config.audioSource != MediaRecorder.AudioSource.MIC) {
            Log.w(TAG, "Source " + config.audioSource + " failed, falling back to MIC");
            config.audioSource = MediaRecorder.AudioSource.MIC;
            config.audioSourceName = "microphone";
            config.fallbackMessage = "This phone can't turn off auto gain; using the standard microphone.";
            audioRecord = createAudioRecord(config);
        }
        if (audioRecord == null) throw new IOException("Unable to open the microphone");

        if (!settings.getGainMode().isManual() && AutomaticGainControl.isAvailable()) {
            try {
                agc = AutomaticGainControl.create(audioRecord.getAudioSessionId());
                if (agc != null) agc.setEnabled(true);
            } catch (Exception e) {
                Log.w(TAG, "AGC unavailable: " + e.getMessage());
            }
        }

        processors = new ProAudioProcessor[config.channels];
        for (int ch = 0; ch < config.channels; ch++) {
            processors[ch] = new ProAudioProcessor(SAMPLE_RATE);
        }
        applySettings();

        mainWriter = createWriter(outputs.main);
        if (outputs.safety != null) safetyWriter = createWriter(outputs.safety);
        if (outputs.original != null) originalWriter = createWriter(outputs.original);

        if (settings.isMonitoringEnabled()) {
            monitorTrack = createMonitorTrack(config.channels);
        }

        audioRecord.startRecording();
        running = true;
        thread = new Thread(this::recordLoop, "ProAudioRecorder");
        thread.start();
    }

    /** Stops capture and finalizes all files. Blocks until the encoders are flushed. */
    public void stop() {
        running = false;
        if (thread != null) {
            try {
                thread.join(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            thread = null;
        }
        closeQuietly(mainWriter);
        closeQuietly(safetyWriter);
        closeQuietly(originalWriter);
        mainWriter = safetyWriter = originalWriter = null;
        if (audioRecord != null) {
            try { audioRecord.stop(); } catch (Exception ignored) {}
            audioRecord.release();
            audioRecord = null;
        }
        if (agc != null) {
            agc.release();
            agc = null;
        }
        if (monitorTrack != null) {
            try { monitorTrack.stop(); } catch (Exception ignored) {}
            monitorTrack.release();
            monitorTrack = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    /** Re-reads focus, environment, noise reduction and gain from settings; safe while recording. */
    public void applySettings() {
        if (processors == null) return;
        ProRecordingSettings.Environment env = settings.getEnvironment();
        ProAudioProcessor.NoiseFocus focus = toProcessorFocus(settings.getFocus());
        int nr = settings.getEffectiveNoiseReduction();
        gainDb = settings.getGainMode().isManual() ? settings.getGainDb() : 0;
        boolean limiter = !settings.getGainMode().isManual() || settings.isLimiterInManual();
        for (ProAudioProcessor p : processors) {
            p.setLowCutHz(env.lowCutHz);
            p.setEchoControl(env.echoControl);
            p.setWindReduction(env.windReduction);
            p.setFocus(focus);
            p.setNoiseReductionPercent(nr);
            p.setGainDb(gainDb);
            p.setLimiterEnabled(limiter);
        }
    }

    /** Peak amplitude (0..32767) since the last call, like MediaRecorder.getMaxAmplitude(). */
    public int getMaxAmplitude() {
        int v = maxAmplitudeSinceLastQuery;
        maxAmplitudeSinceLastQuery = 0;
        return v;
    }

    public int getAudioSessionId() {
        return audioRecord != null ? audioRecord.getAudioSessionId() : 0;
    }

    private void recordLoop() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int channels = config.channels;
        int frames = ProAudioProcessor.HOP_SIZE;
        float[] interleaved = new float[frames * channels];
        float[] processed = new float[frames * channels];
        float[] safety = safetyWriter != null ? new float[frames * channels] : null;
        float[] chIn = new float[frames];
        float[] chOut = new float[frames];
        float[] chPre = new float[frames];

        long lastReport = 0;
        float windowPeak = 0f;
        boolean windowClipped = false;

        try {
            while (running) {
                int read = audioRecord.read(interleaved, 0, interleaved.length, AudioRecord.READ_BLOCKING);
                if (read <= 0) {
                    if (read < 0) throw new IOException("AudioRecord read error " + read);
                    continue;
                }
                int readFrames = read / channels;
                float safetyGain = (float) Math.pow(10.0, (gainDb + ProRecordingSettings.SAFETY_TRACK_OFFSET_DB) / 20.0);

                boolean clipped = false;
                for (int ch = 0; ch < channels; ch++) {
                    for (int i = 0; i < readFrames; i++) {
                        float x = interleaved[i * channels + ch];
                        if (Math.abs(x) >= CLIP_THRESHOLD) clipped = true;   // clipped before the app (hardware/converter)
                        chIn[i] = x;
                    }
                    processors[ch].process(chIn, 0, chOut, 0, readFrames, chPre);
                    for (int i = 0; i < readFrames; i++) {
                        processed[i * channels + ch] = chOut[i];
                        if (safety != null) safety[i * channels + ch] = chPre[i] * safetyGain;
                    }
                }

                float peak = ProAudioProcessor.peak(processed, 0, readFrames * channels);
                if (peak >= CLIP_THRESHOLD) clipped = true;   // clipped by the digital gain (limiter off)

                mainWriter.write(processed, 0, readFrames * channels);
                if (safetyWriter != null) safetyWriter.write(safety, 0, readFrames * channels);
                if (originalWriter != null) originalWriter.write(interleaved, 0, readFrames * channels);
                if (monitorTrack != null) {
                    monitorTrack.write(processed, 0, readFrames * channels, AudioTrack.WRITE_NON_BLOCKING);
                }

                int amp = (int) Math.min(32767f, peak * 32767f);
                if (amp > maxAmplitudeSinceLastQuery) maxAmplitudeSinceLastQuery = amp;

                windowPeak = Math.max(windowPeak, peak);
                windowClipped |= clipped;
                long now = System.currentTimeMillis();
                if (now - lastReport >= LEVEL_REPORT_INTERVAL_MS) {
                    final float dbfs = ProAudioProcessor.toDbfs(windowPeak);
                    final boolean c = windowClipped;
                    final Listener l = listener;
                    if (l != null) mainHandler.post(() -> l.onLevel(dbfs, c));
                    lastReport = now;
                    windowPeak = 0f;
                    windowClipped = false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Recording loop failed", e);
            running = false;
            final Listener l = listener;
            if (l != null) mainHandler.post(() -> l.onError("Recording problem: " + e.getMessage()));
        }
    }

    private ProAudioFileWriter createWriter(FileDescriptor fd) throws IOException {
        if (config.wav) {
            return ProAudioFileWriter.createWav(fd, SAMPLE_RATE, config.channels);
        }
        return ProAudioFileWriter.createAac(fd, SAMPLE_RATE, config.channels, config.aacBitRate);
    }

    @SuppressLint("MissingPermission")
    private static AudioRecord createAudioRecord(Config c) {
        int channelMask = c.channels == 2 ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
        int minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelMask, AudioFormat.ENCODING_PCM_FLOAT);
        if (minBuf <= 0) return null;
        try {
            AudioRecord rec = new AudioRecord.Builder()
                    .setAudioSource(c.audioSource)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(channelMask)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .build())
                    .setBufferSizeInBytes(Math.max(minBuf * 4, ProAudioProcessor.FFT_SIZE * 4 * c.channels * 4))
                    .build();
            if (rec.getState() != AudioRecord.STATE_INITIALIZED) {
                rec.release();
                return null;
            }
            return rec;
        } catch (Exception e) {
            Log.w(TAG, "AudioRecord init failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Monitoring only plays through headphones; on the loudspeaker it would feed back into the mic.
     * Bluetooth works but adds delay, so a warning is spoken.
     */
    private AudioTrack createMonitorTrack(int channels) {
        monitoringNotice = null;
        HeadphoneType type = detectHeadphones(context);
        if (type == HeadphoneType.NONE) return null;
        if (type == HeadphoneType.BLUETOOTH) {
            monitoringNotice = "Bluetooth headphones add delay to monitoring. Use wired headphones for real-time listening.";
        }
        try {
            int channelMask = channels == 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
            int minBuf = AudioTrack.getMinBufferSize(SAMPLE_RATE, channelMask, AudioFormat.ENCODING_PCM_FLOAT);
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(channelMask)
                            .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                            .build())
                    .setBufferSizeInBytes(minBuf)
                    .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.play();
            return track;
        } catch (Exception e) {
            Log.w(TAG, "Monitoring unavailable: " + e.getMessage());
            return null;
        }
    }

    public enum HeadphoneType { NONE, WIRED, BLUETOOTH }

    public static HeadphoneType detectHeadphones(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return HeadphoneType.NONE;
        boolean bluetooth = false;
        for (AudioDeviceInfo d : am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            switch (d.getType()) {
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                case AudioDeviceInfo.TYPE_USB_HEADSET:
                    return HeadphoneType.WIRED;
                case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                case AudioDeviceInfo.TYPE_BLE_HEADSET:
                    bluetooth = true;
                    break;
                default:
                    break;
            }
        }
        return bluetooth ? HeadphoneType.BLUETOOTH : HeadphoneType.NONE;
    }

    public static boolean isUnprocessedSupported(Context context) {
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return am != null && "true".equals(am.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED));
    }

    static ProAudioProcessor.NoiseFocus toProcessorFocus(ProRecordingSettings.Focus focus) {
        switch (focus) {
            case MUSIC: return ProAudioProcessor.NoiseFocus.MUSIC;
            case NATURAL: return ProAudioProcessor.NoiseFocus.NATURAL;
            case VOICE:
            default: return ProAudioProcessor.NoiseFocus.VOICE;
        }
    }

    private static void closeQuietly(ProAudioFileWriter w) {
        if (w == null) return;
        try {
            w.close();
        } catch (Exception e) {
            Log.w(TAG, "Closing writer failed: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------------------------------
    // Sound check (PR6)
    // ---------------------------------------------------------------------------------------

    public static final int SOUND_CHECK_MS = 5000;

    /**
     * Listens for 5 seconds with the same mic source the recording would use, and reports the
     * peak level (including the current input gain) and a recommended gain change.
     */
    @SuppressLint("MissingPermission")
    public static void runSoundCheck(Context context, ProRecordingSettings settings, SoundCheckCallback callback) {
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            ProAudioRecorder probe = new ProAudioRecorder(context, settings);
            Config c = probe.prepare(false);
            AudioRecord rec = createAudioRecord(c);
            if (rec == null) {
                main.post(() -> callback.onError("Could not open the microphone for the sound check."));
                return;
            }
            float peak = 0f;
            try {
                rec.startRecording();
                float[] buf = new float[1024];
                long end = System.currentTimeMillis() + SOUND_CHECK_MS;
                while (System.currentTimeMillis() < end) {
                    int n = rec.read(buf, 0, buf.length, AudioRecord.READ_BLOCKING);
                    if (n > 0) peak = Math.max(peak, ProAudioProcessor.peak(buf, 0, n));
                }
            } catch (Exception e) {
                main.post(() -> callback.onError("Sound check failed: " + e.getMessage()));
                return;
            } finally {
                try { rec.stop(); } catch (Exception ignored) {}
                rec.release();
            }
            int gain = settings.getGainMode().isManual() ? settings.getGainDb() : 0;
            float peakDbfs = ProAudioProcessor.toDbfs(peak) + gain;
            int change = ProAudioProcessor.recommendGainChangeDb(peakDbfs);
            main.post(() -> callback.onResult(peakDbfs, change));
        }, "ProSoundCheck").start();
    }
}
