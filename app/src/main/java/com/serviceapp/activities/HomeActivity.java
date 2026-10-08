package com.serviceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviceapp.R;

import java.util.Calendar;

import de.hdodenhof.circleimageview.CircleImageView;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private TextView tvGreeting, tvUserName;
    private CircleImageView ivProfile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        loadUserProfile();
        setupBottomNav();
        setGreeting();
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tv_greeting);
        tvUserName = findViewById(R.id.tv_user_name);
        ivProfile = findViewById(R.id.iv_profile);

        ivProfile.setOnClickListener(v ->
                startActivity(new Intent(HomeActivity.this, ProfileActivity.class)));

        // Wire up service card clicks
        setupServiceCard(R.id.card_plumbing,   "plumbing",   "Plumbing");
        setupServiceCard(R.id.card_electrical, "electrical", "Electrical");
        setupServiceCard(R.id.card_mechanic,   "mechanic",   "Mechanic");
        setupServiceCard(R.id.card_cleaning,   "cleaning",   "Cleaning");
        setupServiceCard(R.id.card_gardening,  "gardening",  "Gardening");
        setupServiceCard(R.id.card_painting,   "painting",   "Painting");
        setupServiceCard(R.id.card_carpentry,  "carpentry",  "Carpentry");
        setupServiceCard(R.id.card_security,   "security",   "Security");

        // Search bar — optional filter hint
        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Could scroll to matched card or highlight — left for future enhancement
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupServiceCard(int cardId, String serviceId, String serviceName) {
        android.view.View card = findViewById(cardId);
        if (card != null) {
            card.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, SearchingActivity.class);
                intent.putExtra("serviceId", serviceId);
                intent.putExtra("serviceName", serviceName);
                startActivity(intent);
            });
        }
    }

    private void loadUserProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String firstName = doc.getString("firstName");
                        String imageUrl = doc.getString("profileImageUrl");
                        tvUserName.setText(firstName != null ? firstName : "User");
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(this).load(imageUrl)
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .circleCrop()
                                    .into(ivProfile);
                        }
                    }
                });
    }

    private void setGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good morning,";
        else if (hour < 17) greeting = "Good afternoon,";
        else greeting = "Good evening,";
        tvGreeting.setText(greeting);
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_home);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
