package com.serviceapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.serviceapp.R;
import com.serviceapp.adapters.MessageAdapter;
import com.serviceapp.models.Message;
import com.serviceapp.models.ServiceRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private ImageButton btnSend, btnAttach;
    private LinearLayout llAcceptBanner;
    private MaterialButton btnAcceptProvider;
    private CircleImageView ivProviderAvatar;
    private TextView tvProviderName, tvProviderStatus;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String currentUserId, requestId, providerId, providerName, providerImage;
    private List<Message> messageList = new ArrayList<>();
    private MessageAdapter messageAdapter;
    private ListenerRegistration messageListener;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        // Get intent extras
        requestId = getIntent().getStringExtra("requestId");
        providerId = getIntent().getStringExtra("providerId");
        providerName = getIntent().getStringExtra("providerName");
        providerImage = getIntent().getStringExtra("providerImage");

        setupViews();
        setupImagePicker();
        listenForMessages();
        loadRequestStatus();
    }

    private void setupViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        ivProviderAvatar = toolbar.findViewById(R.id.iv_provider_avatar);
        tvProviderName = toolbar.findViewById(R.id.tv_provider_name);
        tvProviderStatus = toolbar.findViewById(R.id.tv_provider_status);

        tvProviderName.setText(providerName);

        if (providerImage != null && !providerImage.isEmpty()) {
            Glide.with(this).load(providerImage)
                    .placeholder(R.drawable.ic_person_placeholder)
                    .circleCrop()
                    .into(ivProviderAvatar);
        }

        rvMessages = findViewById(R.id.rv_messages);
        etMessage = findViewById(R.id.et_message);
        btnSend = findViewById(R.id.btn_send);
        btnAttach = findViewById(R.id.btn_attach);
        llAcceptBanner = findViewById(R.id.ll_accept_banner);
        btnAcceptProvider = findViewById(R.id.btn_accept_provider);

        // Setup RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        messageAdapter = new MessageAdapter(messageList, currentUserId);
        rvMessages.setAdapter(messageAdapter);

        btnSend.setOnClickListener(v -> sendTextMessage());
        btnAttach.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnAcceptProvider.setOnClickListener(v -> showAcceptConfirmation());
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) uploadAndSendImage(uri);
                });
    }

    private void sendTextMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        Message message = new Message(currentUserId, providerId, requestId, text, Message.TYPE_TEXT);
        String messageId = db.collection("requests").document(requestId)
                .collection("messages").document().getId();
        message.setMessageId(messageId);

        db.collection("requests").document(requestId)
                .collection("messages").document(messageId)
                .set(message)
                .addOnSuccessListener(aVoid -> etMessage.setText(""))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show());
    }

    private void uploadAndSendImage(Uri imageUri) {
        String imageId = UUID.randomUUID().toString();
        StorageReference ref = storage.getReference()
                .child("chat_images/" + requestId + "/" + imageId + ".jpg");

        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show();

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            // Send image message
                            Message msg = new Message(currentUserId, providerId, requestId,
                                    "[Image]", Message.TYPE_IMAGE);
                            msg.setImageUrl(uri.toString());
                            String msgId = db.collection("requests").document(requestId)
                                    .collection("messages").document().getId();
                            msg.setMessageId(msgId);

                            db.collection("requests").document(requestId)
                                    .collection("messages").document(msgId).set(msg);
                        }))
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Image upload failed.", Toast.LENGTH_SHORT).show());
    }

    private void listenForMessages() {
        messageListener = db.collection("requests").document(requestId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) return;
                    if (snapshots == null) return;

                    for (DocumentChange change : snapshots.getDocumentChanges()) {
                        if (change.getType() == DocumentChange.Type.ADDED) {
                            Message msg = change.getDocument().toObject(Message.class);
                            messageList.add(msg);
                            messageAdapter.notifyItemInserted(messageList.size() - 1);
                            rvMessages.scrollToPosition(messageList.size() - 1);
                        }
                    }
                });
    }

    private void loadRequestStatus() {
        db.collection("requests").document(requestId)
                .addSnapshotListener((doc, error) -> {
                    if (doc != null && doc.exists()) {
                        String status = doc.getString("status");
                        updateUIForStatus(status);
                    }
                });
    }

    private void updateUIForStatus(String status) {
        if (ServiceRequest.STATUS_PROVIDER_ACCEPTED.equals(status)) {
            // Provider has agreed, show banner for client to accept
            llAcceptBanner.setVisibility(View.VISIBLE);
        } else if (ServiceRequest.STATUS_CLIENT_ACCEPTED.equals(status)) {
            llAcceptBanner.setVisibility(View.GONE);
            // Navigate to tracking
            Intent intent = new Intent(ChatActivity.this, TrackingActivity.class);
            intent.putExtra("requestId", requestId);
            intent.putExtra("providerId", providerId);
            intent.putExtra("providerName", providerName);
            intent.putExtra("providerImage", providerImage);
            startActivity(intent);
        }
    }

    private void showAcceptConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Accept Provider")
                .setMessage("By accepting, you agree to share your location with " + providerName
                        + " so they can navigate to you.")
                .setPositiveButton("Accept & Share Location", (dialog, which) -> acceptProvider())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void acceptProvider() {
        db.collection("requests").document(requestId)
                .update("status", ServiceRequest.STATUS_CLIENT_ACCEPTED)
                .addOnSuccessListener(aVoid -> {
                    llAcceptBanner.setVisibility(View.GONE);
                    Toast.makeText(this, "Location shared! Provider is on the way.",
                            Toast.LENGTH_SHORT).show();
                    // Navigate to tracking screen
                    Intent intent = new Intent(ChatActivity.this, TrackingActivity.class);
                    intent.putExtra("requestId", requestId);
                    intent.putExtra("providerId", providerId);
                    intent.putExtra("providerName", providerName);
                    intent.putExtra("providerImage", providerImage);
                    startActivity(intent);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) messageListener.remove();
    }
}
