package com.kiduyu.klaus.kiduyutv.Ui.testactivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        textView = findViewById(R.id.trendingTitle);
        progressBar = findViewById(R.id.progressBar);

        loadSubtitles();
    }

    private void loadSubtitles() {

        progressBar.setVisibility(View.VISIBLE);
        textView.setText("Fetching subtitles...");

        SubdlService svc = new SubdlService(this);

        svc.fetchSubtitles(
                803796,
                "movie",
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
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            textView.setText("Error: " + error);
                            Log.e("SUBS", error);
                        });
                    }
                }
        );
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
