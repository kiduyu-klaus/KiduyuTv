package com.kiduyu.klaus.kiduyutv.model;

import java.util.List;

public class CategorySection {
    private String categoryName;
    private List<MediaItems> items;

    public CategorySection(String categoryName, List<MediaItems> items) {
        this.categoryName = categoryName;
        this.items = items;
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

    public int getItemCount() {
        return items != null ? items.size() : 0;
    }
}