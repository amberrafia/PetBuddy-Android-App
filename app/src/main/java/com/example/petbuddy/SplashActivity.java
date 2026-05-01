package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private ImageView imgPaw;
    private TextView txtAppName;
    private TextView txtSubtitle;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize views
        imgPaw = findViewById(R.id.imgPaw);
        txtAppName = findViewById(R.id.txtAppName);
        txtSubtitle = findViewById(R.id.txtSubtitle);
        progressBar = findViewById(R.id.progressBar);

        // Start animations
        startSplashAnimations();

        // Navigate after delay
        new Handler().postDelayed(() -> {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                startActivity(new Intent(SplashActivity.this, PetBuddyMainActivity.class));
            } else {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();
        }, 2500); // Increased delay to show animations
    }

    private void startSplashAnimations() {
        // Paw icon bounce animation
        ScaleAnimation pawBounce = new ScaleAnimation(
                0.0f, 1.0f, 0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        pawBounce.setDuration(800);
        pawBounce.setInterpolator(new android.view.animation.BounceInterpolator());
        imgPaw.startAnimation(pawBounce);

        // App name fade in with delay
        AlphaAnimation titleFadeIn = new AlphaAnimation(0.0f, 1.0f);
        titleFadeIn.setDuration(1000);
        titleFadeIn.setStartOffset(400);
        txtAppName.startAnimation(titleFadeIn);

        // Subtitle fade in with delay
        AlphaAnimation subtitleFadeIn = new AlphaAnimation(0.0f, 1.0f);
        subtitleFadeIn.setDuration(800);
        subtitleFadeIn.setStartOffset(800);
        txtSubtitle.startAnimation(subtitleFadeIn);

        // Progress bar fade in
        AlphaAnimation progressFadeIn = new AlphaAnimation(0.0f, 1.0f);
        progressFadeIn.setDuration(600);
        progressFadeIn.setStartOffset(1200);
        progressBar.startAnimation(progressFadeIn);

        // Set initial visibility
        txtAppName.setAlpha(0.0f);
        txtSubtitle.setAlpha(0.0f);
        progressBar.setAlpha(0.0f);
    }
}