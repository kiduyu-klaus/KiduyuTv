package com.kiduyu.klaus.kiduyutv.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import androidx.appcompat.app.AlertDialog;

import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class utils {

    /**
     * Map genre IDs to genre names
     */
    public static String getGenreName(int genreId) {
        switch (genreId) {
            case 28:
                return "Action";
            case 12:
                return "Adventure";
            case 16:
                return "Animation";
            case 35:
                return "Comedy";
            case 80:
                return "Crime";
            case 99:
                return "Documentary";
            case 18:
                return "Drama";
            case 10751:
                return "Family";
            case 14:
                return "Fantasy";
            case 36:
                return "History";
            case 27:
                return "Horror";
            case 10402:
                return "Music";
            case 9648:
                return "Mystery";
            case 10749:
                return "Romance";
            case 878:
                return "Science Fiction";
            case 10770:
                return "TV Movie";
            case 53:
                return "Thriller";
            case 10752:
                return "War";
            case 37:
                return "Western";
            // TV genres
            case 10759:
                return "Action & Adventure";
            case 10762:
                return "Kids";
            case 10763:
                return "News";
            case 10764:
                return "Reality";
            case 10765:
                return "Sci-Fi & Fantasy";
            case 10766:
                return "Soap";
            case 10767:
                return "Talk";
            case 10768:
                return "War & Politics";
            default:
                return null;
        }
    }

    /**
     * Check if device has network connectivity
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
            }
            return false;
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
    }

    /**
     * Check if device has active internet connection
     * Note: This only checks connectivity, not actual internet access
     * For real internet check, you'd need to ping a server
     * @param context Application context
     * @return true if connected, false otherwise
     */
    public static boolean isInternetConnected(Context context) {
        if (context == null) {
            return false;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            NetworkCapabilities capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());

            if (capabilities != null) {
                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
            }
            return false;
        } else {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        }
    }

    /**
     * Check if device is Fire TV
     * @param context Application context
     * @return true if Fire TV device
     */
    public static boolean isFireTV(Context context) {
        if (context == null) {
            return false;
        }

        String manufacturer = Build.MANUFACTURER.toLowerCase();
        String model = Build.MODEL.toLowerCase();

        return manufacturer.equals("amazon") ||
                model.contains("aft");  // Amazon Fire TV models typically start with AFT
    }

    /**
     * Check if device is Android TV
     * @param context Application context
     * @return true if Android TV
     */
    public static boolean isAndroidTV(Context context) {
        if (context == null) {
            return false;
        }

        PackageManager pm = context.getPackageManager();
        return pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    /**
     * Check if device is Google TV (Chromecast with Google TV)
     * @param context Application context
     * @return true if Google TV
     */
    public static boolean isGoogleTV(Context context) {
        if (context == null) {
            return false;
        }

        String model = Build.MODEL.toLowerCase();
        String device = Build.DEVICE.toLowerCase();

        // Google TV devices (Chromecast with Google TV)
        return model.contains("chromecast") ||
                device.contains("sabrina") ||  // Codename for Chromecast with Google TV
                model.contains("google tv");
    }

    /**
     * Get appropriate network settings intent based on device type
     * @param context Application context
     * @return Intent to open network settings
     */
    public static Intent getNetworkSettingsIntent(Context context) {
        Intent intent;

        if (isFireTV(context)) {
            // Fire TV specific settings
            intent = new Intent();
            intent.setClassName("com.amazon.tv.settings",
                    "com.amazon.tv.settings.tv.network.NetworkActivity");

            // Fallback to general settings if specific activity doesn't exist
            if (context.getPackageManager().resolveActivity(intent, 0) == null) {
                intent = new Intent(Settings.ACTION_SETTINGS);
            }
        } else if (isGoogleTV(context) || isAndroidTV(context)) {
            // Google TV / Android TV - use wireless settings
            intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);

            // Alternative: Direct network settings for some Android TV versions
            if (context.getPackageManager().resolveActivity(intent, 0) == null) {
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            }
        } else {
            // Generic fallback
            intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        }

        return intent;
    }

    /**
     * Show alert dialog when no network is available
     * Offers option to open appropriate network settings based on device
     * @param context Activity context
     */
    public static void showNoNetworkDialog(Context context) {
        if (context == null) {
            return;
        }

        String deviceType = "";
        if (isFireTV(context)) {
            deviceType = " (Fire TV)";
        } else if (isGoogleTV(context)) {
            deviceType = " (Google TV)";
        } else if (isAndroidTV(context)) {
            deviceType = " (Android TV)";
        }

        new AlertDialog.Builder(context)
                .setTitle("No Internet Connection")
                .setMessage("Please check your internet connection and try again." + deviceType)
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = getNetworkSettingsIntent(context);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            // Fallback to general settings if network settings can't be opened
                            try {
                                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                                context.startActivity(fallback);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    /**
     * Check network and show dialog if not connected
     * @param context Activity context
     * @return true if connected, false if not connected (and dialog shown)
     */
    public static boolean checkNetworkAndShowDialog(Context context) {
        if (!isNetworkAvailable(context)) {
            showNoNetworkDialog(context);
            return false;
        }
        return true;
    }

    /**
     * Get device type as string
     * @param context Application context
     * @return Device type name
     */
    public static String getDeviceType(Context context) {
        if (isFireTV(context)) {
            return "Fire TV";
        } else if (isGoogleTV(context)) {
            return "Google TV";
        } else if (isAndroidTV(context)) {
            return "Android TV";
        } else {
            return "Unknown TV";
        }
    }

    /**
     * Check if device has actual internet access by attempting to connect to a server
     * This should be called from a background thread as it performs network I/O
     * @return true if internet is accessible, false otherwise
     */
    public static boolean hasActiveInternetConnection() {
        try {
            // Try to connect to Google's DNS server
            java.net.InetAddress address = java.net.InetAddress.getByName("8.8.8.8");
            return !address.equals("");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if device has actual internet access by pinging a URL
     * This should be called from a background thread as it performs network I/O
     * @param timeout Connection timeout in milliseconds
     * @return true if internet is accessible, false otherwise
     */
    public static boolean hasActiveInternetConnection(int timeout) {
        try {
            java.net.URL url = new java.net.URL("https://www.google.com");
            java.net.HttpURLConnection urlConnection = (java.net.HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("User-Agent", "Android");
            urlConnection.setRequestProperty("Connection", "close");
            urlConnection.setConnectTimeout(timeout);
            urlConnection.setReadTimeout(timeout);
            urlConnection.connect();
            return (urlConnection.getResponseCode() == 200);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check actual internet connectivity asynchronously
     * @param context Application context
     * @param callback Callback to receive result
     */
    public static void checkInternetAsync(Context context, final InternetCheckCallback callback) {
        // First check if network is available
        if (!isNetworkAvailable(context)) {
            if (callback != null) {
                callback.onInternetCheckComplete(false);
            }
            return;
        }

        // Run internet check in background thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean hasInternet = hasActiveInternetConnection(3000); // 3 second timeout

                // Post result back to main thread
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onInternetCheckComplete(hasInternet);
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Callback interface for async internet check
     */
    public interface InternetCheckCallback {
        void onInternetCheckComplete(boolean hasInternet);
    }

    /**
     * Check internet and show dialog if not available
     * Use this method for comprehensive internet checking
     * @param context Activity context
     * @param callback Optional callback for result
     */
    public static void checkInternetAndShowDialog(Context context, final InternetCheckCallback callback) {
        // First check network connectivity
        if (!isNetworkAvailable(context)) {
            showNoNetworkDialog(context);
            if (callback != null) {
                callback.onInternetCheckComplete(false);
            }
            return;
        }

        // Show loading indicator (optional - you can implement this)
        // Then check actual internet access
        checkInternetAsync(context, new InternetCheckCallback() {
            @Override
            public void onInternetCheckComplete(boolean hasInternet) {
                if (!hasInternet) {
                    showNoInternetAccessDialog(context);
                }
                if (callback != null) {
                    callback.onInternetCheckComplete(hasInternet);
                }
            }
        });
    }

    /**
     * Show dialog when network is connected but no internet access
     * @param context Activity context
     */
    public static void showNoInternetAccessDialog(Context context) {
        if (context == null) {
            return;
        }

        new AlertDialog.Builder(context)
                .setTitle("No Internet Access")
                .setMessage("You are connected to a network, but there is no internet access. Please check your connection.")
                .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            Intent intent = getNetworkSettingsIntent(context);
                            context.startActivity(intent);
                        } catch (Exception e) {
                            try {
                                Intent fallback = new Intent(Settings.ACTION_SETTINGS);
                                context.startActivity(fallback);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    }
                })
                .setNeutralButton("Retry", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        // User can retry their action
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .show();
    }

    /**
     * Remove duplicate video sources based on URL
     */
    public static List<MediaItems.VideoSource> removeDuplicateSources(List<MediaItems.VideoSource> sources) {
        Map<String, MediaItems.VideoSource> uniqueMap = new LinkedHashMap<>();

        for (MediaItems.VideoSource source : sources) {
            String url = source.getUrl();
            if (url != null && !url.isEmpty()) {
                // Keep first occurrence or prefer higher quality
                if (!uniqueMap.containsKey(url)) {
                    uniqueMap.put(url, source);
                } else {
                    // Compare qualities and keep better one
                    MediaItems.VideoSource existing = uniqueMap.get(url);
                    if (isBetterQuality(source.getQuality(), existing.getQuality())) {
                        uniqueMap.put(url, source);
                    }
                }
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * Remove duplicate subtitles based on URL
     */
    public static List<MediaItems.SubtitleItem> removeDuplicateSubtitles(List<MediaItems.SubtitleItem> subtitles) {
        Map<String, MediaItems.SubtitleItem> uniqueMap = new LinkedHashMap<>();

        for (MediaItems.SubtitleItem subtitle : subtitles) {
            String url = subtitle.getUrl();
            if (url != null && !url.isEmpty()) {
                // Use URL + language as unique key
                String key = url + "_" + subtitle.getLang();
                if (!uniqueMap.containsKey(key)) {
                    uniqueMap.put(key, subtitle);
                }
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    /**
     * Compare quality strings to determine which is better
     */
    public static boolean isBetterQuality(String quality1, String quality2) {
        int q1 = parseQuality(quality1);
        int q2 = parseQuality(quality2);
        return q1 > q2;
    }


    public static List<MediaItems.VideoSource> moveVipSourceToTop(List<MediaItems.VideoSource> sources) {
        if (sources == null || sources.isEmpty()) return sources;

        List<MediaItems.VideoSource> vipSources = new ArrayList<>();
        List<MediaItems.VideoSource> normalSources = new ArrayList<>();

        for (MediaItems.VideoSource source : sources) {
            if (source.getUrl() != null &&
                    source.getUrl().toLowerCase().contains("vip")) {
                vipSources.add(source);
            } else {
                normalSources.add(source);
            }
        }

        vipSources.addAll(normalSources);
        return vipSources;
    }

    /**
     * Parse quality string to numeric value for comparison
     */
    public static int parseQuality(String quality) {
        if (quality == null) return 0;

        quality = quality.toLowerCase();

        // Extract numeric value
        if (quality.contains("2160") || quality.contains("4k")) return 2160;
        if (quality.contains("1440") || quality.contains("2k")) return 1440;
        if (quality.contains("1080")) return 1080;
        if (quality.contains("720")) return 720;
        if (quality.contains("480")) return 480;
        if (quality.contains("360")) return 360;
        if (quality.contains("240")) return 240;

        // Handle special cases
        if (quality.equals("auto") || quality.contains("auto")) return 9999; // Prefer auto
        if (quality.contains("hd")) return 720; // Assume HD is 720p
        if (quality.contains("sd")) return 480; // Assume SD is 480p

        return 0; // Unknown quality
    }
}