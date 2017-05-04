package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MagicSurface {

    private boolean mIsView = true;
    private View mView;
    private Bitmap mBmpBody;
    private Rect mViewRect;
    private boolean mIsPrepared = false;
    private boolean mIsPreparing = false;
    private boolean mReleased = false;
    private boolean mVisible = true;
    private boolean mEnableDepthTest = true;
    private boolean mEnableBlend = true;

    MagicScene mScene;
    private Vec mSize = new Vec(2);
    List<GLParameter<Texture>> mTextures = new ArrayList<>();
    GLParameter<Texture> mBody = new GLUniformParameter<Texture>().value(new Texture(null));
    private GLParameter<Float> mShininess = new GLUniformParameter<Float>().value(100.f);
    private GLParameter<Boolean> mIsCurrent = new GLUniformParameter<Boolean>().value(false);
    private SurfaceModel mModel;
    private boolean mDrawGrid = false;
    private int mRowLineCount = 30;
    private int mColLineCount = 30;

    private float[] mMatrix = new float[16];

    private MagicSurfaceMatrixUpdater mMagicMatrixUpdater;
    private MagicSurfaceModelUpdater mSurfaceUpdater;

    public MagicSurface(View view) {
        this(view, 64);
    }

    MagicSurface(View view, float shininess, Bitmap... textures) {
        mView = view;
        setShininess(shininess);
        initTextures(textures);
    }

    public MagicSurface(Bitmap body, Rect rect) {
        this(body, rect, 64);
    }

    MagicSurface(Bitmap body, Rect rect, Bitmap... textures) {
        this(body, rect, 64, textures);
    }

    MagicSurface(Bitmap body, Rect rect, float shininess, Bitmap... textures) {
        mIsView = false;
        mBmpBody = body;
        mViewRect = rect;
        setShininess(shininess);
        initTextures(textures);
    }

    void setProgram(Program program) {
        mModel.setProgram(program);
        mBody.setProgram(program);
        mShininess.setProgram(program);
        mIsCurrent.setProgram(program);
        for (GLParameter<Texture> t : mTextures) {
            t.setProgram(program);
        }
    }

    /**
     * 获取MagicSurface所在的场景
     * @return 场景
     */
    public MagicScene getScene() {
        return mScene;
    }

    /**
     * 获取MagicSurface的矩形网格模型
     * @return 模型
     */
    public SurfaceModel getModel() {
        return mModel;
    }

    /**
     * 设置模型更新器
     * @param updater 模型更新器
     * @return
     */
    public MagicSurface setModelUpdater(MagicSurfaceModelUpdater updater) {
        this.mSurfaceUpdater = updater;
        if (mSurfaceUpdater != null) {
            mSurfaceUpdater.mSurface = this;
        }
        return this;
    }

    /**
     * 设置矩阵更新器
     * @param updater 矩阵更新器
     * @return
     */
    public MagicSurface setMatrixUpdater(MagicSurfaceMatrixUpdater updater) {
        this.mMagicMatrixUpdater = updater;
        if (mMagicMatrixUpdater != null) {
            mMagicMatrixUpdater.mSurface = this;
        }
        return this;
    }

    /**
     * 设置模型光泽度
     * @param shininess 光泽度 (默认为64)
     * @return
     */
    public MagicSurface setShininess(float shininess) {
        mShininess.value(shininess);
        return this;
    }

    /**
     * 设置网格模型密度
     * @param rowLineCount 行数 (默认为30)
     * @param colLineCount 列数 (默认为30)
     * @return
     */
    public MagicSurface setGrid(int rowLineCount, int colLineCount) {
        mRowLineCount = rowLineCount;
        mColLineCount = colLineCount;
        if (mModel != null) {
            mModel.update(colLineCount, rowLineCount, mModel.getWidth(), mModel.getHeight());
        }
        return this;
    }

    /**
     * 绘制时是否只绘制网格
     * @param drawGrid 是否绘制网格 (默认为false)
     * @return
     */
    public MagicSurface drawGrid(boolean drawGrid) {
        mDrawGrid = drawGrid;
        if (mModel != null) {
            mModel.drawGrid(mDrawGrid);
        }
        return this;
    }

    /**
     * 设置是否绘制此MagicSurface
     * @param visible 是否绘制 (默认为true)
     * @return
     */
    public MagicSurface setVisible(boolean visible) {
        this.mVisible = visible;
        return this;
    }

    /**
     * 设置是否开启深度测试
     * 开启后会按三维坐标正常显示，如果关闭，绘制时将覆盖之前已经绘制的东西
     * @param enableDepthTest (默认为true)
     * @return
     */
    public MagicSurface setEnableDepthTest(boolean enableDepthTest) {
        this.mEnableDepthTest = enableDepthTest;
        return this;
    }

    /**
     * 是否开启混合，为透明对象时需开启.
     * @param enableBlend (默认为true)
     * @return
     */
    public MagicSurface setEnableBlend(boolean enableBlend) {
        this.mEnableBlend = enableBlend;
        return this;
    }

    /**
     * 获取当前是否绘制
     * @return 是否绘制
     */
    public boolean isVisible() {
        return mVisible;
    }

    boolean isPrepared() {
        return mIsPrepared;
    }

    void prepare() {
        if (mIsPrepared || mIsPreparing) {
            return;
        }
        mIsPreparing = true;
        if (mIsView) {
            mViewRect = ViewUtil.getViewRect(mView);
            mViewRect.left = mViewRect.left - mScene.getSceneViewRect().left;
            mViewRect.right = mViewRect.right - mScene.getSceneViewRect().left;
            mViewRect.bottom = mViewRect.bottom - mScene.getSceneViewRect().top;
            mViewRect.top = mViewRect.top - mScene.getSceneViewRect().top;
        }

        mBody.value().mBmp = mBmpBody;
        updateModel(mViewRect);

        if (!mIsView) {
            mIsPreparing = false;
            mIsPrepared = true;
        } else {
            Executors.newSingleThreadExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    mBmpBody = ViewUtil.getDrawingCache(mView);
                    mBody.value().mBmp = mBmpBody;
                    mIsPrepared = true;
                    mIsPreparing = false;
                }
            });
        }
    }

    void setIndex(int index, int textureIndex) {
        mIsCurrent.name(String.format("u_surface%d_", index));
        mShininess.name(String.format("u_s%d_shininess", index));
        mBody.name(String.format("u_s%d_t_body", index));
        mBody.value().mIndex = textureIndex;
        if (mTextures != null) {
            for (int i = 0; i < mTextures.size(); ++i) {
                mTextures.get(i).value().mIndex = textureIndex + i + 1;
                mTextures.get(i).name(String.format("u_s%d_t%d;", index, i));
            }
        }
    }

    void restore() {
        if (mSurfaceUpdater != null && mSurfaceUpdater.isStopped()) {
            mSurfaceUpdater.start();
        }
        if (mMagicMatrixUpdater != null && mMagicMatrixUpdater.isStopped()) {
            mMagicMatrixUpdater.start();
        }
        mBody.refresh();
        mIsCurrent.refresh();
        mShininess.refresh();
        if (mTextures != null) {
            for (GLParameter<Texture> t : mTextures) {
                t.refresh();
            }
        }
    }

    void stop() {
        if (mSurfaceUpdater != null) {
            mSurfaceUpdater.stop();
        }
        if (mMagicMatrixUpdater != null) {
            mMagicMatrixUpdater.stop();
        }
    }

    synchronized void release() {
        if (mReleased) {
            return;
        }
        if (mBody.value().mBmp != null) {
            mBody.value().mBmp.recycle();
        }
        for (GLParameter<Texture> t : mTextures) {
            if (t.value().mBmp != null) {
                t.value().mBmp.recycle();
            }
        }
        mReleased = true;
    }

    private boolean runOnDraw(MatrixManager matrixManager) {
        mBody.runOnDraw();
        mShininess.runOnDraw();
        if (mTextures != null) {
            for (GLParameter<Texture> t : mTextures) {
                t.runOnDraw();
            }
        }
        if (mSurfaceUpdater != null) {
            mSurfaceUpdater.runOnDraw();
        }
        if (mMagicMatrixUpdater != null) {
            mMagicMatrixUpdater.runOnDraw();
        }

        if (MagicUpdater.prepareUpdater(mSurfaceUpdater) && MagicUpdater.prepareUpdater(mMagicMatrixUpdater)) {
            if (mSurfaceUpdater == null && mMagicMatrixUpdater == null) {
                MatrixManager.reset(mMatrix);
                ReusableVec offset = VecPool.get(3);
                mModel.getOffset(offset);
                MatrixManager.translateM(mMatrix, offset.x(), offset.y(), offset.z());
                offset.free();
                matrixManager.setModelMatrix(mMatrix);
            } else if (mMagicMatrixUpdater != null) {
                try {
                    mMagicMatrixUpdater.lock();
                    matrixManager.setModelMatrix(getModel().getMatrix());
                } finally {
                    mMagicMatrixUpdater.unlock();
                }
            } else {
                MatrixManager.reset(mMatrix);
                matrixManager.setModelMatrix(mMatrix);
            }
            mModel.runOnDraw();
            return true;
        }
        return false;
    }

    void draw(MatrixManager matrixManager) {
        if (runOnDraw(matrixManager)) {
            if (isVisible()) {
                mIsCurrent.value(true);
                GLES20.glDisable(GLES20.GL_CULL_FACE);
                GLES20.glFrontFace(GLES20.GL_CCW);
                if (mEnableDepthTest) {
                    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                    GLES20.glDepthFunc(GLES20.GL_LEQUAL);
                    GLES20.glDepthMask(true);
                } else {
                    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                }
                if (mEnableBlend) {
                    GLES20.glEnable(GLES20.GL_BLEND);
                    GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                } else {
                    GLES20.glDisable(GLES20.GL_BLEND);
                }
                mModel.draw();
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glDisable(GLES20.GL_BLEND);
                mIsCurrent.value(false);
            }
            MagicUpdater.doStartedAndStopped(mSurfaceUpdater);
            MagicUpdater.doStartedAndStopped(mMagicMatrixUpdater);
        }
    }

    private void updateModel(Rect rect) {
        Rect sceneRect = mScene.getSceneViewRect();
        float sceneWidth = sceneRect.right - sceneRect.left;
        float sceneHeight = sceneRect.bottom - sceneRect.top;
        boolean isPortrait = true;
        float ratio = sceneWidth / sceneHeight;
        if (ratio > 1) {
            ratio = sceneHeight / sceneWidth;
            isPortrait = false;
        }
        float w = isPortrait ? ratio : 1;
        float unit = w / sceneWidth;

        mSize.setSize(rect.width() * unit, rect.height() * unit);
        mModel = new SurfaceModel(mColLineCount, mRowLineCount, mSize.width(), mSize.height());
        mModel.drawGrid(mDrawGrid);
        mModel.setOffset(
                (rect.centerX() - (sceneRect.centerX() - sceneRect.left)) * unit,
                -(rect.centerY() - (sceneRect.centerY() - sceneRect.top)) * unit,
                0);
    }

    private void initTextures(Bitmap... bitmaps) {
        if (bitmaps == null) {
            return;
        }
        for (Bitmap b : bitmaps) {
            mTextures.add(new GLUniformParameter<Texture>().value(new Texture(b)));
        }
    }

}