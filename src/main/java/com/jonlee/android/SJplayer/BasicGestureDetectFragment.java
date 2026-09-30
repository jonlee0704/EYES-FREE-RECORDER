/*
* Copyright (C) 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.jonlee.android.SJplayer;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

public class BasicGestureDetectFragment extends Fragment{


    public MainActivity activity;



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //View gestureView = getActivity().findViewById(R.id.sample_output);
        View gestureView = getActivity().findViewById(R.id.main_layout);

        gestureView.setClickable(true);
        gestureView.setFocusable(true);
//        View dialView = (View)getActivity().findViewById(R.id.dial_view);
//        dialView.setClickable(true);
//        dialView.setFocusable(true);
//        // a step every 20°
//        dialView.setStepAngle(10f);
//        // area from 30% to 100%
//        dialView.setDiscArea(.20f, 1.00f);

        // BEGIN_INCLUDE(init_detector)

        // First create the GestureListener that will include all our callbacks.
        // Then create the GestureDetector, which takes that listener as an argument.
        GestureDetector.OnGestureListener gestureListener = new GestureListener(activity);
        final GestureDetector gd = new GestureDetector(getActivity(), gestureListener);
        gd.setIsLongpressEnabled(false);

        /* For the view where gestures will occur, create an onTouchListener that sends
         * all motion events to the gesture detector.  When the gesture detector
         * actually detects an event, it will use the callbacks you created in the
         * SimpleOnGestureListener to alert your application.
        */

        gestureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gd.onTouchEvent(motionEvent);
                return true;
            }
        });
        // END_INCLUDE(init_detector)
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        if (item.getItemId() == R.id.sample_action) {
//            clearLog();
//        }
        return true;
    }

    public void setActivity(MainActivity a){
        this.activity = a;
    }


}
