package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TmdbRepository {
    private static final String TAG = "TmdbRepository";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    private static TmdbRepository instance;

    private static final String CUSTOM_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Thread management
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Callback interface for async operations
    public interface TMDBCallback {
        void onSuccess(List<MediaItems> movies);

        void onError(String error);
    }

    public void getFeaturedMoviesAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&region=US&sort_by=popularity.desc";
                List<MediaItems> movies = TmdbApi.fetchMoviesFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(movies));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching featured movies", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
