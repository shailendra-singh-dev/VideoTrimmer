package com.itexico.xtv.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ImageView;

/**
 * Created by iTexico Developer on 4/19/2016.
 */
public class CircleButton extends ImageView {

    private static final int PRESSED_RING_ALPHA = 128;
    private static final String TAG = CircleButton.class.getSimpleName();
    private int mDefaultColor = Color.WHITE;

    private int centerY;
    private int centerX;
    private int outerRadius;
    private Paint circlePaint;

    private int mPressedColor;

    public CircleButton(Context context) {
        super(context);
        init(context, null);
    }

    public CircleButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CircleButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setCirclePressed(boolean pressed) {
        if (circlePaint != null) {
            circlePaint.setColor(pressed ? mPressedColor : mDefaultColor);
        }
        invalidate();
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.v(TAG, "circlePaint.getColor():" + circlePaint.getColor());
        canvas.drawCircle(centerX, centerY, outerRadius, circlePaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = (w) / 2;
        centerY = (h) / 2;
        outerRadius = Math.min(w, h) / 2;
    }

    public void setDefaultColor(int color) {
        this.mDefaultColor = color;
    }

    public void setPressedColor(int color) {
        mPressedColor = color;
    }

    private void init(Context context, AttributeSet attrs) {
        this.setFocusable(true);
        this.setScaleType(ScaleType.CENTER_INSIDE);
        setClickable(true);

        circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        circlePaint.setStyle(Paint.Style.FILL);
        circlePaint.setAlpha(PRESSED_RING_ALPHA);
    }

}
