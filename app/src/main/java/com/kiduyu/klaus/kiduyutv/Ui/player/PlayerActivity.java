package com.kiduyu.klaus.kiduyutv.Ui.player;

import android.os.Bundle;
import android.view.KeyEvent;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.kiduyu.klaus.kiduyutv.R;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView playerView;

    private static final String VIDEO_URL = "https://rrr.app28base.site/p5qm/c5/h6a90f70b8d237f94866b6cfc2c7f06afdb8423c6639e9e32b074383937a06baeef0f9b8cb9be75c7c50ae72297d2e440790079152e890fea1284d59d53a146e35d/4/aGxzLzEwODAvMTA4MA,Ktm0Vt9-cJyXbGG_O3gV_5vGK-kpiQ.m3u8";
    private static final String VIDEO_URL_SUBTITLES = "https://5qm.megaup.cc/v5/bapD3C40jf5SGa2z8LH8Gr9uEI8Zjnp4ysHQ4OTega67vD5uMub51x8UK5yKV2uhCPlVjTWzBDeJW0JvJTuYJoQVP5d7qAZgZXxD67pvQIuNikLe6H8gyXmcoNmdRsqYZzwNrIbH7nA/subs/eng_4.vtt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.player_view);
        setupPlayerView();
        initializePlayer();
    }

    private void setupPlayerView() {
        // Enable controller for TV navigation
        playerView.setControllerAutoShow(true);
        playerView.setControllerHideOnTouch(false);
        playerView.setControllerShowTimeoutMs(5000);

        // Request focus on the player view
        playerView.requestFocus();
    }

    private void initializePlayer() {
        // Create the player instance
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Create a media item with subtitles
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(VIDEO_URL)
                .setSubtitleConfigurations(
                        java.util.Collections.singletonList(
                                new MediaItem.SubtitleConfiguration.Builder(
                                        android.net.Uri.parse(VIDEO_URL_SUBTITLES))
                                        .setMimeType(MimeTypes.TEXT_VTT)
                                        .setLanguage("en")
                                        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                                        .build()
                        )
                )
                .build();

        // Set the media item to the player and prepare it
        player.setMediaItem(mediaItem);
        player.setPlayWhenReady(true);
        player.prepare();
        player.play();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Handle D-pad and media keys
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                togglePlayPause();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                if (player != null && !player.isPlaying()) {
                    player.play();
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                if (player != null && player.isPlaying()) {
                    player.pause();
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekBackward();
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekForward();
                return true;

            case KeyEvent.KEYCODE_BACK:
                // Show controls before going back
                playerView.showController();
                return super.onKeyDown(keyCode, event);

            default:
                // Show controller on any key press
                playerView.showController();
                return super.onKeyDown(keyCode, event);
        }
    }

    private void togglePlayPause() {
        if (player != null) {
            if (player.isPlaying()) {
                player.pause();
            } else {
                player.play();
            }
            playerView.showController();
        }
    }

    private void seekBackward() {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            player.seekTo(Math.max(0, currentPosition - 10000)); // 10 seconds back
            playerView.showController();
        }
    }

    private void seekForward() {
        if (player != null) {
            long currentPosition = player.getCurrentPosition();
            long duration = player.getDuration();
            if (duration != C.TIME_UNSET) {
                player.seekTo(Math.min(duration, currentPosition + 10000)); // 10 seconds forward
            }
            playerView.showController();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        releasePlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && playerView != null) {
            playerView.requestFocus();
        }
    }

    private void releasePlayer() {
        if (player != null) {
            player.release();
            player = null;
        }
    }
}