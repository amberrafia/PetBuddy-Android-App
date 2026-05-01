package com.example.petbuddy;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages adoption status updates with broadcasting and concurrent handling
 * Handles status changes, audit logging, and prevents concurrent adoption conflicts
 */
public class AdoptionStatusManager {
    private static final String TAG = "AdoptionStatusManager";
    private static AdoptionStatusManager instance;
    
    private final DatabaseReference database;
    private final Map<String, AdoptionStatusCallback> statusCallbacks;
    private final Map<String, ValueEventListener> statusListeners;
    
    // Adoption status constants
    public static final String STATUS_AVAILABLE = "available";
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_ADOPTED = "adopted";
    public static final String STATUS_UNAVAILABLE = "unavailable";
    
    private AdoptionStatusManager() {
        database = FirebaseDatabase.getInstance().getReference();
        statusCallbacks = new ConcurrentHashMap<>();
        statusListeners = new ConcurrentHashMap<>();
    }
    
    public static synchronized AdoptionStatusManager getInstance() {
        if (instance == null) {
            instance = new AdoptionStatusManager();
        }
        return instance;
    }
    
    /**
     * Interface for adoption status change callbacks
     */
    public interface AdoptionStatusCallback {
        void onStatusChanged(String animalId, String newStatus, String previousStatus, long timestamp);
        void onStatusChangeError(String animalId, Exception error);
        void onConcurrentAdoptionAttempt(String animalId, String conflictingUserId);
    }
    
    /**
     * Interface for adoption attempt results
     */
    public interface AdoptionAttemptCallback {
        void onAdoptionSuccess(String animalId, String adopterId);
        void onAdoptionFailed(String animalId, String reason, Exception error);
        void onConcurrentAttemptDetected(String animalId, String conflictingUserId);
    }
    
    /**
     * Attempt to adopt an animal with concurrent handling
     */
    public void attemptAdoption(String animalId, String adopterId, String adopterName, 
                               AdoptionAttemptCallback callback) {
        if (animalId == null || adopterId == null || callback == null) {
            callback.onAdoptionFailed(animalId, "Invalid parameters", 
                new IllegalArgumentException("Animal ID, adopter ID, and callback cannot be null"));
            return;
        }
        
        DatabaseReference animalRef = database.child("animals").child(animalId);
        
        // Use transaction to handle concurrent adoption attempts
        animalRef.runTransaction(new com.google.firebase.database.Transaction.Handler() {
            @Override
            public com.google.firebase.database.Transaction.Result doTransaction(
                com.google.firebase.database.MutableData mutableData) {
                
                AdoptablePetModel animal = mutableData.getValue(AdoptablePetModel.class);
                if (animal == null) {
                    return com.google.firebase.database.Transaction.abort();
                }
                
                // Check if animal is still available
                if (!STATUS_AVAILABLE.equals(animal.getAdoptionStatus())) {
                    return com.google.firebase.database.Transaction.abort();
                }
                
                // Update adoption status and adopter info
                animal.setAdoptionStatus(STATUS_PENDING);
                animal.setAdopterId(adopterId);
                animal.setAdopterName(adopterName);
                animal.setLastUpdated(System.currentTimeMillis());
                animal.setLastUpdatedBy(adopterId);
                
                mutableData.setValue(animal);
                return com.google.firebase.database.Transaction.success(mutableData);
            }
            
            @Override
            public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, "Adoption transaction failed", databaseError.toException());
                    callback.onAdoptionFailed(animalId, "Database error", databaseError.toException());
                    return;
                }
                
                if (!committed) {
                    // Transaction was aborted - check why
                    animalRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            AdoptablePetModel animal = snapshot.getValue(AdoptablePetModel.class);
                            if (animal == null) {
                                callback.onAdoptionFailed(animalId, "Animal not found", null);
                            } else if (!STATUS_AVAILABLE.equals(animal.getAdoptionStatus())) {
                                String conflictingUserId = animal.getAdopterId();
                                if (conflictingUserId != null && !conflictingUserId.equals(adopterId)) {
                                    callback.onConcurrentAttemptDetected(animalId, conflictingUserId);
                                } else {
                                    callback.onAdoptionFailed(animalId, 
                                        "Animal is no longer available (status: " + animal.getAdoptionStatus() + ")", null);
                                }
                            } else {
                                callback.onAdoptionFailed(animalId, "Unknown transaction failure", null);
                            }
                        }
                        
                        @Override
                        public void onCancelled(DatabaseError error) {
                            callback.onAdoptionFailed(animalId, "Failed to check animal status", error.toException());
                        }
                    });
                } else {
                    // Success - log the adoption attempt
                    logAdoptionStatusChange(animalId, STATUS_AVAILABLE, STATUS_PENDING, adopterId, 
                        "Adoption attempt by " + adopterName);
                    callback.onAdoptionSuccess(animalId, adopterId);
                }
            }
        });
    }
    
    /**
     * Update adoption status with audit logging
     */
    public void updateAdoptionStatus(String animalId, String newStatus, String userId, 
                                   String reason, AdoptionStatusCallback callback) {
        if (animalId == null || newStatus == null) {
            if (callback != null) {
                callback.onStatusChangeError(animalId, 
                    new IllegalArgumentException("Animal ID and status cannot be null"));
            }
            return;
        }
        
        if (!isValidStatus(newStatus)) {
            if (callback != null) {
                callback.onStatusChangeError(animalId, 
                    new IllegalArgumentException("Invalid adoption status: " + newStatus));
            }
            return;
        }
        
        DatabaseReference animalRef = database.child("animals").child(animalId);
        
        // First get current status for logging
        animalRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                AdoptablePetModel animal = snapshot.getValue(AdoptablePetModel.class);
                if (animal == null) {
                    if (callback != null) {
                        callback.onStatusChangeError(animalId, new Exception("Animal not found"));
                    }
                    return;
                }
                
                String previousStatus = animal.getAdoptionStatus();
                
                // Update the status
                Map<String, Object> updates = new HashMap<>();
                updates.put("adoptionStatus", newStatus);
                updates.put("lastUpdated", ServerValue.TIMESTAMP);
                updates.put("lastUpdatedBy", userId);
                
                // Clear adopter info if status is not pending/adopted
                if (!STATUS_PENDING.equals(newStatus) && !STATUS_ADOPTED.equals(newStatus)) {
                    updates.put("adopterId", null);
                    updates.put("adopterName", null);
                }
                
                animalRef.updateChildren(updates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Log the status change
                        logAdoptionStatusChange(animalId, previousStatus, newStatus, userId, reason);
                        
                        if (callback != null) {
                            callback.onStatusChanged(animalId, newStatus, previousStatus, 
                                System.currentTimeMillis());
                        }
                    } else {
                        Log.e(TAG, "Failed to update adoption status", task.getException());
                        if (callback != null) {
                            callback.onStatusChangeError(animalId, task.getException());
                        }
                    }
                });
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to read animal for status update", error.toException());
                if (callback != null) {
                    callback.onStatusChangeError(animalId, error.toException());
                }
            }
        });
    }
    
    /**
     * Listen for adoption status changes for a specific animal
     */
    public void listenForStatusChanges(String animalId, String listenerId, AdoptionStatusCallback callback) {
        if (animalId == null || listenerId == null || callback == null) {
            return;
        }
        
        // Remove existing listener if any
        removeStatusListener(listenerId);
        
        DatabaseReference animalRef = database.child("animals").child(animalId).child("adoptionStatus");
        
        ValueEventListener listener = new ValueEventListener() {
            private String lastStatus = null;
            
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                String newStatus = snapshot.getValue(String.class);
                if (newStatus != null && !newStatus.equals(lastStatus)) {
                    callback.onStatusChanged(animalId, newStatus, lastStatus, System.currentTimeMillis());
                    lastStatus = newStatus;
                }
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Status listener cancelled", error.toException());
                callback.onStatusChangeError(animalId, error.toException());
            }
        };
        
        animalRef.addValueEventListener(listener);
        statusListeners.put(listenerId, listener);
        statusCallbacks.put(listenerId, callback);
    }
    
    /**
     * Remove status change listener
     */
    public void removeStatusListener(String listenerId) {
        ValueEventListener listener = statusListeners.remove(listenerId);
        if (listener != null) {
            // Remove from all possible references
            database.child("animals").removeEventListener(listener);
        }
        statusCallbacks.remove(listenerId);
    }
    
    /**
     * Log adoption status change for audit purposes
     */
    private void logAdoptionStatusChange(String animalId, String previousStatus, String newStatus, 
                                       String userId, String reason) {
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("animalId", animalId);
        logEntry.put("previousStatus", previousStatus);
        logEntry.put("newStatus", newStatus);
        logEntry.put("userId", userId);
        logEntry.put("reason", reason != null ? reason : "Status change");
        logEntry.put("timestamp", ServerValue.TIMESTAMP);
        
        database.child("adoption_logs").push().setValue(logEntry)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to log adoption status change", e));
    }
    
    /**
     * Validate adoption status
     */
    private boolean isValidStatus(String status) {
        return STATUS_AVAILABLE.equals(status) || 
               STATUS_PENDING.equals(status) || 
               STATUS_ADOPTED.equals(status) || 
               STATUS_UNAVAILABLE.equals(status);
    }
    
    /**
     * Get adoption history for an animal
     */
    public void getAdoptionHistory(String animalId, AdoptionHistoryCallback callback) {
        database.child("adoption_logs")
            .orderByChild("animalId")
            .equalTo(animalId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    // Process adoption history
                    callback.onHistoryLoaded(animalId, snapshot);
                }
                
                @Override
                public void onCancelled(DatabaseError error) {
                    callback.onHistoryError(animalId, error.toException());
                }
            });
    }
    
    /**
     * Interface for adoption history callbacks
     */
    public interface AdoptionHistoryCallback {
        void onHistoryLoaded(String animalId, DataSnapshot historySnapshot);
        void onHistoryError(String animalId, Exception error);
    }
    
    /**
     * Clean up all listeners
     */
    public void cleanup() {
        for (ValueEventListener listener : statusListeners.values()) {
            database.removeEventListener(listener);
        }
        statusListeners.clear();
        statusCallbacks.clear();
    }
}