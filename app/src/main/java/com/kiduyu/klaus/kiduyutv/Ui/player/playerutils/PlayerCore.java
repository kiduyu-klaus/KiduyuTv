package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.Tracks;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.ui.SubtitleView;

import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class PlayerCore {

    private static final String TAG = "PlayerCore";

    private final PlayerActivity activity;

    public PlayerCore(PlayerActivity activity) {
        this.activity = activity;
    }

    public void initializePlayer() {
        // Get buffer duration from preferences (in minutes)
        int bufferMinutes = PreferencesManager.getInstance(activity).getPlaybackBufferDuration();
        int maxBufferMs = bufferMinutes * 60 * 1000; // Convert minutes to milliseconds

        DefaultLoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        PlayerActivity.MIN_BUFFER_MS,   // Min buffer (30s) - required before playback starts
                        maxBufferMs,                    // Max buffer (10min) - maximum ahead of current position
                        PlayerActivity.PLAYBACK_BUFFER_MS,   // Buffer for playback (10s) - after seek
                        PlayerActivity.REBUFFER_MS      // Buffer after rebuffer (10s)
                )
                .setPrioritizeTimeOverSizeThresholds(true)
                .build();
        activity.trackSelector = new DefaultTrackSelector(activity);
        activity.trackSelector.setParameters(
                activity.trackSelector.buildUponParameters()
                        .clearVideoSizeConstraints()
                        .setMaxVideoBitrate(Integer.MAX_VALUE)
                        .setForceHighestSupportedBitrate(true)
                        .setAllowVideoMixedMimeTypeAdaptiveness(true)
                        .setAllowAudioMixedMimeTypeAdaptiveness(true)
                        .setPreferredTextLanguage("en")  // Enable subtitle track
                        .setSelectUndeterminedTextLanguage(true)  // Show subtitles even if language is undefined
        );

        // Enable text rendering for subtitle support (CRITICAL FIX)
        activity.player = new ExoPlayer.Builder(activity)
                .setLoadControl(loadControl)
                .setTrackSelector(activity.trackSelector)
                .setRenderersFactory(
                        new DefaultRenderersFactory(activity)
                                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                )
                .build();


        // Configure subtitle view
        activity.subtitleView.setUserDefaultStyle();
        activity.subtitleView.setUserDefaultTextSize();
        activity.subtitleView.bringToFront();

        activity.player.setVideoSurfaceView(activity.videoSurface);        // CRITICAL FIX: Set as media overlay to ensure subtitles display on top\n        activity.videoSurface.setZOrderMediaOverlay(true);

        // Add player listener
        activity.player.addListener(new Player.Listener() {
            @Override
            public void onCues(@NonNull CueGroup cueGroup) {
                activity.subtitleView.setCues(cueGroup.cues);
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

                        activity.controlsManager.showLoadingServer();
                        break;
                    case Player.STATE_READY:
                        activity.loadingIndicator.setVisibility(android.view.View.GONE);
                        activity.controlsManager.hideLoading();
                        activity.controlsManager.updateLoadingUi(false);


                        // Immediately show seekbar, total time, and reset current time
                        long duration = activity.player.getDuration();
                        activity.progressBar.setMax((int) duration);
                        activity.totalTime.setText(formatTime(duration));
                        activity.currentTime.setText(formatTime(0)); // Reset to 00:00

                        // Make sure buffer is updated immediately
                        updateBufferProgress();

                        // Hide streaming status immediately and show controls
                        activity.controlsManager.hideStreamingStatus();

                        // Select subtitle track after media is ready
                        activity.subtitleManager.selectSubtitleTrack();

                        // Show controls so user can see seekbar and time
                        activity.controlsManager.showControls();

                        activity.dialogManager.showCurrentTrackInfo();
                        activity.dialogManager.updateQualityButton();

                        // Auto-start playback after a short delay for visual feedback
                        activity.handler.postDelayed(() -> {
                            if (!activity.player.isPlaying()) {
                                activity.controlsManager.togglePlayPause();
                            }
                        }, 500);
                        activity.handler.post(activity.updateProgressTask);
                        break;
                    case Player.STATE_ENDED:
                        activity.controlsManager.updateLoadingUi(false);
                        activity.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                        activity.controlsManager.showControls();
                        // Check for auto-play next episode
                        if ("TV".equalsIgnoreCase(activity.mediaType)) {
                            activity.genreTagsManager.playNextEpisode();
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
                activity.controlsManager.updatePlayPauseButton(isPlaying);
                if (isPlaying) {
                    activity.videoSurface.setVisibility(android.view.View.VISIBLE);
                    activity.handler.post(activity.updateProgressTask);
                    activity.controlsManager.hideControls();
                } else {
                    activity.controlsManager.showControls();
                }
            }
        });
    }

    void updateBufferProgress() {
        if (activity.player != null) {
            long bufferedPosition = activity.player.getBufferedPosition();
            long duration = activity.player.getDuration();
            long currentPosition = activity.player.getCurrentPosition();

            if (duration > 0) {
                long bufferDuration = bufferedPosition - currentPosition;

                // Use actual millisecond values since max is set to duration in ms
                activity.progressBar.setSecondaryProgress((int) bufferedPosition);

//                Log.i(TAG, "Current: " + currentPosition + "ms | Buffered: " + bufferedPosition +
//                        "ms | Max: " + duration + "ms | Buffer Duration: " + formatTime((int) bufferDuration));
            }
        }
    }


    public void loadVideoSource(int sourceIndex) {
        if (sourceIndex < 0 || sourceIndex >= activity.videoSources.size()) {
            Toast.makeText(activity, "Invalid video source", Toast.LENGTH_SHORT).show();
            return;
        }

        activity.currentSourceIndex = sourceIndex;
        MediaItems.VideoSource source = activity.videoSources.get(sourceIndex);

        Log.i(TAG, "Loading video source: " + source.getQuality() + " - " + source.getUrl());

        // Show loading
        activity.controlsManager.showLoadingServer();

        // Build data source factory with custom headers
        DataSource.Factory dataSourceFactory = buildDataSourceFactory(source);

        // Build MediaItem with subtitles
        String url = source.getUrl();
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(url);

        // Add subtitle if selected
        if (activity.currentSubtitleIndex >= 0 && activity.currentSubtitleIndex < activity.subtitles.size()) {
            MediaItems.SubtitleItem subtitle = activity.subtitles.get(activity.currentSubtitleIndex);

            // Detect MIME type from subtitle URL file extension
            String mimeType = activity.subtitleManager.detectSubtitleMimeType(subtitle.getUrl());
            // Get actual language code from subtitle metadata
            String languageCode = activity.subtitleManager.getLanguageCodeFromSubtitle(subtitle);

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

            mediaItemBuilder.setSubtitleConfigurations(Collections.singletonList(subtitleConfig));

            Log.i(TAG, "Subtitle configuration successfully added with URL: " + subtitle.getUrl());
        } else {
            Log.i(TAG, "No subtitle selected (index: " + activity.currentSubtitleIndex + ", subtitles size: " +
                    (activity.subtitles != null ? activity.subtitles.size() : "null") + ")");
        }

        MediaItem mediaItem = mediaItemBuilder.build();
        activity.controlsManager.hideLoading();
        activity.controlsManager.showStreamingStatus();

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
        activity.player.setMediaSource(videoSource);
        activity.player.prepare();

        // Seek to start position if provided
        if (activity.startPosition > 0) {
            activity.player.seekTo(activity.startPosition);
            activity.startPosition = 0; // Reset after first use
        }

        activity.player.setPlayWhenReady(true);
        activity.dialogManager.showCurrentTrackInfo();

        // Update UI
        activity.dialogManager.updateServerButton();
        activity.dialogManager.updateQualityButton();
    }

    DataSource.Factory buildDataSourceFactory(MediaItems.VideoSource source) {
        // Clear previous headers - we only want User Agent, Referer, and Origin
        activity.headers.clear();

        // Only add User Agent - the shared OkHttpClient handles cookies/sessions
        String userAgent = FetchStreams.getUserAgent();
        activity.headers.put("User-Agent", userAgent);

        // Add Referer and Origin based on URL patterns
        if (source.getUrl().startsWith("https://one.techparadise")) {
            // Videasy: Use accurate headers from EncDecEndpoints
            activity.headers.put("Referer", "https://videasy.net/");
            activity.headers.put("Origin", "https://player.videasy.net");
        } else if (source.getUrl().startsWith("https://p.10015")) {
            // Hexa: Keep existing headers
            activity.headers.put("Referer", "https://hexa.su/");
            activity.headers.put("Origin", "https://hexa.su/");
        } else if (source.getUrl().startsWith("https://storm.vodvidl.site")) {
            // Vidlink: Use accurate headers from EncDecEndpoints
            activity.headers.put("Referer", "https://vidlink.pro/");
            activity.headers.put("Origin", "https://vidlink.pro");
        } else {
            // Use the referer from the source if available
            String refererUrl = source.getRefererUrl();
            if (refererUrl != null && !refererUrl.isEmpty()) {
                activity.headers.put("Referer", refererUrl);
                // Extract origin from referer URL
                try {
                    java.net.URL url = new java.net.URL(refererUrl);
                    activity.headers.put("Origin", url.getProtocol() + "://" + url.getHost());
                } catch (Exception e) {
                    // Use default origin if parsing fails
                }
            }
        }

        // Use the shared OkHttpClient which maintains cookies/sessions from FetchStreams
        // This shares the connection pool and cookie jar with API calls
        OkHttpDataSource.Factory okHttpDataSourceFactory = new OkHttpDataSource.Factory(activity.sharedOkHttpClient);

        // Set custom headers for the data source
        if (!activity.headers.isEmpty()) {
            okHttpDataSourceFactory.setDefaultRequestProperties(activity.headers);
        }

        return new DefaultDataSource.Factory(activity, okHttpDataSourceFactory);
    }

    void handlePlayerError(PlaybackException error) {
        activity.controlsManager.hideLoading();
        activity.controlsManager.hideStreamingStatus();
        Toast.makeText(activity, "Playback error: " + error.getMessage(), Toast.LENGTH_LONG).show();

        // Try next server if available
        if (activity.currentSourceIndex < activity.videoSources.size() - 1) {

            loadVideoSource(activity.currentSourceIndex + 1);

        } else {
            activity.loadingIndicator.setVisibility(android.view.View.GONE);
            activity.loadingStatusContainer.setVisibility(android.view.View.GONE);
        }
    }

    public void saveWatchProgress() {
        saveWatchProgress(false);
    }

    void saveWatchProgress(boolean completed) {
        if (activity.player == null || activity.mediaItems == null || activity.preferencesManager == null) {
            return;
        }

        long currentPos = activity.player.getCurrentPosition();
        long duration = activity.player.getDuration();

        if (duration <= 0) {
            return;
        }

        // Save watch history
        String mediaId = activity.mediaItems.getId() != null ? activity.mediaItems.getId() : activity.mediaItems.getTmdbId();
        if (mediaId != null && !mediaId.isEmpty()) {
            activity.preferencesManager.saveWatchHistory(
                    activity.mediaItems,
                    currentPos,
                    duration
            );

            Log.i(TAG, "Saved watch progress: " + formatTime(currentPos) + " / " + formatTime(duration));
        }
    }

    public String formatTime(long milliseconds) {
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
}
