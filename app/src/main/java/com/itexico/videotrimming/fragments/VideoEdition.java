package com.itexico.videotrimming.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.itexico.videotrimming.R;

/**
 * Created by darkgeat on 3/16/16.
 */
public class VideoEdition extends Fragment {
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_video_edition,container,false);
        //All the UI link must be here like --> EditText example = (EditText)v.findViewById(R.id.exampleId);
        return v;
    }
}
