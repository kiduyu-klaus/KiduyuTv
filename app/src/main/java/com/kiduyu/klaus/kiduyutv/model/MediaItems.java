package com.kiduyu.klaus.kiduyutv.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MediaItems implements Parcelable {
    private String id;
    private String title;
    private String description;
    private String posterUrl;
    private String videoUrl;
    private String subtitleUrl;
    private String duration;
    private int year;
    private String genre;
    private float rating;
    private boolean isLive;
    private String hlsUrl;
    private String dashUrl;

    // TMDB-specific fields
    private String tmdbId;
    private String mediaType;
    private String season;
    private String episode;
    private boolean isFromTMDB;
    private String status;
    private String tagline;
    private int voteCount;
    private List<String> genres;

    // Image URLs
    private String backgroundImageUrl;
    private String heroImageUrl;
    private String cardImageUrl;

    // API sources
    private List<VideoSource> videoSources;
    private List<SubtitleItem> subtitles;

    // Session management for Cloudflare-protected streams
    private String sessionCookie;
    private String refererUrl;
    private Map<String, String> customHeaders;
    private Map<String, String> responseHeaders;

    public MediaItems() {
        this.videoSources = new ArrayList<>();
        this.subtitles = new ArrayList<>();
        this.genres = new ArrayList<>();
        this.isFromTMDB = false;
        this.customHeaders = new HashMap<>();
        this.responseHeaders = new HashMap<>();
    }

    protected MediaItems(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        posterUrl = in.readString();
        videoUrl = in.readString();
        subtitleUrl = in.readString();
        duration = in.readString();
        year = in.readInt();
        genre = in.readString();
        rating = in.readFloat();
        isLive = in.readByte() != 0;
        hlsUrl = in.readString();
        dashUrl = in.readString();
        tmdbId = in.readString();
        mediaType = in.readString();
        season = in.readString();
        episode = in.readString();
        isFromTMDB = in.readByte() != 0;
        status = in.readString();
        tagline = in.readString();
        voteCount = in.readInt();

        // Read lists
        genres = new ArrayList<>();
        in.readList(genres, String.class.getClassLoader());

        backgroundImageUrl = in.readString();
        heroImageUrl = in.readString();
        cardImageUrl = in.readString();

        videoSources = new ArrayList<>();
        in.readList(videoSources, VideoSource.class.getClassLoader());

        subtitles = new ArrayList<>();
        in.readList(subtitles, SubtitleItem.class.getClassLoader());

        // Read session data
        sessionCookie = in.readString();
        refererUrl = in.readString();

        // Read custom headers - FIXED!
        customHeaders = new HashMap<>();
        int customHeadersSize = in.readInt();
        for (int i = 0; i < customHeadersSize; i++) {
            String key = in.readString();
            String value = in.readString();
            customHeaders.put(key, value);
        }

        // Read response headers
        responseHeaders = new HashMap<>();
        int responseHeadersSize = in.readInt();
        for (int i = 0; i < responseHeadersSize; i++) {
            String key = in.readString();
            String value = in.readString();
            responseHeaders.put(key, value);
        }
    }

    public static final Creator<MediaItems> CREATOR = new Creator<MediaItems>() {
        @Override
        public MediaItems createFromParcel(Parcel in) {
            return new MediaItems(in);
        }

        @Override
        public MediaItems[] newArray(int size) {
            return new MediaItems[size];
        }
    };

    // Session management getters/setters
    public String getSessionCookie() {
        return sessionCookie;
    }

    public void setSessionCookie(String sessionCookie) {
        this.sessionCookie = sessionCookie;
    }

    public String getRefererUrl() {
        return refererUrl;
    }

    public void setRefererUrl(String refererUrl) {
        this.refererUrl = refererUrl;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders != null ? customHeaders : new HashMap<>();
    }

    public void addCustomHeader(String key, String value) {
        if (customHeaders == null) {
            customHeaders = new HashMap<>();
        }
        customHeaders.put(key, value);
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public void setResponseHeaders(Map<String, String> responseHeaders) {
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
    }

    // All other getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public void setPosterUrl(String posterUrl) {
        this.posterUrl = posterUrl;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getSubtitleUrl() {
        return subtitleUrl;
    }

    public void setSubtitleUrl(String subtitleUrl) {
        this.subtitleUrl = subtitleUrl;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public boolean isLive() {
        return isLive;
    }

    public void setLive(boolean live) {
        isLive = live;
    }

    public String getHlsUrl() {
        return hlsUrl;
    }

    public void setHlsUrl(String hlsUrl) {
        this.hlsUrl = hlsUrl;
    }

    public String getDashUrl() {
        return dashUrl;
    }

    public void setDashUrl(String dashUrl) {
        this.dashUrl = dashUrl;
    }

    public String getTmdbId() {
        return tmdbId;
    }

    public void setTmdbId(String tmdbId) {
        this.tmdbId = tmdbId;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getEpisode() {
        return episode;
    }

    public void setEpisode(String episode) {
        this.episode = episode;
    }

    public boolean isFromTMDB() {
        return isFromTMDB;
    }

    public void setFromTMDB(boolean fromTMDB) {
        isFromTMDB = fromTMDB;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public int getVoteCount() {
        return voteCount;
    }

    public void setVoteCount(int voteCount) {
        this.voteCount = voteCount;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getGenresAsString() {
        if (genres == null || genres.isEmpty()) {
            return "";
        }
        return String.join(", ", genres);
    }

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public String getHeroImageUrl() {
        return heroImageUrl;
    }

    public void setHeroImageUrl(String heroImageUrl) {
        this.heroImageUrl = heroImageUrl;
    }

    public String getCardImageUrl() {
        return cardImageUrl;
    }

    public void setCardImageUrl(String cardImageUrl) {
        this.cardImageUrl = cardImageUrl;
    }

    public List<VideoSource> getVideoSources() {
        return videoSources;
    }

    public void setVideoSources(List<VideoSource> videoSources) {
        this.videoSources = videoSources;
    }

    public List<SubtitleItem> getSubtitles() {
        return subtitles;
    }

    public void setSubtitles(List<SubtitleItem> subtitles) {
        this.subtitles = subtitles;
    }

    // Helper methods
    public boolean isFromAPI() {
        return tmdbId != null && !tmdbId.isEmpty();
    }

    public boolean hasValidVideoSources() {
        return (videoSources != null && !videoSources.isEmpty()) ||
                (hlsUrl != null && !hlsUrl.isEmpty()) ||
                (dashUrl != null && !dashUrl.isEmpty()) ||
                (videoUrl != null && !videoUrl.isEmpty());
    }

    public String getPrimaryImageUrl() {
        if (heroImageUrl != null && !heroImageUrl.isEmpty()) {
            return heroImageUrl;
        }
        if (backgroundImageUrl != null && !backgroundImageUrl.isEmpty()) {
            return backgroundImageUrl;
        }
        if (cardImageUrl != null && !cardImageUrl.isEmpty()) {
            return cardImageUrl;
        }
        return posterUrl;
    }

    /**
     * Get the preferred background image URL for ExoPlayer artwork display.
     * Implements a 3-tier fallback logic to ensure a background is always available.
     *
     * Priority order:
     * 1. backgroundImageUrl (Highest resolution, specifically for backgrounds)
     * 2. heroImageUrl (High resolution, usually landscape)
     * 3. cardImageUrl (Lower resolution, used for grid items)
     *
     * @return The preferred background URL, or null if no background is available
     */
    public String getPreferredBackgroundUrl() {
        // First priority: backgroundImageUrl (highest resolution for backgrounds)
        if (backgroundImageUrl != null && !backgroundImageUrl.isEmpty()) {
            return backgroundImageUrl;
        }

        // Second priority: heroImageUrl (high resolution landscape image)
        if (heroImageUrl != null && !heroImageUrl.isEmpty()) {
            return heroImageUrl;
        }

        // Third priority: cardImageUrl (grid item image)
        if (cardImageUrl != null && !cardImageUrl.isEmpty()) {
            return cardImageUrl;
        }

        // No background URL available
        return null;
    }

    public String getBestVideoUrl() {
        if (videoSources != null && !videoSources.isEmpty()) {
            for (VideoSource source : videoSources) {
                if ("1080p".equals(source.getQuality()) || "720p".equals(source.getQuality())) {
                    Log.i("MediaItems", "Using video source with quality: " + source.getQuality());
                    return source.getUrl();
                }
            }
            return videoSources.get(0).getUrl();
        }

        if (hlsUrl != null && !hlsUrl.isEmpty()) {
            return hlsUrl;
        }
        if (dashUrl != null && !dashUrl.isEmpty()) {
            return dashUrl;
        }
        return videoUrl;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(posterUrl);
        dest.writeString(videoUrl);
        dest.writeString(subtitleUrl);
        dest.writeString(duration);
        dest.writeInt(year);
        dest.writeString(genre);
        dest.writeFloat(rating);
        dest.writeByte((byte) (isLive ? 1 : 0));
        dest.writeString(hlsUrl);
        dest.writeString(dashUrl);
        dest.writeString(tmdbId);
        dest.writeString(mediaType);
        dest.writeString(season);
        dest.writeString(episode);
        dest.writeByte((byte) (isFromTMDB ? 1 : 0));
        dest.writeString(status);
        dest.writeString(tagline);
        dest.writeInt(voteCount);
        dest.writeList(genres);
        dest.writeString(backgroundImageUrl);
        dest.writeString(heroImageUrl);
        dest.writeString(cardImageUrl);
        dest.writeList(videoSources);
        dest.writeList(subtitles);
        dest.writeString(sessionCookie);
        dest.writeString(refererUrl);

        // Write custom headers
        if (customHeaders != null) {
            dest.writeInt(customHeaders.size());
            for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(0);
        }

        // Write response headers
        if (responseHeaders != null) {
            dest.writeInt(responseHeaders.size());
            for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        } else {
            dest.writeInt(0);
        }
    }

    // Inner classes
    public static class VideoSource implements Parcelable {
        private String quality;
        private String url;

        // ✅ Per-source authentication headers
        private String sessionCookie;
        private Map<String, String> customHeaders;
        private String refererUrl;
        private Map<String, String> responseHeaders;

        public VideoSource() {
            this.customHeaders = new HashMap<>();
            this.responseHeaders = new HashMap<>();
        }

        public VideoSource(String quality, String url) {
            this.quality = quality;
            this.url = url;
            this.customHeaders = new HashMap<>();
            this.responseHeaders = new HashMap<>();
        }

        protected VideoSource(Parcel in) {
            quality = in.readString();
            url = in.readString();

            // Read per-source authentication data
            sessionCookie = in.readString();
            refererUrl = in.readString();

            // Read custom headers
            customHeaders = new HashMap<>();
            int customHeadersSize = in.readInt();
            for (int i = 0; i < customHeadersSize; i++) {
                String key = in.readString();
                String value = in.readString();
                customHeaders.put(key, value);
            }

            // Read response headers
            responseHeaders = new HashMap<>();
            int responseHeadersSize = in.readInt();
            for (int i = 0; i < responseHeadersSize; i++) {
                String key = in.readString();
                String value = in.readString();
                responseHeaders.put(key, value);
            }
        }

        public static final Creator<VideoSource> CREATOR = new Creator<VideoSource>() {
            @Override
            public VideoSource createFromParcel(Parcel in) {
                return new VideoSource(in);
            }

            @Override
            public VideoSource[] newArray(int size) {
                return new VideoSource[size];
            }
        };

        public String getQuality() {
            return quality;
        }

        public void setQuality(String quality) {
            this.quality = quality;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        // ✅ Getters and setters for per-source authentication
        public String getSessionCookie() {
            return sessionCookie;
        }

        public void setSessionCookie(String sessionCookie) {
            this.sessionCookie = sessionCookie;
        }

        public Map<String, String> getCustomHeaders() {
            return customHeaders;
        }

        public void setCustomHeaders(Map<String, String> customHeaders) {
            this.customHeaders = customHeaders != null ? customHeaders : new HashMap<>();
        }

        public String getRefererUrl() {
            return refererUrl;
        }

        public void setRefererUrl(String refererUrl) {
            this.refererUrl = refererUrl;
        }

        public Map<String, String> getResponseHeaders() {
            return responseHeaders;
        }

        public void setResponseHeaders(Map<String, String> responseHeaders) {
            this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(quality);
            dest.writeString(url);

            // Write per-source authentication data
            dest.writeString(sessionCookie);
            dest.writeString(refererUrl);

            // Write custom headers
            if (customHeaders != null) {
                dest.writeInt(customHeaders.size());
                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                    dest.writeString(entry.getKey());
                    dest.writeString(entry.getValue());
                }
            } else {
                dest.writeInt(0);
            }

            // Write response headers
            if (responseHeaders != null) {
                dest.writeInt(responseHeaders.size());
                for (Map.Entry<String, String> entry : responseHeaders.entrySet()) {
                    dest.writeString(entry.getKey());
                    dest.writeString(entry.getValue());
                }
            } else {
                dest.writeInt(0);
            }
        }

        @Override
        public String toString() {
            return "VideoSource{" +
                    "quality='" + quality + '\'' +
                    ", url='" + url + '\'' +
                    ", hasSessionCookie=" + (sessionCookie != null && !sessionCookie.isEmpty()) +
                    ", hasCustomHeaders=" + (customHeaders != null && !customHeaders.isEmpty()) +
                    ", refererUrl='" + refererUrl + '\'' +
                    '}';
        }
    }

    public static class SubtitleItem implements Parcelable {
        private String url;
        private String lang;
        private String language;

        public SubtitleItem() {}

        public SubtitleItem(String url, String lang, String language) {
            this.url = url;
            this.lang = lang;
            this.language = language;
        }

        protected SubtitleItem(Parcel in) {
            url = in.readString();
            lang = in.readString();
            language = in.readString();
        }

        public static final Creator<SubtitleItem> CREATOR = new Creator<SubtitleItem>() {
            @Override
            public SubtitleItem createFromParcel(Parcel in) {
                return new SubtitleItem(in);
            }

            @Override
            public SubtitleItem[] newArray(int size) {
                return new SubtitleItem[size];
            }
        };

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getLang() {
            return lang;
        }

        public void setLang(String lang) {
            this.lang = lang;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(url);
            dest.writeString(lang);
            dest.writeString(language);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("MediaItems{");
        sb.append("\n  id='").append(id).append('\'');
        sb.append(",\n  title='").append(title).append('\'');
        sb.append(",\n  mediaType='").append(mediaType).append('\'');
        sb.append(",\n  tmdbId='").append(tmdbId).append('\'');

        if ("tv".equals(mediaType)) {
            sb.append(",\n  season='").append(season).append('\'');
            sb.append(",\n  episode='").append(episode).append('\'');
        }

        sb.append(",\n  videoSources=").append(videoSources != null ? videoSources.size() : 0).append(" sources");
        sb.append(",\n  subtitles=").append(subtitles != null ? subtitles.size() : 0).append(" subtitles");
        sb.append(",\n  sessionCookie='").append(sessionCookie != null ? "[SET]" : "[NONE]").append('\'');
        sb.append(",\n  customHeaders=").append(customHeaders != null ? customHeaders.size() : 0).append(" headers");
        sb.append(",\n  responseHeaders=").append(responseHeaders != null ? responseHeaders.size() : 0).append(" headers");
        sb.append("\n}");
        return sb.toString();
    }
}