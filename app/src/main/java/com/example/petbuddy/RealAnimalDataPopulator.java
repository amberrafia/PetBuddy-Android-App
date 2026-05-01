package com.example.petbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to populate Firebase with real animal data and photos
 */
public class RealAnimalDataPopulator {
    private static final String TAG = "RealAnimalDataPopulator";
    
    private final Context context;
    private final FirebaseManager firebaseManager;
    private final ImageStorageService imageStorageService;
    private final ExecutorService executorService;
    private final Random random;
    
    public RealAnimalDataPopulator(Context context) {
        this.context = context;
        this.firebaseManager = FirebaseManager.getInstance();
        this.imageStorageService = ImageStorageService.getInstance(context);
        this.executorService = Executors.newFixedThreadPool(3);
        this.random = new Random();
    }
    
    /**
     * Populate database with real dog data and photos
     */
    public void populateRealDogData(PopulationCallback callback) {
        Log.d(TAG, "Starting to populate real dog data");
        
        List<RealDogData> dogData = createRealDogData();
        
        executorService.execute(() -> {
            int successCount = 0;
            int errorCount = 0;
            
            for (RealDogData dog : dogData) {
                try {
                    // Download and upload dog photo
                    Bitmap dogPhoto = downloadImageFromUrl(dog.photoUrl);
                    if (dogPhoto != null) {
                        // Upload to Firebase Storage
                        imageStorageService.uploadImage(dog.id, dogPhoto, new ImageStorageService.UploadCallback() {
                            @Override
                            public void onSuccess(String imageUrl) {
                                // Create AdoptablePetModel with real data
                                AdoptablePetModel petModel = createPetModelFromDogData(dog, imageUrl);
                                
                                // Save to Firebase Database
                                savePetToDatabase(petModel, new DatabaseCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Successfully added dog: " + dog.name);
                                    }
                                    
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Failed to save dog to database: " + dog.name, e);
                                    }
                                });
                            }
                            
                            @Override
                            public void onFailure(Exception exception) {
                                Log.e(TAG, "Failed to upload dog photo: " + dog.name, exception);
                            }
                        });
                        
                        successCount++;
                    } else {
                        errorCount++;
                        Log.e(TAG, "Failed to download photo for dog: " + dog.name);
                    }
                    
                    // Add delay to avoid overwhelming the services
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    errorCount++;
                    Log.e(TAG, "Error processing dog: " + dog.name, e);
                }
            }
            
            // Notify completion
            if (callback != null) {
                if (errorCount == 0) {
                    callback.onSuccess(successCount);
                } else {
                    callback.onPartialSuccess(successCount, errorCount);
                }
            }
        });
    }
    
    /**
     * Populate database with real cat data and photos
     */
    public void populateRealCatData(PopulationCallback callback) {
        Log.d(TAG, "Starting to populate real cat data");
        
        List<RealCatData> catData = createRealCatData();
        
        executorService.execute(() -> {
            int successCount = 0;
            int errorCount = 0;
            
            for (RealCatData cat : catData) {
                try {
                    // Download and upload cat photo
                    Bitmap catPhoto = downloadImageFromUrl(cat.photoUrl);
                    if (catPhoto != null) {
                        // Upload to Firebase Storage
                        imageStorageService.uploadImage(cat.id, catPhoto, new ImageStorageService.UploadCallback() {
                            @Override
                            public void onSuccess(String imageUrl) {
                                // Create AdoptablePetModel with real data
                                AdoptablePetModel petModel = createPetModelFromCatData(cat, imageUrl);
                                
                                // Save to Firebase Database
                                savePetToDatabase(petModel, new DatabaseCallback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.d(TAG, "Successfully added cat: " + cat.name);
                                    }
                                    
                                    @Override
                                    public void onFailure(Exception e) {
                                        Log.e(TAG, "Failed to save cat to database: " + cat.name, e);
                                    }
                                });
                            }
                            
                            @Override
                            public void onFailure(Exception exception) {
                                Log.e(TAG, "Failed to upload cat photo: " + cat.name, exception);
                            }
                        });
                        
                        successCount++;
                    } else {
                        errorCount++;
                        Log.e(TAG, "Failed to download photo for cat: " + cat.name);
                    }
                    
                    // Add delay to avoid overwhelming the services
                    Thread.sleep(1000);
                    
                } catch (Exception e) {
                    errorCount++;
                    Log.e(TAG, "Error processing cat: " + cat.name, e);
                }
            }
            
            // Notify completion
            if (callback != null) {
                if (errorCount == 0) {
                    callback.onSuccess(successCount);
                } else {
                    callback.onPartialSuccess(successCount, errorCount);
                }
            }
        });
    }
    
    /**
     * Create real dog data with authentic information
     */
    private List<RealDogData> createRealDogData() {
        List<RealDogData> dogs = new ArrayList<>();
        
        // Real dog data with placeholder image URLs (replace with actual URLs)
        dogs.add(new RealDogData("dog_001", "Luna", "Labrador Retriever", 24, "female", "large", 
            "Golden", "Sweet and gentle dog looking for a loving family. Well-trained and house-broken.",
            "Gentle, Calm, Affectionate", "Healthy", true, true, "None",
            "Happy Paws Shelter", "456 Oak Ave, Downtown", "Sarah Johnson", "(555) 123-4567",
            0.0, "https://images.unsplash.com/photo-1552053831-71594a27632d?w=800"));
            
        dogs.add(new RealDogData("dog_002", "Max", "German Shepherd", 36, "male", "large",
            "Black and Tan", "Loyal and intelligent companion. Great with kids and other pets.",
            "Loyal, Intelligent, Protective", "Healthy", true, true, "None",
            "City Animal Rescue", "789 Pine St, Midtown", "Mike Wilson", "(555) 234-5678",
            150.0, "https://images.unsplash.com/photo-1589941013453-ec89f33b5e95?w=800"));
            
        dogs.add(new RealDogData("dog_003", "Bella", "Golden Retriever", 18, "female", "large",
            "Golden", "Energetic and playful. Loves fetch and swimming. Perfect family dog.",
            "Energetic, Playful, Friendly", "Healthy", true, false, "None",
            "Rescue Haven", "321 Elm St, Uptown", "Lisa Chen", "(555) 345-6789",
            200.0, "https://images.unsplash.com/photo-1552053831-71594a27632d?w=800"));
            
        dogs.add(new RealDogData("dog_004", "Charlie", "Beagle", 30, "male", "medium",
            "Tri-color", "Friendly beagle who loves adventures and treats. Good with children.",
            "Curious, Friendly, Gentle", "Healthy", true, true, "None",
            "Animal Friends Society", "654 Maple Ave, Westside", "Tom Rodriguez", "(555) 456-7890",
            120.0, "https://images.unsplash.com/photo-1544717297-fa95b6ee9643?w=800"));
            
        dogs.add(new RealDogData("dog_005", "Daisy", "Border Collie", 42, "female", "medium",
            "Black and White", "Highly intelligent and active. Needs experienced owner who can provide mental stimulation.",
            "Intelligent, Active, Focused", "Healthy", true, true, "Needs active lifestyle",
            "Border Collie Rescue", "987 Cedar Rd, Eastside", "Jennifer Adams", "(555) 567-8901",
            175.0, "https://images.unsplash.com/photo-1551717743-49959800b1f6?w=800"));
        
        return dogs;
    }
    
    /**
     * Create real cat data with authentic information
     */
    private List<RealCatData> createRealCatData() {
        List<RealCatData> cats = new ArrayList<>();
        
        cats.add(new RealCatData("cat_001", "Whiskers", "Maine Coon", 36, "male", "large",
            "Brown Tabby", "Gentle giant with a sweet personality. Great with families and other pets.",
            "Gentle, Calm, Sociable", "Healthy", true, true, "None",
            "Feline Friends Rescue", "123 Cat Lane, Downtown", "Emma Thompson", "(555) 111-2222",
            100.0, "https://images.unsplash.com/photo-1574158622682-e40e69881006?w=800"));
            
        cats.add(new RealCatData("cat_002", "Mittens", "Domestic Shorthair", 24, "female", "medium",
            "Tuxedo", "Playful and affectionate. Loves to chase toys and cuddle on laps.",
            "Playful, Affectionate, Curious", "Healthy", true, true, "None",
            "City Cat Sanctuary", "456 Whisker Way, Midtown", "David Park", "(555) 222-3333",
            75.0, "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=800"));
            
        cats.add(new RealCatData("cat_003", "Shadow", "Russian Blue", 18, "male", "medium",
            "Blue-Gray", "Quiet and reserved but very loyal once he trusts you. Perfect for calm households.",
            "Quiet, Loyal, Independent", "Healthy", true, true, "Prefers quiet homes",
            "Blue Cat Rescue", "789 Purr Street, Uptown", "Rachel Green", "(555) 333-4444",
            125.0, "https://images.unsplash.com/photo-1596854407944-bf87f6fdd49e?w=800"));
            
        cats.add(new RealCatData("cat_004", "Princess", "Persian", 48, "female", "medium",
            "White", "Beautiful long-haired cat who loves to be pampered. Requires regular grooming.",
            "Calm, Regal, Gentle", "Healthy", true, true, "Requires daily grooming",
            "Persian Cat Haven", "321 Fluffy Ave, Westside", "Maria Garcia", "(555) 444-5555",
            150.0, "https://images.unsplash.com/photo-1592194996308-7b43878e84a6?w=800"));
            
        cats.add(new RealCatData("cat_005", "Tiger", "Orange Tabby", 30, "male", "large",
            "Orange", "Friendly and outgoing orange tabby. Loves attention and playing with toys.",
            "Friendly, Outgoing, Playful", "Healthy", true, true, "None",
            "Tabby Cat Rescue", "654 Orange St, Eastside", "Kevin Lee", "(555) 555-6666",
            90.0, "https://images.unsplash.com/photo-1571566882372-1598d88abd90?w=800"));
        
        return cats;
    }
    
    /**
     * Download image from URL
     */
    private Bitmap downloadImageFromUrl(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Failed to download image from URL: " + imageUrl, e);
            return null;
        }
    }
    
    /**
     * Create AdoptablePetModel from dog data
     */
    private AdoptablePetModel createPetModelFromDogData(RealDogData dog, String imageUrl) {
        AdoptablePetModel pet = new AdoptablePetModel();
        pet.setPetId(dog.id);
        pet.setName(dog.name);
        pet.setSpecies("dog");
        pet.setBreed(dog.breed);
        pet.setAge(dog.age);
        pet.setGender(dog.gender);
        pet.setSize(dog.size);
        pet.setColor(dog.color);
        pet.setDescription(dog.description);
        pet.setPersonality(dog.personality);
        pet.setHealthStatus(dog.healthStatus);
        pet.setVaccinated(dog.isVaccinated);
        pet.setNeutered(dog.isNeutered);
        pet.setSpecialNeeds(dog.specialNeeds);
        pet.setShelterName(dog.shelterName);
        pet.setShelterLocation(dog.shelterLocation);
        pet.setContactPersonName(dog.contactPersonName);
        pet.setContactPhone(dog.contactPhone);
        pet.setAdoptionFee(dog.adoptionFee);
        pet.setImageUrl(imageUrl);
        pet.setDateAdded(System.currentTimeMillis());
        return pet;
    }
    
    /**
     * Create AdoptablePetModel from cat data
     */
    private AdoptablePetModel createPetModelFromCatData(RealCatData cat, String imageUrl) {
        AdoptablePetModel pet = new AdoptablePetModel();
        pet.setPetId(cat.id);
        pet.setName(cat.name);
        pet.setSpecies("cat");
        pet.setBreed(cat.breed);
        pet.setAge(cat.age);
        pet.setGender(cat.gender);
        pet.setSize(cat.size);
        pet.setColor(cat.color);
        pet.setDescription(cat.description);
        pet.setPersonality(cat.personality);
        pet.setHealthStatus(cat.healthStatus);
        pet.setVaccinated(cat.isVaccinated);
        pet.setNeutered(cat.isNeutered);
        pet.setSpecialNeeds(cat.specialNeeds);
        pet.setShelterName(cat.shelterName);
        pet.setShelterLocation(cat.shelterLocation);
        pet.setContactPersonName(cat.contactPersonName);
        pet.setContactPhone(cat.contactPhone);
        pet.setAdoptionFee(cat.adoptionFee);
        pet.setImageUrl(imageUrl);
        pet.setDateAdded(System.currentTimeMillis());
        return pet;
    }
    
    /**
     * Save pet to Firebase Database
     */
    private void savePetToDatabase(AdoptablePetModel pet, DatabaseCallback callback) {
        DatabaseReference petRef = firebaseManager.getAdoptablePetsRef().child(pet.getPetId());
        
        Map<String, Object> petData = new HashMap<>();
        
        // Basic info
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("name", pet.getName());
        basicInfo.put("species", pet.getSpecies());
        basicInfo.put("breed", pet.getBreed());
        basicInfo.put("age", pet.getAge());
        basicInfo.put("gender", pet.getGender());
        basicInfo.put("size", pet.getSize());
        basicInfo.put("color", pet.getColor());
        petData.put("basicInfo", basicInfo);
        
        // Description
        Map<String, Object> description = new HashMap<>();
        description.put("description", pet.getDescription());
        description.put("personality", pet.getPersonality());
        petData.put("description", description);
        
        // Health
        Map<String, Object> health = new HashMap<>();
        health.put("healthStatus", pet.getHealthStatus());
        health.put("isVaccinated", pet.isVaccinated());
        health.put("isNeutered", pet.isNeutered());
        health.put("specialNeeds", pet.getSpecialNeeds());
        petData.put("health", health);
        
        // Images
        Map<String, Object> images = new HashMap<>();
        images.put("primary", pet.getImageUrl());
        petData.put("images", images);
        
        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("shelterName", pet.getShelterName());
        location.put("shelterLocation", pet.getShelterLocation());
        petData.put("location", location);
        
        // Adoption
        Map<String, Object> adoption = new HashMap<>();
        adoption.put("status", "available");
        adoption.put("fee", pet.getAdoptionFee());
        
        Map<String, Object> contact = new HashMap<>();
        contact.put("personName", pet.getContactPersonName());
        contact.put("phone", pet.getContactPhone());
        adoption.put("contact", contact);
        petData.put("adoption", adoption);
        
        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dateAdded", pet.getDateAdded());
        metadata.put("lastUpdated", System.currentTimeMillis());
        metadata.put("version", 1);
        metadata.put("isAvailable", true);
        petData.put("metadata", metadata);
        
        petRef.setValue(petData)
            .addOnSuccessListener(aVoid -> callback.onSuccess())
            .addOnFailureListener(callback::onFailure);
    }
    
    // Data classes for real animal information
    private static class RealDogData {
        String id, name, breed, color, description, personality, healthStatus, specialNeeds;
        String shelterName, shelterLocation, contactPersonName, contactPhone, photoUrl;
        int age;
        String gender, size;
        boolean isVaccinated, isNeutered;
        double adoptionFee;
        
        RealDogData(String id, String name, String breed, int age, String gender, String size,
                   String color, String description, String personality, String healthStatus,
                   boolean isVaccinated, boolean isNeutered, String specialNeeds,
                   String shelterName, String shelterLocation, String contactPersonName,
                   String contactPhone, double adoptionFee, String photoUrl) {
            this.id = id;
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.gender = gender;
            this.size = size;
            this.color = color;
            this.description = description;
            this.personality = personality;
            this.healthStatus = healthStatus;
            this.isVaccinated = isVaccinated;
            this.isNeutered = isNeutered;
            this.specialNeeds = specialNeeds;
            this.shelterName = shelterName;
            this.shelterLocation = shelterLocation;
            this.contactPersonName = contactPersonName;
            this.contactPhone = contactPhone;
            this.adoptionFee = adoptionFee;
            this.photoUrl = photoUrl;
        }
    }
    
    private static class RealCatData {
        String id, name, breed, color, description, personality, healthStatus, specialNeeds;
        String shelterName, shelterLocation, contactPersonName, contactPhone, photoUrl;
        int age;
        String gender, size;
        boolean isVaccinated, isNeutered;
        double adoptionFee;
        
        RealCatData(String id, String name, String breed, int age, String gender, String size,
                   String color, String description, String personality, String healthStatus,
                   boolean isVaccinated, boolean isNeutered, String specialNeeds,
                   String shelterName, String shelterLocation, String contactPersonName,
                   String contactPhone, double adoptionFee, String photoUrl) {
            this.id = id;
            this.name = name;
            this.breed = breed;
            this.age = age;
            this.gender = gender;
            this.size = size;
            this.color = color;
            this.description = description;
            this.personality = personality;
            this.healthStatus = healthStatus;
            this.isVaccinated = isVaccinated;
            this.isNeutered = isNeutered;
            this.specialNeeds = specialNeeds;
            this.shelterName = shelterName;
            this.shelterLocation = shelterLocation;
            this.contactPersonName = contactPersonName;
            this.contactPhone = contactPhone;
            this.adoptionFee = adoptionFee;
            this.photoUrl = photoUrl;
        }
    }
    
    // Callback interfaces
    public interface PopulationCallback {
        void onSuccess(int count);
        void onPartialSuccess(int successCount, int errorCount);
        void onFailure(Exception exception);
    }
    
    public interface DatabaseCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }
}