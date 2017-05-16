package com.gplibs.magicsurfaceview;

public abstract class MagicMultiSurfaceUpdater extends MagicUpdater {

    MagicMultiSurface mSurface;
    private SimpleLock mLock = new SimpleLock();

    @Override
    void willStart() {
        willStart(mSurface);
    }

    @Override
    void didStart() {
        didStart(mSurface);
    }

    @Override
    void didStop() {
        didStop(mSurface);
    }

    @Override
    void update() {
        updateBegin(mSurface);
        mLock.lock();
        try {
            for (int r = 0; r < mSurface.getRows(); ++r) {
                for (int c = 0; c < mSurface.getCols(); ++c) {
                    ReusableVec color = VecPool.get(4);
                    ReusableVec offset = VecPool.get(3);
                    SurfaceModel m = mSurface.getModel(r, c);
                    m.getOffset(offset);
                    color.setRGBA(1, 1, 1, 1);

                    update(mSurface, r, c, m.getMatrix(), offset, color);

                    for (int mr = 0; mr < m.getRowLineCount(); ++mr) {
                        for (int mc = 0; mc < m.getColLineCount(); ++mc) {
                            m.setColor(mr, mc, color);
                        }
                    }
                    color.free();
                    offset.free();
                }
            }
        } finally {
            mLock.unlock();
        }
        updateEnd(mSurface);
    }

    void lock() {
        mLock.lock();
    }

    void unlock() {
        mLock.unlock();
    }


    protected void reset(float[] matrix) {
        MatrixManager.reset(matrix);
    }

    protected void rotate(float[] matrix, Vec vec, float angle) {
        MatrixManager.rotateM(matrix, vec.x(), vec.y(), vec.z(), angle);
    }

    protected void translate(float[] matrix, Vec vec) {
        MatrixManager.translateM(matrix, vec.x(), vec.y(), vec.z());
    }

    protected void scale(float[] matrix, Vec vec) {
        MatrixManager.scaleM(matrix, vec.x(), vec.y(), vec.z());
    }


    protected abstract void willStart(MagicMultiSurface surface);

    protected abstract void didStart(MagicMultiSurface surface);

    protected abstract void didStop(MagicMultiSurface surface);

    protected abstract void updateBegin(MagicMultiSurface surface);

    protected abstract void update(MagicMultiSurface surface, int r, int c, float[] matrix, Vec offset, Vec color);

    protected abstract void updateEnd(MagicMultiSurface surface);
}
