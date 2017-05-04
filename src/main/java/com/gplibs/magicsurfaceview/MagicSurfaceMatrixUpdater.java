package com.gplibs.magicsurfaceview;

public abstract class MagicSurfaceMatrixUpdater extends MagicMatrixUpdater {

    MagicSurface mSurface;

    public MagicSurfaceMatrixUpdater() {
    }

    public MagicSurfaceMatrixUpdater(int group) {
        super(group);
    }

    @Override
    float[] getMatrix() {
        return mSurface.getModel().getMatrix();
    }

    @Override
    void updateMatrix(float[] matrix) {
        ReusableVec offset = VecPool.get(3);
        try {
            mSurface.getModel().getOffset(offset);
            updateMatrix(mSurface, offset, matrix);
        } finally {
            offset.free();
        }
    }

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

    /**
     * 在绘制第一帧之前调用 (可以在此方法里进行一些初始化操作)
     * @param surface 需更新的MagicSurface对象
     */
    protected abstract void willStart(MagicSurface surface);

    /**
     * 在开始绘制后调用（绘制第一帧后调用，一般动画可以在此开始）
     * @param surface 需更新的MagicSurface对象
     */
    protected abstract void didStart(MagicSurface surface);

    /**
     * 当调用Updater的stop方法之后，真正停止后会回调此方法
     * @param surface 需更新的MagicSurface对象
     */
    protected abstract void didStop(MagicSurface surface);

    /**
     * 矩阵变换
     * @param surface 需更新的MagicSurface对象
     * @param offset offse为模型相对场景中心的坐标偏移量, 如果不进行 offset 位移， model 就会显示在场景中心；
     *
     *               当使用 View 构造 MagicSurface 时，
     *               View中心位置 相对 MagicSurfaceView中心位置的坐标偏移量 在场景坐标系中的表现就是 offset。
     *
     * @param matrix 矩阵
     */
    protected abstract void updateMatrix(MagicSurface surface, Vec offset, float[] matrix);
}
