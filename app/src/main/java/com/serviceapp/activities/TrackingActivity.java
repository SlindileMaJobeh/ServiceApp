package com.serviceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.serviceapp.R;
import com.serviceapp.models.ServiceRequest;

import de.hdodenhof.circleimageview.CircleImageView;

public class TrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private MapView mapView;
    private GoogleMap googleMap;
    private FirebaseFirestore db;
    private ListenerRegistration requestListener, providerLocationListener;

    private String requestId, providerId, providerName, providerImage;
    private Marker providerMarker;

    private TextView tvStatusTitle, tvEta;
    private LinearLayout llArrived;
    private CircleImageView ivProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        db = FirebaseFirestore.getInstance();

        requestId = getIntent().getStringExtra("requestId");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        providerImage = getIntent().getStringExtra("providerImage");

        initViews();

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        listenToRequestStatus();
        listenToProviderLocation();
    }

    private void initViews() {
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvEta = findViewById(R.id.tv_eta);
        llArrived = findViewById(R.id.ll_arrived);
        ivProvider = findViewById(R.id.iv_provider);
        TextView tvProviderName = findViewById(R.id.tv_provider_name);

        tvProviderName.setText(providerName);

        if (providerImage != null && !providerImage.isEmpty()) {
            Glide.with(this).load(providerImage)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivProvider);
        }

        ImageButton btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> onBackPressed());

        ImageButton btnChat = findViewById(R.id.btn_chat);
        btnChat.setOnClickListener(v -> {
            Intent intent = new Intent(TrackingActivity.this, ChatActivity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("providerId", providerId);
            intent.putExtra("providerName", providerName);
            intent.putExtra("providerImage", providerImage);
            startActivity(intent);
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(true);
        googleMap.getUiSettings().setCompassEnabled(true);
    }

    private void listenToRequestStatus() {
        requestListener = db.collection("requests").document(requestId)
                .addSnapshotListener((doc, error) -> {
                    if (doc == null || !doc.exists()) return;
                    String status = doc.getString("status");

                    if (ServiceRequest.STATUS_ARRIVED.equals(status)) {
                        // Provider has arrived!
                        tvStatusTitle.setText(getString(R.string.provider_arrived));
                        tvEta.setText("The provider is at your door!");
                        llArrived.setVisibility(View.VISIBLE);
                    } else if (ServiceRequest.STATUS_COMPLETED.equals(status)) {
                        // Job done - go to rating screen
                        Intent intent = new Intent(TrackingActivity.this, RatingActivity.class);
                        intent.putExtra("requestId", requestId);
                        intent.putExtra("providerId", providerId);
                        intent.putExtra("providerName", providerName);
                        intent.putExtra("providerImage", providerImage);
                        startActivity(intent);
                        finish();
                    } else if (ServiceRequest.STATUS_ON_WAY.equals(status)) {
                        tvStatusTitle.setText(getString(R.string.provider_on_way));
                    }
                });
    }

    private void listenToProviderLocation() {
        // Provider updates their location in Firestore while en route
        providerLocationListener = db.collection("users").document(providerId)
                .addSnapshotListener((doc, error) -> {
                    if (doc == null || !doc.exists()) return;
                    Double lat = doc.getDouble("latitude");
                    Double lng = doc.getDouble("longitude");

                    if (lat != null && lng != null && googleMap != null) {
                        LatLng provLatLng = new LatLng(lat, lng);
                        if (providerMarker == null) {
                            providerMarker = googleMap.addMarker(new MarkerOptions()
                                    .position(provLatLng)
                                    .title(providerName)
                                    .icon(BitmapDescriptorFactory.defaultMarker(
                                            BitmapDescriptorFactory.HUE_GREEN)));
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(provLatLng, 15f));
                        } else {
                            providerMarker.setPosition(provLatLng);
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestListener != null) requestListener.remove();
        if (providerLocationListener != null) providerLocationListener.remove();
        mapView.onDestroy();
    }

    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out); mapView.onSaveInstanceState(out);
    }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}
