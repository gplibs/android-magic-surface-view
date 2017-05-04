package com.gplibs.magicsurfaceview;

import android.opengl.Matrix;

class MatrixManager extends RunOnDraw {

    private float[] mPMatrix = new float[16];
    private float[] mVMatrix = new float[16];

    private GLParameter<float[]> mMVPMatrix = new GLUniformParameter<float[]>("u_mvp_matrix").value(new float[16]);
    private GLParameter<float[]> mModelMatrix = new GLUniformParameter<float[]>("u_m_matrix").value(new float[16]);
    private GLParameter<Vec> mCameraLocation = new GLUniformParameter<Vec>("u_camera").value(new Vec(3));

    MatrixManager() {
    }

    void setProgram(Program program) {
        mMVPMatrix.setProgram(program);
        mModelMatrix.setProgram(program);
        mCameraLocation.setProgram(program);
    }

    void setModelMatrix(float[] modelMatrix) {
        System.arraycopy(modelMatrix, 0, mModelMatrix.value(), 0, modelMatrix.length);
        updateMVPMatrix();
        mModelMatrix.refresh();
    }

    void setLookAtM(float eyeX, float eyeY, float eyeZ, float centerX, float centerY, float centerZ, float upX, float upY, float upZ) {
        mCameraLocation.value().setXYZ(eyeX, eyeY, eyeZ);
        mCameraLocation.refresh();
        Matrix.setLookAtM(mVMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ);
        updateMVPMatrix();
    }

    void frustumM(float width, float height, float near, float far) {
        Matrix.frustumM(mPMatrix, 0, -width / 2, width / 2, -height / 2, height / 2, near, far);
        updateMVPMatrix();
    }

    @Override
    protected void runOnDraw() {
        super.runOnDraw();
        mMVPMatrix.runOnDraw();
        mModelMatrix.runOnDraw();
        mCameraLocation.runOnDraw();
    }

    private void updateMVPMatrix() {
        Matrix.multiplyMM(mMVPMatrix.value(), 0, mVMatrix, 0, mModelMatrix.value(), 0);
        Matrix.multiplyMM(mMVPMatrix.value(), 0, mPMatrix, 0, mMVPMatrix.value(), 0);
        mMVPMatrix.refresh();
    }

    static void reset(float[] matrixData) {
        Matrix.setIdentityM(matrixData, 0);
    }

    static void rotateM(float[] matrixData, float x, float y, float z, float a) {
        Matrix.rotateM(matrixData, 0, a, x, y, z);
    }

    static void translateM(float[] matrixData, float x, float y, float z) {
        Matrix.translateM(matrixData, 0, x, y, z);
    }

    static void scaleM(float[] matrixData, float x, float y, float z) {
        Matrix.scaleM(matrixData, 0, x, y, z);
    }

}
