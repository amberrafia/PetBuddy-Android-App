package com.example.petbuddy;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages real-time emergency data from Firebase
 */
public class EmergencyDataManager {
    private static final String TAG = "EmergencyDataManager";
    private static EmergencyDataManager instance;
    
    private FirebaseManager firebaseManager;
    private DatabaseReference emergencyRef;
    private DatabaseReference vetsRef;
    private DatabaseReference emergencyContactsRef;
    private DatabaseReference emergencyAlertsRef;
    
    private List<EmergencyDataCallback> callbacks;
    private boolean isListening = false;
    
    // Real-time data
    private List<EmergencyVet> nearbyVets;
    private List<EmergencyContact> emergencyContacts;
    private List<EmergencyAlert> activeAlerts;
    private EmergencyStatus currentStatus;
    
    private EmergencyDataManager(Context context) {
        firebaseManager = FirebaseManager.getInstance();
        callbacks = new ArrayList<>();
        nearbyVets = new ArrayList<>();
        emergencyContacts = new ArrayList<>();
        activeAlerts = new ArrayList<>();
        
        initializeReferences();
    }
    
    public static synchronized EmergencyDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new EmergencyDataManager(context);
        }
        return instance;
    }
    
    private void initializeReferences() {
        try {
            DatabaseReference rootRef = firebaseManager.getDatabase().getReference();
            emergencyRef = rootRef.child("emergency");
            vetsRef = emergencyRef.child("veterinarians");
            emergencyContactsRef = emergencyRef.child("contacts");
            emergencyAlertsRef = emergencyRef.child("alerts");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Firebase references", e);
        }
    }
    
    public void startListening() {
        if (isListening) return;
        
        Log.d(TAG, "Starting real-time emergency data listening");
        
        // Listen for nearby veterinarians
        vetsRef.addValueEventListener(vetsListener);
        
        // Listen for emergency contacts
        emergencyContactsRef.addValueEventListener(contactsListener);
        
        // Listen for emergency alerts
        emergencyAlertsRef.addValueEventListener(alertsListener);
        
        isListening = true;
    }
    
    public void stopListening() {
        if (!isListening) return;
        
        Log.d(TAG, "Stopping real-time emergency data listening");
        
        vetsRef.removeEventListener(vetsListener);
        emergencyContactsRef.removeEventListener(contactsListener);
        emergencyAlertsRef.removeEventListener(alertsListener);
        
        isListening = false;
    }
    
    // Veterinarians listener
    private ValueEventListener vetsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            nearbyVets.clear();
            
            for (DataSnapshot vetSnapshot : dataSnapshot.getChildren()) {
                try {
                    EmergencyVet vet = vetSnapshot.getValue(EmergencyVet.class);
                    if (vet != null && vet.isAvailable) {
                        nearbyVets.add(vet);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing vet data", e);
                }
            }
            
            Log.d(TAG, "Updated nearby vets: " + nearbyVets.size());
            notifyCallbacks();
        }
        
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "Vets listener cancelled", databaseError.toException());
        }
    };
    
    // Emergency contacts listener
    private ValueEventListener contactsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            emergencyContacts.clear();
            
            for (DataSnapshot contactSnapshot : dataSnapshot.getChildren()) {
                try {
                    EmergencyContact contact = contactSnapshot.getValue(EmergencyContact.class);
                    if (contact != null && contact.isActive) {
                        emergencyContacts.add(contact);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing contact data", e);
                }
            }
            
            Log.d(TAG, "Updated emergency contacts: " + emergencyContacts.size());
            notifyCallbacks();
        }
        
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "Contacts listener cancelled", databaseError.toException());
        }
    };
    
    // Emergency alerts listener
    private ValueEventListener alertsListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            activeAlerts.clear();
            
            for (DataSnapshot alertSnapshot : dataSnapshot.getChildren()) {
                try {
                    EmergencyAlert alert = alertSnapshot.getValue(EmergencyAlert.class);
                    if (alert != null && alert.isActive) {
                        activeAlerts.add(alert);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing alert data", e);
                }
            }
            
            Log.d(TAG, "Updated emergency alerts: " + activeAlerts.size());
            notifyCallbacks();
        }
        
        @Override
        public void onCancelled(DatabaseError databaseError) {
            Log.e(TAG, "Alerts listener cancelled", databaseError.toException());
        }
    };
    
    public void reportEmergency(double latitude, double longitude, String description, String petInfo) {
        try {
            String emergencyId = emergencyRef.child("reports").push().getKey();
            
            Map<String, Object> emergencyReport = new HashMap<>();
            emergencyReport.put("id", emergencyId);
            emergencyReport.put("latitude", latitude);
            emergencyReport.put("longitude", longitude);
            emergencyReport.put("description", description);
            emergencyReport.put("petInfo", petInfo);
            emergencyReport.put("timestamp", System.currentTimeMillis());
            emergencyReport.put("status", "active");
            emergencyReport.put("userId", getCurrentUserId());
            
            emergencyRef.child("reports").child(emergencyId).setValue(emergencyReport)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Emergency reported successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to report emergency", e);
                });
                
        } catch (Exception e) {
            Log.e(TAG, "Error reporting emergency", e);
        }
    }
    
    private String getCurrentUserId() {
        try {
            if (firebaseManager.getAuth().getCurrentUser() != null) {
                return firebaseManager.getAuth().getCurrentUser().getUid();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user ID", e);
        }
        return "anonymous_" + System.currentTimeMillis();
    }
    
    public void registerCallback(EmergencyDataCallback callback) {
        if (!callbacks.contains(callback)) {
            callbacks.add(callback);
        }
    }
    
    public void unregisterCallback(EmergencyDataCallback callback) {
        callbacks.remove(callback);
    }
    
    private void notifyCallbacks() {
        for (EmergencyDataCallback callback : callbacks) {
            try {
                callback.onEmergencyDataUpdated(nearbyVets, emergencyContacts, activeAlerts);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying callback", e);
            }
        }
    }
    
    // Getters
    public List<EmergencyVet> getNearbyVets() {
        return new ArrayList<>(nearbyVets);
    }
    
    public List<EmergencyContact> getEmergencyContacts() {
        return new ArrayList<>(emergencyContacts);
    }
    
    public List<EmergencyAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }
    
    public boolean hasActiveAlerts() {
        return !activeAlerts.isEmpty();
    }
    
    public interface EmergencyDataCallback {
        void onEmergencyDataUpdated(List<EmergencyVet> vets, List<EmergencyContact> contacts, List<EmergencyAlert> alerts);
    }
    
    // Data models
    public static class EmergencyVet {
        public String id;
        public String name;
        public String address;
        public String phone;
        public double latitude;
        public double longitude;
        public boolean isAvailable;
        public boolean is24Hour;
        public String specialties;
        public double rating;
        public int responseTimeMinutes;
        
        public EmergencyVet() {} // Required for Firebase
    }
    
    public static class EmergencyContact {
        public String id;
        public String name;
        public String phone;
        public String type; // "hotline", "poison_control", "animal_hospital"
        public boolean isActive;
        public String description;
        public String availability;
        
        public EmergencyContact() {} // Required for Firebase
    }
    
    public static class EmergencyAlert {
        public String id;
        public String title;
        public String message;
        public String type; // "weather", "disease_outbreak", "recall", "general"
        public String severity; // "low", "medium", "high", "critical"
        public boolean isActive;
        public long timestamp;
        public String region;
        
        public EmergencyAlert() {} // Required for Firebase
    }
    
    public static class EmergencyStatus {
        public boolean isOnline;
        public int availableVets;
        public int activeAlerts;
        public long lastUpdate;
        
        public EmergencyStatus() {} // Required for Firebase
    }
}