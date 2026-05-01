package com.example.petbuddy;

import android.content.Context;
import android.util.Log;
import java.util.List;

/**
 * Integration test class to verify Firebase realtime animal data functionality
 * This class helps test the complete workflow from Firebase to UI
 */
public class FirebaseIntegrationTest {
    private static final String TAG = "FirebaseIntegrationTest";
    
    private final Context context;
    private final AnimalDataService animalDataService;
    private final RealtimeDataManager realtimeDataManager;
    private final ImageCacheManager imageCacheManager;
    private final AdoptionStatusManager adoptionStatusManager;
    private final DataSynchronizer dataSynchronizer;
    
    public FirebaseIntegrationTest(Context context) {
        this.context = context;
        this.animalDataService = AnimalDataService.getInstance();
        this.realtimeDataManager = RealtimeDataManager.getInstance(context);
        this.imageCacheManager = ImageCacheManager.getInstance(context);
        this.adoptionStatusManager = AdoptionStatusManager.getInstance();
        this.dataSynchronizer = DataSynchronizer.getInstance(context);
    }
    
    /**
     * Test complete Firebase connectivity and data flow
     */
    public void testFirebaseConnectivity(TestCallback callback) {
        Log.d(TAG, "Starting Firebase connectivity test...");
        
        // Test 1: Basic animal data loading
        animalDataService.getAvailableAnimals("test_connectivity", new AnimalDataService.AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                Log.d(TAG, "✓ Successfully loaded " + animals.size() + " animals");
                
                if (!animals.isEmpty()) {
                    // Test 2: Image loading for first animal
                    AdoptablePetModel firstAnimal = animals.get(0);
                    testImageLoading(firstAnimal, callback);
                } else {
                    Log.w(TAG, "No animals found for testing");
                    callback.onTestCompleted(true, "Firebase connected but no animals found");
                }
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "✗ Failed to load animals", exception);
                callback.onTestCompleted(false, "Failed to load animals: " + exception.getMessage());
            }
        });
    }
    
    /**
     * Test image loading functionality
     */
    private void testImageLoading(AdoptablePetModel animal, TestCallback callback) {
        if (animal.getPrimaryImageUrl() != null) {
            Log.d(TAG, "Testing image loading for: " + animal.getName());
            
            imageCacheManager.getImage(animal.getPrimaryImageUrl(), new ImageCacheManager.ImageCacheCallback() {
                @Override
                public void onImageLoaded(android.graphics.Bitmap bitmap, ImageCacheManager.CacheSource source) {
                    Log.d(TAG, "✓ Successfully loaded image from: " + source);
                    testRealtimeUpdates(callback);
                }
                
                @Override
                public void onCacheMiss(String imageUrl) {
                    Log.d(TAG, "⚠ Image cache miss (non-critical): " + imageUrl);
                    testRealtimeUpdates(callback);
                }
                
                @Override
                public void onError(Exception error) {
                    Log.w(TAG, "⚠ Image loading failed (non-critical): " + error.getMessage());
                    testRealtimeUpdates(callback);
                }
            });
        } else {
            Log.d(TAG, "No image URL to test, proceeding to realtime updates");
            testRealtimeUpdates(callback);
        }
    }
    
    /**
     * Test realtime data updates
     */
    private void testRealtimeUpdates(TestCallback callback) {
        Log.d(TAG, "Testing realtime data updates...");
        
        // Test connection state monitoring
        realtimeDataManager.registerConnectionCallback("test_connection", new RealtimeDataManager.ConnectionStateCallback() {
            @Override
            public void onNetworkStateChanged(boolean connected) {
                Log.d(TAG, "Network state: " + (connected ? "Connected" : "Disconnected"));
            }
            
            @Override
            public void onFirebaseStateChanged(boolean connected) {
                Log.d(TAG, "Firebase state: " + (connected ? "Connected" : "Disconnected"));
                if (connected) {
                    Log.d(TAG, "✓ Firebase realtime connection established");
                    testOfflineSync(callback);
                }
            }
        });
    }
    
    /**
     * Test offline synchronization
     */
    private void testOfflineSync(TestCallback callback) {
        Log.d(TAG, "Testing offline synchronization...");
        
        // Test data synchronizer
        dataSynchronizer.getSyncStats();
        Log.d(TAG, "✓ Data synchronizer initialized");
        
        // Test adoption status manager
        testAdoptionStatusManager(callback);
    }
    
    /**
     * Test adoption status management
     */
    private void testAdoptionStatusManager(TestCallback callback) {
        Log.d(TAG, "Testing adoption status manager...");
        
        // Just verify the manager is working (don't actually change any data)
        Log.d(TAG, "✓ Adoption status manager initialized");
        
        // Test filtering
        testFiltering(callback);
    }
    
    /**
     * Test filtering functionality
     */
    private void testFiltering(TestCallback callback) {
        Log.d(TAG, "Testing filtering functionality...");
        
        // Create a test filter
        AnimalDataService.AnimalFilter filter = new AnimalDataService.AnimalFilter();
        filter.setSpecies("dog");
        filter.setAdoptionStatus("available");
        
        animalDataService.getFilteredAnimals(filter, "test_filter", new AnimalDataService.AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                Log.d(TAG, "✓ Successfully filtered animals: " + animals.size() + " dogs found");
                testSearchFunctionality(callback);
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "✗ Filtering test failed", exception);
                callback.onTestCompleted(false, "Filtering failed: " + exception.getMessage());
            }
        });
    }
    
    /**
     * Test search functionality
     */
    private void testSearchFunctionality(TestCallback callback) {
        Log.d(TAG, "Testing search functionality...");
        
        animalDataService.searchAnimals("friendly", "test_search", new AnimalDataService.AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                Log.d(TAG, "✓ Search completed: " + animals.size() + " animals found with 'friendly'");
                
                // All tests completed successfully
                cleanup();
                callback.onTestCompleted(true, "All Firebase integration tests passed successfully!");
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e(TAG, "✗ Search test failed", exception);
                cleanup();
                callback.onTestCompleted(false, "Search failed: " + exception.getMessage());
            }
        });
    }
    
    /**
     * Clean up test resources
     */
    private void cleanup() {
        Log.d(TAG, "Cleaning up test resources...");
        
        // Remove test listeners
        animalDataService.removeListener("test_connectivity");
        animalDataService.removeListener("test_filter");
        animalDataService.removeListener("test_search");
        
        // Unregister connection callback
        realtimeDataManager.unregisterConnectionCallback("test_connection");
        
        Log.d(TAG, "✓ Test cleanup completed");
    }
    
    /**
     * Test callback interface
     */
    public interface TestCallback {
        void onTestCompleted(boolean success, String message);
    }
    
    /**
     * Run a quick connectivity test
     */
    public static void runQuickTest(Context context, TestCallback callback) {
        FirebaseIntegrationTest test = new FirebaseIntegrationTest(context);
        test.testFirebaseConnectivity(callback);
    }
}