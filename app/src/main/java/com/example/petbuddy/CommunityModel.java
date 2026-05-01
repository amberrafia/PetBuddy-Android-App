package com.example.petbuddy;

public class CommunityModel {

    public String userId;
    public String name;
    public String email;
    public String phone;
    public long joinedAt;
    public String status;

    public CommunityModel() {
        // Required empty constructor for Firebase
    }

    public CommunityModel(String name, String email, String phone, long joinedAt) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinedAt = joinedAt;
        this.status = "active";
    }

    public CommunityModel(String userId, String name, String email, String phone, long joinedAt, String status) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.joinedAt = joinedAt;
        this.status = status;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
