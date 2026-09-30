package com.jonlee.android.SJplayer;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MicrophoneInfo;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Manages companion metadata for audio recordings:
 * - Natural language time-of-day phrases ("Saturday afternoon", "Yesterday morning")
 * - Audio input source detection (Phone Mic, Bluetooth Headset, Wired Earphones)
 * - Stereo / Mono channel detection and recording support
 * - Audio bookmarks (jumping, adding index marks)
 * - Favorite / star toggling
 * - Cached reverse-geocoded place names
 */
public class AudioMetadataHelper {
    private static final String TAG = "AudioMetadataHelper";

    public static class AudioMetadata {
        public String fileName;
        public long recordedAt;
        public String timeOfDay;
        public String audioSource;
        public String locationName;
        public double latitude = 0.0;
        public double longitude = 0.0;
        public boolean isFavorite = false;
        public int channels = 1;
        public String channelMode = "Mono";
        public List<Integer> bookmarks = new ArrayList<>();

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("fileName", fileName != null ? fileName : "");
                json.put("recordedAt", recordedAt);
                json.put("timeOfDay", timeOfDay != null ? timeOfDay : "");
                json.put("audioSource", audioSource != null ? audioSource : "");
                json.put("locationName", locationName != null ? locationName : "");
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("isFavorite", isFavorite);
                json.put("channels", channels);
                json.put("channelMode", channelMode != null ? channelMode : (channels == 2 ? "Stereo" : "Mono"));
                JSONArray bmArray = new JSONArray();
                for (Integer bm : bookmarks) {
                    bmArray.put(bm);
                }
                json.put("bookmarks", bmArray);
            } catch (Exception e) {
                Log.w(TAG, "Error serializing metadata to JSON: " + e.getMessage());
            }
            return json;
        }

        public static AudioMetadata fromJson(JSONObject json) {
            AudioMetadata meta = new AudioMetadata();
            if (json == null) return meta;
            meta.fileName = json.optString("fileName", "");
            meta.recordedAt = json.optLong("recordedAt", 0);
            meta.timeOfDay = json.optString("timeOfDay", "");
            meta.audioSource = json.optString("audioSource", "");
            meta.locationName = json.optString("locationName", "");
            meta.latitude = json.optDouble("latitude", 0.0);
            meta.longitude = json.optDouble("longitude", 0.0);
            meta.isFavorite = json.optBoolean("isFavorite", false);
            meta.channels = json.optInt("channels", 1);
            meta.channelMode = json.optString("channelMode", meta.channels == 2 ? "Stereo" : "Mono");
            JSONArray bmArray = json.optJSONArray("bookmarks");
            if (bmArray != null) {
                for (int i = 0; i < bmArray.length(); i++) {
                    meta.bookmarks.add(bmArray.optInt(i));
                }
            }
            Collections.sort(meta.bookmarks);
            return meta;
        }
    }

    private static File getInternalMetaFile(Context context, File audioFile) {
        if (context == null || audioFile == null) return null;
        File metaDir = new File(context.getFilesDir(), "audio_meta");
        if (!metaDir.exists()) metaDir.mkdirs();
        return new File(metaDir, audioFile.getName() + ".meta.json");
    }

    private static File getCompanionMetaFile(File audioFile) {
        if (audioFile == null || audioFile.getParentFile() == null) return null;
        return new File(audioFile.getParentFile(), "." + audioFile.getName() + ".meta");
    }

    private static AudioMetadata readMetaFromFile(File file) {
        if (file == null || !file.exists()) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return AudioMetadata.fromJson(new JSONObject(sb.toString()));
        } catch (Exception e) {
            Log.w(TAG, "Failed to read metadata from " + file.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }
    }

    private static boolean writeMetaToFile(File file, AudioMetadata meta) {
        if (file == null || meta == null) return false;
        try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(meta.toJson().toString());
            writer.flush();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Failed to write metadata to " + file.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }
    }

    public static AudioMetadata loadMetadata(Context context, File audioFile) {
        if (audioFile == null) return new AudioMetadata();

        // 1. Try reading companion file next to audio file
        File companion = getCompanionMetaFile(audioFile);
        if (companion != null && companion.exists() && companion.canRead()) {
            AudioMetadata meta = readMetaFromFile(companion);
            if (meta != null) {
                if (meta.recordedAt <= 0) {
                    meta.recordedAt = extractRecordedTimestamp(audioFile);
                }
                meta.timeOfDay = getTimeOfDayContext(meta.recordedAt);
                return meta;
            }
        }

        // 2. Try reading from app internal storage
        File internal = getInternalMetaFile(context, audioFile);
        if (internal != null && internal.exists() && internal.canRead()) {
            AudioMetadata meta = readMetaFromFile(internal);
            if (meta != null) {
                if (meta.recordedAt <= 0) {
                    meta.recordedAt = extractRecordedTimestamp(audioFile);
                }
                meta.timeOfDay = getTimeOfDayContext(meta.recordedAt);
                return meta;
            }
        }

        // 3. Fallback: default metadata dynamically derived from file
        AudioMetadata meta = new AudioMetadata();
        meta.fileName = audioFile.getName();
        meta.recordedAt = extractRecordedTimestamp(audioFile);
        meta.timeOfDay = getTimeOfDayContext(meta.recordedAt);
        meta.audioSource = "Phone Microphone";
        return meta;
    }

    /**
     * Extracts recorded timestamp from filename or parent folder if available (e.g. 2018-02-24-083034.m4a),
     * falling back to audioFile.lastModified().
     */
    public static long extractRecordedTimestamp(File audioFile) {
        if (audioFile == null) return 0;
        String name = audioFile.getName();
        // Pattern 1: YYYY-MM-DD-HHmmss (e.g. 2018-02-24-083034.m4a)
        if (name.length() >= 17 && name.charAt(4) == '-' && name.charAt(7) == '-' && (name.charAt(10) == '-' || name.charAt(10) == '_')) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US);
                Date d = sdf.parse(name.substring(0, 17).replace('_', '-'));
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        // Pattern 2: YYYY-MM-DD (e.g. 2018-02-24...)
        if (name.length() >= 10 && name.charAt(4) == '-' && name.charAt(7) == '-') {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date d = sdf.parse(name.substring(0, 10));
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        // Pattern 3: Parent folder is YYYY-MM-DD (e.g. 2018-02-24)
        File parent = audioFile.getParentFile();
        if (parent != null) {
            String pName = parent.getName();
            if (pName.length() >= 10 && pName.charAt(4) == '-' && pName.charAt(7) == '-') {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    Date d = sdf.parse(pName.substring(0, 10));
                    if (d != null) return d.getTime();
                } catch (Exception ignored) {}
            }
        }
        return audioFile.lastModified();
    }

    public static boolean saveMetadata(Context context, File audioFile, AudioMetadata meta) {
        if (audioFile == null || meta == null) return false;
        boolean success = false;

        // 1. Save to app internal storage (guaranteed writable across all API versions)
        File internal = getInternalMetaFile(context, audioFile);
        if (internal != null) {
            success = writeMetaToFile(internal, meta);
        }

        // 2. Also try writing companion file next to audio file
        File companion = getCompanionMetaFile(audioFile);
        if (companion != null) {
            try {
                writeMetaToFile(companion, meta);
            } catch (Exception ignored) {
            }
        }

        return success;
    }

    public static void deleteMetadata(Context context, File audioFile) {
        if (audioFile == null) return;
        try {
            File internal = getInternalMetaFile(context, audioFile);
            if (internal != null && internal.exists()) internal.delete();

            File companion = getCompanionMetaFile(audioFile);
            if (companion != null && companion.exists()) companion.delete();
        } catch (Exception ignored) {
        }
    }

    /**
     * Converts a timestamp into natural conversational date and time:
     * e.g. "Today at 2:30 PM", "Yesterday at 4:15 PM", "March 3, 2019 at 7:04 PM"
     */
    public static String getTimeOfDayContext(long timestamp) {
        if (timestamp <= 0) return "Recent";

        Calendar fileCal = Calendar.getInstance();
        fileCal.setTimeInMillis(timestamp);

        Calendar now = Calendar.getInstance();

        boolean hasTime = !(fileCal.get(Calendar.HOUR_OF_DAY) == 0 &&
                            fileCal.get(Calendar.MINUTE) == 0 &&
                            fileCal.get(Calendar.SECOND) == 0);

        SimpleDateFormat timeFmt = new SimpleDateFormat("h:mm a", Locale.US);
        String timeStr = hasTime ? (" at " + timeFmt.format(new Date(timestamp))) : "";

        boolean isToday = (fileCal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                fileCal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR));

        long diffDays = (now.getTimeInMillis() - timestamp) / (24 * 60 * 60 * 1000L);
        boolean isCurrentYear = (fileCal.get(Calendar.YEAR) == now.get(Calendar.YEAR));

        if (isToday) {
            return hasTime ? ("Today" + timeStr) : "Today";
        } else if (diffDays == 1 || (diffDays == 0 && !isToday)) {
            return hasTime ? ("Yesterday" + timeStr) : "Yesterday";
        } else if (diffDays > 0 && diffDays < 7 && isCurrentYear) {
            SimpleDateFormat dayOfWeekFmt = new SimpleDateFormat("EEEE", Locale.US);
            return dayOfWeekFmt.format(new Date(timestamp)) + timeStr;
        } else if (isCurrentYear) {
            SimpleDateFormat monthDayFmt = new SimpleDateFormat("MMMM d", Locale.US);
            return monthDayFmt.format(new Date(timestamp)) + timeStr;
        } else {
            SimpleDateFormat fullFmt = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
            return fullFmt.format(new Date(timestamp)) + timeStr;
        }
    }

    /**
     * Detects current audio input source: Phone Microphone, Bluetooth Headset, or Wired Earphones.
     */
    public static String getCurrentAudioInputSource(Context context) {
        if (context == null) return "Phone Microphone";
        try {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (am == null) return "Phone Microphone";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
                for (AudioDeviceInfo dev : devices) {
                    int type = dev.getType();
                    if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                        return "Bluetooth Headset";
                    } else if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES) {
                        return "Wired Earphones";
                    } else if (type == AudioDeviceInfo.TYPE_USB_DEVICE || type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                        return "USB Microphone";
                    }
                }
            }

            if (am.isBluetoothScoOn()) {
                return "Bluetooth Headset";
            }
            if (am.isWiredHeadsetOn()) {
                return "Wired Earphones";
            }
        } catch (Exception e) {
            Log.w(TAG, "Audio input detection error: " + e.getMessage());
        }
        return "Phone Microphone";
    }

    public static boolean toggleFavorite(Context context, File audioFile) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        meta.isFavorite = !meta.isFavorite;
        saveMetadata(context, audioFile, meta);
        return meta.isFavorite;
    }

    public static boolean isFavorite(Context context, File audioFile) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        return meta.isFavorite;
    }

    public static int addBookmark(Context context, File audioFile, int positionMs) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        for (int bm : meta.bookmarks) {
            if (Math.abs(bm - positionMs) < 2000) {
                return meta.bookmarks.indexOf(bm) + 1;
            }
        }
        meta.bookmarks.add(positionMs);
        Collections.sort(meta.bookmarks);
        saveMetadata(context, audioFile, meta);
        return meta.bookmarks.indexOf(positionMs) + 1;
    }

    public static List<Integer> getBookmarks(Context context, File audioFile) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        return meta.bookmarks;
    }

    public static Integer getNextBookmark(Context context, File audioFile, int currentPositionMs) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        if (meta.bookmarks.isEmpty()) return null;
        for (int bm : meta.bookmarks) {
            if (bm > currentPositionMs + 500) {
                return bm;
            }
        }
        return meta.bookmarks.get(0);
    }

    public static Integer getPreviousBookmark(Context context, File audioFile, int currentPositionMs) {
        AudioMetadata meta = loadMetadata(context, audioFile);
        if (meta.bookmarks.isEmpty()) return null;
        for (int i = meta.bookmarks.size() - 1; i >= 0; i--) {
            int bm = meta.bookmarks.get(i);
            if (bm < currentPositionMs - 1000) {
                return bm;
            }
        }
        return meta.bookmarks.get(meta.bookmarks.size() - 1);
    }

    public static int getBookmarkIndex(List<Integer> bookmarks, int positionMs) {
        if (bookmarks == null || bookmarks.isEmpty()) return 0;
        for (int i = 0; i < bookmarks.size(); i++) {
            if (Math.abs(bookmarks.get(i) - positionMs) <= 1000) {
                return i + 1;
            }
        }
        return 1;
    }

    public static String formatDuration(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = (millis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.US, "%d hour%s %d minute%s", hours, hours > 1 ? "s" : "", minutes, minutes > 1 ? "s" : "");
        } else if (minutes > 1) {
            return String.format(Locale.US, "%d minutes %d seconds", minutes, seconds);
        } else if (minutes == 1) {
            return String.format(Locale.US, "1 minute %d seconds", seconds);
        } else {
            return String.format(Locale.US, "%d seconds", seconds);
        }
    }

    /**
     * Checks if the device hardware and audio HAL support stereo (2-channel) audio recording.
     * Verifies PCM 16-bit stereo buffer allocation and checks for physical microphone hardware.
     */
    public static boolean isStereoRecordingSupported(Context context) {
        try {
            // 1. Check AudioRecord minimum buffer size for 2-channel stereo at standard sample rates
            int minBuf44 = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            int minBuf48 = AudioRecord.getMinBufferSize(48000, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);
            boolean pcmStereoSupported = (minBuf44 > 0 || minBuf48 > 0);

            if (!pcmStereoSupported) {
                return false;
            }

            // 2. On Android 9 (API 28+), inspect physical microphone count
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && context != null) {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    try {
                        List<MicrophoneInfo> mics = am.getMicrophones();
                        if (mics != null && !mics.isEmpty()) {
                            int builtInMics = 0;
                            for (MicrophoneInfo mic : mics) {
                                int type = mic.getType();
                                if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                                    builtInMics++;
                                }
                            }
                            if (builtInMics >= 2) {
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "getMicrophones error: " + e.getMessage());
                    }
                }
            }

            // 3. On Android 6+ (API 23+), inspect AudioDeviceInfo input channel counts
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null) {
                AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    AudioDeviceInfo[] devices = am.getDevices(AudioManager.GET_DEVICES_INPUTS);
                    if (devices != null) {
                        for (AudioDeviceInfo dev : devices) {
                            if (dev.getType() == AudioDeviceInfo.TYPE_BUILTIN_MIC) {
                                int[] channelCounts = dev.getChannelCounts();
                                if (channelCounts != null) {
                                    for (int count : channelCounts) {
                                        if (count >= 2) return true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            return pcmStereoSupported;
        } catch (Exception e) {
            Log.w(TAG, "isStereoRecordingSupported exception: " + e.getMessage());
            return false;
        }
    }

    /**
     * Inspects an audio file using MediaExtractor to read its exact channel count.
     * Returns 2 for Stereo, 1 for Mono (or fallback 1 if undetectable).
     */
    public static int getAudioFileChannels(File audioFile) {
        if (audioFile == null || !audioFile.exists()) return 1;
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(audioFile.getAbsolutePath());
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        return format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    }
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                extractor.release();
            } catch (Exception ignored) {}
        }
        return 1;
    }
}
