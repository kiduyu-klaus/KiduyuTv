package com.kiduyu.klaus.kiduyutv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.MediaItems;

import java.util.List;

public class ContentCarouselAdapter extends RecyclerView.Adapter<ContentCarouselAdapter.ContentViewHolder> {

    private List<MediaItems> contentList;
    private OnItemClickListener onItemClickListener;
    private int selectedPosition = 0;

    public interface OnItemClickListener {
        void onItemClick(MediaItems mediaItems, int position);
        void onFocusChanged(MediaItems mediaItems, int position, boolean hasFocus);
    }

    public ContentCarouselAdapter(List<MediaItems> contentList) {
        this.contentList = contentList;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public ContentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_content_card, parent, false);
        return new ContentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContentViewHolder holder, int position) {
        MediaItems mediaItems = contentList.get(position);
        holder.bind(mediaItems, position);
    }

    @Override
    public int getItemCount() {
        return contentList != null ? contentList.size() : 0;
    }

    public class ContentViewHolder extends RecyclerView.ViewHolder {
        private ImageView contentImage;
        private View focusOverlay;

        public ContentViewHolder(@NonNull View itemView) {
            super(itemView);
            contentImage = itemView.findViewById(R.id.contentImage);
            focusOverlay = itemView.findViewById(R.id.focusOverlay);


            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (onItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClickListener.onFocusChanged(contentList.get(getAdapterPosition()), getAdapterPosition(), hasFocus);
                }
                updateFocusState(hasFocus);
            });

            itemView.setOnClickListener(v -> {
                if (onItemClickListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    onItemClickListener.onItemClick(contentList.get(getAdapterPosition()), getAdapterPosition());
                }
            });
        }

        public void bind(MediaItems mediaItems, int position) {
            Glide.with(itemView.getContext())
                    .load(mediaItems.getPosterUrl())
                    .centerCrop()
                    .into(contentImage);

            updateFocusState(position == selectedPosition);
        }

        private void updateFocusState(boolean hasFocus) {
            if (hasFocus) {
                focusOverlay.setVisibility(View.VISIBLE);
                itemView.animate()
                        .scaleX(1.05f)
                        .scaleY(1.05f)
                        .setDuration(200)
                        .start();
            } else {
                focusOverlay.setVisibility(View.GONE);
                itemView.animate()
                        .scaleX(1.0f)
                        .scaleY(1.0f)
                        .setDuration(200)
                        .start();
            }
        }
    }
}