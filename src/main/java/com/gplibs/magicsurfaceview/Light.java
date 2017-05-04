package com.gplibs.magicsurfaceview;

public abstract class Light extends RunOnDraw {

    private GLParameter<Boolean> mIsPointLight = new GLUniformParameter<>();
    private GLParameter<Vec> mColor = new GLUniformParameter<Vec>().value(new Vec(4));
    private GLParameter<Boolean> mEnable = new GLUniformParameter<Boolean>().value(true);

    Light(boolean isPointLight, int color) {
        setColor(color);
        this.mIsPointLight.value(isPointLight);
    }

    void setProgram(Program program) {
        mIsPointLight.setProgram(program);
        mColor.setProgram(program);
        mEnable.setProgram(program);
    }

    void setIndex(int index) {
        mEnable.name(String.format("u_light%d_", index));
        mColor.name(String.format("u_l%d_color", index));
        mIsPointLight.name(String.format("u_l%d_is_point_light", index));
    }

    public void setColor(int color) {
        mColor.value().setColor(color);
        mColor.refresh();
    }

    public void setColor(Vec color) {
        mColor.value().copy(color);
        mColor.refresh();
    }

    public void setColor(float r, float g, float b, float a) {
        mColor.value().setRGBA(r, g, b, a);
        mColor.refresh();
    }

    public Vec getColor() {
        return mColor.value();
    }

    public void setEnable(boolean enable) {
        this.mEnable.value(enable);
    }

    public boolean isEnable() {
        return mEnable.value();
    }

    void restore() {
        mEnable.refresh();
        mColor.refresh();
        mIsPointLight.refresh();
    }

    @Override
    protected void runOnDraw() {
        super.runOnDraw();
        mEnable.runOnDraw();
        mColor.runOnDraw();
        mIsPointLight.runOnDraw();
    }
}
