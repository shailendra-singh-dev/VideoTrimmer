package com.itexico.xtv.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.CamcorderProfile;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import com.itexico.xtv.R;

import java.io.File;
import java.io.FilenameFilter;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;


public class AppUtils {

    private static final String TAG = AppUtils.class.getSimpleName();

    public static String getTargetFileName(String inputFileName) {
        final File file = new File(inputFileName).getAbsoluteFile();
        final String fileName = file.getName();

        String[] filenames = file.getParentFile().list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename != null && filename.startsWith("trimmed-") && filename.endsWith(fileName);
            }
        });

        int count = 0;
        String targetFileName;
        List<String> fileList = Arrays.asList(filenames);

        do {
            targetFileName = "trimmed-" + String.format("%03d", count++) + "-" + fileName;
        } while (fileList.contains(targetFileName));

        return new File(file.getParent(), targetFileName).getPath();
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }

    public static Bitmap drawableToBitmap(final Context context, final int resourceId) {
        Drawable drawable = context.getResources().getDrawable(resourceId);
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    public static String convertMsToHMSFormat(long timeInMillis) {
        long millis = timeInMillis % 1000;
        long second = (timeInMillis / 1000) % 60;
        long minute = (timeInMillis / (1000 * 60)) % 60;
        long hour = (timeInMillis / (1000 * 60 * 60)) % 24;
        String time = "00:00:00";
        Log.i(TAG, "millis in convertMsToHMSFormat:" + millis);
        try {
            time = String.format(String.format(Locale.getDefault(), "%02d:%02d:%02d", hour, minute, second));
        } catch (NullPointerException nullPointerException) {
            Log.e(TAG, "NullPointerException in convertMsToHMSFormat:");
            nullPointerException.printStackTrace();
        } catch (IllegalFormatException illegalFormatException) {
            Log.e(TAG, "IllegalFormatException in convertMsToHMSFormat");
            illegalFormatException.printStackTrace();
        }
        return time;
//        Log.i(TAG, "convertMsToHMSFormat:" + time);
//        return timeInMillis == 0 ? "00:00:00" : String.format(Locale.getDefault(), "%02d:%02d:%02d",
//                TimeUnit.MILLISECONDS.toHours(timeInMillis),
//                TimeUnit.MILLISECONDS.toMinutes(timeInMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(timeInMillis)),
//                TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
//                        - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(timeInMillis)));
    }

    public static String readableFileSize(long size) {
        Log.i(TAG, " readableFileSize: " + size);
        if (size <= 0) return "0 MB";
        final String[] units = new String[]{"B", "kB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }

    public static String getPath(final Context context, final Uri uri) {

        //check here to KITKAT or new version
        boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static int getNumberOfFramesToDraw(Context context) {
        final int totalWidth = getScreenWidth();
        final int frameWidth = context.getResources().getDimensionPixelSize(R.dimen.frames_thumbnail_width);
        return totalWidth / frameWidth;
    }

    public static String getTimeForTrackFormat(int timeInMills, boolean display2DigitsInMinsSection) {
        int minutes = (timeInMills / (60 * 1000));
        int seconds = (timeInMills - minutes * 60 * 1000) / 1000;
        String result = display2DigitsInMinsSection && minutes < 10 ? "0" : "";
        result += minutes + ":";
        if (seconds < 10) {
            result += "0" + seconds;
        } else {
            result += seconds;
        }
        return result;
    }

    public static int getIntegerFromString(final String stringValue) {
        int intValue = 0;
        try {
            intValue = Integer.parseInt(stringValue);
        } catch (NumberFormatException exception) {
            intValue = 0;
        }
        return intValue;

    }

    public static int getMeanValue(int[] m) {
        int sum = 0;
        for (int i = 0; i < m.length; i++) {
            sum += m[i];
        }
        return sum / m.length;
    }


    public static String getParentDirectory(String filePath) {
        File file = new File(filePath);
        String parentPath = file.getAbsoluteFile().getParent();
        return parentPath;
    }

    public static Uri getVideoContentUri(Context context, String filePath) {
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID},
                MediaStore.Video.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            cursor.close();
            return Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            final File imageFile= new File(filePath);
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    public static int[] getVideoQualityResolution(int videoQuality){
        int[] resolution = new int[2];
        switch (videoQuality) {
            case 0:
                resolution[0] = 640;
                resolution[1] = 480;
                break;

            case 1:
                resolution[0] = 1280;
                resolution[1] = 720;
                break;

            case 2:
                resolution[0] = 1920;
                resolution[1] = 1080;
                break;

            default:
                break;
        }
        return resolution;
    }

    public static int getCameraStandardVideoQuality(int videoQuality){
        int cameraVideoQuality = 0;
        switch (videoQuality) {
            case 0:
                cameraVideoQuality = CamcorderProfile.QUALITY_480P;
                break;

            case 1:
                cameraVideoQuality = CamcorderProfile.QUALITY_720P;
                break;

            case 2:
                cameraVideoQuality = CamcorderProfile.QUALITY_1080P;
                break;

            default:
                break;
        }
        return cameraVideoQuality;
    }


}
