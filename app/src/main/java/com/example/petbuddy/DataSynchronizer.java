package com.example.petbuddy;

import android.content.Context;
import android.util.Log;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handles offline data synchronization with SQLite caching and operation queuing
 * Manages data freshness, offline operations, and sync when connection is restored
 */
public class DataSynchronizer {
    private static final String TAG = "DataSynchronizer";
    private static DataSynchronizer instance;
    
    private final Context context;
    private final DatabaseReference database;
    private final DatabaseHelper dbHelper;
    private final ExecutorService executorService;
    private final Map<String, Long> lastSyncTimes;
    private final List<PendingOperation> pendingOperations;
    
    // Sync settings
    private static final long SYNC_INTERVAL_MS = 5 * 60 * 1000; // 5 minutes
    private static final long DATA_FRESHNESS_THRESHOLD_MS = 10 * 60 * 1000; // 10 minutes
    private static final int MAX_PENDING_OPERATIONS = 100;
    
    private boolean isOnline = false;
    private boolean isSyncing = false;
    
    private DataSynchronizer(Context context) {
        this.context = context.getApplicationContext();
        this.database = FirebaseDatabase.getInstance().getReference();
        this.dbHelper = new DatabaseHelper(context);
        this.executorService = Executors.newFixedThreadPool(3);
        this.lastSyncTimes = new ConcurrentHashMap<>();
        this.pendingOperations = new ArrayList<>();
    }
    
    public static synchronized DataSynchronizer getInstance(Context context) {
        if (instance == null) {
            instance = new DataSynchronizer(context);
        }
        return instance;
    }
    
    /**
     * Interface for sync callbacks
     */
    public interface SyncCallback {
        void onSyncStarted();
        void onSyncProgress(int completed, int total);
        void onSyncCompleted(int syncedCount, int failedCount);
        void onSyncError(Exception error);
    }
    
    /**
     * Interface for data freshness callbacks
     */
    public interface DataFreshnessCallback {
        void onDataFreshness(String dataType, boolean isFresh, long lastUpdateTime);
    }
    
    /**
     * Represents a pending offline operation
     */
    public static class PendingOperation {
        public enum Type {
            CREATE, UPDATE, DELETE
        }
        
        public String id;
        public Type type;
        public String tableName;
        public Map<String, Object> data;
        public long timestamp;
        public int retryCount;
        
        public PendingOperation(String id, Type type, String tableName, Map<String, Object> data) {
            this.id = id;
            this.type = type;
            this.tableName = tableName;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
            this.retryCount = 0;
        }
    }
    
    /**
     * Set online/offline status
     */
    public void setOnlineStatus(boolean online) {
        boolean wasOffline = !this.isOnline;
        this.isOnline = online;
        
        if (online && wasOffline && !pendingOperations.isEmpty()) {
            // Connection restored - sync pending operations
            syncPendingOperations(null);
        }
    }
    
    /**
     * Cache animal data locally
     */
    public void cacheAnimalData(List<AdoptablePetModel> animals, SyncCallback callback) {
        executorService.execute(() -> {
            try {
                if (callback != null) {
                    callback.onSyncStarted();
                }
                
                int total = animals.size();
                int completed = 0;
                int failed = 0;
                
                for (AdoptablePetModel animal : animals) {
                    try {
                        dbHelper.insertOrUpdateCachedAnimal(animal);
                        completed++;
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to cache animal: " + animal.getPetId(), e);
                        failed++;
                    }
                    
                    if (callback != null) {
                        callback.onSyncProgress(completed + failed, total);
                    }
                }
                
                // Update last sync time
                lastSyncTimes.put("animals", System.currentTimeMillis());
                
                if (callback != null) {
                    callback.onSyncCompleted(completed, failed);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to cache animal data", e);
                if (callback != null) {
                    callback.onSyncError(e);
                }
            }
        });
    }
    
    /**
     * Get cached animal data
     */
    public void getCachedAnimals(String species, DataCallback<List<AdoptablePetModel>> callback) {
        executorService.execute(() -> {
            try {
                List<AdoptablePetModel> cachedAnimals = dbHelper.getCachedAnimals(species);
                callback.onSuccess(cachedAnimals);
            } catch (Exception e) {
                Log.e(TAG, "Failed to get cached animals", e);
                callback.onError(e);
            }
        });
    }
    
    /**
     * Queue offline operation
     */
    public void queueOfflineOperation(String animalId, PendingOperation.Type type, 
                                    Map<String, Object> data, OperationCallback callback) {
        if (pendingOperations.size() >= MAX_PENDING_OPERATIONS) {
            // Remove oldest operation
            pendingOperations.remove(0);
        }
        
        PendingOperation operation = new PendingOperation(animalId, type, "animals", data);
        pendingOperations.add(operation);
        
        // Also store in local database
        dbHelper.insertPendingOperation(operation);
        
        if (callback != null) {
            callback.onOperationQueued(operation.id);
        }
        
        // Try to sync immediately if online
        if (isOnline) {
            syncPendingOperations(null);
        }
    }
    
    /**
     * Sync pending operations when connection is restored
     */
    public void syncPendingOperations(SyncCallback callback) {
        if (isSyncing) {
            return; // Already syncing
        }
        
        executorService.execute(() -> {
            isSyncing = true;
            
            try {
                if (callback != null) {
                    callback.onSyncStarted();
                }
                
                // Load pending operations from database
                List<PendingOperation> allPendingOps = dbHelper.getPendingOperations();
                pendingOperations.clear();
                pendingOperations.addAll(allPendingOps);
                
                int total = pendingOperations.size();
                int completed = 0;
                int failed = 0;
                
                List<PendingOperation> toRemove = new ArrayList<>();
                
                for (PendingOperation operation : pendingOperations) {
                    try {
                        boolean success = executePendingOperation(operation);
                        if (success) {
                            toRemove.add(operation);
                            dbHelper.deletePendingOperation(operation.id);
                            completed++;
                        } else {
                            operation.retryCount++;
                            if (operation.retryCount >= 3) {
                                // Max retries reached, remove operation
                                toRemove.add(operation);
                                dbHelper.deletePendingOperation(operation.id);
                                failed++;
                            } else {
                                // Update retry count in database
                                dbHelper.updatePendingOperationRetryCount(operation.id, operation.retryCount);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to execute pending operation: " + operation.id, e);
                        failed++;
                    }
                    
                    if (callback != null) {
                        callback.onSyncProgress(completed + failed, total);
                    }
                }
                
                // Remove completed operations
                pendingOperations.removeAll(toRemove);
                
                if (callback != null) {
                    callback.onSyncCompleted(completed, failed);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to sync pending operations", e);
                if (callback != null) {
                    callback.onSyncError(e);
                }
            } finally {
                isSyncing = false;
            }
        });
    }
    
    /**
     * Execute a pending operation
     */
    private boolean executePendingOperation(PendingOperation operation) {
        try {
            DatabaseReference ref = database.child(operation.tableName).child(operation.id);
            
            switch (operation.type) {
                case CREATE:
                case UPDATE:
                    ref.updateChildren(operation.data);
                    return true;
                    
                case DELETE:
                    ref.removeValue();
                    return true;
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to execute operation: " + operation.type + " for " + operation.id, e);
            return false;
        }
    }
    
    /**
     * Check data freshness
     */
    public void checkDataFreshness(String dataType, DataFreshnessCallback callback) {
        Long lastSync = lastSyncTimes.get(dataType);
        long currentTime = System.currentTimeMillis();
        
        if (lastSync == null) {
            callback.onDataFreshness(dataType, false, 0);
            return;
        }
        
        boolean isFresh = (currentTime - lastSync) < DATA_FRESHNESS_THRESHOLD_MS;
        callback.onDataFreshness(dataType, isFresh, lastSync);
    }
    
    /**
     * Force sync from Firebase
     */
    public void forceSyncFromFirebase(String species, SyncCallback callback) {
        if (!isOnline) {
            if (callback != null) {
                callback.onSyncError(new Exception("Device is offline"));
            }
            return;
        }
        
        DatabaseReference animalsRef = database.child("animals");
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<AdoptablePetModel> animals = new ArrayList<>();
                
                for (DataSnapshot animalSnapshot : snapshot.getChildren()) {
                    try {
                        AdoptablePetModel animal = animalSnapshot.getValue(AdoptablePetModel.class);
                        if (animal != null && (species == null || species.equalsIgnoreCase(animal.getSpecies()))) {
                            animals.add(animal);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to parse animal data", e);
                    }
                }
                
                // Cache the fresh data
                cacheAnimalData(animals, callback);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to sync from Firebase", error.toException());
                if (callback != null) {
                    callback.onSyncError(error.toException());
                }
            }
        };
        
        animalsRef.addListenerForSingleValueEvent(listener);
    }
    
    /**
     * Get sync statistics
     */
    public SyncStats getSyncStats() {
        return new SyncStats(
            lastSyncTimes.size(),
            pendingOperations.size(),
            isOnline,
            isSyncing,
            lastSyncTimes.get("animals")
        );
    }
    
    /**
     * Sync statistics data class
     */
    public static class SyncStats {
        public final int syncedDataTypes;
        public final int pendingOperations;
        public final boolean isOnline;
        public final boolean isSyncing;
        public final Long lastAnimalSync;
        
        public SyncStats(int syncedDataTypes, int pendingOperations, boolean isOnline, 
                        boolean isSyncing, Long lastAnimalSync) {
            this.syncedDataTypes = syncedDataTypes;
            this.pendingOperations = pendingOperations;
            this.isOnline = isOnline;
            this.isSyncing = isSyncing;
            this.lastAnimalSync = lastAnimalSync;
        }
    }
    
    /**
     * Generic data callback interface
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception error);
    }
    
    /**
     * Operation callback interface
     */
    public interface OperationCallback {
        void onOperationQueued(String operationId);
        void onOperationCompleted(String operationId);
        void onOperationFailed(String operationId, Exception error);
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        executorService.shutdown();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}