package com.kiduyu.klaus.kiduyutv.Ui.home;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.WindowManager;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.CompanyContentActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.movie.DetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.tv.DetailsActivityTv;
import com.kiduyu.klaus.kiduyutv.Ui.search.SearchActivity;
import com.kiduyu.klaus.kiduyutv.Ui.settings.SettingsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.testactivity.TestActivity;
import com.kiduyu.klaus.kiduyutv.adapter.CategoryAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.VerticalSpaceItemDecoration;
import com.kiduyu.klaus.kiduyutv.model.CategorySection;
import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.DialogClass;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;



public class MainActivity extends AppCompatActivity {
    private static final String TAG = "NetflixMainActivity";

    // UI Components
    private ImageView heroBackgroundImage;

    private PreferencesManager preferencesManager;
    private ImageView searchIcon;
    private ImageView homeIcon;
    private ImageView moviesIcon;

    private ImageView tvIcon;
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
    LinearLayout sidebar;
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

    private AlertDialog loadingDialog;

    // DPAD Navigation tracking
    private int currentCategoryIndex = 0;
    private int currentItemIndex = 0;
    private boolean isInitialFocusSet = false;
    private View lastFocusedView = null;

    private DialogClass dialogClass ;

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
        dialogClass = new DialogClass();



    }

    private void initializeViews() {
        // Hero content views
        heroBackgroundImage = findViewById(R.id.heroBackgroundImage);
        searchIcon = findViewById(R.id.searchIcon);
        homeIcon = findViewById(R.id.homeIcon);
        moviesIcon = findViewById(R.id.moviesIcon);
        tvIcon = findViewById(R.id.tvIcon);

        myListIcon = findViewById(R.id.myListIcon);
        settingsIcon = findViewById(R.id.settingsIcon);
        nSeriesBadge = findViewById(R.id.nSeriesBadge);
        nSeriesBadge.setVisibility(View.GONE);
        matchScoreText = findViewById(R.id.matchScoreText);
        matchScoreText.setVisibility(View.GONE);
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

        // Prevent sidebar from taking focus automatically on startup
        navigationSidebar.setFocusable(false);
        navigationSidebar.setFocusableInTouchMode(false);

        // Categories RecyclerView
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        categoriesRecyclerView.setLayoutManager(layoutManager);
        categoriesRecyclerView.addItemDecoration(new VerticalSpaceItemDecoration(10));

        // Loading
        loadingOverlay = findViewById(R.id.loadingOverlay);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        sidebar = findViewById(R.id.navigationSidebar);

        sidebar.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // Enlarge sidebar
                ViewGroup.LayoutParams params = sidebar.getLayoutParams();
                params.width = (int) getResources().getDimension(R.dimen.sidebar_expanded_width);
                sidebar.setLayoutParams(params);

                // Show labels
                showSidebarLabels(true);
            } else {
                // Shrink sidebar
                ViewGroup.LayoutParams params = sidebar.getLayoutParams();
                params.width = (int) getResources().getDimension(R.dimen.sidebar_collapsed_width);
                sidebar.setLayoutParams(params);

                // Hide labels
                showSidebarLabels(false);
            }
        });
    }

    private void showSidebarLabels(boolean show) {
        int visibility = show ? View.VISIBLE : View.GONE;

        findViewById(R.id.searchLabel).setVisibility(visibility);
        findViewById(R.id.homeLabel).setVisibility(visibility);
        findViewById(R.id.moviesLabel).setVisibility(visibility);
        findViewById(R.id.tvLabel).setVisibility(visibility);

        findViewById(R.id.myListLabel).setVisibility(visibility);
        findViewById(R.id.settingsLabel).setVisibility(visibility);
    }

    private void setupNavigationFocus() {
        View.OnFocusChangeListener navFocusListener = (v, hasFocus) -> {
            if (hasFocus) {
                ViewGroup.LayoutParams params = sidebar.getLayoutParams();
                params.width = (int) getResources().getDimension(R.dimen.sidebar_expanded_width);
                sidebar.setLayoutParams(params);
                v.setSelected(true);
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                // Show labels
                showSidebarLabels(true);
            } else {
                // Shrink sidebar
                ViewGroup.LayoutParams params = sidebar.getLayoutParams();
                params.width = (int) getResources().getDimension(R.dimen.sidebar_collapsed_width);
                sidebar.setLayoutParams(params);

                // Hide labels
                showSidebarLabels(false);
                v.setSelected(false);
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
            }
        };

        // Setup navigation icons with focus listeners
        /**
        searchIcon.setOnFocusChangeListener(navFocusListener);
        searchIcon.setSelected(false);
        homeIcon.setOnFocusChangeListener(navFocusListener);
        moviesIcon.setOnFocusChangeListener(navFocusListener);
        tvIcon.setOnFocusChangeListener(navFocusListener);

        myListIcon.setOnFocusChangeListener(navFocusListener);

        **/

        // Set initial focus to first navigation item
        //searchIcon.requestFocus();
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
            //Toast.makeText(this, "TV Shows - Coming Soon", Toast.LENGTH_SHORT).show();

        });



        myListIcon.setOnClickListener(v -> {
            Toast.makeText(this, "My List - Coming Soon", Toast.LENGTH_SHORT).show();
            launcTestActivity();
        });

        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    private void launcTestActivity() {
        Intent intent = new Intent(MainActivity.this, TestActivity.class);
        startActivity(intent);
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
            //remove this later
            //preferencesManager.clearWatchHistory();
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
                //categoryAdapter.getItemId(0).

                // Scroll to top to show the new category
                categoriesRecyclerView.scrollToPosition(0);
                categoriesRecyclerView.requestFocus();
                checkLoadingComplete();

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
        mediaItem.setTmdbId(historyItem.tmdbId);
        mediaItem.setRating(historyItem.rating);

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
        if (loadedCategories >= 3) {
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

    private void loadTopProductionCompanies(){

    }
    private void loadTvShows() {
        if (isLoadingContent) {
            Log.i(TAG, "Already loading content, skipping duplicate request");
            return;
        }


        dialogClass.showLoadingDialog(this, "Loading Tv Shows...");

        categoryAdapter = null;
        isLoadingContent = true;
        loadedCategories = 0;
        loadingOverlay.setVisibility(View.VISIBLE);

        categories.clear();

        // Add API content category
        //categories.add(new CategorySection("🎆 Live API Content", apiRepository.getAPISampleContent()));
/**
        tmdbRepository.getPopularTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " featured Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Featured Tv Series", tvShows);
                    categories.add( featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);
                    checkLoadingComplete();

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
                    categories.add(featuredSection);
                    categoryAdapter.notifyDataSetChanged();
                    currentSelectedItem = tvShows.get(0);
                    updateHeroContent(currentSelectedItem, 0);

                    // Update hero content with first featured tv show if it's the first category loaded
                    if (currentSelectedItem == null) {
                        currentSelectedItem = tvShows.get(0);
                        updateHeroContent(currentSelectedItem, 0);
                    }
                }
                checkLoadingComplete();
            }

            @Override
            public void onError(String error) {
                // Handle error
                Log.e(TAG, "Failed to load featured movies: " + error);
            }
        }); **/

        tmdbRepository.getTrendingTVShowsAsync(new TmdbRepository.TMDBCallback() {
            @Override
            public void onSuccess(List<MediaItems> tvShows) {
                // Handle successful load
                Log.i(TAG, "Successfully loaded " + tvShows.size() + " Trending Tv series");
                if (!tvShows.isEmpty()) {
                    CategorySection featuredSection = new CategorySection("Trending Tv Series", tvShows);
                    categories.add(featuredSection);
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
                    categories.add(featuredSection);
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

        // Load top production companies
        tmdbRepository.getTopProductionCompaniesAsync(new TmdbRepository.CompanyNetworkCallback() {
            @Override
            public void onSuccess(List<CompanyNetwork> companiesNetworks) {
                Log.i(TAG, "Successfully loaded " + companiesNetworks.size() + " production companies");
                if (!companiesNetworks.isEmpty()) {
                    CategorySection companySection = new CategorySection("Production Companies", companiesNetworks, CategorySection.TYPE_COMPANY_NETWORK);
                    categories.add(companySection);
                    if (categoryAdapter != null) {
                        categoryAdapter.notifyDataSetChanged();
                    }
                    dialogClass.hideLoadingDialog();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load production companies: " + error);
            }
        });

        // Load top TV networks
        tmdbRepository.getTopTVNetworksAsync(new TmdbRepository.CompanyNetworkCallback() {
            @Override
            public void onSuccess(List<CompanyNetwork> companiesNetworks) {
                Log.i(TAG, "Successfully loaded " + companiesNetworks.size() + " TV networks");
                if (!companiesNetworks.isEmpty()) {
                    CategorySection networkSection = new CategorySection("TV Networks", companiesNetworks, CategorySection.TYPE_COMPANY_NETWORK);
                    categories.add(networkSection);
                    if (categoryAdapter != null) {
                        categoryAdapter.notifyDataSetChanged();
                    }
                    //dialogClass.hideLoadingDialog();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Failed to load TV networks: " + error);
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
        //


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

        // Add company/network click listener
        categoryAdapter.setOnCompanyNetworkClickListener(new CategoryAdapter.OnCompanyNetworkClickListener() {
            @Override
            public void onCompanyNetworkClick(CompanyNetwork companyNetwork, int categoryPosition, int itemPosition) {
                // Open CompanyContentActivity to show movies/TV shows for this company/network
                Intent intent = CompanyContentActivity.createIntent(MainActivity.this, companyNetwork);
                startActivity(intent);
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