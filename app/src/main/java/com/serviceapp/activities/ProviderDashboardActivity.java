package com.serviceapp.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.serviceapp.R;
import com.serviceapp.adapters.PendingRequestAdapter;
import com.serviceapp.models.Rating;
import com.serviceapp.models.ServiceRequest;
import com.serviceapp.models.User;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProviderDashboardActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // UI
    private CircleImageView ivProviderAvatar;
    private TextView tvProviderGreeting, tvProviderName;
    private TextView tvTodayJobs, tvRating, tvTotalJobs;
    private TextView tvOnlineStatus;
    private SwitchMaterial switchOnline;
    private RecyclerView rvPendingRequests;
    private LinearLayout layoutEmptyState;
    private androidx.cardview.widget.CardView cardActiveRequest;
    private CircleImageView ivClientAvatar;
    private TextView tvClientName, tvServiceType, tvActiveStatus;
    private MaterialButton btnOpenChat, btnNavigateToClient, btnMarkArrived, btnCloseTicket;

    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentProviderId;

    // Data
    private User currentProvider;
    private ServiceRequest activeRequest;
    private List<ServiceRequest> pendingRequests = new ArrayList<>();
    private PendingRequestAdapter pendingAdapter;
    private ListenerRegistration pendingListener;
    private ListenerRegistration activeRequestListener;

    // Location
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_provider_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
            return;
        }
        currentProviderId = mAuth.getCurrentUser().getUid();

        initViews();
        loadProviderProfile();
        setupPendingRequestsList();
        setupLocationCallback();
        listenForActiveRequest();
    }

    private void initViews() {
        ivProviderAvatar = findViewById(R.id.ivProviderAvatar);
        tvProviderGreeting = findViewById(R.id.tvProviderGreeting);
        tvProviderName = findViewById(R.id.tvProviderName);
        tvTodayJobs = findViewById(R.id.tvTodayJobs);
        tvRating = findViewById(R.id.tvRating);
        tvTotalJobs = findViewById(R.id.tvTotalJobs);
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus);
        switchOnline = findViewById(R.id.switchOnline);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        cardActiveRequest = findViewById(R.id.cardActiveRequest);
        ivClientAvatar = findViewById(R.id.ivClientAvatar);
        tvClientName = findViewById(R.id.tvClientName);
        tvServiceType = findViewById(R.id.tvServiceType);
        tvActiveStatus = findViewById(R.id.tvActiveStatus);
        btnOpenChat = findViewById(R.id.btnOpenChat);
        btnNavigateToClient = findViewById(R.id.btnNavigateToClient);
        btnMarkArrived = findViewById(R.id.btnMarkArrived);
        btnCloseTicket = findViewById(R.id.btnCloseTicket);

        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        String greeting = hour < 12 ? "Good Morning," : hour < 17 ? "Good Afternoon," : "Good Evening,";
        tvProviderGreeting.setText(greeting);

        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tvOnlineStatus.setText(isChecked ? "ONLINE" : "OFFLINE");
            updateOnlineStatus(isChecked);
            if (isChecked) {
                startLocationUpdates();
            } else {
                stopLocationUpdates();
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavView);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });

        btnOpenChat.setOnClickListener(v -> {
            if (activeRequest != null) {
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("requestId", activeRequest.getRequestId());
                intent.putExtra("otherUserId", activeRequest.getClientId());
                startActivity(intent);
            }
        });

        btnNavigateToClient.setOnClickListener(v -> {
            if (activeRequest != null) {
                // FIXED: use getClientLatitude() and getClientLongitude()
                String uri = "google.navigation:q=" + activeRequest.getClientLatitude()
                        + "," + activeRequest.getClientLongitude() + "&mode=d";
                Intent mapIntent = new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse(uri));
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show();
                }
                updateRequestStatus(activeRequest.getRequestId(), ServiceRequest.STATUS_ON_WAY);
            }
        });

        btnMarkArrived.setOnClickListener(v -> {
            if (activeRequest != null) {
                updateRequestStatus(activeRequest.getRequestId(), ServiceRequest.STATUS_ARRIVED);
                btnMarkArrived.setVisibility(View.GONE);
                tvActiveStatus.setText("Arrived");
                Toast.makeText(this, "Client has been notified you arrived!", Toast.LENGTH_SHORT).show();
            }
        });

        btnCloseTicket.setOnClickListener(v -> {
            if (activeRequest != null) {
                showRateClientDialog();
            }
        });
    }

    private void loadProviderProfile() {
        db.collection("users").document(currentProviderId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null) return;
                    currentProvider = snapshot.toObject(User.class);
                    if (currentProvider == null) return;

                    tvProviderName.setText(currentProvider.getFirstName() + " " + currentProvider.getLastName());
                    tvRating.setText(String.format(Locale.getDefault(), "%.1f ★", currentProvider.getRating()));
                    tvTotalJobs.setText(String.valueOf(currentProvider.getTotalRatings()));

                    if (currentProvider.getProfileImageUrl() != null && !currentProvider.getProfileImageUrl().isEmpty()) {
                        Glide.with(this).load(currentProvider.getProfileImageUrl()).into(ivProviderAvatar);
                    }

                    switchOnline.setChecked(currentProvider.isOnline());
                    tvOnlineStatus.setText(currentProvider.isOnline() ? "ONLINE" : "OFFLINE");
                });
    }

    private void setupPendingRequestsList() {
        pendingAdapter = new PendingRequestAdapter(pendingRequests, new PendingRequestAdapter.OnRequestActionListener() {
            @Override
            public void onAccept(ServiceRequest request) {
                acceptRequest(request);
            }

            @Override
            public void onDecline(ServiceRequest request) {
                declineRequest(request);
            }
        });
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        rvPendingRequests.setAdapter(pendingAdapter);

        pendingListener = db.collection("requests")
                .whereEqualTo("providerId", currentProviderId)
                .whereEqualTo("status", ServiceRequest.STATUS_PENDING)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    pendingRequests.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        ServiceRequest req = doc.toObject(ServiceRequest.class);
                        if (req != null) pendingRequests.add(req);
                    }
                    pendingAdapter.notifyDataSetChanged();
                    layoutEmptyState.setVisibility(pendingRequests.isEmpty() ? View.VISIBLE : View.GONE);
                    rvPendingRequests.setVisibility(pendingRequests.isEmpty() ? View.GONE : View.VISIBLE);
                });
    }

    private void listenForActiveRequest() {
        // Use Arrays.asList() instead of List.of() for better API compatibility
        List<String> activeStatuses = Arrays.asList(
                ServiceRequest.STATUS_ACCEPTED,
                ServiceRequest.STATUS_PROVIDER_ACCEPTED,
                ServiceRequest.STATUS_CLIENT_ACCEPTED,
                ServiceRequest.STATUS_ON_WAY,
                ServiceRequest.STATUS_ARRIVED,
                ServiceRequest.STATUS_IN_PROGRESS
        );

        activeRequestListener = db.collection("requests")
                .whereEqualTo("providerId", currentProviderId)
                .whereIn("status", activeStatuses)
                .limit(1)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    if (!snapshots.isEmpty()) {
                        activeRequest = snapshots.getDocuments().get(0).toObject(ServiceRequest.class);
                        showActiveRequest();
                    } else {
                        activeRequest = null;
                        cardActiveRequest.setVisibility(View.GONE);
                    }
                });
    }

    private void showActiveRequest() {
        if (activeRequest == null) return;
        cardActiveRequest.setVisibility(View.VISIBLE);

        tvServiceType.setText(activeRequest.getServiceType());
        String status = activeRequest.getStatus();
        tvActiveStatus.setText(formatStatus(status));

        btnMarkArrived.setVisibility(
                status.equals(ServiceRequest.STATUS_ON_WAY) ? View.VISIBLE : View.GONE);

        db.collection("users").document(activeRequest.getClientId())
                .get().addOnSuccessListener(doc -> {
                    User client = doc.toObject(User.class);
                    if (client != null) {
                        tvClientName.setText(client.getFirstName() + " " + client.getLastName());
                        if (client.getProfileImageUrl() != null && !client.getProfileImageUrl().isEmpty()) {
                            Glide.with(this).load(client.getProfileImageUrl()).into(ivClientAvatar);
                        }
                    }
                });
    }

    private String formatStatus(String status) {
        switch (status) {
            case ServiceRequest.STATUS_ACCEPTED: return "Accepted";
            case ServiceRequest.STATUS_CLIENT_ACCEPTED: return "Confirmed";
            case ServiceRequest.STATUS_ON_WAY: return "En Route";
            case ServiceRequest.STATUS_ARRIVED: return "Arrived";
            case ServiceRequest.STATUS_IN_PROGRESS: return "In Progress";
            default: return status;
        }
    }

    private void acceptRequest(ServiceRequest request) {
        db.collection("requests").document(request.getRequestId())
                .update("status", ServiceRequest.STATUS_PROVIDER_ACCEPTED)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request accepted! Open chat to discuss.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(this, ChatActivity.class);
                    intent.putExtra("requestId", request.getRequestId());
                    intent.putExtra("otherUserId", request.getClientId());
                    startActivity(intent);
                });
    }

    private void declineRequest(ServiceRequest request) {
        db.collection("requests").document(request.getRequestId())
                .update("status", ServiceRequest.STATUS_CANCELLED)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Request declined.", Toast.LENGTH_SHORT).show());
    }

    private void updateRequestStatus(String requestId, String newStatus) {
        db.collection("requests").document(requestId).update("status", newStatus);
    }

    private void showRateClientDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_rate_client);
        dialog.getWindow().setLayout(
                android.view.WindowManager.LayoutParams.MATCH_PARENT,
                android.view.WindowManager.LayoutParams.WRAP_CONTENT);

        ImageView[] stars = {
                dialog.findViewById(R.id.star1),
                dialog.findViewById(R.id.star2),
                dialog.findViewById(R.id.star3),
                dialog.findViewById(R.id.star4),
                dialog.findViewById(R.id.star5)
        };
        final int[] selectedRating = {0};

        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i;
            stars[i].setOnClickListener(v -> {
                selectedRating[0] = starIndex + 1;
                for (int j = 0; j < stars.length; j++) {
                    stars[j].setImageResource(j <= starIndex
                            ? R.drawable.ic_star_filled
                            : R.drawable.ic_star_outline);
                }
            });
        }

        TextInputEditText etComment = dialog.findViewById(R.id.etComment);
        MaterialButton btnSubmit = dialog.findViewById(R.id.btnSubmitRating);

        btnSubmit.setOnClickListener(v -> {
            if (selectedRating[0] == 0) {
                Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }
            String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
            submitRatingAndClose(selectedRating[0], comment);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void submitRatingAndClose(int stars, String comment) {
        if (activeRequest == null) return;

        String ratingId = UUID.randomUUID().toString();
        Rating rating = new Rating();
        rating.setRatingId(ratingId);
        rating.setRaterId(currentProviderId);
        rating.setRatedUserId(activeRequest.getClientId());
        rating.setRequestId(activeRequest.getRequestId());
        rating.setStars(stars);
        rating.setComment(comment);
        rating.setTimestamp(new Date().getTime());
        if (currentProvider != null) {
            rating.setRaterName(currentProvider.getFirstName() + " " + currentProvider.getLastName());
            rating.setRaterImageUrl(currentProvider.getProfileImageUrl());
        }

        db.collection("ratings").document(ratingId).set(rating);

        db.collection("requests").document(activeRequest.getRequestId())
                .update("status", ServiceRequest.STATUS_COMPLETED)
                .addOnSuccessListener(aVoid -> {
                    tvTodayJobs.setText(String.valueOf(
                            Integer.parseInt(tvTodayJobs.getText().toString()) + 1));
                    Toast.makeText(this, "Ticket closed! Great job.", Toast.LENGTH_SHORT).show();
                    cardActiveRequest.setVisibility(View.GONE);
                    activeRequest = null;
                });
    }

    private void updateOnlineStatus(boolean isOnline) {
        Map<String, Object> update = new HashMap<>();
        update.put("online", isOnline);
        db.collection("users").document(currentProviderId).update(update);
    }

    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                Location location = result.getLastLocation();
                if (location == null) return;
                Map<String, Object> update = new HashMap<>();
                update.put("lat", location.getLatitude());
                update.put("lng", location.getLongitude());
                db.collection("users").document(currentProviderId).update(update);
            }
        };
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
            return;
        }
        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(3000)
                .build();
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
    }

    private void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (pendingListener != null) pendingListener.remove();
        if (activeRequestListener != null) activeRequestListener.remove();
        stopLocationUpdates();
    }
}
