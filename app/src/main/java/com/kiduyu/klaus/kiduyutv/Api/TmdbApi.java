package com.kiduyu.klaus.kiduyutv.Api;



import android.util.Log;

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

}
