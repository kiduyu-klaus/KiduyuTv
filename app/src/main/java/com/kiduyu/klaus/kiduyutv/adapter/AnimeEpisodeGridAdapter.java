package com.kiduyu.klaus.kiduyutv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.EpisodeModel;

import java.util.List;

/**
 * Adapter for displaying anime episodes in a grid layout
 */
public class AnimeEpisodeGridAdapter extends RecyclerView.Adapter<AnimeEpisodeGridAdapter.EpisodeViewHolder> {

    private List<EpisodeModel> episodes;
    private OnEpisodeClickListener listener;

    public interface OnEpisodeClickListener {
        void onEpisodeClick(EpisodeModel episode, int position);
    }

    public AnimeEpisodeGridAdapter(List<EpisodeModel> episodes) {
        this.episodes = episodes;
    }

    public void setOnEpisodeClickListener(OnEpisodeClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_anime_episode, parent, false);
        return new EpisodeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        EpisodeModel episode = episodes.get(position);
        holder.bind(episode, position);
    }

    @Override
    public int getItemCount() {
        return episodes != null ? episodes.size() : 0;
    }

    class EpisodeViewHolder extends RecyclerView.ViewHolder {
        private TextView episodeNumberText;
        private TextView episodeNameText;
        private ImageView playIcon;

        public EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            episodeNumberText = itemView.findViewById(R.id.episodeNumberText);
            episodeNameText = itemView.findViewById(R.id.episodeNameText);
            playIcon = itemView.findViewById(R.id.playIcon);
        }

        public void bind(EpisodeModel episode, int position) {
            // Episode number
            episodeNumberText.setText("Episode " + episode.getEpisodeNumber());

            // Episode name
            if (episode.getEpisodeName() != null && !episode.getEpisodeName().isEmpty()) {
                episodeNameText.setText(episode.getEpisodeName());
                episodeNameText.setVisibility(View.VISIBLE);
            } else {
                episodeNameText.setVisibility(View.GONE);
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onEpisodeClick(episode, position);
                }
            });

            // Focus listener for TV remote
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                    playIcon.setVisibility(View.VISIBLE);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    playIcon.setVisibility(View.GONE);
                }
            });
        }
    }
}