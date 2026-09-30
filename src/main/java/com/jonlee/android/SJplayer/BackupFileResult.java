package com.jonlee.android.SJplayer;

import com.google.api.services.drive.model.File;

import java.util.HashMap;

/**
 * Created by jongyeong on 7/13/16.
 */

public class BackupFileResult {

    public static int PROGRESS = 0;
    public static int SUCCESS = 1;
    public static int FAIL = 2;
    public static int SKIP = 3;

    private com.google.api.services.drive.model.File file;
    private String fileName;
    private String fileId;
    private String parentFolderName;
    private String backupResultDescription = "";

    /**
     * 0: Process
     * 1: SUCCESS
     * 2: FAIL by unknown exception, to check LOGs
     * 3: SKIP by existing in Drive
     */
    private int status = -1;
    private boolean isSuccess;
    private int logNumber;
    private String timeStamp;

    private int size = 0;
    private boolean isFolder = true;

    private int emoji;

    BackupFileResult(com.google.api.services.drive.model.File file){

        this.file = file;

    }

    BackupFileResult(com.google.api.services.drive.model.File file, String folderName){

        this.file = file;
        this.fileName = file.getName();
        if(file.getMimeType().toLowerCase().indexOf("folder") != -1){
            this.isFolder = false;
        }else{
            this.isFolder = true;
        }
        this.parentFolderName = folderName;

    }

    BackupFileResult(String fileName, int size, boolean isFolder){
        this.fileName = fileName;
        this.size = size;
        this.isFolder = true;
    }

    BackupFileResult(com.google.api.services.drive.model.File file, boolean isSuccess,
                        int logNumber, String timeStamp){
        this.file = file;
        this.isSuccess = isSuccess;
        this.logNumber = logNumber;
        this.timeStamp = timeStamp;
    }

    BackupFileResult(String fileName, String fileId, boolean isSuccess,
                        int logNumber, String timeStamp){
        this.fileName = fileName;
        this.fileId = fileId;
        this.isSuccess = isSuccess;
        this.logNumber = logNumber;
        this.timeStamp = timeStamp;
    }


    BackupFileResult(String fileName, String fileId, int size, boolean isFolder){
        this.fileName = fileName;
        this.size = size;
        this.isFolder = true;
    }

    public String toString(){
//        String result = fileName;
//        if(status == 0)
//            result = fileName + "(" + size + "): NO_UPDATE";
//        else if(status == 1)
//            result = fileName + "(" + size + "): CREATED in Drive";
//        else if(status == 2)
//            result = fileName + "(" + size + "): FAILED by exception";
//        else if(status == 3)
//            result = fileName + "(" + size + "): EXISTING Folder";
//        else
//            result = fileName + "(" + size + "): UNKNOWN";

        if(fileName == null & file != null ) {
            if (parentFolderName.isEmpty()){
                fileName = file.getName();
            }else {
                fileName = parentFolderName + "/" + file.getName();

            }
        }
        return fileName;
    }

    /**
     * 0: Process
     * 1: SUCCESS
     * 2: FAIL by unknown exception, to check LOGs
     * 3: SKIP by existing in Drive
     */
    public int getStatusImage(){
        int result;
        if(status == 0)
            result = R.drawable.ic_sync_black_24dp;
        else if(status == 1)
            result = R.drawable.ic_cloud_done_black_24dp;
        else if(status == 2)
            result = R.drawable.ic_sync_problem_black_24dp;
        else if(status == 3)
            result = R.drawable.ic_cloud_circle_black_24dp;
        else if(status == -1)
            result = R.drawable.ic_cloud_queue_black_24dp;
        else
            result = R.drawable.ic_cloud_black_24dp;

        return result;
    }

    public void setFileName(String str){
        this.fileName = str;
    }

    public void setStatus(int status){
        this.status = status;
    }

    public void isFolder(boolean isFolder){
        this.isFolder = isFolder;
    }

    public void setSize(int i){
        this.size = i;
    }

    public int getSize(){
        return size;
    }

    public File getFile(){
        return this.file;
    }

    public String getFileName(){
        return this.fileName;
    }

    public String getFileId(){
        return this.fileId;
    }

    public String getTimeStamp(){
        return this.timeStamp;
    }

    public boolean isSuccess(){
        return this.isSuccess;
    }

    public int getLogNumber(){
        return this.logNumber;
    }

    public void setParentFolderName(String f){
        parentFolderName = f;
    }

    public String getParentFolderName(){
        return parentFolderName;
    }
//
//    public String getDescriptionForListView(){
//        String mString = "";
//        if(isFolder)
//            mString = mString + new String(Character.toChars(0x1F4C1)); //Folder Emoji
//        else
//            mString = mString + new String(Character.toChars(0x1F5D2)); //File Emoji
//
//        mString = mString + " " + this.fileName;
//
//        if(status == S)
//
//
//
//
//        return ( new String(Character.toChars(0x1F4C1)) //Folder
//                        + " " + localFolderName
//                        +"\n" + new String(Character.toChars(0x1F4E4))
//                        + " total:" + mBackupFileResultArray[i].getSize()
//                        + ", new:" + newInFolder + ", skip:" + skipInFolder
//                        + ", fail:" + failInFolder)
//    }

    public void setBackupResultDescription(String description){
        this.backupResultDescription = description;
    }

    public String getBackupResultDescription(){
        return backupResultDescription;
    }
}
