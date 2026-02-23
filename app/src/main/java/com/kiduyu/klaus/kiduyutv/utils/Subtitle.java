package com.kiduyu.klaus.kiduyutv.utils;

import android.net.Uri;

/**
 * Data model class representing a subtitle track for video playback.
 */
public class Subtitle {
    // The language of the subtitle (e.g., "English", "Swahili")
    public String language;

    // The version or release name of the subtitle file (often indicates sync compatibility)
    public String release;

    // The URI pointing to the locally stored .srt file on the device
    public Uri srtUri;      // local extracted srt file
}