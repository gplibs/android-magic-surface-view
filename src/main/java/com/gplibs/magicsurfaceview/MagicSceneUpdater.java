package com.gplibs.magicsurfaceview;

public abstract class MagicSceneUpdater extends MagicUpdater {

    MagicScene mScene;

    public MagicSceneUpdater() {
    }

    public MagicSceneUpdater(int group) {
        super(group);
    }

    @Override
    void willStart() {
        willStart(mScene);
    }

    @Override
    void didStart() {
        didStart(mScene);
    }

    @Override
    void didStop() {
        didStop(mScene);
    }

    @Override
    void update() {
        try {
            update(mScene, mScene.mAmbientColor.value());
            mScene.mAmbientColor.refresh();
        }catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    protected abstract void willStart(MagicScene scene);

    protected abstract void didStart(MagicScene scene);

    protected abstract void didStop(MagicScene scene);

    protected abstract void update(MagicScene scene, Vec outAmbientColor);
}
