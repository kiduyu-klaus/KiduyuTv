package com.kiduyu.klaus.kiduyutv.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple image loader for ImageView (doesn't interfere with SurfaceView/ExoPlayer)
 */
public class ImageViewImageLoader {
    private static final String TAG = "ImageViewImageLoader";
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public ImageViewImageLoader() {
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Load image from URL into ImageView
     */
    public void loadImageToImageView(ImageView imageView, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            Log.w(TAG, "Empty image URL");
            return;
        }

        executorService.execute(() -> {
            try {
                Log.i(TAG, "Loading image: " + imageUrl);
                Bitmap bitmap = downloadImage(imageUrl);

                if (bitmap != null) {
                    mainHandler.post(() -> {
                        imageView.setImageBitmap(bitmap);
                        Log.i(TAG, "Image loaded successfully");
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading image", e);
            }
        });
    }

    private Bitmap downloadImage(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.connect();

            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = connection.getInputStream();
                return BitmapFactory.decodeStream(input);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error downloading image", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}