package com.kiduyu.klaus.kiduyutv.Ui.details;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.TmdbRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.adapter.CompanyContentAdapter;
import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.Ui.details.movie.DetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.tv.DetailsActivityTv;

import java.util.List;

public class CompanyContentActivity extends AppCompatActivity implements CompanyContentAdapter.OnMediaItemClickListener {


    public static final String EXTRA_COMPANY_NETWORK = "extra_company_network";
    public static final String EXTRA_IS_NETWORK = "extra_is_network";

    //private Toolbar toolbar;
    private ImageView logoImageView;
    private TextView nameTextView;
    private TextView contentTypeTextView;
    private RecyclerView contentRecyclerView;
    private ProgressBar loadingProgressBar;
    private RelativeLayout loadingOverlay;
    private LinearLayout errorView;
    private TextView errorTextView;
    private AppCompatButton retryButton;

    private CompanyContentAdapter adapter;
    private TmdbRepository tmdbRepository;
    private CompanyNetwork companyNetwork;
    private boolean isNetwork;
    private int currentPage = 1;
    private boolean isLoading = false;

    public static Intent createIntent(Context context, CompanyNetwork companyNetwork) {
        Intent intent = new Intent(context, CompanyContentActivity.class);
        intent.putExtra(EXTRA_COMPANY_NETWORK, companyNetwork.getId());
        intent.putExtra(EXTRA_IS_NETWORK, companyNetwork.isNetwork());
        intent.putExtra("company_name", companyNetwork.getName());
        intent.putExtra("company_logo", companyNetwork.getLogoPath());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_company_content);

        // Get intent extras
        int companyId = getIntent().getIntExtra(EXTRA_COMPANY_NETWORK, -1);
        isNetwork = getIntent().getBooleanExtra(EXTRA_IS_NETWORK, false);
        String companyName = getIntent().getStringExtra("company_name");
        String companyLogo = getIntent().getStringExtra("company_logo");

        if (companyId == -1) {
            Toast.makeText(this, "Invalid company/network", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Create company object from intent data
        companyNetwork = new CompanyNetwork();
        companyNetwork.setId(companyId);
        companyNetwork.setName(companyName);
        companyNetwork.setLogoPath(companyLogo);
        companyNetwork.setNetwork(isNetwork);

        initViews();
        setupToolbar();
        setupRecyclerView();
        loadContent();
    }

    private void initViews() {
        //toolbar = findViewById(R.id.toolbar);
        logoImageView = findViewById(R.id.logoImageView);
        nameTextView = findViewById(R.id.nameTextView);
        contentTypeTextView = findViewById(R.id.contentTypeTextView);
        contentRecyclerView = findViewById(R.id.contentRecyclerView);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        errorView = findViewById(R.id.errorView);
        errorTextView = findViewById(R.id.errorTextView);
        retryButton = findViewById(R.id.retryButton);

        tmdbRepository = new TmdbRepository();

        // Set company/network info
        nameTextView.setText(companyNetwork.getName());
        contentTypeTextView.setText(isNetwork ? "TV Shows" : "Movies");

        if (companyNetwork.getLogoPath() != null && !companyNetwork.getLogoPath().isEmpty()) {
            Glide.with(logoImageView.getContext())
                    .load(companyNetwork.getLogoPath())
                    .centerCrop() // or .fitCenter() depending on your layout
                    .into(logoImageView);
        }

        retryButton.setOnClickListener(v -> {
            errorView.setVisibility(View.GONE);
            loadContent();
        });
    }

    private void setupToolbar() {
        //setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(companyNetwork.getName());
        }
        //toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRecyclerView() {
        adapter = new CompanyContentAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        contentRecyclerView.setLayoutManager(layoutManager);
        contentRecyclerView.setAdapter(adapter);

        contentRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                GridLayoutManager gridLayoutManager =
                        (GridLayoutManager) recyclerView.getLayoutManager();

                int visibleItemCount = gridLayoutManager.getChildCount();
                int totalItemCount = gridLayoutManager.getItemCount();
                int firstVisibleItem = gridLayoutManager.findFirstVisibleItemPosition();

                if (!isLoading
                        && (visibleItemCount + firstVisibleItem) >= totalItemCount - 5
                        && firstVisibleItem >= 0) {
                    loadMoreContent();
                }
            }
        });
    }
    private void loadContent() {
        showLoading(true);
        currentPage = 1;

        if (isNetwork) {
            tmdbRepository.discoverTVShowsByNetworkAsync(companyNetwork.getId(), currentPage, new TmdbRepository.TMDBCallback() {
                @Override
                public void onSuccess(List<MediaItems> movies) {
                    showLoading(false);
                    if (movies != null && !movies.isEmpty()) {
                        adapter.setItems(movies);
                    } else {
                        showError("No TV shows found");
                    }
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    showError(error);
                }
            });
        } else {
            tmdbRepository.discoverMoviesByCompanyAsync(companyNetwork.getId(), currentPage, new TmdbRepository.TMDBCallback() {
                @Override
                public void onSuccess(List<MediaItems> movies) {
                    showLoading(false);
                    if (movies != null && !movies.isEmpty()) {
                        adapter.setItems(movies);
                    } else {
                        showError("No movies found");
                    }
                }

                @Override
                public void onError(String error) {
                    showLoading(false);
                    showError(error);
                }
            });
        }
    }

    private void loadMoreContent() {
        if (isLoading) return;
        isLoading = true;
        currentPage++;

        if (isNetwork) {
            tmdbRepository.discoverTVShowsByNetworkAsync(companyNetwork.getId(), currentPage, new TmdbRepository.TMDBCallback() {
                @Override
                public void onSuccess(List<MediaItems> movies) {
                    isLoading = false;
                    if (movies != null && !movies.isEmpty()) {
                        adapter.addItems(movies);
                    }
                }

                @Override
                public void onError(String error) {
                    isLoading = false;
                }
            });
        } else {
            tmdbRepository.discoverMoviesByCompanyAsync(companyNetwork.getId(), currentPage, new TmdbRepository.TMDBCallback() {
                @Override
                public void onSuccess(List<MediaItems> movies) {
                    isLoading = false;
                    if (movies != null && !movies.isEmpty()) {
                        adapter.addItems(movies);
                    }
                }

                @Override
                public void onError(String error) {
                    isLoading = false;
                }
            });
        }
    }

    private void showLoading(boolean show) {
        loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorTextView.setText(message);
        errorView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onMediaItemClick(MediaItems mediaItem) {
        // Determine if it's a movie or TV show and open appropriate activity
        if ("tv".equalsIgnoreCase(mediaItem.getMediaType())) {
            Intent intent = new Intent(this, DetailsActivityTv.class);
            intent.putExtra("media_item", mediaItem);
            startActivity(intent);
        } else {
            Intent intent = new Intent(this, DetailsActivity.class);
            intent.putExtra("media_item", mediaItem);
            startActivity(intent);
        }
    }
}
