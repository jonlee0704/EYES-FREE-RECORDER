package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import com.jonlee.android.common.logger.Log;

/**
 * Created by jongyeong on 6/14/14.
 */
public class DialView extends View {

    private float centerX;
    private float centerY;
    private float minCircle;
    private float maxCircle;
    private float stepAngle;

    public static final String TAG = "DialView";
    public static final int DIR_MODE_4 = 4;
    public static final int DIR_MODE_8 = 8;

    private int DIAL_COLOR = Color.GRAY;
    private boolean progressBarMode = false;
    private int dialProgress = 0;

    private int dir_mode = DIR_MODE_4;
    private boolean SWIPE_HOLD = false;

    private int maxOfProgress = -1;


    public DialView(Context context) {

        super(context);
        this.setBackgroundColor(Color.BLACK);
        stepAngle = 0.5f;

        /**
         * TODO Considering to implement a specific even Listner for dialer and gesture
         */
        setOnTouchListener(new OnTouchListener() {

            private float startAngle;
            private boolean isDragging;
            private int touchCnt;

            public static final String TAG = "DialView";
            public MainActivity activity = (MainActivity)getContext();

            //TODO Need to optimize THRESHOLD numbers to tell single tap or drag and so on.
            private int SWIPE_MIN_DISTANCE = 25;
            private static final int LONGPRESS_THRESHOLD = 250; //millie seconds
            private static final int DOUBLETAB_THRESHOLD = 100; //millie seconds

            private boolean isFired = false;
            private boolean isLongpress = false;
            //This is for DELETE, FINDING LYRICS ... SWIPE+HOLD
            private boolean isMultiFingerMode = false;

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

            // For ACTION_UP
            private float startX = 0;
            private float startY = 0;
            // To check long-press to turn on Dial Mode
            private long startAction = 0;
            // Checking DoubleTap event
            private int tabCount = 0;
            // To Check DoubleTap event
            private long doubleTapActionStart = 0;

            private boolean isStartedOutsideofCircle = false;



            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // For ACTION_MOVE
                float touchX1 = event.getX();
                float touchY1 = event.getY();

//                /**
//                 * Skipping the batch of event
//                 */
//                if(!isInWholeDiscArea(event.getX(),event.getY())) {
//                    isFired = true;
//                    isStartedOutsideofCircle = true;
//                    return true;
//                }else{
//                    isStartedOutsideofCircle = false;
//                }

//                Log.d("DialView.onTouch:", touchY1 + "");
//                // No action when user intends to quit Emersive mode.
//                if (touchY1 < 200){
//                    isFired = true;
//                    return true;
//                }

                switch (event.getActionMasked()) {

                    case MotionEvent.ACTION_DOWN:
                        // To check gesture direction in ACTION_UP
                        // Set the position when the finger touches the screen
                        startX = event.getX();
                        startY = event.getY();

                        Log.i(TAG, "ACTION_DOWN:"+isInWholeDiscArea(startX,startY));

                        if(!isInWholeDiscArea(startX,startY)) {
                            //isStartedOutsideofCircle = true;
                            return true;
                        }

//                        else {
//                            isStartedOutsideofCircle = false;
//                        }

                        startAngle = touchAngle(touchX1, touchY1);
                        isDragging = isInDiscArea(touchX1, touchY1);

                        // Start time of TouchPress without moving
                        this.startAction = System.currentTimeMillis();


                        // Initiate isFired which set up in ACTION_UP
                        isFired = false;

                        // Initiate Longpress mode
                        isLongpress = false;

                        return true;
                    case MotionEvent.ACTION_MOVE:

                        /**
                         * Ignoring action when it starts from outside of circle
                         * However, it does not matter MOVE_UP points
                         */
                        if(!isInWholeDiscArea(startX,startY))
                            return true;

                        this.touchCnt = event.getPointerCount();
                        //Long Press!!! Not enough movement during threshold time.
                        // Checking only when iLongpress is false.
                        // TODO After observing SJ's error of longpress, THRESHOLD value needs to be optimized.
                        if (!isLongpress && (System.currentTimeMillis() - startAction) > LONGPRESS_THRESHOLD &&
                                (Math.abs(startX - event.getX()) < SWIPE_MIN_DISTANCE && Math.abs(startY - event.getY()) < SWIPE_MIN_DISTANCE)) {
                            //Log.i(TAG, "Touch duration: " + (System.currentTimeMillis() - startAction) +":"+Math.abs(startX-event.getX())+":"+Math.abs(startY-event.getY()));
                            //To make vibration when Longpress is recognized.

                            if(isDragging)
                                this.isLongpress = true;
                            // If event is already fired, skips.
                            if(isFired)
                                return true;

                            if(touchCnt == 1)
                                cmd(Commander.SPEAK_FILE_INFO);
                            else
                                cmd(Commander.SPEAK_DATE_TIME);
                            //cmd(Command.LONG_PRESS);
                            // And speak the file info
                            // activity.getCommander().cmd(Command.SPEAK_FILE_INFO);

                            isFired = true;
                            return true;
                        }

                        // Swipe and Hold cases
                        else if (!isLongpress && (System.currentTimeMillis() - startAction) > LONGPRESS_THRESHOLD &&
                                (Math.abs(startX - event.getX()) > SWIPE_MIN_DISTANCE || Math.abs(startY - event.getY()) > SWIPE_MIN_DISTANCE)) {

                            //This is turned off to enable the feature "swipe and move".
                            //If enable this code, it won't work.
//                            if(isFired)
//                                return false;


                            SWIPE_HOLD = true;
                            int dir = getDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY()));
                            Log.i(TAG, "ACTION_MOVE : " + dir + " :TouchCnt: " + touchCnt);

                            switch (dir) {
                                case BOTTOM_TOP:
                                    if(touchCnt == 3) {
                                        if(!isFired) {
                                            cmd(Commander.START_STOP_BACKUP);
                                            isMultiFingerMode = true;
                                        }
                                    } else if(touchCnt == 1 && !isMultiFingerMode) {
                                        cmd(Commander.REWIND_FOLDER);
                                        cmd(Commander.BREAK_50MS);
                                    }
                                    break;
                                case TOP_BOTTOM:
                                    //Backup StartAndStop by 3-fingers swipe down
                                    if(touchCnt == 3) {
                                        if(!isFired)
                                            //cmd(Commander.START_STOP_BACKUP); isMultiFingerMode = true;
                                            cmd(Commander.SETTINGS); isMultiFingerMode = true;
                                    } else if(touchCnt == 1 && !isMultiFingerMode) {
                                        cmd(Commander.FF_FOLDER);
                                        cmd(Commander.BREAK_50MS);
                                    }
                                    break;
                                case LEFT_RIGHT:
                                    if(touchCnt == 4) {
                                        if(!isFired) {
                                            cmd(Commander.DELETE_FILE); isMultiFingerMode = true;
                                        }
                                    }else if(touchCnt == 2 && !isMultiFingerMode) {
                                        cmd(Commander.FAST_FORWARD_3X);

                                    }else if(!isMultiFingerMode) {
                                        cmd(Commander.FAST_FORWARD_2X);
                                    }
                                    break;
                                case RIGHT_LEFT:
                                    if(touchCnt == 2) {
                                        cmd(Commander.FAST_BACKWARD_3X);

                                    }else {
                                        cmd(Commander.FAST_BACKWARD_2X);
                                    }
                                    break;
                            }

                            isFired = true;
                            return true;

                        }

                        if (isDragging && isLongpress) {

                            float touchAngle = touchAngle(touchX1, touchY1);
                            float deltaAngle = (360 + touchAngle - startAngle + 180) % 360 - 180;
                            //Log.i(TAG,"touchAngle:"+touchAngle+":startAngle:"+startAngle);
                            if (Math.abs(deltaAngle) > stepAngle) {
                                // Dial gesture stops speaking File information
                                ((MainActivity)activity).alwaysSpeak("");

                                int offset = (int) deltaAngle / (int) stepAngle;
                                startAngle = touchAngle;
                                // more offset for playing ff.
                                // Checking only negative or positive.
                                // TODO Want to add some different VIBRATION texture
                                // So just use two if else cases here.
                                // Log.i(TAG, "offset:"+offset);
                                if (offset > 0)
                                    cmd(Commander.FAST_FORWARD_3X);
                                else
                                    cmd(Commander.FAST_BACKWARD_3X);

                                SWIPE_HOLD = true;

                                //onRotate(offset);
//                                Log.i(TAG, "ACTION_MOVE>>touchAngle:"+touchAngle+":startAngle:"+startAngle+":"+event.getPointerCount());
                                isFired = true;
                                return true;
                            }
                        } else{
                            isFired = false;
                            return false;
                        }
                    // Nothing is coming up after ACTION_MOVE
                    // TODO Need to clarify if there is any specific events captured here.
                    case MotionEvent.ACTION_SCROLL:

                        //Log.i(TAG, "ACTION_SCROLL:" + getDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY())));
                        return false;
                    case MotionEvent.ACTION_UP:
                        Log.i(TAG, "ACTION_UP:getXY():"+isInWholeDiscArea(event.getX(),event.getY()));
                        Log.i(TAG, "ACTION_UP:startXY()"+isInWholeDiscArea(startX,startY));

                        //Stop speaking when ACTION_UP
                        ((MainActivity)activity).alwaysSpeak("");

                        //Once it's touch_up, then it's set to false
                        isMultiFingerMode = false;

                        Log.i(TAG, "getDownTime:" + event.getDownTime()+" :getEventTime:"+event.getEventTime());

                        if(SWIPE_HOLD) {
                            cmd(Commander.UPDATE_PLAY_ICON);
                            SWIPE_HOLD = false;
                        }
                        // If event is already fired, skips.
                        if(isFired) {
                            return false;
                        }
//
//
//                        if( !isInWholeDiscArea(startX,startY) || !isInWholeDiscArea(event.getX(),event.getY())) {
//                            return false;
//                        }


                        // If it's too short from DOWN to UP with not enough distance, it's SINGLE TAP.
                        if ((System.currentTimeMillis() - startAction) < LONGPRESS_THRESHOLD  &&
                                (Math.abs(startX-event.getX()) < SWIPE_MIN_DISTANCE && Math.abs(startY-event.getY()) < SWIPE_MIN_DISTANCE)) {

                            // TODO MultiTap implementation
//                            // In case there is first SingleTap, then preparing the second Tab.
                            this.tabCount = tabCount + 1;

                            Log.i(TAG,"tabCount:" + tabCount + " touchCnt:" + touchCnt + ":" + (System.currentTimeMillis() - doubleTapActionStart) + ":" +
                                    Math.abs(startY-event.getY()) +":" + Math.abs(startX-event.getX()) );

                            //Log.i(TAG, event.getDownTime()+":"+event.getEventTime());

                            //TODO when system time is changed back to past,
                            // Below is ALWAYS true.
                            // System.currentTimeMillis() - doubleTapActionStart < LONGPRESS_THRESHOLD
                            // SO... tabCount should be 2 or more... can solve? NO... because it counts from 1.
                            if(tabCount >= 1 && System.currentTimeMillis() - doubleTapActionStart < LONGPRESS_THRESHOLD){
                                this.tabCount = 0;
                                Log.i(TAG, "DOUBLE TAB");
                                cmd(Commander.START_RECORD);
                                return true;
                            } else if(tabCount >= 2 &&
                                    System.currentTimeMillis() - doubleTapActionStart < LONGPRESS_THRESHOLD) {
                                Log.i(TAG, "TRIPLE TAB");
                                //ANY case, it stops recording
                                cmd(Commander.STOP_RECORD);
                                return true;
                            } else if(tabCount >= 5) {
                                Log.i(TAG, "5 more TAB");
                                //ANY case, it stops recording
                                cmd(Commander.STOP_RECORD);
                                return true;
                            }


                            if (System.currentTimeMillis() - doubleTapActionStart >= LONGPRESS_THRESHOLD) {
                                tabCount = 0;
                            } else {
                                //To check multiTab action.
                            }


                            activity.getCommander().cmd(Commander.ONETOUCH);

                            // Reset here?
                            this.doubleTapActionStart = System.currentTimeMillis();
                            return true;
                        }



                        if((Math.abs(startX-event.getX()) > SWIPE_MIN_DISTANCE ||
                                Math.abs(startY-event.getY()) > SWIPE_MIN_DISTANCE)){

                            int dir = 0;

                            // 8 dir mode in Virtual directory(Media scann mode) to navigate ARTIST
                            if (dir_mode == DIR_MODE_4)
                                dir = getDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY()));
                            else if(dir_mode == DIR_MODE_8)
                                dir = getHexDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY()));

                            switch (dir) {
                                case BOTTOM_TOP:
                                    cmd(Commander.PREVIOUS_FOLDER);
                                    break;

                                // Next ARTIST
                                case UP_RIGHT:
                                    cmd(Commander.NEXT_ARTIST);
                                    break;
                                case LEFT_RIGHT:
                                    if(touchCnt == 1) {
                                        cmd(Commander.NEXT_SONG);
                                    }
//                                    else if(touchCnt == 2) {
//                                        cmd(Commander.NEXT_SONG);
//                                        cmd(Commander.NEXT_SONG);
//                                    }
                                    break;
//                                case BOTTOM_RIGHT:
//                                    cmd(Commander.NOTHING);
//                                    break;
                                case TOP_BOTTOM:
                                    //From Top to Bottom
                                    if (touchCnt > 1)
                                        cmd(Commander.START_RECORD);
                                    else if (touchCnt == 1)
                                        cmd(Commander.NEXT_FOLDER);
                                    break;
//                                case BOTTOM_LEFT:
//                                    cmd(Commander.NOTHING);
//                                    break;
                                case RIGHT_LEFT:
                                    if(touchCnt == 1)
                                        cmd(Commander.PREVIOUS_SONG);
                                    break;

                                // PREVIOUS ARTIST
                                case UP_LEFT:
                                    cmd(Commander.PREVIOUS_ARTIST);
                                    break;
                            }
                            //Log.i(TAG,"Direction:" + dir);
                            return true;
                        }
                    case MotionEvent.ACTION_CANCEL:
                        Log.i(TAG, "ACTION_CANCEL:"+isInWholeDiscArea(event.getX(),event.getY()));

                        isDragging = false;
                        isFired = false;
                        isLongpress = false;
                        break;
                }
                return false;
            }

            private void cmd(int c){
                this.isFired = true;
                activity.getCommander().cmd(c);
            }

            private void cmd(int c, boolean isFired){
                if (!isFired)
                    activity.getCommander().cmd(c);
            }

            /**
             * n=8 directions control pad
             * 360-(360/n)/2~(360/n)/2
             * 1 = Down -> Up
             * 2 = Upper right
             * 3 = Left -> Right
             * 4 = Down right
             * 5 = Up -> Down
             * 6 = Down left
             * 7 = Right -> Left
             * 8 = Upper left
             * TODO: Consider better flexibility by different direction number.
             * TODO: Now, it implements only 4 direction controls
             */
            private int getDirection(float angle){
                // n = 45 in case 4 direction
                double n = 45;
                if (angle > 360-n || angle < n){
                    return this.BOTTOM_TOP;
                } else if (angle > n && angle < n*3){
                    return this.LEFT_RIGHT;
                } else if (angle > n*3 && angle < n*5){
                    return this.TOP_BOTTOM;
                } else if (angle > n*5 && angle < n*7){
                    return this.RIGHT_LEFT;
                } else{
                    return this.NO_DIRECTION;
                }

            }

            private int getHexDirection(float angle){
                // n = 22.5 in case 8 direction
                double n = 22.5;
                if (angle > 360-n || angle < n){
                    return this.BOTTOM_TOP;
                } else if (angle > n && angle < n*3){
                    return this.UP_RIGHT;
                } else if (angle > n*3 && angle < n*5){
                    return this.LEFT_RIGHT;
                } else if (angle > n*5 && angle < n*7){
                    return this.BOTTOM_RIGHT;
                } else if (angle > n*7 && angle < n*9){
                    return this.TOP_BOTTOM;
                } else if (angle > n*9 && angle < n*11){
                    return this.BOTTOM_LEFT;
                } else if (angle > n*11 && angle < n*13){
                    return this.RIGHT_LEFT;
                } else if (angle > n*13 && angle < n*15){
                    return this.UP_LEFT;
                } else{
                    return this.NO_DIRECTION;
                }
            }

            /**
             * Return degree
             * @param nowX
             * @param nowY
             * @param centerX
             * @param centerY
             * @return
             */
            private float getDegreeFromCartesian(float nowX, float nowY, float centerX, float centerY)
            {
                //Log.i(TAG, "X1/Y1:"+nowX+"/"+centerX+" Y1/Y2:" + nowY + "/" + centerY);
                float angle = (float) Math.atan2((centerX - nowX), (centerY-nowY));
                float angleindegree = (float) (angle * 180/Math.PI);

                return 180-angleindegree;

            }


        });
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        centerX = getMeasuredWidth() / 2f;
        centerY = getMeasuredHeight() / 2f;
        super.onLayout(changed, l, t, r, b);
    }

    public void setProgressBarMode(boolean b){
        progressBarMode = b;
    }

    public void setDialColor(int color){
        DIAL_COLOR = color;
    }

    public void setDialProgress(int progress){
        dialProgress = progress;
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onDraw(Canvas canvas) {
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        Paint paint = new Paint();

        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(null);
//        LinearGradient linearGradient = new LinearGradient(
//                radius, 0, radius, radius,  Color.GRAY,Color.BLACK, Shader.TileMode.MIRROR);
//        paint.setShader(linearGradient);
//        canvas.drawCircle(centerX, centerY, maxCircle * radius, paint);
//        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
//        canvas.drawCircle(centerX, centerY, minCircle * radius, paint);
        paint.setShader(null);
        //
        paint.setColor(DIAL_COLOR);

//        Paint paint2 = new Paint();
//        paint2.setDither(true);
//        paint2.setAntiAlias(true);
//        paint2.setColor(Color.GRAY);
        //paint2.setColor(Color.parseColor("#0092D5"));

//        for (int i = 0, n =  360 / (int) stepAngle; i < n; i++) {
//            double rad = Math.toRadians((int) stepAngle * i);
//            int startX = (int) (centerX + minCircle * radius * Math.cos(rad));
//            int startY = (int) (centerY + minCircle * radius * Math.sin(rad));
//            int stopX = (int) (centerX + maxCircle * radius * Math.cos(rad));
//            int stopY = (int) (centerY + maxCircle * radius * Math.sin(rad));
//            canvas.drawLine(startX, startY, stopX, stopY, paint);
//        }

        drawDial(canvas, paint);
        super.onDraw(canvas);
    }

    private void drawDial(Canvas canvas, Paint paint){
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        for (int i = 0, n =  360 / (int) stepAngle; i < n; i++) {
            double rad = Math.toRadians((int) stepAngle * i);
            int startX = (int) (centerX + minCircle * radius * Math.cos(rad));
            int startY = (int) (centerY + minCircle * radius * Math.sin(rad));
            int stopX = (int) (centerX + maxCircle * radius * Math.cos(rad));
            int stopY = (int) (centerY + maxCircle * radius * Math.sin(rad));
            canvas.drawLine(startX, startY, stopX, stopY, paint);
        }
    }

    /**
     * Define the step angle in degrees for which the
     * dial will call {@link (int)} event
     * @param angle : angle between each position
     */
    public void setStepAngle(float angle) {
        stepAngle = Math.abs(angle % 360);
    }

    /**
     * Define the draggable disc area with relative circle radius
     * based on min(width, height) dimension (0 = center, 1 = border)
     * @param radius1 : internal or external circle radius
     * @param radius2 : internal or external circle radius
     */
    public void setDiscArea(float radius1, float radius2) {
        radius1 = Math.max(0, Math.min(1, radius1));
        radius2 = Math.max(0, Math.min(1, radius2));
        minCircle = Math.min(radius1, radius2);
        maxCircle = Math.max(radius1, radius2);
    }

    /**
     * Check if touch event is located in disc area
     * @param touchX : X position of the finger in this view
     * @param touchY : Y position of the finger in this view
     */
    private boolean isInDiscArea(float touchX, float touchY) {
        float dX2 = (float) Math.pow(centerX - touchX, 2);
        float dY2 = (float) Math.pow(centerY - touchY, 2);
        float distToCenter = (float) Math.sqrt(dX2 + dY2);
        float baseDist = Math.min(centerX, centerY);
        float minDistToCenter = minCircle * baseDist;
        float maxDistToCenter = maxCircle * baseDist;
        return distToCenter >= minDistToCenter && distToCenter <= maxDistToCenter;
    }


    /**
     * Check if touch event is located in Whole disc circle area
     * @param touchX : X position of the finger in this view
     * @param touchY : Y position of the finger in this view
     */
    private boolean isInWholeDiscArea(float touchX, float touchY) {
        //Ignore the touch on the edge of X
        float dX2 = (float) Math.pow(centerX - touchX, 2);
        float dY2 = (float) Math.pow(centerY - touchY, 2);
        float distToCenter = (float) Math.sqrt(dX2 + dY2);
        float baseDist = Math.min(centerX, centerY);
        //float minDistToCenter = minCircle * baseDist;
        float maxDistToCenter = maxCircle * baseDist;
        Log.i("isInWholeDiscArea", maxDistToCenter + ":" + distToCenter + ":==>" + touchX + "/" + touchY);
        //only 80% for touch
        return distToCenter <= maxDistToCenter*0.9;
    }
    /**
     * Compute a touch angle in degrees from center
     * North = 0, East = 90, West = -90, South = +/-180
     * @param touchX : X position of the finger in this view
     * @param touchY : Y position of the finger in this view
     * @return angle
     */
    private float touchAngle(float touchX, float touchY) {
        float dX = touchX - centerX;
        float dY = centerY - touchY;
        return (float) (270 - Math.toDegrees(Math.atan2(dY, dX))) % 360 - 180;
    }

//    protected abstract void onRotate(int offset);

    /**
     * Return degree
     * @param nowX
     * @param nowY
     * @param centerX
     * @param centerY
     * @return
     */
    private float getDegreeFromCartesian(float nowX, float nowY, float centerX, float centerY)
    {

        float angle = (float) Math.atan2((centerX - nowX), (centerY-nowY));
        float angleindegree = (float) (angle * 180/Math.PI);

        return 180-angleindegree;

    }

    /**
     * Changing the mode of directional pad mode(4dir <-> 8dir)
     */
    public void setDirMode(int mode){
        dir_mode = mode;
    }

    public int getDirMode(){
        return this.dir_mode;
    }

    public boolean isInLayout (){
        return false;
    }

}
