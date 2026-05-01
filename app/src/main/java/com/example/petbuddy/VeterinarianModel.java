package com.example.petbuddy;

import java.io.Serializable;

public class VeterinarianModel implements Serializable {

    public String vetId;
    public String clinicName;
    public String doctorName;
    public String experience;
    public String timing;
    public String contact;
    public String address;
    public String specialization;
    public long addedAt;
    
    // Additional fields for EditVeterinarianActivity
    public String name;
    public String qualifications;
    public String phone;
    public String email;
    public double consultationFee;
    public String availableHours;
    public String services;
    public String imageUrl;

    public VeterinarianModel() {
        // Required empty constructor for Firebase
    }

    public VeterinarianModel(String clinicName, String doctorName, String experience, 
                           String timing, String contact, String address, String specialization) {
        this.clinicName = clinicName;
        this.doctorName = doctorName;
        this.name = doctorName; // Set name as doctorName for compatibility
        this.experience = experience;
        this.timing = timing;
        this.availableHours = timing; // Set availableHours as timing for compatibility
        this.contact = contact;
        this.phone = contact; // Set phone as contact for compatibility
        this.address = address;
        this.specialization = specialization;
        this.addedAt = System.currentTimeMillis();
        this.consultationFee = 0.0;
        this.imageUrl = "";
    }

    // Getters and Setters
    public String getVetId() {
        return vetId;
    }

    public void setVetId(String vetId) {
        this.vetId = vetId;
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
    }

    public String getDoctorName() {
        return doctorName != null ? doctorName : name;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
        this.name = doctorName; // Keep both in sync
    }

    public String getName() {
        return name != null ? name : doctorName;
    }

    public void setName(String name) {
        this.name = name;
        this.doctorName = name; // Keep both in sync
    }

    public String getExperience() {
        return experience;
    }

    public void setExperience(String experience) {
        this.experience = experience;
    }

    public String getTiming() {
        return timing != null ? timing : availableHours;
    }

    public void setTiming(String timing) {
        this.timing = timing;
        this.availableHours = timing; // Keep both in sync
    }

    public String getAvailableHours() {
        return availableHours != null ? availableHours : timing;
    }

    public void setAvailableHours(String availableHours) {
        this.availableHours = availableHours;
        this.timing = availableHours; // Keep both in sync
    }

    public String getContact() {
        return contact != null ? contact : phone;
    }

    public void setContact(String contact) {
        this.contact = contact;
        this.phone = contact; // Keep both in sync
    }

    public String getPhone() {
        return phone != null ? phone : contact;
    }

    public void setPhone(String phone) {
        this.phone = phone;
        this.contact = phone; // Keep both in sync
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }

    public String getQualifications() {
        return qualifications != null ? qualifications : "DVM";
    }

    public void setQualifications(String qualifications) {
        this.qualifications = qualifications;
    }

    public String getEmail() {
        return email != null ? email : "";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public double getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(double consultationFee) {
        this.consultationFee = consultationFee;
    }

    public String getServices() {
        return services != null ? services : "General veterinary services";
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
