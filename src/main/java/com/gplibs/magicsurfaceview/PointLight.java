package com.gplibs.magicsurfaceview;

/**
 * 点光源
 */
public class PointLight extends Light {

    private GLParameter<Vec> mPosition = new GLUniformParameter<Vec>().value(new Vec(3));

    /**
     * 构造点光源
     * @param color 取值模型: 0XAARRGGBB
     * @param x 点光源位置x
     * @param y 点光源位置y
     * @param z 点光源位置z
     */
    public PointLight(int color, float x, float y, float z) {
        super(true, color);
        setPosition(x, y, z);
    }

    /**
     * 构造点光源
     * @param color 取值模型: 0XAARRGGBB
     * @param pos 点光源位置
     */
    public PointLight(int color, Vec pos) {
        this(color, pos.x(), pos.y(), pos.z());
    }

    /**
     * 设置光源位置
     * @param x
     * @param y
     * @param z
     */
    public void setPosition(float x, float y, float z) {
        mPosition.value().setXYZ(x, y, z);
        mPosition.refresh();
    }

    /**
     * 设置光源位置
     * @param pos
     */
    public void setPosition(Vec pos) {
        mPosition.value().copy(pos);
        mPosition.refresh();
    }

    /**
     * 获取光源位置
     * @return 光源位置
     */
    public Vec getPosition() {
        return mPosition.value();
    }

    @Override
    void setProgram(Program program) {
        super.setProgram(program);
        mPosition.setProgram(program);
    }

    @Override
    void setIndex(int index) {
        super.setIndex(index);
        mPosition.name(String.format("u_l%d_pos_or_dir", index));
    }

    @Override
    protected void runOnDraw() {
        super.runOnDraw();
        mPosition.runOnDraw();
    }

    @Override
    public void restore() {
        super.restore();
        mPosition.refresh();
    }
}
