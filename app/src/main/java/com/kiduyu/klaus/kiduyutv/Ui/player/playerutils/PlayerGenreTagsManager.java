package com.kiduyu.klaus.kiduyutv.Ui.player.playerutils;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.util.UnstableApi;

import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@UnstableApi
public class PlayerGenreTagsManager {

    private static final String TAG = "PlayerGenreTagsManager";

    private final PlayerActivity activity;

    public PlayerGenreTagsManager(PlayerActivity activity) {
        this.activity = activity;
    }

    /**
     * Update anime server button labels
     */
    void updateAnimeServerButtons() {
        if (activity.currentServerType != null && activity.serverCounts != null) {
            // Update Audio button to show current server type
            String typeLabel = activity.currentServerType.toUpperCase();
            activity.btnAudio.setText(typeLabel);

            // Update Server button to show current server index
            Integer count = activity.serverCounts.get(activity.currentServerType);
            if (count != null && count > 1) {
                activity.btnServer.setText(activity.videoSources.get(activity.currentSourceIndex).getQuality());
            } else {
                activity.btnServer.setText("Server 1");
            }

            // Update hardsub badge to show server type for anime content
            activity.hardsubBadge.setText(typeLabel);
            activity.hardsubBadge.setVisibility(View.VISIBLE);

            Log.i(TAG, "Updated buttons: Type=" + typeLabel + ", Server=" + (activity.currentServerIndex + 1));
        }
    }

    /**
     * Update hardsub badge based on media type for non-anime content
     */
    public void updateHardsubBadgeForMediaType() {
        if ("anime".equals(activity.mediaType)) {
            // For anime, badge will be updated via updateAnimeServerButtons() based on server type
            activity.hardsubBadge.setVisibility(View.GONE);
        } else {
            // For non-anime (movies, TV shows), set badge based on media type
            if (activity.mediaItems != null) {
                activity.hardsubBadge.setText(activity.mediaType);
            } else {
                activity.hardsubBadge.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Populate genre tags from MediaItems genres list
     */
    public void populateGenreTags() {
        if (activity.mediaItems == null) {
            hideAllGenreTags();
            return;
        }

        List<String> genres = activity.mediaItems.getGenres();
        if (genres == null || genres.isEmpty()) {
            hideAllGenreTags();
            return;
        }

        Log.i(TAG, "Populating " + genres.size() + " genre tags");

        // Create array of tag TextViews
        TextView[] tags = {activity.tag1, activity.tag2, activity.tag3, activity.tag4, activity.tag5};

        // Hide all tags first
        for (TextView tag : tags) {
            tag.setVisibility(View.GONE);
        }

        // Populate tags with genre names (up to 5 genres)
        int tagIndex = 0;
        for (String genre : genres) {
            if (tagIndex >= tags.length) {
                break; // Only show first 5 genres
            }

            if (genre != null && !genre.isEmpty()) {
                tags[tagIndex].setText(genre);
                tags[tagIndex].setVisibility(View.VISIBLE);
                Log.i(TAG, "Set tag" + (tagIndex + 1) + " to: " + genre);
                tagIndex++;
            }
        }

        // Hide remaining unused tags
        for (int i = tagIndex; i < tags.length; i++) {
            tags[i].setVisibility(View.GONE);
        }
    }

    /**
     * Populate genre tags for anime content using description string
     * Anime API doesn't provide genres in a structured list, so we extract from description
     */
    void populateAnimeGenreTags(String animeDescription) {
        // Common anime genres to look for in descriptions
        List<String> commonGenres = new ArrayList<>();
        commonGenres.add("Action");
        commonGenres.add("Adventure");
        commonGenres.add("Comedy");
        commonGenres.add("Drama");
        commonGenres.add("Ecchi");
        commonGenres.add("Fantasy");
        commonGenres.add("Horror");
        commonGenres.add("Mahou Shoujo");
        commonGenres.add("Mecha");
        commonGenres.add("Music");
        commonGenres.add("Mystery");
        commonGenres.add("Psychological");
        commonGenres.add("Romance");
        commonGenres.add("Sci-Fi");
        commonGenres.add("Slice of Life");
        commonGenres.add("Sports");
        commonGenres.add("Supernatural");
        commonGenres.add("Thriller");

        // Create array of tag TextViews
        TextView[] tags = {activity.tag1, activity.tag2, activity.tag3, activity.tag4, activity.tag5};

        // Hide all tags first
        for (TextView tag : tags) {
            tag.setVisibility(View.GONE);
        }

        // Try to find genres in description
        List<String> foundGenres = new ArrayList<>();

        if (animeDescription != null) {
            String lowerDesc = animeDescription.toLowerCase(Locale.US);

            for (String genre : commonGenres) {
                if (foundGenres.size() >= 5) {
                    break; // Only need 5 genres
                }

                if (lowerDesc.contains(genre.toLowerCase())) {
                    foundGenres.add(genre);
                    Log.i(TAG, "Found genre in description: " + genre);
                }
            }
        }

        // If no genres found in description, set default anime genres
        if (foundGenres.isEmpty()) {
            foundGenres.add("Anime");
            foundGenres.add("Animation");
            Log.i(TAG, "No genres found in description, using defaults");
        }

        // Populate tags
        int tagIndex = 0;
        for (String genre : foundGenres) {
            if (tagIndex >= tags.length) {
                break;
            }
            tags[tagIndex].setText(genre);
            tags[tagIndex].setVisibility(View.VISIBLE);
            Log.i(TAG, "Set anime tag" + (tagIndex + 1) + " to: " + genre);
            tagIndex++;
        }

        // Hide remaining unused tags
        for (int i = tagIndex; i < tags.length; i++) {
            tags[i].setVisibility(View.GONE);
        }
    }

    /**
     * Hide all genre tag TextViews
     */
    void hideAllGenreTags() {
        activity.tag1.setVisibility(View.GONE);
        activity.tag2.setVisibility(View.GONE);
        activity.tag3.setVisibility(View.GONE);
        activity.tag4.setVisibility(View.GONE);
        activity.tag5.setVisibility(View.GONE);
    }

    /**
     * Switch to a specific server
     */
    void switchToServer(String serverType, int serverIndex) {


        Log.i(TAG, "Switching to " + serverType + " Server " + (serverIndex + 1));

        long currentPosition = activity.player != null ? activity.player.getCurrentPosition() : 0;
        boolean wasPlaying = activity.player != null && activity.player.isPlaying();

        activity.loadingIndicator.setVisibility(View.VISIBLE);
        activity.loadingStatusContainer.setVisibility(View.VISIBLE);
        activity.loadingStatusText.setText("Loading " + serverType.toUpperCase() +
                " Server " + (serverIndex + 1) + "...");
        activity.loadingIndicator.setVisibility(View.GONE);
        activity.loadingStatusContainer.setVisibility(View.GONE);




    }

    void playNextEpisode() {
        if (activity.mediaItems == null || activity.mediaItems.getTmdbId() == null) {
            activity.nextEpisodeTriggered = false;
            return;
        }

        String currentSeasonStr = activity.mediaItems.getSeason();
        String currentEpisodeStr = activity.mediaItems.getEpisode();

        if (currentSeasonStr == null || currentEpisodeStr == null) return;

        try {
            int currentSeason = Integer.parseInt(currentSeasonStr);
            int currentEpisode = Integer.parseInt(currentEpisodeStr);

            activity.controlsManager.showLoadingServer();
            activity.loadingStatusText.setText("CHECKING NEXT EPISODE...");

            TmdbRepository tmdbRepository = new TmdbRepository();
            tmdbRepository.getSeasonEpisodes(activity.mediaItems.getTmdbId(), currentSeason, new TmdbRepository.EpisodesCallback() {
                @Override
                public void onSuccess(List<com.kiduyu.klaus.kiduyutv.model.Episode> episodes) {
                    int totalEpisodes = episodes.size();
                    if (totalEpisodes > currentEpisode) {
                        int nextEpisodeNumber = currentEpisode + 1;
                        fetchNextEpisodeStreams(activity.mediaItems.getTmdbId(), String.valueOf(currentSeason), String.valueOf(nextEpisodeNumber));
                    } else {
                        activity.runOnUiThread(() -> {
                            activity.controlsManager.hideLoading();
                            Toast.makeText(activity, "No more episodes in this season", Toast.LENGTH_SHORT).show();
                        });
                    }
                }

                @Override
                public void onError(String error) {
                    activity.runOnUiThread(() -> {
                        activity.nextEpisodeTriggered = false; // Reset flag to allow retry
                        activity.controlsManager.hideLoading();
                        Log.e(TAG, "Error checking next episode: " + error);
                    });
                }
            });

        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing season/episode: " + e.getMessage());
        }
    }


    void fetchNextEpisodeStreams(String tmdbId, String season, String episodeNumber) {
        activity.runOnUiThread(() -> activity.loadingStatusText.setText("FETCHING NEXT EPISODE..."));

        FetchStreams fetchStreams = new FetchStreams(activity);
        fetchStreams.fetchHexaTV(tmdbId, season, episodeNumber, new FetchStreams.StreamCallback() {
            @Override
            public void onSuccess(MediaItems nextMedia) {
                if (nextMedia.getVideoSources() != null && !nextMedia.getVideoSources().isEmpty()) {
                    // Update current media items with next episode info - matching DetailsActivityTv intent extras
                    activity.mediaItems.setVideoSources(nextMedia.getVideoSources());
                    activity.mediaItems.setSubtitles(nextMedia.getSubtitles());
                    activity.mediaItems.setSeason(season);
                    activity.mediaItems.setEpisode(episodeNumber);

                    // Set mediaType to TV (matching DetailsActivityTv intent extras)
                    activity.mediaItems.setMediaType("tv");

                    // Set backgroundImageUrl to posterUrl (matching DetailsActivityTv line 297)
                    // This ensures the player has the correct background image for the new episode
                    if (activity.mediaItems.getPosterUrl() != null) {
                        activity.mediaItems.setBackgroundImageUrl(activity.mediaItems.getPosterUrl());
                    }

                    // Instead of just updating the info, restart the activity fresh
                    // This ensures all initialization logic runs exactly like DetailsActivityTv launched it
                    activity.runOnUiThread(() -> {
                        activity.nextEpisodeTriggered = false; // Reset flag for the next episode

                        // Restart the activity with the updated mediaItems (fresh start)
                        Intent restartIntent = new Intent(activity, PlayerActivity.class);
                        restartIntent.putExtra("media_item", activity.mediaItems);
                        restartIntent.putExtra("media_type", "tv");
                        restartIntent.putExtra("start_position", 0); // Start from beginning for new episode

                        // Clear any existing activity stack and start fresh
                        restartIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(restartIntent);

                        // Finish this instance so pressing back goes to DetailsActivityTv
                        activity.finish();

                        Toast.makeText(activity, "Playing Episode " + episodeNumber, Toast.LENGTH_SHORT).show();
                    });
                } else {
                    activity.runOnUiThread(() -> {
                        activity.controlsManager.hideLoading();
                        Toast.makeText(activity, "No streams found for next episode", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(String error) {
                activity.runOnUiThread(() -> {
                    activity.nextEpisodeTriggered = false; // Reset flag to allow retry if needed
                    activity.controlsManager.hideLoading();
                    Log.e(TAG, "Error fetching next episode streams: " + error);
                    Toast.makeText(activity, "Failed to load next episode", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
