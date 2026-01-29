package com.kiduyu.klaus.kiduyutv.Ui.home;

import android.content.Intent;
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
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.movie.DetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.tv.DetailsActivityTv;
import com.kiduyu.klaus.kiduyutv.Ui.search.SearchActivity;
import com.kiduyu.klaus.kiduyutv.Ui.settings.SettingsActivity;
import com.kiduyu.klaus.kiduyutv.adapter.CategoryAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.VerticalSpaceItemDecoration;
import com.kiduyu.klaus.kiduyutv.model.CategorySection;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "NetflixMainActivity";

    // UI Components
    private ImageView heroBackgroundImage;

    private PreferencesManager preferencesManager;
    private ImageView searchIcon;
    private ImageView homeIcon;
    private ImageView moviesIcon;
    private ImageView tvIcon;
    private ImageView apiIcon;
    private ImageView myListIcon;
    private ImageView settingsIcon;
    private TextView nSeriesBadge;
    private TextView matchScoreText;
    private TextView contentTitle;
    private TextView yearText;
    private TextView maturityRating;
    private TextView durationText;
    private TextView qualityBadge;
    private TextView synopsisText;
    private Button playButton;
    private Button moreInfoButton;
    private RecyclerView categoriesRecyclerView;
    private RelativeLayout loadingOverlay;
    private ProgressBar loadingProgressBar;

    // Navigation
    private LinearLayout navigationSidebar;

    // Data
    private List<CategorySection> categories = new ArrayList<>();
    private CategoryAdapter categoryAdapter;
    private TmdbRepository tmdbRepository;

    //private MediaRepositoryTV mediaRepositorytv;
    //private MediaRepositoryVideasy apiRepository;
    private MediaItems currentSelectedItem;

    // Loading state
    private boolean isLoadingContent = false;
    private int loadedCategories = 0;
    private static final int TOTAL_CATEGORIES = 6; // Featured, Top Rated, Action, Comedy, Drama, Documentaries

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        tmdbRepository = new TmdbRepository();
        //apiRepository = new MediaRepositoryVideasy();
// CRITICAL: Keep screen on during playback - prevents TV from sleeping
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        preferencesManager = PreferencesManager.getInstance(this);

        initializeViews();
        setupClickListeners();
        setupNavigationFocus();
        loadContent();

    }

    private void initializeViews() {
        // Hero content views
        heroBackgroundImage = findViewById(R.id.heroBackgroundImage);
        searchIcon = findViewById(R.id.searchIcon);
        homeIcon = findViewById(R.id.homeIcon);
        moviesIcon = findViewById(R.id.moviesIcon);
        tvIcon = findViewById(R.id.tvIcon);
        apiIcon = findViewById(R.id.apiIcon);
        myListIcon = findViewById(R.id.myListIcon);
        settingsIcon = findViewById(R.id.settingsIcon);
        nSeriesBadge = findViewById(R.id.nSeriesBadge);
        matchScoreText = findViewById(R.id.matchScoreText);
        contentTitle = findViewById(R.id.contentTitle);
        yearText = findViewById(R.id.yearText);
        maturityRating = findViewById(R.id.maturityRating);
        durationText = findViewById(R.id.durationText);
        qualityBadge = findViewById(R.id.qualityBadge);
        synopsisText = findViewById(R.id.synopsisText);
        ///playButton = findViewById(R.id.playButton);
        //moreInfoButton = findViewById(R.id.moreInfoButton);

        // Navigation
        navigationSidebar = findViewById(R.id.navigationSidebar);

        // Categories RecyclerView
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        categoriesRecyclerView.setLayoutManager(layoutManager);
        categoriesRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(10));

        // Loading
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
    }

    private void setupNavigationFocus() {
        //homeIcon.requestFocus();
        //homeIcon.setSelected(true);


        View.OnFocusChangeListener navFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.setSelected(true);
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
            } else {
                v.setSelected(false);
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };

        searchIcon.setOnFocusChangeListener(navFocusListener);
        searchIcon.setSelected(false);
        homeIcon.setOnFocusChangeListener(navFocusListener);
        moviesIcon.setOnFocusChangeListener(navFocusListener);
        tvIcon.setOnFocusChangeListener(navFocusListener);
        apiIcon.setOnFocusChangeListener(navFocusListener);
        myListIcon.setOnFocusChangeListener(navFocusListener);
        settingsIcon.setOnFocusChangeListener(navFocusListener);
    }

    private void setupClickListeners() {
        /*playButton.setOnClickListener(v -> {
            if (currentSelectedItem != null) {
                if (currentSelectedItem.isFromAPI() || currentSelectedItem.isFromTMDB()) {
                    fetchAndPlay(currentSelectedItem);
                } else {
                    launchPlayer(currentSelectedItem);
                }
            }
        });

        moreInfoButton.setOnClickListener(v -> {
            if (currentSelectedItem != null) {
                launchDetails(currentSelectedItem);
            }
        });*/

        searchIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(intent);
        });

        homeIcon.setOnClickListener(v -> {
            loadContent();
            Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show();
        });

        moviesIcon.setOnClickListener(v -> {
            loadContent();
            Toast.makeText(this, "Movies - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        tvIcon.setOnClickListener(v -> {
            loadTvShows();
            Toast.makeText(this, "TV Shows - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        apiIcon.setOnClickListener(v -> {
            Toast.makeText(this, "API Content", Toast.LENGTH_SHORT).show();
        });

        myListIcon.setOnClickListener(v -> {
            Toast.makeText(this, "My List - Coming Soon", Toast.LENGTH_SHORT).show();
        });

        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void loadContent() {
        if (isLoadingContent) {
            Log.i(TAG, "Already loading content, skipping duplicate request");
            return;
        }

        isLoadingContent = true;
        loadedCategories = 0;
        loadingOverlay.setVisibility(View.VISIBLE);

        categories.clear();

        // Add API content category
        //categories.add(new CategorySection("🎆 Live API Content", apiRepository.getAPISampleContent()));

        // Setup adapter with initial empty structure
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(categories);
            categoriesRecyclerView.setAdapter(categoryAdapter);
            setupCategoryListeners();
        } else {
            categoryAdapter.notifyDataSetChanged();
        }

        loadContinueWatching();

        // Load all categories from TMDB asynchronously
        loadFeaturedMovies();

        loadTopRatedMovies();
        loadActionMovies();
//        loadComedyMovies();
//        loadDramaMovies();
//        loadDocumentaries();
    }

    // ============================================
    // Continue Watching Feature
    // ============================================

    /**
     * Load watch history and create a "Continue Watching" category
     * This category is placed at the top of the list
     */
    private void loadContinueWatching() {
        try {
            List<PreferencesManager.WatchHistoryItem> watchHistory = preferencesManager.getAllWatchHistory();

            // Filter out completed items (95% or more watched)
            List<PreferencesManager.WatchHistoryItem> activeHistory = new ArrayList<>();
            for (PreferencesManager.WatchHistoryItem item : watchHistory) {
                if (!item.isCompleted()) {
                    activeHistory.add(item);
                }
            }

            // Only create category if there are items to show
            if (!activeHistory.isEmpty()) {
                List<MediaItems> continueWatchingItems = new ArrayList<>();

                for (PreferencesManager.WatchHistoryItem historyItem : activeHistory) {
                    MediaItems mediaItem = convertToMediaItem(historyItem);
                    continueWatchingItems.add(mediaItem);
                }

                // Create the Continue Watching category at position 0
                CategorySection continueWatchingSection = new CategorySection("Continue Watching", continueWatchingItems);
                categories.add(0, continueWatchingSection);

                // Notify adapter that items were inserted at position 0
                categoryAdapter.notifyItemInserted(0);

                // Scroll to top to show the new category
                categoriesRecyclerView.scrollToPosition(0);

                // Set focus on the first item in Continue Watching after a short delay
                // This ensures the RecyclerView has time to layout
                categoriesRecyclerView.postDelayed(() -> {
                    setFocusOnFirstContinueWatchingItem();
                }, 100);

                Log.i(TAG, "Continue Watching category loaded with " + continueWatchingItems.size() + " items");
            } else {
                Log.i(TAG, "No active watch history found");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading continue watching: " + e.getMessage());
        }
    }

    /**
     * Convert WatchHistoryItem to MediaItems for display
     */
    private MediaItems convertToMediaItem(PreferencesManager.WatchHistoryItem historyItem) {
        MediaItems mediaItem = new MediaItems();
        mediaItem.setId(historyItem.id);
        mediaItem.setTitle(historyItem.title);
        mediaItem.setPosterUrl(historyItem.posterUrl);
        mediaItem.setBackgroundImageUrl(historyItem.backgroundImageUrl);
        mediaItem.setMediaType(historyItem.mediaType);
        mediaItem.setSeason(historyItem.season);
        mediaItem.setEpisode(historyItem.episode);
        mediaItem.setDescription(historyItem.description);
        if(historyItem.rating != null) {
            mediaItem.setRating(historyItem.rating);
        } else {
            mediaItem.setRating(0);
        }
        //mediaItem.setRating(historyItem.)
        //mediaItem.setRating(historyItem.rating);

        // Set year and duration based on progress
        if (historyItem.totalDuration > 0) {
            int totalMinutes = (int) (historyItem.totalDuration / 60000);
            int hours = totalMinutes / 60;
            int minutes = totalMinutes % 60;
            if (hours > 0) {
                mediaItem.setDuration(hours + "h " + minutes + "m");
            } else {
                mediaItem.setDuration(minutes + "m");
            }
        }

        // Set description with progress info
        int progress = historyItem.getProgressPercentage();
        if(historyItem.description.isEmpty()){
            mediaItem.setDescription("watched "+progress + "% -Tap to Continue Watching");
        } else {
            mediaItem.setDescription(historyItem.description);
        }


        // Mark as from history (not from API)
        mediaItem.setFromTMDB(false);

        return mediaItem;
    }

    /**
     * Set focus on the first item in the Continue Watching category
     */
    private void setFocusOnFirstContinueWatchingItem() {
        try {
            if (categories.isEmpty()) {
                return;
            }

            // First category should be Continue Watching
            CategorySection firstCategory = categories.get(0);
            if ("Continue Watching".equals(firstCategory.getCategoryName()) &&
                    !firstCategory.getItems().isEmpty()) {

                categoriesRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        RecyclerView.ViewHolder categoryViewHolder = categoriesRecyclerView.findViewHolderForAdapterPosition(0);

                        if (categoryViewHolder instanceof CategoryAdapter.CategoryViewHolder) { // Replace with your actual ViewHolder class
                            CategoryAdapter.CategoryViewHolder holder = (CategoryAdapter.CategoryViewHolder) categoryViewHolder;
                            RecyclerView itemsRecyclerView = holder.itemsRecyclerView; // Direct access if you have a reference

                            if (itemsRecyclerView != null) {
                                itemsRecyclerView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        RecyclerView.ViewHolder itemViewHolder = itemsRecyclerView.findViewHolderForAdapterPosition(0);
                                        if (itemViewHolder != null && itemViewHolder.itemView != null) {
                                            itemViewHolder.itemView.requestFocus();
                                            Log.i(TAG, "Focus set on first Continue Watching item");
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "Error setting focus on first item: " + e.getMessage());
        }
    }

    private void loadFeaturedMovies() {
        tmdbRepository.getFeaturedMoviesAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> movies) {
                Log.i(TAG, "Successfully loaded " + movies.size() + " featured movies");

                if (!movies.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Featured Movies", movies);
                    categories.add( featuredSection);
                    categoryAdapter.notifyDataSetChanged();

                    // Update hero content with first featured movie if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = movies.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }

                checkLoadingComplete();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load featured movies: " + error);
                checkLoadingComplete();
            }
        });
    }

    private void loadActionMovies() {
        tmdbRepository.getActionMoviesAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> movies) {
                Log.i(TAG, "Successfully loaded " + movies.size() + " action movies");

                if (!movies.isEmpty()) {
                    CategorySection section = new CategorySection("Action & Adventure", movies);
                    categories.add(section);
                    categoryAdapter.notifyDataSetChanged();
                }

                checkLoadingComplete();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load action movies: " + error);
                checkLoadingComplete();
            }
        });
    }

    private void loadTopRatedMovies() {
        tmdbRepository.getTopRatedMoviesAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> movies) {
                Log.i(TAG, "Successfully loaded " + movies.size() + " top rated movies");

                if (!movies.isEmpty()) {
                    CategorySection section = new CategorySection("Top Rated Movies", movies);
                    categories.add(section);
                    categoryAdapter.notifyDataSetChanged();
                }

                checkLoadingComplete();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load top rated movies: " + error);
                checkLoadingComplete();
            }
        });
    }

    private void checkLoadingComplete() {
        loadedCategories++;

        // if (loadedCategories >= TOTAL_CATEGORIES) {
        if (loadedCategories >= 1) {
            loadingOverlay.setVisibility(View.GONE);
            isLoadingContent = false;

            // If no hero content set yet, set from first available category
            if (currentSelectedItem == null && !categories.isEmpty()) {
                for (CategorySection section : categories) {
                    if (!section.getItems().isEmpty()) {
                        currentSelectedItem = section.getItems().get(0);
                        updateHeroContent(currentSelectedItem, 0);
                        break;
                    }
                }
            }
        }
    }

    private void loadTvShows() {
        if (isLoadingContent) {
            Log.i(TAG, "Already loading content, skipping duplicate request");
            return;
        }

        categoryAdapter = null;
        isLoadingContent = true;
        loadedCategories = 0;
        loadingOverlay.setVisibility(View.VISIBLE);

        categories.clear();

        // Add API content category
        //categories.add(new CategorySection("🎆 Live API Content", apiRepository.getAPISampleContent()));

        tmdbRepository.getPopularTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " featured Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Featured Tv Series", tvShows);
                    categories.add(0, featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);

                    // Update hero content with first featured tv show if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = tvShows.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Handle error
                Log.e(TAG, "Failed to load featured movies: " + error);
            }
        });

        tmdbRepository.getTopRatedTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " top rated Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Top Rated Tv Series", tvShows);
                    categories.add(0, featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);

                    // Update hero content with first featured tv show if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = tvShows.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Handle error
                Log.e(TAG, "Failed to load featured movies: " + error);
            }
        });

        tmdbRepository.getTrendingTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " Trending Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Trending Tv Series", tvShows);
                    categories.add(0, featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);

                    // Update hero content with first featured tv show if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = tvShows.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Handle error
                Log.e(TAG, "Failed to load featured movies: " + error);
            }
        });

        tmdbRepository.getActionAdventureTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " action Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Action & Adventure Tv Series", tvShows);
                    categories.add(0, featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);

                    // Update hero content with first featured tv show if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = tvShows.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }
            }

            @Override
            public void onError(String error) {
                // Handle error
                Log.e(TAG, "Failed to load featured movies: " + error);
            }
        });


        // Setup adapter with initial empty structure
        if (categoryAdapter == null) {
            categoryAdapter = new CategoryAdapter(categories);
            categoriesRecyclerView.setAdapter(categoryAdapter);
            setupCategoryListeners();
        } else {
            categoryAdapter.notifyDataSetChanged();
        }

        isLoadingContent = false;
        loadingOverlay.setVisibility(View.GONE);
    }

    private void setupCategoryListeners() {
        categoryAdapter.setOnItemClickListener(new CategoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(MediaItems mediaItems, int categoryPosition, int itemPosition) {
                currentSelectedItem = mediaItems;
                updateHeroContent(mediaItems, categoryPosition);
                if (mediaItems.getMediaType().toLowerCase().equals("movie")) {
                    launchDetails(currentSelectedItem);
                } else {
                    launchTvDetails(currentSelectedItem);
                }
                //launchDetails(currentSelectedItem);
            }

            @Override
            public void onItemFocusChanged(MediaItems mediaItems, int categoryPosition, int itemPosition, boolean hasFocus) {
                if (hasFocus) {
                    currentSelectedItem = mediaItems;
                    updateHeroContent(mediaItems, categoryPosition);
                }
            }
        });
    }

    private void updateHeroContent(MediaItems mediaItems, int categoryPosition) {
        String imageUrl = mediaItems.getPrimaryImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(heroBackgroundImage);
        }

        contentTitle.setText(mediaItems.getTitle());
        yearText.setText(mediaItems.getYear() > 0 ? String.valueOf(mediaItems.getYear()) : "");
        durationText.setText(mediaItems.getDuration());
        synopsisText.setText(mediaItems.getDescription());

        maturityRating.setText("PG-13");
        qualityBadge.setText(mediaItems.hasValidVideoSources() ? "HD" : "Trailer");

        int matchScore = 85 + (categoryPosition * 2);
        matchScoreText.setText(matchScore + "% Match");

        if (mediaItems.isFromAPI() || mediaItems.isFromTMDB()) {
            nSeriesBadge.setVisibility(View.VISIBLE);
            //nSeriesBadge.setText(mediaItems.isFromTMDB() ? "TMDB" : "N Series");
            nSeriesBadge.setText(categories.get(categoryPosition).getCategoryName());
        } else {
            nSeriesBadge.setVisibility(View.GONE);
        }

        //playButton.setEnabled(true);
        // moreInfoButton.setEnabled(true);
    }

    // When a TV show is clicked
    private void launchTvDetails(MediaItems tvShow) {
        Intent intent = new Intent(this, DetailsActivityTv.class);
        intent.putExtra("media_item", tvShow);
        startActivity(intent);
    }

    private void launchDetails(MediaItems mediaItems) {
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("media_item", mediaItems);
        startActivity(intent);
    }


}