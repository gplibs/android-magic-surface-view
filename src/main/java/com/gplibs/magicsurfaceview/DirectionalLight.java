package com.gplibs.magicsurfaceview;

/**
 * 方向光源
 */
public class DirectionalLight extends Light {

    private GLParameter<Vec> mDirection = new GLUniformParameter<Vec>().value(new Vec(3));

    /**
     * 构造方向光源
     * @param color 取值模型: 0XAARRGGBB
     * @param x 方向向量x分量
     * @param y 方向向量y分量
     * @param z 方向向量z分量
     */
    public DirectionalLight(int color, float x, float y, float z) {
        super(false, color);
        setDirection(x, y, z);
    }

    /**
     * 构造方向光源
     * @param color 取值模型: 0XAARRGGBB
     * @param dir 方向向量
     */
    public DirectionalLight(int color, Vec dir) {
        super(false, color);
        setDirection(dir);
    }

    public void setDirection(float x, float y, float z) {
        mDirection.value().setXYZ(x, y, z);
        mDirection.refresh();
    }

    public void setDirection(Vec dir) {
        mDirection.value().copy(dir);
        mDirection.refresh();
    }

    public Vec getDirection() {
        return mDirection.value();
    }

    @Override
    void setProgram(Program program) {
        super.setProgram(program);
        mDirection.setProgram(program);
    }

    @Override
    void setIndex(int index) {
        super.setIndex(index);
        mDirection.name(String.format("u_l%d_pos_or_dir", index));
    }

    @Override
    protected void runOnDraw() {
        super.runOnDraw();
        mDirection.runOnDraw();
    }

    @Override
    public void restore() {
        super.restore();
        mDirection.refresh();
    }
}
