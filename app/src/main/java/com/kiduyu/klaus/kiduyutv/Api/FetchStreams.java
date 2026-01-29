package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.CloudflareSessionHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
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

    private static final int TIMEOUT_SECONDS = 30;

    private final EncDecApi encDecApi;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final OkHttpClient okHttpClient;

    // Shared static OkHttpClient for both API calls and ExoPlayer streaming
    private static OkHttpClient sharedClient;

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
        Call<EncryptResponse> encryptVidlink(@Query("text") String text);

        @GET("api/enc-movies-flix")
        Call<EncryptResponse> encryptMoviesFlix(@Query("text") String text);

        @GET("api/enc-mapple")
        Call<MappleEncryptResponse> encryptMapple();
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

    public FetchStreams() {
        executorService = Executors.newCachedThreadPool();
        mainHandler = new Handler(Looper.getMainLooper());

        if (sharedClient == null) {
            sharedClient = buildOptimizedOkHttpClient();
        }

        okHttpClient = sharedClient;

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ENC_DEC_API)
                .client(okHttpClient)
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        encDecApi = retrofit.create(EncDecApi.class);
    }

    /**
     * Build an optimized OkHttpClient with connection pooling, retry mechanism, and DNS caching
     */
    private static OkHttpClient buildOptimizedOkHttpClient() {
        // Create connection pool for reusing connections
        ConnectionPool connectionPool = new ConnectionPool(
                10,              // Maximum idle connections
                5,               // Keep alive duration
                TimeUnit.MINUTES // Time unit
        );

        return new OkHttpClient.Builder()
                // Connection timeouts
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)

                // Connection pooling - CRITICAL for preventing socket closure
                .connectionPool(connectionPool)

                // Retry and redirect
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)

                // Protocols - prefer HTTP/2 for better connection management
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))

                // DNS - cache DNS results
                .dns(new DnsSelector())

                // Interceptors
                .addInterceptor(new ConnectionKeepAliveInterceptor())
                .addInterceptor(new RetryInterceptor(3))
                .addNetworkInterceptor(new LoggingInterceptor())

                .build();
    }

    /**
     * Get the shared OkHttpClient instance for use with ExoPlayer streaming.
     * This allows sharing the connection pool and cookies between API calls and media playback.
     */
    public static OkHttpClient getSharedClient() {
        if (sharedClient == null) {
            sharedClient = buildOptimizedOkHttpClient();
        }
        return sharedClient;
    }

    // ============================================
    // Custom Interceptors for Connection Management
    // ============================================

    /**
     * Keeps connections alive by adding Connection: keep-alive header
     */
    private static class ConnectionKeepAliveInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();

            Request request = originalRequest.newBuilder()
                    .header("Connection", "keep-alive")
                    .header("Keep-Alive", "timeout=300, max=1000")
                    .build();

            return chain.proceed(request);
        }
    }

    /**
     * Retries failed requests automatically with exponential backoff
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        public RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            okhttp3.Response response = null;
            IOException exception = null;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    response = chain.proceed(request);

                    // If successful, return
                    if (response.isSuccessful()) {
                        return response;
                    }

                    // Close failed response
                    if (response != null) {
                        response.close();
                    }

                } catch (IOException e) {
                    exception = e;
                    Log.w(TAG, "Request failed, attempt " + (attempt + 1) + "/" + maxRetries);

                    // Wait before retry (exponential backoff)
                    if (attempt < maxRetries - 1) {
                        try {
                            Thread.sleep((long) Math.pow(2, attempt) * 1000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw e;
                        }
                    }
                }
            }

            // All retries failed
            if (exception != null) {
                throw exception;
            }

            return response;
        }
    }

    /**
     * DNS resolver with caching for improved performance
     */
    private static class DnsSelector implements Dns {
        private final Map<String, List<InetAddress>> dnsCache = new HashMap<>();
        private final long CACHE_DURATION = 5 * 60 * 1000; // 5 minutes
        private final Map<String, Long> cacheTimestamps = new HashMap<>();

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            // Check cache
            if (dnsCache.containsKey(hostname)) {
                Long timestamp = cacheTimestamps.get(hostname);
                if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_DURATION) {
                    Log.i(TAG, "Using cached DNS for: " + hostname);
                    return dnsCache.get(hostname);
                }
            }

            // Lookup and cache
            List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
            dnsCache.put(hostname, addresses);
            cacheTimestamps.put(hostname, System.currentTimeMillis());

            return addresses;
        }
    }

    // Logging interceptor (network level for detailed debugging)
    private static class LoggingInterceptor implements Interceptor {
        @Override
        public okhttp3.Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Log.i(TAG, "[NETWORK] Request: " + request.url());

            long startTime = System.currentTimeMillis();
            okhttp3.Response response = chain.proceed(request);
            long endTime = System.currentTimeMillis();

            Log.i(TAG, "[NETWORK] Response: " + response.code() + " in " + (endTime - startTime) + "ms");

            return response;
        }
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
        Map<String, String> responseHeaders = extractHeaders(response);


        Log.i(TAG, "Response headers captured: " + responseHeaders.size());
        for (Map.Entry<String, String> header : responseHeaders.entrySet()) {
            Log.i(TAG, "  " + header.getKey() + ": " +
                    (header.getValue().length() > 100 ?
                            header.getValue().substring(0, 100) + "..." :
                            header.getValue()));
        }



        Response<DecryptResponse> decResponse = encDecApi.decryptVideasy(
                DecryptRequest.withId(encrypted, tmdbId)).execute();

        if (!decResponse.isSuccessful() || decResponse.body() == null) {
            throw new IOException("Failed to decrypt");
        }

        // Convert DecryptResponse.Result to JSON string for parseStreamData
        DecryptResponse.Result result = decResponse.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);
        Log.i(TAG, "fetchVideasyMovie JSON: " + jsonStr);



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
        Map<String, String> responseHeaders = extractHeaders(response);

        Response<DecryptResponse> decResponse = encDecApi.decryptHexa(
                DecryptRequest.withKey(encrypted, key)).execute();

        if (!decResponse.isSuccessful() || decResponse.body() == null) {
            throw new IOException("Failed to decrypt");
        }
        DecryptResponse.Result result = decResponse.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);

        Log.i(TAG, "fetchHexaMovie JSON: " + jsonStr);


        String refererUrl="https://hexa.su/";

        return parseStreamData(jsonStr, refererUrl, new HashMap<>());
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


        DecryptResponse.Result result = decResponse.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);
        return parseStreamData(jsonStr, url, new HashMap<>());
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
                    result = fetchSmashystreamType2(tmdbId, tokenData, "videoophim");
                    Log.i(TAG, "fetchSmashystreamMovie result: " + result.toString());
                }

                mainHandler.post(() -> callback.onSuccess(result));
            } catch (Exception e) {
                Log.e(TAG, "Smashystream Movie error", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
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
                // Implement TV show logic similar to movie
                // This would require additional server endpoints

                mainHandler.post(() -> callback.onError("TV show support not yet implemented"));
            } catch (Exception e) {
                Log.e(TAG, "Smashystream TV error", e);
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

        return parseStreamData(jsonStr, streamUrl, new HashMap<>());
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

        Response<DecryptResponse> decResp = encDecApi.decryptVidstack(
                new VidstackDecryptRequest(file, "2")).execute();

        if (!decResp.isSuccessful() || decResp.body() == null) {
            throw new IOException("Failed to decrypt");
        }


        DecryptResponse.Result result = decResp.body().result;
        String jsonStr = new com.google.gson.Gson().toJson(result);

        return parseStreamData(jsonStr, url, new HashMap<>());
    }

    // ===================== VIDLINK =====================
    public void fetchVidlinkMovie(String tmdbId, StreamCallback callback) {
        executorService.execute(() -> {
            try {
                Response<EncryptResponse> encResp = encDecApi.encryptVidlink(tmdbId).execute();
                if (!encResp.isSuccessful() || encResp.body() == null) {
                    throw new IOException("Failed to encrypt ID");
                }

                String encrypted = encResp.body().result.token;
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
                Response<EncryptResponse> encResp = encDecApi.encryptVidlink(tmdbId).execute();
                if (!encResp.isSuccessful() || encResp.body() == null) {
                    throw new IOException("Failed to encrypt ID");
                }

                String encrypted = encResp.body().result.token;
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

        return parseStreamData(response.body().string(), url, new HashMap<>());
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
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";
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

        JSONArray sourcesArray = data.optJSONArray("sources");
        if (sourcesArray != null) {
            for (int i = 0; i < sourcesArray.length(); i++) {
                JSONObject obj = sourcesArray.getJSONObject(i);
                sources.add(new MediaItems.VideoSource(
                        obj.optString("quality"),
                        obj.optString("url")
                ));
            }
        }

        JSONArray subsArray = data.optJSONArray("subtitles");
        if (subsArray != null) {
            for (int i = 0; i < subsArray.length(); i++) {
                JSONObject obj = subsArray.getJSONObject(i);
                subs.add(new MediaItems.SubtitleItem(
                        obj.optString("url"),
                        obj.optString("lang"),
                        obj.optString("language")
                ));
            }
        }

        // *** NEW: Extract Cloudflare session ***
        CloudflareSessionHandler.CloudflareSession cfSession =
                CloudflareSessionHandler.extractSession(responseHeaders);

        Log.i(TAG, "Cloudflare session extracted:\n" + cfSession.toString());

        MediaItems item = new MediaItems();

        // *** NEW: Store Cloudflare session in MediaItem ***
        if (cfSession.isValid()) {
            item.setSessionCookie(cfSession.sessionCookie);
            Log.i(TAG, "✓ Session cookie stored in MediaItem");
        } else {
            item.setSessionCookie(cfSession.getSessionCookie());
            Log.i(TAG, "⚠ Session cookie set " + cfSession.getSessionCookie());

            Log.w(TAG, "⚠ No valid Cloudflare session to store");
        }



        item.setVideoSources(sources);
        item.setSubtitles(subs);
        item.setRefererUrl(refererUrl);
        item.setResponseHeaders(responseHeaders);

        // Build and store custom headers for requests
        Map<String, String> customHeaders = new HashMap<>();
        customHeaders.put("User-Agent", getUserAgent());
        customHeaders.put("Accept", "*/*");
        customHeaders.put("Accept-Language", "en-US,en;q=0.9");
        customHeaders.put("Accept-Encoding", "gzip, deflate, br");
        customHeaders.put("Connection", "keep-alive");
        customHeaders.put("Keep-Alive", "timeout=300, max=1000");
        customHeaders.put("Referer", refererUrl);
        customHeaders.put("Origin", VIDEASY_API_BASE);


        item.setCustomHeaders(customHeaders);
        item.setRefererUrl(refererUrl);

        Log.i(TAG, "MediaItem configured with:");
        Log.i(TAG, "  - Custom headers: " + customHeaders.size());
        Log.i(TAG, "  - Session cookie: " + (item.getSessionCookie() != null ? "Yes" : "No"));
        Log.i(TAG, "  - Referer: " + item.getRefererUrl());

        return item;
    }



    public void shutdown() {
        executorService.shutdown();
    }
}
