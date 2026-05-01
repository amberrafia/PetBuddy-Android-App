package com.example.petbuddy;

public class PetModel {

    public String petId;
    public String petName;
    public String species; // Cat or Dog
    public String breed;
    public int age;
    public String gender; // Male or Female
    public double weight;
    public String photoUrl;
    public long addedAt;

    public PetModel() {
        // Required empty constructor for Firebase
    }

    public PetModel(String petName, String species, String breed, int age, String gender, double weight, String photoUrl) {
        this.petName = petName;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.weight = weight;
        this.photoUrl = photoUrl;
        this.addedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getPetName() {
        return petName;
    }

    public void setPetName(String petName) {
        this.petName = petName;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }
}
