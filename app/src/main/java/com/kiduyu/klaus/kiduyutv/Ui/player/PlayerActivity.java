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
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerControlsManager;
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerCore;
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerDialogManager;
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerGenreTagsManager;
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerSettingsManager;
import com.kiduyu.klaus.kiduyutv.Ui.player.playerutils.PlayerSubtitleManager;
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
    public static final String TAG = "PlayerActivity";
    public static final int CONTROLS_HIDE_DELAY = 5000; // 5 seconds
    public static final int SEEK_INCREMENT = 10000; // 10 seconds in milliseconds
    public static final int MIN_BUFFER_MS = 30000;     // 30 seconds
    public static final int PLAYBACK_BUFFER_MS = 10000; // 10 seconds
    public static final int REBUFFER_MS = 10000;

    // ExoPlayer
    public ExoPlayer player;
    public DefaultTrackSelector trackSelector;

    // UI Components
    public SurfaceView videoSurface;
    public SubtitleView subtitleView;
    public ImageView backgroundImage;
    public ProgressBar loadingIndicator;
    public ImageView centerPauseIcon;
    public LinearLayout topInfoContainer;
    public LinearLayout bottomControlsContainer;
    public ImageButton btnBack;
    public ImageButton btnPlayPause;
    public SeekBar progressBar;
    public TextView currentTime;
    public TextView totalTime;
    public TextView videoTitle;
    public TextView videoDescription;
    public TextView episodeInfo;
    public Button btnSettings;
    public Button btnSpeed;
    public Button btnServer;
    public Button btnQuality;
    public Button btnAudio;
    public Button btnSubtitles;
    public LinearLayout loadingStatusContainer;
    public TextView loadingStatusText;
    public TextView hardsubBadge;
    public TextView tag1;
    public TextView tag2;
    public TextView tag3;
    public TextView tag4;
    public TextView tag5;

    // Settings Panel Components
    public RelativeLayout settingsPanelContainer;
    public View settingsOverlayBackground;
    public LinearLayout settingsPanel;
    public SwitchCompat switchAutoQuality;
    public SwitchCompat switchShowSubtitles;
    public SwitchCompat switchUseDoh;
    public SwitchCompat switchProgressiveCache;
    public SwitchCompat switchImageCdn;
    public TextView textBufferSize;
    public TextView textSubtitleLanguage;
    public Button btnCloseSettings;
    public boolean settingsPanelVisible = false;

    public Handler handler = new Handler();
    public Handler hideControlsHandler = new Handler();
    public Handler backPressHandler = new Handler();

    // Data
    public MediaItems mediaItems;
    public List<MediaItems.VideoSource> videoSources;
    public List<MediaItems.SubtitleItem> subtitles;
    public int currentSourceIndex = 0;
    public int currentSubtitleIndex = -1; // -1 means no subtitle selected
    public float currentSpeed = 1.0f;
    public String mediaType; // "anime", "movie", "tv", etc.
    public boolean nextEpisodeTriggered = false;

    // Quality selection tracking
    public String currentQuality = "Auto"; // Auto, High, Medium, Low
    public int currentQualityHeight = 0; // 0 = Auto, otherwise actual height in pixels

    // Shared OkHttpClient for streaming - maintains cookies/sessions from API calls
    public OkHttpClient sharedOkHttpClient;

    // Headers map - now only contains User Agent, Referer, and Origin
    public Map<String, String> headers = new HashMap<>();

    // Anime server switching data
    public String episodeToken;
    public String currentServerType;
    public int currentServerIndex;
    public ArrayList<String> availableServerTypes;
    public HashMap<String, Integer> serverCounts;

    // Control visibility
    public Handler controlsHandler = new Handler(Looper.getMainLooper());
    public Runnable hideControlsRunnable = this::hideControls;
    public boolean controlsVisible = true;
    public boolean backPressedOnce = false;

    // Watch history
    public PreferencesManager preferencesManager;
    public long startPosition = 0;
    public Handler progressUpdateHandler = new Handler(Looper.getMainLooper());
    public Runnable progressUpdateRunnable;

    // Helper managers
    public PlayerCore playerCore;
    public PlayerControlsManager controlsManager;
    public PlayerSubtitleManager subtitleManager;
    public PlayerDialogManager dialogManager;
    public PlayerGenreTagsManager genreTagsManager;
    public PlayerSettingsManager settingsManager;

    // Runnables kept here so managers can reference them
    public Runnable hideControlsTask = () -> {
        if (player.isPlaying() && controlsVisible) {
            hideControls();
        }
    };

    public Runnable updateProgressTask = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                controlsManager.updateTimeDisplay();
                handler.postDelayed(this, 1000);
            }
        }
    };

    Runnable backPressResetTask = () -> {
        backPressedOnce = false;
    };

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

        Log.i(TAG, "Subtitles: " + mediaItems.getSubtitles().toString());


        // Initialize preferences manager
        preferencesManager = PreferencesManager.getInstance(this);

        // Initialize helper managers
        playerCore = new PlayerCore(this);
        controlsManager = new PlayerControlsManager(this);
        subtitleManager = new PlayerSubtitleManager(this);
        dialogManager = new PlayerDialogManager(this);
        genreTagsManager = new PlayerGenreTagsManager(this);
        settingsManager = new PlayerSettingsManager(this);

        // Initialize UI
        initializeViews();
        setupClickListeners();
        populateMediaInfo();
        subtitleManager.fetchExternalSubtitles();

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
        playerCore.initializePlayer();

        // Start playing
        playerCore.loadVideoSource(currentSourceIndex);

        // Populate genre tags for media content
        genreTagsManager.populateGenreTags();
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
        settingsManager.initializeSettingsPanel();

        totalTime.setText(playerCore.formatTime(0));
        currentTime.setText(playerCore.formatTime(0));
        if (mediaType.equals("TV") || mediaType.equals("MOVIE")) {
            btnAudio.setVisibility(View.GONE);
        }
    }

    private void setupClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> {
            playerCore.saveWatchProgress();
            finish();
        });

        // Play/Pause button
        btnPlayPause.setOnClickListener(v -> controlsManager.togglePlayPause());

        // SeekBar
        progressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && player != null) {
                    player.seekTo(progress);
                    controlsManager.updateTimeDisplay();
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
        btnSpeed.setOnClickListener(v -> dialogManager.showSpeedDialog());

        // Server/Source button
        btnServer.setOnClickListener(v -> {

            dialogManager.showServerDialog();

        });

        // Quality button
        btnQuality.setOnClickListener(v -> dialogManager.showQualityDialog());

        // Subtitle button
        btnSubtitles.setOnClickListener(v -> subtitleManager.showSubtitleDialog());

        // Settings button
        btnSettings.setOnClickListener(v -> {
            settingsManager.showSettingsPanel();
        });

        // Audio button - used for server type selection in anime
        btnAudio.setOnClickListener(v -> {

            Toast.makeText(this, "Audio track selection coming soon", Toast.LENGTH_SHORT).show();

        });

        // Touch on video surface to toggle controls
        videoSurface.setOnClickListener(v -> controlsManager.toggleControls());

        // Background to toggle controls
        backgroundImage.setOnClickListener(v -> controlsManager.toggleControls());
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
        genreTagsManager.updateHardsubBadgeForMediaType();
    }

    // Kept as package-private shim so runnables can call it via lambda without a manager ref
    void hideControls() {
        controlsManager.hideControls();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Show controls on Up button or Center button when hidden
        if ((keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                && !controlsVisible) {
            controlsManager.showControls();
            btnPlayPause.requestFocus();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
            controlsManager.togglePlayPause();
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
                    controlsManager.updateTimeDisplay();
                    controlsManager.showToast("Forward +10s");

                    // Show controls briefly if hidden
                    if (!controlsVisible) {
                        controlsManager.showControls();
                    }
                    controlsManager.resetHideControlsTimer();
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
                    controlsManager.updateTimeDisplay();
                    controlsManager.showToast("Backward -10s");

                    // Show controls briefly if hidden
                    if (!controlsVisible) {
                        controlsManager.showControls();
                    }
                    controlsManager.resetHideControlsTimer();
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
                controlsManager.updateTimeDisplay();
                controlsManager.showToast("Forward +10s");

                if (!controlsVisible) {
                    controlsManager.showControls();
                }
                controlsManager.resetHideControlsTimer();
            }
            return true;
        }

        // Seek backward 10 seconds on Media Rewind\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_REWIND) {\n            if (player != null) {\n                long currentPosition = player.getCurrentPosition();\n                long newPosition = Math.max(currentPosition - 10000, 0);\n\n                player.seekTo(newPosition);\n                updateTimeDisplay();\n                showToast(\"Backward -10s\");\n\n                if (!controlsVisible) {\n                    showControls();\n                }\n                resetHideControlsTimer();\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PLAY key - start/resume playback\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {\n            if (player != null && !player.isPlaying()) {\n                player.play();\n                showControls();\n                showToast(\"Playing\");\n                resetHideControlsTimer();\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PAUSE key - pause playback\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {\n            if (player != null && player.isPlaying()) {\n                player.pause();\n                showControls();\n                showToast(\"Paused\");\n            }\n            return true;\n        }\n\n        // Handle MEDIA_PLAY_PAUSE key - toggle play/pause\n        if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {\n            togglePlayPause();\n            return true;\n        }\n\n\n        // Back button - double press to exit
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (controlsVisible) {
                controlsManager.hideControls();
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
            controlsManager.showToast("Press back again to exit");

            // Reset flag after 5 seconds
            backPressHandler.postDelayed(backPressResetTask, 5000);
            return true;
        }

        // Play/pause on center when controls visible and no button focused
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && controlsVisible) {
            View focusedView = getCurrentFocus();
            if (focusedView == null) {
                controlsManager.togglePlayPause();
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
            playerCore.saveWatchProgress();
        }
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !player.isPlaying()) {
            controlsManager.showControls();
        }
        progressUpdateHandler.post(progressUpdateRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Save final watch progress
        playerCore.saveWatchProgress();

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

    @Override
    public void onBackPressed() {
        playerCore.saveWatchProgress();
        if (controlsVisible) {
            controlsManager.hideControls();
        }

        // Double press back to exit
        if (backPressedOnce) {
            backPressedOnce = false;
            backPressHandler.removeCallbacks(backPressResetTask);
            finish();
        }

        backPressedOnce = true;
        controlsManager.showToast("Press back again to exit");

        // Reset flag after 5 seconds
        backPressHandler.postDelayed(backPressResetTask, 5000);

    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Handle back button when settings panel is visible
        if (settingsPanelVisible && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (event.getKeyCode() == KeyEvent.KEYCODE_BACK || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE) {
                settingsManager.hideSettingsPanel();
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }
}