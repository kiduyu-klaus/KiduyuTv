package com.kiduyu.klaus.kiduyutv.Ui.details.tv;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.CastRepository;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
//import com.kiduyu.klaus.kiduyutv.Ui.details.actor.ActorDetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.actor.ActorDetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.adapter.CastAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.EpisodeGridAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.SeasonTabAdapter;
import com.kiduyu.klaus.kiduyutv.model.CastMember;
import com.kiduyu.klaus.kiduyutv.model.Episode;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.model.Season;

import java.util.ArrayList;
import java.util.List;

public class DetailsActivityTv extends AppCompatActivity {
    private static final String TAG = "DetailsActivityTv";

    // UI Components - Hero Section
    private ImageView backgroundImage;
    private ImageView posterImage;
    private TextView titleText;
    private TextView yearText;
    private TextView ratingText;
    private TextView durationText;
    private LinearLayout genresLayout;
    private TextView descriptionText;
    private TextView creatorsText;
    private TextView starsText;
    private Button watchButton;
    private ImageView favoriteButton;

    // Cast Section
    private RecyclerView castRecyclerView;
    private ProgressBar castLoadingProgress;
    private TextView emptyCastText;
    private CastAdapter castAdapter;
    private CastRepository castRepository;

    // Season Tabs
    private RecyclerView seasonTabsRecyclerView;

    // Episodes Grid
    private RecyclerView episodesGridRecyclerView;

    // Loading
    private ProgressBar loadingProgress;

    // Data
    private MediaItems tvShow;
    private List<Season> seasons = new ArrayList<>();
    private List<Episode> currentEpisodes = new ArrayList<>();
    private List<CastMember> castList = new ArrayList<>();
    private SeasonTabAdapter seasonTabAdapter;
    private EpisodeGridAdapter episodeGridAdapter;
    private TmdbRepository mediaRepository;
    private int selectedSeasonNumber = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_details_tv);

        mediaRepository = new TmdbRepository();
        castRepository = new CastRepository();

        // Get TV show from intent
        tvShow = getIntent().getParcelableExtra("media_item");
        if (tvShow == null) {
            finish();
            return;
        }

        initializeViews();
        setupRecyclerViews();
        loadTvShowDetails();

    }

    private void initializeViews() {
        // Hero section
        backgroundImage = findViewById(R.id.backgroundImage);
        posterImage = findViewById(R.id.posterImage);
        titleText = findViewById(R.id.titleText);
        yearText = findViewById(R.id.yearText);
        ratingText = findViewById(R.id.ratingText);
        durationText = findViewById(R.id.durationText);
        genresLayout = findViewById(R.id.genresLayout);
        descriptionText = findViewById(R.id.descriptionText);
        creatorsText = findViewById(R.id.creatorsText);
        starsText = findViewById(R.id.starsText);
        //watchButton = findViewById(R.id.watchButton);
        favoriteButton = findViewById(R.id.favoriteButton);

        // Cast section
        castRecyclerView = findViewById(R.id.castRecyclerView);
        castLoadingProgress = findViewById(R.id.castLoadingProgress);
        emptyCastText = findViewById(R.id.emptyCastText);

        // Season tabs and episodes
        seasonTabsRecyclerView = findViewById(R.id.seasonTabsRecyclerView);
        episodesGridRecyclerView = findViewById(R.id.episodesGridRecyclerView);

        // Loading
        loadingProgress = findViewById(R.id.loadingProgress);

        // Set basic info
        titleText.setText(tvShow.getTitle());
        yearText.setText(String.valueOf(tvShow.getYear()));
        descriptionText.setText(tvShow.getDescription());

        // Load images
        String bgUrl = tvShow.getPrimaryImageUrl();
        if (bgUrl != null && !bgUrl.isEmpty()) {
            Glide.with(this)
                    .load(bgUrl)
                    .centerCrop()
                    .into(backgroundImage);
        }

        String posterUrl = tvShow.getPosterUrl();
        if (posterUrl != null && !posterUrl.isEmpty()) {
            Glide.with(this)
                    .load(posterUrl)
                    .centerCrop()
                    .into(posterImage);
        }

        // Watch button
//        watchButton.setOnClickListener(v -> {
//            if (!currentEpisodes.isEmpty()) {
//                playEpisode(currentEpisodes.get(0));
//            }
//        });

        // Favorite button
        favoriteButton.setOnClickListener(v -> {
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        });

        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };
        favoriteButton.setOnFocusChangeListener(focusChangeListener);
    }

    private void setupRecyclerViews() {
        // Cast - horizontal layout
        LinearLayoutManager castLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false);
        castRecyclerView.setLayoutManager(castLayoutManager);
        castRecyclerView.setHasFixedSize(true);

        castAdapter = new CastAdapter();
        castRecyclerView.setAdapter(castAdapter);

        castAdapter.setOnCastClickListener(this::openActorDetails);

        // Season tabs - horizontal
        LinearLayoutManager tabsLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        seasonTabsRecyclerView.setLayoutManager(tabsLayoutManager);

        seasonTabAdapter = new SeasonTabAdapter(seasons);
        seasonTabsRecyclerView.setAdapter(seasonTabAdapter);

        seasonTabAdapter.setOnSeasonClickListener((season, position) -> {
            selectedSeasonNumber = season.getSeasonNumber();
            seasonTabAdapter.setSelectedPosition(position);
            loadEpisodes(selectedSeasonNumber);
        });

        // Episodes grid - 4 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        episodesGridRecyclerView.setLayoutManager(gridLayoutManager);

        episodeGridAdapter = new EpisodeGridAdapter(currentEpisodes);
        episodesGridRecyclerView.setAdapter(episodeGridAdapter);

        episodeGridAdapter.setOnEpisodeClickListener(this::playEpisode);
    }

    private void openActorDetails(CastMember castMember, int position) {
        Intent intent = new Intent(this, ActorDetailsActivity.class);
        intent.putExtra("cast_member", castMember);
        startActivity(intent);
    }

    private void playEpisode(Episode episode) {
        MediaItems episodeMedia = new MediaItems();
        episodeMedia.setTitle(tvShow.getTitle());
        episodeMedia.setDescription(episode.getOverview());
        episodeMedia.setTmdbId(tvShow.getTmdbId());
        episodeMedia.setMediaType("tv");
        episodeMedia.setSeason(String.valueOf(selectedSeasonNumber));
        episodeMedia.setEpisode(String.valueOf(episode.getEpisodeNumber()));
        episodeMedia.setYear(tvShow.getYear());
        episodeMedia.setPosterUrl(episode.getStillPath());
        episodeMedia.setFromTMDB(true);


        String season = episodeMedia.getSeason() != null ? episodeMedia.getSeason() : "1";
        String episode1 = episodeMedia.getEpisode() != null ? episodeMedia.getEpisode() : "1";
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("media_item", episodeMedia);
        startActivity(intent);

    }

    private void loadTvShowDetails() {
        loadingProgress.setVisibility(View.VISIBLE);

        mediaRepository.getTVShowDetails(tvShow.getTmdbId(), new TmdbRepository.TVShowDetailsCallback() {
            @Override
            public void onSuccess(MediaItems detailedShow, List<Season> seasonsList) {
                loadingProgress.setVisibility(View.GONE);

                tvShow = detailedShow;
                seasons.clear();
                seasons.addAll(seasonsList);

                // Update UI
                updateDetailedInfo(detailedShow);

                // Update season tabs
                seasonTabAdapter.notifyDataSetChanged();

                // Load cast
                loadCast();

                // Load first season
                if (!seasons.isEmpty()) {
                    selectedSeasonNumber = seasons.get(0).getSeasonNumber();
                    seasonTabAdapter.setSelectedPosition(0);
                    loadEpisodes(selectedSeasonNumber);
                }
            }

            @Override
            public void onError(String error) {
                loadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error loading TV show details: " + error);
                Toast.makeText(DetailsActivityTv.this, "Failed to load details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Load cast members for the TV show
     */
    private void loadCast() {
        if (tvShow.getTmdbId() == null || tvShow.getTmdbId().isEmpty()) {
            showEmptyCast();
            return;
        }

        castLoadingProgress.setVisibility(View.VISIBLE);
        castRecyclerView.setVisibility(View.GONE);
        emptyCastText.setVisibility(View.GONE);

        castRepository.getTVShowCast(tvShow.getTmdbId(), new CastRepository.CastListCallback() {
            @Override
            public void onSuccess(List<CastMember> castMembers) {
                castLoadingProgress.setVisibility(View.GONE);

                castList.clear();
                castList.addAll(castMembers);

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
                castLoadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error loading cast: " + error);
                showEmptyCast();
            }
        });
    }

    private void showEmptyCast() {
        castRecyclerView.setVisibility(View.GONE);
        emptyCastText.setVisibility(View.VISIBLE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateDetailedInfo(MediaItems show) {
        // Rating
        if (show.getRating() > 0) {
            ratingText.setText(String.format("%.1f", show.getRating()));
            ratingText.setVisibility(View.VISIBLE);
        } else {
            ratingText.setVisibility(View.GONE);
        }

        // Duration
        if (show.getDuration() != null && !show.getDuration().isEmpty()) {
            durationText.setText(show.getDuration());
        }

        // Genres as chips
        genresLayout.removeAllViews();
        if (show.getGenres() != null && !show.getGenres().isEmpty()) {
            for (String genre : show.getGenres()) {
                TextView genreChip = new TextView(this);
                genreChip.setText(genre);
                genreChip.setTextColor(getColor(R.color.white));
                genreChip.setBackgroundResource(R.drawable.genre_chip_background);
                genreChip.setPadding(24, 12, 24, 12);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMarginEnd(12);
                genreChip.setLayoutParams(params);

                genresLayout.addView(genreChip);
            }
        }else{
            genresLayout.setVisibility(View.GONE);
        }

        // Get creators
        mediaRepository.getTVShowCreators(show.getTmdbId(), new TmdbRepository.CreatorsCallback() {
            @Override
            public void onSuccess(List<String> creators) {
                // Handle creators list
                Log.i(TAG, "Creators: " + creators.toString());

                if (creators != null && !creators.isEmpty()) {
                    // Join creators with comma and space
                    String creatorsString = String.join(", ", creators);
                    creatorsText.setText(creatorsString);
                } else {
                    creatorsText.setText("Unknown");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }
        });

        mediaRepository.getTVShowStars(show.getTmdbId(), new TmdbRepository.StarsCallback() {
            @Override
            public void onSuccess(List<String> stars) {
                // Handle stars list
                Log.i(TAG, "Stars: " + stars.toString());
                if (stars != null && !stars.isEmpty()) {
                    // Join stars with comma and space
                    String starsString = String.join(", ", stars);
                    // Truncate if too long (e.g., max 50 characters)
                    if (starsString.length() > 50) {
                        starsString = starsString.substring(0, 47) + "...";
                    }
                    starsText.setText(starsString);
                } else {
                    starsText.setText("Unknown");
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error: " + error);
            }
        });

    }

    private void loadEpisodes(int seasonNumber) {
        loadingProgress.setVisibility(View.VISIBLE);
        currentEpisodes.clear();
        episodeGridAdapter.notifyDataSetChanged();

        mediaRepository.getSeasonEpisodes(tvShow.getTmdbId(), seasonNumber, new TmdbRepository.EpisodesCallback() {
            @Override
            public void onSuccess(List<Episode> episodes) {
                loadingProgress.setVisibility(View.GONE);

                currentEpisodes.clear();
                currentEpisodes.addAll(episodes);
                episodeGridAdapter.notifyDataSetChanged();
            }

            @Override
            public void onError(String error) {
                loadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error loading episodes: " + error);
                Toast.makeText(DetailsActivityTv.this, "Failed to load episodes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
