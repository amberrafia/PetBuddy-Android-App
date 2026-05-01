package com.example.petbuddy;

public class ReminderModel {

    public String id;
    public String title;
    public String message;

    public ReminderModel() {
        // Required empty constructor
    }

    public ReminderModel(String id, String title, String message) {
        this.id = id;
        this.title = title;
        this.message = message;
    }
}
