package com.serviceapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.serviceapp.R;
import com.serviceapp.models.Service;

import java.util.List;

public class ServiceAdapter extends RecyclerView.Adapter<ServiceAdapter.ViewHolder> {

    public interface OnServiceClickListener {
        void onServiceClick(Service service);
    }

    private final List<Service> services;
    private final OnServiceClickListener listener;

    public ServiceAdapter(List<Service> services, OnServiceClickListener listener) {
        this.services = services;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_service, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Service service = services.get(position);
        holder.bind(service);
        holder.itemView.setOnClickListener(v -> listener.onServiceClick(service));
    }

    @Override
    public int getItemCount() { return services.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivIcon;
        private final TextView tvName, tvDescription;
        private final CardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_service_icon);
            tvName = itemView.findViewById(R.id.tv_service_name);
            tvDescription = itemView.findViewById(R.id.tv_service_description);
            cardView = (CardView) itemView;
        }

        public void bind(Service service) {
            Context ctx = itemView.getContext();
            tvName.setText(service.getName());
            tvDescription.setText(service.getDescription());
            ivIcon.setImageResource(service.getIconResId());

            // Tint the icon background with category color
            ivIcon.setBackgroundColor(ContextCompat.getColor(ctx, service.getBackgroundColorResId()) & 0x33FFFFFF
                    | (ContextCompat.getColor(ctx, service.getBackgroundColorResId()) & 0xFFFFFF) | 0x22000000);
        }
    }
}
