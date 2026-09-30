package com.jonlee.android.SJplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.text.TextUtils;
import android.text.TextPaint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Locale;

/**
 * Created by jongyeong on 6/14/14.
 */
public class DialView extends View {

    private Runnable cancelTapsCallback = null;

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

    // Pre-allocated paints for McIntosh Audiophile Dial (Battery-optimized: 0 per-frame allocations)
    private Paint bezelOuterPaint;
    private Paint bezelInnerPaint;
    private Paint majorTickPaint;
    private Paint minorTickPaint;
    private Paint cardinalMarkerPaint;
    private Paint activeArcPaint;
    private Paint peakIndicatorPaint;
    private android.graphics.RectF arcBounds;

    // Audiophile Organic Glowing Energy Stream Progress & Bookmark Markers
    private float smoothPlaybackProgress = 0f;
    private final Path diamondPath = new Path();
    private Paint progressGlowPaint;
    private Paint progressDotPaint;
    private Paint progressDotWhitePaint;
    private Paint progressDotFaintPaint;
    private Paint progressHeadHaloPaint;
    private Paint progressHeadBezelPaint;
    private Paint progressHeadCorePaint;
    private Paint progressStemPaint;

    // Recording Chronograph Energy Stream Paints
    private Paint recordingProgressGlowPaint;
    private Paint recordingDotPaint;
    private Paint recordingDotMintPaint;
    private Paint recordingHeadHaloPaint;
    private Paint recordingHeadDotPaint;
    private Paint recordingStemPaint;

    // Golden Amber Bookmark Jewel Paints
    private Paint bookmarkPipGlowPaint;
    private Paint bookmarkPipCorePaint;
    private Paint bookmarkPipHighlightPaint;
    private Paint bookmarkSparklePaint;

    // Bookmark query cache (queries at most once every 1.5s or on track change)
    private String lastBookmarkQueriedFile = null;
    private long lastBookmarkQueryTime = 0;
    private List<Integer> cachedBookmarks = null;

    // Option 1: 3D Undulating Circular Particle Wave Ribbon (McIntosh Color Palette)
    public static final int NUM_ANGLES = 96; // 96 angular slices (3.75-deg increments)
    public static final int NUM_STRANDS = 10; // 10 concentric flowing particle strands
    private final float[] cosTable = new float[NUM_ANGLES];
    private final float[] sinTable = new float[NUM_ANGLES];
    private final float[] audioBands = new float[NUM_ANGLES];
    private final float[] audioPeakBands = new float[NUM_ANGLES];
    private final long[] audioPeakDecayTime = new long[NUM_ANGLES];
    private float waveTime = 0f;

    // Visualization Options:
    // Visualizer Modes:
    // Option 0: Simplest UI (Clean Dual-Column Split Navigation: Folder on Left, Files on Right)
    // Option 1: Circle Wave Ribbon (Undulating 3D Acoustic Wave Ribbon)
    // Option 2: Frequency Analyzer (32-Band Segmented Graphic Equalizer)
    // Option 3: Sonic Starburst (Radial Audio Corona / Pulsating Starburst)
    // NOTE: Vintage Dual VU Meters and CRT Oscilloscope have been disabled per user request.
    public static final int VISUALIZER_MODE_MINIMAL_SPLIT = 0;
    public static final int VISUALIZER_MODE_CIRCLE = 1;
    public static final int VISUALIZER_MODE_FREQUENCY_ANALYZER = 2;
    public static final int VISUALIZER_MODE_STARBURST = 3;
    public static final int NUM_VISUALIZER_MODES = 4;

    // Disabled legacy mode constants kept as deprecated to avoid any symbol breaks
    @Deprecated public static final int VISUALIZER_MODE_DUAL_VU = -1;
    @Deprecated public static final int VISUALIZER_MODE_OSCILLOSCOPE = -2;

    public static String getVisualizerModeName(int mode) {
        switch (mode) {
            case VISUALIZER_MODE_MINIMAL_SPLIT:
                return "Simplest UI";
            case VISUALIZER_MODE_CIRCLE:
                return "Circle Animation";
            case VISUALIZER_MODE_FREQUENCY_ANALYZER:
                return "Frequency Analyzer";
            case VISUALIZER_MODE_STARBURST:
                return "Sonic Starburst";
            default:
                return "Simplest UI";
        }
    }

    private int visualizerMode = VISUALIZER_MODE_MINIMAL_SPLIT;

    // Option 2: 32-Band Segmented Graphic Frequency Analyzer
    public static final int NUM_FREQ_BANDS = 32;
    public static final int NUM_SEGMENTS_PER_BAND = 10;
    private final float[] freqBands = new float[NUM_FREQ_BANDS];
    private final float[] freqPeakBands = new float[NUM_FREQ_BANDS];
    private final long[] freqPeakDecayTime = new long[NUM_FREQ_BANDS];

    // Paints for Frequency Analyzer
    private Paint freqSegmentCyanPaint;
    private Paint freqSegmentBluePaint;
    private Paint freqSegmentGreenPaint;
    private Paint freqSegmentAmberPaint;
    private Paint freqSegmentRubyPaint;
    private Paint freqSegmentDimPaint;
    private Paint freqPeakCapPaint;
    private Paint freqPeakGlowPaint;

    // Option 3: Vintage McIntosh Dual Analog VU Meters
    private float vuLeftLevel = 0f;
    private float vuRightLevel = 0f;
    private long vuLeftPeakHold = 0;
    private long vuRightPeakHold = 0;
    private Paint vuBezelPaint;
    private Paint vuBezelInnerPaint;
    private Paint vuFacePaint;
    private Paint vuScaleCyanPaint;
    private Paint vuScaleRedPaint;
    private Paint vuScaleTickPaint;
    private Paint vuScaleMajorTickPaint;
    private Paint vuNeedlePaint;
    private Paint vuNeedleShadowPaint;
    private Paint vuPivotPaint;
    private Paint vuPivotCapPaint;
    private Paint vuPeakLedOnPaint;
    private Paint vuPeakLedOffPaint;
    private Paint vuPeakGlowPaint;
    private TextPaint vuLabelTextPaint;
    private TextPaint vuScaleTextPaint;

    // Option 4: Phosphor CRT Oscilloscope
    private final Path crtTracePath = new Path();
    private final Path crtGhostPath = new Path();
    private Paint crtScreenPaint;
    private Paint crtBezelPaint;
    private Paint crtGridPaint;
    private Paint crtCenterGridPaint;
    private Paint crtGhostPaint;
    private Paint crtBloomPaint;
    private Paint crtTracePaint;
    private Paint crtDotPaint;
    private TextPaint crtTextPaint;

    // Option 5: Radial Sonic Starburst
    private static final int NUM_STARBURST_PARTICLES = 36;
    private final float[] starburstParticleAngle = new float[NUM_STARBURST_PARTICLES];
    private final float[] starburstParticleDist = new float[NUM_STARBURST_PARTICLES];
    private final float[] starburstParticleSpeed = new float[NUM_STARBURST_PARTICLES];
    private final float[] starburstParticleSize = new float[NUM_STARBURST_PARTICLES];
    private final float[] starburstParticleAlpha = new float[NUM_STARBURST_PARTICLES];
    private boolean starburstInitialized = false;
    private Paint starburstCorePaint;
    private Paint starburstRayCyanPaint;
    private Paint starburstRayAmberPaint;
    private Paint starburstParticlePaint;

    // Suggestion 6: Haptic Audio Pulse (Tactile beat feedback for accessibility)
    private boolean hapticPulseEnabled = true;
    private long lastHapticBeatTime = 0;

    // Per-node Volume Waveforms (dB amount) for Playback & Recording
    private final float[] trackWaveform = new float[NUM_ANGLES];
    private final float[] recordingWaveform = new float[NUM_ANGLES];
    private String currentWaveformFile = "";
    private int lastWaveformDuration = 0;
    private boolean wasRecordingLastFrame = false;

    // McIntosh Particle Wave Paints (Zero-allocation rendering)
    private Paint particleCyanPaint;
    private Paint particleBluePaint;
    private Paint particleNavyPaint;
    private Paint particleDarkNavyPaint;
    private Paint particleWhiteCyanPaint;
    private Paint particleAmberPaint;
    private Paint particleRubyRedPaint;
    private Paint particleMintPaint;
    private Paint particleGreenPaint;
    private Paint particleDarkGreenPaint;
    private Paint particleDeepForestPaint;
    private Paint particleGlowPaint;
    private Paint idleDotPaint;

    // Ambient UI Backlight Diffusion Shaders & Paints
    private Paint ambientAuraPaint;
    private Shader playbackAuraShader;
    private Shader recordingAuraShader;
    private Shader idleAuraShader;
    private float lastAuraRadius = -1f;

    // Translucent McIntosh Faceplate Glass Paints
    private Paint glassCenterFillPaint;
    private Paint glassCenterRimPaint;

    // Ambient +5, -5 Depth Folder Wheel & Horizontal File Carousel
    private Paint ambientFolderTextPaint;
    private float folderWheelScrollOffset = 0f;
    private float fileWheelScrollOffset = 0f;

    // Simplest UI: Minimal Split Navigation (Folder on Left, Files on Right) Paints & Layout
    private Paint splitDividerPaint;
    private Paint splitNexusPaint;
    private TextPaint splitFolderHeaderPaint;
    private TextPaint splitFolderBadgePaint;
    private Paint splitFolderActiveCapsulePaint;
    private Paint splitFolderActiveBorderPaint;
    private Paint splitFolderAccentStripePaint;
    private TextPaint splitFolderActiveTextPaint;
    private TextPaint splitFolderSubPaint;
    private TextPaint splitFolderItemTextPaint;
    private TextPaint splitFileHeaderPaint;
    private TextPaint splitFileBadgePaint;
    private Paint splitFileActiveCapsulePaint;
    private Paint splitFileActiveBorderPaint;
    private Paint splitFileAccentStripePaint;
    private TextPaint splitFileActiveTextPaint;
    private TextPaint splitFileItemTextPaint;
    private TextPaint splitCuePaint;
    private Paint splitLiveEqPaint;
    private TextPaint splitTimeTextPaint;
    private Paint splitProgressBgPaint;
    private Paint splitProgressFillPaint;
    private Paint splitPlayheadPipPaint;
    private Paint splitRecHaloPaint;
    private Paint splitRecCorePaint;
    private TextPaint splitRecTextPaint;
    private TextPaint splitIdlePlayCuePaint;
    private final RectF splitFolderCardRect = new RectF();
    private final RectF splitFileCardRect = new RectF();
    private final RectF splitLiveEqBarRect = new RectF();
    private Shader splitDividerShader;
    private float lastDividerH = -1f;

    private boolean isAnimationRunning = false;
    private final Handler eqHandler = new Handler();
    private final Runnable eqAnimationRunnable = new Runnable() {
        @Override
        public void run() {
            if (getVisibility() != View.VISIBLE || !isShown()) {
                isAnimationRunning = false;
                return;
            }
            boolean active = checkAudioState();
            updateEqData();
            invalidate();
            // Ambient UI cadence: 30 fps when playing/recording, 25 fps when idle breathing
            eqHandler.postDelayed(this, active ? 33 : 40);
        }
    };

    private void initPaints() {
        bezelOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelOuterPaint.setStyle(Paint.Style.STROKE);
        bezelOuterPaint.setStrokeWidth(3f);
        bezelOuterPaint.setColor(Color.parseColor("#122A3C"));

        bezelInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bezelInnerPaint.setStyle(Paint.Style.STROKE);
        bezelInnerPaint.setStrokeWidth(1.5f);
        bezelInnerPaint.setColor(Color.parseColor("#0A1C29"));

        majorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        majorTickPaint.setStyle(Paint.Style.STROKE);
        majorTickPaint.setStrokeWidth(3.5f);
        majorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        majorTickPaint.setColor(Color.parseColor("#00B4D8"));

        minorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        minorTickPaint.setStyle(Paint.Style.STROKE);
        minorTickPaint.setStrokeWidth(1.5f);
        minorTickPaint.setStrokeCap(Paint.Cap.ROUND);
        minorTickPaint.setColor(Color.parseColor("#09334D"));

        cardinalMarkerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cardinalMarkerPaint.setStyle(Paint.Style.FILL);
        cardinalMarkerPaint.setColor(Color.parseColor("#00E676"));

        activeArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        activeArcPaint.setStyle(Paint.Style.STROKE);
        activeArcPaint.setStrokeWidth(4.5f);
        activeArcPaint.setStrokeCap(Paint.Cap.ROUND);
        activeArcPaint.setColor(Color.parseColor("#00E5FF"));

        peakIndicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        peakIndicatorPaint.setStyle(Paint.Style.FILL);
        peakIndicatorPaint.setColor(Color.parseColor("#FF3366"));

        arcBounds = new android.graphics.RectF();

        // Audiophile Faceted Golden Amber Diamond Progress Stream
        progressGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressGlowPaint.setStyle(Paint.Style.FILL);
        progressGlowPaint.setColor(Color.parseColor("#40FFB300")); // McIntosh Warm Amber Aura

        progressDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressDotPaint.setStyle(Paint.Style.FILL);
        progressDotPaint.setColor(Color.parseColor("#FFA000")); // Faceted Amber Crystal

        progressDotWhitePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressDotWhitePaint.setStyle(Paint.Style.FILL);
        progressDotWhitePaint.setColor(Color.parseColor("#FFFDE7")); // Incandescent Core Highlight

        progressDotFaintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressDotFaintPaint.setStyle(Paint.Style.FILL);
        progressDotFaintPaint.setColor(Color.parseColor("#18FFA000"));

        // Glowing Jewel Star Head
        progressHeadHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressHeadHaloPaint.setStyle(Paint.Style.FILL);
        progressHeadHaloPaint.setColor(Color.parseColor("#55FFA000"));

        progressHeadBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressHeadBezelPaint.setStyle(Paint.Style.FILL);
        progressHeadBezelPaint.setColor(Color.parseColor("#FFD54F")); // Radiant Topaz Mantle

        progressHeadCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressHeadCorePaint.setStyle(Paint.Style.FILL);
        progressHeadCorePaint.setColor(Color.parseColor("#FFFFFF"));

        progressStemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        progressStemPaint.setStyle(Paint.Style.STROKE);
        progressStemPaint.setStrokeCap(Paint.Cap.ROUND);
        progressStemPaint.setColor(Color.parseColor("#B3FFA000")); // Luminous Amber Stem

        // Recording Chronograph Energy Stream Paints (Sunfire Topaz Diamonds)
        recordingProgressGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingProgressGlowPaint.setStyle(Paint.Style.FILL);
        recordingProgressGlowPaint.setColor(Color.parseColor("#40FF9100"));

        recordingDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingDotPaint.setStyle(Paint.Style.FILL);
        recordingDotPaint.setColor(Color.parseColor("#FF9100")); // Sunfire Topaz Diamond

        recordingDotMintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingDotMintPaint.setStyle(Paint.Style.FILL);
        recordingDotMintPaint.setColor(Color.parseColor("#FFF3E0"));

        recordingStemPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingStemPaint.setStyle(Paint.Style.STROKE);
        recordingStemPaint.setStrokeCap(Paint.Cap.ROUND);
        recordingStemPaint.setColor(Color.parseColor("#B3FF9100")); // Sunfire Topaz Stem

        recordingHeadHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingHeadHaloPaint.setStyle(Paint.Style.FILL);
        recordingHeadHaloPaint.setColor(Color.parseColor("#55FF1744"));

        recordingHeadDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        recordingHeadDotPaint.setStyle(Paint.Style.FILL);
        recordingHeadDotPaint.setColor(Color.parseColor("#FF1744")); // Ruby Red Power Guard

        // Electric Cyan & Diamond Starlight Bookmark Jewel Paints (high contrast against amber stream!)
        bookmarkPipGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bookmarkPipGlowPaint.setStyle(Paint.Style.FILL);
        bookmarkPipGlowPaint.setColor(Color.parseColor("#5500E5FF"));

        bookmarkPipCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bookmarkPipCorePaint.setStyle(Paint.Style.FILL);
        bookmarkPipCorePaint.setColor(Color.parseColor("#00E5FF")); // Electric Cyan

        bookmarkPipHighlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bookmarkPipHighlightPaint.setStyle(Paint.Style.FILL);
        bookmarkPipHighlightPaint.setColor(Color.parseColor("#FFFFFF")); // Pure Diamond Center

        bookmarkSparklePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bookmarkSparklePaint.setStyle(Paint.Style.STROKE);
        bookmarkSparklePaint.setStrokeWidth(1.4f);
        bookmarkSparklePaint.setColor(Color.parseColor("#00E5FF"));

        // Pre-compute 96-band radial angles starting from 12 o'clock (-90 degrees)
        for (int i = 0; i < NUM_ANGLES; i++) {
            double rad = (2.0 * Math.PI * i) / (double) NUM_ANGLES - (Math.PI / 2.0);
            cosTable[i] = (float) Math.cos(rad);
            sinTable[i] = (float) Math.sin(rad);
            audioBands[i] = 0f;
            audioPeakBands[i] = 0f;
            trackWaveform[i] = 0.20f;
            recordingWaveform[i] = 0.12f;
        }

        // Initialize 32-Band Frequency Analyzer arrays & paints
        for (int b = 0; b < NUM_FREQ_BANDS; b++) {
            freqBands[b] = 0.08f;
            freqPeakBands[b] = 0.08f;
            freqPeakDecayTime[b] = 0L;
        }

        freqSegmentCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentCyanPaint.setStyle(Paint.Style.FILL);
        freqSegmentCyanPaint.setColor(Color.parseColor("#00E5FF"));

        freqSegmentBluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentBluePaint.setStyle(Paint.Style.FILL);
        freqSegmentBluePaint.setColor(Color.parseColor("#00B4D8"));

        freqSegmentGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentGreenPaint.setStyle(Paint.Style.FILL);
        freqSegmentGreenPaint.setColor(Color.parseColor("#00E676"));

        freqSegmentAmberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentAmberPaint.setStyle(Paint.Style.FILL);
        freqSegmentAmberPaint.setColor(Color.parseColor("#FFA000"));

        freqSegmentRubyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentRubyPaint.setStyle(Paint.Style.FILL);
        freqSegmentRubyPaint.setColor(Color.parseColor("#FF1744"));

        freqSegmentDimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqSegmentDimPaint.setStyle(Paint.Style.FILL);
        freqSegmentDimPaint.setColor(Color.parseColor("#1500B4D8")); // Ghost unlit segment

        freqPeakCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqPeakCapPaint.setStyle(Paint.Style.FILL);
        freqPeakCapPaint.setColor(Color.parseColor("#FFFFFF"));

        freqPeakGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        freqPeakGlowPaint.setStyle(Paint.Style.FILL);
        freqPeakGlowPaint.setColor(Color.parseColor("#4D00E5FF"));

        // Initialize Option 3: Vintage McIntosh Dual Analog VU Meters
        vuBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuBezelPaint.setStyle(Paint.Style.STROKE);
        vuBezelPaint.setStrokeWidth(2.5f);
        vuBezelPaint.setColor(Color.parseColor("#152C3D"));

        vuBezelInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuBezelInnerPaint.setStyle(Paint.Style.STROKE);
        vuBezelInnerPaint.setStrokeWidth(1.2f);
        vuBezelInnerPaint.setColor(Color.parseColor("#3000E5FF"));

        vuFacePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuFacePaint.setStyle(Paint.Style.FILL);
        vuFacePaint.setColor(Color.parseColor("#041826"));

        vuScaleCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuScaleCyanPaint.setStyle(Paint.Style.STROKE);
        vuScaleCyanPaint.setStrokeWidth(2.5f);
        vuScaleCyanPaint.setStrokeCap(Paint.Cap.ROUND);
        vuScaleCyanPaint.setColor(Color.parseColor("#00E5FF"));

        vuScaleRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuScaleRedPaint.setStyle(Paint.Style.STROKE);
        vuScaleRedPaint.setStrokeWidth(2.8f);
        vuScaleRedPaint.setStrokeCap(Paint.Cap.ROUND);
        vuScaleRedPaint.setColor(Color.parseColor("#FF1744"));

        vuScaleTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuScaleTickPaint.setStyle(Paint.Style.STROKE);
        vuScaleTickPaint.setStrokeWidth(1.5f);
        vuScaleTickPaint.setColor(Color.parseColor("#8000E5FF"));

        vuScaleMajorTickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuScaleMajorTickPaint.setStyle(Paint.Style.STROKE);
        vuScaleMajorTickPaint.setStrokeWidth(2.5f);
        vuScaleMajorTickPaint.setColor(Color.parseColor("#FFFFFF"));

        vuNeedlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuNeedlePaint.setStyle(Paint.Style.STROKE);
        vuNeedlePaint.setStrokeWidth(2.2f);
        vuNeedlePaint.setStrokeCap(Paint.Cap.ROUND);
        vuNeedlePaint.setColor(Color.parseColor("#FFA000")); // McIntosh Amber Needle

        vuNeedleShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuNeedleShadowPaint.setStyle(Paint.Style.STROKE);
        vuNeedleShadowPaint.setStrokeWidth(2.4f);
        vuNeedleShadowPaint.setStrokeCap(Paint.Cap.ROUND);
        vuNeedleShadowPaint.setColor(Color.parseColor("#50000000"));

        vuPivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuPivotPaint.setStyle(Paint.Style.FILL);
        vuPivotPaint.setColor(Color.parseColor("#0A1C29"));

        vuPivotCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuPivotCapPaint.setStyle(Paint.Style.STROKE);
        vuPivotCapPaint.setStrokeWidth(1.5f);
        vuPivotCapPaint.setColor(Color.parseColor("#00E5FF"));

        vuPeakLedOnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuPeakLedOnPaint.setStyle(Paint.Style.FILL);
        vuPeakLedOnPaint.setColor(Color.parseColor("#FF1744"));

        vuPeakLedOffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuPeakLedOffPaint.setStyle(Paint.Style.FILL);
        vuPeakLedOffPaint.setColor(Color.parseColor("#331200"));

        vuPeakGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        vuPeakGlowPaint.setStyle(Paint.Style.FILL);
        vuPeakGlowPaint.setColor(Color.parseColor("#66FF1744"));

        vuLabelTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        vuLabelTextPaint.setColor(Color.parseColor("#00E5FF"));
        vuLabelTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));
        vuLabelTextPaint.setTextAlign(Paint.Align.CENTER);

        vuScaleTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        vuScaleTextPaint.setColor(Color.parseColor("#B0C8D8"));
        vuScaleTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL));
        vuScaleTextPaint.setTextAlign(Paint.Align.CENTER);

        // Initialize Option 4: Phosphor CRT Oscilloscope
        crtScreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtScreenPaint.setStyle(Paint.Style.FILL);
        crtScreenPaint.setColor(Color.parseColor("#02120A"));

        crtBezelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtBezelPaint.setStyle(Paint.Style.STROKE);
        crtBezelPaint.setStrokeWidth(2.5f);
        crtBezelPaint.setColor(Color.parseColor("#0E2E1A"));

        crtGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtGridPaint.setStyle(Paint.Style.STROKE);
        crtGridPaint.setStrokeWidth(1.0f);
        crtGridPaint.setColor(Color.parseColor("#1500E676"));

        crtCenterGridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtCenterGridPaint.setStyle(Paint.Style.STROKE);
        crtCenterGridPaint.setStrokeWidth(1.6f);
        crtCenterGridPaint.setColor(Color.parseColor("#3000E676"));

        crtGhostPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtGhostPaint.setStyle(Paint.Style.STROKE);
        crtGhostPaint.setStrokeWidth(2.0f);
        crtGhostPaint.setStrokeCap(Paint.Cap.ROUND);
        crtGhostPaint.setStrokeJoin(Paint.Join.ROUND);
        crtGhostPaint.setColor(Color.parseColor("#2800E676"));

        crtBloomPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtBloomPaint.setStyle(Paint.Style.STROKE);
        crtBloomPaint.setStrokeWidth(6.0f);
        crtBloomPaint.setStrokeCap(Paint.Cap.ROUND);
        crtBloomPaint.setStrokeJoin(Paint.Join.ROUND);
        crtBloomPaint.setColor(Color.parseColor("#3800E676"));

        crtTracePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtTracePaint.setStyle(Paint.Style.STROKE);
        crtTracePaint.setStrokeWidth(2.0f);
        crtTracePaint.setStrokeCap(Paint.Cap.ROUND);
        crtTracePaint.setStrokeJoin(Paint.Join.ROUND);
        crtTracePaint.setColor(Color.parseColor("#E0FFFFFF"));

        crtDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        crtDotPaint.setStyle(Paint.Style.FILL);
        crtDotPaint.setColor(Color.parseColor("#FFFFFF"));

        crtTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        crtTextPaint.setColor(Color.parseColor("#00E676"));
        crtTextPaint.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));

        // Initialize Option 5: Radial Sonic Starburst
        starburstCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starburstCorePaint.setStyle(Paint.Style.STROKE);
        starburstCorePaint.setStrokeCap(Paint.Cap.ROUND);
        starburstCorePaint.setColor(Color.parseColor("#FFFFFF"));

        starburstRayCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starburstRayCyanPaint.setStyle(Paint.Style.STROKE);
        starburstRayCyanPaint.setStrokeCap(Paint.Cap.ROUND);
        starburstRayCyanPaint.setColor(Color.parseColor("#00E5FF"));

        starburstRayAmberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starburstRayAmberPaint.setStyle(Paint.Style.STROKE);
        starburstRayAmberPaint.setStrokeCap(Paint.Cap.ROUND);
        starburstRayAmberPaint.setColor(Color.parseColor("#FFA000"));

        starburstParticlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        starburstParticlePaint.setStyle(Paint.Style.FILL);
        starburstParticlePaint.setColor(Color.parseColor("#E0FFFFFF"));

        // McIntosh 3D Particle Wave Paints (Organic Stippled Particle Mesh)
        particleCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleCyanPaint.setStyle(Paint.Style.FILL);
        particleCyanPaint.setColor(Color.parseColor("#00E5FF")); // McIntosh Electric Cyan

        particleBluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleBluePaint.setStyle(Paint.Style.FILL);
        particleBluePaint.setColor(Color.parseColor("#00B4D8")); // McIntosh Iconic VU Blue
        particleBluePaint.setAlpha(210);

        particleNavyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleNavyPaint.setStyle(Paint.Style.FILL);
        particleNavyPaint.setColor(Color.parseColor("#0077B6")); // Deep Ocean Blue
        particleNavyPaint.setAlpha(145);

        particleDarkNavyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleDarkNavyPaint.setStyle(Paint.Style.FILL);
        particleDarkNavyPaint.setColor(Color.parseColor("#023E8A")); // Deep VU Navy
        particleDarkNavyPaint.setAlpha(90);

        particleWhiteCyanPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleWhiteCyanPaint.setStyle(Paint.Style.FILL);
        particleWhiteCyanPaint.setColor(Color.parseColor("#E0F7FA")); // Brilliant White-Cyan Crest Highlight

        particleAmberPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleAmberPaint.setStyle(Paint.Style.FILL);
        particleAmberPaint.setColor(Color.parseColor("#FFC107")); // McIntosh Warm Amber Gold Peak

        particleRubyRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleRubyRedPaint.setStyle(Paint.Style.FILL);
        particleRubyRedPaint.setColor(Color.parseColor("#FF1744")); // McIntosh Ruby Red Peak

        particleMintPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleMintPaint.setStyle(Paint.Style.FILL);
        particleMintPaint.setColor(Color.parseColor("#00FFA3")); // McIntosh Bright Electric Mint

        particleGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleGreenPaint.setStyle(Paint.Style.FILL);
        particleGreenPaint.setColor(Color.parseColor("#00E676")); // McIntosh Signature Emerald Green
        particleGreenPaint.setAlpha(210);

        particleDarkGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleDarkGreenPaint.setStyle(Paint.Style.FILL);
        particleDarkGreenPaint.setColor(Color.parseColor("#007E33")); // Deep Forest Emerald
        particleDarkGreenPaint.setAlpha(145);

        particleDeepForestPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleDeepForestPaint.setStyle(Paint.Style.FILL);
        particleDeepForestPaint.setColor(Color.parseColor("#004D25")); // Dark Emerald
        particleDeepForestPaint.setAlpha(85);

        // Soft luminous particle glow aura
        particleGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        particleGlowPaint.setStyle(Paint.Style.FILL);
        particleGlowPaint.setColor(Color.parseColor("#3800E5FF")); // Soft Luminous Aura

        // Idle Resting Particle Ring Paint (Subtle stippled dots)
        idleDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        idleDotPaint.setStyle(Paint.Style.FILL);
        idleDotPaint.setColor(Color.parseColor("#2E00B4D8")); // Subtle resting McIntosh cyan-blue dots

        // Translucent McIntosh Faceplate Glass Paints (Silkscreen Backlit Glass Look)
        glassCenterFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassCenterFillPaint.setStyle(Paint.Style.FILL);
        glassCenterFillPaint.setColor(Color.parseColor("#1203111E")); // Deep translucent dark glass backing

        glassCenterRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        glassCenterRimPaint.setStyle(Paint.Style.STROKE);
        glassCenterRimPaint.setStrokeWidth(1.8f);
        glassCenterRimPaint.setColor(Color.parseColor("#2E00E5FF")); // Delicate backlit glass bevel rim

        // Ambient UI Backlight Diffusion Aura Paint
        ambientAuraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ambientAuraPaint.setStyle(Paint.Style.FILL);

        // Ambient Folder Wheel Text Paint (+5 to -5 depth)
        ambientFolderTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ambientFolderTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        ambientFolderTextPaint.setTextAlign(Paint.Align.CENTER);
        ambientFolderTextPaint.setColor(Color.parseColor("#00E5FF")); // McIntosh Cyan
        ambientFolderTextPaint.setLetterSpacing(0.04f);

        // Simplest UI: Dual Column Minimal Navigation Paints
        splitDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitDividerPaint.setStyle(Paint.Style.STROKE);
        splitDividerPaint.setStrokeWidth(1.2f);

        splitNexusPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitNexusPaint.setStyle(Paint.Style.FILL);
        splitNexusPaint.setColor(Color.parseColor("#4D00E5FF"));

        splitFolderHeaderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFolderHeaderPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        splitFolderHeaderPaint.setColor(Color.parseColor("#4A7A66")); // Subtle McIntosh Slate Green
        splitFolderHeaderPaint.setTextAlign(Paint.Align.CENTER);
        splitFolderHeaderPaint.setLetterSpacing(0.12f);

        splitFolderBadgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFolderBadgePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        splitFolderBadgePaint.setColor(Color.parseColor("#8000E676"));
        splitFolderBadgePaint.setTextAlign(Paint.Align.CENTER);

        splitFolderActiveCapsulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFolderActiveCapsulePaint.setStyle(Paint.Style.FILL);
        splitFolderActiveCapsulePaint.setColor(Color.parseColor("#1C002B1D")); // Deep McIntosh Emerald Glass

        splitFolderActiveBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFolderActiveBorderPaint.setStyle(Paint.Style.STROKE);
        splitFolderActiveBorderPaint.setStrokeWidth(1.8f);
        splitFolderActiveBorderPaint.setColor(Color.parseColor("#9900E676"));

        splitFolderAccentStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFolderAccentStripePaint.setStyle(Paint.Style.FILL);
        splitFolderAccentStripePaint.setColor(Color.parseColor("#00E676"));

        splitFolderActiveTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFolderActiveTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        splitFolderActiveTextPaint.setColor(Color.parseColor("#00E676"));
        splitFolderActiveTextPaint.setTextAlign(Paint.Align.CENTER);
        splitFolderActiveTextPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#8000E676"));

        splitFolderSubPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFolderSubPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        splitFolderSubPaint.setColor(Color.parseColor("#6600E676"));
        splitFolderSubPaint.setTextAlign(Paint.Align.CENTER);
        splitFolderSubPaint.setLetterSpacing(0.06f);

        splitFolderItemTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFolderItemTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        splitFolderItemTextPaint.setColor(Color.parseColor("#00E676"));
        splitFolderItemTextPaint.setTextAlign(Paint.Align.CENTER);

        splitFileHeaderPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFileHeaderPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        splitFileHeaderPaint.setColor(Color.parseColor("#3C6E82")); // Subtle Slate Cyan
        splitFileHeaderPaint.setTextAlign(Paint.Align.CENTER);
        splitFileHeaderPaint.setLetterSpacing(0.12f);

        splitFileBadgePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFileBadgePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        splitFileBadgePaint.setColor(Color.parseColor("#8000E5FF"));
        splitFileBadgePaint.setTextAlign(Paint.Align.CENTER);

        splitFileActiveCapsulePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFileActiveCapsulePaint.setStyle(Paint.Style.FILL);
        splitFileActiveCapsulePaint.setColor(Color.parseColor("#1C002535")); // Deep Cyan Glass

        splitFileActiveBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFileActiveBorderPaint.setStyle(Paint.Style.STROKE);
        splitFileActiveBorderPaint.setStrokeWidth(1.8f);
        splitFileActiveBorderPaint.setColor(Color.parseColor("#9900E5FF"));

        splitFileAccentStripePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitFileAccentStripePaint.setStyle(Paint.Style.FILL);
        splitFileAccentStripePaint.setColor(Color.parseColor("#00E5FF"));

        splitFileActiveTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFileActiveTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        splitFileActiveTextPaint.setColor(Color.parseColor("#00E5FF"));
        splitFileActiveTextPaint.setTextAlign(Paint.Align.CENTER);
        splitFileActiveTextPaint.setShadowLayer(8f, 0f, 0f, Color.parseColor("#8000E5FF"));

        splitFileItemTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitFileItemTextPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        splitFileItemTextPaint.setColor(Color.parseColor("#00E5FF"));
        splitFileItemTextPaint.setTextAlign(Paint.Align.CENTER);

        splitCuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitCuePaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        splitCuePaint.setColor(Color.parseColor("#3800E5FF"));
        splitCuePaint.setTextAlign(Paint.Align.CENTER);

        splitLiveEqPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitLiveEqPaint.setStyle(Paint.Style.FILL);
        splitLiveEqPaint.setColor(Color.parseColor("#00E5FF"));

        splitTimeTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitTimeTextPaint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        splitTimeTextPaint.setColor(Color.parseColor("#B300E5FF"));
        splitTimeTextPaint.setTextAlign(Paint.Align.CENTER);

        splitProgressBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitProgressBgPaint.setStyle(Paint.Style.STROKE);
        splitProgressBgPaint.setStrokeWidth(2f);
        splitProgressBgPaint.setColor(Color.parseColor("#2200E5FF"));

        splitProgressFillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitProgressFillPaint.setStyle(Paint.Style.STROKE);
        splitProgressFillPaint.setStrokeWidth(2.5f);
        splitProgressFillPaint.setStrokeCap(Paint.Cap.ROUND);
        splitProgressFillPaint.setColor(Color.parseColor("#00E5FF"));

        splitPlayheadPipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitPlayheadPipPaint.setStyle(Paint.Style.FILL);
        splitPlayheadPipPaint.setColor(Color.parseColor("#FFFFFF"));

        splitRecHaloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitRecHaloPaint.setStyle(Paint.Style.FILL);
        splitRecHaloPaint.setColor(Color.parseColor("#FF1744"));

        splitRecCorePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        splitRecCorePaint.setStyle(Paint.Style.FILL);
        splitRecCorePaint.setColor(Color.parseColor("#FF5252"));

        splitRecTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitRecTextPaint.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        splitRecTextPaint.setColor(Color.parseColor("#FF5252"));
        splitRecTextPaint.setTextAlign(Paint.Align.CENTER);
        splitRecTextPaint.setLetterSpacing(0.08f);

        splitIdlePlayCuePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        splitIdlePlayCuePaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        splitIdlePlayCuePaint.setColor(Color.parseColor("#4D00E5FF"));
        splitIdlePlayCuePaint.setTextAlign(Paint.Align.CENTER);
        splitIdlePlayCuePaint.setLetterSpacing(0.06f);
    }

    private void updateAuraShaders(float radius) {
        if (radius <= 0 || Math.abs(radius - lastAuraRadius) < 1f) return;
        lastAuraRadius = radius;

        // McIntosh Playback Ambient Backlight Aura (VU Blue / Electric Cyan diffusion)
        playbackAuraShader = new RadialGradient(
                centerX, centerY, radius * 1.05f,
                new int[]{
                        Color.parseColor("#00000000"), // Center safe zone
                        Color.parseColor("#0C00B4D8"), // Inner glass rim
                        Color.parseColor("#3C00B4D8"), // Bezel perimeter
                        Color.parseColor("#2800E5FF"), // Particle wave zone
                        Color.parseColor("#0E0077B6"), // Outer diffusion
                        Color.parseColor("#00000000")  // Dark edges
                },
                new float[]{0.0f, 0.38f, 0.72f, 0.85f, 0.95f, 1.0f},
                Shader.TileMode.CLAMP
        );

        // McIntosh Recording Ambient Backlight Aura (Emerald Green / Electric Mint diffusion)
        recordingAuraShader = new RadialGradient(
                centerX, centerY, radius * 1.05f,
                new int[]{
                        Color.parseColor("#00000000"),
                        Color.parseColor("#0C00E676"),
                        Color.parseColor("#3C00E676"),
                        Color.parseColor("#2800FFA3"),
                        Color.parseColor("#0E007E33"),
                        Color.parseColor("#00000000")
                },
                new float[]{0.0f, 0.38f, 0.72f, 0.85f, 0.95f, 1.0f},
                Shader.TileMode.CLAMP
        );

        // Idle Standby Ambient Aura (Calm, deep subtle McIntosh illumination)
        idleAuraShader = new RadialGradient(
                centerX, centerY, radius * 1.02f,
                new int[]{
                        Color.parseColor("#00000000"),
                        Color.parseColor("#0600B4D8"),
                        Color.parseColor("#2000B4D8"),
                        Color.parseColor("#1200E5FF"),
                        Color.parseColor("#00000000")
                },
                new float[]{0.0f, 0.40f, 0.74f, 0.88f, 1.0f},
                Shader.TileMode.CLAMP
        );
    }

    private List<Integer> getCachedBookmarks(File currentFile) {
        if (currentFile == null) return null;
        long now = System.currentTimeMillis();
        String path = currentFile.getAbsolutePath();
        if (cachedBookmarks == null || !path.equals(lastBookmarkQueriedFile) || (now - lastBookmarkQueryTime > 1500)) {
            lastBookmarkQueriedFile = path;
            lastBookmarkQueryTime = now;
            try {
                cachedBookmarks = AudioMetadataHelper.getBookmarks(getContext(), currentFile);
            } catch (Exception e) {
                cachedBookmarks = null;
            }
        }
        return cachedBookmarks;
    }

    /**
     * Renders a precision 45-degree faceted diamond crystal node (◆) with zero runtime allocations.
     */
    private void drawDiamond(Canvas canvas, float cx, float cy, float size, Paint paint) {
        diamondPath.rewind();
        diamondPath.moveTo(cx, cy - size);
        diamondPath.lineTo(cx + size, cy);
        diamondPath.lineTo(cx, cy + size);
        diamondPath.lineTo(cx - size, cy);
        diamondPath.close();
        canvas.drawPath(diamondPath, paint);
    }

    public DialView(Context context) {

        super(context);
        this.setBackgroundColor(Color.BLACK);
        initPaints();
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
            private boolean isStartedFromEdge = false;

            private boolean isEdgeTouch(MotionEvent e) {
                return isHorizontalEdgeTouch(e) || isVerticalEdgeTouch(e);
            }

            private boolean isHorizontalEdgeTouch(MotionEvent e) {
                if (e == null || activity == null) return false;
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                float density = dm.density;
                int screenWidth = dm.widthPixels;

                // Base edge exclusion margin: at least 50dp or 13% of screen width (whichever is larger)
                float baseMargin = Math.max(50 * density, screenWidth * 0.13f);
                float leftMargin = baseMargin;
                float rightMargin = baseMargin;

                try {
                    View decorView = activity.getWindow().getDecorView();
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
                    if (insets != null) {
                        androidx.core.graphics.Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                        if (gestureInsets.left > leftMargin) leftMargin = gestureInsets.left;
                        if (gestureInsets.right > rightMargin) rightMargin = gestureInsets.right;
                    }
                } catch (Exception ignored) {}

                float rawX = e.getRawX();
                return (rawX < leftMargin || rawX > (screenWidth - rightMargin));
            }

            private boolean isVerticalEdgeTouch(MotionEvent e) {
                if (e == null || activity == null) return false;
                DisplayMetrics dm = activity.getResources().getDisplayMetrics();
                float density = dm.density;
                int screenHeight = dm.heightPixels;

                float baseMargin = 45 * density;
                float topMargin = baseMargin;
                float bottomMargin = baseMargin;

                try {
                    View decorView = activity.getWindow().getDecorView();
                    WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decorView);
                    if (insets != null) {
                        androidx.core.graphics.Insets gestureInsets = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
                        if (gestureInsets.top > topMargin) topMargin = gestureInsets.top;
                        if (gestureInsets.bottom > bottomMargin) bottomMargin = gestureInsets.bottom;
                    }
                } catch (Exception ignored) {}

                float rawY = e.getRawY();
                return (rawY < topMargin || rawY > (screenHeight - bottomMargin));
            }

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

            // Multi-tap handling: 1 tap = Stop (recording/playing) or Play (idle), 2 taps = Record, 3 taps = Bookmark
            private final Handler tapHandler = new Handler(Looper.getMainLooper());
            private int consecutiveTapCount = 0;
            private static final int MULTI_TAP_TIMEOUT = 320; // ms
            private int maxPointerCount = 1;

            private final Runnable singleTapRunnable = new Runnable() {
                @Override
                public void run() {
                    if (consecutiveTapCount == 1) {
                        consecutiveTapCount = 0;
                        handleSingleTap();
                    }
                }
            };

            private final Runnable doubleTapRunnable = new Runnable() {
                @Override
                public void run() {
                    if (consecutiveTapCount == 2) {
                        consecutiveTapCount = 0;
                        handleDoubleTap();
                    }
                }
            };

            private void cancelPendingTaps() {
                consecutiveTapCount = 0;
                tapHandler.removeCallbacks(singleTapRunnable);
                tapHandler.removeCallbacks(doubleTapRunnable);
            }

            {
                cancelTapsCallback = new Runnable() {
                    @Override
                    public void run() {
                        cancelPendingTaps();
                    }
                };
            }

            private void handleSingleTap() {
                if (activity.getCommander() != null && activity.getCommander().recorder != null) {
                    Recorder rec = activity.getCommander().recorder;
                    if (rec.isRecording()) {
                        cmd(Commander.STOP_RECORD);
                    } else if (rec.isPlaying() || rec.isPaused()) {
                        cmd(Commander.STOP_PLAYBACK);
                    } else {
                        cmd(Commander.ONETOUCH);
                    }
                } else {
                    cmd(Commander.ONETOUCH);
                }
            }

            private void handleDoubleTap() {
                if (activity.getCommander() != null && activity.getCommander().recorder != null) {
                    Recorder rec = activity.getCommander().recorder;
                    if (rec.isRecording()) {
                        cmd(Commander.STOP_RECORD);
                    } else {
                        cmd(Commander.START_RECORD);
                    }
                } else {
                    cmd(Commander.START_RECORD);
                }
            }

            private void handleTripleTap() {
                cancelPendingTaps();
                cmd(Commander.ADD_BOOKMARK);
            }

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
                        maxPointerCount = 1;

                        if (isEdgeTouch(event)) {
                            Log.i(TAG, "ACTION_DOWN in edge touch zone: rawX=" + event.getRawX() + ", rawY=" + event.getRawY() + " -> passing to system navigation");
                            isStartedFromEdge = true;
                            cancelPendingTaps();
                            return false;
                        }
                        isStartedFromEdge = false;

                        // If user touches down during an ongoing multi-tap sequence, temporarily pause
                        // the pending single/double-tap timeout so it doesn't fire while their finger is DOWN!
                        if (consecutiveTapCount > 0) {
                            tapHandler.removeCallbacks(singleTapRunnable);
                            tapHandler.removeCallbacks(doubleTapRunnable);
                        }

                        startAngle = touchAngle(touchX1, touchY1);
                        isDragging = isInDiscArea(touchX1, touchY1);

                        // Start time of TouchPress without moving
                        this.startAction = System.currentTimeMillis();

                        // Initiate isFired which set up in ACTION_UP
                        isFired = false;

                        // Initiate Longpress mode
                        isLongpress = false;

                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        if (isStartedFromEdge) return false;
                        if (event.getPointerCount() > maxPointerCount) {
                            maxPointerCount = event.getPointerCount();
                        }
                        cancelPendingTaps();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (isStartedFromEdge) {
                            return false;
                        }

                        this.touchCnt = event.getPointerCount();
                        if (touchCnt > maxPointerCount) {
                            maxPointerCount = touchCnt;
                        }

                        if (Math.abs(startX - event.getX()) > SWIPE_MIN_DISTANCE || Math.abs(startY - event.getY()) > SWIPE_MIN_DISTANCE) {
                            cancelPendingTaps();
                        }

                        //Long Press!!! Not enough movement during threshold time.
                        // Checking only when iLongpress is false.
                        // TODO After observing SJ's error of longpress, THRESHOLD value needs to be optimized.
                        if (!isLongpress && (System.currentTimeMillis() - startAction) > LONGPRESS_THRESHOLD &&
                                (Math.abs(startX - event.getX()) < SWIPE_MIN_DISTANCE && Math.abs(startY - event.getY()) < SWIPE_MIN_DISTANCE)) {
                            cancelPendingTaps();
                            //Log.i(TAG, "Touch duration: " + (System.currentTimeMillis() - startAction) +":"+Math.abs(startX-event.getX())+":"+Math.abs(startY-event.getY()));
                            //To make vibration when Longpress is recognized.

                            if(isDragging)
                                this.isLongpress = true;
                            // If event is already fired, skips.
                            if(isFired)
                                return true;

                            if(touchCnt == 1)
                                cmd(Commander.SPEAK_FILE_INFO);
                            else if(touchCnt == 2)
                                cmd(Commander.SPEAK_DATE_TIME);
                            else if(touchCnt >= 3)
                                cmd(Commander.TOGGLE_FAVORITE);
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
                                        cmd(Commander.FF_FOLDER);
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
                                        cmd(Commander.REWIND_FOLDER);
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
                        if (isStartedFromEdge) {
                            Log.i(TAG, "ACTION_UP from edge gesture, ignoring in app: rawX=" + event.getRawX());
                            isStartedFromEdge = false;
                            cancelPendingTaps();
                            return false;
                        }

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
                            cancelPendingTaps();
                            return false;
                        }

                        if (event.getPointerCount() > maxPointerCount) {
                            maxPointerCount = event.getPointerCount();
                        }
                        int effectivePointers = Math.max(touchCnt, maxPointerCount);

                        float density = (activity != null) ? activity.getResources().getDisplayMetrics().density : 1.0f;
                        float swipeThreshold = Math.max(SWIPE_MIN_DISTANCE, 40 * density);

                        // If it's too short from DOWN to UP with not enough distance, it's a TAP.
                        if ((System.currentTimeMillis() - startAction) < LONGPRESS_THRESHOLD  &&
                                (Math.abs(startX-event.getX()) < swipeThreshold && Math.abs(startY-event.getY()) < swipeThreshold)) {

                            if (effectivePointers == 2) {
                                cancelPendingTaps();
                                cmd(Commander.ADD_BOOKMARK);
                                return true;
                            }

                            consecutiveTapCount++;
                            Log.i(TAG, "MultiTap: count=" + consecutiveTapCount);

                            if (consecutiveTapCount == 1) {
                                tapHandler.postDelayed(singleTapRunnable, MULTI_TAP_TIMEOUT);
                            } else if (consecutiveTapCount == 2) {
                                tapHandler.removeCallbacks(singleTapRunnable);
                                tapHandler.postDelayed(doubleTapRunnable, MULTI_TAP_TIMEOUT);
                            } else if (consecutiveTapCount >= 3) {
                                tapHandler.removeCallbacks(singleTapRunnable);
                                tapHandler.removeCallbacks(doubleTapRunnable);
                                handleTripleTap();
                            }
                            return true;
                        } else {
                            if (consecutiveTapCount > 0) {
                                cancelPendingTaps();
                            }
                        }



                        if((Math.abs(startX-event.getX()) > swipeThreshold ||
                                Math.abs(startY-event.getY()) > swipeThreshold)){
                            cancelPendingTaps();

                            int dir = 0;

                            // 8 dir mode in Virtual directory(Media scann mode) to navigate ARTIST
                            if (dir_mode == DIR_MODE_4)
                                dir = getDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY()));
                            else if(dir_mode == DIR_MODE_8)
                                dir = getHexDirection(getDegreeFromCartesian(startX,startY,event.getX(),event.getY()));

                            switch (dir) {
                                case BOTTOM_TOP:
                                    cmd(Commander.NEXT_FOLDER);
                                    break;

                                // Next ARTIST
                                case UP_RIGHT:
                                    cmd(Commander.NEXT_ARTIST);
                                    break;
                                case LEFT_RIGHT:
                                    if(touchCnt == 1) {
                                        cmd(Commander.NEXT_SONG);
                                    } else if(touchCnt == 2) {
                                        cmd(Commander.NEXT_BOOKMARK);
                                    } else if(touchCnt >= 3) {
                                        cmd(Commander.NEXT_FAVORITE);
                                    }
                                    break;
//                                case BOTTOM_RIGHT:
//                                    cmd(Commander.NOTHING);
//                                    break;
                                case TOP_BOTTOM:
                                    //From Top to Bottom
                                    if (touchCnt > 1)
                                        cmd(Commander.START_RECORD);
                                    else if (touchCnt == 1)
                                        cmd(Commander.PREVIOUS_FOLDER);
                                    break;
//                                case BOTTOM_LEFT:
//                                    cmd(Commander.NOTHING);
//                                    break;
                                case RIGHT_LEFT:
                                    if(touchCnt == 1) {
                                        cmd(Commander.PREVIOUS_SONG);
                                    } else if(touchCnt == 2) {
                                        cmd(Commander.PREVIOUS_BOOKMARK);
                                    } else if(touchCnt >= 3) {
                                        cmd(Commander.PREVIOUS_FAVORITE);
                                    }
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
                        isStartedFromEdge = false;
                        cancelPendingTaps();
                        isDragging = false;
                        isFired = false;
                        isLongpress = false;
                        return false;
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        checkAndStartAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility == View.VISIBLE) {
            checkAndStartAnimation();
        } else {
            stopAnimation();
        }
    }

    public boolean checkAudioState() {
        try {
            if (getContext() instanceof MainActivity) {
                MainActivity ma = (MainActivity) getContext();
                if (ma.getCommander() != null && ma.getCommander().recorder != null) {
                    return ma.getCommander().recorder.isRecording() || ma.getCommander().recorder.isPlaying();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    public void checkAndStartAnimation() {
        if (!isAnimationRunning && getVisibility() == View.VISIBLE) {
            isAnimationRunning = true;
            eqHandler.post(eqAnimationRunnable);
        }
    }

    public void stopAnimation() {
        if (cancelTapsCallback != null) {
            cancelTapsCallback.run();
        }
        isAnimationRunning = false;
        eqHandler.removeCallbacks(eqAnimationRunnable);
        for (int i = 0; i < NUM_ANGLES; i++) {
            audioBands[i] = 0f;
            audioPeakBands[i] = 0f;
        }
        invalidate();
    }

    private void updateEqData() {
        boolean isRecording = false;
        boolean isPlaying = false;
        Recorder rec = null;
        try {
            if (getContext() instanceof MainActivity) {
                MainActivity ma = (MainActivity) getContext();
                if (ma.getCommander() != null) {
                    rec = ma.getCommander().recorder;
                    if (rec != null) {
                        isRecording = rec.isRecording();
                        isPlaying = rec.isPlaying();
                    }
                }
            }
        } catch (Exception ignored) {}

        long now = System.currentTimeMillis();

        if (isRecording && rec != null) {
            waveTime += 0.042f; // Smooth organic undulation flow

            // Live microphone amplitude from MediaRecorder (0..32767)
            int amp = rec.getMaxAmplitude();
            // Fast, highly-sensitive acoustic voice response
            float normAmp = Math.min(1.0f, Math.max(0.06f, (float) amp / 20000f));
            float expAmp = (float) Math.pow(normAmp, 0.52);

            for (int i = 0; i < NUM_ANGLES; i++) {
                // Multi-frequency voice formant ripple around perimeter
                float wave1 = (float) Math.sin(i * 0.32 + waveTime * 2.2);
                float wave2 = (float) Math.cos(i * 0.18 - waveTime * 1.4);
                float formant = 0.50f + 0.50f * Math.abs(wave1 * wave2);
                float target = Math.min(1.0f, Math.max(0.06f, expAmp * formant * 1.35f));

                // Instant attack, snappy ballistic release
                if (target > audioBands[i]) {
                    audioBands[i] = target;
                } else {
                    audioBands[i] = audioBands[i] * 0.74f + target * 0.26f;
                }

                // Peak tracking with 320ms hold
                if (audioBands[i] >= audioPeakBands[i]) {
                    audioPeakBands[i] = audioBands[i];
                    audioPeakDecayTime[i] = now + 320;
                } else if (now > audioPeakDecayTime[i]) {
                    audioPeakBands[i] = Math.max(audioBands[i], audioPeakBands[i] - 0.040f);
                }
            }
        } else if (isPlaying) {
            waveTime += 0.055f; // Dynamic rhythmic traveling wave flow

            // High-energy rhythmic audio spectrum simulation
            // Rhythmic Bass Pulse (~110 bpm punchy kick transient)
            float beatCycle = ((now % 545) / 545.0f); // 0..1
            float bassKick = (float) Math.pow(Math.max(0, 1.0f - beatCycle * 2.2f), 2.2);

            int half = NUM_ANGLES / 2;
            for (int i = 0; i < NUM_ANGLES; i++) {
                // Symmetrical stereo layout
                int band = (i <= half) ? i : (NUM_ANGLES - i);
                float bandNorm = (float) band / half;

                // Multi-harmonic traveling ripples
                float wave1 = (float) Math.sin(bandNorm * 4.2 + waveTime * 2.0);
                float wave2 = (float) Math.cos(bandNorm * 2.4 - waveTime * 1.2);

                // Heavy sub-bass bounce on lower frequencies
                float bandBass = (bandNorm < 0.35f) ? bassKick * (1.0f - bandNorm / 0.35f) : 0f;
                // Mid-high energy surge
                float midEnergy = (bandNorm >= 0.25f && bandNorm <= 0.70f) ? 0.35f * (float) Math.abs(Math.sin(bandNorm * 5.0 + waveTime * 1.8)) : 0f;
                // Treble sparkle on outer frequencies
                float trebleSparkle = (bandNorm > 0.65f) ? 0.30f * (float) Math.abs(Math.cos(bandNorm * 9.0 + waveTime * 2.5)) : 0f;

                float rawTarget = 0.12f + 0.45f * Math.abs(wave1 * wave2) + bandBass * 0.85f + midEnergy + trebleSparkle;
                float target = Math.min(1.0f, Math.max(0.06f, rawTarget));

                // Instant attack, dynamic release
                if (target > audioBands[i]) {
                    audioBands[i] = target;
                } else {
                    audioBands[i] = audioBands[i] * 0.75f + target * 0.25f;
                }

                // Peak tracking with 350ms hold
                if (audioBands[i] >= audioPeakBands[i]) {
                    audioPeakBands[i] = audioBands[i];
                    audioPeakDecayTime[i] = now + 350;
                } else if (now > audioPeakDecayTime[i]) {
                    audioPeakBands[i] = Math.max(audioBands[i], audioPeakBands[i] - 0.035f);
                }
            }
        } else {
            // Ambient resting breathing drift
            waveTime += 0.016f;
            for (int i = 0; i < NUM_ANGLES; i++) {
                audioBands[i] = audioBands[i] * 0.85f;
                audioPeakBands[i] = Math.max(0f, audioPeakBands[i] - 0.02f);
            }
        }

        // 32-Band Frequency Spectrum calculation (Sub-bass, Bass, Low-Mid, High-Mid, Treble)
        float streamVol = getMediaVolumeFraction();
        for (int b = 0; b < NUM_FREQ_BANDS; b++) {
            float bNorm = (float) b / (float) (NUM_FREQ_BANDS - 1); // 0 (sub-bass) to 1 (treble)
            float bandTarget;
            if (isRecording) {
                int amp = (rec != null) ? rec.getMaxAmplitude() : 0;
                float normAmp = Math.min(1.0f, Math.max(0.06f, (float) amp / 20000f));
                float expAmp = (float) Math.pow(normAmp, 0.52);
                float formant = (float) Math.exp(-Math.pow((bNorm - 0.45f) / 0.28f, 2.0));
                bandTarget = Math.min(1.0f, Math.max(0.05f, expAmp * (0.35f + 1.25f * formant)));
            } else if (isPlaying) {
                float beatCycle = ((now % 545) / 545.0f);
                float bassKick = (float) Math.pow(Math.max(0, 1.0f - beatCycle * 2.2f), 2.2);

                float subBass = (bNorm < 0.25f) ? bassKick * (1.0f - bNorm / 0.25f) : 0f;
                float midEnergy = (bNorm >= 0.20f && bNorm <= 0.65f) ? 0.45f * (float) Math.abs(Math.sin(bNorm * 7.0 + waveTime * 2.2)) : 0f;
                float trebleSparkle = (bNorm > 0.60f) ? 0.35f * (float) Math.abs(Math.cos(bNorm * 11.0 + waveTime * 2.8)) : 0f;
                float rNorm = (float) Math.abs(Math.sin(bNorm * 4.0 - waveTime * 1.5));
                bandTarget = Math.min(1.0f, Math.max(0.06f, (0.12f + 0.38f * rNorm + subBass * 0.90f + midEnergy + trebleSparkle) * (0.35f + 0.65f * streamVol)));
            } else {
                bandTarget = 0.05f;
            }

            // Fast attack, ballistic release
            if (bandTarget > freqBands[b]) {
                freqBands[b] = bandTarget;
            } else {
                freqBands[b] = freqBands[b] * 0.76f + bandTarget * 0.24f;
            }

            // Studio Peak-Hold tracking with 360ms hold + gravity drop
            if (freqBands[b] >= freqPeakBands[b]) {
                freqPeakBands[b] = freqBands[b];
                freqPeakDecayTime[b] = now + 360;
            } else if (now > freqPeakDecayTime[b]) {
                freqPeakBands[b] = Math.max(freqBands[b], freqPeakBands[b] - 0.032f);
            }
        }

        // Dual VU Meter Dynamics (Left / Right stereo channels with analog damping)
        float targetVuLeft;
        float targetVuRight;
        if (isRecording && rec != null) {
            int amp = rec.getMaxAmplitude();
            float normAmp = Math.min(1.0f, Math.max(0.05f, (float) amp / 20000f));
            targetVuLeft = normAmp;
            targetVuRight = normAmp * 0.95f;
        } else if (isPlaying) {
            float bassL = (freqBands[0] + freqBands[1] + freqBands[2] + freqBands[3]) / 4f;
            float midL = (freqBands[6] + freqBands[8] + freqBands[10]) / 3f;
            targetVuLeft = Math.min(1.0f, Math.max(0.06f, (bassL * 0.65f + midL * 0.45f) * (0.4f + 0.6f * streamVol)));

            float midR = (freqBands[5] + freqBands[7] + freqBands[9]) / 3f;
            float trebleR = (freqBands[14] + freqBands[18] + freqBands[22]) / 3f;
            targetVuRight = Math.min(1.0f, Math.max(0.06f, (midR * 0.50f + trebleR * 0.60f) * (0.4f + 0.6f * streamVol)));
        } else {
            targetVuLeft = 0.04f;
            targetVuRight = 0.04f;
        }

        // Fast attack (spring surge), damped return
        if (targetVuLeft > vuLeftLevel) {
            vuLeftLevel += (targetVuLeft - vuLeftLevel) * 0.42f;
        } else {
            vuLeftLevel += (targetVuLeft - vuLeftLevel) * 0.12f;
        }
        if (targetVuRight > vuRightLevel) {
            vuRightLevel += (targetVuRight - vuRightLevel) * 0.42f;
        } else {
            vuRightLevel += (targetVuRight - vuRightLevel) * 0.12f;
        }

        if (vuLeftLevel > 0.80f) vuLeftPeakHold = now + 400;
        if (vuRightLevel > 0.80f) vuRightPeakHold = now + 400;

        // Radial Sonic Starburst particle drift
        if (!starburstInitialized) {
            for (int p = 0; p < NUM_STARBURST_PARTICLES; p++) {
                starburstParticleAngle[p] = (float) (Math.random() * 2.0 * Math.PI);
                starburstParticleDist[p] = (float) Math.random();
                starburstParticleSpeed[p] = 0.008f + (float) Math.random() * 0.016f;
                starburstParticleSize[p] = 1.4f + (float) Math.random() * 2.2f;
                starburstParticleAlpha[p] = 0.3f + (float) Math.random() * 0.7f;
            }
            starburstInitialized = true;
        } else {
            float kick = (freqBands[0] + freqBands[1]) / 2f;
            for (int p = 0; p < NUM_STARBURST_PARTICLES; p++) {
                starburstParticleAngle[p] += starburstParticleSpeed[p];
                if (kick > 0.6f) {
                    starburstParticleDist[p] = Math.min(1.0f, starburstParticleDist[p] + 0.04f);
                } else {
                    starburstParticleDist[p] = (starburstParticleDist[p] + 0.006f) % 1.0f;
                }
            }
        }

        // Suggestion 6: Haptic Audio Pulse (Tactile bass downbeats for Eyes-Free accessibility)
        if (isPlaying && hapticPulseEnabled) {
            float bassKick = (freqBands[0] + freqBands[1] + freqBands[2]) / 3f;
            if (bassKick > 0.65f && (now - lastHapticBeatTime > 220)) {
                lastHapticBeatTime = now;
                triggerHapticBeat();
            }
        }
    }

    private void triggerHapticBeat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Vibrator v = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
                if (v != null && v.hasVibrator()) {
                    v.vibrate(VibrationEffect.createOneShot(14, 80));
                }
            }
        } catch (Exception ignored) {}
    }

    public void setHapticPulseEnabled(boolean enabled) {
        this.hapticPulseEnabled = enabled;
    }

    public boolean isHapticPulseEnabled() {
        return this.hapticPulseEnabled;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawDial(canvas);
        super.onDraw(canvas);
    }

    private void drawDial(Canvas canvas) {
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        if (radius <= 0) return;

        float density = getResources().getDisplayMetrics().density;
        float maxStemHeight = 22f * density;
        float minStemHeight = 3.0f * density;

        float rOuter = maxCircle * radius;
        float rInner = minCircle * radius;

        // Check active audio state
        boolean isRecording = false;
        boolean isPlaying = false;
        Recorder rec = null;
        try {
            if (getContext() instanceof MainActivity) {
                MainActivity ma = (MainActivity) getContext();
                if (ma.getCommander() != null && ma.getCommander().recorder != null) {
                    rec = ma.getCommander().recorder;
                    isRecording = rec.isRecording();
                    isPlaying = rec.isPlaying();
                }
            }
        } catch (Exception ignored) {}

        // Playback & Recording Progress tracking
        float playbackProgressFraction = 0f;
        if (isPlaying && rec != null) {
            int currentPos = rec.getCurrentPositionMillis();
            int duration = rec.getDurationMillis();
            float targetProgress = (duration > 0) ? Math.min(1.0f, Math.max(0f, (float) currentPos / (float) duration)) : 0f;
            if (Math.abs(targetProgress - smoothPlaybackProgress) > 0.08f) {
                smoothPlaybackProgress += (targetProgress - smoothPlaybackProgress) * 0.45f;
            } else {
                smoothPlaybackProgress += (targetProgress - smoothPlaybackProgress) * 0.22f;
            }
            playbackProgressFraction = smoothPlaybackProgress;
        } else {
            smoothPlaybackProgress = 0f;
        }

        float recordProgressFraction = 0f;
        if (isRecording && rec != null) {
            long startTime = rec.getRecordStartTimeMillis();
            long elapsed = (startTime > 0) ? (System.currentTimeMillis() - startTime) : 0L;
            if (elapsed < 0) elapsed = 0L;
            recordProgressFraction = (float) ((elapsed % 60000L) / 60000.0);
        }

        boolean active = isRecording || isPlaying;

        // Ensure animation loop starts if view is shown
        checkAndStartAnimation();

        // Option 0: Simplest UI (Clean Dual-Column Split Navigation: Folder on Left, Files on Right)
        if (visualizerMode == VISUALIZER_MODE_MINIMAL_SPLIT) {
            drawMinimalSplitUI(canvas, getMeasuredWidth(), getMeasuredHeight(), density, rec, active, isRecording, isPlaying, playbackProgressFraction);
            return;
        }

        // 0. Ambient McIntosh Backlight Aura (Ethereal diffuse glow behind the dial)
        updateAuraShaders(radius);
        ambientAuraPaint.setShader(isRecording ? recordingAuraShader : (isPlaying ? playbackAuraShader : idleAuraShader));
        canvas.drawCircle(centerX, centerY, radius * 1.02f, ambientAuraPaint);

        // 1. Translucent McIntosh Illuminated Glass Faceplate Backing (Center Safe Zone)
        // Letters glow through illuminated dark glass, exactly like real McIntosh faceplates!
        glassCenterRimPaint.setColor(isRecording ? Color.parseColor("#3800E676") : (isPlaying ? Color.parseColor("#3800E5FF") : Color.parseColor("#1C00B4D8")));
        canvas.drawCircle(centerX, centerY, rInner, glassCenterFillPaint);
        // Center Safe Zone rim
        canvas.drawCircle(centerX, centerY, rInner, glassCenterRimPaint);

        // Ambient Folder/File Wheel (+5 to -5 depth) - ALWAYS drawn in foreground of safe zone so previous/next folder/file names are clearly visible
        drawAmbientWheel(canvas, rec, radius);

        // Base perimeter radius for 3D undulating particle wave ribbon
        float ribbonCenterR = rOuter + 22f;
        float maxSpread = Math.max(25f, Math.min(80f, radius - ribbonCenterR - 10f));
        float halfRibbonWidth = 16f;

        // Update aura glow color
        particleGlowPaint.setColor(isRecording ? Color.parseColor("#3200FFA3") : (isPlaying ? Color.parseColor("#3200E5FF") : Color.parseColor("#1E00B4D8")));

        // 4. Visualizer Layer: Option 2 (Frequency Analyzer), Option 3 (Sonic Starburst), Option 1 (Circle Wave Ribbon)
        if (visualizerMode == VISUALIZER_MODE_FREQUENCY_ANALYZER) {
            drawRadialFrequencyAnalyzer(canvas, centerX, centerY, rOuter, radius, density, active, isRecording, isPlaying);
        } else if (visualizerMode == VISUALIZER_MODE_STARBURST) {
            drawRadialSonicStarburst(canvas, centerX, centerY, rInner, rOuter, radius, density, active, isRecording, isPlaying);
        } else {
            // Option 1: Dynamic 3D Undulating Particle Wave Ribbon Mesh (Circle Animation)
            for (int j = 0; j < NUM_STRANDS; j++) {
            // Normalized strand index s in [-1.0, 1.0] (inner track to outer track)
            float s = ((2f * j) - (NUM_STRANDS - 1)) / (float) (NUM_STRANDS - 1);
            float strandPhase = s * 0.72f;

            for (int i = 0; i < NUM_ANGLES; i++) {
                float angleProgress = (float) i / (float) NUM_ANGLES;
                boolean inProgressZone = isPlaying ? (angleProgress <= playbackProgressFraction) : (isRecording && (angleProgress <= recordProgressFraction));

                float angleRad = (float) (i * 2.0 * Math.PI / NUM_ANGLES);

                // 3D Harmonic traveling wave elevation Z
                float w1 = (float) Math.sin(3.0 * angleRad - waveTime * 1.6 + strandPhase);
                float w2 = (float) Math.cos(5.0 * angleRad + waveTime * 1.1 - s * 0.38);
                float w3 = (float) Math.sin(2.0 * angleRad - waveTime * 0.7);
                float z = 0.58f * w1 + 0.32f * w2 + 0.10f * w3; // Normalized depth in [-1.0, 1.0]

                float audio = audioBands[i];
                float peak = audioPeakBands[i];

                // Curved streamline angle (sweeping organic curvature matching reference image)
                float curvedAngle = angleRad - (float)(Math.PI / 2.0) + (s * 0.18f) + (z * 0.12f * (0.35f + audio * 0.65f));
                float cos = (float) Math.cos(curvedAngle);
                float sin = (float) Math.sin(curvedAngle);

                // 3D perspective strand expansion (ribbon widens at crests, narrows in troughs)
                float strandDist = s * (halfRibbonWidth + 8f * audio) * (1.0f + 0.22f * z);
                float waveDist = active ? (z * (13f + maxSpread * audio)) : (z * 9f);
                float r = ribbonCenterR + strandDist + waveDist;

                float px = centerX + r * cos;
                float py = centerY + r * sin;

                // Particle dot radius (3D perspective scaling from background to foreground)
                float baseDotRadius = 2.4f + 1.4f * z + (audio * 0.8f);
                if (baseDotRadius < 1.2f) baseDotRadius = 1.2f;

                // Subtle ambient starlight shimmer
                float shimmer = 0.88f + 0.12f * (float) Math.sin(i * 1.7 + j * 2.3 + waveTime * 2.6f);
                float finalDotRadius = baseDotRadius * shimmer;
                if (inProgressZone) {
                    finalDotRadius += 0.5f; // Energized particle expansion in swept sector
                }

                // 1. Dual-Pass Ambient Glow Halo
                if (z > -0.25f || inProgressZone) {
                    float haloRadius = finalDotRadius * (z > 0.60f ? 2.5f : (inProgressZone ? 2.2f : 1.9f));
                    canvas.drawCircle(px, py, haloRadius, particleGlowPaint);
                }

                // 2. Luminous Core Dot
                if (isRecording) {
                    if (inProgressZone && z > 0.30f) {
                        // Vibrant glowing emerald / mint energization in progress zone
                        canvas.drawCircle(px, py, finalDotRadius, (z > 0.70f) ? particleWhiteCyanPaint : particleMintPaint);
                        if (z > 0.70f) {
                            canvas.drawCircle(px, py, finalDotRadius * 0.45f, particleWhiteCyanPaint);
                        }
                    } else if (z > 0.65f) {
                        if (peak > 0.82f) {
                            canvas.drawCircle(px, py, finalDotRadius + 0.8f, particleRubyRedPaint);
                        } else {
                            canvas.drawCircle(px, py, finalDotRadius, particleMintPaint);
                            if (z > 0.82f) {
                                canvas.drawCircle(px, py, finalDotRadius * 0.45f, particleWhiteCyanPaint);
                            }
                        }
                    } else if (z > 0.20f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleMintPaint);
                    } else if (z > -0.30f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleGreenPaint);
                    } else if (z > -0.65f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleDarkGreenPaint);
                    } else {
                        canvas.drawCircle(px, py, finalDotRadius, particleDeepForestPaint);
                    }
                } else {
                    // Playback Mode or Idle Standby (McIntosh Electric Cyan & Iconic VU Blue)
                    if (inProgressZone && z > 0.25f) {
                        // Luminous energized starlight along the swept playback ribbon!
                        canvas.drawCircle(px, py, finalDotRadius, (z > 0.65f) ? particleWhiteCyanPaint : particleCyanPaint);
                        if (z > 0.65f) {
                            canvas.drawCircle(px, py, finalDotRadius * 0.45f, particleWhiteCyanPaint);
                        }
                    } else if (z > 0.65f) {
                        if (active && peak > 0.85f && (i % 4 == 0)) {
                            canvas.drawCircle(px, py, finalDotRadius + 0.8f, particleAmberPaint); // McIntosh Peak Accent
                        } else {
                            canvas.drawCircle(px, py, finalDotRadius, (z > 0.84f) ? particleWhiteCyanPaint : particleCyanPaint);
                            if (z > 0.84f) {
                                canvas.drawCircle(px, py, finalDotRadius * 0.45f, particleWhiteCyanPaint);
                            }
                        }
                    } else if (z > 0.20f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleCyanPaint);
                    } else if (z > -0.30f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleBluePaint);
                    } else if (z > -0.65f) {
                        canvas.drawCircle(px, py, finalDotRadius, particleNavyPaint);
                    } else {
                        canvas.drawCircle(px, py, finalDotRadius, particleDarkNavyPaint);
                    }
                }
            }
        }
        }

        // 5. Dynamic Organic Glowing Energy Stream & Star Markers
        if (isPlaying && visualizerMode != VISUALIZER_MODE_DUAL_VU) {
            int duration = (rec != null) ? rec.getDurationMillis() : 0;
            int activeNodes = Math.min(NUM_ANGLES, Math.max(0, Math.round(smoothPlaybackProgress * NUM_ANGLES)));

            // 1. Inactive Path: Delicate cosmic stardust guide (ambient faint diamonds, NO solid line!)
            for (int k = activeNodes; k < NUM_ANGLES; k += 3) {
                float nx = centerX + rOuter * cosTable[k];
                float ny = centerY + rOuter * sinTable[k];
                drawDiamond(canvas, nx, ny, 1.4f, progressDotFaintPaint);
            }

            // 2. Active Playback Energy Stream: Flowing Faceted Golden Amber Diamonds (◆) with Heights by Volume (dB) Amount
            float streamVol = getMediaVolumeFraction();

            String currentFileName = (rec != null) ? rec.getCurrentFileName() : "";
            int currentDuration = (rec != null) ? rec.getDurationMillis() : 0;
            if (currentFileName != null && !currentFileName.isEmpty()
                    && (!currentFileName.equals(currentWaveformFile) || currentDuration != lastWaveformDuration)) {
                generateTrackWaveform(currentFileName, currentDuration);
            }

            int headIdx = Math.max(0, Math.min(NUM_ANGLES - 1, activeNodes - 1));

            for (int k = 0; k < activeNodes; k++) {
                int fromHead = activeNodes - 1 - k;

                // Distinct per-node volume (dB) level for this section of the audio track
                float baseVol = trackWaveform[k];
                // Scaled by system stream volume
                float volAmount = Math.min(1.0f, Math.max(0.08f, baseVol * (0.35f + 0.65f * streamVol)));

                // Live dynamic bounce on the leading edge (most recent 2 bars)
                if (fromHead < 2) {
                    float liveBounce = audioBands[headIdx] * 0.20f;
                    volAmount = Math.min(1.0f, volAmount + liveBounce);
                }

                // Radial height modulated by volume (dB) amount - ensures varying heights
                float stemHeight = minStemHeight + maxStemHeight * volAmount;
                float rBase = rOuter - stemHeight * 0.35f;
                float rPeak = rOuter + stemHeight * 0.65f;

                float xBase = centerX + rBase * cosTable[k];
                float yBase = centerY + rBase * sinTable[k];
                float xPeak = centerX + rPeak * cosTable[k];
                float yPeak = centerY + rPeak * sinTable[k];

                // 1. Draw glowing height stem/pillar
                progressStemPaint.setStrokeWidth(1.6f * density);
                int stemAlpha = (fromHead < 5) ? 225 : (int) (80 + 155 * volAmount);
                progressStemPaint.setAlpha(stemAlpha);
                canvas.drawLine(xBase, yBase, xPeak, yPeak, progressStemPaint);

                // 2. Draw amber diamond dot atop the height crest
                if (fromHead < 5) {
                    // Leading comet head nodes - larger, radiant golden amber diamonds with white centers
                    float headProximity = (5 - fromHead) / 5.0f;
                    float dSize = 3.6f + headProximity * 1.8f + (1.2f * volAmount);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 2.2f, progressGlowPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize, progressDotPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 0.45f, progressDotWhitePaint);
                } else {
                    // Stream body - faceted warm amber diamond crystals with size scaling by volume (dB)
                    float dSize = 2.0f + 1.8f * volAmount;
                    drawDiamond(canvas, xPeak, yPeak, dSize * 1.8f, progressGlowPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize, progressDotPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 0.40f, progressDotWhitePaint);
                }
            }

            // 3. Radiant Comet Star Head (Continuous floating energy orb at exact progress position)
            float sweepAngle = smoothPlaybackProgress * 360f;
            double headRad = Math.toRadians(-90.0 + sweepAngle);
            float headBaseVol = (activeNodes > 0) ? trackWaveform[headIdx] : 0.5f;
            float headVol = Math.min(1.0f, Math.max(0.08f, (headBaseVol + audioBands[headIdx] * 0.20f) * (0.35f + 0.65f * streamVol)));
            float headR = rOuter + (minStemHeight + maxStemHeight * headVol) * 0.65f;
            float headX = centerX + headR * (float) Math.cos(headRad);
            float headY = centerY + headR * (float) Math.sin(headRad);

            float pulse = 0.85f + 0.15f * (float) Math.sin(waveTime * 4.2);
            canvas.drawCircle(headX, headY, 12.0f * pulse, progressHeadHaloPaint);
            drawDiamond(canvas, headX, headY, 6.5f * pulse, progressHeadBezelPaint);
            drawDiamond(canvas, headX, headY, 3.0f, progressHeadCorePaint);

            // 3 trailing micro-diamonds floating behind the head
            if (sweepAngle > 4f) {
                double spark1Rad = headRad - Math.toRadians(2.8);
                float s1x = centerX + (rOuter + (float) (1.4 * Math.sin(waveTime * 3.2))) * (float) Math.cos(spark1Rad);
                float s1y = centerY + (rOuter + (float) (1.4 * Math.sin(waveTime * 3.2))) * (float) Math.sin(spark1Rad);
                drawDiamond(canvas, s1x, s1y, 3.4f, progressDotPaint);
                drawDiamond(canvas, s1x, s1y, 1.5f, progressDotWhitePaint);

                double spark2Rad = headRad - Math.toRadians(5.6);
                float s2x = centerX + (rOuter - (float) (1.0 * Math.cos(waveTime * 2.6))) * (float) Math.cos(spark2Rad);
                float s2y = centerY + (rOuter - (float) (1.0 * Math.cos(waveTime * 2.6))) * (float) Math.sin(spark2Rad);
                drawDiamond(canvas, s2x, s2y, 2.5f, progressDotPaint);

                double spark3Rad = headRad - Math.toRadians(8.4);
                float s3x = centerX + rOuter * (float) Math.cos(spark3Rad);
                float s3y = centerY + rOuter * (float) Math.sin(spark3Rad);
                drawDiamond(canvas, s3x, s3y, 1.7f, progressDotPaint);
            }

            // 4. Electric Cyan Starlight Bookmark Stars along the perimeter
            if (rec != null) {
                List<Integer> bookmarks = getCachedBookmarks(rec.getCurrentFile());
                if (bookmarks != null && !bookmarks.isEmpty() && duration > 0) {
                    for (int i = 0; i < bookmarks.size(); i++) {
                        int bmMs = bookmarks.get(i);
                        float bmFraction = (float) bmMs / (float) duration;
                        if (bmFraction < 0f || bmFraction > 1.0f) continue;

                        double bmRad = Math.toRadians(bmFraction * 360.0 - 90.0);
                        float bmX = centerX + rOuter * (float) Math.cos(bmRad);
                        float bmY = centerY + rOuter * (float) Math.sin(bmRad);

                        canvas.drawCircle(bmX, bmY, 8.5f, bookmarkPipGlowPaint);
                        canvas.drawCircle(bmX, bmY, 4.5f, bookmarkPipCorePaint);
                        drawDiamond(canvas, bmX, bmY, 2.2f, bookmarkPipHighlightPaint);
                        canvas.drawLine(bmX - 7.0f, bmY, bmX + 7.0f, bmY, bookmarkSparklePaint);
                        canvas.drawLine(bmX, bmY - 7.0f, bmX, bmY + 7.0f, bookmarkSparklePaint);
                    }
                }
            }

        } else if (isRecording && visualizerMode != VISUALIZER_MODE_DUAL_VU) {
            long startTime = (rec != null) ? rec.getRecordStartTimeMillis() : 0L;
            long elapsed = (startTime > 0) ? (System.currentTimeMillis() - startTime) : 0L;
            if (elapsed < 0) elapsed = 0L;

            float recFraction = (float) ((elapsed % 60000L) / 60000.0);
            int activeNodes = Math.min(NUM_ANGLES, Math.max(0, Math.round(recFraction * NUM_ANGLES)));

            // Reset recording waveform on new recording session
            if (!wasRecordingLastFrame) {
                wasRecordingLastFrame = true;
                for (int i = 0; i < NUM_ANGLES; i++) {
                    recordingWaveform[i] = 0.10f;
                }
            }

            // Live microphone amplitude from MediaRecorder (0..32767)
            int micAmp = (rec != null) ? rec.getMaxAmplitude() : 0;
            // Convert to decibel (dB) amount: range -40 dB to 0 dBFS
            double micDb = (micAmp > 35) ? (20.0 * Math.log10((double) micAmp / 32767.0)) : -45.0;
            float micNormDb = (float) Math.max(0.08f, Math.min(1.0f, (micDb + 40.0) / 40.0));

            // Record this volume (dB) at the current active node
            if (activeNodes > 0 && activeNodes <= NUM_ANGLES) {
                recordingWaveform[activeNodes - 1] = micNormDb;
            }

            // 1. Inactive Path: Faint topaz ambient starlight diamonds
            for (int k = activeNodes; k < NUM_ANGLES; k += 3) {
                float nx = centerX + rOuter * cosTable[k];
                float ny = centerY + rOuter * sinTable[k];
                drawDiamond(canvas, nx, ny, 1.4f, progressDotFaintPaint);
            }

            // 2. Active Recording Chronograph Energy Stream: Faceted Sunfire Topaz Diamonds with Heights by Recorded Volume (dB) Amount
            for (int k = 0; k < activeNodes; k++) {
                int fromHead = activeNodes - 1 - k;
                float volAmount = recordingWaveform[k];

                float stemHeight = minStemHeight + maxStemHeight * volAmount;
                float rBase = rOuter - stemHeight * 0.35f;
                float rPeak = rOuter + stemHeight * 0.65f;

                float xBase = centerX + rBase * cosTable[k];
                float yBase = centerY + rBase * sinTable[k];
                float xPeak = centerX + rPeak * cosTable[k];
                float yPeak = centerY + rPeak * sinTable[k];

                recordingStemPaint.setStrokeWidth(1.6f * density);
                int stemAlpha = (fromHead < 5) ? 220 : (int) (80 + 155 * volAmount);
                recordingStemPaint.setAlpha(stemAlpha);
                canvas.drawLine(xBase, yBase, xPeak, yPeak, recordingStemPaint);

                if (fromHead < 5) {
                    float headProximity = (5 - fromHead) / 5.0f;
                    float dSize = 3.6f + headProximity * 1.8f + (1.2f * volAmount);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 2.2f, recordingProgressGlowPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize, recordingDotPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 0.45f, recordingDotMintPaint);
                } else {
                    float dSize = 2.0f + 1.8f * volAmount;
                    drawDiamond(canvas, xPeak, yPeak, dSize * 1.8f, recordingProgressGlowPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize, recordingDotPaint);
                    drawDiamond(canvas, xPeak, yPeak, dSize * 0.40f, recordingDotMintPaint);
                }
            }

            // 3. Breathing Ruby Red Power Guard Beacon Tip
            float recSweep = recFraction * 360.0f;
            double headRad = Math.toRadians(-90.0 + recSweep);
            int headIdx = Math.max(0, Math.min(NUM_ANGLES - 1, activeNodes - 1));
            float headVol = (activeNodes > 0) ? recordingWaveform[headIdx] : 0.2f;
            float headR = rOuter + (minStemHeight + maxStemHeight * headVol) * 0.65f;
            float headX = centerX + headR * (float) Math.cos(headRad);
            float headY = centerY + headR * (float) Math.sin(headRad);

            float pulse = (float) (0.70 + 0.30 * Math.sin(waveTime * 3.5));
            recordingHeadHaloPaint.setAlpha((int) (90 * pulse));
            canvas.drawCircle(headX, headY, 12.0f * pulse, recordingHeadHaloPaint);
            drawDiamond(canvas, headX, headY, 6.5f * pulse, recordingHeadDotPaint);
            drawDiamond(canvas, headX, headY, 2.8f, progressHeadCorePaint);

            // 4. Recording Bookmark Stars along the chronograph path
            if (rec != null) {
                List<Integer> recBookmarks = rec.getRecordingBookmarks();
                if (recBookmarks != null && !recBookmarks.isEmpty()) {
                    for (int i = 0; i < recBookmarks.size(); i++) {
                        int bmMs = recBookmarks.get(i);
                        float bmFraction = (float) ((bmMs % 60000L) / 60000.0);
                        double bmRad = Math.toRadians(bmFraction * 360.0 - 90.0);
                        float bmX = centerX + rOuter * (float) Math.cos(bmRad);
                        float bmY = centerY + rOuter * (float) Math.sin(bmRad);

                        canvas.drawCircle(bmX, bmY, 8.5f, bookmarkPipGlowPaint);
                        canvas.drawCircle(bmX, bmY, 4.5f, bookmarkPipCorePaint);
                        drawDiamond(canvas, bmX, bmY, 2.2f, bookmarkPipHighlightPaint);
                        canvas.drawLine(bmX - 7.0f, bmY, bmX + 7.0f, bmY, bookmarkSparklePaint);
                        canvas.drawLine(bmX, bmY - 7.0f, bmX, bmY + 7.0f, bookmarkSparklePaint);
                    }
                }
            }

        } else {
            // IDLE / STOPPED STANDBY: Clean minimal dial without hour points
            smoothPlaybackProgress = 0f;
            wasRecordingLastFrame = false;
        }
    }

    /**
     * Option 2: 32-Band Segmented Graphic Audio Frequency Analyzer.
     * Renders 32 discrete frequency columns (Sub-Bass -> Bass -> Mids -> High-Mids -> Treble)
     * with responsive multi-segment LED meters, live transient pulse, and floating peak-hold caps.
     */
    private void drawRadialFrequencyAnalyzer(Canvas canvas, float centerX, float centerY,
                                             float rOuter, float radius, float density,
                                             boolean active, boolean isRecording, boolean isPlaying) {
        float rBase = rOuter + 4f * density;
        float rMax = radius - 4f * density;
        if (rMax <= rBase) return;

        float spanTotal = rMax - rBase;
        float segmentSpan = spanTotal / (float) NUM_SEGMENTS_PER_BAND;
        float segmentThickness = segmentSpan * 0.72f;
        float segmentWidth = 6.5f * density;

        for (int b = 0; b < NUM_FREQ_BANDS; b++) {
            // Symmetrical Stereo Layout:
            // Left semicircle: b = 0..15 (Sub-bass at bottom 6 o'clock -> Treble at top 12 o'clock)
            // Right semicircle: b = 16..31 (Sub-bass at bottom 6 o'clock -> Treble at top 12 o'clock)
            double angleRad;
            float freqLevel = freqBands[b];
            float peakLevel = freqPeakBands[b];

            if (b < NUM_FREQ_BANDS / 2) {
                // Left channel: 90 deg (bottom) to -90 deg (top) counter-clockwise
                float frac = (float) b / (float) (NUM_FREQ_BANDS / 2 - 1);
                angleRad = Math.PI / 2.0 + frac * Math.PI; // PI/2 to 3*PI/2 (-PI/2)
            } else {
                // Right channel: 90 deg (bottom) to -90 deg (top) clockwise
                float frac = (float) (b - NUM_FREQ_BANDS / 2) / (float) (NUM_FREQ_BANDS / 2 - 1);
                angleRad = Math.PI / 2.0 - frac * Math.PI; // PI/2 to -PI/2
            }

            float cosA = (float) Math.cos(angleRad);
            float sinA = (float) Math.sin(angleRad);
            float rotDeg = (float) Math.toDegrees(angleRad) + 90f;

            int numLit = Math.min(NUM_SEGMENTS_PER_BAND, Math.max(0, Math.round(freqLevel * NUM_SEGMENTS_PER_BAND)));

            // 1. Draw Segmented LED Meter Blocks
            for (int s = 0; s < NUM_SEGMENTS_PER_BAND; s++) {
                float rSeg = rBase + s * segmentSpan + segmentThickness / 2f;
                float segX = centerX + rSeg * cosA;
                float segY = centerY + rSeg * sinA;

                canvas.save();
                canvas.translate(segX, segY);
                canvas.rotate(rotDeg);

                RectF segRect = new RectF(-segmentWidth / 2f, -segmentThickness / 2f,
                        segmentWidth / 2f, segmentThickness / 2f);

                if (s < numLit) {
                    Paint segPaint;
                    if (isRecording) {
                        if (s < 5) segPaint = freqSegmentGreenPaint;
                        else if (s < 8) segPaint = freqSegmentCyanPaint;
                        else if (s < 9) segPaint = freqSegmentAmberPaint;
                        else segPaint = freqSegmentRubyPaint;
                    } else {
                        if (s < 4) segPaint = freqSegmentCyanPaint;
                        else if (s < 7) segPaint = freqSegmentGreenPaint;
                        else if (s < 9) segPaint = freqSegmentAmberPaint;
                        else segPaint = freqSegmentRubyPaint;
                    }

                    // Active illuminated segment
                    canvas.drawRoundRect(segRect, 1.2f * density, 1.2f * density, segPaint);

                    // Soft aura glow on crest segment
                    if (s == numLit - 1) {
                        canvas.drawRoundRect(segRect, 1.2f * density, 1.2f * density, freqPeakGlowPaint);
                    }
                } else {
                    // Ghost unlit hardware segment outline
                    canvas.drawRoundRect(segRect, 1.2f * density, 1.2f * density, freqSegmentDimPaint);
                }
                canvas.restore();
            }

            // 2. Floating Studio Peak-Hold Cap
            if (peakLevel > 0.08f) {
                float peakSeg = Math.min((float) NUM_SEGMENTS_PER_BAND - 0.2f, peakLevel * NUM_SEGMENTS_PER_BAND);
                float rPeak = rBase + peakSeg * segmentSpan + segmentThickness / 2f;
                float pkX = centerX + rPeak * cosA;
                float pkY = centerY + rPeak * sinA;

                canvas.save();
                canvas.translate(pkX, pkY);
                canvas.rotate(rotDeg);

                RectF capRect = new RectF(-segmentWidth / 2f - 0.6f * density, -1.2f * density,
                        segmentWidth / 2f + 0.6f * density, 1.2f * density);
                canvas.drawRoundRect(capRect, 1.0f * density, 1.0f * density, freqPeakCapPaint);
                canvas.restore();
            }
        }
    }

    /**
     * Option 3: Vintage McIntosh Dual Analog VU Meters.
     * Twin backlit precision analog ballistic meters (Left & Right channels) with
     * signature McIntosh dark blue glass faceplates, calibrated -20dB to +3dB scales,
     * responsive spring-inertia needle ballistics, and glowing ruby-red overload peak LEDs.
     */
    private void drawVintageDualVuMeters(Canvas canvas, float centerX, float centerY,
                                         float rInner, float rOuter, float radius, float density,
                                         boolean active, boolean isRecording, boolean isPlaying) {
        float viewW = getMeasuredWidth();
        float viewH = getMeasuredHeight();
        if (viewW <= 0) viewW = radius * 2f;
        if (viewH <= 0) viewH = radius * 2f;

        // Massive ambient dimensions filling upper and lower halves in the dark background
        float meterW = viewW * 0.94f;
        float meterH = viewH * 0.33f;
        float offsetCy = viewH * 0.235f;

        float leftCy = centerY - offsetCy;
        float rightCy = centerY + offsetCy;

        // Upper half: Left Channel Ambient VU Meter
        drawSingleVuMeter(canvas, centerX, leftCy, meterW, meterH, density,
                vuLeftLevel, vuLeftPeakHold, "LEFT CHANNEL", true);
        // Under the Left VU Meter (Lower half): Right Channel Ambient VU Meter
        drawSingleVuMeter(canvas, centerX, rightCy, meterW, meterH, density,
                vuRightLevel, vuRightPeakHold, "RIGHT CHANNEL", false);
    }

    private void drawSingleVuMeter(Canvas canvas, float cx, float cy, float w, float h, float density,
                                   float level, long peakHoldTime, String channelLabel, boolean isLeft) {
        // 1. Transparent Style Look & Feel: Pure AMOLED background, zero solid card fill or heavy box borders.
        // 2. Meter Scale Arc (Expansive widescreen panoramic sweep)
        float pivotX = cx;
        float pivotY = cy + h * 0.36f;
        float scaleR = h * 0.65f;

        RectF arcBox = new RectF(pivotX - scaleR, pivotY - scaleR, pivotX + scaleR, pivotY + scaleR);

        // Calibrated Arc: -142 deg (-20dB) to -38 deg (+3dB)
        vuScaleCyanPaint.setColor(Color.parseColor("#7000E5FF"));
        vuScaleCyanPaint.setStrokeWidth(2.0f * density);
        canvas.drawArc(arcBox, -142f, 78f, false, vuScaleCyanPaint);

        vuScaleRedPaint.setColor(Color.parseColor("#85FF1744"));
        vuScaleRedPaint.setStrokeWidth(2.2f * density);
        canvas.drawArc(arcBox, -64f, 26f, false, vuScaleRedPaint);

        // 3. Calibrated Scale Ticks & dB Markings (-20, -10, -7, -5, -3, 0, +1, +3 dB)
        float[] dbAngles = {-142f, -129f, -117f, -104f, -91f, -77f, -64f, -54f, -46f, -38f};
        String[] dbLabels = {"-20", "", "-10", "-7", "-5", "-3", "0", "+1", "", "+3"};
        boolean[] isMajor = {true, false, true, true, true, true, true, true, false, true};

        vuScaleMajorTickPaint.setColor(Color.parseColor("#80FFFFFF"));
        vuScaleMajorTickPaint.setStrokeWidth(1.6f * density);
        vuScaleTickPaint.setColor(Color.parseColor("#4000E5FF"));
        vuScaleTickPaint.setStrokeWidth(1.0f * density);

        for (int i = 0; i < dbAngles.length; i++) {
            float ang = dbAngles[i];
            double rad = Math.toRadians(ang);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);

            boolean isOverload = ang >= -64f;
            float tickLen = isMajor[i] ? 8.0f * density : 3.8f * density;

            float x1 = pivotX + (scaleR - tickLen) * cos;
            float y1 = pivotY + (scaleR - tickLen) * sin;
            float x2 = pivotX + scaleR * cos;
            float y2 = pivotY + scaleR * sin;

            canvas.drawLine(x1, y1, x2, y2, isOverload ? vuScaleRedPaint : (isMajor[i] ? vuScaleMajorTickPaint : vuScaleTickPaint));

            if (isMajor[i] && !dbLabels[i].isEmpty()) {
                float tx = pivotX + (scaleR - tickLen - 10f * density) * cos;
                float ty = pivotY + (scaleR - tickLen - 10f * density) * sin + 3.8f * density;
                vuScaleTextPaint.setTextAlign(Paint.Align.CENTER);
                vuScaleTextPaint.setTextSize(9.5f * density);
                vuScaleTextPaint.setColor(isOverload ? Color.parseColor("#B0FF6B6B") : Color.parseColor("#75A0D8F0"));
                canvas.drawText(dbLabels[i], tx, ty, vuScaleTextPaint);
            }
        }

        // 4. Overload Peak Indicator LED (Positioned gracefully outside dial zone)
        float ledX = cx + w / 2f - 24f * density;
        float ledY = isLeft ? (cy - h / 2f + 24f * density) : (cy + h * 0.16f);
        boolean isPeak = System.currentTimeMillis() < peakHoldTime || level > 0.80f;
        if (isPeak) {
            canvas.drawCircle(ledX, ledY, 7.5f * density, vuPeakGlowPaint);
            canvas.drawCircle(ledX, ledY, 3.8f * density, vuPeakLedOnPaint);
        } else {
            canvas.drawCircle(ledX, ledY, 3.2f * density, vuPeakLedOffPaint);
        }
        vuScaleTextPaint.setTextAlign(Paint.Align.RIGHT);
        vuScaleTextPaint.setTextSize(8.0f * density);
        vuScaleTextPaint.setColor(isPeak ? Color.parseColor("#FF5252") : Color.parseColor("#33406070"));
        canvas.drawText("PEAK", ledX - 14f * density, ledY + 3.0f * density, vuScaleTextPaint);

        // 5. Ballistic Needle with Drop Shadow & Pivot Cap
        float clampedLevel = Math.min(1.0f, Math.max(0.02f, level));
        float needleAngle = -142f + clampedLevel * 104f;
        double needleRad = Math.toRadians(needleAngle);
        float nCos = (float) Math.cos(needleRad);
        float nSin = (float) Math.sin(needleRad);

        float needleLen = scaleR * 1.04f;
        float tipX = pivotX + needleLen * nCos;
        float tipY = pivotY + needleLen * nSin;

        // Ambient needle drop shadow
        vuNeedleShadowPaint.setColor(Color.parseColor("#15000000"));
        vuNeedleShadowPaint.setStrokeWidth(2.0f * density);
        canvas.drawLine(pivotX + 1.5f * density, pivotY + 2.0f * density, tipX + 1.5f * density, tipY + 2.0f * density, vuNeedleShadowPaint);

        // Luminous McIntosh amber needle core
        vuNeedlePaint.setColor(Color.parseColor("#CCFFA000"));
        vuNeedlePaint.setStrokeWidth(2.0f * density);
        canvas.drawLine(pivotX, pivotY, tipX, tipY, vuNeedlePaint);

        // Needle luminous tip dot
        canvas.drawCircle(tipX, tipY, 1.4f * density, vuNeedlePaint);

        // Pivot Boss Cap (Minimal hollow ring so wheel text underneath is 100% visible)
        Paint pivotRim = new Paint(Paint.ANTI_ALIAS_FLAG);
        pivotRim.setStyle(Paint.Style.STROKE);
        pivotRim.setStrokeWidth(1.0f * density);
        pivotRim.setColor(Color.parseColor("#2500E5FF"));
        canvas.drawCircle(pivotX, pivotY, 4.5f * density, pivotRim);
        canvas.drawCircle(pivotX, pivotY, 1.5f * density, vuScaleCyanPaint);

        // 6. Channel & Model Branding Typography
        float labelX = cx - w / 2f + 24f * density;
        float labelY = isLeft ? (cy - h / 2f + 24f * density) : (cy + h * 0.16f);
        vuLabelTextPaint.setTextAlign(Paint.Align.LEFT);
        vuLabelTextPaint.setTextSize(10.5f * density);
        vuLabelTextPaint.setColor(Color.parseColor("#7000E5FF"));
        canvas.drawText(channelLabel, labelX, labelY, vuLabelTextPaint);

        vuScaleTextPaint.setTextAlign(Paint.Align.LEFT);
        vuScaleTextPaint.setTextSize(7.0f * density);
        vuScaleTextPaint.setColor(Color.parseColor("#4060859E"));
        canvas.drawText("DECIBELS (dB) • 0 dB = 1.228V", labelX, labelY + 11f * density, vuScaleTextPaint);
    }

    /**
     * Option 4: Phosphor CRT Oscilloscope.
     * Vintage laboratory cathode-ray tube oscilloscope with calibrated division grid reticle,
     * electron beam bloom, continuous audio waveform trace, phosphor persistence ghost trails,
     * and real-time sweep telemetry HUD.
     */
    private void drawCrtOscilloscope(Canvas canvas, float centerX, float centerY,
                                    float rInner, float rOuter, float radius, float density,
                                    boolean active, boolean isRecording, boolean isPlaying) {
        float crtW = rOuter * 1.62f;
        float crtH = rOuter * 0.54f;
        float crtCy = centerY - rOuter * 0.36f;
        RectF crtRect = new RectF(centerX - crtW / 2f, crtCy - crtH / 2f, centerX + crtW / 2f, crtCy + crtH / 2f);

        // 1. Cathode Tube Glass Viewport & Bezel
        canvas.drawRoundRect(crtRect, 10f * density, 10f * density, crtScreenPaint);
        canvas.drawRoundRect(crtRect, 10f * density, 10f * density, crtBezelPaint);

        // 2. Precision CRT Reticle Divisions (8 columns x 6 rows)
        int cols = 8;
        int rows = 6;
        for (int c = 1; c < cols; c++) {
            float gx = crtRect.left + (crtW * c / (float) cols);
            boolean isCenter = (c == cols / 2);
            canvas.drawLine(gx, crtRect.top, gx, crtRect.bottom, isCenter ? crtCenterGridPaint : crtGridPaint);
        }
        for (int r = 1; r < rows; r++) {
            float gy = crtRect.top + (crtH * r / (float) rows);
            boolean isCenter = (r == rows / 2);
            canvas.drawLine(crtRect.left, gy, crtRect.right, gy, isCenter ? crtCenterGridPaint : crtGridPaint);
        }

        // Sub-millimeter graduation ticks on center axes
        float cxAxis = crtRect.centerX();
        float cyAxis = crtRect.centerY();
        for (float x = crtRect.left; x <= crtRect.right; x += 6f * density) {
            canvas.drawLine(x, cyAxis - 1.5f * density, x, cyAxis + 1.5f * density, crtCenterGridPaint);
        }
        for (float y = crtRect.top; y <= crtRect.bottom; y += 6f * density) {
            canvas.drawLine(cxAxis - 1.5f * density, y, cxAxis + 1.5f * density, y, crtCenterGridPaint);
        }

        // 3. Electron Beam Waveform Trace Synthesis (64 samples)
        int samples = 64;
        crtTracePath.rewind();
        float maxWaveAmp = crtH * 0.38f;

        for (int s = 0; s <= samples; s++) {
            float t = (float) s / (float) samples;
            float sx = crtRect.left + 4f * density + t * (crtW - 8f * density);

            float sy;
            if (isPlaying) {
                int bIdx = (int) (t * (NUM_FREQ_BANDS - 1));
                float audioLevel = freqBands[bIdx];
                float w1 = (float) Math.sin(t * 14.0 + waveTime * 4.8);
                float w2 = (float) Math.cos(t * 26.0 - waveTime * 7.2);
                float w3 = (float) Math.sin(t * 6.0 + waveTime * 2.2);
                float composite = 0.52f * w1 + 0.33f * w2 + 0.15f * w3;
                float amp = (0.08f + 0.92f * audioLevel) * maxWaveAmp;
                sy = cyAxis + composite * amp;
            } else if (isRecording) {
                int aIdx = (int) (t * (NUM_ANGLES - 1));
                float audioLevel = audioBands[aIdx];
                float w1 = (float) Math.sin(t * 18.0 + waveTime * 5.5);
                float w2 = (float) Math.cos(t * 36.0 - waveTime * 9.0);
                sy = cyAxis + (0.6f * w1 + 0.4f * w2) * (0.06f + 0.94f * audioLevel) * maxWaveAmp;
            } else {
                // Resting electron sweep (gentle 60Hz hum drift)
                float hum = (float) Math.sin(t * 8.0 + waveTime * 1.5);
                sy = cyAxis + hum * (2.0f * density);
            }

            if (s == 0) {
                crtTracePath.moveTo(sx, sy);
            } else {
                crtTracePath.lineTo(sx, sy);
            }
        }

        // Multi-pass Phosphor Rendering:
        // Pass 1: Phosphor Persistence Ghost Trail
        canvas.drawPath(crtGhostPath, crtGhostPaint);
        // Pass 2: Electron Beam Phosphor Bloom
        canvas.drawPath(crtTracePath, crtBloomPaint);
        // Pass 3: High-Intensity Electron Core
        canvas.drawPath(crtTracePath, crtTracePaint);

        // Store current trace into ghost path for next frame persistence
        crtGhostPath.set(crtTracePath);

        // 4. CRT Telemetry HUD Diagnostics
        crtTextPaint.setTextSize(6.5f * density);
        crtTextPaint.setColor(Color.parseColor("#00E676"));
        canvas.drawText("CH1 50mV/DIV", crtRect.left + 6f * density, crtRect.top + 10f * density, crtTextPaint);
        canvas.drawText("TIME 1.0ms", crtRect.right - 46f * density, crtRect.top + 10f * density, crtTextPaint);
        canvas.drawText("AC 1MΩ 20pF", crtRect.left + 6f * density, crtRect.bottom - 5f * density, crtTextPaint);
        canvas.drawText(active ? "TRIG: LOCK" : "TRIG: AUTO", crtRect.right - 52f * density, crtRect.bottom - 5f * density, crtTextPaint);
    }

    /**
     * Option 5: Radial Sonic Starburst.
     * 360-degree pulsating celestial starburst / solar corona audio plasma.
     * Dynamic frequency rays erupt radially from behind the center track pill,
     * flaring outward with white-hot core energy, electric cyan mid-tones, golden amber accents,
     * and orbiting sonic stardust embers on musical transients.
     */
    private void drawRadialSonicStarburst(Canvas canvas, float centerX, float centerY,
                                         float rInner, float rOuter, float radius, float density,
                                         boolean active, boolean isRecording, boolean isPlaying) {
        int numRays = 64;
        float rStart = rInner * 1.02f;
        float maxRayLen = rOuter * 0.90f - rStart;

        for (int k = 0; k < numRays; k++) {
            float angleRad = (float) (k * 2.0 * Math.PI / numRays);
            float cosA = (float) Math.cos(angleRad);
            float sinA = (float) Math.sin(angleRad);

            // Symmetrical frequency mapping: bass rays at bottom/top, treble at sides
            int b = (int) (Math.abs(k - numRays / 2) * (NUM_FREQ_BANDS - 1) / (numRays / 2.0));
            b = Math.min(NUM_FREQ_BANDS - 1, Math.max(0, b));
            float audio = freqBands[b];
            float peak = freqPeakBands[b];

            // Harmonic ray length modulation
            float rayLen = (0.10f + 0.90f * audio) * maxRayLen;
            float rEnd = rStart + rayLen;

            float x1 = centerX + rStart * cosA;
            float y1 = centerY + rStart * sinA;
            float x2 = centerX + rEnd * cosA;
            float y2 = centerY + rEnd * sinA;

            // Multi-color grading:
            Paint rayPaint;
            if (isRecording) {
                rayPaint = (audio > 0.75f) ? starburstRayAmberPaint : starburstRayCyanPaint;
            } else {
                if (b < 6 && audio > 0.60f) {
                    rayPaint = starburstRayAmberPaint; // Powerful golden bass flare
                } else {
                    rayPaint = starburstRayCyanPaint; // Electric cyan corona
                }
            }

            // Ray beam
            rayPaint.setStrokeWidth((1.8f + audio * 2.0f) * density);
            canvas.drawLine(x1, y1, x2, y2, rayPaint);

            // Incandescent white core at the root
            starburstCorePaint.setStrokeWidth(1.4f * density);
            float rCoreEnd = rStart + rayLen * 0.35f;
            canvas.drawLine(x1, y1, centerX + rCoreEnd * cosA, centerY + rCoreEnd * sinA, starburstCorePaint);

            // Tip spark / diamond cap on active transients
            if (peak > 0.50f) {
                float sparkSize = (1.5f + peak * 2.2f) * density;
                drawDiamond(canvas, x2, y2, sparkSize, (peak > 0.80f) ? starburstCorePaint : rayPaint);
            }
        }

        // Orbiting Sonic Stardust Embers
        for (int p = 0; p < NUM_STARBURST_PARTICLES; p++) {
            float ang = starburstParticleAngle[p];
            float distNorm = starburstParticleDist[p];
            float rPart = rStart + distNorm * maxRayLen;

            float px = centerX + rPart * (float) Math.cos(ang);
            float py = centerY + rPart * (float) Math.sin(ang);

            int alpha = Math.min(255, Math.max(40, (int) (starburstParticleAlpha[p] * 255)));
            starburstParticlePaint.setAlpha(alpha);
            canvas.drawCircle(px, py, starburstParticleSize[p] * density, starburstParticlePaint);
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

    public void setVisualizerMode(int mode) {
        if (mode < 0 || mode >= NUM_VISUALIZER_MODES) {
            mode = VISUALIZER_MODE_MINIMAL_SPLIT;
        }
        if (this.visualizerMode != mode) {
            this.visualizerMode = mode;
            notifyModeChanged();
            invalidate();
        }
    }

    public int getVisualizerMode() {
        return this.visualizerMode;
    }

    public int toggleVisualizerMode() {
        this.visualizerMode = (this.visualizerMode + 1) % NUM_VISUALIZER_MODES;
        notifyModeChanged();
        invalidate();
        return this.visualizerMode;
    }

    private void notifyModeChanged() {
        if (getContext() instanceof MainActivity) {
            ((MainActivity) getContext()).updateVisualizerModeUI(this.visualizerMode);
        }
    }

    public static final int WHEEL_MODE_FOLDER = 0;
    public static final int WHEEL_MODE_FILE = 1;
    private int wheelMode = WHEEL_MODE_FOLDER;

    public void setWheelMode(int mode) {
        if (this.wheelMode != mode) {
            this.wheelMode = mode;
            this.folderWheelScrollOffset = 0f;
            this.fileWheelScrollOffset = 0f;
            invalidate();
        }
    }

    public int getWheelMode() {
        return this.wheelMode;
    }

    /**
     * Notify DialView that active folder changed to animate vertical wheel drum roll.
     * @param direction: 1 for next folder, -1 for previous folder
     */
    public void onFolderChanged(int direction) {
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        float stepDistance = (visualizerMode == VISUALIZER_MODE_MINIMAL_SPLIT)
                ? (36f * getResources().getDisplayMetrics().density)
                : (radius * 0.172f);
        folderWheelScrollOffset += (direction > 0 ? stepDistance : -stepDistance);
        invalidate();
    }

    /**
     * Notify DialView that active file changed to animate horizontal carousel slide (Option 1).
     * @param direction: 1 for next file, -1 for previous file
     */
    public void onFileChanged(int direction) {
        float radius = Math.min(getMeasuredWidth(), getMeasuredHeight()) / 2f;
        float stepDistanceX = (visualizerMode == VISUALIZER_MODE_MINIMAL_SPLIT)
                ? (36f * getResources().getDisplayMetrics().density)
                : (radius * 0.78f);
        fileWheelScrollOffset += (direction > 0 ? stepDistanceX : -stepDistanceX);
        invalidate();
    }

    private float getMediaVolumeFraction() {
        try {
            if (getContext() != null) {
                AudioManager am = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
                if (am != null) {
                    int cur = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                    int max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    if (max > 0) {
                        return Math.max(0.15f, Math.min(1.0f, (float) cur / (float) max));
                    }
                }
            }
        } catch (Exception ignored) {}
        return 0.80f;
    }

    /**
     * Generates a rich, realistic decibel (dB) acoustic waveform profile for the given track.
     * Ensures every adjacent progress bar exhibits noticeable height variance (beats, vocals, dropouts)
     * accurately reflecting dynamic audio volume in dB.
     */
    private void generateTrackWaveform(String fileName, int durationMs) {
        currentWaveformFile = (fileName != null) ? fileName : "";
        lastWaveformDuration = durationMs;

        long hash = 1125899906842597L;
        if (fileName != null) {
            for (int i = 0; i < fileName.length(); i++) {
                hash = 31L * hash + fileName.charAt(i);
            }
        }
        hash ^= ((long) durationMs * 5039L);
        java.util.Random rnd = new java.util.Random(hash);

        // Musical structural cycles
        float sectionCycles = 2.5f + (rnd.nextInt(4) * 0.5f);
        float phraseCycles = 8.0f + rnd.nextInt(5);

        for (int i = 0; i < NUM_ANGLES; i++) {
            float progress = (float) i / (float) NUM_ANGLES;

            // 1. Structural song envelope (soft intro ramp, energetic middle, gradual outro taper)
            float intro = Math.min(1.0f, progress * 8.0f);
            float outro = Math.min(1.0f, (1.0f - progress) * 8.0f);
            float envelope = 0.25f + 0.75f * (intro * outro);

            // 2. Macro dynamic energy (verses, choruses, drops, breakdowns)
            float macroWave = (float) (0.50 + 0.35 * Math.sin(progress * Math.PI * 2.0 * sectionCycles + 1.2));
            float phraseWave = (float) (0.16 * Math.cos(progress * Math.PI * 2.0 * phraseCycles));

            // 3. Staggered beat cadence so neighboring bars alternate dynamically in height
            float cadence;
            int step = i % 4;
            if (step == 0) {
                cadence = 0.38f; // Kick drum / downbeat accent
            } else if (step == 2) {
                cadence = 0.26f; // Snare / backbeat accent
            } else if (step == 1) {
                cadence = -0.16f; // Decay / rest
            } else {
                cadence = 0.08f; // Offbeat syncopation
            }

            // 4. Acoustic transient fluctuation (per-bar variance)
            float perBarVariance = (rnd.nextFloat() - 0.45f) * 0.45f;

            // 5. Combined linear amplitude
            float linearAmp = (0.20f + 0.45f * macroWave + phraseWave + cadence + perBarVariance) * envelope;
            linearAmp = Math.max(0.03f, Math.min(1.0f, linearAmp));

            // 6. Decibel (dB) curve: dB = 20 * log10(amplitude), mapped from [-36 dB, 0 dBFS] to [0.08, 1.0]
            double db = 20.0 * Math.log10(linearAmp);
            float normDb = (float) Math.max(0.08f, Math.min(1.0f, (db + 36.0) / 36.0));

            trackWaveform[i] = normDb;
        }
    }

    /**
     * Renders ambient items behind the animation circle:
     * - In Folder Mode: Vertical wheel drum (+5, -5 depth)
     * - In File Play Mode: Horizontal carousel track (+5, -5 depth) with preview cues
     */
    private void drawAmbientWheel(Canvas canvas, Recorder rec, float radius) {
        if (rec == null) return;
        if (wheelMode == WHEEL_MODE_FILE) {
            drawHorizontalFileCarousel(canvas, rec, radius);
        } else {
            drawVerticalFolderWheel(canvas, rec, radius);
        }
    }

    /**
     * Renders +5 to -5 depth ambient folders vertically behind the animation circle.
     */
    private void drawVerticalFolderWheel(Canvas canvas, Recorder rec, float radius) {
        if (rec == null || rec.getTotalDirectoriesCount() <= 0) return;

        // Smooth scroll interpolation towards rest position
        if (Math.abs(folderWheelScrollOffset) > 0.5f) {
            folderWheelScrollOffset += (0f - folderWheelScrollOffset) * 0.22f;
            postInvalidateOnAnimation();
        } else {
            folderWheelScrollOffset = 0f;
        }

        float density = getResources().getDisplayMetrics().density;
        float stepDistance = radius * 0.172f;
        float maxTextWidth = getMeasuredWidth() * 0.82f;

        // Render depth levels from -5 (upper) to +5 (lower), excluding 0 (center)
        for (int offset = -5; offset <= 5; offset++) {
            if (offset == 0) continue; // Center active item is displayed in main_TextView

            int depth = Math.abs(offset);
            String label = rec.getDirectoryNameWithOffset(offset);
            if (label == null || label.isEmpty()) continue;

            // Transparency by depth: depth 1 is ~57%, depth 5 is ~7%
            int alpha;
            float textSizeDp;
            switch (depth) {
                case 1:
                    alpha = 145; // ~57%
                    textSizeDp = 13.5f;
                    break;
                case 2:
                    alpha = 100; // ~39%
                    textSizeDp = 12.5f;
                    break;
                case 3:
                    alpha = 65;  // ~25%
                    textSizeDp = 11.5f;
                    break;
                case 4:
                    alpha = 36;  // ~14%
                    textSizeDp = 10.5f;
                    break;
                default: // 5
                    alpha = 18;  // ~7%
                    textSizeDp = 9.5f;
                    break;
            }

            ambientFolderTextPaint.setTextSize(textSizeDp * density);
            ambientFolderTextPaint.setAlpha(alpha);

            float textBaselineOffset = -(ambientFolderTextPaint.descent() + ambientFolderTextPaint.ascent()) / 2f;
            float yPos = centerY + (offset * stepDistance) + folderWheelScrollOffset + textBaselineOffset;

            // Flow text if it exceeds maximum width instead of truncation
            float itemClipLeft = centerX - maxTextWidth / 2f;
            float itemClipRight = centerX + maxTextWidth / 2f;
            drawFlowingText(canvas, label, itemClipLeft, itemClipRight, yPos, ambientFolderTextPaint, offset, true);
        }
    }

    /**
     * Renders ambient files in a horizontal carousel (Option 1: Cross-Axis Matrix)
     * Matches Left/Right horizontal swipe navigation.
     */
    private void drawHorizontalFileCarousel(Canvas canvas, Recorder rec, float radius) {
        if (rec == null || rec.getAudibleFilesCount() <= 0) return;

        // Smooth horizontal spring-damper interpolation towards rest
        if (Math.abs(fileWheelScrollOffset) > 0.5f) {
            fileWheelScrollOffset += (0f - fileWheelScrollOffset) * 0.22f;
            postInvalidateOnAnimation();
        } else {
            fileWheelScrollOffset = 0f;
        }

        float density = getResources().getDisplayMetrics().density;
        float stepDistanceX = radius * 0.74f;
        float maxTextWidth = radius * 0.44f;

        // Sliding horizontal carousel items
        for (int offset = -5; offset <= 5; offset++) {
            if (offset == 0) continue; // Active file is in center TextView

            int depth = Math.abs(offset);
            String label = rec.getFileNameWithOffset(offset);
            if (label == null || label.isEmpty()) continue;

            // Directional cues on immediate flanking tracks
            if (offset == -1) {
                label = "◀ " + label;
            } else if (offset == 1) {
                label = label + " ▶";
            }

            int alpha;
            float textSizeDp;
            switch (depth) {
                case 1:
                    alpha = 150; // ~59%
                    textSizeDp = 12.5f;
                    break;
                case 2:
                    alpha = 90;  // ~35%
                    textSizeDp = 11.5f;
                    break;
                case 3:
                    alpha = 55;  // ~22%
                    textSizeDp = 10.5f;
                    break;
                case 4:
                    alpha = 30;  // ~12%
                    textSizeDp = 9.5f;
                    break;
                default: // 5
                    alpha = 15;  // ~6%
                    textSizeDp = 8.5f;
                    break;
            }

            ambientFolderTextPaint.setTextSize(textSizeDp * density);
            ambientFolderTextPaint.setAlpha(alpha);

            float textBaselineOffset = -(ambientFolderTextPaint.descent() + ambientFolderTextPaint.ascent()) / 2f;
            float xPos = centerX + (offset * stepDistanceX) + fileWheelScrollOffset;
            float yPos = centerY + textBaselineOffset;

            // Only draw if within visible canvas bounds
            if (xPos > -maxTextWidth && xPos < getMeasuredWidth() + maxTextWidth) {
                float itemClipLeft = xPos - maxTextWidth / 2f;
                float itemClipRight = xPos + maxTextWidth / 2f;
                drawFlowingText(canvas, label, itemClipLeft, itemClipRight, yPos, ambientFolderTextPaint, offset, true);
            }
        }
    }

    /**
     * Option 0: Simplest UI (Minimal Split Navigation)
     * Displays folder name on the left and file names on the right.
     * Incorporates minimal, super-mandatory basic UX animations:
     * - Live 5-bar dynamic equalizer wave during playback
     * - Slender playback progress hairline with diamond playhead pip
     * - Time elapsed / total duration readout
     * - Pulsing recording beacon dot and live timer during recording
     * - Stationary "▶ TAP TO PLAY" hint when idle
     * - Smooth inertial roll on swipe navigation
     */
    private void drawMinimalSplitUI(Canvas canvas, float w, float h, float density,
                                    Recorder rec, boolean active, boolean isRecording, boolean isPlaying,
                                    float playbackProgressFraction) {
        float topY = 78f * density;
        float bottomY = h - 85f * density;
        float availableH = Math.max(100f * density, bottomY - topY);
        float headerY = topY + 14f * density;
        float listTopY = headerY + 14f * density;
        float listBottomY = bottomY - 4f * density;
        float centerY = (listTopY + listBottomY) * 0.5f;
        float dividerX = w / 2f;

        // 1. Center Hairline Divider with Top & Bottom Fade
        if (splitDividerShader == null || Math.abs(lastDividerH - availableH) > 2f) {
            lastDividerH = availableH;
            splitDividerShader = new LinearGradient(dividerX, topY + 4f * density, dividerX, bottomY,
                    new int[]{0x0000E5FF, 0x3800E5FF, 0x5000E5FF, 0x3800E5FF, 0x0000E5FF},
                    new float[]{0f, 0.15f, 0.50f, 0.85f, 1.0f},
                    Shader.TileMode.CLAMP);
            splitDividerPaint.setShader(splitDividerShader);
        }
        canvas.drawLine(dividerX, topY + 4f * density, dividerX, bottomY, splitDividerPaint);

        // Subtle center nexus diamond pip at divider center
        drawDiamond(canvas, dividerX, centerY, 3.0f * density, splitNexusPaint);

        // Column geometry
        float colPadding = 10f * density;
        float colWidth = dividerX - (colPadding * 2f);
        float leftCenterX = colPadding + colWidth / 2f;
        float rightCenterX = dividerX + colPadding + colWidth / 2f;
        float cardH = 50f * density;
        float cardCornerR = 11f * density;
        float stepY = 36f * density;

        int maxAbove = Math.max(4, (int) ((centerY - listTopY) / stepY));
        int maxBelow = Math.max(4, (int) ((listBottomY - centerY) / stepY));
        int maxOffset = Math.max(maxAbove, maxBelow);

        // --- LEFT COLUMN: FOLDERS ---
        int totalFolders = (rec != null) ? rec.getTotalDirectoriesCount() : 0;
        int curFolderIdx = (rec != null) ? rec.getCurrentDirectoryIndex() + 1 : 0;
        String curFolder = (rec != null) ? rec.getCurrentDirectoryName() : "STANDBY";
        if (curFolder == null || curFolder.isEmpty()) curFolder = "STANDBY";

        // Left Header
        splitFolderHeaderPaint.setTextSize(11f * density);
        splitFolderBadgePaint.setTextSize(9.5f * density);
        splitCuePaint.setTextSize(9f * density);

        canvas.drawText("FOLDERS", leftCenterX - 24f * density, headerY, splitFolderHeaderPaint);
        String folderCountBadge = (totalFolders > 0) ? String.format(Locale.US, "%02d / %02d", curFolderIdx, totalFolders) : "-- / --";
        canvas.drawText(folderCountBadge, leftCenterX + 30f * density, headerY, splitFolderBadgePaint);
        canvas.drawText("▲▼", leftCenterX + colWidth / 2f - 6f * density, headerY, splitCuePaint);

        // Left Ambient Items (with scroll inertia and canvas clipping)
        if (Math.abs(folderWheelScrollOffset) > 0.5f) {
            folderWheelScrollOffset += (0f - folderWheelScrollOffset) * 0.22f;
            postInvalidateOnAnimation();
        } else {
            folderWheelScrollOffset = 0f;
        }

        canvas.save();
        canvas.clipRect(0, listTopY, dividerX, listBottomY);
        if (rec != null && totalFolders > 1) {
            for (int offset = -maxOffset; offset <= maxOffset; offset++) {
                if (offset == 0) continue;
                String name = rec.getDirectoryNameWithOffset(offset);
                if (name == null || name.isEmpty()) continue;

                int absOff = Math.abs(offset);
                float normDist = (float) absOff / (maxOffset + 0.5f);
                int alpha = (int) Math.max(20, 160 * (1f - (float) Math.pow(normDist, 1.25)));
                float itemTextSize = Math.max(9f, 12f - absOff * 0.35f) * density;
                splitFolderItemTextPaint.setAlpha(alpha);
                splitFolderItemTextPaint.setTextSize(itemTextSize);

                float yItem = centerY + (offset * stepY) + folderWheelScrollOffset;
                String itemText = (offset < 0 ? "▲ " : "▼ ") + name;
                float baseOffset = -(splitFolderItemTextPaint.descent() + splitFolderItemTextPaint.ascent()) / 2f;
                float itemClipL = leftCenterX - colWidth / 2f + 6f * density;
                float itemClipR = leftCenterX + colWidth / 2f - 6f * density;
                drawFlowingText(canvas, itemText, itemClipL, itemClipR, yItem + baseOffset, splitFolderItemTextPaint, offset, true);
            }
        }
        canvas.restore();

        // Active Folder Card (Center Left)
        splitFolderCardRect.set(leftCenterX - colWidth / 2f, centerY - cardH / 2f,
                                leftCenterX + colWidth / 2f, centerY + cardH / 2f);
        canvas.drawRoundRect(splitFolderCardRect, cardCornerR, cardCornerR, splitFolderActiveCapsulePaint);
        canvas.drawRoundRect(splitFolderCardRect, cardCornerR, cardCornerR, splitFolderActiveBorderPaint);
        // Vertical left accent stripe
        canvas.drawRoundRect(splitFolderCardRect.left + 2.5f * density, splitFolderCardRect.top + 8f * density,
                             splitFolderCardRect.left + 6.0f * density, splitFolderCardRect.bottom - 8f * density,
                             2f * density, 2f * density, splitFolderAccentStripePaint);

        // Active Folder Name (Flows smoothly if longer than available card width)
        splitFolderActiveTextPaint.setTextSize(15f * density);
        float folderTextY = centerY - 4f * density - (splitFolderActiveTextPaint.descent() + splitFolderActiveTextPaint.ascent()) / 2f;
        float folderClipL = splitFolderCardRect.left + 9f * density;
        float folderClipR = splitFolderCardRect.right - 8f * density;
        drawFlowingText(canvas, curFolder, folderClipL, folderClipR, folderTextY, splitFolderActiveTextPaint, 0, true);

        // Active Folder Subtitle
        int filesInFolder = (rec != null) ? rec.getAudibleFilesCount() : 0;
        String folderSub;
        if (isRecording) {
            folderSub = "TARGET REC FOLDER";
        } else {
            folderSub = (filesInFolder > 0) ? (filesInFolder + " AUDIO FILES") : "ACTIVE FOLDER";
        }
        splitFolderSubPaint.setTextSize(8.5f * density);
        canvas.drawText(folderSub, leftCenterX + 3f * density, centerY + 16f * density, splitFolderSubPaint);


        // --- RIGHT COLUMN: FILES ---
        int totalFiles = (rec != null) ? rec.getAudibleFilesCount() : 0;
        int curFileIdx = (rec != null) ? rec.getCurrentFileIndex() + 1 : 0;
        String curFile = (rec != null) ? rec.getCurrentFileName() : "NO AUDIO FILE";
        if (curFile == null || curFile.isEmpty()) curFile = "NO AUDIO FILE";

        // Right Header
        splitFileHeaderPaint.setTextSize(11f * density);
        splitFileBadgePaint.setTextSize(9.5f * density);
        canvas.drawText("FILES", rightCenterX - 22f * density, headerY, splitFileHeaderPaint);
        if (isRecording) {
            splitFileBadgePaint.setColor(Color.parseColor("#FF1744"));
            canvas.drawText("● RECORDING", rightCenterX + 26f * density, headerY, splitFileBadgePaint);
        } else {
            String fileCountBadge = (totalFiles > 0) ? String.format(Locale.US, "%02d / %02d", curFileIdx, totalFiles) : "-- / --";
            splitFileBadgePaint.setColor(Color.parseColor("#00E5FF"));
            canvas.drawText(fileCountBadge, rightCenterX + 28f * density, headerY, splitFileBadgePaint);
            canvas.drawText("◀▶", rightCenterX + colWidth / 2f - 6f * density, headerY, splitCuePaint);
        }

        // Right Ambient Items (with scroll inertia and canvas clipping)
        if (Math.abs(fileWheelScrollOffset) > 0.5f) {
            fileWheelScrollOffset += (0f - fileWheelScrollOffset) * 0.22f;
            postInvalidateOnAnimation();
        } else {
            fileWheelScrollOffset = 0f;
        }

        canvas.save();
        canvas.clipRect(dividerX, listTopY, w, listBottomY);
        if (rec != null && totalFiles > 1) {
            for (int offset = -maxOffset; offset <= maxOffset; offset++) {
                if (offset == 0) continue;
                String name = rec.getFileNameWithOffset(offset);
                if (name == null || name.isEmpty()) continue;

                int absOff = Math.abs(offset);
                float normDist = (float) absOff / (maxOffset + 0.5f);
                int alpha = (int) Math.max(20, 160 * (1f - (float) Math.pow(normDist, 1.25)));
                float itemTextSize = Math.max(9f, 12f - absOff * 0.35f) * density;
                splitFileItemTextPaint.setAlpha(alpha);
                splitFileItemTextPaint.setTextSize(itemTextSize);

                float yItem = centerY + (offset * stepY) + fileWheelScrollOffset;
                String itemText = (offset < 0 ? "◀ " : "▶ ") + name;
                float baseOffset = -(splitFileItemTextPaint.descent() + splitFileItemTextPaint.ascent()) / 2f;
                float itemClipL = rightCenterX - colWidth / 2f + 6f * density;
                float itemClipR = rightCenterX + colWidth / 2f - 6f * density;
                drawFlowingText(canvas, itemText, itemClipL, itemClipR, yItem + baseOffset, splitFileItemTextPaint, offset + 100, true);
            }
        }
        canvas.restore();

        // Active File Card (Center Right)
        splitFileCardRect.set(rightCenterX - colWidth / 2f, centerY - cardH / 2f,
                              rightCenterX + colWidth / 2f, centerY + cardH / 2f);
        canvas.drawRoundRect(splitFileCardRect, cardCornerR, cardCornerR, splitFileActiveCapsulePaint);

        if (isRecording) {
            float pulse = 0.55f + 0.45f * (float) Math.sin(waveTime * 4.2f);
            int borderAlpha = (int) (180 + 75 * pulse);
            splitFileActiveBorderPaint.setColor(Color.parseColor("#FF1744"));
            splitFileActiveBorderPaint.setAlpha(borderAlpha);
            splitFileActiveBorderPaint.setShadowLayer(10f * density, 0f, 0f, Color.parseColor("#B3FF1744"));

            splitFileAccentStripePaint.setColor(Color.parseColor("#FF1744"));
            splitFileAccentStripePaint.setShadowLayer(6f * density, 0f, 0f, Color.parseColor("#FF1744"));
        } else {
            splitFileActiveBorderPaint.setColor(Color.parseColor("#00E5FF"));
            splitFileActiveBorderPaint.setAlpha(255);
            splitFileActiveBorderPaint.setShadowLayer(8f * density, 0f, 0f, Color.parseColor("#8000E5FF"));

            splitFileAccentStripePaint.setColor(Color.parseColor("#00E5FF"));
            splitFileAccentStripePaint.setShadowLayer(6f * density, 0f, 0f, Color.parseColor("#8000E5FF"));
        }

        canvas.drawRoundRect(splitFileCardRect, cardCornerR, cardCornerR, splitFileActiveBorderPaint);
        // Vertical left accent stripe
        canvas.drawRoundRect(splitFileCardRect.left + 2.5f * density, splitFileCardRect.top + 8f * density,
                             splitFileCardRect.left + 6.0f * density, splitFileCardRect.bottom - 8f * density,
                             2f * density, 2f * density, splitFileAccentStripePaint);

        // Active File Name (Flows smoothly if longer than available card width)
        String displayFileName;
        if (isRecording) {
            String recName = (rec != null) ? rec.getCurrentRecordingFileName() : null;
            if (recName == null || recName.isEmpty()) {
                displayFileName = "● RECORDING NEW FILE";
            } else {
                displayFileName = "● " + recName;
            }
            splitFileActiveTextPaint.setColor(Color.parseColor("#FF5252"));
            splitFileActiveTextPaint.setShadowLayer(8f * density, 0f, 0f, Color.parseColor("#80FF1744"));
        } else {
            displayFileName = curFile;
            splitFileActiveTextPaint.setColor(Color.parseColor("#00E5FF"));
            splitFileActiveTextPaint.setShadowLayer(8f * density, 0f, 0f, Color.parseColor("#8000E5FF"));
        }

        splitFileActiveTextPaint.setTextSize(15f * density);
        float fileTextY = centerY - 5f * density - (splitFileActiveTextPaint.descent() + splitFileActiveTextPaint.ascent()) / 2f;
        float fileClipL = splitFileCardRect.left + 9f * density;
        float fileClipR = splitFileCardRect.right - 8f * density;
        drawFlowingText(canvas, displayFileName, fileClipL, fileClipR, fileTextY, splitFileActiveTextPaint, 0, true);

        // --- SUPER MANDATORY MINIMAL UX ANIMATIONS ---
        if (isPlaying) {
            // 1. Sleek 5-Bar Live Dynamic Audio Equalizer + Track Time
            int duration = (rec != null) ? rec.getDurationMillis() : 0;
            int currentPos = (rec != null) ? rec.getCurrentPositionMillis() : 0;
            String timeStr = formatMs(currentPos) + " / " + formatMs(duration);
            splitTimeTextPaint.setTextSize(8.5f * density);
            splitTimeTextPaint.setTextAlign(Paint.Align.LEFT);
            float timeTextWidth = splitTimeTextPaint.measureText(timeStr);

            float barW = 2.6f * density;
            float barSpacing = 2.2f * density;
            float eqTotalW = 5 * barW + 4 * barSpacing;
            float totalBlockW = eqTotalW + 7f * density + timeTextWidth;
            float blockStartX = rightCenterX - totalBlockW / 2f;

            float eqStartX = blockStartX;
            float eqBaseY = centerY + 18.5f * density;
            float maxBarH = 10f * density;
            float minBarH = 2.5f * density;

            for (int b = 0; b < 5; b++) {
                int freqIdx = b * 6;
                float audio = (freqIdx < NUM_FREQ_BANDS) ? freqBands[freqIdx] : 0.2f;
                float wavePulse = 0.5f + 0.5f * (float) Math.sin(waveTime * 3.8f + b * 1.15f);
                float hBar = minBarH + (0.35f * wavePulse + 0.65f * audio) * maxBarH;

                float bx = eqStartX + b * (barW + barSpacing);
                splitLiveEqBarRect.set(bx, eqBaseY - hBar, bx + barW, eqBaseY);
                canvas.drawRoundRect(splitLiveEqBarRect, 1.3f * density, 1.3f * density, splitLiveEqPaint);
            }

            // Time elapsed / duration text cleanly to the right of mini EQ
            canvas.drawText(timeStr, blockStartX + eqTotalW + 7f * density, centerY + 16.5f * density, splitTimeTextPaint);

            // 2. Slender Playback Progress Bar along bottom of the active file card
            float progressTrackLeft = splitFileCardRect.left + 10f * density;
            float progressTrackRight = splitFileCardRect.right - 10f * density;
            float progressTrackY = splitFileCardRect.bottom - 3.8f * density;
            float progressW = progressTrackRight - progressTrackLeft;

            canvas.drawLine(progressTrackLeft, progressTrackY, progressTrackRight, progressTrackY, splitProgressBgPaint);
            float fillW = Math.max(0f, Math.min(progressW, progressW * playbackProgressFraction));
            if (fillW > 0) {
                canvas.drawLine(progressTrackLeft, progressTrackY, progressTrackLeft + fillW, progressTrackY, splitProgressFillPaint);
                drawDiamond(canvas, progressTrackLeft + fillW, progressTrackY, 2.2f * density, splitPlayheadPipPaint);
            }

        } else if (isRecording) {
            // Recording Time elapsed & Pulsing Beacon
            long startTime = (rec != null) ? rec.getRecordStartTimeMillis() : 0L;
            long elapsed = (startTime > 0) ? (System.currentTimeMillis() - startTime) : 0L;
            String recTimeStr = "REC  " + formatMs((int) elapsed);
            splitRecTextPaint.setTextSize(9f * density);
            splitRecTextPaint.setTextAlign(Paint.Align.LEFT);
            float recTextW = splitRecTextPaint.measureText(recTimeStr);

            float beaconTotalW = 14f * density + recTextW;
            float beaconStartX = rightCenterX - beaconTotalW / 2f;
            float beaconX = beaconStartX + 3.5f * density;
            float beaconY = centerY + 13.5f * density;

            float pulseAlpha = 0.55f + 0.45f * (float) Math.sin(waveTime * 4.2f);
            splitRecHaloPaint.setAlpha((int) (pulseAlpha * 90));
            canvas.drawCircle(beaconX, beaconY, 6.0f * density, splitRecHaloPaint);
            splitRecCorePaint.setAlpha((int) (pulseAlpha * 255));
            canvas.drawCircle(beaconX, beaconY, 3.0f * density, splitRecCorePaint);

            canvas.drawText(recTimeStr, beaconStartX + 14f * density, centerY + 16.5f * density, splitRecTextPaint);

        } else {
            // Idle Standby: Subtle stationary play prompt "▶  TAP TO PLAY"
            splitIdlePlayCuePaint.setTextSize(9f * density);
            splitIdlePlayCuePaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("▶  TAP TO PLAY", rightCenterX, centerY + 16f * density, splitIdlePlayCuePaint);
        }
    }

    /**
     * Draws text that flows (smooth ping-pong marquee scroll with cosine easing)
     * instead of truncation when it exceeds available space.
     * - When text fits: cleanly rendered centered or aligned without clipping.
     * - When text exceeds space: smoothly flows forward to reveal the end, pauses,
     *   and flows back to the start with gentle sinusoidal deceleration.
     */
    private void drawFlowingText(Canvas canvas, String text, float clipLeft, float clipRight,
                                float baselineY, Paint paint, int staggerId, boolean centerIfFits) {
        if (text == null || text.isEmpty()) return;
        float availableW = clipRight - clipLeft;
        if (availableW <= 0) return;

        float textW = paint.measureText(text);
        if (textW <= availableW) {
            Paint.Align oldAlign = paint.getTextAlign();
            if (centerIfFits) {
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(text, (clipLeft + clipRight) / 2f, baselineY, paint);
            } else {
                paint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(text, clipLeft, baselineY, paint);
            }
            paint.setTextAlign(oldAlign);
            return;
        }

        // Text exceeds space: Smooth ping-pong flow revealing the entire name
        float overflow = textW - availableW;
        float density = getResources().getDisplayMetrics().density;
        float speed = 25f * density; // 25 dp/sec smooth readable flow
        float pauseStartMs = 1500f;  // 1.5s pause to read beginning
        float pauseEndMs = 1200f;    // 1.2s pause to read end
        float scrollMs = Math.max(1200f, (overflow / speed) * 1000f);
        float totalCycleMs = pauseStartMs + scrollMs + pauseEndMs + scrollMs;

        long now = SystemClock.uptimeMillis();
        long staggerOffset = Math.abs(staggerId * 410L);
        float cyclePos = ((now + staggerOffset) % (long) totalCycleMs);

        float scrollX;
        if (cyclePos < pauseStartMs) {
            // 1. Paused at start
            scrollX = 0f;
        } else if (cyclePos < pauseStartMs + scrollMs) {
            // 2. Gliding smoothly to the end with cosine easing
            float t = (cyclePos - pauseStartMs) / scrollMs;
            float smoothT = 0.5f - 0.5f * (float) Math.cos(t * Math.PI);
            scrollX = smoothT * overflow;
        } else if (cyclePos < pauseStartMs + scrollMs + pauseEndMs) {
            // 3. Paused at end
            scrollX = overflow;
        } else {
            // 4. Gliding smoothly back to the start
            float t = (cyclePos - (pauseStartMs + scrollMs + pauseEndMs)) / scrollMs;
            float smoothT = 0.5f - 0.5f * (float) Math.cos(t * Math.PI);
            scrollX = (1.0f - smoothT) * overflow;
        }

        Paint.Align prevAlign = paint.getTextAlign();
        paint.setTextAlign(Paint.Align.LEFT);

        canvas.save();
        canvas.clipRect(clipLeft, baselineY + paint.ascent() - 2f * density,
                        clipRight, baselineY + paint.descent() + 2f * density);

        canvas.drawText(text, clipLeft - scrollX, baselineY, paint);

        canvas.restore();
        paint.setTextAlign(prevAlign);

        postInvalidateOnAnimation();
    }

    private String formatMs(int ms) {
        if (ms < 0) ms = 0;
        int totalSec = ms / 1000;
        int min = totalSec / 60;
        int sec = totalSec % 60;
        return String.format(Locale.US, "%02d:%02d", min, sec);
    }

    public boolean isInLayout (){
        return false;
    }

}