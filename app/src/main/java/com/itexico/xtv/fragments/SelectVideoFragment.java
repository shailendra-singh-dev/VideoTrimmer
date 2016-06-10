package com.itexico.xtv.fragments;

import android.support.v4.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.itexico.xtv.R;
import com.itexico.xtv.activities.SelectVideoActivity;


public class SelectVideoFragment extends Fragment   implements View.OnClickListener {

    private static final String TAG = SelectVideoFragment.class.getSimpleName();

    private SelectVideoActivity mSelectVideoActivity;

    public static SelectVideoFragment newInstance() {
        return new SelectVideoFragment();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mSelectVideoActivity = (SelectVideoActivity)context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_video, container, false);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        FloatingActionButton selectGallery = (FloatingActionButton) view.findViewById(R.id.select_gallery);
        selectGallery.setOnClickListener(this);

        FloatingActionButton selectCamera = (FloatingActionButton) view.findViewById(R.id.select_camera);
        selectCamera.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.select_gallery:
                mSelectVideoActivity.openGallery();
                break;

            case R.id.select_camera:
                mSelectVideoActivity.openCameraRecording();
                break;
        }
    }


}
