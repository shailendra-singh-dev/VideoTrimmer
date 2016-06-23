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
import com.itexico.xtv.frames_render.FramesRenderingManager;
import com.itexico.xtv.util.AppUtils;


public class XTVFramesSeekBar extends ImageView {
    private static final String TAG = XTVFramesSeekBar.class.getSimpleName();

    public static final int SELECT_THUMB_LEFT = 1;
    public static final int SELECT_THUMB_RIGHT = 2;
    public static final int SELECT_THUMB_NON = 0;

    //params
    private Bitmap mThumbSliceLeft = null;
    private Bitmap mThumbSliceRight = null;

    final private FramesRenderingManager mFramesRenderingManager = FramesRenderingManager.getFramesRenderingManagerInstance();

    final private Bitmap mThumbCurrentVideoPosition = BitmapFactory.decodeResource(getResources(), R.drawable.seek_thumb_normal);
    final private Bitmap mDefaultFrame = BitmapFactory.decodeResource(getResources(), R.drawable.lo_video_placeholder);

    final private Paint mProgressDrawPaint = new Paint();
    final private Paint mThumbPaint = new Paint();
    final private Rect mSlidersDrawRect = new Rect();
    final private Paint mFillPaint = new Paint();

    private int mProgressMinDiffMS = 15; //percentage
    private int mProgressMaxDiffMS = 15; //percentage

    private int progressColor = getResources().getColor(R.color.blue);
    private int secondaryProgressColor = getResources().getColor(R.color.blue_light);
    private int mProgressHalfHeight = 3;
    private int mMaxValueInMS = 100;

    private float mProgressMinDiffPixels;
    private float mProgressMaxDiffPixels;

    private float mThumbSliceLeftX;
    private float mThumbSliceRightX;

    private float mThumbCurrentVideoPositionX;
    private int mThumbSliceLeftValueMS;
    private int mThumbSliceRightValueMS;
    private int mThumbSliceY;
    private int mThumbCurrentVideoPositionY;

    private int mSelectedThumb;
    private int mThumbSliceHalfWidth;
    private SeekBarChangeListener mSeekBarChangeListener;

    private int mProgressTop;
    private int mProgressBottom;

    private boolean mIsBlocked;
    private boolean mIsVideoStatusDisplay;

    private Context mContext;

    private int mFramesWidth;

    private Rect mDrawRect = new Rect();

    private RectF mRectF = null;
    private int mLeftProgressMS;
    private int mRightProgressMS;
    private float mTotalWidth;
    private float mActionDownX;
    private float mActionMoveX;

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
        Log.i(TAG, "onWindowFocusChanged()");
        init(getContext());
    }

    public void invalidateView(Context context){
        init(context);
    }

    private void init(Context context) {
        mTotalWidth = getWidth();
        mContext = context;
        mFillPaint.setColor(Color.LTGRAY);
        mFillPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mFillPaint.setAntiAlias(true);
        mFramesWidth = context.getResources().getDimensionPixelSize(R.dimen.frames_thumbnail_width);

        mRectF = new RectF(mThumbSliceLeftX - mThumbSliceHalfWidth, 0, mThumbSliceRightX, mProgressTop);

        mThumbSliceLeft = AppUtils.drawableToBitmap(getContext(), R.drawable.rectangle_thumbnail);
        mThumbSliceRight = AppUtils.drawableToBitmap(getContext(), R.drawable.rectangle_thumbnail);

        if (null != mThumbSliceLeft && mThumbSliceLeft.getHeight() > getHeight() && null != getLayoutParams())
            getLayoutParams().height = mThumbSliceLeft.getHeight();

        mThumbSliceY = getThumbSliceY();
        if (null != mThumbCurrentVideoPosition) {
            mThumbCurrentVideoPositionY = (getHeight()) - (mThumbCurrentVideoPosition.getHeight());
        }
        if (null != mThumbSliceLeft) {
            mThumbSliceHalfWidth = mThumbSliceLeft.getWidth() / 2;
        }

        mProgressMinDiffPixels = calculateCorrds(mProgressMinDiffMS);
        mProgressMaxDiffPixels = calculateCorrds(mProgressMaxDiffMS);
        if (null != mThumbSliceLeft) {
            mProgressTop = getHeight() - mThumbSliceLeft.getHeight() / 2;
        }
        mProgressBottom = mProgressTop + mProgressHalfHeight;

        calculateLeftProgressCoOrdinates();
        calculateRightProgressCoOrdinates();

        Log.i(TAG, "SHAIL init() ,mThumbSliceLeftX:" + mThumbSliceLeftX + ",mThumbSliceRightX:" + mThumbSliceRightX + ",mTotalWidth:" + mTotalWidth);
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
        rect = new Rect(0, mProgressTop,(int)mThumbSliceLeftX, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);
        rect = new Rect((int)mThumbSliceRightX, mProgressTop,(int) mTotalWidth, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);

        //generate and draw secondary progress
        mProgressDrawPaint.setColor(secondaryProgressColor);
        rect = new Rect((int)mThumbSliceLeftX, mProgressTop, (int)mThumbSliceRightX, mProgressBottom);
        canvas.drawRect(rect, mProgressDrawPaint);

        final int thumbSliceWidth = getResources().getDimensionPixelOffset(R.dimen.rounded_rectangle_width);
        final int thumbSliceHeight = getResources().getDimensionPixelOffset(R.dimen.frames_thumbnail_height);
        if (!mIsBlocked) {
            //generate and draw thumbs pointer
            mSlidersDrawRect.top = mDrawRect.top;
            mSlidersDrawRect.bottom = thumbSliceHeight;

            if (null != mThumbSliceLeft) {
                Log.i(TAG, "TEST drawThumbs() mThumbSliceLeftX:" + mThumbSliceLeftX);
                canvas.drawBitmap(mThumbSliceLeft, mThumbSliceLeftX, mThumbSliceY, mThumbPaint);
            }
            if (null != mThumbSliceRight) {
                Log.i(TAG, "TEST drawThumbs() mThumbSliceRightX:" + mThumbSliceRightX);
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

            final String imageCacheKey = String.valueOf(count);
            Bitmap cachedBitmap = mFramesRenderingManager.getFrame(imageCacheKey);
            Log.i(TAG, "drawFrames() imageCacheKey:"+imageCacheKey+",cachedBitmap:" + cachedBitmap);
            if (null != cachedBitmap) {
                drawScaledBitmap(cachedBitmap, mFramesWidth, frameHeight, canvas);
            } else {
                drawScaledBitmap(mDefaultFrame, mFramesWidth, frameHeight, canvas);
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
                    mActionDownX = event.getX();
                    actionId = MotionEvent.ACTION_DOWN;
                    if (mx >= mThumbSliceLeftX - mThumbSliceHalfWidth && mx <= mThumbSliceLeftX + mThumbSliceHalfWidth || mx < mThumbSliceLeftX - mThumbSliceHalfWidth) {
                        mSelectedThumb = SELECT_THUMB_LEFT;
                    } else if (mx >= mThumbSliceRightX - mThumbSliceHalfWidth  && mx <= mThumbSliceRightX + mThumbSliceHalfWidth || mx > mThumbSliceRightX + mThumbSliceHalfWidth) {
                        mSelectedThumb = SELECT_THUMB_RIGHT;
                    } else if (mx - mThumbSliceLeftX + mThumbSliceHalfWidth < mThumbSliceRightX - mThumbSliceHalfWidth - mx) {
                        mSelectedThumb = SELECT_THUMB_LEFT;
                    } else if (mx - mThumbSliceLeftX + mThumbSliceHalfWidth > mThumbSliceRightX - mThumbSliceHalfWidth - mx) {
                        mSelectedThumb = SELECT_THUMB_RIGHT;
                    }
                    Log.i(TAG,"SHAIL onTouchEvent mActionDownX:"+ mActionDownX);
                    break;
                case MotionEvent.ACTION_MOVE:
                    actionId = MotionEvent.ACTION_MOVE;
                    //Movement when selected Video duration is more then limit time..
                    mActionMoveX = event.getX();
                    Log.i(TAG,"SHAIL onTouchEvent mActionMoveX:"+ mActionMoveX);
                    int timeDifference = mThumbSliceRightValueMS - mThumbSliceLeftValueMS;
                    float distanceMoved = mActionMoveX - mActionDownX;
                    boolean isLeftMovement = distanceMoved < 0;
                    if(timeDifference >= mProgressMaxDiffMS ){
                        if((mSelectedThumb == SELECT_THUMB_RIGHT) && (!isLeftMovement)){
                            mThumbSliceRightX = mx;
                            mThumbSliceLeftX = mThumbSliceRightX - mProgressMaxDiffPixels;
                            Log.i(TAG,"TEST onTouchEvent timeDifference >= mProgressMaxDiffMS timeDifference:mSelectedThumb == SELECT_THUMB_RIGHT,mThumbSliceRightX:" +
                                    ""+mThumbSliceRightX+",mThumbSliceLeftX:"+mThumbSliceLeftX+",timeDifference:"+timeDifference);
                        }else if((mSelectedThumb == SELECT_THUMB_LEFT) && (isLeftMovement)){
                            mThumbSliceLeftX = mx;
                            mThumbSliceRightX = mThumbSliceLeftX + mProgressMaxDiffPixels;
                            Log.i(TAG,"TEST onTouchEvent timeDifference >= mProgressMaxDiffMS timeDifference:mSelectedThumb == SELECT_THUMB_LEFT,mThumbSliceLeftX:"+mThumbSliceLeftX+",mThumbSliceRightX:"+mThumbSliceRightX+",timeDifference:"+timeDifference);
                        }else if((mx <= mThumbSliceLeftX + mThumbSliceHalfWidth + mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_RIGHT) ||
                                (mx >= mThumbSliceRightX - mThumbSliceHalfWidth - mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_LEFT)) {
                            mSelectedThumb = SELECT_THUMB_NON;
                            Log.i(TAG,"TEST onTouchEvent timeDifference >= mProgressMaxDiffMS MIN Difference reached.."+",timeDifference:"+timeDifference);
                        }else if(mSelectedThumb == SELECT_THUMB_LEFT) {
                            mThumbSliceLeftX = mx;
                            Log.i(TAG,"TEST onTouchEvent timeDifference >= mProgressMaxDiffMS mSelectedThumb == SELECT_THUMB_LEFT:"+",timeDifference:"+timeDifference);
                        } else if(mSelectedThumb == SELECT_THUMB_RIGHT) {
                            mThumbSliceRightX = mx;
                            Log.i(TAG,"TEST onTouchEvent timeDifference >= mProgressMaxDiffMS mSelectedThumb == SELECT_THUMB_RIGHT:"+",timeDifference:"+timeDifference);
                        }
                    }
                    ///Movement when MIN is reached..
                    else if ((mx <= mThumbSliceLeftX + mThumbSliceHalfWidth + mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_RIGHT) ||
                            (mx >= mThumbSliceRightX - mThumbSliceHalfWidth - mProgressMinDiffPixels && mSelectedThumb == SELECT_THUMB_LEFT)) {
                        mSelectedThumb = SELECT_THUMB_NON;
                        Log.i(TAG,"TEST onTouchEvent MIN Difference reached..");
                    }
                    //Movement without any Condition...
                    else if (mSelectedThumb == SELECT_THUMB_LEFT) {
                        mThumbSliceLeftX = mx;
                        Log.i(TAG,"TEST onTouchEvent mSelectedThumb == SELECT_THUMB_LEFT:");
                    } else if (mSelectedThumb == SELECT_THUMB_RIGHT) {
                        mThumbSliceRightX = mx;
                        Log.i(TAG,"TEST onTouchEvent mSelectedThumb == SELECT_THUMB_RIGHT:");
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    actionId = MotionEvent.ACTION_UP;
                    mSelectedThumb = SELECT_THUMB_NON;
                    mSeekBarChangeListener.onCurrentFrameUpdated(mThumbSliceLeftValueMS);

                    break;
            }

            notifySeekBarValueChanged(actionId, mSelectedThumb);
        }
        return true;
    }

    private void notifySeekBarValueChanged(int actionId, int selectedThumb) {
        if (mThumbSliceLeftX < 0)
            mThumbSliceLeftX = 0;

        if (mThumbSliceRightX < 0)
            mThumbSliceRightX = 0;

        if (mThumbSliceLeftX > mTotalWidth)
            mThumbSliceLeftX = mTotalWidth;

        if (mThumbSliceRightX > mTotalWidth)
            mThumbSliceRightX = mTotalWidth;

        invalidate();
        if (mSeekBarChangeListener != null) {
            calculateThumbValue();
            mSeekBarChangeListener.SeekBarValueChanged(actionId, selectedThumb, mThumbSliceLeftValueMS, mThumbSliceRightValueMS);
        }
    }

    private void calculateThumbValue() {
        mThumbSliceLeftValueMS =(int) ((mThumbSliceLeftX * mMaxValueInMS) / mTotalWidth);
        mThumbSliceRightValueMS =(int) ((mThumbSliceRightX * mMaxValueInMS) / mTotalWidth);
        Log.i(TAG,"TEST calculateThumbValue(),mThumbSliceLeftValueMS:"+mThumbSliceLeftValueMS+",mThumbSliceRightValueMS:"+mThumbSliceRightValueMS+",mThumbSliceLeftX:"+mThumbSliceLeftX+",mThumbSliceRightX"+mThumbSliceRightX);
    }

    private float calculateCorrds(int timeInMilliSeconds) {
        double coordinatesValue =(timeInMilliSeconds * mTotalWidth) / mMaxValueInMS;;
        Log.i(TAG,"SHAIL calculateCorrds(),timeInMilliSeconds:"+timeInMilliSeconds+",coordinatesValue:"+coordinatesValue);
        return (float) coordinatesValue;
    }

    private void calculateLeftProgressCoOrdinates(){
        if (mLeftProgressMS < mThumbSliceRightValueMS - mProgressMinDiffMS) {
            mThumbSliceLeftX = calculateCorrds(mLeftProgressMS);
        }
        notifySeekBarValueChanged(-1, mSelectedThumb);
    }

    private void calculateRightProgressCoOrdinates(){
        if (mRightProgressMS >= mThumbSliceLeftValueMS + mProgressMinDiffMS) {
            mThumbSliceRightX = calculateCorrds(mRightProgressMS);
        }
        notifySeekBarValueChanged(-1, mSelectedThumb);
    }

    public void setLeftProgress(int timeInMS) {
        mLeftProgressMS = timeInMS;
        Log.i(TAG, "SHAIL setLeftProgress(),mLeftProgressMS:" + mLeftProgressMS);
    }

    public void setRightProgress(int timeInMS) {
        mRightProgressMS = timeInMS;
        Log.i(TAG, "SHAIL setRightProgress(),mRightProgressMS:" + mRightProgressMS);
    }

    public int getLeftProgress() {
        return mThumbSliceLeftValueMS;
    }

    public int getRightProgress() {
        return mThumbSliceRightValueMS;
    }

    public void setProgress(int leftProgress, int rightProgress) {
        if (rightProgress - leftProgress > mProgressMinDiffMS) {
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
        mMaxValueInMS = maxValue;
    }

    public void setProgressMinDiff(int progressMinDiff) {
        mProgressMinDiffMS = progressMinDiff;
        mProgressMinDiffPixels = calculateCorrds(progressMinDiff);
    }

    public void setProgressMaxDiff(int progressMaxDiff) {
        mProgressMaxDiffMS = progressMaxDiff;
        mProgressMaxDiffPixels = calculateCorrds(progressMaxDiff);
    }

    public interface SeekBarChangeListener {
        void SeekBarValueChanged(int actionId, int selectedThumb, int leftThumb, int rightThumb);

        void onCurrentFrameUpdated(int currentTime);
    }

    public void updateFramesView() {
        ViewCompat.postInvalidateOnAnimation(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Log.i(TAG, "onAttachedToWindow()");
    }

    @Override
    protected void onDetachedFromWindow() {
        Log.i(TAG, "onDetachedFromWindow()");
        super.onDetachedFromWindow();
        cleanUpResources();
    }

    public void cleanUpResources() {
        Log.i(TAG, "cleanUpResources()");
        mFramesRenderingManager.clearCache();
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
