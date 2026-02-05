package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.kiduyu.klaus.kiduyutv.model.AnimeModel;
import com.kiduyu.klaus.kiduyutv.model.EpisodeModel;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Complete API for AnimeKai scraping
 * Handles anime list fetching and detailed episode information
 */
@RequiresApi(api = Build.VERSION_CODES.O)
public class AnimekaiApi {
    private static final String TAG = "AnimekaiApi";
    private static final String BASE_URL = "https://animekai.to";
    private static final String UPDATES_URL = BASE_URL + "/updates";
    private static final String KAI_AJAX = "https://animekai.to/ajax";
    private static final int TIMEOUT_MS = 15000;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36";
    private static final String REFERER = "https://animekai.to/";
    private static final String MEGA_REFERER = "https://megaup.live/";

    private final ExecutorService executorService;
    private final OkHttpClient httpClient;
    private final EncryptionService encryptionService;
    private final Handler mainHandler;

    public AnimekaiApi() {
        this.executorService = Executors.newFixedThreadPool(3);
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                .build();
        this.encryptionService = new EncryptionService();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /* ========================================
       CALLBACK INTERFACES
       ======================================== */

    public interface AnimeCallback {
        void onSuccess(List<AnimeModel> animeList);
        void onError(String error);
    }

    public interface AnimeDetailsCallback {
        void onSuccess(AnimeModel anime);
        void onError(String error);
    }

    /* ========================================
       FETCH ANIME LIST (Updates Page)
       ======================================== */

    /**
     * Fetch anime updates asynchronously
     */
    public void fetchAnimeUpdatesAsync(final AnimeCallback callback) {
        executorService.execute(() -> {
            try {
                List<AnimeModel> animeList = fetchAnimeUpdates();
                mainHandler.post(() -> callback.onSuccess(animeList));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching anime updates", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch anime updates synchronously
     */
    public List<AnimeModel> fetchAnimeUpdates() throws IOException {
        List<AnimeModel> animeList = new ArrayList<>();

        try {
            Document doc = Jsoup.connect(UPDATES_URL)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            Elements innerDivs = doc.select("div.inner");
            Log.i(TAG, "Found " + innerDivs.size() + " anime entries");

            for (Element inner : innerDivs) {
                try {
                    Element btn = inner.selectFirst("button.ttip-btn");
                    String dataTip = (btn != null) ? btn.attr("data-tip") : null;

                    Element poster = inner.selectFirst("a.poster");
                    String animeLink = null;
                    if (poster != null) {
                        String href = poster.attr("href");
                        animeLink = resolveUrl(BASE_URL, href);
                    }

                    Element img = inner.selectFirst("img.lazyload");
                    String animeImageBackground = null;
                    if (img != null) {
                        String dataSrc = img.attr("data-src");
                        if (dataSrc != null && !dataSrc.isEmpty()) {
                            animeImageBackground = dataSrc.replace("@300", "");
                        }
                    }

                    Element title = inner.selectFirst("a.title");
                    String animeName = (title != null) ? title.text().trim() : null;

                    if (animeName != null || animeLink != null) {
                        AnimeModel anime = new AnimeModel(
                                animeName,
                                dataTip,
                                animeLink,
                                animeImageBackground
                        );
                        animeList.add(anime);
                        Log.i(TAG, "Parsed anime: " + animeName + " (dataTip: " + dataTip + ")");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing individual anime entry", e);
                }
            }

            Log.i(TAG, "Successfully parsed " + animeList.size() + " anime entries");

        } catch (IOException e) {
            Log.e(TAG, "Network error fetching anime updates", e);
            throw e;
        }

        return animeList;
    }

    /* ========================================
       FETCH ANIME DETAILS WITH EPISODES
       ======================================== */

    /**
     * Fetch detailed anime information including episodes
     * @param contentId The data-tip value (e.g., "c4C8-aI")
     * @param animeurl The anime page URL for fetching additional metadata
     * @param callback Callback for results
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void fetchAnimeDetailsAsync(final String contentId, final String animeurl, final AnimeDetailsCallback callback) {
        executorService.execute(() -> {
            try {
                AnimeModel anime = fetchAnimeDetails(contentId, animeurl);
                mainHandler.post(() -> callback.onSuccess(anime));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching anime details for: " + contentId, e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch detailed anime information synchronously
     * @param contentId The data-tip value (e.g., "c4C8-aI")
     * @param animeurl The anime page URL for fetching additional metadata
     * @return AnimeModel with episodes populated
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public AnimeModel fetchAnimeDetails(String contentId, String animeurl) throws IOException, JSONException {
        Log.i(TAG, "Fetching anime details for contentId: " + contentId);

        AnimeModel anime = new AnimeModel();
        anime.setData_tip(contentId);
        anime.setAnime_link(animeurl);

        // Step 1: Encrypt the content ID
        String encryptedId = encryptionService.encrypt(contentId);
        Log.i(TAG, "Encrypted ID: " + encryptedId);

        // Step 2: Fetch episodes list
        String episodesUrl = KAI_AJAX + "/episodes/list?ani_id=" + contentId + "&_=" + encryptedId;
        String episodesJson = getJson(episodesUrl);

        // Step 3: Parse HTML response to get episodes structure
        String parsedEpisodes = encryptionService.parseHtml(episodesJson);
        Log.i(TAG, "Parsed episodes JSON: " + parsedEpisodes);

        // Step 4: Parse episodes into structured data
        Map<Integer, Map<Integer, EpisodeModel>> episodesMap = parseEpisodesJson(parsedEpisodes);
        anime.setEpisodes(episodesMap);

        // Step 5: Fetch additional metadata from anime page URL if available
        if (animeurl != null && !animeurl.isEmpty()) {
            fetchAnimeMetadataFromUrl(animeurl, anime);
        }

        Log.i(TAG, "Successfully fetched details for " + episodesMap.size() + " seasons");

        return anime;
    }


    @RequiresApi(api = Build.VERSION_CODES.O)
    /**
     * Fetch additional anime metadata from the anime page URL
     * Converts the Python scraping logic to Java
     * @param animeurl The anime page URL
     * @param anime The AnimeModel to populate with metadata
     */
    private void fetchAnimeMetadataFromUrl(String animeurl, AnimeModel anime) {
        Log.i(TAG, "Fetching additional metadata from: " + animeurl);

        try {
            Document doc = Jsoup.connect(animeurl)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS)
                    .get();

            // =======================
            // Extract MAL ID from watch-section
            // =======================
            String mal_id = "";
            Element watchSection = doc.selectFirst("div.watch-section");
            if (watchSection != null) {
                mal_id = watchSection.attr("data-mal-id");
                Log.i(TAG, "MAL ID: " + mal_id);
            } else {
                Log.w(TAG, "Could not find watch-section div");
            }

            // =======================
            // Extract background image URL from player-bg
            // =======================
            String backgroundImageUrl = "";
            Element playerBg = doc.selectFirst("div.player-bg");
            if (playerBg != null) {
                String style = playerBg.attr("style");
                if (!style.isEmpty()) {
                    // Extract URL from style attribute using regex
                    java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile("url\\(['\"]?([^'\"\\)]+)['\"]?\\)");
                    java.util.regex.Matcher urlMatcher = urlPattern.matcher(style);
                    if (urlMatcher.find()) {
                        backgroundImageUrl = urlMatcher.group(1);
                        if (!backgroundImageUrl.isEmpty()) {
                            anime.setAnime_image_backgroud(backgroundImageUrl);
                        }
                        Log.i(TAG, "Background Image URL: " + backgroundImageUrl);
                    }
                }
            } else {
                Log.w(TAG, "Could not find player-bg div");
            }

            // Locate main section
            Element mainSection = doc.selectFirst("section.entity-section");
            if (mainSection == null) {
                Log.w(TAG, "Could not find entity-section in anime page");
                return;
            }

            // Select inner detail divs containing label+value
            Elements detailDivs = mainSection.select("div.detail > div > div");

            // Helper: find value based on label text
            java.util.function.Function<String, String> getValue = (label) -> {
                for (Element div : detailDivs) {
                    if (div.text().trim().startsWith(label)) {
                        Element span = div.selectFirst("span");
                        if (span != null) {
                            return span.text().trim();
                        }
                    }
                }
                return "";
            };

            // =======================
            // Metadata
            // =======================
            String country = getValue.apply("Country:");
            if (!country.isEmpty()) {
                anime.setCountry(country);
            }

            // Genres
            List<String> genres = new ArrayList<>();
            Elements genreLinks = mainSection.select("div.detail div:has(span a[href^='/genres']) span a");
            for (Element link : genreLinks) {
                genres.add(link.text().trim());
            }
            if (!genres.isEmpty()) {
                anime.setGenres(genres);
            }

            String premiered = getValue.apply("Premiered:");
            if (!premiered.isEmpty()) {
                anime.setPremiered(premiered);
            }

            // Date aired
            String dateAiredText = getValue.apply("Date aired:");
            if (!dateAiredText.isEmpty()) {
                try {
                    String[] dateParts = dateAiredText.split("to");
                    java.time.LocalDate dateAiredFrom = null;
                    java.time.LocalDate dateAiredTo = null;

                    if (dateParts.length > 0 && !dateParts[0].trim().isEmpty()) {
                        dateAiredFrom = parseDate(dateParts[0].trim());
                    }
                    if (dateParts.length > 1 && !dateParts[1].trim().isEmpty()) {
                        dateAiredTo = parseDate(dateParts[1].trim());
                    }

                    if (dateAiredFrom != null) {
                        anime.setDateAiredFrom(dateAiredFrom);
                    }
                    if (dateAiredTo != null) {
                        anime.setDateAiredTo(dateAiredTo);
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error parsing date aired: " + dateAiredText);
                }
            }

            String broadcast = getValue.apply("Broadcast:");
            if (!broadcast.isEmpty()) {
                anime.setBroadcast(broadcast);
            }

            String totalEpisodesStr = getValue.apply("Episodes:");
            if (!totalEpisodesStr.isEmpty()) {
                try {
                    anime.setTotalEpisodes(Integer.parseInt(totalEpisodesStr));
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Error parsing total episodes: " + totalEpisodesStr);
                }
            }

            String duration = getValue.apply("Duration:");
            if (!duration.isEmpty()) {
                anime.setDuration(duration);
            }

            // =======================
            // Status & Ratings
            // =======================
            String status = getValue.apply("Status:");
            if (!status.isEmpty()) {
                anime.setStatus(status);
            }

            // MAL score
            Element malSpan = mainSection.selectFirst("div.detail div:has(span:contains(\"MAL\")) span");
            if (malSpan != null) {
                String malText = malSpan.text().trim();
                // Extract score using regex
                java.util.regex.Pattern scorePattern = java.util.regex.Pattern.compile("(\\d+\\.\\d+)");
                java.util.regex.Matcher scoreMatcher = scorePattern.matcher(malText);
                if (scoreMatcher.find()) {
                    anime.setMalScore(Double.parseDouble(scoreMatcher.group(1)));
                }
                // Extract user count using regex
                java.util.regex.Pattern usersPattern = java.util.regex.Pattern.compile("by ([\\d,]+) users");
                java.util.regex.Matcher usersMatcher = usersPattern.matcher(malText);
                if (usersMatcher.find()) {
                    String userCountStr = usersMatcher.group(1).replace(",", "");
                    anime.setMalUserCount(Integer.parseInt(userCountStr));
                }
            }

            // =======================
            // Production
            // =======================
            String studio = getValue.apply("Studios:");
            if (!studio.isEmpty()) {
                anime.setStudio(studio);
            }

            List<String> producers = new ArrayList<>();
            Elements producerLinks = mainSection.select("div.detail div:has(span a[href^='/producers']) span a");
            for (Element link : producerLinks) {
                producers.add(link.text().trim());
            }
            if (!producers.isEmpty()) {
                anime.setProducers(producers);
            }
            // =======================
            // Description
            // =======================
            Element descDiv = mainSection.selectFirst("div.desc.text-expand");
            if (descDiv != null) {
                String description = descDiv.text().trim();
                Log.i(TAG, "Description: " + description);
                if (!description.isEmpty()) {
                    anime.setAnimeDescription(description);
                }
            }

            // =======================
            // External Links
            // =======================
            Map<String, String> links = new HashMap<>();
            Elements linkElements = mainSection.select("div.detail div:has(span a[href^='http']) span a");
            for (Element link : linkElements) {
                String linkText = link.text().trim();
                String href = link.attr("href");
                if (!linkText.isEmpty() && !href.isEmpty()) {
                    links.put(linkText, href);
                }
            }
            if (!links.isEmpty()) {
                anime.setLinks(links);
            }

            Log.i(TAG, "Successfully fetched metadata from anime page");

        } catch (IOException e) {
            Log.e(TAG, "Error fetching anime metadata from URL: " + animeurl, e);
        }
    }




    /**
     * Parse date string to LocalDate
     * @param dateStr Date string in format "Oct 03, 2025"
     * @return LocalDate object or null if parsing fails
     */

    private java.time.LocalDate parseDate(String dateStr) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy");
            return java.time.LocalDate.parse(dateStr, formatter);
        } catch (Exception e) {
            Log.w(TAG, "Error parsing date: " + dateStr);
            return null;
        }
    }

    /**
     * Backward compatibility method - fetches details without URL metadata
     */
    public AnimeModel fetchAnimeDetails(String contentId) throws IOException, JSONException {
        return fetchAnimeDetails(contentId, null);
    }

    /* ========================================
       FETCH EPISODE SERVERS AND MEDIA
       ======================================== */

    /**
     * Fetch servers for a specific episode
     * @param episodeToken The episode token
     * @return Map of server types (sub, dub, softsub) to server data
     */
    public Map<String, Map<String, ServerInfo>> fetchEpisodeServers(String episodeToken)
            throws IOException, JSONException {

        Log.i(TAG, "Fetching servers for episode token: " + episodeToken);

        // Encrypt token
        String encryptedToken = encryptionService.encrypt(episodeToken);

        // Fetch servers
        String serversUrl = KAI_AJAX + "/links/list?token=" + episodeToken + "&_=" + encryptedToken;
        String serversJson = getJson(serversUrl);

        // Parse servers
        String parsedServers = encryptionService.parseHtml(serversJson);

        return parseServersJson(parsedServers);
    }

    /**
     * Resolve embed URL to get actual media sources
     * @param linkId Server link ID
     * @return MediaData with sources, tracks, download link, and skip info
     */
    public MediaData resolveEmbedUrl(String linkId) throws IOException, JSONException {
        Log.i(TAG, "Resolving embed for linkId: " + linkId);

        // Step 1: Encrypt link ID
        String encryptedLinkId = encryptionService.encrypt(linkId);

        // Step 2: Get embed URL - using getFullJson to get complete response
        String embedUrl = KAI_AJAX + "/links/view?id=" + linkId + "&_=" + encryptedLinkId;
        JSONObject embedResponse = getFullJson(embedUrl);
        Log.i(TAG, "Embed response: " + embedResponse.toString());


        // Step 3: Extract encrypted result
        String encryptedResult = embedResponse.getString("result");

        // Step 4: Decrypt to get embed URL and skip info
        String decryptedData = encryptionService.decrypt(encryptedResult);
        JSONObject decryptedJson = new JSONObject(decryptedData);

        String embedUrlActual = decryptedJson.getString("url");
        JSONObject skipInfo = decryptedJson.optJSONObject("skip");

        Log.i(TAG, "Embed URL: " + embedUrlActual);

        // Step 5: Resolve the actual media from embed
        MediaData mediaData = resolveEmbed(embedUrlActual);

        // Step 6: Add skip info
        if (skipInfo != null) {
            mediaData.setSkipInfo(parseSkipInfo(skipInfo));
        }

        return mediaData;
    }

    /**
     * Resolve embed URL to actual media sources
     */
    private MediaData resolveEmbed(String embedUrl) throws IOException, JSONException {
        // Replace /e/ with /media/
        String mediaUrl = embedUrl.replace("/e/", "/media/");
        Log.i(TAG, "Media URL: " + mediaUrl);

        // Fetch encrypted media payload
        Request request = new Request.Builder()
                .url(mediaUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", MEGA_REFERER)
                .header("Accept", "application/json")
                .build();

        String encryptedMedia;
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to fetch media: " + response.code());
            }
            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            encryptedMedia = json.getString("result");
        }

        // Decrypt media
        String decryptedMedia = encryptionService.decryptMega(encryptedMedia, USER_AGENT);
        Log.i(TAG, "Decrypted media data"+decryptedMedia);

        // Parse media data
        return parseMediaData(decryptedMedia);
    }

    /* ========================================
       HELPER METHODS
       ======================================== */

    /**
     * Get JSON from URL with proper headers
     * Note: This extracts the "result" field from the response
     */
    private String getJson(String url) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code());
            }

            String responseBody = response.body().string();
            JSONObject json = new JSONObject(responseBody);
            return json.getString("result");
        }
    }

    /**
     * Get full JSON response from URL with proper headers
     * Returns the complete JSONObject without extracting any fields
     */
    private JSONObject getFullJson(String url) throws IOException, JSONException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", USER_AGENT)
                .header("Referer", REFERER)
                .header("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Request failed: " + response.code());
            }

            String responseBody = response.body().string();
            return new JSONObject(responseBody);
        }
    }

    /**
     * Parse episodes JSON into structured map
     */
    private Map<Integer, Map<Integer, EpisodeModel>> parseEpisodesJson(String json) throws JSONException {
        Map<Integer, Map<Integer, EpisodeModel>> episodesMap = new HashMap<>();

        JSONObject seasonsJson = new JSONObject(json);
        Iterator<String> seasonKeys = seasonsJson.keys();

        while (seasonKeys.hasNext()) {
            String seasonKey = seasonKeys.next();
            int seasonNumber = Integer.parseInt(seasonKey);

            JSONObject episodesJson = seasonsJson.getJSONObject(seasonKey);
            Map<Integer, EpisodeModel> episodeMap = new HashMap<>();

            Iterator<String> episodeKeys = episodesJson.keys();
            while (episodeKeys.hasNext()) {
                String episodeKey = episodeKeys.next();
                int episodeNumber = Integer.parseInt(episodeKey);

                JSONObject epData = episodesJson.getJSONObject(episodeKey);
                String title = epData.getString("title");
                String token = epData.getString("token");

                EpisodeModel episode = new EpisodeModel(seasonNumber, episodeNumber, title, token);
                episodeMap.put(episodeNumber, episode);
            }

            episodesMap.put(seasonNumber, episodeMap);
        }

        return episodesMap;
    }

    /**
     * Parse servers JSON
     */
    private Map<String, Map<String, ServerInfo>> parseServersJson(String json) throws JSONException {
        Map<String, Map<String, ServerInfo>> serversMap = new HashMap<>();

        JSONObject serversJson = new JSONObject(json);

        for (String serverType : new String[]{"sub", "dub", "softsub"}) {
            if (serversJson.has(serverType)) {
                JSONObject typeServers = serversJson.getJSONObject(serverType);
                Map<String, ServerInfo> serverInfoMap = new HashMap<>();

                Iterator<String> keys = typeServers.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject serverData = typeServers.getJSONObject(key);

                    ServerInfo info = new ServerInfo();
                    info.linkId = serverData.getString("lid");
                    info.serverType = serverType;
                    info.index = Integer.parseInt(key);

                    serverInfoMap.put(key, info);
                    Log.i(TAG, "Parsed server info: " +info.linkId + " - " + info.serverType + " - " + info.index);
                }

                serversMap.put(serverType, serverInfoMap);
            }
        }

        return serversMap;
    }

    /**
     * Parse media data JSON
     */
    private MediaData parseMediaData(String json) throws JSONException {
        MediaData mediaData = new MediaData();
        JSONObject mediaJson = new JSONObject(json);

        // Parse sources
        if (mediaJson.has("sources")) {
            List<String> sources = new ArrayList<>();
            org.json.JSONArray sourcesArray = mediaJson.getJSONArray("sources");
            for (int i = 0; i < sourcesArray.length(); i++) {
                JSONObject source = sourcesArray.getJSONObject(i);
                sources.add(source.getString("file"));
            }
            mediaData.setSources(sources);
        }

        // Parse tracks
        if (mediaJson.has("tracks")) {
            List<EpisodeModel.Track> tracks = new ArrayList<>();
            org.json.JSONArray tracksArray = mediaJson.getJSONArray("tracks");
            for (int i = 0; i < tracksArray.length(); i++) {
                JSONObject track = tracksArray.getJSONObject(i);

                // Get required fields
                String file = track.getString("file");
                String kind = track.getString("kind");

                // Get optional fields - use optString for label since thumbnails don't have it
                String label = track.optString("label", "");
                boolean isDefault = track.optBoolean("default", false);

                // Only add subtitle/caption tracks, skip thumbnails
                if (!"thumbnails".equals(kind)) {
                    EpisodeModel.Track trackObj = new EpisodeModel.Track(
                            file,
                            label,
                            kind,
                            isDefault
                    );
                    tracks.add(trackObj);
                    Log.i(TAG, "Added track: " + kind + " - " + label);
                } else {
                    Log.i(TAG, "Skipping thumbnails track");
                }
            }
            mediaData.setTracks(tracks);
        }

        // Parse download link
        if (mediaJson.has("download")) {
            mediaData.setDownloadLink(mediaJson.getString("download"));
        }

        return mediaData;
    }

    /**
     * Parse skip info
     */
    private EpisodeModel.SkipInfo parseSkipInfo(JSONObject skipJson) throws JSONException {
        EpisodeModel.SkipInfo skipInfo = new EpisodeModel.SkipInfo();

        if (skipJson.has("intro")) {
            org.json.JSONArray introArray = skipJson.getJSONArray("intro");
            int[] intro = new int[]{introArray.getInt(0), introArray.getInt(1)};
            skipInfo.setIntro(intro);
        }

        if (skipJson.has("outro")) {
            org.json.JSONArray outroArray = skipJson.getJSONArray("outro");
            int[] outro = new int[]{outroArray.getInt(0), outroArray.getInt(1)};
            skipInfo.setOutro(outro);
        }

        return skipInfo;
    }

    /**
     * Resolve relative URLs to absolute URLs
     */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isEmpty()) {
            return null;
        }

        if (relativeUrl.startsWith("http://") || relativeUrl.startsWith("https://")) {
            return relativeUrl;
        }

        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error resolving URL: " + relativeUrl, e);
            if (relativeUrl.startsWith("/")) {
                return baseUrl + relativeUrl;
            } else {
                return baseUrl + "/" + relativeUrl;
            }
        }
    }

    /**
     * Shutdown all services
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (encryptionService != null) {
            encryptionService.shutdown();
        }
        if (httpClient != null) {
            httpClient.dispatcher().executorService().shutdown();
            httpClient.connectionPool().evictAll();
        }
    }

    /* ========================================
       DATA CLASSES
       ======================================== */

    public static class ServerInfo {
        public String linkId;
        public String serverType; // sub, dub, softsub
        public int index;
    }

    public static class MediaData {
        private List<String> sources;
        private List<EpisodeModel.Track> tracks;
        private String downloadLink;
        private EpisodeModel.SkipInfo skipInfo;

        public List<String> getSources() { return sources; }
        public void setSources(List<String> sources) { this.sources = sources; }

        public List<EpisodeModel.Track> getTracks() { return tracks; }
        public void setTracks(List<EpisodeModel.Track> tracks) { this.tracks = tracks; }

        public String getDownloadLink() { return downloadLink; }
        public void setDownloadLink(String downloadLink) { this.downloadLink = downloadLink; }

        public EpisodeModel.SkipInfo getSkipInfo() { return skipInfo; }
        public void setSkipInfo(EpisodeModel.SkipInfo skipInfo) { this.skipInfo = skipInfo; }
    }
}