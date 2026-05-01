package com.example.petbuddy;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PetCarePopupActivity extends AppCompatActivity {
    
    private MediaPlayer mediaPlayer;
    private String type;
    private String petName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d("PetCarePopupActivity", "🚀 Popup activity starting...");
        
        // Enhanced lock screen and screen management for background launches
        setupScreenAndLockHandling();
        
        // Keep screen on while this activity is visible
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        setContentView(R.layout.activity_pet_care_popup);

        // Get data from intent
        type = getIntent().getStringExtra("type");
        petName = getIntent().getStringExtra("petName");
        String title = getIntent().getStringExtra("title");
        String message = getIntent().getStringExtra("message");

        Log.d("PetCarePopupActivity", "📋 Popup data - Type: " + type + ", Pet: " + petName);

        // Initialize views
        TextView titleText = findViewById(R.id.titleText);
        TextView messageText = findViewById(R.id.messageText);
        Button doneButton = findViewById(R.id.doneButton);
        Button snoozeButton = findViewById(R.id.snoozeButton);
        Button skipButton = findViewById(R.id.skipButton);

        // Set content
        titleText.setText(title);
        messageText.setText(message);

        // Play notification sound
        playNotificationSound();

        // Set button listeners
        doneButton.setOnClickListener(v -> {
            stopNotificationSound();
            String emoji = type.equals("feeding") ? "🍽️" : "🎾";
            Toast.makeText(this, emoji + " Great job taking care of " + petName + "!", Toast.LENGTH_SHORT).show();
            finish();
        });

        snoozeButton.setOnClickListener(v -> {
            stopNotificationSound();
            scheduleSnoozeNotification();
            Toast.makeText(this, "Will remind you again in 15 minutes", Toast.LENGTH_SHORT).show();
            finish();
        });

        skipButton.setOnClickListener(v -> {
            stopNotificationSound();
            finish();
        });
        
        Log.d("PetCarePopupActivity", "✅ Popup activity created successfully for " + type + " - " + petName);
    }
    
    /**
     * Enhanced screen and lock handling for reliable background popup display
     */
    private void setupScreenAndLockHandling() {
        try {
            // Make sure this activity can show over lock screen and turn on screen
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                
                // Request to dismiss keyguard
                android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, new android.app.KeyguardManager.KeyguardDismissCallback() {
                        @Override
                        public void onDismissSucceeded() {
                            Log.d("PetCarePopupActivity", "🔓 Keyguard dismissed successfully");
                        }
                        
                        @Override
                        public void onDismissError() {
                            Log.w("PetCarePopupActivity", "⚠️ Keyguard dismiss failed");
                        }
                        
                        @Override
                        public void onDismissCancelled() {
                            Log.w("PetCarePopupActivity", "⚠️ Keyguard dismiss cancelled");
                        }
                    });
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                
                // Dismiss keyguard for older API
                android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
            } else {
                // Fallback for older Android versions
                getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                );
            }
            
            // Additional flags for maximum visibility
            getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
            );
            
            Log.d("PetCarePopupActivity", "🔧 Enhanced screen and lock handling configured");
            
        } catch (Exception e) {
            Log.e("PetCarePopupActivity", "❌ Failed to setup screen and lock handling", e);
            // Fallback to basic flags
            try {
                getWindow().addFlags(
                    android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                    android.view.WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                );
                Log.d("PetCarePopupActivity", "🔧 Fallback screen flags applied");
            } catch (Exception fallbackError) {
                Log.e("PetCarePopupActivity", "❌ Even fallback screen flags failed", fallbackError);
            }
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(this, notification);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopNotificationSound() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private void scheduleSnoozeNotification() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, PetCareReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("notificationId", type.equals("feeding") ? 1001 : 1002);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                type.equals("feeding") ? 1101 : 1102, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for 15 minutes from now
        long triggerTime = System.currentTimeMillis() + (15 * 60 * 1000);
        
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopNotificationSound();
    }

    @Override
    public void onBackPressed() {
        // Prevent back button from closing without action
        Toast.makeText(this, "Please choose an action", Toast.LENGTH_SHORT).show();
        // Don't call super.onBackPressed() to prevent closing the popup
    }
}