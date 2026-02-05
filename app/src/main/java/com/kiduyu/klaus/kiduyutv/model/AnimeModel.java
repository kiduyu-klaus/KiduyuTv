package com.kiduyu.klaus.kiduyutv.model;

import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.RequiresApi;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimeModel implements Parcelable {

    /* =======================
       Default / Core Info
       ======================= */

    private String animeName;
    private String animeDescription;

    private String data_tip;

    private String anime_link;

    private String anime_image_backgroud;



    /**
     * Episodes grouped by season
     * Season -> (Episode Number -> EpisodeModel)
     */
    private Map<Integer, Map<Integer, EpisodeModel>> episodes;

    /* =======================
       Metadata
       ======================= */

    private String country;
    private List<String> genres;
    private String premiered;
    private LocalDate dateAiredFrom;
    private LocalDate dateAiredTo;
    private String broadcast;
    private int totalEpisodes;
    private String duration;

    /* =======================
       Status & Ratings
       ======================= */

    private String status;
    private double malScore;
    private int malUserCount;

    /* =======================
       Production
       ======================= */

    private String studio;


    private List<String> producers;

    /* =======================
       External Links
       ======================= */

    private Map<String, String> links;

    /* =======================
       Constructors
       ======================= */

    public AnimeModel() {
    }

    public AnimeModel(
            String animeName,
            String data_tip,
            String anime_link,
            String anime_image_backgroud
    ) {
        this.animeName = animeName;
        this.data_tip = data_tip;
        this.anime_link = anime_link;
        this.anime_image_backgroud = anime_image_backgroud;
    }

    /* =======================
       Parcelable Implementation
       ======================= */

    @RequiresApi(api = Build.VERSION_CODES.O)
    protected AnimeModel(Parcel in) {
        animeName = in.readString();
        animeDescription = in.readString();
        data_tip = in.readString();
        anime_link = in.readString();
        anime_image_backgroud = in.readString();

        // Read episodes map
        int episodesSize = in.readInt();
        episodes = new HashMap<>();
        for (int i = 0; i < episodesSize; i++) {
            int seasonKey = in.readInt();
            int innerMapSize = in.readInt();
            Map<Integer, EpisodeModel> innerMap = new HashMap<>();
            for (int j = 0; j < innerMapSize; j++) {
                int episodeKey = in.readInt();
                EpisodeModel episode = in.readParcelable(EpisodeModel.class.getClassLoader());
                innerMap.put(episodeKey, episode);
            }
            episodes.put(seasonKey, innerMap);
        }

        country = in.readString();
        genres = in.createStringArrayList();
        premiered = in.readString();
        dateAiredFrom = (LocalDate) in.readValue(LocalDate.class.getClassLoader());
        dateAiredTo = (LocalDate) in.readValue(LocalDate.class.getClassLoader());
        broadcast = in.readString();
        totalEpisodes = in.readInt();
        duration = in.readString();
        status = in.readString();
        malScore = in.readDouble();
        malUserCount = in.readInt();
        studio = in.readString();
        producers = in.createStringArrayList();
        links = new HashMap<>();
        int linksSize = in.readInt();
        for (int i = 0; i < linksSize; i++) {
            String key = in.readString();
            String value = in.readString();
            links.put(key, value);
        }
    }

    public static final Creator<AnimeModel> CREATOR = new Creator<AnimeModel>() {
        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public AnimeModel createFromParcel(Parcel in) {
            return new AnimeModel(in);
        }

        @Override
        public AnimeModel[] newArray(int size) {
            return new AnimeModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }
    public String getData_tip() {
        return data_tip;
    }

    public void setData_tip(String data_tip) {
        this.data_tip = data_tip;
    }

    public String getAnime_link() {
        return anime_link;
    }

    public void setAnime_link(String anime_link) {
        this.anime_link = anime_link;
    }

    public String getAnime_image_backgroud() {
        return anime_image_backgroud;
    }

    public void setAnime_image_backgroud(String anime_image_backgroud) {
        this.anime_image_backgroud = anime_image_backgroud;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(animeName);
        dest.writeString(animeDescription);
        dest.writeString(data_tip);
        dest.writeString(anime_link);
        dest.writeString(anime_image_backgroud);

        // Write episodes map
        if (episodes == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(episodes.size());
            for (Map.Entry<Integer, Map<Integer, EpisodeModel>> entry : episodes.entrySet()) {
                dest.writeInt(entry.getKey());
                if (entry.getValue() == null) {
                    dest.writeInt(0);
                } else {
                    dest.writeInt(entry.getValue().size());
                    for (Map.Entry<Integer, EpisodeModel> innerEntry : entry.getValue().entrySet()) {
                        dest.writeInt(innerEntry.getKey());
                        dest.writeParcelable(innerEntry.getValue(), flags);
                    }
                }
            }
        }

        dest.writeString(country);
        dest.writeStringList(genres);
        dest.writeString(premiered);
        dest.writeValue(dateAiredFrom);
        dest.writeValue(dateAiredTo);
        dest.writeString(broadcast);
        dest.writeInt(totalEpisodes);
        dest.writeString(duration);
        dest.writeString(status);
        dest.writeDouble(malScore);
        dest.writeInt(malUserCount);
        dest.writeString(studio);
        dest.writeStringList(producers);

        // Write links map
        if (links == null) {
            dest.writeInt(0);
        } else {
            dest.writeInt(links.size());
            for (Map.Entry<String, String> entry : links.entrySet()) {
                dest.writeString(entry.getKey());
                dest.writeString(entry.getValue());
            }
        }
    }

    /* =======================
       Getters & Setters
       ======================= */

    public String getAnimeName() {
        return animeName;
    }

    public void setAnimeName(String animeName) {
        this.animeName = animeName;
    }

    public String getAnimeDescription() {
        return animeDescription;
    }

    public void setAnimeDescription(String animeDescription) {
        this.animeDescription = animeDescription;
    }

    public Map<Integer, Map<Integer, EpisodeModel>> getEpisodes() {
        return episodes;
    }

    public void setEpisodes(Map<Integer, Map<Integer, EpisodeModel>> episodes) {
        this.episodes = episodes;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public String getPremiered() {
        return premiered;
    }

    public void setPremiered(String premiered) {
        this.premiered = premiered;
    }

    public LocalDate getDateAiredFrom() {
        return dateAiredFrom;
    }

    public void setDateAiredFrom(LocalDate dateAiredFrom) {
        this.dateAiredFrom = dateAiredFrom;
    }

    public LocalDate getDateAiredTo() {
        return dateAiredTo;
    }

    public void setDateAiredTo(LocalDate dateAiredTo) {
        this.dateAiredTo = dateAiredTo;
    }

    public String getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(String broadcast) {
        this.broadcast = broadcast;
    }

    public int getTotalEpisodes() {
        return totalEpisodes;
    }

    public void setTotalEpisodes(int totalEpisodes) {
        this.totalEpisodes = totalEpisodes;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getMalScore() {
        return malScore;
    }

    public void setMalScore(double malScore) {
        this.malScore = malScore;
    }

    public int getMalUserCount() {
        return malUserCount;
    }

    public void setMalUserCount(int malUserCount) {
        this.malUserCount = malUserCount;
    }

    public String getStudio() {
        return studio;
    }

    public void setStudio(String studio) {
        this.studio = studio;
    }

    public List<String> getProducers() {
        return producers;
    }

    public void setProducers(List<String> producers) {
        this.producers = producers;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }
}
