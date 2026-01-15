package com.kiduyu.klaus.kiduyutv.Ui.player;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
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



import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.ImageViewImageLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlayerActivity extends Activity {
    private static final String TAG = "PlayerActivity";

    private SurfaceView videoSurface;
    private ExoPlayer exoPlayer;

    private LinearLayout topInfoContainer, bottomControlsContainer, loadingStatusContainer;
    private ImageButton btnPlayPause, btnBack;
    private Button btnSettings, btnSpeed, btnServer, btnQuality, btnAudio, btnSubtitles;
    private SeekBar progressBar;
    private TextView videoTitle, currentTime, totalTime, episodeInfo, btnEpisodes, loadingStatusText;
    private ProgressBar loadingIndicator;
    private ImageView centerPauseIcon;

    private Handler handler = new Handler();
    private Handler hideControlsHandler = new Handler();
    private boolean isPlaying = false;
    private boolean areControlsVisible = false;

    // Data lists
    private List<String> subtitleOptions = Arrays.asList("Off", "English", "Spanish", "French", "German", "Original");
    private List<String> qualityOptions = Arrays.asList("Auto", "1080p", "720p HD", "Medium 720p", "480p", "360p");
    private List<String> serverOptions = Arrays.asList("Server 1", "Server 2", "Server 3", "Server 4");
    private List<Float> speedOptions = Arrays.asList(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f);
    private List<String> audioOptions = Arrays.asList("Sub", "Dub");

    //SurfaceViewImageLoader loader = new SurfaceViewImageLoader();

    private int currentSubtitle = 5; // Original
    private int currentQuality = 3; // Medium 720p
    private int currentServer = 0;


    private ImageViewImageLoader imageLoader;

    private MediaItems sourceMediaItem;
    private int currentSpeed = 2; // 1.0x
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

        imageLoader = new ImageViewImageLoader();


        initializeViews();
        loadBackgroundImage();
        setupFocusListeners();
        setupClickListeners();
        setupExoPlayer();

        // Start with controls hidden
        //hideControls();
        showControls();

        //loader.loadImageToSurfaceView(videoSurface, sourceMediaItem.getBackgroundImageUrl());

    }
    private void loadBackgroundImage() {
        // Load background image using ImageView (doesn't interfere with ExoPlayer)
        try {
            backgroundImage.setVisibility(View.VISIBLE);
            imageLoader.loadImageToImageView(backgroundImage, sourceMediaItem.getBackgroundImageUrl());
        } catch (Exception e) {
            Log.e(TAG, "Error loading background image", e);
        }
    }

    private void initializeViews() {
        videoSurface = findViewById(R.id.videoSurface);
        topInfoContainer = findViewById(R.id.topInfoContainer);
        bottomControlsContainer = findViewById(R.id.bottomControlsContainer);
        loadingStatusContainer = findViewById(R.id.loadingStatusContainer);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        backgroundImage = findViewById(R.id.backgroundImage);
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

        // Set initial button texts
        updateButtonTexts();
    }

    private void updateButtonTexts() {
        btnSpeed.setText("Speed " + String.format(Locale.getDefault(), "%.2fx", speedOptions.get(currentSpeed)));
        btnServer.setText(serverOptions.get(currentServer));
        btnQuality.setText(qualityOptions.get(currentQuality));
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

    private void setupExoPlayer() {
        exoPlayer = new ExoPlayer.Builder(this).build();
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
        });

        loadVideo();
    }

    private void loadVideo() {
        showLoadingServer();

        // Create media item with video
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(Uri.parse(sourceMediaItem.getVideoSources().get(0).getUrl()));

        // Add subtitles if enabled
        if (currentSubtitle > 0) { // Not "Off"
            MediaItem.SubtitleConfiguration subtitleConfig = new MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(VIDEO_URL_SUBTITLES))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();
            mediaItemBuilder.setSubtitleConfigurations(Arrays.asList(subtitleConfig));
        }

        MediaItem mediaItem = mediaItemBuilder.build();
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Quality");
        builder.setSingleChoiceItems(qualityOptions.toArray(new String[0]),
                currentQuality, (dialog, which) -> {
                    currentQuality = which;
                    updateButtonTexts();
                    showToast("Quality: " + qualityOptions.get(which));
                    dialog.dismiss();

                    // Show loading and reload video
                    loadVideo();
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
}