package com.example.petbuddy;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AdminHelper {
    private static final String PREFS_NAME = "AdminPrefs";
    private static final String KEY_IS_ADMIN = "is_admin";
    
    // Admin email addresses (you can modify these)
    private static final String[] ADMIN_EMAILS = {
        "admin@petbuddy.com",
        "manager@petbuddy.com",
        "petbuddy.admin@gmail.com"
    };

    public static boolean isCurrentUserAdmin(Context context) {
        // First check SharedPreferences for manual admin setting (for testing)
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean manualAdminStatus = prefs.getBoolean(KEY_IS_ADMIN, false);
        
        if (manualAdminStatus) {
            return true;
        }

        // Then check Firebase user email
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return false;
        }

        String userEmail = currentUser.getEmail();
        if (userEmail == null) {
            return false;
        }

        // Check if user email is in admin list
        for (String adminEmail : ADMIN_EMAILS) {
            if (adminEmail.equalsIgnoreCase(userEmail)) {
                return true;
            }
        }

        return false;
    }

    public static void setAdminStatus(Context context, boolean isAdmin) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_IS_ADMIN, isAdmin).apply();
    }

    public static String getCurrentUserEmail() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        return currentUser != null ? currentUser.getEmail() : null;
    }

    public static String getAdminStatusInfo(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean manualAdminStatus = prefs.getBoolean(KEY_IS_ADMIN, false);
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String userEmail = currentUser != null ? currentUser.getEmail() : "Not logged in";
        
        boolean isEmailAdmin = false;
        if (userEmail != null && !userEmail.equals("Not logged in")) {
            for (String adminEmail : ADMIN_EMAILS) {
                if (adminEmail.equalsIgnoreCase(userEmail)) {
                    isEmailAdmin = true;
                    break;
                }
            }
        }
        
        return "Manual Admin: " + manualAdminStatus + 
               "\nUser Email: " + userEmail + 
               "\nEmail is Admin: " + isEmailAdmin + 
               "\nFinal Status: " + isCurrentUserAdmin(context);
    }

    public static boolean isAdminEmail(String email) {
        if (email == null) return false;
        
        for (String adminEmail : ADMIN_EMAILS) {
            if (adminEmail.equalsIgnoreCase(email)) {
                return true;
            }
        }
        return false;
    }
}