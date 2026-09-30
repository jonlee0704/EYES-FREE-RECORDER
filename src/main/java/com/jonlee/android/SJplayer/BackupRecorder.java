package com.jonlee.android.SJplayer;

/**
 * Created by jongyeong on 6/15/14.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.jonlee.android.common.utils.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
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

/**
 * TODO Adding "Cloud folder" of Playing back YouTube bookamark
 * *** VERY IMPORTANT THAT THIS CLASS SHOULD USE SAME LOGIC TO LOAD TARGET FILES WITH RECORDER.CLASS
 */
public class BackupRecorder {
    private static final String TAG = "BackupRecorder";


    private boolean isRecording = false;
    private boolean isPlaying = false;
    private MediaPlayer mPlayer = null;
    private MusicRetriever musicRetriever;

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


    private int currentFileIndex = 0;
    private int currentDirectoryIndex = 0;

    //Total file duration
    private int audioFileDuration = 0;

    private final String folderNameForAllMusicByMediaScanner = "All-Music-By-MediaScanner";

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

    private boolean isBackupMode = false;



    private File validFolderListLogFile;
    /**
     * This is for "Backup agent" only
     */

    public BackupRecorder(){
        // Log file to store valid folders including audio files
        validFolderListLogFile = new File(rootFolder.getAbsolutePath() + "/" + Recorder.VALID_FOLDER_LIST_LOG_FILE);
//        if(validFolderListLogFile.exists()){

        try {
            BufferedReader in = new BufferedReader(new FileReader(validFolderListLogFile));
            String line = "";
            while ((line = in.readLine()) != null) {
                String parts[] = line.split("\t");
                directories.add(new File(parts[0]));
                scannedFileCnt = scannedFileCnt + Integer.parseInt(parts[1]);
                scannedFolderCnt = scannedFolderCnt + 1;
                this.displayStatus(scannedFolderCnt + " folders and " + scannedFileCnt + " files loaded ");
            }
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        }

    }

    public BackupRecorder(DriveMainActivity a){
        isBackupMode = true;
        mainActivity = a;
        status_TextView = (TextView) mainActivity.findViewById(R.id.backup_main_textView);
        status_TextView.setText("Backup Agent started...");

        aFilter = new AudibleFileFilter();

        // Find a bigger size folder between extSD vs removableExtSDcard
        // And set it the rootFolder
        rootFolder = getBiggerExtSDCardDirectory();
        // Starting with '.' won't be included into File.directories
        trashFolder = new File(rootFolder.getAbsolutePath() + "/.Trash" );

        // Creating a virtual directory to play all musics provided by MediaScanner
        // TODO Create a file to explain that this folder won't be scanned
        // folderNameForAllMusicByMediaScanner will be under Public Music folder
        publicMusicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        //TODO Fail to create a folder under public music folder
        //Testing under app directory
        //folderForAllMusicByMediaScanner = new File(publicMusicFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);
        folderForAllMusicByMediaScanner = new File(rootFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);

        // Starting with '.' won't be included into File.directories
        // Lyrics files are always under PublicMusicFolder as should not be deleted
        // And also easily found when it's copied into.
        lyricsFolder = new File(rootFolder.getAbsolutePath() + "/Lyrics" );

        sharedFolder = new File(rootFolder.getAbsolutePath()+"/"+ Recorder.SHARED_FOLDER);

        if(!sharedFolder.exists()){
            sharedFolder.mkdir();
        }

        // Starting from RootFolder.
        currentFolder = rootFolder;
        // Log file to store valid folders including audio files
        validFolderListLogFile = new File(rootFolder.getAbsolutePath() + "/" + Recorder.VALID_FOLDER_LIST_LOG_FILE);
//        if(validFolderListLogFile.exists()){

        directories = new ArrayList<File>();

        try {
            BufferedReader in = new BufferedReader(new FileReader(validFolderListLogFile));
            String line = "";
            int fCntFromFile = 0;
            while ((line = in.readLine()) != null) {
//                Log.i(TAG, "BackupRecxorder===> " + line);
                String parts[] = line.split("\t");
                fCntFromFile = Integer.parseInt(parts[1]);
                scannedFileCnt = scannedFileCnt + fCntFromFile;
                if(fCntFromFile > 0) {
                    directories.add(new File(parts[0]));
                    scannedFolderCnt = scannedFolderCnt + 1;
                }
//                currentFolder = directories.get(0);
//                initiateFolder();

                isReadyToStartCount++;
                isReadyToStart = true;
            }
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        } finally{
            // Whatever result, it should return TRUE once it quits from the loop
            isReadyToStart = true;
        }
//        for(int i=0;directories.size() > i ; i++){
//            Log.i(TAG, "check2 =====>"+directories.get(i).getName());
//
//        }

        this.displayStatus(scannedFolderCnt + " folders and " + scannedFileCnt + " files loaded ");

        // Sorting
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare (File f1, File f2)
            {
                //displayStatus("Sorting directories...");
                return f2.getName().compareTo(f1.getName());
            }

        });
        if(!directories.isEmpty()) {
            currentFolder = directories.get(0);
        }
        initiateFolder();
    }

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

    public File getSharedFolder() { return sharedFolder; }

    /**
     * TODO: Is this correct way to pass Activity to another class?
     *
     * @param ma: MainActivity
     *            Starting from
     */
    public BackupRecorder(MainActivity ma)
    {
        mainActivity = ma;
        status_TextView = (TextView) ma.findViewById(R.id.trackPos_TextView);
        aFilter = new AudibleFileFilter();

        // Find a bigger size folder between extSD vs removableExtSDcard
        // And set it the rootFolder
        rootFolder = getBiggerExtSDCardDirectory();

        // Starting with '.' won't be included into File.directories
        trashFolder = new File(rootFolder.getAbsolutePath() + "/.Trash" );

        // TODO Same folder name could be created in two different location.
        // This can be treated inside of readDirectories, but this might be easier.
        // TODO a method of Sorting, Cleaning up directories?
        // This causes end user will get two same name of folder.
        // Recording target folder will be always shown up even it has 0 file in.
        // This is why it's added after this.readDiretories() method.
        //
        initiateTargetFolder();

        // Creating root_folder in case there is not.
        if (!rootFolder.exists()) {
            rootFolder.mkdir();
        }

        // Creating root_folder in case there is not.
        if (!trashFolder.exists()) {
            trashFolder.mkdir();
        }

        // Creating a virtual directory to play all musics provided by MediaScanner
        // TODO Create a file to explain that this folder won't be scanned
        // folderNameForAllMusicByMediaScanner will be under Public Music folder
        publicMusicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        if (!publicMusicFolder.exists()) {
            publicMusicFolder.mkdir();
        }


        //TODO Fail to create a folder under public music folder
        //Testing under app directory
        //folderForAllMusicByMediaScanner = new File(publicMusicFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);
        folderForAllMusicByMediaScanner = new File(rootFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);
        if(!folderForAllMusicByMediaScanner.exists())
            folderForAllMusicByMediaScanner.mkdir();

        // Starting with '.' won't be included into File.directories
        // Lyrics files are always under PublicMusicFolder as should not be deleted
        // And also easily found when it's copied into.
        lyricsFolder = new File(rootFolder.getAbsolutePath() + "/Lyrics" );

        // Creating lyrics_folder
        if(!lyricsFolder.exists()) {
            lyricsFolder.mkdir();
        }

        Log.i(TAG, "Initiate Recorder:" + publicMusicFolder + "\n" +
                "folderForAllMusicByMediaScanner:" + folderForAllMusicByMediaScanner + "\n" +
                "LyricsFolder: " + lyricsFolder.getAbsolutePath());

//        mProgress = (ProgressBar) ma.findViewById(R.id.progressBar);
//        new Thread(new Runnable() {
//            public void run() {
//                mProgress.setVisibility(ProgressBar.VISIBLE);
//            }
//        }).start();



        new Thread(new Runnable() {
            public void run() {
                displayStatus("Initializing MediaScanned Files...");

                // Initiate MusicRetriever for MediaScanned files
                musicRetriever = new MusicRetriever(mainActivity.getContentResolver());
                //TODO Disabled it. There are too many files pre-loaded.
                //musicRetriever.prepare();
                displayStatus("Initialized MediaScanned Files!");

                isReadyToStartCount++;
            }
        }).start();



        new Thread(new Runnable() {
            public void run() {
                // Starting from RootFolder.
                currentFolder = rootFolder;

                // After confirming folder is in place
                // Filtering
                initiateFolder();
                //Log.i(TAG, "1 CurrentFolder:" + currentFolder.getAbsolutePath());

                // Initiate folder array, files
                //readDirectoriesFromMediaScanner();
                readDirectories();

                // Sorting and...
                // this.cleanDirectories();
                currentFolder = directories.get(0);
                initiateFolder();

                isReadyToStartCount++;
            }
        }).start();

        new Thread(new Runnable() {
            public void run() {
                    try {
                        while(isReadyToStartCount < 2) {
                            //Nothing
                            Thread.sleep(500);
                        }
                        isReadyToStart = true;
                        displayStatus(scannedFolderCnt + " folders, "
                                + scannedFileCnt + " files scanned. Ready to use!");

                        ((MainActivity)mainActivity).speak(scannedFolderCnt + " folders, "
                                + scannedFileCnt + " files scanned. Ready to use!");
                        //mProgress.setVisibility(ProgressBar.INVISIBLE);

                        //Log.i(TAG, "Total song count by musicRetriever is ===> " + musicRetriever.getSongCount());

                    }catch(InterruptedException ie) {
                        // Nothing
                    }
            }
        }).start();


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

    /**
     * Whenever recorderStart called, it checks if the date_folder exist and add to directories.
     * @return
     */
    public File initiateTargetFolder(){
        recordTargetFolder = new File(rootFolder.getAbsolutePath()+"/" + this.getRecordingFolderName());
        if(!recordTargetFolder.exists()) {
            recordTargetFolder.mkdir();
        }
        return recordTargetFolder;
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
            }catch(IllegalStateException e){
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
     * TODO PUBLIC_MUSIC is included by default, then the path might
     * @return
     */
    public String getCurrentFileFullPath(){
        return currentFolder.getAbsolutePath() + "/" + this.getCurrentFileName();
    }

   //Possibly returning NULL
    //Throw NullPointerException
    public File getCurrentFile(){
        return audibleFiles.get(this.currentFileIndex);
    }

    public String getCurrentFileName(){
        if(this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
            MusicRetriever.Item item = musicRetriever.getCurrentItem();
            // TODO Need to find the real file name(location)
            return musicRetriever.getFilePathFromContentUri();
        }

        if(audibleFiles.size() > 0) {
            return  audibleFiles.get(this.currentFileIndex).getName();
        } else {
            return mainActivity.getResources().getString(R.string.NO_AUDIBLE_FILE_EXIST);
        }
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
    private void readRecursiveDir(File sFile){
        if (sFile == null || !sFile.exists() || !sFile.isDirectory()) {
            return;
        }
        File[] mFile = sFile.listFiles();
        if (mFile == null) {
            return;
        }

        for (File file : mFile) {
            if (file != null && file.isDirectory() && !file.isHidden() && !file.getName().startsWith(".")) {
                File[] audioFiles = file.listFiles(aFilter);
                int fileCnt = (audioFiles != null) ? audioFiles.length : 0;
                scannedFileCnt += fileCnt;

                if (file.equals(this.recordTargetFolder) || fileCnt > 0) {
                    directories.add(file);
                    this.scannedFolderCnt++;
                    this.displayStatus("Scanning: " + scannedFolderCnt + ": " + file.getName());
                }
                readRecursiveDir(file);
            }
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

        // Sorting
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare (File f1, File f2)
            {
                //Log.i(TAG,"Sorting directories...");
                //Recent one is first for Backup.
                //Different with Player's sort ordering
                return f1.getName().compareTo(f2.getName());
            }

        });
    }

    /**
     * Target folders
     * 1) folderForAllMusicByMediaScanner under userDataSpace
     * 2) Public Music folder
     * 3) All ExtSDcards
     */
    private void readDirectories(){
        directories = new ArrayList<File>();
        recordTargetFolder = new File(rootFolder.getAbsolutePath()+"/" + this.getRecordingFolderName());

        //Public Music directory.
        //Add only when there is audio files exist.
        if(musicRetriever != null && musicRetriever.getSongCount() > 0)
            directories.add(folderForAllMusicByMediaScanner);


        //TODO improve codes to get the removable SDcard programable.
        File samsungSDcardMusic =
                new File(FileUtils.getSamsungPublicMusicFolderRoot(rootFolder.getAbsolutePath()));

        Log.i(TAG, "samsungSDcardMusic.Recorder ===> " + samsungSDcardMusic.getAbsolutePath());

        if (samsungSDcardMusic.exists()) {
            readRecursiveDir(samsungSDcardMusic);
        }
//

        // For GS7- devices
        File samsungSDcardMusicGS7 = new File("/storage/extSdCard/Music");
//        File samsungSDcardMusicGS7Temp = new File("/storage/9C33-6BBD/Music");
//

//        if (samsungSDcardMusicGS7.exists())
//            readRecursiveDir(samsungSDcardMusicGS7);

        if (publicMusicFolder != null && publicMusicFolder.listFiles(aFilter).length > 0) {
            directories.add(publicMusicFolder);
        }

        Log.i(TAG, "publicMusicFolder:" + publicMusicFolder +
                ":External" + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC));

        readRecursiveDir(publicMusicFolder);

        // 1) Adding both extSDCard and read all directories having 1+ audible files
        // 2) Checking if recordTargetFolder exists.
        File dir[] = ContextCompat.getExternalFilesDirs((Context) this.mainActivity, Environment.DIRECTORY_MUSIC);

        for(int i = 0; i < dir.length && dir[i] != null ; i++ ) {
            //Log.i(TAG, "inside readDirectories" + dir[i].getAbsolutePath());
            // Record Target Folder needs to be added when it has no Audible file.
            if (dir[i].listFiles(aFilter).length > 0) {
                directories.add((File)dir[i]);
                this.displayStatus(((File) dir[i]).getName() + " loaded");
            }
            readRecursiveDir(dir[i]);
        }

        // Sorting
        Collections.sort(directories, new Comparator<File>() {
            @Override
            public int compare (File f1, File f2)
            {
                //displayStatus("Sorting directories...");
                return f2.getName().compareTo(f1.getName());
            }

        });
        displayStatus("Sorting files...");
    }

    public void displayStatus(String msg){
        statusMsg = msg;
        mHandler.sendMessage(mHandler.obtainMessage());
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
                initiateFolder();
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
                initiateFolder();
            }

            Log.i(TAG, "currentDirectoryIndex:" + this.currentDirectoryIndex + ":Audible file size: " + audibleFiles.size()+ currentFolder.getAbsolutePath());
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    /**
     * 1) Read only audible files(AudibleFileFilter) from current directory
     * 2) Filter out un-audible files
     * 3) Always set current Index to 0
     * 4) This method is called when -nextFolder, previousFolder
     */
    public void initiateFolder(){
        //Toast.makeText(this.mainActivity, "Initiating folders", Toast.LENGTH_SHORT).show();
        //Adding only Audible Files
        //this.displayStatus("Scanning files...");
        try {
            this.audibleFiles = new ArrayList(Arrays.asList(this.currentFolder.listFiles(aFilter)));
            //this.audibleFiles = new ArrayList(Arrays.asList(this.currentFolder.listFiles()));

            //TODO Does it need to run this everytime load?
            Collections.sort(audibleFiles, new Comparator<File>() {
                @Override
                public int compare (File f1, File f2)
                {
                    return f1.getName().compareTo(f2.getName());
                }

            });

        }catch(Exception e){
            //TODO [P0] Null pointException root cause?
            audibleFiles = new ArrayList<File>();
        }
        this.currentFileIndex = 0;
        //this.displayStatus("");
    }

    /**
     * Current code is not returning Removable SD card, instead, it returns the biggest Free space one.
     * #TODO Should this be moved to Common libs?
     * @return File
     */
    public File getBiggerExtSDCardDirectory()
    {
        File dir[] = ContextCompat.getExternalFilesDirs((Context) this.mainActivity,
            Environment.DIRECTORY_MUSIC);

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

            if (dir[i].getTotalSpace() > freeSize) {
                targetFile = dir[i];
                freeSize = dir[i].getTotalSpace();
            }
        }

        Log.i(TAG, "targetFile:" + targetFile.getAbsolutePath().toString() + ":" + targetFile.getFreeSpace());
        return   targetFile;
    }


    /**
     * change current Folder and File index to the just saved file.
     */
    public void moveToLatestRecordedLocation(){
       // TargetFolder is already created and existing in the Directory index because it's always checked when Recoder is instant'ed.

//        // TODO where is DirectoryIndex used for?
//        currentFolder = recordTargetFolder;
//        this.currentFileIndex = recordTargetFolder.list().length - 1;

        for(int i=0; i < directories.size() ; i++){
            if(directories.get(i).equals(this.recordTargetFolder) ) {
                //Log.i(TAG, "Inside of moveToLatestRecordedLocation: " + directories.get(i).getName() + ":" + i);

                this.currentDirectoryIndex = i;
                // Set current target Folder.
                this.currentFolder = directories.get(i);
                this.initiateFolder();
                //TODO Maybe... comparing the exact file name is better way to find the index.
                this.currentFileIndex = currentFolder.listFiles(aFilter).length - 1;
                return;
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

        if(this.currentFolder.equals(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)))
            return "Public Music";

        return this.currentFolder.getName();
    }

    /**
     * Return current folder name to let user know where it is.
     */
    public String getCurrentDirectoryInformation(){

        String info;

        if(this.currentFolder.equals(folderForAllMusicByMediaScanner)) {
            info = "Folder, \"" + getCurrentDirectoryName() + "\" \n\nhas ";
            info = info + "" + this.musicRetriever.getSongCount() + " audio files automatically scanned by system";
        }else {
            info = "Folder, \"" + getCurrentDirectoryName() + "\" \n\nhas ";
            info = info + currentFolder.listFiles(aFilter).length + " audio files and ";

            info = info + String.format("%.2f", (double) currentFolder.getFreeSpace() / 1000000000) + " gigabyte free storage left.\n\n";

            //info = info + Math.round(currentFolder.getFreeSpace() / 1000000000) + " gigabyte free storage left.\n\n";
            info = info + "Located in " + currentFolder.getAbsolutePath();


        }

        return info;
    }

    public void moveCurrentFileToTrash(){
        try{
            File target = new File(getCurrentFileFullPath());
            FileUtils.copyDirectoryOneLocationToAnotherLocation(target, new File(trashFolder+"/"+getCurrentFileName()));
            target.delete();
            initiateFolder();
        }catch(IOException e){
            e.printStackTrace();
        }

        Log.i(TAG, "Current Folder after moving file:" + getCurrentFileFullPath());
    }


}
