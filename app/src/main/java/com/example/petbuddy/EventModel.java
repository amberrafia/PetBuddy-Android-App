package com.example.petbuddy;

import java.io.Serializable;

public class EventModel implements Serializable {
    private String eventId;
    private String title;
    private String description;
    private String category; // "pet_activity", "awareness_program", "community_event"
    private String date;
    private String time;
    private long dateTime; // Combined date and time as timestamp
    private String location;
    private String organizer;
    private String imageUrl;
    private int maxParticipants;
    private int currentParticipants;
    private boolean isRegistrationOpen;
    private long timestamp;
    private String contactInfo;
    private String requirements;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;

    public EventModel() {
        // Default constructor required for Firebase
    }

    public EventModel(String eventId, String title, String description, String category, 
                     String date, String time, String location, String organizer, 
                     String imageUrl, int maxParticipants, String contactInfo, String requirements) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.date = date;
        this.time = time;
        this.location = location;
        this.organizer = organizer;
        this.imageUrl = imageUrl;
        this.maxParticipants = maxParticipants;
        this.currentParticipants = 0;
        this.isRegistrationOpen = true;
        this.timestamp = System.currentTimeMillis();
        this.contactInfo = contactInfo;
        this.requirements = requirements;
    }

    // Getters and Setters
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public int getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(int maxParticipants) { this.maxParticipants = maxParticipants; }

    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }

    public boolean isRegistrationOpen() { return isRegistrationOpen; }
    public void setRegistrationOpen(boolean registrationOpen) { isRegistrationOpen = registrationOpen; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getContactInfo() { return contactInfo; }
    public void setContactInfo(String contactInfo) { this.contactInfo = contactInfo; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public long getDateTime() { return dateTime; }
    public void setDateTime(long dateTime) { this.dateTime = dateTime; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getCategoryDisplayName() {
        switch (category) {
            case "pet_activity":
                return "🎾 Pet Activity";
            case "awareness_program":
                return "📢 Awareness Program";
            case "community_event":
                return "🏘️ Community Event";
            default:
                return "📅 Event";
        }
    }

    public boolean hasAvailableSlots() {
        return currentParticipants < maxParticipants;
    }
}