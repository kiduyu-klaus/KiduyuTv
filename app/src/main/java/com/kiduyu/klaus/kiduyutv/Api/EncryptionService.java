package com.kiduyu.klaus.kiduyutv.Api;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Helper class for encryption/decryption operations
 * Communicates with the enc-dec.app API
 */
public class EncryptionService {
    private static final String TAG = "EncryptionService";
    private static final String API_BASE = "https://enc-dec.app/api";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;

    public EncryptionService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    /**
     * Encrypt text using the kai encryption
     * @param text Text to encrypt
     * @return Encrypted text
     * @throws IOException if request fails
     */
    public String encrypt(String text) throws IOException, JSONException {
        String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8.toString());
        String url = API_BASE + "/enc-kai?text=" + encodedText;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Encryption failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getString("result");
        }
    }

    /**
     * Decrypt text using the kai decryption
     * @param text Encrypted text
     * @return Decrypted text
     * @throws IOException if request fails
     */
    public String decrypt(String text) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("text", text);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(API_BASE + "/dec-kai")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Decryption failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getString("result");
        }
    }

    /**
     * Parse HTML string using the API
     * @param html HTML string to parse
     * @return Parsed JSON string
     * @throws IOException if request fails
     */
    public String parseHtml(String html) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("text", html);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(API_BASE + "/parse-html")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Parse HTML failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getString("result");
        }
    }

    /**
     * Decrypt MEGA embed URL
     * @param encryptedText Encrypted media data
     * @param userAgent User agent string
     * @return Decrypted media data JSON string
     * @throws IOException if request fails
     */
    public String decryptMega(String encryptedText, String userAgent) throws IOException, JSONException {
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("text", encryptedText);
        jsonBody.put("agent", userAgent);

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(API_BASE + "/dec-mega")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Decrypt MEGA failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getString("result");
        }
    }

    /**
     * Shutdown the HTTP client
     */
    public void shutdown() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}