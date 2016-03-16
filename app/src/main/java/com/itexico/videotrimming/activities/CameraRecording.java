package com.itexico.videotrimming.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
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
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.itexico.videotrimming.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * In order to use this activity you must need to send two parameters inside the intent
 *   1. "orientation" is an integer that allows to use the camera in Landscape or Portrait Mode, for Landscape you must send 1 otherwise will be in Portrait Mode
 *   2. "duration" is an integer that configures the time that the user has for recording (time limit recording).
 *
 * Created by DarkGeat on 3/9/2016.
 */

@SuppressWarnings("deprecation")
public class CameraRecording extends AppCompatActivity implements SurfaceHolder.Callback{

    private final String TAG = CameraRecording.class.getSimpleName();

    private Camera myCamera;
    byte[] tempdata;
    boolean myPreviewRunning = false;
    private boolean isRecording = false;
    private SurfaceHolder mySurfaceHolder;
    private SurfaceView mySurfaceView;
    private Camera.CameraInfo info = new Camera.CameraInfo();
    private Camera.Parameters parameters;
    private int cameraId = 0, duration = 0;
    private SeekBar zoom;
    boolean isflashOn = false;
    private ImageButton recordButton;
    private MediaRecorder mediaRecorder;
    private RelativeLayout container;
    private Button camara;
    private ToggleButton flash;
    private TextView timeStamp;
    public static int rotation = 0;
    private static final int MEDIA_TYPE_IMAGE = 1;
    private static final int MEDIA_TYPE_VIDEO = 2;
    private SCREEN_ORIENTATION screen_orientation = SCREEN_ORIENTATION.LANDSCAPE;

    private Camera.PictureCallback myJPG = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            tempdata = data;
            Intent intent = new Intent();
            File pictureFile = getOutputMediaFile(MEDIA_TYPE_IMAGE);
            if(pictureFile!=null){
                try {
                    FileOutputStream fos = new FileOutputStream(pictureFile);
                    fos.write(data);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d("Test", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("Test","Error accessing file: " + e.getMessage());
                } finally{
                    String url = pictureFile.getPath();
                    intent.putExtra("data", url);
                    intent.putExtra("camara", cameraId);
                    setResult(RESULT_OK, intent);
                }
            }else{
                String url = "";
                intent.putExtra("data", url);
                setResult(RESULT_CANCELED, intent);
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(PixelFormat.TRANSLUCENT);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        int mode = getIntent().getIntExtra("orientation",0);
        duration = getIntent().getIntExtra("duration",0);
        screen_orientation = mode == 1 ? SCREEN_ORIENTATION.LANDSCAPE : SCREEN_ORIENTATION.PORTRAIT;
        setRequestedOrientation(screen_orientation == SCREEN_ORIENTATION.PORTRAIT ?
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        setContentView(R.layout.activity_camera_record);

        int numeroCameras = Camera.getNumberOfCameras();
        Camera.getCameraInfo(0, info);
        mySurfaceView = (SurfaceView)findViewById(R.id.surface);
        mySurfaceHolder = mySurfaceView.getHolder();
        mySurfaceHolder.addCallback(this);
        mySurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        container = (RelativeLayout)findViewById(R.id.containerTimer);
        timeStamp = (TextView)findViewById(R.id.textTimer);

        recordButton = (ImageButton) findViewById(R.id.buttonRecord);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clicVideoRecording();
            }
        });

        flash = (ToggleButton) findViewById(R.id.flash);
        flash.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                isflashOn = isChecked;
                if (myPreviewRunning) {
                    myCamera.stopPreview();
                    ObtenerParametros();
                    myCamera.startPreview();
                }
            }
        });
        camara = (Button) findViewById(R.id.buttonCameras);
        if(numeroCameras >1){
            camara.setVisibility(View.VISIBLE);
        }
        camara.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (cameraId == 0)
                    cameraId++;
                else
                    cameraId--;
                if (myPreviewRunning) {
                    myCamera.stopPreview();
                    myCamera.release();
                    myCamera = Camera.open(cameraId);
                    parameters = myCamera.getParameters();
                    ObtenerParametros();
                    try {
                        myCamera.setPreviewDisplay(mySurfaceHolder);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (screen_orientation == SCREEN_ORIENTATION.PORTRAIT) {
                        setCameraDisplayOrientation(CameraRecording.this, cameraId, myCamera);
                    }
                    myCamera.startPreview();
                }
            }
        });
        zoom = (SeekBar)findViewById(R.id.seekBarZoom);
        zoom.setMax(100);
        zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                myCamera.stopPreview();
                ObtenerParametros();
                parameters.setZoom(progress);
                myCamera.startPreview();
            }
        });
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            myCamera = Camera.open(cameraId);
            parameters = myCamera.getParameters();
            if(isflashOn){
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }else{
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
            parameters.setAutoWhiteBalanceLock(true);
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            myCamera.setPreviewDisplay(holder);
            zoom.setMax(parameters.getMaxZoom());
            if (screen_orientation == SCREEN_ORIENTATION.PORTRAIT){
                setCameraDisplayOrientation(this, cameraId, myCamera);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

        try {
            if(myPreviewRunning){
                myCamera.stopPreview();
                myPreviewRunning = false;
            }
            ObtenerParametros();
            myCamera.setPreviewDisplay(holder);
            myCamera.startPreview();
            myPreviewRunning = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void ObtenerParametros(){
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        Camera.Size optimalSize = getOptimalPreviewSize(sizes, CameraRecording.this.getResources().getDisplayMetrics().widthPixels, CameraRecording.this.getResources().getDisplayMetrics().heightPixels);
        parameters.setPreviewSize(optimalSize.width,optimalSize.height);
        if(isflashOn){
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        }else{
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        }
        parameters.setAutoWhiteBalanceLock(true);
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        myCamera.setParameters(parameters);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        myCamera.stopPreview();
        myCamera.release();
    }


    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info =  new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // this helps mirror view
        } else {  // back camera
            result = (info.orientation - degrees + 360) % 360;
        }
        CameraRecording.rotation = rotation;
        camera.setDisplayOrientation(result);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;

        if (sizes==null) return null;

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

    private File getOutputMediaFile(int type){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        if (!Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED)) {
            return  null;
        }

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "CameraSample");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()) {
                Log.d("CameraSample", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE){
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_"+ timeStamp + ".jpg");
        } else if(type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "VID_"+ timeStamp + ".mp4");
        } else {
            return null;
        }

        return mediaFile;
    }

    public void TakePicture(View v){
        myCamera.takePicture(null, null, myJPG);
    }

    private boolean prepareVideoRecorder(){

        mediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        myCamera.unlock();
        mediaRecorder.setCamera(myCamera);

        // Step 2: Set sources
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        if (screen_orientation == SCREEN_ORIENTATION.PORTRAIT){
            if (cameraId > 0){
                mediaRecorder.setOrientationHint(270);
            }else {
                mediaRecorder.setOrientationHint(90);
            }
        }

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        if (cameraId > 0){
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));
            mediaRecorder.setVideoFrameRate(15);
        }else {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        }
        if (duration > 0){
            mediaRecorder.setMaxDuration(duration * 1000);
        }

        // Step 4: Set output file
        mediaRecorder.setOutputFile(getOutputMediaFile(MEDIA_TYPE_VIDEO).toString());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(mySurfaceView.getHolder().getSurface());

        // Step 6: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
            container.setVisibility(View.VISIBLE);
            camara.setVisibility(View.GONE);
            flash.setVisibility(View.GONE);
            zoom.setVisibility(View.GONE);
            new CountDownTimer((duration + 2) * 1000, 1000) {
                public void onTick(long millisUntilFinished) {
                    timeStamp.setText("seconds remaining: " + (millisUntilFinished - 1000) / 1000);
                }
                public void onFinish() {
                    if (isRecording) {
                        timeStamp.setText("done!");
                        recordButton.performClick();
                    }
                }
            }.start();
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

    private void releaseMediaRecorder(){
        if (mediaRecorder != null) {
            mediaRecorder.reset();   // clear recorder configuration
            mediaRecorder.release(); // release the recorder object
            mediaRecorder = null;
            myCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera(){
        if (myCamera != null){
            myCamera.release();        // release the camera for other applications
            myCamera = null;
        }
    }

    private void clicVideoRecording(){
        if (isRecording) {
            // stop recording and release camera
            mediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            myCamera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
            //setCaptureButtonText("Capture");
            isRecording = false;
            recordButton.setImageResource(R.drawable.ic_video_recording);
            finish();
        } else {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mediaRecorder.start();

                // inform the user that recording has started
                //setCaptureButtonText("Stop");
                isRecording = true;
                recordButton.setImageResource(R.drawable.ic_stop);
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                releaseCamera();
                // inform user
            }
        }
    }

    public enum SCREEN_ORIENTATION{
        PORTRAIT,LANDSCAPE
    }

}
