package com.kiduyu.klaus.kiduyutv.Ui.settings;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.format.Formatter;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.widget.NestedScrollView;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.io.File;

public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    // UI Components
    private TextView videoQualityValue;
    private TextView subtitleLanguageValue;
    private TextView cacheSizeValue;
    private TextView bufferSizeValue;
    private TextView appVersionValue;
    private Switch darkThemeSwitch;
    private Switch autoQualitySwitch;
    private Switch voiceSearchSwitch;
    private SeekBar bufferSizeSeekBar;
    private Button clearCacheButton;
    private Button resetDefaultsButton;
    private Button aboutButton;
    private LinearLayout videoQualityContainer;
    private LinearLayout subtitleContainer;
    private LinearLayout themeContainer;
    private LinearLayout voiceSearchContainer;
    private LinearLayout bufferSizeContainer;
    private LinearLayout cacheContainer;
    private LinearLayout autoQualityContainer;
    private LinearLayout appInfoContainer;
    private NestedScrollView scrollView;
    private View scrollOverlay;

    // Data
    private PreferencesManager preferencesManager;

    // Scroll overlay control
    private Handler overlayHandler;
    private Runnable hideOverlayRunnable;
    private static final long OVERLAY_HIDE_DELAY = 1000; // 1 second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);

        preferencesManager = PreferencesManager.getInstance(this);
        overlayHandler = new Handler(Looper.getMainLooper());

        setupToolbar();
        initializeViews();
        setupListeners();
        setupScrollListener();
        loadCurrentSettings();
        updateCacheSize();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void initializeViews() {
        // ScrollView and overlay
        scrollView = findViewById(R.id.scrollView);
        scrollOverlay = findViewById(R.id.scrollOverlay);

        // Value displays
        videoQualityValue = findViewById(R.id.videoQualityValue);
        subtitleLanguageValue = findViewById(R.id.subtitleLanguageValue);
        cacheSizeValue = findViewById(R.id.cacheSizeValue);
        bufferSizeValue = findViewById(R.id.bufferSizeValue);
        appVersionValue = findViewById(R.id.appVersionValue);

        // Switches
        darkThemeSwitch = findViewById(R.id.darkThemeSwitch);
        autoQualitySwitch = findViewById(R.id.autoQualitySwitch);
        voiceSearchSwitch = findViewById(R.id.voiceSearchSwitch);

        // SeekBar
        bufferSizeSeekBar = findViewById(R.id.bufferSizeSeekBar);

        // Buttons
        clearCacheButton = findViewById(R.id.clearCacheButton);
        resetDefaultsButton = findViewById(R.id.resetDefaultsButton);
        aboutButton = findViewById(R.id.aboutButton);

        // Containers for navigation
        videoQualityContainer = findViewById(R.id.videoQualityContainer);
        subtitleContainer = findViewById(R.id.subtitleContainer);
        themeContainer = findViewById(R.id.themeContainer);
        voiceSearchContainer = findViewById(R.id.voiceSearchContainer);
        bufferSizeContainer = findViewById(R.id.bufferSizeContainer);
        cacheContainer = findViewById(R.id.cacheContainer);
        autoQualityContainer = findViewById(R.id.autoQualityContainer);
        appInfoContainer = findViewById(R.id.appInfoContainer);
    }

    private void setupScrollListener() {
        hideOverlayRunnable = () -> scrollOverlay.animate()
                .alpha(0f)
                .setDuration(300)
                .withEndAction(() -> scrollOverlay.setVisibility(View.GONE))
                .start();

        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            // Show overlay when scrolling
            if (scrollOverlay.getVisibility() != View.VISIBLE) {
                scrollOverlay.setVisibility(View.VISIBLE);
                scrollOverlay.setAlpha(0f);
                scrollOverlay.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .start();
            }

            // Cancel any pending hide operations
            overlayHandler.removeCallbacks(hideOverlayRunnable);

            // Schedule overlay to hide after delay
            overlayHandler.postDelayed(hideOverlayRunnable, OVERLAY_HIDE_DELAY);
        });
    }

    private void setupListeners() {
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
                v.setBackgroundResource(R.drawable.generic_focus_selector);
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                v.setBackground(null);
            }
        };

        // Apply focus listener to all focusable items
        videoQualityContainer.setOnFocusChangeListener(focusChangeListener);
        subtitleContainer.setOnFocusChangeListener(focusChangeListener);
        themeContainer.setOnFocusChangeListener(focusChangeListener);
        voiceSearchContainer.setOnFocusChangeListener(focusChangeListener);
        bufferSizeContainer.setOnFocusChangeListener(focusChangeListener);
        cacheContainer.setOnFocusChangeListener(focusChangeListener);
        autoQualityContainer.setOnFocusChangeListener(focusChangeListener);
        appInfoContainer.setOnFocusChangeListener(focusChangeListener);
        clearCacheButton.setOnFocusChangeListener(focusChangeListener);
        resetDefaultsButton.setOnFocusChangeListener(focusChangeListener);
        aboutButton.setOnFocusChangeListener(focusChangeListener);

        // Container clicks for navigation
        videoQualityContainer.setOnClickListener(v -> showVideoQualityDialog());
        subtitleContainer.setOnClickListener(v -> showSubtitleLanguageDialog());

        // Auto Quality container click to toggle switch
        autoQualityContainer.setOnClickListener(v -> autoQualitySwitch.setChecked(!autoQualitySwitch.isChecked()));

        // Theme container click to toggle switch
        themeContainer.setOnClickListener(v -> {
            darkThemeSwitch.setChecked(!darkThemeSwitch.isChecked());
        });

        // Voice Search container click to toggle switch
        voiceSearchContainer.setOnClickListener(v -> {
            voiceSearchSwitch.setChecked(!voiceSearchSwitch.isChecked());
        });

        // Cache container click
        cacheContainer.setOnClickListener(v -> {
            showClearCacheDialog();
        });

        // App Info container click
        appInfoContainer.setOnClickListener(v -> {
            Toast.makeText(this, "App Version: " + appVersionValue.getText(), Toast.LENGTH_SHORT).show();
        });

        // Switches
        darkThemeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setDarkThemeEnabled(isChecked);
            Toast.makeText(this, "Theme " + (isChecked ? "dark" : "light") + " mode enabled", Toast.LENGTH_SHORT).show();
        });

        autoQualitySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setAutoQualityEnabled(isChecked);
            Toast.makeText(this, "Auto quality " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        voiceSearchSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferencesManager.setVoiceSearchEnabled(isChecked);
            Toast.makeText(this, "Voice search " + (isChecked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
        });

        // SeekBar
        bufferSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    bufferSizeValue.setText(progress + " MB");
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                preferencesManager.setPlaybackBufferDuration(seekBar.getProgress());
                Toast.makeText(SettingsActivity.this, "Buffer size set to " + seekBar.getProgress() + "MB", Toast.LENGTH_SHORT).show();
            }
        });

        // Buttons
        clearCacheButton.setOnClickListener(v -> showClearCacheDialog());
        resetDefaultsButton.setOnClickListener(v -> showResetDefaultsDialog());
        aboutButton.setOnClickListener(v -> showAboutDialog());
    }

    private void loadCurrentSettings() {
        // Load current values
        videoQualityValue.setText(preferencesManager.getVideoQuality());
        subtitleLanguageValue.setText(getLanguageName(preferencesManager.getSubtitleLanguage()));
        darkThemeSwitch.setChecked(preferencesManager.isDarkThemeEnabled());
        autoQualitySwitch.setChecked(preferencesManager.isAutoQualityEnabled());
        voiceSearchSwitch.setChecked(preferencesManager.isVoiceSearchEnabled());

        int bufferSize = preferencesManager.getPlaybackBufferDuration();
        bufferSizeSeekBar.setProgress(bufferSize);
        bufferSizeValue.setText(bufferSize + " MB");

        // Load app version
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            appVersionValue.setText(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            appVersionValue.setText("Unknown");
        }
    }

    /**
     * Calculate total cache size
     */
    private void updateCacheSize() {
        new Thread(() -> {
            long totalSize = 0;

            try {
                // Get cache directory
                File cacheDir = getCacheDir();
                totalSize += getDirSize(cacheDir);

                // Get external cache directory if available
                File externalCacheDir = getExternalCacheDir();
                if (externalCacheDir != null) {
                    totalSize += getDirSize(externalCacheDir);
                }

                // Format size for display
                final String formattedSize = Formatter.formatFileSize(this, totalSize);

                // Update UI on main thread
                runOnUiThread(() -> {
                    cacheSizeValue.setText(formattedSize);
                    preferencesManager.setCacheSize(formattedSize);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> cacheSizeValue.setText("Error"));
            }
        }).start();
    }

    /**
     * Calculate directory size recursively
     */
    private long getDirSize(File dir) {
        long size = 0;

        if (dir == null || !dir.exists()) {
            return 0;
        }

        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        size += file.length();
                    } else if (file.isDirectory()) {
                        size += getDirSize(file);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return size;
    }

    /**
     * Clear app cache
     */
    private void clearCache() {
        new Thread(() -> {
            try {
                // Clear internal cache
                File cacheDir = getCacheDir();
                deleteDir(cacheDir);

                // Clear external cache if available
                File externalCacheDir = getExternalCacheDir();
                if (externalCacheDir != null) {
                    deleteDir(externalCacheDir);
                }

                // Update cache size on UI thread
                runOnUiThread(() -> {
                    updateCacheSize();
                    Toast.makeText(this, "Cache cleared successfully", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error clearing cache", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    /**
     * Delete directory contents recursively
     */
    private boolean deleteDir(File dir) {
        if (dir == null || !dir.exists()) {
            return false;
        }

        try {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDir(file);
                    } else {
                        file.delete();
                    }
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showVideoQualityDialog() {
        String[] qualities = {"Auto", "1080p", "720p", "480p", "360p"};
        String currentQuality = preferencesManager.getVideoQuality();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Video Quality");

        int selectedIndex = 0;
        for (int i = 0; i < qualities.length; i++) {
            if (qualities[i].equals(currentQuality)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(qualities, selectedIndex, (dialog, which) -> {
            String selectedQuality = qualities[which];
            preferencesManager.setVideoQuality(selectedQuality);
            videoQualityValue.setText(selectedQuality);
            Toast.makeText(SettingsActivity.this, "Video quality set to " + selectedQuality, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSubtitleLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Italian", "Portuguese", "Japanese", "Korean", "Chinese"};
        String[] languageCodes = {"en", "es", "fr", "de", "it", "pt", "ja", "ko", "zh"};
        String currentCode = preferencesManager.getSubtitleLanguage();

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Select Subtitle Language");

        int selectedIndex = 0;
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(currentCode)) {
                selectedIndex = i;
                break;
            }
        }

        builder.setSingleChoiceItems(languages, selectedIndex, (dialog, which) -> {
            String selectedCode = languageCodes[which];
            preferencesManager.setSubtitleLanguage(selectedCode);
            subtitleLanguageValue.setText(languages[which]);
            Toast.makeText(SettingsActivity.this, "Subtitle language set to " + languages[which], Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showClearCacheDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Clear Cache");
        builder.setMessage("Are you sure you want to clear the app cache? Current cache size: " + cacheSizeValue.getText());
        builder.setPositiveButton("Clear", (dialog, which) -> {
            clearCache();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showResetDefaultsDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Reset to Defaults");
        builder.setMessage("Are you sure you want to reset all settings to their default values?");
        builder.setPositiveButton("Reset", (dialog, which) -> {
            preferencesManager.resetToDefaults();
            loadCurrentSettings();
            Toast.makeText(this, "Settings reset to defaults", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showAboutDialog() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("About CineStream TV");

        String aboutText = "CineStream TV Player\n" +
                "Version: " + appVersionValue.getText().toString() + "\n\n" +
                "A Netflix-style Android TV video player with TMDB integration.\n\n" +
                "Features:\n" +
                "• TMDB API integration for real movies and TV shows\n" +
                "• VideasyAPI streaming source support\n" +
                "• Voice search capability\n" +
                "• Smart caching system\n" +
                "• Multiple video quality options\n" +
                "• Subtitle support\n\n" +
                "Built with:\n" +
                "• ExoPlayer Media3\n" +
                "• TMDB API\n" +
                "• Android TV Leanback Library\n\n" +
                "© 2024 CineStream TV";

        TextView textView = new TextView(this);
        textView.setText(aboutText);
        textView.setPadding(48, 48, 48, 48);
        textView.setTextColor(getResources().getColor(android.R.color.white));
        textView.setMovementMethod(new ScrollingMovementMethod());

        builder.setView(textView);
        builder.setPositiveButton("Close", null);
        builder.show();
    }

    private String getLanguageName(String languageCode) {
        switch (languageCode) {
            case "en": return "English";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "pt": return "Portuguese";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "zh": return "Chinese";
            default: return languageCode.toUpperCase();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Update cache size when returning to settings
        updateCacheSize();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handler callbacks
        if (overlayHandler != null && hideOverlayRunnable != null) {
            overlayHandler.removeCallbacks(hideOverlayRunnable);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}