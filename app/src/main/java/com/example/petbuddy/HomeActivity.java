package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private Button logoutButton;
    private SharedPreferencesHelper prefsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        

        prefsHelper = new SharedPreferencesHelper(this);

        welcomeText = findViewById(R.id.welcomeText);
        logoutButton = findViewById(R.id.logoutButton);

        String email = prefsHelper.getUserEmail();
        welcomeText.setText("Welcome to Pet Buddy!\n" + email);

        logoutButton.setOnClickListener(v -> {
            prefsHelper.logout(); // ✅ FIXED
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        });
    }
}
