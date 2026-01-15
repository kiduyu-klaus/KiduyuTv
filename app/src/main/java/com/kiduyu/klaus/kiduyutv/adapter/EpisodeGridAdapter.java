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
import com.kiduyu.klaus.kiduyutv.model.Episode;

import java.util.List;

public class EpisodeGridAdapter extends RecyclerView.Adapter<EpisodeGridAdapter.EpisodeGridViewHolder> {

    private List<Episode> episodes;
    private OnEpisodeClickListener listener;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(Episode episode);
    }

    public EpisodeGridAdapter(List<Episode> episodes) {
        this.episodes = episodes;
    }

    public void setOnEpisodeClickListener(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EpisodeGridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode_grid, parent, false);
        return new EpisodeGridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeGridViewHolder holder, int position) {
        Episode episode = episodes.get(position);
        holder.bind(episode);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    class EpisodeGridViewHolder extends RecyclerView.ViewHolder {
        ImageView episodeThumbnail;
        TextView episodeTitle;
        TextView ratingBadge;
        TextView newEpisodeBadge;

        EpisodeGridViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeThumbnail = itemView.findViewById(R.id.episodeThumbnail);
            episodeTitle = itemView.findViewById(R.id.episodeTitle);
            ratingBadge = itemView.findViewById(R.id.ratingBadge);
            newEpisodeBadge = itemView.findViewById(R.id.newEpisodeBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEpisodeClick(episodes.get(position));
                }
            });

            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);

            // Focus change listener for scale animation
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).start();
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                }
            });
        }

        void bind(Episode episode) {
            // Episode title format: "E1: Chapter One: The Hellfire Club"
            String title = "E" + episode.getEpisodeNumber() + ": " + episode.getName();
            episodeTitle.setText(title);

            // Load thumbnail
            if (episode.getStillPath() != null && !episode.getStillPath().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(episode.getStillPath())
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_episode)
                        .into(episodeThumbnail);
            }

            // Rating badge
            if (episode.getVoteAverage() > 0) {
                ratingBadge.setText(String.format("⭐ %.1f", episode.getVoteAverage()));
                ratingBadge.setVisibility(View.VISIBLE);
            } else {
                ratingBadge.setVisibility(View.GONE);
            }

            // Show "New Episode" badge for recent episodes (placeholder logic)
            // You can implement actual date checking here
            if (episode.getEpisodeNumber() <= 2) {
                newEpisodeBadge.setVisibility(View.VISIBLE);
            } else {
                newEpisodeBadge.setVisibility(View.GONE);
            }
        }
    }
}