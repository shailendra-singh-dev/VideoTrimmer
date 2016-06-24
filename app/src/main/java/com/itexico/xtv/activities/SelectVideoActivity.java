package com.itexico.xtv.activities;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.itexico.xtv.R;
import com.itexico.xtv.enums.SCREEN;
import com.itexico.xtv.fragments.EditVideoFragment;
import com.itexico.xtv.util.AppUtils;

public class SelectVideoActivity extends AppCompatActivity {

    public static final int TRIMMED_VIDEO_DEFAULT_WIDTH = 640;
    public static final int TRIMMED_VIDEO_DEFAULT_HEIGHT = 480;

    private static final String TAG = SelectVideoActivity.class.getSimpleName();
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1000;
    private static final int REQUEST_READ_EXTERNAL_STORAGE_PERMISSION = 10;
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    private Toolbar mXTVToolBar;
    private TextView mTitle;
    private String selectedVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video);
        mXTVToolBar = (Toolbar) findViewById(R.id.xtv_tool_bar);
        mTitle = (TextView) mXTVToolBar.findViewById(R.id.title);
        mTitle.setText(SCREEN.SELECT_VIDEO.getScreenName());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


    public void openCameraRecording() {
        final String[] permissionsForCameraRecording = new String[]{Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO};
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Requesting Camera Permission");
                builder.setMessage("This app requires permission to record video from Camera..");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ActivityCompat.requestPermissions(SelectVideoActivity.this, permissionsForCameraRecording, REQUEST_CAMERA_PERMISSION);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(SelectVideoActivity.this, permissionsForCameraRecording, REQUEST_CAMERA_PERMISSION);
            }
        } else {
            openCameraRecordingView();
        }
    }

    private void openCameraRecordingView(){
        mXTVToolBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, CameraRecordingActivity.class);
        intent.putExtra("orientation", 0);
        intent.putExtra("duration", 70);
        startActivity(intent);
    }

    public void openGallery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Requesting Read External Storage");
                builder.setMessage("This app requires accesss to External Storage to get the Video for editing..");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        ActivityCompat.requestPermissions(SelectVideoActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_READ_EXTERNAL_STORAGE_PERMISSION);
            }
        } else {
            openGalleryPicker();
        }
    }

    private void openGalleryPicker(){
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
                Uri selectedVideoUri = data.getData();
                selectedVideoPath = AppUtils.getPath(getApplicationContext(), selectedVideoUri);
                Log.i(TAG, "selectedVideoUri:" + selectedVideoUri + ",selectedVideoPath:" + selectedVideoPath);

                Intent intent = new Intent(this, EditVideoActivity.class);
                intent.putExtra(EditVideoActivity.INPUT_MEDIA_PATH, selectedVideoPath);
                intent.putExtra(EditVideoActivity.TRIMMED_VIDEO_MAX_LIMIT, EditVideoFragment.PROGRESS_MAX_DIFFERENCE);

                startActivity(intent);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_READ_EXTERNAL_STORAGE_PERMISSION:
                openGalleryPicker();
                break;

            case REQUEST_CAMERA_PERMISSION:
                openCameraRecordingView();
                break;
        }
    }
}
