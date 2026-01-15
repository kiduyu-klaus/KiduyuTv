package com.kiduyu.klaus.kiduyutv.Ui.details.movie;

import static com.kiduyu.klaus.kiduyutv.Api.FetchStreams.SmashyServer.SMASHYSTREAM;
import static com.kiduyu.klaus.kiduyutv.Api.FetchStreams.SmashyServer.VIDEOFSH;
import static com.kiduyu.klaus.kiduyutv.Api.FetchStreams.SmashyServer.VIDEOOPHIM;

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
    private MediaItems mediaItems;
    private ImageView backdropImageView;
    private ImageView posterImageView;
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
    private RecyclerView recommendationsRecyclerView;
    private TextView recommendationsTitle;
    private ProgressBar recommendationsLoadingBar;

    // Cast section components
    private RecyclerView castRecyclerView;
    private ProgressBar castLoadingBar;
    private TextView emptyCastText;
    private CastAdapter castAdapter;
    private CastRepository castRepository;

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
        // Detailed debugging
        Log.i("PlayerActivity", "Loading media:\n" + mediaItems.toString());
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

    private void initializeViews() {
        backdropImageView = findViewById(R.id.backdropImageView);
        //posterImageView = findViewById(R.id.posterImageView);
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
        /**if (mediaItems.getPosterUrl() != null) {
         Glide.with(this)
         .load(mediaItems.getPosterUrl())
         .centerCrop()
         .into(posterImageView);
         }**/
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

    private void fetchVideoSources() {
        //loadingOverlay.setVisibility(View.GONE);
        loadingOverlay.setVisibility(View.VISIBLE);
        playButton.setEnabled(false);

        String title = mediaItems.getTitle();
        String year = String.valueOf(mediaItems.getYear());
        String tmdbId = mediaItems.getTmdbId();
        String mediaType = mediaItems.getMediaType();

        Log.i(TAG, "Fetching video sources for: " + title + " (" + year + ") [" + tmdbId + "]");
        Log.i(TAG, "fetchVideoSources: " + title + " (" + year + ") [" + tmdbId + "]");
        //FetchStreams.getInstance().fetchSmashyStreamsMovie(tmdbId,tmdbId,VIDEOOPHIM, new FetchStreams.VideasyCallback() {
        //FetchStreams.getInstance().fetchHexaStreamsMovie(tmdbId, new FetchStreams.VideasyCallback() {
        FetchStreams.getInstance().fetchVideasyStreamsMovie(title,year,tmdbId, new FetchStreams.VideasyCallback() {
            @Override
            public void onSuccess(MediaItems updatedItem) {
                // Handle success
                loadingOverlay.setVisibility(View.GONE);
                playButton.setEnabled(true);

                // Update current media item with video sources and headers
                Log.i(TAG, "Video sources fetched: " +
                        updatedItem.getVideoSources().toString());

                mediaItems.setVideoSources(updatedItem.getVideoSources());
                mediaItems.setSubtitles(updatedItem.getSubtitles());

                // Transfer session headers for Cloudflare/protected stream bypass
                mediaItems.setCustomHeaders(updatedItem.getCustomHeaders());
                mediaItems.setResponseHeaders(updatedItem.getResponseHeaders());
                mediaItems.setSessionCookie(updatedItem.getSessionCookie());
                mediaItems.setRefererUrl(updatedItem.getRefererUrl());
                mediaItems.setDescription(mediaItems.getDescription());

                Log.d(TAG, "Video sources fetched: " +
                        mediaItems.getVideoSources().size());
                Log.d(TAG, "Custom headers count: " +
                        (mediaItems.getCustomHeaders() != null ? mediaItems.getCustomHeaders().size() : 0));
                Log.d(TAG, "Response headers count: " +
                        (mediaItems.getResponseHeaders() != null ? mediaItems.getResponseHeaders().size() : 0));

                launchPlayer();
            }

            @Override
            public void onError(String error) {
                // Handle error
                loadingOverlay.setVisibility(View.GONE);
                playButton.setEnabled(true);

                Toast.makeText(DetailsActivity.this,
                        "Failed to fetch video sources: " + error,
                        Toast.LENGTH_LONG).show();

                Log.e(TAG, "Error fetching video sources: " + error);
            }
        });


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
}
