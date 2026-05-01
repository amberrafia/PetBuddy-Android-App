package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;


import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailInput, passwordInput;
    private Button loginButton;
    private TextView signupLink;

    private DatabaseHelper dbHelper;
    private SharedPreferencesHelper prefsHelper;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();
        if (mAuth != null) {
            Toast.makeText(this, "Firebase Connected ✅", Toast.LENGTH_SHORT).show();
        }


        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupLink = findViewById(R.id.signupLink);

        dbHelper = new DatabaseHelper(this);
        prefsHelper = new SharedPreferencesHelper(this);

        loginButton.setOnClickListener(v -> loginUser());

        signupLink.setOnClickListener(v ->
                startActivity(new Intent(LoginActivity.this, SignupActivity.class))
        );
    }
    @Override
    protected void onStart() {
        super.onStart();

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    private void loginUser() {

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(this,
                                "Firebase Login Successful ✅",
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(this, PetBuddyMainActivity.class));
                        finish();

                    } else {

                        String error = task.getException().getMessage();

                        Toast.makeText(this,
                                "Error: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }


}



