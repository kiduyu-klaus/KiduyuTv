package com.kiduyu.klaus.kiduyutv.Ui.testactivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.utils.SubdlService;
import com.kiduyu.klaus.kiduyutv.utils.Subtitle;

import java.util.List;

public class TestActivity extends AppCompatActivity {

    TextView textView;
    ProgressBar progressBar;
    private ProgressButton progressButton;
    private ProgressButton progressButton2;
    private Button resetButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        // Initialize ProgressButtons
        progressButton = findViewById(R.id.progressButton);
        progressButton2 = findViewById(R.id.progressButton2);
        resetButton = findViewById(R.id.resetButton);
        progressButton.setAutoStartOnVisible(true);
        progressButton.loopAnimation(800);

        // Set button text
        progressButton.setButtonText("Click Me");
        progressButton2.setButtonText("Submit Form");
        // Set click listener for first button
        progressButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the 5-second fill animation
                progressButton.startAnimation();

                // You can also set custom duration:
                // progressButton.startAnimation(3000); // 3 seconds
            }
        });

        // Set click listener for second button
        progressButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton2.startAnimation();
            }
        });

        // Reset button to restore initial state
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressButton.reset();
                progressButton2.reset();
            }
        });



    }

    private void loadSubtitles() {

        progressBar.setVisibility(View.VISIBLE);
        textView.setText("Fetching subtitles...");

        SubdlService svc = new SubdlService(this);
        svc.fetchTvSubtitles(
                224372,   // TMDB id
                1,      // season
                1,      // episode
                "EN",
                new SubdlService.Callback() {
                    @Override
                    public void onSuccess(List<Subtitle> subtitles) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);

                            if (subtitles.isEmpty()) {
                                textView.setText("No subtitles found");
                                return;
                            }

                            Subtitle s = subtitles.get(0);
                            Log.i("SRT", s.language + " -> " + s.srtUri);
                            textView.setText(
                                    s.language + " -> " + s.srtUri
                            );
                        });
                    }

                    @Override
                    public void onError(String error) {
                        Log.e("SUBS", error);
                        runOnUiThread(() -> {
                           progressBar.setVisibility(View.GONE);
                            textView.setText("Error: " + error);
                            Log.e("SUBS", error);
                       });
                    }
                }
        );


//        svc.fetchSubtitles(
//                803796,
//                "movie",
//                "EN",
//                new SubdlService.Callback() {
//
//                    @Override
//                    public void onSuccess(List<Subtitle> subtitles) {
//
//                        runOnUiThread(() -> {
//                            progressBar.setVisibility(View.GONE);
//
//                            if (subtitles.isEmpty()) {
//                                textView.setText("No subtitles found");
//                                return;
//                            }
//
//                            Subtitle s = subtitles.get(0);
//                            Log.i("SRT", s.language + " -> " + s.srtUri);
//                            textView.setText(
//                                    s.language + " -> " + s.srtUri
//                            );
//                        });
//                    }
//
//                    @Override
//                    public void onError(String error) {
//                        runOnUiThread(() -> {
//                            progressBar.setVisibility(View.GONE);
//                            textView.setText("Error: " + error);
//                            Log.e("SUBS", error);
//                        });
//                    }
//                }
//        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
