package com.example.petbuddy;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getStringExtra("action");
        int notificationId = intent.getIntExtra("notificationId", 1001);
        
        // Dismiss the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(notificationId);
        
        if ("done".equals(action)) {
            Toast.makeText(context, "✅ Great job taking care of your pet!", Toast.LENGTH_SHORT).show();
        } else if ("snooze".equals(action)) {
            // Schedule snooze notification
            scheduleSnoozeNotification(context, notificationId);
            Toast.makeText(context, "⏰ Will remind you again in 15 minutes", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void scheduleSnoozeNotification(Context context, int originalNotificationId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, PetCareReminderReceiver.class);
        
        String type = originalNotificationId == 1001 ? "feeding" : "playing";
        intent.putExtra("type", type);
        intent.putExtra("notificationId", originalNotificationId);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, 
                originalNotificationId + 100, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for 15 minutes from now
        long triggerTime = System.currentTimeMillis() + (15 * 60 * 1000);
        
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
    }
}