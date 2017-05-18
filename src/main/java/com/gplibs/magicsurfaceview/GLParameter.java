package com.gplibs.magicsurfaceview;

abstract class GLParameter<T> extends RunOnDraw {

    protected String mName;
    protected T mValue;
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            updateValue();
        }
    };
    protected Program mProgram;

    GLParameter() {
    }

    GLParameter(String name) {
        mName = name;
    }

    String name() {
        return mName;
    }

    GLParameter<T> name(String name) {
        mName = name;
        return this;
    }

    T value() {
        return mValue;
    }

    GLParameter<T> value(T value) {
        mValue = value;
        refresh();
        return this;
    }

    void setProgram(Program program) {
        mProgram = program;
    }

    void refresh() {
        if (mProgram != null && !mProgram.isDeleted() && mProgram.isGLThread()) {
            updateValue();
        } else {
            if (runOnDrawSize() > 0) {
                return;
            }
            addRunOnDraw(mRunnable);
        }
    }

    protected abstract int handle();

    protected abstract void updateValue();
}
