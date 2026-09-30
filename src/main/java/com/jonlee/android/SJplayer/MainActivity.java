/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/




package com.jonlee.android.SJplayer;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.core.app.ActivityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Button;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.widget.Toast;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.jonlee.android.common.activities.SampleActivityBase;
import android.util.Log;
import com.jonlee.android.common.logger.LogWrapper;
import com.jonlee.android.common.logger.MessageOnlyLogFilter;
import com.jonlee.android.common.utils.MathTest;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;




/**
 * A simple launcher activity containing a summary sample description
 * and a few action bar buttons.
 */
public class MainActivity extends SampleActivityBase {

    private static MainActivity instance;
    public static MainActivity getInstance() { return instance; }

    public static final String TAG = "MainActivity";
    public static final String FRAGTAG = "BasicGestureDetectFragment";
    static final int REQUEST_ACCOUNT_PICKER = 2;

    // Place to manage all gesture commands
    public Commander commander = null;
    public Recorder recorder;
    private GestureDetector gestureDetector;
    private GestureListener gestureListener;
    private boolean isSangJoon = false;
    private boolean isTTSEnabled = true;
    private boolean isMathTestEnabled = false;
    private boolean isHomemodeEnabled = false;
    private boolean isNavModeEnabled = false;
    private int maxVolume = 100;

    public TextToSpeech ttobj = null;
    private String pendingTtsMessage = null;
    public int batteryLevel = 0;

    private final BroadcastReceiver becomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                if (recorder != null && recorder.isPlaying()) {
                    recorder.pause();
                    speak("Headphones disconnected. Playback paused.");
                }
            }
        }
    };

    private TextView progressTextView;
    private String foldersFromSettings;
    private String homeworkFoldersFromSettings;
    private String homeworkFilePrefix;
    private int pokingEyesWarning;
    private int mathtestFrequency;
    private int mathtestCountOnStart;
    private int mathtestCountAfterStart;
    private int mathtestMultiplyTestScope;

    private int mathtestStartingNumber;
    private int mathtestEndingNumber;
    private int mathTestMode;

    private NotificationManager notificationManager;
    private GoogleAccountCredential mCredential;
    private DriveMainActivity driveMainActivity;
    private MathTest mathTest;
    private SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener;
    private SharedPreferences sharedPref;

    private TextView trackPos_TextView;
    private TextView main_TextView;

    private final Handler wheelScrubHandler = new Handler(Looper.getMainLooper());
    private boolean isWheelScrubbing = false;
    private float wheelDownX = 0;
    private float wheelDownY = 0;
    private float wheelLastStepY = 0;
    private long lastWheelStepTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        createNotificationChannel();
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        /**
         * mathtestMultipleyTestScope value is set in setSettingsValues.
         */
        setSettingsValues();
        mathTest = new MathTest(mathtestStartingNumber, mathtestEndingNumber, mathTestMode);

        //Remove title bar
        //this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // TODO: Only when app runs recorder
        //this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        applyFullScreenMode();

        setContentView(R.layout.activity_main);

        main_TextView = (TextView) findViewById(R.id.main_TextView);
        if (main_TextView != null) {
            main_TextView.setSelected(true);
        }
        progressTextView = (TextView) findViewById(R.id.progress_TextView);
        trackPos_TextView = (TextView) findViewById(R.id.trackPos_TextView);

        updateTtsButtonState();
//        main_TextView.setText(new String(Character.toChars(0x1F4C1)) + " Swipe Up/Down: Folder navigation\n"
//                + new String(Character.toChars(0x1F4C3))  + " Swipe Left/Right: File navigation\n"
//                + new String(Character.toChars(0x25B6)) + " Single Tab: Play/Stop\n"
//                + new String(Character.toChars(0x3030)) + " Double Tab: Record\n"
//                + new String(Character.toChars(0x1F446)) + " long press: File information\n"
//                + new String(Character.toChars(0x270C)) + " long press: Date and time\n"
//                + new String(Character.toChars(0x1F4C1)) + "x2 Swipe Up/Down and Hold: \t \t \t Faster Folder navigation\n"
//                + new String(Character.toChars(0x25B6))
//                        + "x2 Swipe Left/Right and Hold: \t \t \t Faster forward and Rewind\n"
//
//        );
        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    ttobj.setLanguage(Locale.US);
                    ttobj.setSpeechRate(2.5f);
                    if (pendingTtsMessage != null) {
                        speak(pendingTtsMessage);
                        pendingTtsMessage = null;
                    }
                }
            }
        });

        commander = new Commander(this);
        recorder = commander.getRecorder();
        int savedVisualizerMode = DialView.VISUALIZER_MODE_MINIMAL_SPLIT;
        try {
            if (sharedPref.contains("pref_visualizer_mode_str")) {
                savedVisualizerMode = Integer.parseInt(sharedPref.getString("pref_visualizer_mode_str", "0"));
            } else {
                savedVisualizerMode = sharedPref.getInt("pref_visualizer_mode", DialView.VISUALIZER_MODE_MINIMAL_SPLIT);
            }
        } catch (Exception ignored) {}
        if (savedVisualizerMode < 0 || savedVisualizerMode >= DialView.NUM_VISUALIZER_MODES) {
            savedVisualizerMode = DialView.VISUALIZER_MODE_MINIMAL_SPLIT;
        }
        boolean hapticPulse = sharedPref.getBoolean("pref_haptic_audio_pulse", true);
        if (commander.dialView != null) {
            commander.dialView.setVisualizerMode(savedVisualizerMode);
            commander.dialView.setHapticPulseEnabled(hapticPulse);
        }
        updateVisualizerModeUI(savedVisualizerMode);



        View btnHelp = findViewById(R.id.btn_help);
        setupTopButtonTouchAction(btnHelp,
                new TopButtonNarrativeProvider() {
                    @Override
                    public String getNarrative() {
                        return "Help guide. Tap to view guide. Long press on help page to hear full details.";
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        showGestureHelpDialog(false);
                    }
                });

        View gestureHintBar = findViewById(R.id.gesture_hint_bar);
        if (gestureHintBar != null) {
            gestureHintBar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showGestureHelpDialog(false);
                }
            });
            gestureHintBar.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {}
                    showGestureHelpDialog(true);
                    return true;
                }
            });
        }

        View btnSettings = findViewById(R.id.btn_settings);
        setupTopButtonTouchAction(btnSettings,
                new TopButtonNarrativeProvider() {
                    @Override
                    public String getNarrative() {
                        return "Settings. You can change app settings, audio quality, and Google Drive cloud backup.";
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (commander != null) {
                            commander.cmd(Commander.SETTINGS);
                        } else {
                            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                        }
                    }
                });

        setupTopButtonTouchAction(progressTextView,
                new TopButtonNarrativeProvider() {
                    @Override
                    public String getNarrative() {
                        return isTTSEnabled
                                ? "Voice feedback is currently on. Release your touch if you want to turn it off."
                                : "Voice feedback is currently off. Release your touch to turn voice feedback on.";
                    }
                },
                new Runnable() {
                    @Override
                    public void run() {
                        if (commander != null) {
                            commander.cmd(Commander.TURN_ON_OFF_TTS);
                        }
                        updateTtsButtonState();
                    }
                });

        setupGestureDetection();

        requestPermissions();
    }

    private interface TopButtonNarrativeProvider {
        String getNarrative();
    }

    /**
     * Top header buttons (Help, Settings, TTS-On/Off):
     * - On Touch Down: Speaks conversational narrative explaining what the button does, with visual highlight.
     * - On Touch Release (inside button): Runs the menu / toggles the setting immediately upon release.
     * - If user drags finger outside before releasing: cancels action without running the menu.
     */
    private void setupTopButtonTouchAction(final View buttonView, final TopButtonNarrativeProvider narrativeProvider, final Runnable onReleaseAction) {
        if (buttonView == null) return;

        buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onReleaseAction != null) {
                    onReleaseAction.run();
                }
            }
        });

        buttonView.setOnTouchListener(new View.OnTouchListener() {
            private boolean isDownInside = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        isDownInside = true;
                        v.animate().scaleX(1.12f).scaleY(1.12f).setDuration(120).start();
                        if (narrativeProvider != null) {
                            String narrative = narrativeProvider.getNarrative();
                            if (narrative != null && !narrative.isEmpty()) {
                                alwaysSpeak(narrative);
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isDownInside) {
                            float x = event.getX();
                            float y = event.getY();
                            boolean inside = (x >= 0 && x <= v.getWidth() && y >= 0 && y <= v.getHeight());
                            if (!inside) {
                                isDownInside = false;
                                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        if (isDownInside) {
                            isDownInside = false;
                            v.performClick();
                            if (onReleaseAction != null) {
                                onReleaseAction.run();
                            }
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        isDownInside = false;
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start();
                        return true;
                }
                return false;
            }
        });
    }

    /**
     * Set up full-screen gesture detection on main layout.
     * Horizontal edge swipes (left/right screen edges) are passed through to the Android system
     * so that system back gesture navigation is not intercepted or confused with song skipping.
     * Content area swipes (inside screen) smoothly trigger app commands.
     */
    private void setupGestureDetection() {
        gestureListener = new GestureListener(this);
        gestureDetector = new GestureDetector(this, gestureListener);
        gestureDetector.setIsLongpressEnabled(false);

        View mainLayout = findViewById(R.id.main_layout);
        if (mainLayout != null) {
            mainLayout.setClickable(true);
            mainLayout.setFocusable(true);
            mainLayout.setOnTouchListener(new View.OnTouchListener() {
                private boolean isDownInEdgeZone = false;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    int action = event.getActionMasked();
                    float rawX = event.getRawX();
                    float rawY = event.getRawY();

                    if (action == MotionEvent.ACTION_DOWN) {
                        isDownInEdgeZone = isHorizontalEdgeTouch(event);
                        if (isDownInEdgeZone) {
                            Log.d(TAG, "Touch DOWN in horizontal edge zone, passing to system navigation: rawX=" + rawX);
                            return false;
                        }

                        wheelDownX = rawX;
                        wheelDownY = rawY;
                        wheelLastStepY = rawY;
                        isWheelScrubbing = false;

                        // Schedule long-press activation for wheel controller scrubbing
                        wheelScrubHandler.removeCallbacksAndMessages(null);
                        wheelScrubHandler.postDelayed(() -> {
                            if (!isDownInEdgeZone && event.getPointerCount() == 1) {
                                isWheelScrubbing = true;
                                if (commander != null) {
                                    commander.vibrate(25);
                                }
                                showFolderWheel(0);
                            }
                        }, 280);

                    } else if (isDownInEdgeZone) {
                        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                            isDownInEdgeZone = false;
                        }
                        return false;
                    } else if (action == MotionEvent.ACTION_MOVE) {
                        float density = getResources().getDisplayMetrics().density;
                        float deltaX = rawX - wheelDownX;

                        // If user moved significantly horizontally, cancel long-press wheel mode
                        if (!isWheelScrubbing && Math.abs(deltaX) > 28 * density) {
                            wheelScrubHandler.removeCallbacksAndMessages(null);
                        }

                        // Multi-finger touch cancels single-finger wheel scrubbing
                        if (event.getPointerCount() > 1) {
                            wheelScrubHandler.removeCallbacksAndMessages(null);
                            isWheelScrubbing = false;
                        }

                        if (isWheelScrubbing) {
                            float deltaStepY = rawY - wheelLastStepY;
                            float stepThreshold = 32 * density; // responsive step per ~32dp vertical drag
                            long now = System.currentTimeMillis();

                            if (Math.abs(deltaStepY) >= stepThreshold && (now - lastWheelStepTime > 110)) {
                                lastWheelStepTime = now;
                                wheelLastStepY = rawY;

                                boolean isFileMode = (commander != null && commander.getDisplayMode() == Commander.DISPLAY_MODE_FILE);
                                if (deltaStepY < 0) {
                                    // Dragging up -> Next item (rolls drum upward, pulling lower item up)
                                    if (commander != null) {
                                        commander.cmd(isFileMode ? Commander.NEXT_SONG : Commander.NEXT_FOLDER);
                                    }
                                } else {
                                    // Dragging down -> Previous item (rolls drum downward, pulling upper item down)
                                    if (commander != null) {
                                        commander.cmd(isFileMode ? Commander.PREVIOUS_SONG : Commander.PREVIOUS_FOLDER);
                                    }
                                }
                            }
                            return true;
                        }
                    } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                        wheelScrubHandler.removeCallbacksAndMessages(null);
                        if (isWheelScrubbing) {
                            isWheelScrubbing = false;
                            if (commander != null) {
                                commander.vibrate(15);
                            }
                            scheduleFolderWheelFadeOut();
                            return true;
                        }
                    }

                    if (gestureDetector != null) {
                        boolean handled = gestureDetector.onTouchEvent(event);
                        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
                            return true;
                        }
                        return handled;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Ambient folder wheel display controller.
     * Updates folder position text and triggers smooth roll animation in DialView.
     * Folders are drawn ambiently behind the animation circle (+5, -5 depth).
     * @param direction: -1 for previous folder roll, 1 for next folder roll, 0 for static update
     */
    public void showFolderWheel(int direction) {
        runOnUiThread(() -> {
            if (recorder != null && trackPos_TextView != null && recorder.getTotalDirectoriesCount() > 0) {
                trackPos_TextView.setText("FOLDER " + (recorder.getCurrentDirectoryIndex() + 1) + " OF " + recorder.getTotalDirectoriesCount());
            }

            if (commander != null) {
                commander.updateFolderDisplay();
                if (commander.dialView != null) {
                    if (direction != 0) {
                        commander.dialView.onFolderChanged(direction);
                    } else {
                        commander.dialView.postInvalidateOnAnimation();
                    }
                }
            }
        });
    }

    /**
     * Ambient file wheel display controller.
     * Updates file position text and triggers smooth roll animation in DialView.
     * @param direction: -1 for previous file roll, 1 for next file roll, 0 for static update
     */
    public void showFileWheel(int direction) {
        runOnUiThread(() -> {
            if (commander != null) {
                commander.updateFileDisplay(direction);
            }
        });
    }

    public void scheduleFolderWheelFadeOut() {
        // Ambient folders remain continuously visible behind animation circle without disappearing
    }

    public void hideFolderWheelImmediately() {
        // No-op: ambient folders remain continuously visible
    }

    public boolean isHorizontalEdgeTouch(MotionEvent e) {
        return GestureListener.isHorizontalEdgeTouch(this, e);
    }

    public boolean isVerticalEdgeTouch(MotionEvent e) {
        return GestureListener.isVerticalEdgeTouch(this, e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isHorizontalEdgeTouch(event)) {
            return super.onTouchEvent(event);
        }
        if (gestureDetector != null) {
            return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
        }
        return super.onTouchEvent(event);
    }

    private boolean isSpeakingFullHelp = false;

    public void stopSpeakingHelp() {
        if (isSpeakingFullHelp) {
            isSpeakingFullHelp = false;
            if (ttobj != null) {
                try {
                    ttobj.stop();
                    ttobj.setSpeechRate(2.5f);
                } catch (Exception ignored) {}
            }
        }
    }

    public void speakFullHelpDetails() {
        if (ttobj == null) return;
        isSpeakingFullHelp = true;
        try {
            ttobj.stop();
            ttobj.setPitch(1.0f);
            ttobj.setSpeechRate(1.2f);
        } catch (Exception ignored) {}

        String[] sections = new String[] {
                "EYES-FREE Recorder complete user guide. Long press anywhere to restart, or tap anywhere to stop.",
                "Touch and tap controls on the dial: " +
                        "Single tap plays the current audio file. When playing or recording, a single tap stops playback or recording immediately. " +
                        "Double tap starts recording a new audio file, or stops recording if already in progress. " +
                        "Triple tap adds a bookmark at the current playback position.",
                "Track and folder navigation: " +
                        "Swipe left to jump to the next track. " +
                        "Swipe right to jump to the previous track. " +
                        "Swipe up or down to switch folders. " +
                        "Long press and swipe up or down to activate the rotary wheel controller for continuous folder browsing with ambient preview. " +
                        "Swipe left or right and hold for continuous fast forward or rewind.",
                "Multi-finger accessibility shortcuts: " +
                        "One finger long press speaks the file name, date, duration, and recording location. " +
                        "Two finger long press announces the current time and battery level. " +
                        "Three finger long press marks or unmarks the track as a favorite. " +
                        "Two finger tap quickly adds a bookmark. " +
                        "Two finger swipe left or right jumps between bookmarks. " +
                        "Three finger swipe down opens settings. " +
                        "Three finger swipe up starts cloud backup. " +
                        "Four finger swipe right and hold deletes the current audio file.",
                "Display modes and visualizers: " +
                        "The default split user interface displays folders on the left and audio files on the right with vertical scrolling and text marquee for long titles. " +
                        "Display animation styles such as Circular Dial, Frequency Spectrum, and Starburst can be selected in settings under Animation and Visualizer.",
                "Top controls: " +
                        "Tap question mark for help. Tap gear icon for settings. Tap TTS pill to toggle voice assistance on or off."
        };

        for (int i = 0; i < sections.length; i++) {
            int queueMode = (i == 0) ? TextToSpeech.QUEUE_FLUSH : TextToSpeech.QUEUE_ADD;
            try {
                ttobj.speak(sections[i], queueMode, null, "full_help_step_" + i);
            } catch (Exception ignored) {}
        }
    }

    public void showGestureHelpDialog() {
        showGestureHelpDialog(false);
    }

    public void showGestureHelpDialog(final boolean speakImmediately) {
        float density = getResources().getDisplayMetrics().density;
        int pad = (int) (18 * density);

        final android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);

        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#061019"));
        bg.setCornerRadius(16 * density);
        bg.setStroke((int) (1.5f * density), Color.parseColor("#1B4965"));
        root.setBackground(bg);
        root.setPadding(pad, pad, pad, pad);

        TextView titleView = new TextView(this);
        titleView.setText("EYES-FREE RECORDER");
        titleView.setTextColor(Color.parseColor("#00E676"));
        titleView.setTextSize(17f);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLetterSpacing(0.08f);
        titleView.setGravity(android.view.Gravity.CENTER);
        root.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("Touch & Gesture Guide • Long-press to listen");
        subtitleView.setTextColor(Color.parseColor("#69F0AE"));
        subtitleView.setTextSize(11f);
        subtitleView.setLetterSpacing(0.04f);
        subtitleView.setGravity(android.view.Gravity.CENTER);
        subtitleView.setPadding(0, (int) (2 * density), 0, (int) (4 * density));
        root.addView(subtitleView);

        TextView audioHintBadge = new TextView(this);
        audioHintBadge.setText("🎧 Long press anywhere to hear full details read aloud");
        audioHintBadge.setTextColor(Color.parseColor("#80D8FF"));
        audioHintBadge.setTextSize(10.5f);
        audioHintBadge.setGravity(android.view.Gravity.CENTER);
        audioHintBadge.setPadding(0, 0, 0, (int) (10 * density));
        root.addView(audioHintBadge);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#00B4D8"));
        divider.setLayoutParams(new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, (int) (2 * density)));
        root.addView(divider);

        String helpHtml =
                "<font color=\"#00E676\"><b>▶ TOUCH &amp; TAP CONTROLS (ON DIAL)</b></font><br/>" +
                "• <b>1x Tap (Single Tap)</b>: Play current audio file.<br/>" +
                "  <i>* When Playing or Recording: 1x Tap <b>STOPS</b> immediately!</i><br/>" +
                "• <b>2x Tap (Double Tap)</b>: Start Recording (or Stop if recording).<br/>" +
                "• <b>3x Tap (Triple Tap)</b>: Add Bookmark at current playback time.<br/>" +
                "<br/>" +
                "<font color=\"#FFD54F\"><b>↔ TRACK &amp; FOLDER NAVIGATION (SWIPES)</b></font><br/>" +
                "• <b>Swipe Left</b>: Next track / audio file.<br/>" +
                "• <b>Swipe Right</b>: Previous track / audio file.<br/>" +
                "• <b>Swipe Up/Down</b>: Previous / Next folder (ambient wheel display).<br/>" +
                "• <b>Long Press &amp; Swipe Up/Down</b>: Rotary Wheel Controller — drag continuously through folders with ambient previous &amp; next preview.<br/>" +
                "• <b>Swipe Left/Right &amp; Hold</b>: Continuous Fast Forward / Rewind.<br/>" +
                "<br/>" +
                "<font color=\"#00E5FF\"><b>✦ MULTI-FINGER ACCESSIBILITY SHORTCUTS</b></font><br/>" +
                "• <b>1-Finger Long Press</b>: Voice readout of File Name, Date, Duration &amp; Location.<br/>" +
                "• <b>2-Finger Long Press</b>: Voice readout of Current Time &amp; Battery Level.<br/>" +
                "• <b>3-Finger Long Press</b>: Mark / Unmark track as Favorite.<br/>" +
                "• <b>2-Finger Tap</b>: Quick Bookmark (alternate shortcut).<br/>" +
                "• <b>2-Finger Swipe Left / Right</b>: Jump to Next / Previous Bookmark.<br/>" +
                "• <b>3-Finger Swipe Down</b>: Open Settings.<br/>" +
                "• <b>3-Finger Swipe Up</b>: Start / Stop Cloud Backup.<br/>" +
                "• <b>4-Finger Swipe Right &amp; Hold</b>: Delete current audio file.<br/>" +
                "<br/>" +
                "<font color=\"#FF9100\"><b>◆ DISPLAY MODES &amp; VISUALIZERS</b></font><br/>" +
                "• <b>Simplest Split UI (Default)</b>: Folders on the left, audio files on the right with vertical scrolling and text marquee for long titles.<br/>" +
                "• <b>Circular Dial &amp; Diamonds</b>: Circulating progress bar with amber diamonds and jewel markers.<br/>" +
                "• <b>Change Modes</b>: Select between Split UI, Frequency Spectrum, and Starburst in Settings under Animation &amp; Visualizer.<br/>" +
                "<br/>" +
                "<font color=\"#80D8FF\"><b>⚙ TOP CONTROLS</b></font><br/>" +
                "• <b>Help (?)</b>: Opens this guide. <i>Long-press anywhere on this page to listen to full details read aloud!</i><br/>" +
                "• <b>Settings (Gear)</b>: Configure visualizer style, audio quality, speech rate &amp; cloud backup.<br/>" +
                "• <b>TTS-On / TTS-Off</b>: Tap pill to toggle voice assistance on/off.";

        ScrollView scrollView = new ScrollView(this);
        TextView textView = new TextView(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            textView.setText(android.text.Html.fromHtml(helpHtml, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            textView.setText(android.text.Html.fromHtml(helpHtml));
        }
        textView.setTextColor(Color.parseColor("#E0F7FA"));
        textView.setTextSize(13f);
        textView.setLineSpacing(0, 1.35f);
        textView.setPadding(0, (int) (12 * density), 0, (int) (16 * density));
        scrollView.addView(textView);
        root.addView(scrollView, new android.widget.LinearLayout.LayoutParams(android.view.ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        final GestureDetector helpGestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                try {
                    root.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                Toast.makeText(MainActivity.this, "Reading full guide details aloud...", Toast.LENGTH_SHORT).show();
                speakFullHelpDetails();
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isSpeakingFullHelp) {
                    stopSpeakingHelp();
                    Toast.makeText(MainActivity.this, "Guide readout stopped", Toast.LENGTH_SHORT).show();
                    alwaysSpeak("Guide readout stopped");
                    return true;
                }
                return false;
            }
        });

        View.OnLongClickListener helpLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                try {
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {}
                Toast.makeText(MainActivity.this, "Reading full guide details aloud...", Toast.LENGTH_SHORT).show();
                speakFullHelpDetails();
                return true;
            }
        };

        View.OnClickListener helpClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSpeakingFullHelp) {
                    stopSpeakingHelp();
                    Toast.makeText(MainActivity.this, "Guide readout stopped", Toast.LENGTH_SHORT).show();
                    alwaysSpeak("Guide readout stopped");
                }
            }
        };

        root.setOnLongClickListener(helpLongClickListener);
        root.setOnClickListener(helpClickListener);
        scrollView.setOnLongClickListener(helpLongClickListener);
        scrollView.setOnClickListener(helpClickListener);
        textView.setOnLongClickListener(helpLongClickListener);
        textView.setOnClickListener(helpClickListener);
        titleView.setOnLongClickListener(helpLongClickListener);
        subtitleView.setOnLongClickListener(helpLongClickListener);
        audioHintBadge.setOnLongClickListener(helpLongClickListener);

        final android.app.Dialog dialog = new android.app.Dialog(this) {
            @Override
            public boolean dispatchTouchEvent(MotionEvent ev) {
                if (helpGestureDetector != null) {
                    helpGestureDetector.onTouchEvent(ev);
                }
                return super.dispatchTouchEvent(ev);
            }
        };
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        Button closeBtn = new Button(this);
        closeBtn.setText("GOT IT");
        closeBtn.setTextColor(Color.parseColor("#002811"));
        android.graphics.drawable.GradientDrawable btnBg = new android.graphics.drawable.GradientDrawable();
        btnBg.setColor(Color.parseColor("#00E676"));
        btnBg.setCornerRadius(10 * density);
        closeBtn.setBackground(btnBg);
        closeBtn.setTypeface(null, android.graphics.Typeface.BOLD);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopSpeakingHelp();
                dialog.dismiss();
            }
        });
        root.addView(closeBtn);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                stopSpeakingHelp();
            }
        });

        dialog.setContentView(root);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
            int h = (int) (getResources().getDisplayMetrics().heightPixels * 0.78);
            dialog.getWindow().setLayout(w, h);
        }
        dialog.show();

        if (speakImmediately) {
            speakFullHelpDetails();
        } else {
            if (ttobj == null || !ttobj.isSpeaking()) {
                alwaysSpeak("Gesture instructions opened. Long press to hear full details.");
            }
        }
    }

    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO);
            }
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (!permissionsToRequest.isEmpty()) {
            speak("Welcome to SJ Player. Please grant microphone, storage, and location permissions on screen to get started.");
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 0);
        }
    }


    public void setSettingsValues(){
        // Ensure legacy defaults for math test and poking eyes warning are turned Off by default
        if (!sharedPref.contains("pref_poking_eyes_default_off_migrated")) {
            String currentPoking = sharedPref.getString("poking_eye_warning", "0");
            if ("31".equals(currentPoking) || "10".equals(currentPoking) || !sharedPref.contains("poking_eye_warning")) {
                sharedPref.edit().putString("poking_eye_warning", "0").apply();
            }
            sharedPref.edit().putBoolean("pref_poking_eyes_default_off_migrated", true).apply();
        }
        if (!sharedPref.contains("pref_math_test_default_off_migrated")) {
            sharedPref.edit()
                    .putBoolean("isMATHTESTEnabled", false)
                    .putBoolean("pref_math_test_default_off_migrated", true)
                    .apply();
        }

        isTTSEnabled = sharedPref.getBoolean( "isTTSEnabled",true);
        updateTtsButtonState();
        isMathTestEnabled = sharedPref.getBoolean( "isMATHTESTEnabled",false);
        isHomemodeEnabled = sharedPref.getBoolean("isHomeWorkMode",false);
        homeworkFoldersFromSettings = sharedPref.getString( "homework_folders",getResources().getString(R.string.pref_default_homework_folders));
        foldersFromSettings = sharedPref.getString( "target_folders",getResources().getString(R.string.pref_default_target_folders));
        homeworkFilePrefix = sharedPref.getString( "homework_file_prefix",getResources().getString(R.string.pref_default_homework_file_prefix));
        isNavModeEnabled = sharedPref.getBoolean("isNavModeEnabled",false);

        /**
         * Avoid crashes by legacy Pref values which are imcompatible with Int.
         */
        try {
            maxVolume = Integer.parseInt(sharedPref.getString("max_volume", "90"));
        }catch(NumberFormatException ne){
            maxVolume = 90;
            ne.printStackTrace();
        }
        try {
            pokingEyesWarning = Integer.parseInt(sharedPref.getString("poking_eye_warning", "0"));
        } catch (NumberFormatException ne) {
            pokingEyesWarning = 0;
            ne.printStackTrace();
        }
        try {
            mathtestFrequency = Integer.parseInt(sharedPref.getString( "mathtest_frequency","3"));
        }catch(NumberFormatException ne){
            mathtestFrequency = 3;
            ne.printStackTrace();
        }
        try {
            mathtestCountOnStart = Integer.parseInt(sharedPref.getString( "mathtest_count_on_start","10"));
        }catch(NumberFormatException ne){
            mathtestCountOnStart = 10;
            ne.printStackTrace();
        }
        try {
            mathtestCountAfterStart = Integer.parseInt(sharedPref.getString( "mathtest_count_after_start","2"));
        }catch(NumberFormatException ne){
            mathtestCountAfterStart = 2;
            ne.printStackTrace();
        }
        try {
            mathtestMultiplyTestScope = Integer.parseInt(sharedPref.getString( "mathtest_multiplytest_scope","9"));
        }catch(NumberFormatException ne){
            mathtestMultiplyTestScope = 9;
            ne.printStackTrace();
        }
        try{
            mathtestStartingNumber = Integer.parseInt(sharedPref.getString( "mathtest_starting_number","0"));
        }catch(NumberFormatException ne){
            mathtestStartingNumber = 0;
            ne.printStackTrace();
        }
        try{
            mathtestEndingNumber = Integer.parseInt(sharedPref.getString( "mathtest_ending_number","10"));
        }catch(NumberFormatException ne){
            mathtestEndingNumber = 10;
            ne.printStackTrace();
        }
        try{
            mathTestMode = Integer.parseInt(sharedPref.getString( "mathtest_mode","0"));
        }catch(NumberFormatException ne){
            mathTestMode = 10;
            ne.printStackTrace();
        }

        try {
            int mode = DialView.VISUALIZER_MODE_MINIMAL_SPLIT;
            if (sharedPref.contains("pref_visualizer_mode_str")) {
                mode = Integer.parseInt(sharedPref.getString("pref_visualizer_mode_str", "0"));
            } else {
                mode = sharedPref.getInt("pref_visualizer_mode", DialView.VISUALIZER_MODE_MINIMAL_SPLIT);
            }
            if (mode < 0 || mode >= DialView.NUM_VISUALIZER_MODES) {
                mode = DialView.VISUALIZER_MODE_MINIMAL_SPLIT;
            }
            if (commander != null && commander.dialView != null) {
                commander.dialView.setVisualizerMode(mode);
            }
            updateVisualizerModeUI(mode);
        } catch (Exception ignored) {}

        boolean hapticPulse = sharedPref.getBoolean("pref_haptic_audio_pulse", true);
        if (commander != null && commander.dialView != null) {
            commander.dialView.setHapticPulseEnabled(hapticPulse);
        }
    }

    public void updateVisualizerModeUI(final int mode) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View centerContainer = findViewById(R.id.center_display_container);
                if (centerContainer != null) {
                    centerContainer.setVisibility(mode == DialView.VISUALIZER_MODE_MINIMAL_SPLIT ? View.GONE : View.VISIBLE);
                }
                View detailContainer = findViewById(R.id.detail_info_container);
                if (detailContainer != null) {
                    detailContainer.setVisibility(mode == DialView.VISUALIZER_MODE_MINIMAL_SPLIT ? View.GONE : View.VISIBLE);
                }
            }
        });
    }

    public boolean isTTSEnabled() { return this.isTTSEnabled; }
    public boolean isNavModeEnabled() { return this.isNavModeEnabled; }
    public void isTTSEnabled(boolean b){ setIsTTSEnabled(b); }
    public String getFoldersFromSettings() { return foldersFromSettings; }
    public String getHomeworkFoldersFromSettings() { return homeworkFoldersFromSettings; }
    public Recorder getRecorder() { return recorder; }
    public boolean isHomemodeEnabled() {return this.isHomemodeEnabled;}
    public String getHomeworkFilePrefix(){ return homeworkFilePrefix;}
    public int getPokingEyesWarning(){ return this.pokingEyesWarning;}
    public int getMathtestFrequency(){return this.mathtestFrequency;}
    public int getMathtestCountOnStart(){return this.mathtestCountOnStart;}
    public int getMathtestCountAfterStart(){return this.mathtestCountAfterStart;}
    public int getMathtestMultiplyTestScope(){return this.mathtestMultiplyTestScope;}
    public int getMathtestMode(){return this.mathTestMode;}

    /**
     * TODO in case there needs some cases only for SangJoon.
     * @return
     */
//    public boolean isSangJoon(){
//        /**
//         * Checking if user is my lovely son, Sangjoon
//         */
//        Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
//        Account[] accounts = AccountManager.get(this).getAccounts();
//        for (Account account : accounts) {
//            Log.i(TAG, "Account --> " + account.name);
//            if (emailPattern.matcher(account.name).matches()) {
//                if(account.name.equalsIgnoreCase("sanglee1014@gmail.com"))
//                    this.isSangJoon = true;
//            }
//        }
//        return isSangJoon;
//    }

    public void setIsTTSEnabled(boolean enabled){
        this.isTTSEnabled = enabled;
        if (sharedPref != null) {
            sharedPref.edit().putBoolean("isTTSEnabled", enabled).apply();
        }
        updateTtsButtonState();
    }

    public void updateTtsButtonState() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressTextView != null) {
                    if (isTTSEnabled) {
                        progressTextView.setText("TTS-On");
                        progressTextView.setTextColor(Color.parseColor("#00E5FF"));
                    } else {
                        progressTextView.setText("TTS-Off");
                        progressTextView.setTextColor(Color.parseColor("#4A6B80"));
                    }
                }
            }
        });
    }

    public boolean getIsTTSEnabled(){
        return this.isTTSEnabled;
    }

    public int getBatteryLevel(){
        return this.batteryLevel;
    }

    /**
     * Let TTS speaks
     * @param w
     */
    public void speak(String w) {
        if(isTTSEnabled) {
            if (ttobj == null) {
                pendingTtsMessage = w;
                return;
            }
            ttobj.setPitch(1.0f);
            int res = ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
            if (res == TextToSpeech.ERROR) {
                pendingTtsMessage = w;
            }
        }
        try {
            if (getWindow() != null && getWindow().getDecorView() != null) {
                getWindow().getDecorView().announceForAccessibility(w);
            }
        } catch (Exception ignored) {}
    }

    public void speak(String w, float pitch) {
        if(isTTSEnabled) {
            if (ttobj == null) {
                pendingTtsMessage = w;
                return;
            }
            ttobj.setPitch(pitch);
            int res = ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
            if (res == TextToSpeech.ERROR) {
                pendingTtsMessage = w;
            }
        }
    }

    public void speak(String w, float pitch, int queue) {
        if(isTTSEnabled) {
            if (ttobj == null) {
                pendingTtsMessage = w;
                return;
            }
            ttobj.setPitch(pitch);
            int res = ttobj.speak(w, queue, null, null);
            if (res == TextToSpeech.ERROR) {
                pendingTtsMessage = w;
            }
        }
    }

    public void waitUntilTTSFinished(boolean wait){
        if(wait && ttobj != null){
            try {
                while(ttobj.isSpeaking()){
                    Thread.sleep(50);
                }
            } catch (InterruptedException ignored) {}
        }
    }

    public TextToSpeech getTextToSpeech(){
        return ttobj;
    }

    public void alwaysSpeak(String w){
        Log.i(TAG, "alwaysSpeak: " + w);
        if (ttobj == null) {
            pendingTtsMessage = w;
            return;
        }
        ttobj.setPitch(1.0f);
        int res = ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
        if (res == TextToSpeech.ERROR) {
            pendingTtsMessage = w;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        com.jonlee.android.common.logger.Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

    }

    /**
     * Same behavior with HOME button by BACK key
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    /**
     * To keep existing codes when command method existed inside of MainActivity
     * @return
     */
    public Commander getCommander(){
        return this.commander;
    }

    /**
     *      * TODO Isn't this Battery level regulary called and Broadcasted?
     */
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver(){
        @Override
        public void onReceive(Context arg0, Intent intent) {
            // TODO Auto-generated method stub
            batteryLevel = intent.getIntExtra("level", 0);
        }

    };

//    private BroadcastReceiver mStorageInfoReceiver = new BroadcastReceiver(){
//        @Override
//        public void onReceive(Context arg0, Intent intent) {
//            //speak((getResources().getString(R.string.LOW_STORAGE)));
//            //TODO Auto backup and delete unused files
//            //
//        }
//    };


    public static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
//        if (VERSION.SDK_INT > VERSION_CODES.JELLY_BEAN) {
//            isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
//        }
        return isPlugged;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);

        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:

                if (action == KeyEvent.ACTION_DOWN) {

                        //TODO click action
                        Log.i(TAG, "Volume key up:" + volume_level
                                + ":" + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + ":"
                                + ((volume_level*100)/am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) +
                                ":maxVolume:" + maxVolume);

                        if(maxVolume < ((volume_level*100)/am.getStreamMaxVolume(AudioManager.STREAM_MUSIC))){
//                            am.setStreamVolume(
//                                    AudioManager.STREAM_MUSIC,
//                                    volume_level*(maxVolume/100),
//                                    0);
                        }else{
                            if(volume_level <= am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) {
                                volume_level = volume_level + 1;
                                am.setStreamVolume(
                                        AudioManager.STREAM_MUSIC,
                                        volume_level,
                                        0);
                            }
                        }
                   // }
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    volume_level = volume_level - 1;
                    if (event.getEventTime() - event.getDownTime() > ViewConfiguration.getLongPressTimeout()) {
                        //TODO long click action
                        volume_level = volume_level - 10;
                        am.setStreamVolume(
                                AudioManager.STREAM_MUSIC,
                                volume_level,
                                0);
                    } else {
                        if(volume_level >= 0) {
                            Log.i(TAG, "Volume key down:" + volume_level
                                    + ":" + am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + ":"
                                    + ((volume_level * 100) / am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)));
                            am.setStreamVolume(
                                    AudioManager.STREAM_MUSIC,
                                    volume_level,
                                    0);
                        }
                    }
                }
                return true;
            case KeyEvent.KEYCODE_POWER:
                if(action == KeyEvent.ACTION_DOWN){
                    //HOME SCREEN?
                }
                Log.i(TAG, "KEYCODE_POWER");

            default:
                return super.dispatchKeyEvent(event);
        }
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getResources().getString(R.string.channel_id), name, importance);
            channel.setDescription(description);
            channel.enableVibration(false);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    public NotificationManager getNotificationManager(){
        return notificationManager;
    }

//    public void startBackupActivity(){
//        driveMainActivity = new DriveMainActivity(this,recorder);
//    }

    public GoogleAccountCredential getGoogleAccountCredential() {
        if(mCredential == null){
            mCredential = GoogleAccountCredential.usingOAuth2(this, Arrays.asList(DriveScopes.DRIVE));
            startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        }
        return mCredential;
    }

//    public void setGoogleAccountCredential(GoogleAccountCredential cred){
//        this.mCredential = cred;
//    }

    /**
     * Read $SHARED_FOLDER/welcome_msg.txt
     *
     */
    public void readWelcomeMsg(){
        StringBuilder sb = new StringBuilder();
        try {
            String line = "";
            BufferedReader br = new BufferedReader(
                    new FileReader(new File( recorder.getRootFolder().getAbsolutePath() + "/" +
                            "" +Recorder.SHARED_FOLDER + "/SHARED ROOT/welcome_msg.txt")));
            Log.i(TAG, "welcome msg >>> ");
            while((line = br.readLine()) != null){
                Log.i(TAG, "welcome mgs >>> " + line);
                sb.append(line + "\n");
            }
            br.close();
        }catch(FileNotFoundException fnfe){
            fnfe.printStackTrace();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }

        speak(sb.toString());
    }

    public void readWelcomeMsgFromWeb(){
        final String generate_URL = "https://docs.google.com/document/d/18k5apl6L5GZmVhY2d7O5aUljkQB2e6EyBq1L3kE13sQ/edit";
        Log.i(TAG, "Read webpage >>>" + generate_URL);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL data = new URL(generate_URL);
                    HttpURLConnection con = (HttpURLConnection) data.openConnection();
                    con.setConnectTimeout(5000);
                    con.setReadTimeout(5000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    final StringBuilder sb = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        Log.i(TAG, "WebMsg >>>" + inputLine);
                        sb.append(inputLine);
                    }
                    in.close();
                    con.disconnect();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            speak(sb.toString());
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Failed reading web welcome message: " + e.getMessage());
                }
            }
        }).start();
    }

    public MathTest getMathTest(){
        return mathTest;
    }

    public boolean isMathTestEnabled(){
        return this.isMathTestEnabled;
    }

    @Override
    public void onResume() {
        super.onResume();
        setSettingsValues();
        applyFullScreenMode();
        if (commander != null) {
            commander.updateFolderDisplay();
        }
        try {
            registerReceiver(mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            IntentFilter noisyFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(becomingNoisyReceiver, noisyFilter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(becomingNoisyReceiver, noisyFilter);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error registering receivers: " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(mBatInfoReceiver);
        } catch (Exception ignored) {}
        try {
            unregisterReceiver(becomingNoisyReceiver);
        } catch (Exception ignored) {}
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mBatInfoReceiver);
        } catch (Exception ignored) {}
        try {
            unregisterReceiver(becomingNoisyReceiver);
        } catch (Exception ignored) {}

        if (recorder != null) {
            recorder.stopRecording();
            recorder.stopPlaying();
        }
        if (ttobj != null) {
            ttobj.stop();
            ttobj.shutdown();
            ttobj = null;
        }
        instance = null;
    }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
       Log.i(TAG, "Selected menu >>>" + item.getTitle());
       // Handle item selection
       final int itemId = item.getItemId();
       if (itemId == R.id.settings_action) {
           startActivity(new Intent(this, SettingsActivity.class));
           return true;
       }
       return super.onOptionsItemSelected(item);
   }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            applyFullScreenMode();
        }
    }

    public void applyFullScreenMode() {
        boolean isFullScreen = (sharedPref != null) ? sharedPref.getBoolean("pref_fullscreen_mode", true) : true;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = isFullScreen
                    ? WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                    : WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT;
            getWindow().setAttributes(lp);
        }

        WindowCompat.setDecorFitsSystemWindows(getWindow(), !isFullScreen);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            if (isFullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars());
            }
        }
    }
}
