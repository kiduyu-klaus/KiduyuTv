package com.kiduyu.klaus.kiduyutv.Ui.player;

import static com.kiduyu.klaus.kiduyutv.Api.ApiClient.DEFAULT_USER_AGENT;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import android.content.pm.ActivityInfo;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.bumptech.glide.Glide;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.SubtitleView;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.EpisodeModel;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;
import com.kiduyu.klaus.kiduyutv.utils.SubdlService;
import com.kiduyu.klaus.kiduyutv.utils.Subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;

@UnstableApi
@RequiresApi(api = Build.VERSION_CODES.O)
public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "PlayerActivity";
    private static final int CONTROLS_HIDE_DELAY = 5000; // 5 seconds
    private static final int SEEK_INCREMENT = 10000; // 10 seconds in milliseconds

    // ExoPlayer
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;

    // UI Components
    private SurfaceView videoSurface;
    private SubtitleView subtitleView;
    private ImageView backgroundImage;
    private ProgressBar loadingIndicator;
    private ImageView centerPauseIcon;
    private LinearLayout topInfoContainer;
    private LinearLayout bottomControlsContainer;
    private ImageButton btnBack;
    private ImageButton btnPlayPause;
    private SeekBar progressBar;
    private TextView currentTime;
    private TextView totalTime;
    private TextView videoTitle;
    private TextView videoDescription;
    private TextView episodeInfo;
    private Button btnSettings;
    private Button btnSpeed;
    private Button btnServer;
    private Button btnQuality;
    private Button btnAudio;
    private Button btnSubtitles;
    private LinearLayout loadingStatusContainer;
    private TextView loadingStatusText;
    private TextView hardsubBadge;
    private TextView tag1;
    private TextView tag2;
    private TextView tag3;
    private TextView tag4;
    private TextView tag5;

    // Settings Panel Components
    private RelativeLayout settingsPanelContainer;
    private View settingsOverlayBackground;
    private LinearLayout settingsPanel;
    private SwitchCompat switchAutoQuality;
    private SwitchCompat switchShowSubtitles;
    private SwitchCompat switchUseDoh;
    private SwitchCompat switchProgressiveCache;
    private SwitchCompat switchImageCdn;
    private TextView textBufferSize;
    private TextView textSubtitleLanguage;
    private Button btnCloseSettings;
    private boolean settingsPanelVisible = false;

    private Handler handler = new Handler();
    private Handler hideControlsHandler = new Handler();
    private Handler backPressHandler = new Handler();

    // Data
    private MediaItems mediaItems;
    private List<MediaItems.VideoSource> videoSources;
    private List<MediaItems.SubtitleItem> subtitles;
    private int currentSourceIndex = 0;
    private int currentSubtitleIndex = -1; // -1 means no subtitle selected
    private float currentSpeed = 1.0f;
    private String mediaType; // "anime", "movie", "tv", etc.
    private boolean nextEpisodeTriggered = false;

    // Quality selection tracking
    private String currentQuality = "Auto"; // Auto, High, Medium, Low
    private int currentQualityHeight = 0; // 0 = Auto, otherwise actual height in pixels

    // Shared OkHttpClient for streaming - maintains cookies/sessions from API calls
    private OkHttpClient sharedOkHttpClient;

    // Headers map - now only contains User Agent, Referer, and Origin
    Map<String, String> headers = new HashMap<>();

    // Anime server switching data
    private String episodeToken;
    private String currentServerType;
    private int currentServerIndex;
    private ArrayList<String> availableServerTypes;
    private HashMap<String, Integer> serverCounts;


    // Control visibility
    private Handler controlsHandler = new Handler(Looper.getMainLooper());
    private Runnable hideControlsRunnable = this::hideControls;
    private boolean controlsVisible = true;
    private boolean backPressedOnce = false;

    private static final int MIN_BUFFER_MS = 30000;     // 30 seconds
    private static final int PLAYBACK_BUFFER_MS = 10000; // 10 seconds
    private static final int REBUFFER_MS = 10000;
    // Watch history
    private PreferencesManager preferencesManager;
    private long startPosition = 0;
    private Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Enable fullscreen and keep screen on
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_player);

        // Continue with normal playback flow for non-anime content
        // Get media item from intent
        mediaItems = getIntent().getParcelableExtra("media_item");
        mediaType = getIntent().getStringExtra("media_type");
        if (mediaType == null) {
            mediaType = mediaItems.getMediaType();
        }
        // Get media type from intent
        Log.i(TAG, "Media type: " + mediaType);

        Log.i(TAG,"genres: "+mediaItems.getGenres());

        startPosition = getIntent().getLongExtra("start_position", 0);

        if (mediaItems == null) {
            Toast.makeText(this, "Error: No media to play", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize preferences manager
        preferencesManager = PreferencesManager.getInstance(this);

        // Initialize UI
        initializeViews();
        setupClickListeners();
        populateMediaInfo();
        fetchExternalSubtitles();

        // Initialize video sources and subtitles
        videoSources = mediaItems.getVideoSources();
        subtitles = mediaItems.getSubtitles();

        if (videoSources == null || videoSources.isEmpty()) {
            Toast.makeText(this, "No video sources available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize shared OkHttpClient - maintains cookies/sessions from FetchStreams API calls
        sharedOkHttpClient = FetchStreams.getSharedClient(this);

        // Initialize player
        initializePlayer();

        // Start playing
        loadVideoSource(currentSourceIndex);

        // Populate genre tags for media content
        populateGenreTags();


    }

    /**
     * Handle anime playback with extras passed from DetailsActivityAnime
     */



    private void initializeViews() {
        videoSurface = findViewById(R.id.videoSurface);
        backgroundImage = findViewById(R.id.backgroundImage);
        loadingIndicator = findViewById(R.id.loadingIndicator);
        centerPauseIcon = findViewById(R.id.centerPauseIcon);
        topInfoContainer = findViewById(R.id.topInfoContainer);
        bottomControlsContainer = findViewById(R.id.bottomControlsContainer);
        btnBack = findViewById(R.id.btnBack);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        progressBar = findViewById(R.id.progressBar);
        currentTime = findViewById(R.id.currentTime);
        totalTime = findViewById(R.id.totalTime);
        videoTitle = findViewById(R.id.videoTitle);
        videoDescription = findViewById(R.id.videoDescription);
        episodeInfo = findViewById(R.id.episodeInfo);
        btnSettings = findViewById(R.id.btnSettings);
        btnSpeed = findViewById(R.id.btnSpeed);
        btnServer = findViewById(R.id.btnServer);
        btnQuality = findViewById(R.id.btnQuality);
        btnAudio = findViewById(R.id.btnAudio);
        btnSubtitles = findViewById(R.id.btnSubtitles);
        btnSubtitles.setText("None"); // show no subtitle by default
        loadingStatusContainer = findViewById(R.id.loadingStatusContainer);
        loadingStatusText = findViewById(R.id.loadingStatusText);
        hardsubBadge = findViewById(R.id.hardsubBadge);
        subtitleView = findViewById(R.id.subtitleView);

        // Initialize genre tags
        tag1 = findViewById(R.id.tag1);
        tag2 = findViewById(R.id.tag2);
        tag3 = findViewById(R.id.tag3);
        tag4 = findViewById(R.id.tag4);
        tag5 = findViewById(R.id.tag5);

        // Initialize Settings Panel
        initializeSettingsPanel();

        totalTime.setText(formatTime(0));
        currentTime.setText(formatTime(0));
        if (mediaType.equals("TV") || mediaType.equals("MOVIE")) {
            btnAudio.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            saveWatchProgress();
            finish();
        });

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> togglePlayPause());

        // SeekBar
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
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

        // Speed button
        btnSpeed.setOnClickListener(v -> showSpeedDialog());

        // Server/Source button
        btnServer.setOnClickListener(v -> {

            showServerDialog();

        });

        // Quality button
        btnQuality.setOnClickListener(v -> showQualityDialog());

        // Subtitle button
        btnSubtitles.setOnClickListener(v -> showSubtitleDialog());

        // Settings button
        btnSettings.setOnClickListener(v -> {
            showSettingsPanel();
        });

        // Audio button - used for server type selection in anime
        btnAudio.setOnClickListener(v -> {

            Toast.makeText(this, "Audio track selection coming soon", Toast.LENGTH_SHORT).show();

        });

        // Touch on video surface to toggle controls
        videoSurface.setOnClickListener(v -> toggleControls());

        // Background to toggle controls
        backgroundImage.setOnClickListener(v -> toggleControls());
    }

    private void populateMediaInfo() {
        videoTitle.setText(mediaItems.getTitle());
        videoDescription.setText(mediaItems.getDescription());

        // Load background image
        String backgroundUrl = mediaItems.getPreferredBackgroundUrl();
        if (backgroundUrl != null) {
            Glide.with(this)
                    .load(backgroundUrl)
                    .centerCrop()
                    .timeout(15000)
                    .into(backgroundImage);
        }

        // Update server button with current source
        //updateServerButton();

        // Update speed button
        btnSpeed.setText(String.format(Locale.US, "Speed %.2fx", currentSpeed));

        // Set hardsub badge based on media type for non-anime content
        updateHardsubBadgeForMediaType();
    }

    private void initializePlayer() {
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
        trackSelector = new DefaultTrackSelector(this);
        trackSelector.setParameters(
                trackSelector.buildUponParameters()
                        .clearVideoSizeConstraints()
                        .setMaxVideoBitrate(Integer.MAX_VALUE)
                        .setForceHighestSupportedBitrate(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowAudioMixedMimeTypeAdaptiveness(true)
                        .setPreferredTextLanguage("en")  // Enable subtitle track
                        .setSelectUndeterminedTextLanguage(true)  // Show subtitles even if language is undefined
        );

        // Enable legacy text rendering for subtitle support
        player = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector)
                .setRenderersFactory(
                        new DefaultRenderersFactory(this)
                )
                .build();


        // Configure subtitle view
        subtitleView.setUserDefaultStyle();
        subtitleView.setUserDefaultTextSize();
        subtitleView.bringToFront();

        player.setVideoSurfaceView(videoSurface);

        // Add player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onCues(@NonNull CueGroup cueGroup) {
                subtitleView.setCues(cueGroup.cues);
                if (cueGroup.cues.isEmpty()) {
                    Log.i(TAG, "Subtitle cues cleared (empty)");
                } else {
                    for (int i = 0; i < cueGroup.cues.size(); i++) {
                        Cue cue = cueGroup.cues.get(i);
                        Log.i(TAG, "Cue[" + i + "] text: " + cue.text
                                + " | presentationTimeUs: " + cueGroup.presentationTimeUs);
                    }
                }
            }

            @Override
            public void onTracksChanged(@NonNull Tracks tracks) {
                // Log available tracks for debugging
                for (Tracks.Group trackGroup : tracks.getGroups()) {
                    if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                        Log.i(TAG, "Text track group found with " + trackGroup.length + " tracks");
                        for (int i = 0; i < trackGroup.length; i++) {
                            if (trackGroup.isTrackSupported(i)) {
                                Log.i(TAG, "  Track " + i + " is supported and selected: " +
                                        trackGroup.isTrackSelected(i));
                            }
                        }
                    }
                }
            }

            @Override
            public void onPlaybackStateChanged(int playbackState) {
                switch (playbackState) {
                    case Player.STATE_BUFFERING:

                        showLoadingServer();
                        break;
                    case Player.STATE_READY:
                        loadingIndicator.setVisibility(View.GONE);
                        hideLoading();
                        updateLoadingUi(false);


                        // Immediately show seekbar, total time, and reset current time
                        long duration = player.getDuration();
                        progressBar.setMax((int) duration);
                        totalTime.setText(formatTime(duration));
                        currentTime.setText(formatTime(0)); // Reset to 00:00

                        // Make sure buffer is updated immediately
                        updateBufferProgress();

                        // Hide streaming status immediately and show controls
                        hideStreamingStatus();

                        // Select subtitle track after media is ready
                        selectSubtitleTrack();

                        // Show controls so user can see seekbar and time
                        showControls();

                        showCurrentTrackInfo();
                        updateQualityButton();

                        // Auto-start playback after a short delay for visual feedback
                        handler.postDelayed(() -> {
                            if (!player.isPlaying()) {
                                togglePlayPause();
                            }
                        }, 500);
                        handler.post(updateProgressTask);
                        break;
                    case Player.STATE_ENDED:
                        updateLoadingUi(false);
                        btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        showControls();
                        // Check for auto-play next episode
                        if ("TV".equalsIgnoreCase(mediaType)) {
                            playNextEpisode();
                        }
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Player error: " + error.getMessage());
                handlePlayerError(error);
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseButton(isPlaying);
                if (isPlaying) {
                    videoSurface.setVisibility(View.VISIBLE);
                    handler.post(updateProgressTask);
                    hideControls();
                } else {
                    showControls();
                }
            }
        });
    }

    private void updateBufferProgress() {
        if (player != null) {
            long bufferedPosition = player.getBufferedPosition();
            long duration = player.getDuration();
            long currentPosition = player.getCurrentPosition();

            if (duration > 0) {
                long bufferDuration = bufferedPosition - currentPosition;

                // Use actual millisecond values since max is set to duration in ms
                progressBar.setSecondaryProgress((int) bufferedPosition);

//                Log.i(TAG, "Current: " + currentPosition + "ms | Buffered: " + bufferedPosition +
//                        "ms | Max: " + duration + "ms | Buffer Duration: " + formatTime((int) bufferDuration));
            }
        }
    }


    private void loadVideoSource(int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= videoSources.size()) {
            Toast.makeText(this, "Invalid video source", Toast.LENGTH_SHORT).show();
            return;
        }

        currentSourceIndex = sourceIndex;
        MediaItems.VideoSource source = videoSources.get(sourceIndex);

        Log.i(TAG, "Loading video source: " + source.getQuality() + " - " + source.getUrl());

        // Show loading
        showLoadingServer();

        // Build data source factory with custom headers
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(source);

        // Build MediaItem with subtitles
        String url = source.getUrl();
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(url);

        // Add subtitle if selected
        if (currentSubtitleIndex >= 0 && currentSubtitleIndex < subtitles.size()) {
            MediaItems.SubtitleItem subtitle = subtitles.get(currentSubtitleIndex);

            // Detect MIME type from subtitle URL file extension
            String mimeType = detectSubtitleMimeType(subtitle.getUrl());
            // Get actual language code from subtitle metadata
            String languageCode = getLanguageCodeFromSubtitle(subtitle);

            Log.i(TAG, "Adding subtitle to MediaItem: " + subtitle.getLanguage() + " - " + subtitle.getUrl());
            Log.i(TAG, "Detected MIME type: " + mimeType + ", Language code: " + languageCode);

            // Use actual language code from subtitle metadata instead of hardcoded "en"
            MediaItem.SubtitleConfiguration.Builder subtitleBuilder =
                    new MediaItem.SubtitleConfiguration.Builder(
                            android.net.Uri.parse(subtitle.getUrl())
                    )
                            .setMimeType(mimeType)
                            // Use the actual language code from subtitle metadata
                            .setLanguage(languageCode)
                            .setLabel(subtitle.getLanguage())
                            .setSelectionFlags(
                                    C.SELECTION_FLAG_DEFAULT
                                            | C.SELECTION_FLAG_AUTOSELECT
                            )
                            .setRoleFlags(C.ROLE_FLAG_SUBTITLE);

            // Note: In Media3, subtitle tracks use the same DataSource.Factory as the video,
            // so custom headers are automatically applied through buildDataSourceFactory()

            MediaItem.SubtitleConfiguration subtitleConfig = subtitleBuilder.build();

            mediaItemBuilder.setSubtitleConfigurations(java.util.Collections.singletonList(subtitleConfig));

            Log.i(TAG, "Subtitle configuration successfully added with URL: " + subtitle.getUrl());
        } else {
            Log.i(TAG, "No subtitle selected (index: " + currentSubtitleIndex + ", subtitles size: " +
                    (subtitles != null ? subtitles.size() : "null") + ")");
        }

        MediaItem mediaItem = mediaItemBuilder.build();
        hideLoading();
        showStreamingStatus();

        // Build media source based on URL type
        MediaSource videoSource;

        if (url.contains(".m3u8")) {
            // HLS stream
            videoSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else if (url.contains(".mpd")) {
            // DASH stream
            videoSource = new DashMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            // Progressive stream
            videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }

        // Prepare player
        player.setMediaSource(videoSource);
        player.prepare();

        // Seek to start position if provided
        if (startPosition > 0) {
            player.seekTo(startPosition);
            startPosition = 0; // Reset after first use
        }

        player.setPlayWhenReady(true);
        showCurrentTrackInfo();

        // Update UI
        updateServerButton();
        updateQualityButton();
    }

    private DataSource.Factory buildDataSourceFactory(MediaItems.VideoSource source) {
        // Clear previous headers - we only want User Agent, Referer, and Origin
        headers.clear();

        // Only add User Agent - the shared OkHttpClient handles cookies/sessions
        String userAgent = FetchStreams.getUserAgent();
        headers.put("User-Agent", userAgent);

        // Add Referer and Origin based on URL patterns
        if (source.getUrl().startsWith("https://one.techparadise")) {
            // Videasy: Use accurate headers from EncDecEndpoints
            headers.put("Referer", "https://videasy.net/");
            headers.put("Origin", "https://player.videasy.net");
        } else if (source.getUrl().startsWith("https://p.10015")) {
            // Hexa: Keep existing headers
            headers.put("Referer", "https://hexa.su/");
            headers.put("Origin", "https://hexa.su/");
        } else if (source.getUrl().startsWith("https://storm.vodvidl.site")) {
            // Vidlink: Use accurate headers from EncDecEndpoints
            headers.put("Referer", "https://vidlink.pro/");
            headers.put("Origin", "https://vidlink.pro");
        } else {
            // Use the referer from the source if available
            String refererUrl = source.getRefererUrl();
            if (refererUrl != null && !refererUrl.isEmpty()) {
                headers.put("Referer", refererUrl);
                // Extract origin from referer URL
                try {
                    java.net.URL url = new java.net.URL(refererUrl);
                    headers.put("Origin", url.getProtocol() + "://" + url.getHost());
                } catch (Exception e) {
                    // Use default origin if parsing fails
                }
            }
        }

        // Use the shared OkHttpClient which maintains cookies/sessions from FetchStreams
        // This shares the connection pool and cookie jar with API calls
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(sharedOkHttpClient);

        // Set custom headers for the data source
        if (!headers.isEmpty()) {
            okHttpDataSourceFactory.setDefaultRequestProperties(headers);
        }

        return new DefaultDataSource.Factory(this, okHttpDataSourceFactory);
    }

    /**
     * Detect MIME type from subtitle file URL based on extension
     */
    private String detectSubtitleMimeType(String subtitleUrl) {
        if (subtitleUrl == null) {
            return MimeTypes.TEXT_VTT;
        }

        // strip query parameters to reliably inspect extension
        String path = subtitleUrl.split("\\?")[0].toLowerCase(Locale.US);

        if (path.endsWith(".srt")) {
            Log.i(TAG, "Detected subtitle format: SRT");
            return MimeTypes.APPLICATION_SUBRIP;
        } else if (path.endsWith(".ssa") || path.endsWith(".ass")) {
            Log.i(TAG, "Detected subtitle format: SSA/ASS");
            return MimeTypes.TEXT_SSA;
        } else if (path.endsWith(".ttml") || path.endsWith(".xml")) {
            Log.i(TAG, "Detected subtitle format: TTML");
            return MimeTypes.APPLICATION_TTML;
        } else if (path.endsWith(".vtt")) {
            Log.i(TAG, "Detected subtitle format: WebVTT");
            return MimeTypes.TEXT_VTT;
        } else {
            // Default to WebVTT and log a warning
            Log.w(TAG, "Unknown subtitle format, defaulting to WebVTT. URL: " + subtitleUrl);
            return MimeTypes.TEXT_VTT;
        }
    }

    /**
     * Get language code from subtitle metadata, with fallback to 'und' (undetermined)
     */
    private String getLanguageCodeFromSubtitle(MediaItems.SubtitleItem subtitle) {
        if (subtitle == null) {
            return "und";
        }

        // Try to get language code from 'lang' field first (usually 2-letter ISO code)
        String lang = subtitle.getLang();
        if (lang != null && !lang.isEmpty()) {
            // Normalize language code to 2-letter ISO 639-1 if possible
            if (lang.length() > 2) {
                lang = lang.substring(0, 2);
            }
            Log.i(TAG, "Using language code from subtitle metadata: " + lang);
            return lang;
        }

        // Fallback to language label and try to extract code
        String language = subtitle.getLanguage();
        if (language != null && !language.isEmpty()) {
            String extractedCode = extractLanguageCode(language);
            if (extractedCode != null) {
                Log.i(TAG, "Extracted language code from label: " + extractedCode);
                return extractedCode;
            }
        }

        Log.w(TAG, "No language code found in subtitle metadata, using 'und'");
        return "und"; // Undetermined language
    }

    /**
     * Extract language code from common language label formats
     */
    private String extractLanguageCode(String languageLabel) {
        if (languageLabel == null) {
            return null;
        }

        String lower = languageLabel.toLowerCase(Locale.US);

        if (lower.contains("english") || lower.equals("en") || lower.contains("eng")) {
            return "en";
        } else if (lower.contains("spanish") || lower.equals("es") || lower.contains("español")) {
            return "es";
        } else if (lower.contains("french") || lower.equals("fr") || lower.contains("français")) {
            return "fr";
        } else if (lower.contains("german") || lower.equals("de") || lower.contains("deutsch")) {
            return "de";
        } else if (lower.contains("portuguese") || lower.equals("pt")) {
            return "pt";
        } else if (lower.contains("japanese") || lower.equals("ja") || lower.contains("jp")) {
            return "ja";
        } else if (lower.contains("korean") || lower.equals("ko")) {
            return "ko";
        } else if (lower.contains("chinese") || lower.equals("zh")) {
            return "zh";
        } else if (lower.contains("arabic") || lower.equals("ar")) {
            return "ar";
        } else if (lower.contains("russian") || lower.equals("ru")) {
            return "ru";
        } else if (lower.contains("italian") || lower.equals("it")) {
            return "it";
        } else if (lower.contains("dutch") || lower.equals("nl")) {
            return "nl";
        } else if (lower.contains("polish") || lower.equals("pl")) {
            return "pl";
        } else if (lower.contains("turkish") || lower.equals("tr")) {
            return "tr";
        } else if (lower.contains("indonesian") || lower.equals("id")) {
            return "id";
        } else if (lower.contains("thai") || lower.equals("th")) {
            return "th";
        } else if (lower.contains("vietnamese") || lower.equals("vi")) {
            return "vi";
        }

        // Return null if no match found
        return null;
    }



    private void selectSubtitleTrack() {
        if (player == null) {
            Log.w(TAG, "Cannot select subtitle track: player is null");
            return;
        }

        if (currentSubtitleIndex < 0) {
            Log.i(TAG, "No subtitle selected (index -1), disabling text tracks");
            // Properly disable text renderer instead of fiddling with flags
            TrackSelectionParameters disableParams = player.getTrackSelectionParameters()
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build();
            player.setTrackSelectionParameters(disableParams);
            // clear any existing cues so the subtitle overlay disappears immediately
            subtitleView.setCues(Collections.emptyList());
            return;
        }

        if (subtitles == null || currentSubtitleIndex >= subtitles.size()) {
            Log.w(TAG, "Cannot select subtitle track: subtitles list is null or index is out of bounds");
            return;
        }

        MediaItems.SubtitleItem subtitle = subtitles.get(currentSubtitleIndex);
        if (subtitle == null) {
            Log.w(TAG, "Cannot select subtitle track: subtitle is null");
            return;
        }

        // Get the actual language code from subtitle metadata
        String subtitleLanguage = getLanguageCodeFromSubtitle(subtitle);
        Log.i(TAG, "Selecting subtitle track: " + subtitle.getLanguage() + " (language code: " + subtitleLanguage + ")");

        // Check available tracks first
        Tracks tracks = player.getCurrentTracks();
        boolean hasTextTracks = false;
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_TEXT) {
                hasTextTracks = true;
                Log.i(TAG, "Found text track group with " + trackGroup.length + " tracks");
                for (int i = 0; i < trackGroup.length; i++) {
                    Log.i(TAG, "  Track " + i + ": supported=" + trackGroup.isTrackSupported(i) +
                            ", selected=" + trackGroup.isTrackSelected(i));
                }
            }
        }

        if (!hasTextTracks) {
            Log.w(TAG, "No text tracks found in media - subtitles may be embedded externally");
        }

        // Build track selection parameters to enable text tracks using actual subtitle language
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters()
                .buildUpon()
                // make sure text renderer is enabled in case it was disabled previously
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(subtitleLanguage)
                .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE | C.ROLE_FLAG_CAPTION)
                .setSelectUndeterminedTextLanguage(true)
                .setIgnoredTextSelectionFlags(0); // Don't ignore any text tracks

        // Apply track selection parameters
        player.setTrackSelectionParameters(builder.build());

        Log.i(TAG, "Subtitle track selection completed for: " + subtitle.getLanguage());
    }


    private void handlePlayerError(PlaybackException error) {
        hideLoading();
        hideStreamingStatus();
        Toast.makeText(this, "Playback error: " + error.getMessage(), Toast.LENGTH_LONG).show();

        // Try next server if available
        if (currentSourceIndex < videoSources.size() - 1) {

            loadVideoSource(currentSourceIndex + 1);

        } else {
            loadingIndicator.setVisibility(View.GONE);
            loadingStatusContainer.setVisibility(View.GONE);
        }
    }

    private void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            centerPauseIcon.setVisibility(View.GONE);
        } else {
            btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            centerPauseIcon.setVisibility(View.VISIBLE);
        }
    }

    private void togglePlayPause() {
        if (player == null) return;

        if (player.isPlaying()) {
            player.pause();
        } else {
            player.play();
            showCurrentTrackInfo();
        }
        showControls();
    }

    private void toggleControls() {
        if (controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }


    private void showControls() {
        topInfoContainer.setVisibility(View.VISIBLE);
        bottomControlsContainer.setVisibility(View.VISIBLE);
        //btnBack.setVisibility(View.VISIBLE);
        controlsVisible = true;

        // Set focus to Settings button by default
        btnSettings.requestFocus();

        resetHideControlsTimer();
    }

    private void resetHideControlsTimer() {
        hideControlsHandler.removeCallbacks(hideControlsTask);
        if (player.isPlaying()) {
            hideControlsHandler.postDelayed(hideControlsTask, 6000);
        }
    }

    private Runnable hideControlsTask = () -> {
        if (player.isPlaying() && controlsVisible) {
            hideControls();
        }
    };

    private Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                updateTimeDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };

    private void updateTimeDisplay() {
        if (player != null) {
            int currentPos = (int) player.getCurrentPosition();
            long duration = player.getDuration();

            // Update current time text
            currentTime.setText(formatTime(currentPos));
//            if (currentTime.getVisibility() == View.GONE) {
//                currentTime.setVisibility(View.VISIBLE);
//            }

            // Update progress in milliseconds (matching the max value)
            if (duration > 0) {
                progressBar.setProgress(currentPos);

                // Check if 3 minutes (180,000 ms) are remaining
                if ("TV".equals(mediaType) && !nextEpisodeTriggered) {
                    long remainingTime = duration - currentPos;
                    if (remainingTime <= 180000 && remainingTime > 0) {
                        nextEpisodeTriggered = true;
                        playNextEpisode();
                    }
                }
            }

            // Update buffered progress display
            updateBufferProgress();
            hideLoading();
            updateLoadingUi(false);
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


    private void hideControls() {
        topInfoContainer.setVisibility(View.GONE);
        bottomControlsContainer.setVisibility(View.GONE);
        //btnEpisodes.setVisibility(View.GONE);
        //btnBack.setVisibility(View.GONE);
        controlsVisible = false;
    }


    private void showLoadingServer() {
        updateLoadingUi(true);
        // Hide play/pause button and current time when loading
        btnPlayPause.setVisibility(View.GONE);
        currentTime.setVisibility(View.GONE);
        loadingStatusText.setText("LOADING SERVER");
    }

    private void showStreamingStatus() {
        updateLoadingUi(true);
        // Hide play/pause button and current time when loading
        btnPlayPause.setVisibility(View.GONE);
        currentTime.setVisibility(View.GONE);
        loadingStatusText.setText("STREAMING VIDEO");
    }

    private void hideLoading() {
        loadingIndicator.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
        currentTime.setVisibility(View.VISIBLE);
    }

    private void hideStreamingStatus() {
        loadingStatusContainer.setVisibility(View.GONE);
        btnPlayPause.setVisibility(View.VISIBLE);
        currentTime.setVisibility(View.VISIBLE);
    }


    private void updateTimeTexts() {
        if (player == null) return;
        currentTime.setText(formatTime(player.getCurrentPosition()));
        totalTime.setText(formatTime(player.getDuration()));
    }

    private String formatTime(long milliseconds) {
        if (milliseconds <= 0) return "00:00";

        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void showSpeedDialog() {
        float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        String[] speedLabels = new String[speeds.length];

        for (int i = 0; i < speeds.length; i++) {
            speedLabels[i] = String.format(Locale.US, "%.2fx", speeds[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Playback Speed");
        builder.setItems(speedLabels, (dialog, which) -> {
            currentSpeed = speeds[which];
            if (player != null) {
                player.setPlaybackSpeed(currentSpeed);
            }
            btnSpeed.setText(String.format(Locale.US, "Speed %.2fx", currentSpeed));
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();

    }

    private void showServerDialog() {
        if (videoSources == null || videoSources.isEmpty()) {
            Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] serverLabels = new String[videoSources.size()];
        for (int i = 0; i < videoSources.size(); i++) {
            serverLabels[i] = videoSources.get(i).getQuality();
            Log.i(TAG, "Server label: " + videoSources.get(i).getQuality());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Server");
        builder.setItems(serverLabels, (dialog, which) -> {
            currentSourceIndex = which;
            startPosition = player.getCurrentPosition();
            loadVideoSource(currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    private void showQualityDialog() {
        if (player == null) {
            showToast("No player");
            return;
        }

        // Get available tracks to determine what qualities are available
        Tracks tracks = player.getCurrentTracks();

        // Determine available heights from video tracks
        List<Integer> availableHeights = new ArrayList<>();
        if (tracks != null && !tracks.isEmpty()) {
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i = 0; i < trackGroup.length; i++) {
                        Format format = trackGroup.getTrackFormat(i);
                        if (format.height > 0 && !availableHeights.contains(format.height)) {
                            availableHeights.add(format.height);
                        }
                    }
                }
            }
        }

        // Sort heights in descending order
        Collections.sort(availableHeights, Collections.reverseOrder());

        // Find the highest quality available
        int highestHeight = availableHeights.isEmpty() ? 1080 : availableHeights.get(0);

        // Inflate custom layout
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_quality, null);

        // Get references to views
        RadioButton radioAuto = dialogView.findViewById(R.id.radioAuto);
        RadioButton radioHigh = dialogView.findViewById(R.id.radioHigh);
        RadioButton radioMedium = dialogView.findViewById(R.id.radioMedium);
        RadioButton radioLow = dialogView.findViewById(R.id.radioLow);

        LinearLayout optionAuto = dialogView.findViewById(R.id.optionAuto);
        LinearLayout optionHigh = dialogView.findViewById(R.id.optionHigh);
        LinearLayout optionMedium = dialogView.findViewById(R.id.optionMedium);
        LinearLayout optionLow = dialogView.findViewById(R.id.optionLow);

        // Set initial selection based on current quality
        switch (currentQuality) {
            case "Auto":
                radioAuto.setChecked(true);
                break;
            case "High":
                radioHigh.setChecked(true);
                break;
            case "Medium":
                radioMedium.setChecked(true);
                break;
            case "Low":
                radioLow.setChecked(true);
                break;
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Set click listeners for each option
        optionAuto.setOnClickListener(v -> {
            selectQuality("Auto", 0, highestHeight);
            dialog.dismiss();
        });

        optionHigh.setOnClickListener(v -> {
            selectQuality("High", highestHeight, highestHeight);
            dialog.dismiss();
        });

        optionMedium.setOnClickListener(v -> {
            selectQuality("Medium", 720, highestHeight);
            dialog.dismiss();
        });

        optionLow.setOnClickListener(v -> {
            selectQuality("Low", 360, highestHeight);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Select video quality and update track selection
     * @param qualityName Quality level: Auto, High, Medium, Low
     * @param targetHeight Target height in pixels (0 for Auto)
     * @param highestAvailable Highest quality available in the stream
     */
    private void selectQuality(String qualityName, int targetHeight, int highestAvailable) {
        currentQuality = qualityName;
        currentQualityHeight = targetHeight;

        // Update button text
        String buttonText = qualityName;
        if (qualityName.equals("High")) {
            buttonText = "High " + highestAvailable + "p";
        } else if (qualityName.equals("Medium")) {
            buttonText = "Medium 720p";
        } else if (qualityName.equals("Low")) {
            buttonText = "Low 360p";
        }
        btnQuality.setText(buttonText);

        if (player == null) {
            return;
        }

        if (qualityName.equals("Auto")) {
            // Enable auto quality - clear overrides and let player decide
            TrackSelectionParameters params = player.getTrackSelectionParameters()
                    .buildUpon()
                    .clearOverrides()
                    .build();
            player.setTrackSelectionParameters(params);
            showToast("Quality: Auto (best network quality)");
        } else {
            // Find the track closest to target height
            Tracks tracks = player.getCurrentTracks();
            if (tracks == null || tracks.isEmpty()) {
                showToast("No tracks available");
                return;
            }

            int selectedHeight = 0;
            TrackSelectionOverride selectedOverride = null;

            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                    TrackGroup mediaTrackGroup = trackGroup.getMediaTrackGroup();

                    for (int i = 0; i < trackGroup.length; i++) {
                        Format format = trackGroup.getTrackFormat(i);
                        int height = format.height;

                        if (height > 0) {
                            // For High: select highest available
                            // For Medium: select 720 or closest below
                            // For Low: select 360 or closest below
                            boolean shouldSelect = false;

                            if (qualityName.equals("High")) {
                                // Select highest quality available
                                if (height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            } else if (qualityName.equals("Medium")) {
                                // Select 720p or closest below
                                if (height <= 720 && height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            } else if (qualityName.equals("Low")) {
                                // Select 360p or closest below
                                if (height <= 360 && height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            }

                            if (shouldSelect) {
                                selectedOverride = new TrackSelectionOverride(
                                        mediaTrackGroup,
                                        Collections.singletonList(i)
                                );
                            }
                        }
                    }
                }
            }

            if (selectedOverride != null) {
                TrackSelectionParameters params = player.getTrackSelectionParameters()
                        .buildUpon()
                        .clearOverrides()
                        .addOverride(selectedOverride)
                        .build();
                player.setTrackSelectionParameters(params);
                showToast("Quality: " + qualityName + " " + selectedHeight + "p");
            } else {
                // If the requested quality is not available, fall back to auto
                showToast("Quality " + targetHeight + "p not available, using Auto");
                selectQuality("Auto", 0, highestAvailable);
            }
        }
    }

    // Helper method to switch tracks
    private void switchToTrack(TrackSelectionOverride override) {
        if (player == null) return;

        TrackSelectionParameters.Builder parametersBuilder = player.getTrackSelectionParameters()
                .buildUpon()
                .clearOverrides();

        if (override != null) {
            // Specific quality selected
            parametersBuilder.addOverride(override);
            showToast("Quality changed to " + override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");
            Log.i(TAG, "Quality changed to " + override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");

            btnQuality.setText(override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");
        } else {
            // Auto quality selected
            showToast("Auto quality selected");
        }

        player.setTrackSelectionParameters(parametersBuilder.build());
    }


    private void showCurrentTrackInfo() {
        String info = getCurrentTrackInfo();
        Toast.makeText(this, info, Toast.LENGTH_SHORT).show();
    }

    private String getCurrentTrackInfo() {
        if (player == null) {
            return "No player";
        }

        Tracks tracks = player.getCurrentTracks();
        if (tracks == null || tracks.isEmpty()) {
            return "No tracks";
        }

        StringBuilder info = new StringBuilder();
        info.append("Current Tracks:\n");

        for (Tracks.Group trackGroup : tracks.getGroups()) {
            int trackType = trackGroup.getType();

            for (int i = 0; i < trackGroup.length; i++) {
                if (trackGroup.isTrackSelected(i)) {
                    // Access format directly
                    int width = trackGroup.getTrackFormat(i).width;
                    int height = trackGroup.getTrackFormat(i).height;
                    int bitrate = trackGroup.getTrackFormat(i).bitrate;
                    int sampleRate = trackGroup.getTrackFormat(i).sampleRate;
                    String language = trackGroup.getTrackFormat(i).language;

                    if (trackType == C.TRACK_TYPE_VIDEO) {
                        info.append("Video: ");
                        if (width > 0 && height > 0) {
                            info.append(width).append("x").append(height);
                        } else {
                            info.append("Auto");
                        }
                        if (bitrate > 0) {
                            info.append(" (").append(bitrate / 1000).append(" kbps)");
                        }
                        info.append("\n");
                    } else if (trackType == C.TRACK_TYPE_AUDIO) {
                        info.append("Audio: ").append(sampleRate > 0 ? sampleRate + " Hz" : "Unknown");
                        if (bitrate > 0) {
                            info.append(" (").append(bitrate / 1000).append(" kbps)");
                        }
                        info.append("\n");
                    } else if (trackType == C.TRACK_TYPE_TEXT) {
                        info.append("Subtitle: ").append(language != null ? language : "Unknown");
                        info.append("\n");
                    }
                }
            }
        }
        Log.i(TAG, "Current Tracks: " + info.toString());

        return info.toString();
    }

    private void showSubtitleDialog() {
        if (subtitles == null || subtitles.isEmpty()) {
            fetchExternalSubtitles();
            return;
        }

        // prepare labels array with an explicit "None" option at index 0
        String[] subtitleLabels = new String[subtitles.size() + 1];
        subtitleLabels[0] = "None";
        for (int i = 0; i < subtitles.size(); i++) {
            subtitleLabels[i + 1] = subtitles.get(i).getLanguage();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle");
        builder.setItems(subtitleLabels, (dialog, which) -> {
            if (which == 0) {
                // user chose "None" – clear selection
                currentSubtitleIndex = -1;
                btnSubtitles.setText("None");
            } else {
                currentSubtitleIndex = which - 1;
                btnSubtitles.setText(subtitles.get(currentSubtitleIndex).getLanguage());
            }
            // remember current playback position so we can resume after reload
            if (player != null) {
                startPosition = player.getCurrentPosition();
            }
            // reload source so MediaItem is rebuilt without a subtitle track when none selected
            loadVideoSource(currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    private void fetchExternalSubtitles() {
        if (mediaItems == null || mediaItems.getTmdbId() == null) {
            Toast.makeText(this, "Cannot fetch subtitles: No TMDB ID", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Fetching subtitles for TMDB ID: " + mediaItems.getTmdbId());


        showLoadingServer();
        loadingStatusText.setText("FETCHING SUBTITLES...");

        SubdlService subdlService = new SubdlService(this);
        SubdlService.Callback callback = new SubdlService.Callback() {
            @Override
            public void onSuccess(List<Subtitle> externalSubtitles) {
                runOnUiThread(() -> {
                    hideLoading();
                    if (externalSubtitles.isEmpty()) {
                        Toast.makeText(PlayerActivity.this, "No external subtitles found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert Subdl subtitles to MediaItems.SubtitleItem
                    subtitles = new ArrayList<>();
                    for (Subtitle sub : externalSubtitles) {
                        MediaItems.SubtitleItem item = new MediaItems.SubtitleItem();
                        String fileName = sub.srtUri.getLastPathSegment();
                        item.setLanguage(sub.language + " (" + fileName + ")");
                        item.setUrl(sub.srtUri.toString());
                        item.setLang(sub.language);
                        subtitles.add(item);
                    }

                    // reset any previously selected index – user will choose from new list
                    currentSubtitleIndex = -1;
                    if (btnSubtitles != null) {
                        btnSubtitles.setText("None");
                    }

                    // Show dialog again with new subtitles
                    //showSubtitleDialog();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    hideLoading();
                    Log.e(TAG, "Subdl error: " + error);
                    Toast.makeText(PlayerActivity.this, "Error fetching subtitles: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        };

        try {
            int tmdbId = Integer.parseInt(mediaItems.getTmdbId());
            if ("TV".equals(mediaType)) {
                int season = Integer.parseInt(mediaItems.getSeason());
                int episode = Integer.parseInt(mediaItems.getEpisode());
                subdlService.fetchTvSubtitles(tmdbId, season, episode, "EN", callback);
            } else {
                subdlService.fetchSubtitles(tmdbId, "movie", "EN", callback);
            }
        } catch (NumberFormatException e) {
            runOnUiThread(() -> {
                hideLoading();
                Toast.makeText(PlayerActivity.this, "Invalid TMDB ID or metadata", Toast.LENGTH_SHORT).show();
            });
        }
    }

    /**
     * Update anime server button labels
     */
    private void updateAnimeServerButtons() {
        if (currentServerType != null && serverCounts != null) {
            // Update Audio button to show current server type
            String typeLabel = currentServerType.toUpperCase();
            btnAudio.setText(typeLabel);

            // Update Server button to show current server index
            Integer count = serverCounts.get(currentServerType);
            if (count != null && count > 1) {
                btnServer.setText(videoSources.get(currentSourceIndex).getQuality());
            } else {
                btnServer.setText("Server 1");
            }

            // Update hardsub badge to show server type for anime content
            hardsubBadge.setText(typeLabel);
            hardsubBadge.setVisibility(View.VISIBLE);

            Log.i(TAG, "Updated buttons: Type=" + typeLabel + ", Server=" + (currentServerIndex + 1));
        }
    }

    /**
     * Update hardsub badge based on media type for non-anime content
     */
    private void updateHardsubBadgeForMediaType() {
        if ("anime".equals(mediaType)) {
            // For anime, badge will be updated via updateAnimeServerButtons() based on server type
            hardsubBadge.setVisibility(View.GONE);
        } else {
            // For non-anime (movies, TV shows), set badge based on media type
            if (mediaItems != null) {
                hardsubBadge.setText(mediaType);
            } else {
                hardsubBadge.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Populate genre tags from MediaItems genres list
     */
    private void populateGenreTags() {
        if (mediaItems == null) {
            hideAllGenreTags();
            return;
        }

        List<String> genres = mediaItems.getGenres();
        if (genres == null || genres.isEmpty()) {
            hideAllGenreTags();
            return;
        }

        Log.i(TAG, "Populating " + genres.size() + " genre tags");

        // Create array of tag TextViews
        TextView[] tags = {tag1, tag2, tag3, tag4, tag5};

        // Hide all tags first
        for (TextView tag : tags) {
            tag.setVisibility(View.GONE);
        }

        // Populate tags with genre names (up to 5 genres)
        int tagIndex = 0;
        for (String genre : genres) {
            if (tagIndex >= tags.length) {
                break; // Only show first 5 genres
            }

            if (genre != null && !genre.isEmpty()) {
                tags[tagIndex].setText(genre);
                tags[tagIndex].setVisibility(View.VISIBLE);
                Log.i(TAG, "Set tag" + (tagIndex + 1) + " to: " + genre);
                tagIndex++;
            }
        }

        // Hide remaining unused tags
        for (int i = tagIndex; i < tags.length; i++) {
            tags[i].setVisibility(View.GONE);
        }
    }

    /**
     * Populate genre tags for anime content using description string
     * Anime API doesn't provide genres in a structured list, so we extract from description
     */
    private void populateAnimeGenreTags(String animeDescription) {
        // Common anime genres to look for in descriptions
        List<String> commonGenres = new ArrayList<>();
        commonGenres.add("Action");
        commonGenres.add("Adventure");
        commonGenres.add("Comedy");
        commonGenres.add("Drama");
        commonGenres.add("Ecchi");
        commonGenres.add("Fantasy");
        commonGenres.add("Horror");
        commonGenres.add("Mahou Shoujo");
        commonGenres.add("Mecha");
        commonGenres.add("Music");
        commonGenres.add("Mystery");
        commonGenres.add("Psychological");
        commonGenres.add("Romance");
        commonGenres.add("Sci-Fi");
        commonGenres.add("Slice of Life");
        commonGenres.add("Sports");
        commonGenres.add("Supernatural");
        commonGenres.add("Thriller");

        // Create array of tag TextViews
        TextView[] tags = {tag1, tag2, tag3, tag4, tag5};

        // Hide all tags first
        for (TextView tag : tags) {
            tag.setVisibility(View.GONE);
        }

        // Try to find genres in description
        List<String> foundGenres = new ArrayList<>();

        if (animeDescription != null) {
            String lowerDesc = animeDescription.toLowerCase(Locale.US);

            for (String genre : commonGenres) {
                if (foundGenres.size() >= 5) {
                    break; // Only need 5 genres
                }

                if (lowerDesc.contains(genre.toLowerCase())) {
                    foundGenres.add(genre);
                    Log.i(TAG, "Found genre in description: " + genre);
                }
            }
        }

        // If no genres found in description, set default anime genres
        if (foundGenres.isEmpty()) {
            foundGenres.add("Anime");
            foundGenres.add("Animation");
            Log.i(TAG, "No genres found in description, using defaults");
        }

        // Populate tags
        int tagIndex = 0;
        for (String genre : foundGenres) {
            if (tagIndex >= tags.length) {
                break;
            }
            tags[tagIndex].setText(genre);
            tags[tagIndex].setVisibility(View.VISIBLE);
            Log.i(TAG, "Set anime tag" + (tagIndex + 1) + " to: " + genre);
            tagIndex++;
        }

        // Hide remaining unused tags
        for (int i = tagIndex; i < tags.length; i++) {
            tags[i].setVisibility(View.GONE);
        }
    }

    /**
     * Hide all genre tag TextViews
     */
    private void hideAllGenreTags() {
        tag1.setVisibility(View.GONE);
        tag2.setVisibility(View.GONE);
        tag3.setVisibility(View.GONE);
        tag4.setVisibility(View.GONE);
        tag5.setVisibility(View.GONE);
    }




    /**
     * Switch to a specific server
     */
    private void switchToServer(String serverType, int serverIndex) {


        Log.i(TAG, "Switching to " + serverType + " Server " + (serverIndex + 1));

        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        boolean wasPlaying = player != null && player.isPlaying();

        loadingIndicator.setVisibility(View.VISIBLE);
        loadingStatusContainer.setVisibility(View.VISIBLE);
        loadingStatusText.setText("Loading " + serverType.toUpperCase() +
                " Server " + (serverIndex + 1) + "...");
        loadingIndicator.setVisibility(View.GONE);
        loadingStatusContainer.setVisibility(View.GONE);





    }

    private void updateServerButton() {
        btnServer.setText(videoSources.get(currentSourceIndex).getQuality());
    }

    private void updateQualityButton() {
        // Use the current quality selection to update button text
        switch (currentQuality) {
            case "Auto":
                btnQuality.setText("Auto");
                break;
            case "High":
                // Try to find the highest available quality
                if (player != null) {
                    Tracks tracks = player.getCurrentTracks();
                    if (tracks != null && !tracks.isEmpty()) {
                        int highestHeight = 0;
                        for (Tracks.Group trackGroup : tracks.getGroups()) {
                            if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                                for (int i = 0; i < trackGroup.length; i++) {
                                    Format format = trackGroup.getTrackFormat(i);
                                    if (format.height > highestHeight) {
                                        highestHeight = format.height;
                                    }
                                }
                            }
                        }
                        if (highestHeight > 0) {
                            btnQuality.setText("High " + highestHeight + "p");
                        } else {
                            btnQuality.setText("High");
                        }
                    } else {
                        btnQuality.setText("High");
                    }
                } else {
                    btnQuality.setText("High");
                }
                break;
            case "Medium":
                btnQuality.setText("Medium 720p");
                break;
            case "Low":
                btnQuality.setText("Low 360p");
                break;
            default:
                btnQuality.setText("Auto");
                break;
        }
    }

    private void saveWatchProgress() {
        saveWatchProgress(false);
    }

    private void saveWatchProgress(boolean completed) {
        if (player == null || mediaItems == null || preferencesManager == null) {
            return;
        }

        long currentPos = player.getCurrentPosition();
        long duration = player.getDuration();

        if (duration <= 0) {
            return;
        }

        // Save watch history
        String mediaId = mediaItems.getId() != null ? mediaItems.getId() : mediaItems.getTmdbId();
        if (mediaId != null && !mediaId.isEmpty()) {
            preferencesManager.saveWatchHistory(
                    mediaItems,
                    currentPos,
                    duration
            );


            Log.i(TAG, "Saved watch progress: " + formatTime(currentPos) + " / " + formatTime(duration));
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Show controls on Up button or Center button when hidden
        if ((keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                && !controlsVisible) {
            showControls();
            btnPlayPause.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            togglePlayPause();
            return true;
        }

        // Seek forward 10 seconds on Right D-pad
        if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (!controlsVisible) {
                if (player != null) {
                    long currentPosition = player.getCurrentPosition();
                    long duration = player.getDuration();
                    long newPosition = Math.min(currentPosition + 10000, duration); // +10 seconds (10000ms)

                    player.seekTo(newPosition);
                    updateTimeDisplay();
                    showToast("Forward +10s");

                    // Show controls briefly if hidden
                    if (!controlsVisible) {
                        showControls();
                    }
                    resetHideControlsTimer();
                }
                return true;
            }
        }

        // Seek backward 10 seconds on Left D-pad
        if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (!controlsVisible) {
                if (player != null) {
                    long currentPosition = player.getCurrentPosition();
                    long newPosition = Math.max(currentPosition - 10000, 0); // -10 seconds (10000ms)

                    player.seekTo(newPosition);
                    updateTimeDisplay();
                    showToast("Backward -10s");

                    // Show controls briefly if hidden
                    if (!controlsVisible) {
                        showControls();
                    }
                    resetHideControlsTimer();
                }
                return true;
            }
        }

        // Seek forward 10 seconds on Media Fast Forward
        if (keyCode == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (player != null) {
                long currentPosition = player.getCurrentPosition();
                long duration = player.getDuration();
                long newPosition = Math.min(currentPosition + 10000, duration);

                player.seekTo(newPosition);
                updateTimeDisplay();
                showToast("Forward +10s");

                if (!controlsVisible) {
                    showControls();
                }
                resetHideControlsTimer();
            }
            return true;
        }

        // Seek backward 10 seconds on Media Rewind\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {\n            if (player != null) {\n                long currentPosition = player.getCurrentPosition();\n                long newPosition = Math.max(currentPosition - 10000, 0);\n\n                player.seekTo(newPosition);\n                updateTimeDisplay();\n                showToast(\"Backward -10s\");\n\n                if (!controlsVisible) {\n                    showControls();\n                }\n                resetHideControlsTimer();\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PLAY key - start/resume playback\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {\n            if (player != null && !player.isPlaying()) {\n                player.play();\n                showControls();\n                showToast(\"Playing\");\n                resetHideControlsTimer();\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PAUSE key - pause playback\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {\n            if (player != null && player.isPlaying()) {\n                player.pause();\n                showControls();\n                showToast(\"Paused\");\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PLAY_PAUSE key - toggle play/pause\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {\n            togglePlayPause();\n            return true;\n        }\n\n\n        // Back button - double press to exit
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (controlsVisible) {
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
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && controlsVisible) {
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

    private void applyFocusHighlight(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            ListView listView = dialog.getListView();
            if (listView != null) {
                // Set the selector directly on the ListView
                listView.setSelector(R.drawable.dialog_item_focus_selector);
                listView.setDrawSelectorOnTop(false);
                listView.setItemsCanFocus(true);

                listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        child.setFocusable(true);
                        child.setOnFocusChangeListener((v, hasFocus) -> {
                            if (hasFocus) {
                                // Sync ListView selection with focus for D-pad
                                int position = listView.getPositionForView(v);
                                if (position != ListView.INVALID_POSITION) {
                                    listView.setSelection(position);
                                }
                            }
                        });
                        // REMOVE the manual setOnClickListener here!
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
            }
        });
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            saveWatchProgress();
        }
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !player.isPlaying()) {
            showControls();
        }
        progressUpdateHandler.post(progressUpdateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Save final watch progress
        saveWatchProgress();

        // Release player
        if (player != null) {
            player.release();
            player = null;
        }



        // Clear handlers
        controlsHandler.removeCallbacks(hideControlsRunnable);
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);

        // Clear screen flags
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void playNextEpisode() {
        if (mediaItems == null || mediaItems.getTmdbId() == null) {
            nextEpisodeTriggered = false;
            return;
        }

        String currentSeasonStr = mediaItems.getSeason();
        String currentEpisodeStr = mediaItems.getEpisode();

        if (currentSeasonStr == null || currentEpisodeStr == null) return;

        try {
            int currentSeason = Integer.parseInt(currentSeasonStr);
            int currentEpisode = Integer.parseInt(currentEpisodeStr);

            showLoadingServer();
            loadingStatusText.setText("CHECKING NEXT EPISODE...");

            TmdbRepository tmdbRepository = new TmdbRepository();
            tmdbRepository.getSeasonEpisodes(mediaItems.getTmdbId(), currentSeason, new TmdbRepository.EpisodesCallback() {
                @Override
                public void onSuccess(List<com.kiduyu.klaus.kiduyutv.model.Episode> episodes) {
                    int totalEpisodes = episodes.size();
                    if (totalEpisodes > currentEpisode) {
                        int nextEpisodeNumber = currentEpisode + 1;
                        fetchNextEpisodeStreams(mediaItems.getTmdbId(), String.valueOf(currentSeason), String.valueOf(nextEpisodeNumber));
                    } else {
                        runOnUiThread(() -> {
                            hideLoading();
                            Toast.makeText(PlayerActivity.this, "No more episodes in this season", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        nextEpisodeTriggered = false; // Reset flag to allow retry
                        hideLoading();
                        Log.e(TAG, "Error checking next episode: " + error);
                    });
                }
            });

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing season/episode: " + e.getMessage());
        }
    }


    private void fetchNextEpisodeStreams(String tmdbId, String season, String episodeNumber) {
        runOnUiThread(() -> loadingStatusText.setText("FETCHING NEXT EPISODE..."));

        FetchStreams fetchStreams = new FetchStreams(this);
        fetchStreams.fetchHexaTV(tmdbId, season, episodeNumber, new FetchStreams.StreamCallback() {
            @Override
            public void onSuccess(MediaItems nextMedia) {
                if (nextMedia.getVideoSources() != null && !nextMedia.getVideoSources().isEmpty()) {
                    // Update current media items with next episode info - matching DetailsActivityTv intent extras
                    mediaItems.setVideoSources(nextMedia.getVideoSources());
                    mediaItems.setSubtitles(nextMedia.getSubtitles());
                    mediaItems.setSeason(season);
                    mediaItems.setEpisode(episodeNumber);

                    // Set mediaType to TV (matching DetailsActivityTv intent extras)
                    mediaItems.setMediaType("tv");

                    // Set backgroundImageUrl to posterUrl (matching DetailsActivityTv line 297)
                    // This ensures the player has the correct background image for the new episode
                    if (mediaItems.getPosterUrl() != null) {
                        mediaItems.setBackgroundImageUrl(mediaItems.getPosterUrl());
                    }

                    // Instead of just updating the info, restart the activity fresh
                    // This ensures all initialization logic runs exactly like DetailsActivityTv launched it
                    runOnUiThread(() -> {
                        nextEpisodeTriggered = false; // Reset flag for the next episode

                        // Restart the activity with the updated mediaItems (fresh start)
                        Intent restartIntent = new Intent(PlayerActivity.this, PlayerActivity.class);
                        restartIntent.putExtra("media_item", mediaItems);
                        restartIntent.putExtra("media_type", "tv");
                        restartIntent.putExtra("start_position", 0); // Start from beginning for new episode

                        // Clear any existing activity stack and start fresh
                        restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(restartIntent);

                        // Finish this instance so pressing back goes to DetailsActivityTv
                        finish();

                        Toast.makeText(PlayerActivity.this, "Playing Episode " + episodeNumber, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    runOnUiThread(() -> {
                        hideLoading();
                        Toast.makeText(PlayerActivity.this, "No streams found for next episode", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    nextEpisodeTriggered = false; // Reset flag to allow retry if needed
                    hideLoading();
                    Log.e(TAG, "Error fetching next episode streams: " + error);
                    Toast.makeText(PlayerActivity.this, "Failed to load next episode", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public void onBackPressed() {
        saveWatchProgress();
        if (controlsVisible) {
            hideControls();
        }

        // Double press back to exit
        if (backPressedOnce) {
            backPressedOnce = false;
            backPressHandler.removeCallbacks(backPressResetTask);
            finish();
        }

        backPressedOnce = true;
        showToast("Press back again to exit");

        // Reset flag after 5 seconds
        backPressHandler.postDelayed(backPressResetTask, 5000);

    }

    // ============================================
    // Settings Panel Methods
    // ============================================

    /**
     * Initialize the settings panel and its components
     */
    private void initializeSettingsPanel() {
        settingsPanelContainer = findViewById(R.id.settingsPanelContainer);
        settingsOverlayBackground = findViewById(R.id.settingsOverlayBackground);
        settingsPanel = findViewById(R.id.settingsPanel);
        switchAutoQuality = findViewById(R.id.switchAutoQuality);
        switchShowSubtitles = findViewById(R.id.switchShowSubtitles);
        switchUseDoh = findViewById(R.id.switchUseDoh);
        switchProgressiveCache = findViewById(R.id.switchProgressiveCache);
        switchImageCdn = findViewById(R.id.switchImageCdn);
        textBufferSize = findViewById(R.id.textBufferSize);
        textSubtitleLanguage = findViewById(R.id.textSubtitleLanguage);
        btnCloseSettings = findViewById(R.id.btnCloseSettings);

        // Load current settings values
        loadSettingsValues();

        // Setup click listeners
        settingsOverlayBackground.setOnClickListener(v -> hideSettingsPanel());
        btnCloseSettings.setOnClickListener(v -> hideSettingsPanel());

        // Toggle listeners
        switchAutoQuality.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setAutoQualityEnabled(isChecked);
            showToast("Auto Quality " + (isChecked ? "enabled" : "disabled"));
        });

        switchShowSubtitles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && currentSubtitleIndex < 0 && subtitles != null && !subtitles.isEmpty()) {
                // If enabling subtitles but none selected, show subtitle dialog
                currentSubtitleIndex = 0;
                if (subtitles.size() > 0) {
                    btnSubtitles.setText(subtitles.get(0).getLanguage());
                    loadVideoSource(currentSourceIndex);
                }
            } else if (!isChecked) {
                // Disable subtitles
                currentSubtitleIndex = -1;
                btnSubtitles.setText("None");
                loadVideoSource(currentSourceIndex);
            }
            showToast("Subtitles " + (isChecked ? "enabled" : "disabled"));
        });

        switchUseDoh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast("DoH " + (isChecked ? "enabled" : "disabled"));
        });

        switchProgressiveCache.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast("Progressive Cache " + (isChecked ? "enabled" : "disabled"));
        });

        switchImageCdn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showToast("Image CDN " + (isChecked ? "enabled" : "disabled"));
        });

        // Setting click listeners
        findViewById(R.id.settingBufferSize).setOnClickListener(v -> showBufferSizeDialog());
        findViewById(R.id.settingSubtitleLanguage).setOnClickListener(v -> showSubtitleLanguageDialog());
    }

    /**
     * Load current settings values from PreferencesManager
     */
    private void loadSettingsValues() {
        switchAutoQuality.setChecked(preferencesManager.isAutoQualityEnabled());
        switchShowSubtitles.setChecked(currentSubtitleIndex >= 0);
        textBufferSize.setText(preferencesManager.getPlaybackBufferDuration() + " min");

        // Load subtitle language
        String subtitleLang = preferencesManager.getSubtitleLanguage();
        textSubtitleLanguage.setText(getLanguageName(subtitleLang));

        // Set default toggles (these would normally be stored in preferences)
        switchUseDoh.setChecked(true);
        switchProgressiveCache.setChecked(true);
        switchImageCdn.setChecked(true);
    }

    /**
     * Show the settings panel with slide animation
     */
    private void showSettingsPanel() {
        if (settingsPanelVisible) return;

        settingsPanelVisible = true;
        settingsPanelContainer.setVisibility(View.VISIBLE);

        // Load latest values before showing
        loadSettingsValues();

        // Animate in
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        settingsPanel.startAnimation(slideIn);

        // Hide video controls
        hideControls();
    }

    /**
     * Hide the settings panel with slide animation
     */
    private void hideSettingsPanel() {
        if (!settingsPanelVisible) return;

        // Animate out
        Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_right);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                settingsPanelContainer.setVisibility(View.GONE);
                settingsPanelVisible = false;
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        settingsPanel.startAnimation(slideOut);
    }

    /**
     * Show buffer size selection dialog
     */
    private void showBufferSizeDialog() {
        String[] bufferOptions = {"5 min", "10 min", "15 min", "20 min", "30 min"};
        int[] bufferValues = {5, 10, 15, 20, 30};
        int currentValue = preferencesManager.getPlaybackBufferDuration();
        int currentIndex = 0;
        for (int i = 0; i < bufferValues.length; i++) {
            if (bufferValues[i] == currentValue) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Buffer Duration");
        builder.setSingleChoiceItems(bufferOptions, currentIndex, (dialog, which) -> {
            preferencesManager.setPlaybackBufferDuration(bufferValues[which]);
            textBufferSize.setText(bufferOptions[which]);
            dialog.dismiss();
            showToast("Buffer duration set to " + bufferOptions[which]);

            // Restart player to apply new buffer settings
            if (player != null) {
                long currentPos = player.getCurrentPosition();
                loadVideoSource(currentSourceIndex);
                player.seekTo(currentPos);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Show subtitle language selection dialog
     */
    private void showSubtitleLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Portuguese", "Italian", "Japanese", "Korean", "Chinese", "Russian"};
        String[] langCodes = {"en", "es", "fr", "de", "pt", "it", "ja", "ko", "zh", "ru"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subtitle Language");
        builder.setItems(languages, (dialog, which) -> {
            preferencesManager.setSubtitleLanguage(langCodes[which]);
            textSubtitleLanguage.setText(languages[which]);
            dialog.dismiss();
            showToast("Subtitle language set to " + languages[which]);
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Get language name from language code
     */
    private String getLanguageName(String langCode) {
        if (langCode == null) return "English";
        switch (langCode) {
            case "en": return "English";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "pt": return "Portuguese";
            case "it": return "Italian";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "zh": return "Chinese";
            case "ru": return "Russian";
            default: return "English";
        }
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle back button when settings panel is visible
        if (settingsPanelVisible && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
                hideSettingsPanel();
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }
}