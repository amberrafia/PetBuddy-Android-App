package com.example.petbuddy;

public class PetReminderModel {
    private String id;
    private String petName;
    private String feedingTime;
    private String playingTime;
    private boolean feedingEnabled;
    private boolean playingEnabled;
    private String userId;
    private long createdAt;
    private long lastFeedingTime;
    private long lastPlayingTime;

    public PetReminderModel() {
        // Default constructor required for Firebase
    }

    public PetReminderModel(String petName, String feedingTime, String playingTime, 
                           boolean feedingEnabled, boolean playingEnabled, String userId) {
        this.petName = petName;
        this.feedingTime = feedingTime;
        this.playingTime = playingTime;
        this.feedingEnabled = feedingEnabled;
        this.playingEnabled = playingEnabled;
        this.userId = userId;
        this.createdAt = System.currentTimeMillis();
        this.lastFeedingTime = 0;
        this.lastPlayingTime = 0;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getFeedingTime() { return feedingTime; }
    public void setFeedingTime(String feedingTime) { this.feedingTime = feedingTime; }

    public String getPlayingTime() { return playingTime; }
    public void setPlayingTime(String playingTime) { this.playingTime = playingTime; }

    public boolean isFeedingEnabled() { return feedingEnabled; }
    public void setFeedingEnabled(boolean feedingEnabled) { this.feedingEnabled = feedingEnabled; }

    public boolean isPlayingEnabled() { return playingEnabled; }
    public void setPlayingEnabled(boolean playingEnabled) { this.playingEnabled = playingEnabled; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getLastFeedingTime() { return lastFeedingTime; }
    public void setLastFeedingTime(long lastFeedingTime) { this.lastFeedingTime = lastFeedingTime; }

    public long getLastPlayingTime() { return lastPlayingTime; }
    public void setLastPlayingTime(long lastPlayingTime) { this.lastPlayingTime = lastPlayingTime; }
}