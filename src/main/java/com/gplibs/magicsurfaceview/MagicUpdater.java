package com.gplibs.magicsurfaceview;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class MagicUpdater extends RunOnDraw {

    private final int STATE_NONE = 0;
    private final int STATE_STARTING = 1;
    private final int STATE_READY = 2;
    private final int STATE_RUNNING = 3;
    private final int STATE_STOPPED = 4;

    private int mState = STATE_NONE;
    private boolean mIsFirstTimeCheckStarted = true;
    private boolean mIsFirstTimeCheckStopped = true;
    private AtomicBoolean mNeedUpdate = new AtomicBoolean(true);
    private List<MagicUpdaterListener> mListeners = new ArrayList<>();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private SimpleLock mStateLock = new SimpleLock();
    private WaitNotify mWaitNotify = new WaitNotify();
    private MagicUpdaterGroup mUpdaterGroup;

    private MagicUpdaterGroup.MagicUpdaterRunnable mRunnable = new MagicUpdaterGroup.MagicUpdaterRunnable() {

        @Override
        public boolean isStopped() {
            return MagicUpdater.this.isStopped();
        }

        @Override
        public boolean needUpdate() {
            return mNeedUpdate.get();
        }

        @Override
        public void run() {
            if (isRunning()) {
                mNeedUpdate.set(false);
                update();
                mWaitNotify.doNotify();
            } else if (isStarting() || isReadyToUpdate()) {
                mUpdaterGroup.update();
            }
        }
    };

    MagicUpdater() {
        mUpdaterGroup = MagicUpdaterGroup.getNewGroup();
        start();
    }

    MagicUpdater(int group) {
        mUpdaterGroup = MagicUpdaterGroup.getGroup(group);
        start();
    }

    public void setGroup(int group) {
        mUpdaterGroup = MagicUpdaterGroup.getGroup(group);
    }

    public void addListener(MagicUpdaterListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    public void removeListener(MagicUpdaterListener listener) {
        if (mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    protected void notifyChanged() {
        mNeedUpdate.set(true);
        mUpdaterGroup.update();
    }

    private boolean isStarting() {
        return mState == STATE_STARTING;
    }

    private boolean isReadyToUpdate() {
        return mState == STATE_READY;
    }

    private boolean isRunning() {
        return mState == STATE_RUNNING;
    }

    boolean isStopped() {
        return mState == STATE_STOPPED;
    }

    private boolean isFirstTimeCheckStarted() {
        if (mIsFirstTimeCheckStarted) {
            mIsFirstTimeCheckStarted = false;
            return true;
        }
        return false;
    }

    private boolean isFirstTimeCheckStopped() {
        if (mIsFirstTimeCheckStopped) {
            mIsFirstTimeCheckStopped = false;
            return true;
        }
        return false;
    }

    private int getState() {
        return mState;
    }

    private void setState(int state) {
        mStateLock.lock();
        mState = state;
        mStateLock.unlock();
        notifyChanged();
    }


    void start() {
        mIsFirstTimeCheckStarted = true;
        mIsFirstTimeCheckStopped = true;
        mStateLock.lock();
        try {
            if (mState != STATE_NONE && mState != STATE_STOPPED) {
                return;
            }
            mState = STATE_STARTING;
        } finally {
            mStateLock.unlock();
        }
        addRunOnDraw(new Runnable() {
            @Override
            public void run() {
                mStateLock.lock();
                try {
                    mUpdaterGroup.addRunnable(mRunnable);
                    if (isStopped()) {
                        return;
                    }
                    mState = STATE_READY;
                } finally {
                    mStateLock.unlock();
                }
                mUpdaterGroup.start();
            }
        });
    }

    public void stop() {
        setState(STATE_STOPPED);
    }


    abstract void willStart();

    abstract void didStart();

    abstract void didStop();

    abstract void update();


    private void waitUpdate() {
        mWaitNotify.doWait();
    }

    private void run() {
        setState(STATE_RUNNING);
    }

    private void doStopped() {
        didStop();
        if (mListeners.size() > 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (MagicUpdaterListener l : mListeners) {
                        if (l != null) {
                            l.onStop();
                        }
                    }
                }
            });
        }
    }

    private void doWillStart() {
        mWaitNotify.reset();
        notifyChanged();
        willStart();
    }

    private void doStarted() {
        notifyChanged();
        didStart();
        if (mListeners.size() > 0) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    for (MagicUpdaterListener l : mListeners) {
                        if (l != null) {
                            l.onStart();
                        }
                    }
                }
            });
        }
    }


    static boolean prepareUpdater(MagicUpdater updater) {
        if (updater == null) {
            return true;
        }
        if (updater.isReadyToUpdate()) {
            updater.doWillStart();
            updater.run();
            updater.waitUpdate();
            return true;
        }
        return updater.getState() >= updater.STATE_RUNNING;
    }

    static void doStartedAndStopped(MagicUpdater updater) {
        if (updater == null) {
            return;
        }
        if (updater.isFirstTimeCheckStarted()) {
            updater.doStarted();
        }
        if (updater.isStopped() && updater.isFirstTimeCheckStopped()) {
            updater.doStopped();
        }
    }
}
