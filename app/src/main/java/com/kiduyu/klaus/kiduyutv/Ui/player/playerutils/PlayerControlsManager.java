package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.util.UnstableApi;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;

import java.util.Locale;

@UnstableApi
public class PlayerControlsManager {

    private static final String TAG = "PlayerControlsManager";

    private final PlayerActivity activity;

    public PlayerControlsManager(PlayerActivity activity) {
        this.activity = activity;
    }

    public void togglePlayPause() {
        if (activity.player == null) return;

        if (activity.player.isPlaying()) {
            activity.player.pause();
        } else {
            activity.player.play();
            activity.dialogManager.showCurrentTrackInfo();
        }
        showControls();
    }

    public void toggleControls() {
        if (activity.controlsVisible) {
            hideControls();
        } else {
            showControls();
        }
    }

    public void showControls() {
        activity.topInfoContainer.setVisibility(View.VISIBLE);
        activity.bottomControlsContainer.setVisibility(View.VISIBLE);
        //btnBack.setVisibility(View.VISIBLE);
        activity.controlsVisible = true;

        // Set focus to Settings button by default
        activity.btnSettings.requestFocus();

        resetHideControlsTimer();
    }

    public void resetHideControlsTimer() {
        activity.hideControlsHandler.removeCallbacks(activity.hideControlsTask);
        if (activity.player.isPlaying()) {
            activity.hideControlsHandler.postDelayed(activity.hideControlsTask, 6000);
        }
    }

    public void hideControls() {
        activity.topInfoContainer.setVisibility(View.GONE);
        activity.bottomControlsContainer.setVisibility(View.GONE);
        //btnEpisodes.setVisibility(View.GONE);
        //btnBack.setVisibility(View.GONE);
        activity.controlsVisible = false;
    }

    void updatePlayPauseButton(boolean isPlaying) {
        if (isPlaying) {
            activity.btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
            activity.centerPauseIcon.setVisibility(View.GONE);
        } else {
            activity.btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            activity.centerPauseIcon.setVisibility(View.VISIBLE);
        }
    }

    void updateLoadingUi(boolean isLoading) {
        if (isLoading) {
            // Hide play/pause button and current time when loading
            activity.btnPlayPause.setVisibility(View.GONE);
            activity.currentTime.setVisibility(View.GONE);
            // Show loading status in the same location
            activity.loadingStatusContainer.setVisibility(View.VISIBLE);
        } else {
            // Hide loading/streaming status
            activity.loadingStatusContainer.setVisibility(View.GONE);
            // Show play/pause button and current time immediately
            activity.btnPlayPause.setVisibility(View.VISIBLE);
            activity.currentTime.setVisibility(View.VISIBLE);
        }
    }

    void showLoadingServer() {
        updateLoadingUi(true);
        // Hide play/pause button and current time when loading
        activity.btnPlayPause.setVisibility(View.GONE);
        activity.currentTime.setVisibility(View.GONE);
        activity.loadingStatusText.setText("LOADING SERVER");
    }

    void showStreamingStatus() {
        updateLoadingUi(true);
        // Hide play/pause button and current time when loading
        activity.btnPlayPause.setVisibility(View.GONE);
        activity.currentTime.setVisibility(View.GONE);
        activity.loadingStatusText.setText("STREAMING VIDEO");
    }

    void hideLoading() {
        activity.loadingIndicator.setVisibility(View.GONE);
        activity.btnPlayPause.setVisibility(View.VISIBLE);
        activity.currentTime.setVisibility(View.VISIBLE);
    }

    void hideStreamingStatus() {
        activity.loadingStatusContainer.setVisibility(View.GONE);
        activity.btnPlayPause.setVisibility(View.VISIBLE);
        activity.currentTime.setVisibility(View.VISIBLE);
    }

    public void updateTimeDisplay() {
        if (activity.player != null) {
            int currentPos = (int) activity.player.getCurrentPosition();
            long duration = activity.player.getDuration();

            // Update current time text
            activity.currentTime.setText(activity.playerCore.formatTime(currentPos));
//            if (currentTime.getVisibility() == View.GONE) {
//                currentTime.setVisibility(View.VISIBLE);
//            }

            // Update progress in milliseconds (matching the max value)
            if (duration > 0) {
                activity.progressBar.setProgress(currentPos);

                // Check if 3 minutes (180,000 ms) are remaining
                if ("TV".equals(activity.mediaType) && !activity.nextEpisodeTriggered) {
                    long remainingTime = duration - currentPos;
                    if (remainingTime <= 180000 && remainingTime > 0) {
                        activity.nextEpisodeTriggered = true;
                        activity.genreTagsManager.playNextEpisode();
                    }
                }
            }

            // Update buffered progress display
            activity.playerCore.updateBufferProgress();
            hideLoading();
            updateLoadingUi(false);
        }
    }

    void updateTimeTexts() {
        if (activity.player == null) return;
        activity.currentTime.setText(activity.playerCore.formatTime(activity.player.getCurrentPosition()));
        activity.totalTime.setText(activity.playerCore.formatTime(activity.player.getDuration()));
    }

    public void showToast(String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    void applyFocusHighlight(AlertDialog dialog) {
        dialog.setOnShowListener(d -> {
            ListView listView = dialog.getListView();
            if (listView != null) {
                // Set the selector directly on the ListView
                listView.setSelector(R.drawable.dialog_item_focus_selector);
                listView.setDrawSelectorOnTop(false);
                listView.setItemsCanFocus(true);

                listView.setOnHierarchyChangeListener(new ViewGroup.OnHierarchyChangeListener() {
                    @Override
                    public void onChildViewAdded(View parent, View child) {
                        child.setFocusable(true);
                        child.setOnFocusChangeListener((v, hasFocus) -> {
                            if (hasFocus) {
                                // Sync ListView selection with focus for D-pad
                                int position = listView.getPositionForView(v);
                                if (position != ListView.INVALID_POSITION) {
                                    listView.setSelection(position);
                                }
                            }
                        });
                        // REMOVE the manual setOnClickListener here!
                    }

                    @Override
                    public void onChildViewRemoved(View parent, View child) {
                    }
                });
            }
        });
    }
}
