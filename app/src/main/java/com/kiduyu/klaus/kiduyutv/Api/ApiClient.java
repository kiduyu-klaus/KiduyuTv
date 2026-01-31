package com.kiduyu.klaus.kiduyutv.Api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.ConnectionPool;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.Dns;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ApiClient - Singleton OkHttpClient manager with persistent CookieJar
 *
 * This class provides a shared OkHttpClient instance across the entire application,
 * ensuring consistent session management and cookie handling, especially critical
 * for Cloudflare-protected streaming sources.
 *
 * Key Features:
 * - Singleton pattern for application-wide client sharing
 * - Persistent CookieJar using SharedPreferences for session continuity
 * - Custom User-Agent header for all requests
 * - Connection pooling and retry mechanisms
 * - DNS caching for improved performance
 *
 * Usage:
 *   OkHttpClient client = ApiClient.getClient(context);
 *   // Use client for all network requests
 */
public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String PREFS_NAME = "api_client_prefs";
    private static final String COOKIES_KEY = "http_cookies";

    // Configuration constants
    private static final int TIMEOUT_SECONDS = 30;
    private static final int MAX_IDLE_CONNECTIONS = 10;
    private static final int KEEP_ALIVE_DURATION_MINUTES = 5;
    private static final long DNS_CACHE_DURATION_MS = 5 * 60 * 1000; // 5 minutes
    private static final int MAX_DNS_CACHE_SIZE = 100; // Prevent unbounded growth

    // Default User-Agent mimicking a modern desktop browser
    // Updated to a recent stable version
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Singleton instance
    private static volatile ApiClient instance;
    private final OkHttpClient client;
    private final Context context;
    private final PersistentCookieJar cookieJar;

    // Enable/disable debug logging
    private static boolean debugLogging = false;

    /**
     * Private constructor - use getInstance() or getClient()
     */
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        this.cookieJar = new PersistentCookieJar(this.context);
        this.client = buildClient();
        Log.i(TAG, "ApiClient singleton initialized");
    }

    /**
     * Get the singleton ApiClient instance
     */
    public static ApiClient getInstance(Context context) {
        if (instance == null) {
            synchronized (ApiClient.class) {
                if (instance == null) {
                    instance = new ApiClient(context);
                }
            }
        }
        return instance;
    }

    /**
     * Get the shared OkHttpClient instance
     * This is the main method to use for network requests
     */
    public static OkHttpClient getClient(Context context) {
        return getInstance(context).client;
    }

    /**
     * Clear all stored cookies (useful for logout or troubleshooting)
     */
    public static void clearCookies(Context context) {
        getInstance(context).cookieJar.clearCookies();
    }

    /**
     * Enable or disable debug logging
     */
    public static void setDebugLogging(boolean enabled) {
        debugLogging = enabled;
    }

    /**
     * Build the OkHttpClient with all configurations
     */
    private OkHttpClient buildClient() {
        // Create connection pool for connection reuse
        ConnectionPool connectionPool = new ConnectionPool(
                MAX_IDLE_CONNECTIONS,
                KEEP_ALIVE_DURATION_MINUTES,
                TimeUnit.MINUTES
        );

        return new OkHttpClient.Builder()
                // Connection timeouts
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)

                // Connection pooling
                .connectionPool(connectionPool)

                // Retry and redirect settings
                .retryOnConnectionFailure(true)
                .followRedirects(true)
                .followSslRedirects(true)

                // Protocol preference (HTTP/2 for better performance)
                .protocols(Arrays.asList(Protocol.HTTP_2, Protocol.HTTP_1_1))

                // Custom DNS with caching
                .dns(new CachedDnsSelector())

                // Interceptors
                .addInterceptor(new UserAgentInterceptor(DEFAULT_USER_AGENT))
                .addInterceptor(new ConnectionKeepAliveInterceptor())
                .addInterceptor(new RetryInterceptor(3))
                .addNetworkInterceptor(new LoggingInterceptor())

                // Cookie jar for session persistence
                .cookieJar(cookieJar)

                .build();
    }

    /**
     * Get the User-Agent string being used
     */
    public static String getUserAgent() {
        return DEFAULT_USER_AGENT;
    }

    // ============================================
    // Custom CookieJar implementation
    // ============================================

    /**
     * PersistentCookieJar - CookieJar implementation using SharedPreferences
     *
     * This implementation:
     * - Stores cookies in SharedPreferences as JSON for persistence across app restarts
     * - Automatically handles cookie serialization/deserialization with all attributes
     * - Filters cookies by domain and path
     * - Expires old cookies automatically
     * - Thread-safe operations
     */
    private static class PersistentCookieJar implements CookieJar {
        private final SharedPreferences preferences;
        private final ConcurrentHashMap<String, List<Cookie>> cookieStore = new ConcurrentHashMap<>();
        private final Object saveLock = new Object();

        PersistentCookieJar(Context context) {
            this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            loadCookiesFromPrefs();
        }

        /**
         * Load cookies from SharedPreferences
         * Fixed: Now properly deserializes all cookie attributes from JSON
         */
        private void loadCookiesFromPrefs() {
            String cookiesJson = preferences.getString(COOKIES_KEY, null);
            if (cookiesJson != null && !cookiesJson.isEmpty()) {
                try {
                    JSONArray jsonArray = new JSONArray(cookiesJson);
                    int loadedCount = 0;

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject cookieJson = jsonArray.getJSONObject(i);
                        Cookie cookie = deserializeCookie(cookieJson);

                        if (cookie != null && cookie.expiresAt() >= System.currentTimeMillis()) {
                            String domain = cookie.domain();
                            List<Cookie> domainCookies = cookieStore.get(domain);
                            if (domainCookies == null) {
                                domainCookies = new ArrayList<>();
                                cookieStore.put(domain, domainCookies);
                            }
                            domainCookies.add(cookie);
                            loadedCount++;
                        }
                    }

                    if (debugLogging) {
                        Log.i(TAG, "Loaded " + loadedCount + " cookies from storage");
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "Error loading cookies from JSON", e);
                    // Clear corrupted data
                    preferences.edit().remove(COOKIES_KEY).apply();
                }
            }
        }

        /**
         * Save cookies to SharedPreferences as JSON
         * Fixed: Now properly serializes all cookie attributes
         */
        private void saveCookiesToPrefs() {
            synchronized (saveLock) {
                try {
                    JSONArray jsonArray = new JSONArray();
                    long now = System.currentTimeMillis();
                    int savedCount = 0;

                    for (List<Cookie> cookies : cookieStore.values()) {
                        for (Cookie cookie : cookies) {
                            // Only save non-expired cookies
                            if (cookie.expiresAt() >= now) {
                                JSONObject cookieJson = serializeCookie(cookie);
                                jsonArray.put(cookieJson);
                                savedCount++;
                            }
                        }
                    }

                    if (savedCount > 0) {
                        preferences.edit()
                                .putString(COOKIES_KEY, jsonArray.toString())
                                .apply();
                    } else {
                        preferences.edit().remove(COOKIES_KEY).apply();
                    }

                    if (debugLogging) {
                        Log.i(TAG, "Saved " + savedCount + " cookies to storage");
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "Error saving cookies to JSON", e);
                }
            }
        }

        /**
         * Serialize a Cookie to JSON with all attributes
         */
        private JSONObject serializeCookie(Cookie cookie) throws JSONException {
            JSONObject json = new JSONObject();
            json.put("name", cookie.name());
            json.put("value", cookie.value());
            json.put("expiresAt", cookie.expiresAt());
            json.put("domain", cookie.domain());
            json.put("path", cookie.path());
            json.put("secure", cookie.secure());
            json.put("httpOnly", cookie.httpOnly());
            json.put("hostOnly", cookie.hostOnly());
            json.put("persistent", cookie.persistent());
            return json;
        }

        /**
         * Deserialize a Cookie from JSON
         */
        private Cookie deserializeCookie(JSONObject json) {
            try {
                Cookie.Builder builder = new Cookie.Builder();

                builder.name(json.getString("name"));
                builder.value(json.getString("value"));
                builder.expiresAt(json.getLong("expiresAt"));

                String domain = json.getString("domain");
                if (json.getBoolean("hostOnly")) {
                    builder.hostOnlyDomain(domain);
                } else {
                    builder.domain(domain);
                }

                builder.path(json.getString("path"));

                if (json.getBoolean("secure")) {
                    builder.secure();
                }

                if (json.getBoolean("httpOnly")) {
                    builder.httpOnly();
                }

                return builder.build();

            } catch (JSONException | IllegalArgumentException e) {
                Log.e(TAG, "Error deserializing cookie", e);
                return null;
            }
        }

        @Override
        public synchronized void saveFromResponse(okhttp3.HttpUrl url, List<Cookie> cookies) {
            String domain = url.host();
            long now = System.currentTimeMillis();

            // Filter out expired cookies
            List<Cookie> validCookies = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (cookie.expiresAt() >= now) {
                    validCookies.add(cookie);
                }
            }

            if (!validCookies.isEmpty()) {
                // Merge with existing cookies for this domain
                List<Cookie> existingCookies = cookieStore.get(domain);
                if (existingCookies == null) {
                    existingCookies = new ArrayList<>();
                }

                // Remove cookies with the same name (update existing)
                Iterator<Cookie> iterator = existingCookies.iterator();
                while (iterator.hasNext()) {
                    Cookie existing = iterator.next();
                    for (Cookie newCookie : validCookies) {
                        if (existing.name().equals(newCookie.name())) {
                            iterator.remove();
                            break;
                        }
                    }
                }

                // Add new cookies
                existingCookies.addAll(validCookies);
                cookieStore.put(domain, existingCookies);

                // Save to preferences
                saveCookiesToPrefs();

                // Log Cloudflare cookies
                if (debugLogging) {
                    for (Cookie cookie : validCookies) {
                        if (cookie.name().equals("cf_clearance") ||
                                cookie.name().equals("__cf_bm") ||
                                cookie.name().equals("__cfduid")) {
                            Log.i(TAG, "Saved Cloudflare cookie: " + cookie.name());
                        }
                    }
                }
            }
        }

        @Override
        public synchronized List<Cookie> loadForRequest(okhttp3.HttpUrl url) {
            String domain = url.host();
            List<Cookie> result = new ArrayList<>();
            long now = System.currentTimeMillis();

            // Iterate through all stored cookies
            for (ConcurrentHashMap.Entry<String, List<Cookie>> entry : cookieStore.entrySet()) {
                String storedDomain = entry.getKey();

                // Domain match (subdomain-safe)
                if (domain.equals(storedDomain) || domain.endsWith("." + storedDomain)) {
                    List<Cookie> cookies = entry.getValue();

                    // Use traditional iteration instead of streams (API 24+ compatibility)
                    Iterator<Cookie> iterator = cookies.iterator();
                    while (iterator.hasNext()) {
                        Cookie cookie = iterator.next();

                        if (cookie.expiresAt() < now) {
                            iterator.remove(); // Remove expired cookie
                        } else if (cookie.matches(url)) {
                            result.add(cookie);
                        }
                    }

                    // Remove domain if no cookies left
                    if (cookies.isEmpty()) {
                        cookieStore.remove(storedDomain);
                    }
                }
            }

            if (debugLogging && !result.isEmpty()) {
                Log.i(TAG, "Sending " + result.size() + " cookies for " + domain);
            }

            return result;
        }

        /**
         * Clear all stored cookies
         */
        public synchronized void clearCookies() {
            cookieStore.clear();
            preferences.edit().remove(COOKIES_KEY).apply();
            Log.i(TAG, "All cookies cleared");
        }
    }

    // ============================================
    // Custom Interceptors
    // ============================================

    /**
     * UserAgentInterceptor - Adds User-Agent header to all requests
     */
    private static class UserAgentInterceptor implements Interceptor {
        private final String userAgent;

        UserAgentInterceptor(String userAgent) {
            this.userAgent = userAgent;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request requestWithUserAgent = originalRequest.newBuilder()
                    .header("User-Agent", userAgent)
                    .build();
            return chain.proceed(requestWithUserAgent);
        }
    }

    /**
     * ConnectionKeepAliveInterceptor - Adds keep-alive headers
     */
    private static class ConnectionKeepAliveInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request originalRequest = chain.request();
            Request request = originalRequest.newBuilder()
                    .header("Connection", "keep-alive")
                    .header("Keep-Alive", "timeout=300, max=1000")
                    .build();
            return chain.proceed(request);
        }
    }

    /**
     * RetryInterceptor - Retries failed requests with exponential backoff
     * Fixed: Now handles both IOExceptions and retryable HTTP errors (500, 502, 503, 504)
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;

        // HTTP status codes that should be retried
        private static final int[] RETRYABLE_STATUS_CODES = {500, 502, 503, 504};

        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;

            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    // Close previous response if exists
                    if (response != null) {
                        response.close();
                    }

                    response = chain.proceed(request);

                    // Success - return immediately
                    if (response.isSuccessful()) {
                        return response;
                    }

                    // Check if this is a retryable error
                    boolean shouldRetry = false;
                    for (int retryableCode : RETRYABLE_STATUS_CODES) {
                        if (response.code() == retryableCode) {
                            shouldRetry = true;
                            break;
                        }
                    }

                    // If not retryable, return the error response
                    if (!shouldRetry) {
                        return response;
                    }

                    // Log retry attempt
                    if (debugLogging && attempt < maxRetries - 1) {
                        Log.w(TAG, "HTTP " + response.code() + " error, retry " +
                                (attempt + 1) + "/" + maxRetries);
                    }

                } catch (IOException e) {
                    lastException = e;

                    if (debugLogging && attempt < maxRetries - 1) {
                        Log.w(TAG, "Request failed: " + e.getMessage() + ", retry " +
                                (attempt + 1) + "/" + maxRetries);
                    }

                    // If this is the last attempt, throw the exception
                    if (attempt == maxRetries - 1) {
                        throw e;
                    }
                }

                // Exponential backoff before retry (skip on last attempt)
                if (attempt < maxRetries - 1) {
                    try {
                        long backoffMs = (long) Math.pow(2, attempt) * 1000;
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        // If interrupted, throw the last exception or close response
                        if (lastException != null) {
                            throw lastException;
                        }
                        if (response != null) {
                            response.close();
                        }
                        throw new IOException("Retry interrupted", ie);
                    }
                }
            }

            // Should not reach here, but if we do, return the last response
            return response;
        }
    }

    /**
     * CachedDnsSelector - DNS resolver with in-memory caching
     * Fixed: Now uses ConcurrentHashMap for thread safety and implements cache size limits
     */
    private class CachedDnsSelector implements Dns {
        private final ConcurrentHashMap<String, List<InetAddress>> cache = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, Long> timestamps = new ConcurrentHashMap<>();
        private final AtomicInteger cacheSize = new AtomicInteger(0);

        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            // Check cache
            Long timestamp = timestamps.get(hostname);
            if (timestamp != null &&
                    (System.currentTimeMillis() - timestamp) < DNS_CACHE_DURATION_MS) {
                List<InetAddress> cached = cache.get(hostname);
                if (cached != null) {
                    if (debugLogging) {
                        Log.i(TAG, "Using cached DNS for: " + hostname);
                    }
                    return cached;
                }
            }

            // Perform DNS lookup
            List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);

            // Evict old entries if cache is full
            if (cacheSize.get() >= MAX_DNS_CACHE_SIZE) {
                evictOldestEntry();
            }

            // Store in cache
            cache.put(hostname, addresses);
            timestamps.put(hostname, System.currentTimeMillis());
            cacheSize.incrementAndGet();

            return addresses;
        }

        /**
         * Evict the oldest entry from the cache
         */
        private void evictOldestEntry() {
            String oldestHost = null;
            long oldestTime = Long.MAX_VALUE;

            for (ConcurrentHashMap.Entry<String, Long> entry : timestamps.entrySet()) {
                if (entry.getValue() < oldestTime) {
                    oldestTime = entry.getValue();
                    oldestHost = entry.getKey();
                }
            }

            if (oldestHost != null) {
                cache.remove(oldestHost);
                timestamps.remove(oldestHost);
                cacheSize.decrementAndGet();

                if (debugLogging) {
                    Log.i(TAG, "Evicted DNS cache entry: " + oldestHost);
                }
            }
        }
    }

    /**
     * LoggingInterceptor - Logs request/response information
     * Fixed: Now respects debugLogging flag
     */
    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (debugLogging) {
                Log.i(TAG, "[NETWORK] Request: " + request.url());
            }

            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(request);
            long endTime = System.currentTimeMillis();

            if (debugLogging) {
                Log.i(TAG, "[NETWORK] Response: " + response.code() + " in " +
                        (endTime - startTime) + "ms");
            }

            return response;
        }
    }
}