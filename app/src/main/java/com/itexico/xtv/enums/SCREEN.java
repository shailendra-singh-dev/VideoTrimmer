package com.itexico.xtv.enums;


public enum SCREEN {
    EDIT_VIDEO("Edit Video"),SELECT_VIDEO("Select Video"),VIDEO_RECORD("Record Video");

    private String mScreenName;

    SCREEN(String name) {
        mScreenName = name;
    }

    public String getScreenName() {
        return mScreenName;
    }
}
