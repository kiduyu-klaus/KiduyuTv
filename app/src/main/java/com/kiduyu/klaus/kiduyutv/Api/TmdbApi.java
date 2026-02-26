package com.kiduyu.klaus.kiduyutv.Api;



import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.utils.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TmdbApi {
    private static final String TAG = "TmdbApi";
    public static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/";
    public static final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiI0MTAzZmMzMDY1YzEyMmViNWRiNmJkY2ZmNzQ5ZmRlNyIsIm5iZiI6MTY2ODA2NDAzNC4yNDk5OTk4LCJzdWIiOiI2MzZjYTMyMjA0OTlmMjAwN2ZlYjA4MWEiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.tjvtYPTPfLOyMdOouQ14GGgOzmfnZRW4RgvOzfoq19w";

    public static final String POSTER_SIZE = "w500";
    public static final String BACKDROP_SIZE = "w1280";
    private static final String ORIGINAL_SIZE = "original";
    public static final int TIMEOUT_MS = 10000;



    public enum ContentType {
        MOVIE, TV, ALL
    }
    public static List<MediaItems> fetchMoviesFromTMDB(String urlString) throws IOException, JSONException {
        List<MediaItems> movies = new ArrayList<>();

        Connection.Response response = Jsoup.connect(urlString)
                .header("accept", "application/json")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        if (response.statusCode() == 200) {
            Log.i(TAG, "fetchMoviesFromTMDB: Success");

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray results = jsonResponse.getJSONArray("results");

            for (int i = 0; i < results.length() && i < 20; i++) { // Limit to 20 items
                JSONObject movieJson = results.getJSONObject(i);
                MediaItems movie = createMediaItemFromTMDB(movieJson, TmdbApi.ContentType.MOVIE);
                if (movie != null) {
                    movies.add(movie);
                }
            }
        } else {

            Log.e(TAG, "fetchMoviesFromTMDB: Failed with status " + response.statusCode());
            throw new IOException("Failed to fetch data: " + response.statusCode());
        }

        return movies;
    }

    public static MediaItems createMediaItemFromTMDB(JSONObject tmdbItem, ContentType contentType) {
        try {
            MediaItems mediaItems = new MediaItems();

            // Basic info
            int id = tmdbItem.getInt("id");
            String title = contentType == ContentType.MOVIE ?
                    tmdbItem.getString("title") : tmdbItem.getString("name");
            String description = tmdbItem.optString("overview", "No description available");
            String releaseDate = contentType == ContentType.MOVIE ?
                    tmdbItem.optString("release_date", "") :
                    tmdbItem.optString("first_air_date", "");
            double rating = tmdbItem.optDouble("vote_average", 0.0);

            // Set basic properties
            mediaItems.setId(String.valueOf(id));
            mediaItems.setTitle(title);
            mediaItems.setDescription(description);

            // Parse year safely
            if (!releaseDate.isEmpty() && releaseDate.length() >= 4) {
                try {
                    mediaItems.setYear(Integer.parseInt(releaseDate.substring(0, 4)));
                } catch (NumberFormatException e) {
                    mediaItems.setYear(0);
                }
            } else {
                mediaItems.setYear(0);
            }

            mediaItems.setRating((float) rating);
            mediaItems.setTmdbId(String.valueOf(id));

            // Set content type
            if (contentType == ContentType.MOVIE) {
                mediaItems.setMediaType("movie");
            } else {
                mediaItems.setMediaType("tv");
            }

            // Set poster and backdrop URLs
            String posterPath = tmdbItem.optString("poster_path", "");
            String backdropPath = tmdbItem.optString("backdrop_path", "");

            if (!posterPath.isEmpty()) {
                mediaItems.setPosterUrl(IMAGE_BASE_URL + POSTER_SIZE + posterPath);
                mediaItems.setCardImageUrl(IMAGE_BASE_URL + POSTER_SIZE + posterPath);
            }

            if (!backdropPath.isEmpty()) {
                mediaItems.setBackgroundImageUrl(IMAGE_BASE_URL + BACKDROP_SIZE + backdropPath);
                mediaItems.setHeroImageUrl(IMAGE_BASE_URL + ORIGINAL_SIZE + backdropPath);
            } else if (!posterPath.isEmpty()) {
                // Fallback to poster if no backdrop
                mediaItems.setBackgroundImageUrl(IMAGE_BASE_URL + ORIGINAL_SIZE + posterPath);
                mediaItems.setHeroImageUrl(IMAGE_BASE_URL + ORIGINAL_SIZE + posterPath);
            }

            // Handle genres from genre_ids array
            JSONArray genreIds = tmdbItem.optJSONArray("genre_ids");
            if (genreIds != null && genreIds.length() > 0) {
                List<String> genres = new ArrayList<>();
                for (int i = 0; i < genreIds.length(); i++) {
                    String genreName = utils.getGenreName(genreIds.getInt(i));
                    if (genreName != null) {
                        genres.add(genreName);
                    }
                }
                mediaItems.setGenres(genres);

                // Set first genre as the genre string for backward compatibility
                if (!genres.isEmpty()) {
                    mediaItems.setGenre(genres.get(0));
                }
            }

            // Set from TMDB flag
            mediaItems.setFromTMDB(true);

            return mediaItems;

        } catch (JSONException e) {
            Log.e(TAG, "Error creating MediaItem from TMDB data", e);
            return null;
        }
    }

    /**
     * Common method to fetch TV shows from TMDB API - runs on background thread
     */
    public static List<MediaItems> fetchTVShowsFromTMDB(String urlString) throws IOException, JSONException {
        List<MediaItems> tvShows = new ArrayList<>();

        Connection.Response response = Jsoup.connect(urlString)
                .header("accept", "application/json")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        if (response.statusCode() == 200) {
            Log.i(TAG, "fetchTVShowsFromTMDB: Success");

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray results = jsonResponse.getJSONArray("results");

            for (int i = 0; i < results.length() && i < 20; i++) { // Limit to 20 items
                JSONObject tvShowJson = results.getJSONObject(i);
                MediaItems tvShow = createMediaItemFromTMDB(tvShowJson, ContentType.TV);
                if (tvShow != null) {
                    tvShows.add(tvShow);
                }
            }
        } else {
            Log.e(TAG, "fetchTVShowsFromTMDB: Failed with status " + response.statusCode());
            throw new IOException("Failed to fetch data: " + response.statusCode());
        }

        return tvShows;
    }

    /**
     * Fetch recommendations from TMDB API using Jsoup
     */
    public static List<MediaItems> fetchRecommendationsFromTMDB(String urlString, String mediaType) throws IOException, JSONException {
        List<MediaItems> recommendations = new ArrayList<>();

        Connection.Response response = Jsoup.connect(urlString)
                .header("accept", "application/json")
                .header("Authorization", "Bearer " + BEARER_TOKEN)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        if (response.statusCode() == 200) {
            Log.i(TAG, "fetchRecommendationsFromTMDB: Success");

            JSONObject jsonResponse = new JSONObject(response.body());
            JSONArray results = jsonResponse.getJSONArray("results");

            for (int i = 0; i < results.length() && i < 20; i++) {
                JSONObject itemJson = results.getJSONObject(i);

                // Determine content type for parsing
                ContentType contentType = "movie".equals(mediaType) ?
                        ContentType.MOVIE : ContentType.TV;

                MediaItems item = createMediaItemFromTMDB(itemJson, contentType);
                if (item != null) {
                    recommendations.add(item);
                }
            }
        } else {
            Log.e(TAG, "fetchRecommendationsFromTMDB: Failed with status " + response.statusCode());
            throw new IOException("Failed to fetch recommendations: " + response.statusCode());
        }

        return recommendations;
    }

    public static List<MediaItems> searchContent(String query, ContentType contentType, int page) {
        List<MediaItems> results = new ArrayList<>();

        try {
            String endpoint = contentType == ContentType.MOVIE ? "movie" : "tv";
            String url = String.format("%s/search/%s?query=%s&page=%d",
                    TmdbRepository.TMDB_BASE_URL, endpoint, query.replace(" ", "%20"), page);
            String response = makeRequest(url);

            if (response != null) {
                JSONObject jsonObject = new JSONObject(response);
                JSONArray resultsArray = jsonObject.getJSONArray("results");

                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject item = resultsArray.getJSONObject(i);
                    MediaItems mediaItems = createMediaItemFromTMDB(item, contentType);
                    if (mediaItems != null) {
                        results.add(mediaItems);
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing search results", e);
        } catch (Exception e) {
            Log.e(TAG, "Error searching content", e);
        }

        return results;
    }

    /**
     * Make HTTP request to TMDB API using Jsoup
     */
    private static String makeRequest(String urlString) {
        try {
            Connection.Response response = Jsoup.connect(urlString)
                    .header("accept", "application/json")
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .ignoreContentType(true)  // Important: allows Jsoup to fetch JSON
                    .timeout(TIMEOUT_MS)
                    .method(Connection.Method.GET)
                    .execute();

            if (response.statusCode() == 200) {
                return response.body();
            } else {
                Log.e(TAG, "HTTP Error: " + response.statusCode() + " - " + response.statusMessage());
                System.out.println("HTTP Error: " + response.statusCode() + " - " + response.statusMessage());
                return null;
            }

        } catch (IOException e) {
            Log.e(TAG, "Error making HTTP request", e);
            return null;
        }
    }

    /**
     * Fetch top production companies from a remote JSON list,
     * then fetch full details for each from TMDB /company/{id}
     */
    public static List<CompanyNetwork> fetchTopProductionCompanies() throws IOException, JSONException {
        List<CompanyNetwork> companies = new ArrayList<>();

        // Step 1: Fetch company IDs from the remote JSON
        String companiesJsonUrl = "https://raw.githubusercontent.com/kiduyu-klaus/KiduyuTv/refs/heads/main/companies.json";

        Connection.Response listResponse = Jsoup.connect(companiesJsonUrl)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        if (listResponse.statusCode() != 200) {
            Log.e(TAG, "fetchTopProductionCompanies: Failed to fetch company list, status " + listResponse.statusCode());
            throw new IOException("Failed to fetch company list: " + listResponse.statusCode());
        }

        JSONArray companyList = new JSONArray(listResponse.body());

        // Step 2: For each company_id, fetch full details from TMDB
        for (int i = 0; i < companyList.length(); i++) {
            JSONObject item = companyList.getJSONObject(i);
            int companyId = item.getInt("company_id");

            String detailUrl = TmdbRepository.TMDB_BASE_URL + "/company/" + companyId;

            try {
                Connection.Response detailResponse = Jsoup.connect(detailUrl)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (detailResponse.statusCode() == 200) {
                    Log.i(TAG, "fetchTopProductionCompanies: Fetched company id=" + companyId);

                    JSONObject companyJson = new JSONObject(detailResponse.body());
                    CompanyNetwork company = createCompanyFromTMDB(companyJson, false);
                    if (company != null) {
                        companies.add(company);
                    }
                } else {
                    Log.w(TAG, "fetchTopProductionCompanies: Skipping company id=" + companyId
                            + ", status=" + detailResponse.statusCode());
                }

            } catch (IOException | JSONException e) {
                Log.e(TAG, "fetchTopProductionCompanies: Error fetching company id=" + companyId, e);
                // Continue to next company instead of failing entirely
            }
        }

        return companies;
    }


    /**
     * Fetch top TV networks from a remote JSON list,
     * then fetch full details for each from TMDB /network/{id}
     */
    public static List<CompanyNetwork> fetchTopTVNetworks() throws IOException, JSONException {
        List<CompanyNetwork> networks = new ArrayList<>();

        // Step 1: Fetch network IDs from the remote JSON
        String networksJsonUrl = "https://raw.githubusercontent.com/kiduyu-klaus/KiduyuTv/refs/heads/main/networks.json";

        Connection.Response listResponse = Jsoup.connect(networksJsonUrl)
                .ignoreContentType(true)
                .timeout(TIMEOUT_MS)
                .method(Connection.Method.GET)
                .execute();

        if (listResponse.statusCode() != 200) {
            Log.e(TAG, "fetchTopTVNetworks: Failed to fetch network list, status " + listResponse.statusCode());
            throw new IOException("Failed to fetch network list: " + listResponse.statusCode());
        }

        JSONArray networkList = new JSONArray(listResponse.body());

        // Step 2: For each network_id, fetch full details from TMDB
        for (int i = 0; i < networkList.length(); i++) {
            JSONObject item = networkList.getJSONObject(i);
            int networkId = item.getInt("network_id");

            String detailUrl = TmdbRepository.TMDB_BASE_URL + "/network/" + networkId;

            try {
                Connection.Response detailResponse = Jsoup.connect(detailUrl)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (detailResponse.statusCode() == 200) {
                    Log.i(TAG, "fetchTopTVNetworks: Fetched network id=" + networkId);

                    JSONObject networkJson = new JSONObject(detailResponse.body());
                    CompanyNetwork network = createCompanyFromTMDB(networkJson, true);
                    if (network != null) {
                        networks.add(network);
                    }
                } else {
                    Log.w(TAG, "fetchTopTVNetworks: Skipping network id=" + networkId
                            + ", status=" + detailResponse.statusCode());
                }

            } catch (IOException | JSONException e) {
                Log.e(TAG, "fetchTopTVNetworks: Error fetching network id=" + networkId, e);
                // Continue to next network instead of failing entirely
                //throw new IOException("Failed to fetch networks: " + e.getMessage());
            }
        }

        return networks;
    }
    /**
     * Create a CompanyNetwork object from TMDB JSON response
     */
    private static CompanyNetwork createCompanyFromTMDB(JSONObject tmdbItem, boolean isNetwork) {
        try {
            CompanyNetwork companyNetwork = new CompanyNetwork();

            int id = tmdbItem.getInt("id");
            String name = tmdbItem.getString("name");

            companyNetwork.setId(id);
            companyNetwork.setName(name);
            companyNetwork.setNetwork(isNetwork);

            // Set logo URL if available
            String logoPath = tmdbItem.optString("logo_path", "");
            if (!logoPath.isEmpty()) {
                companyNetwork.setLogoPath(IMAGE_BASE_URL + "w500" + logoPath);
            }

            return companyNetwork;

        } catch (JSONException e) {
            Log.e(TAG, "Error creating CompanyNetwork from TMDB data", e);
            return null;
        }
    }

    /**
     * Discover movies by production company
     * @param companyId The TMDB company ID
     * @param page Page number for pagination
     */
    public static List<MediaItems> discoverMoviesByCompany(int companyId, int page) throws IOException, JSONException {
        List<MediaItems> movies = new ArrayList<>();

        String urlString = TmdbRepository.TMDB_BASE_URL + "/discover/movie?include_adult=false&include_video=false&language=en-US&page=" + page + "&sort_by=popularity.desc&with_companies=" + companyId;

        movies = fetchMoviesFromTMDB(urlString);

        return movies;
    }

    /**
     * Discover TV shows by network
     * @param networkId The TMDB network ID
     * @param page Page number for pagination
     */
    public static List<MediaItems> discoverTVShowsByNetwork(int networkId, int page) throws IOException, JSONException {
        List<MediaItems> tvShows = new ArrayList<>();

        String urlString = TmdbRepository.TMDB_BASE_URL + "/discover/tv?include_adult=false&include_null_first_air_dates=false&language=en-US&page=" + page + "&sort_by=popularity.desc&with_networks=" + networkId;

        tvShows = fetchTVShowsFromTMDB(urlString);

        return tvShows;
    }

    /**
     * Discover movies by network
     * @param networkId The TMDB network ID
     * @param page Page number for pagination
     */
    public static List<MediaItems> discoverMoviesByNetwork(int networkId, int page) throws IOException, JSONException {
        String urlString = TmdbRepository.TMDB_BASE_URL + "/discover/movie?include_adult=false&include_video=false&language=en-US&page=" + page + "&sort_by=popularity.desc&with_networks=" + networkId;

        return fetchMoviesFromTMDB(urlString);
    }

    /**
     * Discover TV shows by production company
     * @param companyId The TMDB company ID
     * @param page Page number for pagination
     */
    public static List<MediaItems> discoverTVShowsByCompany(int companyId, int page) throws IOException, JSONException {
        String urlString = TmdbRepository.TMDB_BASE_URL + "/discover/tv?include_adult=false&include_null_first_air_dates=false&language=en-US&page=" + page + "&sort_by=popularity.desc&with_companies=" + companyId;

        return fetchTVShowsFromTMDB(urlString);
    }

}
