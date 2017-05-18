package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

class Program {

    int handle;
    private long mGLThreadId;
    private boolean mDeleted = false;

    Program(String vShader, String fShader) {
        mGLThreadId = Thread.currentThread().getId();
        handle = GLUtil.createProgram(vShader, fShader);
    }

    void use() {
        GLES20.glUseProgram(handle);
        GLUtil.checkGlError("glUseProgram");
    }

    void delete() {
        if (isGLThread()) {
            GLES20.glDeleteProgram(handle);
            GLUtil.checkGlError("glDeleteProgram");
        }
        mDeleted = true;
    }

    boolean isDeleted() {
        return mDeleted;
    }

    boolean isGLThread() {
        return Thread.currentThread().getId() == mGLThreadId;
    }

}
