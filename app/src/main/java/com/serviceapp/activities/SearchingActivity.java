package com.serviceapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.serviceapp.R;
import com.serviceapp.adapters.ProviderAdapter;
import com.serviceapp.models.User;

import java.util.ArrayList;
import java.util.List;

public class SearchingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;
    private static final double SEARCH_RADIUS_KM = 10.0;

    private MapView mapView;
    private GoogleMap googleMap;
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private LinearLayout llSearching;
    private CardView cardProviders;
    private RecyclerView rvProviders;
    private TextView tvSearchStatus;
    private ProviderAdapter providerAdapter;
    private List<User> providerList = new ArrayList<>();

    private String serviceId, serviceName;
    private double userLat, userLng;
    private boolean locationReceived = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_searching);

        serviceId = getIntent().getStringExtra("serviceId");
        serviceName = getIntent().getStringExtra("serviceName");

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        locationClient = LocationServices.getFusedLocationProviderClient(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Find " + serviceName);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        llSearching = findViewById(R.id.ll_searching);
        cardProviders = findViewById(R.id.card_providers);
        rvProviders = findViewById(R.id.rv_providers);

        // Show status text while locating
        tvSearchStatus = llSearching.findViewWithTag("status_text");

        mapView = findViewById(R.id.map_view);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);

        rvProviders.setLayoutManager(new LinearLayoutManager(this));
        providerAdapter = new ProviderAdapter(providerList, provider -> openChatWith(provider));
        rvProviders.setAdapter(providerAdapter);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            getUserLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
        }
        googleMap.getUiSettings().setZoomControlsEnabled(true);
    }

    private void getUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        // First try getLastLocation (fast)
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && !locationReceived) {
                locationReceived = true;
                userLat = location.getLatitude();
                userLng = location.getLongitude();
                stopLocationUpdates();
                updateMapWithUserLocation();
                searchNearbyProviders();
            }
        });

        // Also request fresh location in case last location is null
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult result) {
                if (locationReceived) return; // already got it from getLastLocation
                Location location = result.getLastLocation();
                if (location != null) {
                    locationReceived = true;
                    userLat = location.getLatitude();
                    userLng = location.getLongitude();
                    stopLocationUpdates();
                    updateMapWithUserLocation();
                    searchNearbyProviders();
                }
            }
        };

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateIntervalMillis(2000)
                .setMaxUpdates(3) // stop after 3 attempts
                .build();

        locationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        // Timeout after 15 seconds if still no location
        llSearching.postDelayed(() -> {
            if (!locationReceived) {
                stopLocationUpdates();
                Toast.makeText(this,
                        "Could not get your location. Please enable GPS and try again.",
                        Toast.LENGTH_LONG).show();
                llSearching.setVisibility(View.GONE);
            }
        }, 15000);
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            locationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void updateMapWithUserLocation() {
        if (googleMap == null) return;
        LatLng userLatLng = new LatLng(userLat, userLng);
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13f));
        googleMap.addMarker(new MarkerOptions()
                .position(userLatLng)
                .title("Your Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
    }

    private void searchNearbyProviders() {
        llSearching.setVisibility(View.VISIBLE);
        cardProviders.setVisibility(View.GONE);

        // Query Firestore for providers matching the selected service type
        db.collection("users")
                .whereEqualTo("accountType", "provider")
                .whereEqualTo("online", true)
                .whereEqualTo("serviceType", serviceId) // only match selected service
                .get()
                .addOnSuccessListener(snapshots -> {
                    providerList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        User provider = doc.toObject(User.class);
                        // Filter by distance
                        double distance = calculateDistance(
                                userLat, userLng,
                                provider.getLatitude(), provider.getLongitude());
                        if (distance <= SEARCH_RADIUS_KM) {
                            providerList.add(provider);
                            // Pin on map
                            if (googleMap != null) {
                                LatLng pLatLng = new LatLng(
                                        provider.getLatitude(), provider.getLongitude());
                                googleMap.addMarker(new MarkerOptions()
                                        .position(pLatLng)
                                        .title(provider.getFullName())
                                        .snippet(String.format("%.1f km away", distance)));
                            }
                        }
                    }

                    llSearching.setVisibility(View.GONE);
                    if (!providerList.isEmpty()) {
                        cardProviders.setVisibility(View.VISIBLE);
                        providerAdapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this,
                                "No " + serviceName + " providers found nearby. Try again later.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    llSearching.setVisibility(View.GONE);
                    Toast.makeText(this, "Error finding providers: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void openChatWith(User provider) {
        // Create a service request in Firestore, then open chat
        String clientId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        String requestId = db.collection("requests").document().getId();

        com.serviceapp.models.ServiceRequest request =
                new com.serviceapp.models.ServiceRequest(requestId, clientId, serviceId, userLat, userLng);
        request.setProviderId(provider.getUid());
        request.setStatus(com.serviceapp.models.ServiceRequest.STATUS_ACCEPTED);

        db.collection("requests").document(requestId).set(request)
                .addOnSuccessListener(aVoid -> {
                    Intent intent = new Intent(SearchingActivity.this, ChatActivity.class);
                    intent.putExtra("requestId", requestId);
                    intent.putExtra("providerId", provider.getUid());
                    intent.putExtra("providerName", provider.getFullName());
                    intent.putExtra("providerImage", provider.getProfileImageUrl());
                    startActivity(intent);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send request.", Toast.LENGTH_SHORT).show());
    }

    private double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0] / 1000.0; // Convert metres to km
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getUserLocation();
            } else {
                Toast.makeText(this, getString(R.string.location_permission_required),
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // MapView lifecycle methods
    @Override protected void onResume() { super.onResume(); mapView.onResume(); }
    @Override protected void onPause() { super.onPause(); mapView.onPause(); }
    @Override protected void onDestroy() { super.onDestroy(); mapView.onDestroy(); stopLocationUpdates(); }
    @Override protected void onStart() { super.onStart(); mapView.onStart(); }
    @Override protected void onStop() { super.onStop(); mapView.onStop(); }
    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState);
    }
    @Override public void onLowMemory() { super.onLowMemory(); mapView.onLowMemory(); }
}