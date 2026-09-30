package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;

import com.jonlee.android.common.utils.MimeUtils;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by jongyeong on 6/25/14.
 */
public class AudibleFileFilter implements FileFilter {

    @SuppressLint("DefaultLocale")
    @Override
    public boolean accept(File file) {
        if (file == null || file.isDirectory() || file.isHidden()) {
            return false;
        }
        String name = file.getName();
        if (name == null || name.startsWith(".") || !MimeUtils.isAudioFile(name)) {
            return false;
        }
        return file.length() >= 1000;
    }

    public String getFileExtension( String fileName ) {
        String fName;
        int i = fileName.lastIndexOf('.');
        if (i > 0) {
            fName = fileName.substring(i+1);
        } else
            fName = null;
        return fName;
    }

    /**
     * Files formats currently supported by Library
     */
    public enum SupportedFileFormat {
        _3GP("3gp"), MP4("mp4"), M4A("m4a"), AAC("aac"), FLAC("flac"), MP3(
                "mp3"), MID("mid"), OGG("ogg"), MKV("mkv"), WAV("wav");

        private String filesuffix;

        SupportedFileFormat(String filesuffix) {
            this.filesuffix = filesuffix;
        }

        @Override
        public String toString() {
            return filesuffix;
        }
    }
}
