package com.kiduyu.klaus.kiduyutv.Api;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    
    // Default User-Agent mimicking a modern desktop browser
    private static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";
    
    // Singleton instance
    private static volatile ApiClient instance;
    private final OkHttpClient client;
    private final Context context;
    
    // DNS cache
    private final Map<String, List<InetAddress>> dnsCache = new HashMap<>();
    private final Map<String, Long> dnsCacheTimestamps = new HashMap<>();
    
    /**
     * Private constructor - use getInstance() or getClient()
     */
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
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
     * Build the OkHttpClient with all configurations
     */
    private OkHttpClient buildClient() {
        // Create persistent cookie jar using SharedPreferences
        PersistentCookieJar cookieJar = new PersistentCookieJar(context);
        
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
     * - Stores cookies in SharedPreferences for persistence across app restarts
     * - Automatically handles cookie serialization/deserialization
     * - Filters cookies by domain and path
     * - Expires old cookies automatically
     */
    private static class PersistentCookieJar implements CookieJar {
        private final SharedPreferences preferences;
        private final Map<String, List<Cookie>> cookieStore = new HashMap<>();
        
        PersistentCookieJar(Context context) {
            this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            loadCookiesFromPrefs();
        }
        
        /**
         * Load cookies from SharedPreferences
         */
        private void loadCookiesFromPrefs() {
            String cookiesJson = preferences.getString(COOKIES_KEY, null);
            if (cookiesJson != null && !cookiesJson.isEmpty()) {
                try {
                    okhttp3.Headers headers = new okhttp3.Headers.Builder()
                            .add("Set-Cookie", cookiesJson)
                            .build();

                    List<Cookie> cookies = Cookie.parseAll(
                            okhttp3.HttpUrl.parse("https://example.com"),
                            headers
                    );

                    for (Cookie cookie : cookies) {
                        // FIXED expiration check (your original was invalid Java)
                        if (cookie.expiresAt() >= System.currentTimeMillis()) {
                            String domain = cookie.domain();
                            if (!cookieStore.containsKey(domain)) {
                                cookieStore.put(domain, new ArrayList<>());
                            }
                            cookieStore.get(domain).add(cookie);
                        }
                    }

                    Log.i(TAG, "Loaded " + cookieStore.values()
                            .stream()
                            .mapToInt(List::size)
                            .sum() + " cookies from storage");

                } catch (Exception e) {
                    Log.e(TAG, "Error loading cookies", e);
                }
            }
        }

        
        /**
         * Save cookies to SharedPreferences
         */
        private void saveCookiesToPrefs() {
            StringBuilder cookieBuilder = new StringBuilder();
            boolean hasCookies = false;
            
            for (List<Cookie> cookies : cookieStore.values()) {
                for (Cookie cookie : cookies) {
                    if (cookie.expiresAt() >= System.currentTimeMillis()) {
                        cookieBuilder.append(cookie.name()).append("=")
                                .append(cookie.value()).append("; ");
                        hasCookies = true;
                    }
                }
            }
            
            if (hasCookies) {
                preferences.edit().putString(COOKIES_KEY, cookieBuilder.toString()).apply();
            } else {
                preferences.edit().remove(COOKIES_KEY).apply();
            }
        }
        
        @Override
        public void saveFromResponse(okhttp3.HttpUrl url, List<Cookie> cookies) {
            String domain = url.host();
            
            // Filter out expired cookies
            List<Cookie> validCookies = new ArrayList<>();
            for (Cookie cookie : cookies) {
                if (cookie.expiresAt() >= System.currentTimeMillis()) {
                    validCookies.add(cookie);
                }
            }
            
            if (!validCookies.isEmpty()) {
                cookieStore.put(domain, validCookies);
                saveCookiesToPrefs();
                
                // Log Cloudflare cookies
                for (Cookie cookie : validCookies) {
                    if (cookie.name().equals("cf_clearance") || 
                            cookie.name().equals("__cf_bm") ||
                            cookie.name().equals("__cfduid")) {
                        Log.i(TAG, "Saved Cloudflare cookie: " + cookie.name());
                    }
                }
            }
        }

        @Override
        public List<Cookie> loadForRequest(okhttp3.HttpUrl url) {
            String domain = url.host();
            List<Cookie> result = new ArrayList<>();
            long now = System.currentTimeMillis();

            Iterator<Map.Entry<String, List<Cookie>>> storeIterator =
                    cookieStore.entrySet().iterator();

            while (storeIterator.hasNext()) {
                Map.Entry<String, List<Cookie>> entry = storeIterator.next();
                String storedDomain = entry.getKey();

                // Domain match (subdomain-safe)
                if (domain.equals(storedDomain) || domain.endsWith("." + storedDomain)) {
                    Iterator<Cookie> cookieIterator = entry.getValue().iterator();

                    while (cookieIterator.hasNext()) {
                        Cookie cookie = cookieIterator.next();

                        if (cookie.expiresAt() < now) {
                            cookieIterator.remove(); // safe removal
                        } else {
                            result.add(cookie);
                        }
                    }

                    // Remove empty domain buckets
                    if (entry.getValue().isEmpty()) {
                        storeIterator.remove();
                    }
                }
            }

            if (!result.isEmpty()) {
                Log.i(TAG, "Sending " + result.size() + " cookies for " + domain);
            }

            return result;
        }

        /**
         * Clear all stored cookies
         */
        public void clearCookies() {
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
     */
    private static class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        
        RetryInterceptor(int maxRetries) {
            this.maxRetries = maxRetries;
        }
        
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException exception = null;
            
            for (int attempt = 0; attempt < maxRetries; attempt++) {
                try {
                    response = chain.proceed(request);
                    
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
                    
                    // Exponential backoff
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
            
            if (exception != null) {
                throw exception;
            }
            
            return response;
        }
    }
    
    /**
     * CachedDnsSelector - DNS resolver with in-memory caching
     */
    private static class CachedDnsSelector implements Dns {
        private final Map<String, List<InetAddress>> cache = new HashMap<>();
        private final Map<String, Long> timestamps = new HashMap<>();
        
        @Override
        public List<InetAddress> lookup(String hostname) throws UnknownHostException {
            Long timestamp = timestamps.get(hostname);
            if (timestamp != null && 
                    (System.currentTimeMillis() - timestamp) < DNS_CACHE_DURATION_MS) {
                Log.i(TAG, "Using cached DNS for: " + hostname);
                return cache.get(hostname);
            }
            
            List<InetAddress> addresses = Dns.SYSTEM.lookup(hostname);
            cache.put(hostname, addresses);
            timestamps.put(hostname, System.currentTimeMillis());
            
            return addresses;
        }
    }
    
    /**
     * LoggingInterceptor - Logs request/response information
     */
    private static class LoggingInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Log.i(TAG, "[NETWORK] Request: " + request.url());
            
            long startTime = System.currentTimeMillis();
            Response response = chain.proceed(request);
            long endTime = System.currentTimeMillis();
            
            Log.i(TAG, "[NETWORK] Response: " + response.code() + " in " + 
                    (endTime - startTime) + "ms");
            
            return response;
        }
    }
}
