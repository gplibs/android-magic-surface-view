package com.gplibs.magicsurfaceview;

import java.util.LinkedList;
import java.util.Queue;

class RunOnDraw {

    private Queue<Runnable> mRunOnDraw = new LinkedList<>();

    void addRunOnDraw(Runnable runnable) {
        mRunOnDraw.offer(runnable);
    }

    void runOnDraw() {
        Runnable r;
        while ((r = mRunOnDraw.poll()) != null) {
            r.run();
        }
    }

    void clearRunOnDraw() {
        mRunOnDraw.clear();
    }

    int runOnDrawSize() {
        return mRunOnDraw.size();
    }
}
