package com.jonlee.android.SJplayer;

import java.io.File;
import java.net.URI;

/**
 * Created by jongyeong on 7/1/14.
 */
public class SJFile extends File {

    // The folder name displays in SJ Player
    private String displayName;
    // MediaScanned folder = true
    private boolean isVirtualDirectory;

    public SJFile(File dir, String name) {
        super(dir, name);
    }

    public SJFile(String path) {
        super(path);
    }

    public SJFile(String dirPath, String name) {
        super(dirPath, name);
    }

    public SJFile(URI uri) {
        super(uri);
    }

    public void setDisplayName(String name){
        this.displayName = name;
    }

    public String getName(){
        if(this.displayName == null || this.displayName == "")
            return super.getName();
        else
            return this.displayName;
    }
}
