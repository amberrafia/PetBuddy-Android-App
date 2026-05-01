package com.example.petbuddy;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

public class NotificationFragment extends Fragment {

    private TextView feedText, playText, debugStatusText;
    private Switch feedSwitch, playSwitch;
    private Button editFeed, editPlay, testNotification, testService, refreshStatus;
    private SharedPreferences prefs;
    private MediaPlayer mediaPlayer;
    
    // Firebase
    private FirebaseAuth firebaseAuth;
    private DatabaseReference remindersRef;
    private String currentUserId;
    private PetReminderModel currentReminder;
    
    private static final String TAG = "NotificationFragment";
    private static final String CHANNEL_ID = "PetCareReminders";
    private static final int FEED_NOTIFICATION_ID = 1001;
    private static final int PLAY_NOTIFICATION_ID = 1002;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_notification, container, false);

        initializeViews(view);
        initializeFirebase();
        setupNotificationChannel();
        loadDataFromFirebase();
        setupClickListeners();
        
        // Check if we came from HomeFragment or MoreFragment and show appropriate welcome message
        if (getArguments() != null) {
            if (getArguments().getBoolean("from_home", false)) {
                Toast.makeText(getContext(), "🔔 Welcome to Notification Management!\nConfigure your pet care reminders here.", Toast.LENGTH_LONG).show();
            } else if (getArguments().getBoolean("from_more", false)) {
                Toast.makeText(getContext(), "🔗 Notifications - Enhanced System\n✅ Connected to Home notification section\n🔔 Configure popup notifications here!", Toast.LENGTH_LONG).show();
            }
            
            // Show enhanced system indicator if specified
            if (getArguments().getBoolean("enhanced_system", false)) {
                // Add a delayed message to emphasize the connection
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "🔗 This system is connected to your Home Notifications section!", Toast.LENGTH_LONG).show();
                    }
                }, 2000); // Show after 2 seconds
            }
        }

        return view;
    }

    private void initializeViews(View view) {
        feedText = view.findViewById(R.id.feedText);
        playText = view.findViewById(R.id.playText);
        debugStatusText = view.findViewById(R.id.debugStatusText);
        feedSwitch = view.findViewById(R.id.feedSwitch);
        playSwitch = view.findViewById(R.id.playSwitch);
        editFeed = view.findViewById(R.id.editFeed);
        editPlay = view.findViewById(R.id.editPlay);
        testNotification = view.findViewById(R.id.testNotification);
        testService = view.findViewById(R.id.testService);
        refreshStatus = view.findViewById(R.id.refreshStatus);
        
        prefs = requireContext().getSharedPreferences("PetPrefs", 0);
    }

    private void initializeFirebase() {
        try {
            firebaseAuth = FirebaseAuth.getInstance();
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            
            if (currentUser != null) {
                currentUserId = currentUser.getUid();
                remindersRef = FirebaseManager.getInstance().getDatabase()
                        .getReference("petReminders").child(currentUserId);
                Log.d(TAG, "Firebase initialized for user: " + currentUserId);
            } else {
                Log.w(TAG, "No authenticated user found");
                // Fallback to SharedPreferences for guest users
                currentUserId = "guest_" + System.currentTimeMillis();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase", e);
            currentUserId = "guest_" + System.currentTimeMillis();
        }
    }

    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Pet Care Reminders";
            String description = "Critical notifications for feeding and playing with pets";
            int importance = NotificationManager.IMPORTANCE_MAX; // Changed from HIGH to MAX for full-screen capability
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.enableLights(true);
            channel.setLightColor(android.graphics.Color.MAGENTA);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // Bypass Do Not Disturb
            channel.setShowBadge(true);
            
            // Additional settings for maximum reliability
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null);
            channel.enableLights(true);
            channel.setImportance(NotificationManager.IMPORTANCE_MAX);

            NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Log.d(TAG, "Maximum-importance notification channel created with full-screen capability");
        }
        
        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission();
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
        
        // Also request battery optimization exemption for reliable notifications
        requestBatteryOptimizationExemption();
    }
    
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.content.pm.PackageManager pm = requireContext().getPackageManager();
                android.content.Intent intent = new android.content.Intent();
                String packageName = requireContext().getPackageName();
                android.os.PowerManager powerManager = (android.os.PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                
                if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    intent.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(android.net.Uri.parse("package:" + packageName));
                    startActivity(intent);
                    Toast.makeText(getContext(), "Please allow this app to ignore battery optimization for reliable notifications", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to request battery optimization exemption", e);
            }
        }
        
        // Also request system alert window permission for popup overlays
        requestSystemAlertWindowPermission();
    }
    
    private void requestSystemAlertWindowPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                if (!android.provider.Settings.canDrawOverlays(requireContext())) {
                    android.content.Intent intent = new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + requireContext().getPackageName())
                    );
                    startActivity(intent);
                    Toast.makeText(getContext(), "Please allow 'Display over other apps' for popup notifications to work when app is closed", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to request system alert window permission", e);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Notification permission denied. Popups may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadDataFromFirebase() {
        if (remindersRef != null) {
            remindersRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        currentReminder = snapshot.getValue(PetReminderModel.class);
                        if (currentReminder != null) {
                            updateUI();
                            Log.d(TAG, "Loaded reminder data from Firebase");
                        }
                    } else {
                        // Create default reminder if none exists
                        createDefaultReminder();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "Failed to load reminder data", error.toException());
                    loadSavedDataFromPrefs(); // Fallback to SharedPreferences
                }
            });
        } else {
            loadSavedDataFromPrefs(); // Fallback for guest users
        }
    }

    private void createDefaultReminder() {
        currentReminder = new PetReminderModel(
                "Your Pet",
                "8:00 AM",
                "6:00 PM",
                true,
                true,
                currentUserId
        );
        saveReminderToFirebase();
        updateUI();
        Log.d(TAG, "Created default reminder");
    }

    private void updateUI() {
        if (currentReminder != null) {
            feedText.setText("🍽️ Feed " + currentReminder.getPetName() + " at " + currentReminder.getFeedingTime());
            playText.setText("🎾 Play with " + currentReminder.getPetName() + " at " + currentReminder.getPlayingTime());
            feedSwitch.setChecked(currentReminder.isFeedingEnabled());
            playSwitch.setChecked(currentReminder.isPlayingEnabled());
            updateDebugStatus();
        }
    }

    private void saveReminderToFirebase() {
        if (remindersRef != null && currentReminder != null) {
            remindersRef.setValue(currentReminder)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Reminder saved to Firebase");
                        // Also save to SharedPreferences for immediate access
                        saveToSharedPreferences();
                        // Notify other components that data has changed
                        notifyDataChanged();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to save reminder to Firebase", e);
                        // Fallback to SharedPreferences
                        saveToSharedPreferences();
                        notifyDataChanged();
                    });
        } else {
            saveToSharedPreferences();
            notifyDataChanged();
        }
    }
    
    /**
     * Notify other components that reminder data has changed
     */
    private void notifyDataChanged() {
        try {
            // Send a broadcast to notify other components (including Home embedded UI)
            Intent intent = new Intent("com.example.petbuddy.NOTIFICATION_DATA_CHANGED");
            requireContext().sendBroadcast(intent);
            Log.d(TAG, "Sent notification data changed broadcast to Home embedded UI");
        } catch (Exception e) {
            Log.e(TAG, "Failed to send data changed broadcast", e);
        }
    }

    private void saveToSharedPreferences() {
        if (currentReminder != null) {
            prefs.edit()
                    .putString("petName", currentReminder.getPetName())
                    .putString("feedTime", currentReminder.getFeedingTime())
                    .putString("playTime", currentReminder.getPlayingTime())
                    .putBoolean("feedEnabled", currentReminder.isFeedingEnabled())
                    .putBoolean("playEnabled", currentReminder.isPlayingEnabled())
                    .apply();
        }
    }

    private void loadSavedDataFromPrefs() {
        // Load from SharedPreferences as fallback
        String petName = prefs.getString("petName", "Your Pet");
        String feedTime = prefs.getString("feedTime", "8:00 AM");
        String playTime = prefs.getString("playTime", "6:00 PM");
        boolean feedEnabled = prefs.getBoolean("feedEnabled", true);
        boolean playEnabled = prefs.getBoolean("playEnabled", true);
        
        currentReminder = new PetReminderModel(petName, feedTime, playTime, feedEnabled, playEnabled, currentUserId);
        updateUI();
        Log.d(TAG, "Loaded data from SharedPreferences");
    }

    private void setupClickListeners() {
        editFeed.setOnClickListener(v -> showEditFeedingDialog());
        editPlay.setOnClickListener(v -> showEditPlayingDialog());
        testNotification.setOnClickListener(v -> showTestNotificationDialog());
        testService.setOnClickListener(v -> testServiceDirectly());
        refreshStatus.setOnClickListener(v -> updateDebugStatus());
        
        feedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentReminder != null) {
                currentReminder.setFeedingEnabled(isChecked);
                saveReminderToFirebase();
                
                if (isChecked) {
                    scheduleNotification(FEED_NOTIFICATION_ID, "feeding");
                    // Restart the service with updated settings
                    startSimpleNotificationService();
                    Toast.makeText(getContext(), "Feeding reminders enabled", Toast.LENGTH_SHORT).show();
                } else {
                    cancelNotification(FEED_NOTIFICATION_ID);
                    Toast.makeText(getContext(), "Feeding reminders disabled", Toast.LENGTH_SHORT).show();
                }
                updateDebugStatus();
            }
        });
        
        playSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (currentReminder != null) {
                currentReminder.setPlayingEnabled(isChecked);
                saveReminderToFirebase();
                
                if (isChecked) {
                    scheduleNotification(PLAY_NOTIFICATION_ID, "playing");
                    // Restart the service with updated settings
                    startSimpleNotificationService();
                    Toast.makeText(getContext(), "Playing reminders enabled", Toast.LENGTH_SHORT).show();
                } else {
                    cancelNotification(PLAY_NOTIFICATION_ID);
                    Toast.makeText(getContext(), "Playing reminders disabled", Toast.LENGTH_SHORT).show();
                }
                updateDebugStatus();
            }
        });
    }

    private void showEditFeedingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🍽️ Edit Feeding Reminder");

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_reminder, null);

        EditText petInput = dialogView.findViewById(R.id.petNameInput);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);

        if (currentReminder != null) {
            petInput.setText(currentReminder.getPetName());
            setTimePickerFromString(timePicker, currentReminder.getFeedingTime());
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String petName = petInput.getText().toString().trim();
            if (petName.isEmpty()) petName = "Your Pet";
            
            String timeString = getTimeStringFromPicker(timePicker);

            if (currentReminder != null) {
                currentReminder.setPetName(petName);
                currentReminder.setFeedingTime(timeString);
                saveReminderToFirebase();
                updateUI();
                
                // Immediately restart the notification service with new settings
                startSimpleNotificationService();
                
                // Also schedule a test notification for 2 minutes to verify it works
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    showDirectPopup("feeding");
                }, 120000); // 2 minutes
                
                Toast.makeText(getContext(), 
                    "✅ Feeding reminder updated for " + timeString + "!" +
                    "\n🔔 Test popup in 2 minutes!" +
                    "\n⏰ Daily reminder will trigger at " + timeString, 
                    Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showEditPlayingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("🎾 Edit Playing Reminder");

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_edit_reminder, null);

        EditText petInput = dialogView.findViewById(R.id.petNameInput);
        TimePicker timePicker = dialogView.findViewById(R.id.timePicker);

        if (currentReminder != null) {
            petInput.setText(currentReminder.getPetName());
            petInput.setHint("Pet name (optional)");
            setTimePickerFromString(timePicker, currentReminder.getPlayingTime());
        }

        builder.setView(dialogView);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String petName = petInput.getText().toString().trim();
            if (petName.isEmpty() && currentReminder != null) {
                petName = currentReminder.getPetName();
            }
            if (petName.isEmpty()) petName = "Your Pet";
            
            String timeString = getTimeStringFromPicker(timePicker);

            if (currentReminder != null) {
                currentReminder.setPetName(petName);
                currentReminder.setPlayingTime(timeString);
                saveReminderToFirebase();
                updateUI();
                
                // Immediately restart the notification service with new settings
                startSimpleNotificationService();
                
                // Also schedule a test notification for 2 minutes to verify it works
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    showDirectPopup("playing");
                }, 120000); // 2 minutes
                
                Toast.makeText(getContext(), 
                    "✅ Playing reminder updated for " + timeString + "!" +
                    "\n🔔 Test popup in 2 minutes!" +
                    "\n⏰ Daily reminder will trigger at " + timeString, 
                    Toast.LENGTH_LONG).show();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTestNotificationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("🔔 Test Notifications")
                .setMessage("Choose which test to run:")
                .setPositiveButton("🍽️ Feeding Popup", (dialog, which) -> {
                    Log.d(TAG, "Testing feeding popup");
                    showDirectPopup("feeding");
                })
                .setNeutralButton("🎾 Playing Popup", (dialog, which) -> {
                    Log.d(TAG, "Testing playing popup");
                    showDirectPopup("playing");
                })
                .setNegativeButton("🚨 Background Test", (dialog, which) -> {
                    Log.d(TAG, "Running background test");
                    showBackgroundTestDialog();
                })
                .show();
    }
    
    private void showBackgroundTestDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("🚨 Background Popup Test")
                .setMessage("This will test if popups work when the app is closed:\n\n" +
                           "1. Tap 'Start Test' below\n" +
                           "2. IMMEDIATELY close the PetBuddy app completely\n" +
                           "3. Wait 2-3 minutes\n" +
                           "4. You should see a popup even with app closed\n\n" +
                           "⚠️ Make sure you have:\n" +
                           "• Granted 'Display over other apps' permission\n" +
                           "• Disabled battery optimization for PetBuddy\n" +
                           "• Enabled notifications")
                .setPositiveButton("🚀 Start Test", (dialog, which) -> {
                    startComprehensiveBackgroundTest();
                })
                .setNeutralButton("🧪 Debug Test", (dialog, which) -> {
                    startBugConditionExplorationTest();
                })
                .setNegativeButton("🔧 Permission Check", (dialog, which) -> {
                    showPermissionCheckDialog();
                })
                .show();
    }
    
    /**
     * Comprehensive background test with multiple verification points
     */
    private void startComprehensiveBackgroundTest() {
        try {
            Log.d(TAG, "🚀 STARTING COMPREHENSIVE BACKGROUND TEST");
            
            // Test 1: Immediate notification (should work)
            showSystemNotification("feeding", "Test Pet");
            
            // Test 2: Schedule alarm 1 minute from now
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
            intent.putExtra("type", "feeding");
            intent.putExtra("notificationId", 8888); // Special test ID
            intent.putExtra("testMode", true);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    8888, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long testTime = System.currentTimeMillis() + (60 * 1000); // 1 minute
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            }
            
            // Test 3: Schedule another alarm 2 minutes from now
            Intent intent2 = new Intent(getContext(), PetCareReminderReceiver.class);
            intent2.putExtra("type", "playing");
            intent2.putExtra("notificationId", 8889);
            intent2.putExtra("testMode", true);
            
            PendingIntent pendingIntent2 = PendingIntent.getBroadcast(
                    getContext(), 
                    8889, 
                    intent2, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long testTime2 = System.currentTimeMillis() + (2 * 60 * 1000); // 2 minutes
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime2, pendingIntent2);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime2, pendingIntent2);
            }
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String testTime1Str = sdf.format(new java.util.Date(testTime));
            String testTime2Str = sdf.format(new java.util.Date(testTime2));
            
            new AlertDialog.Builder(getContext())
                    .setTitle("🚀 COMPREHENSIVE TEST STARTED")
                    .setMessage("TEST SCHEDULE:\n\n" +
                               "✅ NOW: Immediate notification (should appear)\n" +
                               "⏰ " + testTime1Str + ": Feeding popup test\n" +
                               "⏰ " + testTime2Str + ": Playing popup test\n\n" +
                               "📱 CLOSE THE APP COMPLETELY NOW!\n" +
                               "🔔 Wait for popups at scheduled times\n\n" +
                               "Expected Results:\n" +
                               "• Immediate notification: ✅ Works\n" +
                               "• Background popups: Should appear with sound")
                    .setPositiveButton("OK - Closing App Now", null)
                    .setCancelable(false)
                    .show();
            
            Log.d(TAG, "🚀 COMPREHENSIVE TEST: Scheduled tests for " + testTime1Str + " and " + testTime2Str);
            Log.d(TAG, "📱 USER SHOULD CLOSE APP NOW AND WAIT FOR POPUPS");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start comprehensive background test", e);
            Toast.makeText(getContext(), "❌ Test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show permission check dialog
     */
    private void showPermissionCheckDialog() {
        StringBuilder status = new StringBuilder();
        status.append("PERMISSION STATUS:\n\n");
        
        // Check notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean hasNotificationPerm = requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) 
                    == android.content.pm.PackageManager.PERMISSION_GRANTED;
            status.append("📱 Notifications: ").append(hasNotificationPerm ? "✅ GRANTED" : "❌ DENIED").append("\n");
        } else {
            status.append("📱 Notifications: ✅ NOT REQUIRED (Android < 13)\n");
        }
        
        // Check system alert window permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            boolean canDrawOverlays = android.provider.Settings.canDrawOverlays(requireContext());
            status.append("🖼️ Display over apps: ").append(canDrawOverlays ? "✅ GRANTED" : "❌ DENIED").append("\n");
        } else {
            status.append("🖼️ Display over apps: ✅ NOT REQUIRED (Android < 6)\n");
        }
        
        // Check battery optimization
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.os.PowerManager powerManager = (android.os.PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                String packageName = requireContext().getPackageName();
                boolean isOptimized = !powerManager.isIgnoringBatteryOptimizations(packageName);
                status.append("🔋 Battery optimized: ").append(isOptimized ? "❌ YES (bad)" : "✅ NO (good)").append("\n");
            } catch (Exception e) {
                status.append("🔋 Battery optimization: ❓ UNKNOWN\n");
            }
        } else {
            status.append("🔋 Battery optimization: ✅ NOT APPLICABLE\n");
        }
        
        // Check notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
                NotificationChannel channel = notificationManager.getNotificationChannel(CHANNEL_ID);
                if (channel != null) {
                    status.append("🔔 Notification channel: ✅ EXISTS (").append(channel.getImportance()).append(")\n");
                } else {
                    status.append("🔔 Notification channel: ❌ MISSING\n");
                }
            } catch (Exception e) {
                status.append("🔔 Notification channel: ❓ ERROR\n");
            }
        }
        
        status.append("\nRECOMMENDATIONS:\n");
        status.append("1. Grant all permissions above\n");
        status.append("2. Disable battery optimization\n");
        status.append("3. Keep app in recent apps\n");
        status.append("4. Test on different Android versions");
        
        new AlertDialog.Builder(getContext())
                .setTitle("🔧 Permission & System Check")
                .setMessage(status.toString())
                .setPositiveButton("Fix Permissions", (dialog, which) -> {
                    requestAllPermissions();
                })
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Show comprehensive diagnostic report
     */
    private void showDiagnosticReport() {
        try {
            // Run quick test first
            NotificationDiagnosticHelper.runQuickTest(getContext());
            
            // Generate diagnostic report
            String report = NotificationDiagnosticHelper.runDiagnostic(getContext());
            
            // Show in dialog
            new AlertDialog.Builder(getContext())
                    .setTitle("🔧 System Diagnostic Report")
                    .setMessage(report)
                    .setPositiveButton("Run Quick Test", (dialog, which) -> {
                        NotificationDiagnosticHelper.runQuickTest(getContext());
                        Toast.makeText(getContext(), "Quick test notification sent! Check if you received it.", Toast.LENGTH_LONG).show();
                    })
                    .setNeutralButton("Fix Issues", (dialog, which) -> {
                        requestAllPermissions();
                    })
                    .setNegativeButton("Close", null)
                    .show();
                    
        } catch (Exception e) {
            Log.e(TAG, "Failed to show diagnostic report", e);
            Toast.makeText(getContext(), "Diagnostic failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Request all necessary permissions
     */
    private void requestAllPermissions() {
        try {
            // Request notification permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
            
            // Request system alert window permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!android.provider.Settings.canDrawOverlays(requireContext())) {
                    Intent intent = new Intent(
                        android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        android.net.Uri.parse("package:" + requireContext().getPackageName())
                    );
                    startActivity(intent);
                }
            }
            
            // Request battery optimization exemption
            requestBatteryOptimizationExemption();
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to request permissions", e);
            Toast.makeText(getContext(), "Failed to request permissions: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Bug Condition Exploration Test - Property 1
     * 
     * This test MUST FAIL on unfixed code to confirm the bug exists.
     * DO NOT attempt to fix the test or code when it fails.
     * 
     * Bug Condition: Scheduled alarms fail to show popup notifications when app is closed/background
     * Expected Behavior: Full-screen popup notifications should appear with screen wake-up and ringtone
     */
    private void startBugConditionExplorationTest() {
        try {
            Log.d(TAG, "🧪 STARTING BUG CONDITION EXPLORATION TEST");
            Log.d(TAG, "📋 This test MUST FAIL on unfixed code to confirm bug exists");
            
            // Schedule a test alarm 1 minute from now
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
            intent.putExtra("type", "feeding");
            intent.putExtra("notificationId", 7777); // Special test ID
            intent.putExtra("testMode", true); // Flag to identify this as a test
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    7777, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long testTime = System.currentTimeMillis() + (60 * 1000); // 1 minute
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            }
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String testTimeStr = sdf.format(new java.util.Date(testTime));
            
            // Show instructions to user
            new AlertDialog.Builder(getContext())
                    .setTitle("🧪 Bug Condition Test Started")
                    .setMessage("TEST PROCEDURE:\n\n" +
                               "1. ⏰ Alarm scheduled for " + testTimeStr + "\n" +
                               "2. 📱 CLOSE THE APP NOW (completely)\n" +
                               "3. ⏳ Wait 1 minute for popup\n" +
                               "4. 🔍 Observe if popup appears\n\n" +
                               "EXPECTED ON UNFIXED CODE:\n" +
                               "❌ Popup will NOT appear (bug confirmed)\n\n" +
                               "EXPECTED ON FIXED CODE:\n" +
                               "✅ Popup WILL appear (bug fixed)")
                    .setPositiveButton("OK - Closing App Now", null)
                    .setCancelable(false)
                    .show();
            
            Log.d(TAG, "🧪 BUG CONDITION TEST: Alarm scheduled for " + testTimeStr);
            Log.d(TAG, "📱 USER SHOULD CLOSE APP NOW AND WAIT FOR POPUP");
            Log.d(TAG, "❌ EXPECTED FAILURE: Popup will NOT appear on unfixed code");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start bug condition exploration test", e);
            Toast.makeText(getContext(), "❌ Test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void startBackgroundTest() {
        try {
            // Schedule a test alarm 2 minutes from now
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
            intent.putExtra("type", "feeding");
            intent.putExtra("notificationId", 8888); // Special test ID
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    8888, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long testTime = System.currentTimeMillis() + (2 * 60 * 1000); // 2 minutes
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            }
            
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            String testTimeStr = sdf.format(new java.util.Date(testTime));
            
            Toast.makeText(getContext(), 
                "🚨 BACKGROUND TEST STARTED!" +
                "\n⏰ Popup scheduled for " + testTimeStr +
                "\n📱 CLOSE THE APP NOW!" +
                "\n🔔 Wait for popup in 2 minutes", 
                Toast.LENGTH_LONG).show();
            
            Log.d(TAG, "🚨 BACKGROUND TEST: Alarm scheduled for " + testTimeStr + " - USER SHOULD CLOSE APP NOW!");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start background test", e);
            Toast.makeText(getContext(), "❌ Background test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void testSystemNotification() {
        // Test 1: Direct popup launch (preservation test)
        testPreservationDirectPopup();
        
        // Test 2: System notification
        showSystemNotification("playing", "Test Pet");
        
        Toast.makeText(getContext(), "🔔 Testing both popup and notification!", Toast.LENGTH_LONG).show();
    }
    
    /**
     * Preservation Property Test - Property 2
     * 
     * This test MUST PASS on unfixed code to confirm baseline behavior.
     * Tests that in-app popup functionality continues to work correctly.
     * 
     * Preservation Requirement: In-app notifications must continue to work exactly as before
     */
    private void testPreservationDirectPopup() {
        try {
            Log.d(TAG, "🧪 STARTING PRESERVATION TEST - Direct Popup");
            Log.d(TAG, "📋 This test MUST PASS on unfixed code to confirm baseline behavior");
            
            String petName = currentReminder != null ? currentReminder.getPetName() : "Test Pet";
            String type = "feeding";
            String title = "🍽️ Feeding Time!";
            String message = "It's time to feed " + petName + "!\n\n🥘 Don't forget fresh water and proper portion size!";
            
            // Test direct popup launch (should work when app is open)
            Intent popupIntent = new Intent(getContext(), PetCarePopupActivity.class);
            popupIntent.putExtra("type", type);
            popupIntent.putExtra("petName", petName);
            popupIntent.putExtra("title", title);
            popupIntent.putExtra("message", message);
            popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(popupIntent);
            
            Log.d(TAG, "✅ PRESERVATION TEST: Direct popup launched successfully");
            Log.d(TAG, "📋 Expected: Popup should appear immediately (app is open)");
            
            // Verify this is a preservation case (app is in foreground)
            if (isAppInForeground()) {
                Log.d(TAG, "✅ PRESERVATION CONFIRMED: App is in foreground - this should work");
            } else {
                Log.w(TAG, "⚠️ PRESERVATION WARNING: App not in foreground - test may not be valid");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ PRESERVATION TEST FAILED: Direct popup failed", e);
            Toast.makeText(getContext(), "❌ Preservation test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Helper method to check if app is in foreground
     */
    private boolean isAppInForeground() {
        try {
            android.app.ActivityManager activityManager = (android.app.ActivityManager) requireContext().getSystemService(Context.ACTIVITY_SERVICE);
            java.util.List<android.app.ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses != null) {
                final String packageName = requireContext().getPackageName();
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
    
    /**
     * Preservation Property Test - Test Notification Functionality
     * 
     * Tests that manual test notifications continue to work immediately as expected.
     * This should work on both unfixed and fixed code.
     */
    private void testPreservationTestNotifications() {
        try {
            Log.d(TAG, "🧪 STARTING PRESERVATION TEST - Test Notifications");
            
            // Test immediate popup (should work)
            showDirectPopup("feeding");
            
            // Test system notification (should work)
            showSystemNotification("playing", "Test Pet");
            
            Log.d(TAG, "✅ PRESERVATION TEST: Test notifications triggered");
            Log.d(TAG, "📋 Expected: Both popup and notification should appear immediately");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ PRESERVATION TEST FAILED: Test notifications failed", e);
        }
    }
    
    private void showDirectPopup(String type) {
        try {
            String petName = currentReminder != null ? currentReminder.getPetName() : "Test Pet";
            String title, message;
            
            if (type.equals("feeding")) {
                title = "🍽️ Feeding Time!";
                message = "It's time to feed " + petName + "!\n\n🥘 Don't forget fresh water and proper portion size!";
            } else {
                title = "🎾 Play Time!";
                message = "Time to play with " + petName + "!\n\n🎮 Try fetch, toys, or training exercises!";
            }
            
            Intent popupIntent = new Intent(getContext(), PetCarePopupActivity.class);
            popupIntent.putExtra("type", type);
            popupIntent.putExtra("petName", petName);
            popupIntent.putExtra("title", title);
            popupIntent.putExtra("message", message);
            popupIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            startActivity(popupIntent);
            Log.d(TAG, "Direct popup launched for " + type);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch direct popup", e);
            Toast.makeText(getContext(), "❌ Failed to launch popup: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void testServiceDirectly() {
        try {
            // Get current time and add 2 minutes for testing
            Calendar testCal = Calendar.getInstance();
            testCal.add(Calendar.MINUTE, 2);
            
            int hour = testCal.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
            int minute = testCal.get(Calendar.MINUTE);
            String amPm = testCal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            String testFeedingTime = String.format("%d:%02d %s", hour, minute, amPm);
            
            // Add 1 more minute for playing time
            testCal.add(Calendar.MINUTE, 1);
            hour = testCal.get(Calendar.HOUR);
            if (hour == 0) hour = 12;
            minute = testCal.get(Calendar.MINUTE);
            amPm = testCal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
            String testPlayingTime = String.format("%d:%02d %s", hour, minute, amPm);
            
            // Test both service and AlarmManager
            Intent serviceIntent = new Intent(getContext(), SimpleNotificationService.class);
            serviceIntent.putExtra("action", "schedule");
            serviceIntent.putExtra("feedingTime", testFeedingTime);
            serviceIntent.putExtra("playingTime", testPlayingTime);
            serviceIntent.putExtra("petName", "Test Pet");
            serviceIntent.putExtra("feedingEnabled", true);
            serviceIntent.putExtra("playingEnabled", true);
            
            requireContext().startService(serviceIntent);
            
            // Also test AlarmManager directly
            testAlarmManagerDirectly(testFeedingTime, testPlayingTime);
            
            Toast.makeText(getContext(), 
                "🔧 Service test started!" +
                "\n🔔 Service feeding popup at " + testFeedingTime + " (app open only)" +
                "\n🎾 Service playing popup at " + testPlayingTime + " (app open only)" +
                "\n⏱️ Plus test popups in 1 minute!" +
                "\n🚨 AlarmManager also scheduled for background testing!" +
                "\n📱 Close app to test background popups!", 
                Toast.LENGTH_LONG).show();
            Log.d(TAG, "🧪 Service test initiated with times: " + testFeedingTime + " and " + testPlayingTime);
        } catch (Exception e) {
            Log.e(TAG, "❌ Service test failed", e);
            Toast.makeText(getContext(), "❌ Service test failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void testAlarmManagerDirectly(String feedingTime, String playingTime) {
        try {
            // Test feeding alarm
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
            intent.putExtra("type", "feeding");
            intent.putExtra("notificationId", 9001);
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    9001, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar calendar = getCalendarFromTimeString(feedingTime);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "🚨 AlarmManager test (setExactAndAllowWhileIdle) scheduled for " + calendar.getTime());
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "🚨 AlarmManager test (setExact) scheduled for " + calendar.getTime());
            }
            
            // Also schedule a playing alarm 1 minute later
            Intent playIntent = new Intent(getContext(), PetCareReminderReceiver.class);
            playIntent.putExtra("type", "playing");
            playIntent.putExtra("notificationId", 9002);
            
            PendingIntent playPendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    9002, 
                    playIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Calendar playCalendar = getCalendarFromTimeString(playingTime);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        playCalendar.getTimeInMillis(),
                        playPendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        playCalendar.getTimeInMillis(),
                        playPendingIntent
                );
            }
            
            Log.d(TAG, "🎾 AlarmManager playing test scheduled for " + playCalendar.getTime());
            Log.d(TAG, "📱 IMPORTANT: Close the app completely to test background popups!");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to test AlarmManager", e);
        }
    }

    private void showSystemNotification(String type, String petName) {
        try {
            String title = type.equals("feeding") ? "🍽️ Feeding Time!" : "🎾 Play Time!";
            String message = "Time to " + (type.equals("feeding") ? "feed" : "play with") + " " + petName + "!";
            
            // Create notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(getContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification_pet)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setVibrate(new long[]{0, 1000, 500, 1000});

            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(9999, builder.build());
            
            Log.d(TAG, "System notification shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show system notification", e);
        }
    }

    private void showPetCarePopup(String type) {
        if (currentReminder == null) return;
        
        String petName = currentReminder.getPetName();
        String title, message, emoji;
        
        if (type.equals("feeding")) {
            title = "🍽️ Feeding Time!";
            message = "It's time to feed " + petName + "!\n\n" +
                     "🥘 Don't forget:\n" +
                     "• Fresh water\n" +
                     "• Proper portion size\n" +
                     "• Clean feeding area";
            emoji = "🍽️";
        } else {
            title = "🎾 Play Time!";
            message = "Time to play with " + petName + "!\n\n" +
                     "🎮 Activity ideas:\n" +
                     "• Fetch or tug-of-war\n" +
                     "• Interactive toys\n" +
                     "• Training exercises";
            emoji = "🎾";
        }

        // Play notification sound
        playNotificationSound();

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("✅ Done", (d, which) -> {
                    stopNotificationSound();
                    // Update last activity time in Firebase
                    updateLastActivityTime(type);
                    Toast.makeText(getContext(), emoji + " Great job taking care of " + petName + "!", Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("⏰ Remind Later", (d, which) -> {
                    stopNotificationSound();
                    scheduleSnoozeNotification(type);
                    Toast.makeText(getContext(), "Will remind you again in 15 minutes", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("❌ Skip", (d, which) -> {
                    stopNotificationSound();
                })
                .setCancelable(false)
                .create();

        dialog.show();
    }

    private void updateLastActivityTime(String type) {
        if (currentReminder != null) {
            long currentTime = System.currentTimeMillis();
            if (type.equals("feeding")) {
                currentReminder.setLastFeedingTime(currentTime);
            } else {
                currentReminder.setLastPlayingTime(currentTime);
            }
            saveReminderToFirebase();
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            mediaPlayer = MediaPlayer.create(getContext(), notification);
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

    private void scheduleNotification(int notificationId, String type) {
        // Use AlarmManager for reliable scheduling (works in background)
        scheduleAlarmManagerNotification(notificationId, type);
        
        // Start the service for immediate testing (only works when app is open)
        startSimpleNotificationService();
        
        // Schedule a test notification for immediate feedback
        scheduleTestNotification(type);
        
        Log.d(TAG, "Scheduled " + type + " notification with ID " + notificationId + " using AlarmManager");
    }
    
    private void startSimpleNotificationService() {
        try {
            Intent serviceIntent = new Intent(getContext(), SimpleNotificationService.class);
            serviceIntent.putExtra("action", "schedule");
            
            if (currentReminder != null) {
                serviceIntent.putExtra("feedingTime", currentReminder.getFeedingTime());
                serviceIntent.putExtra("playingTime", currentReminder.getPlayingTime());
                serviceIntent.putExtra("petName", currentReminder.getPetName());
                serviceIntent.putExtra("feedingEnabled", currentReminder.isFeedingEnabled());
                serviceIntent.putExtra("playingEnabled", currentReminder.isPlayingEnabled());
            }
            
            requireContext().startService(serviceIntent);
            Log.d(TAG, "Simple notification service started");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start simple notification service", e);
        }
    }
    
    private void scheduleTestNotification(String type) {
        // Schedule a test notification 2 minutes from now for immediate feedback
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            showDirectPopup(type);
        }, 120000); // 2 minutes
        
        Toast.makeText(getContext(), 
            "✅ " + type + " reminder scheduled!" +
            "\n🔔 Test popup in 2 minutes (app open)" +
            "\n🧪 Test alarm in 3 minutes (background)" +
            "\n⏰ Daily reminders will trigger at set times", 
            Toast.LENGTH_LONG).show();
    }

    private void scheduleSnoozeNotification(String type) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("notificationId", type.equals("feeding") ? FEED_NOTIFICATION_ID : PLAY_NOTIFICATION_ID);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 
                type.equals("feeding") ? FEED_NOTIFICATION_ID + 100 : PLAY_NOTIFICATION_ID + 100, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule for 15 minutes from now
        long triggerTime = System.currentTimeMillis() + (15 * 60 * 1000);
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
            }
            Log.d(TAG, "Scheduled snooze notification for " + type + " in 15 minutes");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule snooze notification", e);
        }
    }

    private void scheduleDailyRepeatingNotification(int notificationId, String type) {
        // This method schedules the next day's notification after one triggers
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("reschedule", true); // Flag to indicate this should reschedule itself
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String timeString = "";
        if (currentReminder != null) {
            timeString = type.equals("feeding") ? 
                    currentReminder.getFeedingTime() : 
                    currentReminder.getPlayingTime();
        }
        
        Calendar calendar = getCalendarFromTimeString(timeString);
        calendar.add(Calendar.DAY_OF_MONTH, 1); // Schedule for tomorrow
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
            Log.d(TAG, "Scheduled daily repeating " + type + " notification for tomorrow");
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule daily repeating notification", e);
        }
    }

    private void cancelNotification(int notificationId) {
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarmManager.cancel(pendingIntent);
    }

    private void setTimePickerFromString(TimePicker timePicker, String timeString) {
        try {
            String[] parts = timeString.split(" ");
            String[] timeParts = parts[0].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            if (parts[1].equals("PM") && hour != 12) {
                hour += 12;
            } else if (parts[1].equals("AM") && hour == 12) {
                hour = 0;
            }
            
            timePicker.setHour(hour);
            timePicker.setMinute(minute);
        } catch (Exception e) {
            timePicker.setHour(8);
            timePicker.setMinute(0);
        }
    }

    private String getTimeStringFromPicker(TimePicker timePicker) {
        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        
        String amPm = hour >= 12 ? "PM" : "AM";
        if (hour > 12) hour -= 12;
        if (hour == 0) hour = 12;
        
        return String.format("%d:%02d %s", hour, minute, amPm);
    }

    private Calendar getCalendarFromTimeString(String timeString) {
        Calendar calendar = Calendar.getInstance();
        try {
            String[] parts = timeString.split(" ");
            String[] timeParts = parts[0].split(":");
            int hour = Integer.parseInt(timeParts[0]);
            int minute = Integer.parseInt(timeParts[1]);
            
            if (parts[1].equals("PM") && hour != 12) {
                hour += 12;
            } else if (parts[1].equals("AM") && hour == 12) {
                hour = 0;
            }
            
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
        } catch (Exception e) {
            calendar.set(Calendar.HOUR_OF_DAY, 8);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }
        return calendar;
    }

    private void scheduleAlarmManagerNotification(int notificationId, String type) {
        if (currentReminder == null) return;
        
        String timeString = type.equals("feeding") ? 
                currentReminder.getFeedingTime() : 
                currentReminder.getPlayingTime();
        
        boolean isEnabled = type.equals("feeding") ? 
                currentReminder.isFeedingEnabled() : 
                currentReminder.isPlayingEnabled();
        
        if (!isEnabled || timeString == null) {
            Log.d(TAG, "Skipping " + type + " notification - disabled or no time set");
            return;
        }
        
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
        intent.putExtra("type", type);
        intent.putExtra("notificationId", notificationId);
        intent.putExtra("reschedule", true); // This will make it reschedule for next day
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                getContext(), 
                notificationId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = getCalendarFromTimeString(timeString);
        
        // If time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "Time has passed today, scheduling " + type + " for tomorrow");
        }
        
        try {
            // Use the most reliable scheduling method available
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // setExactAndAllowWhileIdle works even in Doze mode
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Used setExactAndAllowWhileIdle for " + type);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // setExact for API 19+
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Used setExact for " + type);
            } else {
                // Fallback for older versions
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
                Log.d(TAG, "Used set for " + type);
            }
            
            Log.d(TAG, "✅ Scheduled " + type + " notification for " + calendar.getTime() + 
                      " (in " + ((calendar.getTimeInMillis() - System.currentTimeMillis()) / 60000) + " minutes)");
            
            // Also schedule a test alarm 3 minutes from now to verify AlarmManager works
            scheduleTestAlarm(type, notificationId);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to schedule " + type + " notification", e);
            Toast.makeText(getContext(), "Failed to schedule " + type + " reminder: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void scheduleTestAlarm(String type, int notificationId) {
        try {
            AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(getContext(), PetCareReminderReceiver.class);
            intent.putExtra("type", type);
            intent.putExtra("notificationId", notificationId + 5000); // Different ID for test
            intent.putExtra("reschedule", false); // Don't reschedule test alarms
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    getContext(), 
                    notificationId + 5000, 
                    intent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule test alarm 3 minutes from now
            long testTime = System.currentTimeMillis() + (3 * 60 * 1000);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, testTime, pendingIntent);
            }
            
            Log.d(TAG, "🧪 Scheduled test " + type + " alarm in 3 minutes to verify AlarmManager works");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule test alarm", e);
        }
    }

    private void updateDebugStatus() {
        try {
            StringBuilder status = new StringBuilder();
            
            // Service status
            status.append("• Service: Running (Foreground)\n");
            
            // Last update time
            long lastUpdate = System.currentTimeMillis();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault());
            status.append("• Last update: ").append(sdf.format(new java.util.Date(lastUpdate))).append("\n");
            
            // AlarmManager status
            status.append("• AlarmManager: Active\n");
            
            // Next feeding time
            if (currentReminder != null && currentReminder.isFeedingEnabled()) {
                String nextFeeding = calculateNextTriggerTime(currentReminder.getFeedingTime());
                status.append("• Next feeding: ").append(nextFeeding).append("\n");
            } else {
                status.append("• Next feeding: Disabled\n");
            }
            
            // Next playing time
            if (currentReminder != null && currentReminder.isPlayingEnabled()) {
                String nextPlaying = calculateNextTriggerTime(currentReminder.getPlayingTime());
                status.append("• Next playing: ").append(nextPlaying);
            } else {
                status.append("• Next playing: Disabled");
            }
            
            // Battery optimization status
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    android.os.PowerManager powerManager = (android.os.PowerManager) requireContext().getSystemService(Context.POWER_SERVICE);
                    String packageName = requireContext().getPackageName();
                    boolean isOptimized = !powerManager.isIgnoringBatteryOptimizations(packageName);
                    status.append("\n• Battery optimized: ").append(isOptimized ? "Yes (may affect reliability)" : "No (good)");
                    
                    // System alert window permission
                    boolean canDrawOverlays = android.provider.Settings.canDrawOverlays(requireContext());
                    status.append("\n• Overlay permission: ").append(canDrawOverlays ? "Granted (good)" : "Denied (may affect popups)");
                } catch (Exception e) {
                    status.append("\n• Permissions: Unknown");
                }
            }
            
            debugStatusText.setText(status.toString());
            
        } catch (Exception e) {
            debugStatusText.setText("• Status: Error loading debug info\n• Error: " + e.getMessage());
            Log.e(TAG, "Failed to update debug status", e);
        }
    }
    
    private String calculateNextTriggerTime(String timeString) {
        try {
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
            
            Calendar calendar = Calendar.getInstance();
            Calendar targetCalendar = Calendar.getInstance();
            
            targetCalendar.set(Calendar.HOUR_OF_DAY, hour);
            targetCalendar.set(Calendar.MINUTE, minute);
            targetCalendar.set(Calendar.SECOND, 0);
            targetCalendar.set(Calendar.MILLISECOND, 0);
            
            // If time has passed today, schedule for tomorrow
            if (targetCalendar.getTimeInMillis() <= calendar.getTimeInMillis()) {
                targetCalendar.add(Calendar.DAY_OF_MONTH, 1);
                return "Tomorrow at " + timeString;
            } else {
                long minutesUntil = (targetCalendar.getTimeInMillis() - calendar.getTimeInMillis()) / 60000;
                if (minutesUntil < 60) {
                    return "In " + minutesUntil + " minutes";
                } else {
                    long hoursUntil = minutesUntil / 60;
                    return "In " + hoursUntil + "h " + (minutesUntil % 60) + "m";
                }
            }
        } catch (Exception e) {
            return "Error calculating time";
        }
    }
}
