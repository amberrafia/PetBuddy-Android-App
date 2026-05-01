package com.example.petbuddy;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class SimpleNotificationService extends Service {
    private static final String TAG = "SimpleNotificationService";
    private static final String CHANNEL_ID = "PetCareService";
    private static final int FOREGROUND_ID = 1000;
    
    private Handler handler;
    private Runnable feedingRunnable;
    private Runnable playingRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createServiceNotificationChannel();
        Log.d(TAG, "Service created");
    }
    
    private void createServiceNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Pet Care Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background service for pet care reminders");
            
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Pet Care Reminders Active")
                .setContentText("Monitoring your pet care schedule")
                .setSmallIcon(R.drawable.ic_notification_pet)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        
        startForeground(FOREGROUND_ID, notification);
        Log.d(TAG, "Started foreground service");
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start as foreground service for better reliability
        startForegroundService();
        
        if (intent != null) {
            String action = intent.getStringExtra("action");
            if ("schedule".equals(action)) {
                String feedingTime = intent.getStringExtra("feedingTime");
                String playingTime = intent.getStringExtra("playingTime");
                String petName = intent.getStringExtra("petName");
                boolean feedingEnabled = intent.getBooleanExtra("feedingEnabled", false);
                boolean playingEnabled = intent.getBooleanExtra("playingEnabled", false);
                
                Log.d(TAG, "Service restarted with new schedule");
                scheduleNotifications(feedingTime, playingTime, petName, feedingEnabled, playingEnabled);
            } else if ("stop".equals(action)) {
                Log.d(TAG, "Service stop requested");
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        return START_STICKY; // Restart if killed
    }
    
    private void scheduleNotifications(String feedingTime, String playingTime, String petName, 
                                     boolean feedingEnabled, boolean playingEnabled) {
        // Cancel existing runnables
        if (feedingRunnable != null) {
            handler.removeCallbacks(feedingRunnable);
        }
        if (playingRunnable != null) {
            handler.removeCallbacks(playingRunnable);
        }
        
        Log.d(TAG, "Scheduling notifications - Feeding: " + feedingEnabled + " at " + feedingTime + 
                   ", Playing: " + playingEnabled + " at " + playingTime + " for " + petName);
        
        if (feedingEnabled && feedingTime != null) {
            long feedingDelay = calculateDelay(feedingTime);
            Log.d(TAG, "Feeding delay calculated: " + (feedingDelay/1000) + " seconds (" + (feedingDelay/60000) + " minutes)");
            
            feedingRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Triggering feeding notification for " + petName);
                    showNotificationPopup("feeding", petName);
                    // Reschedule for next day
                    long nextDayDelay = 24 * 60 * 60 * 1000; // 24 hours
                    handler.postDelayed(this, nextDayDelay);
                    Log.d(TAG, "Feeding notification rescheduled for next day");
                }
            };
            handler.postDelayed(feedingRunnable, feedingDelay);
            Log.d(TAG, "Feeding notification scheduled for " + feedingTime + " (delay: " + (feedingDelay/1000) + " seconds)");
        }
        
        if (playingEnabled && playingTime != null) {
            long playingDelay = calculateDelay(playingTime);
            Log.d(TAG, "Playing delay calculated: " + (playingDelay/1000) + " seconds (" + (playingDelay/60000) + " minutes)");
            
            playingRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Triggering playing notification for " + petName);
                    showNotificationPopup("playing", petName);
                    // Reschedule for next day
                    long nextDayDelay = 24 * 60 * 60 * 1000; // 24 hours
                    handler.postDelayed(this, nextDayDelay);
                    Log.d(TAG, "Playing notification rescheduled for next day");
                }
            };
            handler.postDelayed(playingRunnable, playingDelay);
            Log.d(TAG, "Playing notification scheduled for " + playingTime + " (delay: " + (playingDelay/1000) + " seconds)");
        }
        
        // For immediate testing - schedule test notifications in 1 minute
        handler.postDelayed(() -> {
            Log.d(TAG, "Test notification triggered after 1 minute");
            if (feedingEnabled) {
                showNotificationPopup("feeding", petName + " (Test)");
            }
            if (playingEnabled) {
                // Schedule playing test 30 seconds later
                handler.postDelayed(() -> {
                    showNotificationPopup("playing", petName + " (Test)");
                }, 30000);
            }
        }, 60000); // 1 minute for testing
    }
    
    private long calculateDelay(String timeString) {
        try {
            Log.d(TAG, "Calculating delay for time: " + timeString);
            
            String[] parts = timeString.split(" ");
            String[] timeParts = parts[0].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            // Convert to 24-hour format
            if (parts.length > 1) {
                if (parts[1].equalsIgnoreCase("PM") && hour != 12) {
                    hour += 12;
                } else if (parts[1].equalsIgnoreCase("AM") && hour == 12) {
                    hour = 0;
                }
            }
            
            Log.d(TAG, "Parsed time: " + hour + ":" + minute + " (24-hour format)");
            
            Calendar calendar = Calendar.getInstance();
            Calendar targetCalendar = Calendar.getInstance();
            
            targetCalendar.set(Calendar.HOUR_OF_DAY, hour);
            targetCalendar.set(Calendar.MINUTE, minute);
            targetCalendar.set(Calendar.SECOND, 0);
            targetCalendar.set(Calendar.MILLISECOND, 0);
            
            long currentTime = calendar.getTimeInMillis();
            long targetTime = targetCalendar.getTimeInMillis();
            
            Log.d(TAG, "Current time: " + calendar.getTime());
            Log.d(TAG, "Target time today: " + targetCalendar.getTime());
            
            // If time has passed today, schedule for tomorrow
            if (targetTime <= currentTime) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1);
                targetTime = targetCalendar.getTimeInMillis();
                Log.d(TAG, "Time has passed today, scheduling for tomorrow: " + targetCalendar.getTime());
            }
            
            long delay = targetTime - currentTime;
            Log.d(TAG, "Final delay: " + (delay/1000) + " seconds (" + (delay/60000) + " minutes)");
            
            return delay;
        } catch (Exception e) {
            Log.e(TAG, "Failed to calculate delay for time: " + timeString, e);
            return 60000; // Default to 1 minute
        }
    }
    
    private void showNotificationPopup(String type, String petName) {
        try {
            String title, message;
            
            if ("feeding".equals(type)) {
                title = "🍽️ Feeding Time!";
                message = "It's time to feed " + petName + "!\n\n🥘 Don't forget:\n• Fresh water\n• Proper portion size\n• Clean feeding area";
            } else {
                title = "🎾 Play Time!";
                message = "Time to play with " + petName + "!\n\n🎮 Activity ideas:\n• Fetch or tug-of-war\n• Interactive toys\n• Training exercises";
            }
            
            Intent popupIntent = new Intent(this, PetCarePopupActivity.class);
            popupIntent.putExtra("type", type);
            popupIntent.putExtra("petName", petName);
            popupIntent.putExtra("title", title);
            popupIntent.putExtra("message", message);
            popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(popupIntent);
            Log.d(TAG, "Popup launched for " + type + " - " + petName);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to show notification popup", e);
        }
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null) {
            if (feedingRunnable != null) {
                handler.removeCallbacks(feedingRunnable);
            }
            if (playingRunnable != null) {
                handler.removeCallbacks(playingRunnable);
            }
        }
        stopForeground(true);
        Log.d(TAG, "Service destroyed");
    }
}