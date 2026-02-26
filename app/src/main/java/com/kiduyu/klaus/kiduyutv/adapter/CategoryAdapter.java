package com.kiduyu.klaus.kiduyutv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.CategorySection;
import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategorySection> categories;
    private OnItemClickListener listener;
    private OnCompanyNetworkClickListener companyNetworkListener;

    public interface OnItemClickListener {
        void onItemClick(MediaItems mediaItems, int categoryPosition, int itemPosition);
        void onItemFocusChanged(MediaItems mediaItems, int categoryPosition, int itemPosition, boolean hasFocus);
    }

    public interface OnCompanyNetworkClickListener {
        void onCompanyNetworkClick(CompanyNetwork companyNetwork, int categoryPosition, int itemPosition);
    }

    public CategoryAdapter(List<CategorySection> categories) {
        this.categories = categories;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnCompanyNetworkClickListener(OnCompanyNetworkClickListener listener) {
        this.companyNetworkListener = listener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_row, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        CategorySection category = categories.get(position);
        holder.bind(category, position);
    }

    @Override
    public int getItemCount() {
        return categories != null ? categories.size() : 0;
    }

    public class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryTitle;
        public RecyclerView itemsRecyclerView;
        private ContentCarouselAdapter contentAdapter;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTitle = itemView.findViewById(R.id.categoryTitle);
            itemsRecyclerView = itemView.findViewById(R.id.itemsRecyclerView);

            // Setup horizontal RecyclerView with focus handling for nested navigation
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
            );
            itemsRecyclerView.setLayoutManager(layoutManager);
            itemsRecyclerView.setHasFixedSize(true);

            // Important: Disable focusability on the RecyclerView itself
            // so focus goes directly to child items
            itemsRecyclerView.setFocusable(false);
            itemsRecyclerView.setClickable(false);

            // Add focus listener to handle vertical navigation between categories
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    // Animate category title when focused
                    categoryTitle.animate()
                            .scaleX(1.05f)
                            .scaleY(1.05f)
                            .setDuration(200)
                            .start();
                } else {
                    categoryTitle.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(200)
                            .start();
                }
            });
        }

        public void bind(CategorySection category, int categoryPosition) {
            categoryTitle.setText(category.getCategoryName());

            // Check if this is a company/network section
            if (category.getSectionType() == CategorySection.TYPE_COMPANY_NETWORK) {
                // Setup company network adapter
                CompanyNetworkAdapter companyAdapter = new CompanyNetworkAdapter(category.getCompanyNetworks());
                itemsRecyclerView.setAdapter(companyAdapter);

                // Set click listener
                companyAdapter.setOnItemClickListener(new CompanyNetworkAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(CompanyNetwork companyNetwork, int position) {
                        if (companyNetworkListener != null) {
                            companyNetworkListener.onCompanyNetworkClick(companyNetwork, categoryPosition, position);
                        }
                    }
                });
            } else {
                // Setup content adapter for this category (media items)
                contentAdapter = new ContentCarouselAdapter(category.getItems());
                itemsRecyclerView.setAdapter(contentAdapter);

                // Set click listener
                contentAdapter.setOnItemClickListener(new ContentCarouselAdapter.OnItemClickListener() {
                    @Override
                    public void onItemClick(MediaItems mediaItems, int position) {
                        if (listener != null) {
                            listener.onItemClick(mediaItems, categoryPosition, position);
                        }
                    }

                    @Override
                    public void onFocusChanged(MediaItems mediaItems, int position, boolean hasFocus) {
                        if (listener != null) {
                            listener.onItemFocusChanged(mediaItems, categoryPosition, position, hasFocus);
                        }
                    }
                });
            }
        }
    }
}