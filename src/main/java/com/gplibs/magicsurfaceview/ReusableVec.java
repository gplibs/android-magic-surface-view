package com.gplibs.magicsurfaceview;

/**
 * 可重用Vec
 */
public class ReusableVec extends Vec {

    private boolean mIsFree = true;

    ReusableVec(int length) {
        super(length);
    }

    ReusableVec(float... data) {
        super(data);
    }

    boolean isFree() {
        return mIsFree;
    }

    void use() {
        mIsFree = false;
    }

    /**
     * 使用完后记得 free()
     */
    public void free() {
        mIsFree = true;
    }
}
