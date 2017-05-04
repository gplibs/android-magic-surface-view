package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

class Program {

    int handle;
    private long mGLThreadId;

    Program(String vShader, String fShader) {
        mGLThreadId = Thread.currentThread().getId();
        handle = GLUtil.createProgram(vShader, fShader);
    }

    void use() {
        GLES20.glUseProgram(handle);
        GLUtil.checkGlError("glUseProgram");
    }

    void delete() {
        GLES20.glDeleteProgram(handle);
        GLUtil.checkGlError("glDeleteProgram");
    }

    boolean isGLThread() {
        return Thread.currentThread().getId() == mGLThreadId;
    }

}
