package com.gplibs.magicsurfaceview;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 通用向量对象
 * 提供向量和颜色通用赋值取值方法
 */
public class Vec {

    private AtomicInteger mUpdateTimes = new AtomicInteger(0);
    private float[] mData;

    public Vec(int length) {
        mData = new float[length];
    }

    public Vec(float... data) {
        mData = data;
        mUpdateTimes.addAndGet(1);
    }

    public Vec(Vec vec) {
        this(vec.getData().length);
        System.arraycopy(vec.getData(), 0, mData, 0, mData.length);
        mUpdateTimes.addAndGet(1);
    }

    public void setColor(int color) {
        setRGBA(
                (color >> 16 & 0XFF) / 255.f,   // r
                (color >> 8 & 0XFF) / 255.f,    // g
                (color & 0XFF) / 255.f,         // b
                (color >> 24 & 0XFF) / 255.f    // a
        );
    }

    public int getColor() {
        return ((int)(a() * 255)) << 24 |
                ((int)(r() * 255)) << 16 |
                ((int)(g() * 255)) << 8 |
                ((int)(b() * 255));
    }

    public float[] getData() {
        return mData;
    }

    public void copy(Vec v) {
        for (int i = 0; i < mData.length; ++i) {
            if (i > v.getData().length - 1) {
                break;
            }
            mData[i] = v.value(i);
        }
        mUpdateTimes.addAndGet(1);
    }

    public void add(Vec v) {
        for (int i = 0; i < mData.length; ++i) {
            if (i > v.getData().length - 1) {
                break;
            }
            mData[i] = value(i) + v.value(i);
        }
        mUpdateTimes.addAndGet(1);
    }

    public void add(float x, float y, float z) {
        mData[0] = x() + x;
        mData[1] = y() + y;
        mData[2] = z() + z;
        mUpdateTimes.addAndGet(1);
    }

    public void sub(Vec v) {
        for (int i = 0; i < mData.length; ++i) {
            if (i > v.getData().length - 1) {
                break;
            }
            mData[i] = value(i) - v.value(i);
        }
        mUpdateTimes.addAndGet(1);
    }

    public void sub(float x, float y, float z) {
        add(-x, -y, -z);
    }

    public float x() {
        return mData[0];
    }

    public float y() {
        return mData[1];
    }

    public float z() {
        return mData[2];
    }

    public void x(float x) {
        mData[0] = x;
        mUpdateTimes.addAndGet(1);
    }

    public void y(float y) {
        mData[1] = y;
        mUpdateTimes.addAndGet(1);
    }

    public void z(float z) {
        mData[2] = z;
        mUpdateTimes.addAndGet(1);
    }

    public void setXY(float x, float y) {
        mData[0] = x;
        mData[1] = y;
        mUpdateTimes.addAndGet(1);
    }

    public void setXYZ(float x, float y, float z) {
        mData[0] = x;
        mData[1] = y;
        mData[2] = z;
        mUpdateTimes.addAndGet(1);
    }

    public void setXYZ(float value) {
        setXYZ(value, value, value);
    }

    public float r() {
        return mData[0];
    }

    public float g() {
        return mData[1];
    }

    public float b() {
        return mData[2];
    }

    public float a() {
        return mData[3];
    }

    public void r(float r) {
        mData[0] = r;
        mUpdateTimes.addAndGet(1);
    }

    public void g(float g) {
        mData[1] = g;
        mUpdateTimes.addAndGet(1);
    }

    public void b(float b) {
        mData[2] = b;
        mUpdateTimes.addAndGet(1);
    }

    public void a(float a) {
        mData[3] = a;
        mUpdateTimes.addAndGet(1);
    }

    public void setRGBA(float r, float g, float b, float a) {
        mData[0] = r;
        mData[1] = g;
        mData[2] = b;
        mData[3] = a;
        mUpdateTimes.addAndGet(1);
    }

    public float width() {
        return mData[0];
    }

    public float height() {
        return mData[1];
    }

    public void setSize(float w, float h) {
        mData[0] = w;
        mData[1] = h;
        mUpdateTimes.addAndGet(1);
    }

    public void setValue(int index, float value) {
        mData[index] = value;
        mUpdateTimes.addAndGet(1);
    }

    public float value(int index) {
        return mData[index];
    }

    public int getUpdateTimes() {
        return mUpdateTimes.get();
    }

    void setUpdateTimes(int times) {
        mUpdateTimes.set(times);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (float f : mData) {
            sb.append(String.format("%f,", f));
        }
        if (sb.length() > 0) {
            sb.delete(sb.length() - 1, sb.length());
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Vec)) {
            return false;
        }
        Vec v = (Vec) obj;
        if (v.getData().length != getData().length) {
            return false;
        }
        for (int i = 0; i < v.getData().length; ++i) {
            if (v.getData()[i] != getData()[i]) {
                return false;
            }
        }
        return true;
    }
}
