package com.kiduyu.klaus.kiduyutv.Ui.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
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
    private List<String> subtitleOptions = Arrays.asList("Off", "English", "Spanish", "French", "German", "Original");
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
    SurfaceViewImageLoader loader ;


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
        Log.i(TAG, "Loading media:\n" + sourceMediaItem.getVideoSources().toString());

        Log.i(TAG, "Quality options:\n" + qualityOptions.toString());
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
        btnSubtitles.setText("🗨 " + subtitleOptions.get(currentSubtitle));
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
        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(new DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                15000,  // Min buffer
                                50000,  // Max buffer
                                2500,   // Buffer for playback
                                5000    // Buffer for playback after rebuffer
                        ).build())
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
                hideLoading();
                hideStreamingStatus();

                // Show user-friendly error message
                String errorMessage = "Playback error";
                if (error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED) {
                    errorMessage = "Video decoder failed. Try a different quality.";
                } else if (error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED) {
                    errorMessage = "Network error. Check your connection.";
                }

                showToast(errorMessage);
                finish();

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
            }
        });

        loadVideo();
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

        // Apply saved speed
        //exoPlayer.setPlaybackSpeed(currentSpeed);
    }

    /**
     * Create media source with session headers if available
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createMediaSourceFromMediaItem(MediaItem mediaItem) {
        Log.d("PlayerActivity", "Creating media source");
        // Check if we need to apply custom headers
        boolean needsCustomHeaders = sourceMediaItem != null &&
                sourceMediaItem.getCustomHeaders() != null &&
                !sourceMediaItem.getCustomHeaders().isEmpty();

        if (needsCustomHeaders) {
            Log.d("PlayerActivity", "Applying session headers for protected stream");
            return createProtectedMediaSource(mediaItem);
        } else {
            // Standard media source without custom headers
            return createStandardMediaSource(mediaItem);
        }
    }

    /**
     * Create media source WITH session headers for Cloudflare bypass
     */
    @OptIn(markerClass = UnstableApi.class)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createProtectedMediaSource(MediaItem mediaItem) {
        // Build custom headers
        Map<String, String> headers = new HashMap<>();

        // Add session cookie first (highest priority)
        if (sourceMediaItem.getSessionCookie() != null && !sourceMediaItem.getSessionCookie().isEmpty()) {
            headers.put("Cookie", sourceMediaItem.getSessionCookie());
            Log.d("PlayerActivity", "Added session cookie to headers");
        }

        // Add custom headers from MediaItems
        if (sourceMediaItem.getCustomHeaders() != null) {
            headers.putAll(sourceMediaItem.getCustomHeaders());
            Log.d("PlayerActivity", "Added " + sourceMediaItem.getCustomHeaders().size() + " custom headers");
        }

        // Add referer if available (override from custom headers if needed)
        if (sourceMediaItem.getRefererUrl() != null && !sourceMediaItem.getRefererUrl().isEmpty()) {
            headers.put("Referer", sourceMediaItem.getRefererUrl());
            Log.d("PlayerActivity", "Added referer: " + sourceMediaItem.getRefererUrl());
        }

        // CRITICAL: Include Cloudflare response headers in request headers
        // This helps maintain the session when playing protected streams
        if (sourceMediaItem.getResponseHeaders() != null) {
            // Add Cloudflare-specific headers that might be needed for session continuity
            if (sourceMediaItem.getResponseHeaders().containsKey("cf-ray")) {
                String cfRay = sourceMediaItem.getResponseHeaders().get("cf-ray");
                headers.put("X-CF-RAY", cfRay);
                Log.d("PlayerActivity", "Added CF-RAY header for session continuity");
            }

            if (sourceMediaItem.getResponseHeaders().containsKey("cf-cache-status")) {
                String cfCacheStatus = sourceMediaItem.getResponseHeaders().get("cf-cache-status");
                headers.put("X-CF-Cache-Status", cfCacheStatus);
                Log.d("PlayerActivity", "Added CF-Cache-Status header");
            }

            if (sourceMediaItem.getResponseHeaders().containsKey("server")) {
                String server = sourceMediaItem.getResponseHeaders().get("server");
                headers.put("X-Source-Server", server);
            }

            // Log all response headers being used
            Log.d("PlayerActivity", "Response headers available: " + sourceMediaItem.getResponseHeaders().size());
            for (Map.Entry<String, String> entry : sourceMediaItem.getResponseHeaders().entrySet()) {
                Log.d("PlayerActivity", "  Response header: " + entry.getKey() + " = " + entry.getValue());
            }
        }

        // Log all headers being applied
        Log.d("PlayerActivity", "Applying " + headers.size() + " headers for protected stream:");
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            String value = entry.getValue();
            // Mask sensitive header values for logging
            if (entry.getKey().equalsIgnoreCase("Cookie") || entry.getKey().equalsIgnoreCase("Authorization")) {
                Log.d("PlayerActivity", "  " + entry.getKey() + ": [MASKED - length: " + value.length() + "]");
            } else {
                Log.d("PlayerActivity", "  " + entry.getKey() + ": " + value);
            }
        }
        headers.put("Connection", "keep-alive");
        headers.put("Accept-Encoding", "gzip, deflate");
        // Create custom HTTP data source with headers
        androidx.media3.datasource.DefaultHttpDataSource.Factory httpDataSourceFactory =
                new androidx.media3.datasource.DefaultHttpDataSource.Factory()
                        .setDefaultRequestProperties(headers)
                        .setConnectTimeoutMs(30000)
                        .setReadTimeoutMs(30000)
                        .setKeepPostFor302Redirects(true)
                        .setAllowCrossProtocolRedirects(true)
                        .setUserAgent(headers.getOrDefault("User-Agent",
                                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

        DefaultDataSource.Factory dataSourceFactory =
                new DefaultDataSource.Factory(this, httpDataSourceFactory);

        String uriString = mediaItem.localConfiguration.uri.toString().toLowerCase();

        Log.d("PlayerActivity", "Creating media source for URI: " + uriString);

        if (uriString.contains(".m3u8") || uriString.contains("m3u8")) {
            Log.d("PlayerActivity", "Using HLS media source with custom headers");
            return new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else if (uriString.contains(".mpd") || uriString.contains("mpd")) {
            Log.d("PlayerActivity", "Using DASH media source with custom headers");
            return new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            Log.d("PlayerActivity", "Using Progressive media source with custom headers");
            return new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }
    }

    /**
     * Create standard media source WITHOUT custom headers
     */
    @OptIn(markerClass = UnstableApi.class)
    private MediaSource createStandardMediaSource(MediaItem mediaItem) {
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitles");
        builder.setSingleChoiceItems(subtitleOptions.toArray(new String[0]),
                currentSubtitle, (dialog, which) -> {
                    currentSubtitle = which;
                    updateButtonTexts();
                    showToast("Subtitles: " + subtitleOptions.get(which));
                    dialog.dismiss();

                    // Reload video with/without subtitles
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

        // Load background image to surface
        //loader.loadImageToSurfaceView(videoSurface, sourceMediaItem.getBackgroundImageUrl());

        // Delay ExoPlayer setup to let background image load
        handler.postDelayed(() -> {
            if (isSurfaceReady) {
                setupExoPlayer();
            }
        }, 1000);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface changed: " + width + "x" + height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "Surface destroyed");
        isSurfaceReady = false;
    }
}