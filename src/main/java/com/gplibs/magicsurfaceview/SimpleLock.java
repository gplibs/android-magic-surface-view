package com.gplibs.magicsurfaceview;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 系统Lock进行lock操作时会有部分新内存的分配，大量密集调用会导致gc频繁调用影响性能。
 * 需要频繁调用的地方可以用此锁
 */
class SimpleLock {

    private AtomicReference<SimpleLock> t = new AtomicReference<>(null);

    void lock() {
        while (!t.compareAndSet(null, this));
    }

    void unlock() {
        t.set(null);
    }
}
