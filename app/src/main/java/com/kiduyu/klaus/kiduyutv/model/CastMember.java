package com.kiduyu.klaus.kiduyutv.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Model class representing a cast member (actor/actress) from TMDB
 */
public class CastMember implements Parcelable {
    private int id;
    private String name;
    private String character;
    private String profilePath;
    private int order;
    private String knownForDepartment;
    private String biography;
    private String birthday;
    private String deathday;
    private String placeOfBirth;
    private float popularity;

    public CastMember() {
    }

    protected CastMember(Parcel in) {
        id = in.readInt();
        name = in.readString();
        character = in.readString();
        profilePath = in.readString();
        order = in.readInt();
        knownForDepartment = in.readString();
        biography = in.readString();
        birthday = in.readString();
        deathday = in.readString();
        placeOfBirth = in.readString();
        popularity = in.readFloat();
    }

    public static final Creator<CastMember> CREATOR = new Creator<CastMember>() {
        @Override
        public CastMember createFromParcel(Parcel in) {
            return new CastMember(in);
        }

        @Override
        public CastMember[] newArray(int size) {
            return new CastMember[size];
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

    public String getCharacter() {
        return character;
    }

    public void setCharacter(String character) {
        this.character = character;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public void setProfilePath(String profilePath) {
        this.profilePath = profilePath;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public String getKnownForDepartment() {
        return knownForDepartment;
    }

    public void setKnownForDepartment(String knownForDepartment) {
        this.knownForDepartment = knownForDepartment;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getDeathday() {
        return deathday;
    }

    public void setDeathday(String deathday) {
        this.deathday = deathday;
    }

    public String getPlaceOfBirth() {
        return placeOfBirth;
    }

    public void setPlaceOfBirth(String placeOfBirth) {
        this.placeOfBirth = placeOfBirth;
    }

    public float getPopularity() {
        return popularity;
    }

    public void setPopularity(float popularity) {
        this.popularity = popularity;
    }

    public String getProfileImageUrl() {
        if (profilePath != null && !profilePath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w300" + profilePath;
        }
        return null;
    }

    public String getProfileImageUrlHighRes() {
        if (profilePath != null && !profilePath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w500" + profilePath;
        }
        return null;
    }

    public int getAge() {
        if (birthday == null || birthday.isEmpty()) {
            return 0;
        }
        try {
            String yearStr = birthday.substring(0, 4);
            int birthYear = Integer.parseInt(yearStr);
            return 2025 - birthYear;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeString(name);
        dest.writeString(character);
        dest.writeString(profilePath);
        dest.writeInt(order);
        dest.writeString(knownForDepartment);
        dest.writeString(biography);
        dest.writeString(birthday);
        dest.writeString(deathday);
        dest.writeString(placeOfBirth);
        dest.writeFloat(popularity);
    }
}
