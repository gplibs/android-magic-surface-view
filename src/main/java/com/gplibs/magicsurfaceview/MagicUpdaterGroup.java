package com.gplibs.magicsurfaceview;

import android.util.SparseArray;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executors;

class MagicUpdaterGroup {

    private static LinkedList<MagicUpdaterGroup> sNoNumberGroups = new LinkedList<>();
    private static SparseArray<MagicUpdaterGroup> sGroups = new SparseArray<>();
    private static SimpleLock sLock = new SimpleLock();

    private List<WeakReference<MagicUpdaterRunnable>> mRunnables = new ArrayList<>();
    private WaitNotify mWaitNotify = new WaitNotify();
    private long mLastUpdateTime;
    private boolean mIsRunning = false;
    private SimpleLock mLock = new SimpleLock();
    private Integer mGroupId = null;

    private MagicUpdaterGroup() {
    }

    void addRunnable(MagicUpdaterRunnable runnable) {
        try {
            mLock.lock();
            mRunnables.add(new WeakReference<MagicUpdaterRunnable>(runnable));
            update();
        } finally {
            mLock.unlock();
        }
    }

    void start() {
        update();
        if (mIsRunning) {
            return;
        }
        mIsRunning = true;
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                while (MagicUpdaterGroup.this.run()) {
                    if (System.currentTimeMillis() - mLastUpdateTime > 100) { // 100毫秒无任何更新，休眠线程，节约资源
                        mWaitNotify.doWait();
                        mWaitNotify.reset();
                    }
                }
                mIsRunning = false;
            }
        });
    }

    void update() {
        mLastUpdateTime = System.currentTimeMillis();
        mWaitNotify.doNotify();
    }

    private boolean run() {
        try {
            mLock.lock();
            boolean hasRunning = false;
            for (int i = 0; i < mRunnables.size(); ++i) {
                MagicUpdaterRunnable r = mRunnables.get(i).get();
                if (r == null || r.isStopped()) {
                    continue;
                }
                if (r.needUpdate()) {
                    r.run();
                }
                hasRunning = true;
            }
            if (!hasRunning) {
                mRunnables.clear();
                if (mGroupId != null) {
                    try {
                        sLock.lock();
                        sGroups.remove(mGroupId);
                    } finally {
                        sLock.unlock();
                    }
                }
            }
            return hasRunning;
        } finally {
            mLock.unlock();
        }
    }

    interface MagicUpdaterRunnable extends Runnable {
        boolean isStopped();
        boolean needUpdate();
        void run();
    }

    static MagicUpdaterGroup getGroup(int group) {
        activeAllGroup();
        try {
            sLock.lock();
            MagicUpdaterGroup g = sGroups.get(group);
            if (g == null) {
                g = new MagicUpdaterGroup();
                g.mGroupId = group;
                sGroups.put(group, g);
            }
            return g;
        } finally {
            sLock.unlock();
        }
    }

    static MagicUpdaterGroup getNewGroup() {
        activeAllGroup();
        try {
            sLock.lock();
            MagicUpdaterGroup g = new MagicUpdaterGroup();
            sNoNumberGroups.add(g);
            return g;
        } finally {
            sLock.unlock();
        }
    }

    private static void activeAllGroup() {
        try {
            sLock.lock();
            for (int i = 0; i < sGroups.size(); ++i) {
                sGroups.valueAt(i).update();
            }
            for (MagicUpdaterGroup g : sNoNumberGroups) {
                g.update();
            }
            Queue<MagicUpdaterGroup> q = new LinkedList<>();
            for (MagicUpdaterGroup g : sNoNumberGroups) {
                if (g.mRunnables.isEmpty()) {
                    q.add(g);
                }
            }
            MagicUpdaterGroup g;
            while ((g = q.poll()) != null) {
                sNoNumberGroups.remove(g);
            }
        } finally {
            sLock.unlock();
        }
    }
}
