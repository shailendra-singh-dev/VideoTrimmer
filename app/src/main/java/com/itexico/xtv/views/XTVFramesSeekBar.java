package com.itexico.xtv.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.widget.ImageView;

import com.itexico.xtv.R;
import com.itexico.xtv.util.AppUtils;

import java.util.ArrayList;
import java.util.List;


public class XTVFramesSeekBar extends ImageView {
    private static final String TAG = XTVFramesSeekBar.class.getSimpleName();

    public static final int SELECT_THUMB_LEFT = 1;
    public static final int SELECT_THUMB_RIGHT = 2;
    public static final int SELECT_THUMB_NON = 0;

    //params
    private Bitmap mThumbSliceLeft = null;
    private Bitmap mThumbSliceRight = null;
    final private Bitmap mThumbCurrentVideoPosition = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
    final private Bitmap mDefaultFrame = BitmapFactory.decodeResource(getResources(), R.drawable.lo_video_placeholder);

    final private Paint mProgressDrawPaint = new Paint();
    final private Paint mThumbPaint = new Paint();
    final private Rect mSlidersDrawRect = new Rect();
    final private Paint mFillPaint = new Paint();

    private int mProgressMinDiff = 15; //percentage
    private int progressColor = getResources().getColor(R.color.blue);
    private int secondaryProgressColor = getResources().getColor(R.color.blue_light);
    private int mProgressHalfHeight = 3;
    private int thumbPadding = getResources().getDimensionPixelOffset(R.dimen.frames_default_margin);
    private int maxValue = 100;

    private int mProgressMinDiffPixels;
    private int mThumbSliceLeftX;
    private int mThumbSliceRightX;
    private int mThumbCurrentVideoPositionX;
    private int mThumbSliceLeftValue;
    private int mThumbSliceRightValue;
    private int mThumbSliceY;
    private int mThumbCurrentVideoPositionY;

    private int mSelectedThumb;
    private int mThumbSliceHalfWidth;
    private int mThumbCurrentVideoPositionHalfWidth;
    private SeekBarChangeListener mSeekBarChangeListener;

    private int mProgressTop;
    private int mProgressBottom;

    private boolean mIsBlocked;
    private boolean mIsVideoStatusDisplay;

    private Context mContext;

    private int mFramesWidth;

    private Rect mDrawRect = new Rect();

    private RectF mRectF = null;
    private ArrayList<Bitmap> mFramesList = null;

    public XTVFramesSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public XTVFramesSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public XTVFramesSeekBar(Context context) {
        super(context);
        init(context);
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        init(getContext());
    }

    private void init(Context context) {
        mFramesList = new ArrayList<Bitmap>();
        mContext = context;
        mFillPaint.setColor(Color.LTGRAY);
        mFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFillPaint.setAntiAlias(true);
        mFramesWidth = context.getResources().getDimensionPixelSize(R.dimen.frames_thumbnail_width);

        mRectF = new RectF(mThumbSliceLeftX - mThumbSliceHalfWidth, 0, mThumbSliceRightX, mProgressTop);
        int numberOfFramesToDraw = AppUtils.getNumberOfFramesToDraw(mContext);
        for (int i = 0; i < numberOfFramesToDraw; i++) {
            mFramesList.add(mDefaultFrame);
        }
        mThumbSliceLeft = AppUtils.drawableToBitmap(getContext(), R.drawable.rectangle_thumbnail);
        mThumbSliceRight = AppUtils.drawableToBitmap(getContext(), R.drawable.rectangle_thumbnail);

        if (null != mThumbSliceLeft && mThumbSliceLeft.getHeight() > getHeight() && null != getLayoutParams())
            getLayoutParams().height = mThumbSliceLeft.getHeight();

        mThumbSliceY = getThumbSliceY();
        Log.i(TAG, "init() mSlidersDrawRect: mThumbSliceY" + mThumbSliceY);
        if (null != mThumbCurrentVideoPosition) {
            mThumbCurrentVideoPositionY = (getHeight()) - (mThumbCurrentVideoPosition.getHeight());
        }
        if (null != mThumbSliceLeft) {
            mThumbSliceHalfWidth = mThumbSliceLeft.getWidth() / 2;
        }
        if (null != mThumbCurrentVideoPosition) {
            mThumbCurrentVideoPositionHalfWidth = mThumbCurrentVideoPosition.getWidth() / 2;
        }
        if (mThumbSliceLeftX == 0 || mThumbSliceRightX == 0) {
            mThumbSliceLeftX = thumbPadding;
            mThumbSliceRightX = getWidth() - thumbPadding;
        }
        mProgressMinDiffPixels = calculateCorrds(mProgressMinDiff) - 2 * thumbPadding;
        if (null != mThumbSliceLeft) {
            mProgressTop = getHeight() - mThumbSliceLeft.getHeight() / 2;
        }
        mProgressBottom = mProgressTop + mProgressHalfHeight;
        invalidate();
    }

    public void setSeekBarChangeListener(SeekBarChangeListener scl) {
        this.mSeekBarChangeListener = scl;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawFrames(canvas);
        drawThumbs(canvas);
        drawSeekProgressRectangle(canvas);
    }

    private void drawThumbs(Canvas canvas) {
        Rect rect;
        //generate and draw progress
        mProgressDrawPaint.setColor(progressColor);
        rect = new Rect(thumbPadding, mProgressTop, mThumbSliceLeftX, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);
        rect = new Rect(mThumbSliceRightX, mProgressTop, getWidth() - thumbPadding, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);

        //generate and draw secondary progress
        mProgressDrawPaint.setColor(secondaryProgressColor);
        rect = new Rect(mThumbSliceLeftX, mProgressTop, mThumbSliceRightX, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);

        final int thumbSliceWidth = getResources().getDimensionPixelOffset(R.dimen.rounded_rectangle_width);
        final int thumbSliceHeight = getResources().getDimensionPixelOffset(R.dimen.frames_thumbnail_height);
        if (!mIsBlocked) {
            //generate and draw thumbs pointer
            mSlidersDrawRect.top = mDrawRect.top;
            mSlidersDrawRect.bottom = thumbSliceHeight;

            if (null != mThumbSliceLeft) {
                Log.i(TAG, "drawThumbs() mSlidersDrawRect: mThumbSliceY" + mThumbSliceY);
                canvas.drawBitmap(mThumbSliceLeft, mThumbSliceLeftX, mThumbSliceY, mThumbPaint);
            }
            if (null != mThumbSliceRight) {
                Log.i(TAG, "drawThumbs() mSlidersDrawRect: mThumbSliceY" + mThumbSliceY);
                canvas.drawBitmap(mThumbSliceRight, mThumbSliceRightX - thumbSliceWidth, mThumbSliceY, mThumbPaint);
            }
        }
        if (mIsVideoStatusDisplay && null != mThumbCurrentVideoPosition) {
            //generate and draw video thump pointer
            Log.i(TAG, "drawBitmap() mThumbCurrentVideoPositionX: " + mThumbCurrentVideoPositionX);
            canvas.drawBitmap(mThumbCurrentVideoPosition, mThumbCurrentVideoPositionX - thumbSliceWidth,
                    mThumbCurrentVideoPositionY, mThumbPaint);
        }
    }

    private void drawFrames(Canvas canvas) {
        if (null == mFramesList || mFramesList.isEmpty()) {
            return;
        }
        int rowWidth = getMeasuredWidth();
        int itemWidth = mFramesWidth;
        int frameHeight = getMeasuredHeight();
        float xOffset = 0.0f;
        int count = 0;
        int numberOfFramesToDraw = AppUtils.getNumberOfFramesToDraw(mContext);
        Log.i(TAG, "drawFrames() numberOfFramesToDraw:" + numberOfFramesToDraw);
        while (xOffset < rowWidth && count < numberOfFramesToDraw) {
            canvas.save();
            canvas.translate(xOffset, 0);
            canvas.clipRect(0, 0, itemWidth, frameHeight);

            if (count >= mFramesList.size()) {
                count = mFramesList.size() - 1;
            }

            Bitmap cachedBitmap = mFramesList.get(count);
            // Pass Program as parameter here.
            Bitmap bitmapToDraw;
            if (null != cachedBitmap) {
                bitmapToDraw = cachedBitmap;
            } else {
                bitmapToDraw = mDefaultFrame;
            }
            if (null != bitmapToDraw) {
                Log.i(TAG, "drawFrames() count:" + count);
                drawScaledBitmap(bitmapToDraw, mFramesWidth, frameHeight, canvas);
            }
            canvas.restore();

            xOffset += itemWidth;
            count++;
        }
    }

    private void drawScaledBitmap(final Bitmap bitmap, final int requiredWidth, final int requiredHeight,
                                  final Canvas canvas) {
        if (null == bitmap || bitmap.isRecycled()) {
            return;
        }
        // Get current dimensions
        final int width = bitmap.getWidth();
        final int height = bitmap.getHeight();

        // Determine how much to mScale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        final float xScale = ((float) requiredWidth) / (float) width;
        final float yScale = ((float) requiredHeight) / (float) height;
        final float scale = (xScale <= yScale) ? xScale : yScale;

        // get the resulting size after scaling
        final int scaledWidth = (int) (scale * (float) width);
        final int scaledHeight = (int) (scale * (float) height);

        final int relativeX = (requiredWidth - scaledWidth) / 2;
        final int relativeY = (requiredHeight - scaledHeight) / 2;

        mDrawRect.right = scaledWidth + relativeX;
        mDrawRect.bottom = scaledHeight + relativeY;
        mDrawRect.top = relativeY;
        mDrawRect.left = relativeX;
        canvas.drawBitmap(bitmap, null, mDrawRect, mFillPaint);
    }

    private int getThumbSliceY() {
        final int width = mDefaultFrame.getWidth();
        final int height = mDefaultFrame.getHeight();
        int frameHeight = getMeasuredHeight();
        final float xScale = ((float) mFramesWidth) / (float) width;
        final float yScale = ((float) frameHeight) / (float) height;
        final float scale = (xScale <= yScale) ? xScale : yScale;
        final int scaledHeight = (int) (scale * (float) height);
        final int relativeY = (frameHeight - scaledHeight) / 2;
        return relativeY;
    }

    private void drawSeekProgressRectangle(Canvas canvas) {
        mProgressDrawPaint.setStyle(Paint.Style.FILL);
        mProgressDrawPaint.setStrokeWidth(10);
        mProgressDrawPaint.setColor(Color.rgb(0, 0, 0));
        mProgressDrawPaint.setAlpha(150);

        mProgressDrawPaint.setAntiAlias(true);
        //Left Gray Rect
        mRectF.left = 0;
        mRectF.top = mDrawRect.top;
        ;
        mRectF.right = mThumbSliceLeftX;
        mRectF.bottom = mDrawRect.bottom;
        canvas.drawRect(mRectF, mProgressDrawPaint);

        //Calculate screen width
        Display display = this.getDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        //Right Gray rect
        mRectF.left = mThumbSliceRightX;
        mRectF.top = mDrawRect.top;
        mRectF.right = width - 6;
        mRectF.bottom = mDrawRect.bottom;
        canvas.drawRect(mRectF, mProgressDrawPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsBlocked) {
            int mx = (int) event.getX();
            int actionId = -1;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    actionId = MotionEvent.ACTION_DOWN;
                    if (mx >= mThumbSliceLeftX - mThumbSliceHalfWidth
                            && mx <= mThumbSliceLeftX + mThumbSliceHalfWidth || mx < mThumbSliceLeftX - mThumbSliceHalfWidth) {
                        mSelectedThumb = SELECT_THUMB_LEFT;
                    } else if (mx >= mThumbSliceRightX - mThumbSliceHalfWidth
                            && mx <= mThumbSliceRightX + mThumbSliceHalfWidth || mx > mThumbSliceRightX + mThumbSliceHalfWidth) {
                        mSelectedThumb = SELECT_THUMB_RIGHT;
                    } else if (mx - mThumbSliceLeftX + mThumbSliceHalfWidth < mThumbSliceRightX - mThumbSliceHalfWidth - mx) {
                        mSelectedThumb = SELECT_THUMB_LEFT;
                    } else if (mx - mThumbSliceLeftX + mThumbSliceHalfWidth > mThumbSliceRightX - mThumbSliceHalfWidth - mx) {
                        mSelectedThumb = SELECT_THUMB_RIGHT;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    actionId = MotionEvent.ACTION_MOVE;
                    if ((mx <= mThumbSliceLeftX + mThumbSliceHalfWidth + mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_RIGHT) ||
                            (mx >= mThumbSliceRightX - mThumbSliceHalfWidth - mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_LEFT)) {
                        mSelectedThumb = SELECT_THUMB_NON;
                    }

                    if (mSelectedThumb == SELECT_THUMB_LEFT) {
                        mThumbSliceLeftX = mx;
                    } else if (mSelectedThumb == SELECT_THUMB_RIGHT) {
                        mThumbSliceRightX = mx;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    actionId = MotionEvent.ACTION_UP;
                    mSelectedThumb = SELECT_THUMB_NON;
                    mSeekBarChangeListener.onCurrentFrameUpdated(mThumbSliceLeftValue);

                    break;
            }

            notifySeekBarValueChanged(actionId, mSelectedThumb);
        }
        return true;
    }

    private void notifySeekBarValueChanged(int actionId, int selectedThumb) {
        if (mThumbSliceLeftX < thumbPadding)
            mThumbSliceLeftX = thumbPadding;

        if (mThumbSliceRightX < thumbPadding)
            mThumbSliceRightX = thumbPadding;

        if (mThumbSliceLeftX > getWidth() - thumbPadding)
            mThumbSliceLeftX = getWidth() - thumbPadding;

        if (mThumbSliceRightX > getWidth() - thumbPadding)
            mThumbSliceRightX = getWidth() - thumbPadding;

        invalidate();
        if (mSeekBarChangeListener != null) {
            calculateThumbValue();
            mSeekBarChangeListener.SeekBarValueChanged(actionId, selectedThumb, mThumbSliceLeftValue, mThumbSliceRightValue);
        }
    }

    private void calculateThumbValue() {
        mThumbSliceLeftValue = (maxValue * (mThumbSliceLeftX - thumbPadding)) / (getWidth() - 2 * thumbPadding);
        mThumbSliceRightValue = (maxValue * (mThumbSliceRightX - thumbPadding)) / (getWidth() - 2 * thumbPadding);
    }

    private int calculateCorrds(int progress) {
        return (int) (((getWidth() - 2d * thumbPadding) / maxValue) * progress) + thumbPadding;
    }

    public void setLeftProgress(int progress) {
        if (progress < mThumbSliceRightValue - mProgressMinDiff) {
            mThumbSliceLeftX = calculateCorrds(progress);
        }
        notifySeekBarValueChanged(-1, mSelectedThumb);
    }

    public void setRightProgress(int progress) {
        if (progress > mThumbSliceLeftValue + mProgressMinDiff) {
            mThumbSliceRightX = calculateCorrds(progress);
        }
        notifySeekBarValueChanged(-1, mSelectedThumb);
    }

    public int getLeftProgress() {
        return mThumbSliceLeftValue;
    }

    public int getRightProgress() {
        return mThumbSliceRightValue;
    }

    public void resetProgress(int leftProgress, int rightProgress) {
        if (rightProgress - leftProgress > mProgressMinDiff) {
            mThumbSliceLeftX = calculateCorrds(leftProgress);
            mThumbSliceRightX = calculateCorrds(rightProgress);
        }
        notifySeekBarValueChanged(-1, mSelectedThumb);
    }

    public void videoPlayingProgress(int progress) {
        mIsVideoStatusDisplay = true;
        mThumbCurrentVideoPositionX = calculateCorrds(progress);
        Log.i(TAG, "videoPlayingProgress() ,mThumbSliceLeftX:" + mThumbSliceLeftX + "mThumbSliceRightX:" + mThumbSliceRightX + ",progress:" + progress + ",mThumbCurrentVideoPositionX:" + mThumbCurrentVideoPositionX);
        invalidate();
    }

    public void removeVideoStatusThumb() {
        mIsVideoStatusDisplay = false;
        invalidate();
    }

    public void setSliceBlocked(boolean isBLock) {
        mIsBlocked = isBLock;
        invalidate();
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public void setProgressMinDiff(int mProgressMinDiff) {
        this.mProgressMinDiff = mProgressMinDiff;
        mProgressMinDiffPixels = calculateCorrds(mProgressMinDiff);
    }


    public interface SeekBarChangeListener {
        void SeekBarValueChanged(int actionId, int selectedThumb, int leftThumb, int rightThumb);

        void onCurrentFrameUpdated(int currentTime);
    }

    public void updateFramesView(List<Bitmap> frames) {
        if (null == mFramesList) {
            return;
        }
        mFramesList.clear();
        mFramesList.addAll(frames);
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cleanUpResources();
    }

    public void setProgressHeight(int progressHeight) {
        this.mProgressHalfHeight = mProgressHalfHeight / 2;
        invalidate();
    }

    public void setProgressColor(int progressColor) {
        this.progressColor = progressColor;
        invalidate();
    }

    public void setSecondaryProgressColor(int secondaryProgressColor) {
        this.secondaryProgressColor = secondaryProgressColor;
        invalidate();
    }

    public void setThumbPadding(int thumbPadding) {
        this.thumbPadding = thumbPadding;
        invalidate();
    }

    public void cleanUpResources() {
        Log.i(TAG, "cleanUpResources()");
        if (null != mFramesList) {
            for (final Bitmap bitmap : mFramesList) {
                if (null != bitmap) {
                    bitmap.recycle();
                }
            }
            mFramesList.clear();
            mFramesList = null;
        }
        if (null != mDefaultFrame) {
            mDefaultFrame.recycle();
        }

        if (null != mThumbSliceLeft) {
            mThumbSliceLeft.recycle();
            mThumbSliceLeft = null;
        }

        if (null != mThumbSliceRight) {
            mThumbSliceRight.recycle();
            mThumbSliceRight = null;
        }

        if (null != mThumbCurrentVideoPosition) {
            mThumbCurrentVideoPosition.recycle();
        }
    }
}
