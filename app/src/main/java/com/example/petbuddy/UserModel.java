package com.example.petbuddy;

public class UserModel {
    public String userId;
    public String name;
    public String email;
    public boolean isOnline;
    public long lastSeen;

    public UserModel() {
        // Required empty constructor for Firebase
    }

    public UserModel(String userId, String name, String email, boolean isOnline) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.isOnline = isOnline;
        this.lastSeen = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public long getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
}
