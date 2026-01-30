package com.kiduyu.klaus.kiduyutv.utils;

import static com.google.common.base.Preconditions.checkNotNull;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.C;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.BaseDataSource;
import androidx.media3.datasource.DataSourceException;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.datasource.HttpUtil;
import androidx.media3.datasource.TransferListener;
import androidx.media3.common.MediaItem;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.google.common.base.Predicate;
import com.google.common.collect.ForwardingMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.kiduyu.klaus.kiduyutv.Api.ApiClient;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.NoRouteToHostException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import javax.net.ssl.HttpsURLConnection;

/**
 * KiduyuDataSource - Custom HTTP DataSource for ExoPlayer streaming
 *
 * This class provides enhanced HTTP data source functionality similar to AnimeTV's AnimeDataSource,
 * with support for:
 * - Custom header injection (Origin, Referer, User-Agent, Cookies)
 * - Dynamic header configuration based on streaming domains
 * - SSL certificate bypass for problematic hosts
 * - Connection pooling and keep-alive
 * - Redirect handling (up to 20 redirects)
 * - Cookie management for Cloudflare-protected streams
 * - DNS over HTTPS support
 *
 * Usage:
 * <pre>
 * // Create factory with custom headers
 * Map&lt;String, String&gt; headers = new HashMap&lt;&gt;();
 * headers.put("Cookie", "cf_clearance=...");
 * headers.put("Referer", "https://streaming-site.com/");
 *
 * KiduyuDataSource.Factory factory = new KiduyuDataSource.Factory(context)
 *     .setUserAgent("Mozilla/5.0...")
 *     .setDefaultRequestProperties(headers)
 *     .setAllowCrossProtocolRedirects(true);
 *
 * // Use with HLS source
 * HlsMediaSource.Factory hlsFactory = new HlsMediaSource.Factory(factory);
 * </pre>
 */
@UnstableApi
public class KiduyuDataSource extends BaseDataSource implements HttpDataSource {

    private static final String TAG = "KiduyuDataSource";
    private static final int MAX_REDIRECTS = 20;
    private static final int HTTP_STATUS_TEMPORARY_REDIRECT = 307;
    private static final int HTTP_STATUS_PERMANENT_REDIRECT = 308;
    private static final long MAX_BYTES_TO_DRAIN = 2048;

    // Default timeouts
    public static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 8000;
    public static final int DEFAULT_READ_TIMEOUT_MILLIS = 8000;

    // Known streaming domains for automatic header configuration
    private static final Map<String, DomainConfig> STREAMING_DOMAINS = new HashMap<>();

    // Static shared OkHttpClient reference for ExoPlayer integration
    private static okhttp3.OkHttpClient sharedOkHttpClient = null;

    // Shared cookie store for Cloudflare sessions
    private static final ConcurrentHashMap<String, List<Cookie>> sharedCookieStore = new ConcurrentHashMap<>();

    static {
        // Initialize known streaming domains with their expected headers
        // These domains require specific Origin/Referer headers to avoid 403 errors

        // Video hosting platforms
        STREAMING_DOMAINS.put("mp4upload.com", new DomainConfig(
                "https://mp4upload.com",
                "https://www.mp4upload.com/",
                false
        ));

        STREAMING_DOMAINS.put("cloudvideo", new DomainConfig(
                "https://cloudvideo.tv",
                "https://cloudvideo.tv/",
                false
        ));

        STREAMING_DOMAINS.put("gogoanime", new DomainConfig(
                "https://gogoanime.gg",
                "https://gogoanime.gg/",
                false
        ));

        STREAMING_DOMAINS.put("streamtape", new DomainConfig(
                "https://streamtape.com",
                "https://streamtape.com/",
                false
        ));

        STREAMING_DOMAINS.put("videovard", new DomainConfig(
                "https://videovard.com",
                "https://videovard.com/",
                false
        ));

        STREAMING_DOMAINS.put("mega", new DomainConfig(
                "https://mega.nz",
                "https://mega.nz/",
                false
        ));

        // Additional streaming domains commonly used
        STREAMING_DOMAINS.put("vidstreaming", new DomainConfig(
                "https://vidstreaming.io",
                "https://vidstreaming.io/",
                false
        ));

        STREAMING_DOMAINS.put("mystream", new DomainConfig(
                "https://mystream.to",
                "https://mystream.to/",
                false
        ));

        STREAMING_DOMAINS.put("upstream", new DomainConfig(
                "https://upstream.to",
                "https://upstream.to/",
                false
        ));

        STREAMING_DOMAINS.put("streamlare", new DomainConfig(
                "https://streamlare.com",
                "https://streamlare.com/",
                false
        ));

        STREAMING_DOMAINS.put("videovip", new DomainConfig(
                "https://videovip.org",
                "https://videovip.org/",
                false
        ));

        STREAMING_DOMAINS.put("ok.ru", new DomainConfig(
                "https://ok.ru",
                "https://ok.ru/",
                false
        ));

        STREAMING_DOMAINS.put("vk.com", new DomainConfig(
                "https://vk.com",
                "https://vk.com/",
                false
        ));

        // Cloudflare-protected streaming CDNs
        STREAMING_DOMAINS.put("megacloud", new DomainConfig(
                "https://megacloud.blog",
                "https://megacloud.blog/",
                false
        ));

        STREAMING_DOMAINS.put("vidfast", new DomainConfig(
                "https://vidfast.org",
                "https://vidfast.org/",
                false
        ));

        STREAMING_DOMAINS.put("vidsfly", new DomainConfig(
                "https://vidsfly.com",
                "https://vidsfly.com/",
                false
        ));

        STREAMING_DOMAINS.put("vidbeehive", new DomainConfig(
                "https://vidbeehive.com",
                "https://vidbeehive.com/",
                false
        ));

        // Video API domains (commonly used in streaming apps)
        STREAMING_DOMAINS.put("videasy.net", new DomainConfig(
                "https://videasy.net",
                "https://videasy.net/",
                false
        ));

        STREAMING_DOMAINS.put("vidlink.pro", new DomainConfig(
                "https://vidlink.pro",
                "https://vidlink.pro/",
                false
        ));

        STREAMING_DOMAINS.put("smashystream.top", new DomainConfig(
                "https://smashystream.top",
                "https://smashystream.top/",
                false
        ));

        STREAMING_DOMAINS.put("hexa.su", new DomainConfig(
                "https://hexa.su",
                "https://hexa.su/",
                false
        ));

        // =====================================================
        // CLOUDFLARE WORKERS DOMAINS - CRITICAL FOR 403 BYPASS
        // These domains require specific Origin headers to avoid 403 errors
        // =====================================================

        // TechParadise Workers domains (frostveil88.live streaming)
        STREAMING_DOMAINS.put("techparadise-92b.workers.dev", new DomainConfig(
                "https://frostveil88.live",
                "https://frostveil88.live/",
                false
        ));

        // VOD Video proxy domains
        STREAMING_DOMAINS.put("vodvidl.site", new DomainConfig(
                "https://vodvidl.site",
                "https://vodvidl.site/",
                false
        ));

        // 10015 Workers proxy domains (commonly used for streaming)
        STREAMING_DOMAINS.put("10015.workers.dev", new DomainConfig(
                "https://frostveil88.live",
                "https://frostveil88.live/",
                false
        ));

        // Generic workers.dev domains - catch-all for dynamic workers
        STREAMING_DOMAINS.put("workers.dev", new DomainConfig(
                "https://frostveil88.live",
                "https://frostveil88.live/",
                false
        ));

        // Live streaming domains commonly used with workers
        STREAMING_DOMAINS.put("frostveil88.live", new DomainConfig(
                "https://frostveil88.live",
                "https://frostveil88.live/",
                false
        ));

        // Additional video proxy/CDN domains
        STREAMING_DOMAINS.put("megafiles.store", new DomainConfig(
                "https://megafiles.store",
                "https://megafiles.store/",
                false
        ));

        STREAMING_DOMAINS.put("streamhub.tv", new DomainConfig(
                "https://streamhub.tv",
                "https://streamhub.tv/",
                false
        ));

        Log.i(TAG, "Initialized " + STREAMING_DOMAINS.size() + " streaming domain configurations");
    }

    /**
     * Initialize KiduyuDataSource with the shared OkHttpClient from FetchStreams.
     * This ensures cookies and connection pooling are shared between API calls and playback.
     */
    public static void initializeWithSharedClient(okhttp3.OkHttpClient client) {
        sharedOkHttpClient = client;
        Log.i(TAG, "✓ KiduyuDataSource initialized with shared OkHttpClient");
    }

    /**
     * Get the shared OkHttpClient for use with OkHttpDataSource in ExoPlayer.
     * Returns null if not initialized (will use default factory behavior).
     */
    public static okhttp3.OkHttpClient getSharedOkHttpClient() {
        return sharedOkHttpClient;
    }

    /**
     * Add Cloudflare clearance cookie to shared store
     */
    public static void addCloudflareCookie(String domain, String cookieValue) {
        try {
            HttpUrl url = HttpUrl.parse("https://" + domain + "/");
            if (url != null) {
                Cookie cookie = Cookie.parse(url, "cf_clearance=" + cookieValue + "; path=/; secure; HttpOnly; SameSite=Lax");
                if (cookie != null) {
                    List<Cookie> cookies = new ArrayList<>();
                    cookies.add(cookie);
                    sharedCookieStore.put(domain, cookies);
                    Log.i(TAG, "✓ Added cf_clearance cookie for domain: " + domain);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adding Cloudflare cookie: " + e.getMessage());
        }
    }

    /**
     * Get cookies for a domain from shared store
     */
    public static List<Cookie> getCookiesForDomain(String domain) {
        return sharedCookieStore.get(domain);
    }

    /**
     * Check if we have Cloudflare cookies for a domain
     */
    public static boolean hasCloudflareCookies(String domain) {
        List<Cookie> cookies = sharedCookieStore.get(domain);
        if (cookies == null) return false;
        for (Cookie cookie : cookies) {
            if (cookie.name().equals("cf_clearance") || cookie.name().equals("__cf_bm")) {
                return true;
            }
        }
        return false;
    }

    // Instance variables
    private final boolean allowCrossProtocolRedirects;
    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    @Nullable private final String userAgent;
    @Nullable private final RequestProperties defaultRequestProperties;
    private final RequestProperties requestProperties;
    private final boolean keepPostFor302Redirects;
    @Nullable private final String sslBypassHost;
    private final boolean enableKeepAlive;
    private final int maxRedirects;

    @Nullable private Predicate<String> contentTypePredicate;
    @Nullable private DataSpec dataSpec;
    @Nullable private HttpURLConnection connection;
    @Nullable private InputStream inputStream;
    private boolean opened;
    private int responseCode;
    private long bytesToRead;
    private long bytesRead;

    /**
     * Factory for creating KiduyuDataSource instances
     */
    public static final class Factory implements HttpDataSource.Factory {

        private final RequestProperties defaultRequestProperties;
        private int connectTimeoutMs = DEFAULT_CONNECT_TIMEOUT_MILLIS;
        private int readTimeoutMs = DEFAULT_READ_TIMEOUT_MILLIS;
        private boolean allowCrossProtocolRedirects = false;
        private boolean keepPostFor302Redirects = false;
        private boolean enableKeepAlive = true;
        private int maxRedirects = MAX_REDIRECTS;
        @Nullable private String userAgent;
        @Nullable private Predicate<String> contentTypePredicate;
        @Nullable private TransferListener transferListener;
        @Nullable private String sslBypassHost;

        public Factory() {
            this.defaultRequestProperties = new RequestProperties();
        }

        /**
         * Sets the user agent string for HTTP requests
         */
        public Factory setUserAgent(@Nullable String userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        /**
         * Sets the connection timeout in milliseconds
         */
        public Factory setConnectTimeoutMs(int connectTimeoutMs) {
            this.connectTimeoutMs = connectTimeoutMs;
            return this;
        }

        /**
         * Sets the read timeout in milliseconds
         */
        public Factory setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
            return this;
        }

        /**
         * Sets whether to allow cross-protocol redirects (HTTP to HTTPS)
         */
        public Factory setAllowCrossProtocolRedirects(boolean allow) {
            this.allowCrossProtocolRedirects = allow;
            return this;
        }

        /**
         * Sets whether to keep POST body for 302 redirects
         */
        public Factory setKeepPostFor302Redirects(boolean keep) {
            this.keepPostFor302Redirects = keep;
            return this;
        }

        /**
         * Sets whether to enable keep-alive connections
         */
        public Factory setEnableKeepAlive(boolean enable) {
            this.enableKeepAlive = enable;
            return this;
        }

        /**
         * Sets the maximum number of redirects to follow
         */
        public Factory setMaxRedirects(int maxRedirects) {
            this.maxRedirects = maxRedirects;
            return this;
        }

        /**
         * Sets default request properties (headers) for all requests
         */
        public Factory setDefaultRequestProperties(Map<String, String> properties) {
            this.defaultRequestProperties.clearAndSet(properties);
            return this;
        }

        /**
         * Sets a content type predicate for validating response content types
         */
        public Factory setContentTypePredicate(@Nullable Predicate<String> predicate) {
            this.contentTypePredicate = predicate;
            return this;
        }

        /**
         * Sets the transfer listener for monitoring data transfers
         */
        public Factory setTransferListener(@Nullable TransferListener listener) {
            this.transferListener = listener;
            return this;
        }

        /**
         * Sets a host for which SSL certificate validation will be bypassed
         * WARNING: This weakens security and should only be used for known problematic hosts
         */
        public Factory setSslBypassHost(@Nullable String host) {
            this.sslBypassHost = host;
            return this;
        }

        @Override
        public KiduyuDataSource createDataSource() {
            KiduyuDataSource dataSource = new KiduyuDataSource(
                    userAgent,
                    connectTimeoutMs,
                    readTimeoutMs,
                    allowCrossProtocolRedirects,
                    defaultRequestProperties,
                    contentTypePredicate,
                    keepPostFor302Redirects,
                    sslBypassHost,
                    enableKeepAlive,
                    maxRedirects
            );
            if (transferListener != null) {
                dataSource.addTransferListener(transferListener);
            }
            return dataSource;
        }
    }

    /**
     * Domain configuration for automatic header injection
     */
    private static class DomainConfig {
        final String origin;
        final String referer;
        final boolean sslBypass;

        DomainConfig(String origin, String referer, boolean sslBypass) {
            this.origin = origin;
            this.referer = referer;
            this.sslBypass = sslBypass;
        }
    }

    // Private constructor - use Factory instead
    private KiduyuDataSource(
            @Nullable String userAgent,
            int connectTimeoutMillis,
            int readTimeoutMillis,
            boolean allowCrossProtocolRedirects,
            @Nullable RequestProperties defaultRequestProperties,
            @Nullable Predicate<String> contentTypePredicate,
            boolean keepPostFor302Redirects,
            @Nullable String sslBypassHost,
            boolean enableKeepAlive,
            int maxRedirects) {
        super(true);
        this.userAgent = userAgent;
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
        this.allowCrossProtocolRedirects = allowCrossProtocolRedirects;
        this.defaultRequestProperties = defaultRequestProperties;
        this.contentTypePredicate = contentTypePredicate;
        this.requestProperties = new RequestProperties();
        this.keepPostFor302Redirects = keepPostFor302Redirects;
        this.sslBypassHost = sslBypassHost;
        this.enableKeepAlive = enableKeepAlive;
        this.maxRedirects = maxRedirects;
    }

    @Override
    @Nullable
    public Uri getUri() {
        return connection == null ? null : Uri.parse(connection.getURL().toString());
    }

    @Override
    public int getResponseCode() {
        return connection == null || responseCode <= 0 ? -1 : responseCode;
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        if (connection == null) {
            return ImmutableMap.of();
        }
        return new NullFilteringHeadersMap(connection.getHeaderFields());
    }

    @Override
    public void setRequestProperty(String name, String value) {
        checkNotNull(name);
        checkNotNull(value);
        requestProperties.set(name, value);
    }

    @Override
    public void clearRequestProperty(String name) {
        checkNotNull(name);
        requestProperties.remove(name);
    }

    @Override
    public void clearAllRequestProperties() {
        requestProperties.clear();
    }

    @Override
    public long open(DataSpec dataSpec) throws HttpDataSourceException {
        this.dataSpec = dataSpec;
        bytesRead = 0;
        bytesToRead = 0;

        transferInitializing(dataSpec);

        String responseMessage;
        HttpURLConnection connection;
        try {
            this.connection = makeConnection(dataSpec);
            connection = this.connection;
            responseCode = connection.getResponseCode();
            responseMessage = connection.getResponseMessage();
        } catch (IOException e) {
            closeConnectionQuietly();
            throw HttpDataSourceException.createForIOException(
                    e, dataSpec, HttpDataSourceException.TYPE_OPEN);
        }

        // Check for a valid response code
        if (responseCode < 200 || responseCode > 299) {
            Map<String, List<String>> headers = connection.getHeaderFields();
            if (responseCode == 416) {
                long documentSize = HttpUtil.getDocumentSize(
                        connection.getHeaderField("Content-Range"));
                if (dataSpec.position == documentSize) {
                    opened = true;
                    transferStarted(dataSpec);
                    return dataSpec.length != C.LENGTH_UNSET ? dataSpec.length : 0;
                }
            }

            @Nullable InputStream errorStream = connection.getErrorStream();
            byte[] errorResponseBody;
            try {
                errorResponseBody = errorStream != null ?
                        Util.toByteArray(errorStream) : Util.EMPTY_BYTE_ARRAY;
            } catch (IOException e) {
                errorResponseBody = Util.EMPTY_BYTE_ARRAY;
            }
            closeConnectionQuietly();

            @Nullable IOException cause = responseCode == 416 ?
                    new DataSourceException(
                            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE) : null;
            throw new InvalidResponseCodeException(
                    responseCode, responseMessage, cause, headers, dataSpec, errorResponseBody);
        }

        // Check for a valid content type
        String contentType = connection.getContentType();
        if (contentTypePredicate != null && !contentTypePredicate.apply(contentType)) {
            closeConnectionQuietly();
            throw new InvalidContentTypeException(contentType, dataSpec);
        }

        // Handle partial content responses
        long bytesToSkip = (responseCode == 200) && dataSpec.position != 0 ? dataSpec.position : 0;

        // Determine the length of the data to be read
        boolean isCompressed = isCompressed(connection);
        if (!isCompressed) {
            if (dataSpec.length != C.LENGTH_UNSET) {
                bytesToRead = dataSpec.length;
            } else {
                long contentLength = HttpUtil.getContentLength(
                        connection.getHeaderField("Content-Length"),
                        connection.getHeaderField("Content-Range"));
                bytesToRead = contentLength != C.LENGTH_UNSET ?
                        (contentLength - bytesToSkip) : C.LENGTH_UNSET;
            }
        } else {
            bytesToRead = dataSpec.length;
        }

        try {
            inputStream = connection.getInputStream();
            if (isCompressed) {
                inputStream = new GZIPInputStream(inputStream);
            }
        } catch (IOException e) {
            closeConnectionQuietly();
            throw new HttpDataSourceException(
                    e, dataSpec, PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSourceException.TYPE_OPEN);
        }

        opened = true;
        transferStarted(dataSpec);

        try {
            skipFully(bytesToSkip, dataSpec);
        } catch (IOException e) {
            closeConnectionQuietly();
            if (e instanceof HttpDataSourceException) {
                throw (HttpDataSourceException) e;
            }
            throw new HttpDataSourceException(
                    e, dataSpec, PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                    HttpDataSourceException.TYPE_OPEN);
        }

        return bytesToRead;
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws HttpDataSourceException {
        try {
            return readInternal(buffer, offset, length);
        } catch (IOException e) {
            throw HttpDataSourceException.createForIOException(
                    e, checkNotNull(dataSpec), HttpDataSourceException.TYPE_READ);
        }
    }

    @Override
    public void close() throws HttpDataSourceException {
        try {
            @Nullable InputStream inputStream = this.inputStream;
            if (inputStream != null) {
                long bytesRemaining = bytesToRead == C.LENGTH_UNSET ?
                        C.LENGTH_UNSET : bytesToRead - bytesRead;
                maybeTerminateInputStream(connection, bytesRemaining);
                try {
                    inputStream.close();
                } catch (IOException e) {
                    throw new HttpDataSourceException(
                            e, checkNotNull(dataSpec),
                            PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                            HttpDataSourceException.TYPE_CLOSE);
                }
            }
        } finally {
            inputStream = null;
            closeConnectionQuietly();
            if (opened) {
                opened = false;
                transferEnded();
            }
        }
    }

    /**
     * Creates HTTP connection with custom headers and automatic domain configuration
     */
    private HttpURLConnection makeConnection(DataSpec dataSpec) throws IOException {
        URL url = new URL(dataSpec.uri.toString());

        // Apply domain-specific header configuration
        Map<String, String> requestHeaders = buildRequestHeaders(url, dataSpec.httpRequestHeaders);

        if (!allowCrossProtocolRedirects && !keepPostFor302Redirects) {
            return makeConnection(
                    url,
                    dataSpec.httpMethod,
                    dataSpec.httpBody,
                    dataSpec.position,
                    dataSpec.length,
                    dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP),
                    true,
                    requestHeaders);
        }

        // Handle redirects manually for cross-protocol or POST preservation
        int redirectCount = 0;
        URL currentUrl = url;
        int httpMethod = dataSpec.httpMethod;
        @Nullable byte[] httpBody = dataSpec.httpBody;

        while (redirectCount++ <= maxRedirects) {
            HttpURLConnection connection = makeConnection(
                    currentUrl,
                    httpMethod,
                    httpBody,
                    dataSpec.position,
                    dataSpec.length,
                    dataSpec.isFlagSet(DataSpec.FLAG_ALLOW_GZIP),
                    false,
                    requestHeaders);

            int responseCode = connection.getResponseCode();
            String location = connection.getHeaderField("Location");

            // Check for redirect status codes
            boolean isRedirect = (httpMethod == DataSpec.HTTP_METHOD_GET ||
                    httpMethod == DataSpec.HTTP_METHOD_HEAD) &&
                    (responseCode == HttpURLConnection.HTTP_MULT_CHOICE ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                            responseCode == HTTP_STATUS_TEMPORARY_REDIRECT ||
                            responseCode == HTTP_STATUS_PERMANENT_REDIRECT);

            boolean isPostRedirect = httpMethod == DataSpec.HTTP_METHOD_POST &&
                    (responseCode == HttpURLConnection.HTTP_MULT_CHOICE ||
                            responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                            responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                            responseCode == HttpURLConnection.HTTP_SEE_OTHER);

            if (isRedirect) {
                connection.disconnect();
                currentUrl = handleRedirect(currentUrl, location, dataSpec);
            } else if (isPostRedirect) {
                connection.disconnect();
                boolean shouldKeepPost = keepPostFor302Redirects &&
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP;
                if (!shouldKeepPost) {
                    httpMethod = DataSpec.HTTP_METHOD_GET;
                    httpBody = null;
                }
                currentUrl = handleRedirect(currentUrl, location, dataSpec);
            } else {
                return connection;
            }
        }

        throw new HttpDataSourceException(
                new NoRouteToHostException("Too many redirects: " + redirectCount),
                dataSpec,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                HttpDataSourceException.TYPE_OPEN);
    }

    /**
     * Builds request headers with automatic domain configuration
     * CRITICAL: Domain config is applied FIRST to ensure proper Origin/Referer headers
     */
    private Map<String, String> buildRequestHeaders(URL url, Map<String, String> extraHeaders) {
        Map<String, String> headers = new HashMap<>();
        String host = url.getHost();

        // FIRST: Apply domain-specific configuration (highest priority)
        applyDomainConfiguration(host, headers);

        // SECOND: Add User-Agent
        if (userAgent != null) {
            headers.put("User-Agent", userAgent);
        }

        // THIRD: Add cookies from shared cookie store
        List<Cookie> cookies = getCookiesForDomain(host);
        if (cookies != null && !cookies.isEmpty()) {
            StringBuilder cookieHeader = new StringBuilder();
            for (int i = 0; i < cookies.size(); i++) {
                Cookie cookie = cookies.get(i);
                cookieHeader.append(cookie.name()).append("=").append(cookie.value());
                if (i < cookies.size() - 1) {
                    cookieHeader.append("; ");
                }
            }
            headers.put("Cookie", cookieHeader.toString());
            Log.d(TAG, "Applied cookies from shared store for: " + host);
        }

        // FOURTH: Add default request properties
        if (defaultRequestProperties != null) {
            for (Map.Entry<String, String> entry : defaultRequestProperties.getSnapshot().entrySet()) {
                headers.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        // FIFTH: Add instance-specific request properties
        for (Map.Entry<String, String> entry : requestProperties.getSnapshot().entrySet()) {
            headers.putIfAbsent(entry.getKey(), entry.getValue());
        }

        // SIXTH: Add extra headers from DataSpec (lowest priority)
        if (extraHeaders != null) {
            headers.putAll(extraHeaders);
        }

        // CRITICAL: Ensure Origin header when Referer is present
        if (headers.containsKey("Referer")) {
            String referer = headers.get("Referer");
            if (referer != null && !referer.isEmpty()) {
                try {
                    java.net.URL refererUrl = new java.net.URL(referer);
                    String origin = refererUrl.getProtocol() + "://" + refererUrl.getHost();
                    headers.putIfAbsent("Origin", origin);
                    Log.d(TAG, "Set Origin from Referer: " + origin);
                } catch (Exception e) {
                    Log.w(TAG, "Failed to extract Origin from Referer: " + referer);
                }
            }
        }

        // Add keep-alive headers if enabled
        if (enableKeepAlive) {
            headers.putIfAbsent("Connection", "keep-alive");
            headers.putIfAbsent("Keep-Alive", "timeout=300, max=1000");
        }

        // Add Accept headers if missing
        headers.putIfAbsent("Accept", "*/*");
        headers.putIfAbsent("Accept-Language", "en-US,en;q=0.9");
        headers.putIfAbsent("Accept-Encoding", "gzip, deflate, br");

        return headers;
    }

    /**
     * Applies domain-specific header configuration for known streaming domains
     * This is called FIRST in header building to ensure proper Origin/Referer headers
     */
    private void applyDomainConfiguration(String host, Map<String, String> headers) {
        // Check for known streaming domains
        for (Map.Entry<String, DomainConfig> entry : STREAMING_DOMAINS.entrySet()) {
            if (host.contains(entry.getKey())) {
                DomainConfig config = entry.getValue();

                // Set Origin
                if (config.origin != null) {
                    headers.put("Origin", config.origin);
                }

                // Set Referer
                if (config.referer != null) {
                    headers.put("Referer", config.referer);
                }

                Log.d(TAG, "Applied domain config for: " + host + " (Origin: " + config.origin + ")");
                return;
            }
        }

        // Dynamic configuration for unknown domains
        Log.d(TAG, "No specific config for domain: " + host + ", using defaults");
    }

    /**
     * Creates and configures a single HTTP connection
     */
    private HttpURLConnection makeConnection(
            URL url,
            @DataSpec.HttpMethod int httpMethod,
            @Nullable byte[] httpBody,
            long position,
            long length,
            boolean allowGzip,
            boolean followRedirects,
            Map<String, String> requestHeaders) throws IOException {

        HttpURLConnection connection = openConnection(url);

        // Configure SSL bypass for problematic hosts
        configureSslBypass(connection, url.getHost());

        // Set timeouts
        connection.setConnectTimeout(connectTimeoutMillis);
        connection.setReadTimeout(readTimeoutMillis);

        // Apply request headers
        for (Map.Entry<String, String> property : requestHeaders.entrySet()) {
            connection.setRequestProperty(property.getKey(), property.getValue());
        }

        // Add Range header for video streaming
        @Nullable String rangeHeader = HttpUtil.buildRangeRequestHeader(position, length);
        if (rangeHeader != null) {
            connection.setRequestProperty("Range", rangeHeader);
        }

        // Set encoding header
        connection.setRequestProperty("Accept-Encoding", allowGzip ? "gzip" : "identity");
        connection.setInstanceFollowRedirects(followRedirects);
        connection.setDoOutput(httpBody != null);
        connection.setRequestMethod(DataSpec.getStringForHttpMethod(httpMethod));

        // Write POST body if present
        if (httpBody != null) {
            connection.setFixedLengthStreamingMode(httpBody.length);
            connection.connect();
            OutputStream os = connection.getOutputStream();
            os.write(httpBody);
            os.close();
        } else {
            connection.connect();
        }

        return connection;
    }

    /**
     * Configures SSL certificate bypass for known problematic hosts
     */
    private void configureSslBypass(HttpURLConnection connection, String host) {
        if (sslBypassHost != null && host.contains(sslBypassHost)) {
            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConn = (HttpsURLConnection) connection;
                try {
                    // Import the SSL bypass utilities
                    Class<?> sslSocketFactoryClass = Class.forName(
                            "android.net.SSLCertificateSocketFactory");
                    Object insecureFactory = sslSocketFactoryClass.getMethod(
                            "getInsecure", int.class, null).invoke(null, 0, null);
                    httpsConn.setSSLSocketFactory(
                            (javax.net.ssl.SSLSocketFactory) insecureFactory);

                    // Use hostname verifier that accepts all hosts
                    Class<?> allowAllHostnameVerifierClass = Class.forName(
                            "org.apache.http.conn.ssl.AllowAllHostnameVerifier");
                    httpsConn.setHostnameVerifier(
                            (javax.net.ssl.HostnameVerifier) allowAllHostnameVerifierClass.newInstance());

                    Log.w(TAG, "SSL bypass enabled for host: " + host);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to configure SSL bypass", e);
                }
            }
        }
    }

    /**
     * Handles URL redirects
     */
    private URL handleRedirect(URL originalUrl, @Nullable String location, DataSpec dataSpec)
            throws HttpDataSourceException {
        if (location == null) {
            throw new HttpDataSourceException(
                    "Null location redirect",
                    dataSpec,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    HttpDataSourceException.TYPE_OPEN);
        }

        try {
            URL url = new URL(originalUrl, location);

            // Validate protocol
            String protocol = url.getProtocol();
            if (!"https".equals(protocol) && !"http".equals(protocol)) {
                throw new HttpDataSourceException(
                        "Unsupported protocol redirect: " + protocol,
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        HttpDataSourceException.TYPE_OPEN);
            }

            // Check for cross-protocol redirect
            if (!allowCrossProtocolRedirects && !protocol.equals(originalUrl.getProtocol())) {
                throw new HttpDataSourceException(
                        "Disallowed cross-protocol redirect",
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        HttpDataSourceException.TYPE_OPEN);
            }

            return url;
        } catch (MalformedURLException e) {
            throw new HttpDataSourceException(
                    e, dataSpec,
                    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                    HttpDataSourceException.TYPE_OPEN);
        }
    }

    /**
     * Skips the specified number of bytes
     */
    private void skipFully(long bytesToSkip, DataSpec dataSpec) throws IOException {
        if (bytesToSkip == 0) return;

        byte[] skipBuffer = new byte[4096];
        while (bytesToSkip > 0) {
            int readLength = (int) Math.min(bytesToSkip, skipBuffer.length);
            int read = checkNotNull(inputStream).read(skipBuffer, 0, readLength);
            if (read == -1) {
                throw new HttpDataSourceException(
                        dataSpec,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
                        HttpDataSourceException.TYPE_OPEN);
            }
            bytesToSkip -= read;
            if (opened) {
                bytesTransferred(read);
            }
        }
    }

    /**
     * Reads data from the input stream
     */
    private int readInternal(byte[] buffer, int offset, int readLength) throws IOException {
        if (readLength == 0) return 0;

        if (bytesToRead != C.LENGTH_UNSET) {
            long bytesRemaining = bytesToRead - bytesRead;
            if (bytesRemaining == 0) {
                return C.RESULT_END_OF_INPUT;
            }
            readLength = (int) Math.min(readLength, bytesRemaining);
        }

        int read = checkNotNull(inputStream).read(buffer, offset, readLength);
        if (read == -1) {
            return C.RESULT_END_OF_INPUT;
        }

        bytesRead += read;
        if (opened) {
            bytesTransferred(read);
        }
        return read;
    }

    /**
     * Attempts to terminate the input stream on older Android versions
     */
    private static void maybeTerminateInputStream(
            @Nullable HttpURLConnection connection, long bytesRemaining) {
        if (connection == null || Util.SDK_INT < 19 || Util.SDK_INT > 20) {
            return;
        }

        try {
            InputStream inputStream = connection.getInputStream();
            if (bytesRemaining == C.LENGTH_UNSET) {
                if (inputStream.read() == -1) return;
            } else if (bytesRemaining <= MAX_BYTES_TO_DRAIN) {
                return;
            }

            String className = inputStream.getClass().getName();
            if ("com.android.okhttp.internal.http.HttpTransport$ChunkedInputStream".equals(className) ||
                    "com.android.okhttp.internal.http.HttpTransport$FixedLengthInputStream".equals(className)) {
                Class<?> superclass = inputStream.getClass().getSuperclass();
                Method unexpectedEndOfInput = checkNotNull(superclass)
                        .getDeclaredMethod("unexpectedEndOfInput");
                unexpectedEndOfInput.setAccessible(true);
                unexpectedEndOfInput.invoke(inputStream);
            }
        } catch (Exception ignored) {
            // Ignore exceptions during stream termination
        }
    }

    /**
     * Closes the connection quietly
     */
    private void closeConnectionQuietly() {
        if (connection != null) {
            try {
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error while disconnecting", e);
            }
            connection = null;
        }
    }

    /**
     * Checks if the response is gzip-compressed
     */
    private static boolean isCompressed(HttpURLConnection connection) {
        String contentEncoding = connection.getHeaderField("Content-Encoding");
        return "gzip".equalsIgnoreCase(contentEncoding);
    }

    /**
     * Opens a connection to the specified URL
     */
    @VisibleForTesting
    /* package */ HttpURLConnection openConnection(URL url) throws IOException {
        return (HttpURLConnection) url.openConnection();
    }

    /**
     * Helper class for filtering null keys from header maps
     */
    private static class NullFilteringHeadersMap extends ForwardingMap<String, List<String>> {
        private final Map<String, List<String>> headers;

        public NullFilteringHeadersMap(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        protected Map<String, List<String>> delegate() {
            return headers;
        }

        @Override
        public boolean containsKey(@Nullable Object key) {
            return key != null && super.containsKey(key);
        }

        @Nullable
        @Override
        public List<String> get(@Nullable Object key) {
            return key == null ? null : super.get(key);
        }

        @Override
        public Set<String> keySet() {
            return Sets.filter(super.keySet(), key -> key != null);
        }

        @Override
        public Set<Entry<String, List<String>>> entrySet() {
            return Sets.filter(super.entrySet(), entry -> entry.getKey() != null);
        }

        @Override
        public int size() {
            return super.size() - (super.containsKey(null) ? 1 : 0);
        }

        @Override
        public boolean isEmpty() {
            return super.isEmpty() || (super.size() == 1 && super.containsKey(null));
        }

        @Override
        public boolean containsValue(@Nullable Object value) {
            return super.standardContainsValue(value);
        }

        @Override
        public boolean equals(@Nullable Object object) {
            return object != null && super.standardEquals(object);
        }

        @Override
        public int hashCode() {
            return super.standardHashCode();
        }
    }

    // ============================================
    // Static Helper Methods (Moved from PlayerActivity)
    // ============================================

    /**
     * Parse JSON-formatted cookie string to extract name=value pairs
     * Example input: [{"name":"perf_dv6Tr4n","value":"1",...}]
     * Example output: perf_dv6Tr4n=1
     */
    public static String parseJsonCookie(String jsonCookie) {
        try {
            StringBuilder cookieString = new StringBuilder();

            // Simple JSON parsing to extract name and value
            String json = jsonCookie.trim();
            if (json.startsWith("[")) json = json.substring(1);
            if (json.endsWith("]")) json = json.substring(0, json.length() - 1);

            // Split by "},{"
            String[] cookieObjects = json.split("\\},\\{");

            for (String cookieObj : cookieObjects) {
                String name = extractJsonValue(cookieObj, "name");
                String value = extractJsonValue(cookieObj, "value");

                if (name != null && value != null) {
                    if (cookieString.length() > 0) {
                        cookieString.append("; ");
                    }
                    cookieString.append(name).append("=").append(value);
                }
            }

            String result = cookieString.toString();
            Log.i(TAG, "Parsed JSON cookie to: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing JSON cookie: " + e.getMessage());
            return jsonCookie; // Return as-is if parsing fails
        }
    }

    /**
     * Extract value from simple JSON string
     */
    private static String extractJsonValue(String json, String key) {
        try {
            String searchFor = "\"" + key + "\":\"";
            int startIndex = json.indexOf(searchFor);
            if (startIndex == -1) return null;

            startIndex += searchFor.length();
            int endIndex = json.indexOf("\"", startIndex);
            if (endIndex == -1) return null;

            return json.substring(startIndex, endIndex);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get default User-Agent string for streaming
     * Uses a recent Chrome version to avoid 403 errors from servers checking User-Agent
     */
    public static String getDefaultUserAgent() {
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
    }

    /**
     * Apply Cloudflare response headers for session continuity
     * @param responseHeaders Map of response headers from Cloudflare clearance
     * @param requestHeaders Map to apply headers to
     */
    public static void applyCloudflareHeaders(
            Map<String, String> responseHeaders,
            Map<String, String> requestHeaders) {

        if (responseHeaders == null) return;

        // Pass through cf-ray for session continuity
        if (responseHeaders.containsKey("cf-ray")) {
            requestHeaders.put("X-CF-RAY", responseHeaders.get("cf-ray"));
            Log.d(TAG, "Added CF-RAY header for session continuity");
        }

        // Pass through cache status
        if (responseHeaders.containsKey("cf-cache-status")) {
            requestHeaders.put("X-CF-Cache-Status", responseHeaders.get("cf-cache-status"));
            Log.d(TAG, "Added CF-Cache-Status header");
        }

        // Pass through server info
        if (responseHeaders.containsKey("server")) {
            requestHeaders.put("X-Source-Server", responseHeaders.get("server"));
            Log.d(TAG, "Added X-Source-Server header");
        }
    }

    /**
     * Get video URI with fallback source selection
     */
    public static Uri getVideoUriWithFallback(MediaItems mediaItem, int sourceIndex) {

        // If we have a specific source index set, use that source
        if (sourceIndex > 0 && mediaItem.getVideoSources() != null &&
                sourceIndex < mediaItem.getVideoSources().size()) {
            String url = mediaItem.getVideoSources().get(sourceIndex).getUrl();
            Log.i(TAG, "Using fallback source " + sourceIndex + ": " + url);
            return Uri.parse(url);
        }

        // Otherwise use the best URL as usual
        String bestUrl = mediaItem.getBestVideoUrl();
        return Uri.parse(bestUrl);
    }

    /**
     * Get all available sources for a media item
     */
    public static List<String> getAvailableSources(MediaItems mediaItem) {
        List<String> sources = new ArrayList<>();
        if (mediaItem.getVideoSources() != null) {
            for (MediaItems.VideoSource source : mediaItem.getVideoSources()) {
                sources.add(source.getUrl());
            }
        }
        return sources;
    }

    /**
     * Build headers for protected stream with session cookie
     * @param sessionCookie The Cloudflare session cookie
     * @param customHeaders Additional custom headers from MediaItems
     * @param refererUrl The referer URL if available
     * @param responseHeaders Cloudflare response headers for continuity
     * @return Complete headers map for the request
     */
    public static Map<String, String> buildProtectedStreamHeaders(
            String sessionCookie,
            Map<String, String> customHeaders,
            String refererUrl,
            Map<String, String> responseHeaders) {

        Map<String, String> headers = new HashMap<>();
        String host = extractHost(refererUrl);

        // FIRST: Apply domain-specific configuration
        applyDomainConfigurationForHeaders(host, headers);

        // SECOND: Add User-Agent
        headers.put("User-Agent", getDefaultUserAgent());

        // THIRD: Add cookies from multiple sources (priority order)
        StringBuilder cookieBuilder = new StringBuilder();

        // 1. Add session cookie if provided
        if (sessionCookie != null && !sessionCookie.isEmpty()) {
            String cookie = sessionCookie;
            // Parse JSON format if needed
            if (cookie.startsWith("[{") && cookie.contains("\"name\"")) {
                cookie = parseJsonCookie(cookie);
            }
            cookieBuilder.append(cookie);
            if (cookieBuilder.length() > 0) {
                cookieBuilder.append("; ");
            }
        }

        // 2. Add cookies from shared cookie store
        List<Cookie> storedCookies = getCookiesForDomain(host);
        if (storedCookies != null && !storedCookies.isEmpty()) {
            for (Cookie cookie : storedCookies) {
                cookieBuilder.append(cookie.name()).append("=").append(cookie.value()).append("; ");
            }
            Log.i(TAG, "Added " + storedCookies.size() + " cookies from shared store for: " + host);
        }

        String cookieHeader = cookieBuilder.toString().trim();
        if (!cookieHeader.isEmpty()) {
            if (cookieHeader.endsWith(";")) {
                cookieHeader = cookieHeader.substring(0, cookieHeader.length() - 1);
            }
            headers.put("Cookie", cookieHeader);
            Log.i(TAG, "Final cookie header (length: " + cookieHeader.length() + ")");
        }

        // CRITICAL: Always add keep-alive headers to prevent socket closure
        headers.put("Connection", "keep-alive");
        headers.put("Keep-Alive", "timeout=300, max=1000");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Accept", "*/*");
        headers.put("Accept-Language", "en-US,en;q=0.9");

        // Add custom headers from MediaItems
        if (customHeaders != null) {
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                headers.put(entry.getKey(), entry.getValue());
            }
            Log.i(TAG, "Added/overriden with " + customHeaders.size() + " custom headers");
        }

        // Add referer if available
        if (refererUrl != null && !refererUrl.isEmpty()) {
            headers.put("Referer", refererUrl);

            // CRITICAL: Add Origin header to match Referer for CORS compliance
            try {
                java.net.URL refererParsed = new java.net.URL(refererUrl);
                String origin = refererParsed.getProtocol() + "://" + refererParsed.getHost();
                headers.put("Origin", origin);
                Log.i(TAG, "Set Origin from Referer: " + origin);
            } catch (Exception e) {
                Log.w(TAG, "Failed to extract Origin from Referer: " + refererUrl);
            }
        }

        // Apply Cloudflare response headers for session continuity
        if (responseHeaders != null) {
            applyCloudflareHeaders(responseHeaders, headers);
        }

        // Log all headers being applied (mask sensitive values)
        Log.i(TAG, "Built " + headers.size() + " headers for protected stream:");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase("Cookie")) {
                Log.i(TAG, "  " + entry.getKey() + ": [MASKED - length: " + entry.getValue().length() + "]");
            } else {
                Log.i(TAG, "  " + entry.getKey() + ": " + entry.getValue());
            }
        }

        return headers;
    }

    /**
     * Apply domain configuration for headers map
     */
    private static void applyDomainConfigurationForHeaders(String host, Map<String, String> headers) {
        if (host == null || host.isEmpty()) return;

        for (Map.Entry<String, DomainConfig> entry : STREAMING_DOMAINS.entrySet()) {
            if (host.contains(entry.getKey())) {
                DomainConfig config = entry.getValue();
                if (config.origin != null) {
                    headers.put("Origin", config.origin);
                }
                if (config.referer != null) {
                    headers.put("Referer", config.referer);
                }
                Log.d(TAG, "Applied domain config for: " + host);
                return;
            }
        }
    }

    /**
     * Extract host from URL string
     */
    private static String extractHost(String url) {
        if (url == null || url.isEmpty()) return null;
        try {
            java.net.URL parsed = new java.net.URL(url);
            return parsed.getHost();
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================
    // Media Source Creation Methods
    // ============================================

    /**
     * Create standard media source WITHOUT custom headers
     * @param context Android context
     * @param mediaItem The media item to play
     * @return Configured media source
     */
    @UnstableApi
    public static MediaSource createStandardMediaSource(Context context, MediaItem mediaItem) {
        Log.i(TAG, "Creating standard media source");

        KiduyuDataSource.Factory dataSourceFactory = new KiduyuDataSource.Factory()
                .setUserAgent(getDefaultUserAgent())
                .setEnableKeepAlive(true);

        return createMediaSource(context, mediaItem, dataSourceFactory);
    }

    /**
     * Create media source WITH session headers for Cloudflare bypass
     * Now uses shared OkHttpClient from ApiClient for cookie sharing between API calls and playback
     * @param context Android context
     * @param mediaItem The media item to play
     * @param sessionCookie The Cloudflare session cookie
     * @param customHeaders Additional custom headers from MediaItems
     * @param refererUrl The referer URL if available
     * @param responseHeaders Cloudflare response headers for continuity
     * @return Configured media source with all headers applied
     */
    @UnstableApi
    public static MediaSource createProtectedMediaSource(
            Context context,
            MediaItem mediaItem,
            String sessionCookie,
            Map<String, String> customHeaders,
            String refererUrl,
            Map<String, String> responseHeaders) {

        Log.i(TAG, "Creating protected media source for: " + mediaItem.localConfiguration.uri);
        Log.i(TAG, "Session cookie: " + (sessionCookie != null ? "[length: " + sessionCookie.length() + "]" : "null"));
        Log.i(TAG, "Custom headers: " + (customHeaders != null ? customHeaders.size() + " headers" : "null"));
        Log.i(TAG, "Referer URL: " + refererUrl);

        // Build headers with session cookie and Cloudflare continuity
        Map<String, String> headers = buildProtectedStreamHeaders(
                sessionCookie, customHeaders, refererUrl, responseHeaders);

        // Get shared OkHttpClient from ApiClient for cookie sharing
        okhttp3.OkHttpClient sharedClient = ApiClient.getClient(context);

        // Initialize KiduyuDataSource with shared client for cookie sharing
        initializeWithSharedClient(sharedClient);

        // Create data source factory with headers and use OkHttpDataSource with shared client
        KiduyuDataSource.Factory dataSourceFactory = new KiduyuDataSource.Factory()
                .setDefaultRequestProperties(headers)
                .setEnableKeepAlive(true)
                .setUserAgent(getDefaultUserAgent());

        return createMediaSourceWithSharedClient(context, mediaItem, dataSourceFactory, sharedClient);
    }

    /**
     * Create appropriate media source using shared OkHttpClient
     * @param context Android context
     * @param mediaItem The media item to play
     * @param dataSourceFactory Configured data source factory
     * @param sharedClient Shared OkHttpClient for OkHttpDataSource
     * @return MediaSource configured for the media type
     */
    private static MediaSource createMediaSourceWithSharedClient(
            Context context,
            MediaItem mediaItem,
            KiduyuDataSource.Factory dataSourceFactory,
            okhttp3.OkHttpClient sharedClient) {

        // Use OkHttpDataSource.Factory with shared client for proper cookie sharing
        okhttp3.OkHttpClient okHttpClient = ApiClient.getClient(context);
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(okHttpClient);

        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(
                context, dataSourceFactory);
        // Override with OkHttpDataSource for proper HTTP/2 and cookie handling
        upstreamFactory = new DefaultDataSource.Factory(context, okHttpDataSourceFactory);

        String uriString = mediaItem.localConfiguration.uri.toString().toLowerCase();

        if (uriString.contains(".m3u8") || uriString.contains("m3u8")) {
            Log.i(TAG, "Using HLS media source with shared OkHttpClient");
            return new HlsMediaSource.Factory(upstreamFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem);
        } else if (uriString.contains(".mpd") || uriString.contains("mpd")) {
            Log.i(TAG, "Using DASH media source with shared OkHttpClient");
            return new DashMediaSource.Factory(upstreamFactory)
                    .createMediaSource(mediaItem);
        } else {
            Log.i(TAG, "Using Progressive media source with shared OkHttpClient");
            return new ProgressiveMediaSource.Factory(upstreamFactory)
                    .createMediaSource(mediaItem);
        }
    }

    /**
     * Create appropriate media source based on URI
     * @param context Android context
     * @param mediaItem The media item to play
     * @param dataSourceFactory Configured data source factory
     * @return MediaSource configured for the media type
     */
    private static MediaSource createMediaSource(
            Context context,
            MediaItem mediaItem,
            KiduyuDataSource.Factory dataSourceFactory) {

        DefaultDataSource.Factory upstreamFactory = new DefaultDataSource.Factory(
                context, dataSourceFactory);

        String uriString = mediaItem.localConfiguration.uri.toString().toLowerCase();

        if (uriString.contains(".m3u8") || uriString.contains("m3u8")) {
            Log.i(TAG, "Using HLS media source");
            return new HlsMediaSource.Factory(upstreamFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem);
        } else if (uriString.contains(".mpd") || uriString.contains("mpd")) {
            Log.i(TAG, "Using DASH media source");
            return new DashMediaSource.Factory(upstreamFactory)
                    .createMediaSource(mediaItem);
        } else {
            Log.i(TAG, "Using Progressive media source");
            return new ProgressiveMediaSource.Factory(upstreamFactory)
                    .createMediaSource(mediaItem);
        }
    }
}
