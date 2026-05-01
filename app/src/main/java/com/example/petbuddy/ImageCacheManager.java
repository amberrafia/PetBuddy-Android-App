package com.example.petbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local image caching system for offline viewing and performance optimization
 */
public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static final String CACHE_DIR_NAME = "animal_images";
    private static final int MEMORY_CACHE_SIZE = 20 * 1024 * 1024; // 20MB
    private static final int DISK_CACHE_SIZE = 100 * 1024 * 1024; // 100MB
    private static final int BITMAP_QUALITY = 90; // 90% quality for cached images
    
    private static ImageCacheManager instance;
    private final Context context;
    private final LruCache<String, Bitmap> memoryCache;
    private final File diskCacheDir;
    private final ExecutorService executorService;
    
    // Cache statistics
    private int memoryHits = 0;
    private int diskHits = 0;
    private int misses = 0;
    
    private ImageCacheManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newFixedThreadPool(2);
        
        // Initialize memory cache
        this.memoryCache = new LruCache<String, Bitmap>(MEMORY_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (evicted) {
                    Log.d(TAG, "Memory cache evicted: " + key);
                }
            }
        };
        
        // Initialize disk cache directory
        this.diskCacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!diskCacheDir.exists()) {
            boolean created = diskCacheDir.mkdirs();
            Log.d(TAG, "Disk cache directory created: " + created);
        }
        
        // Clean up old cache files on startup
        cleanupOldCacheFiles();
    }
    
    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context);
        }
        return instance;
    }
    
    /**
     * Get image from cache (memory first, then disk)
     */
    public void getImage(String imageUrl, ImageCacheCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onError(new IllegalArgumentException("Image URL cannot be null or empty"));
            return;
        }
        
        String cacheKey = generateCacheKey(imageUrl);
        
        // Check memory cache first
        Bitmap memoryBitmap = memoryCache.get(cacheKey);
        if (memoryBitmap != null && !memoryBitmap.isRecycled()) {
            memoryHits++;
            Log.d(TAG, "Memory cache hit: " + cacheKey);
            callback.onImageLoaded(memoryBitmap, CacheSource.MEMORY);
            return;
        }
        
        // Check disk cache in background thread
        executorService.execute(() -> {
            Bitmap diskBitmap = loadFromDiskCache(cacheKey);
            if (diskBitmap != null) {
                diskHits++;
                Log.d(TAG, "Disk cache hit: " + cacheKey);
                
                // Add to memory cache for faster future access
                memoryCache.put(cacheKey, diskBitmap);
                
                // Return on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    callback.onImageLoaded(diskBitmap, CacheSource.DISK));
            } else {
                misses++;
                Log.d(TAG, "Cache miss: " + cacheKey);
                
                // Return on main thread
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    callback.onCacheMiss(imageUrl));
            }
        });
    }
    
    /**
     * Cache image in both memory and disk
     */
    public void cacheImage(String imageUrl, Bitmap bitmap, CacheCallback callback) {
        if (imageUrl == null || bitmap == null || bitmap.isRecycled()) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Invalid image URL or bitmap"));
            }
            return;
        }
        
        String cacheKey = generateCacheKey(imageUrl);
        
        // Add to memory cache immediately
        memoryCache.put(cacheKey, bitmap);
        Log.d(TAG, "Image cached in memory: " + cacheKey);
        
        // Save to disk cache in background
        executorService.execute(() -> {
            try {
                saveToDiskCache(cacheKey, bitmap);
                Log.d(TAG, "Image cached to disk: " + cacheKey);
                
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to cache image to disk: " + cacheKey, e);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onError(e));
                }
            }
        });
    }
    
    /**
     * Preload images for better user experience
     */
    public void preloadImages(java.util.List<String> imageUrls, PreloadCallback callback) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            if (callback != null) {
                callback.onPreloadComplete(0, 0);
            }
            return;
        }
        
        executorService.execute(() -> {
            int loaded = 0;
            int failed = 0;
            
            for (String imageUrl : imageUrls) {
                try {
                    String cacheKey = generateCacheKey(imageUrl);
                    
                    // Check if already cached
                    if (memoryCache.get(cacheKey) != null || diskCacheExists(cacheKey)) {
                        loaded++;
                        continue;
                    }
                    
                    // Download and cache image
                    Bitmap bitmap = downloadImageFromUrl(imageUrl);
                    if (bitmap != null) {
                        cacheImage(imageUrl, bitmap, null);
                        loaded++;
                    } else {
                        failed++;
                    }
                    
                    // Add small delay to avoid overwhelming the network
                    Thread.sleep(100);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to preload image: " + imageUrl, e);
                    failed++;
                }
            }
            
            final int finalLoaded = loaded;
            final int finalFailed = failed;
            
            if (callback != null) {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                    callback.onPreloadComplete(finalLoaded, finalFailed));
            }
        });
    }
    
    /**
     * Clear memory cache
     */
    public void clearMemoryCache() {
        memoryCache.evictAll();
        Log.d(TAG, "Memory cache cleared");
    }
    
    /**
     * Clear disk cache
     */
    public void clearDiskCache(ClearCacheCallback callback) {
        executorService.execute(() -> {
            try {
                File[] files = diskCacheDir.listFiles();
                int deletedCount = 0;
                
                if (files != null) {
                    for (File file : files) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
                
                final int finalDeletedCount = deletedCount;
                Log.d(TAG, "Disk cache cleared: " + finalDeletedCount + " files deleted");
                
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onCacheCleared(finalDeletedCount));
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to clear disk cache", e);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> 
                        callback.onError(e));
                }
            }
        });
    }
    
    /**
     * Get cache statistics
     */
    public CacheStats getCacheStats() {
        CacheStats stats = new CacheStats();
        stats.memoryHits = memoryHits;
        stats.diskHits = diskHits;
        stats.misses = misses;
        stats.memoryCacheSize = memoryCache.size();
        stats.memoryCacheMaxSize = memoryCache.maxSize();
        stats.diskCacheFileCount = getDiskCacheFileCount();
        stats.diskCacheSizeBytes = getDiskCacheSize();
        return stats;
    }
    
    /**
     * Check if image is cached
     */
    public boolean isImageCached(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }
        
        String cacheKey = generateCacheKey(imageUrl);
        
        // Check memory cache
        if (memoryCache.get(cacheKey) != null) {
            return true;
        }
        
        // Check disk cache
        return diskCacheExists(cacheKey);
    }
    
    /**
     * Remove specific image from cache
     */
    public void removeFromCache(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }
        
        String cacheKey = generateCacheKey(imageUrl);
        
        // Remove from memory cache
        memoryCache.remove(cacheKey);
        
        // Remove from disk cache
        executorService.execute(() -> {
            File cacheFile = new File(diskCacheDir, cacheKey);
            if (cacheFile.exists()) {
                boolean deleted = cacheFile.delete();
                Log.d(TAG, "Removed from disk cache: " + cacheKey + " (deleted: " + deleted + ")");
            }
        });
    }
    
    /**
     * Generate cache key from image URL
     */
    private String generateCacheKey(String imageUrl) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(imageUrl.getBytes());
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "MD5 algorithm not available", e);
            return String.valueOf(imageUrl.hashCode());
        }
    }
    
    /**
     * Load bitmap from disk cache
     */
    private Bitmap loadFromDiskCache(String cacheKey) {
        File cacheFile = new File(diskCacheDir, cacheKey);
        
        if (!cacheFile.exists()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(cacheFile)) {
            return BitmapFactory.decodeStream(fis);
        } catch (IOException e) {
            Log.e(TAG, "Failed to load from disk cache: " + cacheKey, e);
            return null;
        }
    }
    
    /**
     * Save bitmap to disk cache
     */
    private void saveToDiskCache(String cacheKey, Bitmap bitmap) throws IOException {
        File cacheFile = new File(diskCacheDir, cacheKey);
        
        try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, BITMAP_QUALITY, fos);
            fos.flush();
        }
    }
    
    /**
     * Check if file exists in disk cache
     */
    private boolean diskCacheExists(String cacheKey) {
        File cacheFile = new File(diskCacheDir, cacheKey);
        return cacheFile.exists();
    }
    
    /**
     * Get disk cache file count
     */
    private int getDiskCacheFileCount() {
        File[] files = diskCacheDir.listFiles();
        return files != null ? files.length : 0;
    }
    
    /**
     * Get disk cache size in bytes
     */
    private long getDiskCacheSize() {
        File[] files = diskCacheDir.listFiles();
        long totalSize = 0;
        
        if (files != null) {
            for (File file : files) {
                totalSize += file.length();
            }
        }
        
        return totalSize;
    }
    
    /**
     * Clean up old cache files (older than 7 days)
     */
    private void cleanupOldCacheFiles() {
        executorService.execute(() -> {
            try {
                File[] files = diskCacheDir.listFiles();
                if (files == null) return;
                
                long currentTime = System.currentTimeMillis();
                long maxAge = 7 * 24 * 60 * 60 * 1000L; // 7 days in milliseconds
                int deletedCount = 0;
                
                for (File file : files) {
                    if (currentTime - file.lastModified() > maxAge) {
                        if (file.delete()) {
                            deletedCount++;
                        }
                    }
                }
                
                Log.d(TAG, "Cleanup completed: " + deletedCount + " old files deleted");
            } catch (Exception e) {
                Log.e(TAG, "Failed to cleanup old cache files", e);
            }
        });
    }
    
    /**
     * Download image from URL (helper method)
     */
    private Bitmap downloadImageFromUrl(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            java.io.InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (Exception e) {
            Log.e(TAG, "Failed to download image from URL: " + imageUrl, e);
            return null;
        }
    }
    
    // Data classes and enums
    public enum CacheSource {
        MEMORY,
        DISK,
        NETWORK
    }
    
    public static class CacheStats {
        public int memoryHits;
        public int diskHits;
        public int misses;
        public int memoryCacheSize;
        public int memoryCacheMaxSize;
        public int diskCacheFileCount;
        public long diskCacheSizeBytes;
        
        public double getHitRate() {
            int totalRequests = memoryHits + diskHits + misses;
            if (totalRequests == 0) return 0.0;
            return (double) (memoryHits + diskHits) / totalRequests;
        }
    }
    
    // Callback interfaces
    public interface ImageCacheCallback {
        void onImageLoaded(Bitmap bitmap, CacheSource source);
        void onCacheMiss(String imageUrl);
        void onError(Exception exception);
    }
    
    public interface CacheCallback {
        void onSuccess();
        void onError(Exception exception);
    }
    
    public interface PreloadCallback {
        void onPreloadComplete(int loadedCount, int failedCount);
    }
    
    public interface ClearCacheCallback {
        void onCacheCleared(int deletedFileCount);
        void onError(Exception exception);
    }
}