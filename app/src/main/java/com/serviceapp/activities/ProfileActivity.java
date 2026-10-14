package com.serviceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviceapp.R;
import com.serviceapp.models.User;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private CircleImageView ivAvatar;
    private TextView tvName, tvEmail, tvPhone, tvUsername, tvAccountType, tvRating, tvJoinDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("My Profile");
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        initViews();
        loadProfile();

        MaterialButton btnLogout = findViewById(R.id.btn_logout);
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(ProfileActivity.this, WelcomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void initViews() {
        ivAvatar = findViewById(R.id.iv_avatar);
        tvName = findViewById(R.id.tv_name);
        tvEmail = findViewById(R.id.tv_email);
        tvPhone = findViewById(R.id.tv_phone);
        tvUsername = findViewById(R.id.tv_username);
        tvAccountType = findViewById(R.id.tv_account_type);
        tvRating = findViewById(R.id.tv_rating);
        tvJoinDate = findViewById(R.id.tv_join_date);
    }

    private void loadProfile() {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        if (user == null) return;

                        tvName.setText(user.getFullName());
                        tvEmail.setText(user.getEmail());
                        tvPhone.setText(user.getPhone());
                        tvUsername.setText("@" + user.getUsername());
                        tvAccountType.setText(user.getAccountType().substring(0, 1).toUpperCase()
                                + user.getAccountType().substring(1));
                        tvRating.setText(String.format("%.1f (%d reviews)",
                                user.getRating(), user.getTotalRatings()));

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(this).load(user.getProfileImageUrl())
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .circleCrop()
                                    .into(ivAvatar);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load profile.", Toast.LENGTH_SHORT).show());
    }
}
