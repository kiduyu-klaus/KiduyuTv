package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;


import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FetchStreams {
    private static final String TAG = "MediaRepository";
    private static final String VIDEASY_API_BASE = "https://api.videasy.net";
    private static final String DECRYPT_API = "https://enc-dec.app/api/dec-videasy";
    // Add these constants at the top of the class
    private static final String HEXA_API_BASE = "https://themoviedb.hexa.su/api/tmdb";
    private static final String DECRYPT_HEXA_API = "https://enc-dec.app/api/dec-hexa";
    private static final int TIMEOUT_MS = 10000; // timeout

    private static FetchStreams instance;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static synchronized FetchStreams getInstance() {
        if (instance == null) instance = new FetchStreams();
        return instance;
    }

    public interface VideasyCallback {
        void onSuccess(MediaItems item);
        void onError(String error);
    }

    public void fetchVideasyStreamsMovie(String title, String year, String tmdbId, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                String encodedTitle = URLEncoder.encode(title, "UTF-8");
                String url = VIDEASY_API_BASE + "/myflixerzupcloud/sources-with-title?title=" + encodedTitle +
                        "&mediaType=movie&year=" + year + "&tmdbId=" + tmdbId;

                MediaItems result = fetchStreams(url, tmdbId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchVideasyStreamsTV(String title, String year, String tmdbId,
                                      String season, String episode, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                String encodedTitle = URLEncoder.encode(title, "UTF-8");
                String url = VIDEASY_API_BASE + "/myflixerzupcloud/sources-with-title?title=" + encodedTitle +
                        "&mediaType=tv&year=" + year + "&tmdbId=" + tmdbId +
                        "&seasonId=" + season + "&episodeId=" + episode;

                MediaItems result = fetchStreams(url, tmdbId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchStreams(String url, String tmdbId) throws Exception {

        Connection.Response enc = Jsoup.connect(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Referer", "https://api.videasy.net/")
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject payload = new JSONObject();
        payload.put("text", enc.body());
        payload.put("id", tmdbId);

        // Capture all response headers
        Map<String, String> responseHeaders = new HashMap<>();
        Log.i(TAG, "Response Headers:");
        for (Map.Entry<String, String> header : enc.headers().entrySet()) {
            responseHeaders.put(header.getKey(), header.getValue());
            Log.i(TAG, "  " + header.getKey() + ": " + header.getValue());
        }

        Connection.Response dec = Jsoup.connect(DECRYPT_API)
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent())
                .requestBody(payload.toString())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.POST)
                .execute();

        JSONObject data = new JSONObject(new JSONObject(dec.body()).getString("result"));
        Log.i(TAG, "Decrypted data: " + data.toString());


        List<MediaItems.VideoSource> sources = new ArrayList<>();
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();

        JSONArray s = data.optJSONArray("sources");
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                JSONObject o = s.getJSONObject(i);
                sources.add(new MediaItems.VideoSource(o.optString("quality"), o.optString("url")));
            }
        }
        Log.i(TAG, "Sources count: " + sources.size());


        JSONArray sub = data.optJSONArray("subtitles");
        if (sub != null) {
            for (int i = 0; i < sub.length(); i++) {
                JSONObject o = sub.getJSONObject(i);
                Log.i(TAG, "Subtitle: " + o.toString());
                subs.add(new MediaItems.SubtitleItem(
                        o.optString("url"),
                        o.optString("lang"),
                        o.optString("language")
                ));
            }
        }
        Log.i(TAG, "Subtitles count: " + subs.size());


        MediaItems item = new MediaItems();
        item.setRefererUrl(url);
        // Set comprehensive custom headers
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36");
        customHeaders.put("Accept", "*/*");
        customHeaders.put("Accept-Language", "en-US,en;q=0.9");
        customHeaders.put("Accept-Encoding", "gzip, deflate, br");
        customHeaders.put("Origin", VIDEASY_API_BASE);
        customHeaders.put("Referer", url);
        customHeaders.put("Report-To", "keep-alive");
        customHeaders.put("Connection", "keep-alive");
        customHeaders.put("Range", "bytes=0-");
        customHeaders.put("Sec-Fetch-Dest", "video");
        customHeaders.put("Sec-Fetch-Mode", "cors");
        customHeaders.put("Sec-Fetch-Site", "cross-site");



        // Add Cloudflare-specific headers if present
        if (responseHeaders.containsKey("cf-ray")) {
            customHeaders.put("CF-RAY", responseHeaders.get("cf-ray"));
        }

        item.setCustomHeaders(customHeaders);
        item.setResponseHeaders(responseHeaders);

        Log.i(TAG, "Custom headers count: " + item.getResponseHeaders().size());


        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(url);

        return item;
    }



    // Add this method to generate random hex key
    private String generateHexKey() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        StringBuilder hexString = new StringBuilder();
        for (byte b : randomBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    // Movie method
    public void fetchHexaStreamsMovie(String tmdbId, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                String hexKey = generateHexKey();
                String url = HEXA_API_BASE + "/movie/" + tmdbId + "/images";

                MediaItems result = fetchHexaStreams(url, hexKey);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Hexa Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // TV Show method
    public void fetchHexaStreamsTV(String tmdbId, String season, String episode, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                String hexKey = generateHexKey();
                String url = HEXA_API_BASE + "/tv/" + tmdbId + "/season/" + season + "/episode/" + episode + "/images";

                MediaItems result = fetchHexaStreams(url, hexKey);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Hexa TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // Helper method to fetch and decrypt Hexa streams
    private MediaItems fetchHexaStreams(String url, String hexKey) throws Exception {

        // Fetch encrypted data with X-Api-Key header
        Connection.Response enc = Jsoup.connect(url)
                .header("User-Agent", userAgent())
                .header("Accept", "plain/text")
                .header("X-Api-Key", hexKey)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        // Prepare decrypt request
        JSONObject payload = new JSONObject();
        payload.put("text", enc.body());
        payload.put("key", hexKey);

        // Decrypt the response
        Connection.Response dec = Jsoup.connect(DECRYPT_HEXA_API)
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent())
                .requestBody(payload.toString())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.POST)
                .execute();

        JSONObject data = new JSONObject(new JSONObject(dec.body()).getString("result"));

        List<MediaItems.VideoSource> sources = new ArrayList<>();
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();

        // Parse sources
        JSONArray s = data.optJSONArray("sources");
        if (s != null) {
            for (int i = 0; i < s.length(); i++) {
                JSONObject o = s.getJSONObject(i);
                sources.add(new MediaItems.VideoSource(o.optString("quality"), o.optString("url")));
            }
        }

        // Parse subtitles
        JSONArray sub = data.optJSONArray("subtitles");
        if (sub != null) {
            for (int i = 0; i < sub.length(); i++) {
                JSONObject o = sub.getJSONObject(i);
                subs.add(new MediaItems.SubtitleItem(
                        o.optString("url"),
                        o.optString("lang"),
                        o.optString("language")
                ));
            }
        }

        MediaItems item = new MediaItems();
        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(url);

        return item;
    }

    // Add these constants at the top of the class
    private static final String SMASHYSTREAM_API_BASE = "https://api.smashystream.top/api/v1";
    private static final String ENCRYPT_VIDSTACK_API = "https://enc-dec.app/api/enc-vidstack";
    private static final String DECRYPT_VIDSTACK_API = "https://enc-dec.app/api/dec-vidstack";

    // Server types enum
    public enum SmashyServer {
        SMASHYSTREAM("videosmashyi", 1),      // Player SY - type 1
        SHORT2EMBED("short2embed", 2),         // Player SM - type 2
        VIDEOOPHIM("videoophim", 2),           // Player O - type 2
        VIDEOFSH("videofsh", 2);               // Player FS - type 2

        final String endpoint;
        final int type;

        SmashyServer(String endpoint, int type) {
            this.endpoint = endpoint;
            this.type = type;
        }
    }

    // Movie method
    public void fetchSmashyStreamsMovie(String imdbId, String tmdbId, SmashyServer server, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                // Get token data first
                Map<String, String> tokenData = getVidstackToken();

                MediaItems result;
                if (server.type == 1) {
                    result = fetchSmashyType1Movie(imdbId, tokenData, server);
                } else {
                    result = fetchSmashyType2Movie(tmdbId, tokenData, server);
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "SmashyStream Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    // TV Show method
    public void fetchSmashyStreamsTV(String imdbId, String tmdbId, String season, String episode,
                                     SmashyServer server, VideasyCallback callback) {
        executorService.execute(() -> {
            try {
                // Get token data first
                Map<String, String> tokenData = getVidstackToken();

                MediaItems result;
                if (server.type == 1) {
                    result = fetchSmashyType1TV(imdbId, season, episode, tokenData, server);
                } else {
                    result = fetchSmashyType2TV(tmdbId, season, episode, tokenData, server);
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "SmashyStream TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Unknown error"));
            }
        });
    }

    // Get token data for vidstack
    private Map<String, String> getVidstackToken() throws Exception {
        Connection.Response response = Jsoup.connect(ENCRYPT_VIDSTACK_API)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject result = new JSONObject(new JSONObject(response.body()).getString("result"));

        Map<String, String> tokenData = new HashMap<>();
        tokenData.put("token", result.getString("token"));
        tokenData.put("user_id", result.getString("user_id"));

        return tokenData;
    }

    // Type 1 - Movie (smashystream)
    private MediaItems fetchSmashyType1Movie(String imdbId, Map<String, String> tokenData, SmashyServer server) throws Exception {
        // Get player parts
        String url = SMASHYSTREAM_API_BASE + "/" + server.endpoint + "/" + imdbId +
                "?token=" + tokenData.get("token") + "&user_id=" + tokenData.get("user_id");

        Connection.Response response = Jsoup.connect(url)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject json = new JSONObject(response.body());
        String data = json.getString("data");
        String[] parts = data.split("/#");
        String host = parts[0];
        String id = parts[1];

        // Get encrypted stream data
        String streamUrl = host + "/api/v1/video?id=" + id;
        Connection.Response streamResponse = Jsoup.connect(streamUrl)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        return decryptVidstack(streamResponse.body(), server.type, streamUrl);
    }

    // Type 1 - TV Show (smashystream)
    private MediaItems fetchSmashyType1TV(String imdbId, String season, String episode,
                                          Map<String, String> tokenData, SmashyServer server) throws Exception {
        // Get player parts
        String url = SMASHYSTREAM_API_BASE + "/" + server.endpoint + "/" + imdbId + "/" + season + "/" + episode +
                "?token=" + tokenData.get("token") + "&user_id=" + tokenData.get("user_id");

        Connection.Response response = Jsoup.connect(url)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject json = new JSONObject(response.body());
        String data = json.getString("data");
        String[] parts = data.split("/#");
        String host = parts[0];
        String id = parts[1];

        // Get encrypted stream data
        String streamUrl = host + "/api/v1/video?id=" + id;
        Connection.Response streamResponse = Jsoup.connect(streamUrl)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        return decryptVidstack(streamResponse.body(), server.type, streamUrl);
    }

    // Type 2 - Movie (videofsh, short2embed, videoophim)
    private MediaItems fetchSmashyType2Movie(String tmdbId, Map<String, String> tokenData, SmashyServer server) throws Exception {
        String url = SMASHYSTREAM_API_BASE + "/" + server.endpoint + "/" + tmdbId +
                "?token=" + tokenData.get("token") + "&user_id=" + tokenData.get("user_id");

        Log.i(TAG, "URL: " + url);


        Connection.Response response = Jsoup.connect(url)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject json = new JSONObject(response.body());
        JSONObject data = json.getJSONObject("data");

        // Get encrypted file
        String encryptedFile = data.getJSONArray("sources").getJSONObject(0).getString("file");
        String subtitles = data.optString("tracks", "");

        return decryptVidstackType2(encryptedFile, subtitles, url);
    }

    // Type 2 - TV Show (videofsh, short2embed, videoophim)
    private MediaItems fetchSmashyType2TV(String tmdbId, String season, String episode,
                                          Map<String, String> tokenData, SmashyServer server) throws Exception {
        String url = SMASHYSTREAM_API_BASE + "/" + server.endpoint + "/" + tmdbId + "/" + season + "/" + episode +
                "?token=" + tokenData.get("token") + "&user_id=" + tokenData.get("user_id");

        Connection.Response response = Jsoup.connect(url)
                .header("User-Agent", userAgent())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .execute();

        JSONObject json = new JSONObject(response.body());
        JSONObject data = json.getJSONObject("data");

        // Get encrypted file
        String encryptedFile = data.getJSONArray("sources").getJSONObject(0).getString("file");
        String subtitles = data.optString("tracks", "");

        return decryptVidstackType2(encryptedFile, subtitles, url);
    }

    // Decrypt vidstack data (Type 1)
    private MediaItems decryptVidstack(String encryptedText, int type, String refererUrl) throws Exception {
        JSONObject payload = new JSONObject();
        payload.put("text", encryptedText);
        payload.put("type", String.valueOf(type));

        Connection.Response dec = Jsoup.connect(DECRYPT_VIDSTACK_API)
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent())
                .requestBody(payload.toString())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.POST)
                .execute();

        JSONObject result = new JSONObject(new JSONObject(dec.body()).getString("result"));

        List<MediaItems.VideoSource> sources = new ArrayList<>();
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();

        // Parse sources
        JSONArray sourcesArray = result.optJSONArray("sources");
        if (sourcesArray != null) {
            for (int i = 0; i < sourcesArray.length(); i++) {
                JSONObject o = sourcesArray.getJSONObject(i);
                sources.add(new MediaItems.VideoSource(o.optString("quality"), o.optString("url")));
            }
        }

        // Parse subtitles
        JSONArray subsArray = result.optJSONArray("subtitles");
        if (subsArray != null) {
            for (int i = 0; i < subsArray.length(); i++) {
                JSONObject o = subsArray.getJSONObject(i);
                subs.add(new MediaItems.SubtitleItem(
                        o.optString("url"),
                        o.optString("lang"),
                        o.optString("language")
                ));
            }
        }

        MediaItems item = new MediaItems();
        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(refererUrl);

        return item;
    }

    // Decrypt vidstack data (Type 2)
    private MediaItems decryptVidstackType2(String encryptedFile, String encryptedSubtitles, String refererUrl) throws Exception {
        // Decrypt file URL
        JSONObject filePayload = new JSONObject();
        filePayload.put("text", encryptedFile);
        filePayload.put("type", "2");

        Connection.Response fileDec = Jsoup.connect(DECRYPT_VIDSTACK_API)
                .header("Content-Type", "application/json")
                .header("User-Agent", userAgent())
                .requestBody(filePayload.toString())
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.POST)
                .execute();

        String decryptedFile = new JSONObject(fileDec.body()).getString("result");

        List<MediaItems.VideoSource> sources = parseListFormat(decryptedFile);
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();

        // Decrypt subtitles if present
        if (encryptedSubtitles != null && !encryptedSubtitles.isEmpty()) {
            JSONObject subPayload = new JSONObject();
            subPayload.put("text", encryptedSubtitles);
            subPayload.put("type", "2");

            Connection.Response subDec = Jsoup.connect(DECRYPT_VIDSTACK_API)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", userAgent())
                    .requestBody(subPayload.toString())
                    .ignoreContentType(true)
                    .timeout(TIMEOUT_MS)
                    .method(Connection.Method.POST)
                    .execute();

            String decryptedSubs = new JSONObject(subDec.body()).getString("result");
            subs = parseSubtitleFormat(decryptedSubs);
        }

        MediaItems item = new MediaItems();
        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(refererUrl);

        return item;
    }

    // Parse list format (e.g., "[Auto] url1, [720p] url2, [1080p] url3")
    private List<MediaItems.VideoSource> parseListFormat(String text) {
        List<MediaItems.VideoSource> sources = new ArrayList<>();

        text = text.trim().replaceAll(",$", "").replace(" or ", ",");
        String[] items = text.split(",");

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            if (item.startsWith("[") && item.contains("]")) {
                int closeBracket = item.indexOf("]");
                String quality = item.substring(1, closeBracket).trim();
                String url = item.substring(closeBracket + 1).trim();
                sources.add(new MediaItems.VideoSource(quality, url));
            } else {
                sources.add(new MediaItems.VideoSource("default", item));
            }
        }

        return sources;
    }

    // Parse subtitle format
    private List<MediaItems.SubtitleItem> parseSubtitleFormat(String text) {
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();

        text = text.trim().replaceAll(",$", "").replace(" or ", ",");
        String[] items = text.split(",");

        for (String item : items) {
            item = item.trim();
            if (item.isEmpty()) continue;

            if (item.startsWith("[") && item.contains("]")) {
                int closeBracket = item.indexOf("]");
                String lang = item.substring(1, closeBracket).trim();
                String url = item.substring(closeBracket + 1).trim();
                subs.add(new MediaItems.SubtitleItem(url, lang, lang));
            } else {
                subs.add(new MediaItems.SubtitleItem(item, "unknown", "Unknown"));
            }
        }

        return subs;
    }

    private String userAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/143.0.0.0 Safari/537.36";
    }

    public void cleanup() {
        executorService.shutdown();
    }
}
