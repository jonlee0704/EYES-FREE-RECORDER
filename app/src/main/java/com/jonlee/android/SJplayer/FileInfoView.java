package com.jonlee.android.SJplayer;

import com.jonlee.android.common.logger.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by jongyeong on 6/15/14.
 * TODO This View will be used for displaying any details of file info including location, lilycs and so on.
 */
public class FileInfoView {

    final public static String TAG = "FileInfoView";

    /**
     *
     * @param lyricsFolder is under PUBLIC MUSIC FOLDER/.Lyrics/
     * @param audioFileName
     * @return
     */
    public static String getLyric(File lyricsFolder, String audioFileName){
        StringBuffer lyric = new StringBuffer("");
        //Checking if same-file-name.lyric or same-file-name.txt exist in ROOT_FOLDER/.lyrics/

        File lyricFile = new File(lyricsFolder.getAbsolutePath()+"/"+audioFileName +".txt");

        Log.i(TAG, "Lyrics:" + lyricFile.getAbsolutePath()+ ":exist?" + lyricFile.exists());

        if(lyricFile.exists()){
            try {
                BufferedReader br = new BufferedReader(new FileReader(lyricFile));
                String line;

                while ((line = br.readLine()) != null) {
                    lyric.append(line);
                    lyric.append('\n');
                }
            }
            catch (IOException e) {
                //You'll need to add proper error handling here
                e.printStackTrace();
            }
        }

        return lyric.toString();
    }

    public static String removeFileExtension( String fileName ) {
        String fName;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fName = fileName.substring(0,i);
        } else
            fName = null;

        Log.i(TAG, "EXT:" + fName);
        return fName;
    }
}
