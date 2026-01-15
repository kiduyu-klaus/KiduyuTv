package com.kiduyu.klaus.kiduyutv.Api;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.kiduyu.klaus.kiduyutv.model.CastMember;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CastRepository {
    private static final String TAG = "CastRepository";
    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Callback interfaces
    public interface CastListCallback {
        void onSuccess(List<CastMember> castList);
        void onError(String error);
    }

    public interface CastDetailsCallback {
        void onSuccess(CastMember castMember);
        void onError(String error);
    }

    public interface PersonCreditsCallback {
        void onSuccess(List<MediaItems> credits);
        void onError(String error);
    }

    /**
     * Fetch cast list for a TV show
     */
    public void getTVShowCast(String tvShowId, CastListCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/tv/" + tvShowId + "/credits?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray castArray = jsonResponse.optJSONArray("cast");

                    List<CastMember> castList = new ArrayList<>();

                    if (castArray != null) {
                        int limit = Math.min(castArray.length(), 20); // Limit to 20 cast members
                        for (int i = 0; i < limit; i++) {
                            JSONObject castJson = castArray.getJSONObject(i);
                            CastMember castMember = parseCastMember(castJson);
                            castList.add(castMember);
                        }
                    }

                    List<CastMember> finalCastList = castList;
                    mainHandler.post(() -> callback.onSuccess(finalCastList));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching TV show cast", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Fetch cast list for a movie
     */
    public void getMovieCast(String movieId, CastListCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/movie/" + movieId + "/credits?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray castArray = jsonResponse.optJSONArray("cast");

                    List<CastMember> castList = new ArrayList<>();

                    if (castArray != null) {
                        int limit = Math.min(castArray.length(), 20);
                        for (int i = 0; i < limit; i++) {
                            JSONObject castJson = castArray.getJSONObject(i);
                            CastMember castMember = parseCastMember(castJson);
                            castList.add(castMember);
                        }
                    }

                    List<CastMember> finalCastList = castList;
                    mainHandler.post(() -> callback.onSuccess(finalCastList));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching movie cast", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get detailed information about a cast member (person)
     */
    public void getCastDetails(int personId, CastDetailsCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/person/" + personId + "?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());

                    CastMember castMember = new CastMember();
                    castMember.setId(jsonResponse.optInt("id", 0));
                    castMember.setName(jsonResponse.optString("name", "Unknown"));
                    castMember.setBiography(jsonResponse.optString("biography", "No biography available"));
                    castMember.setBirthday(jsonResponse.optString("birthday", ""));
                    castMember.setDeathday(jsonResponse.optString("deathday", ""));
                    castMember.setPlaceOfBirth(jsonResponse.optString("place_of_birth", ""));
                    castMember.setKnownForDepartment(jsonResponse.optString("known_for_department", ""));
                    castMember.setPopularity((float) jsonResponse.optDouble("popularity", 0.0));
                    castMember.setProfilePath(jsonResponse.optString("profile_path", ""));

                    CastMember finalCastMember = castMember;
                    mainHandler.post(() -> callback.onSuccess(finalCastMember));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching cast details", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Get movie and TV credits for a person (filmography)
     */
    public void getPersonCredits(int personId, PersonCreditsCallback callback) {
        executorService.execute(() -> {
            try {
                String url = TMDB_BASE_URL + "/person/" + personId + "/combined_credits?language=en-US";

                Connection.Response response = Jsoup.connect(url)
                        .header("accept", "application/json")
                        .header("Authorization", "Bearer " + TmdbApi.BEARER_TOKEN)
                        .ignoreContentType(true)
                        .timeout(TmdbApi.TIMEOUT_MS)
                        .method(Connection.Method.GET)
                        .execute();

                if (response.statusCode() == 200) {
                    JSONObject jsonResponse = new JSONObject(response.body());
                    JSONArray castArray = jsonResponse.optJSONArray("cast");

                    List<MediaItems> credits = new ArrayList<>();

                    if (castArray != null) {
                        int limit = Math.min(castArray.length(), 40); // Limit to 40 credits
                        for (int i = 0; i < limit; i++) {
                            JSONObject creditJson = castArray.getJSONObject(i);

                            String mediaType = creditJson.optString("media_type", "");
                            if (!mediaType.equals("movie") && !mediaType.equals("tv")) {
                                continue;
                            }

                            MediaItems mediaItem = new MediaItems();

                            int id = creditJson.optInt("id", 0);
                            String title = mediaType.equals("movie") ?
                                    creditJson.optString("title", "Unknown") :
                                    creditJson.optString("name", "Unknown");
                            String overview = creditJson.optString("overview", "No description available");
                            String releaseDate = mediaType.equals("movie") ?
                                    creditJson.optString("release_date", "") :
                                    creditJson.optString("first_air_date", "");

                            mediaItem.setId(String.valueOf(id));
                            mediaItem.setTitle(title);
                            mediaItem.setDescription(overview);
                            mediaItem.setMediaType(mediaType);
                            mediaItem.setTmdbId(String.valueOf(id));
                            mediaItem.setFromTMDB(true);

                            // Parse year
                            if (!releaseDate.isEmpty() && releaseDate.length() >= 4) {
                                try {
                                    mediaItem.setYear(Integer.parseInt(releaseDate.substring(0, 4)));
                                } catch (NumberFormatException e) {
                                    mediaItem.setYear(0);
                                }
                            }

                            // Set poster and backdrop
                            String posterPath = creditJson.optString("poster_path", "");
                            String backdropPath = creditJson.optString("backdrop_path", "");

                            if (!posterPath.isEmpty()) {
                                mediaItem.setPosterUrl(TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + posterPath);
                                mediaItem.setCardImageUrl(TmdbApi.IMAGE_BASE_URL + TmdbApi.POSTER_SIZE + posterPath);
                            }

                            if (!backdropPath.isEmpty()) {
                                mediaItem.setBackgroundImageUrl(TmdbApi.IMAGE_BASE_URL + TmdbApi.BACKDROP_SIZE + backdropPath);
                                mediaItem.setHeroImageUrl(TmdbApi.IMAGE_BASE_URL + "original" + backdropPath);
                            }

                            // Set character name if available
                            String character = creditJson.optString("character", "");
                            if (!character.isEmpty()) {
                                mediaItem.setGenre(character);
                            }

                            credits.add(mediaItem);
                        }
                    }

                    // Sort by release date (newest first)
                    credits.sort((a, b) -> Integer.compare(b.getYear(), a.getYear()));

                    List<MediaItems> finalCredits = credits;
                    mainHandler.post(() -> callback.onSuccess(finalCredits));

                } else {
                    throw new IOException("Failed with status: " + response.statusCode());
                }

            } catch (Exception e) {
                Log.e(TAG, "Error fetching person credits", e);
                mainHandler.post(() -> callback.onError(e.getMessage()));
            }
        });
    }

    /**
     * Parse a cast member from JSON
     */
    private CastMember parseCastMember(JSONObject json) {
        CastMember castMember = new CastMember();
        castMember.setId(json.optInt("id", 0));
        castMember.setName(json.optString("name", "Unknown"));
        castMember.setCharacter(json.optString("character", ""));
        castMember.setProfilePath(json.optString("profile_path", ""));
        castMember.setOrder(json.optInt("order", 0));
        castMember.setKnownForDepartment(json.optString("known_for_department", "Acting"));
        return castMember;
    }
}
