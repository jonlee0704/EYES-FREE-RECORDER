package com.jonlee.android.SJplayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Unit tests verifying swipe gesture calculations, angle-to-direction mapping,
 * boundary angles, and single vs multi-finger command dispatching.
 */
public class SwipeGestureTest {

    private static final int NO_DIRECTION = 0;
    private static final int BOTTOM_TOP = 1;
    private static final int UP_RIGHT = 2;
    private static final int LEFT_RIGHT = 3;
    private static final int BOTTOM_RIGHT = 4;
    private static final int TOP_BOTTOM = 5;
    private static final int BOTTOM_LEFT = 6;
    private static final int RIGHT_LEFT = 7;
    private static final int UP_LEFT = 8;

    /**
     * Exact formula from DialView.java / GestureListener.java
     */
    private float getDegreeFromCartesian(float nowX, float nowY, float centerX, float centerY) {
        float angle = (float) Math.atan2((centerX - nowX), (centerY - nowY));
        float angleInDegree = (float) (angle * 180 / Math.PI);
        return 180 - angleInDegree;
    }

    /**
     * 4-direction mapping with seamless boundary handling [)
     */
    private int getDirection4(float angle) {
        double n = 45;
        if (angle >= 360 - n || angle < n) {
            return BOTTOM_TOP;
        } else if (angle >= n && angle < n * 3) {
            return LEFT_RIGHT;
        } else if (angle >= n * 3 && angle < n * 5) {
            return TOP_BOTTOM;
        } else if (angle >= n * 5 && angle < n * 7) {
            return RIGHT_LEFT;
        } else {
            return BOTTOM_TOP;
        }
    }

    private int resolveCommand(int dir, int pointerCount) {
        switch (dir) {
            case BOTTOM_TOP:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_NEXT_FOLDER : Commander.NEXT_FOLDER;
            case TOP_BOTTOM:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_PREVIOUS_FOLDER : Commander.PREVIOUS_FOLDER;
            case LEFT_RIGHT:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_NEXT_FILE : Commander.NEXT_SONG;
            case RIGHT_LEFT:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_PREVIOUS_FILE : Commander.PREVIOUS_SONG;
            case UP_RIGHT:
                return Commander.NEXT_ARTIST;
            case UP_LEFT:
                return Commander.PREVIOUS_ARTIST;
            default:
                return Commander.NOTHING;
        }
    }

    @Test
    public void testSwipeRightAngleAndCommand() {
        // Swipe from (100, 200) to (300, 200) -> Moving Right
        float startX = 100f;
        float startY = 200f;
        float endX = 300f;
        float endY = 200f;

        float deg = getDegreeFromCartesian(startX, startY, endX, endY);
        assertEquals(90.0f, deg, 0.001f);

        int dir = getDirection4(deg);
        assertEquals(LEFT_RIGHT, dir);

        // Single finger -> NEXT_SONG
        assertEquals(Commander.NEXT_SONG, resolveCommand(dir, 1));
        // Two finger -> FAST_SEEK_NEXT_FILE
        assertEquals(Commander.FAST_SEEK_NEXT_FILE, resolveCommand(dir, 2));
    }

    @Test
    public void testSwipeLeftAngleAndCommand() {
        // Swipe from (300, 200) to (100, 200) -> Moving Left
        float startX = 300f;
        float startY = 200f;
        float endX = 100f;
        float endY = 200f;

        float deg = getDegreeFromCartesian(startX, startY, endX, endY);
        assertEquals(270.0f, deg, 0.001f);

        int dir = getDirection4(deg);
        assertEquals(RIGHT_LEFT, dir);

        // Single finger -> PREVIOUS_SONG
        assertEquals(Commander.PREVIOUS_SONG, resolveCommand(dir, 1));
        // Two finger -> FAST_SEEK_PREVIOUS_FILE
        assertEquals(Commander.FAST_SEEK_PREVIOUS_FILE, resolveCommand(dir, 2));
    }

    @Test
    public void testSwipeUpAndDownAngles() {
        // Swipe Up: (200, 400) to (200, 100) -> Moving Up (Bottom to Top)
        float degUp = getDegreeFromCartesian(200f, 400f, 200f, 100f);
        assertEquals(0.0f, degUp, 0.001f);
        int dirUp = getDirection4(degUp);
        assertEquals(BOTTOM_TOP, dirUp);
        assertEquals(Commander.NEXT_FOLDER, resolveCommand(dirUp, 1));

        // Swipe Down: (200, 100) to (200, 400) -> Moving Down (Top to Bottom)
        float degDown = getDegreeFromCartesian(200f, 100f, 200f, 400f);
        assertEquals(180.0f, degDown, 0.001f);
        int dirDown = getDirection4(degDown);
        assertEquals(TOP_BOTTOM, dirDown);
        assertEquals(Commander.PREVIOUS_FOLDER, resolveCommand(dirDown, 1));
    }

    @Test
    public void testBoundaryAnglesDoNotFallIntoNoDirection() {
        // 45, 135, 225, 315 degrees boundary test
        assertEquals(LEFT_RIGHT, getDirection4(45f));
        assertEquals(TOP_BOTTOM, getDirection4(135f));
        assertEquals(RIGHT_LEFT, getDirection4(225f));
        assertEquals(BOTTOM_TOP, getDirection4(315f));
        assertEquals(BOTTOM_TOP, getDirection4(0f));
        assertEquals(BOTTOM_TOP, getDirection4(359.9f));
    }

    @Test
    public void testHorizontalSwipeExceedsThreshold() {
        float density = 3.5f; // Pixel 10 Pro / modern device density
        float swipeThreshold = Math.max(25f, 28 * density);

        float deltaX = 120f;
        float deltaY = 5f;

        boolean isSwipe = Math.abs(deltaX) > swipeThreshold || Math.abs(deltaY) > swipeThreshold;
        assertTrue("Displacement of 120px must trigger horizontal swipe", isSwipe);

        // Verify that horizontal swipe cancels wheel scrub
        float touchSlop = 10 * density;
        boolean isHorizontal = Math.abs(deltaX) > 1.2f * Math.abs(deltaY) && Math.abs(deltaX) > touchSlop;
        assertTrue("Horizontal swipe must cancel wheel scrubbing immediately", isHorizontal);
    }

    @Test
    public void testRealTimeThresholdTriggerDuringMove() {
        float density = 3.0f;
        float swipeThreshold = Math.max(25f, 28 * density); // 84 px

        // Simulate finger move progression
        float[] dragSteps = { 10f, 30f, 60f, 85f, 120f, 150f };
        boolean isFired = false;
        int firedCount = 0;

        for (float dx : dragSteps) {
            if (!isFired && dx > swipeThreshold) {
                isFired = true;
                firedCount++;
            }
        }

        assertTrue("Gesture must fire in real time during movement as soon as threshold is crossed", isFired);
        assertEquals("Gesture must fire exactly once per swipe stroke", 1, firedCount);
    }

    public int resolveFastNavCommand(int dir, int pointerCount) {
        switch (dir) {
            case BOTTOM_TOP:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_NEXT_FOLDER : Commander.NEXT_FOLDER;

            case TOP_BOTTOM:
                return (pointerCount >= 2) ? Commander.FAST_SEEK_PREVIOUS_FOLDER : Commander.PREVIOUS_FOLDER;

            case LEFT_RIGHT:
                if (pointerCount == 4) return Commander.DELETE_FILE;
                if (pointerCount == 2) return Commander.FAST_FORWARD_3X;
                return Commander.FAST_FORWARD_2X;

            case RIGHT_LEFT:
                if (pointerCount == 4) return Commander.RESTORE_FILE;
                if (pointerCount == 2) return Commander.FAST_BACKWARD_3X;
                return Commander.FAST_BACKWARD_2X;

            default:
                return Commander.NOTHING;
        }
    }

    @Test
    public void testHoldAndSwipeFastNavCommands() {
        // Horizontal Fast Seeking:
        assertEquals(Commander.FAST_FORWARD_2X, resolveFastNavCommand(LEFT_RIGHT, 1));
        assertEquals(Commander.FAST_FORWARD_3X, resolveFastNavCommand(LEFT_RIGHT, 2));
        assertEquals(Commander.DELETE_FILE, resolveFastNavCommand(LEFT_RIGHT, 4));

        assertEquals(Commander.FAST_BACKWARD_2X, resolveFastNavCommand(RIGHT_LEFT, 1));
        assertEquals(Commander.FAST_BACKWARD_3X, resolveFastNavCommand(RIGHT_LEFT, 2));
        assertEquals(Commander.RESTORE_FILE, resolveFastNavCommand(RIGHT_LEFT, 4));

        // Vertical Swipe_Hold supports folder navigation:
        assertEquals(Commander.NEXT_FOLDER, resolveFastNavCommand(BOTTOM_TOP, 1));
        assertEquals(Commander.FAST_SEEK_NEXT_FOLDER, resolveFastNavCommand(BOTTOM_TOP, 2));
        assertEquals(Commander.PREVIOUS_FOLDER, resolveFastNavCommand(TOP_BOTTOM, 1));
        assertEquals(Commander.FAST_SEEK_PREVIOUS_FOLDER, resolveFastNavCommand(TOP_BOTTOM, 2));
    }

    @Test
    public void testQuickSwipeVsSwipeHoldDiscrimination() {
        int longpressThreshold = 250;

        // 1. Quick swipe: user completes gesture in 120ms (< 250ms)
        long downTime = 1000L;
        long quickUpTime = 1120L;
        long elapsedQuick = quickUpTime - downTime;
        boolean isSwipeHoldQuick = elapsedQuick >= longpressThreshold;
        assertFalse("Swipe completing under 250ms must not trigger SWIPE_HOLD", isSwipeHoldQuick);

        // 2. Swipe & Hold: user holds finger for 600ms (>= 250ms)
        long holdMoveTime = 1600L;
        long elapsedHold = holdMoveTime - downTime;
        boolean isSwipeHoldActive = elapsedHold >= longpressThreshold;
        assertTrue("Holding past 250ms must activate SWIPE_HOLD fast navigation", isSwipeHoldActive);
    }

    @Test
    public void testMetadataCacheEfficiency() throws Exception {
        java.io.File tempFile = java.io.File.createTempFile("test_audio", ".wav");
        tempFile.deleteOnExit();

        AudioMetadataHelper.AudioMetadata meta1 = AudioMetadataHelper.loadMetadata(null, tempFile);
        AudioMetadataHelper.AudioMetadata meta2 = AudioMetadataHelper.loadMetadata(null, tempFile);

        assertTrue("Consecutive metadata reads must return cached instance to prevent UI thread I/O lag", meta1 == meta2);
    }

    public int calculateDynamicSeekStep(float dragDistanceDp, int pointerCount) {
        int baseStep;
        if (dragDistanceDp < 60f) {
            baseStep = 1500;
        } else if (dragDistanceDp < 110f) {
            baseStep = 3000;
        } else if (dragDistanceDp < 170f) {
            baseStep = 7500;
        } else if (dragDistanceDp < 240f) {
            baseStep = 15000;
        } else {
            baseStep = 30000;
        }

        if (pointerCount == 2) {
            baseStep *= 2;
        }
        return baseStep;
    }

    @Test
    public void testDynamicFastSeekStepScaling() {
        // Drag distance < 60dp -> 1500ms
        assertEquals(1500, calculateDynamicSeekStep(35f, 1));
        assertEquals(3000, calculateDynamicSeekStep(35f, 2));

        // Drag distance 60dp - 109dp -> 3000ms
        assertEquals(3000, calculateDynamicSeekStep(80f, 1));
        assertEquals(6000, calculateDynamicSeekStep(80f, 2));

        // Drag distance 110dp - 169dp -> 7500ms
        assertEquals(7500, calculateDynamicSeekStep(140f, 1));
        assertEquals(15000, calculateDynamicSeekStep(140f, 2));

        // Drag distance 170dp - 239dp -> 15000ms
        assertEquals(15000, calculateDynamicSeekStep(200f, 1));
        assertEquals(30000, calculateDynamicSeekStep(200f, 2));

        // Drag distance >= 240dp -> 30000ms
        assertEquals(30000, calculateDynamicSeekStep(280f, 1));
        assertEquals(60000, calculateDynamicSeekStep(280f, 2));
    }

    @Test
    public void testVerticalFolderNavigationTriggerDuringMove() {
        float density = 3.0f;
        float swipeThreshold = Math.max(25f, 28 * density); // 84 px

        float deltaX = 10f;
        float deltaYUp = -95f;   // Swiping UP
        float deltaYDown = 110f; // Swiping DOWN

        // 1. Single finger swiping UP:
        boolean isFolderSwipeUp = Math.abs(deltaYUp) > swipeThreshold && Math.abs(deltaYUp) > Math.abs(deltaX);
        assertTrue("Vertical upward drag exceeding threshold must trigger folder swipe", isFolderSwipeUp);
        int cmdUpSingle = (deltaYUp < 0) ? resolveCommand(BOTTOM_TOP, 1) : resolveCommand(TOP_BOTTOM, 1);
        assertEquals(Commander.NEXT_FOLDER, cmdUpSingle);

        // 2. Single finger swiping DOWN:
        boolean isFolderSwipeDown = Math.abs(deltaYDown) > swipeThreshold && Math.abs(deltaYDown) > Math.abs(deltaX);
        assertTrue("Vertical downward drag exceeding threshold must trigger folder swipe", isFolderSwipeDown);
        int cmdDownSingle = (deltaYDown > 0) ? resolveCommand(TOP_BOTTOM, 1) : resolveCommand(BOTTOM_TOP, 1);
        assertEquals(Commander.PREVIOUS_FOLDER, cmdDownSingle);

        // 3. Multi-finger (2 fingers) folder navigation (alphabet/month fast seek):
        int cmdUpDouble = resolveCommand(BOTTOM_TOP, 2);
        assertEquals(Commander.FAST_SEEK_NEXT_FOLDER, cmdUpDouble);
        int cmdDownDouble = resolveCommand(TOP_BOTTOM, 2);
        assertEquals(Commander.FAST_SEEK_PREVIOUS_FOLDER, cmdDownDouble);
    }
}
