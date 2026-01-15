package com.kiduyu.klaus.kiduyutv.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class SurfaceViewImageLoader {
    private static final String TAG = "SurfaceViewImageLoader";

    public void loadImageToSurfaceView(SurfaceView videoSurface, String imageUrl) {
        Glide.with(videoSurface.getContext())
                .asBitmap()
                .load(imageUrl)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        drawBitmapOnSurface(videoSurface, bitmap);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {

                    }
                });
    }

    private void drawBitmapOnSurface(SurfaceView surfaceView, Bitmap bitmap) {
        SurfaceHolder holder = surfaceView.getHolder();
        Canvas canvas = null;

        try {
            // Check if surface is valid before locking
            if (!holder.getSurface().isValid()) {
                Log.w(TAG, "Surface not valid, cannot draw bitmap");
                return;
            }

            canvas = holder.lockCanvas();
            if (canvas != null) {
                // Clear the canvas
                canvas.drawColor(Color.BLACK);

                // Draw the bitmap scaled to fit the surface
                android.graphics.Rect destRect = new android.graphics.Rect(
                        0, 0, canvas.getWidth(), canvas.getHeight()
                );
                canvas.drawBitmap(bitmap, null, destRect, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error drawing bitmap on surface", e);
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    Log.e(TAG, "Error unlocking canvas", e);
                }
            }
        }
    }

    /**
     * Clear the SurfaceView by filling it with black
     * Should be called BEFORE ExoPlayer takes control
     */
    public void clearSurfaceView(SurfaceView surfaceView) {
        SurfaceHolder holder = surfaceView.getHolder();
        Canvas canvas = null;

        try {
            // Check if surface is valid before locking
            if (!holder.getSurface().isValid()) {
                Log.w(TAG, "Surface not valid, cannot clear");
                return;
            }

            canvas = holder.lockCanvas();
            if (canvas != null) {
                // Clear to black
                canvas.drawColor(Color.BLACK);
            }
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Surface already released or in use by another thread", e);
        } catch (Exception e) {
            Log.e(TAG, "Error clearing surface", e);
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    Log.e(TAG, "Error unlocking canvas", e);
                }
            }
        }
    }
}