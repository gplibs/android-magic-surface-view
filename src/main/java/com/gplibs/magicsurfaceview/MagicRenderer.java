package com.gplibs.magicsurfaceview;

import android.opengl.GLSurfaceView;

import java.util.concurrent.Executors;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

class MagicRenderer implements GLSurfaceView.Renderer {

    private MagicScene mScene;
    private boolean mOnDestroyed = false;
    private boolean mReleased = false;
    private boolean mNeedRestore = false;

    MagicRenderer() {
    }

    void render(final MagicScene scene) {
        if (mScene != null) {
            mScene.release();
        }
        mScene = scene;
    }

    MagicScene getScene() {
        return mScene;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mReleased = false;
        mOnDestroyed = false;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        if (mScene != null && mScene.isReady()) {
            mScene.updateFrustum();
            if (mNeedRestore) {
                mNeedRestore = false;
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

    void onDestroy() {
        mOnDestroyed = true;
        if (mScene != null) {
            mScene.stop();
        }
        if (mReleased) {
            mNeedRestore = false;
            releaseScene();
        } else {
            mNeedRestore = true;
        }
    }

    void release() {
        mReleased = true;
        mNeedRestore = false;
        if (mOnDestroyed) {
            releaseScene();
        }
    }

    private void releaseScene() {
        if (mScene != null) {
            mScene.release();
            mScene = null;
        }
    }

}
