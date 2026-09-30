package com.jonlee.android.common.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Environment;
import androidx.core.content.ContextCompat;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.jonlee.android.SJplayer.AudibleFileFilter;
import com.jonlee.android.SJplayer.MainActivity;
import com.jonlee.android.SJplayer.MusicRetriever;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by jongyeong on 7/11/16.
 */

public class FileUtils {

    private static final String TAG = "FileUtils";

    /**
     * Target folders
     * 1) folderForAllMusicByMediaScanner under userDataSpace
     * 2) Public Music folder
     * 3) All ExtSDcards
     */
    public static byte[] convertFileToByteArray(File f)
    {
        byte[] byteArray = null;
        try
        {
            InputStream inputStream = new FileInputStream(f);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024*8];
            int bytesRead =0;

            while ((bytesRead = inputStream.read(b)) != -1)
            {
                bos.write(b, 0, bytesRead);
            }

            byteArray = bos.toByteArray();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return byteArray;
    }

    //Assuming the 3rd folder are the first of its public music folder
    //
    public static String getSamsungPublicMusicFolderRoot(String fullPathString){
        //Inside of Loop getExternalSDCardDirectory():/storage/extSdCard/Android/data/com.jonlee.android.SJplayer/files/Music Total: 128545980416 Free:64410746880
//        String testStr = "/storage/extSdCard/Android/data/com.jonlee.android.SJplayer/files/Music";
//        fullPathString = testStr;
        String result = "";
        int startMusicFolderIdx = -1;
        int startAndroidFolderIdx = -1;
        startMusicFolderIdx = fullPathString.indexOf("/Music");
        //To compare assumption IDX is going to same with Android location
        //Assuming Music and Android folders are in same level
        //What if it's new blank one and SJ recorder didn't create Android/data folder yet?
        startAndroidFolderIdx = fullPathString.indexOf("/Android");
        result = fullPathString.substring(0, startAndroidFolderIdx) + "/Music";
//
//
//        if(startMusicFolderIdx == -1)
//            return null;

        return result;
    }

    public static String getOrderNumber(String n){
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

    public static Map FileToMap(File targetFile, String splitter){
        Map<String, String> map = new HashMap<String, String>();
        try {
            BufferedReader in = new BufferedReader(new FileReader(targetFile));
            String line = "";
            while ((line = in.readLine()) != null) {
                String parts[] = line.split(splitter);
                map.put(parts[0], parts[1]);
            }
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        return map;
    }

    public static ArrayList FileToArrayList(File targetFile, String splitter){
        ArrayList<File> map = new ArrayList();
        try {
            BufferedReader in = new BufferedReader(new FileReader(targetFile));
            String line = "";
            while ((line = in.readLine()) != null) {
                String parts[] = line.split(splitter);
                map.add(new File(parts[0]));
            }
            in.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        return map;
    }

    public static File getAbsoluteFile(String relativePath, Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return new File(context.getExternalFilesDir(null), relativePath);
        } else {
            return new File(context.getFilesDir(), relativePath);
        }
    }



}
