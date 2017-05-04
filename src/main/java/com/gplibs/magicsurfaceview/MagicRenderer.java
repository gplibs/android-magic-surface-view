package com.gplibs.magicsurfaceview;

import android.opengl.GLSurfaceView;

import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MagicRenderer implements GLSurfaceView.Renderer {

    private MagicScene mScene;
    private int mPause = 0;
    private boolean mDestroy = false;
    private MagicRendererListener mListener;

    MagicRenderer() {
    }

    void setListener(MagicRendererListener listener) {
        this.mListener = listener;
    }

    void pause() {
        mPause = 1;
    }

    void render(final MagicScene scene) {
        if (mScene != null) {
            mScene.release();
        }
        mScene = scene;
        mPause = 0;
    }

    MagicScene getScene() {
        return mScene;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mPause = 0;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mScene != null && mScene.isReady()) {
            mScene.updateFrustum();
            if (mDestroy) {
                mDestroy = false;
                mScene.restore();
            }
        }
    }

    private void runOnDraw() {
        if (mScene != null) {
            mScene.runOnDraw();
        }
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        try {
            MagicSurfaceViewLock.lock();
            if (checkPaused()) {
                return;
            }
            runOnDraw();
            if (mScene != null && mScene.isReady()) {
                mScene.draw();
            }
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            MagicSurfaceViewLock.unlock();
        }
    }

    private boolean checkPaused() {
        if (mPause == 1) {
            mPause++;
            if (mScene != null && mScene.isReady()) {
                mScene.clearGLResource();
                mScene = null;
            }
            mDestroy = true;
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    mListener.onGLResourceReleased();
                }
            });
        }
        return mPause > 0;
    }

    void onDestroy() {
        mDestroy = true;
        if (mScene != null) {
            mScene.stop();
        }
    }

    interface MagicRendererListener {
        void onGLResourceReleased();
    }

}
