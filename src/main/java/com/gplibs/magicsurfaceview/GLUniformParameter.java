package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

class GLUniformParameter<T> extends GLParameter<T> {

    private int mHandle;

    GLUniformParameter() {
    }

    GLUniformParameter(String name) {
        super(name);
    }

    @Override
    protected int handle() {
        if (mHandle == 0) {
            mHandle = GLES20.glGetUniformLocation(mProgram.handle, mName);
        }
        return mHandle;
    }

    @Override
    protected void updateValue() {
        if (mValue instanceof Float) {
            updateFloatValue((Float) mValue);
        } else if (mValue instanceof float[]) {
            updateFloatValue((float[]) mValue);
        } else if (mValue instanceof Vec) {
            updateFloatValue(((Vec) mValue).getData());
        } else if (mValue instanceof Boolean) {
            updateBooleanValue((Boolean) mValue);
        } else if (mValue instanceof Texture) {
            updateTextureValue((Texture) mValue);
        }
    }

    private void updateBooleanValue(boolean value) {
        GLES20.glUniform1i(handle(), value ? 1 : 0);
    }

    private void updateFloatValue(float... value) {
        switch (value.length) {
            case 1:
                GLES20.glUniform1f(handle(), value[0]);
                break;
            case 2:
                GLES20.glUniform2f(handle(), value[0], value[1]);
                break;
            case 3:
                GLES20.glUniform3f(handle(), value[0], value[1], value[2]);
                break;
            case 4:
                GLES20.glUniform4f(handle(), value[0], value[1], value[2], value[3]);
                break;
            case 16:
                GLES20.glUniformMatrix4fv(handle(), 1, false, value, 0);
            default:
                break;
        }
    }

    private void updateTextureValue(Texture texture) {
        GLES20.glActiveTexture(texture.mIndex);
        GLUtil.checkGlError("glActiveTexture");
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.mId);
        GLES20.glUniform1i(handle(), texture.mIndex - GLES20.GL_TEXTURE0);
    }
}
