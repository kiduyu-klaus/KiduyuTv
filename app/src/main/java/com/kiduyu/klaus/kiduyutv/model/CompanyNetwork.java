package com.kiduyu.klaus.kiduyutv.model;

/**
 * Model class representing a Production Company or TV Network
 * from TMDB
 */
public class CompanyNetwork {
    private int id;
    private String name;
    private String logoPath;
    private String originCountry;
    private boolean isNetwork; // true = TV Network, false = Production Company

    public CompanyNetwork() {
    }

    public CompanyNetwork(int id, String name, String logoPath, String originCountry, boolean isNetwork) {
        this.id = id;
        this.name = name;
        this.logoPath = logoPath;
        this.originCountry = originCountry;
        this.isNetwork = isNetwork;
    }

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

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public void setOriginCountry(String originCountry) {
        this.originCountry = originCountry;
    }

    public boolean isNetwork() {
        return isNetwork;
    }

    public void setNetwork(boolean network) {
        isNetwork = network;
    }

    /**
     * Get the full logo URL for TMDB images
     */
    public String getLogoUrl() {
        if (logoPath != null && !logoPath.isEmpty()) {
            return "https://image.tmdb.org/t/p/w300" + logoPath;
        }
        return null;
    }
}
