package com.kiduyu.klaus.kiduyutv.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Season implements Parcelable {
    private int id;
    private String name;
    private String overview;
    private String posterPath;
    private int seasonNumber;
    private int episodeCount;
    private String airDate;

    public Season() {}

    protected Season(Parcel in) {
        id = in.readInt();
        name = in.readString();
        overview = in.readString();
        posterPath = in.readString();
        seasonNumber = in.readInt();
        episodeCount = in.readInt();
        airDate = in.readString();
    }

    public static final Creator<Season> CREATOR = new Creator<Season>() {
        @Override
        public Season createFromParcel(Parcel in) {
            return new Season(in);
        }

        @Override
        public Season[] newArray(int size) {
            return new Season[size];
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

    public String getPosterPath() {
        return posterPath;
    }

    public void setPosterPath(String posterPath) {
        this.posterPath = posterPath;
    }

    public int getSeasonNumber() {
        return seasonNumber;
    }

    public void setSeasonNumber(int seasonNumber) {
        this.seasonNumber = seasonNumber;
    }

    public int getEpisodeCount() {
        return episodeCount;
    }

    public void setEpisodeCount(int episodeCount) {
        this.episodeCount = episodeCount;
    }

    public String getAirDate() {
        return airDate;
    }

    public void setAirDate(String airDate) {
        this.airDate = airDate;
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
        dest.writeString(posterPath);
        dest.writeInt(seasonNumber);
        dest.writeInt(episodeCount);
        dest.writeString(airDate);
    }
}