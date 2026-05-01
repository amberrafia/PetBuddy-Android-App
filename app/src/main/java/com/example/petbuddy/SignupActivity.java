package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class SignupActivity extends AppCompatActivity {

    private TextInputLayout nameLayout, emailLayout, passwordLayout;
    private TextInputEditText nameInput, emailInput, passwordInput;
    private Button signupButton;
    private TextView loginLink;
    private CheckBox termsCheckbox;

    private DatabaseHelper dbHelper;
    private SharedPreferencesHelper prefsHelper;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        nameLayout = findViewById(R.id.nameLayout);
        emailLayout = findViewById(R.id.emailLayout);
        passwordLayout = findViewById(R.id.passwordLayout);

        nameInput = findViewById(R.id.nameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);

        signupButton = findViewById(R.id.signupButton);
        loginLink = findViewById(R.id.loginLink);
        termsCheckbox = findViewById(R.id.termsCheckbox);

        dbHelper = new DatabaseHelper(this);
        prefsHelper = new SharedPreferencesHelper(this);

        signupButton.setOnClickListener(v -> signupUser());
        mAuth = FirebaseAuth.getInstance();


        loginLink.setOnClickListener(v ->
                startActivity(new Intent(SignupActivity.this, LoginActivity.class))
        );
    }

    private void signupUser() {

        String name = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        String email = emailInput.getText() == null ? "" : emailInput.getText().toString().trim();
        String password = passwordInput.getText() == null ? "" : passwordInput.getText().toString().trim();

        nameLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);

        if (TextUtils.isEmpty(name)) {
            nameLayout.setError("Name required");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailLayout.setError("Email required");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Invalid email");
            return;
        }

        if (TextUtils.isEmpty(password) || password.length() < 6) {
            passwordLayout.setError("Password must be at least 6 characters");
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept Terms & Conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🔥 FIREBASE SIGNUP (MUST BE LAST)
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        FirebaseUser user = mAuth.getCurrentUser();

                        prefsHelper.setLoggedIn(true);
                        prefsHelper.setUserEmail(email);
                        prefsHelper.setUserName(name);

                        // 🔥 ADD USER TO COMMUNITY DATABASE
                        if (user != null) {
                            String uid = user.getUid();
                            
                            // Use explicit database URL
                            FirebaseDatabase database = FirebaseDatabase.getInstance(
                                    "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
                            );
                            DatabaseReference communityRef = database.getReference("communityMembers");

                            CommunityModel communityMember = new CommunityModel(
                                    name,
                                    email,
                                    "", // Phone number can be added later
                                    System.currentTimeMillis()
                            );

                            communityRef.child(uid).setValue(communityMember)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(this, 
                                                "✅ Account Created & Added to Community!", 
                                                Toast.LENGTH_LONG).show();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, 
                                                "❌ Community join failed: " + e.getMessage(), 
                                                Toast.LENGTH_LONG).show();
                                        e.printStackTrace();
                                    });
                        }

                        startActivity(new Intent(this, PetBuddyMainActivity.class));
                        finish();

                    } else {
                        Toast.makeText(this,
                                "Signup Failed ❌ " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
