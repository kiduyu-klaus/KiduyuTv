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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.TrackSelector;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;
import com.kiduyu.klaus.kiduyutv.utils.SurfaceViewImageLoader;
import com.kiduyu.klaus.kiduyutv.utils.KiduyuDataSource;
import com.kiduyu.klaus.kiduyutv.utils.PlayerUtils;

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
    private TextView videoTitle, currentTime, totalTime, episodeInfo, btnEpisodes, loadingStatusText,videoDescription,hardsubBadge;
    private ProgressBar loadingIndicator;
    private ImageView centerPauseIcon;

    private Handler handler = new Handler();
    private Handler hideControlsHandler = new Handler();
    private Handler backPressHandler = new Handler();
    private boolean isPlaying = false;
    private boolean areControlsVisible = false;
    private boolean backPressedOnce = false;
    private boolean hasAutoLoadedSubtitles = false;


    // Data lists
    private List<MediaItems.SubtitleItem> subtitleOptions ;
    private List<MediaItems.VideoSource> qualityOptions;
    private List<String> serverOptions = Arrays.asList("Server 1", "Server 2", "Server 3", "Server 4");
    private List<Float> speedOptions = Arrays.asList(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f);
    private List<String> audioOptions = Arrays.asList("Sub", "Dub");

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
    private static final int MAX_RETRIES = 8;

    // Network monitoring
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isNetworkAvailable = true;

    private static final int MIN_BUFFER_MS = 30000;     // 30 seconds
    private static final int PLAYBACK_BUFFER_MS = 10000; // 10 seconds
    private static final int REBUFFER_MS = 10000;
    private MediaItems sourceMediaItem;
    private int currentSpeed = 2; // 1.0x
    private int currentAudio = 1; // Dub
    private long startPosition = 0;
    private Handler watchHistoryHandler = new Handler();
    ImageView backgroundImage;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // Get media item from intent
        sourceMediaItem = getIntent().getParcelableExtra("media_item");
        startPosition = getIntent().getLongExtra("start_position", 0);

        if (sourceMediaItem == null) {
            finish();
            return;
        }

        // Detailed debugging for headers
        Log.i(TAG, "Headers:\n" + sourceMediaItem.getSubtitles().toString());
        Log.i(TAG, "Loading media:\n" + sourceMediaItem.getVideoSources().toString());

        for(MediaItems.VideoSource videoSource : sourceMediaItem.getVideoSources()){
            Log.i(TAG, "Quality: " + videoSource.getQuality());
            Log.i(TAG, "URL: " + videoSource.getUrl());
        }

        if (sourceMediaItem.getSubtitles() != null && !sourceMediaItem.getSubtitles().isEmpty()) {
            for (MediaItems.SubtitleItem subtitleItem : sourceMediaItem.getSubtitles()) {
                Log.i(TAG, "Subtitle: " + subtitleItem.getLang());
                Log.i(TAG, "Subtitle URL: " + subtitleItem.getUrl());
            }
        }

        Log.i(TAG, "background image url:\n" + sourceMediaItem.getBackgroundImageUrl());

        initializeViews();

        preferencesManager = PreferencesManager.getInstance(this);
        setupFocusListeners();
        setupClickListeners();
        setupExoPlayer();

        // Start with controls visible
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
        hardsubBadge = findViewById(R.id.hardsubBadge);

        hardsubBadge.setText(sourceMediaItem.getMediaType());
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
        videoSurface.setVisibility(View.VISIBLE);

        // Get buffer duration from preferences (in minutes)
        int bufferMinutes = PreferencesManager.getInstance(this).getPlaybackBufferDuration();
        int maxBufferMs = bufferMinutes * 60 * 1000; // Convert minutes to milliseconds

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        MIN_BUFFER_MS,   // Min buffer (30s) - required before playback starts
                        maxBufferMs,     // Max buffer (10min) - maximum ahead of current position
                        PLAYBACK_BUFFER_MS,   // Buffer for playback (10s) - after seek
                        REBUFFER_MS      // Buffer after rebuffer (10s)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();

        // Create DefaultTrackSelector for subtitle control
        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
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
                        showStreamingStatus();
                        // Immediately show seekbar, total time, and reset current time
                        long duration = exoPlayer.getDuration();
                        progressBar.setMax((int) duration);
                        totalTime.setText(formatTime((int) duration));
                        currentTime.setText(formatTime(0)); // Reset to 00:00

                        // Make sure buffer is updated immediately
                        updateBufferProgress();

                        // Hide streaming status immediately and show controls
                        hideStreamingStatus();

                        // Select subtitle track after media is ready
                        selectSubtitleTrack();

                        // Show controls so user can see seekbar and time
                        showControls();

                        // Auto-start playback after a short delay for visual feedback
                        handler.postDelayed(() -> {
                            if (!isPlaying) {
                                togglePlayPause();
                            }
                        }, 500);
                        handler.post(updateProgressTask);
                        break;
                    case Player.STATE_ENDED:
                        updateLoadingUi(false);
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

    /**
     * Find the index of English subtitles
     * @return Index for currentSubtitle (0 = Off, 1+ = subtitle index)
     */
    private int findEnglishSubtitleIndex() {
        if (sourceMediaItem == null || sourceMediaItem.getSubtitles() == null ||
                sourceMediaItem.getSubtitles().isEmpty()) {
            return 0; // No subtitles available
        }

        // Check each subtitle for English
        for (int i = 0; i < sourceMediaItem.getSubtitles().size(); i++) {
            MediaItems.SubtitleItem subtitle = sourceMediaItem.getSubtitles().get(i);
            String lang = subtitle.getLang().toLowerCase();
            String language = subtitle.getLanguage() != null ?
                    subtitle.getLanguage().toLowerCase() : "";

            // Check for English in various formats
            if (lang.equals("en") ||
                    lang.equals("eng") ||
                    lang.equals("english") ||
                    language.contains("english")) {

                Log.i(TAG, "Found English subtitle at index: " + i);
                return i + 1; // +1 because index 0 is "Off"
            }
        }

        // Check fallback subtitle URL if it exists
        if (sourceMediaItem.getSubtitleUrl() != null &&
                !sourceMediaItem.getSubtitleUrl().isEmpty()) {
            Log.i(TAG, "Using fallback subtitle as English");
            return 1; // First subtitle after "Off"
        }

        Log.i(TAG, "No English subtitle found");
        return 0; // Default to Off
    }

    /**
     * Select subtitle track in ExoPlayer
     */
    @OptIn(markerClass = UnstableApi.class)
    private void selectSubtitleTrack() {
        if (exoPlayer == null) {
            Log.w(TAG, "Cannot select subtitle track: exoPlayer is null");
            return;
        }

        // Check if we have subtitles available
        boolean hasSubtitles = sourceMediaItem != null &&
                sourceMediaItem.getSubtitles() != null &&
                !sourceMediaItem.getSubtitles().isEmpty();

        Log.i(TAG, "selectSubtitleTrack called: currentSubtitle=" + currentSubtitle +
                ", hasSubtitles=" + hasSubtitles);

        TrackSelector trackSelector = exoPlayer.getTrackSelector();
        if (!(trackSelector instanceof DefaultTrackSelector)) {
            Log.w(TAG, "TrackSelector is not DefaultTrackSelector");
            return;
        }

        DefaultTrackSelector defaultTrackSelector = (DefaultTrackSelector) trackSelector;

        if (currentSubtitle == 0 || !hasSubtitles) {
            // Disable all subtitle tracks
            defaultTrackSelector.setParameters(
                    defaultTrackSelector.buildUponParameters()
                            .setRendererDisabled(C.TRACK_TYPE_TEXT, true)
            );
            Log.i(TAG, "Subtitles disabled (currentSubtitle=" + currentSubtitle + ", hasSubtitles=" + hasSubtitles + ")");
        } else {
            // Enable subtitle renderer with preferred language
            String language = getSelectedSubtitleLanguage();
            Log.i(TAG, "Enabling subtitles for language: " + language +
                    ", subtitleIndex=" + currentSubtitle);

            defaultTrackSelector.setParameters(
                    defaultTrackSelector.buildUponParameters()
                            .setRendererDisabled(C.TRACK_TYPE_TEXT, false)
                            .setPreferredTextLanguage(language)
                            .setSelectUndeterminedTextLanguage(true)  // Allow fallback matching
            );

            // Log available tracks for debugging
            Log.i(TAG, "Subtitle track selection complete for language: " + language);
        }
    }

    /**
     * Get the language code of the currently selected subtitle
     */
    private String getSelectedSubtitleLanguage() {
        if (sourceMediaItem == null || sourceMediaItem.getSubtitles() == null) {
            return "en";
        }

        int subtitleIndex = currentSubtitle - 1; // Adjust for "Off" at index 0
        if (subtitleIndex >= 0 && subtitleIndex < sourceMediaItem.getSubtitles().size()) {
            return sourceMediaItem.getSubtitles().get(subtitleIndex).getLang();
        }
        return "en";
    }

    private void updateBufferProgress() {
        if (exoPlayer != null) {
            long bufferedPosition = exoPlayer.getBufferedPosition();
            long duration = exoPlayer.getDuration();
            long currentPosition = exoPlayer.getCurrentPosition();

            if (duration > 0) {
                long bufferDuration = bufferedPosition - currentPosition;

                // Use actual millisecond values since max is set to duration in ms
                progressBar.setSecondaryProgress((int) bufferedPosition);

                Log.i(TAG, "Current: " + currentPosition + "ms | Buffered: " + bufferedPosition +
                        "ms | Max: " + duration + "ms | Buffer Duration: " + formatTime((int) bufferDuration));
            }
        }
    }

    // ============================================
    // Error Handling and Retry Logic
    // ============================================

    private void handlePlaybackError(PlaybackException error) {
        hideLoading();
        hideStreamingStatus();

        PlayerUtils.PlaybackErrorInfo errorInfo = PlayerUtils.parsePlaybackError(error);

        // Check if we have more video sources to try
        boolean hasMoreSources = sourceMediaItem != null &&
                sourceMediaItem.getVideoSources() != null &&
                currentSourceIndex < sourceMediaItem.getVideoSources().size() - 1;

        if (errorInfo.canRetry) {
            if (retryCount < MAX_RETRIES) {
                // Retry with current source
                retryCount++;
                Log.i(TAG, "Retrying playback with current source, attempt " + retryCount + "/" + MAX_RETRIES);
                showToast("Connection issue, retrying... (" + retryCount + "/" + MAX_RETRIES + ")");
                handler.postDelayed(() -> retryPlayback(), 2000);
            } else if (hasMoreSources) {
                // Max retries reached for current source, try next source
                retryCount = 0; // Reset retry count for new source
                currentSourceIndex++;

                String currentQuality = sourceMediaItem.getVideoSources().get(currentSourceIndex).getQuality();
                Log.i(TAG, "Switching to next video source [" + (currentSourceIndex + 1) + "/" +
                        sourceMediaItem.getVideoSources().size() + "]: " + currentQuality);

                showToast("Switching to " + currentQuality + " quality...");
                handler.postDelayed(() -> retryPlayback(), 1000);
            } else {
                // No more sources to try
                showToast("All video sources failed: " + errorInfo.errorMessage);
                retryCount = 0;
                currentSourceIndex = 0; // Reset for next time
            }
        } else {
            // Non-retryable error
            if (hasMoreSources) {
                // Try next source even for non-retryable errors
                currentSourceIndex++;
                retryCount = 0;

                String currentQuality = sourceMediaItem.getVideoSources().get(currentSourceIndex).getQuality();
                Log.i(TAG, "Non-retryable error, switching to next source [" + (currentSourceIndex + 1) + "/" +
                        sourceMediaItem.getVideoSources().size() + "]: " + currentQuality);

                showToast("Error occurred, trying " + currentQuality + " quality...");
                handler.postDelayed(() -> retryPlayback(), 1000);
            } else {
                // No more sources
                showToast(errorInfo.errorMessage);
                retryCount = 0;
                currentSourceIndex = 0;
            }
        }
    }

    private void retryPlayback() {
        if (exoPlayer != null && sourceMediaItem != null) {
            // Save position
            long position = exoPlayer.getCurrentPosition();

            // Log current source info
            if (sourceMediaItem.getVideoSources() != null &&
                    currentSourceIndex < sourceMediaItem.getVideoSources().size()) {
                MediaItems.VideoSource currentSource = sourceMediaItem.getVideoSources().get(currentSourceIndex);
                Log.i(TAG, "Retrying with source [" + (currentSourceIndex + 1) + "/" +
                        sourceMediaItem.getVideoSources().size() + "]: Quality=" + currentSource.getQuality() +
                        ", URL=" + currentSource.getUrl().substring(0, Math.min(50, currentSource.getUrl().length())) + "...");
            }

            // Reset player
            exoPlayer.stop();
            exoPlayer.clearMediaItems();

            // Show loading
            showLoadingServer();

            // Reload with new source index (KiduyuDataSource will use currentSourceIndex)
            loadVideo();

            // Restore position
            if (position > 0) {
                exoPlayer.seekTo(position);
            }
        }
    }

    private boolean isNetworkError(IOException error) {
        return PlayerUtils.isNetworkError(error);
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
        String backgroundUrl = sourceMediaItem.getBackgroundImageUrl();

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

        if (sourceMediaItem.getId() != null) {
            mediaItemBuilder.setMediaId(sourceMediaItem.getId());
        }

        // Add subtitles if available
        List<MediaItem.SubtitleConfiguration> subtitleList = new ArrayList<>();

        if (sourceMediaItem.getSubtitles() != null && !sourceMediaItem.getSubtitles().isEmpty()) {
            for (int i = 0; i < sourceMediaItem.getSubtitles().size(); i++) {
                MediaItems.SubtitleItem subtitleItem = sourceMediaItem.getSubtitles().get(i);

                // Skip subtitles without valid URLs
                if (subtitleItem.getUrl() == null || subtitleItem.getUrl().isEmpty()) {
                    Log.w(TAG, "Skipping subtitle with empty URL at index " + i);
                    continue;
                }

                // Get language codes with null safety
                String lang = subtitleItem.getLang();
                String langLower = (lang != null) ? lang.toLowerCase() : "und";
                String language = subtitleItem.getLanguage();
                String languageLower = (language != null) ? language.toLowerCase() : "";

                // Determine if this is the English subtitle
                boolean isEnglish = langLower.equals("en") || langLower.equals("eng") ||
                        langLower.equals("english") || languageLower.contains("english");

                // Build subtitle configuration with proper language handling
                MediaItem.SubtitleConfiguration.Builder subtitleBuilder =
                        new MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleItem.getUrl()))
                                .setMimeType(MimeTypes.TEXT_VTT)
                                .setSelectionFlags(isEnglish ? C.SELECTION_FLAG_DEFAULT : C.SELECTION_FLAG_AUTOSELECT)
                                .setLabel(language != null && !language.isEmpty() ? language : lang);

                // Set language - use "und" (undefined) if null to avoid NPE
                subtitleBuilder.setLanguage(lang != null ? lang : "und");

                MediaItem.SubtitleConfiguration subtitle = subtitleBuilder.build();
                subtitleList.add(subtitle);

                Log.i(TAG, "Added subtitle[" + i + "]: lang=" + lang +
                        ", label=" + (language != null ? language : lang) +
                        ", isDefault=" + isEnglish +
                        ", url=" + subtitleItem.getUrl());
            }
            Log.i(TAG, "Total subtitles added: " + subtitleList.size());
        } else if (sourceMediaItem.getSubtitleUrl() != null &&
                !sourceMediaItem.getSubtitleUrl().isEmpty()) {
            MediaItem.SubtitleConfiguration subtitle = new MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(sourceMediaItem.getSubtitleUrl()))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .setLabel("English")
                    .build();
            subtitleList.add(subtitle);
            Log.i(TAG, "Added fallback subtitle as English [DEFAULT]");
        }

        if (!subtitleList.isEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleList);
        }

        // Auto-load English subtitles on first load only
        if (!hasAutoLoadedSubtitles) {
            currentSubtitle = findEnglishSubtitleIndex();
            hasAutoLoadedSubtitles = true;
            updateButtonTexts();

            if (currentSubtitle > 0) {
                Log.i(TAG, "Auto-loading English subtitles at index: " + currentSubtitle);
                showToast("English subtitles loaded");
            } else {
                Log.i(TAG, "No English subtitles found, defaulting to Off");
            }
        }

        currentMediaItem = mediaItemBuilder.build();

        // CRITICAL: Select subtitle track BEFORE preparing player
        // This ensures the subtitle renderer is enabled with correct language
        // before playback starts. Previously this was only called in
        // onPlaybackStateChanged which caused a race condition.
        selectSubtitleTrack();

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

        // Seek to saved position if resuming
        if (startPosition > 0) {
            Log.i(TAG, "Resuming from position: " + formatTime((int) startPosition));
            exoPlayer.seekTo(startPosition);
        }

        exoPlayer.setPlayWhenReady(true);

        // Initialize time display to saved position or 00:00
        currentTime.setText(formatTime((int) startPosition));
        progressBar.setProgress((int) startPosition);

        handler.post(updateProgressTask);

        // Start watch history save timer (saves every 15 seconds)
        startWatchHistoryTimer();
    }

    // ============================================
    // Watch History Methods
    // ============================================

    /**
     * Start the watch history save timer
     * Saves position every 15 seconds during playback
     */
    private void startWatchHistoryTimer() {
        // Save immediately when starting
        saveWatchHistory();

        // Schedule regular saves every 15 seconds
        watchHistoryHandler.postDelayed(watchHistoryTask, 15000);
    }

    /**
     * Runnable to save watch history periodically
     */
    private Runnable watchHistoryTask = new Runnable() {
        @Override
        public void run() {
            if (isPlaying && exoPlayer != null) {
                saveWatchHistory();
                // Schedule next save
                watchHistoryHandler.postDelayed(this, 15000);
            }
        }
    };

    /**
     * Save current playback position to watch history
     */
    private void saveWatchHistory() {
        if (exoPlayer == null || sourceMediaItem == null) {
            return;
        }

        long currentPosition = exoPlayer.getCurrentPosition();
        long duration = exoPlayer.getDuration();

        // Don't save if position is at or near the end (95% or more)
        if (duration > 0 && currentPosition >= (duration * 0.95)) {
            Log.i(TAG, "Video completed, not saving watch history");
            return;
        }

        preferencesManager.saveWatchHistory(sourceMediaItem, currentPosition, duration);
    }

    /**
     * Stop the watch history save timer
     */
    private void stopWatchHistoryTimer() {
        // Save one final time when stopping
        saveWatchHistory();
        watchHistoryHandler.removeCallbacks(watchHistoryTask);
    }

    /**
     * Create media source with session headers if available
     * Now always uses protected source for consistent streaming behavior
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createMediaSourceFromMediaItem(MediaItem mediaItem) {
        Log.i("PlayerActivity", "Creating media source with KiduyuDataSourceFactory");
        return createProtectedMediaSource(mediaItem);
    }

    /**
     * Create media source WITH session headers for Cloudflare bypass
     * ✅ NOW USES PER-SOURCE HEADERS from the current video source
     */
    @OptIn(markerClass = UnstableApi.class)
    @RequiresApi(api = Build.VERSION_CODES.N)
    private MediaSource createProtectedMediaSource(MediaItem mediaItem) {
        // ✅ Get the CURRENT video source based on currentSourceIndex
        if (sourceMediaItem.getVideoSources() == null ||
                sourceMediaItem.getVideoSources().isEmpty() ||
                currentSourceIndex >= sourceMediaItem.getVideoSources().size()) {

            Log.e(TAG, "No video sources available or invalid currentSourceIndex: " + currentSourceIndex);
            // Fallback to global headers (backward compatibility)
            return KiduyuDataSource.createProtectedMediaSource(
                    this,
                    mediaItem,
                    sourceMediaItem.getSessionCookie(),
                    sourceMediaItem.getCustomHeaders(),
                    sourceMediaItem.getRefererUrl(),
                    sourceMediaItem.getResponseHeaders()
            );
        }

        // ✅ Get headers from CURRENT source
        MediaItems.VideoSource currentSource = sourceMediaItem.getVideoSources().get(currentSourceIndex);

        Log.i(TAG, "✅ Creating media source for current source [" + (currentSourceIndex + 1) +
                "/" + sourceMediaItem.getVideoSources().size() + "]");
        Log.i(TAG, "   Quality: " + currentSource.getQuality());
        Log.i(TAG, "   URL: " + (currentSource.getUrl().length() > 60 ?
                currentSource.getUrl().substring(0, 60) + "..." : currentSource.getUrl()));
        Log.i(TAG, "   Using source-specific headers:");
        Log.i(TAG, "     - Cookie: " + (currentSource.getSessionCookie() != null &&
                !currentSource.getSessionCookie().isEmpty() ?
                "[SET, length=" + currentSource.getSessionCookie().length() + "]" : "[NONE]"));
        Log.i(TAG, "     - Referer: " + currentSource.getRefererUrl());
        Log.i(TAG, "     - Custom headers: " + (currentSource.getCustomHeaders() != null ?
                currentSource.getCustomHeaders().size() : 0));

        // ✅ Use source-specific authentication headers
        return KiduyuDataSource.createProtectedMediaSource(
                this,
                mediaItem,
                currentSource.getSessionCookie(),
                currentSource.getCustomHeaders(),
                currentSource.getRefererUrl(),
                currentSource.getResponseHeaders()
        );
    }

    /**
     * Create standard media source WITHOUT custom headers
     * Now delegates to KiduyuDataSourceFactory
     */
    @OptIn(markerClass = UnstableApi.class)
    private MediaSource createStandardMediaSource(MediaItem mediaItem) {
        return KiduyuDataSource.createStandardMediaSource(this, mediaItem);
    }

    @OptIn(markerClass = UnstableApi.class)
    private Uri getMediaUri(MediaItems mediaItem) {
        // Use KiduyuDataSourceFactory for source fallback logic
        return KiduyuDataSource.getVideoUriWithFallback(mediaItem, currentSourceIndex);
    }

    private void showLoadingServer() {
        updateLoadingUi(true);
        loadingStatusText.setText("BUFFERING");
    }

    private void showStreamingStatus() {
        updateLoadingUi(true);
        loadingStatusText.setText("STREAMING");
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
    }

    private void hideStreamingStatus() {
        loadingStatusContainer.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
    }

    private void togglePlayPause() {
        if (exoPlayer == null) return;

        if (isPlaying) {
            exoPlayer.pause();

            currentTime.setVisibility(View.VISIBLE);
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            centerPauseIcon.setVisibility(View.VISIBLE);
            centerPauseIcon.setImageResource(android.R.drawable.ic_media_pause);
            handler.post(updateProgressTask);
            isPlaying = false;
            showControls();
        } else {
            exoPlayer.play();

            currentTime.setVisibility(View.VISIBLE);
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            centerPauseIcon.setVisibility(View.GONE);
            isPlaying = true;
            // Immediately update time display when playback starts
            handler.post(updateProgressTask);
            resetHideControlsTimer();
        }
    }

    private void showControls() {
        topInfoContainer.setVisibility(View.VISIBLE);
        bottomControlsContainer.setVisibility(View.VISIBLE);
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

    private void updateLoadingUi(boolean isLoading) {
        if (isLoading) {
            // Hide play/pause button and current time when loading
            btnPlayPause.setVisibility(View.GONE);
            currentTime.setVisibility(View.GONE);
            // Show loading status in the same location
            loadingStatusContainer.setVisibility(View.VISIBLE);
        } else {
            // Hide loading/streaming status
            loadingStatusContainer.setVisibility(View.GONE);
            // Show play/pause button and current time immediately
            btnPlayPause.setVisibility(View.VISIBLE);
            currentTime.setVisibility(View.VISIBLE);
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
            long duration = exoPlayer.getDuration();

            // Update current time text
            currentTime.setText(formatTime(currentPos));
            if(currentTime.getVisibility() == View.GONE){
                currentTime.setVisibility(View.VISIBLE);
            }

            // Update progress in milliseconds (matching the max value)
            if (duration > 0) {
                progressBar.setProgress(currentPos);
            }

            // Update buffered progress display
            updateBufferProgress();
        }
    }

    private String formatTime(int millis) {
        return preferencesManager.formatTime(millis);
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

            // Don't reload video, just update track selection
            selectSubtitleTrack();
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

        // Seek forward 10 seconds on Right D-pad
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!areControlsVisible) {
                if (exoPlayer != null) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    long duration = exoPlayer.getDuration();
                    long newPosition = Math.min(currentPosition + 10000, duration); // +10 seconds (10000ms)

                    exoPlayer.seekTo(newPosition);
                    updateTimeDisplay();
                    showToast("Forward +10s");

                    // Show controls briefly if hidden
                    if (!areControlsVisible) {
                        showControls();
                    }
                    resetHideControlsTimer();
                }
                return true;
            }
        }

        // Seek backward 10 seconds on Left D-pad
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!areControlsVisible) {
                if (exoPlayer != null) {
                    long currentPosition = exoPlayer.getCurrentPosition();
                    long newPosition = Math.max(currentPosition - 10000, 0); // -10 seconds (10000ms)

                    exoPlayer.seekTo(newPosition);
                    updateTimeDisplay();
                    showToast("Backward -10s");

                    // Show controls briefly if hidden
                    if (!areControlsVisible) {
                        showControls();
                    }
                    resetHideControlsTimer();
                }
                return true;
            }
        }

        // Back button - double press to exit
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (areControlsVisible) {
                hideControls();
                return true;
            }

            // Double press back to exit
            if (backPressedOnce) {
                backPressedOnce = false;
                backPressHandler.removeCallbacks(backPressResetTask);
                finish();
                return true;
            }

            backPressedOnce = true;
            showToast("Press back again to exit");

            // Reset flag after 5 seconds
            backPressHandler.postDelayed(backPressResetTask, 5000);
            return true;
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

    // Runnable to reset back press flag after 5 seconds
    private Runnable backPressResetTask = () -> {
        backPressedOnce = false;
    };

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Save watch history before pausing
        saveWatchHistory();
        if (exoPlayer != null && isPlaying) {
            exoPlayer.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Save watch history before destroying
        saveWatchHistory();
        stopWatchHistoryTimer();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
        handler.removeCallbacks(updateProgressTask);
        hideControlsHandler.removeCallbacks(hideControlsTask);
        backPressHandler.removeCallbacks(backPressResetTask);
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