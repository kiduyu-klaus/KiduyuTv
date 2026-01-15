package com.kiduyu.klaus.kiduyutv.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.kiduyu.klaus.kiduyutv.R;
import com.kiduyu.klaus.kiduyutv.model.Season;

import java.util.List;

public class SeasonTabAdapter extends RecyclerView.Adapter<SeasonTabAdapter.SeasonTabViewHolder> {

    private List<Season> seasons;
    private OnSeasonClickListener listener;
    private int selectedPosition = 0;

    public interface OnSeasonClickListener {
        void onSeasonClick(Season season, int position);
    }

    public SeasonTabAdapter(List<Season> seasons) {
        this.seasons = seasons;
    }

    public void setOnSeasonClickListener(OnSeasonClickListener listener) {
        this.listener = listener;
    }

    public void setSelectedPosition(int position) {
        int previousPosition = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(previousPosition);
        notifyItemChanged(selectedPosition);
    }

    @NonNull
    @Override
    public SeasonTabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_season_tab, parent, false);
        return new SeasonTabViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SeasonTabViewHolder holder, int position) {
        Season season = seasons.get(position);
        holder.bind(season, position == selectedPosition);
    }


    @Override
    public int getItemCount() {
        return seasons.size();
    }

    class SeasonTabViewHolder extends RecyclerView.ViewHolder {
        TextView seasonName;
        View container;

        SeasonTabViewHolder(@NonNull View itemView) {
            super(itemView);
            seasonName = itemView.findViewById(R.id.seasonName);
            container = itemView.findViewById(R.id.seasonTabContainer);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onSeasonClick(seasons.get(position), position);
                }
            });

            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    itemView.setBackgroundColor(itemView.getContext().getColor(android.R.color.holo_red_light));
                } else {
                    itemView.setBackground(itemView.getContext().getDrawable(R.drawable.season_tab_normal));
                    seasonName.setTextColor(itemView.getContext().getColor(R.color.white));
                }
            });

            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(true);
        }

        void bind(Season season, boolean isSelected) {
            seasonName.setText(season.getName());

            if (isSelected) {
                container.setBackgroundResource(R.drawable.season_tab_selected);
                seasonName.setTextColor(itemView.getContext().getColor(android.R.color.black));
            } else {
                container.setBackground(itemView.getContext().getDrawable(R.drawable.season_tab_normal));
                seasonName.setTextColor(itemView.getContext().getColor(R.color.white));
            }
        }
    }
}