package com.example.petbuddy;

public class Clinic {

    public String name;
    public String location;
    public String phone;
    public String imageUrl;

    public Clinic() {
        // Required empty constructor
    }

    public Clinic(String name, String location, String phone, String imageUrl) {
        this.name = name;
        this.location = location;
        this.phone = phone;
        this.imageUrl = imageUrl;
    }
}
