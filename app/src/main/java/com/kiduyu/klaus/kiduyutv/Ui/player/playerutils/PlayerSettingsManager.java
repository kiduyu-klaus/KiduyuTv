package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.media3.common.util.UnstableApi;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;

@UnstableApi
public class PlayerSettingsManager {

    private final PlayerActivity activity;

    public PlayerSettingsManager(PlayerActivity activity) {
        this.activity = activity;
    }

    /**
     * Initialize the settings panel and its components
     */
    public void initializeSettingsPanel() {
        activity.settingsPanelContainer = activity.findViewById(R.id.settingsPanelContainer);
        activity.settingsOverlayBackground = activity.findViewById(R.id.settingsOverlayBackground);
        activity.settingsPanel = activity.findViewById(R.id.settingsPanel);
        activity.switchAutoQuality = activity.findViewById(R.id.switchAutoQuality);
        activity.switchShowSubtitles = activity.findViewById(R.id.switchShowSubtitles);
        activity.switchUseDoh = activity.findViewById(R.id.switchUseDoh);
        activity.switchProgressiveCache = activity.findViewById(R.id.switchProgressiveCache);
        activity.switchImageCdn = activity.findViewById(R.id.switchImageCdn);
        activity.textBufferSize = activity.findViewById(R.id.textBufferSize);
        activity.textSubtitleLanguage = activity.findViewById(R.id.textSubtitleLanguage);
        activity.btnCloseSettings = activity.findViewById(R.id.btnCloseSettings);

        // Load current settings values
        loadSettingsValues();

        // Setup click listeners
        activity.settingsOverlayBackground.setOnClickListener(v -> hideSettingsPanel());
        activity.btnCloseSettings.setOnClickListener(v -> hideSettingsPanel());

        // Toggle listeners
        activity.switchAutoQuality.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.preferencesManager.setAutoQualityEnabled(isChecked);
            activity.controlsManager.showToast("Auto Quality " + (isChecked ? "enabled" : "disabled"));
        });

        activity.switchShowSubtitles.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && activity.currentSubtitleIndex < 0 && activity.subtitles != null && !activity.subtitles.isEmpty()) {
                // If enabling subtitles but none selected, show subtitle dialog
                activity.currentSubtitleIndex = 0;
                if (activity.subtitles.size() > 0) {
                    activity.btnSubtitles.setText(activity.subtitles.get(0).getLanguage());
                    activity.playerCore.loadVideoSource(activity.currentSourceIndex);
                }
            } else if (!isChecked) {
                // Disable subtitles
                activity.currentSubtitleIndex = -1;
                activity.btnSubtitles.setText("None");
                activity.playerCore.loadVideoSource(activity.currentSourceIndex);
            }
            activity.controlsManager.showToast("Subtitles " + (isChecked ? "enabled" : "disabled"));
        });

        activity.switchUseDoh.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.controlsManager.showToast("DoH " + (isChecked ? "enabled" : "disabled"));
        });

        activity.switchProgressiveCache.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.controlsManager.showToast("Progressive Cache " + (isChecked ? "enabled" : "disabled"));
        });

        activity.switchImageCdn.setOnCheckedChangeListener((buttonView, isChecked) -> {
            activity.controlsManager.showToast("Image CDN " + (isChecked ? "enabled" : "disabled"));
        });

        // Setting click listeners
        activity.findViewById(R.id.settingBufferSize).setOnClickListener(v -> showBufferSizeDialog());
        activity.findViewById(R.id.settingSubtitleLanguage).setOnClickListener(v -> showSubtitleLanguageDialog());
        activity.findViewById(R.id.settingAutoQuality).setOnClickListener(v ->
                activity.controlsManager.showToast("Auto Quality clicked"));
        activity.findViewById(R.id.settingShowSubtitles).setOnClickListener(v ->
                activity.controlsManager.showToast("Show Subtitles clicked"));
        activity.findViewById(R.id.settingUseDoh).setOnClickListener(v ->
                activity.controlsManager.showToast("Use DoH clicked"));
        activity.findViewById(R.id.settingProgressiveCache).setOnClickListener(v ->
                activity.controlsManager.showToast("Progressive Cache clicked"));
        activity.findViewById(R.id.settingImageCdn).setOnClickListener(v ->
                activity.controlsManager.showToast("Use Image CDN clicked"));
    }

    /**
     * Load current settings values from PreferencesManager
     */
    void loadSettingsValues() {
        activity.switchAutoQuality.setChecked(activity.preferencesManager.isAutoQualityEnabled());
        activity.switchShowSubtitles.setChecked(activity.currentSubtitleIndex >= 0);
        activity.textBufferSize.setText(activity.preferencesManager.getPlaybackBufferDuration() + " min");

        // Load subtitle language
        String subtitleLang = activity.preferencesManager.getSubtitleLanguage();
        activity.textSubtitleLanguage.setText(getLanguageName(subtitleLang));

        // Set default toggles (these would normally be stored in preferences)
        activity.switchUseDoh.setChecked(true);
        activity.switchProgressiveCache.setChecked(true);
        activity.switchImageCdn.setChecked(true);
    }

    /**
     * Show the settings panel with slide animation.
     * Traps D-pad focus inside the overlay until it is dismissed.
     */
    public void showSettingsPanel() {
        if (activity.settingsPanelVisible) return;

        activity.settingsPanelVisible = true;
        activity.settingsPanelContainer.setVisibility(View.VISIBLE);

        // Load latest values before showing
        loadSettingsValues();

        // Animate in
        Animation slideIn = AnimationUtils.loadAnimation(activity, R.anim.slide_in_right);
        activity.settingsPanel.startAnimation(slideIn);

        // Hide video controls so they are invisible while settings are open
        activity.controlsManager.hideControls();

        // ── Focus trap ──────────────────────────────────────────────────
        // Block the player controls from receiving focus while the overlay is open.
        activity.topInfoContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        activity.bottomControlsContainer.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);

        // Allow focus to move freely among the settings panel's children.
        activity.settingsPanelContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        activity.settingsPanelContainer.setFocusable(false); // container itself shouldn't steal focus

        // Send focus to the Close button as the natural entry point.
        activity.btnCloseSettings.requestFocus();
    }

    /**
     * Hide the settings panel with slide animation.
     * Releases the focus trap and returns focus to the Settings button.
     */
    public void hideSettingsPanel() {
        if (!activity.settingsPanelVisible) return;

        // ── Release focus trap ──────────────────────────────────────────
        // Restore normal focus traversal for player controls.
        activity.topInfoContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
        activity.bottomControlsContainer.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

        // Animate out
        Animation slideOut = AnimationUtils.loadAnimation(activity, R.anim.slide_out_right);
        slideOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {
                activity.settingsPanelContainer.setVisibility(View.GONE);
                activity.settingsPanelVisible = false;

                // Return focus to the Settings button that opened the panel.
                activity.controlsManager.showControls(); // makes bottomControlsContainer visible again
                activity.btnSettings.requestFocus();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        activity.settingsPanel.startAnimation(slideOut);
    }

    /**
     * Show buffer size selection dialog
     */
    void showBufferSizeDialog() {
        String[] bufferOptions = {"5 min", "10 min", "15 min", "20 min", "30 min"};
        int[] bufferValues = {5, 10, 15, 20, 30};
        int currentValue = activity.preferencesManager.getPlaybackBufferDuration();
        int currentIndex = 0;
        for (int i = 0; i < bufferValues.length; i++) {
            if (bufferValues[i] == currentValue) {
                currentIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Buffer Duration");
        builder.setSingleChoiceItems(bufferOptions, currentIndex, (dialog, which) -> {
            activity.preferencesManager.setPlaybackBufferDuration(bufferValues[which]);
            activity.textBufferSize.setText(bufferOptions[which]);
            dialog.dismiss();
            activity.controlsManager.showToast("Buffer duration set to " + bufferOptions[which]);

            // Restart player to apply new buffer settings
            if (activity.player != null) {
                long currentPos = activity.player.getCurrentPosition();
                activity.playerCore.loadVideoSource(activity.currentSourceIndex);
                activity.player.seekTo(currentPos);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        activity.controlsManager.applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Show subtitle language selection dialog
     */
    void showSubtitleLanguageDialog() {
        String[] languages = {"English", "Spanish", "French", "German", "Portuguese", "Italian", "Japanese", "Korean", "Chinese", "Russian"};
        String[] langCodes = {"en", "es", "fr", "de", "pt", "it", "ja", "ko", "zh", "ru"};

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Subtitle Language");
        builder.setItems(languages, (dialog, which) -> {
            activity.preferencesManager.setSubtitleLanguage(langCodes[which]);
            activity.textSubtitleLanguage.setText(languages[which]);
            dialog.dismiss();
            activity.controlsManager.showToast("Subtitle language set to " + languages[which]);
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        activity.controlsManager.applyFocusHighlight(dialog);
        dialog.show();
    }

    /**
     * Get language name from language code
     */
    String getLanguageName(String langCode) {
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
}
