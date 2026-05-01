package com.example.petbuddy;

public class EventRegistrationModel {
    private String registrationId;
    private String eventId;
    private String userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String petName;
    private String petType;
    private String specialRequirements;
    private long registrationTimestamp;
    private String status; // "registered", "cancelled", "attended"

    public EventRegistrationModel() {
        // Default constructor required for Firebase
    }

    public EventRegistrationModel(String eventId, String userId, String userName, String userEmail, 
                                String userPhone, String petName, String petType, String specialRequirements) {
        this.eventId = eventId;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
        this.userPhone = userPhone;
        this.petName = petName;
        this.petType = petType;
        this.specialRequirements = specialRequirements;
        this.registrationTimestamp = System.currentTimeMillis();
        this.status = "registered";
    }

    // Getters and Setters
    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }

    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }

    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }

    public String getPetType() { return petType; }
    public void setPetType(String petType) { this.petType = petType; }

    public String getSpecialRequirements() { return specialRequirements; }
    public void setSpecialRequirements(String specialRequirements) { this.specialRequirements = specialRequirements; }

    public long getRegistrationTimestamp() { return registrationTimestamp; }
    public void setRegistrationTimestamp(long registrationTimestamp) { this.registrationTimestamp = registrationTimestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}