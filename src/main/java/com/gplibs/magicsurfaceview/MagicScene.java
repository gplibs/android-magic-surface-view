package com.gplibs.magicsurfaceview;

import android.graphics.Rect;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.util.ArrayList;
import java.util.List;

public class MagicScene extends RunOnDraw {

    private final float NEAR = 0.1f;

    MagicSurfaceView mSurfaceView;

    private Rect mSceneViewRect;
    private Vec mSceneSize = new Vec(2);
    private boolean mIsPrepared = false;
    private boolean mIsPreparing = false;
    private boolean mInited = false;

    private Program mProgram;
    private String mVertexShader;
    private String mFragmentShader;
    private int[] mTextureIds;

    private MatrixManager mMatrixManager = new MatrixManager();
    private float mCameraZ = 5;
    private Vec mCameraPosition = new Vec(0, 0, mCameraZ);
    private Vec mCameraCenter = new Vec(0, 0, 0);
    private Vec mCameraUp = new Vec(0, 1, 0);

    List<MagicBaseSurface> mSurfaces = new ArrayList<>();
    List<Light> mLights = new ArrayList<>();
    GLParameter<Vec> mAmbientColor = new GLUniformParameter<Vec>("u_ambient_color").value(new Vec(1.f, 1.f, 1.f, 1.f));
    private MagicSceneUpdater mUpdater;

    MagicScene() {
    }

    /**
     * 获取MagicSurfaceView在场景中宽度
     * @return Surface
     */
    public float getWidth() {
        return mSceneSize.width();
    }

    /**
     * 获取MagicSurfaceView在场景中高度
     * @return Surface
     */
    public float getHeight() {
        return mSceneSize.height();
    }

    /**
     * 获取 某光源
     * @param index 光源索引
     * @param <T> LightType
     * @return Surface
     */
    public <T extends Light> T getLight(int index) {
        if (index < 0 || index > mLights.size() - 1) {
            return null;
        }
        return (T) mLights.get(index);
    }

    /**
     * * 获取某 场景中某 MagicSurface 对象
     * @param index 索引
     * @param <T> Surface Type
     * @return Surface
     */
    public <T extends MagicBaseSurface> T getSurface(int index) {
        if (index < 0 || index > mSurfaces.size() - 1) {
            return null;
        }
        return (T) mSurfaces.get(index);
    }

    /**
     * 获取MagicSurfaceView在场景中对应点的坐标
     * 如获取左上角坐标 调用方法为 getPosition(0.0f, 0.0f, out)
     * @param ratioX 0 表示MagicSurfaceView的最左边 1表示最右边
     * @param ratioY 0 表示MagicSurfaceView的最上边 1表示最下边
     * @param out 存储取到的坐标值
     */
    public void getPosition(float ratioX, float ratioY, Vec out) {
        out.setXYZ(
                -mSceneSize.width() / 2 + mSceneSize.width() * ratioX,
                mSceneSize.height() / 2 - mSceneSize.height() * ratioY,
                0
        );
    }

    /**
     * 获取摄像机坐标
     * @param outPos 存储取到的摄像机坐标
     */
    public void getCameraPos(Vec outPos) {
        outPos.copy(mCameraPosition);
    }

    /**
     * 设置 摄像机z轴位置
     * @param cameraZ z轴坐标 (默认值为5 值越小,透视效果越明显，不能小于0.1)
     */
    public void setCameraZ(float cameraZ) {
        if (cameraZ < NEAR) {
            throw new IllegalArgumentException("cameraZ required greater than 0.1");
        }
        mCameraZ = cameraZ;
        if (mCameraPosition.y() != cameraZ && mCameraPosition.x() == 0 && mCameraPosition.y() == 0) {
            mCameraPosition.z(cameraZ);
            updateCamera();
        }
        updateFrustum();
    }

    void setAmbientColor(int color) {
        mAmbientColor.value().setColor(color);
        mAmbientColor.refresh();
    }

    void setUpdater(MagicSceneUpdater updater) {
        mUpdater = updater;
        mUpdater.mScene = this;
    }

    void setLookAtM(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        mCameraPosition.setXYZ(eyeX, eyeY, eyeZ);
        mCameraCenter.setXYZ(centerX, centerY, centerZ);
        mCameraUp.setXYZ(upX, upY, upZ);
        updateCamera();
    }

    boolean isReady() {
        return isPrepared() && mInited;
    }

    private boolean prepare() {
        if (isPrepared()) {
            return true;
        }
        if (mIsPreparing) {
            return false;
        }
        mIsPreparing = true;
        try {
            for (MagicBaseSurface<?> s : mSurfaces) {
                s.prepare();
            }
            updateCamera();
            updateFrustum();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isPrepared();
    }

    private boolean isPrepared() {
        if (!mIsPrepared) {
            boolean prepared = true;
            for (MagicBaseSurface<?> s : mSurfaces) {
                if (!s.isPrepared()) {
                    prepared = false;
                    break;
                }
            }
            mIsPrepared = prepared;
        }
        if (mIsPrepared) {
            mIsPreparing = false;
        }
        return mIsPrepared;
    }

    void init(String vertexShader, String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        mInited = false;
        addRunOnDraw(new Runnable() {
            @Override
            public void run() {
                prepare();
            }
        });
    }

    private void init() {
        if (!mInited && isPrepared()) {
            mProgram = new Program(mVertexShader, mFragmentShader);
            mProgram.use();
            mAmbientColor.setProgram(mProgram);
            mMatrixManager.setProgram(mProgram);

            mTextureIds = new int[getTextureCount()];
            GLES20.glGenTextures(mTextureIds.length, mTextureIds, 0);
            int textureIndex = GLES20.GL_TEXTURE0;
            for (int i = 0; i < mSurfaces.size(); ++i) {
                MagicBaseSurface<?> s = mSurfaces.get(i);
                s.setIndex(i, textureIndex);
                s.setProgram(mProgram);

                Texture texture = s.mBody.value();
                int id = mTextureIds[textureIndex - GLES20.GL_TEXTURE0];
                initTexture(texture, id);
                textureIndex++;

                for (GLParameter<Texture> p : s.mTextures) {
                    texture = p.value();
                    id = mTextureIds[textureIndex - GLES20.GL_TEXTURE0];
                    initTexture(texture, id);
                    textureIndex++;
                }
            }

            for (int i = 0; i < mLights.size(); ++i) {
                mLights.get(i).setProgram(mProgram);
                mLights.get(i).setIndex(i);
            }
            mInited = true;
        }
    }

    @Override
    protected void runOnDraw() {
        super.runOnDraw();
        init();
        if (isReady()) {
            if (mUpdater != null) {
                mUpdater.runOnDraw();
            }
            MagicUpdater.prepareUpdater(mUpdater);
            mMatrixManager.runOnDraw();
            mAmbientColor.runOnDraw();
            if (mLights != null) {
                for (Light l : mLights) {
                    l.runOnDraw();
                }
            }
        }
    }

    void restore() {
        init(mVertexShader, mFragmentShader);
        init();
        mAmbientColor.refresh();
        if (mLights != null) {
            for (Light l : mLights) {
                l.restore();
            }
        }
        if (mSurfaces != null) {
            for (MagicBaseSurface<?> s : mSurfaces) {
                s.restore();
            }
        }
    }

    void stop() {
        if (mSurfaces != null) {
            for (MagicBaseSurface<?> s : mSurfaces) {
                s.stop();
            }
        }
        if (mUpdater != null) {
            mUpdater.stop();
        }
        mProgram.delete();
    }

    void release() {
        stop();
        for (MagicBaseSurface<?> s : mSurfaces) {
            s.release();
        }
    }

    Rect getSceneViewRect() {
        if (mSceneViewRect == null) {
            mSceneViewRect = ViewUtil.getViewRect(mSurfaceView);
        }
        return mSceneViewRect;
    }

    void updateFrustum() {
        Rect r = getSceneViewRect();
        float w = r.right - r.left;
        float h = r.bottom - r.top;
        GLES20.glViewport(0, 0, (int) w, (int) h);
        GLUtil.checkGlError("glViewport");
        float ratio = w / h;
        if (ratio > 1) {
            w = 1;
            h = 1 / ratio;
        } else {
            w = ratio;
            h = 1;
        }
        float nh = NEAR * h / mCameraZ;
        float nw = w * nh / h;
        mSceneSize.setSize(w, h);
        mMatrixManager.frustumM(nw, nh, NEAR, 10);
    }

    void draw() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        if (mSurfaces != null) {
            for (MagicBaseSurface<?> s : mSurfaces) {
                s.draw(mMatrixManager);
            }
        }
        MagicUpdater.doStartedAndStopped(mUpdater);
    }

    private void initTexture(Texture texture, int id) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, texture.mBmp, 0);
        texture.mId = id;
        GLUtil.initTexParams();
    }

    private int getTextureCount() {
        int c = 0;
        for (MagicBaseSurface<?> s : mSurfaces) {
            c += (1 + s.mTextures.size());
        }
        return c;
    }

    private void updateCamera() {
        mMatrixManager.setLookAtM(
                mCameraPosition.x(), mCameraPosition.y(), mCameraPosition.z(),
                mCameraCenter.x(), mCameraCenter.y(), mCameraCenter.z(),
                mCameraUp.x(), mCameraUp.y(), mCameraUp.z());
    }

}
