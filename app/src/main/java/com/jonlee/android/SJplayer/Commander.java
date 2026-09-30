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

import com.google.android.gms.drive.Drive;
import com.jonlee.android.common.logger.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
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
        // area from 30% to 100%
        dialView.setDiscArea(.30f, 0.9f);

        main_TextView = (TextView)findViewById (R.id.main_TextView);
        trackPos_TextView = (TextView) findViewById(R.id.trackPos_TextView);
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
                    ttobj.speak(tmp, TextToSpeech.QUEUE_ADD, null, null);

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
            // Vibrate for 500 milliseconds
            v.vibrate(s);
        }catch(Exception e) {
            // Do nothing
        }
    }



    /**
     * TODO Needs to be graphical effect to make it prettier.
     * @param w
     */
    public void displayText(String w){
        main_TextView.setText(w);
        main_TextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

        //previous_TextView.setText(recorder.getPreviousDirectories(3));
        //next_TextView.setText(recorder.getNextDirectories(3));
        //main_TextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    public void displayText(String w, boolean append){
        if(append){
            main_TextView.append(w);
        }else {
            main_TextView.setText(w);
        }
        //previous_TextView.setText(recorder.getPreviousDirectories(3));
        //next_TextView.setText(recorder.getNextDirectories(3));
        //main_TextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    /**
     * Jogg Dial SEEK
     */
    public void seek(int i){
        recorder.seek(i);
    }

    private void progressBarVisible(boolean visible){
        if(visible){
            ((ProgressBar) mainActivity.findViewById(R.id.musicProgressBar)).setVisibility(ProgressBar.VISIBLE);

        }else {
            ((ProgressBar) mainActivity.findViewById(R.id.musicProgressBar)).setVisibility(ProgressBar.INVISIBLE);
        }
    }

    /**
     * Command
     * var c = Commander.{CONSTANT}
     * TODO Move this method to Command class which makes much more sense.
     */
    public boolean cmd(int c){

        recorder.displayStatus("");

        if(!recorder.isReadyToStart() && c != Commander.START_STOP_BACKUP){
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

        // If it's on recording, Speak "On air now and QUIT"
        // TODO Would like to consider to STOP by any touch events to make it easier.
        if(recorder.isRecording()) {
            Log.i(TAG, "Command in isRecording ===>" + c);
            if (c == Commander.ONETOUCH || c == Commander.STOP_RECORD) {
                recorder.stopRecording();
                cmdStr = getResources().getString(R.string.STOP_RECORD);
                speak(getResources().getString(R.string.STOP_RECORD));
                imageView.setImageResource(R.drawable.ic_action_stop);
                this.displayNotification(cmdStr, R.drawable.ic_action_stop);
            }else {
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

                    break;
                case Commander.ONETOUCH:
                    vibrate(this.VIBRATOR_STRENTH);

                    Log.i(TAG, "isPaused:"+recorder.isPaused()+":isPlaying:"+recorder.isPlaying());
                    if (recorder.isPlaying() && !recorder.isPaused()) {
                        recorder.pause();
                        cmdStr = getResources().getString(R.string.PAUSE);
                        this.displayText(recorder.getCurrentFileDisplayInformation());
                        imageView.setImageResource(R.drawable.ic_action_pause);
                        speak(cmdStr);
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_pause);
                    } else if(!recorder.isPlaying() && recorder.isPaused()) {
                        recorder.resume();
                        cmdStr = getResources().getString(R.string.RESUME);
                        this.displayText(recorder.getCurrentFileDisplayInformation());
                        imageView.setImageResource(R.drawable.ic_action_play);
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        speak(cmdStr);
                    } else if(!recorder.isPlaying() && !recorder.isPaused()) {
                        // This display MUST be earlier than actual play
//                        if(recorder.isJustRecordFinished()){
//                            this.displayText(recorder.getNewRecordedFile().getName() +
//                                    " is just recorded.");
//                            this.displayNotification(recorder.getNewRecordedFile().getName() +
//                                    " is just recorded.", R.drawable.ic_action_play);
//                        }else {
//                            this.displayText(recorder.getCurrentFileDisplayInformation());
//                            this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
//                        }
                        recorder.startPlaying();
                        this.displayText(recorder.getCurrentFileDisplayInformation());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        imageView.setImageResource(R.drawable.ic_action_play);
                    }
                    break;
                case Commander.NEXT_SONG:
                    vibrate(this.VIBRATOR_STRENTH);

                    cmdStr = getResources().getString(R.string.NEXT_SONG);
                    recorder.stopPlaying();
                    if(recorder.nextSong()) {
                        recorder.startPlaying();
                        this.displayText(recorder.getCurrentFileDisplayInformation());
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
                        this.displayText(recorder.getCurrentFileDisplayInformation());
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
                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
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
                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
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
                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
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

                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
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
                    this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_about);
                    this.alwaysSpeak(main_TextView.getText().toString());
                    //this.trackPos_TextView.setText(recorder.getPosition());

                    break;
                case Commander.SPEAK_DATE_TIME:
                    vibrate(this.VIBRATOR_STRENTH);
                    imageView.setImageResource(R.drawable.ic_action_about);
                    this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_about);
                    String temp = "It is " + getDateTime() + "\n" + "Battery level is "
                            + ((MainActivity) mainActivity).getBatteryLevel() + " percents";
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
                        alwaysSpeak("Stopped speaking");
                    }
                    else {
                        mainActivity.isTTSEnabled(true);
                        ((MainActivity)mainActivity).setIsTTSEnabled(true);
                        progress_TextView.setText("TTS-On");
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
                    this.displayText("---Lyrics---\n\n" +
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
                    mainActivity.setSettingsValues();

                    break;
            }
        }
        //speak(cmdStr);

        return true;
    }

    public String getDateTime() {
        Calendar c = Calendar.getInstance();
        //SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, ''yyyy 'at' h:mm a", Locale.US);
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a, EEE, MMM d", Locale.US);

        String currentDateandTime = sdf.format(Calendar.getInstance().getTime());
        return currentDateandTime;
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
        boolean wasSpoke = false;
        int div = mainActivity.getPokingEyesWarning();
        // Just in case, failing to get value from Settings(pokingEyesWarning
        // Default == Once a while
        if(div < 5){
            div = 11;
        }

        Log.i(TAG, "Poking: Folder Move Cnt:div ---> " + folderMoveCnt + ":" + div);

        if(folderMoveCnt%div == 0) {
            wasSpoke = true;
            //ttobj.setPitch(1.2f);
            ttobj.speak("Sangjoon, Stop poking eyes!", TextToSpeech.QUEUE_ADD, null, null);
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
            ttobj.speak(randomTest, TextToSpeech.QUEUE_ADD, null, null);
            Log.i(TAG,"ramdomMathTest.LuckNumber--->"+randomTest);
        }

        return wasSpoken;

    }

}
