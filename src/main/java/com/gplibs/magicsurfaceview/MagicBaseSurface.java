package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.opengl.GLES20;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public abstract class MagicBaseSurface<T> {
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
    private Vec mOffset = new Vec(3);
    private Vec mSize = new Vec(2);
    List<GLParameter<Texture>> mTextures = new ArrayList<>();
    GLParameter<Texture> mBody = new GLUniformParameter<Texture>().value(new Texture(null));
    private GLParameter<Float> mShininess = new GLUniformParameter<Float>().value(100.f);
    private GLParameter<Boolean> mIsCurrent = new GLUniformParameter<Boolean>().value(false);

    public MagicBaseSurface(View view) {
        this(view, 64);
    }

    MagicBaseSurface(View view, float shininess, Bitmap... textures) {
        mView = view;
        setShininess(shininess);
        initTextures(textures);
    }

    public MagicBaseSurface(Bitmap body, Rect rect) {
        this(body, rect, 64);
    }

    MagicBaseSurface(Bitmap body, Rect rect, Bitmap... textures) {
        this(body, rect, 64, textures);
    }

    MagicBaseSurface(Bitmap body, Rect rect, float shininess, Bitmap... textures) {
        mIsView = false;
        mBmpBody = body;
        mViewRect = rect;
        setShininess(shininess);
        initTextures(textures);
    }

    void setProgram(Program program) {
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
     * 设置模型光泽度
     * @param shininess 光泽度 (默认为64)
     * @return Surface
     */
    public T setShininess(float shininess) {
        mShininess.value(shininess);
        return (T) this;
    }

    /**
     * 设置是否绘制此MagicSurface
     * @param visible 是否绘制 (默认为true)
     * @return Surface
     */
    public T setVisible(boolean visible) {
        this.mVisible = visible;
        return (T) this;
    }

    /**
     * 设置是否开启深度测试
     * 开启后会按三维坐标正常显示，如果关闭，绘制时将覆盖之前已经绘制的东西
     * @param enableDepthTest (默认为true)
     * @return Surface
     */
    public T setEnableDepthTest(boolean enableDepthTest) {
        this.mEnableDepthTest = enableDepthTest;
        return (T) this;
    }

    /**
     * 是否开启混合，为透明对象时需开启.
     * @param enableBlend (默认为true)
     * @return Surface
     */
    public T setEnableBlend(boolean enableBlend) {
        this.mEnableBlend = enableBlend;
        return (T) this;
    }

    /**
     * 获取当前是否绘制
     * @return 是否绘制
     */
    public boolean isVisible() {
        return mVisible;
    }

    /**
     * 获取Surface宽度
     * @return 宽度
     */
    public float getWidth() {
        return mSize.width();
    }

    /**
     * 获取Surface高度
     * @return 高度
     */
    public float getHeight() {
        return mSize.height();
    }

    /**
     * 获取Surface在场景中对应点的坐标 (包含偏移量)
     * 如获取Surface左上角坐标 调用方法为 getPosition(0.0f, 0.0f, outPos)
     * @param ratioX 0 表示Surface的最左边 1表示最右边
     * @param ratioY 0 表示Surface的最上边 1表示最下边
     * @param outPos 存储取到的坐标值
     */
    public void getPosition(float ratioX, float ratioY, Vec outPos) {
        outPos.setXYZ(
                ratioX * mSize.width() - mSize.width() / 2 + mOffset.x(),
                -ratioY * mSize.height() + mSize.height() / 2 + mOffset.y(),
                0
        );
    }

    /**
     * 获取Surface在场景中对应点的坐标 (不包含偏移量)
     * 如获取Surface左上角坐标 调用方法为 getPosition(0.0f, 0.0f, outPos)
     * @param ratioX 0 表示Surface的最左边 1表示最右边
     * @param ratioY 0 表示Surface的最上边 1表示最下边
     * @param outPos 存储取到的坐标值
     */
    public void getPositionExcludeOffset(float ratioX, float ratioY, Vec outPos) {
        outPos.setXYZ(
                ratioX * mSize.width() - mSize.width() / 2,
                -ratioY * mSize.height() + mSize.height() / 2,
                0
        );
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
        mBody.refresh();
        mIsCurrent.refresh();
        mShininess.refresh();
        if (mTextures != null) {
            for (GLParameter<Texture> t : mTextures) {
                t.refresh();
            }
        }
    }

    abstract void stop();

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

    private boolean checkRunOnDraw(MatrixManager matrixManager) {
        mBody.runOnDraw();
        mShininess.runOnDraw();
        if (mTextures != null) {
            for (GLParameter<Texture> t : mTextures) {
                t.runOnDraw();
            }
        }
        return runOnDraw(matrixManager);
    }

    protected abstract boolean runOnDraw(MatrixManager matrixManager);

    void draw(MatrixManager matrixManager) {
        if (checkRunOnDraw(matrixManager)) {
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
                drawModel(matrixManager);
                GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                GLES20.glDisable(GLES20.GL_BLEND);
                mIsCurrent.value(false);
            }
            doUpdaterStartedAndStopped();
        }
    }

    protected abstract void drawModel(MatrixManager matrixManager);

    protected abstract void doUpdaterStartedAndStopped();

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

        mOffset.setXYZ((rect.centerX() - (sceneRect.centerX() - sceneRect.left)) * unit,
                -(rect.centerY() - (sceneRect.centerY() - sceneRect.top)) * unit,
                0
        );
        updateModel(mSize, mOffset);
    }

    protected abstract void updateModel(Vec size, Vec offset);

    private void initTextures(Bitmap... bitmaps) {
        if (bitmaps == null) {
            return;
        }
        for (Bitmap b : bitmaps) {
            mTextures.add(new GLUniformParameter<Texture>().value(new Texture(b)));
        }
    }
}
