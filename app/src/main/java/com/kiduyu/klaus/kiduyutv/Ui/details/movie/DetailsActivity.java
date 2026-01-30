package com.kiduyu.klaus.kiduyutv.Ui.details.movie;


import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.CastRepository;
import com.kiduyu.klaus.kiduyutv.Api.FetchStreams;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.actor.ActorDetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.adapter.CastAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.RecommendationsAdapter;
import com.kiduyu.klaus.kiduyutv.model.CastMember;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;
import com.kiduyu.klaus.kiduyutv.utils.utils;

import java.util.ArrayList;
import java.util.List;

public class DetailsActivity extends AppCompatActivity {
    private static final String TAG = "DetailsActivity";

    // UI Components
    private MediaItems mediaItems;
    private ImageView backdropImageView;
    private TextView titleTextView;
    private TextView descriptionTextView;
    private TextView yearTextView;
    private TextView ratingTextView;
    private TextView durationTextView;
    private TextView genresTextView;
    private TextView creatorsTextView;
    private TextView starsTextView;
    private AppCompatButton playButton;
    private AppCompatButton favoriteButton;
    private ProgressBar loadingProgressBar;
    // Watch history
    private PreferencesManager preferencesManager;
    private long savedPosition = 0;
    private boolean hasWatchHistory = false;
    private RelativeLayout loadingOverlay;
    private TextView loadingText;
    private RecyclerView recommendationsRecyclerView;
    private TextView recommendationsTitle;
    private ProgressBar recommendationsLoadingBar;

    // Cast section components
    private RecyclerView castRecyclerView;
    private ProgressBar castLoadingBar;
    private TextView emptyCastText;
    private CastAdapter castAdapter;
    private CastRepository castRepository;

    // FetchStreams for video source fetching
    private FetchStreams fetchStreams;
    private int currentServerIndex = 0;
    private List<ServerSource> serverSources;

    private TmdbRepository mediaRepository;
    private RecommendationsAdapter recommendationsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_details);

        mediaItems = getIntent().getParcelableExtra("media_item");

        if (mediaItems == null) {
            finish();
            return;
        }

        Log.i("PlayerActivity", "Loading media:\n" + mediaItems.toString());

        // Initialize FetchStreams instance
        initializeFetchStreams();

        mediaRepository = new TmdbRepository();
        castRepository = new CastRepository();
        // Initialize PreferencesManager and check watch history
        preferencesManager = PreferencesManager.getInstance(this);


        initializeViews();
        setupViews();
        setupClickListeners();
        setupCastSection();
        setupRecommendations();
        checkWatchHistory();
        loadCast();
        loadRecommendations();
    }

    /**
     * Initialize FetchStreams instance and setup server sources
     */
    private void initializeFetchStreams() {
        fetchStreams = new FetchStreams(this);
        setupServerSources();
    }

    /**
     * Setup the list of servers to try in order
     */
    private void setupServerSources() {
        serverSources = new ArrayList<>();

        // Add servers in priority order
        serverSources.add(new ServerSource("Videasy", ServerType.VIDEASY));
        serverSources.add(new ServerSource("Vidlink", ServerType.VIDLINK));
        serverSources.add(new ServerSource("Hexa", ServerType.HEXA));
        serverSources.add(new ServerSource("OneTouchTV", ServerType.ONETOUCHTV));
        serverSources.add(new ServerSource("Smashystream Type 1", ServerType.SMASHYSTREAM_TYPE1));
        serverSources.add(new ServerSource("Smashystream Type 2", ServerType.SMASHYSTREAM_TYPE2));
    }

    private void initializeViews() {
        backdropImageView = findViewById(R.id.backdropImageView);
        titleTextView = findViewById(R.id.titleTextView);
        descriptionTextView = findViewById(R.id.descriptionTextView);
        yearTextView = findViewById(R.id.yearTextView);
        ratingTextView = findViewById(R.id.ratingTextView);
        durationTextView = findViewById(R.id.durationTextView);
        genresTextView = findViewById(R.id.genresTextView);
        creatorsTextView = findViewById(R.id.creatorsTextView);
        starsTextView = findViewById(R.id.starsTextView);
        playButton = findViewById(R.id.playButton);
        favoriteButton = findViewById(R.id.favoriteButton);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText); // Add this to your layout
        recommendationsRecyclerView = findViewById(R.id.recommendationsRecyclerView);
        recommendationsTitle = findViewById(R.id.recommendationsTitle);
        recommendationsLoadingBar = findViewById(R.id.recommendationsLoadingBar);

        // Cast section views
        castRecyclerView = findViewById(R.id.castRecyclerView);
        castLoadingBar = findViewById(R.id.castLoadingBar);
        emptyCastText = findViewById(R.id.emptyCastText);
    }

    private void setupViews() {
        if (mediaItems.getBackgroundImageUrl() != null) {
            Glide.with(this)
                    .load(mediaItems.getBackgroundImageUrl())
                    .centerCrop()
                    .into(backdropImageView);
        }

        titleTextView.setText(mediaItems.getTitle());
        descriptionTextView.setText(mediaItems.getDescription());

        if (mediaItems.getYear() > 0) {
            yearTextView.setText(String.valueOf(mediaItems.getYear()));
        } else {
            yearTextView.setVisibility(View.GONE);
        }

        if (mediaItems.getRating() > 0) {
            ratingTextView.setText(String.format("%.1f", mediaItems.getRating()));
        } else {
            ratingTextView.setVisibility(View.GONE);
        }

        if (mediaItems.getDuration() != null && !mediaItems.getDuration().isEmpty()) {
            durationTextView.setText(mediaItems.getDuration());
        } else {
            durationTextView.setVisibility(View.GONE);
        }

        if (mediaItems.getGenres() != null && !mediaItems.getGenres().isEmpty()) {
            genresTextView.setText(String.join(" • ", mediaItems.getGenres()));
        } else if (mediaItems.getGenre() != null && !mediaItems.getGenre().isEmpty()) {
            genresTextView.setText(mediaItems.getGenre());
        } else {
            genresTextView.setVisibility(View.GONE);
        }

        creatorsTextView.setText("Loading...");
        starsTextView.setText("Loading...");
    }

    /**
     * Setup the cast section with horizontal RecyclerView
     */
    private void setupCastSection() {
        LinearLayoutManager castLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        castRecyclerView.setLayoutManager(castLayoutManager);
        castRecyclerView.setHasFixedSize(true);

        castAdapter = new CastAdapter();
        castRecyclerView.setAdapter(castAdapter);

        castAdapter.setOnCastClickListener(this::openActorDetails);
    }

    /**
     * Open actor details activity when a cast member is clicked
     */
    private void openActorDetails(CastMember castMember, int position) {
        Intent intent = new Intent(this, ActorDetailsActivity.class);
        intent.putExtra("cast_member", castMember);
        startActivity(intent);
    }

    /**
     * Load cast members for the movie
     */
    private void loadCast() {
        if (mediaItems.getTmdbId() == null || mediaItems.getTmdbId().isEmpty()) {
            showEmptyCast();
            return;
        }

        castLoadingBar.setVisibility(View.VISIBLE);
        castRecyclerView.setVisibility(View.GONE);
        emptyCastText.setVisibility(View.GONE);

        castRepository.getMovieCast(mediaItems.getTmdbId(), new CastRepository.CastListCallback() {
            @Override
            public void onSuccess(List<CastMember> castList) {
                castLoadingBar.setVisibility(View.GONE);

                if (castList != null && !castList.isEmpty()) {
                    castAdapter.setCastList(castList);
                    castRecyclerView.setVisibility(View.VISIBLE);
                    emptyCastText.setVisibility(View.GONE);
                } else {
                    showEmptyCast();
                }
            }

            @Override
            public void onError(String error) {
                castLoadingBar.setVisibility(View.GONE);
                Log.e(TAG, "Error loading cast: " + error);
                showEmptyCast();
            }
        });
    }

    /**
     * Show empty cast state
     */
    private void showEmptyCast() {
        castRecyclerView.setVisibility(View.GONE);
        emptyCastText.setVisibility(View.VISIBLE);
    }

    private void setupRecommendations() {
        recommendationsRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        recommendationsAdapter = new RecommendationsAdapter(this, item -> {
            Intent intent = new Intent(DetailsActivity.this, DetailsActivity.class);
            intent.putExtra("media_item", item);
            startActivity(intent);
        });
        recommendationsRecyclerView.setAdapter(recommendationsAdapter);
    }

    private void loadRecommendations() {
        if (mediaItems.getTmdbId() == null || mediaItems.getTmdbId().isEmpty()) {
            recommendationsTitle.setVisibility(View.GONE);
            recommendationsRecyclerView.setVisibility(View.GONE);
            return;
        }

        recommendationsLoadingBar.setVisibility(View.VISIBLE);

        mediaRepository.getRecommendationsAsync(
                mediaItems.getTmdbId(),
                mediaItems.getMediaType() != null ? mediaItems.getMediaType() : "movie",
                new TmdbRepository.TMDBCallback() {
                    @Override
                    public void onSuccess(List<MediaItems> recommendations) {
                        recommendationsLoadingBar.setVisibility(View.GONE);

                        if (recommendations != null && !recommendations.isEmpty()) {
                            recommendationsTitle.setVisibility(View.VISIBLE);
                            recommendationsRecyclerView.setVisibility(View.VISIBLE);
                            recommendationsAdapter.setItems(recommendations);
                        } else {
                            recommendationsTitle.setVisibility(View.GONE);
                            recommendationsRecyclerView.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onError(String error) {
                        recommendationsLoadingBar.setVisibility(View.GONE);
                        recommendationsTitle.setVisibility(View.GONE);
                        recommendationsRecyclerView.setVisibility(View.GONE);
                    }
                }
        );
    }

    private void setupClickListeners() {
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };

        playButton.setOnClickListener(v -> {
            // Check if we need to fetch video sources
            fetchVideoSources();
        });
        playButton.setOnFocusChangeListener(focusChangeListener);

        favoriteButton.setOnClickListener(v -> Toast.makeText(DetailsActivity.this,
                "Added to favorites",
                Toast.LENGTH_SHORT).show());
        favoriteButton.setOnFocusChangeListener(focusChangeListener);
    }

    /**
     * Fetch video sources from multiple servers sequentially
     */
    /**
     * Fetch video sources from all movie servers (similar to fetchAllTVStreams)
     */
    private void fetchVideoSources() {
        loadingOverlay.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);

        String title = mediaItems.getTitle();
        String year = String.valueOf(mediaItems.getYear());
        String tmdbId = mediaItems.getId();
        String imdbId = mediaItems.getId(); // Assuming this contains IMDB ID

        Log.i(TAG, "Fetching video sources for: " + title + " (" + year + ") [" + tmdbId + "]");

        final List<MediaItems.VideoSource> allVideoSources = new ArrayList<>();
        final List<MediaItems.SubtitleItem> allSubtitles = new ArrayList<>();

        // Counter to track completed fetches
        final int[] completedFetches = {0};
        final int totalServers = 6; // Number of servers we're querying

        // Callback to handle each server response
        FetchStreams.StreamCallback callback = new FetchStreams.StreamCallback() {
            @Override
            public void onSuccess(MediaItems item) {
                // Add video sources
                if (item.getVideoSources() != null && !item.getVideoSources().isEmpty()) {
                    allVideoSources.addAll(item.getVideoSources());
                    Log.i(TAG, "Added " + item.getVideoSources().size() + " sources");
                    for (MediaItems.VideoSource source : item.getVideoSources()) {
                        Log.i(TAG, "Source: " + source.getUrl());
                        Log.i(TAG, "Quality: " + source.getQuality());
                    }
                }

                // Add subtitles
                if (item.getSubtitles() != null && !item.getSubtitles().isEmpty()) {
                    allSubtitles.addAll(item.getSubtitles());
                    Log.i(TAG, "Added " + item.getSubtitles().size() + " subtitles");
                }

                // Copy session data from first successful source
                if (mediaItems.getSessionCookie() == null && item.getSessionCookie() != null) {
                    mediaItems.setSessionCookie(item.getSessionCookie());
                    mediaItems.setCustomHeaders(item.getCustomHeaders());
                    mediaItems.setRefererUrl(item.getRefererUrl());
                    mediaItems.setResponseHeaders(item.getResponseHeaders());
                }
                mediaItems.setBackgroundImageUrl(mediaItems.getBackgroundImageUrl());

                checkAndProceed();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Server fetch error: " + error);
                checkAndProceed();
            }

            private void checkAndProceed() {
                completedFetches[0]++;

                // Update loading text if available
                if (loadingText != null) {
                    runOnUiThread(() -> {
                        loadingText.setText("Fetching streams... " + completedFetches[0] + "/" + totalServers);
                    });
                }

                // Once all servers have responded
                if (completedFetches[0] >= totalServers) {
                    runOnUiThread(() -> {
                        loadingOverlay.setVisibility(View.GONE);
                        playButton.setEnabled(true);

                        if (allVideoSources.isEmpty()) {
                            Toast.makeText(DetailsActivity.this,
                                    "No streams found for this movie", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // Remove duplicates based on URL
                        List<MediaItems.VideoSource> uniqueSources = utils.removeDuplicateSources(allVideoSources);
                        List<MediaItems.SubtitleItem> uniqueSubtitles = utils.removeDuplicateSubtitles(allSubtitles);

                        // Move VIP source(s) to index 0
                        uniqueSources = utils.moveVipSourceToTop(uniqueSources);

                        // Set all sources and subtitles to the media item
                        mediaItems.setVideoSources(uniqueSources);
                        mediaItems.setSubtitles(uniqueSubtitles);

                        Log.i(TAG, "Total unique sources: " + uniqueSources.size());
                        Log.i(TAG, "Total unique subtitles: " + uniqueSubtitles.size());

                        // Launch player
                        launchPlayer();
                    });
                }
            }
        };

        // 1. Fetch from Videasy
        fetchStreams.fetchVideasyMovie(title, year, tmdbId, callback);

        // 2. Fetch from Hexa
        fetchStreams.fetchHexaMovie(tmdbId, callback);

        // 3. Fetch from Vidlink
        fetchStreams.fetchVidlinkMovie(tmdbId, callback);

        // 4. Fetch from SmashyStream/Vidstack Type 1
        fetchStreams.fetchSmashystreamMovie(imdbId, tmdbId, "1", callback);

        // 5. Fetch from SmashyStream/Vidstack Type 2
        fetchStreams.fetchSmashystreamMovie(imdbId, tmdbId, "2", callback);

        // 6. Fetch from XPrime (using "primebox" server)
        fetchStreams.fetchXprimeMovie(title, year, tmdbId, imdbId, "primebox", callback);
    }



    /**
     * Launch the player activity
     * If watch history exists, starts from saved position
     */
    private void launchPlayer() {
        if (!mediaItems.hasValidVideoSources()) {
            Toast.makeText(this, "No video sources available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("media_item", mediaItems);

                // Pass start position if continuing watch history
        if (hasWatchHistory && savedPosition > 0) {
            intent.putExtra("start_position", savedPosition);
            Log.i(TAG, "Launching player from saved position: " + preferencesManager.formatTime((int) savedPosition));
        }

        startActivity(intent);
    }

    /**
     * Server type enum for video source fetching
     */
    private enum ServerType {
        VIDEASY,
        VIDLINK,
        HEXA,
        ONETOUCHTV,
        SMASHYSTREAM_TYPE1,
        SMASHYSTREAM_TYPE2
    }

    /**
     * Server source class to hold server information
     */
    private static class ServerSource {
        String name;
        ServerType type;

        ServerSource(String name, ServerType type) {
            this.name = name;
            this.type = type;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fetchStreams != null) {
            fetchStreams.shutdown();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh watch history status when returning to this activity
        if (preferencesManager != null && mediaItems != null) {
            checkWatchHistory();
        }
    }

    // ============================================
    // Watch History Methods
    // ============================================

    /**
     * Check if this media has watch history and update play button accordingly
     */
    private void checkWatchHistory() {
        if (mediaItems == null || preferencesManager == null) {
            return;
        }

        // Get media ID (use tmdbId or id)
        String mediaId = mediaItems.getId() != null ? mediaItems.getId() : mediaItems.getTmdbId();

        if (mediaId == null || mediaId.isEmpty()) {
            return;
        }

        // Check for watch history
        PreferencesManager.WatchHistoryItem historyItem = preferencesManager.getWatchHistory(mediaId);

        if (historyItem != null && historyItem.currentPosition > 0 && !historyItem.isCompleted()) {
            hasWatchHistory = true;
            savedPosition = historyItem.currentPosition;

            // Update button text and style for "Continue Watching"
            playButton.setText("Continue Watching");

                    // Format the time for display
                    String timeStr = preferencesManager.formatTime((int) historyItem.currentPosition);
            String totalTimeStr = preferencesManager.formatTime((int) historyItem.totalDuration);

            // Show a hint about progress
            int progressPercent = historyItem.getProgressPercentage();
            Log.i(TAG, "Watch history found: " + historyItem.title + " - " +
                    progressPercent + "% complete at " + timeStr + " " + totalTimeStr);
        } else {
            hasWatchHistory = false;
            savedPosition = 0;
            playButton.setText("Play");
        }
    }
}
