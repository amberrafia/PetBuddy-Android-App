package com.example.petbuddy;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class PetCareReminderReceiver extends BroadcastReceiver {
    
    private static final String CHANNEL_ID = "PetCareReminders";
    private static final String TAG = "PetCareReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String type = intent.getStringExtra("type");
        int notificationId = intent.getIntExtra("notificationId", 1001);
        boolean shouldReschedule = intent.getBooleanExtra("reschedule", false);
        boolean testMode = intent.getBooleanExtra("testMode", false);
        
        if (testMode) {
            Log.d(TAG, "🧪 BUG CONDITION TEST ALARM TRIGGERED!");
            Log.d(TAG, "🔍 Testing if popup appears when app is closed/background");
        }
        
        Log.d(TAG, "🔔 ALARM TRIGGERED! Type: " + type + ", ID: " + notificationId + ", reschedule: " + shouldReschedule + ", testMode: " + testMode);
        Log.d(TAG, "📱 App process state: " + (isAppInForeground(context) ? "FOREGROUND" : "BACKGROUND"));
        
        // CRITICAL FIX: Ensure we have proper context and permissions
        try {
            // Request critical permissions if not granted
            ensureCriticalPermissions(context);
            
            // Try to get data from Firebase first, fallback to SharedPreferences
            loadReminderDataAndShowNotification(context, type, notificationId);
            
            // If this is a daily repeating notification, schedule the next one
            if (shouldReschedule) {
                scheduleNextDayNotification(context, type, notificationId);
                Log.d(TAG, "📅 Scheduled next day notification for " + type);
            }
            
            if (testMode) {
                Log.d(TAG, "🧪 BUG CONDITION TEST: If no popup appeared, bug is confirmed");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ CRITICAL ERROR in onReceive: " + e.getMessage(), e);
            // Fallback: Show basic notification even if popup fails
            showFallbackNotification(context, type, notificationId);
        }
    }
    
    /**
     * Ensure critical permissions are available for reliable notifications
     */
    private void ensureCriticalPermissions(Context context) {
        try {
            // Check notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                        != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "⚠️ POST_NOTIFICATIONS permission not granted");
                }
            }
            
            // Check system alert window permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(context)) {
                    Log.w(TAG, "⚠️ SYSTEM_ALERT_WINDOW permission not granted");
                }
            }
            
            // Check battery optimization
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                String packageName = context.getPackageName();
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    Log.w(TAG, "⚠️ Battery optimization is enabled - may affect reliability");
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error checking permissions", e);
        }
    }
    
    /**
     * Fallback notification when popup fails
     */
    private void showFallbackNotification(Context context, String type, int notificationId) {
        try {
            String title = type.equals("feeding") ? "🍽️ Feeding Time!" : "🎾 Play Time!";
            String message = "Time to " + (type.equals("feeding") ? "feed" : "play with") + " your pet!";
            
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_pet)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000})
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
                Log.d(TAG, "✅ Fallback notification shown for " + type);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Even fallback notification failed", e);
        }
    }
    
    private boolean isAppInForeground(Context context) {
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses != null) {
                final String packageName = context.getPackageName();
                for (android.app.ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                    if (appProcess.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
                            && appProcess.processName.equals(packageName)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking app foreground state", e);
        }
        return false;
    }
    
    private void loadReminderDataAndShowNotification(Context context, String type, int notificationId) {
        try {
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
                                showNotificationWithData(context, type, notificationId, reminder.getPetName());
                                return;
                            }
                        }
                        // Fallback to SharedPreferences
                        showNotificationWithSharedPrefs(context, type, notificationId);
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        Log.e(TAG, "Failed to load reminder from Firebase", error.toException());
                        showNotificationWithSharedPrefs(context, type, notificationId);
                    }
                });
            } else {
                // No authenticated user, use SharedPreferences
                showNotificationWithSharedPrefs(context, type, notificationId);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error accessing Firebase", e);
            showNotificationWithSharedPrefs(context, type, notificationId);
        }
    }
    
    private void showNotificationWithSharedPrefs(Context context, String type, int notificationId) {
        SharedPreferences prefs = context.getSharedPreferences("PetPrefs", Context.MODE_PRIVATE);
        String petName = prefs.getString("petName", "Your Pet");
        showNotificationWithData(context, type, notificationId, petName);
    }
    
    private void showNotificationWithData(Context context, String type, int notificationId, String petName) {
        String title, message, bigText;
        
        if ("feeding".equals(type)) {
            title = "🍽️ Feeding Time!";
            message = "Time to feed " + petName + "!";
            bigText = "It's time to feed " + petName + "!\n\n" +
                     "🥘 Don't forget:\n" +
                     "• Fresh water\n" +
                     "• Proper portion size\n" +
                     "• Clean feeding area";
        } else {
            title = "🎾 Play Time!";
            message = "Time to play with " + petName + "!";
            bigText = "Time to play with " + petName + "!\n\n" +
                     "🎮 Activity ideas:\n" +
                     "• Fetch or tug-of-war\n" +
                     "• Interactive toys\n" +
                     "• Training exercises";
        }

        // CRITICAL FIX: Enhanced popup launch with multiple strategies
        boolean popupLaunched = false;
        
        // Strategy 1: Try direct activity launch first
        try {
            Intent popupIntent = createPopupIntent(context, type, petName, title, bigText);
            context.startActivity(popupIntent);
            popupLaunched = true;
            Log.d(TAG, "✅ Strategy 1: Direct activity launch successful for " + type);
        } catch (Exception e) {
            Log.w(TAG, "⚠️ Strategy 1: Direct activity launch failed: " + e.getMessage());
        }
        
        // Strategy 2: System alert window overlay (if permission granted)
        if (!popupLaunched && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (android.provider.Settings.canDrawOverlays(context)) {
                    Intent overlayIntent = createPopupIntent(context, type, petName, title, bigText);
                    overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                                         Intent.FLAG_ACTIVITY_CLEAR_TASK |
                                         Intent.FLAG_ACTIVITY_NO_ANIMATION |
                                         Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                    context.startActivity(overlayIntent);
                    popupLaunched = true;
                    Log.d(TAG, "✅ Strategy 2: System alert window launch successful for " + type);
                } else {
                    Log.w(TAG, "⚠️ Strategy 2: Cannot draw overlays - permission not granted");
                }
            } catch (Exception e) {
                Log.w(TAG, "⚠️ Strategy 2: System alert window launch failed: " + e.getMessage());
            }
        }
        
        // Strategy 3: Enhanced notification with full-screen intent
        try {
            showEnhancedNotificationWithFullScreen(context, type, notificationId, petName, title, message, bigText, popupLaunched);
        } catch (Exception e) {
            Log.e(TAG, "❌ Strategy 3: Enhanced notification failed: " + e.getMessage());
            // Final fallback
            showFallbackNotification(context, type, notificationId);
        }
        
        // Strategy 4: Enhanced wake-up mechanism
        wakeUpScreenReliably(context, type);
        
        Log.d(TAG, "🔔 Notification strategies completed for " + type + " - " + petName + " (popup launched: " + popupLaunched + ")");
    }
    
    /**
     * Create popup intent with enhanced configuration
     */
    private Intent createPopupIntent(Context context, String type, String petName, String title, String message) {
        Intent popupIntent = new Intent(context, PetCarePopupActivity.class);
        popupIntent.putExtra("type", type);
        popupIntent.putExtra("petName", petName);
        popupIntent.putExtra("title", title);
        popupIntent.putExtra("message", message);
        
        // Enhanced flags for reliable background activity launch
        popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
                           Intent.FLAG_ACTIVITY_CLEAR_TOP | 
                           Intent.FLAG_ACTIVITY_SINGLE_TOP |
                           Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        
        return popupIntent;
    }
    
    /**
     * Enhanced notification with full-screen intent and maximum priority
     */
    private void showEnhancedNotificationWithFullScreen(Context context, String type, int notificationId, 
                                                       String petName, String title, String message, String bigText, boolean popupLaunched) {
        
        // Create content intent
        Intent popupIntent = createPopupIntent(context, type, petName, title, bigText);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                notificationId, 
                popupIntent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create full-screen intent with different ID for maximum reliability
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                notificationId + 2000, // Different ID from content intent
                popupIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Get notification sound
        Uri notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        // Build maximum-priority notification with enhanced full-screen intent
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_pet)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Maximum priority
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Use ALARM category for highest priority
                .setSound(notificationSound)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(fullScreenIntent, true) // Force full-screen popup
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setTimeoutAfter(5 * 60 * 1000) // Auto-dismiss after 5 minutes
                .addAction(R.drawable.ic_check, "Done", createActionIntent(context, "done", notificationId))
                .addAction(R.drawable.ic_snooze, "Snooze 15min", createActionIntent(context, "snooze", notificationId))
                .setDefaults(NotificationCompat.DEFAULT_ALL) // Use all default behaviors
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                // Additional flags for maximum reliability
                .setOnlyAlertOnce(false) // Always alert
                .setLocalOnly(false) // Allow on wearables
                .setColorized(true)
                .setColor(0xFFE8A7B4); // Pink theme color

        // Show notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(notificationId, builder.build());
            Log.d(TAG, "🔔 Enhanced notification with full-screen intent shown for " + type + " - " + petName);
        } else {
            Log.e(TAG, "❌ NotificationManager is null!");
        }
        
        // If popup wasn't launched directly, try delayed launch
        if (!popupLaunched) {
            scheduleDelayedPopupLaunch(context, popupIntent, type);
        }
    }
    
    /**
     * Schedule delayed popup launch as final fallback
     */
    private void scheduleDelayedPopupLaunch(Context context, Intent popupIntent, String type) {
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    context.startActivity(popupIntent);
                    Log.d(TAG, "✅ Delayed popup launch successful for " + type);
                } catch (Exception e) {
                    Log.e(TAG, "❌ Delayed popup launch failed: " + e.getMessage());
                }
            }, 2000); // 2 second delay
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule delayed popup launch: " + e.getMessage());
        }
    }
    
    /**
     * Enhanced popup launch using multiple strategies for maximum reliability
     */
    private void launchPopupWithMultipleStrategies(Context context, Intent popupIntent, String type) {
        // This method is now integrated into showNotificationWithData
        // Keeping for backward compatibility but functionality moved
        Log.d(TAG, "launchPopupWithMultipleStrategies called - functionality integrated into main notification method");
    }
    
    /**
     * Enhanced screen wake-up mechanism for maximum reliability
     */
    private void wakeUpScreenReliably(Context context, String type) {
        try {
            android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                // Create a wake lock to turn on the screen
                android.os.PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    android.os.PowerManager.SCREEN_BRIGHT_WAKE_LOCK | 
                    android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP | 
                    android.os.PowerManager.ON_AFTER_RELEASE, 
                    "PetBuddy:NotificationWakeUp"
                );
                
                // Acquire wake lock for longer duration
                wakeLock.acquire(30000); // 30 seconds
                
                // Release after a delay to avoid battery drain
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        if (wakeLock.isHeld()) {
                            wakeLock.release();
                            Log.d(TAG, "💡 Wake lock released for " + type);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error releasing wake lock", e);
                    }
                }, 30000);
                
                Log.d(TAG, "💡 Enhanced screen wake-up triggered for " + type);
                
                // Additional screen management
                try {
                    // Try to dismiss keyguard if possible
                    android.app.KeyguardManager keyguardManager = (android.app.KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
                    if (keyguardManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Note: This requires the activity to handle keyguard dismissal
                        Log.d(TAG, "💡 Keyguard manager available for dismissal");
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Keyguard dismissal not available: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to wake up screen: " + e.getMessage());
        }
    }
    
    private void scheduleNextDayNotification(Context context, String type, int notificationId) {
        // Schedule the same notification for tomorrow
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PetCareReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("reschedule", true);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for same time tomorrow
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled next day notification for " + type);
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule next day notification", e);
        }
    }
    
    private PendingIntent createActionIntent(Context context, String action, int notificationId) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.putExtra("action", action);
        intent.putExtra("notificationId", notificationId);
        
        return PendingIntent.getBroadcast(
                context, 
                notificationId + (action.equals("done") ? 1000 : 2000), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}