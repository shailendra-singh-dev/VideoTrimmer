package com.itexico.xtv.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.itexico.xtv.R;
import com.itexico.xtv.util.AppConst;
import com.itexico.xtv.util.AppUtils;
import com.itexico.xtv.views.AnimatedCircleImageView;
import com.itexico.xtv.views.CircleButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by DarkGeat on 3/9/2016.
 */

@SuppressWarnings("deprecation")
public class CameraRecordingActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    public static final int VIDEO_QUALITY_1080P_SIZE = 1920 * 1080;

    private final String TAG = CameraRecordingActivity.class.getSimpleName();

    private Camera myCamera;
    boolean myPreviewRunning = false;
    private boolean isRecording = false;
    private SurfaceHolder mySurfaceHolder;
    private SurfaceView mySurfaceView;
    private Camera.CameraInfo info = new Camera.CameraInfo();
    private Camera.Parameters parameters;
    private int cameraId = 0;
    private int duration = 0;
    boolean isflashOn = false;
    private CircleButton recordButton;
    private MediaRecorder mediaRecorder;
    private RelativeLayout container;
    private TextView mRecordVideoDuration;
    public static int rotation = 0;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private int screen_orientation = -1;

    private AnimatedCircleImageView mAnimatedCircleImageView;
    private String mRecordedVideoLocation;
    private CountDownTimer mCountDownTimer = null;
    private int mAllowedTrimmedVideoQaulity =1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Intent intent = getIntent();
        cameraId = intent.getIntExtra("CAMERA_TYPE", 0);

        duration = getIntent().getIntExtra("duration", 0);
        screen_orientation = getResources().getConfiguration().orientation;

        Log.i(TAG, "screen_orientation:" + screen_orientation);
        setContentView(R.layout.activity_camera_record);

        Camera.getCameraInfo(cameraId, info);
        mySurfaceView = (SurfaceView) findViewById(R.id.surface);
        mySurfaceHolder = mySurfaceView.getHolder();
        mySurfaceHolder.addCallback(this);
        mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        container = (RelativeLayout) findViewById(R.id.containerTimer);

        if (null != container) {
            mRecordVideoDuration = (TextView) container.findViewById(R.id.textTimer);
        }

        if (null != mRecordVideoDuration) {
            final String durationStr = AppUtils.convertMsToHMSFormat(duration);
            mRecordVideoDuration.setText(durationStr);
        }

        if (null != container) {
            mAnimatedCircleImageView = (AnimatedCircleImageView) container.findViewById(R.id.record_circle);
            mAnimatedCircleImageView.setDefaultColor(Color.WHITE);
            mAnimatedCircleImageView.setPressedColor(Color.RED);
        }

        recordButton = (CircleButton) findViewById(R.id.buttonRecord);
        if (null != recordButton) {
            recordButton.setCirclePressed(false);
            recordButton.setDefaultColor(Color.WHITE);
            recordButton.setPressedColor(Color.RED);

            recordButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mAnimatedCircleImageView.startAnimation();
                    clicVideoRecording();
                }
            });
        }

        Button recordCancel = (Button) findViewById(R.id.record_cancel);
        if (null != recordCancel) {
            recordCancel.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    CameraRecordingActivity.this.finish();
                }
            });
        }

        ImageButton changeCamera = (ImageButton) findViewById(R.id.change_camera);
        if (null != changeCamera) {
            changeCamera.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    //changeCamera();
                }
            });
        }
    }

    public void changeCamera() {
        if(cameraId == Camera.CameraInfo.CAMERA_FACING_BACK){
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        onCreate(null);
        if (myCamera != null) {
            if (myPreviewRunning) {
                myCamera.stopPreview();
                try {
                    myCamera.setPreviewDisplay(null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                myCamera.release();
                myPreviewRunning = false;
                myCamera =null;
            }
            Intent intent = getIntent();
            intent.putExtra("CAMERA_TYPE", cameraId);
            finish();
            overridePendingTransition(0, 0);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            myCamera = Camera.open(cameraId);
            parameters = myCamera.getParameters();
            if (isflashOn) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            } else {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            parameters.setAutoWhiteBalanceLock(true);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            myCamera.setPreviewDisplay(holder);
            if (screen_orientation == Configuration.ORIENTATION_PORTRAIT) {
                setCameraDisplayOrientation(this, cameraId, myCamera);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        try {
            if (myPreviewRunning) {
                myCamera.stopPreview();
                myPreviewRunning = false;
            }
            obtainParameters();
            myCamera.setPreviewDisplay(holder);
            myCamera.startPreview();
            myPreviewRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void obtainParameters() {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();

        Camera.Size optimalSize = getOptimalPreviewSize(sizes, CameraRecordingActivity.this.getResources().getDisplayMetrics().widthPixels, CameraRecordingActivity.this.getResources().getDisplayMetrics().heightPixels);
        parameters.setPreviewSize(optimalSize.width, optimalSize.height);
        if (isflashOn) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        } else {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        parameters.setAutoWhiteBalanceLock(true);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        myCamera.setParameters(parameters);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mySurfaceHolder.removeCallback(this);
        if(null != myCamera){
            myCamera.stopPreview();
            myCamera.release();
        }
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // this helps mirror view
        } else {  // back camera
            result = (info.orientation - degrees + 360) % 360;
        }
        CameraRecordingActivity.rotation = rotation;
        camera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w / h;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), getString(R.string.app_name));
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        final String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_" + timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    private boolean prepareVideoRecorder() {

        mediaRecorder = new MediaRecorder();
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                Log.v(TAG, " onInfo ");
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    Log.v(TAG, " Maximum Duration Reached");
                    if (isRecording) {
                        mAnimatedCircleImageView.setCirclePressed(false);
                        if (null != recordButton) {
                            recordButton.performClick();
                        }
                    }
                }
            }
        });

        mediaRecorder.setOnErrorListener(new MediaRecorder.OnErrorListener() {
            @Override
            public void onError(MediaRecorder mr, int what, int extra) {
                Log.v(TAG, " onError ");
            }
        });

        // Step 1: Unlock and set camera to MediaRecorder
        myCamera.unlock();
        mediaRecorder.setCamera(myCamera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (screen_orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (cameraId > 0) {
                mediaRecorder.setOrientationHint(270);
            } else {
                mediaRecorder.setOrientationHint(90);
            }
        }

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        int expectedVideoQulaity = AppUtils.getCameraStandardVideoQuality(mAllowedTrimmedVideoQaulity);
        Log.i(TAG, "SHAIL mediaRecorder video expectedVideoQulaity:" + expectedVideoQulaity);
        if (cameraId > 0) {
            mediaRecorder.setProfile(CamcorderProfile.get(expectedVideoQulaity));
            mediaRecorder.setVideoFrameRate(15);
        } else {
            mediaRecorder.setProfile(CamcorderProfile.get(expectedVideoQulaity));
        }
        if (duration > 0) {
            mediaRecorder.setMaxDuration(duration * AppConst.SEC_TO_MILLI_SEC_FACTOR);
        }

        // Step 4: Set output file
        mRecordedVideoLocation = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
        mediaRecorder.setOutputFile(mRecordedVideoLocation);

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mySurfaceView.getHolder().getSurface());


        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
            container.setVisibility(View.VISIBLE);

            Log.i(TAG, " mDuration * AppConst.SEC_TO_MILLI_SEC_FACTOR:" + duration * AppConst.SEC_TO_MILLI_SEC_FACTOR);
            mCountDownTimer = new CountDownTimer(duration * AppConst.SEC_TO_MILLI_SEC_FACTOR, AppConst.RECORD_COUNT_DOWN_TIMER_DELAY) {

                long countDownTimerInMillis = 0;

                public void onTick(long millisUntilFinished) {
                    countDownTimerInMillis = millisUntilFinished;
                    Log.i(TAG, " onTick millisUntilFinished:" + millisUntilFinished);
                    final String timeLeft = AppUtils.convertMsToHMSFormat(millisUntilFinished);
                    mRecordVideoDuration.setText(timeLeft);
                    if (countDownTimerInMillis % 2 == 0) {
                        mAnimatedCircleImageView.setCirclePressed(false);
                    } else {
                        mAnimatedCircleImageView.setCirclePressed(true);
                    }
                    countDownTimerInMillis -= 1000;
                }

                public void onFinish() {
                    final String timeLeft = AppUtils.convertMsToHMSFormat(countDownTimerInMillis);
                    Log.i(TAG, " onFinish millisUntilFinished:" + countDownTimerInMillis);
                    mRecordVideoDuration.setText(timeLeft);
                }
            };
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
        }
    }

    private void releaseCamera() {
        if (myCamera != null) {
            myCamera.release();        // release the camera for other applications
            myCamera = null;
        }
    }

    private void clicVideoRecording() {
        if (isRecording) {
            // stop recording and release camera
//            mediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            // inform the user that recording has stopped
            isRecording = false;
            recordButton.setCirclePressed(false);
            mAnimatedCircleImageView.setVisibility(View.GONE);
            mAnimatedCircleImageView.setCirclePressed(false);

            launchEditVideo();
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mediaRecorder.start();
                mCountDownTimer.start();

                // inform the user that recording has started
                isRecording = true;
                recordButton.setCirclePressed(true);
                mAnimatedCircleImageView.setVisibility(View.VISIBLE);
                mAnimatedCircleImageView.setCirclePressed(true);
            } else {
                // prepare didn't work, release the camera...
                releaseMediaRecorder();
                releaseCamera();
                // inform user
            }
        }
    }

    private void launchEditVideo() {
        finish();
        Intent editVideoIntent = new Intent(this, EditVideoActivity.class);
        Log.i(TAG, "launchEditVideo() mRecordedVideoLocation:" + mRecordedVideoLocation);
        editVideoIntent.putExtra(EditVideoActivity.INPUT_MEDIA_PATH, mRecordedVideoLocation);
        String fileUri = AppUtils.getVideoContentUri(this, mRecordedVideoLocation).toString();
        editVideoIntent.putExtra(EditVideoActivity.INPUT_MEDIA_URI_PATH, fileUri);
        startActivity(editVideoIntent);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCamera();
        releaseMediaRecorder();
    }
}
