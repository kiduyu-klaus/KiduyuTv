package com.kiduyu.klaus.kiduyutv.model;

import java.util.List;

import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;

public class CategorySection {
    public static final int TYPE_MEDIA = 0;
    public static final int TYPE_COMPANY_NETWORK = 1;

    private String categoryName;
    private List<MediaItems> items;
    private List<CompanyNetwork> companyNetworks;
    private int sectionType = TYPE_MEDIA;

    public CategorySection(String categoryName, List<MediaItems> items) {
        this.categoryName = categoryName;
        this.items = items;
        this.sectionType = TYPE_MEDIA;
    }

    public CategorySection(String categoryName, List<CompanyNetwork> companyNetworks, int sectionType) {
        this.categoryName = categoryName;
        this.companyNetworks = companyNetworks;
        this.sectionType = sectionType;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public List<MediaItems> getItems() {
        return items;
    }

    public void setItems(List<MediaItems> items) {
        this.items = items;
    }

    public List<CompanyNetwork> getCompanyNetworks() {
        return companyNetworks;
    }

    public void setCompanyNetworks(List<CompanyNetwork> companyNetworks) {
        this.companyNetworks = companyNetworks;
    }

    public int getSectionType() {
        return sectionType;
    }

    public void setSectionType(int sectionType) {
        this.sectionType = sectionType;
    }

    public int getItemCount() {
        if (sectionType == TYPE_COMPANY_NETWORK) {
            return companyNetworks != null ? companyNetworks.size() : 0;
        }
        return items != null ? items.size() : 0;
    }
}