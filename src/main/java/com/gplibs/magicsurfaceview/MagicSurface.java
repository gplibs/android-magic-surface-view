package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

public class MagicSurface extends MagicBaseSurface<MagicSurface> {


    private SurfaceModel mModel;
    private boolean mDrawGrid = false;
    private int mRowLineCount = 30;
    private int mColLineCount = 30;
    private float[] mMatrix = new float[16];
    private MagicSurfaceMatrixUpdater mMatrixUpdater;
    private MagicSurfaceModelUpdater mModelUpdater;

    /**
     * 构造函数
     * @param view 需要进行动画的View
     */
    public MagicSurface(View view) {
        super(view);
    }

    /**
     * 构造函数
     * @param body 需要进行动画的Bitmap
     * @param rect 相对于MagicSurfaceView的Rect
     */
    public MagicSurface(Bitmap body, Rect rect) {
        super(body, rect);
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
        this.mModelUpdater = updater;
        if (mModelUpdater != null) {
            mModelUpdater.mSurface = this;
        }
        return this;
    }

    /**
     * 设置矩阵更新器
     * @param updater 矩阵更新器
     * @return
     */
    public MagicSurface setMatrixUpdater(MagicSurfaceMatrixUpdater updater) {
        this.mMatrixUpdater = updater;
        if (mMatrixUpdater != null) {
            mMatrixUpdater.mSurface = this;
        }
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

    @Override
    void setProgram(Program program) {
        mModel.setProgram(program);
        super.setProgram(program);
    }

    @Override
    protected void updateModel(Vec size, Vec offset) {
        mModel = new SurfaceModel(mColLineCount, mRowLineCount, size.width(), size.height());
        mModel.drawGrid(mDrawGrid);
        mModel.setOffset(offset.x(), offset.y(), offset.z());
    }

    @Override
    void restore() {
        if (mModelUpdater != null && mModelUpdater.isStopped()) {
            mModelUpdater.start();
        }
        if (mMatrixUpdater != null && mMatrixUpdater.isStopped()) {
            mMatrixUpdater.start();
        }
        super.restore();
    }

    @Override
    synchronized void release() {
        super.release();
        mModel = null;
    }

    @Override
    void stop() {
        if (mModelUpdater != null) {
            mModelUpdater.stop();
        }
        if (mMatrixUpdater != null) {
            mMatrixUpdater.stop();
        }
    }

    @Override
    protected boolean runOnDraw(MatrixManager matrixManager) {
        if (mModelUpdater != null) {
            mModelUpdater.runOnDraw();
        }
        if (mMatrixUpdater != null) {
            mMatrixUpdater.runOnDraw();
        }
        if (MagicUpdater.prepareUpdater(mModelUpdater) && MagicUpdater.prepareUpdater(mMatrixUpdater)) {
            if (mModelUpdater == null && mMatrixUpdater == null) {
                MatrixManager.reset(mMatrix);
                ReusableVec offset = VecPool.get(3);
                mModel.getOffset(offset);
                MatrixManager.translateM(mMatrix, offset.x(), offset.y(), offset.z());
                offset.free();
                matrixManager.setModelMatrix(mMatrix);
            } else if (mMatrixUpdater != null) {
                try {
                    mMatrixUpdater.lock();
                    matrixManager.setModelMatrix(getModel().getMatrix());
                } finally {
                    mMatrixUpdater.unlock();
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

    @Override
    protected void drawModel(MatrixManager matrixManager) {
        mModel.draw();
    }

    @Override
    protected void doUpdaterStartedAndStopped() {
        MagicUpdater.doStartedAndStopped(mModelUpdater);
        MagicUpdater.doStartedAndStopped(mMatrixUpdater);
    }

}