package com.jonlee.android.SJplayer;

/**
 * Created by jongyeong on 6/15/14.
 */

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

/**
 * TODO Adding "Cloud folder" of Playing back YouTube bookamark
 * TODO Adding "Cloud folder" of
 */
public class Recorder{
    private static final String TAG = "Recorder";


    private boolean isRecording = false;
    private boolean isPlaying = false;
    private boolean isPaused = false;
    private MediaPlayer mPlayer = null;
    private MediaRecorder mRecorder = null;
    private MusicRetriever musicRetriever;

    private File rootFolder;
    // The folder current cursor is on.
    private File currentFolder;
    // MMMM-yyyy under the root folder
    private File recordTargetFolder;
    private File publicMusicFolder;
    private File folderForAllMusicByMediaScanner;
    private ArrayList<File> audibleFiles;
    // only below the ROOT and 1 level folders only
    private ArrayList<File> directories = null;

    private MainActivity mainActivity = null;
    private int currentFileIndex = 0;
    private int currentDirectoryIndex = 0;

    //Total file duration
    private int audioFileDuration = 0;

    //Manual file name
//    private final String manualFile = "WELCOME.txt";
    private final String folderNameForAllMusicByMediaScanner = "All-Music-By-MediaScanner";

    //Total continuously playback file count
    private int playCnt = 0;
    //Stop automatic playback when it gets in this nunber
    private final int MAX_CONTINOUS_PLAYBACK = 30;


    /**
     * TODO: Is this correct way to pass Activity to another class?
     *
     * @param ma: MainActivity
     *            Starting from
     */
    public Recorder(MainActivity ma)
    {
        this.mainActivity = ma;
        // Find a bigger size folder between extSD vs removableExtSDcard
        // And set it the rootFolder
        rootFolder = getBiggerExtSDCardDirectory();
        recordTargetFolder = new File(rootFolder.getAbsolutePath()+"/" + this.getRecordingFolderName());
        // TODO Same folder name could be created in two different location.
        // This can be treated inside of readDirectories, but this might be easier.
        // TODO a method of Sorting, Cleaning up directories?
        // This causes end user will get two same name of folder.
        // Recording target folder will be always shown up even it has 0 file in.
        // This is why it's added after this.readDiretories() method.
        if(!recordTargetFolder.exists()) {
            recordTargetFolder.mkdir();
        }

        // Creating root_folder in case there is not.
        // TODO whatif there are two same folder name in extSD and removableExtSD?
        if (!rootFolder.exists()) {
            rootFolder.mkdir();
        }


        // Creating a virtual directory to play all musics provided by MediaScanner
        // TODO Create a file to explain that this folder won't be scanned
        // folderNameForAllMusicByMediaScanner will be under Public Music folder
        publicMusicFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);

        folderForAllMusicByMediaScanner = new File(publicMusicFolder.getAbsolutePath()+"/"+folderNameForAllMusicByMediaScanner);
        if(!folderForAllMusicByMediaScanner.exists())
            folderForAllMusicByMediaScanner.mkdir();

//        if (rootFolder.listFiles().length == 0){
//            String string = ma.getString(R.string.MANUAL);
//            try {
//                // Creating a manual
//                File file = new File(rootFolder.getAbsolutePath() + File.separator + manualFile);
//                FileOutputStream fos = new FileOutputStream(file);
//                fos.write(string.getBytes());
//                fos.flush();
//                fos.close();
//            }catch(FileNotFoundException e){
//                e.printStackTrace();
//            }catch(IOException e){
//                e.printStackTrace();
//            }
//
//        }

        // Initiate MusicRetriever for MediaScanned files
        this.musicRetriever = new MusicRetriever(this.mainActivity.getContentResolver());
        musicRetriever.prepare();

        // Starting from RootFolder.
        this.currentFolder = this.rootFolder;
        // After confirming folder is in place
        this.initiateFolder();
        // Initiate folder array, files
        this.readDirectories();

        // move on the first folder
        // At least the recordTarget folder.
        this.currentFolder = directories.get(0);

        /**
         * TEST
         */
//        for(int i = 0 ; directories.size() > i ; i ++)
//            Log.i(TAG, "Listing directories: " + directories.get(i).getAbsolutePath());


    }

    /**
     * Cleaning up Directories
     * 1) Sorting
     * 2) Checking if recordTargetFolder exists.
     */
    public void cleanDirectories(){

        //

    }

    /**
     * TODO: Need to make a decision if it's really need to configurable in Settings.
     * Default value is YYYY-MM-DD-HH-MM (or +LOCATION)
     *
     * @return
     */
    public String createFileName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM-dd-yyyy-HHmmss", Locale.US);
        return sdf.format(c.getTime());
    }

    /**
     * Get folder name by MMMM-YYYY
     * @return
     */
    public String getRecordingFolderName() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("MMMM-yyyy",Locale.US);
        return sdf.format(c.getTime());
    }

    /**
     * When player ends up playing music, it calls the next song and play
     * @return
     */
//    @Override
//    public void onCompletion(MediaPlayer mp){
//        //if(mp != null)
//            Log.i(TAG, "inside of onCompletion");
//    }



    public int getCurrentFileIndex() {
        return this.currentFileIndex;
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
        if(mPlayer != null)
            mPlayer.seekTo(mPlayer.getCurrentPosition()+d);
    }

    /**
     * Playing from the latest file to old
     */
    public void startPlaying() {
        //Log.i(TAG+"startPlaying()",currentFileIndex + ":" + files.size());

        // If there is no files, just do nothing.
        // What will be better way than this ... way?
        if(audibleFiles.size() == 0)
            return;

        Log.i(TAG,"playing?:"+getCurrentFileFullPath());
        this.isPlaying = true;

        try {
            if (!isPaused || mPlayer == null){ // Resume does not need to initiate Instance
                mPlayer = new MediaPlayer();
                mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.i(TAG, "inside of onCompletion"); // finish current activity
                        if( playCnt < MAX_CONTINOUS_PLAYBACK ) {
                            mainActivity.cmd(Command.NEXT_SONG);
                            playCnt++;
                        }
                        else {
                            mainActivity.speak(mainActivity.getResources().getString(R.string.STILL_THERE));
                            playCnt = 0;
                        }
                    }
                });
//
//                mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
//                    public boolean onError(MediaPlayer paramMediaPlayer, int paramInt1,int paramInt2) {
//                        Log.i(TAG, "inside of onErrorListener"); // finish current activity
//                        mainActivity.cmd(Command.NEXT_SONG);
//                        return true;
//                    }
//                });

                Log.i(TAG, "getCurrentFileFullPath():" + getCurrentFileFullPath());
                if(this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
                    mPlayer.setDataSource(mainActivity, musicRetriever.getCurrentUri());
                }else {
                    mPlayer.setDataSource(getCurrentFileFullPath());
                }
                mPlayer.prepare();
                //mPlayer.setVolume(1, 1);
                mPlayer.start();
                //Set the duration of file to use in SEEK()
                this.audioFileDuration = mPlayer.getDuration();
            } else {
                mPlayer.start();
                this.isPaused = false;
            }
        } catch (IOException e) {
            //Log.i(TAG, "prepare() failed");
            if(this.getCurrentFileName().startsWith("WELCOME.txt")) {
                this.mainActivity.speak(mainActivity.getResources().getString(R.string.WELCOME));
            } else {
                //TODO Should be moving onto the next song or wait for user's action?
                //TODO Waiting for user's action is better way.
                //mainActivity.cmd(Command.NEXT_SONG);
                this.mainActivity.speak(mainActivity.getResources().getString(R.string.FAIL_TO_PLAY));
                this.isPaused = false;
                this.isPlaying = false;
//                playCnt++;
            }
            Log.i(TAG, e.toString());
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
        Log.i(TAG+"resume()",currentFileIndex + ":" + audibleFiles.size());

        if(mPlayer != null && !mPlayer.isPlaying()){
            mPlayer.start();
            this.isPlaying = true;
            this.isPaused = false;
        }
    }

    public void pause(){
        Log.i(TAG+"pause()",currentFileIndex + ":" + audibleFiles.size());

        if(mPlayer != null && this.isPlaying) {
            mPlayer.pause();
            this.isPlaying = false;
            this.isPaused = true;
        }
    }

    public void stopPlaying() {
        if(mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            this.isPlaying = false;
            this.isPaused = false;
        }
    }

    public boolean isPaused(){
        return this.isPaused;
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

    public String getFullInformation(String title, String trackNumber, String album, String artist){
        String fileInfo = "";
        if( trackNumber == null || title == null || album == null || artist == null) {
            fileInfo = getCurrentFileName();
        } else {
            try {
                fileInfo = "Title: " + title + "\n\nTrack: " + trackNumber
                        + " in album - " + album + " BY " + artist + "\n\nFile: " + getCurrentFileName();
            }catch(Exception e){
                e.printStackTrace();
                Log.i(TAG, e.toString());
            }
        }
        return fileInfo;
    }

    /**
     * TODO When card UI is completed, this Item instance will be fw'ed to card UI component.
     * @return
     */
    public String getCurrentFileDisplayInformation(){
        String fileInfo = "";
        String trackNumber = "";
        String title = "";
        String album = "";
        String artist = "";

        if(this.getCurrentDirectoryName().startsWith(this.folderNameForAllMusicByMediaScanner)) {
            MusicRetriever.Item item = musicRetriever.getCurrentItem();
            return getFullInformation(item.getTitle(), item.getTrackNumber(), item.getAlbum(), item.getArtist());
        }

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        String albumName =
                mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);

        if(audibleFiles.size() > 0) {
            File current = audibleFiles.get(this.currentFileIndex);
            mmr.setDataSource(current.getAbsolutePath());
            trackNumber = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER);
            title = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            album = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
            artist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
            fileInfo = getFullInformation(title, trackNumber,album,artist);
            return fileInfo;
        } else {
            return mainActivity.getResources().getString(R.string.NO_AUDIBLE_FILE_EXIST);
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
     */
    private void readRecursiveDir(File sFile){
        //Log.i(TAG, "inside of recursive:" + sFile.getAbsolutePath());
        File[] mFile = sFile.listFiles();
        for(File file : mFile){
            //Log.i(TAG, "inside of recursive loop:" + file.getAbsolutePath());

            if ( file.isDirectory() && file != null
                    && !file.isHidden()
                    && !file.getName().startsWith("."))
            {
                if( file.equals(this.recordTargetFolder) || file.listFiles(new AudibleFileFilter()).length > 0)
                    directories.add((File)file);
                readRecursiveDir(file);
            }

        }
    }

    /**
     * Target folders
     * 1) folderForAllMusicByMediaScanner under userDataSpace
     * 2) Public Music folder
     * 3) All ExtSDcards
     */
    public void readDirectories(){
        directories = new ArrayList<File>();
        recordTargetFolder = new File(rootFolder.getAbsolutePath()+"/" + this.getRecordingFolderName());

        //Public Music directory.
        //Add only when there is audio files exist.
        if(musicRetriever != null && musicRetriever.getSongCount() > 0)
            directories.add(folderForAllMusicByMediaScanner);

        if(publicMusicFolder.listFiles(new AudibleFileFilter()).length > 0)
            directories.add(publicMusicFolder);
        readRecursiveDir(publicMusicFolder);

        // 1) Adding both extSDCard and read all directories having 1+ audible files
        // 2) Checking if recordTargetFolder exists.
        File dir[] = ContextCompat.getExternalFilesDirs((Context) this.mainActivity, Environment.DIRECTORY_MUSIC);

        for(int i = 0; i < dir.length && dir[i] != null ; i++ ) {
            //Log.i(TAG, "inside readDirectories" + dir[i].getAbsolutePath());
            // Record Target Folder needs to be added when it has no Audible file.
            if (dir[i].listFiles(new AudibleFileFilter()).length > 0) {
                directories.add((File)dir[i]);
            }
            readRecursiveDir(dir[i]);
        }

//        Log.i(TAG, "BEFORE SORTING...");
//
//        for(int i = 0; directories.size() > i ; i++)
//            Log.i(TAG,"Directories:" + directories.get(i).getAbsolutePath());
//
//        Collections.sort(directories);
//
//        Log.i(TAG, "AFTER SORTING...");
//
//        for(int i = 0; directories.size() > i ; i++)
//            Log.i(TAG,"Directories:" + directories.get(i).getAbsolutePath());

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

            Log.i(TAG, "currentDirectoryIndex:" + this.currentDirectoryIndex + ":Audible file size: " + audibleFiles.size()+ currentFolder.getAbsolutePath());
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
        //Adding only Audible Files
        FileFilter aFilter = new AudibleFileFilter();
        Log.i(TAG, "initiateFolder:" + currentFolder.getAbsolutePath());
        this.audibleFiles = new ArrayList(Arrays.asList(this.currentFolder.listFiles(aFilter)));
        this.currentFileIndex = 0;
    }

    public void startRecording() {
        try {
            //Initiate MediaRecorder
            mRecorder = new MediaRecorder();

            mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            // SamplingRate needs to be moved into Settings
            if (Build.VERSION.SDK_INT >= 10) {
                mRecorder.setAudioSamplingRate(16000);
                mRecorder.setAudioEncodingBitRate(96000);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            } else {
                // older version of Android, use crappy sounding voice codec
                // Not that neccessary code here. Or something inside of Exception part.
                mRecorder.setAudioSamplingRate(8000);
                mRecorder.setAudioEncodingBitRate(12200);
                mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
                mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            }
            // In case setting has an option for storage, this part needs to be updated
            String newFileName = recordTargetFolder.getAbsolutePath();
            newFileName += "/" + this.createFileName() + ".3gp";

            Log.i(TAG, "NewFileName:"+newFileName);
            //Log.i(TAG, "currentFileIndex:" + currentFileIndex + ": size" + this.files.size());
            mRecorder.setOutputFile(newFileName);
            mRecorder.prepare();
            mRecorder.start();
            this.isRecording = true;
        } catch (IOException e) {
            Log.i(TAG, e.toString());
            e.printStackTrace();
            // Any exception sets it to False
            // Need to show why it fails.
            this.isRecording = false;
        }

    }

    /**
     * Current code is not returning Removable SD card, instead, it returns the biggest Free space one.
     * @return File
     */
    public File getBiggerExtSDCardDirectory()
    {
        File dir[] = ContextCompat.getExternalFilesDirs((Context) this.mainActivity, Environment.DIRECTORY_MUSIC);
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
            //Log.i(TAG, "Inside of Loop getExternalSDCardDirectory():"+ dir[i].getAbsolutePath() + " : " + dir[i].getTotalSpace());
            if (dir[i].getFreeSpace() > freeSize) {
                targetFile = dir[i];
                freeSize = dir[i].getFreeSpace();
            }
        }

        //Log.i(this.TAG, "targetFile:" + targetFile.getAbsolutePath().toString() + ":" + targetFile.getFreeSpace());
        return   targetFile;
    }

    /**
     * TODO Don't know what else needs to be added more
     */
    public void stopRecording() {
        try {
//            Log.i(TAG, Boolean.toString(this.isRecording) + ":" + Boolean.toString(this.isPlaying));

            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
            isRecording = false;
            // change current Folder and File index to the just saved file.
            moveToLatestRecordedLocation();
        } catch (Exception e){ //No idea what kind of Exception is coming...
            e.printStackTrace();
            this.isRecording = false;
        }

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
                this.currentFileIndex = currentFolder.listFiles(new AudibleFileFilter()).length - 1;
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
            info = "Folder, \"" + getCurrentDirectoryName() + "\" has ";
            info = info + "" + this.musicRetriever.getSongCount() + " audio files automatically scanned by system";
        }else {
            info = "Folder, \"" + getCurrentDirectoryName() + "\" has ";
            info = info + currentFolder.listFiles(new AudibleFileFilter()).length + " audio files and ";
            info = info + Math.round(currentFolder.getFreeSpace() / 1000000) + " megabyte free storage left.\n\n";
            info = info + "This folder is under " + currentFolder.getAbsolutePath();

        }

        return info;
    }

}
