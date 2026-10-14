package com.serviceapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviceapp.R;
import com.serviceapp.models.ServiceRequest;
import com.serviceapp.models.User;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class PendingRequestAdapter extends RecyclerView.Adapter<PendingRequestAdapter.ViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(ServiceRequest request);
        void onDecline(ServiceRequest request);
    }

    private final List<ServiceRequest> requests;
    private final OnRequestActionListener listener;
    private final FirebaseFirestore db;

    public PendingRequestAdapter(List<ServiceRequest> requests, OnRequestActionListener listener) {
        this.requests = requests;
        this.listener = listener;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pending_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceRequest request = requests.get(position);

        holder.tvServiceType.setText(request.getServiceType());

        // Load client details
        db.collection("users").document(request.getClientId())
                .get().addOnSuccessListener(doc -> {
                    User client = doc.toObject(User.class);
                    if (client != null) {
                        holder.tvClientName.setText(client.getFirstName() + " " + client.getLastName());
                        if (client.getProfileImageUrl() != null && !client.getProfileImageUrl().isEmpty()) {
                            Glide.with(holder.itemView.getContext())
                                    .load(client.getProfileImageUrl())
                                    .into(holder.ivClientAvatar);
                        }
                    }
                });

        // Show distance if available
        if (request.getClientLatitude() != 0 && request.getClientLongitude() != 0) {
            holder.tvDistance.setText("Client nearby");
        }

        holder.btnAccept.setOnClickListener(v -> listener.onAccept(request));
        holder.btnDecline.setOnClickListener(v -> listener.onDecline(request));
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CircleImageView ivClientAvatar;
        TextView tvClientName, tvServiceType, tvDistance;
        MaterialButton btnAccept, btnDecline;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivClientAvatar = itemView.findViewById(R.id.ivClientAvatar);
            tvClientName = itemView.findViewById(R.id.tvClientName);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }
    }
}
