package com.example.petbuddy;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

/**
 * Diagnostic helper for notification system debugging
 */
public class NotificationDiagnosticHelper {
    
    private static final String TAG = "NotificationDiagnostic";
    private static final String CHANNEL_ID = "PetCareReminders";
    
    /**
     * Run comprehensive diagnostic check
     */
    public static String runDiagnostic(Context context) {
        StringBuilder report = new StringBuilder();
        report.append("🔧 NOTIFICATION SYSTEM DIAGNOSTIC REPORT\n");
        report.append("=====================================\n\n");
        
        // Basic system info
        report.append("📱 SYSTEM INFO:\n");
        report.append("• Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        report.append("• Device: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n\n");
        
        // Permission checks
        report.append("🔐 PERMISSION STATUS:\n");
        checkPermissions(context, report);
        report.append("\n");
        
        // Notification channel check
        report.append("🔔 NOTIFICATION CHANNEL:\n");
        checkNotificationChannel(context, report);
        report.append("\n");
        
        // AlarmManager check
        report.append("⏰ ALARM MANAGER:\n");
        checkAlarmManager(context, report);
        report.append("\n");
        
        // Battery optimization check
        report.append("🔋 BATTERY OPTIMIZATION:\n");
        checkBatteryOptimization(context, report);
        report.append("\n");
        
        // Recommendations
        report.append("💡 RECOMMENDATIONS:\n");
        addRecommendations(context, report);
        
        String reportString = report.toString();
        Log.d(TAG, reportString);
        return reportString;
    }
    
    private static void checkPermissions(Context context, StringBuilder report) {
        try {
            // Notification permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean hasNotificationPerm = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;
                report.append("• POST_NOTIFICATIONS: ").append(hasNotificationPerm ? "✅ GRANTED" : "❌ DENIED").append("\n");
            } else {
                report.append("• POST_NOTIFICATIONS: ✅ NOT REQUIRED (Android < 13)\n");
            }
            
            // System alert window permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDrawOverlays = android.provider.Settings.canDrawOverlays(context);
                report.append("• SYSTEM_ALERT_WINDOW: ").append(canDrawOverlays ? "✅ GRANTED" : "❌ DENIED").append("\n");
            } else {
                report.append("• SYSTEM_ALERT_WINDOW: ✅ NOT REQUIRED (Android < 6)\n");
            }
            
        } catch (Exception e) {
            report.append("• ERROR checking permissions: ").append(e.getMessage()).append("\n");
        }
    }
    
    private static void checkNotificationChannel(Context context, StringBuilder report) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
                if (notificationManager != null) {
                    NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                    if (channel != null) {
                        report.append("• Channel exists: ✅ YES\n");
                        report.append("• Importance: ").append(getImportanceString(channel.getImportance())).append("\n");
                        report.append("• Sound enabled: ").append(channel.getSound() != null ? "✅ YES" : "❌ NO").append("\n");
                        report.append("• Vibration enabled: ").append(channel.shouldVibrate() ? "✅ YES" : "❌ NO").append("\n");
                        report.append("• Bypass DND: ").append(channel.canBypassDnd() ? "✅ YES" : "❌ NO").append("\n");
                    } else {
                        report.append("• Channel exists: ❌ NO - CRITICAL ISSUE!\n");
                    }
                    
                    // Check if notifications are enabled for the app
                    boolean notificationsEnabled = notificationManager.areNotificationsEnabled();
                    report.append("• App notifications enabled: ").append(notificationsEnabled ? "✅ YES" : "❌ NO").append("\n");
                } else {
                    report.append("• NotificationManager: ❌ NULL\n");
                }
            } else {
                report.append("• Notification channels: ✅ NOT REQUIRED (Android < 8)\n");
            }
        } catch (Exception e) {
            report.append("• ERROR checking notification channel: ").append(e.getMessage()).append("\n");
        }
    }
    
    private static void checkAlarmManager(Context context, StringBuilder report) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                report.append("• AlarmManager available: ✅ YES\n");
                
                // Check if we can schedule exact alarms (Android 12+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    boolean canScheduleExact = alarmManager.canScheduleExactAlarms();
                    report.append("• Can schedule exact alarms: ").append(canScheduleExact ? "✅ YES" : "❌ NO").append("\n");
                } else {
                    report.append("• Exact alarms: ✅ AVAILABLE (Android < 12)\n");
                }
                
                // Test alarm scheduling
                try {
                    Intent testIntent = new Intent(context, PetCareReminderReceiver.class);
                    testIntent.putExtra("type", "test");
                    testIntent.putExtra("notificationId", 99999);
                    
                    PendingIntent testPendingIntent = PendingIntent.getBroadcast(
                            context, 
                            99999, 
                            testIntent, 
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                    );
                    
                    // Try to schedule a test alarm (then immediately cancel it)
                    long testTime = System.currentTimeMillis() + (24 * 60 * 60 * 1000); // 24 hours from now
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, testPendingIntent);
                    alarmManager.cancel(testPendingIntent); // Cancel immediately
                    
                    report.append("• Alarm scheduling test: ✅ SUCCESS\n");
                } catch (Exception e) {
                    report.append("• Alarm scheduling test: ❌ FAILED - ").append(e.getMessage()).append("\n");
                }
            } else {
                report.append("• AlarmManager available: ❌ NO - CRITICAL ISSUE!\n");
            }
        } catch (Exception e) {
            report.append("• ERROR checking AlarmManager: ").append(e.getMessage()).append("\n");
        }
    }
    
    private static void checkBatteryOptimization(Context context, StringBuilder report) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    String packageName = context.getPackageName();
                    boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName);
                    report.append("• Battery optimization ignored: ").append(isIgnoringBatteryOptimizations ? "✅ YES (good)" : "❌ NO (may affect reliability)").append("\n");
                    
                    // Check if device is in Doze mode
                    boolean isDeviceIdleMode = powerManager.isDeviceIdleMode();
                    report.append("• Device in Doze mode: ").append(isDeviceIdleMode ? "⚠️ YES" : "✅ NO").append("\n");
                } else {
                    report.append("• PowerManager: ❌ NULL\n");
                }
            } else {
                report.append("• Battery optimization: ✅ NOT APPLICABLE (Android < 6)\n");
            }
        } catch (Exception e) {
            report.append("• ERROR checking battery optimization: ").append(e.getMessage()).append("\n");
        }
    }
    
    private static void addRecommendations(Context context, StringBuilder report) {
        try {
            boolean hasIssues = false;
            
            // Check for common issues and provide specific recommendations
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean hasNotificationPerm = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;
                if (!hasNotificationPerm) {
                    report.append("1. ❗ Grant notification permission in Settings > Apps > PetBuddy > Permissions\n");
                    hasIssues = true;
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                boolean canDrawOverlays = android.provider.Settings.canDrawOverlays(context);
                if (!canDrawOverlays) {
                    report.append("2. ❗ Enable 'Display over other apps' in Settings > Apps > PetBuddy > Advanced\n");
                    hasIssues = true;
                }
                
                android.os.PowerManager powerManager = (android.os.PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager != null) {
                    String packageName = context.getPackageName();
                    boolean isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName);
                    if (!isIgnoringBatteryOptimizations) {
                        report.append("3. ❗ Disable battery optimization in Settings > Battery > Battery Optimization\n");
                        hasIssues = true;
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                    report.append("4. ❗ Enable 'Alarms & reminders' permission in Settings > Apps > PetBuddy\n");
                    hasIssues = true;
                }
            }
            
            if (!hasIssues) {
                report.append("✅ All permissions and settings look good!\n");
                report.append("If notifications still don't work:\n");
                report.append("• Try restarting the device\n");
                report.append("• Check manufacturer-specific battery settings\n");
                report.append("• Test on different Android versions/devices\n");
            }
            
        } catch (Exception e) {
            report.append("• ERROR generating recommendations: ").append(e.getMessage()).append("\n");
        }
    }
    
    private static String getImportanceString(int importance) {
        switch (importance) {
            case NotificationManager.IMPORTANCE_NONE: return "NONE (notifications blocked)";
            case NotificationManager.IMPORTANCE_MIN: return "MIN (no sound/vibration)";
            case NotificationManager.IMPORTANCE_LOW: return "LOW (no sound)";
            case NotificationManager.IMPORTANCE_DEFAULT: return "DEFAULT (sound)";
            case NotificationManager.IMPORTANCE_HIGH: return "HIGH (sound + heads-up)";
            case NotificationManager.IMPORTANCE_MAX: return "MAX (sound + heads-up + full-screen)";
            default: return "UNKNOWN (" + importance + ")";
        }
    }
    
    /**
     * Quick test to verify basic notification functionality
     */
    public static void runQuickTest(Context context) {
        try {
            Log.d(TAG, "🧪 Running quick notification test...");
            
            // Test 1: Show immediate notification
            androidx.core.app.NotificationCompat.Builder builder = new androidx.core.app.NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_pet)
                    .setContentTitle("🧪 Quick Test")
                    .setContentText("If you see this, basic notifications work!")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(88888, builder.build());
                Log.d(TAG, "✅ Quick test notification sent");
            } else {
                Log.e(TAG, "❌ NotificationManager is null");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Quick test failed: " + e.getMessage(), e);
        }
    }
}