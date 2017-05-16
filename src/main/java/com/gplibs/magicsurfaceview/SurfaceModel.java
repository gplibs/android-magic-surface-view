package com.gplibs.magicsurfaceview;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * 网格模型
 */
public class SurfaceModel extends BaseModel {

    private Vec mOffset = new Vec(3);

    private int mColLineCount;
    private int mRowLineCount;
    private float mWidth;
    private float mHeight;
    private float mParentWidth;
    private float mParentHeight;
    private boolean mDrawGrid = false;
    private boolean mIsMulti = false;

    private float[] mTexCoord;
    GLParameter<FloatBuffer> mTexCoordBuffer = new GLAttributeParameter("a_tex_coord", 2, mBufferLock);

    SurfaceModel(int colCount, int rowCount, float width, float height) {
        mIsMulti = false;
        update(colCount, rowCount, width, height);
    }

    SurfaceModel(int colCount, int rowCount, float width, float height, Vec offset, Vec parentSize) {
        mIsMulti = true;
        mOffset.copy(offset);
        mParentWidth = parentSize.width();
        mParentHeight = parentSize.height();
        update(colCount, rowCount, width, height);
    }

    void getOffset(Vec offset) {
        offset.copy(mOffset);
    }

    void setOffset(float x, float y, float z) {
        mOffset.setXYZ(x, y, z);
    }

    void update(int colCount, int rowCount, float width, float height) {
        mColLineCount = colCount;
        mRowLineCount = rowCount;
        mWidth = width;
        mHeight = height;
        init();
    }

    void drawGrid(boolean drawGrid) {
        mDrawGrid = drawGrid;
    }

    private void init() {
        if (!mIsMulti) {
            mPositions = new float[mColLineCount * mRowLineCount * 3];
            mNormals = new float[mColLineCount * mRowLineCount * 3];
            mIndices = new short[(mRowLineCount - 1) * (mColLineCount * 2) + (mRowLineCount - 2) * 2];
        }
        mColors = new float[mColLineCount * mRowLineCount * 4];
        mTexCoord = new float[mColLineCount * mRowLineCount * 2];

        float startX = -mWidth / 2;
        float startY = mHeight / 2;
        float offsetX = mWidth / (mColLineCount - 1);
        float offsetY = mHeight / (mRowLineCount - 1);

        int n = 0;

        for (int r = 0; r < mRowLineCount; ++r) {
            for (int c = 0; c < mColLineCount; ++c) {
                int i = r * mColLineCount + c;
                float x = startX + offsetX * c;
                float y = startY - offsetY * r;
                if (!mIsMulti) {
                    setPosition(i, x, y, 0);
                    setNormal(i, 0, 0, 1);
                    if (r < mRowLineCount - 1) {
                        mIndices[n++] = (short) i;
                        mIndices[n++] = (short) (i + mColLineCount);
                    }
                    if (c == mColLineCount - 1 && r < mRowLineCount - 2) {
                        mIndices[n++] = (short) (i + mColLineCount);
                        mIndices[n++] = (short) (i + 1);
                    }
                }
                setColor(i, 1, 1, 1, 1);
                if (mIsMulti) {
                    mTexCoord[i * 2] = (x + mOffset.x() + mParentWidth / 2) / mParentWidth;
                    mTexCoord[i * 2 + 1] = 1 - (y + mOffset.y() + mParentHeight / 2) / mParentHeight;
                } else {
                    mTexCoord[i * 2] = (x + getWidth() / 2) / getWidth();
                    mTexCoord[i * 2 + 1] = 1 - (y + getHeight() / 2) / getHeight();
                }
            }
        }
        if (!mIsMulti) {
            preparePositions();
            prepareIndices();
            prepareNormals();
            prepareColors();
            prepareTexCoord();
        }
    }

    /**
     * 获取SurfaceModel在场景坐标系中的宽度
     * @return 宽度
     */
    public float getWidth() {
        return mWidth;
    }

    /**
     * 获取SurfaceModel在场景坐标系中的高度
     * @return 高度
     */
    public float getHeight() {
        return mHeight;
    }

    /**
     * 获取总列数
     * @return 总列数
     */
    public int getColLineCount() {
        return mColLineCount;
    }

    /**
     * 获取总行数
     * @return 总行数
     */
    public int getRowLineCount() {
        return mRowLineCount;
    }

    /**
     * 返回某行某列处在场景坐标系中的坐标，包含offset偏移量，
     * @param row 行
     * @param col 列
     * @param out 返回值
     */
    public void getPosition(int row, int col, Vec out) {
        out.setXYZ(
                getWidth() / (mColLineCount - 1) * col - getWidth() / 2 + mOffset.x(),
                -getHeight() / (mRowLineCount - 1) * row + getHeight() / 2 + mOffset.y(),
                0
        );
    }

    /**
     * 返回某行某列处在场景坐标系中的坐标，不包含offset偏移量。
     * @param row 行
     * @param col 列
     * @param out 返回值
     */
    public void getPositionExcludeOffset(int row, int col, Vec out) {
        out.setXYZ(
                getWidth() / (mColLineCount - 1) * col - getWidth() / 2,
                -getHeight() / (mRowLineCount - 1) * row + getHeight() / 2,
                0
        );
    }

    void updatePositionUseOffset() {
        Vec vec = new Vec(3);
        for (int r = 0; r < getRowLineCount(); ++r) {
            for (int c = 0; c < getColLineCount(); ++c) {
                getPosition(r, c, vec);
                setPosition(r, c, vec);
            }
        }
        preparePositions();
    }

    void setPosition(int row, int col, Vec pos) {
        int i = row * mColLineCount + col;
        setPosition(i, pos.x(), pos.y(), pos.z());
    }

    void setColor(int row, int col, Vec color) {
        int i = row * mColLineCount + col;
        setColor(i, color.r(), color.g(), color.b(), color.a());
    }

    @Override
    void setProgram(Program program) {
        super.setProgram(program);
        mTexCoordBuffer.setProgram(program);
    }

    @Override
    void runOnDraw() {
        if (!mIsMulti) {
            super.runOnDraw();
            mTexCoordBuffer.runOnDraw();
        }
    }

    @Override
    protected void drawModel() {
        if (mDrawGrid) {
            GLES20.glDrawElements(GLES20.GL_LINE_STRIP, mIndices.length, GLES20.GL_UNSIGNED_SHORT, mIndicesBuffer);
        } else {
            super.drawModel();
        }
    }

    void prepareTexCoord() {
        try {
            mBufferLock.lock();
            if (mTexCoordBuffer.value() == null) {
                FloatBuffer b = ByteBuffer.allocateDirect(mTexCoord.length * GLAttributeParameter.FLOAT_SIZE_BYTES)
                        .order(ByteOrder.nativeOrder()).asFloatBuffer();
                mTexCoordBuffer.value(b);
            }
            mTexCoordBuffer.value().put(mTexCoord).position(0);
            mTexCoordBuffer.refresh();
        } finally {
            mBufferLock.unlock();
        }
    }

}
