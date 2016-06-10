package com.itexico.xtv.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class PlaybackSeekBar extends SeekBar implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = PlaybackSeekBar.class.getSimpleName();

    public interface IPlaybackOnSeekBarChangeListener{
        void onProgressChanged(final int progress, final boolean isFromUser);
    }

    private IPlaybackOnSeekBarChangeListener mListener;

    public PlaybackSeekBar(final Context context) {
        super(context);
    }

    public PlaybackSeekBar(final Context context, final AttributeSet attrs) {
        super(context, attrs);
        setOnSeekBarChangeListener(this);
    }

    public PlaybackSeekBar(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setIPlaybackOnSeekBarChangeListener(final IPlaybackOnSeekBarChangeListener iPlaybackOnSeekBarChangeListener){
        mListener = iPlaybackOnSeekBarChangeListener;
    }

    @Override
    public boolean dispatchTouchEvent(final MotionEvent event) {
        boolean isEventHandled = super.dispatchTouchEvent(event);;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            isEventHandled = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isEventHandled && (mListener != null)) {
                mListener.onProgressChanged(getProgress(),true);
            }
        }
        return isEventHandled;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mListener.onProgressChanged(progress, fromUser);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public synchronized void setProgress(int progress) {
        super.setProgress(progress);
        onSizeChanged(getWidth(), getHeight(), 0, 0);
    }


}
