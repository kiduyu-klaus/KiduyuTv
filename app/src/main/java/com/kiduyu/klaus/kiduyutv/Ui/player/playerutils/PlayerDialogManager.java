package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.util.UnstableApi;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class PlayerDialogManager {

    private static final String TAG = "PlayerDialogManager";

    private final PlayerActivity activity;

    public PlayerDialogManager(PlayerActivity activity) {
        this.activity = activity;
    }

    public void showSpeedDialog() {
        float[] speeds = {0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f};
        String[] speedLabels = new String[speeds.length];

        for (int i = 0; i < speeds.length; i++) {
            speedLabels[i] = String.format(Locale.US, "%.2fx", speeds[i]);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Playback Speed");
        builder.setItems(speedLabels, (dialog, which) -> {
            activity.currentSpeed = speeds[which];
            if (activity.player != null) {
                activity.player.setPlaybackSpeed(activity.currentSpeed);
            }
            activity.btnSpeed.setText(String.format(Locale.US, "Speed %.2fx", activity.currentSpeed));
        });

        AlertDialog dialog = builder.create();
        activity.controlsManager.applyFocusHighlight(dialog);
        dialog.show();

    }

    public void showServerDialog() {
        if (activity.videoSources == null || activity.videoSources.isEmpty()) {
            Toast.makeText(activity, "No servers available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] serverLabels = new String[activity.videoSources.size()];
        for (int i = 0; i < activity.videoSources.size(); i++) {
            serverLabels[i] = activity.videoSources.get(i).getQuality();
            Log.i(TAG, "Server label: " + activity.videoSources.get(i).getQuality());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Select Server");
        builder.setItems(serverLabels, (dialog, which) -> {
            activity.currentSourceIndex = which;
            activity.startPosition = activity.player.getCurrentPosition();
            activity.playerCore.loadVideoSource(activity.currentSourceIndex);
        });

        AlertDialog dialog = builder.create();
        activity.controlsManager.applyFocusHighlight(dialog);
        dialog.show();
    }

    public void showQualityDialog() {
        if (activity.player == null) {
            activity.controlsManager.showToast("No player");
            return;
        }

        // Get available tracks to determine what qualities are available
        Tracks tracks = activity.player.getCurrentTracks();

        // Determine available heights from video tracks
        List<Integer> availableHeights = new ArrayList<>();
        if (tracks != null && !tracks.isEmpty()) {
            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                    for (int i = 0; i < trackGroup.length; i++) {
                        Format format = trackGroup.getTrackFormat(i);
                        if (format.height > 0 && !availableHeights.contains(format.height)) {
                            availableHeights.add(format.height);
                        }
                    }
                }
            }
        }

        // Sort heights in descending order
        Collections.sort(availableHeights, Collections.reverseOrder());

        // Find the highest quality available
        int highestHeight = availableHeights.isEmpty() ? 1080 : availableHeights.get(0);

        // Inflate custom layout
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_quality, null);

        // Get references to views
        RadioButton radioAuto = dialogView.findViewById(R.id.radioAuto);
        RadioButton radioHigh = dialogView.findViewById(R.id.radioHigh);
        RadioButton radioMedium = dialogView.findViewById(R.id.radioMedium);
        RadioButton radioLow = dialogView.findViewById(R.id.radioLow);

        LinearLayout optionAuto = dialogView.findViewById(R.id.optionAuto);
        LinearLayout optionHigh = dialogView.findViewById(R.id.optionHigh);
        LinearLayout optionMedium = dialogView.findViewById(R.id.optionMedium);
        LinearLayout optionLow = dialogView.findViewById(R.id.optionLow);

        // Set initial selection based on current quality
        switch (activity.currentQuality) {
            case "Auto":
                radioAuto.setChecked(true);
                break;
            case "High":
                radioHigh.setChecked(true);
                break;
            case "Medium":
                radioMedium.setChecked(true);
                break;
            case "Low":
                radioLow.setChecked(true);
                break;
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();

        // Set click listeners for each option
        optionAuto.setOnClickListener(v -> {
            selectQuality("Auto", 0, highestHeight);
            dialog.dismiss();
        });

        optionHigh.setOnClickListener(v -> {
            selectQuality("High", highestHeight, highestHeight);
            dialog.dismiss();
        });

        optionMedium.setOnClickListener(v -> {
            selectQuality("Medium", 720, highestHeight);
            dialog.dismiss();
        });

        optionLow.setOnClickListener(v -> {
            selectQuality("Low", 360, highestHeight);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Select video quality and update track selection
     * @param qualityName Quality level: Auto, High, Medium, Low
     * @param targetHeight Target height in pixels (0 for Auto)
     * @param highestAvailable Highest quality available in the stream
     */
    void selectQuality(String qualityName, int targetHeight, int highestAvailable) {
        activity.currentQuality = qualityName;
        activity.currentQualityHeight = targetHeight;

        // Update button text
        String buttonText = qualityName;
        if (qualityName.equals("High")) {
            buttonText = "High " + highestAvailable + "p";
        } else if (qualityName.equals("Medium")) {
            buttonText = "Medium 720p";
        } else if (qualityName.equals("Low")) {
            buttonText = "Low 360p";
        }
        activity.btnQuality.setText(buttonText);

        if (activity.player == null) {
            return;
        }

        if (qualityName.equals("Auto")) {
            // Enable auto quality - clear overrides and let player decide
            TrackSelectionParameters params = activity.player.getTrackSelectionParameters()
                    .buildUpon()
                    .clearOverrides()
                    .build();
            activity.player.setTrackSelectionParameters(params);
            activity.controlsManager.showToast("Quality: Auto (best network quality)");
        } else {
            // Find the track closest to target height
            Tracks tracks = activity.player.getCurrentTracks();
            if (tracks == null || tracks.isEmpty()) {
                activity.controlsManager.showToast("No tracks available");
                return;
            }

            int selectedHeight = 0;
            TrackSelectionOverride selectedOverride = null;

            for (Tracks.Group trackGroup : tracks.getGroups()) {
                if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                    TrackGroup mediaTrackGroup = trackGroup.getMediaTrackGroup();

                    for (int i = 0; i < trackGroup.length; i++) {
                        Format format = trackGroup.getTrackFormat(i);
                        int height = format.height;

                        if (height > 0) {
                            // For High: select highest available
                            // For Medium: select 720 or closest below
                            // For Low: select 360 or closest below
                            boolean shouldSelect = false;

                            if (qualityName.equals("High")) {
                                // Select highest quality available
                                if (height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            } else if (qualityName.equals("Medium")) {
                                // Select 720p or closest below
                                if (height <= 720 && height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            } else if (qualityName.equals("Low")) {
                                // Select 360p or closest below
                                if (height <= 360 && height > selectedHeight) {
                                    selectedHeight = height;
                                    shouldSelect = true;
                                }
                            }

                            if (shouldSelect) {
                                selectedOverride = new TrackSelectionOverride(
                                        mediaTrackGroup,
                                        Collections.singletonList(i)
                                );
                            }
                        }
                    }
                }
            }

            if (selectedOverride != null) {
                TrackSelectionParameters params = activity.player.getTrackSelectionParameters()
                        .buildUpon()
                        .clearOverrides()
                        .addOverride(selectedOverride)
                        .build();
                activity.player.setTrackSelectionParameters(params);
                activity.controlsManager.showToast("Quality: " + qualityName + " " + selectedHeight + "p");
            } else {
                // If the requested quality is not available, fall back to auto
                activity.controlsManager.showToast("Quality " + targetHeight + "p not available, using Auto");
                selectQuality("Auto", 0, highestAvailable);
            }
        }
    }

    // Helper method to switch tracks
    void switchToTrack(TrackSelectionOverride override) {
        if (activity.player == null) return;

        TrackSelectionParameters.Builder parametersBuilder = activity.player.getTrackSelectionParameters()
                .buildUpon()
                .clearOverrides();

        if (override != null) {
            // Specific quality selected
            parametersBuilder.addOverride(override);
            activity.controlsManager.showToast("Quality changed to " + override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");
            Log.i(TAG, "Quality changed to " + override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");

            activity.btnQuality.setText(override.mediaTrackGroup.getFormat(override.trackIndices.get(0)).height + "p");
        } else {
            // Auto quality selected
            activity.controlsManager.showToast("Auto quality selected");
        }

        activity.player.setTrackSelectionParameters(parametersBuilder.build());
    }

    void showCurrentTrackInfo() {
        String info = getCurrentTrackInfo();
        Toast.makeText(activity, info, Toast.LENGTH_SHORT).show();
    }

    String getCurrentTrackInfo() {
        if (activity.player == null) {
            return "No player";
        }

        Tracks tracks = activity.player.getCurrentTracks();
        if (tracks == null || tracks.isEmpty()) {
            return "No tracks";
        }

        StringBuilder info = new StringBuilder();
        info.append("Current Tracks:\n");

        for (Tracks.Group trackGroup : tracks.getGroups()) {
            int trackType = trackGroup.getType();

            for (int i = 0; i < trackGroup.length; i++) {
                if (trackGroup.isTrackSelected(i)) {
                    // Access format directly
                    int width = trackGroup.getTrackFormat(i).width;
                    int height = trackGroup.getTrackFormat(i).height;
                    int bitrate = trackGroup.getTrackFormat(i).bitrate;
                    int sampleRate = trackGroup.getTrackFormat(i).sampleRate;
                    String language = trackGroup.getTrackFormat(i).language;

                    if (trackType == C.TRACK_TYPE_VIDEO) {
                        info.append("Video: ");
                        if (width > 0 && height > 0) {
                            info.append(width).append("x").append(height);
                        } else {
                            info.append("Auto");
                        }
                        if (bitrate > 0) {
                            info.append(" (").append(bitrate / 1000).append(" kbps)");
                        }
                        info.append("\n");
                    } else if (trackType == C.TRACK_TYPE_AUDIO) {
                        info.append("Audio: ").append(sampleRate > 0 ? sampleRate + " Hz" : "Unknown");
                        if (bitrate > 0) {
                            info.append(" (").append(bitrate / 1000).append(" kbps)");
                        }
                        info.append("\n");
                    } else if (trackType == C.TRACK_TYPE_TEXT) {
                        info.append("Subtitle: ").append(language != null ? language : "Unknown");
                        info.append("\n");
                    }
                }
            }
        }
        Log.i(TAG, "Current Tracks: " + info.toString());

        return info.toString();
    }

    void updateServerButton() {
        activity.btnServer.setText(activity.videoSources.get(activity.currentSourceIndex).getQuality());
    }

    void updateQualityButton() {
        // Use the current quality selection to update button text
        switch (activity.currentQuality) {
            case "Auto":
                activity.btnQuality.setText("Auto");
                break;
            case "High":
                // Try to find the highest available quality
                if (activity.player != null) {
                    Tracks tracks = activity.player.getCurrentTracks();
                    if (tracks != null && !tracks.isEmpty()) {
                        int highestHeight = 0;
                        for (Tracks.Group trackGroup : tracks.getGroups()) {
                            if (trackGroup.getType() == C.TRACK_TYPE_VIDEO) {
                                for (int i = 0; i < trackGroup.length; i++) {
                                    Format format = trackGroup.getTrackFormat(i);
                                    if (format.height > highestHeight) {
                                        highestHeight = format.height;
                                    }
                                }
                            }
                        }
                        if (highestHeight > 0) {
                            activity.btnQuality.setText("High " + highestHeight + "p");
                        } else {
                            activity.btnQuality.setText("High");
                        }
                    } else {
                        activity.btnQuality.setText("High");
                    }
                } else {
                    activity.btnQuality.setText("High");
                }
                break;
            case "Medium":
                activity.btnQuality.setText("Medium 720p");
                break;
            case "Low":
                activity.btnQuality.setText("Low 360p");
                break;
            default:
                activity.btnQuality.setText("Auto");
                break;
        }
    }
}
