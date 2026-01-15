package com.kiduyu.klaus.kiduyutv.Ui.player;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.kiduyu.klaus.kiduyutv.R;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;
    private static final String VIDEO_URL1 = "https://p.10015.workers.dev/stormgleam42.xyz/file2/pAERwzr3C~3d1LB3KpT7w26SOSpPS7+kyBZkHGWP6NarmfYytks~hmaSQtoKbh2OYVAVIAZkgC6eQjNzf9g7P3qLsRqUZf09qwwK9cDiFph7QRnYetuhv81ZPsfkbSGS0eS~qAWkrCLCs~pm~9AJ4bCwVsblW1QBktT8Rhnb6vA=/cGxheWxpc3QubTN1OA==.m3u8";
    private static final String VIDEO_URL = "https://rrr.net22lab.site/plgv/c5/h6a90f70b8d237f94866b6cfc2c7d06afdb8423c6639e9e32f9742c3637a729a8f201998cb9be75c7c50ae72297d2e4196f1b791a3b8d0fe01c84c4d60ebb5bf2/4/aGxzLzEwODAvMTA4MA,Ktm0Vt9-cJyXbGG_O3gV_5vGK-kpiQ.m3u8";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.player_view);
        initializePlayer();

    }

    private void initializePlayer() {
        // Create the player instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Create a media item from the URL
        MediaItem mediaItem = MediaItem.fromUri(VIDEO_URL);

        // Set the media item to the player and prepare it
        player.setMediaItem(mediaItem);
        player.setPlayWhenReady(true); // Start playing immediately
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer(); // Release resources when app stops
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}