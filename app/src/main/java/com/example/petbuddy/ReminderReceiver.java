package com.example.petbuddy;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Toast.makeText(context, "Reminder Triggered!", Toast.LENGTH_LONG).show();

        String title = intent.getStringExtra("title");
        String message = intent.getStringExtra("message");

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "pet_reminders";

        Uri soundUri = Uri.parse(
                "android.resource://" + context.getPackageName() + "/raw/alarm_sound"
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            AudioAttributes attributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Pet Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setSound(soundUri, attributes);
            channel.enableVibration(true);
            channel.enableLights(true);

            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setSound(soundUri)
                        .setAutoCancel(true)
                        .setCategory(NotificationCompat.CATEGORY_ALARM);
    }
}
