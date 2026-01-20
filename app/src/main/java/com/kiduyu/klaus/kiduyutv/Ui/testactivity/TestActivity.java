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



        webView = findViewById(R.id.webview);



        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setBuiltInZoomControls(false);
        webSettings.setDisplayZoomControls(false);

        // Disable some ad-related features
        webSettings.setSaveFormData(false);
        webSettings.setAllowFileAccess(false);

        // Add JavaScript interface for controlling iframe controls
        webView.addJavascriptInterface(new VideoPlayerJSInterface(), "AndroidInterface");

        // Set WebChromeClient to handle console messages and better iframe communication
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber, String sourceId) {
                Log.d("WebView", "Console: " + message + " (From line " + lineNumber + " of " + sourceId + ")");
            }
        });

        // Set WebViewClient to inject our control script when page loads
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                // Inject the control script after page loads
                injectControlScript();
            }
        });

        // Set the OnKeyListener for remote control interactions
        webView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_UP:
                            // Custom logic for Remote Up button
                            Log.i("WebView", "D-Pad UP pressed!");
                            Toast.makeText(v.getContext(), "D-Pad UP!", Toast.LENGTH_SHORT).show();
                            return true;
                        case KeyEvent.KEYCODE_DPAD_CENTER:
                        case KeyEvent.KEYCODE_ENTER:
                            // Show iframe controls when center/enter is pressed
                            Log.i("WebView", "D-Pad center/Enter pressed - showing controls!");
                            Toast.makeText(v.getContext(), "Showing controls!", Toast.LENGTH_SHORT).show();

                            // Use JavaScript interface for robust control
                            webView.evaluateJavascript(
                                    "javascript:(function() { " +
                                            "    console.log('Android: Show controls triggered'); " +
                                            "    " +
                                            "    // Method 1: Try to find and show the control container by exact classes" +
                                            "    var possibleSelectors = [ " +
                                            "        '.absolute.bottom-0.left-0.w-full.h-\\[100px\\].md\\:h-32', " +
                                            "        '[class*=\"bottom-0\"][class*=\"w-full\"]', " +
                                            "        '.bg-gradient-to-t', " +
                                            "        '[class*=\"control-bar\"]', " +
                                            "        '.videasy-controls' " +
                                            "    ]; " +
                                            "    " +
                                            "    possibleSelectors.forEach(function(selector) { " +
                                            "        try { " +
                                            "            var el = document.querySelector(selector); " +
                                            "            if (el) { " +
                                            "                el.style.opacity = '1'; " +
                                            "                el.style.visibility = 'visible'; " +
                                            "                el.style.display = 'flex'; " +
                                            "                console.log('Android: Applied styles to', selector); " +
                                            "            } " +
                                            "        } catch(e) { console.log('Android: Error with selector', selector, e); } " +
                                            "    }); " +
                                            "    " +
                                            "    // Method 2: Click on video to show controls" +
                                            "    var videoEl = document.querySelector('video'); " +
                                            "    if (videoEl) { " +
                                            "        videoEl.click(); " +
                                            "        console.log('Android: Clicked video element'); " +
                                            "    } " +
                                            "    " +
                                            "    // Method 3: Dispatch spacebar key event" +
                                            "    var keyEvt = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                                            "    document.dispatchEvent(keyEvt); " +
                                            "    " +
                                            "    // Method 4: Trigger mouseenter on player area" +
                                            "    var playerArea = document.querySelector('.relative.h-full.w-full.overflow-hidden, .relative.overflow-hidden'); " +
                                            "    if (playerArea) { " +
                                            "        var mouseEvt = new MouseEvent('mouseenter', {bubbles: true, cancelable: true, view: window}); " +
                                            "        playerArea.dispatchEvent(mouseEvt); " +
                                            "        console.log('Android: Dispatched mouseenter'); " +
                                            "    } " +
                                            "    " +
                                            "    // Method 5: Try to access internal React state (advanced)" +
                                            "    // Some players expose their API globally" +
                                            "    if (typeof window.player !== 'undefined' && typeof window.player.showControls === 'function') { " +
                                            "        window.player.showControls(); " +
                                            "        console.log('Android: Called player.showControls()'); " +
                                            "    } " +
                                            "    " +
                                            "    console.log('Android: Show controls complete'); " +
                                            "})();", null);
                            return true;
                    }
                }
                // For other keys or actions, return false to allow default WebView behavior
                return false;
            }
        });


        webView.loadUrl("https://player.videasy.net/movie/299534");
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
        webView.evaluateJavascript(
                "javascript:(function() { " +
                        "    console.log('Android: Hide controls triggered (back press)'); " +
                        "    " +
                        "    // Method 1: Find and hide control container by multiple selectors" +
                        "    var possibleSelectors = [ " +
                        "        '.absolute.bottom-0.left-0.w-full.h-\\[100px\\].md\\:h-32', " +
                        "        '[class*=\"bottom-0\"][class*=\"w-full\"]', " +
                        "        '.bg-gradient-to-t', " +
                        "        '[class*=\"control-bar\"]', " +
                        "        '.videasy-controls' " +
                        "    ]; " +
                        "    " +
                        "    possibleSelectors.forEach(function(selector) { " +
                        "        try { " +
                        "            var el = document.querySelector(selector); " +
                        "            if (el) { " +
                        "                el.style.opacity = '0'; " +
                        "                el.style.visibility = 'hidden'; " +
                        "                el.style.display = 'none'; " +
                        "                console.log('Android: Hidden element', selector); " +
                        "            } " +
                        "        } catch(e) { console.log('Android: Error with selector', selector, e); } " +
                        "    }); " +
                        "    " +
                        "    // Method 2: Click on video to hide controls (toggle behavior)" +
                        "    var videoEl = document.querySelector('video'); " +
                        "    if (videoEl) { " +
                        "        videoEl.click(); " +
                        "        console.log('Android: Clicked video to hide controls'); " +
                        "    } " +
                        "    " +
                        "    // Method 3: Dispatch spacebar key event (toggle)" +
                        "    var keyEvt = new KeyboardEvent('keydown', {key: ' ', code: 'Space', keyCode: 32, bubbles: true}); " +
                        "    document.dispatchEvent(keyEvt); " +
                        "    " +
                        "    // Method 4: Trigger mouseleave on player area" +
                        "    var playerArea = document.querySelector('.relative.h-full.w-full.overflow-hidden, .relative.overflow-hidden'); " +
                        "    if (playerArea) { " +
                        "        var mouseEvt = new MouseEvent('mouseleave', {bubbles: true, cancelable: true, view: window}); " +
                        "        playerArea.dispatchEvent(mouseEvt); " +
                        "        console.log('Android: Dispatched mouseleave'); " +
                        "    } " +
                        "    " +
                        "    // Method 5: Try to access internal React state (advanced)" +
                        "    if (typeof window.player !== 'undefined' && typeof window.player.hideControls === 'function') { " +
                        "        window.player.hideControls(); " +
                        "        console.log('Android: Called player.hideControls()'); " +
                        "    } " +
                        "    " +
                        "    console.log('Android: Hide controls complete'); " +
                        "})();", null);

        Log.i("WebView", "Back pressed - hiding controls");
        Toast.makeText(this, "Hiding controls", Toast.LENGTH_SHORT).show();

        // Small delay to allow hide animation, then exit
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                TestActivity.super.onBackPressed();
            }
        }, 200);
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
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}