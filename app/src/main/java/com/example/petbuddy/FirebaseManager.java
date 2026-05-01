package com.example.petbuddy;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

/**
 * Central Firebase configuration and initialization manager
 */
public class FirebaseManager {
    private static final String TAG = "FirebaseManager";
    private static FirebaseManager instance;
    
    private FirebaseAuth firebaseAuth;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseStorage firebaseStorage;
    private boolean isInitialized = false;
    
    private FirebaseManager() {}
    
    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }
    
    /**
     * Initialize Firebase services
     */
    public void initialize(Context context) {
        if (isInitialized) {
            Log.d(TAG, "Firebase already initialized");
            return;
        }
        
        try {
            // Initialize Firebase App
            FirebaseApp.initializeApp(context);
            
            // Initialize Firebase services
            firebaseAuth = FirebaseAuth.getInstance();
            firebaseDatabase = FirebaseDatabase.getInstance();
            firebaseStorage = FirebaseStorage.getInstance();
            
            // Configure database for offline persistence
            firebaseDatabase.setPersistenceEnabled(true);
            
            isInitialized = true;
            Log.d(TAG, "Firebase services initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase services", e);
        }
    }
    
    /**
     * Get Firebase Auth instance
     */
    public FirebaseAuth getAuth() {
        if (!isInitialized) {
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return firebaseAuth;
    }
    
    /**
     * Get Firebase Database instance
     */
    public FirebaseDatabase getDatabase() {
        if (!isInitialized) {
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return firebaseDatabase;
    }
    
    /**
     * Get Firebase Storage instance
     */
    public FirebaseStorage getStorage() {
        if (!isInitialized) {
            throw new IllegalStateException("Firebase not initialized. Call initialize() first.");
        }
        return firebaseStorage;
    }
    
    /**
     * Check if Firebase is initialized
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * Get database reference for adoptable pets
     */
    public com.google.firebase.database.DatabaseReference getAdoptablePetsRef() {
        return getDatabase().getReference("adoptablePets");
    }
    
    /**
     * Get database reference for adoption status
     */
    public com.google.firebase.database.DatabaseReference getAdoptionStatusRef() {
        return getDatabase().getReference("adoptionStatus");
    }
    
    /**
     * Get storage reference for animal images
     */
    public com.google.firebase.storage.StorageReference getAnimalImagesRef() {
        return getStorage().getReference("animal_images");
    }
    
    /**
     * Get storage reference for injury photos
     */
    public com.google.firebase.storage.StorageReference getInjuryPhotosRef() {
        return getStorage().getReference("injury_photos");
    }
    
    /**
     * Get database reference for pet reminders
     */
    public com.google.firebase.database.DatabaseReference getPetRemindersRef() {
        return getDatabase().getReference("petReminders");
    }
}