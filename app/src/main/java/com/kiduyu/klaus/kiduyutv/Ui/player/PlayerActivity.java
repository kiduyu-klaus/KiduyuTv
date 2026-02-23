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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
            Toast.makeText(this, "Settings clicked", Toast.LENGTH_SHORT).show();
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
                        if ("TV".equals(mediaType)) {
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

            // Build subtitle configuration with detected/actual values
            MediaItem.SubtitleConfiguration.Builder subtitleBuilder =
                    new MediaItem.SubtitleConfiguration.Builder(
                            android.net.Uri.parse(subtitle.getUrl())
                    )
                            .setMimeType(mimeType)
                            // Hardcoded to English
                            .setLanguage("en")
                            .setLabel("English")
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
        if (source.getUrl().startsWith("https://one.techparadise") || source.getUrl().startsWith("https://one.trueparadise")) {
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

        Tracks tracks = player.getCurrentTracks();
        if (tracks == null || tracks.isEmpty()) {
            showToast("No tracks");
            return;
        }

        // List to store quality options
        List<String> qualityList = new ArrayList<>();
        List<TrackSelectionOverride> overrideList = new ArrayList<>();

        // Add "Auto" option
        //overrideList.add(null); // null indicates auto selection

        // Iterate through track groups to find video tracks
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            int trackType = trackGroup.getType();

            if (trackType == C.TRACK_TYPE_VIDEO) {
                TrackGroup mediaTrackGroup = trackGroup.getMediaTrackGroup();

                for (int i = 0; i < trackGroup.length; i++) {
                    Format format = trackGroup.getTrackFormat(i);
                    int width = format.width;
                    int height = format.height;
                    int bitrate = format.bitrate;

                    StringBuilder qualityLabel = new StringBuilder();

                    // Build quality label
                    if (width > 0 && height > 0) {
                        qualityLabel.append(height).append("p");

                        if (bitrate > 0) {
                            // Convert bitrate to Mbps
                            float bitrateMbps = bitrate / 1_000_000f;
                            qualityLabel.append(" (").append(String.format("%.2f", bitrateMbps)).append(" Mbps)");
                        }
                    } else {
                        qualityLabel.append("Unknown Quality");
                    }

                    qualityList.add(qualityLabel.toString());

                    // Create override for this specific track
                    TrackSelectionOverride override = new TrackSelectionOverride(
                            mediaTrackGroup,
                            Collections.singletonList(i)
                    );
                    overrideList.add(override);
                }
            }
        }

        if (qualityList.size() <= 1) {
            showToast("No quality options available");
            return;
        }

        // Convert list to array for AlertDialog
        String[] qualityLabels = qualityList.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quality");
        builder.setItems(qualityLabels, (dialog, which) -> {
            TrackSelectionOverride selectedOverride = overrideList.get(which);
            switchToTrack(selectedOverride);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
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
        if (player == null) {
            btnQuality.setText("Auto");
            return;
        }

        Tracks tracks = player.getCurrentTracks();
        if (tracks == null || tracks.isEmpty()) {
            btnQuality.setText("Auto");
            return;
        }

        // Find currently selected video track
        for (Tracks.Group trackGroup : tracks.getGroups()) {
            if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                for (int i = 0; i < trackGroup.length; i++) {
                    if (trackGroup.isTrackSelected(i)) {
                        Format format = trackGroup.getTrackFormat(i);
                        int height = format.height;

                        if (height > 0) {
                            btnQuality.setText(height + "p");
                        } else {
                            btnQuality.setText("Auto");
                        }
                        return;
                    }
                }
            }
        }

        // No track selected or found
        btnQuality.setText("Auto");
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

        // Seek backward 10 seconds on Media Rewind
        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {
            if (player != null) {
                long currentPosition = player.getCurrentPosition();
                long newPosition = Math.max(currentPosition - 10000, 0);

                player.seekTo(newPosition);
                updateTimeDisplay();
                showToast("Backward -10s");

                if (!controlsVisible) {
                    showControls();
                }
                resetHideControlsTimer();
            }
            return true;
        }


        // Back button - double press to exit
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
                    // Update current media items with next episode info
                    mediaItems.setVideoSources(nextMedia.getVideoSources());
                    mediaItems.setSubtitles(nextMedia.getSubtitles());
                    mediaItems.setSeason(season);
                    mediaItems.setEpisode(episodeNumber);

                    // Update UI info
                    runOnUiThread(() -> {
                        nextEpisodeTriggered = false; // Reset flag for the next episode
                        populateMediaInfo();
                        currentSourceIndex = 0;
                        loadVideoSource(currentSourceIndex);
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
}
