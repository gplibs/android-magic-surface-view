package com.gplibs.magicsurfaceview;

public abstract class MagicSurfaceModelUpdater extends MagicUpdater {

    MagicSurface mSurface;

    public MagicSurfaceModelUpdater() {
    }

    public MagicSurfaceModelUpdater(int group) {
        super(group);
    }

    @Override
    void willStart() {
        mSurface.getModel().updatePositionUseOffset();
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
        ReusableVec pos = VecPool.get(3);
        ReusableVec color = VecPool.get(4);
        boolean posChanged = false;
        boolean colorChanged = false;
        for (int r = 0; r < mSurface.getModel().getRowLineCount(); ++r) {
            for (int c = 0; c < mSurface.getModel().getColLineCount(); ++c) {
                mSurface.getModel().getPosition(r, c, pos);
                color.setRGBA(1, 1, 1, 1);
                int posUpdateTimes = pos.getUpdateTimes();
                int colorUpdateTimes = color.getUpdateTimes();

                updatePosition(mSurface, r, c, pos, color);

                if (posChanged || pos.getUpdateTimes() != posUpdateTimes) {
                    posChanged = true;
                    mSurface.getModel().setPosition(r, c, pos);
                }
                if (colorChanged || color.getUpdateTimes() != colorUpdateTimes) {
                    colorChanged = true;
                    mSurface.getModel().setColor(r, c, color);
                }
            }
        }
        pos.free();
        color.free();
        if (posChanged) {
            mSurface.getModel().preparePositions();
        }
        if (colorChanged){
            mSurface.getModel().prepareColors();
        }
        updateEnd(mSurface);
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
     * 每次顶点更新之前调用
     * @param surface 需更新的MagicSurface对象
     */
    protected abstract void updateBegin(MagicSurface surface);

    /**
     * 修改网格模型 r行, c列处 的坐标及颜色， 修改后的值存到 outPos 和 outColor
     * (只需要修改网格模型各行各列点及可，点与点之间的坐标和颜色由 openGL 自动进行插值计算完成)
     * 注：此方法执行频率非常高；一般不要有分配新的堆内存的逻辑，会频繁产生gc操作影响性能
     *
     * @param surface 需更新的MagicSurface对象
     * @param r 行
     * @param c 列
     * @param outPos 默认值为 r行c列点包含偏移量的原始坐标, 计算完成后的新坐标要更新到此变量
     * @param outColor 默认值为 rgba(1,1,1,1), 计算完成后的新颜色要更新到此变量
     */
    protected abstract void updatePosition(MagicSurface surface, int r, int c, Vec outPos, Vec outColor);

    /**
     * 每次所有顶点更新完成后调用
     * @param surface 需更新的MagicSurface对象
     */
    protected abstract void updateEnd(MagicSurface surface);

}