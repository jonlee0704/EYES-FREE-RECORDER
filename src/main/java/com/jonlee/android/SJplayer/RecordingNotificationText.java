package com.jonlee.android.SJplayer;

/**
 * Spoken-friendly text for the recording notification, e.g. "Recording, 12 minutes".
 * Plain Java so it can be unit tested. Whole minutes only: screen readers read the
 * notification as words, and a ticking seconds counter would be noise.
 */
public final class RecordingNotificationText {

    private RecordingNotificationText() {}

    /** "Recording, less than a minute", "Recording, 1 minute", "Pro recording, 1 hour 5 minutes". */
    public static String title(long elapsedMillis, boolean pro) {
        return (pro ? "Pro recording, " : "Recording, ") + duration(elapsedMillis);
    }

    public static String duration(long elapsedMillis) {
        long minutes = Math.max(0, elapsedMillis) / 60_000L;
        if (minutes == 0) return "less than a minute";
        long hours = minutes / 60;
        long rest = minutes % 60;
        if (hours == 0) return plural(rest, "minute");
        if (rest == 0) return plural(hours, "hour");
        return plural(hours, "hour") + " " + plural(rest, "minute");
    }

    /** Milliseconds until the spoken minute count changes, so the notification updates on time. */
    public static long millisUntilNextMinute(long elapsedMillis) {
        long e = Math.max(0, elapsedMillis);
        return 60_000L - (e % 60_000L);
    }

    private static String plural(long n, String unit) {
        return n + " " + unit + (n == 1 ? "" : "s");
    }
}
