package com.kiduyu.klaus.kiduyutv.Ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.kiduyu.klaus.kiduyutv.Ui.testactivity.TestActivity;
import com.kiduyu.klaus.kiduyutv.adapter.CategoryAdapter;
import com.kiduyu.klaus.kiduyutv.adapter.VerticalSpaceItemDecoration;
import com.kiduyu.klaus.kiduyutv.model.CategorySection;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
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

    // DPAD Navigation tracking
    private int currentCategoryIndex = 0;
    private int currentItemIndex = 0;
    private boolean isInitialFocusSet = false;
    private View lastFocusedView = null;

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
        loadContent();

        // Set up DPAD navigation handling
        setupDPADNavigation();
        setupNavigationFocus();

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
        searchIcon.setOnFocusChangeListener(navFocusListener);
        searchIcon.setSelected(false);
        homeIcon.setOnFocusChangeListener(navFocusListener);
        moviesIcon.setOnFocusChangeListener(navFocusListener);
        tvIcon.setOnFocusChangeListener(navFocusListener);

        myListIcon.setOnFocusChangeListener(navFocusListener);
        settingsIcon.setOnFocusChangeListener(navFocusListener);

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

        });

        tvIcon.setOnClickListener(v -> {
            loadTvShows();

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

                Log.i(TAG, "Continue Watching category loaded with " + continueWatchingItems.size() + " items");

                // Set focus on Continue Watching section since it exists
                setInitialFocusToContent();
                setFocusOnFirstContinueWatchingItem();
            } else {
                Log.i(TAG, "No active watch history found");
                // Set focus to first category when no continue watching
                setInitialFocusToContent();
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
            Log.i(TAG, "First category: " + firstCategory.getCategoryName());

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

    /**
     * Set initial focus to content area (not sidebar) for Android TV
     * Prioritizes Continue Watching section if available, otherwise first category
     */
    private void setInitialFocusToContent() {
        if (isInitialFocusSet || categories.isEmpty()) {
            return;
        }

        isInitialFocusSet = true;
        currentCategoryIndex = 0;
        currentItemIndex = 0;

        // Give the RecyclerView time to layout
        categoriesRecyclerView.postDelayed(() -> {
            requestFocusOnCategoryItem(0, 0);
        }, 200);
    }

    /**
     * Request focus on a specific category and item position
     * @param categoryIndex The category position
     * @param itemIndex The item position within that category
     */
    private void requestFocusOnCategoryItem(int categoryIndex, int itemIndex) {
        if (categoryIndex < 0 || categoryIndex >= categories.size()) {
            return;
        }

        CategorySection category = categories.get(categoryIndex);
        if (category.getItems().isEmpty()) {
            return;
        }

        // Clamp item index to valid range
        itemIndex = Math.max(0, Math.min(itemIndex, category.getItems().size() - 1));

        currentCategoryIndex = categoryIndex;
        currentItemIndex = itemIndex;

        // Scroll the category into view
        categoriesRecyclerView.scrollToPosition(categoryIndex);

        // Find the ViewHolder for this category
        RecyclerView.ViewHolder categoryViewHolder = categoriesRecyclerView.findViewHolderForAdapterPosition(categoryIndex);

        if (categoryViewHolder instanceof CategoryAdapter.CategoryViewHolder) {
            CategoryAdapter.CategoryViewHolder holder = (CategoryAdapter.CategoryViewHolder) categoryViewHolder;
            RecyclerView itemsRecyclerView = holder.itemsRecyclerView;

            if (itemsRecyclerView != null) {
                // Scroll to the item in the horizontal list
                LinearLayoutManager layoutManager = (LinearLayoutManager) itemsRecyclerView.getLayoutManager();
                if (layoutManager != null) {
                    layoutManager.scrollToPositionWithOffset(itemIndex, 16);
                }

                // Request focus on the specific item
                int finalItemIndex = itemIndex;
                itemsRecyclerView.postDelayed(() -> {
                    RecyclerView.ViewHolder itemViewHolder = itemsRecyclerView.findViewHolderForAdapterPosition(finalItemIndex);
                    if (itemViewHolder != null && itemViewHolder.itemView != null) {
                        // Clear focus from sidebar first
                        if (navigationSidebar != null) {
                            navigationSidebar.clearFocus();
                        }

                        itemViewHolder.itemView.requestFocus();
                        lastFocusedView = itemViewHolder.itemView;
                        Log.i(TAG, "Focus set on category " + categoryIndex + ", item " + finalItemIndex);
                    }
                }, 100);
            }
        }
    }

    /**
     * Set up DPAD navigation handling for Android TV
     * Handles UP, DOWN, LEFT, RIGHT navigation between categories and items
     */
    private void setupDPADNavigation() {
        // Handle key events for DPAD navigation
        categoriesRecyclerView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) {
                return false;
            }

            // Clear focus from sidebar when navigating content
            if (navigationSidebar != null && navigationSidebar.hasFocus()) {
                navigationSidebar.clearFocus();
            }

            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    return handleDPADRight();

                case KeyEvent.KEYCODE_DPAD_LEFT:
                    return handleDPADLeft();

                case KeyEvent.KEYCODE_DPAD_DOWN:
                    return handleDPADDown();

                case KeyEvent.KEYCODE_DPAD_UP:
                    return handleDPADUp();

                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    return handleDPADCenter();

                default:
                    return false;
            }
        });
    }

    /**
     * Handle DPAD RIGHT key - move to next item in current category
     */
    private boolean handleDPADRight() {
        if (categories.isEmpty()) return false;

        CategorySection currentCategory = categories.get(currentCategoryIndex);
        int maxItems = currentCategory.getItems().size();

        if (currentItemIndex < maxItems - 1) {
            // Move to next item in same category
            currentItemIndex++;
            focusCurrentItem();
            Log.i(TAG, "DPAD RIGHT: category=" + currentCategoryIndex + ", item=" + currentItemIndex);
            return true;
        } else {
            // At end of current category, try to move to next category
            if (currentCategoryIndex < categories.size() - 1) {
                currentCategoryIndex++;
                currentItemIndex = 0;
                focusCurrentItem();
                Log.i(TAG, "DPAD RIGHT (wrap to next category): category=" + currentCategoryIndex + ", item=" + currentItemIndex);
                return true;
            }
        }
        return false;
    }

    /**
     * Handle DPAD LEFT key - move to previous item in current category
     */
    private boolean handleDPADLeft() {
        if (categories.isEmpty()) return false;

        if (currentItemIndex > 0) {
            // Move to previous item in same category
            currentItemIndex--;
            focusCurrentItem();
            Log.i(TAG, "DPAD LEFT: category=" + currentCategoryIndex + ", item=" + currentItemIndex);
            return true;
        } else {
            // At beginning of current category, try to move to previous category
            if (currentCategoryIndex > 0) {
                currentCategoryIndex--;
                // Go to last item of previous category
                currentItemIndex = Math.max(0, categories.get(currentCategoryIndex).getItems().size() - 1);
                focusCurrentItem();
                Log.i(TAG, "DPAD LEFT (wrap to prev category): category=" + currentCategoryIndex + ", item=" + currentItemIndex);
                return true;
            }
        }
        return false;
    }

    /**
     * Handle DPAD DOWN key - move to next category
     */
    private boolean handleDPADDown() {
        if (categories.isEmpty()) return false;

        if (currentCategoryIndex < categories.size() - 1) {
            currentCategoryIndex++;
            // Keep same item index if available, otherwise use last available
            CategorySection newCategory = categories.get(currentCategoryIndex);
            if (currentItemIndex >= newCategory.getItems().size()) {
                currentItemIndex = Math.max(0, newCategory.getItems().size() - 1);
            }
            focusCurrentItem();
            Log.i(TAG, "DPAD DOWN: category=" + currentCategoryIndex + ", item=" + currentItemIndex);
            return true;
        }
        return false;
    }

    /**
     * Handle DPAD UP key - move to previous category
     */
    private boolean handleDPADUp() {
        if (categories.isEmpty()) return false;

        if (currentCategoryIndex > 0) {
            currentCategoryIndex--;
            // Keep same item index if available, otherwise use last available
            CategorySection newCategory = categories.get(currentCategoryIndex);
            if (currentItemIndex >= newCategory.getItems().size()) {
                currentItemIndex = Math.max(0, newCategory.getItems().size() - 1);
            }
            focusCurrentItem();
            Log.i(TAG, "DPAD UP: category=" + currentCategoryIndex + ", item=" + currentItemIndex);
            return true;
        }
        return false;
    }

    /**
     * Handle DPAD CENTER/ENTER key - launch the selected content
     */
    private boolean handleDPADCenter() {
        if (categories.isEmpty() || currentCategoryIndex >= categories.size()) {
            return false;
        }

        CategorySection category = categories.get(currentCategoryIndex);
        if (currentItemIndex >= category.getItems().size()) {
            return false;
        }

        MediaItems selectedItem = category.getItems().get(currentItemIndex);
        if (selectedItem != null) {
            currentSelectedItem = selectedItem;

            // Launch details activity based on media type
            if ("movie".equalsIgnoreCase(selectedItem.getMediaType()) ||
                    "Movie".equalsIgnoreCase(selectedItem.getMediaType())) {
                launchDetails(selectedItem);
            } else {
                launchTvDetails(selectedItem);
            }
            return true;
        }
        return false;
    }

    /**
     * Focus on the current category/item position
     */
    private void focusCurrentItem() {
        requestFocusOnCategoryItem(currentCategoryIndex, currentItemIndex);
    }

    /**
     * Get current category index (for external access)
     */
    public int getCurrentCategoryIndex() {
        return currentCategoryIndex;
    }

    /**
     * Get current item index within category (for external access)
     */
    public int getCurrentItemIndex() {
        return currentItemIndex;
    }

    /**
     * Check if Continue Watching section exists
     */
    private boolean hasContinueWatching() {
        for (CategorySection category : categories) {
            if ("Continue Watching".equals(category.getCategoryName())) {
                return true;
            }
        }
        return false;
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
                    categories.add( featuredSection);
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
                    categories.add(1, featuredSection);
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