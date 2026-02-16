package com.kiduyu.klaus.kiduyutv.Ui.testactivity;

import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.kiduyu.klaus.kiduyutv.R;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.List;

public class TestActivity extends AppCompatActivity {
    private WebView webView;
    private static final int SCROLL_AMOUNT = 100;
    private boolean areControlsVisible = true;

    // Common ad domains to block
    private static final List<String> AD_HOSTS = Arrays.asList(
            "doubleclick.net",
            "googlesyndication.com",
            "googleadservices.com",
            "google-analytics.com",
            "adservice.google.com",
            "ads.google.com",
            "adbrite.com",
            "advertising.com",
            "adnxs.com",
            "facebook.net",
            "connect.facebook.net",
            "moatads.com",
            "scorecardresearch.com",
            "pubmatic.com",
            "outbrain.com",
            "taboola.com",
            "criteo.com",
            "adsafeprotected.com",
            "adform.net",
            "serving-sys.com",
            "advertising.com",
            "adsrvr.org",
            "quantserve.com",
            "rubiconproject.com",
            "openx.net",
            "casalemedia.com"
    );

    /**
     * JavaScript interface to communicate with the iframe player
     * This allows controlling the player controls from Android
     */
    private class VideoPlayerJSInterface {
        @android.webkit.JavascriptInterface
        public void showControls() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Execute comprehensive JavaScript to show iframe controls
                    String jsCode = "javascript:(function() { " +
                            "console.log('AndroidInterface: showControls called'); " +
                            " " +
                            "// Method 1: Inject event listener for postMessage communication" +
                            "window.addEventListener('message', function(event) { " +
                            "    try { " +
                            "        var data = JSON.parse(event.data); " +
                            "        if (data.type === 'showControls') { " +
                            "            // Trigger React state change by simulating hover" +
                            "            var playerArea = document.querySelector('.relative.h-full.w-full.overflow-hidden'); " +
                            "            if (playerArea) { " +
                            "                playerArea.dispatchEvent(new MouseEvent('mouseenter', {bubbles: true, cancelable: true, view: window})); " +
                            "                console.log('AndroidInterface: Dispatched mouseenter to show controls'); " +
                            "            } " +
                            "        } " +
                            "    } catch(e) {} " +
                            "}); " +
                            " " +
                            "// Method 2: Direct React state manipulation (for React-based players)" +
                            "// Find React internal state by searching for components with control-related props" +
                            "var controlElements = document.querySelectorAll('[class*=\"control\"], [class*=\"bottom-0\"], [class*=\"gradient\"]'); " +
                            "controlElements.forEach(function(el) { " +
                            "    // Force show by removing any hiding classes" +
                            "    el.style.opacity = '1'; " +
                            "    el.style.visibility = 'visible'; " +
                            "    el.style.display = 'flex'; " +
                            "    // Remove hidden attributes" +
                            "    el.removeAttribute('hidden'); " +
                            "}); " +
                            " " +
                            "// Method 3: Find React root and attempt state manipulation" +
                            "var reactRoot = document.querySelector('#__next'); " +
                            "if (reactRoot) { " +
                            "    // Try to trigger any state update mechanism" +
                            "    var mouseEvent = new MouseEvent('mouseenter', {bubbles: true, cancelable: true, view: window}); " +
                            "    reactRoot.dispatchEvent(mouseEvent); " +
                            "} " +
                            " " +
                            "// Method 4: Click on video area to show controls (common player behavior)" +
                            "var videoElement = document.querySelector('video'); " +
                            "if (videoElement) { " +
                            "    videoElement.click(); " +
                            "    console.log('AndroidInterface: Clicked video element'); " +
                            "} " +
                            " " +
                            "// Method 5: Simulate spacebar keypress (common toggle control)" +
                            "var keyEvent = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                            "document.dispatchEvent(keyEvent); " +
                            "console.log('AndroidInterface: Dispatched spacebar event'); " +
                            "})();";
                    webView.evaluateJavascript(jsCode, null);
                    areControlsVisible = true;
                    Log.i("VideoControls", "Showing iframe controls");
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void hideControls() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Execute comprehensive JavaScript to hide iframe controls
                    String jsCode = "javascript:(function() { " +
                            "console.log('AndroidInterface: hideControls called'); " +
                            " " +
                            "// Method 1: Direct CSS manipulation to hide controls" +
                            "var controlElements = document.querySelectorAll('[class*=\"control\"], [class*=\"bottom-0\"], [class*=\"gradient\"]'); " +
                            "controlElements.forEach(function(el) { " +
                            "    el.style.opacity = '0'; " +
                            "    el.style.visibility = 'hidden'; " +
                            "    el.style.display = 'none'; " +
                            "}); " +
                            " " +
                            "// Method 2: Trigger mouseleave to hide controls" +
                            "var playerArea = document.querySelector('.relative.h-full.w-full.overflow-hidden'); " +
                            "if (playerArea) { " +
                            "    playerArea.dispatchEvent(new MouseEvent('mouseleave', {bubbles: true, cancelable: true, view: window})); " +
                            "    console.log('AndroidInterface: Dispatched mouseleave to hide controls'); " +
                            "} " +
                            " " +
                            "// Method 3: Click on video to hide controls (toggle behavior)" +
                            "var videoElement = document.querySelector('video'); " +
                            "if (videoElement) { " +
                            "    videoElement.click(); " +
                            "    console.log('AndroidInterface: Clicked video to hide controls'); " +
                            "} " +
                            " " +
                            "// Method 4: Simulate spacebar keypress" +
                            "var keyEvent = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                            "document.dispatchEvent(keyEvent); " +
                            "})();";
                    webView.evaluateJavascript(jsCode, null);
                    areControlsVisible = false;
                    Log.i("VideoControls", "Hiding iframe controls");
                }
            });
        }

        @android.webkit.JavascriptInterface
        public void toggleControls() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String jsCode = "javascript:(function() { " +
                            "console.log('AndroidInterface: toggleControls called'); " +
                            "var videoElement = document.querySelector('video'); " +
                            "if (videoElement) { " +
                            "    videoElement.click(); " +
                            "    console.log('AndroidInterface: Toggled controls via video click'); " +
                            "} " +
                            "var keyEvent = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                            "document.dispatchEvent(keyEvent); " +
                            "})();";
                    webView.evaluateJavascript(jsCode, null);
                    areControlsVisible = !areControlsVisible;
                }
            });
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);




    }

    /**
     * Inject a script that sets up communication with the player controls
     * This script listens for Android commands and controls the player accordingly
     */
    private void injectControlScript() {
        String controlScript = "javascript:(function() { " +
                "console.log('AndroidControl: Injecting control script'); " +
                " " +
                "// Create a namespace for our control functions " +
                "window.AndroidControl = { " +
                "    showControls: function() { " +
                "        console.log('AndroidControl: showControls called'); " +
                "        " +
                "        // Method 1: Find the control container and show it " +
                "        var controlContainer = document.querySelector('.absolute.bottom-0.left-0.w-full.h-\\\\[100px\\\\].md\\\\:h-32'); " +
                "        if (controlContainer) { " +
                "            controlContainer.style.display = 'flex'; " +
                "            controlContainer.style.opacity = '1'; " +
                "            controlContainer.style.visibility = 'visible'; " +
                "            console.log('AndroidControl: Showed control container'); " +
                "        } " +
                "        " +
                "        // Method 2: Find by gradient pattern " +
                "        var gradientControls = document.querySelector('.bg-gradient-to-t'); " +
                "        if (gradientControls) { " +
                "            gradientControls.style.opacity = '1'; " +
                "            gradientControls.style.visibility = 'visible'; " +
                "            gradientControls.style.display = 'flex'; " +
                "        } " +
                "        " +
                "        // Method 3: Click video to show controls " +
                "        var video = document.querySelector('video'); " +
                "        if (video) { " +
                "            video.click(); " +
                "        } " +
                "        " +
                "        // Method 4: Dispatch spacebar " +
                "        var spaceEvent = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                "        document.dispatchEvent(spaceEvent); " +
                "    }, " +
                "    " +
                "    hideControls: function() { " +
                "        console.log('AndroidControl: hideControls called'); " +
                "        " +
                "        // Method 1: Find the control container and hide it " +
                "        var controlContainer = document.querySelector('.absolute.bottom-0.left-0.w-full.h-\\\\[100px\\\\].md\\\\:h-32'); " +
                "        if (controlContainer) { " +
                "            controlContainer.style.opacity = '0'; " +
                "            controlContainer.style.visibility = 'hidden'; " +
                "            controlContainer.style.display = 'none'; " +
                "            console.log('AndroidControl: Hid control container'); " +
                "        } " +
                "        " +
                "        // Method 2: Find by gradient pattern " +
                "        var gradientControls = document.querySelector('.bg-gradient-to-t'); " +
                "        if (gradientControls) { " +
                "            gradientControls.style.opacity = '0'; " +
                "            gradientControls.style.visibility = 'hidden'; " +
                "            gradientControls.style.display = 'none'; " +
                "        } " +
                "        " +
                "        // Method 3: Click video to hide controls " +
                "        var video = document.querySelector('video'); " +
                "        if (video) { " +
                "            video.click(); " +
                "        } " +
                "        " +
                "        // Method 4: Dispatch spacebar " +
                "        var spaceEvent = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                "        document.dispatchEvent(spaceEvent); " +
                "    }, " +
                "    " +
                "    toggleControls: function() { " +
                "        console.log('AndroidControl: toggleControls called'); " +
                "        var video = document.querySelector('video'); " +
                "        if (video) { " +
                "            video.click(); " +
                "        } " +
                "    } " +
                "}; " +
                " " +
                "// Set up listener for postMessage commands from Android " +
                "window.addEventListener('message', function(event) { " +
                "    try { " +
                "        var data = JSON.parse(event.data); " +
                "        if (data.action === 'showControls' && typeof window.AndroidControl.showControls === 'function') { " +
                "            window.AndroidControl.showControls(); " +
                "        } else if (data.action === 'hideControls' && typeof window.AndroidControl.hideControls === 'function') { " +
                "            window.AndroidControl.hideControls(); " +
                "        } else if (data.action === 'toggleControls' && typeof window.AndroidControl.toggleControls === 'function') { " +
                "            window.AndroidControl.toggleControls(); " +
                "        } " +
                "    } catch(e) { " +
                "        console.log('AndroidControl: Error processing message', e); " +
                "    } " +
                "}); " +
                " " +
                "console.log('AndroidControl: Script injection complete'); " +
                "})();";

        webView.evaluateJavascript(controlScript, null);
    }

    /**
     * Handle back press to hide iframe controls before exiting
     */
    @Override
    public void onBackPressed() {
        // Hide controls when back is pressed
       super.onBackPressed();
    }





    /**
     @Override
     public boolean onKeyDown(int keyCode, KeyEvent event) {
     switch (keyCode) {
     case KeyEvent.KEYCODE_DPAD_UP:
     webView.scrollBy(0, -SCROLL_AMOUNT);
     return true;

     case KeyEvent.KEYCODE_DPAD_DOWN:
     webView.scrollBy(0, SCROLL_AMOUNT);
     return true;

     case KeyEvent.KEYCODE_DPAD_LEFT:
     webView.scrollBy(-SCROLL_AMOUNT, 0);
     return true;

     case KeyEvent.KEYCODE_DPAD_RIGHT:
     webView.scrollBy(SCROLL_AMOUNT, 0);
     return true;

     case KeyEvent.KEYCODE_DPAD_CENTER:
     case KeyEvent.KEYCODE_ENTER:
     showVideoControls();
     return true;

     case KeyEvent.KEYCODE_BACK:
     if (webView.canGoBack()) {
     webView.goBack();
     return true;
     }
     break;
     }
     return super.onKeyDown(keyCode, event);
     }
     **/



    @Override
    protected void onDestroy() {

        super.onDestroy();
    }
}