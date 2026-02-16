package com.kiduyu.klaus.kiduyutv.utils;



import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.core.content.FileProvider;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

import org.json.JSONArray;
import org.json.JSONObject;

import net.lingala.zip4j.ZipFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class SubdlService {
    private static final String TAG = "SubdlService";
    private static final String BASE_URL = "https://api.subdl.com/api/v1/subtitles";
    private static final String API_KEY = "weZQC0DkGXUBp_n-oLi-6-KoW6kjLQ0g";

    private final OkHttpClient client;
    private final Context context;

    public interface Callback {
        void onSuccess(List<Subtitle> subtitles);
        void onError(String error);
    }

    public SubdlService(Context context) {
        this.context = context.getApplicationContext();

        // Setup caching (10MB)
        File httpCacheDir = new File(context.getCacheDir(), "http");
        Cache cache = new Cache(httpCacheDir, 10 * 1024 * 1024);

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        client = new OkHttpClient.Builder()
                .cache(cache)
                .addInterceptor(logging)
                .build();
    }

    public void fetchSubtitles(
            int tmdbId,
            String type,
            String language,
            Callback cb
    ) {
        String url = BASE_URL +
                "?api_key=" + API_KEY +
                "&tmdb_id=" + tmdbId +
                "&type=" + type +
                "&languages=" + language;

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .build();

        new Thread(() -> {
            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    cb.onError("HTTP " + response.code());
                    return;
                }

                String jsonText = response.body().string();
                JSONObject root = new JSONObject(jsonText);

                if (!root.optBoolean("status")) {
                    cb.onError("API returned false status");
                    return;
                }

                JSONArray arr = root.optJSONArray("subtitles");
                if (arr == null || arr.length() == 0) {
                    cb.onError("No subtitles found");
                    return;
                }

                List<Subtitle> out = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);

                    String dlPath = o.optString("url");
                    String zipUrl = "https://dl.subdl.com" + dlPath;
                    Log.i(TAG, "Downloading " + zipUrl);


                    // download & extract
                    Uri srtUri = downloadAndExtractSrt(zipUrl);

                    if (srtUri != null) {
                        Subtitle s = new Subtitle();
                        s.language = o.optString("language");
                        s.release = o.optString("release");
                        s.srtUri = srtUri;
                        out.add(s);
                    }
                }

                cb.onSuccess(out);

            } catch (Exception e) {
                cb.onError(e.getMessage());
            }
        }).start();
    }

    private Uri downloadAndExtractSrt(String zipUrl) {
        try {
            Request zipReq = new Request.Builder().url(zipUrl).build();
            Response zipResp = client.newCall(zipReq).execute();
            if (!zipResp.isSuccessful()) return null;

            // save zip to cache
            File zipFile = new File(context.getCacheDir(), "subtitle_" + System.currentTimeMillis() + ".zip");
            Log.i(TAG, "Saving zip to " + zipFile.getAbsolutePath());
            try (InputStream in = zipResp.body().byteStream();
                 FileOutputStream fos = new FileOutputStream(zipFile)) {
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) {
                    fos.write(buf, 0, r);
                }
                fos.flush();
            }

            // unzip
            ZipFile zip = new ZipFile(zipFile);
            zip.extractAll(context.getCacheDir().getAbsolutePath());

            // find .srt
            for (File f : context.getCacheDir().listFiles()) {
                Log.i(TAG, "Found file: " + f.getName());
                if (f.getName().endsWith(".srt")) {
                    Log.i(TAG, "Found .srt file: " + f.getAbsolutePath());
                    Log.i(TAG, "FileProvider.getUriForFile: " + FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".provider",
                            f
                    ));
                    return FileProvider.getUriForFile(
                            context,
                            context.getPackageName() + ".provider",
                            f
                    );
                }
            }

        } catch (Exception e) {
            Log.e("SUBDL_PROVIDER", "FileProvider error", e);
            return null;
        }

        return null;
    }
}

