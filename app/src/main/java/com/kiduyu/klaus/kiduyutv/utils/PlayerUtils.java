package com.kiduyu.klaus.kiduyutv.utils;

import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.PlaybackParameters;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * PlayerUtils - Utility class for video player operations
 * 
 * Contains static helper methods extracted from PlayerActivity for:
 * - Time formatting
 * - Error handling
 * - Media item building
 * - Options builders
 * - Configuration helpers
 * 
 * Usage:
 * <pre>
 * // Format time from milliseconds
 * String formattedTime = PlayerUtils.formatTime(125000); // "02:05"
 * 
 * // Check if error is network-related
 * boolean isNetwork = PlayerUtils.isNetworkError(exception);
 * 
 * // Build media item with subtitles
 * MediaItem item = PlayerUtils.buildMediaItem(uri, title, null, null, null, subtitles, null);
 * 
 * // Get speed options
 * List&lt;Float&gt; speeds = PlayerUtils.getDefaultSpeedOptions();
 * </pre>
 */
public final class PlayerUtils {

    private static final String TAG = "PlayerUtils";

    // Default speed options for playback
    public static final List<Float> DEFAULT_SPEED_OPTIONS = Arrays.asList(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f);

    // Default audio options
    public static final List<String> DEFAULT_AUDIO_OPTIONS = Arrays.asList("Sub", "Dub");

    // Default server options
    public static final List<String> DEFAULT_SERVER_OPTIONS = Arrays.asList("Server 1", "Server 2", "Server 3", "Server 4");

    // Private constructor to prevent instantiation
    private PlayerUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ============================================
    // Time Formatting
    // ============================================

    /**
     * Formats milliseconds to time string (HH:MM:SS or MM:SS)
     * 
     * @param millis Time in milliseconds
     * @return Formatted time string
     */
    public static String formatTime(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        int hours = (millis / (1000 * 60 * 60));

        if (hours > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        }
    }

    /**
     * Formats milliseconds to time string with hours always shown
     * 
     * @param millis Time in milliseconds
     * @return Formatted time string (always HH:MM:SS format)
     */
    public static String formatTimeWithHours(int millis) {
        int seconds = (millis / 1000) % 60;
        int minutes = (millis / (1000 * 60)) % 60;
        int hours = millis / (1000 * 60 * 60);

        return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds);
    }

    // ============================================
    // Error Handling
    // ============================================

    /**
     * Checks if an IOException is a network-related error
     * 
     * @param error The IOException to check
     * @return true if the error is network-related
     */
    public static boolean isNetworkError(IOException error) {
        if (error == null) return false;

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

    /**
     * Parses a PlaybackException and returns detailed error information
     * 
     * @param error The PlaybackException to parse
     * @return PlaybackErrorInfo containing error details and retry eligibility
     */
    @NonNull
    public static PlaybackErrorInfo parsePlaybackError(@NonNull PlaybackException error) {
        String errorMessage = "Playback error";
        boolean canRetry = false;

        switch (error.errorCode) {
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED:
            case PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT:
            case PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE:
                errorMessage = "Network connection failed";
                canRetry = true;
                Log.w(TAG, "Network-related playback error: " + error.getMessage());
                break;

            case PlaybackException.ERROR_CODE_DECODER_INIT_FAILED:
                errorMessage = "Video decoder failed. Try different quality.";
                Log.w(TAG, "Decoder initialization failed");
                break;

            case PlaybackException.ERROR_CODE_IO_UNSPECIFIED:
                errorMessage = "Stream error occurred";
                canRetry = true;
                Log.w(TAG, "Unspecified I/O error: " + error.getMessage());
                break;

            case PlaybackException.ERROR_CODE_REMOTE_ERROR:
                errorMessage = "Remote server error. Please try again later.";
                canRetry = true;
                break;

            case PlaybackException.ERROR_CODE_TIMEOUT:
                errorMessage = "Playback timed out. Please try again.";
                canRetry = true;
                break;

            case PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS:
                errorMessage = "Server returned error: " + error.getMessage();
                canRetry = true;
                break;

            default:
                errorMessage = "Playback error: " + error.getErrorCodeName();
                // Network-related errors typically have codes 1000-1999
                canRetry = error.errorCode >= 1000 && error.errorCode < 2000;
                Log.w(TAG, "Unknown playback error code: " + error.errorCode);
                break;
        }

        return new PlaybackErrorInfo(errorMessage, canRetry, error.errorCode);
    }

    /**
     * Container class for parsed playback error information
     */
    public static class PlaybackErrorInfo {
        public final String errorMessage;
        public final boolean canRetry;
        public final int errorCode;

        public PlaybackErrorInfo(String errorMessage, boolean canRetry, int errorCode) {
            this.errorMessage = errorMessage;
            this.canRetry = canRetry;
            this.errorCode = errorCode;
        }

        @Override
        public String toString() {
            return "PlaybackErrorInfo{message='" + errorMessage + "', canRetry=" + canRetry + ", code=" + errorCode + "}";
        }
    }

    // ============================================
    // Retry Configuration
    // ============================================

    /**
     * Returns the default retry configuration for playback errors
     * 
     * @return RetryConfig with default settings (3 retries, 2 second delay)
     */
    @NonNull
    public static RetryConfig getDefaultRetryConfig() {
        return new RetryConfig(
                3,                                      // maxRetries
                2000,                                   // retryDelayMillis
                Arrays.asList(                          // retryableErrorCodes
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                        PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
                        PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE
                )
        );
    }

    /**
     * Container class for retry configuration
     */
    public static class RetryConfig {
        public final int maxRetries;
        public final long retryDelayMillis;
        public final List<Integer> retryableErrorCodes;

        public RetryConfig(int maxRetries, long retryDelayMillis, List<Integer> retryableErrorCodes) {
            this.maxRetries = maxRetries;
            this.retryDelayMillis = retryDelayMillis;
            this.retryableErrorCodes = retryableErrorCodes;
        }

        /**
         * Checks if a given error code is eligible for retry
         * 
         * @param errorCode The error code to check
         * @return true if the error can be retried
         */
        public boolean isRetryable(int errorCode) {
            return retryableErrorCodes.contains(errorCode);
        }

        @Override
        public String toString() {
            return "RetryConfig{maxRetries=" + maxRetries + ", delayMs=" + retryDelayMillis + "}";
        }
    }

    // ============================================
    // Media Item Building
    // ============================================

    /**
     * Builds a MediaItem with all optional components
     * 
     * @param videoUri The video URI to play
     * @param title Media title
     * @param description Media description (can be null)
     * @param artworkUrl Artwork URL (can be null)
     * @param mediaId Media ID (can be null)
     * @param subtitles List of subtitle items (can be null)
     * @param subtitleUrl Fallback single subtitle URL (can be null)
     * @return Configured MediaItem
     */
    @NonNull
    public static MediaItem buildMediaItem(
            @NonNull Uri videoUri,
            @NonNull String title,
            String description,
            String artworkUrl,
            String mediaId,
            List<MediaItems.SubtitleItem> subtitles,
            String subtitleUrl) {

        // Build metadata
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title);

        if (description != null && !description.isEmpty()) {
            metadataBuilder.setDescription(description);
        }

        if (artworkUrl != null && !artworkUrl.isEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(artworkUrl));
            Log.d(TAG, "Setting artwork for media: " + artworkUrl);
        }

        // Build media item
        MediaItem.Builder mediaItemBuilder = new MediaItem.Builder()
                .setUri(videoUri)
                .setMediaMetadata(metadataBuilder.build());

        if (mediaId != null && !mediaId.isEmpty()) {
            mediaItemBuilder.setMediaId(mediaId);
        }

        // Add subtitles
        List<MediaItem.SubtitleConfiguration> subtitleList = buildSubtitleConfigurations(subtitles, subtitleUrl);

        if (!subtitleList.isEmpty()) {
            mediaItemBuilder.setSubtitleConfigurations(subtitleList);
        }

        return mediaItemBuilder.build();
    }

    /**
     * Builds a simple MediaItem with just video URI and title
     * 
     * @param videoUri The video URI
     * @param title Media title
     * @return Configured MediaItem
     */
    @NonNull
    public static MediaItem buildSimpleMediaItem(@NonNull Uri videoUri, @NonNull String title) {
        MediaMetadata metadata = new MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .build();

        return new MediaItem.Builder()
                .setUri(videoUri)
                .setMediaMetadata(metadata)
                .build();
    }

    /**
     * Builds subtitle configurations from subtitle items
     * 
     * @param subtitles List of subtitle items
     * @param fallbackUrl Fallback subtitle URL
     * @return List of subtitle configurations
     */
    @NonNull
    private static List<MediaItem.SubtitleConfiguration> buildSubtitleConfigurations(
            List<MediaItems.SubtitleItem> subtitles,
            String fallbackUrl) {

        List<MediaItem.SubtitleConfiguration> subtitleList = new ArrayList<>();

        if (subtitles != null && !subtitles.isEmpty()) {
            for (MediaItems.SubtitleItem subtitleItem : subtitles) {
                if (subtitleItem.getUrl() != null && !subtitleItem.getUrl().isEmpty()) {
                    MediaItem.SubtitleConfiguration subtitle = new MediaItem.SubtitleConfiguration.Builder(
                            Uri.parse(subtitleItem.getUrl()))
                            .setMimeType(MimeTypes.TEXT_VTT)
                            .setLanguage(subtitleItem.getLang() != null ? subtitleItem.getLang() : "und")
                            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                            .build();
                    subtitleList.add(subtitle);
                }
            }
            Log.d(TAG, "Added " + subtitleList.size() + " subtitle configurations");
        } else if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
            MediaItem.SubtitleConfiguration subtitle = new MediaItem.SubtitleConfiguration.Builder(
                    Uri.parse(fallbackUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage("en")
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build();
            subtitleList.add(subtitle);
            Log.d(TAG, "Added fallback subtitle configuration");
        }

        return subtitleList;
    }

    // ============================================
    // Options Builders
    // ============================================

    /**
     * Builds subtitle display options list
     * 
     * @param mediaItem The media item containing subtitles
     * @return List of subtitle display options (starts with "Off")
     */
    @NonNull
    public static List<String> buildSubtitleOptions(MediaItems mediaItem) {
        List<String> subtitleOptionsList = new ArrayList<>();
        subtitleOptionsList.add("Off"); // Always add "Off" as first option

        if (mediaItem != null) {
            List<MediaItems.SubtitleItem> subtitles = mediaItem.getSubtitles();

            if (subtitles != null && !subtitles.isEmpty()) {
                for (MediaItems.SubtitleItem subtitle : subtitles) {
                    String displayName = subtitle.getLanguage() != null && !subtitle.getLanguage().isEmpty()
                            ? subtitle.getLanguage()
                            : subtitle.getLang();
                    subtitleOptionsList.add(displayName);
                }
                Log.d(TAG, "Built " + subtitleOptionsList.size() + " subtitle options");
            } else if (mediaItem.getSubtitleUrl() != null && !mediaItem.getSubtitleUrl().isEmpty()) {
                subtitleOptionsList.add("English");
            }
        }

        return subtitleOptionsList;
    }

    /**
     * Builds quality label array from video sources
     * 
     * @param mediaItem The media item containing video sources
     * @return Array of quality labels
     */
    @NonNull
    public static String[] buildQualityLabels(MediaItems mediaItem) {
        if (mediaItem == null || mediaItem.getVideoSources() == null ||
                mediaItem.getVideoSources().isEmpty()) {
            Log.w(TAG, "No video sources available for quality labels");
            return new String[0];
        }

        List<MediaItems.VideoSource> sources = mediaItem.getVideoSources();
        String[] qualityLabels = new String[sources.size()];

        for (int i = 0; i < sources.size(); i++) {
            qualityLabels[i] = sources.get(i).getQuality();
        }

        Log.d(TAG, "Built " + qualityLabels.length + " quality labels");
        return qualityLabels;
    }

    /**
     * Gets the default speed options for playback
     * 
     * @return List of available playback speeds
     */
    @NonNull
    public static List<Float> getDefaultSpeedOptions() {
        return DEFAULT_SPEED_OPTIONS;
    }

    /**
     * Converts speed options to display labels
     * 
     * @param speedOptions List of speed values
     * @return Array of speed labels (e.g., "0.5x", "1.0x")
     */
    @NonNull
    public static String[] getSpeedLabels(@NonNull List<Float> speedOptions) {
        String[] labels = new String[speedOptions.size()];
        for (int i = 0; i < speedOptions.size(); i++) {
            labels[i] = speedOptions.get(i) + "x";
        }
        return labels;
    }

    /**
     * Gets the default audio track options
     * 
     * @return List of available audio options
     */
    @NonNull
    public static List<String> getDefaultAudioOptions() {
        return DEFAULT_AUDIO_OPTIONS;
    }

    /**
     * Gets the default server options
     * 
     * @return List of available server options
     */
    @NonNull
    public static List<String> getDefaultServerOptions() {
        return DEFAULT_SERVER_OPTIONS;
    }

    // ============================================
    // Playback Control Helpers
    // ============================================

    /**
     * Calculates the appropriate playback speed based on user selection
     * 
     * @param speedIndex Index in the speed options list
     * @param speedOptions List of available speeds
     * @return The selected playback speed
     */
    public static float getPlaybackSpeed(int speedIndex, List<Float> speedOptions) {
        if (speedIndex >= 0 && speedIndex < speedOptions.size()) {
            return speedOptions.get(speedIndex);
        }
        return 1.0f; // Default speed
    }

    /**
     * Validates and clamps a seek position to valid range
     * 
     * @param position Desired position in milliseconds
     * @param duration Total duration in milliseconds
     * @return Clamped position
     */
    public static long clampSeekPosition(long position, long duration) {
        if (duration <= 0) return 0;
        if (position < 0) return 0;
        if (position > duration) return duration;
        return position;
    }

    /**
     * Calculates buffered position for progress bar
     * 
     * @param bufferedPosition Buffered position in milliseconds
     * @param duration Total duration in milliseconds
     * @return Buffered position as progress (0 to max)
     */
    public static int calculateBufferProgress(long bufferedPosition, long duration) {
        if (duration <= 0) return 0;
        return (int) ((bufferedPosition * 100) / duration);
    }

    // ============================================
    // Validation Helpers
    // ============================================

    /**
     * Validates that a media item has playable content
     * 
     * @param mediaItem The media item to validate
     * @return true if the media item is valid for playback
     */
    public static boolean isValidMediaItem(MediaItems mediaItem) {
        if (mediaItem == null) {
            Log.w(TAG, "Media item is null");
            return false;
        }

        String videoUrl = mediaItem.getBestVideoUrl();
        if (videoUrl == null || videoUrl.isEmpty()) {
            Log.w(TAG, "Media item has no video URL");
            return false;
        }

        // Check for valid URI scheme
        Uri uri = Uri.parse(videoUrl);
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            Log.w(TAG, "Invalid URI scheme: " + scheme);
            return false;
        }

        return true;
    }

    /**
     * Validates subtitle index is within valid range
     * 
     * @param index Subtitle index to validate
     * @param maxIndex Maximum valid index
     * @return Validated index (0 if invalid)
     */
    public static int validateSubtitleIndex(int index, int maxIndex) {
        if (index < 0 || index > maxIndex) {
            return 0; // Default to "Off"
        }
        return index;
    }

    /**
     * Validates quality index is within valid range
     * 
     * @param index Quality index to validate
     * @param maxIndex Maximum valid index
     * @return Validated index (0 if invalid)
     */
    public static int validateQualityIndex(int index, int maxIndex) {
        if (index < 0 || index >= maxIndex) {
            return 0; // Default to first quality
        }
        return index;
    }

    // ============================================
    // Logging Helpers
    // ============================================

    /**
     * Logs media item details for debugging
     * 
     * @param mediaItem The media item to log
     */
    public static void logMediaItemDetails(MediaItems mediaItem) {
        if (mediaItem == null) {
            Log.d(TAG, "Media item is null");
            return;
        }

        Log.i(TAG, "=== Media Item Details ===");
        Log.i(TAG, "Title: " + mediaItem.getTitle());
        Log.i(TAG, "Video URL: " + mediaItem.getBestVideoUrl());
        Log.i(TAG, "Background URL: " + mediaItem.getBackgroundImageUrl());

        if (mediaItem.getVideoSources() != null) {
            Log.i(TAG, "Quality options: " + mediaItem.getVideoSources().size());
            for (int i = 0; i < mediaItem.getVideoSources().size(); i++) {
                MediaItems.VideoSource source = mediaItem.getVideoSources().get(i);
                Log.i(TAG, "  [" + i + "] " + source.getQuality() + ": " + source.getUrl());
            }
        }

        if (mediaItem.getSubtitles() != null) {
            Log.i(TAG, "Subtitle options: " + mediaItem.getSubtitles().size());
            for (MediaItems.SubtitleItem subtitle : mediaItem.getSubtitles()) {
                Log.i(TAG, "  - " + subtitle.getLanguage() + " (" + subtitle.getLang() + "): " + subtitle.getUrl());
            }
        }

        Log.i(TAG, "===========================");
    }

    /**
     * Creates a playback info summary for logging
     * 
     * @param currentPosition Current playback position
     * @param duration Total duration
     * @param bufferedPosition Buffered position
     * @return Formatted status string
     */
    public static String getPlaybackStatus(long currentPosition, long duration, long bufferedPosition) {
        StringBuilder sb = new StringBuilder();
        sb.append("Position: ").append(formatTime((int) currentPosition));
        sb.append(" / ").append(formatTime((int) duration));
        sb.append(" | Buffered: ").append(formatTime((int) bufferedPosition));
        return sb.toString();
    }
}
