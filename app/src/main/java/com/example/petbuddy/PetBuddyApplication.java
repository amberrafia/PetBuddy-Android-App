package com.example.petbuddy;

import android.app.Application;
import android.util.Log;

/**
 * Custom Application class for PetBuddy app initialization
 */
public class PetBuddyApplication extends Application {
    private static final String TAG = "PetBuddyApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        Log.d(TAG, "Initializing PetBuddy Application");
        
        // Initialize Firebase services
        FirebaseManager.getInstance().initialize(this);
        
        // Apply saved theme
        ThemeHelper.applyTheme(ThemeHelper.getSavedTheme(this));
        
        Log.d(TAG, "PetBuddy Application initialized successfully");
    }
}