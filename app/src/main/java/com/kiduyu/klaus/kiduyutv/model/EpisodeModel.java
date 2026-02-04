package com.kiduyu.klaus.kiduyutv.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class EpisodeModel implements Parcelable {

    /* =======================
       Core Episode Info
       ======================= */
    private int season;
    private int episodeNumber;
    private String episodeName;
    private String episodeToken;

    /* =======================
       Video / Media Info
       ======================= */
    private String embedUrl; // decrypted embed url
    private List<String> sources; // list of m3u8 or stream URLs

    private List<Track> tracks; // subtitle / captions tracks
    private String downloadLink; // direct download

    private SkipInfo skip; // intro/outro skip info in seconds

    /* =======================
       Constructors
       ======================= */
    public EpisodeModel() {
    }

    public EpisodeModel(int season, int episodeNumber, String episodeName, String episodeToken) {
        this.season = season;
        this.episodeNumber = episodeNumber;
        this.episodeName = episodeName;
        this.episodeToken = episodeToken;
    }

    /* =======================
       Parcelable Implementation
       ======================= */

    protected EpisodeModel(Parcel in) {
        season = in.readInt();
        episodeNumber = in.readInt();
        episodeName = in.readString();
        episodeToken = in.readString();
        embedUrl = in.readString();
        sources = in.createStringArrayList();
        tracks = in.createTypedArrayList(Track.CREATOR);
        downloadLink = in.readString();
        skip = in.readParcelable(SkipInfo.class.getClassLoader());
    }

    public static final Creator<EpisodeModel> CREATOR = new Creator<EpisodeModel>() {
        @Override
        public EpisodeModel createFromParcel(Parcel in) {
            return new EpisodeModel(in);
        }

        @Override
        public EpisodeModel[] newArray(int size) {
            return new EpisodeModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(season);
        dest.writeInt(episodeNumber);
        dest.writeString(episodeName);
        dest.writeString(episodeToken);
        dest.writeString(embedUrl);
        dest.writeStringList(sources);
        dest.writeTypedList(tracks);
        dest.writeString(downloadLink);
        dest.writeParcelable(skip, flags);
    }

    /* =======================
       Getters & Setters
       ======================= */
    public int getSeason() { return season; }
    public void setSeason(int season) { this.season = season; }

    public int getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(int episodeNumber) { this.episodeNumber = episodeNumber; }

    public String getEpisodeName() { return episodeName; }
    public void setEpisodeName(String episodeName) { this.episodeName = episodeName; }

    public String getEpisodeToken() { return episodeToken; }
    public void setEpisodeToken(String episodeToken) { this.episodeToken = episodeToken; }

    public String getEmbedUrl() { return embedUrl; }
    public void setEmbedUrl(String embedUrl) { this.embedUrl = embedUrl; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public List<Track> getTracks() { return tracks; }
    public void setTracks(List<Track> tracks) { this.tracks = tracks; }

    public String getDownloadLink() { return downloadLink; }
    public void setDownloadLink(String downloadLink) { this.downloadLink = downloadLink; }

    public SkipInfo getSkip() { return skip; }
    public void setSkip(SkipInfo skip) { this.skip = skip; }

    /* =======================
       Nested Classes
       ======================= */
    public static class Track implements Parcelable {
        private String file;
        private String label;
        private String kind;
        private boolean isDefault;

        public Track() {
        }

        public Track(String file, String label, String kind, boolean isDefault) {
            this.file = file;
            this.label = label;
            this.kind = kind;
            this.isDefault = isDefault;
        }

        protected Track(Parcel in) {
            file = in.readString();
            label = in.readString();
            kind = in.readString();
            isDefault = in.readByte() != 0;
        }

        public static final Creator<Track> CREATOR = new Creator<Track>() {
            @Override
            public Track createFromParcel(Parcel in) {
                return new Track(in);
            }

            @Override
            public Track[] newArray(int size) {
                return new Track[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(file);
            dest.writeString(label);
            dest.writeString(kind);
            dest.writeByte((byte) (isDefault ? 1 : 0));
        }

        public String getFile() { return file; }
        public void setFile(String file) { this.file = file; }

        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public String getKind() { return kind; }
        public void setKind(String kind) { this.kind = kind; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }

    public static class SkipInfo implements Parcelable {
        private int[] intro; // [startSec, endSec]
        private int[] outro; // [startSec, endSec]

        public SkipInfo() {
        }

        public SkipInfo(int[] intro, int[] outro) {
            this.intro = intro;
            this.outro = outro;
        }

        protected SkipInfo(Parcel in) {
            intro = in.createIntArray();
            outro = in.createIntArray();
        }

        public static final Creator<SkipInfo> CREATOR = new Creator<SkipInfo>() {
            @Override
            public SkipInfo createFromParcel(Parcel in) {
                return new SkipInfo(in);
            }

            @Override
            public SkipInfo[] newArray(int size) {
                return new SkipInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeIntArray(intro);
            dest.writeIntArray(outro);
        }

        public int[] getIntro() { return intro; }
        public void setIntro(int[] intro) { this.intro = intro; }

        public int[] getOutro() { return outro; }
        public void setOutro(int[] outro) { this.outro = outro; }
    }
}
