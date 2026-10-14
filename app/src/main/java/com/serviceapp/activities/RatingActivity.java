package com.serviceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviceapp.R;
import com.serviceapp.models.Rating;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class RatingActivity extends AppCompatActivity {

    private float selectedStars = 0;
    private ImageView[] stars;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId, requestId, providerId, providerName, providerImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rating);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        requestId = getIntent().getStringExtra("requestId");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        providerImage = getIntent().getStringExtra("providerImage");

        setupViews();
    }

    private void setupViews() {
        TextView tvProviderName = findViewById(R.id.tv_provider_name);
        CircleImageView ivProviderAvatar = findViewById(R.id.iv_provider_avatar);
        TextInputEditText etComment = findViewById(R.id.et_comment);
        MaterialButton btnSubmit = findViewById(R.id.btn_submit);

        tvProviderName.setText(providerName);

        if (providerImage != null && !providerImage.isEmpty()) {
            Glide.with(this).load(providerImage)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivProviderAvatar);
        }

        // Setup star rating
        stars = new ImageView[]{
                findViewById(R.id.star_1),
                findViewById(R.id.star_2),
                findViewById(R.id.star_3),
                findViewById(R.id.star_4),
                findViewById(R.id.star_5)
        };

        for (int i = 0; i < stars.length; i++) {
            final int starIndex = i + 1;
            stars[i].setOnClickListener(v -> {
                selectedStars = starIndex;
                updateStarDisplay(starIndex);
            });
        }

        btnSubmit.setOnClickListener(v -> {
            String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";
            if (selectedStars == 0) {
                Toast.makeText(this, "Please select a star rating.", Toast.LENGTH_SHORT).show();
                return;
            }
            submitRating(comment);
        });
    }

    private void updateStarDisplay(int filled) {
        for (int i = 0; i < stars.length; i++) {
            stars[i].setImageResource(i < filled ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
        }
    }

    private void submitRating(String comment) {
        // Save rating to Firestore
        Rating rating = new Rating(currentUserId, providerId, requestId, selectedStars, comment);

        // Save rating document
        String ratingId = db.collection("ratings").document().getId();
        rating.setRatingId(ratingId);

        db.collection("ratings").document(ratingId).set(rating)
                .addOnSuccessListener(aVoid -> {
                    // Update provider's average rating using a Firestore transaction
                    updateProviderAverageRating();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to submit rating.", Toast.LENGTH_SHORT).show());
    }

    private void updateProviderAverageRating() {
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentReference provRef =
                    db.collection("users").document(providerId);
            com.google.firebase.firestore.DocumentSnapshot provSnap = transaction.get(provRef);

            double currentRating = provSnap.contains("rating") ?
                    provSnap.getDouble("rating") : 0.0;
            long totalRatings = provSnap.contains("totalRatings") ?
                    provSnap.getLong("totalRatings") : 0L;

            double newTotal = (currentRating * totalRatings) + selectedStars;
            long newCount = totalRatings + 1;
            double newAverage = newTotal / newCount;

            Map<String, Object> updates = new HashMap<>();
            updates.put("rating", newAverage);
            updates.put("totalRatings", newCount);
            transaction.update(provRef, updates);

            return null;
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Thank you for your feedback!", Toast.LENGTH_SHORT).show();
            // Go back to home
            Intent intent = new Intent(RatingActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}
