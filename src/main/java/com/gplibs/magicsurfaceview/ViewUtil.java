package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

class ViewUtil {

    static Bitmap getDrawingCache(final View v) {
        final WaitNotify waitNotify = new WaitNotify();
        final BitmapHolder bmpHolder = new BitmapHolder();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                try {
                    v.clearFocus();
                    v.setPressed(false);

                    boolean willNotCache = v.willNotCacheDrawing();
                    v.setWillNotCacheDrawing(false);

                    int color = v.getDrawingCacheBackgroundColor();
                    v.setDrawingCacheBackgroundColor(0);

                    if (color != 0) {
                        v.destroyDrawingCache();
                    }
                    v.buildDrawingCache();
                    Bitmap cacheBitmap = v.getDrawingCache();
                    if (cacheBitmap == null) {
                        bmpHolder.bmp = null;
                    }

                    bmpHolder.bmp = Bitmap.createBitmap(cacheBitmap);

                    v.destroyDrawingCache();
                    v.setWillNotCacheDrawing(willNotCache);
                    v.setDrawingCacheBackgroundColor(color);
                } finally {
                    waitNotify.doNotify();
                }
            }
        });
        waitNotify.doWait();
        return bmpHolder.bmp;
    }

    static Rect getViewRect(View view) {
        Point p = new Point(0, 0);
        getViewPosition(view, p);
        return new Rect(p.x, p.y, p.x + view.getWidth(), p.y + view.getHeight());
    }

    private static void getViewPosition(View view, Point p) {
        p.x += view.getLeft();
        p.y += view.getTop();
        if (view.getParent() != null && view.getParent() instanceof View) {
            getViewPosition((View) view.getParent(), p);
        }
    }

    private static class BitmapHolder {
        Bitmap bmp;
    }
}
