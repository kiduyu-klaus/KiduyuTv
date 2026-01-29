package com.kiduyu.klaus.kiduyutv.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages app-wide preferences and settings
 */
public class PreferencesManager {
    private static final String TAG = "PreferencesManager";
    private static final String PREF_NAME = "CineStreamTV_Preferences";

    // Preference keys
    private static final String KEY_VIDEO_QUALITY = "video_quality";
    private static final String KEY_SUBTITLE_LANGUAGE = "subtitle_language";
    private static final String KEY_DARK_THEME = "dark_theme";
    private static final String KEY_AUTO_QUALITY = "auto_quality";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    private static final String KEY_CACHE_SIZE = "cache_size";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_VOICE_SEARCH_ENABLED = "voice_search_enabled";
    private static final String KEY_PLAYBACK_BUFFER_SIZE = "playback_buffer_size";
    private static final String KEY_WATCH_HISTORY = "watch_history";
    private static final int MAX_HISTORY_ITEMS = 50;

    // Default values
    private static final String DEFAULT_VIDEO_QUALITY = "Auto";
    private static final String DEFAULT_SUBTITLE_LANGUAGE = "en";
    private static final boolean DEFAULT_DARK_THEME = true;
    private static final boolean DEFAULT_AUTO_QUALITY = true;
    private static final boolean DEFAULT_VOICE_SEARCH_ENABLED = true;
    // Add these constants
    private static final String KEY_PLAYBACK_BUFFER_DURATION = "playback_buffer_duration";
    private static final int DEFAULT_BUFFER_DURATION = 10; // 10 minutes

    private static PreferencesManager instance;
    private final SharedPreferences preferences;
    private final Context context;

    private PreferencesManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }

    // Video Quality Settings
    public String getVideoQuality() {
        return preferences.getString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY);
    }

    public void setVideoQuality(String quality) {
        preferences.edit().putString(KEY_VIDEO_QUALITY, quality).apply();
        Log.i(TAG, "Video quality set to: " + quality);
    }

    public boolean isAutoQualityEnabled() {
        return preferences.getBoolean(KEY_AUTO_QUALITY, DEFAULT_AUTO_QUALITY);
    }

    public void setAutoQualityEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_QUALITY, enabled).apply();
        Log.i(TAG, "Auto quality " + (enabled ? "enabled" : "disabled"));
    }

    // Subtitle Settings
    public String getSubtitleLanguage() {
        return preferences.getString(KEY_SUBTITLE_LANGUAGE, DEFAULT_SUBTITLE_LANGUAGE);
    }

    public void setSubtitleLanguage(String languageCode) {
        preferences.edit().putString(KEY_SUBTITLE_LANGUAGE, languageCode).apply();
        Log.i(TAG, "Subtitle language set to: " + languageCode);
    }

    // Theme Settings
    public boolean isDarkThemeEnabled() {
        return preferences.getBoolean(KEY_DARK_THEME, DEFAULT_DARK_THEME);
    }

    public void setDarkThemeEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_DARK_THEME, enabled).apply();
        Log.i(TAG, "Dark theme " + (enabled ? "enabled" : "disabled"));
    }

    // Search History
    public List<String> getSearchHistory() {
        String jsonString = preferences.getString(KEY_SEARCH_HISTORY, "[]");
        List<String> history = new ArrayList<>();

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                history.add(jsonArray.getString(i));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading search history", e);
        }

        return history;
    }

    public void addToSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }

        List<String> historyList = new ArrayList<>(getSearchHistory());

        // Remove if already exists (to move to front)
        historyList.remove(query);

        // Add to front
        historyList.add(0, query);

        // Store as JSON to preserve order (no max limit)
        try {
            JSONArray jsonArray = new JSONArray(historyList);
            preferences.edit().putString(KEY_SEARCH_HISTORY, jsonArray.toString()).apply();
            Log.i(TAG, "Added to search history: " + query);
        } catch (Exception e) {
            Log.e(TAG, "Error saving search history", e);
        }
    }

    public void clearSearchHistory() {
        preferences.edit().remove(KEY_SEARCH_HISTORY).apply();
        Log.i(TAG, "Search history cleared");
    }

    public int getSearchHistoryCount() {
        return getSearchHistory().size();
    }

    // Voice Search Settings
    public boolean isVoiceSearchEnabled() {
        return preferences.getBoolean(KEY_VOICE_SEARCH_ENABLED, DEFAULT_VOICE_SEARCH_ENABLED);
    }

    public void setVoiceSearchEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_VOICE_SEARCH_ENABLED, enabled).apply();
        Log.i(TAG, "Voice search " + (enabled ? "enabled" : "disabled"));
    }

    // Playback Buffer Settings
    public int getPlaybackBufferDuration() {
        return preferences.getInt(KEY_PLAYBACK_BUFFER_DURATION, DEFAULT_BUFFER_DURATION);
    }


    public void setPlaybackBufferDuration(int durationMinutes) {
        preferences.edit().putInt(KEY_PLAYBACK_BUFFER_DURATION, durationMinutes).apply();
        Log.i(TAG, "Playback buffer duration set to: " + durationMinutes + " minutes");
    }

    // Cache Management
    public void setCacheSize(String size) {
        preferences.edit().putString(KEY_CACHE_SIZE, size).apply();
    }

    public String getCacheSize() {
        return preferences.getString(KEY_CACHE_SIZE, "0 MB");
    }

    public void clearAllPreferences() {
        preferences.edit().clear().apply();
        Log.i(TAG, "All preferences cleared");
    }

    // First Launch Check
    public boolean isFirstLaunch() {
        return preferences.getBoolean(KEY_FIRST_LAUNCH, true);
    }

    public void setFirstLaunchComplete() {
        preferences.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
    }

    // Get all settings as a readable string (for About screen)
    public String getAllSettingsInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Video Quality: ").append(getVideoQuality()).append("\n");
        info.append("Auto Quality: ").append(isAutoQualityEnabled() ? "On" : "Off").append("\n");
        info.append("Subtitle Language: ").append(getSubtitleLanguage()).append("\n");
        info.append("Dark Theme: ").append(isDarkThemeEnabled() ? "On" : "Off").append("\n");
        info.append("Voice Search: ").append(isVoiceSearchEnabled() ? "On" : "Off").append("\n");
        info.append("Playback Buffer: ").append(getPlaybackBufferDuration()).append("MB\n");
        info.append("Search History: ").append(getSearchHistoryCount()).append(" items\n");
        info.append("Cache Size: ").append(getCacheSize()).append("\n");
        return info.toString();
    }

    // Reset to defaults
    public void resetToDefaults() {
        preferences.edit()
                .putString(KEY_VIDEO_QUALITY, DEFAULT_VIDEO_QUALITY)
                .putString(KEY_SUBTITLE_LANGUAGE, DEFAULT_SUBTITLE_LANGUAGE)
                .putBoolean(KEY_DARK_THEME, DEFAULT_DARK_THEME)
                .putBoolean(KEY_AUTO_QUALITY, DEFAULT_AUTO_QUALITY)
                .putBoolean(KEY_VOICE_SEARCH_ENABLED, DEFAULT_VOICE_SEARCH_ENABLED)
                .putInt(KEY_PLAYBACK_BUFFER_DURATION, DEFAULT_BUFFER_DURATION)
                .apply();
        Log.i(TAG, "Preferences reset to defaults");
    }

    // ============================================
    // Watch History Methods
    // ============================================

    /**
     * Save or update a watch history item
     * Stores media info and current playback position
     */
    public void saveWatchHistory(MediaItems mediaItem, long currentPosition, long totalDuration) {
        try {
            WatchHistoryItem item = new WatchHistoryItem();
            item.id = mediaItem.getId() != null ? mediaItem.getId() : mediaItem.getTmdbId();
            item.tmdbId = mediaItem.getTmdbId();
            item.title = mediaItem.getTitle();
            item.posterUrl = mediaItem.getPosterUrl();
            item.backgroundImageUrl = mediaItem.getBackgroundImageUrl();
            item.mediaType = mediaItem.getMediaType();
            item.rating = mediaItem.getRating();
            item.currentPosition = currentPosition;
            item.totalDuration = totalDuration;
            item.description = mediaItem.getDescription();
            item.lastWatchedTimestamp = System.currentTimeMillis();

            // TV specific fields
            item.season = mediaItem.getSeason();
            item.episode = mediaItem.getEpisode();

            // Get existing history
            List<WatchHistoryItem> history = getAllWatchHistory();

            // Remove existing item with same ID if exists
            history.removeIf(existing -> existing.id != null && existing.id.equals(item.id));

            // Add new item at the beginning
            history.add(0, item);

            // Limit history size to prevent SharedPreferences bloat
            if (history.size() > MAX_HISTORY_ITEMS) {
                history = history.subList(0, MAX_HISTORY_ITEMS);
            }

            // Save to SharedPreferences
            JSONArray jsonArray = new JSONArray();
            for (WatchHistoryItem historyItem : history) {
                jsonArray.put(historyItem.toJson());
            }
            preferences.edit().putString(KEY_WATCH_HISTORY, jsonArray.toString()).apply();

            Log.i(TAG, "Saved watch history: " + item.title + " at " + formatTime((int) currentPosition));
        } catch (Exception e) {
            Log.e(TAG, "Error saving watch history: " + e.getMessage());
        }
    }

    /**
     * Get watch history item for a specific media
     * @return WatchHistoryItem or null if not found
     */
    public WatchHistoryItem getWatchHistory(String mediaId) {
        List<WatchHistoryItem> history = getAllWatchHistory();
        for (WatchHistoryItem item : history) {
            if (item.id != null && item.id.equals(mediaId)) {
                return item;
            }
        }
        return null;
    }

    /**
     * Get all watch history items
     */
    public List<WatchHistoryItem> getAllWatchHistory() {
        List<WatchHistoryItem> history = new ArrayList<>();
        String jsonString = preferences.getString(KEY_WATCH_HISTORY, null);

        if (jsonString == null || jsonString.isEmpty()) {
            return history;
        }

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                WatchHistoryItem item = WatchHistoryItem.fromJson(jsonObject);
                history.add(item);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing watch history: " + e.getMessage());
        }

        return history;
    }

    /**
     * Check if media has watch history
     */
    public boolean hasWatchHistory(String mediaId) {
        return getWatchHistory(mediaId) != null;
    }

    /**
     * Clear watch history
     */
    public void clearWatchHistory() {
        preferences.edit().remove(KEY_WATCH_HISTORY).apply();
        Log.i(TAG, "Watch history cleared");
    }

    /**
     * Remove specific item from watch history
     */
    public void removeFromWatchHistory(String mediaId) {
        List<WatchHistoryItem> history = getAllWatchHistory();
        history.removeIf(item -> item.id != null && item.id.equals(mediaId));

        JSONArray jsonArray = new JSONArray();
        for (WatchHistoryItem item : history) {
            jsonArray.put(item.toJson());
        }
        preferences.edit().putString(KEY_WATCH_HISTORY, jsonArray.toString()).apply();
    }

    /**
     * Format milliseconds to readable time string
     */
    public String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    /**
     * Watch history item data class
     */
    public static class WatchHistoryItem {
        public String id;
        public String tmdbId;
        public String title;
        public String posterUrl;
        public String backgroundImageUrl;
        public String mediaType;
        public long currentPosition;
        public long totalDuration;
        public long lastWatchedTimestamp;
        public String season;
        public String episode;
        public Float rating;
        public String description;

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put("id", id);
                json.put("tmdbId", tmdbId);
                json.put("title", title);
                json.put("posterUrl", posterUrl);
                json.put("backgroundImageUrl", backgroundImageUrl);
                json.put("mediaType", mediaType);
                json.put("currentPosition", currentPosition);
                json.put("totalDuration", totalDuration);
                json.put("lastWatchedTimestamp", lastWatchedTimestamp);
                json.put("season", season);
                json.put("episode", episode);
                json.put("description", description);
                json.put("rating", rating);
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON: " + e.getMessage());
            }
            return json;
        }

        public static WatchHistoryItem fromJson(JSONObject json) {
            WatchHistoryItem item = new WatchHistoryItem();
            try {
                item.id = json.optString("id", null);
                item.tmdbId = json.optString("tmdbId", null);
                item.title = json.optString("title", null);
                item.posterUrl = json.optString("posterUrl", null);
                item.backgroundImageUrl = json.optString("backgroundImageUrl", null);
                item.mediaType = json.optString("mediaType", null);
                item.currentPosition = json.optLong("currentPosition", 0);
                item.totalDuration = json.optLong("totalDuration", 0);
                item.lastWatchedTimestamp = json.optLong("lastWatchedTimestamp", 0);
                item.season = json.optString("season", null);
                item.episode = json.optString("episode", null);
                item.rating = Float.valueOf(json.optString("rating", String.valueOf(0)));
                item.description = json.optString("description", "");
                item.episode = json.optString("episode", null);
            } catch (Exception e) {
                Log.e(TAG, "Error parsing JSON: " + e.getMessage());
            }
            return item;
        }

        /**
         * Get progress percentage
         */
        public int getProgressPercentage() {
            if (totalDuration > 0) {
                return (int) ((currentPosition * 100) / totalDuration);
            }
            return 0;
        }

        /**
         * Check if playback is complete (95% or more)
         */
        public boolean isCompleted() {
            return totalDuration > 0 && currentPosition >= (totalDuration * 0.95);
        }
    }
}