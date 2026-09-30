package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by jongyeong on 6/25/14.
 */
public class ImageFileFilter implements FileFilter {

    private final String TAG = "ImageFileFilter";
    // No need to loop array to compare, right?
    // TODO maybe need to find a better way later.
    private String extensions = "[.jpg,.png,.gif,]";


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

}
