package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.Episode;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;
import com.kiduyu.klaus.kiduyutv.model.Season;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TmdbRepository {
    private static final String TAG = "TmdbRepository";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    private static TmdbRepository instance;

    private static final String CUSTOM_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    // Thread management
    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());




    // Callback interface for async operations
    public interface TMDBCallback {
        void onSuccess(List<MediaItems> movies);

        void onError(String error);
    }
    public interface TVShowDetailsCallback {
        void onSuccess(MediaItems detailedShow, List<Season> seasons);

        void onError(String error);
    }
    public void getFeaturedMoviesAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&region=US&sort_by=popularity.desc";
                List<MediaItems> movies = TmdbApi.fetchMoviesFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(movies));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching featured movies", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Async method to fetch top rated movies from TMDB API
     */
    public void getTopRatedMoviesAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/movie/top_rated?language=en-US&page=1&region=US&sort_by=popularity.desc";
                List<MediaItems> movies = TmdbApi.fetchMoviesFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(movies));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching top rated movies", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Async method to fetch action movies from TMDB API
     */
    public void getActionMoviesAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                // Genre ID 28 is Action
                String urlString = TMDB_BASE_URL + "/discover/movie?include_adult=false&include_video=false&language=en-US&page=1&sort_by=popularity.desc&with_genres=28";
                List<MediaItems> movies = TmdbApi.fetchMoviesFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(movies));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching action movies", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch popular TV shows in the US
     */
    public void getPopularTVShowsAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/tv/popular?language=en-US&page=1&region=US";
                List<MediaItems> tvShows = TmdbApi.fetchTVShowsFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(tvShows));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching popular TV shows", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch top rated TV shows in the US
     */
    public void getTopRatedTVShowsAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/tv/top_rated?language=en-US&page=1&region=US";
                List<MediaItems> tvShows = TmdbApi.fetchTVShowsFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(tvShows));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching top rated TV shows", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch trending TV shows (weekly)
     */
    public void getTrendingTVShowsAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/trending/tv/week?language=en-US";
                List<MediaItems> tvShows = TmdbApi.fetchTVShowsFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(tvShows));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching trending TV shows", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch action & adventure TV shows
     */
    public void getActionAdventureTVShowsAsync(TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                // Genre ID 10759 is Action & Adventure
                String urlString = TMDB_BASE_URL + "/discover/tv?include_adult=false&include_null_first_air_dates=false&language=en-US&page=1&sort_by=popularity.desc&with_genres=10759&region=US";
                List<MediaItems> tvShows = TmdbApi.fetchTVShowsFromTMDB(urlString);
                mainHandler.post(() -> callback.onSuccess(tvShows));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching action & adventure TV shows", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Async method to fetch movie/TV recommendations
     */
    public void getRecommendationsAsync(String tmdbId, String mediaType, TMDBCallback callback) {
        executorService.execute(() -> {
            try {
                String urlString = TMDB_BASE_URL + "/" + mediaType + "/" + tmdbId + "/recommendations?language=en-US&page=1";
                List<MediaItems> recommendations = TmdbApi.fetchRecommendationsFromTMDB(urlString, mediaType);
                mainHandler.post(() -> callback.onSuccess(recommendations));
            } catch (Exception e) {
                Log.e(TAG, "Error fetching recommendations", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get detailed TV show information including all seasons
     */
    public void getTVShowDetails(String tmdbId, TVShowDetailsCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/tv/" + tmdbId + "?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());

                    // Create detailed MediaItems
                    MediaItems tvShow = TmdbApi.createMediaItemFromTMDB(jsonResponse, TmdbApi.ContentType.TV);

                    // Parse seasons
                    List<Season> seasons = new ArrayList<>();
                    JSONArray seasonsArray = jsonResponse.optJSONArray("seasons");

                    if (seasonsArray != null) {
                        for (int i = 0; i < seasonsArray.length(); i++) {
                            JSONObject seasonJson = seasonsArray.getJSONObject(i);

                            // Skip "Season 0" (specials)
                            int seasonNumber = seasonJson.optInt("season_number", 0);
                            if (seasonNumber == 0) continue;

                            Season season = new Season();
                            season.setId(seasonJson.optInt("id", 0));
                            season.setName(seasonJson.optString("name", "Season " + seasonNumber));
                            season.setOverview(seasonJson.optString("overview", ""));
                            season.setSeasonNumber(seasonNumber);
                            season.setEpisodeCount(seasonJson.optInt("episode_count", 0));
                            season.setAirDate(seasonJson.optString("air_date", ""));

                            String posterPath = seasonJson.optString("poster_path", "");
                            if (!posterPath.isEmpty()) {
                                season.setPosterPath(TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + posterPath);
                            }

                            seasons.add(season);
                        }
                    }

                    MediaItems finalTvShow = tvShow;
                    mainHandler.post(() -> callback.onSuccess(finalTvShow, seasons));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching TV show details", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    public interface EpisodesCallback {
        void onSuccess(List<Episode> episodes);

        void onError(String error);
    }

    /**
     * Get episodes for a specific season
     */
    public void getSeasonEpisodes(String tmdbId, int seasonNumber, EpisodesCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/tv/" + tmdbId + "/season/" + seasonNumber + "?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());

                    List<Episode> episodes = new ArrayList<>();
                    JSONArray episodesArray = jsonResponse.optJSONArray("episodes");

                    if (episodesArray != null) {
                        for (int i = 0; i < episodesArray.length(); i++) {
                            JSONObject episodeJson = episodesArray.getJSONObject(i);

                            Episode episode = new Episode();
                            episode.setId(episodeJson.optInt("id", 0));
                            episode.setName(episodeJson.optString("name", "Episode " + (i + 1)));
                            episode.setOverview(episodeJson.optString("overview", "No description available"));
                            episode.setEpisodeNumber(episodeJson.optInt("episode_number", i + 1));
                            episode.setSeasonNumber(seasonNumber);
                            episode.setAirDate(episodeJson.optString("air_date", ""));
                            episode.setVoteAverage(episodeJson.optDouble("vote_average", 0.0));
                            episode.setRuntime(episodeJson.optInt("runtime", 0));

                            String stillPath = episodeJson.optString("still_path", "");
                            if (!stillPath.isEmpty()) {
                                episode.setStillPath(TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + stillPath);
                            }

                            episodes.add(episode);
                        }
                    }

                    mainHandler.post(() -> callback.onSuccess(episodes));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching season episodes", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }
}
