package com.itexico.xtv.frames_render;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.util.Log;

import com.itexico.xtv.R;
import com.itexico.xtv.model.MediaInfo;
import com.itexico.xtv.util.AppUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by iTexico Developer on 4/4/2016.
 */
public class FramesRenderingManager {

    public interface ICurrentFrameRunnable{
        void onCurrentFrameReceived(final Bitmap currentFrame);
    }

    private static final String TAG = FramesRenderingManager.class.getSimpleName();

    private static final int INITIAL_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 1;
    /* Sets the amount of time an idle thread waits before terminating */
    private static final int KEEP_ALIVE_TIME = 10;
    /* Sets the Time Unit to seconds */
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static final FramesRenderingManager mFramesRenderingManagerInstance = new FramesRenderingManager();

    final private MediaMetadataRetriever mMediaMetadataRetriever = new MediaMetadataRetriever();

    final private MediaInfo mMediaInfo = new MediaInfo();

    private final BlockingQueue<Runnable> mFramesOperationQueue;
    private ThreadPoolExecutor mThreadPool;

    private ICurrentFrameRunnable mICurrentFrameRunnable;

    public void setmICurrentFrameListener(ICurrentFrameRunnable mICurrentFrameRunnable) {
        this.mICurrentFrameRunnable = mICurrentFrameRunnable;
    }

    private FramesRenderingManager() {
        //Making Singleton..
        mFramesOperationQueue = new LinkedBlockingQueue<Runnable>();
    }

    public static FramesRenderingManager getFramesRenderingManagerInstance() {
        return mFramesRenderingManagerInstance;
    }

    public void updateVideoMetaDataInfo() {
        String mInputFileLocation = mMediaInfo.getInputFileLocation();

        final File file = new File(mInputFileLocation);
        long mSize = file.length();
        mMediaInfo.setSize(mSize);

        mMediaMetadataRetriever.setDataSource(mInputFileLocation);

        String keyDuration = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long duration = AppUtils.getIntegerFromString(keyDuration);
        mMediaInfo.setDuration(duration);

        String keyWidth = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
        long width = AppUtils.getIntegerFromString(keyWidth);
        mMediaInfo.setWidth(width);

        String keyHeight = mMediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
        long height = AppUtils.getIntegerFromString(keyHeight);
        mMediaInfo.setHeight(height);

    }

    public List<Bitmap> fetchAllVideoFrames(final Context context) {
        ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
        int frameWidth = context.getResources().getDimensionPixelSize(R.dimen.frames_thumbnail_width);
        int frameWidthCount = 0;
        final int totalWidth = AppUtils.getScreenWidth();
        Log.i(TAG, "fetchAllVideoFrames totalWidth:" + totalWidth + ",frameWidth" + frameWidth);
        long mDuration = mMediaInfo.getDuration();
        for (int i = 1000000; i < mDuration * 1000; i += 1000000) {
            if (frameWidthCount < totalWidth) {
                Bitmap bitmap = mMediaMetadataRetriever.getFrameAtTime(i, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                Log.i(TAG, "fetchAllVideoFrames bitmap:" + bitmap);
                bitmapArrayList.add(bitmap);
                frameWidthCount += frameWidth;
            } else {
                break;
            }
        }
        Log.i(TAG, "fetchAllVideoFrames size:" + bitmapArrayList.size());
        return bitmapArrayList;
    }

    public Bitmap getCurrentFrame(long frameTimeLong) {
        return mMediaMetadataRetriever.getFrameAtTime(frameTimeLong, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
    }

    public void setInputFileLocation(String inputFileLocation) {
        mMediaInfo.setInputFileLocation(inputFileLocation);
    }

    public MediaInfo getMediaInfo(){
        return mMediaInfo;
    }

    public void startFramesThreadPool() {
        if ((mThreadPool == null) || mThreadPool.isShutdown()) {
            mThreadPool = new ThreadPoolExecutor(INITIAL_POOL_SIZE, MAX_POOL_SIZE, KEEP_ALIVE_TIME,
                    KEEP_ALIVE_TIME_UNIT, mFramesOperationQueue);
        }
    }


    public void insertFrameRunnableInThreadPool(final long mMediaCurrentTime) {
        if (mThreadPool != null) {
            mThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    final FramesRenderingManager framesRenderingManager = FramesRenderingManager.getFramesRenderingManagerInstance();
                    final Bitmap currentFrame=  framesRenderingManager.getCurrentFrame(mMediaCurrentTime);
                    mICurrentFrameRunnable.onCurrentFrameReceived(currentFrame);
                }
            });
        }
    }
    public void stopFramesThreadPool() {
        mFramesOperationQueue.clear();
        if ((mThreadPool != null) && !mThreadPool.isShutdown()) {
            mThreadPool.shutdownNow();
        }
        mThreadPool = null;
    }

}
