package com.gplibs.magicsurfaceview;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

/**
 * 可重用Vec管理池，提供可重复使用的向量，减少内存分配次数。
 * 内存分配次数过多，会导致频繁的 gc, 影响性能
 */
public final class VecPool {

    private static SimpleLock sLock = new SimpleLock();
    private static ArrayList<Reference<? extends ReusableVec>> mCache = new ArrayList<>(25);
    private static ReferenceQueue<ReusableVec> mQueue = new ReferenceQueue<>();

    private VecPool(){
    }

    /**
     * 获取长度为length的ReusableVec
     * @param length 长度
     * @return ReusableVec
     */
    public static ReusableVec get(int length) {
        try {
            sLock.lock();
            removeEmpty();
            for (int i = 0; i < mCache.size(); ++i) {
                Reference<? extends ReusableVec> ref = mCache.get(i);
                ReusableVec v = ref.get();
                if (v == null) {
                    continue;
                }
                if (v.isFree() && v.getData().length == length) {
                    v.setUpdateTimes(0);
                    v.use();
                    return v;
                }
            }
            ReusableVec v = new ReusableVec(length);
            mCache.add(new SoftReference<>(v, mQueue));
            v.use();
            return v;
        } finally {
            sLock.unlock();
        }
    }

    /**
     * 获取长度为length的ReusableVec 并将数据重置清0
     * @param length 长度
     * @return ReusableVec
     */
    public static ReusableVec getAndReset(int length) {
        ReusableVec v = get(length);
        for (int i = 0; i < v.getData().length; ++i) {
            v.setValue(i, 0);
        }
        return v;
    }

    private static void removeEmpty() {
        Reference<? extends ReusableVec> ref;
        while ((ref = mQueue.poll()) != null) {
            mCache.remove(ref);
        }
    }
}
