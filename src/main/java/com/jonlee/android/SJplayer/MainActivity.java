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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jonlee.android.common.activities.SampleActivityBase;
import com.jonlee.android.common.logger.Log;
import com.jonlee.android.common.logger.LogWrapper;
import com.jonlee.android.common.logger.MessageOnlyLogFilter;

import java.util.Locale;



/**
 * A simple launcher activity containing a summary sample description
 * and a few action bar buttons.
 */
public class MainActivity extends SampleActivityBase{

    public static final String TAG = "MainActivity";
    public static final String FRAGTAG = "BasicGestureDetectFragment";
    public TextToSpeech ttobj = null;
    public TextView textView = null;
    public Recorder recorder = null;

    // Creating notificaiton builder
    NotificationCompat.Builder  mBuilder;

    /**
     * Notification
     */
    private NotificationManager mNotificationManager;
    private int notificationID = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Remove title bar
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // TODO: Only when app runs recoder
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

//        int newUiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//
//        // Navigation bar hiding:  Backwards compatible to ICS.
//        if (Build.VERSION.SDK_INT >= 14) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
//        }
//
//        // Status bar hiding: Backwards compatible to Jellybean
//        if (Build.VERSION.SDK_INT >= 16) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
//        }
//
//        // Immersive mode: Backward compatible to KitKat.
//        // Note that this flag doesn't do anything by itself, it only augments the behavior
//        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
//        // all three flags are being toggled together.
//        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
//        // Sticky immersive mode differs in that it makes the navigation and status bars
//        // semi-transparent, and the UI flag does not get cleared when the user interacts with
//        // the screen.
//        if (Build.VERSION.SDK_INT >= 18) {
//            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
//        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        //getWindow().getDecorView().setSystemUiVisibility(newUiOptions);

//        setContentView(R.layout.activity_main);
//        textView = (TextView) findViewById(R.id.sample_output);

        setContentView(new RelativeLayout(this) {
            {
                addView(new DialView(getContext()) {
                    {
                        // a step every 5°
                        setStepAngle(10f);
                        // area from 30% to 100%
                        setDiscArea(.30f, 1.00f);

                    }
                    @Override
                    protected void onRotate(int offset) {
                        //textView.setText(String.valueOf(recorder.getCurrentFileName()));
//                        textView.setText(String.valueOf(value += offset));
                    }
                }, new RelativeLayout.LayoutParams(0, 0) {
                    {
                        width = MATCH_PARENT;
                        height = MATCH_PARENT;
                        addRule(RelativeLayout.CENTER_IN_PARENT);
                    }
                });
                addView(textView = new TextView(getContext()) {
                    {
                        setText(getString(R.string.WELCOME));
                        setTextColor(Color.parseColor("#2cc3ba"));
                        //setTextColor(Color.parseColor("#178ad0"));
//                        Typeface face = Typeface.createFromAsset(getAssets(),
//                                "font/minisys.ttf");
                        Typeface face = Typeface.SERIF;
                        DisplayMetrics displaymetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
                        int height = displaymetrics.heightPixels;
                        int width = displaymetrics.widthPixels;
                        setWidth((int)(width*0.9));
                        setTypeface(face);
                        setTextSize(18);
                        setTextAlignment(TEXT_ALIGNMENT_CENTER);

                    }
                }, new RelativeLayout.LayoutParams(0, 0) {
                    {
                        width = WRAP_CONTENT;
                        height = WRAP_CONTENT;
                        addRule(RelativeLayout.CENTER_IN_PARENT);
                    }
                });
            }
        });

//        super.onCreate(savedInstanceState);
//
//        if (getSupportFragmentManager().findFragmentByTag(FRAGTAG) == null ) {
//            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//            BasicGestureDetectFragment fragment = new BasicGestureDetectFragment();
//
//            fragment.setActivity(this);
//            transaction.add(fragment, FRAGTAG);
//            transaction.commit();
//        }

        ttobj=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR){
//                    ttobj.setLanguage(Locale.US);
                    ttobj.setSpeechRate(2.5f);
                    //ttobj.setPitch(1);
                    speak(getResources().getString(R.string.WELCOME));
                }

            }
        }
        );

        // Create Recorder
        recorder = new Recorder(this);

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
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
//        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
//                .findFragmentById(R.id.log_fragment);
//        msgFilter.setNext(logFragment.getLogView());
//        logFragment.getLogView().setTextAppearance(this, R.style.Log);
//        logFragment.getLogView().setBackgroundColor(Color.WHITE);
//
//        Log.i(TAG, "Ready");

    }
//
//    public void setTtsString(String str){
//        this.ttsString = str;
//    }

    /**
     * Let TTS speaks
     * @param w
     */
    public void speak(String w) {
        ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null);
        //textView.append("Command: " + w + "\n");
    }

    public void vibrate(){
        try {
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(100);
        }catch(Exception e) {
            // Do nothing
        }
    }

    public void vibrate(int s){
        try{
            Vibrator v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
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
        textView.setText(w);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
    }

    /**
     * Jogg Dial SEEK
     */
    public void seek(int i){
        recorder.seek(i);
    }

    /**
     * Command
     * var c = Command.{CONSTANT}
     */
    public boolean cmd(int c){
        String cmdStr = "Invalid command";
        vibrate();
        //Stop speaking when new action is coming
        speak("");

        //Log.i(TAG,"cmd:"+c);

        // If it's on recording, Speak "On air now and QUIT"
        // TODO Would like to consider to STOP by any touch events to make it easier.
        if(recorder.isRecording()) {
            if (c == Command.ONETOUCH || c == Command.STOP_RECORD) {
                recorder.stopRecording();
                cmdStr = getResources().getString(R.string.STOP_RECORD);
                speak(getResources().getString(R.string.STOP_RECORD));
                this.displayNotification(cmdStr, R.drawable.ic_action_stop);
            }else {
                cmdStr = getResources().getString(R.string.ON_AIR);
                this.displayNotification(cmdStr, R.drawable.ic_action_record);
            }
            displayText(cmdStr);
        } else {
            switch (c) {
                case Command.START_RECORD:
                    //TODO any possibility of exception?
                    recorder.stopPlaying();

                    cmdStr = getResources().getString(R.string.START_RECORD);
                    this.displayText(cmdStr);
                    //Waiting for until tts ends up.
                    speak(getResources().getString(R.string.START_RECORD) + ", Start!");

                    while(ttobj.isSpeaking()) {
                        //Waiting until tts speaks out.
                        try {
                            Thread.sleep(500);
                        }catch (Exception e){
                            // In case of Exception, stopping to talk.

                            Log.i(TAG, e.toString());
                            speak("");
                        }
                    }

                    recorder.startRecording();
                    this.displayNotification(cmdStr, R.drawable.ic_action_record);
                    break;
                case Command.ONETOUCH:
                    Log.i(TAG, "isPaused:"+recorder.isPaused()+":isPlaying:"+recorder.isPlaying());
                    if (recorder.isPlaying() && !recorder.isPaused()) {
                        recorder.pause();
                        cmdStr = getResources().getString(R.string.PAUSE);
                        this.displayText("||\n\n" + recorder.getCurrentFileDisplayInformation());
                        speak(cmdStr);
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_pause);

                    } else if(!recorder.isPlaying() && recorder.isPaused()) {
                        recorder.resume();
                        cmdStr = getResources().getString(R.string.RESUME);
                        this.displayText(">>\n\n" + recorder.getCurrentFileDisplayInformation());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        speak(cmdStr);
                    } else if(!recorder.isPlaying() && !recorder.isPaused()) {
                        recorder.startPlaying();
                        cmdStr = getResources().getString(R.string.START_PLAYBACK);
                        this.displayText(">>\n\n" + recorder.getCurrentFileDisplayInformation());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                        //speak(cmdStr);
                    }
                    break;
                case Command.NEXT_SONG:
                    cmdStr = getResources().getString(R.string.NEXT_SONG);
                    recorder.stopPlaying();
                    if(recorder.nextSong()) {
                        recorder.startPlaying();
                        this.displayText(">>\n\n" + recorder.getCurrentFileDisplayInformation());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                    } else{
                        this.displayText("No file exists in this folder");
                        this.displayNotification(cmdStr, R.drawable.ic_action_stop);
                    }
                    break;
                case Command.PREVIOUS_SONG:
                    cmdStr = getResources().getString(R.string.PREVIOUS_SONG);
                    recorder.stopPlaying();
                    if(recorder.previousSong()) {
                        recorder.startPlaying();
                        this.displayText(">>\n\n"+recorder.getCurrentFileDisplayInformation());
                        this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_play);
                    } else{
                        this.displayText("No file exists in this folder");
                        this.displayNotification(cmdStr, R.drawable.ic_action_stop);
                    }
                    break;
                case Command.NEXT_FOLDER:
                    recorder.stopPlaying();
                    recorder.nextFolder();
                    cmdStr = getResources().getString(R.string.NEXT_FOLDER);
                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);

                    break;
                case Command.PREVIOUS_FOLDER:
                    recorder.stopPlaying();
                    recorder.previousFolder();
                    cmdStr = getResources().getString(R.string.PREVIOUS_FOLDER);
                    speak("Folder " + recorder.getCurrentDirectoryName());
                    this.displayText(recorder.getCurrentDirectoryInformation());
                    this.displayNotification(recorder.getCurrentDirectoryName(), R.drawable.ic_action_collection);


                    break;
//                case Command.FAST_FORWARD_2X:
//                    cmdStr = getResources().getString(R.string.FAST_FORWARD_2X);
//                    this.displayText(cmdStr);
//                    break;
//                case Command.FAST_BACKWARD_2X:
//                    cmdStr = getResources().getString(R.string.FAST_BACKWARD_2X);
//                    break;
                case Command.SPEAK_FILE_INFO:
                    this.displayNotification(recorder.getCurrentFileDisplayInformation(), R.drawable.ic_action_about);
                    this.speak(textView.getText() + "");

                    break;
//                case Command.NOTHING:
//                    break;
            }
        }
        //speak(cmdStr);
        return true;
    }

//    /**
//     * TODO make this better as it calls a method in DialView which needs to be called from Recorder.
//     */
//    public void setDirMode(int mode){
//        dialView.setDirMode(mode);
//    }

    /**
     * Adding notification
     */

    public void displayNotification(String msg, int imageR) {
        Log.i("Start", "notification");

      /* Invoking the default notification service */
        mBuilder =
                new NotificationCompat.Builder(this);

        mBuilder.setContentTitle("SJ Player is running");
        mBuilder.setContentText(msg);
        //mBuilder.setTicker("New Activity");
        //mBuilder.setSmallIcon(R.drawable.ic_action_play);
        mBuilder.setSmallIcon(imageR);

      /* Increase notification number every time a new notification arrives */
        //mBuilder.setNumber(++numMessages);

      /* Creates an explicit intent for an Activity in your app */
        //Intent resultIntent = new Intent(this, MainActivity.class);

        Intent intent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(getPackageManager().getLaunchIntentForPackage(getPackageName()).getComponent());

//        resultIntent.setAction(Intent.ACTION_MAIN);
//        resultIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        //TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        //stackBuilder.addParentStack(MainActivity.class);

        //stackBuilder.setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));

      /* Adds the Intent that starts the Activity to the top of the stack */
        //stackBuilder.addNextIntent(intent);


        /**
         * Blocked to launch Activity as it brings to HOME always.
         */
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
//                intent, 0);

//        try {
//            PendingIntent.getActivity(this, 0, intent, 0).send();
//        } catch (PendingIntent.CanceledException e) {
//            e.printStackTrace();
//        }

        //mBuilder.setContentIntent(pendingIntent);

        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

      /* notificationID allows you to update the notification later on. */
        mNotificationManager.notify(notificationID, mBuilder.build());
    }

    /**
     * Same behavior with HOME button by BACK key
     */
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

}
