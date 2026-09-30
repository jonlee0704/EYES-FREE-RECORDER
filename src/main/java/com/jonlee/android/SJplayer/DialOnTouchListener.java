/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jonlee.android.SJplayer;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.jonlee.android.common.logger.Log;

public abstract class DialOnTouchListener implements
        View.OnTouchListener,
        GestureDetector.OnDoubleTapListener
{

    public static final String TAG = "GestureListener";
    public MainActivity activity;
    private static int SWIPE_MIN_DISTANCE = 50;
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;
    public int touchCnt = 0;

    private boolean isFired = false;

    private float centerX;
    private float centerY;
    private float minCircle;
    private float maxCircle;
    private float stepAngle;
    private float startAngle;
    private boolean isDragging;

    /**
     * n=8 directions control pad
     * 360-(360/n)/2~(360/n)/2
     * 1 = Bottom -> Top
     * 2 = Upper right
     * 3 = Left -> Right
     * 4 = Bottom right
     * 5 = Top -> Bottom
     * 6 = Bottom left
     * 7 = Right -> Left
     * 8 = Upper left
     */
    private final int NO_DIRECTION = 0;
    private final int BOTTOM_TOP = 1;
    private final int UP_RIGHT = 2;
    private final int LEFT_RIGHT = 3;
    private final int BOTTOM_RIGHT = 4;
    private final int TOP_BOTTOM = 5;
    private final int BOTTOM_LEFT = 6;
    private final int RIGHT_LEFT = 7;
    private final int UP_LEFT = 8;

    /**
     * Vairables for Wheel jog controller
     * start
     */
    private float startPoint;
    private float movePoint;
    private float startDir = -1;
    private float moveDir;

    public DialOnTouchListener(MainActivity a){
        this.activity = a;
        Log.i(TAG,"Constructor...");
    }

    public DialOnTouchListener(){
        Log.i(TAG,"Default Constructor...");
    }
//    // BEGIN_INCLUDE(init_gestureListener)
//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        // Up motion completing a single tap occurred.
//        //Log.i(TAG, "Single Tap Up: ");
//        return true;
//    }
//
//
//    @Override
//    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//        return false;
//    }
//
//    @Override
//    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
//        return false;
//    }
//
//    @Override
//    public void onShowPress(MotionEvent e) {
//        // User performed a down event, and hasn't moved yet.
//        // Set the threshold not to get gesture event.
//        this.SWIPE_MIN_DISTANCE = 1000;
//        activity.cmd(Command.NOTHING);
//    }
//
//    @Override
//    public boolean onDown(MotionEvent e) {
//        // "Down" event - User touched the screen.
//        this.SWIPE_MIN_DISTANCE = 50;
//        Log.i(TAG,"Down: ");
//        this.isFired = false;
//        //Down reset the start Jog event.
//        this.startDir = -1;
//        return true;
//    }


    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        // Since double-tap is actually several events which are considered one aggregate
        // gesture, there's a separate callback for an individual event within the doubletap
        // occurring.  This occurs for down, up, and move.
        //Log.i(TAG, "Event within double tap");
//        cmd(T"Event within double tap");
        ((MainActivity)activity).speak("Double Tab event");

        return false;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        // A confirmed single-tap event has occurred.  Only called when the detector has
        // determined that the first tap stands alone, and is not part of a double tap.
        //Log.i(TAG, "onSingleTapConfirmed");
        ((MainActivity)activity).speak("Single Tab");

        return true;
    }

}
