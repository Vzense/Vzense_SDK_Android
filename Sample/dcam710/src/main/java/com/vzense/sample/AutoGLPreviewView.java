package com.vzense.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class AutoGLPreviewView extends FrameLayout {

    public GLSurfaceView textureView;
    private MyRenderer render;
    private int videoWidth = 0;
    private int videoHeight = 0;


    private int previewWidth = 0;
    private int previewHeight = 0;
    private static int scale = 2;

    public AutoGLPreviewView(Context context) {
        super(context);
        init();
    }

    public AutoGLPreviewView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AutoGLPreviewView(Context context, AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }


    private Handler handler = new Handler(Looper.getMainLooper());

    private void init() {
        textureView = new GLSurfaceView(getContext());
        textureView.setEGLContextClientVersion(2);
        render = new MyRenderer();
        textureView.setRenderer(render);
        textureView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        addView(textureView);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        previewWidth = getWidth();
        previewHeight = getHeight();

        if (videoWidth == 0 || videoHeight == 0 || previewWidth == 0 || previewHeight == 0) {
            return;
        }


        if (previewWidth * videoHeight > previewHeight * videoWidth) {
            int adjustPreviewWidth = videoWidth * previewHeight / videoHeight;
            textureView.layout((previewWidth - adjustPreviewWidth)/2, 0,
                    (previewWidth + adjustPreviewWidth)/2, previewHeight);
        } else {
            int adjustPreviewHeight = videoHeight * previewWidth / videoWidth;
            textureView.layout(0, (previewHeight - adjustPreviewHeight) / 2,
                    previewWidth, (previewHeight + adjustPreviewHeight) / 2);
        }


    }

    public GLSurfaceView getTextureView() {
        return textureView;
    }

    public int getPreviewWidth() {
        return previewWidth;
    }

    public int getPreviewHeight() {
        return previewHeight;
    }

    public void setPreviewSize(int width, int height) {
        if (this.videoWidth == width && this.videoHeight == height) {
            return;
        }
        this.videoWidth = width;
        this.videoHeight = height;
        handler.post(new Runnable() {
            @Override
            public void run() {
                requestLayout();
            }
        });

    }

    public void draw(Bitmap bm){
        int h = bm.getHeight();
        int w = bm.getWidth();
        if (videoWidth != w || videoHeight != h)
        {
            setPreviewSize(w, h);
        }
        render.setBuf(bm);
        textureView.requestRender();
    }




}
