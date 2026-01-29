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
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<CategorySection> categories;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MediaItems mediaItems, int categoryPosition, int itemPosition);
        void onItemFocusChanged(MediaItems mediaItems, int categoryPosition, int itemPosition, boolean hasFocus);
    }

    public CategoryAdapter(List<CategorySection> categories) {
        this.categories = categories;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
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

            // Setup horizontal RecyclerView
            LinearLayoutManager layoutManager = new LinearLayoutManager(
                    itemView.getContext(),
                    LinearLayoutManager.HORIZONTAL,
                    false
            );
            itemsRecyclerView.setLayoutManager(layoutManager);
            itemsRecyclerView.setHasFixedSize(true);
        }

        public void bind(CategorySection category, int categoryPosition) {
            categoryTitle.setText(category.getCategoryName());

            // Setup content adapter for this category
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