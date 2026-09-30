package com.jonlee.android.SJplayer;

import android.widget.TextView;

import com.jonlee.android.common.logger.Log;

import org.w3c.dom.Text;

/**
 * Created by jongyeong on 7/6/14.
 */
public class SJAssist implements Runnable {

    TextView progress_TextView;

    MainActivity mainActivity;
    private String TAG = "SJAssist";

    public SJAssist(MainActivity m){
        this.mainActivity = m;
        progress_TextView = (TextView) m.findViewById(R.id.progress_TextView);
    }

    public void run(){
        mainActivity.getCommander().cmd(Commander.UPDATE_PROGRESS);

        try {
            Thread.sleep(1000);
        }catch (Exception e){
            // In case of Exception, stopping to talk.

            Log.i(TAG, e.toString());
        }
    }
}
