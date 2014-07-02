package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;

import com.jonlee.android.common.logger.Log;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * Created by jongyeong on 6/25/14.
 */
public class AudibleFileFilter implements FileFilter {

    private final String TAG = "AudibleFileFilter";
    // No need to loop array to compare, right?
    // TODO maybe need to find a better way later.
    private String extensions = "[.mp4,.3gp,.m4a,.aac,.ts,.flac,.mp3,.mid,.xmf,.mxmf,.rtttl,.rtx,.ota,.imy,.ogg,.mkv,.wav]";

//    @Override
//    public boolean accept(File dir, String name) {
//        if(name.lastIndexOf('.')>0)
//        {
//            // get last index for '.' char
//            int lastIndex = name.lastIndexOf('.');
//
//            // get extension
//            String str = name.substring(lastIndex);
//
//            // match path name extension
//            if(str.equals(".txt"))
//            {
//                return true;
//            }
//        }
//        return false;
//    }
//
    @SuppressLint("DefaultLocale")
    @Override
    public boolean accept(File file) {
        boolean isSupport = false;
        String ext = getFileExtension(file.getName());
        //TODO Directory inclusion or not?
        if(file == null || ext == null || file.isDirectory() || file.isHidden() || file.getName().startsWith("."))
            return false;

//        Log.i(TAG, "this.extensions.indexOf:"+this.extensions.indexOf("."+ext));

        if (this.extensions.toUpperCase().indexOf("."+ext.toUpperCase()) > 0)
            return true;

        // Default return false;
        return isSupport;

    }

    public String getFileExtension( String fileName ) {
        String fName;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fName = fileName.substring(i+1);
        } else
            fName = null;

//        Log.i(TAG, "EXT:" + fName);
        return fName;
    }

    /**
     * Files formats currently supported by Library
     */
    public static enum SupportedFileFormat {
        _3GP("3gp"), MP4("mp4"), M4A("m4a"), AAC("aac"), TS("ts"), FLAC("flac"), MP3(
                "mp3"), MID("mid"), XMF("xmf"), MXMF("mxmf"), RTTTL("rtttl"), RTX(
                "rtx"), OTA("ota"), IMY("imy"), OGG("ogg"), MKV("mkv"), WAV(
                "wav");

        private String filesuffix;

        SupportedFileFormat(String filesuffix) {
            this.filesuffix = filesuffix;
        }

        public String toString() {
            return filesuffix;
        }
    }
}
