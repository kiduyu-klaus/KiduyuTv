package com.kiduyu.klaus.kiduyutv.Ui.details.actor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.Api.CastRepository;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.movie.DetailsActivity;
import com.kiduyu.klaus.kiduyutv.Ui.details.tv.DetailsActivityTv;
import com.kiduyu.klaus.kiduyutv.adapter.FilmographyAdapter;
import com.kiduyu.klaus.kiduyutv.model.CastMember;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity displaying detailed information about an actor/actress
 * including biography and filmography (movies and TV shows)
 */
public class ActorDetailsActivity extends AppCompatActivity {
    private static final String TAG = "ActorDetailsActivity";

    // UI Components
    private ImageView backgroundImage;
    private ImageView profileImage;
    private TextView nameText;
    private TextView knownForText;
    private TextView birthInfoText;
    private TextView placeOfBirthText;
    private TextView popularityText;
    private TextView biographyText;
    private RecyclerView filmographyRecyclerView;
    private ProgressBar loadingProgress;
    private ProgressBar filmographyLoading;
    private TextView emptyFilmographyText;

    // Data
    private CastMember castMember;
    private CastRepository castRepository;
    private FilmographyAdapter filmographyAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_actor_details);

        castRepository = new CastRepository();

        // Get cast member from intent
        castMember = getIntent().getParcelableExtra("cast_member");
        if (castMember == null) {
            Toast.makeText(this, "Actor not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupRecyclerView();
        loadActorDetails();
    }

    private void initializeViews() {
        backgroundImage = findViewById(R.id.backgroundImage);
        profileImage = findViewById(R.id.profileImage);
        nameText = findViewById(R.id.nameText);
        knownForText = findViewById(R.id.knownForText);
        birthInfoText = findViewById(R.id.birthInfoText);
        placeOfBirthText = findViewById(R.id.placeOfBirthText);
        popularityText = findViewById(R.id.popularityText);
        biographyText = findViewById(R.id.biographyText);
        filmographyRecyclerView = findViewById(R.id.filmographyRecyclerView);
        loadingProgress = findViewById(R.id.loadingProgress);
        filmographyLoading = findViewById(R.id.filmographyLoading);
        emptyFilmographyText = findViewById(R.id.emptyFilmographyText);
    }

    private void setupRecyclerView() {
        // Filmography grid - 5 columns
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 5);
        filmographyRecyclerView.setLayoutManager(gridLayoutManager);

        filmographyAdapter = new FilmographyAdapter(new ArrayList<>());
        filmographyRecyclerView.setAdapter(filmographyAdapter);

        filmographyAdapter.setOnCreditClickListener(this::openMediaDetails);
    }

    private void loadActorDetails() {
        loadingProgress.setVisibility(View.VISIBLE);

        // First load profile image if available
        String profileUrl = castMember.getProfileImageUrlHighRes();
        if (profileUrl != null && !profileUrl.isEmpty()) {
            Glide.with(this)
                    .load(profileUrl)
                    .centerCrop()
                    .into(profileImage);

            // Also set as background
            Glide.with(this)
                    .load(profileUrl)
                    .centerCrop()
                    .into(backgroundImage);
        }

        // Set basic info
        nameText.setText(castMember.getName());
        knownForText.setText(castMember.getKnownForDepartment());

        // Birth info
        if (castMember.getBirthday() != null && !castMember.getBirthday().isEmpty()) {
            birthInfoText.setText("Born: " + formatDate(castMember.getBirthday()));

            int age = castMember.getAge();
            if (age > 0) {
                birthInfoText.setText("Born: " + formatDate(castMember.getBirthday()) + " (" + age + " years old)");
            }
        } else {
            birthInfoText.setText("Born: Unknown");
        }

        // Place of birth
        if (castMember.getPlaceOfBirth() != null && !castMember.getPlaceOfBirth().isEmpty()) {
            placeOfBirthText.setText(castMember.getPlaceOfBirth());
            placeOfBirthText.setVisibility(View.VISIBLE);
        } else {
            placeOfBirthText.setVisibility(View.GONE);
        }

        // Popularity
        popularityText.setText(String.format("Popularity: %.1f", castMember.getPopularity()));

        // Biography
        if (castMember.getBiography() != null && !castMember.getBiography().isEmpty()) {
            biographyText.setText(castMember.getBiography());
        } else {
            biographyText.setText("No biography available for this actor/actress.");
        }

        // Now fetch detailed info and filmography
        castRepository.getCastDetails(castMember.getId(), new CastRepository.CastDetailsCallback() {
            @Override
            public void onSuccess(CastMember detailedCast) {
                loadingProgress.setVisibility(View.GONE);

                // Update with detailed info
                castMember = detailedCast;

                // Update biography
                if (detailedCast.getBiography() != null && !detailedCast.getBiography().isEmpty()) {
                    biographyText.setText(detailedCast.getBiography());
                }

                // Update birth info
                if (detailedCast.getBirthday() != null && !detailedCast.getBirthday().isEmpty()) {
                    birthInfoText.setText("Born: " + formatDate(detailedCast.getBirthday()));
                    int age = detailedCast.getAge();
                    if (age > 0) {
                        birthInfoText.setText("Born: " + formatDate(detailedCast.getBirthday()) + " (" + age + " years old)");
                    }
                }

                // Update place of birth
                if (detailedCast.getPlaceOfBirth() != null && !detailedCast.getPlaceOfBirth().isEmpty()) {
                    placeOfBirthText.setText(detailedCast.getPlaceOfBirth());
                    placeOfBirthText.setVisibility(View.VISIBLE);
                }

                // Update known for
                if (detailedCast.getKnownForDepartment() != null && !detailedCast.getKnownForDepartment().isEmpty()) {
                    knownForText.setText(detailedCast.getKnownForDepartment());
                }

                // Update profile image with higher quality
                String highResUrl = detailedCast.getProfileImageUrlHighRes();
                if (highResUrl != null && !highResUrl.isEmpty()) {
                    Glide.with(ActorDetailsActivity.this)
                            .load(highResUrl)
                            .centerCrop()
                            .into(profileImage);

                    Glide.with(ActorDetailsActivity.this)
                            .load(highResUrl)
                            .centerCrop()
                            .into(backgroundImage);
                }

                // Load filmography
                loadFilmography();
            }

            @Override
            public void onError(String error) {
                loadingProgress.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching cast details: " + error);
                Toast.makeText(ActorDetailsActivity.this, "Failed to load actor details", Toast.LENGTH_SHORT).show();

                // Still try to load filmography with available data
                loadFilmography();
            }
        });
    }

    private void loadFilmography() {
        filmographyLoading.setVisibility(View.VISIBLE);
        emptyFilmographyText.setVisibility(View.GONE);

        castRepository.getPersonCredits(castMember.getId(), new CastRepository.PersonCreditsCallback() {
            @Override
            public void onSuccess(List<MediaItems> credits) {
                filmographyLoading.setVisibility(View.GONE);

                if (credits != null && !credits.isEmpty()) {
                    filmographyAdapter.setCreditsList(credits);
                    filmographyRecyclerView.setVisibility(View.VISIBLE);
                    emptyFilmographyText.setVisibility(View.GONE);
                } else {
                    filmographyRecyclerView.setVisibility(View.GONE);
                    emptyFilmographyText.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String error) {
                filmographyLoading.setVisibility(View.GONE);
                Log.e(TAG, "Error fetching filmography: " + error);
                filmographyRecyclerView.setVisibility(View.GONE);
                emptyFilmographyText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void openMediaDetails(MediaItems mediaItem, int position) {
        Intent intent;

        // Determine media type and open appropriate activity
        String mediaType = mediaItem.getMediaType();
        if (mediaType != null && mediaType.equals("tv")) {
            intent = new Intent(ActorDetailsActivity.this, DetailsActivityTv.class);
        } else {
            intent = new Intent(ActorDetailsActivity.this, DetailsActivity.class);
        }

        intent.putExtra("media_item", mediaItem);
        startActivity(intent);
    }

    private String formatDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return "Unknown";
        }

        try {
            String[] parts = dateString.split("-");
            if (parts.length >= 3) {
                String year = parts[0];
                String month = parts[1];
                String day = parts[2];

                // Convert month number to name
                String monthName;
                switch (month) {
                    case "01": monthName = "January"; break;
                    case "02": monthName = "February"; break;
                    case "03": monthName = "March"; break;
                    case "04": monthName = "April"; break;
                    case "05": monthName = "May"; break;
                    case "06": monthName = "June"; break;
                    case "07": monthName = "July"; break;
                    case "08": monthName = "August"; break;
                    case "09": monthName = "September"; break;
                    case "10": monthName = "October"; break;
                    case "11": monthName = "November"; break;
                    case "12": monthName = "December"; break;
                    default: monthName = month; break;
                }

                return monthName + " " + day + ", " + year;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formatting date", e);
        }

        return dateString;
    }
}
