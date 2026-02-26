package com.kiduyu.klaus.kiduyutv.adapter;

/**
 * Created by Kiduyu Klaus on 2/23/2026.
 */

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.Ui.details.CompanyContentActivity;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.ArrayList;
import java.util.List;

public class CompanyContentAdapter extends RecyclerView.Adapter<CompanyContentAdapter.ViewHolder> {

    /**
     * Interface for handling media item clicks
     */
    public interface OnMediaItemClickListener {
        void onMediaItemClick(MediaItems mediaItem);
    }

    /**
     * Interface for handling media item focus changes (Android TV D-pad navigation)
     */
    public interface OnMediaItemFocusChangeListener {
        void onMediaItemFocusChanged(MediaItems mediaItem, int position, boolean hasFocus);
    }

    private final OnMediaItemClickListener listener;
    private OnMediaItemFocusChangeListener focusChangeListener;
    private List<MediaItems> items = new ArrayList<>();

    public CompanyContentAdapter(OnMediaItemClickListener listener) {
        this.listener = listener;
    }

    public void setOnFocusChangeListener(OnMediaItemFocusChangeListener focusChangeListener) {
        this.focusChangeListener = focusChangeListener;
    }

    public void setItems(List<MediaItems> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void addItems(List<MediaItems> newItems) {
        int startPosition = items.size();
        items.addAll(newItems);
        notifyItemRangeInserted(startPosition, newItems.size());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_grid_media, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItems item = items.get(position);
        holder.bind(item, position, listener, focusChangeListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        private final TextView ratingTextView;
        private final TextView yearTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.posterImageView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            ratingTextView = itemView.findViewById(R.id.ratingTextView);
            yearTextView = itemView.findViewById(R.id.yearTextView);
        }

        void bind(MediaItems item, int position, OnMediaItemClickListener listener, OnMediaItemFocusChangeListener focusChangeListener) {
            titleTextView.setText(item.getTitle());

            // Set rating
            if (item.getRating() > 0) {
                ratingTextView.setText(String.format("%.1f", item.getRating()));
                ratingTextView.setVisibility(View.VISIBLE);
            } else {
                ratingTextView.setVisibility(View.GONE);
            }

            // Set year
            if (item.getYear() > 0) {
                yearTextView.setText(String.valueOf(item.getYear()));
                yearTextView.setVisibility(View.VISIBLE);
            } else {
                yearTextView.setVisibility(View.GONE);
            }

            // Load poster
            if (item.getPosterUrl() != null && !item.getPosterUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getPosterUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .into(posterImageView);
            } else if (item.getCardImageUrl() != null && !item.getCardImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(item.getCardImageUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .into(posterImageView);
            }

            // Set click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMediaItemClick(item);
                }
            });

            // Focus listener for Android TV D-pad navigation
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (focusChangeListener != null) {
                    focusChangeListener.onMediaItemFocusChanged(item, position, hasFocus);
                }
                // Scale + elevation animation for focused card feedback
                float scale = hasFocus ? 1.10f : 1.0f;
                float elevation = hasFocus ? 12f : 2f;
                v.animate()
                        .scaleX(scale)
                        .scaleY(scale)
                        .setDuration(150)
                        .start();
                v.setElevation(elevation);
            });

            // Make focusable for TV remote navigation
            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(false);
        }
    }
}