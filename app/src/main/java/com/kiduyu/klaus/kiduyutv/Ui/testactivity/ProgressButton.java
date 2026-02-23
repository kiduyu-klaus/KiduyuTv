package com.kiduyu.klaus.kiduyutv.Ui.testactivity;

import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ProgressButton - A custom button replicating the SVG fill animation exactly.
 *
 * SVG behaviour:
 *  1. White background with a blue (#FD6C38) rounded border.
 *  2. A blue fill sweeps from LEFT to RIGHT over 5 s via an expanding ClipDrawable
 *     (mirrors the SVG clipPath rect whose width grows 0 to 200).
 *  3. The button label transitions its colour uniformly from blue to white over the
 *     same 5 s (mirrors the SVG animate attributeName="fill" on the text element).
 *  4. Both animations freeze at their final state (mirrors fill="freeze").
 *
 * Extra features beyond the SVG:
 *  - loopAnimation()         — repeats the animation indefinitely.
 *  - loopAnimation(int)      — repeats with a custom inter-cycle delay (ms).
 *  - stopLoopAnimation()     — stops looping after the current cycle finishes.
 *  - setLoopDelay(int)       — configure the pause (ms) between cycles.
 *  - setAutoStartOnVisible   — starts automatically when the view appears.
 */
public class ProgressButton extends FrameLayout {

    // ── colours ───────────────────────────────────────────────────────────────
    private static final int PRIMARY_BLUE = Color.parseColor("#FD6C38");
    private static final int WHITE        = Color.WHITE;
    private static final int STROKE_DP    = 2;

    // ── views ─────────────────────────────────────────────────────────────────
    private TextView     labelView;
    private View         fillView;
    private ClipDrawable clipDrawable;

    // ── animation state ───────────────────────────────────────────────────────
    private AnimatorSet animatorSet;
    private int     defaultDuration    = 5000; // ms — matches SVG dur="5s"
    private int     loopDelay          = 500;  // ms pause between loop cycles
    private boolean isAnimating        = false;
    private boolean isLooping          = false;
    private boolean autoStartOnVisible = false;
    private boolean hasStartedOnce     = false;

    /**
     * Posted after each cycle when looping is active.
     * Snaps visuals back to the start state then fires the next cycle.
     */
    private final Runnable loopRestartRunnable = () -> {
        if (!isLooping) return;   // loop was stopped while the delay was pending
        resetToInitialState();    // snap fill and text colour back to start
        isAnimating = false;      // allow startAnimation() to proceed
        startAnimation();         // kick off the next cycle
    };

    // ── constructors ──────────────────────────────────────────────────────────

    public ProgressButton(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ProgressButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProgressButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    // ── initialisation ────────────────────────────────────────────────────────

    private void init(Context context) {
        setWillNotDraw(false); // FrameLayout skips onDraw by default; needed for border

        float dp = getResources().getDisplayMetrics().density;

        // Layer 1 — white background, always visible beneath the fill.
        View whiteBackground = new View(context);
        whiteBackground.setBackground(makeRoundedBackground(WHITE, dp));
        addView(whiteBackground, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Layer 2 — blue fill, starts fully clipped and sweeps left to right.
        //   ClipDrawable(Gravity.START, HORIZONTAL): level 0 = fully hidden,
        //   level 10 000 = fully revealed.
        //   Mirrors the SVG clipPath rect whose width animates 0 -> 200
        //   while staying anchored at the left edge (x="100").
        clipDrawable = new ClipDrawable(
                makeRoundedBackground(PRIMARY_BLUE, dp),
                Gravity.START,
                ClipDrawable.HORIZONTAL);
        clipDrawable.setLevel(0);

        fillView = new View(context);
        fillView.setBackground(clipDrawable);
        addView(fillView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        // Layer 3 — label, starts blue and transitions to white as the fill covers it.
        //   Mirrors: <animate attributeName="fill" from="#FD6C38" to="#FFFFFF">
        labelView = new TextView(context);
        labelView.setText("Click Me");
        labelView.setTextColor(PRIMARY_BLUE);
        labelView.setTextSize(18);
        labelView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        labelView.setGravity(Gravity.CENTER);
        labelView.setBackground(null); // transparent so the fill layer shows through
        addView(labelView, new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        post(this::checkAndStartAnimationIfNeeded);
    }

    // ── drawing ───────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Rounded blue border — mirrors stroke="#FD6C38" stroke-width="2" rx="5".
        float dp           = getResources().getDisplayMetrics().density;
        float cornerRadius = 5 * dp;
        float strokeWidth  = STROKE_DP * dp;

        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(PRIMARY_BLUE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(strokeWidth);

        canvas.drawRoundRect(new RectF(
                strokeWidth / 2f,
                strokeWidth / 2f,
                getWidth()  - strokeWidth / 2f,
                getHeight() - strokeWidth / 2f
        ), cornerRadius, cornerRadius, borderPaint);
    }

    // ── core animation ────────────────────────────────────────────────────────

    /**
     * Run one fill cycle: blue fill sweeps left to right, text colour transitions
     * from blue to white. After completion the visuals freeze at the final state.
     *
     * If {@link #isLooping} is {@code true} the cycle restarts automatically after
     * {@link #loopDelay} ms.
     */
    public void startAnimation() {
        if (isAnimating) return;
        isAnimating    = true;
        hasStartedOnce = true;
        isLooping = true;

        // Animator 1 — left-to-right fill reveal via ClipDrawable level.
        ValueAnimator fillAnimator = ValueAnimator.ofInt(0, 10_000);
        fillAnimator.setDuration(defaultDuration);
        fillAnimator.addUpdateListener(anim ->
                clipDrawable.setLevel((int) anim.getAnimatedValue()));

        // Animator 2 — smooth text colour transition using ArgbEvaluator.
        ValueAnimator colorAnimator = ValueAnimator.ofObject(
                new ArgbEvaluator(), PRIMARY_BLUE, WHITE);
        colorAnimator.setDuration(defaultDuration);
        colorAnimator.addUpdateListener(anim ->
                labelView.setTextColor((int) anim.getAnimatedValue()));

        animatorSet = new AnimatorSet();
        animatorSet.playTogether(fillAnimator, colorAnimator);
        animatorSet.addListener(new android.animation.AnimatorListenerAdapter() {

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isAnimating = false;
                if (isLooping) {
                    // After loopDelay ms, snap back to start and play again.
                    postDelayed(loopRestartRunnable, loopDelay);
                }
                // Not looping: visuals stay at final filled state (fill="freeze").
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {
                isAnimating = false;
            }
        });
        animatorSet.start();
    }

    /** Start a single animation cycle with a custom duration (ms). */
    public void startAnimation(long durationMs) {
        if (isAnimating) return;
        this.defaultDuration = (int) durationMs;
        startAnimation();
    }

    // ── loop API ──────────────────────────────────────────────────────────────

    /**
     * Start the animation and repeat it indefinitely.
     *
     * Each cycle:
     *   1. Runs the fill sweep and text colour transition for {@link #defaultDuration} ms.
     *   2. Waits {@link #loopDelay} ms.
     *   3. Snaps back to the initial state (white bg, blue text, no fill).
     *   4. Repeats from step 1.
     *
     * Call {@link #stopLoopAnimation()} to stop after the current cycle finishes,
     * or {@link #reset()} to stop and snap back immediately.
     */
    public void loopAnimation() {
        isLooping = true;
        if (!isAnimating) startAnimation();
    }

    /**
     * Start looping with a custom pause between cycles.
     *
     * @param delayBetweenCyclesMs Pause in milliseconds between each cycle.
     */
    public void loopAnimation(int delayBetweenCyclesMs) {
        this.loopDelay = delayBetweenCyclesMs;
        loopAnimation();
    }

    /**
     * Stop looping gracefully. The current in-progress cycle completes normally
     * and the button freezes at the fully-filled state.
     * Call {@link #reset()} to stop and snap back to the initial state immediately.
     */
    public void stopLoopAnimation() {
        isLooping = false;
        removeCallbacks(loopRestartRunnable); // cancel any pending restart
    }

    /**
     * Set the pause between loop cycles.
     *
     * @param delayMs Delay in milliseconds (default: 500).
     */
    public void setLoopDelay(int delayMs) {
        this.loopDelay = delayMs;
    }

    /** @return The current delay between loop cycles in milliseconds. */
    public int getLoopDelay() { return loopDelay; }

    /** @return {@code true} if the animation is set to loop. */
    public boolean isLooping() { return isLooping; }

    // ── reset ─────────────────────────────────────────────────────────────────

    /**
     * Cancel any running or pending animation immediately and return the button
     * to its initial visual state. Also stops looping if active.
     */
    public void reset() {
        stopLoopAnimation();
        if (animatorSet != null && animatorSet.isRunning()) {
            animatorSet.cancel();
        }
        isAnimating = false;
        resetToInitialState();
    }

    /** Snap drawables back to start without touching loop or isAnimating flags. */
    private void resetToInitialState() {
        clipDrawable.setLevel(0);
        labelView.setTextColor(PRIMARY_BLUE);
    }

    // ── auto-start on visible ─────────────────────────────────────────────────

    /**
     * When {@code true} the animation starts automatically the first time the
     * button becomes visible on screen.
     */
    public void setAutoStartOnVisible(boolean autoStart) {
        this.autoStartOnVisible = autoStart;
        if (autoStart && !hasStartedOnce && getVisibility() == VISIBLE) {
            post(this::checkAndStartAnimationIfNeeded);
        }
    }

    public boolean isAutoStartOnVisible() { return autoStartOnVisible; }

    /** Reset the auto-start flag so the animation can trigger on visible again. */
    public void resetAutoStart() {
        hasStartedOnce = false;
        reset();
    }

    private void checkAndStartAnimationIfNeeded() {
        if (autoStartOnVisible && !hasStartedOnce
                && getVisibility() == VISIBLE
                && getWidth() > 0 && getHeight() > 0
                && isShown()) {
            startAnimation();
        }
    }

    @Override
    protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        if (visibility == VISIBLE && autoStartOnVisible && !hasStartedOnce) {
            post(this::checkAndStartAnimationIfNeeded);
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (autoStartOnVisible) post(this::checkAndStartAnimationIfNeeded);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        // Clean up all animation resources to prevent memory leaks.
        stopLoopAnimation();
        if (animatorSet != null) animatorSet.cancel();
    }

    // ── public API ────────────────────────────────────────────────────────────

    public void setButtonText(String text) { labelView.setText(text); }
    public String getButtonText()          { return labelView.getText().toString(); }
    public void setDuration(int ms)        { this.defaultDuration = ms; }
    public int  getDuration()              { return defaultDuration; }
    public boolean isAnimating()           { return isAnimating; }

    @Override
    public void setOnClickListener(@Nullable OnClickListener l) {
        labelView.setOnClickListener(l);
        super.setOnClickListener(l);
    }

    /**
     * Register a focus change listener.
     *
     * Built-in focus behaviour:
     *  - Focus gained: starts the animation (if not already running).
     *  - Focus lost:   stops looping (if active) and resets to the initial state.
     *
     * The supplied listener is called after this built-in behaviour so callers
     * can layer their own logic on top.
     *
     * @param listener OnFocusChangeListener to attach, or {@code null} to clear.
     */
    @Override
    public void setOnFocusChangeListener(@Nullable OnFocusChangeListener listener) {
        super.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (!isAnimating) startAnimation();
            } else {
                reset();
            }
            if (listener != null) listener.onFocusChange(v, hasFocus);
        });
        setFocusable(true); // ensure the view can actually receive focus events
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ShapeDrawable makeRoundedBackground(int color, float dp) {
        float r = 5 * dp; // rx="5" from the SVG
        float[] radii = { r, r, r, r, r, r, r, r };
        ShapeDrawable sd = new ShapeDrawable(new RoundRectShape(radii, null, null));
        sd.getPaint().setColor(color);
        return sd;
    }
}