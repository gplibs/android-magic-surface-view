package com.gplibs.magicsurfaceview;

class WaitNotify {

    private boolean mWait = false;
    private boolean mNotify = false;
    private SimpleLock mLock = new SimpleLock();

    WaitNotify() {
    }

    void doNotify() {
        mLock.lock();
        try {
            mNotify = true;
            if (mWait) {
                synchronized (this) {
                    this.notify();
                }
            }
        } finally {
            mLock.unlock();
        }
    }

    void doWait() {
        mLock.lock();
        boolean unlock = false;
        try {
            if (mNotify) {
                return;
            }
            mWait = true;
            synchronized (this) {
                try {
                    mLock.unlock();
                    unlock = true;
                    this.wait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            if (!unlock) {
                mLock.unlock();
            }
        }
    }

    void reset() {
        mLock.lock();
        mWait = false;
        mNotify = false;
        mLock.unlock();
    }
}
