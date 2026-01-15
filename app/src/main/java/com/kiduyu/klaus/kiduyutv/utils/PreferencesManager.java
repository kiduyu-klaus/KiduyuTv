package com.kiduyu.klaus.kiduyutv.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

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
    
    // Default values
    private static final String DEFAULT_VIDEO_QUALITY = "Auto";
    private static final String DEFAULT_SUBTITLE_LANGUAGE = "en";
    private static final boolean DEFAULT_DARK_THEME = true;
    private static final boolean DEFAULT_AUTO_QUALITY = true;
    private static final boolean DEFAULT_VOICE_SEARCH_ENABLED = true;
    private static final int DEFAULT_BUFFER_SIZE = 10; // 10MB
    
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
        Set<String> historySet = preferences.getStringSet(KEY_SEARCH_HISTORY, new HashSet<>());
        return new ArrayList<>(historySet);
    }
    
    public void addToSearchHistory(String query) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        Set<String> historySet = new HashSet<>(getSearchHistory());
        
        // Remove if already exists (to move to front)
        historySet.remove(query);
        
        // Add to front
        historySet.add(query);
        
        // Keep only last 10 items
        List<String> historyList = new ArrayList<>(historySet);
        if (historyList.size() > 10) {
            historyList = historyList.subList(0, 10);
        }
        
        preferences.edit().putStringSet(KEY_SEARCH_HISTORY, new HashSet<>(historyList)).apply();
        Log.i(TAG, "Added to search history: " + query);
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
    public int getPlaybackBufferSize() {
        return preferences.getInt(KEY_PLAYBACK_BUFFER_SIZE, DEFAULT_BUFFER_SIZE);
    }
    
    public void setPlaybackBufferSize(int sizeMB) {
        preferences.edit().putInt(KEY_PLAYBACK_BUFFER_SIZE, sizeMB).apply();
        Log.i(TAG, "Playback buffer size set to: " + sizeMB + "MB");
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
        info.append("Playback Buffer: ").append(getPlaybackBufferSize()).append("MB\n");
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
            .putInt(KEY_PLAYBACK_BUFFER_SIZE, DEFAULT_BUFFER_SIZE)
            .apply();
        Log.i(TAG, "Preferences reset to defaults");
    }
}