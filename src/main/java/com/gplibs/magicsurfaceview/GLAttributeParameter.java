package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

import java.nio.FloatBuffer;
import java.util.concurrent.locks.ReentrantLock;

class GLAttributeParameter extends GLParameter<FloatBuffer> {

    static final int FLOAT_SIZE_BYTES = 4;

    private int mHandle;
    private int mSize;
    private ReentrantLock mBufferLock;

    GLAttributeParameter(int size, ReentrantLock lock) {
        mSize = size;
        mBufferLock = lock;
    }

    GLAttributeParameter(String name, int size, ReentrantLock lock) {
        this(size, lock);
        mName = name;
    }

    @Override
    protected int handle() {
        if (mHandle == 0) {
            mHandle = GLES20.glGetAttribLocation(mProgram.handle, mName);
        }
        return mHandle;
    }

    @Override
    protected void runOnDraw() {
        updateValue();
    }

    @Override
    protected void updateValue() {
        if (mValue == null) {
            return;
        }
        try {
            mBufferLock.lock();
            if (handle() >= 0) {
                GLES20.glVertexAttribPointer(
                        handle(),
                        mSize,
                        GLES20.GL_FLOAT,
                        false,
                        mSize * FLOAT_SIZE_BYTES,
                        mValue);
                GLES20.glEnableVertexAttribArray(handle());
            }
        } finally {
            mBufferLock.unlock();
        }
    }
}
