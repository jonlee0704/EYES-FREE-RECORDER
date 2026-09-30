package com.jonlee.android.SJplayer;

/**
 * Created by jongyeong on 6/15/14.
 */

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.provider.DocumentsContract;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.preference.PreferenceManager;


import com.jonlee.android.common.utils.FileUtils;
import com.jonlee.android.common.utils.MimeUtils;
import android.Manifest;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * TODO Adding "Cloud folder" of Playing back YouTube bookamark
 * TODO Adding "Cloud folder" of
 */
public class Recorder {
    private static final String TAG = "SJRecorder.Recorder";
    public static final String SHARED_FOLDER = "SJ-RECORDER-SHARED-FOLDER";


    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private boolean isHomeWorkMode = false;
    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;
    private MusicRetriever musicRetriever;

    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private File rootFolder;
    // The folder current cursor is on.
    private File currentFolder;
    // MMMM-yyyy under the root folder
    private File recordTargetFolder;
    private File publicMusicFolder;
    private File trashFolder;
    private File lyricsFolder;
    private File sharedFolder;
    private File folderForAllMusicByMediaScanner;
    private File albumImage;
    private ArrayList<File> audibleFiles;
    // only below the ROOT and 1 level folders only
    private ArrayList<File> directories = null;
    private final Set<String> directoryPathSet = new HashSet<>();
    private final Set<String> scannedDirPaths = new HashSet<>();
    private long lastStatusUpdateTime = 0;

    // Audio recording session metadata
    private long recordStartTimeMillis = 0;
    private String recordingAudioSource = "Phone Microphone";
    private String recordingLocationName = null;
    private int recordingChannels = 1;
    private final List<Integer> recordingBookmarks = new ArrayList<>();


    private int currentFileIndex = 0;
    private int currentDirectoryIndex = 0;

    //Total file duration
    private int audioFileDuration = 0;

    //Only for comparing FolderName
    List<String> mFolderNameinLocalStorage;

    //Manual file name
//    private final String manualFile = "WELCOME.txt";
    private final String folderNameForAllMusicByMediaScanner = "All-Music-By-MediaScanner";
    private final String STRING_SJ_RECORDER_DATA = "SJ-RECORDER-DATA";
    // If SJ recorder reads and knows which folders have audio files,
    // no need to re-read again when it's launch next time.
    public final static String VALID_FOLDER_LIST_LOG_FILE = "SJ-RECORDER-VALID-FOLDERS.txt";
    private File validFolderListLogFile;
    private FileWriter validFolderWriter;
    private Map<String, String> validFolderListMap;

    //Total continuously playback file count
    private int playCnt = 0;
    //Stop automatic playback when it gets in this nunber
    private final int MAX_CONTINOUS_PLAYBACK = 30;

    private final int DURATION_SHORT_FORM = 0;
    private final int DURATION_LONG_FORM = 1;

    private TextView status_TextView;


    private String statusMsg;

    private boolean isReadyToStart = false;

    // To check multiple Thread of reading whole directories
    private int isReadyToStartCount = 0;

    private int scannedFolderCnt = 0;
    private int scannedFileCnt = 0;

    AudibleFileFilter aFilter;

    private Activity mainActivity;

    //Timer
    private double mil = 0;

    //runs without a timer by reposting this handler at the end of the runnable
    final Handler timerHandler = new Handler();

    private Thread timerThread = null;
    private int readFromFileCnt = 0;

    //File name just just created.
    private  String newFileName = null;
    private String currentRecordingFileName = null;
    private Uri currentRecordingUri = null;
    //To playback a record file that is just recorded
    private boolean justRecordFinished = false;
    private String preFixStr;
    /**
     * This is for "Backup agent" only
     */

    /**
     * Number initially scanned.
     * Meaningful to compare after backup
     * @return
     */
    public int getTotalScannedFolderCnt(){
        return scannedFolderCnt;
    }
    public int getTotalScannedFileCnt(){
        return scannedFileCnt;
    }

    public File getRecordTargetFolder(){
        return recordTargetFolder;
    }

    public File getRootFolder(){
        return rootFolder;
    }

    /**
     * TODO: Is this correct way to pass Activity to another class?
     *
     * @param ma: MainActivity
     *            Starting from
     */
    public Recorder(MainActivity ma)
    {
        mainActivity = ma;
        setupAudioFocus();
        status_TextView = (TextView) ma.findViewById(R.id.trackPos_TextView);
        preFixStr = ((MainActivity)mainActivity).getHomeworkFilePrefix();
        aFilter = new AudibleFileFilter();
        //To use boolean value in the recursive mode, not to ask too many time.
        //DEPRECATED
//        aFilter.setPreFixMode(((MainActivity)mainActivity).isHomemodeEnabled());
//        aFilter.setPreFix(((MainActivity)mainActivity).getHomeworkFilePrefix());
        directories = new ArrayList<File>();
        createSystemFolders();
        isHomeWorkMode = ((MainActivity) mainActivity).isHomemodeEnabled();


        validFolderListMap = FileUtils.FileToMap(validFolderListLogFile, "\t");
        Log.i(TAG, "Folder loaded from log file...");

        musicRetriever = new MusicRetriever(mainActivity.getContentResolver());

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    displayStatus("Scanning audio folders...");
                    readDirectories();
                    flushValidFoldersLog();

                    sortDirectories();

                    if (directories.size() < 1) {
                        ((MainActivity) mainActivity).speak("No external storage found. Please select a folder in the settings.");
                    } else {
                        currentFolder = directories.get(0);
                        readAudibleFilesInCurrentFolder();
                    }

                    isReadyToStart = true;

                    String readyMsg = directories.size() + " folders, "
                            + scannedFileCnt + " files scanned. Ready to use!";
                    displayStatus(readyMsg);
                    Log.i(TAG, readyMsg);
                    ((MainActivity) mainActivity).speak(readyMsg);
                    mainActivity.runOnUiThread(() -> {
                        if (((MainActivity) mainActivity).getCommander() != null) {
                            ((MainActivity) mainActivity).getCommander().updateFolderDisplay();
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Error in audio folder scan thread", e);
                    isReadyToStart = true;
                }
            }
        }).start();

    }

    private void setupAudioFocus() {
        audioManager = (AudioManager) mainActivity.getSystemService(Context.AUDIO_SERVICE);
        audioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                switch (focusChange) {
                    case AudioManager.AUDIOFOCUS_LOSS:
                        if (isPlaying) {
                            pause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                        if (isPlaying) {
                            pause();
                        }
                        break;
                    case AudioManager.AUDIOFOCUS_GAIN:
                        if (isPaused) {
                            resume();
                        }
                        break;
                }
            }
        };
    }

    public boolean requestAudioFocus() {
        if (audioManager == null) return false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest == null) {
                AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(playbackAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(audioFocusChangeListener)
                        .build();
            }
            return audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            return audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
    }

    public void abandonAudioFocus() {
        if (audioManager == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            if (audioFocusChangeListener != null) {
                audioManager.abandonAudioFocus(audioFocusChangeListener);
            }
        }
    }

    private void sortDirectories(){
        // Descending sorting
        // (X->A)->(10->1)->!@#$
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare (File f1, File f2)
            {
                //displayStatus("Sorting directories...");
                return f2.getName().compareTo(f1.getName());
            }

        });
    }

    public synchronized boolean addDirectory(File dir) {
        if (dir == null) return false;
        String path = dir.getAbsolutePath();
        if (directoryPathSet.add(path)) {
            directories.add(dir);
            return true;
        }
        return false;
    }

    public synchronized boolean hasDirectory(File dir) {
        if (dir == null) return false;
        return directoryPathSet.contains(dir.getAbsolutePath());
    }

    public void displayStatusThrottled(String msg) {
        long now = System.currentTimeMillis();
        if (now - lastStatusUpdateTime > 200) {
            lastStatusUpdateTime = now;
            displayStatus(msg);
        }
    }

    public void logValidFolders(String folderName, int fileCnt) {
        try {
            if (validFolderWriter == null && validFolderListLogFile != null) {
                validFolderWriter = new FileWriter(validFolderListLogFile, true);
            }

            if (validFolderWriter != null) {
                validFolderWriter.append(folderName).append("\t").append(String.valueOf(fileCnt)).append("\n");
            }
        } catch (IOException ie) {
            Log.i(TAG, "Fail to write " + this.VALID_FOLDER_LIST_LOG_FILE + ": " + ie.getMessage());
        }
    }

    public void flushValidFoldersLog() {
        try {
            if (validFolderWriter != null) {
                validFolderWriter.flush();
            }
        } catch (IOException ignored) {}
    }

    private void createSystemFolders() {
        // Find a bigger size folder between extSD vs removableExtSDcard
        // And set it the rootFolder
        rootFolder = getBiggerExtSDCardDirectory();
        if (rootFolder == null) {
            rootFolder = mainActivity.getFilesDir();
        }
        // Creating root_folder in case there is not.
        if (!rootFolder.exists()) {
            rootFolder.mkdirs();
        }

        // Starting with '.' won't be included into File.directories
        trashFolder = new File(rootFolder.getAbsolutePath() + "/.Trash");
        // Creating root_folder in case there is not.
        if (!trashFolder.exists()) {
            trashFolder.mkdirs();
        }

        // Creating a virtual directory to play all musics provided by MediaScanner
        // TODO Create a file to explain that this folder won't be scanned
        // folderNameForAllMusicByMediaScanner will be under Public Music folder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            publicMusicFolder = mainActivity.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            // On Android 10+, the 'Music' folder for MediaStore is the system folder, 
            // but we'll also point recordTargetFolder to a subfolder in system Music 
            // so the UI can find and play the recordings.
            recordTargetFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SJPlayer/" + this.getRecordingFolderName());
        } else {
            publicMusicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            recordTargetFolder = new File(rootFolder.getAbsolutePath() + "/" + this.getRecordingFolderName());
        }

        if (!publicMusicFolder.exists()) {
            publicMusicFolder.mkdirs();
        }

        //TODO Fail to create a folder under public music folder
        //Testing under app directory
        //folderForAllMusicByMediaScanner = new File(publicMusicFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);
        folderForAllMusicByMediaScanner = new File(rootFolder.getAbsolutePath() + "/" + folderNameForAllMusicByMediaScanner);
        if (!folderForAllMusicByMediaScanner.exists())
            folderForAllMusicByMediaScanner.mkdirs();

        if (!recordTargetFolder.exists()) {
            recordTargetFolder.mkdirs();
        }

        sharedFolder = new File(rootFolder.getAbsolutePath() + "/" + SHARED_FOLDER);
        if (!sharedFolder.exists()) {
            sharedFolder.mkdirs();
        }

        // Starting with '.' won't be included into File.directories
        // Lyrics files are always under PublicMusicFolder as should not be deleted
        // And also easily found when it's copied into.
        lyricsFolder = new File(rootFolder.getAbsolutePath() + "/Lyrics");
        // Creating lyrics_folder
        if (!lyricsFolder.exists()) {
            lyricsFolder.mkdirs();
        }

        // Log file to store valid folders including audio files
        validFolderListLogFile = new File(rootFolder.getAbsolutePath() + "/" + this.VALID_FOLDER_LIST_LOG_FILE);
        if (!validFolderListLogFile.exists()) {
            try {
                validFolderListLogFile.createNewFile();
            } catch (IOException io) {
                io.printStackTrace();
            }
        }

        Log.i(TAG,
                "folderForAllMusicByMediaScanner:" + folderForAllMusicByMediaScanner + "\n" +
                        "RootFolder: " + rootFolder.getAbsolutePath() + "\n" +
                        "TrashFolder: " + trashFolder.getAbsolutePath() + "\n" +
                        "PublicMusicFolder: " + publicMusicFolder.getAbsolutePath() + "\n" +
                        "recordTargetFolder: " + recordTargetFolder.getAbsolutePath() + "\n" +
                        "sharedFolder: " + sharedFolder.getAbsolutePath() + "\n" +
                        "foldersFromSettings: " + ((MainActivity) mainActivity).getFoldersFromSettings() + "\n" +
                        "homeworkFoldersFromSettings: " + ((MainActivity) mainActivity).getHomeworkFoldersFromSettings() + "\n" +
                        "homeworkFoldersPreFixSettings: " + ((MainActivity) mainActivity).getHomeworkFilePrefix() + "\n" +
                        "LyricsFolder: " + lyricsFolder.getAbsolutePath()
        );
    }

    // Handler 객체를 생성하고
    // handleMessage(Message)를 오버라이딩하여
    // mHandler 인스턴스에게 전달 되는 모든 메시지를 처리함.
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //mProgress.setProgress(mProgressStatus);
            status_TextView.setText(statusMsg);
        }
    };


    private Handler mToastHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            //mProgress.setProgress(mProgressStatus);
            Toast.makeText(mainActivity.getApplicationContext(), statusMsg, Toast.LENGTH_SHORT).show();
        }
    };

    //Return audio file number in current folder
    public int getNumberOfAudioFiles(){
        return audibleFiles.size();
    }

    public boolean isReadyToStart(){
        return this.isReadyToStart;
    }

    public File getLyricsFolder(){
        return this.lyricsFolder;
    }

    public ArrayList<File> getDirectories(){
        return directories;
    }

    public String getAllTargetFoldersSummary() {
        if (directories == null || directories.isEmpty()) {
            return "No folders scanned";
        }

        Set<String> parentPaths = new HashSet<>();
        Pattern datePattern = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}.*");

        for (File dir : directories) {
            String folderName = dir.getName();
            File parent = dir.getParentFile();

            // If the folder name matches a date pattern (yyyy-MM-dd), use its parent.
            if (datePattern.matcher(folderName).matches() && parent != null) {
                parentPaths.add(parent.getAbsolutePath());
            } else {
                parentPaths.add(dir.getAbsolutePath());
            }
        }

        if (parentPaths.isEmpty()) {
            return "No folders scanned";
        }

        List<String> sortedParents = new ArrayList<>(parentPaths);
        Collections.sort(sortedParents);

        StringBuilder sb = new StringBuilder();
        for (String path : sortedParents) {
            sb.append(path).append("\n");
        }

        return sb.toString().trim();
    }

    /**
     * Whenever recorderStart called, it checks if the date_folder exist and add to directories.
     * @return
     */
    public void initiateTargetFolder(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recordTargetFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SJPlayer/" + this.getRecordingFolderName());
        } else {
            recordTargetFolder = new File(rootFolder.getAbsolutePath() + "/" + this.getRecordingFolderName());
        }
        if(!recordTargetFolder.exists()) {
            recordTargetFolder.mkdirs();
        }


        // After adding a new folder, Sort again!
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare (File f1, File f2)
            {
                //displayStatus("Sorting directories...");
                return f1.getName().compareTo(f2.getName());
            }

        });
    }

    /**
     * TODO: Need to make a decision if it's really need to configurable in Settings.
     * Default value is YYYY-MM-DD-HH-MM (or +LOCATION)
     *
     * @return
     */
    public String createFileName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HHmmss", Locale.US);
        return sdf.format(c.getTime());
    }

    /**
     * Get folder name by MMMM-YYYY
     * @return
     */
    public String getRecordingFolderName() {
        Calendar c = Calendar.getInstance();
        //SJ has too many recording files a month.
        //It's already more than 4K in July
        //This needs to be separated into Day
        //yyyy-MMMM-dd for sorting by date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        return sdf.format(c.getTime());
    }


    public int getCurrentFileIndex() {
        return this.currentFileIndex;
    }
    public void setCurrentFileIndex(int i) { currentFileIndex = i; }
    public void setCurrentDirectoryIndex(int i) {
        currentDirectoryIndex = i;
        currentFolder = directories.get(i);
    }

    /**
     * Retunr String converting from Array
     * @param n
     * @return
     */
//    public String getPreviousDirectories(int n) {
//        String str = "";
//        int i = this.currentDirectoryIndex;
//        Log.d("1 currentDirectory Index and Name:", i+":"+this.currentFolder.getName());
//        while( n-- > 0 ){
//            Log.d("getPreviousDirectorStrings i and n:", i + ":" +n);
//            if( i == 0) {
//                i = this.directories.size() - 1;
//            }
//            str = str + "\n" + this.directories.get(--i).getName();
//
//        }
//        return str;
//    }
//
//    public String getNextDirectories(int n) {
//        String str = "";
//        int i = this.currentDirectoryIndex;
//        Log.d("2 currentDirectory Index and Name:", i+":"+this.currentFolder.getName());
//        for( ; n > 0 ; n--, ++i){
//            if( i >= this.directories.size()) {
//                i = 0;
//            }
//            Log.d("getNextDirectorStrings i and n:", i + ":" +n);
//            str = str + "\n" + this.directories.get(i).getName();
//        }
//        return str;
//    }

    /**
     * Get list of previous Directories to display ...
     * @param n
     * @return
     */
    public File[] getNextDirectorList(int n) {
        File[] files = new File[n];
        for(int i = 0; i < n ; i++){
            files[i] = this.directories.get(i);
        }
        return files;
    }

    public boolean isPlaying() {
        return this.isPlaying;
    }

    public void isPlaying(boolean i) {
        this.isPlaying = i;
    }

    public boolean isRecording() {
        return this.isRecording;
    }

    public void isRecording(boolean i) {
        this.isRecording = i;
    }

    /**
     * FF
     * seekTo between 0~mPlayer.getDuration
     * Once it's called, it's +10 on current index.
     */
    public void seek(int d) {
        //When Folder is changed, mPlayer will be NULL
        if (d > 0) {
            if (mPlayer != null && this.audioFileDuration > mPlayer.getCurrentPosition() + d && d > 0)
                mPlayer.seekTo(mPlayer.getCurrentPosition() + d);
        } else {
            if (mPlayer != null && mPlayer.getCurrentPosition() + d > 0)
                mPlayer.seekTo(mPlayer.getCurrentPosition() + d);
        }
    }

    /**
     * Return 123/12345(current/total)
     * @return
     */
    public String getPosition(){
        String pos;
        if(mPlayer != null) {
            try {
                pos = getDurationBreakdown(mPlayer.getCurrentPosition(), this.DURATION_SHORT_FORM)
                        + "/" + getDurationBreakdown(mPlayer.getDuration(), this.DURATION_SHORT_FORM);
                //pos = mPlayer.getCurrentPosition() + "/" + mPlayer.getDuration();
//                pos = (mPlayer.getCurrentPosition()*100)/mPlayer.getDuration() + "%";
            }catch(java.lang.IllegalStateException e){
                //e.printStackTrace();
                pos = "---";
            }
        }
        else {
            pos = "---";
        }

        return pos;
    }

    /**
     * Playing from the latest file to old
     */
    public void startPlaying() {

        // If there is no files, just do nothing.
        // What will be better way than this ... way?
        if(audibleFiles.size() == 0)
            return;

        this.isPlaying = true;

        Log.i(TAG, "startPlaying() ===> " + currentFileIndex + ":" + audibleFiles.size());

        try {
            if (!isPaused || mPlayer == null) { // Resume does not need to initiate Instance
                mPlayer = new MediaPlayer();
            }

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    // 1) In case, phone incorrectly sending onCompletion event
                    // 2) Preventing to move next song in case Exception or 0 duration audio file exist.
                    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (playCnt < MAX_CONTINOUS_PLAYBACK) {
                                ((MainActivity) mainActivity).getCommander().cmd(Commander.NEXT_SONG);
                                playCnt++;
                            } else {
                                ((MainActivity) mainActivity).speak(mainActivity.getResources().getString(R.string.STILL_THERE));
                                playCnt = 0;
                            }
                        }
                    }, 300);
                }
            });

            if (this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
                /**
                 * TODO sometimes(fail to read file, then musicRetriever seems gettingto null)
                 * TODO it returns NULL, not clear when it is.
                 */
                try {
                    Log.i(TAG, "MusicRetriever:" + musicRetriever.getCurrentUri());
                    mPlayer.setDataSource(mainActivity, musicRetriever.getCurrentUri());
                } catch (Exception e) {
                    //Log.i("",e.toString());
                    e.printStackTrace();
                    return;
                }
            } else {
//                if(justRecordFinished){
//                    Log.i(TAG, "just after recording, playing new file:" + newFileName);
//                    mPlayer.setDataSource(this.newFileName);
//                }else {
//                    Log.i(TAG, "getCurrentFileFullPath():" + getCurrentFileFullPath());
//                    mPlayer.setDataSource(getCurrentFileFullPath());
//                }

                Log.i(TAG, "getCurrentFileFullPath():" + getCurrentFileFullPath());
                try {
                    mPlayer.setDataSource(getCurrentFileFullPath());
                } catch (Exception e) {
                    Log.w(TAG, "Direct path playback failed for " + getCurrentFileFullPath() + ", trying MediaStore URI fallback", e);
                    Uri contentUri = getUriForFilePath(getCurrentFileFullPath());
                    if (contentUri != null) {
                        mPlayer.setDataSource(mainActivity, contentUri);
                    } else {
                        throw e;
                    }
                }
            }
            mPlayer.prepare();
            mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    requestAudioFocus();
                    mediaPlayer.start();
                }
            });
            this.isPaused = false;
            //Set the duration of file to use in SEEK()
            this.audioFileDuration = mPlayer.getDuration();

            updateCurrentPosition();

        }catch(IllegalStateException ie){ //In case failed to play the datasource
            displayStatus("Something happened when to play back. Please try another cool sounds.");
            ie.printStackTrace();
        } catch (IOException e) {
//            //Log.i(TAG, "prepare() failed");
//            if(this.getCurrentFileName().startsWith("WELCOME.txt")) {
//                this.mainActivity.speak(mainActivity.getResources().getString(R.string.WELCOME));
//            } else {
//                //TODO Should be moving onto the next song or wait for user's action?
//                //TODO Waiting for user's action is better way.
//                //mainActivity.cmd(Commander.NEXT_SONG);
//                this.mainActivity.speak(mainActivity.getResources().getString(R.string.FAIL_TO_PLAY));
//                this.isPaused = false;
//                this.isPlaying = false;
////                playCnt++;
//            }
            audioFileDuration = -1;
            displayStatus(e.toString());
            Log.i(TAG, e.toString());
        } finally{
            justRecordFinished = false;
        }
    }

    /**
     * TODO PUBLIC_MUSIC is included by default, then the path might
     * @return
     */
    public String getCurrentFileFullPath(){
        return currentFolder.getAbsolutePath() + "/" + this.getCurrentFileName();
    }

    public void resume(){
        Log.i(TAG, currentFileIndex + ":" + audibleFiles.size());

        if(mPlayer != null && !mPlayer.isPlaying()){
            requestAudioFocus();
            mPlayer.start();
            this.isPlaying = true;
            this.isPaused = false;
            updateCurrentPosition();
        }
    }

    public void pause(){
        Log.i(TAG, currentFileIndex + ":" + audibleFiles.size());
        playCnt = 0;

        if(mPlayer != null && this.isPlaying) {
            mPlayer.pause();
            this.isPlaying = false;
            this.isPaused = true;
            abandonAudioFocus();
            stopUpdateCurrentPosition();
        }
    }

    public void stopPlaying() {
        playCnt = 0;
        abandonAudioFocus();
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            this.isPlaying = false;
            this.isPaused = false;
        }
        stopUpdateCurrentPosition();
    }

    public boolean isPaused(){
        return this.isPaused;
    }

    // Possibly returning NULL
    public File getCurrentFile() {
        if (audibleFiles != null && !audibleFiles.isEmpty() && currentFileIndex >= 0 && currentFileIndex < audibleFiles.size()) {
            return audibleFiles.get(this.currentFileIndex);
        }
        return null;
    }

    public String getCurrentRecordingFileName() {
        return this.currentRecordingFileName;
    }

    public String getCurrentFileName(){
        if (isRecording && currentRecordingFileName != null) {
            return currentRecordingFileName;
        }
        if(audibleFiles.size() > 0) {
            if (this.currentFileIndex >= 0 && this.currentFileIndex < audibleFiles.size()) {
                return audibleFiles.get(this.currentFileIndex).getName();
            } else {
                return audibleFiles.get(0).getName();
            }
        } else {
            return mainActivity.getResources().getString(R.string.NO_AUDIBLE_FILE_EXIST);
        }
    }

    public String getFullInformation(String title, String trackNumber, String album, String artist){
        return getFullInformation(title, trackNumber, album, artist, null, null);
    }

    public String getFullInformation(String title, String trackNumber, String album, String artist, String placeName){
        return getFullInformation(title, trackNumber, album, artist, placeName, null);
    }

    public String getFullInformation(String title, String trackNumber, String album, String artist, String placeName, AudioMetadataHelper.AudioMetadata meta){
        String fileInfo = "";
        try {
            if (meta == null && audibleFiles != null && currentFileIndex >= 0 && currentFileIndex < audibleFiles.size()) {
                meta = AudioMetadataHelper.loadMetadata(mainActivity, audibleFiles.get(currentFileIndex));
            }

            String effectivePlace = placeName;
            if ((effectivePlace == null || effectivePlace.isEmpty()) && meta != null && meta.locationName != null && !meta.locationName.isEmpty()) {
                effectivePlace = meta.locationName;
            }

            StringBuilder sb = new StringBuilder();

            if (trackNumber == null || title == null || album == null || artist == null) {
                if (meta != null && meta.isFavorite) {
                    sb.append("★ Favorite Recording\n");
                }
                sb.append("File name: ").append(getCurrentFileName());

                if (meta != null && meta.timeOfDay != null && !meta.timeOfDay.isEmpty()) {
                    sb.append("\nRecorded: ").append(meta.timeOfDay);
                }

                if (effectivePlace != null && !effectivePlace.isEmpty()) {
                    sb.append("\nLocation: ").append(effectivePlace);
                }

                if (meta != null && meta.audioSource != null && !meta.audioSource.isEmpty()) {
                    sb.append("\nMic: ").append(meta.audioSource);
                }

                int ch = (meta != null && meta.channels > 0) ? meta.channels
                        : (audibleFiles != null && currentFileIndex >= 0 && currentFileIndex < audibleFiles.size()
                           ? AudioMetadataHelper.getAudioFileChannels(audibleFiles.get(currentFileIndex)) : 1);
                sb.append("\nAudio: ").append(ch == 2 ? "Stereo (2 ch)" : "Mono (1 ch)");

                sb.append("\nDuration: ").append(getDurationBreakdown(audioFileDuration, this.DURATION_LONG_FORM));

                if (meta != null && meta.bookmarks != null && !meta.bookmarks.isEmpty()) {
                    int count = meta.bookmarks.size();
                    sb.append("\nBookmarks: ").append(count).append(count == 1 ? " mark" : " marks");
                }
            } else {
                if (meta != null && meta.isFavorite) {
                    sb.append("★ Favorite Track\n");
                }
                sb.append("Title: ").append(title)
                        .append("\nThis song is the ").append(getOrderNumber(trackNumber))
                        .append(" track in the album of \"").append(album).append("\" by ").append(artist)
                        .append("\nDuration: ").append(getDurationBreakdown(audioFileDuration, this.DURATION_LONG_FORM))
                        .append("\nFile name: \"").append(getCurrentFileName()).append("\"");

                int ch = (meta != null && meta.channels > 0) ? meta.channels
                        : (audibleFiles != null && currentFileIndex >= 0 && currentFileIndex < audibleFiles.size()
                           ? AudioMetadataHelper.getAudioFileChannels(audibleFiles.get(currentFileIndex)) : 1);
                sb.append("\nAudio: ").append(ch == 2 ? "Stereo (2 ch)" : "Mono (1 ch)");

                if (effectivePlace != null && !effectivePlace.isEmpty()) {
                    sb.append("\nLocation: ").append(effectivePlace);
                }

                if (meta != null && meta.bookmarks != null && !meta.bookmarks.isEmpty()) {
                    int count = meta.bookmarks.size();
                    sb.append("\nBookmarks: ").append(count).append(count == 1 ? " mark" : " marks");
                }

                if (meta != null && meta.timeOfDay != null && !meta.timeOfDay.isEmpty()) {
                    sb.append("\nRecorded: ").append(meta.timeOfDay);
                }
            }
            fileInfo = sb.toString();
        } catch(Exception e) {
            e.printStackTrace();
            Log.i(TAG, e.toString());
            fileInfo = "File name: " + getCurrentFileName() + "\nDuration: Error";
        }
        return fileInfo;
    }


    /**
     * Convert a millisecond duration to a string format
     *
     * @param millis A duration to convert to a string form
     * @param mode SHORT and LONG form
     * @return A string of the form "X Days Y Hours Z Minutes A Seconds".
     */
    public String getDurationBreakdown(long millis, int mode)
    {
//        if(millis < 1000)
//        {
//            throw new IllegalArgumentException("Duration must be greater than zero --> " + millis);
//        }

        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        StringBuilder sb = new StringBuilder(64);
        if(days > 0) {
            sb.append(days);
            if(mode == DURATION_LONG_FORM)
                sb.append(" days ");
            else
                sb.append(":");
        }
        if(hours > 0) {
            sb.append(hours);
            if(mode == DURATION_LONG_FORM)
                sb.append(" hours ");
            else
                sb.append(":");
        }
        if(minutes > 0) {
            sb.append(minutes);
            if(mode == DURATION_LONG_FORM)
                sb.append(" minutes ");
            else
                sb.append(":");
        }
        sb.append(seconds);
        if(mode == DURATION_LONG_FORM)
            sb.append(" seconds");

        return(sb.toString());
    }

    public String getOrderNumber(String n){
        if(n == null)
            return "";
        int num = 0;
        try{
            num = Integer.parseInt(n.trim());
            //Log.i(TAG, "intNumber:" + num + ":" + num%10);
            if(num == 1 || (num > 20 && num%10 == 1))
                return num+"st";
            else if(num == 2 || (num > 20 && num%10 == 2))
                return num+"nd";
            else if(num == 3 || (num > 20 && num%10 == 3))
                return num+"rd";
            else
                return num+"th";
        }catch(Exception e){
            Log.i(TAG, e.toString());
            return n+"th";
        }
    }

    /**
     * TODO When card UI is completed, this Item instance will be fw'ed to card UI component.
     * @return
     */
    public String getCurrentFileDisplayInformation(){
        if (isRecording) {
            String name = (currentRecordingFileName != null) ? currentRecordingFileName : "New Recording";
            return "● RECORDING: " + name;
        }

        String fileInfo = "";
        String trackNumber = "";
        String title = "";
        String album = "";
        String artist = "";

        try {

//            if (this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
//                MusicRetriever.Item item = musicRetriever.getCurrentItem();
//                return getFullInformation(item.getTitle(), item.getTrackNumber(), item.getAlbum(), item.getArtist());
//            }

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
//            String albumName =
//                    mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

            if (audibleFiles.size() > 0) {
                File current;
                current = audibleFiles.get(this.currentFileIndex);
                mmr.setDataSource(current.getAbsolutePath());
                trackNumber = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
                title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String locationMeta = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LOCATION);
                String placeName = null;
                if (locationMeta != null && !locationMeta.isEmpty()) {
                    double[] coords = LocationHelper.parseIso6709Location(locationMeta);
                    if (coords != null) {
                        placeName = LocationHelper.getPlaceName(mainActivity, coords[0], coords[1]);
                    }
                }
                AudioMetadataHelper.AudioMetadata meta = AudioMetadataHelper.loadMetadata(mainActivity, current);
                if (placeName == null && meta.locationName != null && !meta.locationName.isEmpty()) {
                    placeName = meta.locationName;
                }
                fileInfo = getFullInformation(title, trackNumber, album, artist, placeName, meta);
                return fileInfo;
            } else {
                return mainActivity.getResources().getString(R.string.NO_AUDIBLE_FILE_EXIST);
            }
        }catch(Exception e){
            e.printStackTrace();
            return "Had a problem to read more information of audio file: " + e.getMessage();
        }
    }

    /**
     * This does not play audio, just set the currentFileIndex to the next one.
     */
    public boolean nextSong(){
        if(this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
            if(musicRetriever.getSongCount() > 0)
                musicRetriever.next();
            return true;
        }

//        for(int i = 0;i < audibleFiles.size() ; i++)
//            Log.i(TAG,">>>" + audibleFiles.get(i).getAbsolutePath());

        if (audibleFiles.size() == 0)
            return false;

        //If it's the end of files, it moves to the first one.
        //Turn around!
        if (currentFileIndex == audibleFiles.size() - 1)
            currentFileIndex = 0;
        else
            currentFileIndex = currentFileIndex+1;

        return true;
    }


    public boolean previousSong(){
        if(this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
            if(musicRetriever.getSongCount() > 0)
                musicRetriever.previous();
            return true;
        }
        //Log.i(TAG + ">>>1 previousSong", currentFileIndex + ":" + audibleFiles.size());
        if (audibleFiles.size() == 0)
            return false;

        // Turn-around when it's at the first of files.
        if (currentFileIndex == 0)
            currentFileIndex = audibleFiles.size() - 1;
        else
            currentFileIndex = currentFileIndex - 1;


        return true;
    }

    /**
     * Recursively reading folders with AudioFiles.
     * @param sFile
     * Same code exists in FileUtils, needs to be consolidated
     */
    private void readRecursiveDir(File sFile) {
        if (sFile == null || !sFile.exists() || !sFile.isDirectory()) {
            return;
        }

        // Avoid re-scanning directories already visited in this session
        if (!scannedDirPaths.add(sFile.getAbsolutePath())) {
            return;
        }

        File[] entries = sFile.listFiles();
        if (entries == null || entries.length == 0) {
            return;
        }

        int directAudioCnt = 0;
        List<File> childDirs = new ArrayList<>();

        for (File entry : entries) {
            if (entry.isDirectory()) {
                if (!entry.isHidden() && !entry.getName().startsWith(".")) {
                    childDirs.add(entry);
                }
            } else if (aFilter.accept(entry)) {
                directAudioCnt++;
            }
        }

        if (directAudioCnt > 0 && !hasDirectory(sFile)) {
            if (!isHomeWorkMode || sFile.getAbsolutePath().indexOf(preFixStr) != -1) {
                addDirectory(sFile);
                scannedFileCnt += directAudioCnt;
                this.scannedFolderCnt++;
                logValidFolders(sFile.getAbsolutePath(), directAudioCnt);
            }
        }

        for (File childDir : childDirs) {
            displayStatusThrottled("Scanning: " + scannedFolderCnt + " folders (" + childDir.getName() + ")");
            readRecursiveDir(childDir);
        }
    }

    private void readDirectoriesFromMediaScanner(){
        musicRetriever = new MusicRetriever(mainActivity.getContentResolver());
        musicRetriever.prepare();

        directories = new ArrayList<File>();

        //Adding all folders by MediaScanner

        for (int i = 0; musicRetriever.getSongCount() > i ; i++){
//            Log.i(TAG, "Folder list from MusicRetriever ===> "
//                    + musicRetriever.getFolderPathFromContentUri());
//
            directories.add(new File(musicRetriever.getFolderPathFromContentUri()));
            musicRetriever.next();
        }

        Log.i(TAG, "Size of directories.readDirectoriesFromMediaScanner ===> " + directories.size());

//        // Sorting
//        Collections.sort(directories, new Comparator<File>() {
//            @Override
//            public int compare (File f1, File f2)
//            {
//                //Log.i(TAG,"Sorting directories...");
//                return f1.getName().compareTo(f2.getName());
//            }
//
//        });
    }

    /**
     * Target folders
     * 1) folderForAllMusicByMediaScanner under userDataSpace
     * 2) Public Music folder
     * 3) All ExtSDcards
     */
    public void readDirectories() {
        if (directories == null) {
            directories = new ArrayList<File>();
        } else {
            directories.clear();
        }
        directoryPathSet.clear();
        scannedDirPaths.clear();

        // 1) User-configured custom folder from Settings
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String folderUri = sharedPreferences.getString("target_folders", null);

        if (folderUri != null) {
            try {
                Uri treeUri = Uri.parse(folderUri);
                if (treeUri != null && "content".equalsIgnoreCase(treeUri.getScheme())
                        && DocumentsContract.isTreeUri(treeUri)) {
                    String docId = DocumentsContract.getTreeDocumentId(treeUri);
                    File realFolder = null;
                    if (docId != null) {
                        if (docId.startsWith("primary:")) {
                            String sub = docId.substring("primary:".length());
                            realFolder = new File(Environment.getExternalStorageDirectory(), sub);
                        } else if (docId.contains(":")) {
                            String[] parts = docId.split(":", 2);
                            if (parts.length == 2) {
                                realFolder = new File("/storage/" + parts[0] + "/" + parts[1]);
                            }
                        }
                    }

                    if (realFolder != null && realFolder.exists()) {
                        readRecursiveDir(realFolder);
                    } else {
                        DocumentFile pickedDir = DocumentFile.fromTreeUri(mainActivity, treeUri);
                        if (pickedDir != null && pickedDir.exists()) {
                            for (DocumentFile file : pickedDir.listFiles()) {
                                if (file != null && file.isDirectory() && file.getName() != null && realFolder != null) {
                                    File sub = new File(realFolder, file.getName());
                                    if (sub.exists()) {
                                        readRecursiveDir(sub);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error resolving target_folders URI: " + folderUri, e);
            }
        }

        // 2) Scan all standard Android public audio directories (Music, Recordings, Podcasts, Downloads, SD)
        scanStandardPublicAudioFolders();

        // 3) Scan MediaStore for audio files across all mounted internal and external volumes
        try {
            ContentResolver contentResolver = mainActivity.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String[] projection = new String[]{ MediaStore.Audio.Media.DATA };
            Cursor cursor = contentResolver.query(uri, projection, null, null, null);

            if (cursor != null) {
                try {
                    int dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    while (cursor.moveToNext()) {
                        if (dataIdx != -1) {
                            String path = cursor.getString(dataIdx);
                            if (path != null) {
                                int lastSlash = path.lastIndexOf('/');
                                if (lastSlash > 0) {
                                    String parentPath = path.substring(0, lastSlash);
                                    if (!directoryPathSet.contains(parentPath)) {
                                        if (!isHomeWorkMode || parentPath.indexOf(preFixStr) != -1) {
                                            File parent = new File(parentPath);
                                            if (parent.exists() && parent.isDirectory()) {
                                                addDirectory(parent);
                                                scannedFolderCnt++;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore for audio folders", e);
        }

        // 4) Scan root app data directory
        if (rootFolder != null && rootFolder.exists()) {
            readRecursiveDir(rootFolder);
        }

        // 5) Always ensure the active daily recording folder is present
        if (recordTargetFolder != null && !hasDirectory(recordTargetFolder)) {
            addDirectory(recordTargetFolder);
        }

        // 6) Sort directories
        sortDirectories();
    }

    /**
     * Scans standard public audio storage locations to ensure all accessible user audio folders are indexed.
     */
    private void scanStandardPublicAudioFolders() {
        List<File> candidateFolders = new ArrayList<>();

        // Standard Music Directory & SJPlayer
        File musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
        if (musicDir != null && musicDir.exists()) {
            candidateFolders.add(musicDir);
            File sjPlayerFolder = new File(musicDir, "SJPlayer");
            if (sjPlayerFolder.exists()) {
                candidateFolders.add(sjPlayerFolder);
            }
        }

        // Standard Recordings Directory (API 31+ or fallback)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                File recDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS);
                if (recDir != null && recDir.exists() && !candidateFolders.contains(recDir)) {
                    candidateFolders.add(recDir);
                }
            }
        } catch (Throwable ignored) {}
        File legacyRecDir = new File(Environment.getExternalStorageDirectory(), "Recordings");
        if (legacyRecDir.exists() && !candidateFolders.contains(legacyRecDir)) {
            candidateFolders.add(legacyRecDir);
        }
        File voiceRecDir = new File(Environment.getExternalStorageDirectory(), "Voice Recorder");
        if (voiceRecDir.exists() && !candidateFolders.contains(voiceRecDir)) {
            candidateFolders.add(voiceRecDir);
        }

        // Standard Podcasts Directory
        File podcastsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PODCASTS);
        if (podcastsDir != null && podcastsDir.exists() && !candidateFolders.contains(podcastsDir)) {
            candidateFolders.add(podcastsDir);
        }

        // Standard Downloads Directory
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (downloadDir != null && downloadDir.exists() && !candidateFolders.contains(downloadDir)) {
            candidateFolders.add(downloadDir);
        }

        // External Storage / MicroSD Card directories
        try {
            File[] extDirs = ContextCompat.getExternalFilesDirs(mainActivity, null);
            if (extDirs != null) {
                for (File ext : extDirs) {
                    if (ext != null) {
                        String path = ext.getAbsolutePath();
                        int androidIdx = path.indexOf("/Android");
                        if (androidIdx > 0) {
                            String rootVolume = path.substring(0, androidIdx);
                            File sdMusic = new File(rootVolume, "Music");
                            if (sdMusic.exists() && !candidateFolders.contains(sdMusic)) {
                                candidateFolders.add(sdMusic);
                            }
                            File sdRec = new File(rootVolume, "Recordings");
                            if (sdRec.exists() && !candidateFolders.contains(sdRec)) {
                                candidateFolders.add(sdRec);
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}

        for (File folder : candidateFolders) {
            readRecursiveDir(folder);
        }
    }



    public void displayStatus(String msg){
        statusMsg = msg;
        mHandler.sendMessage(mHandler.obtainMessage());
    }

    public void displayToast(final String msg) {
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(mainActivity, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public int getMaxAmplitude() {
        if (mRecorder != null && isRecording) {
            try {
                return mRecorder.getMaxAmplitude();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public int getAudioSessionId() {
        if (mPlayer != null && isPlaying) {
            try {
                return mPlayer.getAudioSessionId();
            } catch (Exception e) {
                return -1;
            }
        }
        return -1;
    }

    public File getAlbumImage(){
        return this.albumImage;
    }

    public void lowBatteryWarning(){
        Intent batteryIntent = this.mainActivity.getApplicationContext().registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int rawlevel = batteryIntent.getIntExtra("level", -1);
        double scale = batteryIntent.getIntExtra("scale", -1);
        double level = -1;
        if (rawlevel >= 0 && scale > 0) {
            level = rawlevel / scale;
        }

        //When battery is lower than 30%, it keeps bugging SJ to connect his device to charger
        if(!((MainActivity)mainActivity).isPlugged(this.mainActivity) && level < 30) {
            ((MainActivity) mainActivity).speak(mainActivity.getResources().getString(R.string.LOW_BATTERY));
            status_TextView.setText("Battery level:" + level);
        }

    }

    /**
     * Move to the next folder
     * 1) list files 2) pick folders 3) get Array(folder[]) 4) set this.target = Array[selected] and initateFolder() 5) Same
     */

    public boolean nextFolder(){
        try {
            //Reset index to the first file when the folder changed
            this.currentFileIndex = 0;
            if (currentDirectoryIndex == this.directories.size() - 1)
                currentDirectoryIndex = 0;
            else
                this.currentDirectoryIndex = currentDirectoryIndex + 1;

            this.currentFolder = this.directories.get(this.currentDirectoryIndex);
            // Read folder to get Files when it's not a VirtualFolder
            if(!this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)){
                readAudibleFilesInCurrentFolder();
//                ((DialView) mainActivity.findViewById(R.id.dial_view)).setDirMode(DialView.DIR_MODE_4);
            } else {
//                ((DialView) mainActivity.findViewById(R.id.dial_view)).setDirMode(DialView.DIR_MODE_8);
            }

            // TODO This job can be done when directories get initiated.
            // Need to optimize this method
//            File[] images  = currentFolder.listFiles(new ImageFileFilter());
//            if( images.length > 0)
//                this.albumImage = images[0];
//            else
//                this.albumImage = null; //TODO set the default AlbumImage or something blank one.

//            Log.i(TAG, "currentDirectoryIndex:" + this.currentDirectoryIndex +
//                    ":Audible file size: " + audibleFiles.size()+ currentFolder.getAbsolutePath() +
//                    ": AlbumImage: " + albumImage.getAbsolutePath());

            return true;
        }catch(Exception e){
            return false;
        }

    }

    /**
     * Move to the previous folder
     */
    public boolean previousFolder(){

        try {
            //Reset index to the first file when the folder changed
            this.currentFileIndex = 0;
            if (currentDirectoryIndex == 0)
                currentDirectoryIndex = this.directories.size() - 1;
            else
                this.currentDirectoryIndex = currentDirectoryIndex - 1;

            this.currentFolder = this.directories.get(this.currentDirectoryIndex);

            // Read folder to get Files when it's not a VirtualFolder
            if(!this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)){
                readAudibleFilesInCurrentFolder();
            }

            Log.i(TAG, "currentDirectoryIndex:" + this.currentDirectoryIndex + ":Audible file size: " + audibleFiles.size()+ currentFolder.getAbsolutePath());
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    /**
     * Extracts folder grouping key:
     * - "YYYY-MM" if name starts with YYYY-MM-DD (or YYYY_MM_DD or YYYY.MM.DD)
     * - First alphabet character (uppercase) for general folders
     */
    public static String getFolderGroupingKey(String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) {
            return "#";
        }
        String name = folderName.trim();
        // Check date pattern: YYYY-MM-DD or YYYY_MM_DD or YYYY.MM.DD
        if (name.length() >= 7) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d{4}[-_\\.]\\d{2})([-_\\.]\\d{2})?").matcher(name);
            if (m.find()) {
                return m.group(1).replace('_', '-').replace('.', '-');
            }
        }
        // Non-date: Find first alphanumeric character
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                return String.valueOf(Character.toUpperCase(ch));
            }
        }
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    /**
     * Extracts file grouping key:
     * - If file name starts with date-time format (e.g. YYYY-MM-DD-HHmmss or YYYY-MM-DD_HHmmss),
     *   group by hour e.g. "HOUR_HH"
     * - If file name starts with date format: "YYYY-MM-DD"
     * - Otherwise alphabet level: first alphanumeric character uppercase (e.g. "A", "B", "1")
     */
    public static String getFileGroupingKey(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "#";
        }
        String name = fileName.trim();
        java.util.regex.Matcher mDateTime = java.util.regex.Pattern.compile("^\\d{4}[-_\\.]\\d{2}[-_\\.]\\d{2}[-_\\.](\\d{2})").matcher(name);
        if (mDateTime.find()) {
            return "HOUR_" + mDateTime.group(1);
        }
        java.util.regex.Matcher mDate = java.util.regex.Pattern.compile("^(\\d{4}[-_\\.]\\d{2}[-_\\.]\\d{2})").matcher(name);
        if (mDate.find()) {
            return mDate.group(1).replace('_', '-').replace('.', '-');
        }
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (Character.isLetterOrDigit(ch)) {
                return String.valueOf(Character.toUpperCase(ch));
            }
        }
        return String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    /**
     * Fast seek to next/previous folder group.
     * If folders have YYYY-MM-DD format, moves by YYYY-MM month basis.
     * Otherwise moves by alphabet level (first letter).
     * @param direction: +1 for next group, -1 for previous group
     * @return true if folder was changed
     */
    public boolean fastSeekFolder(int direction) {
        if (directories == null || directories.size() <= 1) {
            return false;
        }
        int total = directories.size();
        String currentKey = getFolderGroupingKey(getCurrentDirectoryName());
        int targetIndex = -1;

        if (direction > 0) {
            // Find next group
            for (int step = 1; step < total; step++) {
                int idx = (currentDirectoryIndex + step) % total;
                String key = getFolderGroupingKey(directories.get(idx).getName());
                if (!key.equalsIgnoreCase(currentKey)) {
                    targetIndex = idx;
                    break;
                }
            }
        } else {
            // Find previous group and jump to the START of that group
            String prevGroupKey = null;
            int foundIdx = -1;
            for (int step = 1; step < total; step++) {
                int idx = (currentDirectoryIndex - step + total) % total;
                String key = getFolderGroupingKey(directories.get(idx).getName());
                if (!key.equalsIgnoreCase(currentKey)) {
                    prevGroupKey = key;
                    foundIdx = idx;
                    break;
                }
            }
            if (prevGroupKey != null) {
                targetIndex = foundIdx;
                int check = (targetIndex - 1 + total) % total;
                int count = 0;
                while (count < total && getFolderGroupingKey(directories.get(check).getName()).equalsIgnoreCase(prevGroupKey)) {
                    targetIndex = check;
                    check = (targetIndex - 1 + total) % total;
                    count++;
                }
            }
        }

        if (targetIndex != -1 && targetIndex != currentDirectoryIndex) {
            try {
                this.currentFileIndex = 0;
                this.currentDirectoryIndex = targetIndex;
                this.currentFolder = this.directories.get(this.currentDirectoryIndex);
                if (!this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
                    readAudibleFilesInCurrentFolder();
                }
                Log.i(TAG, "fastSeekFolder: moved to " + targetIndex + ": " + getCurrentDirectoryName());
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Error in fastSeekFolder", e);
                return false;
            }
        }
        return false;
    }

    /**
     * Fast seek to next/previous file group within current folder.
     * Moves by alphabet level (or hour if timestamped recordings).
     * @param direction: +1 for next group, -1 for previous group
     * @return true if file was changed
     */
    public boolean fastSeekFile(int direction) {
        if (audibleFiles == null || audibleFiles.size() <= 1) {
            return false;
        }
        int total = audibleFiles.size();
        String currentKey = getFileGroupingKey(getCurrentFileName());
        int targetIndex = -1;

        if (direction > 0) {
            // Find next group
            for (int step = 1; step < total; step++) {
                int idx = (currentFileIndex + step) % total;
                String key = getFileGroupingKey(audibleFiles.get(idx).getName());
                if (!key.equalsIgnoreCase(currentKey)) {
                    targetIndex = idx;
                    break;
                }
            }
        } else {
            // Find previous group and jump to the START of that group
            String prevGroupKey = null;
            int foundIdx = -1;
            for (int step = 1; step < total; step++) {
                int idx = (currentFileIndex - step + total) % total;
                String key = getFileGroupingKey(audibleFiles.get(idx).getName());
                if (!key.equalsIgnoreCase(currentKey)) {
                    prevGroupKey = key;
                    foundIdx = idx;
                    break;
                }
            }
            if (prevGroupKey != null) {
                targetIndex = foundIdx;
                int check = (targetIndex - 1 + total) % total;
                int count = 0;
                while (count < total && getFileGroupingKey(audibleFiles.get(check).getName()).equalsIgnoreCase(prevGroupKey)) {
                    targetIndex = check;
                    check = (targetIndex - 1 + total) % total;
                    count++;
                }
            }
        }

        if (targetIndex != -1 && targetIndex != currentFileIndex) {
            this.currentFileIndex = targetIndex;
            Log.i(TAG, "fastSeekFile: moved to " + targetIndex + ": " + getCurrentFileName());
            return true;
        }
        return false;
    }

    /**
     * 1) Read only audible files(AudibleFileFilter) from current directory
     * 2) Filter out un-audible files
     * 3) Always set current Index to 0
     * 4) This method is called when -nextFolder, previousFolder
     */
    public void readAudibleFilesInCurrentFolder(){
        try {
            File[] files = (this.currentFolder != null) ? this.currentFolder.listFiles(aFilter) : null;
            
            if (files != null && files.length > 0) {
                this.audibleFiles = new ArrayList<>(Arrays.asList(files));
            } else {
                this.audibleFiles = new ArrayList<>();
                // Fallback: Query MediaStore for this folder on Android 10+ or when File.listFiles returns empty
                if (this.currentFolder != null) {
                    List<File> mediaStoreFiles = queryAudibleFilesFromMediaStore(this.currentFolder);
                    if (mediaStoreFiles != null && !mediaStoreFiles.isEmpty()) {
                        this.audibleFiles.addAll(mediaStoreFiles);
                    }
                }
            }

            Collections.sort(audibleFiles, new Comparator<File>() {
                @Override
                public int compare (File f1, File f2)
                {
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });

        }catch(Exception e){
            e.printStackTrace();
            audibleFiles = new ArrayList<File>();
        }
        this.currentFileIndex = 0;
    }

    /**
     * Fallback query against MediaStore to retrieve audio files in a specific folder
     * when Scoped Storage blocks java.io.File.listFiles() or returns empty.
     */
    private List<File> queryAudibleFilesFromMediaStore(File folder) {
        List<File> results = new ArrayList<>();
        if (folder == null || mainActivity == null) return results;

        try {
            ContentResolver resolver = mainActivity.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String folderPath = folder.getAbsolutePath();
            if (!folderPath.endsWith("/")) {
                folderPath = folderPath + "/";
            }

            String[] projection = new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.SIZE
            };

            String selection = MediaStore.Audio.Media.DATA + " LIKE ?";
            String[] selectionArgs = new String[]{ folderPath + "%" };

            Cursor cursor = resolver.query(uri, projection, selection, selectionArgs, null);
            if (cursor != null) {
                try {
                    int dataIdx = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                    int sizeIdx = cursor.getColumnIndex(MediaStore.Audio.Media.SIZE);

                    while (cursor.moveToNext()) {
                        String filePath = (dataIdx != -1) ? cursor.getString(dataIdx) : null;
                        long size = (sizeIdx != -1) ? cursor.getLong(sizeIdx) : 0;

                        if (filePath != null && size >= 1000) {
                            File f = new File(filePath);
                            File parent = f.getParentFile();
                            if (parent != null && parent.getAbsolutePath().equals(folder.getAbsolutePath())) {
                                if (aFilter == null || aFilter.accept(f)) {
                                    results.add(f);
                                }
                            }
                        }
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error querying MediaStore files for folder: " + folder.getAbsolutePath(), e);
        }
        return results;
    }

    /**
     * Resolves a content:// URI from MediaStore for playback if direct file path access fails under Scoped Storage.
     */
    public Uri getUriForFilePath(String path) {
        if (path == null || mainActivity == null) return null;
        try {
            ContentResolver resolver = mainActivity.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            Cursor cursor = resolver.query(uri, new String[]{ MediaStore.Audio.Media._ID },
                    MediaStore.Audio.Media.DATA + "=?", new String[]{ path }, null);
            if (cursor != null) {
                try {
                    if (cursor.moveToFirst()) {
                        long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));
                        return ContentUris.withAppendedId(uri, id);
                    }
                } finally {
                    cursor.close();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }


    /**
     * TODO Find out what is the best optimal audio configuration
     */
    public void startRecording() {

        // 1) Create a folder when date is changed
        // 2) Add new targetFolder to directories
        // 3) Sort again
        if (!recordTargetFolder.getName().equalsIgnoreCase(getRecordingFolderName())) {
            initiateTargetFolder();
        }

        if (!hasDirectory(recordTargetFolder)) {
            addDirectory(recordTargetFolder);
            sortDirectories();
        }

        // Navigate immediately to the target recording folder!
        currentFolder = recordTargetFolder;
        currentDirectoryIndex = directories.indexOf(recordTargetFolder);
        readAudibleFilesInCurrentFolder();
        currentFileIndex = Math.max(0, audibleFiles.size() - 1);

        try {
            // Initiate MediaRecorder (using Context-aware constructor on API 31+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainActivity != null) {
                mRecorder = new MediaRecorder(mainActivity);
            } else {
                mRecorder = new MediaRecorder();
            }

            // Read audio quality, stereo, and noise suppression preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            String qualityPref = prefs.getString("pref_audio_quality", "high");
            boolean useNoiseSuppression = prefs.getBoolean("pref_noise_suppression", true);
            boolean stereoPref = prefs.getBoolean("pref_stereo_recording", true);

            boolean isDeviceStereoSupported = AudioMetadataHelper.isStereoRecordingSupported(mainActivity);
            boolean recordStereo = stereoPref && isDeviceStereoSupported && !"saver".equalsIgnoreCase(qualityPref);

            // Configure audio source:
            // When recording in Stereo, use MediaRecorder.AudioSource.MIC so hardware top/bottom mics separate into L/R.
            // (VOICE_RECOGNITION collapses multi-mic stereo into a mono beamformed speech signal).
            int audioSource;
            if (recordStereo) {
                audioSource = MediaRecorder.AudioSource.MIC;
            } else {
                audioSource = useNoiseSuppression ? MediaRecorder.AudioSource.VOICE_RECOGNITION : MediaRecorder.AudioSource.MIC;
            }
            try {
                mRecorder.setAudioSource(audioSource);
            } catch (Exception e) {
                Log.w(TAG, "AudioSource " + audioSource + " failed, falling back to MIC", e);
                mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }

            // Output container: MPEG-4
            mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);

            // Configure codec, sample rate, bit rate, and channel count based on profile and stereo capability
            int audioEncoder = MediaRecorder.AudioEncoder.AAC;
            int samplingRate = 48000;
            int bitRate = 128000;
            int channels = recordStereo ? 2 : 1;

            if ("saver".equalsIgnoreCase(qualityPref)) {
                // Storage Saver: HE-AAC 32 kbps, 44.1 kHz, Mono (~14 MB/hr)
                audioEncoder = MediaRecorder.AudioEncoder.HE_AAC;
                samplingRate = 44100;
                bitRate = 32000;
                channels = 1;
            } else if ("studio".equalsIgnoreCase(qualityPref)) {
                // Studio Quality: AAC 256 kbps (Stereo) or 128 kbps (Mono), 48 kHz
                audioEncoder = MediaRecorder.AudioEncoder.AAC;
                samplingRate = 48000;
                bitRate = channels == 2 ? 256000 : 128000;
            } else {
                // High Quality (Default): AAC 128 kbps (Stereo) or 64 kbps (Mono), 48 kHz
                audioEncoder = MediaRecorder.AudioEncoder.AAC;
                samplingRate = 48000;
                bitRate = channels == 2 ? 128000 : 64000;
            }

            try {
                mRecorder.setAudioEncoder(audioEncoder);
            } catch (Exception e) {
                Log.w(TAG, "Encoder " + audioEncoder + " failed, falling back to standard AAC", e);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            }

            mRecorder.setAudioChannels(channels);
            mRecorder.setAudioSamplingRate(samplingRate);
            mRecorder.setAudioEncodingBitRate(bitRate);
            this.recordingChannels = channels;

            String fileName = this.createFileName();
            currentRecordingFileName = fileName + ".m4a";

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DISPLAY_NAME, currentRecordingFileName);
                values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4");
                // Match the recordTargetFolder structure: Music/SJPlayer/yyyy-MM-dd
                values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/SJPlayer/" + this.getRecordingFolderName());
                values.put(MediaStore.Audio.Media.IS_PENDING, 1);

                ContentResolver resolver = mainActivity.getContentResolver();
                currentRecordingUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);

                if (currentRecordingUri != null) {
                    mRecorder.setOutputFile(resolver.openFileDescriptor(currentRecordingUri, "w").getFileDescriptor());
                }
            } else {
                newFileName = recordTargetFolder.getAbsolutePath();
                newFileName += "/" + currentRecordingFileName;
                mRecorder.setOutputFile(newFileName);
            }

            recordStartTimeMillis = System.currentTimeMillis();
            recordingBookmarks.clear();
            recordingAudioSource = AudioMetadataHelper.getCurrentAudioInputSource(mainActivity);
            recordingLocationName = null;

            // Tag recording with location if enabled in settings and permission is granted
            boolean saveLocation = prefs.getBoolean("pref_save_location", true);
            if (saveLocation && LocationHelper.isLocationPermissionGranted(mainActivity)) {
                android.location.Location loc = LocationHelper.getLastKnownLocation(mainActivity);
                if (loc != null) {
                    try {
                        mRecorder.setLocation((float) loc.getLatitude(), (float) loc.getLongitude());
                        recordingLocationName = LocationHelper.getPlaceName(mainActivity, loc.getLatitude(), loc.getLongitude());
                        Log.i(TAG, "Recording geotagged with: " + loc.getLatitude() + ", " + loc.getLongitude() + " (" + recordingLocationName + ")");
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to embed location in recording: " + e.getMessage());
                    }
                }
            }

            try {
                mRecorder.prepare();
                mRecorder.start();
                this.isRecording = true;
                Log.i(TAG, "Recording started (" + (channels == 2 ? "Stereo 2ch" : "Mono 1ch") + ", " + bitRate / 1000 + " kbps, " + samplingRate + " Hz)");
            } catch (Exception e) {
                if (channels == 2) {
                    Log.w(TAG, "Stereo recording failed on prepare/start, retrying with Mono fallback: " + e.getMessage());
                    try {
                        mRecorder.reset();
                        mRecorder.release();
                    } catch (Exception ignored) {}

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && mainActivity != null) {
                        mRecorder = new MediaRecorder(mainActivity);
                    } else {
                        mRecorder = new MediaRecorder();
                    }
                    mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mRecorder.setAudioChannels(1);
                    mRecorder.setAudioSamplingRate(44100);
                    mRecorder.setAudioEncodingBitRate(64000);
                    this.recordingChannels = 1;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && currentRecordingUri != null) {
                        ContentResolver resolver = mainActivity.getContentResolver();
                        mRecorder.setOutputFile(resolver.openFileDescriptor(currentRecordingUri, "w").getFileDescriptor());
                    } else if (newFileName != null) {
                        mRecorder.setOutputFile(newFileName);
                    }
                    mRecorder.prepare();
                    mRecorder.start();
                    this.isRecording = true;
                    Log.i(TAG, "Recording successfully started with Mono fallback");
                } else {
                    throw e;
                }
            }

            startTimer();

        } catch (Exception e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
            this.isRecording = false;
        }

        Log.i(TAG, "End of starting record...");

    }

    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {
            //@TODO should be corrected
            int day = (int) mil/(3600*24);
            int hour = (int) mil/3600;
            int minutes = (int) mil/60;
            int seconds = (int) mil%60;
            if(!isPaused)
                mil = mil + 0.5;

            if(hour > 24){
                status_TextView.setText(String.format("%.1f days", day));
            }else {
                status_TextView.setText(String.format("%02d:%02d:%02d", hour, minutes, seconds));
            }
            timerHandler.postDelayed(this, 500);
        }

    };


    int currentPos = 0;
        Runnable playerCurrentPositionRunnable = new Runnable() {
        @Override
        public void run() {
            currentPos =  mPlayer.getCurrentPosition();
            int progressPercent = (int) (Math.round((double)currentPos*100/audioFileDuration));
//            Log.i(TAG, "progressPercent===>" + (double)currentPos*100/audioFileDuration + ":" +
//                    + progressPercent+"%:"+currentPos+":"+audioFileDuration);


            currentPos = currentPos/1000;
//            int hour = (int) currentPos/3600;
//            int minutes = (int) currentPos/60;
//            int seconds = (int) currentPos%60;
            status_TextView.setText(
                    String.format("%02d:%02d:%02d", (int) currentPos/3600, (int) currentPos/60, (int) currentPos%60) +"/"+
                            String.format("%02d:%02d:%02d", (int) (audioFileDuration/1000)/3600, (int) (audioFileDuration/1000)/60, (int) (audioFileDuration/1000)%60));
            timerHandler.postDelayed(this, 100);
        }

    };

    /**
     * Current code is not returning Removable SD card, instead, it returns the biggest Free space one.
     * @return File
     */
    public File getBiggerExtSDCardDirectory()
    {
        File dir[] = ContextCompat.getExternalFilesDirs((Context) this.mainActivity,
            null);

//
//        Log.i(TAG, "getExternalStoragePublicDirectory:"
//                + Environment.getExternalStoragePublicDirectory(Environment.MEDIA_SHARED).getAbsolutePath()
//        + ":" + Environment.isExternalStorageEmulated());
//        Log.i(TAG, "getDataDirectory:"+ Environment.getDataDirectory().getAbsolutePath());


        File targetFile = null;
        //Pick the largest free space
        long freeSize = 0;

        /**
         * Interestingly dir[] has null even without removable sdcard. This may be an issue of samsung phone.
         * TODO Test with another device and report Samsugn.
         * Tested in HTC M8 and same result...
         *
         * And picking up the larger storage to save recording files
         */

        for(int i = 0; i < dir.length && dir[i] != null ; i++ ){
            Log.i(TAG, "Inside of Loop getExternalSDCardDirectory():"+ dir[i].getAbsolutePath()
                    + " Total: " + dir[i].getTotalSpace() + " Free:" + dir[i].getFreeSpace());

            /**
             * If the total size bigger, then pick it as the primary storage
             * Below code will take one that has bigger 'Free' space
             */
//            if (dir[i].getFreeSpace() > freeSize) {
//                targetFile = dir[i];
//                freeSize = dir[i].getFreeSpace();
//            }
            if (dir[i].getTotalSpace() > freeSize) {
                targetFile = dir[i];
                freeSize = dir[i].getTotalSpace();
            }
        }

        if (targetFile != null) {
            Log.i(this.TAG, "targetFile:" + targetFile.getAbsolutePath().toString() + ":" + targetFile.getFreeSpace());
        }
        return   targetFile;
    }

    /**
     * TODO Don't know what else needs to be added more
     */
    public void stopRecording() {
        try {
//            Log.i(TAG, Boolean.toString(this.isRecording) + ":" + Boolean.toString(this.isPlaying));

            mil = 0;
            mRecorder.stop();
            mRecorder.reset();
            mRecorder.release();
            mRecorder = null;
            isRecording = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && currentRecordingUri != null) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.IS_PENDING, 0);
                mainActivity.getContentResolver().update(currentRecordingUri, values, null, null);
                currentRecordingUri = null;
            }

            // change current Folder and File index to the just saved file.
            moveToLatestRecordedLocation();
            if (audibleFiles != null && audibleFiles.size() > 0 && currentFileIndex >= 0 && currentFileIndex < audibleFiles.size()) {
                File savedFile = audibleFiles.get(currentFileIndex);
                AudioMetadataHelper.AudioMetadata meta = new AudioMetadataHelper.AudioMetadata();
                meta.fileName = savedFile.getName();
                meta.recordedAt = recordStartTimeMillis > 0 ? recordStartTimeMillis : System.currentTimeMillis();
                meta.timeOfDay = AudioMetadataHelper.getTimeOfDayContext(meta.recordedAt);
                meta.audioSource = recordingAudioSource;
                meta.locationName = recordingLocationName;
                meta.channels = recordingChannels;
                meta.channelMode = recordingChannels == 2 ? "Stereo" : "Mono";
                meta.bookmarks = new ArrayList<>(recordingBookmarks);
                AudioMetadataHelper.saveMetadata(mainActivity, savedFile, meta);
                Log.i(TAG, "Saved companion metadata for " + savedFile.getName() + " (" + meta.channelMode + ") with " + meta.bookmarks.size() + " bookmarks");
            }
            //currentFolder = recordTargetFolder;
            justRecordFinished = true;
        } catch (Exception e){ //No idea what kind of Exception is coming...
            e.printStackTrace();
            justRecordFinished = false;
        } finally {
            this.isRecording = false;
            stopTimer();
        }

    }

    Thread playerCurrentPositionRunnableThread = null;

    public void updateCurrentPosition(){
        if(playerCurrentPositionRunnableThread == null){
            playerCurrentPositionRunnableThread = new Thread(playerCurrentPositionRunnable);
        }

        playerCurrentPositionRunnableThread.run();
    }

    public void stopUpdateCurrentPosition(){
        timerHandler.removeCallbacks(playerCurrentPositionRunnable);
        if(playerCurrentPositionRunnableThread != null) {
            playerCurrentPositionRunnableThread.interrupt();
        }
    }


    public void startTimer(){
        //TODO Timer of Recorder
        if(timerThread == null) {
            timerThread = new Thread(timerRunnable);
        }

        timerThread.run();
    }

    public void stopTimer(){
        mil = 0;
        timerHandler.removeCallbacks(timerRunnable);
        if(timerThread != null) {
            timerThread.interrupt();
        }
    }

    /**
     * change current Folder and File index to the just saved file.
     */
    public void moveToLatestRecordedLocation(){
       // TargetFolder is already created and existing in the Directory index
        // because it's always checked when Recorder is instant'ed.

        if (!hasDirectory(recordTargetFolder)) {
            addDirectory(recordTargetFolder);
            sortDirectories();
        }

        currentFolder = recordTargetFolder;
        currentDirectoryIndex = directories.indexOf(recordTargetFolder);
        
        // Refresh the file list for the current folder
        this.readAudibleFilesInCurrentFolder();

        int n = audibleFiles.size();
        if (n == 0) {
            Log.w(TAG, "moveToLatestRecordedLocation: No audible files found in " + currentFolder.getAbsolutePath());
            return;
        }

        for(int i = 1; n >= i ; i++){
            if( currentRecordingFileName != null && currentRecordingFileName.equals(audibleFiles.get(n-i).getName())){
                this.currentFileIndex = n-i;
                Log.i(TAG, "FOUND recorded file at index ===>" + currentFileIndex + " in " + currentFolder.getName());
                break;
            } else {
                // Default to the last file if specific match fails
                currentFileIndex = n - 1;
            }
        }
    }

    /**
     * Checks if external storage is available for read and write
     * Not being used now. 
     */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /**
     * Return current folder name to let user know where it is.
     */
    public String getCurrentDirectoryName(){
        if (this.currentFolder == null)
            return "";
        if (publicMusicFolder != null && this.currentFolder.equals(publicMusicFolder))
            return "Public Music";
        return this.currentFolder.getName();
    }

    public int getCurrentDirectoryIndex() {
        return this.currentDirectoryIndex;
    }

    public int getTotalDirectoriesCount() {
        return (this.directories != null) ? this.directories.size() : 0;
    }

    public String getPreviousDirectoryName() {
        if (this.directories == null || this.directories.isEmpty()) return "";
        int size = this.directories.size();
        int prevIndex = (this.currentDirectoryIndex - 1 + size) % size;
        File folder = this.directories.get(prevIndex);
        if (publicMusicFolder != null && folder.equals(publicMusicFolder))
            return "Public Music";
        return (folder != null) ? folder.getName() : "";
    }

    public String getNextDirectoryName() {
        if (this.directories == null || this.directories.isEmpty()) return "";
        int size = this.directories.size();
        int nextIndex = (this.currentDirectoryIndex + 1) % size;
        File folder = this.directories.get(nextIndex);
        if (publicMusicFolder != null && folder.equals(publicMusicFolder))
            return "Public Music";
        return (folder != null) ? folder.getName() : "";
    }

    public String getDirectoryNameWithOffset(int offset) {
        if (this.directories == null || this.directories.size() <= 1) return "";
        int size = this.directories.size();
        if (size <= 2) {
            if (Math.abs(offset) > 1) return "";
        }
        int targetIndex = (this.currentDirectoryIndex + offset) % size;
        if (targetIndex < 0) targetIndex += size;
        try {
            if (targetIndex >= 0 && targetIndex < this.directories.size()) {
                File folder = this.directories.get(targetIndex);
                if (publicMusicFolder != null && folder.equals(publicMusicFolder))
                    return "Public Music";
                return (folder != null) ? folder.getName() : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    /**
     * Return current folder name to let user know where it is.
     */
    public String getCurrentDirectoryInformation(){

        String info;


        int index = -1;
        //DEPRECATED, No more use of MediaScanner
//        if(this.currentFolder.equals(folderForAllMusicByMediaScanner)) {
//            info = "Folder, \"" + getCurrentDirectoryName() + "\" \n\nhas ";
//            info = info + "" + this.musicRetriever.getSongCount() + " audio files automatically scanned by system";
//        }else {
            String dirName = getCurrentDirectoryName();
            info = "Folder: \"" + dirName + "\"\n";
            String folderYear = extractYearFromFolder(currentFolder);
            if (folderYear != null && !folderYear.isEmpty()) {
                info = info + "Year: " + folderYear + "\n";
            }
            File[] files = currentFolder.listFiles(aFilter);
            if (files != null) {
                info = info + files.length + " audio files\n";
            } else {
                info = info + "0 audio files\n";
            }
            //info = info + Math.round(currentFolder.getFreeSpace() / 1000000000) + " gigabyte free storage left.\n";
            //info = info + "in " + currentFolder.getParentFile().getAbsolutePath() + "\n";

            String fullPathString = currentFolder.getParentFile().getAbsolutePath();
            if( (index = fullPathString.indexOf(this.rootFolder.getPath()) ) != -1){
                String rootFoler = rootFolder.getPath();
                Log.i(TAG, "getCurrentFileName ---> " + index + ":" +fullPathString);
                fullPathString = "Root Folder"+fullPathString.substring(rootFolder.getPath().length(), fullPathString.length());
            }

            info = info + "in " + fullPathString + "\n";

            info = info + String.format("%.2f", (double) currentFolder.getFreeSpace() / 1000000000)
                        + " GB free storage left";

//        }

        return info;
    }

    public String getCurrentDirectoryDetailsPrimary() {
        if (currentFolder == null) return "No folder selected";
        StringBuilder sb = new StringBuilder();
        File[] files = currentFolder.listFiles(aFilter);
        int fileCount = (files != null) ? files.length : 0;
        sb.append(fileCount).append(" audio files");

        String folderYear = extractYearFromFolder(currentFolder);
        if (folderYear != null && !folderYear.isEmpty()) {
            sb.append("   •   ").append(folderYear);
        }

        try {
            double freeGb = (double) currentFolder.getFreeSpace() / 1000000000.0;
            sb.append("   •   ").append(String.format(java.util.Locale.US, "%.1f GB free", freeGb));
        } catch (Exception ignored) {}

        return sb.toString();
    }

    public String getCurrentDirectoryDetailsSecondary() {
        if (currentFolder == null) return "";
        StringBuilder sb = new StringBuilder();
        if (this.directories != null && this.directories.size() > 0) {
            sb.append("Folder ").append(this.currentDirectoryIndex + 1)
              .append(" of ").append(this.directories.size());
        }

        try {
            String fullPathString = currentFolder.getParentFile() != null ? currentFolder.getParentFile().getAbsolutePath() : "";
            int index = -1;
            if (this.rootFolder != null && (index = fullPathString.indexOf(this.rootFolder.getPath())) != -1) {
                fullPathString = "Root" + fullPathString.substring(rootFolder.getPath().length());
            }
            if (!fullPathString.isEmpty()) {
                if (sb.length() > 0) sb.append("   •   ");
                sb.append(fullPathString);
            }
        } catch (Exception ignored) {}

        return sb.toString();
    }

    public int getAudibleFilesCount() {
        return (this.audibleFiles != null) ? this.audibleFiles.size() : 0;
    }

    public String getFileNameWithOffset(int offset) {
        if (this.audibleFiles == null || this.audibleFiles.size() <= 1) return "";
        int size = this.audibleFiles.size();
        if (size <= 2) {
            if (Math.abs(offset) > 1) return "";
        }
        int targetIndex = (this.currentFileIndex + offset) % size;
        if (targetIndex < 0) targetIndex += size;
        try {
            if (targetIndex >= 0 && targetIndex < this.audibleFiles.size()) {
                File file = this.audibleFiles.get(targetIndex);
                return (file != null) ? file.getName() : "";
            }
        } catch (Exception ignored) {}
        return "";
    }

    public String getCurrentFileDetailsPrimary() {
        if (isRecording) {
            String ch = (recordingChannels == 2) ? "Stereo" : "Mono";
            return "RECORDING IN PROGRESS   •   " + ch + " AAC";
        }
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex < 0 || currentFileIndex >= audibleFiles.size()) {
            return "No file in folder";
        }
        File current = audibleFiles.get(currentFileIndex);
        StringBuilder sb = new StringBuilder();

        long durationMs = audioFileDuration;
        if (durationMs <= 0) {
            try {
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(current.getAbsolutePath());
                String durStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                if (durStr != null) {
                    durationMs = Long.parseLong(durStr);
                }
                mmr.release();
            } catch (Exception ignored) {}
        }

        if (durationMs > 0) {
            long totalSec = durationMs / 1000;
            long hours = totalSec / 3600;
            long mins = (totalSec % 3600) / 60;
            long secs = totalSec % 60;
            if (hours > 0) {
                sb.append(String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs));
            } else {
                sb.append(String.format(java.util.Locale.US, "%02d:%02d", mins, secs));
            }
        }

        AudioMetadataHelper.AudioMetadata meta = null;
        try {
            meta = AudioMetadataHelper.loadMetadata(mainActivity, current);
        } catch (Exception ignored) {}

        if (meta != null && meta.timeOfDay != null && !meta.timeOfDay.isEmpty()) {
            if (sb.length() > 0) sb.append("   •   ");
            sb.append(meta.timeOfDay);
        } else {
            String year = extractYearFromFolder(currentFolder);
            if (year != null && !year.isEmpty()) {
                if (sb.length() > 0) sb.append("   •   ");
                sb.append(year);
            }
        }

        int ch = (meta != null && meta.channels > 0) ? meta.channels
                : AudioMetadataHelper.getAudioFileChannels(current);
        if (sb.length() > 0) sb.append("   •   ");
        sb.append(ch == 2 ? "Stereo" : "Mono");

        return sb.toString();
    }

    public String getCurrentFileDetailsSecondary() {
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex < 0 || currentFileIndex >= audibleFiles.size()) {
            return "";
        }
        if (isRecording) {
            return "Target Folder: " + getCurrentDirectoryName();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("File ").append(currentFileIndex + 1).append(" of ").append(audibleFiles.size());

        if (currentFolder != null) {
            sb.append("   •   Folder: ").append(getCurrentDirectoryName());
        }

        return sb.toString();
    }

    public static String extractYearFromFolder(File folder) {
        if (folder == null) return null;
        String name = folder.getName();
        if (name.length() >= 4 && Character.isDigit(name.charAt(0)) && Character.isDigit(name.charAt(1))
                && Character.isDigit(name.charAt(2)) && Character.isDigit(name.charAt(3))) {
            return name.substring(0, 4);
        }
        File parent = folder.getParentFile();
        if (parent != null) {
            String pName = parent.getName();
            if (pName.length() >= 4 && Character.isDigit(pName.charAt(0)) && Character.isDigit(pName.charAt(1))
                    && Character.isDigit(pName.charAt(2)) && Character.isDigit(pName.charAt(3))) {
                return pName.substring(0, 4);
            }
        }
        return null;
    }

    public void moveCurrentFileToTrash(){
        try{
            File target = new File(getCurrentFileFullPath());
            AudioMetadataHelper.deleteMetadata(mainActivity, target);
            copyDirectoryOneLocationToAnotherLocation(target, new File(trashFolder+"/"+getCurrentFileName()));
            target.delete();
            readAudibleFilesInCurrentFolder();
        }catch(IOException e){
            e.printStackTrace();
        }

    }

    public int addRecordingBookmark() {
        if (!isRecording) return 0;
        int offsetMs = (int) (System.currentTimeMillis() - recordStartTimeMillis);
        if (offsetMs < 0) offsetMs = 0;
        recordingBookmarks.add(offsetMs);
        return recordingBookmarks.size();
    }

    public int getCurrentPositionMillis() {
        if (mPlayer != null) {
            try {
                return mPlayer.getCurrentPosition();
            } catch (Exception e) {
                return 0;
            }
        }
        return 0;
    }

    public int getDurationMillis() {
        if (mPlayer != null) {
            try {
                return mPlayer.getDuration();
            } catch (Exception e) {
                return (int) audioFileDuration;
            }
        }
        return (int) audioFileDuration;
    }

    public long getRecordStartTimeMillis() {
        return recordStartTimeMillis;
    }

    public List<Integer> getRecordingBookmarks() {
        return recordingBookmarks;
    }

    public Integer nextBookmark() {
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex >= audibleFiles.size()) return null;
        File current = audibleFiles.get(currentFileIndex);
        int currentMs = getCurrentPositionMillis();
        Integer target = AudioMetadataHelper.getNextBookmark(mainActivity, current, currentMs);
        if (target != null && mPlayer != null) {
            try {
                mPlayer.seekTo(target);
            } catch (Exception e) {
                Log.w(TAG, "Error seeking to bookmark: " + e.getMessage());
            }
        }
        return target;
    }

    public Integer previousBookmark() {
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex >= audibleFiles.size()) return null;
        File current = audibleFiles.get(currentFileIndex);
        int currentMs = getCurrentPositionMillis();
        Integer target = AudioMetadataHelper.getPreviousBookmark(mainActivity, current, currentMs);
        if (target != null && mPlayer != null) {
            try {
                mPlayer.seekTo(target);
            } catch (Exception e) {
                Log.w(TAG, "Error seeking to bookmark: " + e.getMessage());
            }
        }
        return target;
    }

    public boolean toggleCurrentFileFavorite() {
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex >= audibleFiles.size()) return false;
        File current = audibleFiles.get(currentFileIndex);
        return AudioMetadataHelper.toggleFavorite(mainActivity, current);
    }

    public boolean isCurrentFileFavorite() {
        if (audibleFiles == null || audibleFiles.isEmpty() || currentFileIndex >= audibleFiles.size()) return false;
        File current = audibleFiles.get(currentFileIndex);
        return AudioMetadataHelper.isFavorite(mainActivity, current);
    }

    public boolean nextFavorite() {
        if (audibleFiles == null || audibleFiles.isEmpty()) return false;
        int count = audibleFiles.size();
        for (int i = 1; i <= count; i++) {
            int nextIndex = (currentFileIndex + i) % count;
            File f = audibleFiles.get(nextIndex);
            if (AudioMetadataHelper.isFavorite(mainActivity, f)) {
                currentFileIndex = nextIndex;
                return true;
            }
        }
        return false;
    }

    public boolean previousFavorite() {
        if (audibleFiles == null || audibleFiles.isEmpty()) return false;
        int count = audibleFiles.size();
        for (int i = 1; i <= count; i++) {
            int prevIndex = (currentFileIndex - i + count) % count;
            File f = audibleFiles.get(prevIndex);
            if (AudioMetadataHelper.isFavorite(mainActivity, f)) {
                currentFileIndex = prevIndex;
                return true;
            }
        }
        return false;
    }


    public static void copyDirectoryOneLocationToAnotherLocation(File sourceLocation, File targetLocation)
            throws IOException {

        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            String[] children = sourceLocation.list();
            for (int i = 0; i < sourceLocation.listFiles().length; i++) {

                copyDirectoryOneLocationToAnotherLocation(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {

            InputStream in = new FileInputStream(sourceLocation);

            OutputStream out = new FileOutputStream(targetLocation);

            // Copy the bits from instream to outstream
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
        }

    }

    private void deleteFile(String inputPath, String inputFile) {
        try {
            // delete the original file
            new File(inputPath + inputFile).delete();


        }
        catch (Exception e) {
            com.jonlee.android.common.logger.Log.e("tag", e.getMessage());
        }
    }

    public void showToast(final String toast) {
        Toast.makeText(mainActivity.getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
    }

    public void importFolder(final Uri uri) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme()) || !android.provider.DocumentsContract.isTreeUri(uri)) {
            displayToast("Invalid folder selected.");
            return;
        }

        DocumentFile pickedDir;
        try {
            pickedDir = DocumentFile.fromTreeUri(mainActivity, uri);
        } catch (Exception e) {
            Log.e(TAG, "Error opening tree URI: " + uri, e);
            displayToast("Cannot open folder.");
            return;
        }
        if (pickedDir == null) return;

        String folderName = pickedDir.getName();
        String uriString = uri.toString();

        // 1. Check if it's an app-managed folder
        if (SHARED_FOLDER.equalsIgnoreCase(folderName) ||
            "SJPlayer".equalsIgnoreCase(folderName) ||
            "Lyrics".equalsIgnoreCase(folderName)) {
            displayToast("This folder is already managed by SJPlayer.");
            return;
        }

        // 2. Check if it's the currently selected target folder in preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mainActivity);
        String currentTargetFolderUri = sharedPreferences.getString("target_folders", null);
        if (uriString.equals(currentTargetFolderUri)) {
            displayToast("This folder is already being scanned.");
            return;
        }

        // 3. Check if it's within the app's own data directory (via URI check)
        if (uriString.contains(mainActivity.getPackageName())) {
            displayToast("Cannot import from the app's internal storage.");
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(mainActivity, uri);
                    if (pickedDir != null) {
                        displayStatus("Importing files from " + pickedDir.getName() + "...");
                        int importedCount = importRecursive(pickedDir, rootFolder);
                        displayStatus("Imported " + importedCount + " files from " + pickedDir.getName());
                        displayToast("Imported " + importedCount + " files.");

                        // Refresh directories and files
                        readDirectories();
                        sortDirectories();
                        readAudibleFilesInCurrentFolder();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error importing files: " + e.getMessage(), e);
                }
            }
        }).start();
    }

    private int importRecursive(DocumentFile sourceDir, File targetDir) {
        int count = 0;
        for (DocumentFile file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                File nextTargetDir = new File(targetDir, file.getName());
                if (!nextTargetDir.exists()) {
                    nextTargetDir.mkdirs();
                }
                count += importRecursive(file, nextTargetDir);
            } else {
                if (MimeUtils.isAudioFile(file.getName())) {
                    File targetFile = new File(targetDir, file.getName());
                    if (copyFileFromUri(file.getUri(), targetFile)) {
                        count++;
                        displayStatus("Importing: " + file.getName());
                    }
                }
            }
        }
        return count;
    }

    private boolean copyFileFromUri(Uri sourceUri, File targetFile) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = mainActivity.getContentResolver().openInputStream(sourceUri);
            out = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + e.getMessage());
            return false;
        } finally {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public boolean isJustRecordFinished(){
        return justRecordFinished;
    }

    public File getNewRecordedFile(){
        return new File(newFileName);
    }

}
