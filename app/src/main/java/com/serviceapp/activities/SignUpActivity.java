package com.serviceapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.serviceapp.R;
import com.serviceapp.models.User;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilFirstName, tilLastName, tilUsername, tilEmail, tilPhone,
            tilPassword, tilConfirmPassword;
    private TextInputEditText etFirstName, etLastName, etUsername, etEmail, etPhone,
            etPassword, etConfirmPassword;
    private MaterialButtonToggleGroup toggleAccountType;
    private MaterialButton btnCreateAccount;
    private ProgressBar progressBar;
    private LinearLayout layoutServiceType;

    // Service selector buttons
    private LinearLayout btnServicePlumbing, btnServiceElectrical, btnServiceMechanic,
            btnServiceCleaning, btnServiceGardening, btnServicePainting,
            btnServiceCarpentry, btnServiceSecurity;
    private LinearLayout selectedServiceButton = null;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private String selectedAccountType = "client";
    private String selectedServiceType = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilFirstName = findViewById(R.id.til_first_name);
        tilLastName = findViewById(R.id.til_last_name);
        tilUsername = findViewById(R.id.til_username);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etUsername = findViewById(R.id.et_username);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        toggleAccountType = findViewById(R.id.toggle_account_type);
        btnCreateAccount = findViewById(R.id.btn_create_account);
        progressBar = findViewById(R.id.progress_bar);
        layoutServiceType = findViewById(R.id.layout_service_type);

        // Service type buttons
        btnServicePlumbing   = findViewById(R.id.btn_service_plumbing);
        btnServiceElectrical = findViewById(R.id.btn_service_electrical);
        btnServiceMechanic   = findViewById(R.id.btn_service_mechanic);
        btnServiceCleaning   = findViewById(R.id.btn_service_cleaning);
        btnServiceGardening  = findViewById(R.id.btn_service_gardening);
        btnServicePainting   = findViewById(R.id.btn_service_painting);
        btnServiceCarpentry  = findViewById(R.id.btn_service_carpentry);
        btnServiceSecurity   = findViewById(R.id.btn_service_security);
    }

    private void setupListeners() {
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        // Account type toggle — show/hide service selector
        toggleAccountType.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                selectedAccountType = (checkedId == R.id.btn_client) ? "client" : "provider";
                layoutServiceType.setVisibility(
                        "provider".equals(selectedAccountType) ? View.VISIBLE : View.GONE);
                if ("client".equals(selectedAccountType)) {
                    selectedServiceType = "";
                }
            }
        });
        toggleAccountType.check(R.id.btn_client);

        // Service type click listeners
        setupServiceButton(btnServicePlumbing,   "plumbing");
        setupServiceButton(btnServiceElectrical, "electrical");
        setupServiceButton(btnServiceMechanic,   "mechanic");
        setupServiceButton(btnServiceCleaning,   "cleaning");
        setupServiceButton(btnServiceGardening,  "gardening");
        setupServiceButton(btnServicePainting,   "painting");
        setupServiceButton(btnServiceCarpentry,  "carpentry");
        setupServiceButton(btnServiceSecurity,   "security");

        TextView tvLogin = findViewById(R.id.tv_login);
        tvLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignUpActivity.this, LoginActivity.class));
            finish();
        });

        btnCreateAccount.setOnClickListener(v -> attemptSignUp());
    }

    private void setupServiceButton(LinearLayout button, String serviceType) {
        button.setOnClickListener(v -> {
            // Deselect previous
            if (selectedServiceButton != null) {
                selectedServiceButton.setBackgroundResource(R.drawable.bg_service_selector_unselected);
            }
            // Select this one
            button.setBackgroundResource(R.drawable.bg_service_selector_selected);
            selectedServiceButton = button;
            selectedServiceType = serviceType;
        });
    }

    private void attemptSignUp() {
        String firstName      = getText(etFirstName);
        String lastName       = getText(etLastName);
        String username       = getText(etUsername);
        String email          = getText(etEmail);
        String phone          = getText(etPhone);
        String password       = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        clearErrors();

        if (TextUtils.isEmpty(firstName)) { tilFirstName.setError("First name is required"); return; }
        if (TextUtils.isEmpty(lastName))  { tilLastName.setError("Last name is required");   return; }
        if (TextUtils.isEmpty(username))  { tilUsername.setError("Username is required");     return; }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_invalid_email)); return;
        }
        if (TextUtils.isEmpty(phone))     { tilPhone.setError("Phone number is required");   return; }
        if (password.length() < 6)        { tilPassword.setError(getString(R.string.error_password_short)); return; }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_password_mismatch)); return;
        }

        // Validate service type for providers
        if ("provider".equals(selectedAccountType) && TextUtils.isEmpty(selectedServiceType)) {
            Toast.makeText(this, "Please select the service you provide", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().getUser() != null) {
                        String uid = task.getResult().getUser().getUid();
                        saveUserToFirestore(uid, firstName, lastName, username, email, phone);
                    } else {
                        showLoading(false);
                        String errorMsg = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_signup_failed);
                        Toast.makeText(SignUpActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void saveUserToFirestore(String uid, String firstName, String lastName,
                                     String username, String email, String phone) {
        User user = new User(uid, firstName, lastName, username, email, phone, selectedAccountType);

        // Save service type for providers
        if ("provider".equals(selectedAccountType)) {
            user.setServiceType(selectedServiceType);
        }

        db.collection("users").document(uid).set(user)
                .addOnSuccessListener(aVoid -> {
                    showLoading(false);
                    Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    Intent intent;
                    if ("provider".equals(selectedAccountType)) {
                        intent = new Intent(SignUpActivity.this, ProviderDashboardActivity.class);
                    } else {
                        intent = new Intent(SignUpActivity.this, HomeActivity.class);
                    }
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    showLoading(false);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private void clearErrors() {
        tilFirstName.setError(null);
        tilLastName.setError(null);
        tilUsername.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnCreateAccount.setEnabled(!show);
    }
}
