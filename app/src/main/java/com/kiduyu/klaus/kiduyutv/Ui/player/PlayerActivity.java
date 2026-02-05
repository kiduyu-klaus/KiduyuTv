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
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.ui.SubtitleView;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Api.AnimekaiApi;
import com.kiduyu.klaus.kiduyutv.model.EpisodeModel;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    // Anime server switching data
    private String episodeToken;
    private String currentServerType;
    private int currentServerIndex;
    private ArrayList<String> availableServerTypes;
    private HashMap<String, Integer> serverCounts;
    private AnimekaiApi animekaiApi;

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

        // Get media type from intent
        mediaType = getIntent().getStringExtra("media_type");
        Log.i(TAG, "Media type: " + mediaType);

        // Check if this is anime playback
        if ("anime".equals(mediaType)) {
            // Handle anime playback - extract all extras from DetailsActivityAnime
            handleAnimePlayback();
            return;
        }

        // Continue with normal playback flow for non-anime content
        // Get media item from intent
        mediaItems = getIntent().getParcelableExtra("media_item");
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

        // Initialize video sources and subtitles
        videoSources = mediaItems.getVideoSources();
        subtitles = mediaItems.getSubtitles();

        if (videoSources == null || videoSources.isEmpty()) {
            Toast.makeText(this, "No video sources available", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize player
        initializePlayer();

        // Auto-select English subtitle
        autoSelectEnglishSubtitle();

        // Start playing
        loadVideoSource(currentSourceIndex);


    }

    /**
     * Handle anime playback with extras passed from DetailsActivityAnime
     */

    private void handleAnimePlayback() {
        Log.i(TAG, "Handling anime playback");

        // Extract all intent extras from DetailsActivityAnime
        String videoUrl = getIntent().getStringExtra("video_url");
        String animeName = getIntent().getStringExtra("anime_name");
        String episodeName = getIntent().getStringExtra("episode_name");
        String animeDescription = getIntent().getStringExtra("anime_description");
        Log.i(TAG, "Episode name: " + episodeName);
        Log.i(TAG, "Anime description: " + animeDescription);
        Log.i(TAG, "Anime name: " + animeName);

        String backgroundImageUrl = getIntent().getStringExtra("background_image_url");
        Log.i(TAG, "Background image URL: " + backgroundImageUrl);
        String title = getIntent().getStringExtra("title");
        String subtitle = getIntent().getStringExtra("subtitle");

        ArrayList<String> subtitleUrls = getIntent().getStringArrayListExtra("subtitle_urls");
        ArrayList<String> subtitleLabels = getIntent().getStringArrayListExtra("subtitle_labels");

        // Server switching data
        episodeToken = getIntent().getStringExtra("episode_token");
        currentServerType = getIntent().getStringExtra("current_server_type");
        currentServerIndex = getIntent().getIntExtra("current_server_index", 0);
        availableServerTypes = getIntent().getStringArrayListExtra("available_server_types");
        serverCounts = (HashMap<String, Integer>) getIntent().getSerializableExtra("server_counts");

        // Skip info
        int introStart = getIntent().getIntExtra("intro_start", -1);
        int introEnd = getIntent().getIntExtra("intro_end", -1);
        int outroStart = getIntent().getIntExtra("outro_start", -1);
        int outroEnd = getIntent().getIntExtra("outro_end", -1);

        // Validate required data
        if (videoUrl == null || videoUrl.isEmpty()) {
            Toast.makeText(this, "Error: No video URL provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.i(TAG, "Anime video URL: " + videoUrl);
        Log.i(TAG, "Anime Name: " + animeName);
        Log.i(TAG, "Episode Name: " + episodeName);
        Log.i(TAG, "Server Type: " + currentServerType + ", Server Index: " + currentServerIndex);

        // Initialize AnimekaiApi for server switching
        if (episodeToken != null && !episodeToken.isEmpty()) {
            animekaiApi = new AnimekaiApi();
            Log.i(TAG, "AnimekaiApi initialized for server switching");
        }

        // Create a MediaItems object for anime playback
        mediaItems = new MediaItems();
        mediaItems.setTitle(animeName != null ? animeName : "Anime");
        mediaItems.setDescription(animeDescription != null ? animeDescription : "");

        // Create video source
        videoSources = new ArrayList<>();
        MediaItems.VideoSource videoSource = new MediaItems.VideoSource();
        videoSource.setUrl(videoUrl);
        videoSource.setQuality("Default");
        //videoSource.setType("anime");
        videoSources.add(videoSource);
        mediaItems.setVideoSources(videoSources);

        // Create subtitles if available
        subtitles = new ArrayList<>();
        if (subtitleUrls != null && subtitleLabels != null &&
                subtitleUrls.size() == subtitleLabels.size()) {

            Log.i(TAG, "Found " + subtitleUrls.size() + " subtitles");

            for (int i = 0; i < subtitleUrls.size(); i++) {
                MediaItems.SubtitleItem subtitleItem = new MediaItems.SubtitleItem();
                subtitleItem.setUrl(subtitleUrls.get(i));
                subtitleItem.setLanguage(subtitleLabels.get(i));
                subtitles.add(subtitleItem);
                Log.i(TAG, "Subtitle " + i + ": " + subtitleLabels.get(i) + " - " + subtitleUrls.get(i));
            }

            mediaItems.setSubtitles(subtitles);
        }

        // Initialize preferences manager
        preferencesManager = PreferencesManager.getInstance(this);

        // Initialize UI
        initializeViews();
        setupClickListeners();

        // Set anime-specific UI info
        // videoTitle should show current episode name
        if (episodeName != null && !episodeName.isEmpty()) {
            videoTitle.setText(episodeName);
            Log.i(TAG, "Set videoTitle to episode name: " + episodeName);
        } else {
            videoTitle.setText(animeName != null ? animeName : "Anime");
            Log.i(TAG, "Set videoTitle to anime name: " + animeName);
        }

        // videoDescription should show anime description
        if (animeDescription != null && !animeDescription.isEmpty()) {
            videoDescription.setText(animeDescription);
            Log.i(TAG, "Set videoDescription to: " + animeDescription);
        } else {
            videoDescription.setText("");
        }

        // episodeInfo shows the formatted subtitle (S1E1 - Episode Name)
        //episodeInfo.setVisibility(View.VISIBLE);
        //episodeInfo.setText(subtitle != null ? subtitle : "");

        // Load background image
        if (backgroundImageUrl != null && !backgroundImageUrl.isEmpty()) {
            Log.i(TAG, "Loading background image: " + backgroundImageUrl);
            Glide.with(this)
                    .load(backgroundImageUrl)
                    .centerCrop()
                    .into(backgroundImage);
        } else {
            Log.w(TAG, "No background image URL provided");
        }

        // Initialize player
        initializePlayer();

        // Auto-select first subtitle if available (usually English) - BEFORE loading video
        if (subtitles != null && !subtitles.isEmpty()) {
            currentSubtitleIndex = 0; // Select first subtitle by default
            btnSubtitles.setText(subtitles.get(0).getLanguage());
            Log.i(TAG, "Auto-selected subtitle: " + subtitles.get(0).getLanguage());
        }

        // Update button labels for anime
        updateAnimeServerButtons();

        // Start playing - now subtitle will be included
        currentSourceIndex = 0;
        loadVideoSource(currentSourceIndex);

        // Handle skip info (intro/outro) if provided
        if (introStart >= 0 && introEnd >= 0) {
            Log.i(TAG, "Intro skip: " + introStart + " - " + introEnd);
            // TODO: Implement skip intro button/functionality
        }

        if (outroStart >= 0 && outroEnd >= 0) {
            Log.i(TAG, "Outro skip: " + outroStart + " - " + outroEnd);
            // TODO: Implement skip outro button/functionality
        }
    }

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
        loadingStatusContainer = findViewById(R.id.loadingStatusContainer);
        loadingStatusText = findViewById(R.id.loadingStatusText);
        hardsubBadge = findViewById(R.id.hardsubBadge);
        subtitleView = findViewById(R.id.subtitleView);
        totalTime.setText(formatTime(0));
        currentTime.setText(formatTime(0));
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
            if ("anime".equals(mediaType) && episodeToken != null) {
                showAnimeServerDialog();
            } else {
                showServerDialog();
            }
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
            if ("anime".equals(mediaType) && availableServerTypes != null) {
                showAnimeServerTypeDialog();
            } else {
                Toast.makeText(this, "Audio track selection coming soon", Toast.LENGTH_SHORT).show();
            }
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
                    .into(backgroundImage);
        }

        // Update server button with current source
        updateServerButton();

        // Update speed button
        btnSpeed.setText(String.format(Locale.US, "Speed %.2fx", currentSpeed));

        // Hide hardsub badge initially (show if needed based on source)
        hardsubBadge.setVisibility(View.GONE);
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

        player.setVideoSurfaceView(videoSurface);

        // Add player listener
        player.addListener(new Player.Listener() {
            @Override
            public void onCues(@NonNull CueGroup cueGroup) {
                subtitleView.setCues(cueGroup.cues);
                Log.i(TAG, "Subtitle cues updated");
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

                Log.i(TAG, "Current: " + currentPosition + "ms | Buffered: " + bufferedPosition +
                        "ms | Max: " + duration + "ms | Buffer Duration: " + formatTime((int) bufferDuration));
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

            Log.i(TAG, "Adding subtitle to MediaItem: " + subtitle.getLanguage() + " - " + subtitle.getUrl());

            // Build subtitle configuration with proper headers
            MediaItem.SubtitleConfiguration.Builder subtitleBuilder =
                    new MediaItem.SubtitleConfiguration.Builder(android.net.Uri.parse(subtitle.getUrl()))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage("en") // Use generic "en" for English
                            .setLabel(subtitle.getLanguage())
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT | C.SELECTION_FLAG_AUTOSELECT)
                            .setRoleFlags(C.ROLE_FLAG_SUBTITLE);

            // Note: In Media3, subtitle tracks use the same DataSource.Factory as the video,
            // so custom headers are automatically applied through buildDataSourceFactory()

            MediaItem.SubtitleConfiguration subtitleConfig = subtitleBuilder.build();

            mediaItemBuilder.setSubtitleConfigurations(java.util.Collections.singletonList(subtitleConfig));
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

        // Update UI
        updateServerButton();
        updateQualityButton();
    }

    private DataSource.Factory buildDataSourceFactory(MediaItems.VideoSource source) {
        // Build custom headers from the video source
        Map<String, String> headers = new HashMap<>();

        headers.put("User-Agent", DEFAULT_USER_AGENT);
        headers.put("Accept", "*/*");
        headers.put("Accept-Encoding", "gzip, deflate");
        headers.put("Accept-Language", "en-US,en;q=0.9");
        headers.put("Connection", "keep-alive");
        if (source.getUrl().startsWith("https://one.techparadise")) {
            headers.put("Referer", "https://videasy.net/");
            headers.put("Origin", "https://videasy.net/");
        }
        if (source.getUrl().startsWith("https://p.10015")) {
            headers.put("Referer", "https://hexa.su/");
            headers.put("Origin", "https://hexa.su/");
        }

        // Add referer if available
//        if (source.getRefererUrl() != null && !source.getRefererUrl().isEmpty()) {
//            headers.put("Referer", source.getRefererUrl());
//        }

        // Build HTTP data source with headers
        DefaultHttpDataSource.Factory httpDataSourceFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS)
                .setReadTimeoutMs(DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS);

        if (!headers.isEmpty()) {
            httpDataSourceFactory.setDefaultRequestProperties(headers);
        }

        return new DefaultDataSource.Factory(this, httpDataSourceFactory);
    }

    private void autoSelectEnglishSubtitle() {
        if (subtitles == null || subtitles.isEmpty()) {
            btnSubtitles.setText("No Subtitles");
            return;
        }

        // Try to find English subtitle
        for (int i = 0; i < subtitles.size(); i++) {
            MediaItems.SubtitleItem subtitle = subtitles.get(i);
            String lang = subtitle.getLang() != null ? subtitle.getLang().toLowerCase() : "";
            String language = subtitle.getLanguage() != null ? subtitle.getLanguage().toLowerCase() : "";

            if (lang.contains("en") || lang.equals("eng") ||
                    language.contains("english") || language.contains("en")) {
                currentSubtitleIndex = i;
                btnSubtitles.setText(subtitle.getLanguage());
                Log.i(TAG, "Auto-selected English subtitle: " + subtitle.getLanguage());
                return;
            }
        }

        // If no English subtitle found, select first subtitle
        if (!subtitles.isEmpty()) {
            currentSubtitleIndex = 0;
            btnSubtitles.setText(subtitles.get(0).getLanguage());
        } else {
            btnSubtitles.setText("No Subtitles");
        }
    }

    private void selectSubtitleTrack() {
        if (player == null) {
            Log.w(TAG, "Cannot select subtitle track: player is null");
            return;
        }

        if (currentSubtitleIndex < 0) {
            Log.i(TAG, "No subtitle selected (index -1), disabling text tracks");
            // Disable text tracks
            TrackSelectionParameters disableParams = player.getTrackSelectionParameters()
                    .buildUpon()
                    .setPreferredTextLanguage(null)
                    .setIgnoredTextSelectionFlags(~C.SELECTION_FLAG_DEFAULT)
                    .build();
            player.setTrackSelectionParameters(disableParams);
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

        Log.i(TAG, "Selecting subtitle track: " + subtitle.getLanguage());

        // Build track selection parameters to enable text tracks
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters()
                .buildUpon()
                .setPreferredTextLanguage("en")
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
            hideControlsHandler.postDelayed(hideControlsTask, 3000);
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
            serverLabels[i] = "Server " + (i + 1);
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
        if (videoSources == null || videoSources.isEmpty()) {
            Toast.makeText(this, "No qualities available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] qualityLabels = new String[videoSources.size()];
        for (int i = 0; i < videoSources.size(); i++) {
            qualityLabels[i] = videoSources.get(i).getQuality();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quality");
        builder.setItems(qualityLabels, (dialog, which) -> {
            currentSourceIndex = which;
            loadVideoSource(currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    private void showSubtitleDialog() {
        if (subtitles == null || subtitles.isEmpty()) {
            Toast.makeText(this, "No subtitles available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] subtitleLabels = new String[subtitles.size()];
        for (int i = 0; i < subtitles.size(); i++) {
            subtitleLabels[i] = subtitles.get(i).getLanguage();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle");
        builder.setItems(subtitleLabels, (dialog, which) -> {
            currentSubtitleIndex = which;
            btnSubtitles.setText(subtitles.get(which).getLanguage());
            loadVideoSource(currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog); // ✅ apply focus listeners
        dialog.show();
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
                btnServer.setText("Server " + (currentServerIndex + 1));
            } else {
                btnServer.setText("Server 1");
            }

            Log.i(TAG, "Updated buttons: Type=" + typeLabel + ", Server=" + (currentServerIndex + 1));
        }
    }

    /**
     * Show dialog to switch between different server types (SUB/DUB/SOFTSUB)
     */
    private void showAnimeServerTypeDialog() {
        if (availableServerTypes == null || availableServerTypes.isEmpty()) {
            Toast.makeText(this, "No server types available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] typeLabels = availableServerTypes.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Server Type");
        builder.setItems(typeLabels, (dialog, which) -> {
            currentServerType = availableServerTypes.get(which);
            btnAudio.setText(currentServerType);
            // TODO: reload video with new server type
            switchToServerType(currentServerType);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Show dialog to switch between different servers of the same type
     */
    private void showAnimeServerDialog() {
        if (videoSources == null || videoSources.isEmpty()) {
            Toast.makeText(this, "No anime servers available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] serverLabels = new String[videoSources.size()];
        for (int i = 0; i < videoSources.size(); i++) {
            serverLabels[i] = "Anime Server " + (i + 1);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Anime Server");
        builder.setItems(serverLabels, (dialog, which) -> {
            currentSourceIndex = which;
            loadVideoSource(currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Switch to a different server type (SUB/DUB/SOFTSUB)
     */
    private void switchToServerType(String newServerType) {
        Log.i(TAG, "Switching from " + currentServerType + " to " + newServerType);

        loadingIndicator.setVisibility(View.VISIBLE);
        loadingStatusContainer.setVisibility(View.VISIBLE);
        loadingStatusText.setText("Switching to " + newServerType.toUpperCase() + "...");

        // Switch to first server of the new type
        switchToServer(newServerType, 0);
    }

    /**
     * Switch to a specific server
     */
    private void switchToServer(String serverType, int serverIndex) {
        if (animekaiApi == null || episodeToken == null) {
            Toast.makeText(this, "Cannot switch servers", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "Switching to " + serverType + " Server " + (serverIndex + 1));

        long currentPosition = player != null ? player.getCurrentPosition() : 0;
        boolean wasPlaying = player != null && player.isPlaying();

        loadingIndicator.setVisibility(View.VISIBLE);
        loadingStatusContainer.setVisibility(View.VISIBLE);
        loadingStatusText.setText("Loading " + serverType.toUpperCase() +
                " Server " + (serverIndex + 1) + "...");

        new Thread(() -> {
            try {
                // Fetch servers again
                Map<String, Map<String, AnimekaiApi.ServerInfo>> servers =
                        animekaiApi.fetchEpisodeServers(episodeToken);

                if (!servers.containsKey(serverType)) {
                    runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);
                        loadingStatusContainer.setVisibility(View.GONE);
                        Toast.makeText(this, serverType.toUpperCase() +
                                " not available", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                Map<String, AnimekaiApi.ServerInfo> typeServers = servers.get(serverType);
                AnimekaiApi.ServerInfo targetServer = null;

                // Find server by index
                for (AnimekaiApi.ServerInfo server : typeServers.values()) {
                    if (server.index == serverIndex+1) {
                        targetServer = server;
                        break;
                    }
                }

                if (targetServer == null) {
                    runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);
                        loadingStatusContainer.setVisibility(View.GONE);
                        Toast.makeText(this, "Server not found", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                // Resolve new video URL
                AnimekaiApi.MediaData mediaData = animekaiApi.resolveEmbedUrl(targetServer.linkId);

                List<String> sources = mediaData.getSources();
                if (sources == null || sources.isEmpty()) {
                    runOnUiThread(() -> {
                        loadingIndicator.setVisibility(View.GONE);
                        loadingStatusContainer.setVisibility(View.GONE);
                        Toast.makeText(this, "No video sources found", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }

                String newVideoUrl = sources.get(0);

                // Update subtitles if available
                final ArrayList<MediaItems.SubtitleItem> newSubtitles = new ArrayList<>();
                if (mediaData.getTracks() != null && !mediaData.getTracks().isEmpty()) {
                    for (EpisodeModel.Track track : mediaData.getTracks()) {
                        MediaItems.SubtitleItem subtitleItem = new MediaItems.SubtitleItem();
                        subtitleItem.setUrl(track.getFile());
                        subtitleItem.setLanguage(track.getLabel());
                        newSubtitles.add(subtitleItem);
                    }
                }

                runOnUiThread(() -> {
                    // Update current server info
                    currentServerType = serverType;
                    currentServerIndex = serverIndex;

                    // Update video source
                    if (!videoSources.isEmpty()) {
                        videoSources.get(0).setUrl(newVideoUrl);
                    }

                    // Update subtitles
                    if (!newSubtitles.isEmpty()) {
                        subtitles = newSubtitles;
                        mediaItems.setSubtitles(subtitles);
                        currentSubtitleIndex = 0; // Auto-select first subtitle
                    }

                    // Update button labels
                    updateAnimeServerButtons();

                    // Reload video
                    loadVideoSource(0);

                    // Restore position and playback state
                    if (player != null) {
                        player.seekTo(currentPosition);
                        if (wasPlaying) {
                            player.play();
                        }
                    }

                    loadingIndicator.setVisibility(View.GONE);
                    loadingStatusContainer.setVisibility(View.GONE);

                    Toast.makeText(this, "Switched to " + serverType.toUpperCase() +
                            " Server " + (serverIndex + 1), Toast.LENGTH_SHORT).show();

                    Log.i(TAG, "Successfully switched to " + serverType +
                            " Server " + (serverIndex + 1));
                });

            } catch (Exception e) {
                Log.e(TAG, "Error switching server", e);
                runOnUiThread(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    loadingStatusContainer.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to switch server: " +
                            e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateServerButton() {
        btnServer.setText("Server " + (currentSourceIndex + 1));
    }

    private void updateQualityButton() {
        if (currentSourceIndex >= 0 && currentSourceIndex < videoSources.size()) {
            String quality = videoSources.get(currentSourceIndex).getQuality();
            btnQuality.setText(quality != null ? quality : "Auto");
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
                listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        child.setOnFocusChangeListener((v, hasFocus) -> {
                            if (hasFocus) {
                                v.setBackgroundColor(0xFF0080F);   // focused item → blue
                            } else {
                                v.setBackgroundColor(Color.TRANSPARENT); // reset
                            }
                        });
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                        // no-op
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

        // Shutdown AnimekaiApi if used for server switching
        if (animekaiApi != null) {
            animekaiApi.shutdown();
            animekaiApi = null;
        }

        // Clear handlers
        controlsHandler.removeCallbacks(hideControlsRunnable);
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);

        // Clear screen flags
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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