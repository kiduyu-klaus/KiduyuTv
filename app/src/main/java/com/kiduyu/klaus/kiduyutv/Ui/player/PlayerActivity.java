package com.kiduyu.klaus.kiduyutv.Ui.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.analytics.AnalyticsListener;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.LoadEventInfo;
import androidx.media3.exoplayer.source.MediaLoadData;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.SurfaceViewImageLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PlayerActivity extends Activity implements SurfaceHolder.Callback {
    private static final String TAG = "PlayerActivity";

    private SurfaceView videoSurface;
    private ExoPlayer exoPlayer;

    private LinearLayout topInfoContainer, bottomControlsContainer, loadingStatusContainer;
    private ImageButton btnPlayPause, btnBack;
    private Button btnSettings, btnSpeed, btnServer, btnQuality, btnAudio, btnSubtitles;
    private SeekBar progressBar;
    private TextView videoTitle, currentTime, totalTime, episodeInfo, btnEpisodes, loadingStatusText,videoDescription;
    private ProgressBar loadingIndicator;
    private ImageView centerPauseIcon;

    private Handler handler = new Handler();
    private Handler hideControlsHandler = new Handler();
    private boolean isPlaying = false;
    private boolean areControlsVisible = false;

    // Data lists
    private List<MediaItems.SubtitleItem> subtitleOptions ;
    private List<MediaItems.VideoSource> qualityOptions;
    private List<String> serverOptions = Arrays.asList("Server 1", "Server 2", "Server 3", "Server 4");
    private List<Float> speedOptions = Arrays.asList(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f);
    private List<String> audioOptions = Arrays.asList("Sub", "Dub");

    //SurfaceViewImageLoader loader = new SurfaceViewImageLoader();

    private int currentSubtitle = 0; // Original
    private int currentQuality = 0; // Medium 720p
    private int currentServer = 0;

    private MediaItem currentMediaItem;
    private boolean isSurfaceReady = false;

    // Source fallback mechanism
    private int currentSourceIndex = 0;
    SurfaceViewImageLoader loader;

    // Retry mechanism for playback errors
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    // Network monitoring
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkAvailable = true;


    private MediaItems sourceMediaItem;
    private int currentSpeed = 1; // 1.0x
    private int currentAudio = 1; // Dub
    ImageView backgroundImage;
    private static final String VIDEO_URL = "https://rrr.app28base.site/p5qm/c5/h6a90f70b8d237f94866b6cfc2c7f06afdb8423c6639e9e32b074383937a06baeef0f9b8cb9be75c7c50ae72297d2e440790079152e890fea1284d59d53a146e35d/4/aGxzLzEwODAvMTA4MA,Ktm0Vt9-cJyXbGG_O3gV_5vGK-kpiQ.m3u8";
    private static final String VIDEO_URL_SUBTITLES = "https://5qm.megaup.cc/v5/bapD3C40jf5SGa2z8LH8Gr9uEI8Zjnp4ysHQ4OTega67vD5uMub51x8UK5yKV2uhCPlVjTWzBDeJW0JvJTuYJoQVP5d7qAZgZXxD67pvQIuNikLe6H8gyXmcoNmdRsqYZzwNrIbH7nA/subs/eng_4.vtt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Get media item from intent
        sourceMediaItem = getIntent().getParcelableExtra("media_item");
        if (sourceMediaItem == null) {
            finish();
            return;
        }

        //loader = new SurfaceViewImageLoader();
        // Detailed debugging for headers
        Log.i(TAG, "Headers:\n" + sourceMediaItem.getSubtitles().toString());
        Log.i(TAG, "Loading media:\n" + sourceMediaItem.getVideoSources().toString());

        for(MediaItems.VideoSource videoSource : sourceMediaItem.getVideoSources()){
            Log.i(TAG, "Quality: " + videoSource.getQuality());
            Log.i(TAG, "URL: " + videoSource.getUrl());



        }

        //Log.i(TAG, "Quality options:\n" + qualityOptions.toString());
        Log.i(TAG, "background image url:\n" + sourceMediaItem.getBackgroundImageUrl());


        initializeViews();
        //loader.loadImageToSurfaceView(videoSurface, sourceMediaItem.getBackgroundImageUrl());



        setupFocusListeners();
        setupClickListeners();
        // Delay ExoPlayer setup slightly to ensure background image loads first
        setupExoPlayer();

        // Start with controls hidden
        //hideControls();
        showControls();

        // Register network callback for monitoring connectivity
        //registerNetworkCallback();


    }


    private void initializeViews() {
        videoSurface = findViewById(R.id.videoSurface);
        topInfoContainer = findViewById(R.id.topInfoContainer);
        bottomControlsContainer = findViewById(R.id.bottomControlsContainer);
        loadingStatusContainer = findViewById(R.id.loadingStatusContainer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        backgroundImage = findViewById(R.id.backgroundImage);
        videoDescription = findViewById(R.id.videoDescription);
        btnBack = findViewById(R.id.btnBack);
        btnSettings = findViewById(R.id.btnSettings);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnServer = findViewById(R.id.btnServer);
        btnQuality = findViewById(R.id.btnQuality);
        btnAudio = findViewById(R.id.btnAudio);
        btnSubtitles = findViewById(R.id.btnSubtitles);
        progressBar = findViewById(R.id.progressBar);
        videoTitle = findViewById(R.id.videoTitle);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        episodeInfo = findViewById(R.id.episodeInfo);
        btnEpisodes = findViewById(R.id.btnEpisodes);
        loadingStatusText = findViewById(R.id.loadingStatusText);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        centerPauseIcon = findViewById(R.id.centerPauseIcon);

        videoDescription.setText(sourceMediaItem.getDescription());
        videoTitle.setText(sourceMediaItem.getTitle());

        // Set initial button texts
        updateButtonTexts();
    }

    private void updateButtonTexts() {
        btnSpeed.setText("Speed " + String.format(Locale.getDefault(), "%.2fx", speedOptions.get(currentSpeed)));
        btnServer.setText(serverOptions.get(currentServer));

        // Safe quality button text update
        if (sourceMediaItem != null && sourceMediaItem.getVideoSources() != null &&
                !sourceMediaItem.getVideoSources().isEmpty() &&
                currentQuality < sourceMediaItem.getVideoSources().size()) {
            btnQuality.setText(sourceMediaItem.getVideoSources().get(currentQuality).getQuality());
        } else {
            btnQuality.setText("Quality");
        }

        btnAudio.setText(audioOptions.get(currentAudio));

        // Dynamic subtitle button text
        String subtitleText;
        if (currentSubtitle == 0) {
            subtitleText = "🗨 Off";
        } else if (sourceMediaItem != null && sourceMediaItem.getSubtitles() != null &&
                !sourceMediaItem.getSubtitles().isEmpty()) {
            int subtitleIndex = currentSubtitle - 1;
            if (subtitleIndex < sourceMediaItem.getSubtitles().size()) {
                MediaItems.SubtitleItem subtitle = sourceMediaItem.getSubtitles().get(subtitleIndex);
                String displayName = subtitle.getLanguage() != null && !subtitle.getLanguage().isEmpty()
                        ? subtitle.getLanguage()
                        : subtitle.getLang();
                subtitleText = "🗨 " + displayName;
            } else {
                subtitleText = "🗨 Subtitles";
            }
        } else {
            subtitleText = "🗨 Subtitles";
        }
        btnSubtitles.setText(subtitleText);
    }

    private void setupFocusListeners() {
        // Dark blue background on focus for all buttons
        View.OnFocusChangeListener focusListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundColor(Color.parseColor("#FF1565C0"));
                resetHideControlsTimer();
            } else {
                // Reset to default drawable
                if (v instanceof Button) {
                    v.setBackgroundResource(R.drawable.tv_control_selector);
                }
            }
        };

        btnSettings.setOnFocusChangeListener(focusListener);
        btnSpeed.setOnFocusChangeListener(focusListener);
        btnServer.setOnFocusChangeListener(focusListener);
        btnQuality.setOnFocusChangeListener(focusListener);
        btnAudio.setOnFocusChangeListener(focusListener);
        btnSubtitles.setOnFocusChangeListener(focusListener);

        btnPlayPause.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setAlpha(1.0f);
                resetHideControlsTimer();
            } else {
                v.setAlpha(0.7f);
            }
        });

        btnBack.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                v.setBackgroundResource(R.drawable.back_button_bg);
                resetHideControlsTimer();
            }
        });

        progressBar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                resetHideControlsTimer();
            }
        });
    }

    private void setupClickListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        btnBack.setOnClickListener(v -> finish());

        btnSettings.setOnClickListener(v -> showToast("Settings clicked"));

        btnSpeed.setOnClickListener(v -> showSpeedDialog());
        btnServer.setOnClickListener(v -> showServerDialog());
        btnQuality.setOnClickListener(v -> showQualityDialog());
        btnAudio.setOnClickListener(v -> showAudioDialog());
        btnSubtitles.setOnClickListener(v -> showSubtitlesDialog());

        btnEpisodes.setOnClickListener(v -> showToast("Episodes list"));

        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && exoPlayer != null) {
                    exoPlayer.seekTo(progress);
                    updateTimeDisplay();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateProgressTask);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.post(updateProgressTask);
            }
        });
    }

    @OptIn(markerClass = UnstableApi.class)
    private void setupExoPlayer() {
        // Clear the background image from SurfaceView before ExoPlayer takes over
        //loader.clearSurfaceView(videoSurface);

        // Enhanced LoadControl for better buffering - prevents socket closure due to buffering issues

        videoSurface.setVisibility(View.VISIBLE);
        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        30000,   // Min buffer (30s) - increased from 15s for smoother playback
                        120000,  // Max buffer (2min) - increased from 50s for longer buffer
                        10000,    // Buffer for playback (5s) - increased from 2.5s
                        10000    // Buffer after rebuffer (10s) - increased from 5s
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        exoPlayer.setVideoSurfaceHolder(videoSurface.getHolder());

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:
                        showLoadingServer();
                        break;
                    case Player.STATE_READY:
                        //hideLoading();
                        showStreamingStatus();
                        progressBar.setMax((int) exoPlayer.getDuration());
                        totalTime.setText(formatTime((int) exoPlayer.getDuration()));

                        // Auto-start playback after streaming is ready
                        handler.postDelayed(() -> {
                            hideStreamingStatus();
                            if (!isPlaying) {
                                togglePlayPause();
                            }
                        }, 1500);
                        break;
                    case Player.STATE_ENDED:
                        isPlaying = false;
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        showControls();
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean playing) {
                if (playing) {
                    handler.post(updateProgressTask);
                }
            }
            @Override
            public void onPlayerError(@NonNull PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage(), error);
                handlePlaybackError(error);
            }

        });
        exoPlayer.addAnalyticsListener(new AnalyticsListener() {
            @Override
            public void onLoadError(EventTime eventTime, LoadEventInfo loadEventInfo,
                                    MediaLoadData mediaLoadData, IOException error,
                                    boolean wasCanceled) {
                Log.e(TAG, "Load error: " + error.getMessage() +
                        " | URL: " + loadEventInfo.uri +
                        " | Canceled: " + wasCanceled);

                // Auto-retry on network errors
                if (!wasCanceled && isNetworkError(error)) {
                    runOnUiThread(() -> {
                        if (retryCount < MAX_RETRIES) {
                            retryCount++;
                            Log.i(TAG, "Network load error, retry attempt " + retryCount + "/" + MAX_RETRIES);
                            showToast("Connection issue, retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                            handler.postDelayed(() -> retryPlayback(), 2000);
                        }
                    });
                }
            }
        });

        loadVideo();
    }

    // ============================================
    // Error Handling and Retry Logic
    // ============================================

    private void handlePlaybackError(PlaybackException error) {
        hideLoading();
        hideStreamingStatus();

        String errorMessage = "Playback error";
        boolean canRetry = false;

        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
            case PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE:
                errorMessage = "Network connection failed";
                canRetry = true;
                break;

            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
                errorMessage = "Video decoder failed. Try different quality.";
                break;

            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED:
                errorMessage = "Stream error occurred";
                canRetry = true;
                break;

            default:
                errorMessage = "Playback error: " + error.getErrorCodeName();
                canRetry = error.errorCode >= 1000 && error.errorCode < 2000; // Network-related errors
                break;
        }

        if (canRetry && retryCount < MAX_RETRIES) {
            retryCount++;
            Log.i(TAG, "Retrying playback, attempt " + retryCount + "/" + MAX_RETRIES);
            showToast("Connection issue, retrying... (" + retryCount + "/" + MAX_RETRIES + ")");

            handler.postDelayed(() -> retryPlayback(), 2000);
        } else {
            showToast(errorMessage);
            retryCount = 0;
            finish();
        }
    }

    private void retryPlayback() {
        if (exoPlayer != null) {
            // Save position
            long position = exoPlayer.getCurrentPosition();

            // Reset player
            exoPlayer.stop();
            exoPlayer.clearMediaItems();

            // Reload
            loadVideo();

            // Restore position
            if (position > 0) {
                exoPlayer.seekTo(position);
            }
        }
    }

    private boolean isNetworkError(IOException error) {
        return error instanceof java.net.SocketException ||
                error instanceof java.net.SocketTimeoutException ||
                error instanceof java.net.UnknownHostException ||
                (error.getMessage() != null && (
                        error.getMessage().contains("Connection") ||
                                error.getMessage().contains("Socket") ||
                                error.getMessage().contains("timeout") ||
                                error.getMessage().contains("EAI")
                ));
    }

    @OptIn(markerClass = UnstableApi.class)
    private void loadVideo() {
        showLoadingServer();
        if (sourceMediaItem == null) {
            Toast.makeText(this, "Invalid media item", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get video URI and background/artwork URL
        Uri videoUri = getMediaUri(sourceMediaItem);
        //Uri videoUri = Uri.parse(VIDEO_URL);
        String backgroundUrl = sourceMediaItem.getPreferredBackgroundUrl();

        Log.i("PlayerActivity", "Preparing playback - Video URI: " + videoUri);
        Log.i("PlayerActivity", "Background/Artwork URL: " +
                (backgroundUrl != null ? backgroundUrl : "Not available"));

        // Build MediaMetadata with title and artwork
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(sourceMediaItem.getTitle())
                .setDisplayTitle(sourceMediaItem.getTitle());

        // Add artwork if background URL is available
        if (backgroundUrl != null && !backgroundUrl.isEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(backgroundUrl));
            Log.i("PlayerActivity", "Setting artwork for player: " + backgroundUrl);
        }

        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(videoUri)
                .setMediaMetadata(metadataBuilder.build());

        if (sourceMediaItem.getId()!=null) {
            mediaItemBuilder.setMediaId(sourceMediaItem.getId());
        }

        // Add subtitles if available
        List<MediaItem.SubtitleConfiguration> subtitleList = new ArrayList<>();

        if (sourceMediaItem.getSubtitles() != null && !sourceMediaItem.getSubtitles().isEmpty()) {
            for (MediaItems.SubtitleItem subtitleItem : sourceMediaItem.getSubtitles()) {
                MediaItem.SubtitleConfiguration subtitle = new MediaItem.SubtitleConfiguration.Builder(
                        Uri.parse(subtitleItem.getUrl()))
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(subtitleItem.getLang())
                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                        .build();
                subtitleList.add(subtitle);
            }
        } else if (sourceMediaItem.getSubtitleUrl() != null && !sourceMediaItem.getSubtitleUrl().isEmpty()) {
            MediaItem.SubtitleConfiguration subtitle = new MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(sourceMediaItem.getSubtitleUrl()))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();
            subtitleList.add(subtitle);
        }

        if (!subtitleList.isEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleList);
        }

        currentMediaItem = mediaItemBuilder.build();

        // Determine media source based on MediaItem
        MediaSource mediaSource = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mediaSource = createMediaSourceFromMediaItem(currentMediaItem);
        }
        if (mediaSource == null) {
            Toast.makeText(this, "Unsupported media format", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set media source and prepare player
        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.play();
        handler.post(updateProgressTask);

        // Apply saved speed
        //exoPlayer.setPlaybackSpeed(currentSpeed);
    }

    /**
     * Create media source with session headers if available
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createMediaSourceFromMediaItem(MediaItem mediaItem) {
        Log.i("PlayerActivity", "Creating media source");
        // Check if we need to apply custom headers
//        boolean needsCustomHeaders = sourceMediaItem != null &&
//                sourceMediaItem.getCustomHeaders() != null &&
//                !sourceMediaItem.getCustomHeaders().isEmpty();
        boolean needsCustomHeaders = false;

        if (needsCustomHeaders) {
            Log.i("PlayerActivity", "Applying session headers for protected stream");
            return createProtectedMediaSource(mediaItem);
        } else {
            // Standard media source without custom headers
            return createStandardMediaSource(mediaItem);
        }
    }

    /**
     * Create media source WITH session headers for Cloudflare bypass
     * Enhanced with Keep-Alive headers to prevent socket closure
     */


    @OptIn(markerClass = UnstableApi.class)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createProtectedMediaSource(MediaItem mediaItem) {
        // Build custom headers with keep-alive
        Map<String, String> headers = new HashMap<>();

        // CRITICAL: Always add these headers to prevent socket closure
        headers.put("Connection", "keep-alive");
        headers.put("Keep-Alive", "timeout=300, max=1000");
        headers.put("Accept-Encoding", "gzip, deflate");

        // *** DEBUG: Log what we have ***
        Log.i(TAG, "DEBUG: sourceMediaItem.getSessionCookie() = " +
                (sourceMediaItem.getSessionCookie() != null ?
                        sourceMediaItem.getSessionCookie().substring(0, Math.min(100, sourceMediaItem.getSessionCookie().length())) + "..." :
                        "NULL"));

        // *** FIX: Add session cookie FIRST (highest priority) ***
        if (sourceMediaItem.getSessionCookie() != null && !sourceMediaItem.getSessionCookie().isEmpty()) {
            // Check if it's JSON format (default cookies) - parse it
            String cookie = sourceMediaItem.getSessionCookie();

            // If it looks like JSON, parse it to extract cookie value
            if (cookie.startsWith("[{") && cookie.contains("\"name\"")) {
                cookie = parseJsonCookie(cookie);
            }

            headers.put("Cookie", cookie);
            Log.i(TAG, "✓ Added session cookie to headers (length: " + cookie.length() + ")");
        } else {
            Log.w(TAG, "⚠ No session cookie available!");
        }

        // Add custom headers from MediaItems (may override some values)
        if (sourceMediaItem.getCustomHeaders() != null) {
            headers.putAll(sourceMediaItem.getCustomHeaders());
            Log.i(TAG, "Added " + sourceMediaItem.getCustomHeaders().size() + " custom headers");
        }

        // Add referer if available (override from custom headers if needed)
        if (sourceMediaItem.getRefererUrl() != null && !sourceMediaItem.getRefererUrl().isEmpty()) {
            headers.put("Referer", sourceMediaItem.getRefererUrl());
            Log.i(TAG, "Added referer: " + sourceMediaItem.getRefererUrl());
        }

        // CRITICAL: Include Cloudflare response headers in request headers
        if (sourceMediaItem.getResponseHeaders() != null) {
            if (sourceMediaItem.getResponseHeaders().containsKey("cf-ray")) {
                String cfRay = sourceMediaItem.getResponseHeaders().get("cf-ray");
                headers.put("X-CF-RAY", cfRay);
                Log.i(TAG, "Added CF-RAY header for session continuity");
            }

            if (sourceMediaItem.getResponseHeaders().containsKey("cf-cache-status")) {
                String cfCacheStatus = sourceMediaItem.getResponseHeaders().get("cf-cache-status");
                headers.put("X-CF-Cache-Status", cfCacheStatus);
                Log.i(TAG, "Added CF-Cache-Status header");
            }

            if (sourceMediaItem.getResponseHeaders().containsKey("server")) {
                String server = sourceMediaItem.getResponseHeaders().get("server");
                headers.put("X-Source-Server", server);
            }

            Log.i(TAG, "Response headers available: " + sourceMediaItem.getResponseHeaders().size());
        }

        // Log all headers being applied
        Log.i(TAG, "Applying " + headers.size() + " headers for protected stream:");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = entry.getValue();
            // Mask sensitive header values for logging
            if (entry.getKey().equalsIgnoreCase("Cookie") || entry.getKey().equalsIgnoreCase("Authorization")) {
                Log.i(TAG, "  " + entry.getKey() + ": [MASKED - length: " + value.length() + "]");
            } else {
                Log.i(TAG, "  " + entry.getKey() + ": " + value);
            }
        }

        // Add range header for video streaming
        headers.put("Range", "bytes=0-");

        // Create OkHttpDataSource.Factory using the shared OkHttpClient from FetchStreams
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(
                FetchStreams.getSharedClient()
        ).setDefaultRequestProperties(headers);

        // Wrap with DefaultDataSource.Factory for proper media source handling
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this, okHttpDataSourceFactory);

        String uriString = mediaItem.localConfiguration.uri.toString().toLowerCase();

        Log.i(TAG, "Creating protected media source for URI: " + uriString);

        if (uriString.contains(".m3u8") || uriString.contains("m3u8")) {
            Log.i(TAG, "Using HLS media source with shared OkHttpClient and keep-alive");
            return new HlsMediaSource.Factory(dataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(mediaItem);
        } else if (uriString.contains(".mpd") || uriString.contains("mpd")) {
            Log.i(TAG, "Using DASH media source with shared OkHttpClient and keep-alive");
            return new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            Log.i(TAG, "Using Progressive media source with shared OkHttpClient and keep-alive");
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }
    }

    /**
     * Parse JSON-formatted cookie string to extract name=value pairs
     * Example input: [{"name":"perf_dv6Tr4n","value":"1",...}]
     * Example output: perf_dv6Tr4n=1
     */
    private String parseJsonCookie(String jsonCookie) {
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

    /**
     * Create standard media source WITHOUT custom headers
     */
    @OptIn(markerClass = UnstableApi.class)
    private MediaSource createStandardMediaSource(MediaItem mediaItem) {
        // Use shared OkHttpClient from FetchStreams for connection pooling
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(
                FetchStreams.getSharedClient());

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(
                this, okHttpDataSourceFactory);

        String uriString = mediaItem.localConfiguration.uri.toString().toLowerCase();

        if (uriString.contains(".m3u8") || uriString.contains("m3u8")) {
            return new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else if (uriString.contains(".mpd") || uriString.contains("mpd")) {
            return new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }
    }

    private Uri getMediaUri(MediaItems mediaItem) {
        // If we have a specific source index set, use that source
        if (currentSourceIndex > 0 && mediaItem.getVideoSources() != null &&
                currentSourceIndex < mediaItem.getVideoSources().size()) {
            String url = mediaItem.getVideoSources().get(currentSourceIndex).getUrl();
            Log.i("PlayerActivity", "Using fallback source " + currentSourceIndex + ": " + url);
            return Uri.parse(url);
        }

        // Otherwise use the best URL as usual
        String bestUrl = mediaItem.getBestVideoUrl();
        return Uri.parse(bestUrl);
    }
    private void showLoadingServer() {
        loadingStatusContainer.setVisibility(View.VISIBLE);
        loadingStatusText.setText("LOADING SERVER");
        //btnPlayPause.setVisibility(View.GONE);
    }

    private void showStreamingStatus() {
        loadingStatusContainer.setVisibility(View.VISIBLE);
        loadingStatusText.setText("STREAMING VIDEO");
        //btnPlayPause.setVisibility(View.GONE);
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
    }

    private void hideStreamingStatus() {
        loadingStatusContainer.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
    }

    private void togglePlayPause() {
        if (exoPlayer == null) return;

        if (isPlaying) {
            exoPlayer.pause();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            centerPauseIcon.setVisibility(View.VISIBLE);
            centerPauseIcon.setImageResource(android.R.drawable.ic_media_pause);
            isPlaying = false;
            showControls();
        } else {
            exoPlayer.play();
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            centerPauseIcon.setVisibility(View.GONE);
            isPlaying = true;
            resetHideControlsTimer();
        }
    }

    private void showControls() {
        topInfoContainer.setVisibility(View.VISIBLE);
        bottomControlsContainer.setVisibility(View.VISIBLE);
        //btnEpisodes.setVisibility(View.VISIBLE);
        btnBack.setVisibility(View.VISIBLE);
        areControlsVisible = true;

        // Set focus to Settings button by default
        btnSettings.requestFocus();

        resetHideControlsTimer();
    }

    private void hideControls() {
        topInfoContainer.setVisibility(View.GONE);
        bottomControlsContainer.setVisibility(View.GONE);
        btnEpisodes.setVisibility(View.GONE);
        btnBack.setVisibility(View.GONE);
        areControlsVisible = false;
    }

    private void resetHideControlsTimer() {
        hideControlsHandler.removeCallbacks(hideControlsTask);
        if (isPlaying) {
            hideControlsHandler.postDelayed(hideControlsTask, 3000);
        }
    }

    private Runnable hideControlsTask = () -> {
        if (isPlaying && areControlsVisible) {
            hideControls();
        }
    };

    private Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (exoPlayer != null && isPlaying) {
                updateTimeDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimeDisplay() {
        if (exoPlayer != null) {
            int currentPos = (int) exoPlayer.getCurrentPosition();
            progressBar.setProgress(currentPos);
            currentTime.setText(formatTime(currentPos));
        }
    }

    private String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        int hours = (millis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    private void showSubtitlesDialog() {
        // Build subtitle options list
        List<String> subtitleOptionsList = new ArrayList<>();
        subtitleOptionsList.add("Off"); // Always add "Off" as first option

        // Add subtitles from sourceMediaItem if available
        if (sourceMediaItem != null && sourceMediaItem.getSubtitles() != null &&
                !sourceMediaItem.getSubtitles().isEmpty()) {
            for (MediaItems.SubtitleItem subtitle : sourceMediaItem.getSubtitles()) {
                // Use language name if available, otherwise use lang code
                String displayName = subtitle.getLanguage() != null && !subtitle.getLanguage().isEmpty()
                        ? subtitle.getLanguage()
                        : subtitle.getLang();
                subtitleOptionsList.add(displayName);
            }
        } else if (sourceMediaItem != null && sourceMediaItem.getSubtitleUrl() != null &&
                !sourceMediaItem.getSubtitleUrl().isEmpty()) {
            // Fallback: if single subtitle URL exists
            subtitleOptionsList.add("English"); // Default to English for single subtitle
        }

        // Convert to array for dialog
        String[] subtitleLabels = subtitleOptionsList.toArray(new String[0]);

        // Ensure currentSubtitle index is valid
        if (currentSubtitle >= subtitleLabels.length) {
            currentSubtitle = 0;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitles");
        builder.setSingleChoiceItems(subtitleLabels, currentSubtitle, (dialog, which) -> {
            currentSubtitle = which;
            updateButtonTexts();

            if (which == 0) {
                showToast("Subtitles: Off");
            } else {
                showToast("Subtitles: " + subtitleLabels[which]);
            }

            dialog.dismiss();

            // Reload video with selected subtitles
            loadVideo();
        });
        builder.show();
    }

    private void showQualityDialog() {
        // Get quality options from sourceMediaItem
        if (sourceMediaItem == null || sourceMediaItem.getVideoSources() == null ||
                sourceMediaItem.getVideoSources().isEmpty()) {
            showToast("No quality options available");
            return;
        }

        // Build quality labels array from video sources
        List<MediaItems.VideoSource> sources = sourceMediaItem.getVideoSources();
        String[] qualityLabels = new String[sources.size()];

        for (int i = 0; i < sources.size(); i++) {
            qualityLabels[i] = sources.get(i).getQuality();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quality");
        builder.setSingleChoiceItems(qualityLabels, currentQuality, (dialog, which) -> {
            // Save current playback position
            long currentPosition = exoPlayer != null ? exoPlayer.getCurrentPosition() : 0;
            boolean wasPlaying = isPlaying;

            currentQuality = which;
            updateButtonTexts();
            showToast("Quality: " + sources.get(which).getQuality());
            dialog.dismiss();

            // Show loading and reload video
            showLoadingServer();
            loadVideo();

            // Restore playback position after video loads
            if (exoPlayer != null && currentPosition > 0) {
                exoPlayer.seekTo(currentPosition);
                if (wasPlaying) {
                    exoPlayer.play();
                }
            }
        });
        builder.show();
    }

    private void showServerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Server");
        builder.setSingleChoiceItems(serverOptions.toArray(new String[0]),
                currentServer, (dialog, which) -> {
                    currentServer = which;
                    updateButtonTexts();
                    showToast("Switched to " + serverOptions.get(which));
                    dialog.dismiss();

                    // Show loading and reload video
                    loadVideo();
                });
        builder.show();
    }

    private void showSpeedDialog() {
        String[] speedLabels = new String[speedOptions.size()];
        for (int i = 0; i < speedOptions.size(); i++) {
            speedLabels[i] = speedOptions.get(i) + "x";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Playback Speed");
        builder.setSingleChoiceItems(speedLabels, currentSpeed, (dialog, which) -> {
            currentSpeed = which;
            float speed = speedOptions.get(which);
            updateButtonTexts();

            if (exoPlayer != null) {
                exoPlayer.setPlaybackParameters(new PlaybackParameters(speed));
            }
            showToast("Speed: " + speed + "x");
            dialog.dismiss();
        });
        builder.show();
    }

    private void showAudioDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Audio");
        builder.setSingleChoiceItems(audioOptions.toArray(new String[0]),
                currentAudio, (dialog, which) -> {
                    currentAudio = which;
                    updateButtonTexts();
                    showToast("Audio: " + audioOptions.get(which));
                    dialog.dismiss();
                });
        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Show controls on Up button or Center button when hidden
        if ((keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
                && !areControlsVisible) {
            showControls();
            return true;
        }

        // Back button
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (areControlsVisible) {
                hideControls();
                return true;
            }
        }

        // Play/pause on center when controls visible and no button focused
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && areControlsVisible) {
            View focusedView = getCurrentFocus();
            if (focusedView == null) {
                togglePlayPause();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null && isPlaying) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        handler.removeCallbacks(updateProgressTask);
        hideControlsHandler.removeCallbacks(hideControlsTask);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.i(TAG, "Surface created");
        isSurfaceReady = true;

                setupExoPlayer();

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i(TAG, "Surface destroyed");
        isSurfaceReady = false;
    }
}