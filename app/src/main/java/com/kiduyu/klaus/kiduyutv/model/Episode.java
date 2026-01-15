package com.kiduyu.klaus.kiduyutv.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Episode implements Parcelable {
    private int id;
    private String name;
    private String overview;
    private String stillPath;
    private int episodeNumber;
    private int seasonNumber;
    private String airDate;
    private double voteAverage;
    private int runtime;

    public Episode() {}

    protected Episode(Parcel in) {
        id = in.readInt();
        name = in.readString();
        overview = in.readString();
        stillPath = in.readString();
        episodeNumber = in.readInt();
        seasonNumber = in.readInt();
        airDate = in.readString();
        voteAverage = in.readDouble();
        runtime = in.readInt();
    }

    public static final Creator<Episode> CREATOR = new Creator<Episode>() {
        @Override
        public Episode createFromParcel(Parcel in) {
            return new Episode(in);
        }

        @Override
        public Episode[] newArray(int size) {
            return new Episode[size];
        }
    };

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOverview() {
        return overview;
    }

    public void setOverview(String overview) {
        this.overview = overview;
    }

    public String getStillPath() {
        return stillPath;
    }

    public void setStillPath(String stillPath) {
        this.stillPath = stillPath;
    }

    public int getEpisodeNumber() {
        return episodeNumber;
    }

    public void setEpisodeNumber(int episodeNumber) {
        this.episodeNumber = episodeNumber;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
    }

    public double getVoteAverage() {
        return voteAverage;
    }

    public void setVoteAverage(double voteAverage) {
        this.voteAverage = voteAverage;
    }

    public int getRuntime() {
        return runtime;
    }

    public void setRuntime(int runtime) {
        this.runtime = runtime;
    }

    public String getEpisodeTitle() {
        return "Chapter " + (episodeNumber < 10 ? "0" + episodeNumber : episodeNumber) + ": " + name + " - S" + seasonNumber + ",E" + episodeNumber;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(overview);
        dest.writeString(stillPath);
        dest.writeInt(episodeNumber);
        dest.writeInt(seasonNumber);
        dest.writeString(airDate);
        dest.writeDouble(voteAverage);
        dest.writeInt(runtime);
    }
}