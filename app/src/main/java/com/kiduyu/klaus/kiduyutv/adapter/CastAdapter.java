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
import com.kiduyu.klaus.kiduyutv.model.CastMember;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying cast members in a horizontal RecyclerView
 */
public class CastAdapter extends RecyclerView.Adapter<CastAdapter.CastViewHolder> {

    private List<CastMember> castList;
    private OnCastClickListener listener;

    public interface OnCastClickListener {
        void onCastClick(CastMember castMember, int position);
    }

    public CastAdapter() {
        this.castList = new ArrayList<>();
    }

    public CastAdapter(List<CastMember> castList) {
        this.castList = castList != null ? castList : new ArrayList<>();
    }

    public void setCastList(List<CastMember> castList) {
        this.castList = castList != null ? castList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setOnCastClickListener(OnCastClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public CastViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cast_member, parent, false);
        return new CastViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CastViewHolder holder, int position) {
        CastMember castMember = castList.get(position);
        holder.bind(castMember);
    }

    @Override
    public int getItemCount() {
        return castList != null ? castList.size() : 0;
    }

    class CastViewHolder extends RecyclerView.ViewHolder {
        private ImageView profileImage;
        private TextView nameText;
        private TextView characterText;

        public CastViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            nameText = itemView.findViewById(R.id.nameText);
            characterText = itemView.findViewById(R.id.characterText);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onCastClick(castList.get(position), position);
                }
            });

            // Focus change listener for TV remote navigation
            itemView.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    v.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start();
                    //v.setBackgroundResource(R.drawable.content_card_focus_overlay);
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start();
                    //v.setBackgroundResource(android.R.color.transparent);
                }
            });
        }

        public void bind(CastMember castMember) {
            nameText.setText(castMember.getName());
            characterText.setText(castMember.getCharacter());

            // Load profile image
            String imageUrl = castMember.getProfileImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.placeholder_movie)
                        .error(R.drawable.placeholder_movie)
                        .circleCrop()
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.placeholder_movie);
            }
        }
    }
}
