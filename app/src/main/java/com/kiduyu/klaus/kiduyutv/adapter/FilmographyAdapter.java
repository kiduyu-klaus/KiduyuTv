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
 * Adapter for displaying an actor/actress's filmography (movies and TV shows)
 */
public class FilmographyAdapter extends RecyclerView.Adapter<FilmographyAdapter.FilmographyViewHolder> {

    private List<MediaItems> creditsList;
    private OnCreditClickListener listener;

    public interface OnCreditClickListener {
        void onCreditClick(MediaItems mediaItem, int position);
    }

    public FilmographyAdapter() {
        this.creditsList = new ArrayList<>();
    }

    public FilmographyAdapter(List<MediaItems> creditsList) {
        this.creditsList = creditsList != null ? creditsList : new ArrayList<>();
    }

    public void setCreditsList(List<MediaItems> creditsList) {
        this.creditsList = creditsList != null ? creditsList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnCreditClickListener(OnCreditClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public FilmographyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_filmography, parent, false);
        return new FilmographyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilmographyViewHolder holder, int position) {
        MediaItems mediaItem = creditsList.get(position);
        holder.bind(mediaItem);
    }

    @Override
    public int getItemCount() {
        return creditsList != null ? creditsList.size() : 0;
    }

    class FilmographyViewHolder extends RecyclerView.ViewHolder {
        private ImageView posterImage;
        private TextView titleText;
        private TextView yearText;
        private TextView mediaTypeBadge;

        public FilmographyViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImage = itemView.findViewById(R.id.posterImage);
            titleText = itemView.findViewById(R.id.titleText);
            yearText = itemView.findViewById(R.id.yearText);
            mediaTypeBadge = itemView.findViewById(R.id.mediaTypeBadge);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCreditClick(creditsList.get(position), position);
                }
            });

            // Focus change listener for TV remote navigation
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                }
            });
        }

        public void bind(MediaItems mediaItem) {
            titleText.setText(mediaItem.getTitle());

            // Show year
            if (mediaItem.getYear() > 0) {
                yearText.setText(String.valueOf(mediaItem.getYear()));
                yearText.setVisibility(View.VISIBLE);
            } else {
                yearText.setVisibility(View.GONE);
            }

            // Show media type badge
            String mediaType = mediaItem.getMediaType();
            if (mediaType != null) {
                if (mediaType.equals("movie")) {
                    mediaTypeBadge.setText("Movie");
                    mediaTypeBadge.setVisibility(View.VISIBLE);
                } else if (mediaType.equals("tv")) {
                    mediaTypeBadge.setText("TV");
                    mediaTypeBadge.setVisibility(View.VISIBLE);
                } else {
                    mediaTypeBadge.setVisibility(View.GONE);
                }
            } else {
                mediaTypeBadge.setVisibility(View.GONE);
            }

            // Load poster image
            String imageUrl = mediaItem.getPosterUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .into(posterImage);
            } else {
                posterImage.setImageResource(R.drawable.placeholder_movie);
            }
        }
    }
}
