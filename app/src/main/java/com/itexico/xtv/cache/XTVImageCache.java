package com.itexico.xtv.cache;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

/**
 * Created by iTexico Developer on 6/23/2016.
 */
public class XTVImageCache {

    private static final String LOG = XTVImageCache.class.getSimpleName();

    private LruCache<String, Bitmap> mBitmapCache = null;

    public XTVImageCache(final int inputCacheSize) {
        int cacheSize = inputCacheSize;
        if (cacheSize <= 0) {
            final int maxMemory = (int)(Runtime.getRuntime().maxMemory());
            cacheSize = maxMemory / 4;// 1/4th of the RAM only
        }
        Log.i(LOG, " cache size: " + cacheSize);
        mBitmapCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(final String key, final Bitmap value) {
                final int size = getBitmapSize(value);
                Log.i(LOG, " bitmap size : " + size);
                return size;
            }

            @Override
            protected void entryRemoved(final boolean evicted, final String key, final Bitmap oldValue,
                                        final Bitmap newValue) {
                Log.i(LOG, " entry removed: " + key);
                if (oldValue != null) {
                    oldValue.recycle();
                }
            }
        };
    }

    /**
     * Get the size in bytes of a bitmap in a BitmapDrawable.
     *
     * @param bitmap
     * @return size in bytes
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static int getBitmapSize(final Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        }
        return bitmap.getRowBytes() * bitmap.getHeight();
    }

    /**
     * function to get the bitmap from the cache
     *
     * @param key - key to the bitmap
     * @return bitmap
     */
    public Bitmap getBitmap(final String key) {
        Bitmap bit = null;
        if (key != null) {
            synchronized (mBitmapCache) {
                bit = mBitmapCache.get(key);
            }
            return bit;
        }
        return null;

    }

    /**
     * function to insert the bitmap into the cache
     *
     * @param key - unique identifier for the bitmap.
     * @param bitmap - the bitmap to be stored.
     */
    public void putBitmap(final String key, final Bitmap bitmap) {
        Log.i(LOG, " key: " + key);
        synchronized (mBitmapCache) {
            if (mBitmapCache.get(key) == null) {
                mBitmapCache.put(key, bitmap);
                Log.i(LOG, " insert: " + key);
            } else {
                Log.i(LOG, " already in cache " + key);
            }
        }
    }

    /**
     * function to clear the cache contents.
     */
    public void clearCache() {
        mBitmapCache.evictAll();
    }

}
