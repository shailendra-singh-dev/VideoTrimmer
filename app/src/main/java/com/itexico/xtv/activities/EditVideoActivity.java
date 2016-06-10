package com.itexico.xtv.activities;

import android.app.ProgressDialog;
import android.media.MediaCodecInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.itexico.xtv.R;
import com.itexico.xtv.enums.SCREEN;
import com.itexico.xtv.fragments.EditVideoFragment;
import com.itexico.xtv.util.AppUtils;

import org.m4m.MediaFile;
import org.m4m.android.AndroidMediaObjectFactory;
import org.m4m.android.AudioFormatAndroid;
import org.m4m.android.VideoFormatAndroid;
import org.m4m.domain.Pair;

import java.io.IOException;


public class EditVideoActivity extends AppCompatActivity implements View.OnClickListener, EditVideoFragment.IVideoInfoListener {

    public static final String INPUT_MEDIA_PATH = "input_media_path";

    private static final String TAG = EditVideoActivity.class.getSimpleName();
    public static final String INPUT_MEDIA_URI_PATH = "input_media_uri";

    private String mSelectedVideoPath;

    private EditVideoFragment mEditVideoFragment;
    private String mSelectedVideoUriPath;

    public static final int TRIMMED_VIDEO_DEFAULT_WIDTH = 640;
    public static final int TRIMMED_VIDEO_DEFAULT_HEIGHT = 480;

    protected int mVideoWidthOut = TRIMMED_VIDEO_DEFAULT_WIDTH;
    protected int mVideoHeightOut = TRIMMED_VIDEO_DEFAULT_HEIGHT;

    protected int mVideoWidthIn = TRIMMED_VIDEO_DEFAULT_WIDTH;
    protected int mVideoHeightIn = TRIMMED_VIDEO_DEFAULT_HEIGHT;

    protected static final String videoMimeType = "video/avc";
    protected static int VIDEO_BIT_RATE_IN_KB_BYTES = 5000;
    protected static final int VIDEO_FRAME_RATE = 30;

    protected static final int VIDEO_I_FRAME_INTERVAL = 1;
    // Audio
    protected static final String AUDIO_MIME_TYPE = "audio/mp4a-latm";

    protected static final int AUDIO_BIT_RATE = 96 * 1024;

    protected org.m4m.AudioFormat mAudioFormat = null;
    protected org.m4m.VideoFormat mVideoFormat = null;

    protected org.m4m.MediaFileInfo mMediaFileInfo = null;

    protected String mSrcMediaName1 = null;
    protected String mDstMediaPath = null;
    protected org.m4m.Uri mMediaUri1;
    protected org.m4m.MediaComposer mMediaComposer;
    protected long mDuration = 0;
    private long mSegmentStartFrom = 0;
    private long mSegmentEndTo = 0;
    protected AndroidMediaObjectFactory mAndroidMediaObjectFactory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_video);

        mEditVideoFragment = (EditVideoFragment) getSupportFragmentManager().findFragmentById(R.id.edit_video_panel);
        mEditVideoFragment.setmEditedVideoInfoListener(this);

        RelativeLayout mXTVToolBar = (RelativeLayout) findViewById(R.id.xtv_tool_bar);

        final TextView title = (TextView) mXTVToolBar.findViewById(R.id.title);
        title.setText(SCREEN.EDIT_VIDEO.getScreenName());

        final TextView next = (TextView) mXTVToolBar.findViewById(R.id.edit_video_finish);
        next.setVisibility(View.VISIBLE);
        next.setOnClickListener(this);

        final TextView close = (TextView) mXTVToolBar.findViewById(R.id.edit_video_cancel);
        close.setVisibility(View.VISIBLE);
        close.setOnClickListener(this);

        mSelectedVideoPath = getIntent().getStringExtra(INPUT_MEDIA_PATH);
        mSelectedVideoUriPath = getIntent().getStringExtra(INPUT_MEDIA_URI_PATH);
        Log.i(TAG, " onCreate() ,mSelectedVideoPath:" + mSelectedVideoPath);

    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    public String getSelectedVideoPath() {
        return mSelectedVideoPath;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.edit_video_finish:
                int inputVideoDuration = mEditVideoFragment.getInputVideoDuration();
                int trimmedVideoDuration= (int) mEditVideoFragment.getTrimmedVideoLength();
                int diffDuration = inputVideoDuration - trimmedVideoDuration;
                boolean isTrimActionNotAllowed = diffDuration == 0 || diffDuration == mEditVideoFragment.getDiff();
                Log.i(TAG, "onClick() ,inputVideoDuration:" + inputVideoDuration+",trimmedVideoDuration:"+trimmedVideoDuration+",isTrimActionNotAllowed:"+isTrimActionNotAllowed);
                if(!isTrimActionNotAllowed){
                    new VideoTrimmingAsyncTask().execute();
                }else{
                    finish();
                }
                break;

            case R.id.edit_video_cancel:
                finish();
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEditVideoFragment.updateView();
        final VideoAudioVideoEncoderAsyncTask videoAudioVideoEncoderAsyncTask = new VideoAudioVideoEncoderAsyncTask();
        videoAudioVideoEncoderAsyncTask.execute();
    }

    @Override
    public void onInfo(long width, long height) {
        mVideoWidthOut = (int) width;
        mVideoHeightOut = (int) height;
        Log.i(TAG, "onInfo() ,mVideoWidthOut:" + mVideoWidthOut + ",mVideoHeightOut:" + mVideoHeightOut);
    }

    private class VideoTrimmingAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            startTranscode();
            return null;
        }

    }

    private class VideoAudioVideoEncoderAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            getActivityInputs();
            getFileInfo();
            try {
                transcode();
            } catch (Exception e) {
                Log.i(TAG, "onError(), Error while setting audio/video encoding params:" + e.getMessage());
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public org.m4m.IProgressListener mIProgressListener = new org.m4m.IProgressListener() {

        private ProgressDialog mProgressDialog = null;

        @Override
        public void onMediaStart() {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onMediaStart");
                        mProgressDialog = ProgressDialog.show(EditVideoActivity.this, "Please wait ...", "Trimming Video ...", true);
                        mProgressDialog.setCancelable(true);
                        mProgressDialog.show();
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Error in onMediaStart()");
            }
        }

        @Override
        public void onMediaProgress(float progress) {
            final float mediaProgress = progress;
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onMediaProgress() ,mediaProgress:" + mediaProgress);
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Error in onMediaProgress()");
            }
        }

        @Override
        public void onMediaDone() {
            try {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "onMediaDone(),mSegmentStartFrom:" + mSegmentStartFrom + ",mSegmentEndTo:" + mSegmentEndTo + "," + mSrcMediaName1 + ",mSrcMediaName1" +
                                ",mDstMediaPath:" + mDstMediaPath + ",mMediaUri1:" + mMediaUri1 + ",mVideoWidthOut:" + mVideoWidthOut + ",mVideoHeightOut:" + mVideoHeightOut);
                        Toast.makeText(EditVideoActivity.this, "Success", Toast.LENGTH_SHORT).show();
                        mProgressDialog.dismiss();
                        EditVideoActivity.this.finish();
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Error in onMediaDone()");
            }
        }

        @Override
        public void onMediaPause() {
            Log.i(TAG, "onMediaPause()");
        }

        @Override
        public void onMediaStop() {
            Log.i(TAG, "onMediaStop()");
        }

        @Override
        public void onError(Exception exception) {
            try {
                final Exception e = exception;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
                        Log.i(TAG, "onError(), message:" + message);
                        if (null != mProgressDialog) {
                            mProgressDialog.dismiss();
                        }
                        Toast.makeText(EditVideoActivity.this, "Error:" + message, Toast.LENGTH_LONG).show();
                        EditVideoActivity.this.finish();
                    }
                });
            } catch (Exception e) {
                Log.i(TAG, "Error in onError()");
            }
        }
    };


    protected void getFileInfo() {
        try {
            mMediaFileInfo = new org.m4m.MediaFileInfo(new AndroidMediaObjectFactory(getApplicationContext()));
            mMediaFileInfo.setUri(mMediaUri1);

            mDuration = mMediaFileInfo.getDurationInMicroSec();

            mAudioFormat = (org.m4m.AudioFormat) mMediaFileInfo.getAudioFormat();
            if (mAudioFormat == null) {
                Log.e(TAG, "Audio format info unavailable");
            }

            mVideoFormat = (org.m4m.VideoFormat) mMediaFileInfo.getVideoFormat();
            if (mVideoFormat == null) {
                Log.e(TAG, "Video format info unavailable");
            } else {
                mVideoWidthIn = mVideoFormat.getVideoFrameSize().width();
                mVideoHeightIn = mVideoFormat.getVideoFrameSize().height();
                Log.i(TAG, "getFileInfo() mVideoWidthIn:" + mVideoWidthIn + ",mVideoHeightIn:" + mVideoHeightIn);
            }
        } catch (Exception e) {
            String message = (e.getMessage() != null) ? e.getMessage() : e.toString();
            Log.e(TAG, "Error while getting file Info " + message);

        }
    }

    protected void getActivityInputs() {
        mSrcMediaName1 = mSelectedVideoPath;
        mDstMediaPath = AppUtils.getTargetFileName(mSelectedVideoPath);
        mMediaUri1 = new org.m4m.Uri(mSelectedVideoUriPath);
    }

    protected void transcode() throws Exception {
        mAndroidMediaObjectFactory = new AndroidMediaObjectFactory(getApplicationContext());
        mMediaComposer = new org.m4m.MediaComposer(mAndroidMediaObjectFactory, mIProgressListener);
        setTranscodeParameters();
    }

    protected void setTranscodeParameters() throws IOException {

        mMediaComposer.addSourceFile(mMediaUri1);
        mMediaComposer.setTargetFile(mDstMediaPath);

        configureVideoEncoder(mMediaComposer, mVideoWidthOut, mVideoHeightOut);
        configureAudioEncoder(mMediaComposer);
    }

    private void updateSegments() {
        MediaFile mediaFile = mMediaComposer.getSourceFiles().get(0);
        mSegmentStartFrom = mEditVideoFragment.getTrimmedVideoStartTime() * 1000;//1000;
        mSegmentEndTo = mEditVideoFragment.getTrimmedVideoEndTime() * 1000;//2000;

        mediaFile.addSegment(new Pair<Long, Long>(mSegmentStartFrom, mSegmentEndTo));
    }

    protected void configureVideoEncoder(org.m4m.MediaComposer mediaComposer, int width, int height) {

        VideoFormatAndroid videoFormat = new VideoFormatAndroid(videoMimeType, width, height);

        videoFormat.setVideoBitRateInKBytes(VIDEO_BIT_RATE_IN_KB_BYTES);
        videoFormat.setVideoFrameRate(VIDEO_FRAME_RATE);
        videoFormat.setVideoIFrameInterval(VIDEO_I_FRAME_INTERVAL);

        mediaComposer.setTargetVideoFormat(videoFormat);
    }

    protected void configureAudioEncoder(org.m4m.MediaComposer mediaComposer) {
        /**
         * TODO: Audio resampling is unsupported by current m4m release
         * Output sample rate and channel count are the same as for input.
         */
        AudioFormatAndroid aFormat = new AudioFormatAndroid(AUDIO_MIME_TYPE, mAudioFormat.getAudioSampleRateInHz(), mAudioFormat.getAudioChannelCount());

        aFormat.setAudioBitrateInBytes(AUDIO_BIT_RATE);
        aFormat.setAudioProfile(MediaCodecInfo.CodecProfileLevel.AACObjectLC);

        mediaComposer.setTargetAudioFormat(aFormat);
    }

    private boolean startTranscode() {
        boolean isSuccessful = true;
        try {
            updateSegments();
            if (null != mMediaComposer) {
                mMediaComposer.start();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while Transcoding..." + e.getMessage());
            e.printStackTrace();
            isSuccessful = false;
        }
        return isSuccessful;
    }
}
