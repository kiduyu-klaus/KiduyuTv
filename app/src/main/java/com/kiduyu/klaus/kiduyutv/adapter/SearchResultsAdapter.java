package com.kiduyu.klaus.kiduyutv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for search results display
 */
public class SearchResultsAdapter extends RecyclerView.Adapter<SearchResultsAdapter.SearchResultViewHolder> {
    
    private List<MediaItems> results = new ArrayList<>();
    private OnItemClickListener clickListener;
    
    public interface OnItemClickListener {
        void onItemClick(MediaItems mediaItems, int position);
    }
    
    public SearchResultsAdapter(List<MediaItems> results) {
        this.results = results;
    }
    
    public void updateResults(List<MediaItems> newResults) {
        this.results.clear();
        this.results.addAll(newResults);
        notifyDataSetChanged();
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_search_result, parent, false);
        return new SearchResultViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
        MediaItems item = results.get(position);
        holder.bind(item, position);
    }
    
    @Override
    public int getItemCount() {
        return results.size();
    }
    
    class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private ImageView posterImageView;
        private TextView titleTextView;
        private TextView yearTextView;
        private TextView typeTextView;
        private View itemView;
        
        public SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = itemView;
            posterImageView = itemView.findViewById(R.id.posterImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            yearTextView = itemView.findViewById(R.id.yearTextView);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            
            itemView.setOnClickListener(v -> {
                if (clickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(results.get(getAdapterPosition()), getAdapterPosition());
                }
            });

            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                    v.setBackgroundResource(R.drawable.generic_focus_selector);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    v.setBackground(null);
                }
            });
        }
        
        public void bind(MediaItems item, int position) {
            // Load poster image
            String imageUrl = item.getPrimaryImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(posterImageView);
            } else {
                posterImageView.setImageResource(R.drawable.placeholder_movie);
            }
            
            // Set title
            titleTextView.setText(item.getTitle());
            
            // Set year
            if (item.getYear() > 0) {
                yearTextView.setText(String.valueOf(item.getYear()));
                yearTextView.setVisibility(View.VISIBLE);
            } else {
                yearTextView.setVisibility(View.GONE);
            }
            
            // Set type badge
            String type = "movie".equals(item.getMediaType()) ? "Movie" : "TV Show";
            typeTextView.setText(type);
            
            // Set content type specific styling
            if ("movie".equals(item.getMediaType())) {
                typeTextView.setBackgroundResource(R.drawable.badge_movie);
            } else {
                typeTextView.setBackgroundResource(R.drawable.badge_tv);
            }
        }
    }
}