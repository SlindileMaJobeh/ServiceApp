package com.serviceapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.serviceapp.R;
import com.serviceapp.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProviderAdapter extends RecyclerView.Adapter<ProviderAdapter.ViewHolder> {

    public interface OnProviderClickListener {
        void onProviderClick(User provider);
    }

    private final List<User> providers;
    private final OnProviderClickListener listener;

    public ProviderAdapter(List<User> providers, OnProviderClickListener listener) {
        this.providers = providers;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_provider, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User provider = providers.get(position);
        holder.bind(provider);
        holder.btnRequest.setOnClickListener(v -> listener.onProviderClick(provider));
    }

    @Override
    public int getItemCount() { return providers.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivAvatar;
        TextView tvName, tvService, tvRating, tvDistance, tvJobsDone;
        MaterialButton btnRequest;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.iv_avatar);
            tvName = itemView.findViewById(R.id.tv_provider_name);
            tvService = itemView.findViewById(R.id.tv_provider_service);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvJobsDone = itemView.findViewById(R.id.tv_jobs_done);
            btnRequest = itemView.findViewById(R.id.btn_request);
        }

        public void bind(User provider) {
            tvName.setText(provider.getFullName());
            tvRating.setText(String.format("%.1f", provider.getRating()));
            tvJobsDone.setText(provider.getTotalRatings() + " jobs done");

            if (provider.getProfileImageUrl() != null && !provider.getProfileImageUrl().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(provider.getProfileImageUrl())
                        .placeholder(R.drawable.ic_person_placeholder)
                        .circleCrop()
                        .into(ivAvatar);
            }
        }
    }
}
