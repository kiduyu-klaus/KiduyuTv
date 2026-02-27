package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.C;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;

import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.SubdlService;
import com.kiduyu.klaus.kiduyutv.utils.Subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class PlayerSubtitleManager {

    private static final String TAG = "PlayerSubtitleManager";

    private final PlayerActivity activity;

    public PlayerSubtitleManager(PlayerActivity activity) {
        this.activity = activity;
    }

    void selectSubtitleTrack() {
        if (activity.player == null) {
            Log.w(TAG, "Cannot select subtitle track: player is null");
            return;
        }

        if (activity.currentSubtitleIndex < 0) {
            Log.i(TAG, "No subtitle selected (index -1), disabling text tracks");
            // Properly disable text renderer instead of fiddling with flags
            TrackSelectionParameters disableParams = activity.player.getTrackSelectionParameters()
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                    .build();
            activity.player.setTrackSelectionParameters(disableParams);
            // clear any existing cues so the subtitle overlay disappears immediately
            activity.subtitleView.setCues(Collections.emptyList());
            return;
        }

        if (activity.subtitles == null || activity.currentSubtitleIndex >= activity.subtitles.size()) {
            Log.w(TAG, "Cannot select subtitle track: subtitles list is null or index is out of bounds");
            return;
        }

        MediaItems.SubtitleItem subtitle = activity.subtitles.get(activity.currentSubtitleIndex);
        if (subtitle == null) {
            Log.w(TAG, "Cannot select subtitle track: subtitle is null");
            return;
        }

        // Get the actual language code from subtitle metadata
        String subtitleLanguage = getLanguageCodeFromSubtitle(subtitle);
        Log.i(TAG, "Selecting subtitle track: " + subtitle.getLanguage() + " (language code: " + subtitleLanguage + ")");

        // Check available tracks first
        Tracks tracks = activity.player.getCurrentTracks();
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
        TrackSelectionParameters.Builder builder = activity.player.getTrackSelectionParameters()
                .buildUpon()
                // make sure text renderer is enabled in case it was disabled previously
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                .setPreferredTextLanguage(subtitleLanguage)
                .setPreferredTextRoleFlags(C.ROLE_FLAG_SUBTITLE | C.ROLE_FLAG_CAPTION)
                .setSelectUndeterminedTextLanguage(true)
                .setIgnoredTextSelectionFlags(0); // Don't ignore any text tracks

        // Apply track selection parameters
        activity.player.setTrackSelectionParameters(builder.build());

        Log.i(TAG, "Subtitle track selection completed for: " + subtitle.getLanguage());
    }

    /**
     * Detect MIME type from subtitle file URL based on extension
     */
    String detectSubtitleMimeType(String subtitleUrl) {
        if (subtitleUrl == null) {
            return androidx.media3.common.MimeTypes.TEXT_VTT;
        }

        // strip query parameters to reliably inspect extension
        String path = subtitleUrl.split("\\?")[0].toLowerCase(Locale.US);

        if (path.endsWith(".srt")) {
            Log.i(TAG, "Detected subtitle format: SRT");
            return androidx.media3.common.MimeTypes.APPLICATION_SUBRIP;
        } else if (path.endsWith(".ssa") || path.endsWith(".ass")) {
            Log.i(TAG, "Detected subtitle format: SSA/ASS");
            return androidx.media3.common.MimeTypes.TEXT_SSA;
        } else if (path.endsWith(".ttml") || path.endsWith(".xml")) {
            Log.i(TAG, "Detected subtitle format: TTML");
            return androidx.media3.common.MimeTypes.APPLICATION_TTML;
        } else if (path.endsWith(".vtt")) {
            Log.i(TAG, "Detected subtitle format: WebVTT");
            return androidx.media3.common.MimeTypes.TEXT_VTT;
        } else {
            // Default to WebVTT and log a warning
            Log.w(TAG, "Unknown subtitle format, defaulting to WebVTT. URL: " + subtitleUrl);
            return androidx.media3.common.MimeTypes.TEXT_VTT;
        }
    }

    /**
     * Get language code from subtitle metadata, with fallback to 'und' (undetermined)
     */
    String getLanguageCodeFromSubtitle(MediaItems.SubtitleItem subtitle) {
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
    String extractLanguageCode(String languageLabel) {
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

    public void showSubtitleDialog() {
        if (activity.subtitles == null || activity.subtitles.isEmpty()) {
            fetchExternalSubtitles();
            return;
        }

        // prepare labels array with an explicit "None" option at index 0
        String[] subtitleLabels = new String[activity.subtitles.size() + 1];
        subtitleLabels[0] = "None";
        for (int i = 0; i < activity.subtitles.size(); i++) {
            subtitleLabels[i + 1] = activity.subtitles.get(i).getLanguage();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Subtitle");
        builder.setItems(subtitleLabels, (dialog, which) -> {
            if (which == 0) {
                // user chose "None" – clear selection
                activity.currentSubtitleIndex = -1;
                activity.btnSubtitles.setText("None");
            } else {
                activity.currentSubtitleIndex = which - 1;
                activity.btnSubtitles.setText(activity.subtitles.get(activity.currentSubtitleIndex).getLanguage());
            }
            // remember current playback position so we can resume after reload
            if (activity.player != null) {
                activity.startPosition = activity.player.getCurrentPosition();
            }
            // reload source so MediaItem is rebuilt without a subtitle track when none selected
            activity.playerCore.loadVideoSource(activity.currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        activity.controlsManager.applyFocusHighlight(dialog);
        dialog.show();
    }

    public void fetchExternalSubtitles() {
        if (activity.mediaItems == null || activity.mediaItems.getTmdbId() == null) {
            Toast.makeText(activity, "Cannot fetch subtitles: No TMDB ID", Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(TAG, "Fetching subtitles for TMDB ID: " + activity.mediaItems.getTmdbId());


        activity.controlsManager.showLoadingServer();
        activity.loadingStatusText.setText("FETCHING SUBTITLES...");

        SubdlService subdlService = new SubdlService(activity);
        SubdlService.Callback callback = new SubdlService.Callback() {
            @Override
            public void onSuccess(List<Subtitle> externalSubtitles) {
                activity.runOnUiThread(() -> {
                    activity.controlsManager.hideLoading();
                    if (externalSubtitles.isEmpty()) {
                        Toast.makeText(activity, "No external subtitles found", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Convert Subdl subtitles to MediaItems.SubtitleItem
                    activity.subtitles = new ArrayList<>();
                    for (Subtitle sub : externalSubtitles) {
                        MediaItems.SubtitleItem item = new MediaItems.SubtitleItem();
                        String fileName = sub.srtUri.getLastPathSegment();
                        item.setLanguage(sub.language + " (" + fileName + ")");
                        item.setUrl(sub.srtUri.toString());
                        item.setLang(sub.language);
                        activity.subtitles.add(item);
                    }

                    // reset any previously selected index – user will choose from new list
                    activity.currentSubtitleIndex = -1;
                    if (activity.btnSubtitles != null) {
                        activity.btnSubtitles.setText("None");
                    }

                    // Show dialog again with new subtitles
                    //showSubtitleDialog();
                });
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> {
                    activity.controlsManager.hideLoading();
                    Log.e(TAG, "Subdl error: " + error);
                    Toast.makeText(activity, "Error fetching subtitles: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        };

        try {
            int tmdbId = Integer.parseInt(activity.mediaItems.getTmdbId());
            if ("TV".equals(activity.mediaType)) {
                int season = Integer.parseInt(activity.mediaItems.getSeason());
                int episode = Integer.parseInt(activity.mediaItems.getEpisode());
                subdlService.fetchTvSubtitles(tmdbId, season, episode, "EN", callback);
            } else {
                subdlService.fetchSubtitles(tmdbId, "movie", "EN", callback);
            }
        } catch (NumberFormatException e) {
            activity.runOnUiThread(() -> {
                activity.controlsManager.hideLoading();
                Toast.makeText(activity, "Invalid TMDB ID or metadata", Toast.LENGTH_SHORT).show();
            });
        }
    }
}
