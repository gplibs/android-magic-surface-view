package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;

class Texture {

    int mId;
    int mIndex;
    Bitmap mBmp;

    Texture(Bitmap bmp) {
        this.mBmp = bmp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Texture)) {
            return false;
        }
        Texture t = (Texture) obj;
        if (mBmp == null) {
            if (t.mBmp != null) {
                return false;
            }
        } else {
            if (!mBmp.equals(t.mBmp)) {
                return false;
            }
        }
        return mId == t.mId && mIndex == t.mIndex;
    }
}
