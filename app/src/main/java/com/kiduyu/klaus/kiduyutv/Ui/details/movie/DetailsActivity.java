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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
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

        initializeViews();
        setupViews();
        setupClickListeners();
        setupCastSection();
        setupRecommendations();
        loadCast();
        loadRecommendations();
    }

    /**
     * Initialize FetchStreams instance and setup server sources
     */
    private void initializeFetchStreams() {
        fetchStreams = new FetchStreams();
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

        favoriteButton.setOnClickListener(v -> {
            Toast.makeText(DetailsActivity.this,
                    "Added to favorites",
                    Toast.LENGTH_SHORT).show();
        });
        favoriteButton.setOnFocusChangeListener(focusChangeListener);
    }

    /**
     * Fetch video sources from multiple servers sequentially
     */
    private void fetchVideoSources() {
        loadingOverlay.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);
        currentServerIndex = 0;

        String title = mediaItems.getTitle();
        String year = String.valueOf(mediaItems.getYear());
        String tmdbId = mediaItems.getTmdbId();
        String imdbId = mediaItems.getId(); // Assuming this contains IMDB ID

        Log.i(TAG, "Fetching video sources for: " + title + " (" + year + ") [" + tmdbId + "]");
        Log.i(TAG, "fetchVideoSources: " + title + " (" + year + ") [" + tmdbId + "]");

        // Start trying servers for MOVIE only
        tryNextServer(title, year, tmdbId, imdbId);
    }

    /**
     * Try the next server in the list

     private void tryNextServer(String title, String year, String tmdbId, String imdbId) {
     if (currentServerIndex >= serverSources.size()) {
     // All servers failed
     loadingOverlay.setVisibility(View.GONE);
     playButton.setEnabled(true);

     Toast.makeText(DetailsActivity.this,
     "Unable to find video sources from any server",
     Toast.LENGTH_LONG).show();

     Log.e(TAG, "All servers failed to fetch video sources");
     return;
     }

     ServerSource currentServer = serverSources.get(currentServerIndex);
     Log.i(TAG, "Trying server [" + (currentServerIndex + 1) + "/" + serverSources.size() + "]: " +
     currentServer.name);

     // Show which server we're trying
     if (loadingText != null) {
     loadingText.setText("Loading from " + currentServer.name + "...");
     }

     FetchStreams.StreamCallback callback = new FetchStreams.StreamCallback() {
    @Override public void onSuccess(MediaItems updatedItem) {
    // Validate the response has actual video sources
    if (updatedItem != null && updatedItem.hasValidVideoSources()) {
    loadingOverlay.setVisibility(View.GONE);
    playButton.setEnabled(true);

    Log.i(TAG, "✓ Success with " + currentServer.name);
    Log.i(TAG, "Video sources: " + updatedItem.getVideoSources().size());
    Log.i(TAG, "Subtitles: " + updatedItem.getSubtitles().size());

    // Update current media item with video sources
    mediaItems.setVideoSources(updatedItem.getVideoSources());
    mediaItems.setSubtitles(updatedItem.getSubtitles());
    mediaItems.setRefererUrl(updatedItem.getRefererUrl());
    mediaItems.setCustomHeaders(updatedItem.getCustomHeaders());
    mediaItems.setResponseHeaders(updatedItem.getResponseHeaders());

    // Also copy over URLs if available
    if (updatedItem.getHlsUrl() != null) {
    mediaItems.setHlsUrl(updatedItem.getHlsUrl());
    }
    if (updatedItem.getDashUrl() != null) {
    mediaItems.setDashUrl(updatedItem.getDashUrl());
    }
    if (updatedItem.getVideoUrl() != null) {
    mediaItems.setVideoUrl(updatedItem.getVideoUrl());
    }

    Toast.makeText(DetailsActivity.this,
    "Loaded from " + currentServer.name,
    Toast.LENGTH_SHORT).show();

    launchPlayer();
    } else {
    // No valid sources, try next server
    Log.w(TAG, "✗ " + currentServer.name + " returned no valid sources");
    currentServerIndex++;
    tryNextServer(title, year, tmdbId, imdbId);
    }
    }

    @Override public void onError(String error) {
    Log.e(TAG, "✗ " + currentServer.name + " failed: " + error);

    // Try next server
    currentServerIndex++;
    tryNextServer(title, year, tmdbId, imdbId);
    }
    };

     // Call appropriate server based on type - MOVIES ONLY
     try {
     switch (currentServer.type) {
     case VIDEASY:
     fetchStreams.fetchVideasyMovie(title, year, tmdbId, callback);
     break;

     case VIDLINK:
     fetchStreams.fetchVidlinkMovie(tmdbId, callback);
     break;

     case HEXA:
     fetchStreams.fetchHexaMovie(tmdbId, callback);
     break;

     case ONETOUCHTV:
     // OneTouchTV requires vodId - you may need to search/map tmdbId to vodId
     // For now, using tmdbId as placeholder
     // Note: This might need adjustment based on actual API requirements
     fetchStreams.fetchOnetouchtvMovie(tmdbId, callback);
     break;

     case SMASHYSTREAM_TYPE1:
     // Requires IMDB ID
     if (imdbId != null && !imdbId.isEmpty()) {
     fetchStreams.fetchSmashystreamMovie(imdbId, tmdbId, "1", callback);
     } else {
     // Skip if no IMDB ID
     Log.w(TAG, "Skipping Smashystream Type 1 - no IMDB ID");
     currentServerIndex++;
     tryNextServer(title, year, tmdbId, imdbId);
     }
     break;

     case SMASHYSTREAM_TYPE2:
     fetchStreams.fetchSmashystreamMovie(imdbId, tmdbId, "2", callback);
     break;

     default:
     // Unknown server type, skip
     currentServerIndex++;
     tryNextServer(title, year, tmdbId, imdbId);
     break;
     }
     } catch (Exception e) {
     Log.e(TAG, "Exception calling " + currentServer.name + ": " + e.getMessage(), e);
     currentServerIndex++;
     tryNextServer(title, year, tmdbId, imdbId);
     }
     }
     */

    /**
     * Try the next server in the list (SMASHYSTREAM_TYPE2 and HEXA only)
     */
    private void tryNextServer(String title, String year, String tmdbId, String imdbId) {
        if (currentServerIndex >= serverSources.size()) {
            // All servers failed
            loadingOverlay.setVisibility(View.GONE);
            playButton.setEnabled(true);

            Toast.makeText(DetailsActivity.this,
                    "Unable to find video sources from any server",
                    Toast.LENGTH_LONG).show();

            Log.e(TAG, "All servers failed to fetch video sources");
            return;
        }

        ServerSource currentServer = serverSources.get(currentServerIndex);
        Log.i(TAG, "Trying server [" + (currentServerIndex + 1) + "/" + serverSources.size() + "]: " +
                currentServer.name);

        // Show which server we're trying
        if (loadingText != null) {
            loadingText.setText("Loading from " + currentServer.name + "...");
        }

        FetchStreams.StreamCallback callback = new FetchStreams.StreamCallback() {
            @Override
            public void onSuccess(MediaItems updatedItem) {
                // Validate the response has actual video sources
                if (updatedItem != null && updatedItem.hasValidVideoSources()) {
                    loadingOverlay.setVisibility(View.GONE);
                    playButton.setEnabled(true);

                    Log.i(TAG, "✓ Success with " + currentServer.name);
                    Log.i(TAG, "Video sources: " + updatedItem.getVideoSources().size());
                    Log.i(TAG, "Subtitles: " + updatedItem.getSubtitles().size());

                    // Update current media item with video sources
                    mediaItems.setVideoSources(updatedItem.getVideoSources());
                    mediaItems.setSubtitles(updatedItem.getSubtitles());
                    mediaItems.setRefererUrl(updatedItem.getRefererUrl());
                    mediaItems.setCustomHeaders(updatedItem.getCustomHeaders());
                    mediaItems.setResponseHeaders(updatedItem.getResponseHeaders());

                    // Also copy over URLs if available
                    if (updatedItem.getHlsUrl() != null) {
                        mediaItems.setHlsUrl(updatedItem.getHlsUrl());
                    }
                    if (updatedItem.getDashUrl() != null) {
                        mediaItems.setDashUrl(updatedItem.getDashUrl());
                    }
                    if (updatedItem.getVideoUrl() != null) {
                        mediaItems.setVideoUrl(updatedItem.getVideoUrl());
                    }

                    Toast.makeText(DetailsActivity.this,
                            "Loaded from " + currentServer.name,
                            Toast.LENGTH_SHORT).show();

                    launchPlayer();
                } else {
                    // No valid sources, try next server
                    Log.w(TAG, "✗ " + currentServer.name + " returned no valid sources");
                    currentServerIndex++;
                    tryNextServer(title, year, tmdbId, imdbId);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "✗ " + currentServer.name + " failed: " + error);

                // Try next server
                currentServerIndex++;
                tryNextServer(title, year, tmdbId, imdbId);
            }
        };

        // Call appropriate server based on type - HEXA and SMASHYSTREAM_TYPE2 only
        try {
            switch (currentServer.type) {
                case HEXA:
                    fetchStreams.fetchHexaMovie(tmdbId, callback);
                    break;

                case SMASHYSTREAM_TYPE2:
                    fetchStreams.fetchSmashystreamMovie(imdbId, tmdbId, "2", callback);
                    break;

                default:
                    // Unknown server type, skip
                    Log.w(TAG, "Skipping unsupported server type: " + currentServer.type);
                    currentServerIndex++;
                    tryNextServer(title, year, tmdbId, imdbId);
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception calling " + currentServer.name + ": " + e.getMessage(), e);
            currentServerIndex++;
            tryNextServer(title, year, tmdbId, imdbId);
        }
    }

    private void launchPlayer() {
        if (!mediaItems.hasValidVideoSources()) {
            Toast.makeText(this, "No video sources available", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("media_item", mediaItems);
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
}
