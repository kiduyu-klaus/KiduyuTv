package com.kiduyu.klaus.kiduyutv.Api;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.CloudflareSessionHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.Url;

public class FetchStreams {
    private static final String TAG = "FetchStreams";

    // Base URLs
    private static final String ENC_DEC_API = "https://enc-dec.app/";
    private static final String VIDEASY_API_BASE = "https://api.videasy.net";
    private static final String HEXA_API_BASE = "https://themoviedb.hexa.su";
    private static final String MAPPLE_API_BASE = "https://mapple.uk";
    private static final String ONETOUCHTV_API_BASE = "https://api3.devcorp.me";
    private static final String SMASHYSTREAM_API_BASE = "https://api.smashystream.top";
    private static final String VIDLINK_API_BASE = "https://vidlink.pro";
    private static final String YFLIX_API_BASE = "https://yflix.to";
    private static final String XPRIME_API_BASE = "https://backend.xprime.tv";
    private static final String KISSKH_API_BASE = "https://kisskh.do";
    private static final String ANIMEKAI_API_BASE = "https://animekai.to";
    private static final String DATABASE_BASE = "https://enc-dec.app/db";

    private static final int TIMEOUT_SECONDS = 30;

    private final EncDecApi encDecApi;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final OkHttpClient okHttpClient;

    // Application context for ApiClient
    private static Context applicationContext;




    static class DecryptResponseType2 {
        int status;
        String result;  // ✅ String for Type 2
    }

    static class VidlinkEncryptResponse {
        String result;  // Just a string, not an object
    }
    // Retrofit service interfaces
    interface EncDecApi {
        @POST("api/dec-videasy")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptVideasy(@Body DecryptRequest request);

        @POST("api/dec-hexa")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptHexa(@Body DecryptRequest request);

        @POST("api/dec-onetouchtv")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptOnetouchtv(@Body DecryptRequest request);

        @POST("api/dec-vidstack")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptVidstack(@Body VidstackDecryptRequest request);

        @POST("api/dec-vidstack")
        @Headers("Content-Type: application/json")
        Call<DecryptResponseType2> decryptVidstackType2(@Body VidstackDecryptRequest request);

        @POST("api/dec-rapid")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptRapid(@Body RapidDecryptRequest request);

        @POST("api/dec-movies-flix")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptMoviesFlix(@Body DecryptRequest request);

        @POST("api/parse-html")
        @Headers("Content-Type: application/json")
        Call<ParseHtmlResponse> parseHtml(@Body ParseHtmlRequest request);

        @GET("api/enc-vidstack")
        Call<EncryptResponse> encryptVidstack();

        @GET("api/enc-vidlink")
        Call<VidlinkEncryptResponse> encryptVidlink(@Query("text") String text);

        @GET("api/enc-movies-flix")
        Call<EncryptResponse> encryptMoviesFlix(@Query("text") String text);

        @GET("api/enc-mapple")
        Call<MappleEncryptResponse> encryptMapple();

        // Megaup (similar to Rapidshare)
        @POST("api/dec-mega")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptMega(@Body RapidDecryptRequest request);

        // XPrime
        @GET("api/enc-xprime")
        Call<EncryptResponse> encryptXprime();

        @POST("api/dec-xprime")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptXprime(@Body DecryptRequest request);

        // KissKH
        @GET("api/enc-kisskh")
        Call<EncryptResponse> encryptKisskh(@Query("text") String text, @Query("type") String type);

        @GET("api/dec-kisskh")
        Call<String> decryptKisskh(@Query("url") String url);

        // AnimeKai
        @GET("api/enc-kai")
        Call<EncryptResponse> encryptKai(@Query("text") String text);

        @POST("api/dec-kai")
        @Headers("Content-Type: application/json")
        Call<DecryptResponse> decryptKai(@Body DecryptRequest request);
    }

    // Generic API interface for dynamic URLs
    interface GenericApi {
        @GET
        @Headers("Accept: */*")
        Call<String> getString(@Url String url, @Header("User-Agent") String userAgent,
                               @Header("X-Api-Key") String apiKey);

        @GET
        @Headers("Accept: application/json")
        Call<ResponseBody> getResponse(@Url String url, @Header("User-Agent") String userAgent,
                                       @Header("Referer") String referer);

        @POST
        @Headers("Content-Type: application/json")
        Call<ResponseBody> postJson(@Url String url, @Body Object body,
                                    @Header("User-Agent") String userAgent,
                                    @Header("Referer") String referer,
                                    @Header("Next-Action") String nextAction);
    }

    // Data classes
    static class DecryptRequest {
        String text;
        String id;
        String key;

        private DecryptRequest(String text, String id, String key) {
            this.text = text;
            this.id = id;
            this.key = key;
        }

        static DecryptRequest withId(String text, String id) {
            return new DecryptRequest(text, id, null);
        }

        static DecryptRequest withKey(String text, String key) {
            return new DecryptRequest(text, null, key);
        }
    }


    static class VidstackDecryptRequest {
        String text;
        String type;

        VidstackDecryptRequest(String text, String type) {
            this.text = text;
            this.type = type;
        }
    }

    static class RapidDecryptRequest {
        String text;
        String agent;

        RapidDecryptRequest(String text, String agent) {
            this.text = text;
            this.agent = agent;
        }
    }

    static class ParseHtmlRequest {
        String text;

        ParseHtmlRequest(String text) {
            this.text = text;
        }
    }

    static class DecryptResponse {
        int status;
        Result result;  // ✅ Change to object

        static class Result {
            List<Source> sources;
            List<Subtitle> subtitles;

            static class Source {
                String url;
                String quality;
            }

            static class Subtitle {
                String url;
                String lang;
                String language;
            }
        }
    }

    static class EncryptResponse {
        EncryptResult result;

        static class EncryptResult {
            String token;
            String user_id;
            String sessionId;
            String nextAction;
        }
    }

    static class MappleEncryptResponse {
        MappleResult result;

        static class MappleResult {
            String sessionId;
            String nextAction;
        }
    }

    static class ParseHtmlResponse {
        Object result;
    }

    // Callback interface
    public interface StreamCallback {
        void onSuccess(MediaItems item);
        void onError(String error);
    }

    public FetchStreams(Context context) {
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        // Store application context for ApiClient
        if (applicationContext == null) {
            applicationContext = context.getApplicationContext();
        }

        // Use the shared OkHttpClient from ApiClient
        okHttpClient = ApiClient.getClient(applicationContext);

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENC_DEC_API)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        encDecApi = retrofit.create(EncDecApi.class);
    }

    /**
     * Get the shared OkHttpClient instance for use with ExoPlayer streaming.
     * This allows sharing the connection pool and cookies between API calls and media playback.
     * Note: Context is required for first initialization to set up persistent CookieJar.
     */
    public static OkHttpClient getSharedClient(Context context) {
        return ApiClient.getClient(context.getApplicationContext());
    }

    // ===================== VIDEASY =====================
    public void fetchVideasyMovie(String title, String year, String tmdbId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String encodedTitle = URLEncoder.encode(title, "UTF-8");
                String url = VIDEASY_API_BASE + "/myflixerzupcloud/sources-with-title?title=" + encodedTitle +
                        "&mediaType=movie&year=" + year + "&tmdbId=" + tmdbId;

                MediaItems result = fetchVideasyStreams(url, tmdbId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Videasy Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchVideasyTV(String title, String year, String tmdbId, String season,
                               String episode, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String encodedTitle = URLEncoder.encode(title, "UTF-8");
                String url = VIDEASY_API_BASE + "/myflixerzupcloud/sources-with-title?title=" + encodedTitle +
                        "&mediaType=tv&year=" + year + "&tmdbId=" + tmdbId +
                        "&seasonId=" + season + "&episodeId=" + episode;

                MediaItems result = fetchVideasyStreams(url, tmdbId);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Videasy TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchVideasyStreams(String url, String tmdbId) throws Exception {
        GenericApi api = createGenericApi(VIDEASY_API_BASE);
        Log.i(TAG, "Fetching from: " + url);

        String refererUrl = "https://videasy.net/";
        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), refererUrl).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch encrypted data");
        }

        String encrypted = response.body().string();



        Response<DecryptResponse> decResponse = encDecApi.decryptVideasy(
                DecryptRequest.withId(encrypted, tmdbId)).execute();

        if (!decResponse.isSuccessful() || decResponse.body() == null) {
            throw new IOException("Failed to decrypt");
        }

        // Convert DecryptResponse.Result to JSON string for parseStreamData
        DecryptResponse.Result result = decResponse.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);
        Log.i(TAG, "fetchVideasyMovie JSON: " + jsonStr);


        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchVideasyStreams: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }



        return parseStreamData(jsonStr, refererUrl, responseHeaders);
    }

    // ===================== HEXA =====================
    public void fetchHexaMovie(String tmdbId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String key = generateHexKey();
                String url = HEXA_API_BASE + "/api/tmdb/movie/" + tmdbId + "/images";

                MediaItems result = fetchHexaStreams(url, key);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Hexa Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchHexaTV(String tmdbId, String season, String episode, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String key = generateHexKey();
                String url = HEXA_API_BASE + "/api/tmdb/tv/" + tmdbId + "/season/" +
                        season + "/episode/" + episode + "/images";

                MediaItems result = fetchHexaStreams(url, key);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Hexa TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchHexaStreams(String url, String key) throws Exception {
        GenericApi api = createGenericApi(HEXA_API_BASE);

        Response<String> response = api.getString(url, getUserAgent(), key).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch encrypted data");
        }

        String encrypted = response.body();

        Response<DecryptResponse> decResponse = encDecApi.decryptHexa(
                DecryptRequest.withKey(encrypted, key)).execute();

        if (!decResponse.isSuccessful() || decResponse.body() == null) {
            throw new IOException("Failed to decrypt");
        }
        DecryptResponse.Result result = decResponse.body().result;
        Log.i(TAG, "DecryptResponse.Result: " + result.toString());
        String jsonStr = new com.google.gson.Gson().toJson(result);

        Log.i(TAG, "fetchHexaMovie JSON: " + jsonStr);
        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchHexaStreams: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }


        String refererUrl="https://hexa.su/";

        return parseStreamData(jsonStr, refererUrl, responseHeaders);
    }

    // ===================== ONETOUCHTV =====================
    public void fetchOnetouchtvMovie(String vodId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String url = ONETOUCHTV_API_BASE + "/web/vod/" + vodId + "/episode/1";
                MediaItems result = fetchOnetouchtvStreams(url);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "OneTouchTV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchOnetouchtvTV(String vodId, String episode, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                String url = ONETOUCHTV_API_BASE + "/web/vod/" + vodId + "/episode/" + episode;
                MediaItems result = fetchOnetouchtvStreams(url);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "OneTouchTV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchOnetouchtvStreams(String url) throws Exception {
        GenericApi api = createGenericApi(ONETOUCHTV_API_BASE);

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch encrypted data");
        }

        String encrypted = response.body().string();

        Response<DecryptResponse> decResponse = encDecApi.decryptOnetouchtv(
                DecryptRequest.withId(encrypted, null)).execute();
// or if it should be key-based:
// DecryptRequest.withKey(encrypted, null)


        if (!decResponse.isSuccessful() || decResponse.body() == null) {
            throw new IOException("Failed to decrypt");
        }
        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchOnetouchtvStreams: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }


        DecryptResponse.Result result = decResponse.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);

        return parseStreamData(jsonStr, url, responseHeaders);
    }

    // ===================== SMASHYSTREAM (VIDSTACK) =====================
    public void fetchSmashystreamMovie(String imdbId, String tmdbId, String serverType,
                                       StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<EncryptResponse> tokenResp = encDecApi.encryptVidstack().execute();
                if (!tokenResp.isSuccessful() || tokenResp.body() == null) {
                    throw new IOException("Failed to get token");
                }

                EncryptResponse.EncryptResult tokenData = tokenResp.body().result;
                MediaItems result;

                if ("1".equals(serverType)) {
                    result = fetchSmashystreamType1(imdbId, tokenData);
                } else {
                    result = fetchSmashystreamType2(tmdbId, tokenData, "videofsh");
                    Log.i(TAG, "fetchSmashystreamMovie result: " + result.toString());
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Smashystream Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchSmashystreamType1(String imdbId, EncryptResponse.EncryptResult tokenData)
            throws Exception {
        GenericApi api = createGenericApi(SMASHYSTREAM_API_BASE);

        String url = SMASHYSTREAM_API_BASE + "/api/v1/videosmashyi/" + imdbId +
                "?token=" + tokenData.token + "&user_id=" + tokenData.user_id;

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch player data");
        }

        JSONObject json = new JSONObject(response.body().string());
        String[] parts = json.getJSONObject("data").getString("data").split("/#");
        String host = parts[0];
        String id = parts[1];

        String streamUrl = host + "/api/v1/video?id=" + id;
        Response<ResponseBody> streamResp = api.getResponse(streamUrl, getUserAgent(), null).execute();

        if (!streamResp.isSuccessful() || streamResp.body() == null) {
            throw new IOException("Failed to fetch stream");
        }

        String encrypted = streamResp.body().string();
        Response<DecryptResponse> decResp = encDecApi.decryptVidstack(
                new VidstackDecryptRequest(encrypted, "1")).execute();

        if (!decResp.isSuccessful() || decResp.body() == null) {
            throw new IOException("Failed to decrypt");
        }
        DecryptResponse.Result result = decResp.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);
        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchSmashystreamType1: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }

        return parseStreamData(jsonStr, streamUrl, responseHeaders);
    }


    private MediaItems fetchSmashystreamType2(String tmdbId, EncryptResponse.EncryptResult tokenData,
                                              String server) throws Exception {
        GenericApi api = createGenericApi(SMASHYSTREAM_API_BASE);

        String url = SMASHYSTREAM_API_BASE + "/api/v1/" + server + "/" + tmdbId +
                "?token=" + tokenData.token + "&user_id=" + tokenData.user_id;

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch player data");
        }

        JSONObject json = new JSONObject(response.body().string());
        JSONObject data = json.getJSONObject("data");
        String file = data.getJSONArray("sources").getJSONObject(0).getString("file");

        // ✅ Use DecryptResponseType2 for type "2"
        Response<DecryptResponseType2> decResp = encDecApi.decryptVidstackType2(
                new VidstackDecryptRequest(file, "2")).execute();

        if (!decResp.isSuccessful() || decResp.body() == null) {
            throw new IOException("Failed to decrypt");
        }

        // ✅ Result is a string, parse it using listParser logic
        String resultString = decResp.body().result;

        Map<String, String> responseHeaders = extractHeaders(decResp);


        Log.i(TAG, "Response headers captured fetchSmashystreamType2: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }

        // Parse the string result (similar to Python's listParser)
        MediaItems mediaItem = parseListResult(resultString, url);

        // Handle subtitles
        String subtitles = data.optString("tracks", "");
        if (!subtitles.isEmpty()) {
            List<MediaItems.SubtitleItem> parsedSubs = parseListSubtitles(subtitles);
            mediaItem.getSubtitles().addAll(parsedSubs);
        }

        return mediaItem;
    }

    // Add helper method to parse list-format results
    private MediaItems parseListResult(String text, String refererUrl) {
        MediaItems item = new MediaItems();
        List<MediaItems.VideoSource> sources = new ArrayList<>();

        // Parse format: "[Label] url, [Label2] url2" or "url1, url2"
        String cleaned = text.trim().replaceAll("\\s+or\\s+", ",");
        String[] items = cleaned.split(",");

        for (String s : items) {
            s = s.trim();
            if (s.isEmpty()) continue;

            String quality = "auto";
            String url = s;

            // Check if format is "[Quality] url"
            if (s.startsWith("[") && s.contains("]")) {
                int closeBracket = s.indexOf(']');
                quality = s.substring(1, closeBracket).trim();
                url = s.substring(closeBracket + 1).trim();
            }

            if (!url.isEmpty()) {
                sources.add(new MediaItems.VideoSource(quality, url));
            }
        }

        item.setVideoSources(sources);
        item.setRefererUrl(refererUrl);

        return item;
    }



    public void fetchSmashystreamTV(String imdbId, String tmdbId, String season, String episode,
                                    String serverType, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<EncryptResponse> tokenResp = encDecApi.encryptVidstack().execute();
                if (!tokenResp.isSuccessful() || tokenResp.body() == null) {
                    throw new IOException("Failed to get token");
                }

                EncryptResponse.EncryptResult tokenData = tokenResp.body().result;
                MediaItems result;

                if ("1".equals(serverType)) {
                    // Type 1: smashystream server
                    result = fetchSmashystreamTVType1(imdbId, tmdbId, season, episode, tokenData);
                } else {
                    // Type 2: videoophim, short2embed, videofsh servers
                    String server = getServerName("2");
                    result = fetchSmashystreamTVType2(tmdbId, season, episode, tokenData, "videoophim");
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Smashystream TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch TV streams for Type 1 servers (smashystream)
     * URL format: /api/v1/{server}/{imdbId}/{tmdbId}/{season}/{episode}
     */
    private MediaItems fetchSmashystreamTVType1(String imdbId, String tmdbId, String season,
                                                String episode, EncryptResponse.EncryptResult tokenData)
            throws Exception {
        GenericApi api = createGenericApi(SMASHYSTREAM_API_BASE);

        // Build URL with season and episode
        String url = SMASHYSTREAM_API_BASE + "/api/v1/videosmashyi/" + imdbId + "/" +
                tmdbId + "/" + season + "/" + episode +
                "?token=" + tokenData.token + "&user_id=" + tokenData.user_id;

        Log.i(TAG, "Fetching Type 1 TV stream: " + url);

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch player data");
        }

        JSONObject json = new JSONObject(response.body().string());
        String[] parts = json.getJSONObject("data").getString("data").split("/#");
        String host = parts[0];
        String id = parts[1];

        String streamUrl = host + "/api/v1/video?id=" + id;
        Log.i(TAG, "Fetching encrypted stream from: " + streamUrl);

        Response<ResponseBody> streamResp = api.getResponse(streamUrl, getUserAgent(), null).execute();

        if (!streamResp.isSuccessful() || streamResp.body() == null) {
            throw new IOException("Failed to fetch stream");
        }

        String encrypted = streamResp.body().string();
        Response<DecryptResponse> decResp = encDecApi.decryptVidstack(
                new VidstackDecryptRequest(encrypted, "1")).execute();

        if (!decResp.isSuccessful() || decResp.body() == null) {
            throw new IOException("Failed to decrypt");
        }

        DecryptResponse.Result result = decResp.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);

        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchSmashystreamType1: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }
        Log.i(TAG, "Decrypted Type 1 TV data: " + jsonStr);

        return parseStreamData(jsonStr, streamUrl, responseHeaders);
    }

    /**
     * Fetch TV streams for Type 2 servers (videoophim, short2embed, videofsh)
     * URL format: /api/v1/{server}/{tmdbId}/{season}/{episode}
     */

    private MediaItems fetchSmashystreamTVType2(String tmdbId, String season, String episode,
                                                EncryptResponse.EncryptResult tokenData, String server)
            throws Exception {
        GenericApi api = createGenericApi(SMASHYSTREAM_API_BASE);

        String url = SMASHYSTREAM_API_BASE + "/api/v1/" + server + "/" + tmdbId + "/" +
                season + "/" + episode +
                "?token=" + tokenData.token + "&user_id=" + tokenData.user_id;

        Log.i(TAG, "Fetching Type 2 TV stream: " + url);

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch player data");
        }

        JSONObject json = new JSONObject(response.body().string());
        JSONObject data = json.getJSONObject("data");
        String file = data.getJSONArray("sources").getJSONObject(0).getString("file");

        Log.i(TAG, "Encrypted file: " + file.substring(0, Math.min(50, file.length())) + "...");

        // ✅ Use Type 2 response
        Response<DecryptResponseType2> decResp = encDecApi.decryptVidstackType2(
                new VidstackDecryptRequest(file, "2")).execute();

        if (!decResp.isSuccessful() || decResp.body() == null) {
            throw new IOException("Failed to decrypt");
        }

        String resultString = decResp.body().result;
        Log.i(TAG, "Decrypted Type 2 TV data: " + resultString);

        MediaItems mediaItem = parseListResult(resultString, url);

        // Handle subtitles
        String subtitles = data.optString("tracks", "");
        if (!subtitles.isEmpty()) {
            List<MediaItems.SubtitleItem> parsedSubs = parseListSubtitles(subtitles);
            if (!parsedSubs.isEmpty()) {
                mediaItem.getSubtitles().addAll(parsedSubs);
                Log.i(TAG, "Added " + parsedSubs.size() + " subtitles");
            }
        }

        return mediaItem;
    }

    /**
     * Get server name from server type code
     * Maps server codes to actual server names
     */
    private String getServerName(String serverType) {
        // Default server mapping based on Python script comments
        switch (serverType) {
            case "2":
                return "short2embed";  // Player SM
            case "3":
                return "videoophim";   // Player O
            case "4":
                return "videofsh";     // Player FS
            default:
                return "videoophim";   // Default to Player O
        }
    }

    /**
     * Parse subtitle list from vidstack format
     * Format: "[Label] url, [Label2] url2" or "url1, url2"
     */
    private List<MediaItems.SubtitleItem> parseListSubtitles(String subtitlesText) {
        List<MediaItems.SubtitleItem> subtitles = new ArrayList<>();

        if (subtitlesText == null || subtitlesText.trim().isEmpty()) {
            return subtitles;
        }

        try {
            // Remove 'or' separators and split by comma
            String cleaned = subtitlesText.trim().replaceAll("\\s+or\\s+", ",");
            String[] items = cleaned.split(",");

            for (String item : items) {
                item = item.trim();
                if (item.isEmpty()) continue;

                String label = "Unknown";
                String url = item;

                // Check if format is "[Label] url"
                if (item.startsWith("[") && item.contains("]")) {
                    int closeBracket = item.indexOf(']');
                    label = item.substring(1, closeBracket).trim();
                    url = item.substring(closeBracket + 1).trim();
                }

                // Only add if URL is not empty
                if (!url.isEmpty()) {
                    subtitles.add(new MediaItems.SubtitleItem(url, label, label));
                    Log.i(TAG, "Parsed subtitle: " + label + " -> " + url);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing subtitles: " + subtitlesText, e);
        }

        return subtitles;
    }


    // ===================== VIDLINK =====================
    public void fetchVidlinkMovie(String tmdbId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<VidlinkEncryptResponse> encResp = encDecApi.encryptVidlink(tmdbId).execute();
                if (!encResp.isSuccessful() || encResp.body() == null) {
                    throw new IOException("Failed to encrypt ID");
                }

                String encrypted = encResp.body().result;
                String url = VIDLINK_API_BASE + "/api/b/movie/" + encrypted;

                MediaItems result = fetchVidlinkStreams(url);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Vidlink Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchVidlinkTV(String tmdbId, String season, String episode, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                //Response<EncryptResponse> encResp = encDecApi.encryptVidlink(tmdbId).execute();

                Response<VidlinkEncryptResponse> encResp = encDecApi.encryptVidlink(tmdbId).execute();
                if (!encResp.isSuccessful() || encResp.body() == null) {
                    throw new IOException("Failed to encrypt ID");
                }

                String encrypted = encResp.body().result;
                String url = VIDLINK_API_BASE + "/api/b/tv/" + encrypted + "/" + season + "/" + episode;

                MediaItems result = fetchVidlinkStreams(url);
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Vidlink TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    private MediaItems fetchVidlinkStreams(String url) throws Exception {
        GenericApi api = createGenericApi(VIDLINK_API_BASE);

        Response<ResponseBody> response = api.getResponse(url, getUserAgent(),
                VIDLINK_API_BASE + "/").execute();
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("Failed to fetch streams");
        }
        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured fetchVidlinkStreams: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }
        Log.i(TAG, "Response headers captured: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()){
            Log.i(TAG, "  " + header.getKey() + ": " + header.getValue());
        }


        return parseStreamData(response.body().string(), url, responseHeaders);
    }

    // ===================== YFLIX/RAPIDSHARE =====================
    public void fetchYflixMovie(String contentId, StreamCallback callback) {
        // Complex multi-step process
        executorService.execute(() -> {
            try {
                // Implementation would follow the yflix_rapidshare_combine.py logic
                mainHandler.post(() -> callback.onError("Yflix implementation pending"));
            } catch (Exception e) {
                Log.e(TAG, "Yflix error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== MAPPLE =====================
    public void fetchMappleMovie(String tmdbId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<MappleEncryptResponse> encResp = encDecApi.encryptMapple().execute();
                if (!encResp.isSuccessful() || encResp.body() == null) {
                    throw new IOException("Failed to get Mapple session");
                }

                MappleEncryptResponse.MappleResult sessionData = encResp.body().result;
                String url = MAPPLE_API_BASE + "/api/v1/movies/" + tmdbId;

                GenericApi api = createGenericApi(MAPPLE_API_BASE);
                Response<ResponseBody> response = api.getResponse(url, getUserAgent(), MAPPLE_API_BASE).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to fetch Mapple streams");
                }

                MediaItems result = parseStreamData(response.body().string(), url, new HashMap<>());
                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Mapple error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== HELPER METHODS =====================
    private GenericApi createGenericApi(String baseUrl) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .build();
        return retrofit.create(GenericApi.class);
    }

    private String getUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";
    }

    private String generateHexKey() {
        byte[] bytes = new byte[32];
        new java.security.SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private Map<String, String> extractHeaders(Response<?> response) {
        Map<String, String> headers = new HashMap<>();
        okhttp3.Response raw = response.raw();
        for (String name : raw.headers().names()) {
            headers.put(name, raw.header(name));
        }
        return headers;
    }





    private MediaItems parseStreamData(String jsonStr, String refererUrl,
                                       Map<String, String> responseHeaders) throws Exception {
        JSONObject data = new JSONObject(jsonStr);

        List<MediaItems.VideoSource> sources = new ArrayList<>();
        List<MediaItems.SubtitleItem> subs = new ArrayList<>();
        Map<String, String> customHeaders = new HashMap<>();

        // Parse sourceId (it's a string, not an array)
        String sourceId = data.optString("sourceId", "");
        Log.i(TAG, "SourceId: " + sourceId);

        // ============================================
        // HEXA FORMAT: Parse sources array with server names
        // ============================================
        JSONArray hexaSourcesArray = data.optJSONArray("sources");
        if (hexaSourcesArray != null && hexaSourcesArray.length() > 0) {
            // Check if first element has "server" field (Hexa format indicator)
            JSONObject firstSource = hexaSourcesArray.optJSONObject(0);
            if (firstSource != null && firstSource.has("server")) {
                Log.i(TAG, "Detected Hexa format response");

                for (int i = 0; i < hexaSourcesArray.length(); i++) {
                    JSONObject sourceObj = hexaSourcesArray.getJSONObject(i);
                    String server = sourceObj.optString("server", "");
                    String url = sourceObj.optString("url", "");

                    if (!url.isEmpty()) {
                        // Use server name as quality identifier
                        sources.add(new MediaItems.VideoSource(server, url));
                        Log.i(TAG, "Added Hexa source - Server: " + server + ", URL: " + url);
                    }
                }

                // Set headers for Hexa format
                customHeaders.put("User-Agent", getUserAgent());
                customHeaders.put("Accept", "*/*");
                customHeaders.put("Accept-Encoding", "gzip, deflate");
                customHeaders.put("Accept-Language", "en-US,en;q=0.9");
                customHeaders.put("Connection", "keep-alive");
                customHeaders.put("Referer", "https://api.videasy.net/");
                customHeaders.put("Origin", "https://api.videasy.net");

                Log.i(TAG, "Applied Hexa format headers");
            }
            // ============================================
            // VIDLINK FORMAT: Check for stream object
            // ============================================
            else {
                JSONObject streamObj = data.optJSONObject("stream");
                if (streamObj != null) {
                    Log.i(TAG, "Detected Vidlink format response");

                    // Get playlist URL from stream object
                    String playlistUrl = streamObj.optString("playlist", "");
                    if (!playlistUrl.isEmpty()) {
                        Log.i(TAG, "Original playlist URL: " + playlistUrl);

                        // Extract headers and clean URL
                        String cleanUrl = playlistUrl;
                        Map<String, String> extractedHeaders = extractHeadersFromUrl(playlistUrl);

                        // Remove the ?headers=...&host=... part to get clean URL
                        int headersIndex = playlistUrl.indexOf("?headers=");
                        if (headersIndex != -1) {
                            cleanUrl = playlistUrl.substring(0, headersIndex);
                            Log.i(TAG, "Clean playlist URL: " + cleanUrl);
                        }

                        // Add the CLEAN playlist URL as a video source
                        String streamType = streamObj.optString("type", "hls").toUpperCase();
                        sources.add(new MediaItems.VideoSource(streamType, cleanUrl));

                        Log.i(TAG, "Added source - Type: " + streamType + ", URL: " + cleanUrl);

                        // Apply extracted headers to custom headers
                        if (!extractedHeaders.isEmpty()) {
                            customHeaders.put("User-Agent", getUserAgent());
                            customHeaders.putAll(extractedHeaders);

                            // Update referer if extracted from URL
                            if (extractedHeaders.containsKey("Referer")) {
                                refererUrl = extractedHeaders.get("Referer");
                                Log.i(TAG, "Updated referer from URL: " + refererUrl);
                            }

                            Log.i(TAG, "Extracted " + extractedHeaders.size() + " headers from playlist URL");
                            Log.i(TAG, "extractedHeaders: " + extractedHeaders.toString());
                        }
                    }

                    // Parse captions array from stream object (NOT from root)
                    JSONArray captionsArray = streamObj.optJSONArray("captions");
                    if (captionsArray != null) {
                        Log.i(TAG, "Found " + captionsArray.length() + " caption(s)");

                        for (int i = 0; i < captionsArray.length(); i++) {
                            JSONObject captionObj = captionsArray.getJSONObject(i);
                            String captionUrl = captionObj.optString("url", "");
                            String captionId = captionObj.optString("id", "");
                            String language = captionObj.optString("language", "Unknown");

                            // Use url if available, otherwise use id
                            String subtitleUrl = !captionUrl.isEmpty() ? captionUrl : captionId;

                            if (!subtitleUrl.isEmpty()) {
                                // Extract language code (e.g., "English" -> "en")
                                String langCode = extractLanguageCode(language, captionId);

                                subs.add(new MediaItems.SubtitleItem(
                                        subtitleUrl,
                                        langCode,
                                        language
                                ));

                                Log.i(TAG, "Added subtitle: " + language + " (" + langCode + ")");
                            }
                        }
                    }
                }
                // ============================================
                // STANDARD FORMAT: Fallback for other APIs
                // ============================================
                else {
                    Log.i(TAG, "Detected standard format response");

                    // Parse sources array (standard format)
                    if (hexaSourcesArray != null) {
                        for (int i = 0; i < hexaSourcesArray.length(); i++) {
                            JSONObject obj = hexaSourcesArray.getJSONObject(i);
                            sources.add(new MediaItems.VideoSource(
                                    obj.optString("quality", "auto"),
                                    obj.optString("url", "")
                            ));
                        }
                        Log.i(TAG, "Parsed " + sources.size() + " source(s)");
                    }

                    // Parse subtitles array
                    JSONArray subsArray = data.optJSONArray("subtitles");
                    if (subsArray != null) {
                        for (int i = 0; i < subsArray.length(); i++) {
                            JSONObject obj = subsArray.getJSONObject(i);
                            subs.add(new MediaItems.SubtitleItem(
                                    obj.optString("url", ""),
                                    obj.optString("lang", ""),
                                    obj.optString("language", "")
                            ));
                        }
                        Log.i(TAG, "Parsed " + subs.size() + " subtitle(s)");
                    }

                    // Also check for captions at root level (some APIs)
                    JSONArray captionsArray = data.optJSONArray("captions");
                    if (captionsArray != null && subs.isEmpty()) {
                        for (int i = 0; i < captionsArray.length(); i++) {
                            JSONObject captionObj = captionsArray.getJSONObject(i);
                            String captionUrl = captionObj.optString("url", "");
                            String language = captionObj.optString("language", "Unknown");
                            String langCode = extractLanguageCode(language, captionObj.optString("id", ""));

                            if (!captionUrl.isEmpty()) {
                                subs.add(new MediaItems.SubtitleItem(
                                        captionUrl,
                                        langCode,
                                        language
                                ));
                            }
                        }
                    }
                }
            }
        }

        // ============================================
        // Extract Cloudflare session
        // ============================================
        CloudflareSessionHandler.CloudflareSession cfSession =
                CloudflareSessionHandler.extractSession(responseHeaders);

        Log.i(TAG, "Cloudflare session extracted:\n" + cfSession.toString());

        MediaItems item = new MediaItems();

        // Store Cloudflare session in MediaItem (for backward compatibility)
        if (cfSession.isValid()) {
            item.setSessionCookie(cfSession.sessionCookie);
            Log.i(TAG, "✓ Session cookie stored in MediaItem "+cfSession.sessionCookie);
        } else {
            item.setSessionCookie(cfSession.getSessionCookie());
            Log.i(TAG, "⚠ Session cookie set " + cfSession.getSessionCookie());
            Log.w(TAG, "⚠ No valid Cloudflare session to store");
        }

        // ============================================
        // Build custom headers
        // ============================================

        // If headers were extracted from URL, use them as base
        if (customHeaders.isEmpty()) {
            // No extracted headers, use defaults
            customHeaders.put("User-Agent", getUserAgent());
            customHeaders.put("Accept", "*/*");
            customHeaders.put("Accept-Language", "en-US,en;q=0.9");
            customHeaders.put("Accept-Encoding", "gzip, deflate, br");
            customHeaders.put("Connection", "keep-alive");
            customHeaders.put("Keep-Alive", "timeout=300, max=1000");
            customHeaders.put("Referer", refererUrl);
            customHeaders.put("Origin", getOriginFromUrl(refererUrl));
        }

        // ✅ CRITICAL FIX: Attach headers to EACH VideoSource
        Log.i(TAG, "Attaching authentication headers to " + sources.size() + " video sources");
        for (MediaItems.VideoSource source : sources) {
            source.setSessionCookie(cfSession.getSessionCookie());
            source.setCustomHeaders(new HashMap<>(customHeaders));  // Copy to avoid reference sharing
            source.setRefererUrl(refererUrl);
            source.setResponseHeaders(new HashMap<>(responseHeaders));  // Copy to avoid reference sharing

            Log.i(TAG, "✅ Source attached: " + source.getQuality());
            Log.i(TAG, "   URL: " + (source.getUrl().length() > 60 ?
                    source.getUrl().substring(0, 60) + "..." : source.getUrl()));
            Log.i(TAG, "   Cookie: " + (source.getSessionCookie() != null &&
                    !source.getSessionCookie().isEmpty() ? "[SET, length=" +
                    source.getSessionCookie().length() + "]" : "[NONE]"));
            Log.i(TAG, "   Referer: " + source.getRefererUrl());
            Log.i(TAG, "   Custom headers: " + source.getCustomHeaders().size());
        }

        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(refererUrl);
        item.setResponseHeaders(responseHeaders);
        item.setCustomHeaders(customHeaders);

        Log.i(TAG, "MediaItem configured with:");
        Log.i(TAG, "  - SourceId: " + sourceId);
        Log.i(TAG, "  - Video sources: " + sources.size() + " (each with attached headers)");
        Log.i(TAG, "  - Subtitles: " + subs.size());
        Log.i(TAG, "  - Global custom headers: " + customHeaders.size() + " (backward compatibility)");
        Log.i(TAG, "  - Session cookie: " + (item.getSessionCookie() != null ? "Yes" : "No"));
        Log.i(TAG, "  - Referer: " + item.getRefererUrl());

        return item;
    }

    /**
     * Extracts headers from the ?headers= query parameter in a URL.
     * Example: .m3u8?headers={"referer":"https://megacloud.blog/","origin":"https://megacloud.blog"}&host=...
     *
     * @param url The URL containing the headers parameter
     * @return Map of extracted headers with proper capitalization
     */
    private Map<String, String> extractHeadersFromUrl(String url) {
        Map<String, String> headers = new HashMap<>();

        try {
            // Find ?headers= in the URL
            int headersIndex = url.indexOf("?headers=");
            if (headersIndex == -1) {
                return headers; // No headers parameter found
            }

            // Extract the headers JSON string
            int startIndex = headersIndex + "?headers=".length();
            int endIndex = url.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = url.length();
            }

            String headersJsonStr = url.substring(startIndex, endIndex);

            // URL decode the headers JSON string (if needed)
            try {
                headersJsonStr = java.net.URLDecoder.decode(headersJsonStr, "UTF-8");
            } catch (Exception e) {
                // Already decoded or not URL encoded
            }

            Log.i(TAG, "Headers JSON string: " + headersJsonStr);

            // Parse the JSON object
            JSONObject headersJson = new JSONObject(headersJsonStr);

            // Extract each header with proper capitalization
            Iterator<String> keys = headersJson.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                String value = headersJson.optString(key, "");
                if (!value.isEmpty()) {
                    // Capitalize header names properly
                    String capitalizedKey = capitalizeHeaderName(key);
                    headers.put(capitalizedKey, value);
                    Log.i(TAG, "Extracted header: " + capitalizedKey + " = " + value);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error extracting headers from URL", e);
        }

        return headers;
    }

    /**
     * Capitalizes HTTP header names properly.
     * Examples: "referer" -> "Referer", "origin" -> "Origin", "user-agent" -> "User-Agent"
     */
    private String capitalizeHeaderName(String headerName) {
        if (headerName == null || headerName.isEmpty()) {
            return headerName;
        }

        // Split by hyphen for multi-word headers
        String[] parts = headerName.split("-");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (!part.isEmpty()) {
                // Capitalize first letter, lowercase the rest
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }

            // Add hyphen between parts (except for the last part)
            if (i < parts.length - 1) {
                result.append("-");
            }
        }

        return result.toString();
    }

    /**
     * Extracts language code from language name or ID.
     * Examples: "English" -> "en", "eng-3.vtt" -> "en"
     */
    private String extractLanguageCode(String language, String id) {
        // Try to extract from ID first (e.g., "eng-3.vtt" -> "eng")
        if (id != null && id.contains("eng")) {
            return "en";
        }
        if (id != null && id.contains("spa")) {
            return "es";
        }
        if (id != null && id.contains("fre") || (id != null && id.contains("fra"))) {
            return "fr";
        }

        // Extract from language name
        if (language == null || language.isEmpty()) {
            return "unknown";
        }

        String lower = language.toLowerCase();
        if (lower.contains("english")) return "en";
        if (lower.contains("spanish")) return "es";
        if (lower.contains("french")) return "fr";
        if (lower.contains("german")) return "de";
        if (lower.contains("italian")) return "it";
        if (lower.contains("portuguese")) return "pt";
        if (lower.contains("japanese")) return "ja";
        if (lower.contains("chinese")) return "zh";
        if (lower.contains("korean")) return "ko";
        if (lower.contains("arabic")) return "ar";
        if (lower.contains("russian")) return "ru";

        // Default: take first 2 characters
        return language.length() >= 2 ? language.substring(0, 2).toLowerCase() : language.toLowerCase();
    }

    /**
     * Extracts origin from a URL.
     * Example: "https://megacloud.blog/path" -> "https://megacloud.blog"
     */
    private String getOriginFromUrl(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            return parsedUrl.getProtocol() + "://" + parsedUrl.getHost();
        } catch (Exception e) {
            return VIDEASY_API_BASE; // Fallback
        }
    }

    // ===================== MEGAUP =====================
    /**
     * Fetch streams from Megaup embed URL
     * @param embedUrl Format: https://megaup.live/e/{id}
     */
    public void fetchMegaup(String embedUrl, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                // Replace /e/ with /media/
                String mediaUrl = embedUrl.replace("/e/", "/media/");

                GenericApi api = createGenericApi("https://megaup.live");
                Response<ResponseBody> response = api.getResponse(mediaUrl, getUserAgent(), null).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to fetch encrypted data");
                }

                JSONObject json = new JSONObject(response.body().string());
                String encrypted = json.getString("result");

                Response<DecryptResponse> decResponse = encDecApi.decryptMega(
                        new RapidDecryptRequest(encrypted, getUserAgent())).execute();

                if (!decResponse.isSuccessful() || decResponse.body() == null) {
                    throw new IOException("Failed to decrypt");
                }

                DecryptResponse.Result result = decResponse.body().result;
                String jsonStr = new com.google.gson.Gson().toJson(result);

                MediaItems mediaItem = parseStreamData(jsonStr, embedUrl, new HashMap<>());
                mainHandler.post(() -> callback.onSuccess(mediaItem));
            } catch (Exception e) {
                Log.e(TAG, "Megaup error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== XPRIME =====================
    /**
     * Fetch streams from XPrime
     * Supports multiple servers: primebox, etc.
     */
    public void fetchXprimeMovie(String title, String year, String tmdbId, String imdbId,
                                 String server, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                // Get turnstile token
                Response<EncryptResponse> tokenResp = encDecApi.encryptXprime().execute();
                if (!tokenResp.isSuccessful() || tokenResp.body() == null) {
                    throw new IOException("Failed to get turnstile token");
                }

                String token = tokenResp.body().result.token;
                String encodedTitle = URLEncoder.encode(title, "UTF-8");

                String url = XPRIME_API_BASE + "/" + server + "?name=" + encodedTitle +
                        "&year=" + year + "&id=" + tmdbId + "&imdb=" + imdbId +
                        "&turnstile=" + token;

                GenericApi api = createGenericApi(XPRIME_API_BASE);
                Map<String, String> headers = new HashMap<>();
                headers.put("Origin", "https://xprime.tv");

                Response<ResponseBody> response = api.getResponse(url, getUserAgent(),
                        "https://xprime.tv").execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to fetch encrypted data");
                }

                String encrypted = response.body().string();
                Response<DecryptResponse> decResponse = encDecApi.decryptXprime(
                        DecryptRequest.withId(encrypted, null)).execute();

                if (!decResponse.isSuccessful() || decResponse.body() == null) {
                    throw new IOException("Failed to decrypt");
                }

                DecryptResponse.Result result = decResponse.body().result;
                String jsonStr = new com.google.gson.Gson().toJson(result);

                MediaItems mediaItem = parseStreamData(jsonStr, url, new HashMap<>());
                mainHandler.post(() -> callback.onSuccess(mediaItem));
            } catch (Exception e) {
                Log.e(TAG, "XPrime error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void fetchXprimeTV(String title, String year, String tmdbId, String imdbId,
                              String season, String episode, String server, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<EncryptResponse> tokenResp = encDecApi.encryptXprime().execute();
                if (!tokenResp.isSuccessful() || tokenResp.body() == null) {
                    throw new IOException("Failed to get turnstile token");
                }

                String token = tokenResp.body().result.token;
                String encodedTitle = URLEncoder.encode(title, "UTF-8");

                String url = XPRIME_API_BASE + "/" + server + "?name=" + encodedTitle +
                        "&year=" + year + "&id=" + tmdbId + "&imdb=" + imdbId +
                        "&season=" + season + "&episode=" + episode +
                        "&turnstile=" + token;

                GenericApi api = createGenericApi(XPRIME_API_BASE);
                Response<ResponseBody> response = api.getResponse(url, getUserAgent(),
                        "https://xprime.tv").execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to fetch encrypted data");
                }

                String encrypted = response.body().string();
                Response<DecryptResponse> decResponse = encDecApi.decryptXprime(
                        DecryptRequest.withId(encrypted, null)).execute();

                if (!decResponse.isSuccessful() || decResponse.body() == null) {
                    throw new IOException("Failed to decrypt");
                }

                DecryptResponse.Result result = decResponse.body().result;
                String jsonStr = new com.google.gson.Gson().toJson(result);

                MediaItems mediaItem = parseStreamData(jsonStr, url, new HashMap<>());
                mainHandler.post(() -> callback.onSuccess(mediaItem));
            } catch (Exception e) {
                Log.e(TAG, "XPrime TV error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== KISSKH =====================
    /**
     * Fetch streams and subtitles from KissKH
     * @param contentId The KissKH content ID
     */
    public void fetchKisskh(String contentId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                // Get video encryption key
                Response<EncryptResponse> vidKeyResp = encDecApi.encryptKisskh(contentId, "vid").execute();
                if (!vidKeyResp.isSuccessful() || vidKeyResp.body() == null) {
                    throw new IOException("Failed to get video key");
                }
                String vidKey = vidKeyResp.body().result.token;

                // Fetch video sources
                String videoUrl = KISSKH_API_BASE + "/api/DramaList/Episode/" + contentId +
                        ".png?err=false&ts=&time=&kkey=" + vidKey;

                GenericApi api = createGenericApi(KISSKH_API_BASE);
                Response<ResponseBody> videoResp = api.getResponse(videoUrl, getUserAgent(), null).execute();

                if (!videoResp.isSuccessful() || videoResp.body() == null) {
                    throw new IOException("Failed to fetch video data");
                }

                JSONObject videoData = new JSONObject(videoResp.body().string());

                // Get subtitle encryption key
                Response<EncryptResponse> subKeyResp = encDecApi.encryptKisskh(contentId, "sub").execute();
                if (!subKeyResp.isSuccessful() || subKeyResp.body() == null) {
                    throw new IOException("Failed to get subtitle key");
                }
                String subKey = subKeyResp.body().result.token;

                // Fetch subtitles
                String subtitleUrl = KISSKH_API_BASE + "/api/Sub/" + contentId + "?kkey=" + subKey;
                Response<ResponseBody> subResp = api.getResponse(subtitleUrl, getUserAgent(), null).execute();

                List<MediaItems.SubtitleItem> subtitles = new ArrayList<>();
                if (subResp.isSuccessful() && subResp.body() != null) {
                    JSONArray subArray = new JSONArray(subResp.body().string());

                    for (int i = 0; i < subArray.length(); i++) {
                        JSONObject subObj = subArray.getJSONObject(i);
                        String subUrl = subObj.optString("src", "");
                        String label = subObj.optString("label", "Unknown");

                        if (!subUrl.isEmpty()) {
                            String langCode = extractLanguageCode(label, "");
                            subtitles.add(new MediaItems.SubtitleItem(subUrl, langCode, label));
                        }
                    }
                }

                // Parse video sources
                MediaItems mediaItem = parseStreamData(videoData.toString(), videoUrl, new HashMap<>());
                mediaItem.getSubtitles().addAll(subtitles);

                mainHandler.post(() -> callback.onSuccess(mediaItem));
            } catch (Exception e) {
                Log.e(TAG, "KissKH error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== ANIMEKAI =====================
    /**
     * Fetch anime streams from AnimeKai
     * @param contentId The AnimeKai content ID
     */
    public void fetchAnimeKai(String contentId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                // Get episodes list
                Response<EncryptResponse> encIdResp = encDecApi.encryptKai(contentId).execute();
                if (!encIdResp.isSuccessful() || encIdResp.body() == null) {
                    throw new IOException("Failed to encrypt content ID");
                }
                String encId = encIdResp.body().result.token;

                String episodesUrl = ANIMEKAI_API_BASE + "/ajax/episodes/list?ani_id=" +
                        contentId + "&_=" + encId;

                GenericApi api = createGenericApi(ANIMEKAI_API_BASE);
                Response<ResponseBody> episodesResp = api.getResponse(episodesUrl, getUserAgent(), null).execute();

                if (!episodesResp.isSuccessful() || episodesResp.body() == null) {
                    throw new IOException("Failed to fetch episodes");
                }

                JSONObject episodesJson = new JSONObject(episodesResp.body().string());

                // Parse HTML to get episode tokens (would need parse-html API here)
                Response<ParseHtmlResponse> parseResp = encDecApi.parseHtml(
                        new ParseHtmlRequest(episodesJson.getString("result"))).execute();

                if (!parseResp.isSuccessful() || parseResp.body() == null) {
                    throw new IOException("Failed to parse episodes HTML");
                }

                // Extract first episode token (simplified - actual implementation would need proper parsing)
                String episodeToken = ""; // This would come from parsed HTML

                // Get servers list
                Response<EncryptResponse> encTokenResp = encDecApi.encryptKai(episodeToken).execute();
                if (!encTokenResp.isSuccessful() || encTokenResp.body() == null) {
                    throw new IOException("Failed to encrypt token");
                }
                String encToken = encTokenResp.body().result.token;

                String serversUrl = ANIMEKAI_API_BASE + "/ajax/links/list?token=" +
                        episodeToken + "&_=" + encToken;
                Response<ResponseBody> serversResp = api.getResponse(serversUrl, getUserAgent(), null).execute();

                if (!serversResp.isSuccessful() || serversResp.body() == null) {
                    throw new IOException("Failed to fetch servers");
                }

                JSONObject serversJson = new JSONObject(serversResp.body().string());

                // Parse servers HTML
                Response<ParseHtmlResponse> parseServersResp = encDecApi.parseHtml(
                        new ParseHtmlRequest(serversJson.getString("result"))).execute();

                if (!parseServersResp.isSuccessful() || parseServersResp.body() == null) {
                    throw new IOException("Failed to parse servers HTML");
                }

                // Get first server link ID (would need proper parsing)
                String lid = ""; // This would come from parsed servers HTML

                // Get embed URL
                Response<EncryptResponse> encLidResp = encDecApi.encryptKai(lid).execute();
                if (!encLidResp.isSuccessful() || encLidResp.body() == null) {
                    throw new IOException("Failed to encrypt lid");
                }
                String encLid = encLidResp.body().result.token;

                String embedUrl = ANIMEKAI_API_BASE + "/ajax/links/view?id=" + lid + "&_=" + encLid;
                Response<ResponseBody> embedResp = api.getResponse(embedUrl, getUserAgent(), null).execute();

                if (!embedResp.isSuccessful() || embedResp.body() == null) {
                    throw new IOException("Failed to fetch embed");
                }

                JSONObject embedJson = new JSONObject(embedResp.body().string());
                String encrypted = embedJson.getString("result");

                // Decrypt
                Response<DecryptResponse> decResp = encDecApi.decryptKai(
                        DecryptRequest.withId(encrypted, null)).execute();

                if (!decResp.isSuccessful() || decResp.body() == null) {
                    throw new IOException("Failed to decrypt");
                }

                DecryptResponse.Result result = decResp.body().result;
                String jsonStr = new com.google.gson.Gson().toJson(result);

                MediaItems mediaItem = parseStreamData(jsonStr, embedUrl, new HashMap<>());
                mainHandler.post(() -> callback.onSuccess(mediaItem));
            } catch (Exception e) {
                Log.e(TAG, "AnimeKai error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    // ===================== DATABASE QUERIES =====================
    /**
     * Query Flix database by TMDB ID
     */
    public interface DatabaseCallback {
        void onSuccess(Object result);
        void onError(String error);
    }

    public void queryFlixDatabase(String tmdbId, String type, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                String url = DATABASE_BASE + "/flix/find?tmdb_id=" + tmdbId;
                if (type != null && !type.isEmpty()) {
                    url += "&type=" + type;
                }

                GenericApi api = createGenericApi(DATABASE_BASE);
                Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to query database");
                }

                String jsonResult = response.body().string();
                mainHandler.post(() -> callback.onSuccess(jsonResult));
            } catch (Exception e) {
                Log.e(TAG, "Database query error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public void queryKaiDatabase(String malId, DatabaseCallback callback) {
        executorService.execute(() -> {
            try {
                String url = DATABASE_BASE + "/kai/find?mal_id=" + malId;

                GenericApi api = createGenericApi(DATABASE_BASE);
                Response<ResponseBody> response = api.getResponse(url, getUserAgent(), null).execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new IOException("Failed to query database");
                }

                String jsonResult = response.body().string();
                mainHandler.post(() -> callback.onSuccess(jsonResult));
            } catch (Exception e) {
                Log.e(TAG, "Database query error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }



    public void shutdown() {
        executorService.shutdown();
    }
}