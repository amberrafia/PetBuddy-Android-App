package com.example.petbuddy;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for Firebase listener lifecycle and connection state management
 */
public class RealtimeDataManager {
    private static final String TAG = "RealtimeDataManager";
    private static final long RECONNECTION_DELAY_MS = 2000; // 2 seconds
    private static final int MAX_RECONNECTION_ATTEMPTS = 5;
    
    private static RealtimeDataManager instance;
    private final Context context;
    private final FirebaseManager firebaseManager;
    private final AnimalDataService animalDataService;
    private final ConnectivityManager connectivityManager;
    
    // Connection state management
    private boolean isConnected = false;
    private boolean isFirebaseConnected = false;
    private int reconnectionAttempts = 0;
    
    // Listener management
    private final Map<String, ListenerInfo> activeListeners = new ConcurrentHashMap<>();
    private final Map<String, ConnectionStateCallback> connectionCallbacks = new HashMap<>();
    
    // Network callback for monitoring connectivity
    private ConnectivityManager.NetworkCallback networkCallback;
    
    private RealtimeDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseManager = FirebaseManager.getInstance();
        this.animalDataService = AnimalDataService.getInstance();
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        
        initializeNetworkMonitoring();
        initializeFirebaseConnectionMonitoring();
    }
    
    public static synchronized RealtimeDataManager getInstance(Context context) {
        if (instance == null) {
            instance = new RealtimeDataManager(context);
        }
        return instance;
    }
    
    /**
     * Initialize network connectivity monitoring
     */
    private void initializeNetworkMonitoring() {
        NetworkRequest.Builder builder = new NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
        
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                Log.d(TAG, "Network connection available");
                handleNetworkAvailable();
            }
            
            @Override
            public void onLost(Network network) {
                Log.d(TAG, "Network connection lost");
                handleNetworkLost();
            }
            
            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                boolean hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                Log.d(TAG, "Network capabilities changed. Has internet: " + hasInternet);
                
                if (hasInternet && !isConnected) {
                    handleNetworkAvailable();
                } else if (!hasInternet && isConnected) {
                    handleNetworkLost();
                }
            }
        };
        
        connectivityManager.registerNetworkCallback(builder.build(), networkCallback);
        
        // Check initial connectivity state
        checkInitialConnectivity();
    }
    
    /**
     * Initialize Firebase connection monitoring
     */
    private void initializeFirebaseConnectionMonitoring() {
        DatabaseReference connectedRef = firebaseManager.getDatabase().getReference(".info/connected");
        connectedRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                boolean connected = Boolean.TRUE.equals(snapshot.getValue(Boolean.class));
                Log.d(TAG, "Firebase connection state changed: " + connected);
                handleFirebaseConnectionChange(connected);
            }
            
            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {
                Log.e(TAG, "Firebase connection monitoring cancelled", error.toException());
            }
        });
    }
    
    /**
     * Check initial network connectivity
     */
    private void checkInitialConnectivity() {
        if (connectivityManager != null) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                if (capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
                    handleNetworkAvailable();
                } else {
                    handleNetworkLost();
                }
            } else {
                handleNetworkLost();
            }
        }
    }
    
    /**
     * Handle network becoming available
     */
    private void handleNetworkAvailable() {
        if (!isConnected) {
            isConnected = true;
            reconnectionAttempts = 0;
            Log.d(TAG, "Network connection restored");
            
            // Notify connection callbacks
            notifyConnectionStateChanged(true);
            
            // Attempt to restore Firebase listeners
            restoreFirebaseListeners();
        }
    }
    
    /**
     * Handle network connection lost
     */
    private void handleNetworkLost() {
        if (isConnected) {
            isConnected = false;
            Log.d(TAG, "Network connection lost");
            
            // Notify connection callbacks
            notifyConnectionStateChanged(false);
        }
    }
    
    /**
     * Handle Firebase connection state change
     */
    private void handleFirebaseConnectionChange(boolean connected) {
        boolean wasConnected = isFirebaseConnected;
        isFirebaseConnected = connected;
        
        if (connected && !wasConnected) {
            Log.d(TAG, "Firebase connection established");
            reconnectionAttempts = 0;
            notifyFirebaseConnectionStateChanged(true);
        } else if (!connected && wasConnected) {
            Log.d(TAG, "Firebase connection lost");
            notifyFirebaseConnectionStateChanged(false);
            
            // Attempt reconnection if network is available
            if (isConnected) {
                attemptReconnection();
            }
        }
    }
    
    /**
     * Attempt to reconnect to Firebase
     */
    private void attemptReconnection() {
        if (reconnectionAttempts >= MAX_RECONNECTION_ATTEMPTS) {
            Log.w(TAG, "Max reconnection attempts reached");
            return;
        }
        
        reconnectionAttempts++;
        Log.d(TAG, "Attempting Firebase reconnection #" + reconnectionAttempts);
        
        // Delay before reconnection attempt
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFirebaseConnected && isConnected) {
                // Try to trigger reconnection by accessing Firebase
                firebaseManager.getDatabase().goOnline();
                
                // Schedule next attempt if this one fails
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFirebaseConnected && reconnectionAttempts < MAX_RECONNECTION_ATTEMPTS) {
                        attemptReconnection();
                    }
                }, RECONNECTION_DELAY_MS);
            }
        }, RECONNECTION_DELAY_MS);
    }
    
    /**
     * Restore Firebase listeners after connection is restored
     */
    private void restoreFirebaseListeners() {
        Log.d(TAG, "Restoring " + activeListeners.size() + " Firebase listeners");
        
        for (Map.Entry<String, ListenerInfo> entry : activeListeners.entrySet()) {
            String listenerId = entry.getKey();
            ListenerInfo listenerInfo = entry.getValue();
            
            try {
                // Re-establish listener based on type
                switch (listenerInfo.type) {
                    case ALL_ANIMALS:
                        animalDataService.getAvailableAnimals(listenerId, listenerInfo.animalDataCallback);
                        break;
                    case ANIMALS_BY_SPECIES:
                        animalDataService.getAnimalsBySpecies(listenerInfo.species, listenerId, listenerInfo.animalDataCallback);
                        break;
                    case SINGLE_ANIMAL:
                        animalDataService.getAnimalById(listenerInfo.animalId, listenerId, listenerInfo.singleAnimalCallback);
                        break;
                    case NEW_ANIMALS:
                        animalDataService.listenForNewAnimals(listenerId, listenerInfo.newAnimalCallback);
                        break;
                }
                
                Log.d(TAG, "Restored listener: " + listenerId);
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to restore listener: " + listenerId, e);
            }
        }
    }
    
    /**
     * Register a listener for management
     */
    public void registerListener(String listenerId, ListenerType type, String species, String animalId,
                                AnimalDataService.AnimalDataCallback animalDataCallback,
                                AnimalDataService.SingleAnimalCallback singleAnimalCallback,
                                AnimalDataService.NewAnimalCallback newAnimalCallback) {
        
        ListenerInfo listenerInfo = new ListenerInfo();
        listenerInfo.type = type;
        listenerInfo.species = species;
        listenerInfo.animalId = animalId;
        listenerInfo.animalDataCallback = animalDataCallback;
        listenerInfo.singleAnimalCallback = singleAnimalCallback;
        listenerInfo.newAnimalCallback = newAnimalCallback;
        listenerInfo.registrationTime = System.currentTimeMillis();
        
        activeListeners.put(listenerId, listenerInfo);
        Log.d(TAG, "Registered listener: " + listenerId + " (" + type + ")");
    }
    
    /**
     * Unregister a listener
     */
    public void unregisterListener(String listenerId) {
        ListenerInfo removed = activeListeners.remove(listenerId);
        if (removed != null) {
            animalDataService.removeListener(listenerId);
            animalDataService.removeChildListener(listenerId);
            Log.d(TAG, "Unregistered listener: " + listenerId);
        }
    }
    
    /**
     * Register connection state callback
     */
    public void registerConnectionCallback(String callbackId, ConnectionStateCallback callback) {
        connectionCallbacks.put(callbackId, callback);
    }
    
    /**
     * Unregister connection state callback
     */
    public void unregisterConnectionCallback(String callbackId) {
        connectionCallbacks.remove(callbackId);
    }
    
    /**
     * Notify all callbacks of connection state change
     */
    private void notifyConnectionStateChanged(boolean connected) {
        for (ConnectionStateCallback callback : connectionCallbacks.values()) {
            try {
                callback.onNetworkStateChanged(connected);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying connection state callback", e);
            }
        }
    }
    
    /**
     * Notify all callbacks of Firebase connection state change
     */
    private void notifyFirebaseConnectionStateChanged(boolean connected) {
        for (ConnectionStateCallback callback : connectionCallbacks.values()) {
            try {
                callback.onFirebaseStateChanged(connected);
            } catch (Exception e) {
                Log.e(TAG, "Error notifying Firebase connection state callback", e);
            }
        }
    }
    
    /**
     * Get current connection state
     */
    public boolean isNetworkConnected() {
        return isConnected;
    }
    
    /**
     * Get current Firebase connection state
     */
    public boolean isFirebaseConnected() {
        return isFirebaseConnected;
    }
    
    /**
     * Get connection quality info
     */
    public ConnectionInfo getConnectionInfo() {
        ConnectionInfo info = new ConnectionInfo();
        info.isNetworkConnected = isConnected;
        info.isFirebaseConnected = isFirebaseConnected;
        info.reconnectionAttempts = reconnectionAttempts;
        info.activeListenersCount = activeListeners.size();
        return info;
    }
    
    /**
     * Force Firebase to go offline (for testing)
     */
    public void goOffline() {
        firebaseManager.getDatabase().goOffline();
        Log.d(TAG, "Firebase forced offline");
    }
    
    /**
     * Force Firebase to go online
     */
    public void goOnline() {
        firebaseManager.getDatabase().goOnline();
        Log.d(TAG, "Firebase forced online");
    }
    
    /**
     * Clean up resources
     */
    public void cleanup() {
        if (networkCallback != null && connectivityManager != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        
        animalDataService.removeAllListeners();
        activeListeners.clear();
        connectionCallbacks.clear();
        
        Log.d(TAG, "RealtimeDataManager cleaned up");
    }
    
    // Data classes and enums
    public enum ListenerType {
        ALL_ANIMALS,
        ANIMALS_BY_SPECIES,
        SINGLE_ANIMAL,
        NEW_ANIMALS
    }
    
    private static class ListenerInfo {
        ListenerType type;
        String species;
        String animalId;
        AnimalDataService.AnimalDataCallback animalDataCallback;
        AnimalDataService.SingleAnimalCallback singleAnimalCallback;
        AnimalDataService.NewAnimalCallback newAnimalCallback;
        long registrationTime;
    }
    
    public static class ConnectionInfo {
        public boolean isNetworkConnected;
        public boolean isFirebaseConnected;
        public int reconnectionAttempts;
        public int activeListenersCount;
    }
    
    // Callback interface
    public interface ConnectionStateCallback {
        void onNetworkStateChanged(boolean connected);
        void onFirebaseStateChanged(boolean connected);
    }
}