package com.jonlee.android.SJplayer;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jonlee.android.common.logger.Log;

import java.io.File;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static android.R.attr.value;
import static com.jonlee.android.SJplayer.SettingsActivity.KEY_PREF_SYNC_CONN;

/**
 * Created by jongyeong on 6/17/14.
 */
public class Commander{

    public static final String TAG = "Commander";

    public final static int START_RECORD = 1;
    public final static int STOP_RECORD = 2;
    public final static int START_PLAYBACK = 3;
    public final static int STOP_PLAYBACK = 4;
    public final static int FAST_FORWARD_2X = 5;
    public final static int FAST_BACKWARD_2X = 6;
    public final static int JOG_CLOCK_WISE = 7;
    public final static int JOG_ANTI_CLOCK_WISE = 8;
    public final static int NEXT_SONG = 9;
    public final static int PREVIOUS_SONG = 10;
    public final static int NEXT_FOLDER = 11;
    public final static int PREVIOUS_FOLDER = 12;
    public final static int SPEAK_FILE_INFO = 13;
    public final static int ONETOUCH = 14;
    public final static int FAST_FORWARD_3X = 15;
    public final static int FAST_BACKWARD_3X = 16;
    public final static int NEXT_DAY = 17;
    public final static int PREVIOUS_DAY = 18;
    public final static int PAUSE = 19;
    public final static int LONG_PRESS = 20;
    public final static int NEXT_ARTIST = 21;
    public final static int PREVIOUS_ARTIST = 22;
    public final static int FF_FOLDER = 23;
    public final static int REWIND_FOLDER = 24;
    public final static int FF_FILE = 25;
    public final static int REWIND_FILE = 26;
    public final static int STOP_BROWSE_MODE = 27;
    public final static int BREAK_500MS = 28;
    public final static int BREAK_100MS = 29;
    public final static int BREAK_50MS = 37;
    public final static int UPDATE_PLAY_ICON = 30;
    public final static int TURN_ON_OFF_TTS = 31;
    public final static int DELETE_FILE = 32;
    public final static int UPDATE_PROGRESS = 33;
    public final static int READ_LYRICS = 34;
    public final static int SPEAK_DATE_TIME = 35;
    public final static int START_STOP_BACKUP = 36;
    public final static int SETTINGS = 38;
    public final static int TOGGLE_FAVORITE = 39;
    public final static int ADD_BOOKMARK = 40;
    public final static int NEXT_BOOKMARK = 41;
    public final static int PREVIOUS_BOOKMARK = 42;
    public final static int NEXT_FAVORITE = 43;
    public final static int PREVIOUS_FAVORITE = 44;




    public final static int NOTHING = 100;

    public Recorder recorder = null;

    //public TextToSpeech ttobj = null;

    //Vibrate strenth;'
    private int vibrateStrenth = 0;
    // Default one
    private final int VIBRATOR_STRENTH = 100;
    private final int VIBRATOR_STRENTH_MID = 50;
    private final int VIBRATE_STRENGTH_WEAK = 40;

    private Thread trackPosUpdater = null;
    public TextView main_TextView = null;
    public TextView previous_TextView = null;
    public TextView next_TextView = null;

    public TextView progress_TextView = null;
    public TextView trackPos_TextView = null;
    public TextView detail_primary_text = null;
    public TextView detail_secondary_text = null;
    public View detail_info_container = null;
    public ImageView imageView = null;
    public ImageView albumImageView = null;
    public ProgressBar progressBar = null;
    public DialView dialView = null;

    private MainActivity mainActivity = null;
    private TextToSpeech ttobj = null;
    private boolean isNavModeEnabled;

    // Creating notificaiton builder
    NotificationCompat.Builder  mBuilder;
    /**
     * Notification
     */
    private NotificationManager mNotificationManager;
    private int notificationID = 100;

    private int warningCnt = 0;
    // To calculate frequency of MathTest and NoForkWarning
    // Default is set to 20 for a case
    // that the frequency value is set to smaller than 20 and Cnt is not yet 20,
    private int folderMoveCnt = 20;


    public Commander(MainActivity activity){
        this.mainActivity = activity;
        isNavModeEnabled = mainActivity.isNavModeEnabled();
        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (recorder != null && recorder.isRecording()) {
                    cmd(Commander.STOP_RECORD);
                } else {
                    cmd(Commander.ONETOUCH);
                }
            }
        });
        //albumImageView = (ImageView) findViewById(R.id.album_imageView);
        imageView.setImageResource(R.drawable.ic_action_about);
        progress_TextView = (TextView) findViewById(R.id.progress_TextView);
        if(mainActivity.isTTSEnabled()){
            progress_TextView.setText("TTS-On");
        }else{
            progress_TextView.setText("TTS-Off");

        }

        //String syncConnPref = sharedPref.getString(SettingsActivity.KEY_PREF_SYNC_CONN, "");

//        previous_TextView = (TextView)findViewById (R.id.previous_TextView);
//        next_TextView = (TextView)findViewById (R.id.next_TextView);

        dialView = new DialView(mainActivity);
        dialView.setClickable(true);
        dialView.setFocusable(true);
        ((RelativeLayout) findViewById(R.id.main_layout)).addView(dialView, 0);

        // a step every 5°
        dialView.setStepAngle(5f);
        // area from 30% to 82% (larger wave presence while maintaining clean edge clearance)
        dialView.setDiscArea(.30f, 0.82f);

        main_TextView = (TextView)findViewById (R.id.main_TextView);
        trackPos_TextView = (TextView) findViewById(R.id.trackPos_TextView);
        detail_primary_text = (TextView) findViewById(R.id.detail_primary_text);
        detail_secondary_text = (TextView) findViewById(R.id.detail_secondary_text);
        detail_info_container = findViewById(R.id.detail_info_container);
//        Final String batteryWarningMsg = "";
//        int batteryLevel = ((MainActivity) mainActivity).getBatteryLevel();
//        if (batteryLevel < 30)
//            batteryWarningMsg = getResources().getString(R.string.LOW_BATTERY);
//        else
//            batteryWarningMsg = batteryLevel + "%";



        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
                    String tmp;
                    ttobj.setLanguage(Locale.US);
                    ttobj.setSpeechRate(2.5f);
                    ttobj.setPitch(1.0f);
                    SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, ''yyyy 'at' h:mm a", Locale.US);
                    tmp ="Today is " + sdf.format(Calendar.getInstance().getTime()) + "\n";
                    ttobj.speak(tmp, TextToSpeech.QUEUE_ADD, null);

                    // this.ttobj is used only for StopPoking and MathTest
                    // 3.0f, 1.2f are global setting, no need to reset.
                    ttobj.setSpeechRate(3.0f);
                    ttobj.setPitch(1.2f);
                    if(mainActivity.isMathTestEnabled()) {

                        // NO MORE VALID e.g, mainActivity.getMathtestMultiplyTestScope() = 9, 9*9*1 = total 81 quiz
                        // mainActivity.getMathtestMultiplyTestScope() returns the TOTAL NUMBER of Quiz
                        ttobj.speak("\n" +
                                mainActivity.getMathTest().getRamdomTest(mainActivity.getMathtestMultiplyTestScope()),
                                TextToSpeech.QUEUE_ADD,
                                null);
                    }
                    //ttobj.setPitch(1.0f);
                    mainActivity.speak(getResources().getString(R.string.HELP_INSTRUCTION));
                }
            }
        }
        );
        //Log.i(TAG, "99dan--->" + mainActivity.getMathTest().getAll99DanString());

        // Create Recorder
        recorder = new Recorder(mainActivity);
        updateFolderDisplay();
    }


    //TODO consolidate to MainActivity or here.
    private void alwaysSpeak(String w){
        (mainActivity).alwaysSpeak(w);
    }

    private void speak(String w) {
        (mainActivity).speak(w);
    }

    private void speak(String w, float pitch) {
        (mainActivity).speak(w, pitch);
    }

    public Recorder getRecorder(){
        return this.recorder;
    }

//    public boolean isSpeaking(){
//        return ttobj.isSpeaking();
//    }


    /**
     * Override all method
     * @param id
     * @return
     */
    private View findViewById(int id){
        return (View)mainActivity.findViewById(id);
    }

    private Context getApplicationContext(){
        return mainActivity.getApplicationContext();
    }

    private Resources getResources(){
        return mainActivity.getResources();
    }



    public void vibrate(int s){
        try{
            Vibrator v = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);
            if (v != null && v.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(s, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(s);
                }
            }
        }catch(Exception e) {
            // Do nothing
        }
    }



    public static final int DISPLAY_MODE_FOLDER = 0;
    public static final int DISPLAY_MODE_FILE = 1;
    private int currentDisplayMode = DISPLAY_MODE_FOLDER;

    public int getDisplayMode() {
        return currentDisplayMode;
    }

    public void setDisplayMode(int mode) {
        this.currentDisplayMode = mode;
        if (dialView != null) {
            dialView.setWheelMode(mode == DISPLAY_MODE_FILE ? DialView.WHEEL_MODE_FILE : DialView.WHEEL_MODE_FOLDER);
        }
    }

    /**
     * Updates center active folder display and the detail information panel located below the circle.
     */
    public void updateFolderDisplay() {
        setDisplayMode(DISPLAY_MODE_FOLDER);
        if (mainActivity == null) return;
        mainActivity.runOnUiThread(() -> {
            if (recorder != null) {
                String folderName = recorder.getCurrentDirectoryName();
                if (folderName == null || folderName.isEmpty()) {
                    folderName = "STANDBY";
                }
                if (main_TextView != null) {
                    main_TextView.setText(folderName);
                }
                if (detail_primary_text != null) {
                    detail_primary_text.setText(recorder.getCurrentDirectoryDetailsPrimary());
                }
                if (detail_secondary_text != null) {
                    detail_secondary_text.setText(recorder.getCurrentDirectoryDetailsSecondary());
                }
                if (trackPos_TextView != null && recorder.getTotalDirectoriesCount() > 0) {
                    trackPos_TextView.setText("FOLDER " + (recorder.getCurrentDirectoryIndex() + 1) + " OF " + recorder.getTotalDirectoriesCount());
                }
                if (dialView != null) {
                    dialView.setWheelMode(DialView.WHEEL_MODE_FOLDER);
                    dialView.postInvalidateOnAnimation();
                }
            }
        });
    }

    /**
     * Updates center active file display and ambient file wheel when in file play mode.
     * @param rollDirection: 1 for next file roll, -1 for previous file roll, 0 for static update
     */
    public void updateFileDisplay(int rollDirection) {
        setDisplayMode(DISPLAY_MODE_FILE);
        if (mainActivity == null) return;
        mainActivity.runOnUiThread(() -> {
            if (recorder != null) {
                String fileName = recorder.getCurrentFileName();
                if (fileName == null || fileName.isEmpty()) {
                    fileName = "NO AUDIO FILE";
                }
                if (main_TextView != null) {
                    main_TextView.setText(fileName);
                    if (rollDirection != 0) {
                        float startX = (rollDirection > 0) ? 180f : -180f;
                        main_TextView.setTranslationX(startX);
                        main_TextView.setAlpha(0.2f);
                        main_TextView.animate()
                                .translationX(0f)
                                .alpha(1.0f)
                                .setDuration(240)
                                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                                .start();
                    } else {
                        main_TextView.setTranslationX(0f);
                        main_TextView.setAlpha(1.0f);
                    }
                }
                if (detail_primary_text != null) {
                    detail_primary_text.setText(recorder.getCurrentFileDetailsPrimary());
                }
                if (detail_secondary_text != null) {
                    detail_secondary_text.setText(recorder.getCurrentFileDetailsSecondary());
                }
                if (trackPos_TextView != null) {
                    if (recorder.isRecording()) {
                        trackPos_TextView.setText("● RECORDING NEW FILE");
                    } else if (recorder.getAudibleFilesCount() > 0) {
                        trackPos_TextView.setText("FILE " + (recorder.getCurrentFileIndex() + 1) + " OF " + recorder.getAudibleFilesCount());
                    }
                }
                if (dialView != null) {
                    dialView.setWheelMode(DialView.WHEEL_MODE_FILE);
                    if (rollDirection != 0) {
                        dialView.onFileChanged(rollDirection);
                    } else {
                        dialView.postInvalidateOnAnimation();
                    }
                }
            }
        });
    }

    public void displayText(String w){
        if (w == null) return;
        if (recorder != null && w.startsWith("Folder: \"")) {
            updateFolderDisplay();
            return;
        }
        if (w.contains("\n")) {
            String[] lines = w.split("\n", 2);
            if (detail_primary_text != null) {
                detail_primary_text.setText(lines[0].trim());
            }
            if (detail_secondary_text != null && lines.length > 1) {
                detail_secondary_text.setText(lines[1].replace("\n", "   •   ").trim());
            }
        } else {
            if (detail_primary_text != null) {
                detail_primary_text.setText(w);
            } else if (main_TextView != null) {
                main_TextView.setText(w);
            }
        }
        if (dialView != null) {
            dialView.checkAndStartAnimation();
        }
    }

    public void displayText(String w, boolean append){
        if(append){
            main_TextView.append(w);
        }else {
            main_TextView.setText(w);
        }
        if (dialView != null) {
            dialView.checkAndStartAnimation();
        }
    }

    /**
     * Jogg Dial SEEK
     */
    public void seek(int i){
        recorder.seek(i);
    }

    private void progressBarVisible(boolean visible){
        if (dialView != null) {
            dialView.checkAndStartAnimation();
        }
    }

    /**
     * Command
     * var c = Commander.{CONSTANT}
     * TODO Move this method to Command class which makes much more sense.
     */
    public boolean cmd(int c){

        recorder.displayStatus("");

        if(!recorder.isReadyToStart() && c != Commander.START_STOP_BACKUP && c != Commander.SETTINGS){
            vibrate(VIBRATOR_STRENTH);
            //TODO let SangJoon know actual number of files he has
            if(warningCnt < 2)
                speak("Today is " + getDateTime() + ", "
                        + getResources().getString(R.string.TOO_MANY_FILES));
            else if(warningCnt >= 2 && warningCnt <= 3)
                speak(getResources().getString(R.string.GIVE_ME_A_MIN));
            else if(warningCnt >= 4 && warningCnt <= 10 )
                speak(getResources().getString(R.string.NOT_YET_READY))  ;
            else if(warningCnt > 10)
                speak(getResources().getString(R.string.LOVE_YOU_SANGJOON));

            warningCnt = warningCnt + 1;
            return false;
        } else if(recorder.isReadyToStart() && warningCnt != -1) {
            recorder.displayStatus("");
            // Set the cnt back to -1, it's for not to run this whenever cmd() called.
            warningCnt = -1;
        }

        String cmdStr = "Invalid command";
        //Stop speaking when new action is coming
        alwaysSpeak("");

        // If it's on recording, handle bookmarks, stop record, or announce "On air"
        if(recorder.isRecording()) {
            Log.i(TAG, "Command in isRecording ===>" + c);
            if (c == Commander.ADD_BOOKMARK) {
                int markIdx = recorder.addRecordingBookmark();
                vibrate(80);
                alwaysSpeak("Bookmark " + markIdx + " added");
                displayText("Bookmark " + markIdx + " added");
                return true;
            } else if (c == Commander.STOP_RECORD || c == Commander.ONETOUCH || c == Commander.STOP_PLAYBACK) {
                recorder.stopRecording();
                cmdStr = getResources().getString(R.string.STOP_RECORD);
                speak(getResources().getString(R.string.STOP_RECORD));
                imageView.setImageResource(R.drawable.ic_action_stop);
                this.displayNotification(cmdStr, R.drawable.ic_action_stop);
                updateFolderDisplay();
                updateFileDisplay(0);
            } else {
                cmdStr = getResources().getString(R.string.ON_AIR);
                imageView.setImageResource(R.drawable.ic_action_record);
                this.displayNotification(cmdStr, R.drawable.ic_action_record);
            }
            displayText(cmdStr);
            vibrate(this.VIBRATOR_STRENTH);
        } else {
            Log.i(TAG, "Command in else-isRecording ===>" + c);

            switch (c) {
                case Commander.START_RECORD:
                    progressBarVisible(false);
                    vibrate(VIBRATOR_STRENTH);

                    if(mainActivity.isHomemodeEnabled()) {
                        speak(getResources().getString(R.string.HOMEWORK_MODE_ENABLED));
                        displayText(getResources().getString(R.string.HOMEWORK_MODE_ENABLED));
                        break;
                    }

                    //TODO any possibility of exception?
                    recorder.stopPlaying();

                    cmdStr = getResources().getString(R.string.START_RECORD);
                    //Blocking SingleTab during it's waitinf for TTS.isSpeaking()
                    recorder.isRecording(true);
                    displayText(cmdStr);
                    //Waiting for until tts ends up.
                    speak(getResources().getString(R.string.START_RECORD));

//                    new Thread(new Runnable() {
//                        public void run() {
                            // TODO Need to use Handler for all commander work not to impact MainThread.
                            // Does Recorder need to be Thread(Runnable) class?
                            try {
                                Thread.sleep(500);
                            }catch (Exception e){
                                // In case of Exception, stopping to talk.
                                Log.i(TAG, e.toString());
                            }
                            // In case upper code fails to catch up, then it stops speaking
                            alwaysSpeak("");
                            vibrate(VIBRATOR_STRENTH);
                            recorder.startRecording();
//                        }
//                    }).start();
                    imageView.setImageResource(R.drawable.ic_action_mic);
                    displayNotification(cmdStr, R.drawable.ic_action_mic);
                    updateFolderDisplay();
                    updateFileDisplay(0);

                    break;
                case Commander.ONETOUCH:
                    if (recorder.isRecording()) {
                        cmd(Commander.STOP_RECORD);
                        break;
                    }

                    vibrate(this.VIBRATOR_STRENTH);

                    Log.i(TAG, "isPaused:"+recorder.isPaused()+":isPlaying:"+recorder.isPlaying());
                    if (recorder.isPlaying()) {
                        recorder.pause();
                        cmdStr = getResources().getString(R.string.PAUSE);
                        imageView.setImageResource(R.drawable.ic_action_stop);
                        speak(cmdStr);
                        this.displayNotification(cmdStr, R.drawable.ic_action_stop);
                    } else if (recorder.isPaused()) {
                        recorder.resume();
                        this.updateFileDisplay(0);
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        imageView.setImageResource(R.drawable.ic_action_play);
                    } else if(!recorder.isPlaying() && !recorder.isPaused()) {
                        recorder.startPlaying();
                        this.updateFileDisplay(0);
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        imageView.setImageResource(R.drawable.ic_action_play);
                    }
                    break;
                case Commander.STOP_PLAYBACK:
                    vibrate(this.VIBRATOR_STRENTH);
                    if (recorder.isPlaying() || recorder.isPaused()) {
                        recorder.stopPlaying();
                        cmdStr = getResources().getString(R.string.STOP_PLAYBACK);
                        this.updateFileDisplay(0);
                        imageView.setImageResource(R.drawable.ic_action_stop);
                        speak(cmdStr);
                        this.displayNotification(cmdStr, R.drawable.ic_action_stop);
                    }
                    break;
                case Commander.NEXT_SONG:
                    vibrate(this.VIBRATOR_STRENTH);

                    cmdStr = getResources().getString(R.string.NEXT_SONG);
                    recorder.stopPlaying();
                    if(recorder.nextSong()) {
                        recorder.startPlaying();
                        this.updateFileDisplay(1);
                        imageView.setImageResource(R.drawable.ic_action_play);

                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);

                    } else{
                        this.displayText("No file exists in this folder");
                        imageView.setImageResource(R.drawable.ic_action_about);
                        this.displayNotification(cmdStr, R.drawable.ic_action_about);
                    }
                    break;
                case Commander.PREVIOUS_SONG:
                    vibrate(this.VIBRATOR_STRENTH);

                    cmdStr = getResources().getString(R.string.PREVIOUS_SONG);
                    recorder.stopPlaying();
                    if(recorder.previousSong()) {
                        recorder.startPlaying();
                        this.updateFileDisplay(-1);
                        imageView.setImageResource(R.drawable.ic_action_play);
                        //albumImageView.setImageResource(new MediaStore.Images());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);

                    } else{
                        imageView.setImageResource(R.drawable.ic_action_about);
                        this.displayNotification(cmdStr, R.drawable.ic_action_about);
                    }
                    break;
                case Commander.NEXT_FOLDER:
                    progressBarVisible(false);

                    vibrate(this.VIBRATOR_STRENTH);

                    recorder.stopPlaying();
                    recorder.nextFolder();

                    folderMoveCnt++;
                    this.speakStopPoking("Folder " + recorder.getCurrentDirectoryName());
                    this.speakMathTest("Folder " + recorder.getCurrentDirectoryName());
                    speak("Folder " + getSpokenDirectoryName(recorder.getCurrentDirectoryName()));
                    this.updateFolderDisplay();
                    if (mainActivity != null) {
                        mainActivity.showFolderWheel(1);
                    } else if (dialView != null) {
                        dialView.onFolderChanged(1);
                    }
                    imageView.setImageResource(R.drawable.ic_action_collection);

                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);
                    //Speak random 'No poking eyes'

                    break;
                case Commander.PREVIOUS_FOLDER:
                    progressBarVisible(false);

                    vibrate(this.VIBRATOR_STRENTH);

                    recorder.stopPlaying();
                    recorder.previousFolder();

                    folderMoveCnt++;
                    this.speakStopPoking("Folder " + recorder.getCurrentDirectoryName());
                    this.speakMathTest("Folder " + recorder.getCurrentDirectoryName());
                    speak("Folder " + getSpokenDirectoryName(recorder.getCurrentDirectoryName()));
                    this.updateFolderDisplay();
                    if (mainActivity != null) {
                        mainActivity.showFolderWheel(-1);
                    } else if (dialView != null) {
                        dialView.onFolderChanged(-1);
                    }
                    imageView.setImageResource(R.drawable.ic_action_collection);

                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);

                    //Speak random 'No poking eyes'
                    //this.speakStopPoking();
                    break;
                case Commander.FF_FOLDER:
                    progressBarVisible(false);

                    vibrate(VIBRATOR_STRENTH_MID);
                    recorder.stopPlaying();
                    recorder.nextFolder();

                    folderMoveCnt++;
                    this.speakStopPoking("Folder " + recorder.getCurrentDirectoryName());
                    this.speakMathTest("Folder " + recorder.getCurrentDirectoryName());
//                    cmdStr = getResources().getString(R.string.NEXT_FOLDER);
                    speak("Folder " + getSpokenDirectoryName(recorder.getCurrentDirectoryName()));
                    this.updateFolderDisplay();
                    if (mainActivity != null) {
                        mainActivity.showFolderWheel(1);
                    } else if (dialView != null) {
                        dialView.onFolderChanged(1);
                    }
                    imageView.setImageResource(R.drawable.ic_action_collection);

                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);
                    break;
                case Commander.REWIND_FOLDER:
                    folderMoveCnt++;
                    progressBarVisible(false);

                    vibrate(VIBRATOR_STRENTH_MID);

                    recorder.stopPlaying();
                    recorder.previousFolder();
//                    cmdStr = getResources().getString(R.string.PREVIOUS_FOLDER);
                    folderMoveCnt++;
                    this.speakStopPoking("Folder " + recorder.getCurrentDirectoryName());
                    this.speakMathTest("Folder " + recorder.getCurrentDirectoryName());

                    speak("Folder " + getSpokenDirectoryName(recorder.getCurrentDirectoryName()));
                    this.updateFolderDisplay();
                    if (mainActivity != null) {
                        mainActivity.showFolderWheel(-1);
                    } else if (dialView != null) {
                        dialView.onFolderChanged(-1);
                    }
                    imageView.setImageResource(R.drawable.ic_action_collection);
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);
                    break;
                case Commander.FAST_FORWARD_2X:
                    vibrate(VIBRATE_STRENGTH_WEAK);

                    if(!recorder.isPlaying())
                        recorder.startPlaying();
                    else if(recorder.isPaused())
                        recorder.resume();
                    seek(1500);
                    //this.progress_TextView.setText(recorder.getPosition());
                    imageView.setImageResource(R.drawable.ic_action_fast_forward);
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_fast_forward);
                    //this.trackPos_TextView.setText(recorder.getPosition());
                    break;
                case Commander.FAST_BACKWARD_2X:
                    vibrate(VIBRATE_STRENGTH_WEAK);

                    if(!recorder.isPlaying())
                        recorder.startPlaying();
                    else if(recorder.isPaused())
                        recorder.resume();
                    imageView.setImageResource(R.drawable.ic_action_rewind);
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_rewind);
                    seek(-1500);
                    //this.trackPos_TextView.setText(recorder.getPosition());
                    break;
                case Commander.FAST_FORWARD_3X:
                    vibrate(VIBRATE_STRENGTH_WEAK);

                    if(!recorder.isPlaying())
                        recorder.startPlaying();
                    else if(recorder.isPaused())
                        recorder.resume();
                    imageView.setImageResource(R.drawable.ic_action_fast_forward);
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_fast_forward);
                    seek(3000);
                    //this.trackPos_TextView.setText(recorder.getPosition());

                    break;
                case Commander.FAST_BACKWARD_3X:
                    vibrate(VIBRATE_STRENGTH_WEAK);

                    if(!recorder.isPlaying())
                        recorder.startPlaying();
                    else if(recorder.isPaused())
                        recorder.resume();
                    imageView.setImageResource(R.drawable.ic_action_rewind);
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_rewind);
                    seek(-3000);
                    //this.trackPos_TextView.setText(recorder.getPosition());

                    break;
                case Commander.SPEAK_FILE_INFO:
                    vibrate(this.VIBRATOR_STRENTH);
                    imageView.setImageResource(R.drawable.ic_action_about);
                    String fileInfo = recorder.getCurrentFileDisplayInformation();
                    this.displayText(fileInfo);
                    this.displayNotification(fileInfo, R.drawable.ic_action_about);
                    this.alwaysSpeak(fileInfo);
                    break;
                case Commander.SPEAK_DATE_TIME:
                    vibrate(this.VIBRATOR_STRENTH);
                    imageView.setImageResource(R.drawable.ic_action_about);
                    this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_about);
                    String storageInfo = getAvailableStorageString();
                    String temp = "It is " + getDateTime() + ". Battery level is "
                            + ((MainActivity) mainActivity).getBatteryLevel() + " percent."
                            + (storageInfo.isEmpty() ? "" : " " + storageInfo + ".");
                    this.alwaysSpeak(temp);
                    break;
                case Commander.BREAK_500MS:
                    try {
                        Thread.sleep(500);
                    }catch (Exception e){
                        // In case of Exception, stopping to talk.
                        Log.i(TAG, e.toString());
                    }
                    break;
                case Commander.BREAK_100MS:
                    try {
                        Thread.sleep(100);
                    }catch (Exception e){
                        // In case of Exception, stopping to talk.
                        Log.i(TAG, e.toString());
                    }
                    break;
                case Commander.BREAK_50MS:
                    try {
                        Thread.sleep(50);
                    }catch (Exception e){
                        // In case of Exception, stopping to talk.
                        Log.i(TAG, e.toString());
                    }
                    break;
                case Commander.UPDATE_PLAY_ICON:
                    // This will be called after FF/REWIND to update icons to Play mode.
                    //this.displayText(recorder.getCurrentFileDisplayInformation());
                    imageView.setImageResource(R.drawable.ic_action_play);
                    this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                    break;
                //TODO use TTSEnabled state here or MainActivity?
                //DEPRECATED, moved to settings
                case Commander.TURN_ON_OFF_TTS:
                    if(((MainActivity)mainActivity).getIsTTSEnabled()) {
                        ((MainActivity)mainActivity).setIsTTSEnabled(false);
                        mainActivity.isTTSEnabled(false);
                        progress_TextView.setText("TTS-Off");
                        progress_TextView.setTextColor(Color.parseColor("#4A6B80"));
                        alwaysSpeak("Stopped speaking");
                    }
                    else {
                        mainActivity.isTTSEnabled(true);
                        ((MainActivity)mainActivity).setIsTTSEnabled(true);
                        progress_TextView.setText("TTS-On");
                        progress_TextView.setTextColor(Color.parseColor("#00E5FF"));
                        alwaysSpeak("Speaking again");
                    }
                    break;
                case Commander.DELETE_FILE:
                    progressBarVisible(false);
                    alwaysSpeak("Deleted " + recorder.getCurrentFileName());
                    displayText("Deleted " + recorder.getCurrentFileName());
                    recorder.moveCurrentFileToTrash();
                    break;
                case Commander.UPDATE_PROGRESS:
                    this.progress_TextView.setText(recorder.getPosition() + " " + progress_TextView.getText());
                    break;
                case Commander.READ_LYRICS:
                    this.displayText("---Lyrics---\n" +
                            FileInfoView.getLyric(recorder.getLyricsFolder(), recorder.getCurrentFileName()) );
                    break;
                //TODO Need to think about whether Activity should be called or...
                case Commander.START_STOP_BACKUP:
                    progressBarVisible(false);

                    recorder.displayStatus("Preparing Backup agent...");
                    recorder.stopPlaying();

                    //Read any new Folders after logging folders
//                    recorder.readAudibleFilesInCurrentFolder();
                    Intent myIntent = new Intent(mainActivity, DriveMainActivity.class);
                    //myIntent.putExtra("Recorder", recorder);
                    mainActivity.startActivity(myIntent);

//                    new DriveMainActivity(mainActivity, recorder);

//                    UploadToGoogleDrive backup = new UploadToGoogleDrive(mainActivity);
//                    Log.i(TAG, "START_STOP_BACKUP.commander");
//                    backup.backupBatch();
                    break;
                case Commander.SETTINGS:
                    progressBarVisible(false);

                    Intent settingsIntent = new Intent(mainActivity, SettingsActivity.class);
                    mainActivity.startActivity(settingsIntent);

                    break;
                case Commander.TOGGLE_FAVORITE:
                    boolean isFav = recorder.toggleCurrentFileFavorite();
                    if (isFav) {
                        vibrate(150);
                        String favMsg = "Starred as favorite";
                        displayText(favMsg + "\n" + recorder.getCurrentFileName());
                        alwaysSpeak(favMsg);
                    } else {
                        vibrate(60);
                        String unFavMsg = "Removed from favorites";
                        displayText(unFavMsg + "\n" + recorder.getCurrentFileName());
                        alwaysSpeak(unFavMsg);
                    }
                    break;
                case Commander.ADD_BOOKMARK:
                    vibrate(80);
                    if (recorder.isRecording()) {
                        int markIdx = recorder.addRecordingBookmark();
                        alwaysSpeak("Bookmark " + markIdx + " added");
                        displayText("Bookmark " + markIdx + " added");
                    } else {
                        File currentFile = recorder.getCurrentFile();
                        if (currentFile != null) {
                            int currentPosMs = recorder.getCurrentPositionMillis();
                            int markIdx = AudioMetadataHelper.addBookmark(mainActivity, currentFile, currentPosMs);
                            String timeStr = AudioMetadataHelper.formatDuration(currentPosMs);
                            alwaysSpeak("Bookmark " + markIdx + " at " + timeStr);
                            displayText("Bookmark " + markIdx + " at " + timeStr);
                        }
                    }
                    break;
                case Commander.NEXT_BOOKMARK:
                    vibrate(80);
                    Integer nextBm = recorder.nextBookmark();
                    if (nextBm != null) {
                        List<Integer> bms = AudioMetadataHelper.getBookmarks(mainActivity, recorder.getCurrentFile());
                        int bmIdx = AudioMetadataHelper.getBookmarkIndex(bms, nextBm);
                        String timeStr = AudioMetadataHelper.formatDuration(nextBm);
                        alwaysSpeak("Bookmark " + bmIdx + ", " + timeStr);
                        displayText("Bookmark " + bmIdx + " (" + timeStr + ")");
                    } else {
                        alwaysSpeak("No bookmarks in this recording");
                        displayText("No bookmarks");
                    }
                    break;
                case Commander.PREVIOUS_BOOKMARK:
                    vibrate(80);
                    Integer prevBm = recorder.previousBookmark();
                    if (prevBm != null) {
                        List<Integer> bms = AudioMetadataHelper.getBookmarks(mainActivity, recorder.getCurrentFile());
                        int bmIdx = AudioMetadataHelper.getBookmarkIndex(bms, prevBm);
                        String timeStr = AudioMetadataHelper.formatDuration(prevBm);
                        alwaysSpeak("Bookmark " + bmIdx + ", " + timeStr);
                        displayText("Bookmark " + bmIdx + " (" + timeStr + ")");
                    } else {
                        alwaysSpeak("No bookmarks in this recording");
                        displayText("No bookmarks");
                    }
                    break;
                case Commander.NEXT_FAVORITE:
                    vibrate(80);
                    recorder.stopPlaying();
                    if (recorder.nextFavorite()) {
                        recorder.startPlaying();
                        String favInfo = recorder.getCurrentFileDisplayInformation();
                        displayText(favInfo);
                        alwaysSpeak("Favorite: " + favInfo);
                        imageView.setImageResource(R.drawable.ic_action_play);
                    } else {
                        alwaysSpeak("No favorite recordings in this folder");
                        displayText("No favorite recordings in this folder");
                    }
                    break;
                case Commander.PREVIOUS_FAVORITE:
                    vibrate(80);
                    recorder.stopPlaying();
                    if (recorder.previousFavorite()) {
                        recorder.startPlaying();
                        String favInfo = recorder.getCurrentFileDisplayInformation();
                        displayText(favInfo);
                        alwaysSpeak("Favorite: " + favInfo);
                        imageView.setImageResource(R.drawable.ic_action_play);
                    } else {
                        alwaysSpeak("No favorite recordings in this folder");
                        displayText("No favorite recordings in this folder");
                    }
                    break;
            }
        }
        if (dialView != null) {
            dialView.invalidate();
        }

        return true;
    }

    public String getDateTime() {
        Calendar c = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, ''yyyy 'at' h:mm a", Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a, EEE, MMM d", Locale.US);

        String currentDateandTime = sdf.format(Calendar.getInstance().getTime());
        return currentDateandTime;
    }

    public String getAvailableStorageString() {
        try {
            File path = recorder != null && recorder.getRootFolder() != null ? recorder.getRootFolder() : Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long availableBytes = stat.getAvailableBytes();
            double availableGb = availableBytes / (1024.0 * 1024.0 * 1024.0);
            return String.format(Locale.US, "%.1f gigabytes free storage", availableGb);
        } catch (Exception e) {
            return "";
        }
    }
//
//
//    public int getBatteryLevel() {
//        int level = this.mainActivity.getIntent().getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
//        com.jonlee.android.common.logger.Log.i(TAG, "inside of getBatteryLevel() --> " + level);
//        return level;
//    }


    /**
     * Adding notification
     */

    public void displayNotification(String msg, int imageR) {
//        Log.i("Start", "notification");

        mBuilder = new NotificationCompat.Builder(mainActivity, getResources().getString(R.string.channel_id))
                .setSmallIcon(imageR)
                .setContentTitle("SJ Player for Blind")
                .setContentText(recorder.getCurrentFileName())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(msg))
                ;

        //mBuilder.setTicker("New Activity");
        //mBuilder.setSmallIcon(R.drawable.ic_action_play);
        //mBuilder.setSmallIcon(imageR);

//        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
//                .setComponent(mainActivity.getPackageManager().getLaunchIntentForPackage(mainActivity.getPackageName()).getComponent());

//        PendingIntent intent = PendingIntent.getActivity(mainActivity, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
//        mBuilder.setContentIntent(intent);

        mNotificationManager = mainActivity.getNotificationManager();
      /* notificationID allows you to update the notification later on. */
        // TODO: figure out how to turn off VIBRATION
        //mNotificationManager.notify(notificationID, mBuilder.build());
    }

    /**
     * Random speaking "Stop poking your eyes"
     * speak when folderMoveCnt % warning_values == 0
     * 2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59,
     *
     *     <string-array name="pref_stop_poking_warning_titles">
     *         <item>Very frequent</item>
     *         <item>Frequent</item>
     *      *  <item>Normal</item>
     *         <item>Once a while</item>
     *         <item></item>
     *
     *     </string-array>
     *     <string-array name="pref_stop_poking_warning_values">
     *         <item>5</item>
     *         <item>11</item>
     *         <item>23</item>
     *         <item>31</item>
     *     </string-array>
     */
    private boolean speakStopPoking(String msg){
        int div = mainActivity.getPokingEyesWarning();
        // If 0 or negative, poking eyes warning is disabled (Off)
        if (div <= 0) {
            return false;
        }

        boolean wasSpoke = false;
        Log.i(TAG, "Poking: Folder Move Cnt:div ---> " + folderMoveCnt + ":" + div);

        if(folderMoveCnt%div == 0) {
            wasSpoke = true;
            //ttobj.setPitch(1.2f);
            ttobj.speak("Sangjoon, Stop poking eyes!", TextToSpeech.QUEUE_ADD, null);
        }
        return wasSpoke;
    }

    /**
     * 2, 3, *5, 7, *11, 13, 17, 19, *23, 29, *31, 37, 41, 43, 47, 53, 59,
     *     <string-array name="pref_mathtest_frequency_titles">
     *         <item>Very frequent</item>
     *         <item>Frequent</item>
     *         <item>Normal</item>
     *         <item>Once a while</item>
     *     </string-array>
     *
     *     <string-array name="pref_mathtest_frequency_values">
     *         <item>3</item>
     *         <item>7</item>
     *         <item>13</item>
     *         <item>19</item>
     *     </string-array>
     * @param msg Folder name inherited prior msg to deliver
     */
    private boolean speakMathTest(String msg){
        boolean wasSpoken = false;
        // Only when MathTest option is opted in.
        if(!mainActivity.isMathTestEnabled()) { return wasSpoken;}

        //Reuse EyePokingRate for MathTest
        int div = mainActivity.getMathtestFrequency();
        if(div < 3) {
            div = 7;
        }
        String randomTest;

        //More frequent than PokingEye warning.

        Log.i(TAG, "Math: Folder Move Cnt:div ---> " + folderMoveCnt + ":" + div);
        if(folderMoveCnt%div == 0 ) {
            wasSpoken = true;
            randomTest = mainActivity.getMathTest().getRamdomTest(mainActivity.getMathtestCountAfterStart());
            //ttobj.speak(randomTest + "\n", 1.2f, TextToSpeech.QUEUE_ADD);
            //ttobj.setPitch(1.2f);
            ttobj.speak(randomTest, TextToSpeech.QUEUE_ADD, null);
            Log.i(TAG,"ramdomMathTest.LuckNumber--->"+randomTest);
        }

        return wasSpoken;
    }

    public static String getSpokenDirectoryName(String folderName) {
        if (folderName == null || folderName.isEmpty()) return "";
        // If folder starts with YYYY-MM-DD
        if (folderName.length() >= 10 && folderName.charAt(4) == '-' && folderName.charAt(7) == '-') {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date date = sdf.parse(folderName.substring(0, 10));
                if (date != null) {
                    SimpleDateFormat spokenFmt = new SimpleDateFormat("MMMM d, yyyy", Locale.US);
                    String rest = folderName.length() > 10 ? folderName.substring(10) : "";
                    return spokenFmt.format(date) + rest;
                }
            } catch (Exception ignored) {}
        } else if (folderName.length() == 4 && Character.isDigit(folderName.charAt(0))
                && Character.isDigit(folderName.charAt(1)) && Character.isDigit(folderName.charAt(2))
                && Character.isDigit(folderName.charAt(3))) {
            return "Year " + folderName;
        }
        return folderName;
    }

}
