package com.kiduyu.klaus.kiduyutv.adapter;

import android.content.Context;
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

public class RecommendationsAdapter extends RecyclerView.Adapter<RecommendationsAdapter.ViewHolder> {

    private Context context;
    private List<MediaItems> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(MediaItems item);
    }

    public RecommendationsAdapter(Context context, OnItemClickListener listener) {
        this.context = context;
        this.items = new ArrayList<>();
        this.listener = listener;
    }

    public void setItems(List<MediaItems> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_recommendation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MediaItems item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView posterImageView;
        TextView titleTextView;
        TextView ratingTextView;

        ViewHolder(View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.recommendationPoster);
            titleTextView = itemView.findViewById(R.id.recommendationTitle);
            ratingTextView = itemView.findViewById(R.id.recommendationRating);
        }

        void bind(MediaItems item, OnItemClickListener listener) {
            // Load poster
            Glide.with(itemView.getContext())
                    .load(item.getPosterUrl())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_movie)
                    .into(posterImageView);

            // Set title
            titleTextView.setText(item.getTitle());

            // Set rating
            if (item.getRating() > 0) {
                ratingTextView.setVisibility(View.VISIBLE);
                ratingTextView.setText(String.format("★ %.1f", item.getRating()));
            } else {
                ratingTextView.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                    v.setBackgroundResource(R.drawable.generic_focus_selector);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    v.setBackground(null);
                }
            });
        }
    }
}