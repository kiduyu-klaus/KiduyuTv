package com.kiduyu.klaus.kiduyutv.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class AnimeModel {

    /* =======================
       Default / Core Info
       ======================= */

    private String animeName;
    private String animeDescription;

    private String data_tip;

    private String anime_link;

    private String anime_image_backgroud;

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

