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


    public void fetchTvSubtitles(
            int tmdbId,
            int seasonNumber,
            int episodeNumber,
            String language,
            Callback cb
    ) {

        String url = BASE_URL +
                "?api_key=" + API_KEY +
                "&tmdb_id=" + tmdbId +
                "&type=tv" +
                "&season_number=" + seasonNumber +
                "&episode_number=" + episodeNumber +
                "&languages=" + language +
                "&releases=1";

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
                    cb.onSuccess(new ArrayList<>());
                    return;
                }

                List<Subtitle> out = new ArrayList<>();

                for (int i = 0; i < arr.length(); i++) {

                    JSONObject o = arr.getJSONObject(i);

                    // Skip hearing impaired if you want normal subtitles
                    if (o.optBoolean("hi", false))
                        continue;

                    String path = o.optString("url");
                    if (path == null || path.isEmpty())
                        continue;

                    String zipUrl = "https://dl.subdl.com" + path;

                    Uri srtUri = downloadAndExtractSrt(zipUrl);

                    if (srtUri != null) {
                        Subtitle s = new Subtitle();
                        s.language = o.optString("language");
                        s.release = o.optString("release_name");
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

            // Create a temporary directory for extraction with a short name
            File extractDir = new File(context.getCacheDir(), "sub_extract_" + System.currentTimeMillis());
            extractDir.mkdirs();

            // unzip with custom extraction handling
            try (java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(
                    new java.io.FileInputStream(zipFile))) {

                java.util.zip.ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;

                    String entryName = entry.getName();

                    // Check if it's an SRT file
                    if (entryName.toLowerCase().endsWith(".srt")) {
                        // Generate a safe short filename
                        String safeFileName = "subtitle_" + System.currentTimeMillis() + "_" +
                                (entryName.hashCode() & 0x7FFFFFFF) + ".srt";

                        File outputFile = new File(extractDir, safeFileName);

                        // Extract the file
                        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                            byte[] buffer = new byte[4096];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        }

                        Log.i(TAG, "Extracted SRT file: " + outputFile.getAbsolutePath());

                        // Return URI for the extracted file
                        return FileProvider.getUriForFile(
                                context,
                                context.getPackageName() + ".provider",
                                outputFile
                        );
                    }
                }
            }

            // Clean up the zip file
            zipFile.delete();

        } catch (Exception e) {
            Log.e("SUBDL_PROVIDER", "Error downloading/extracting subtitle", e);
            return null;
        }

        return null;
    }


}

