package com.itexico.xtv.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.itexico.xtv.R;
import com.itexico.xtv.enums.SCREEN;
import com.itexico.xtv.util.AppUtils;

public class SelectVideoActivity extends AppCompatActivity {

    private static final String TAG = SelectVideoActivity.class.getSimpleName();
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 1000;
    private RelativeLayout mXTVToolBar;
    private TextView mTitle;
    private String selectedVideoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_video);
        mXTVToolBar = (RelativeLayout) findViewById(R.id.xtv_tool_bar);
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
        mXTVToolBar.setVisibility(View.GONE);
        Intent intent = new Intent(this, CameraRecordingActivity.class);
        intent.putExtra("orientation", 0);
        intent.putExtra("duration", 5);
        startActivity(intent);
    }

    public void openGallery() {
        //Request for Videos from Gallery..
//                Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
//                startActivityForResult(i, REQUEST_TAKE_GALLERY_VIDEO);
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

                intent.putExtra(EditVideoActivity.INPUT_MEDIA_URI_PATH, selectedVideoUri.toString());

                startActivity(intent);
            }
        }
    }

}
