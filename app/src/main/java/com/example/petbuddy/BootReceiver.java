package com.example.petbuddy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Received action: " + action);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || 
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(action) ||
            Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            
            Log.d(TAG, "Device booted or app updated, restarting notification service");
            restartNotificationService(context);
        }
    }
    
    private void restartNotificationService(Context context) {
        try {
            // Try to get data from Firebase first
            FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            
            if (currentUser != null) {
                String userId = currentUser.getUid();
                DatabaseReference remindersRef = FirebaseManager.getInstance().getDatabase()
                        .getReference("petReminders").child(userId);
                
                remindersRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            PetReminderModel reminder = snapshot.getValue(PetReminderModel.class);
                            if (reminder != null) {
                                startServiceWithData(context, reminder);
                                return;
                            }
                        }
                        // Fallback to SharedPreferences
                        startServiceWithSharedPrefs(context);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load reminder from Firebase", error.toException());
                        startServiceWithSharedPrefs(context);
                    }
                });
            } else {
                // No authenticated user, use SharedPreferences
                startServiceWithSharedPrefs(context);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing Firebase", e);
            startServiceWithSharedPrefs(context);
        }
    }
    
    private void startServiceWithSharedPrefs(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("PetPrefs", Context.MODE_PRIVATE);
        String petName = prefs.getString("petName", "Your Pet");
        String feedTime = prefs.getString("feedTime", "8:00 AM");
        String playTime = prefs.getString("playTime", "6:00 PM");
        boolean feedEnabled = prefs.getBoolean("feedEnabled", true);
        boolean playEnabled = prefs.getBoolean("playEnabled", true);
        
        PetReminderModel reminder = new PetReminderModel(petName, feedTime, playTime, feedEnabled, playEnabled, "guest");
        startServiceWithData(context, reminder);
    }
    
    private void startServiceWithData(Context context, PetReminderModel reminder) {
        try {
            Intent serviceIntent = new Intent(context, SimpleNotificationService.class);
            serviceIntent.putExtra("action", "schedule");
            serviceIntent.putExtra("feedingTime", reminder.getFeedingTime());
            serviceIntent.putExtra("playingTime", reminder.getPlayingTime());
            serviceIntent.putExtra("petName", reminder.getPetName());
            serviceIntent.putExtra("feedingEnabled", reminder.isFeedingEnabled());
            serviceIntent.putExtra("playingEnabled", reminder.isPlayingEnabled());
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Notification service restarted after boot");
        } catch (Exception e) {
            Log.e(TAG, "Failed to restart notification service", e);
        }
    }
}