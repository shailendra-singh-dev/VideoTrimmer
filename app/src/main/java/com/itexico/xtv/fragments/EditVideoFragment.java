package com.itexico.xtv.fragments;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;

import com.itexico.xtv.R;
import com.itexico.xtv.activities.EditVideoActivity;
import com.itexico.xtv.frames_render.FramesRenderingManager;
import com.itexico.xtv.model.MediaInfo;
import com.itexico.xtv.util.AppUtils;
import com.itexico.xtv.views.PlaybackSeekBar;
import com.itexico.xtv.views.XTVFramesSeekBar;


public class EditVideoFragment extends Fragment implements View.OnClickListener, MediaPlayer.OnPreparedListener,
        MediaPlayer.OnCompletionListener, View.OnTouchListener, GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener, PlaybackSeekBar.IPlaybackOnSeekBarChangeListener {

    private static final String PLAYBACK_SEEKBAR_CURRENT_POSITION = "playback_seekbar_current_position";
    private static final String FRAMES_SEEKBAR_LEFT_POSITION = "frames_seekbar_left_position";
    private static final String FRAMES_SEEKBAR_RIGHT_POSITION = "frames_seekbar_right_position";
    private static final String VIDEO_DURATION = "video_duration";
    private static final String TRIMMED_VIDEO_DURATION = "trimmed_video_duration";

    private static final String TAG = EditVideoFragment.class.getSimpleName();
    final private StateObserver mVideoStateObserver = new StateObserver();

    public static final int PROGRESS_MIN_DIFFERENCE = 1 * 1000;
    public static final long PROGRESS_MAX_DIFFERENCE = 3 * 1000;

    private EditVideoActivity mEditVideoActivity = null;

    private PlaybackSeekBar mPlaybackSeekBar = null;
    private GestureDetectorCompat mGestureDetectorCompat = null;

    private TextView mOriginalVideoInfoView;
    private TextView mEditedVideoInfoView;
    private XTVFramesSeekBar mXTVFramesSeekBar;
    private VideoView mVideoView;

    private ImageButton mPlaybackPlayButton;

    private long mCurrentPositionInMS;
    private long mLeftPositionInMS;
    private long mRightPositionInMS;

    private long mVideoDurationInMS;
    private long mTrimmedVideoLengthInMS;
    private long mTrimmedVideoMaxLimitInMS;

    private long mFileSize;

    protected int mVideoWidthIn = 0;
    protected int mVideoHeightIn = 0;

    protected int mVideoWidthOut = 0;
    protected int mVideoHeightOut = 0;

    private long mWrongVideoDuration;
    private long mCorrectVideoDuration;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mEditVideoActivity = (EditVideoActivity) context;
        mGestureDetectorCompat = new GestureDetectorCompat(context, this);
        mGestureDetectorCompat.setOnDoubleTapListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mEditVideoActivity = null;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final LinearLayout editVideoView = (LinearLayout) inflater.inflate(R.layout.fragment_edit_video, container, false);

        final RelativeLayout videoViewController = (RelativeLayout) editVideoView.findViewById(R.id.playback_controller);
        mVideoView = (VideoView) videoViewController.findViewById(R.id.playback_view);
        mVideoView.setOnClickListener(this);
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mGestureDetectorCompat.onTouchEvent(event);
                return true;
            }
        });

        mPlaybackPlayButton = (ImageButton) videoViewController.findViewById(R.id.playback_play);
        mPlaybackPlayButton.setOnClickListener(this);

        mPlaybackSeekBar = (PlaybackSeekBar) editVideoView.findViewById(R.id.playback_seek);
        mPlaybackSeekBar.setIPlaybackOnSeekBarChangeListener(this);

        mXTVFramesSeekBar = (XTVFramesSeekBar) editVideoView.findViewById(R.id.video_slice_seekbar);

        final LinearLayout playbackVideoInfo = (LinearLayout) editVideoView.findViewById(R.id.playback_video_info);
        mOriginalVideoInfoView = (TextView) playbackVideoInfo.findViewById(R.id.original_video_info);
        mEditedVideoInfoView = (TextView) playbackVideoInfo.findViewById(R.id.edited_video_info);

        updateEditedVideoInfo(mVideoWidthIn, mVideoHeightIn,mVideoWidthOut, mVideoHeightOut);

        return editVideoView;
    }


    private void initVideoView() {
        mVideoView.setOnPreparedListener(this);
        mVideoView.setOnCompletionListener(this);
        final String fileName = mEditVideoActivity.getSelectedVideoPath();
        if (null != fileName) {
            mVideoView.setVideoURI(Uri.parse(fileName));
        }
    }

    private void performVideoPlayback() {
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
            mXTVFramesSeekBar.setSliceBlocked(false);
            mXTVFramesSeekBar.removeVideoStatusThumb();

            mPlaybackPlayButton.setVisibility(View.VISIBLE);
        } else {
            mPlaybackPlayButton.setVisibility(View.GONE);

            mVideoView.seekTo(mXTVFramesSeekBar.getLeftProgress());
            mVideoView.start();
            mXTVFramesSeekBar.setSliceBlocked(true);
            mVideoStateObserver.startVideoProgressObserving();
        }
    }

    @Override
    public void onPrepared(final MediaPlayer mp) {
        mXTVFramesSeekBar.setSeekBarChangeListener(new XTVFramesSeekBar.SeekBarChangeListener() {

            @Override
            public void onSeekBarValueChanged(int actionId, int selectedThumb, int leftThumb, int rightThumb) {
                mTrimmedVideoLengthInMS = rightThumb - leftThumb;
                Log.i(TAG, "onSeekBarValueChanged() mTrimmedVideoLengthInMS:" + mTrimmedVideoLengthInMS);
                updateSeekBar(selectedThumb, leftThumb, rightThumb);
                updateEditedVideoDuration(true, mTrimmedVideoLengthInMS);
            }

            @Override
            public void onCurrentFrameUpdated(int currentTime) {
                mVideoView.seekTo(currentTime);
            }

        });
        mp.start();
        mp.pause();
        mp.seekTo(0);

        mVideoDurationInMS = mp.getDuration();
        mTrimmedVideoLengthInMS = mVideoDurationInMS;
        Log.i(TAG, " updateSeekBar() mTrimmedVideoLengthInMS:" + mTrimmedVideoLengthInMS);
        mPlaybackSeekBar.setMax((int) mVideoDurationInMS);
        updateVideoSliceSeekBar((int) mVideoDurationInMS);
    }

    private void updateSeekBar(int selectedThumb, int leftPosition, int rightPosition) {
        Log.i(TAG, "updateSeekBar(),leftPosition:" + leftPosition + ",rightPosition:" + rightPosition);
        if(mRightPositionInMS != rightPosition){
            mRightPositionInMS = rightPosition;
        }
        if (mLeftPositionInMS != leftPosition) {
            boolean isLeftMovement = leftPosition < mLeftPositionInMS;
            final int movementDiff = (int) (isLeftMovement ? mLeftPositionInMS - leftPosition : leftPosition - mLeftPositionInMS);
            Log.i(TAG, " updateSeekBar(), isLeftMovement:" + isLeftMovement + ",mLeftPositionInMS:" + mLeftPositionInMS + ",leftPosition:" + leftPosition + ",movementDiff:" + movementDiff);

            mLeftPositionInMS = leftPosition;
            if (selectedThumb == XTVFramesSeekBar.SELECT_THUMB_LEFT) {
                if (isLeftMovement) {
                    mCurrentPositionInMS = mCurrentPositionInMS + movementDiff;
                } else {
                    mCurrentPositionInMS = mCurrentPositionInMS - movementDiff;
                }
                if (mCurrentPositionInMS < 0) {
                    mCurrentPositionInMS = 0;
                }
                Log.i(TAG, " updateSeekBar() ,mCurrentPositionInMS:" + mCurrentPositionInMS);
                mPlaybackSeekBar.setProgress((int) mCurrentPositionInMS);
            }
        } else if (selectedThumb == XTVFramesSeekBar.SELECT_THUMB_RIGHT) {
            mPlaybackSeekBar.setMax(rightPosition - leftPosition);
            Log.i(TAG, " updateSeekBar(),SELECT_THUMB_RIGHT");
        }
    }

    private void updateVideoSliceSeekBar(final int duration) {
        Log.i(TAG, "OOOOOOOOOOOO  updateVideoSliceSeekBar(),mLeftPositionInMS:"+mLeftPositionInMS+",mRightPositionInMS:"+ mRightPositionInMS);
        mXTVFramesSeekBar.setMaxValue(duration);
        if(mLeftPositionInMS > 0){
            mXTVFramesSeekBar.setLeftProgress((int) mLeftPositionInMS);
        }else{
            mXTVFramesSeekBar.setLeftProgress(0);
        }
        if(mRightPositionInMS > 0 && mRightPositionInMS != mTrimmedVideoMaxLimitInMS){
            mXTVFramesSeekBar.setRightProgress((int) mRightPositionInMS);
        }else{
            mXTVFramesSeekBar.setRightProgress((int) mTrimmedVideoMaxLimitInMS);
        }
        mXTVFramesSeekBar.setProgressMinDiff(PROGRESS_MIN_DIFFERENCE);
        mXTVFramesSeekBar.setProgressMaxDiff((int) PROGRESS_MAX_DIFFERENCE);
        mXTVFramesSeekBar.invalidateView(mEditVideoActivity);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mPlaybackPlayButton.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i(TAG, "onTouch");
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        Log.i(TAG, "onDown;");
        return true;
    }

    @Override
    public void onShowPress(MotionEvent e) {
        Log.i(TAG, "onShowPress;");
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        Log.i(TAG, "onSingleTapUp;");
        return true;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        Log.i(TAG, "onScroll;");
        return true;
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.i(TAG, "onLongPress");
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        Log.i(TAG, "onFling");
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        performVideoPlayback();
        Log.i(TAG, "onSingleTapConfirmed");
        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        Log.i(TAG, "onDoubleTap");
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        Log.i(TAG, "onDoubleTapEvent");
        return true;
    }

    public int getTrimmedVideoStartTime() {
        return mXTVFramesSeekBar.getLeftProgress();
    }

    public int getTrimmedVideoEndTime() {
        return mXTVFramesSeekBar.getRightProgress();
    }

    public int getInputVideoDuration() {
        return (int) mVideoDurationInMS;
    }

    @Override
    public void onProgressChanged(int position, boolean isFromUser) {
        Log.i(TAG, " onProgressChanged() ,mCurrentPositionInMS:" + mCurrentPositionInMS + "position:" + position);
        if (mCurrentPositionInMS != position) {
            mCurrentPositionInMS = position;
            //Updating seekbar...
            mPlaybackSeekBar.setProgress(position);
            //Updating Video position..
            Log.i(TAG, "FRAME seekTo(position),position:" + position);
            mVideoView.seekTo(position);
        }
    }

    private class StateObserver extends Handler {

        private boolean alreadyStarted = false;

        private void startVideoProgressObserving() {
            if (!alreadyStarted) {
                alreadyStarted = true;
                sendEmptyMessage(0);
            }
        }

        private Runnable observerWork = new Runnable() {
            @Override
            public void run() {
                startVideoProgressObserving();
            }
        };

        @Override
        public void handleMessage(Message msg) {
            alreadyStarted = false;
            final int leftProgress = mXTVFramesSeekBar.getLeftProgress();
            final int rightProgress = mXTVFramesSeekBar.getRightProgress();
            boolean isInRange =  mVideoView.getCurrentPosition() > mXTVFramesSeekBar.getLeftProgress() && mVideoView.getCurrentPosition() < mXTVFramesSeekBar.getRightProgress();
            if (mVideoView.isPlaying() && mVideoView.getCurrentPosition() < mXTVFramesSeekBar.getRightProgress()) {
                postDelayed(observerWork, 50);
                Log.i(TAG, "handleMessage() currentPosition():"+mVideoView.getCurrentPosition()+",leftProgress:"+leftProgress+",rightProgress:"+rightProgress);
                if(isInRange){
                    mXTVFramesSeekBar.videoPlayingProgress(mVideoView.getCurrentPosition());
                }
            } else {
                if (mVideoView.isPlaying()) {
                    mVideoView.pause();
                }
                mXTVFramesSeekBar.setSliceBlocked(false);
                mXTVFramesSeekBar.removeVideoStatusThumb();
                mPlaybackPlayButton.setVisibility(View.VISIBLE);
            }
        }
    }

    public static EditVideoFragment newInstance() {
        return new EditVideoFragment();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mTrimmedVideoMaxLimitInMS = getActivity().getIntent().getExtras().getLong(EditVideoActivity.TRIMMED_VIDEO_MAX_LIMIT);
        //mEditVideoActivity.getIntent().getLongExtra(EditVideoActivity.TRIMMED_VIDEO_MAX_LIMIT,0);
        Log.i(TAG, "AAAAAAAAAAAAAAA onActivityCreated(),mTrimmedVideoMaxLimitInMS:" + mTrimmedVideoMaxLimitInMS);
        initVideoView();

    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateEditedVideoDuration(false, mTrimmedVideoLengthInMS);
        updateVideoSliceSeekBar((int) mVideoDurationInMS);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mXTVFramesSeekBar.cleanUpResources();
        mCurrentPositionInMS = 0;
        mLeftPositionInMS = 0;
        mVideoDurationInMS = 0;
        mFileSize = 0;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.playback_play:
                performVideoPlayback();
                break;
        }
    }

    final public class VideoMetaDataAsyncTask extends AsyncTask<Void, Void, MediaInfo> {

        @Override
        protected MediaInfo doInBackground(Void... params) {
            return getMediaInfo();
        }

        @Override
        protected void onPostExecute(MediaInfo mediaInfo) {
            mVideoDurationInMS = mediaInfo.getDuration();
            long width = mediaInfo.getWidth();
            mVideoWidthOut = (int) width;
            long height = mediaInfo.getHeight();
            mVideoHeightOut = (int) height;
            mFileSize = mediaInfo.getSize();

            updateEditedVideoInfo(width, height, mVideoWidthOut, mVideoHeightOut);
        }
    }

    public long getTrimmedVideoLength() {
        return mTrimmedVideoLengthInMS;
    }

    private void updateEditedVideoInfo(long originalVideoWidth, long originalVideoHeight,long editedVideoWidth, long editedVideoHeight) {
        final String hoursMinutesStr = AppUtils.convertMsToHMSFormat(mVideoDurationInMS);
        final String fileSizeStr = AppUtils.readableFileSize(mFileSize);
        String originalVideoInfo = String.format(getResources().getString(R.string.original_video_info), "" + originalVideoWidth, "" + originalVideoHeight, hoursMinutesStr, "" + fileSizeStr);
        mOriginalVideoInfoView.setText(originalVideoInfo);

        String editedVideoInfo = String.format(getResources().getString(R.string.edited_video_info), "" + editedVideoWidth, "" + editedVideoHeight, hoursMinutesStr, "" + fileSizeStr);
        mEditedVideoInfoView.setText(editedVideoInfo);

        updateEditedVideoDuration(false, mTrimmedVideoLengthInMS);
    }

    private MediaInfo getMediaInfo() {
        final FramesRenderingManager framesRenderingManager = FramesRenderingManager.getFramesRenderingManagerInstance();
        framesRenderingManager.setInputFileLocation(mEditVideoActivity.getSelectedVideoPath());
        framesRenderingManager.updateVideoMetaDataInfo();
        return framesRenderingManager.getMediaInfo();
    }

    private void updateEditedVideoDuration(final boolean isFromSeekBar, final long trimmedVideoLength) {
        Log.i(TAG, "updateEditedVideoDuration(),mTrimmedVideoLengthInMS:" + mTrimmedVideoLengthInMS);
        long trimmedVideoSize = mFileSize;
        if (mVideoDurationInMS > 0) {
            float percentage = ((trimmedVideoLength * 100) / mVideoDurationInMS);
            if (100.0 == percentage) {
                mCorrectVideoDuration = trimmedVideoLength;
            } else if (99.0 == percentage) {
                mWrongVideoDuration = trimmedVideoLength;
            }
            if (trimmedVideoSize > 0) {
                trimmedVideoSize = (long) ((mFileSize * percentage) / 100);
                Log.i(TAG, "isFromSeekBar:" + isFromSeekBar + ",percentage:" + percentage + ",mFileSize:" + mFileSize + ",trimmedVideoSize:" + trimmedVideoSize);
            }
        }
        final String fileSizeStr = AppUtils.readableFileSize(trimmedVideoSize);
        final String hoursMinutes = AppUtils.convertMsToHMSFormat(trimmedVideoLength);

        String editedVideoInfo = String.format(getResources().getString(R.string.edited_video_info), "" + mVideoWidthOut, "" + mVideoHeightOut, hoursMinutes, "" + fileSizeStr);
        mEditedVideoInfoView.setText(editedVideoInfo);
    }


    public long getDiff() {
        return mCorrectVideoDuration - mWrongVideoDuration;
    }

    final public class VideoAllFramesAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            final FramesRenderingManager framesRenderingManager = FramesRenderingManager.getFramesRenderingManagerInstance();
            framesRenderingManager.fetchAllVideoFrames(mEditVideoActivity);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            mXTVFramesSeekBar.updateFramesView();
        }
    }

    public void updateView() {
        new VideoMetaDataAsyncTask().execute();
        new VideoAllFramesAsyncTask().execute();
    }

    @Override
    public void onStart() {
        super.onStart();
    }


}
