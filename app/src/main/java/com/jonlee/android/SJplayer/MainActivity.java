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

import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import androidx.multidex.MultiDex;
import androidx.core.app.ActivityCompat;

import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.services.drive.DriveScopes;
import com.jonlee.android.common.activities.SampleActivityBase;
import com.jonlee.android.common.logger.Log;
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
import java.util.Arrays;
import java.util.Locale;

import static com.jonlee.android.SJplayer.DriveMainActivity.REQUEST_ACCOUNT_PICKER;


/**
 * A simple launcher activity containing a summary sample description
 * and a few action bar buttons.
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "MainActivity";
    public static final String FRAGTAG = "BasicGestureDetectFragment";

    // Place to manage all gesture commands
    public Commander commander = null;
    public Recorder recorder;
    private boolean isSangJoon = false;
    private boolean isTTSEnabled = true;
    private boolean isMathTestEnabled = false;
    private boolean isHomemodeEnabled = false;
    private boolean isNavModeEnabled = false;
    private int maxVolume = 100;

    public TextToSpeech ttobj = null;
    public int batteryLevel = 0;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MultiDex.install(this);
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

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        setContentView(R.layout.activity_main);

        TextView main_TextView = (TextView)findViewById (R.id.main_TextView);
        progressTextView = (TextView)findViewById (R.id.progress_TextView);
        ((ProgressBar)findViewById(R.id.musicProgressBar)).setVisibility(ProgressBar.INVISIBLE);
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
                }
            }
        }
        );


        this.registerReceiver(this.mBatInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

//        this.registerReceiver(this.mStorageInfoReceiver,
//                new IntentFilter(Intent.ACTION_DEVICE_STORAGE_LOW));

        commander = new Commander(this);
        recorder = commander.getRecorder();
        //readWelcomeMsg();
        //speak(getResources().getString(R.string.WELCOME));

        // PackageManager.PERMISSION_GRANTED = 0, PackageManager.PERMISSION_DENIED = -1, checking either case.
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) < 1 ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) < 1  ||
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) < 1 )
        {


//            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
//                    Manifest.permission.RECORD_AUDIO)) {
//                // Show an explanation to the user *asynchronously* -- don't block
//                // this thread waiting for the user's response! After the user
//                // sees the explanation, try again to request the permission.
//            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.RECORD_AUDIO,
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        },
                        0);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
//            }
        }

//        preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
//            @Override
//            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
//                Log.i(TAG, "SharedPref Changed!");
//
//            }
//        };

    }

    public void setSettingsValues(){
        isTTSEnabled = sharedPref.getBoolean( "isTTSEnabled",true);
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
            pokingEyesWarning = Integer.parseInt(sharedPref.getString( "poking_eye_warning","10"));
        }catch(NumberFormatException ne){
            pokingEyesWarning = 10;
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
    }

    public boolean isTTSEnabled() { return this.isTTSEnabled; }
    public boolean isNavModeEnabled() { return this.isNavModeEnabled; }
    public void isTTSEnabled(boolean b){ this.isTTSEnabled = b; }
    public String getFoldersFromSettings() { return foldersFromSettings; }
    public String getHomeworkFoldersFromSettings() { return homeworkFoldersFromSettings; }
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
            ttobj.setPitch(1.0f);
            ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
        }

    }

    public void speak(String w, float pitch) {
        if(isTTSEnabled) {
            ttobj.setPitch(pitch);
            ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
        }

    }

    public void speak(String w, float pitch, int queue) {
        if(isTTSEnabled) {
            ttobj.setPitch(pitch);
            ttobj.speak(w, queue, null, null);
        }
    }

    public void waitUntilTTSFinished(boolean wait){
        if(wait){
            while(ttobj.isSpeaking()){
                //Doing something...

            }
        }
    }

    public TextToSpeech getTextToSpeech(){
        return ttobj;
    }

        public void alwaysSpeak(String w){
        ttobj.setPitch(1.0f);
        ttobj.speak(w, TextToSpeech.QUEUE_FLUSH, null, null);
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
    protected void onPause(){
        //this.sharedPref.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
        super.onPause();
        try {
            this.unregisterReceiver(this.mBatInfoReceiver);
        }catch(Exception e){
            //Nothing
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        int volume_level= am.getStreamVolume(AudioManager.STREAM_MUSIC);
        progressTextView.setText("Volume:" + (volume_level*100)/am.getStreamMaxVolume(AudioManager.STREAM_MUSIC) + "%");

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

        String generate_URL = "https://docs.google.com/document/d/18k5apl6L5GZmVhY2d7O5aUljkQB2e6EyBq1L3kE13sQ/edit";
        String inputLine;

        Log.i(TAG, "Read webpage >>>" + generate_URL);
        try {
            URL data = new URL(generate_URL);
            /**
             * Proxy code start
             * If you are working behind firewall uncomment below lines.
             * Set your proxy server
             */

            /* Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("192.168.0.202", 8080)); */
            /* HttpURLConnection con = (HttpURLConnection) data.openConnection(proxy); */

            /* Proxy code end */

            /* Open connection */
            /* comment below line in case of Proxy */
            HttpURLConnection con = (HttpURLConnection) data.openConnection();
            /* Read webpage coontent */
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            /* Read line by line */
            StringBuilder sb = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                Log.i(TAG, "WebMsg >>>" + inputLine);
                sb.append(inputLine);
            }
            /* close BufferedReader */
            in.close();
            /* close HttpURLConnection */
            con.disconnect();
            speak(sb.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

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
        //setSettingsValues();
       // sharedPref.registerOnSharedPreferenceChangeListener(preferenceChangeListener);
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu..game_menu, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        Log.i(TAG, "Selected menu >>>" + item.getTitle());
//        // Handle item selection
//        switch (item.getItemId()) {
//            case R.xml.pref_general:
//                readHomeWorkAssigned();
//                return true;
////            case R.id.help:
////                showHelp();
////                return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
}
