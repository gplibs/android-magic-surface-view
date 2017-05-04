package com.gplibs.magicsurfaceview;

abstract class MagicMatrixUpdater extends MagicUpdater {

    private SimpleLock mLock = new SimpleLock();
    private float[] mMatrix = new float[16];

    MagicMatrixUpdater() {
        super();
    }

    MagicMatrixUpdater(int group) {
        super(group);
        MatrixManager.reset(mMatrix);
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

    abstract void updateMatrix(float[] matrix);

    abstract float[] getMatrix();

    @Override
    void update() {
        updateMatrix(mMatrix);
        try {
            mLock.lock();
            System.arraycopy(mMatrix, 0, getMatrix(), 0, mMatrix.length);
        } finally {
            mLock.unlock();
        }
    }

    void lock() {
        mLock.lock();
    }

    void unlock() {
        mLock.unlock();
    }
}
