package com.gplibs.magicsurfaceview;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.View;

public class MagicMultiSurface extends MagicBaseSurface<MagicMultiSurface> {

    private MagicMultiSurfaceUpdater mUpdater;
    private int mRows;
    private int mCols;
    private SurfaceModel mDrawModel;
    private SurfaceModel[] mModels;
    private float[][] mMatrix;

    /**
     * 构造函数
     * @param view 需要进行动画的View
     * @param rows 行数 (纵向Surface数量)
     * @param cols 列数 (横向Surface数量)
     */
    public MagicMultiSurface(View view, int rows, int cols) {
        super(view);
        init(rows, cols);
    }

    /**
     * 构造函数
     * @param body 需要进行动画的Bitmap
     * @param rect 相对于MagicSurfaceView的Rect
     * @param rows 行数 (纵向Surface数量)
     * @param cols 列数 (横向Surface数量)
     */
    public MagicMultiSurface(Bitmap body, Rect rect, int rows, int cols) {
        super(body, rect);
        init(rows, cols);
    }

    private void init(int rows, int cols) {
        if (rows < 1 || cols < 1) {
            throw new IllegalArgumentException();
        }
        mRows = rows;
        mCols = cols;
        mModels = new SurfaceModel[mRows * mCols];
    }

    public MagicMultiSurface setUpdater(MagicMultiSurfaceUpdater updater) {
        updater.mSurface = this;
        this.mUpdater = updater;
        return this;
    }

    /**
     * 行数 (纵向Surface数量)
     * @return 行数
     */
    public int getRows() {
        return mRows;
    }

    /**
     * 列数 (横向Surface数量)
     * @return 列数
     */
    public int getCols() {
        return mCols;
    }

    SurfaceModel getModel(int r, int c) {
        return mModels[r * mCols + c];
    }

    @Override
    void setProgram(Program program) {
        mDrawModel.setProgram(program);
        for (SurfaceModel m : mModels) {
            m.setProgram(program);
        }
        super.setProgram(program);
    }

    @Override
    protected void updateModel(Vec size, Vec offset) {
        float w = size.width() / mCols;
        float h = size.height() / mRows;
        float halfW = size.width() / 2;
        float halfH = size.height() / 2;
        mDrawModel = new SurfaceModel(2, 2, w, h);
        for (int r = 0; r < mRows; ++r) {
            for (int c = 0; c < mCols; ++c) {
                ReusableVec o = VecPool.get(3);
                float x = (c + 0.5f) * w - halfW;
                float y = -(r + 0.5f) * h + halfH;
                o.setXYZ(offset.x() + x, offset.y() + y, offset.z());
                SurfaceModel model = new SurfaceModel(2, 2, w, h, o, size);
                o.free();
                mModels[r * mCols + c] = model;
            }
        }
    }

    @Override
    void stop() {
        if (mUpdater != null) {
            mUpdater.stop();
        }
    }

    @Override
    void restore() {
        if (mUpdater != null && mUpdater.isStopped()) {
            mUpdater.start();
        }
        super.restore();
    }

    @Override
    synchronized void release() {
        super.release();
        mModels = null;
        mDrawModel = null;
    }

    @Override
    protected boolean runOnDraw(MatrixManager matrixManager) {
        if (mUpdater != null) {
            mUpdater.runOnDraw();
        }
        if (MagicUpdater.prepareUpdater(mUpdater)) {
            mDrawModel.runOnDraw();
            return true;
        }
        return false;
    }

    @Override
    protected void drawModel(MatrixManager matrixManager) {
        if (mUpdater != null) {
            mUpdater.lock();
        }
        try {
            for (SurfaceModel m : mModels) {
                matrixManager.setModelMatrix(m.getMatrix());
                m.prepareColors();
                m.prepareTexCoord();
                mDrawModel.mColorsBuffer = m.mColorsBuffer;
                mDrawModel.mTexCoordBuffer = m.mTexCoordBuffer;
                mDrawModel.draw();
            }
        } finally {
            if (mUpdater != null) {
                mUpdater.unlock();
            }
        }
    }


    @Override
    protected void doUpdaterStartedAndStopped() {
        MagicUpdater.doStartedAndStopped(mUpdater);
    }
}
