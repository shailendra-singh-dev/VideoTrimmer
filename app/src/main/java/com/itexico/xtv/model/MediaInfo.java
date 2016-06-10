package com.itexico.xtv.model;

import android.os.Parcel;
import android.os.Parcelable;


public class MediaInfo implements Parcelable {

    private String mInputFileLocation;
    private long mDuration;
    private long mWidth;
    private long mHeight;
    private long mSize;

    public String getInputFileLocation() {
        return mInputFileLocation;
    }

    public void setInputFileLocation(String mInputFileLocation) {
        this.mInputFileLocation = mInputFileLocation;
    }

    public static final Creator<MediaInfo> CREATOR = new Creator<MediaInfo>() {
        @Override
        public MediaInfo createFromParcel(Parcel in) {
            return new MediaInfo(in);
        }

        @Override
        public MediaInfo[] newArray(int size) {
            return new MediaInfo[size];
        }
    };

    public MediaInfo() {
        mDuration = 0l;
        mWidth = 0l;
        mHeight = 0l;
        mSize = 0l;
    }

    protected MediaInfo(Parcel in) {
        mDuration = in.readLong();
        mWidth = in.readLong();
        mHeight = in.readLong();
        mSize = in.readLong();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mDuration);
        dest.writeLong(mWidth);
        dest.writeLong(mHeight);
        dest.writeLong(mSize);
    }

    public long getHeight() {
        return mHeight;
    }

    public void setHeight(long height) {
        this.mHeight = height;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long size) {
        mSize = size;
    }

    public long getDuration() {
        return mDuration;
    }

    public void setDuration(long duration) {
        this.mDuration = duration;
    }

    @Override
    public int describeContents() {
        return 0;
    }


    public long getWidth() {
        return mWidth;
    }

    public void setWidth(long width) {
        this.mWidth = width;
    }
}
