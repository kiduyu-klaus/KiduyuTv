package com.kiduyu.klaus.kiduyutv.adapter;

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
import com.kiduyu.klaus.kiduyutv.model.CompanyNetwork;

import java.util.ArrayList;
import java.util.List;

public class CompanyNetworkAdapter extends RecyclerView.Adapter<CompanyNetworkAdapter.ViewHolder> {

    private List<CompanyNetwork> items = new ArrayList<>();
    private OnItemClickListener listener;

    public CompanyNetworkAdapter() {
    }

    public CompanyNetworkAdapter(List<CompanyNetwork> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public interface OnItemClickListener {
        void onItemClick(CompanyNetwork companyNetwork, int position);
    }

    public interface OnFocusChangeListener {
        void onFocusChanged(CompanyNetwork companyNetwork, int position, boolean hasFocus);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    private OnFocusChangeListener focusChangeListener;

    public void setOnFocusChangeListener(OnFocusChangeListener listener) {
        this.focusChangeListener = listener;
    }

    public void setItems(List<CompanyNetwork> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void addItems(List<CompanyNetwork> items) {
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_company_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CompanyNetwork item = items.get(position);
        holder.bind(item);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item, position);
            }
        });

        holder.itemView.setOnFocusChangeListener((v, hasFocus) -> {
            if (focusChangeListener != null) {
                focusChangeListener.onFocusChanged(item, position, hasFocus);
            }
            // Scale animation for TV D-pad focus feedback
            float scale = hasFocus ? 1.08f : 1.0f;
            v.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(150)
                    .start();
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView logoImage;
        private final TextView nameText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            logoImage = itemView.findViewById(R.id.companyLogo);
            nameText = itemView.findViewById(R.id.companyName);
            // Required for Android TV D-pad navigation
            itemView.setFocusable(true);
            itemView.setFocusableInTouchMode(false);
        }

        void bind(CompanyNetwork item) {
            nameText.setText(item.getName());

            // Load logo with Glide
            if (item.getLogoUrl() != null) {
                Glide.with(itemView.getContext())
                        .load(item.getLogoUrl())
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_gray)
                        .error(R.drawable.placeholder_gray)
                        .into(logoImage);
            } else {
                logoImage.setImageResource(R.drawable.placeholder_gray);
            }
        }
    }
}