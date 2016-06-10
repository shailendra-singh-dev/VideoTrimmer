package com.itexico.xtv.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by iTexico Developer on 4/20/2016.
 */
public class AnimatedCircleImageView extends ImageView {

    private static final int PRESSED_RING_ALPHA = 128;
    private static final String TAG = AnimatedCircleImageView.class.getSimpleName();

    private int centerY;
    private int centerX;
    private int outerRadius;

    private Paint circlePaint;
    private int defaultColor = Color.WHITE;
    private int pressedColor;
    private AnimatorSet mAnimationSet;

    public AnimatedCircleImageView(Context context) {
        super(context);
        init(context, null);
        initAnimator();
    }

    public AnimatedCircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
        initAnimator();
    }

    public AnimatedCircleImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
        initAnimator();
    }

    public void setCirclePressed(boolean pressed) {
        if (circlePaint != null) {
            Log.i(TAG, "setCirclePressed() called..pressed:" + pressed);
            circlePaint.setColor(pressed ? pressedColor : defaultColor);
        }
        invalidate();
    }

    public void startAnimation(){
        mAnimationSet.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.i(TAG, "onDraw() called..color:" + circlePaint.getColor());
        canvas.drawCircle(centerX, centerY, outerRadius, circlePaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        outerRadius = Math.min(w, h) / 2;
    }

    public void setPressedColor(int color) {
        Log.i(TAG, "setPressedColor() called..color:" + color);
        this.pressedColor = color;
    }

    public void setDefaultColor(int color) {
        Log.i(TAG, "setDefaultColor() called..color:" + color);
        this.defaultColor = color;
    }

    private void init(Context context, AttributeSet attrs) {
        this.setFocusable(true);
        this.setScaleType(ScaleType.CENTER_INSIDE);
        setClickable(true);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);

        circlePaint.setColor(defaultColor);
        circlePaint.setAlpha(PRESSED_RING_ALPHA);
    }

    private void initAnimator() {
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(this, "alpha", 1f, .3f);
        fadeOut.setDuration(2000);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(this, "alpha", .3f, 1f);
        fadeIn.setDuration(2000);

         mAnimationSet = new AnimatorSet();

        mAnimationSet.play(fadeIn).after(fadeOut);

        mAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mAnimationSet.start();
            }
        });
    }
}
