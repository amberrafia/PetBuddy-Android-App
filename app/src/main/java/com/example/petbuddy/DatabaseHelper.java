package com.example.petbuddy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SQLite database helper for local data caching and offline operations
 * Manages cached animal data, images, and pending operations
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseHelper";
    
    // Database info
    private static final String DATABASE_NAME = "petbuddy_cache.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table names
    private static final String TABLE_CACHED_ANIMALS = "cached_animals";
    private static final String TABLE_CACHED_IMAGES = "cached_images";
    private static final String TABLE_PENDING_OPERATIONS = "pending_operations";
    
    // Cached Animals table columns
    private static final String COLUMN_PET_ID = "pet_id";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SPECIES = "species";
    private static final String COLUMN_BREED = "breed";
    private static final String COLUMN_AGE = "age";
    private static final String COLUMN_SIZE = "size";
    private static final String COLUMN_GENDER = "gender";
    private static final String COLUMN_DESCRIPTION = "description";
    private static final String COLUMN_LOCATION = "location";
    private static final String COLUMN_CONTACT_INFO = "contact_info";
    private static final String COLUMN_ADOPTION_STATUS = "adoption_status";
    private static final String COLUMN_DATE_ADDED = "date_added";
    private static final String COLUMN_PRIMARY_IMAGE_URL = "primary_image_url";
    private static final String COLUMN_IMAGE_URLS = "image_urls"; // JSON array
    private static final String COLUMN_LAST_UPDATED = "last_updated";
    private static final String COLUMN_LAST_UPDATED_BY = "last_updated_by";
    private static final String COLUMN_VERSION = "version";
    private static final String COLUMN_SYNC_STATUS = "sync_status";
    private static final String COLUMN_IS_LOCAL_ONLY = "is_local_only";
    private static final String COLUMN_METADATA = "metadata"; // JSON object
    private static final String COLUMN_CACHED_AT = "cached_at";
    
    // Cached Images table columns
    private static final String COLUMN_IMAGE_URL = "image_url";
    private static final String COLUMN_LOCAL_PATH = "local_path";
    private static final String COLUMN_FILE_SIZE = "file_size";
    private static final String COLUMN_CACHE_TIME = "cache_time";
    private static final String COLUMN_ACCESS_COUNT = "access_count";
    private static final String COLUMN_LAST_ACCESS = "last_access";
    
    // Pending Operations table columns
    private static final String COLUMN_OPERATION_ID = "operation_id";
    private static final String COLUMN_OPERATION_TYPE = "operation_type";
    private static final String COLUMN_TABLE_NAME = "table_name";
    private static final String COLUMN_OPERATION_DATA = "operation_data"; // JSON
    private static final String COLUMN_TIMESTAMP = "timestamp";
    private static final String COLUMN_RETRY_COUNT = "retry_count";
    
    private final Gson gson;
    
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.gson = new Gson();
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        createCachedAnimalsTable(db);
        createCachedImagesTable(db);
        createPendingOperationsTable(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades
        Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);
        
        // For now, just recreate tables (in production, you'd want proper migration)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_ANIMALS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHED_IMAGES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PENDING_OPERATIONS);
        onCreate(db);
    }
    
    /**
     * Create cached animals table
     */
    private void createCachedAnimalsTable(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_CACHED_ANIMALS + " ("
                + COLUMN_PET_ID + " TEXT PRIMARY KEY, "
                + COLUMN_NAME + " TEXT NOT NULL, "
                + COLUMN_SPECIES + " TEXT NOT NULL, "
                + COLUMN_BREED + " TEXT, "
                + COLUMN_AGE + " INTEGER, "
                + COLUMN_SIZE + " TEXT, "
                + COLUMN_GENDER + " TEXT, "
                + COLUMN_DESCRIPTION + " TEXT, "
                + COLUMN_LOCATION + " TEXT, "
                + COLUMN_CONTACT_INFO + " TEXT, "
                + COLUMN_ADOPTION_STATUS + " TEXT, "
                + COLUMN_DATE_ADDED + " INTEGER, "
                + COLUMN_PRIMARY_IMAGE_URL + " TEXT, "
                + COLUMN_IMAGE_URLS + " TEXT, "
                + COLUMN_LAST_UPDATED + " INTEGER, "
                + COLUMN_LAST_UPDATED_BY + " TEXT, "
                + COLUMN_VERSION + " INTEGER, "
                + COLUMN_SYNC_STATUS + " TEXT, "
                + COLUMN_IS_LOCAL_ONLY + " INTEGER, "
                + COLUMN_METADATA + " TEXT, "
                + COLUMN_CACHED_AT + " INTEGER DEFAULT (strftime('%s','now') * 1000)"
                + ")";
        
        db.execSQL(CREATE_TABLE);
        
        // Create indexes for better query performance
        db.execSQL("CREATE INDEX idx_species ON " + TABLE_CACHED_ANIMALS + "(" + COLUMN_SPECIES + ")");
        db.execSQL("CREATE INDEX idx_adoption_status ON " + TABLE_CACHED_ANIMALS + "(" + COLUMN_ADOPTION_STATUS + ")");
        db.execSQL("CREATE INDEX idx_last_updated ON " + TABLE_CACHED_ANIMALS + "(" + COLUMN_LAST_UPDATED + ")");
    }
    
    /**
     * Create cached images table
     */
    private void createCachedImagesTable(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_CACHED_IMAGES + " ("
                + COLUMN_IMAGE_URL + " TEXT PRIMARY KEY, "
                + COLUMN_LOCAL_PATH + " TEXT NOT NULL, "
                + COLUMN_FILE_SIZE + " INTEGER, "
                + COLUMN_CACHE_TIME + " INTEGER DEFAULT (strftime('%s','now') * 1000), "
                + COLUMN_ACCESS_COUNT + " INTEGER DEFAULT 0, "
                + COLUMN_LAST_ACCESS + " INTEGER DEFAULT (strftime('%s','now') * 1000)"
                + ")";
        
        db.execSQL(CREATE_TABLE);
        
        // Create indexes
        db.execSQL("CREATE INDEX idx_cache_time ON " + TABLE_CACHED_IMAGES + "(" + COLUMN_CACHE_TIME + ")");
        db.execSQL("CREATE INDEX idx_last_access ON " + TABLE_CACHED_IMAGES + "(" + COLUMN_LAST_ACCESS + ")");
    }
    
    /**
     * Create pending operations table
     */
    private void createPendingOperationsTable(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_PENDING_OPERATIONS + " ("
                + COLUMN_OPERATION_ID + " TEXT PRIMARY KEY, "
                + COLUMN_OPERATION_TYPE + " TEXT NOT NULL, "
                + COLUMN_TABLE_NAME + " TEXT NOT NULL, "
                + COLUMN_OPERATION_DATA + " TEXT NOT NULL, "
                + COLUMN_TIMESTAMP + " INTEGER DEFAULT (strftime('%s','now') * 1000), "
                + COLUMN_RETRY_COUNT + " INTEGER DEFAULT 0"
                + ")";
        
        db.execSQL(CREATE_TABLE);
        
        // Create index
        db.execSQL("CREATE INDEX idx_timestamp ON " + TABLE_PENDING_OPERATIONS + "(" + COLUMN_TIMESTAMP + ")");
    }
    
    /**
     * Insert or update cached animal
     */
    public void insertOrUpdateCachedAnimal(AdoptablePetModel animal) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_PET_ID, animal.getPetId());
        values.put(COLUMN_NAME, animal.getName());
        values.put(COLUMN_SPECIES, animal.getSpecies());
        values.put(COLUMN_BREED, animal.getBreed());
        values.put(COLUMN_AGE, animal.getAge());
        values.put(COLUMN_SIZE, animal.getSize());
        values.put(COLUMN_GENDER, animal.getGender());
        values.put(COLUMN_DESCRIPTION, animal.getDescription());
        values.put(COLUMN_LOCATION, animal.getLocation());
        values.put(COLUMN_CONTACT_INFO, animal.getContactInfo());
        values.put(COLUMN_ADOPTION_STATUS, animal.getAdoptionStatus());
        values.put(COLUMN_DATE_ADDED, animal.getDateAdded());
        values.put(COLUMN_PRIMARY_IMAGE_URL, animal.getPrimaryImageUrl());
        values.put(COLUMN_IMAGE_URLS, gson.toJson(animal.getImageUrls()));
        values.put(COLUMN_LAST_UPDATED, animal.getLastUpdated());
        values.put(COLUMN_LAST_UPDATED_BY, animal.getLastUpdatedBy());
        values.put(COLUMN_VERSION, animal.getVersion());
        values.put(COLUMN_SYNC_STATUS, animal.getSyncStatus());
        values.put(COLUMN_IS_LOCAL_ONLY, animal.isLocalOnly() ? 1 : 0);
        values.put(COLUMN_METADATA, gson.toJson(animal.getMetadata()));
        
        db.insertWithOnConflict(TABLE_CACHED_ANIMALS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    /**
     * Get cached animals by species
     */
    public List<AdoptablePetModel> getCachedAnimals(String species) {
        List<AdoptablePetModel> animals = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selection = null;
        String[] selectionArgs = null;
        
        if (species != null) {
            selection = COLUMN_SPECIES + " = ?";
            selectionArgs = new String[]{species};
        }
        
        Cursor cursor = db.query(TABLE_CACHED_ANIMALS, null, selection, selectionArgs, 
                                null, null, COLUMN_LAST_UPDATED + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                AdoptablePetModel animal = cursorToAnimal(cursor);
                if (animal != null) {
                    animals.add(animal);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return animals;
    }
    
    /**
     * Convert cursor to AdoptablePetModel
     */
    private AdoptablePetModel cursorToAnimal(Cursor cursor) {
        try {
            AdoptablePetModel animal = new AdoptablePetModel();
            
            animal.setPetId(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PET_ID)));
            animal.setName(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME)));
            animal.setSpecies(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SPECIES)));
            animal.setBreed(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_BREED)));
            animal.setAge(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_AGE)));
            animal.setSize(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SIZE)));
            animal.setGender(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_GENDER)));
            animal.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION)));
            animal.setLocation(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LOCATION)));
            animal.setContactInfo(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_INFO)));
            animal.setAdoptionStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_ADOPTION_STATUS)));
            animal.setDateAdded(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DATE_ADDED)));
            animal.setPrimaryImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PRIMARY_IMAGE_URL)));
            
            // Parse JSON fields
            String imageUrlsJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URLS));
            if (imageUrlsJson != null) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                animal.setImageUrls(gson.fromJson(imageUrlsJson, listType));
            }
            
            animal.setLastUpdated(cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_LAST_UPDATED)));
            animal.setLastUpdatedBy(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_LAST_UPDATED_BY)));
            animal.setVersion(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_VERSION)));
            animal.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_SYNC_STATUS)));
            animal.setLocalOnly(cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_IS_LOCAL_ONLY)) == 1);
            
            String metadataJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_METADATA));
            if (metadataJson != null) {
                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                animal.setMetadata(gson.fromJson(metadataJson, mapType));
            }
            
            return animal;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to convert cursor to animal", e);
            return null;
        }
    }
    
    /**
     * Insert pending operation
     */
    public void insertPendingOperation(DataSynchronizer.PendingOperation operation) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_OPERATION_ID, operation.id);
        values.put(COLUMN_OPERATION_TYPE, operation.type.name());
        values.put(COLUMN_TABLE_NAME, operation.tableName);
        values.put(COLUMN_OPERATION_DATA, gson.toJson(operation.data));
        values.put(COLUMN_TIMESTAMP, operation.timestamp);
        values.put(COLUMN_RETRY_COUNT, operation.retryCount);
        
        db.insertWithOnConflict(TABLE_PENDING_OPERATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    /**
     * Get all pending operations
     */
    public List<DataSynchronizer.PendingOperation> getPendingOperations() {
        List<DataSynchronizer.PendingOperation> operations = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_PENDING_OPERATIONS, null, null, null, 
                                null, null, COLUMN_TIMESTAMP + " ASC");
        
        if (cursor.moveToFirst()) {
            do {
                try {
                    String id = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPERATION_ID));
                    String typeStr = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPERATION_TYPE));
                    String tableName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TABLE_NAME));
                    String dataJson = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_OPERATION_DATA));
                    long timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_TIMESTAMP));
                    int retryCount = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_RETRY_COUNT));
                    
                    DataSynchronizer.PendingOperation.Type type = 
                        DataSynchronizer.PendingOperation.Type.valueOf(typeStr);
                    
                    Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                    Map<String, Object> data = gson.fromJson(dataJson, mapType);
                    
                    DataSynchronizer.PendingOperation operation = 
                        new DataSynchronizer.PendingOperation(id, type, tableName, data);
                    operation.timestamp = timestamp;
                    operation.retryCount = retryCount;
                    
                    operations.add(operation);
                    
                } catch (Exception e) {
                    Log.e(TAG, "Failed to parse pending operation", e);
                }
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        return operations;
    }
    
    /**
     * Delete pending operation
     */
    public void deletePendingOperation(String operationId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_PENDING_OPERATIONS, COLUMN_OPERATION_ID + " = ?", new String[]{operationId});
    }
    
    /**
     * Update pending operation retry count
     */
    public void updatePendingOperationRetryCount(String operationId, int retryCount) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_RETRY_COUNT, retryCount);
        
        db.update(TABLE_PENDING_OPERATIONS, values, COLUMN_OPERATION_ID + " = ?", new String[]{operationId});
    }
    
    /**
     * Cache image info
     */
    public void cacheImageInfo(String imageUrl, String localPath, long fileSize) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE_URL, imageUrl);
        values.put(COLUMN_LOCAL_PATH, localPath);
        values.put(COLUMN_FILE_SIZE, fileSize);
        
        db.insertWithOnConflict(TABLE_CACHED_IMAGES, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }
    
    /**
     * Get cached image path
     */
    public String getCachedImagePath(String imageUrl) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_CACHED_IMAGES, new String[]{COLUMN_LOCAL_PATH}, 
                                COLUMN_IMAGE_URL + " = ?", new String[]{imageUrl}, 
                                null, null, null);
        
        String localPath = null;
        if (cursor.moveToFirst()) {
            localPath = cursor.getString(0);
            
            // Update access count and time
            updateImageAccess(imageUrl);
        }
        
        cursor.close();
        return localPath;
    }
    
    /**
     * Update image access statistics
     */
    private void updateImageAccess(String imageUrl) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCESS_COUNT, COLUMN_ACCESS_COUNT + " + 1");
        values.put(COLUMN_LAST_ACCESS, System.currentTimeMillis());
        
        db.update(TABLE_CACHED_IMAGES, values, COLUMN_IMAGE_URL + " = ?", new String[]{imageUrl});
    }
    
    /**
     * Clean up old cached data
     */
    public void cleanupOldData(long maxAge) {
        SQLiteDatabase db = this.getWritableDatabase();
        long cutoffTime = System.currentTimeMillis() - maxAge;
        
        // Clean up old cached animals
        int deletedAnimals = db.delete(TABLE_CACHED_ANIMALS, 
                                      COLUMN_CACHED_AT + " < ?", 
                                      new String[]{String.valueOf(cutoffTime)});
        
        // Clean up old cached images
        int deletedImages = db.delete(TABLE_CACHED_IMAGES, 
                                     COLUMN_CACHE_TIME + " < ?", 
                                     new String[]{String.valueOf(cutoffTime)});
        
        // Clean up old pending operations (older than 7 days)
        long operationCutoff = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000);
        int deletedOperations = db.delete(TABLE_PENDING_OPERATIONS, 
                                         COLUMN_TIMESTAMP + " < ?", 
                                         new String[]{String.valueOf(operationCutoff)});
        
        Log.d(TAG, "Cleanup completed: " + deletedAnimals + " animals, " + 
              deletedImages + " images, " + deletedOperations + " operations");
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStats getStats() {
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Count cached animals
        Cursor animalCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CACHED_ANIMALS, null);
        int animalCount = 0;
        if (animalCursor.moveToFirst()) {
            animalCount = animalCursor.getInt(0);
        }
        animalCursor.close();
        
        // Count cached images
        Cursor imageCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_CACHED_IMAGES, null);
        int imageCount = 0;
        if (imageCursor.moveToFirst()) {
            imageCount = imageCursor.getInt(0);
        }
        imageCursor.close();
        
        // Count pending operations
        Cursor opCursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_PENDING_OPERATIONS, null);
        int operationCount = 0;
        if (opCursor.moveToFirst()) {
            operationCount = opCursor.getInt(0);
        }
        opCursor.close();
        
        return new DatabaseStats(animalCount, imageCount, operationCount);
    }
    
    /**
     * Database statistics data class
     */
    public static class DatabaseStats {
        public final int cachedAnimals;
        public final int cachedImages;
        public final int pendingOperations;
        
        public DatabaseStats(int cachedAnimals, int cachedImages, int pendingOperations) {
            this.cachedAnimals = cachedAnimals;
            this.cachedImages = cachedImages;
            this.pendingOperations = pendingOperations;
        }
    }
    
    // Additional methods for shop functionality compatibility
    
    public android.database.Cursor getProductsByCategory(String category) {
        // Return empty cursor for compatibility - shop functionality not implemented in this version
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT 1 WHERE 0", null); // Empty result set
    }
    
    public void insertProduct(String name, String price, String category, String type) {
        // No-op for compatibility - shop functionality not implemented in this version
        Log.d(TAG, "insertProduct called but not implemented in this version");
    }
}