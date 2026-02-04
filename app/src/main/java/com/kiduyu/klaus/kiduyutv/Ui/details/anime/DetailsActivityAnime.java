package com.kiduyu.klaus.kiduyutv.Ui.details.anime;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.OptIn;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.AnimekaiApi;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.player.PlayerActivity;
import com.kiduyu.klaus.kiduyutv.adapter.AnimeEpisodeGridAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.SeasonTabAdapter;
import com.kiduyu.klaus.kiduyutv.model.AnimeModel;
import com.kiduyu.klaus.kiduyutv.model.EpisodeModel;
import com.kiduyu.klaus.kiduyutv.model.Season;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DetailsActivityAnime extends AppCompatActivity {
    private static final String TAG = "DetailsActivityAnime";

    // UI Components - Hero Section
    private ImageView backgroundImage;
    private ImageView posterImage;
    private TextView titleText;
    private TextView yearText;
    private TextView statusText;
    private TextView ratingText;
    private TextView durationText;
    private LinearLayout genresLayout;
    private TextView descriptionText;
    private TextView studioText;
    private TextView countryText;
    private ImageView favoriteButton;

    // Episodes Section
    private TextView episodesTitle;
    private RecyclerView seasonTabsRecyclerView;
    private LinearLayout serverTypeLayout;
    private Button subButton;
    private Button dubButton;
    private Button softsubButton;
    private RecyclerView episodesGridRecyclerView;
    private TextView emptyEpisodesText;

    // Loading
    private RelativeLayout loadingOverlay;
    private TextView loadingText;

    // Data
    private AnimeModel anime;
    private List<Season> seasons = new ArrayList<>();
    private List<EpisodeModel> currentEpisodes = new ArrayList<>();
    private SeasonTabAdapter seasonTabAdapter;
    private AnimeEpisodeGridAdapter episodeGridAdapter;
    private AnimekaiApi animekaiApi;
    private ExecutorService executorService;

    private int selectedSeasonNumber = 1;
    private String selectedServerType = "sub"; // Default to SUB
    private Map<String, Map<String, AnimekaiApi.ServerInfo>> currentServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_details_anime);

        // Initialize API
        animekaiApi = new AnimekaiApi();
        executorService = Executors.newFixedThreadPool(3);

        // Get anime from intent
        anime = (AnimeModel) getIntent().getSerializableExtra("anime");
        if (anime == null) {
            Toast.makeText(this, "Error loading anime", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerViews();
        loadAnimeDetails();
    }

    private void initializeViews() {
        // Hero section
        backgroundImage = findViewById(R.id.backgroundImage);
        posterImage = findViewById(R.id.posterImage);
        titleText = findViewById(R.id.titleText);
        yearText = findViewById(R.id.yearText);
        statusText = findViewById(R.id.statusText);
        ratingText = findViewById(R.id.ratingText);
        durationText = findViewById(R.id.durationText);
        genresLayout = findViewById(R.id.genresLayout);
        descriptionText = findViewById(R.id.descriptionText);
        studioText = findViewById(R.id.studioText);
        countryText = findViewById(R.id.countryText);
        favoriteButton = findViewById(R.id.favoriteButton);

        // Episodes section
        episodesTitle = findViewById(R.id.episodesTitle);
        seasonTabsRecyclerView = findViewById(R.id.seasonTabsRecyclerView);
        serverTypeLayout = findViewById(R.id.serverTypeLayout);
        subButton = findViewById(R.id.subButton);
        dubButton = findViewById(R.id.dubButton);
        softsubButton = findViewById(R.id.softsubButton);
        episodesGridRecyclerView = findViewById(R.id.episodesGridRecyclerView);
        emptyEpisodesText = findViewById(R.id.emptyEpisodesText);

        // Loading
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingText = findViewById(R.id.loadingText);

        // Set basic info
        titleText.setText(anime.getAnimeName());

        if (anime.getAnimeDescription() != null && !anime.getAnimeDescription().isEmpty()) {
            descriptionText.setText(anime.getAnimeDescription());
        }

        // Load images
        String bgUrl = anime.getAnime_image_backgroud();
        if (bgUrl != null && !bgUrl.isEmpty()) {
            Glide.with(this)
                    .load(bgUrl)
                    .centerCrop()
                    .into(backgroundImage);

            // Use same image for poster if no separate poster available
            Glide.with(this)
                    .load(bgUrl)
                    .centerCrop()
                    .into(posterImage);
        }

        // Server type buttons
        setupServerTypeButtons();

        // Favorite button
        favoriteButton.setOnClickListener(v -> {
            Toast.makeText(this, "Added to favorites", Toast.LENGTH_SHORT).show();
        });

        // Focus listeners
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };

        favoriteButton.setOnFocusChangeListener(focusChangeListener);
        subButton.setOnFocusChangeListener(focusChangeListener);
        dubButton.setOnFocusChangeListener(focusChangeListener);
        softsubButton.setOnFocusChangeListener(focusChangeListener);
    }

    private void setupServerTypeButtons() {
        subButton.setOnClickListener(v -> {
            selectServerType("sub");
        });

        dubButton.setOnClickListener(v -> {
            selectServerType("dub");
        });

        softsubButton.setOnClickListener(v -> {
            selectServerType("softsub");
        });
    }

    private void selectServerType(String serverType) {
        selectedServerType = serverType;

        // Update button appearances
        subButton.setBackgroundResource(
                serverType.equals("sub") ? R.drawable.button_selected : R.drawable.button_unselected
        );
        dubButton.setBackgroundResource(
                serverType.equals("dub") ? R.drawable.button_selected : R.drawable.button_unselected
        );
        softsubButton.setBackgroundResource(
                serverType.equals("softsub") ? R.drawable.button_selected : R.drawable.button_unselected
        );

        // Note: Server type selection is now set, episodes will use this when played
        Toast.makeText(this, "Selected " + serverType.toUpperCase(), Toast.LENGTH_SHORT).show();
    }

    private void setupRecyclerViews() {
        // Season tabs - horizontal
        LinearLayoutManager tabsLayoutManager = new LinearLayoutManager(
                this, LinearLayoutManager.HORIZONTAL, false
        );
        seasonTabsRecyclerView.setLayoutManager(tabsLayoutManager);

        seasonTabAdapter = new SeasonTabAdapter(seasons);
        seasonTabsRecyclerView.setAdapter(seasonTabAdapter);

        seasonTabAdapter.setOnSeasonClickListener((season, position) -> {
            selectedSeasonNumber = season.getSeasonNumber();
            seasonTabAdapter.setSelectedPosition(position);
            loadEpisodesForSeason(selectedSeasonNumber);
        });

        // Episodes grid - 4 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 4);
        episodesGridRecyclerView.setLayoutManager(gridLayoutManager);

        episodeGridAdapter = new AnimeEpisodeGridAdapter(currentEpisodes);
        episodesGridRecyclerView.setAdapter(episodeGridAdapter);

        episodeGridAdapter.setOnEpisodeClickListener(this::onEpisodeClick);
    }

    private void loadAnimeDetails() {
        String contentId = anime.getData_tip();

        if (contentId == null || contentId.isEmpty()) {
            Toast.makeText(this, "Invalid anime data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadingOverlay.setVisibility(View.VISIBLE);
        loadingText.setText("Loading anime details...");

        // Fetch detailed anime information with episodes
        animekaiApi.fetchAnimeDetailsAsync(contentId, new AnimekaiApi.AnimeDetailsCallback() {
            @Override
            public void onSuccess(AnimeModel detailedAnime) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);

                    // Update anime object
                    anime = detailedAnime;

                    // Update UI with detailed info
                    updateDetailedInfo(detailedAnime);

                    // Setup seasons and episodes
                    setupSeasonsAndEpisodes(detailedAnime);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading anime details: " + error);
                    Toast.makeText(DetailsActivityAnime.this,
                            "Failed to load anime details: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void updateDetailedInfo(AnimeModel anime) {
        // Rating
        if (anime.getMalScore() > 0) {
            ratingText.setText(String.format("%.1f", anime.getMalScore()));
            ratingText.setVisibility(View.VISIBLE);
        } else {
            ratingText.setVisibility(View.GONE);
        }

        // Status
        if (anime.getStatus() != null && !anime.getStatus().isEmpty()) {
            statusText.setText(anime.getStatus());
        } else {
            statusText.setText("Unknown");
        }

        // Duration
        if (anime.getDuration() != null && !anime.getDuration().isEmpty()) {
            durationText.setText(anime.getDuration());
        }

        // Studio
        if (anime.getStudio() != null && !anime.getStudio().isEmpty()) {
            studioText.setText(anime.getStudio());
        }

        // Country
        if (anime.getCountry() != null && !anime.getCountry().isEmpty()) {
            countryText.setText(anime.getCountry());
        }

        // Genres
        genresLayout.removeAllViews();
        if (anime.getGenres() != null && !anime.getGenres().isEmpty()) {
            for (String genre : anime.getGenres()) {
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
        }
    }

    private void setupSeasonsAndEpisodes(AnimeModel anime) {
        Map<Integer, Map<Integer, EpisodeModel>> episodesMap = anime.getEpisodes();

        if (episodesMap == null || episodesMap.isEmpty()) {
            emptyEpisodesText.setText("No episodes available");
            emptyEpisodesText.setVisibility(View.VISIBLE);
            return;
        }

        // Convert to Season objects for the adapter
        seasons.clear();
        for (Map.Entry<Integer, Map<Integer, EpisodeModel>> entry : episodesMap.entrySet()) {
            int seasonNumber = entry.getKey();
            int episodeCount = entry.getValue().size();

            Season season = new Season();
            season.setSeasonNumber(seasonNumber);
            season.setEpisodeCount(episodeCount);
            season.setName("Season " + seasonNumber);

            seasons.add(season);
        }

        // Show/hide season tabs based on number of seasons
        if (seasons.size() > 1) {
            seasonTabsRecyclerView.setVisibility(View.VISIBLE);
            seasonTabAdapter.notifyDataSetChanged();
        } else {
            seasonTabsRecyclerView.setVisibility(View.GONE);
        }

        // Show server type selector
        serverTypeLayout.setVisibility(View.VISIBLE);

        // Load first season
        if (!seasons.isEmpty()) {
            selectedSeasonNumber = seasons.get(0).getSeasonNumber();
            seasonTabAdapter.setSelectedPosition(0);
            loadEpisodesForSeason(selectedSeasonNumber);
        }
    }

    private void loadEpisodesForSeason(int seasonNumber) {
        Map<Integer, Map<Integer, EpisodeModel>> episodesMap = anime.getEpisodes();

        if (episodesMap == null || !episodesMap.containsKey(seasonNumber)) {
            currentEpisodes.clear();
            episodeGridAdapter.notifyDataSetChanged();
            emptyEpisodesText.setText("No episodes in this season");
            emptyEpisodesText.setVisibility(View.VISIBLE);
            return;
        }

        Map<Integer, EpisodeModel> seasonEpisodes = episodesMap.get(seasonNumber);
        currentEpisodes.clear();

        // Add episodes in order
        for (int i = 1; i <= seasonEpisodes.size(); i++) {
            if (seasonEpisodes.containsKey(i)) {
                currentEpisodes.add(seasonEpisodes.get(i));
            }
        }

        episodeGridAdapter.notifyDataSetChanged();
        emptyEpisodesText.setVisibility(View.GONE);

        Log.i(TAG, "Loaded " + currentEpisodes.size() + " episodes for season " + seasonNumber);
    }

    private void onEpisodeClick(EpisodeModel episode, int position) {
        Log.i(TAG, "Episode clicked: S" + episode.getSeason() + "E" + episode.getEpisodeNumber());

        loadingOverlay.setVisibility(View.VISIBLE);
        loadingText.setText("Loading servers...");

        // Fetch servers for this episode
        executorService.execute(() -> {
            try {
                Map<String, Map<String, AnimekaiApi.ServerInfo>> servers =
                        animekaiApi.fetchEpisodeServers(episode.getEpisodeToken());

                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    currentServers = servers;
                    showServerSelectionDialog(episode, servers);
                });

            } catch (Exception e) {
                Log.e(TAG, "Error fetching servers", e);
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(DetailsActivityAnime.this,
                            "Failed to load servers: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void showServerSelectionDialog(EpisodeModel episode,
                                           Map<String, Map<String, AnimekaiApi.ServerInfo>> servers) {

        // Try to use selected server type first
        if (servers.containsKey(selectedServerType) &&
                !servers.get(selectedServerType).isEmpty()) {

            Map<String, AnimekaiApi.ServerInfo> typeServers = servers.get(selectedServerType);

            if (typeServers.size() == 1) {
                // Only one server, play directly
                AnimekaiApi.ServerInfo server = typeServers.values().iterator().next();
                playEpisode(episode, server);
            } else {
                // Multiple servers, show selection
                showServerList(episode, typeServers, selectedServerType.toUpperCase());
            }

        } else {
            // Selected type not available, show all available servers
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select Server Type");

            List<String> availableTypes = new ArrayList<>();
            if (servers.containsKey("sub")) availableTypes.add("SUB");
            if (servers.containsKey("dub")) availableTypes.add("DUB");
            if (servers.containsKey("softsub")) availableTypes.add("SOFTSUB");

            if (availableTypes.isEmpty()) {
                Toast.makeText(this, "No servers available", Toast.LENGTH_SHORT).show();
                return;
            }

            builder.setItems(availableTypes.toArray(new String[0]), (dialog, which) -> {
                String type = availableTypes.get(which).toLowerCase();
                Map<String, AnimekaiApi.ServerInfo> typeServers = servers.get(type);

                if (typeServers.size() == 1) {
                    AnimekaiApi.ServerInfo server = typeServers.values().iterator().next();
                    playEpisode(episode, server);
                } else {
                    showServerList(episode, typeServers, type.toUpperCase());
                }
            });

            builder.show();
        }
    }

    private void showServerList(EpisodeModel episode,
                                Map<String, AnimekaiApi.ServerInfo> servers,
                                String serverTypeName) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(serverTypeName + " Servers");

        List<String> serverNames = new ArrayList<>();
        List<AnimekaiApi.ServerInfo> serverList = new ArrayList<>();

        for (Map.Entry<String, AnimekaiApi.ServerInfo> entry : servers.entrySet()) {
            serverNames.add("Server " + (entry.getValue().index + 1));
            serverList.add(entry.getValue());
        }

        builder.setItems(serverNames.toArray(new String[0]), (dialog, which) -> {
            AnimekaiApi.ServerInfo selectedServer = serverList.get(which);
            playEpisode(episode, selectedServer);
        });

        builder.show();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void playEpisode(EpisodeModel episode, AnimekaiApi.ServerInfo server) {
        loadingOverlay.setVisibility(View.VISIBLE);
        loadingText.setText("Loading video...");

        executorService.execute(() -> {
            try {
                // Resolve embed URL to get video sources
                AnimekaiApi.MediaData mediaData = animekaiApi.resolveEmbedUrl(server.linkId);

                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);

                    List<String> sources = mediaData.getSources();
                    if (sources != null && !sources.isEmpty()) {
                        String videoUrl = sources.get(0);

                        Log.i(TAG, "Playing: " + videoUrl);

                        // Launch player
                        Intent intent = new Intent(DetailsActivityAnime.this, PlayerActivity.class);
                        intent.putExtra("video_url", videoUrl);
                        intent.putExtra("title", anime.getAnimeName());
                        intent.putExtra("subtitle", "S" + episode.getSeason() +
                                "E" + episode.getEpisodeNumber() +
                                " - " + episode.getEpisodeName());

                        // Add subtitles if available
                        if (mediaData.getTracks() != null && !mediaData.getTracks().isEmpty()) {
                            ArrayList<String> subtitleUrls = new ArrayList<>();
                            ArrayList<String> subtitleLabels = new ArrayList<>();

                            for (EpisodeModel.Track track : mediaData.getTracks()) {
                                subtitleUrls.add(track.getFile());
                                subtitleLabels.add(track.getLabel());
                            }

                            intent.putStringArrayListExtra("subtitle_urls", subtitleUrls);
                            intent.putStringArrayListExtra("subtitle_labels", subtitleLabels);
                        }

                        // Add skip info if available
                        EpisodeModel.SkipInfo skipInfo = mediaData.getSkipInfo();
                        if (skipInfo != null) {
                            if (skipInfo.getIntro() != null) {
                                intent.putExtra("intro_start", skipInfo.getIntro()[0]);
                                intent.putExtra("intro_end", skipInfo.getIntro()[1]);
                            }
                            if (skipInfo.getOutro() != null) {
                                intent.putExtra("outro_start", skipInfo.getOutro()[0]);
                                intent.putExtra("outro_end", skipInfo.getOutro()[1]);
                            }
                        }

                        startActivity(intent);

                    } else {
                        Toast.makeText(DetailsActivityAnime.this,
                                "No video sources found",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error resolving video", e);
                runOnUiThread(() -> {
                    loadingOverlay.setVisibility(View.GONE);
                    Toast.makeText(DetailsActivityAnime.this,
                            "Failed to load video: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (animekaiApi != null) {
            animekaiApi.shutdown();
        }
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}