// ============================================
// CloudflareSessionHandler - Cloudflare Session Management Utility
// ============================================

package com.kiduyu.klaus.kiduyutv.utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudflareSessionHandler {
    private static final String TAG = "CFSessionHandler";

    /**
     * Extracts all Cloudflare-related information from response headers
     * and organizes them for use in subsequent requests
     */
    public static class CloudflareSession {

        // JSON string containing default cookies
        public String defaultCookiesJson = "[{\"domain\":\"www.videasy.net\",\"hostOnly\":true,\"httpOnly\":false,\"name\":\"perf_dv6Tr4n\",\"path\":\"/\",\"sameSite\":\"unspecified\",\"secure\":false,\"session\":true,\"storeId\":\"0\",\"value\":\"1\"}]";

        public String sessionCookie;           // Combined cookie string for requests
        public String cfRay;                   // CF-RAY identifier (for logging only)
        public String cfCacheStatus;           // Cache status
        public String server;                  // Server header
        public Map<String, String> allCookies; // Individual cookies
        public long capturedAt;                // Timestamp when captured

        public CloudflareSession() {
            allCookies = new HashMap<>();
            capturedAt = System.currentTimeMillis();
        }

        public boolean isValid() {
            // Session is valid if we have cookies and it's not too old (24 hours)
            long age = System.currentTimeMillis() - capturedAt;
            long maxAge = 24 * 60 * 60 * 1000; // 24 hours

            return sessionCookie != null && !sessionCookie.isEmpty() && age < maxAge;
        }

        /**
         * Get the session cookie, parsing defaultCookiesJson if sessionCookie is null or empty
         */
        public String getSessionCookie() {
            if (sessionCookie == null || sessionCookie.isEmpty()) {
                return parseDefaultCookies();
            }
            return sessionCookie;
        }

        /**
         * Parse the defaultCookiesJson and extract cookies in "name=value" format
         */
        private String parseDefaultCookies() {
            try {
                // Simple JSON parsing to extract name and value
                // Expected format: [{"name":"cookie_name","value":"cookie_value",...}]
                StringBuilder cookieString = new StringBuilder();

                // Remove brackets and split by objects
                String json = defaultCookiesJson.trim();
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

                return cookieString.toString();
            } catch (Exception e) {
                Log.e(TAG, "Error parsing default cookies: " + e.getMessage());
                return "";
            }
        }

        /**
         * Extract value from simple JSON string
         */
        private String extractJsonValue(String json, String key) {
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

        @Override
        public String toString() {
            return "CloudflareSession{" +
                    "\n  sessionCookie='" + maskSensitive(sessionCookie) + "'" +
                    ",\n  cfRay='" + cfRay + "'" +
                    ",\n  cfCacheStatus='" + cfCacheStatus + "'" +
                    ",\n  server='" + server + "'" +
                    ",\n  cookies=" + allCookies.size() +
                    ",\n  age=" + (System.currentTimeMillis() - capturedAt) / 1000 + "s" +
                    "\n}";
        }

        private String maskSensitive(String value) {
            if (value == null || value.length() < 10) return "[masked]";
            return value.substring(0, 10) + "..." + " (length: " + value.length() + ")";
        }
    }

    /**
     * Extract Cloudflare session from response headers
     * Handles both single and multiple Set-Cookie headers
     */
    public static CloudflareSession extractSession(Map<String, String> responseHeaders) {
        CloudflareSession session = new CloudflareSession();

        if (responseHeaders == null || responseHeaders.isEmpty()) {
            Log.w(TAG, "No response headers provided");
            return session;
        }

        Log.i(TAG, "Extracting CF session from " + responseHeaders.size() + " headers");

        // Extract CF-RAY (response only, don't send back)
        session.cfRay = extractHeader(responseHeaders, "cf-ray", "CF-RAY");
        if (session.cfRay != null) {
            Log.i(TAG, "  CF-RAY: " + session.cfRay);
        }

        // Extract CF-Cache-Status
        session.cfCacheStatus = extractHeader(responseHeaders, "cf-cache-status", "CF-Cache-Status");
        if (session.cfCacheStatus != null) {
            Log.i(TAG, "  CF-Cache-Status: " + session.cfCacheStatus);
        }

        // Extract Server header
        session.server = extractHeader(responseHeaders, "server", "Server");
        if (session.server != null && session.server.toLowerCase().contains("cloudflare")) {
            Log.i(TAG, "  Server: Cloudflare detected");
        }

        // Extract cookies from Set-Cookie header(s)
        extractCookies(responseHeaders, session);

        // Build final cookie string for requests
        session.sessionCookie = buildCookieString(session.allCookies);

        // Use default cookies if no session cookie was extracted
        if (session.sessionCookie == null || session.sessionCookie.isEmpty()) {
            Log.w(TAG, "⚠ No cookies found in response, will use default cookies");
            session.sessionCookie = session.parseDefaultCookies();
        } else {
            Log.i(TAG, "✓ Session extracted successfully with " +
                    session.allCookies.size() + " cookies");
        }

        return session;
    }

    /**
     * Extract header value (case-insensitive)
     */
    private static String extractHeader(Map<String, String> headers, String... names) {
        for (String name : names) {
            // Try exact match first
            if (headers.containsKey(name)) {
                return headers.get(name);
            }

            // Try case-insensitive match
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Extract all cookies from Set-Cookie headers
     * Handles multiple Set-Cookie headers and comma-separated cookies
     */
    private static void extractCookies(Map<String, String> headers, CloudflareSession session) {
        // Look for Set-Cookie header (case-insensitive)
        String setCookieHeader = extractHeader(headers, "set-cookie", "Set-Cookie");

        if (setCookieHeader == null || setCookieHeader.isEmpty()) {
            Log.i(TAG, "  No Set-Cookie header found");
            return;
        }

        Log.i(TAG, "  Processing Set-Cookie header");

        // Split by comma (but not comma inside quotes or dates)
        List<String> cookieStrings = splitCookieHeader(setCookieHeader);

        for (String cookieString : cookieStrings) {
            parseSingleCookie(cookieString.trim(), session);
        }
    }

    /**
     * Parse a single cookie from Set-Cookie value
     */
    private static void parseSingleCookie(String cookieString, CloudflareSession session) {
        if (cookieString.isEmpty()) return;

        // Split by semicolon to get cookie and attributes
        String[] parts = cookieString.split(";");
        if (parts.length == 0) return;

        // First part is the actual cookie (name=value)
        String[] nameValue = parts[0].trim().split("=", 2);
        if (nameValue.length != 2) return;

        String name = nameValue[0].trim();
        String value = nameValue[1].trim();

        // Store the cookie
        session.allCookies.put(name, value);

        // Log important Cloudflare cookies
        if (name.equals("cf_clearance")) {
            Log.i(TAG, "    ✓ cf_clearance cookie found (length: " + value.length() + ")");
        } else if (name.equals("__cf_bm")) {
            Log.i(TAG, "    ✓ __cf_bm cookie found (length: " + value.length() + ")");
        } else if (name.equals("__cfduid")) {
            Log.i(TAG, "    ✓ __cfduid cookie found (length: " + value.length() + ")");
        } else {
            Log.i(TAG, "    - " + name + " cookie found");
        }
    }

    /**
     * Split Set-Cookie header properly (handles multiple cookies)
     */
    private static List<String> splitCookieHeader(String header) {
        List<String> cookies = new ArrayList<>();

        // Simple split for now - more complex parsing may be needed
        // for edge cases with commas in cookie values
        String[] parts = header.split(",(?=[^;]+?=)");

        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                cookies.add(trimmed);
            }
        }

        return cookies;
    }

    /**
     * Build Cookie header string from individual cookies
     * Priority: cf_clearance > __cf_bm > others
     */
    private static String buildCookieString(Map<String, String> cookies) {
        if (cookies.isEmpty()) return null;

        StringBuilder cookieBuilder = new StringBuilder();

        // Add Cloudflare cookies first (priority order)
        addCookie(cookieBuilder, cookies, "cf_clearance");
        addCookie(cookieBuilder, cookies, "__cf_bm");
        addCookie(cookieBuilder, cookies, "__cfduid");

        // Add any other cookies
        for (Map.Entry<String, String> entry : cookies.entrySet()) {
            String name = entry.getKey();
            if (!name.equals("cf_clearance") &&
                    !name.equals("__cf_bm") &&
                    !name.equals("__cfduid")) {
                addCookie(cookieBuilder, cookies, name);
            }
        }

        // Remove trailing "; "
        if (cookieBuilder.length() > 2) {
            cookieBuilder.setLength(cookieBuilder.length() - 2);
        }

        return cookieBuilder.toString();
    }

    /**
     * Add a single cookie to the cookie string builder
     */
    private static void addCookie(StringBuilder builder, Map<String, String> cookies, String name) {
        if (cookies.containsKey(name)) {
            builder.append(name).append("=").append(cookies.get(name)).append("; ");
        }
    }
}