package com.example.petbuddy;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesHelper {

    private static final String PREF_NAME = "PetBuddyPrefs";
    private static final String KEY_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "userEmail";
    private static final String KEY_NAME = "userName";
    private static final String KEY_PHONE = "userPhone";
    private static final String KEY_PROFILE_PHOTO = "userProfilePhoto";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public void setLoggedIn(boolean value) {
        editor.putBoolean(KEY_LOGGED_IN, value).apply();
    }

    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setUserEmail(String email) {
        editor.putString(KEY_EMAIL, email).apply();
    }

    public String getUserEmail() {
        return prefs.getString(KEY_EMAIL, "");
    }

    public void setUserName(String name) {
        editor.putString(KEY_NAME, name).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_NAME, "");
    }

    public void setUserPhone(String phone) {
        editor.putString(KEY_PHONE, phone).apply();
    }

    public String getUserPhone() {
        return prefs.getString(KEY_PHONE, "");
    }

    // Profile photo methods
    public void setUserProfilePhoto(String encodedPhoto) {
        editor.putString(KEY_PROFILE_PHOTO, encodedPhoto).apply();
    }

    public String getUserProfilePhoto() {
        return prefs.getString(KEY_PROFILE_PHOTO, null);
    }

    public void removeUserProfilePhoto() {
        editor.remove(KEY_PROFILE_PHOTO).apply();
    }

    public void logout() {
        editor.clear().apply();
    }

    // ✅ ADD THESE METHODS HERE
    public void saveUser(String name, String email) {
        editor.putString(KEY_NAME, name);
        editor.putString(KEY_EMAIL, email);
        editor.apply();
    }

    public void clearUser() {
        editor.clear().apply();
    }

    // Generic getString method
    public String getString(String key, String defaultValue) {
        return prefs.getString(key, defaultValue);
    }

    // Generic edit method
    public SharedPreferences.Editor edit() {
        return editor;
    }
}
