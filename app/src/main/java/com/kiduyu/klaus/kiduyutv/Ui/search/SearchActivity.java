package com.kiduyu.klaus.kiduyutv.Ui.search;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.kiduyutv.Api.TmdbApi;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.movie.DetailsActivity;
import com.kiduyu.klaus.kiduyutv.adapter.SearchResultsAdapter;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    private static final int VOICE_SEARCH_REQUEST_CODE = 1001;
    private static final int PERMISSION_REQUEST_CODE = 1002;
    private static final int SEARCH_DEBOUNCE_DELAY = 500; // 500ms

    // UI Components
    private EditText searchEditText;
    private ImageButton voiceSearchButton;
    private ImageButton clearSearchButton;
    private Button filterAllButton;
    private Button filterMoviesButton;
    private Button filterTVButton;
    private RecyclerView searchResultsRecyclerView;
    private FrameLayout searchHistoryContainer;
    private FrameLayout trendingContainer;
    private TextView noResultsText;
    private ProgressBar loadingProgressBar;
    private TextView searchHintText;

    // Data
    private SearchResultsAdapter resultsAdapter;
    private TmdbRepository tmdbRepository;
    private PreferencesManager preferencesManager;
    private List<MediaItems> currentResults = new ArrayList<>();
    private List<String> searchHistory = new ArrayList<>();
    private List<String> trendingSearches = new ArrayList<>();
    private TmdbApi.ContentType currentFilter = TmdbApi.ContentType.ALL;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_search);

        tmdbRepository =  new TmdbRepository();
        preferencesManager = PreferencesManager.getInstance(this);

        initializeViews();
        setupListeners();
        loadInitialData();

    }
    private void initializeViews() {
        searchEditText = findViewById(R.id.searchEditText);
        voiceSearchButton = findViewById(R.id.voiceSearchButton);
        clearSearchButton = findViewById(R.id.clearSearchButton);
        filterAllButton = findViewById(R.id.filterAllButton);
        filterMoviesButton = findViewById(R.id.filterMoviesButton);
        filterTVButton = findViewById(R.id.filterTVButton);
        searchResultsRecyclerView = findViewById(R.id.searchResultsRecyclerView);
        searchHistoryContainer = findViewById(R.id.searchHistoryContainer);
        trendingContainer = findViewById(R.id.trendingContainer);
        noResultsText = findViewById(R.id.noResultsText);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        searchHintText = findViewById(R.id.searchHintText);

        // Setup RecyclerView
        resultsAdapter = new SearchResultsAdapter(new ArrayList<>());
        resultsAdapter.setOnItemClickListener(this::onSearchResultClick);
        searchResultsRecyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        searchResultsRecyclerView.setAdapter(resultsAdapter);

        // Set initial filter
        updateFilterButtonStates();
    }

    private void setupListeners() {
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus) {
                v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                if (!(v instanceof EditText)) {
                    v.setBackgroundResource(R.drawable.generic_focus_selector);
                }
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                if (!(v instanceof EditText)) {
                    v.setBackground(null);
                }
            }
        };

        searchEditText.setOnFocusChangeListener(focusChangeListener);
        voiceSearchButton.setOnFocusChangeListener(focusChangeListener);
        clearSearchButton.setOnFocusChangeListener(focusChangeListener);
        filterAllButton.setOnFocusChangeListener(focusChangeListener);
        filterMoviesButton.setOnFocusChangeListener(focusChangeListener);
        filterTVButton.setOnFocusChangeListener(focusChangeListener);

        // Search text watcher with debounce
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);

                if (searchRunnable != null) {
                    searchHandler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    String query = s.toString().trim();
                    if (query.isEmpty()) {
                        showInitialState();
                    } else {
                        performSearch(query);
                    }
                };

                searchHandler.postDelayed(searchRunnable, SEARCH_DEBOUNCE_DELAY);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Clear search button
        clearSearchButton.setOnClickListener(v -> {
            searchEditText.setText("");
            searchEditText.requestFocus();
            showInitialState();
        });

        // Voice search button
        voiceSearchButton.setOnClickListener(v -> {
            if (preferencesManager.isVoiceSearchEnabled()) {
                startVoiceSearch();
            } else {
                Toast.makeText(this, "Voice search is disabled in settings", Toast.LENGTH_SHORT).show();
            }
        });

        // Filter buttons
        filterAllButton.setOnClickListener(v -> setFilter(TmdbApi.ContentType.ALL));
        filterMoviesButton.setOnClickListener(v -> setFilter(TmdbApi.ContentType.MOVIE));
        filterTVButton.setOnClickListener(v -> setFilter(TmdbApi.ContentType.TV));

        // Handle keyboard search action
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                String query = searchEditText.getText().toString().trim();
                if (!query.isEmpty()) {
                    performSearch(query);
                    return true;
                }
            }
            return false;
        });
    }

    private void loadInitialData() {
        // Load search history and trending searches
        searchHistory = preferencesManager.getSearchHistory();
        tmdbRepository.getTrendingSearches(new TmdbRepository.TMDBSCallback<List<String>>() {
            @Override
            public void onSuccess(List<String> trending) {
                trendingSearches = trending;
                showInitialState();
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading trending searches", new Exception(error));
                showInitialState();
            }
        });
    }

    private void showInitialState() {
        searchResultsRecyclerView.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);
        loadingProgressBar.setVisibility(View.GONE);
        searchHintText.setVisibility(View.VISIBLE);

        // Show search history if available
        if (!searchHistory.isEmpty()) {
            searchHistoryContainer.setVisibility(View.VISIBLE);
            displaySearchHistory();
        } else {
            searchHistoryContainer.setVisibility(View.GONE);
        }

        // Always show trending searches
        trendingContainer.setVisibility(View.VISIBLE);
        displayTrendingSearches();
    }

    private void displaySearchHistory() {
        TextView historyTitle = findViewById(R.id.historyTitle);
        RecyclerView historyRecyclerView = findViewById(R.id.historyRecyclerView);

        historyTitle.setText("Recent Searches");

        // Create simple history adapter
        androidx.recyclerview.widget.LinearLayoutManager layoutManager =
                new androidx.recyclerview.widget.LinearLayoutManager(this,
                        androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false);
        historyRecyclerView.setLayoutManager(layoutManager);

        // Simple text adapter for history
        android.widget.ArrayAdapter<String> historyAdapter = new android.widget.ArrayAdapter<>(
                this, R.layout.item_search_history, searchHistory);
        //historyRecyclerView.setAdapter(historyAdapter);
    }

    private void displayTrendingSearches() {
        TextView trendingTitle = findViewById(R.id.trendingTitle);
        android.widget.LinearLayout trendingChipsContainer = findViewById(R.id.trendingChipsContainer);

        trendingTitle.setText("Trending Searches");
        trendingChipsContainer.removeAllViews();

        for (String trending : trendingSearches) {
            Button chip = new Button(this);
            chip.setText(trending);
            chip.setBackgroundResource(R.drawable.chip_background);
            chip.setTextColor(getResources().getColor(android.R.color.white));
            chip.setPadding(32, 16, 32, 16);

            chip.setOnClickListener(v -> {
                searchEditText.setText(trending);
                performSearch(trending);
            });

            trendingChipsContainer.addView(chip);
        }
    }

    private void setFilter(TmdbApi.ContentType filter) {
        currentFilter = filter;
        updateFilterButtonStates();

        // Re-run search with new filter if there's text
        String query = searchEditText.getText().toString().trim();
        if (!query.isEmpty()) {
            performSearch(query);
        }
    }

    private void updateFilterButtonStates() {
        // Reset all buttons
        filterAllButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterMoviesButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        filterTVButton.setBackgroundColor(getResources().getColor(android.R.color.transparent));

        filterAllButton.setTextColor(getResources().getColor(android.R.color.white));
        filterMoviesButton.setTextColor(getResources().getColor(android.R.color.white));
        filterTVButton.setTextColor(getResources().getColor(android.R.color.white));

        // Highlight selected filter
        switch (currentFilter) {
            case ALL:
                filterAllButton.setBackgroundColor(getResources().getColor(R.color.cinema_red));
                filterAllButton.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case MOVIE:
                filterMoviesButton.setBackgroundColor(getResources().getColor(R.color.cinema_red));
                filterMoviesButton.setTextColor(getResources().getColor(android.R.color.white));
                break;
            case TV:
                filterTVButton.setBackgroundColor(getResources().getColor(R.color.cinema_red));
                filterTVButton.setTextColor(getResources().getColor(android.R.color.white));
                break;
        }
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            showInitialState();
            return;
        }

        if (isLoading) return;

        isLoading = true;
        loadingProgressBar.setVisibility(View.VISIBLE);
        searchHintText.setVisibility(View.GONE);
        searchHistoryContainer.setVisibility(View.GONE);
        trendingContainer.setVisibility(View.GONE);
        noResultsText.setVisibility(View.GONE);

        Log.d(TAG, "Searching for: " + query + " (filter: " + currentFilter + ")");

        // Add to search history
        preferencesManager.addToSearchHistory(query);

        // Perform search based on filter
        switch (currentFilter) {
            case ALL:
                tmdbRepository.searchAllContent(query, 1, searchCallback);
                break;
            case MOVIE:
                tmdbRepository.searchMovies(query, 1, searchCallback);
                break;
            case TV:
                tmdbRepository.searchTVShows(query, 1, searchCallback);
                break;
        }
    }

    private TmdbRepository.TMDBSCallback<List<MediaItems>> searchCallback = new TmdbRepository.TMDBSCallback<List<MediaItems>>() {
        @Override
        public void onSuccess(List<MediaItems> results) {
            isLoading = false;
            loadingProgressBar.setVisibility(View.GONE);

            currentResults = results;

            if (results.isEmpty()) {
                noResultsText.setVisibility(View.VISIBLE);
                searchResultsRecyclerView.setVisibility(View.GONE);
                noResultsText.setText("No results found for \"" + searchEditText.getText().toString() + "\"");
            } else {
                resultsAdapter.updateResults(results);
                searchResultsRecyclerView.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onError(String error) {
            isLoading = false;
            loadingProgressBar.setVisibility(View.GONE);

            Log.e(TAG, "Search error: " + error);
            noResultsText.setVisibility(View.VISIBLE);
            noResultsText.setText("Search failed: " + error);
            Toast.makeText(SearchActivity.this, "Search failed: " + error, Toast.LENGTH_SHORT).show();
        }
    };

    private void onSearchResultClick(MediaItems mediaItems, int position) {
        // Add to search history if not already there
        String title = mediaItems.getTitle();
        if (!searchHistory.contains(title)) {
            preferencesManager.addToSearchHistory(title);
        }

        // Launch details activity
        Intent intent = new Intent(this, DetailsActivity.class);
        intent.putExtra("media_item", mediaItems);
        startActivity(intent);
    }

    private void startVoiceSearch() {
        // Check microphone permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    PERMISSION_REQUEST_CODE);
            return;
        }

        // Start voice recognition
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What would you like to search for?");

        startActivityForResult(intent, VOICE_SEARCH_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == VOICE_SEARCH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            List<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String query = results.get(0);
                searchEditText.setText(query);
                performSearch(query);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceSearch();
            } else {
                Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!searchEditText.getText().toString().isEmpty()) {
            searchEditText.setText("");
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchHandler != null && searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }
}