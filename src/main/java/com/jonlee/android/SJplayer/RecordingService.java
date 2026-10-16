package com.jonlee.android.SJplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

/**
 * Microphone foreground service (PRD R10, C3). It keeps the app's microphone access alive while a
 * recording runs with the screen off, in a pocket or behind another app. Android silences the mic
 * of a background app that has no foreground service of type "microphone".
 *
 * The recording itself stays in {@link Recorder}; this service only holds the foreground state,
 * a partial wake lock and a quiet notification ("Recording, 12 minutes") with a Stop action.
 * Stop goes through {@link Commander#cmd(int)}, so it saves the file and speaks like a double tap.
 */
public class RecordingService extends Service {
    private static final String TAG = "SJRecorder.RecService";

    public static final String CHANNEL_ID = "eyes_free_recording_status";
    private static final int NOTIFICATION_ID = 4210;
    private static final int SAVED_NOTIFICATION_ID = 4211;

    private static final String ACTION_START = "com.jonlee.android.SJplayer.action.RECORDING_START";
    private static final String ACTION_STOP = "com.jonlee.android.SJplayer.action.RECORDING_STOP";
    private static final String EXTRA_START_MILLIS = "start_millis";
    private static final String EXTRA_PRO = "pro";

    // Long recordings are the point; the timeout only guards against a leaked lock.
    private static final long WAKE_LOCK_TIMEOUT_MS = 12L * 60 * 60 * 1000;

    /** The running service, or null. Same process as the activity, so a plain reference is enough. */
    private static RecordingService running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;
    private long startMillis;
    private boolean pro;
    private boolean inForeground;

    private final Runnable minuteTick = new Runnable() {
        @Override
        public void run() {
            updateNotification();
        }
    };

    /**
     * Called by {@link Recorder} once the microphone is capturing. Must be called while the app is
     * in the foreground (the user just started recording). If the service already runs, for example
     * when a Pro WAV file is split at 4 GB, it keeps running and the clock carries on.
     */
    public static void start(Context context, long recordStartMillis, boolean proMode) {
        if (running != null) {
            running.pro = proMode;
            running.updateNotification();
            return;
        }
        Intent intent = new Intent(context, RecordingService.class)
                .setAction(ACTION_START)
                .putExtra(EXTRA_START_MILLIS, recordStartMillis)
                .putExtra(EXTRA_PRO, proMode);
        try {
            ContextCompat.startForegroundService(context, intent);
        } catch (Exception e) {
            // ForegroundServiceStartNotAllowedException (app already in background) or SecurityException
            Log.e(TAG, "Could not start the recording service", e);
            warnNotProtected();
        }
    }

    /** Called by {@link Recorder} when a recording ends. */
    public static void stop(Context context) {
        if (running != null) {
            running.shutDown();
        } else if (context != null) {
            context.stopService(new Intent(context, RecordingService.class));
        }
    }

    /**
     * The activity was closed (for example swiped away from recent apps) during a recording, so
     * the recording was saved and stopped. Speech is no longer available; leave a notification so
     * it never ends silently.
     */
    public static void notifyStoppedBecauseAppClosed(Context context) {
        try {
            createChannel(context);
            String text = "Recording stopped and saved because the app was closed.";
            Notification n = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_action_stop)
                    .setContentTitle("Recording saved")
                    .setContentText(text)
                    .setTicker(text)
                    .setContentIntent(openAppIntent(context))
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build();
            context.getSystemService(NotificationManager.class).notify(SAVED_NOTIFICATION_ID, n);
        } catch (Exception e) {
            Log.w(TAG, "Could not post the saved notification", e);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = this;
        createChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_STOP.equals(action)) {
            Log.i(TAG, "Stop pressed in the notification");
            stopRecordingFromNotification();
            return START_NOT_STICKY;
        }
        if (intent != null) {
            startMillis = intent.getLongExtra(EXTRA_START_MILLIS, System.currentTimeMillis());
            pro = intent.getBooleanExtra(EXTRA_PRO, false);
        } else if (startMillis == 0) {
            startMillis = System.currentTimeMillis();
        }
        try {
            Notification n = buildNotification();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
            } else {
                startForeground(NOTIFICATION_ID, n);
            }
            inForeground = true;
        } catch (Exception e) {
            Log.e(TAG, "startForeground failed", e);
            warnNotProtected();
            shutDown();
            return START_NOT_STICKY;
        }
        acquireWakeLock();
        scheduleTick();
        Log.i(TAG, "Recording service in foreground (microphone)");
        // Never restart on its own: a restarted service would hold no recording
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(minuteTick);
        releaseWakeLock();
        if (running == this) running = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void shutDown() {
        handler.removeCallbacks(minuteTick);
        releaseWakeLock();
        if (inForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
            inForeground = false;
        }
        if (running == this) running = null;
        stopSelf();
    }

    /** Same path as a double tap: saves the file, gives the stop haptic and speaks "Recording stopped". */
    private void stopRecordingFromNotification() {
        MainActivity activity = MainActivity.getInstance();
        Commander commander = activity != null ? activity.getCommander() : null;
        if (commander != null && commander.getRecorder() != null && commander.getRecorder().isRecording()) {
            commander.cmd(Commander.STOP_RECORD);
        }
        // Recorder.stopRecording() normally stops the service; make sure it is gone either way
        shutDown();
    }

    private void updateNotification() {
        if (!inForeground) return;
        try {
            getSystemService(NotificationManager.class).notify(NOTIFICATION_ID, buildNotification());
        } catch (Exception e) {
            Log.w(TAG, "Could not update the recording notification", e);
        }
        scheduleTick();
    }

    private void scheduleTick() {
        handler.removeCallbacks(minuteTick);
        long elapsed = System.currentTimeMillis() - startMillis;
        handler.postDelayed(minuteTick, RecordingNotificationText.millisUntilNextMinute(elapsed));
    }

    private Notification buildNotification() {
        long elapsed = System.currentTimeMillis() - startMillis;
        String title = RecordingNotificationText.title(elapsed, pro);
        PendingIntent stopIntent = PendingIntent.getService(this, 1,
                new Intent(this, RecordingService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_action_mic)
                .setContentTitle(title)
                .setContentText("Stop saves the recording.")
                .setTicker(title)
                .setContentIntent(openAppIntent(this))
                // "Stop recording" so a screen reader announces the button by what it does
                .addAction(R.drawable.ic_action_stop, "Stop recording", stopIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setShowWhen(false)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private static PendingIntent openAppIntent(Context context) {
        Intent open = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        return PendingIntent.getActivity(context, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) return;
        try {
            nm.deleteNotificationChannel("eyes_free_recording");
        } catch (Exception ignored) {}
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Recording", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Shows while a recording is running, with a Stop button.");
        channel.enableVibration(false);
        channel.setSound(null, null);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(channel);
    }

    private void acquireWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) return;
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm == null) return;
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EyesFree:Recording");
        wakeLock.setReferenceCounted(false);
        wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS);
    }

    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            try { wakeLock.release(); } catch (Exception ignored) {}
        }
        wakeLock = null;
    }

    /** Recording goes on, but Android may silence it with the screen off. Say so; never fail silently. */
    private static void warnNotProtected() {
        new Handler(Looper.getMainLooper()).post(() -> {
            MainActivity activity = MainActivity.getInstance();
            if (activity == null) return;
            if (activity.getCommander() != null) activity.getCommander().vibrateBoundary();
            activity.alwaysSpeak("Warning: background recording is not available. Keep the screen on and the app open, or the recording may go silent.");
        });
    }
}
