package com.gplibs.magicsurfaceview;

import android.content.Context;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.List;

public class MagicSurfaceView extends GLSurfaceView {

    private boolean mHasNewScene = false;
    private MagicRenderer mRenderer = new MagicRenderer();
    private MagicRenderer.MagicRendererListener mMagicRendererListener = new MagicRenderer.MagicRendererListener() {
        @Override
        public void onGLResourceReleased() {
            if (!mHasNewScene) {
                MagicSurfaceView.super.onPause();
            }
        }
    };

    public MagicSurfaceView(Context context) {
        super(context);
        init();
    }

    public MagicSurfaceView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 24, 0);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        setZOrderOnTop(true);
        mRenderer.setListener(mMagicRendererListener);
        setRenderer(mRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (mRenderer.getScene() == null) {
                            onPause();
                        }
                    }
                }, 50);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                mRenderer.onDestroy();
                mHasNewScene = false;
            }
        });
    }

    public MagicScene render(Object... sceneComponents) {
        List<MagicSurface> surfaces = new ArrayList<>();
        List<Light> lights = new ArrayList<>();
        int color = -1;
        for (Object c : sceneComponents) {
            if (c instanceof MagicSurface) {
                surfaces.add((MagicSurface)c);
            } else if (c instanceof Light) {
                lights.add((Light) c);
            } else if (c instanceof Integer) {
                color = (int) c;
            } else {
                throw new IllegalArgumentException();
            }
        }
        MagicSceneBuilder builder = new MagicSceneBuilder(this);
        if (surfaces.size() > 0) {
            MagicSurface[] a = new MagicSurface[surfaces.size()];
            surfaces.toArray(a);
            builder.addSurfaces(a);
        }
        if (lights.size() > 0) {
            Light[] a = new Light[lights.size()];
            lights.toArray(a);
            builder.addLights(a);
        }
        if (color != -1) {
            builder.ambientColor(color);
        }
        MagicScene scene = builder.build();
        render(builder.build());
        return scene;
    }

    public void render(MagicScene magicScene) {
        mHasNewScene = true;
        mRenderer.render(magicScene);
        if (getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        } else {
            onResume();
        }
    }

    @Override
    public void onPause() {
        mHasNewScene = false;
        mRenderer.pause();
    }

    @Override
    public void setVisibility(final int visibility) {
        if (visibility == VISIBLE) {
            super.setVisibility(VISIBLE);
            onResume();
        } else {
            onPause();
            postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!mHasNewScene) {
                        MagicSurfaceView.super.setVisibility(visibility);
                    }
                }
            }, 15);
        }
    }
}
