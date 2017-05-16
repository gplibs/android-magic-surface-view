package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.concurrent.locks.ReentrantLock;

public abstract class BaseModel extends RunOnDraw {

    protected float[] mPositions;
    protected short[] mIndices;
    protected float[] mNormals;
    protected float[] mColors;

    private float[] mMatrix;
    protected ReentrantLock mBufferLock = new ReentrantLock();

    protected GLParameter<FloatBuffer> mPositionsBuffer = new GLAttributeParameter("a_position", 3, mBufferLock);
    protected ShortBuffer mIndicesBuffer;
    protected GLParameter<FloatBuffer> mNormalsBuffer = new GLAttributeParameter("a_normal", 3, mBufferLock);
    GLParameter<FloatBuffer> mColorsBuffer = new GLAttributeParameter("a_color", 4, mBufferLock);

    public BaseModel() {
        mMatrix = new float[16];
        MatrixManager.reset(mMatrix);
    }

    void setProgram(Program program) {
        mPositionsBuffer.setProgram(program);
        mNormalsBuffer.setProgram(program);
        mColorsBuffer.setProgram(program);
    }

    float[] getMatrix() {
        return mMatrix;
    }

    @Override
    void runOnDraw() {
        super.runOnDraw();
        mPositionsBuffer.runOnDraw();
        mNormalsBuffer.runOnDraw();
        mColorsBuffer.runOnDraw();
    }

    void draw() {
        drawModel();
    }

    protected void drawModel() {
        GLES20.glDrawElements(GLES20.GL_TRIANGLE_STRIP, mIndices.length, GLES20.GL_UNSIGNED_SHORT, mIndicesBuffer);
    }

    void prepareIndices() {
        try {
            mBufferLock.lock();
            if (mIndicesBuffer == null) {
                mIndicesBuffer = ByteBuffer.allocateDirect(mIndices.length * 2)
                        .order(ByteOrder.nativeOrder()).asShortBuffer();
            }
            mIndicesBuffer.put(mIndices).position(0);
        } finally {
            mBufferLock.unlock();
        }
    }

    void preparePositions() {
        try {
            mBufferLock.lock();
            if (mPositionsBuffer.value() == null) {
                FloatBuffer b = ByteBuffer.allocateDirect(mPositions.length * GLAttributeParameter.FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                mPositionsBuffer.value(b);
            }
            mPositionsBuffer.value().put(mPositions).position(0);
            mPositionsBuffer.refresh();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            mBufferLock.unlock();
        }
    }

    void prepareColors() {
        try {
            mBufferLock.lock();
            if (mColorsBuffer.value() == null) {
                FloatBuffer b = ByteBuffer.allocateDirect(mColors.length * GLAttributeParameter.FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                mColorsBuffer.value(b);
            }
            mColorsBuffer.value().put(mColors).position(0);
            mColorsBuffer.refresh();
        } finally {
            mBufferLock.unlock();
        }
    }

    void prepareNormals() {
        try {
            mBufferLock.lock();
            if (mNormalsBuffer.value() == null) {
                FloatBuffer b = ByteBuffer.allocateDirect(mNormals.length * GLAttributeParameter.FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                mNormalsBuffer.value(b);
            }
            mNormalsBuffer.value().put(mNormals).position(0);
            mNormalsBuffer.refresh();
        } finally {
            mBufferLock.unlock();
        }
    }

    public void updateModelNormal() {
        if (mNormals == null) {
            mNormals = new float[mPositions.length];
        }
        ReusableVec surfaceNormal = VecPool.get(3);
        boolean normalState = false;
        for (int i = 0; i < mIndices.length - 2; ++i) {
            short index = mIndices[i];
            short index1 = mIndices[i + 1];
            short index2 = mIndices[i + 2];

            if (!checkIndices(index, index1, index2)) {
                normalState = false;
                continue;
            }

            if (normalState) {
                getNormal(mPositions, index1 * 3, index * 3, index2 * 3, surfaceNormal);
            } else {
                getNormal(mPositions, index * 3, index1 * 3, index2 * 3, surfaceNormal);
            }
            normalState = !normalState;
            normalize(surfaceNormal);
            attachModelNormalData(surfaceNormal, i);
        }
        surfaceNormal.free();
        prepareNormals();
    }

    void setPosition(int i, float x, float y, float z) {
        mPositions[i * 3] = x;
        mPositions[i * 3 + 1] = y;
        mPositions[i * 3 + 2] = z;
    }

    void setColor(int i, float r, float g, float b, float a) {
        mColors[i * 4] = r;
        mColors[i * 4 + 1] = g;
        mColors[i * 4 + 2] = b;
        mColors[i * 4 + 3] = a;
    }

    void setNormal(int i, float x, float y, float z) {
        mNormals[i * 3] = x;
        mNormals[i * 3 + 1] = y;
        mNormals[i * 3 + 2] = z;
    }

    private void attachModelNormalData(Vec surfaceNormal, int i) {
        for (int j = i; j < i + 3; ++j) {
            short index = mIndices[j];
            mNormals[index * 3] = mNormals[index * 3] + surfaceNormal.x();
            mNormals[index * 3 + 1] = mNormals[index * 3 + 1] + surfaceNormal.y();
            mNormals[index * 3 + 2] = mNormals[index * 3 + 2] + surfaceNormal.z();
        }
    }

    private boolean checkIndices(short index0, short index1, short index2) {
        return  index0 != index1 &&
                index0 != index2 &&
                index1 != index2;
    }

    private void getNormal(float[] points, int start1, int start2, int start3, Vec out) {
        ReusableVec vec1 = VecPool.get(3);
        ReusableVec vec2 = VecPool.get(3);
        getVec(points, start1, start2, vec1);
        getVec(points, start1, start3, vec2);
        getNormal(vec1, vec2, out);
        vec1.free();
        vec2.free();
    }

    private void getVec(float[] points, int start1, int start2, Vec out) {
        out.setXYZ(
                points[start2] - points[start1],
                points[start2 + 1] - points[start1 + 1],
                points[start2 + 2] - points[start1 + 2]
        );
    }

    private void getNormal(Vec vec1, Vec vec2, Vec out) {
        out.setXYZ(
                vec1.y() * vec2.z() - vec1.z() * vec2.y(),
                vec1.z() * vec2.x() - vec1.x() * vec2.z(),
                vec1.x() * vec2.y() - vec1.y() * vec2.x()
        );
    }

    private void normalize(Vec normal) {
        float length = (float) Math.sqrt(Math.pow(normal.x(), 2) + Math.pow(normal.y(), 2) + Math.pow(normal.z(), 2));
        normal.setXYZ(
                normal.x() / length,
                normal.y() / length,
                normal.z() / length
        );
    }

}
