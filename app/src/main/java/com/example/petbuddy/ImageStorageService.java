package com.example.petbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing animal image storage and retrieval from Firebase Storage
 */
public class ImageStorageService {
    private static final String TAG = "ImageStorageService";
    private static final int MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final int COMPRESSED_QUALITY = 85; // 85% quality for compression
    private static final int MAX_DIMENSION = 1200; // Max width/height for compression
    
    private static ImageStorageService instance;
    private final FirebaseManager firebaseManager;
    private final Context context;
    
    private ImageStorageService(Context context) {
        this.context = context.getApplicationContext();
        this.firebaseManager = FirebaseManager.getInstance();
    }
    
    public static synchronized ImageStorageService getInstance(Context context) {
        if (instance == null) {
            instance = new ImageStorageService(context);
        }
        return instance;
    }
    
    /**
     * Upload a single image for an animal
     */
    public void uploadImage(String animalId, Bitmap image, UploadCallback callback) {
        uploadImageToReference(firebaseManager.getAnimalImagesRef(), animalId, image, callback);
    }
    
    /**
     * Upload an injury photo
     */
    public void uploadInjuryPhoto(String injuryPhotoId, Bitmap image, UploadCallback callback) {
        uploadImageToReference(firebaseManager.getInjuryPhotosRef(), injuryPhotoId, image, callback);
    }
    
    /**
     * Generic method to upload image to a specific storage reference
     */
    private void uploadImageToReference(StorageReference storageRef, String imageId, Bitmap image, UploadCallback callback) {
        if (image == null) {
            callback.onFailure(new IllegalArgumentException("Image cannot be null"));
            return;
        }
        
        try {
            // Compress image
            Bitmap compressedImage = compressImage(image);
            byte[] imageData = bitmapToByteArray(compressedImage);
            
            // Validate size
            if (imageData.length > MAX_IMAGE_SIZE) {
                callback.onFailure(new IllegalArgumentException("Image size exceeds 5MB limit"));
                return;
            }
            
            // Generate unique filename
            String fileName = imageId + "_" + UUID.randomUUID().toString() + ".jpg";
            StorageReference imageRef = storageRef.child(fileName);
            
            // Upload image
            UploadTask uploadTask = imageRef.putBytes(imageData);
            uploadTask.addOnSuccessListener(taskSnapshot -> {
                // Get download URL
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Log.d(TAG, "Image uploaded successfully: " + uri.toString());
                    callback.onSuccess(uri.toString());
                }).addOnFailureListener(callback::onFailure);
            }).addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to upload image", exception);
                callback.onFailure(exception);
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image upload", e);
            callback.onFailure(e);
        }
    }
    
    /**
     * Upload multiple images for an animal
     */
    public void uploadMultipleImages(String animalId, List<Bitmap> images, MultiUploadCallback callback) {
        if (images == null || images.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Images list cannot be null or empty"));
            return;
        }
        
        List<String> uploadedUrls = new ArrayList<>();
        List<Exception> errors = new ArrayList<>();
        final int totalImages = images.size();
        
        for (int i = 0; i < images.size(); i++) {
            final int index = i;
            Bitmap image = images.get(i);
            
            uploadImage(animalId, image, new UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    synchronized (uploadedUrls) {
                        uploadedUrls.add(imageUrl);
                        
                        // Check if all uploads completed
                        if (uploadedUrls.size() + errors.size() == totalImages) {
                            if (errors.isEmpty()) {
                                callback.onSuccess(uploadedUrls);
                            } else {
                                callback.onPartialSuccess(uploadedUrls, errors);
                            }
                        }
                    }
                }
                
                @Override
                public void onFailure(Exception exception) {
                    synchronized (uploadedUrls) {
                        errors.add(exception);
                        
                        // Check if all uploads completed
                        if (uploadedUrls.size() + errors.size() == totalImages) {
                            if (uploadedUrls.isEmpty()) {
                                callback.onFailure(new Exception("All image uploads failed"));
                            } else {
                                callback.onPartialSuccess(uploadedUrls, errors);
                            }
                        }
                    }
                }
            });
        }
    }
    
    /**
     * Load image from URL using Glide (to be implemented in adapter)
     */
    public void loadImage(String imageUrl, ImageLoadCallback callback) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            callback.onFailure(new IllegalArgumentException("Image URL cannot be null or empty"));
            return;
        }
        
        // This method provides the interface for loading images
        // Actual implementation will be in the adapter using Glide
        callback.onImageReady(imageUrl);
    }
    
    /**
     * Delete images for an animal
     */
    public void deleteImages(String animalId, DeleteCallback callback) {
        StorageReference animalImagesRef = firebaseManager.getAnimalImagesRef();
        
        // List all images for this animal
        animalImagesRef.listAll().addOnSuccessListener(listResult -> {
            List<StorageReference> imagesToDelete = new ArrayList<>();
            
            // Filter images that belong to this animal
            for (StorageReference item : listResult.getItems()) {
                if (item.getName().startsWith(animalId + "_")) {
                    imagesToDelete.add(item);
                }
            }
            
            if (imagesToDelete.isEmpty()) {
                callback.onSuccess();
                return;
            }
            
            // Delete each image
            int[] deletedCount = {0};
            int[] errorCount = {0};
            
            for (StorageReference imageRef : imagesToDelete) {
                imageRef.delete().addOnSuccessListener(aVoid -> {
                    deletedCount[0]++;
                    if (deletedCount[0] + errorCount[0] == imagesToDelete.size()) {
                        if (errorCount[0] == 0) {
                            callback.onSuccess();
                        } else {
                            callback.onFailure(new Exception("Some images failed to delete"));
                        }
                    }
                }).addOnFailureListener(exception -> {
                    errorCount[0]++;
                    Log.e(TAG, "Failed to delete image: " + imageRef.getName(), exception);
                    if (deletedCount[0] + errorCount[0] == imagesToDelete.size()) {
                        callback.onFailure(new Exception("Some images failed to delete"));
                    }
                });
            }
            
        }).addOnFailureListener(callback::onFailure);
    }
    
    /**
     * Compress image to optimize storage and loading
     */
    private Bitmap compressImage(Bitmap original) {
        int width = original.getWidth();
        int height = original.getHeight();
        
        // Calculate new dimensions if image is too large
        if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            float ratio = Math.min((float) MAX_DIMENSION / width, (float) MAX_DIMENSION / height);
            int newWidth = Math.round(width * ratio);
            int newHeight = Math.round(height * ratio);
            
            return Bitmap.createScaledBitmap(original, newWidth, newHeight, true);
        }
        
        return original;
    }
    
    /**
     * Convert bitmap to byte array with compression
     */
    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSED_QUALITY, baos);
        return baos.toByteArray();
    }
    
    /**
     * Create bitmap from byte array
     */
    public Bitmap byteArrayToBitmap(byte[] byteArray) {
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.length);
    }
    
    /**
     * Validate image format and size
     */
    public boolean isValidImage(Uri imageUri) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            
            BitmapFactory.decodeFile(imageUri.getPath(), options);
            
            // Check if it's a valid image
            if (options.outWidth == -1 || options.outHeight == -1) {
                return false;
            }
            
            // Check MIME type
            String mimeType = options.outMimeType;
            return mimeType != null && (mimeType.equals("image/jpeg") || mimeType.equals("image/png"));
            
        } catch (Exception e) {
            Log.e(TAG, "Error validating image", e);
            return false;
        }
    }
    
    // Callback interfaces
    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(Exception exception);
    }
    
    public interface MultiUploadCallback {
        void onSuccess(List<String> imageUrls);
        void onPartialSuccess(List<String> successUrls, List<Exception> errors);
        void onFailure(Exception exception);
    }
    
    public interface ImageLoadCallback {
        void onImageReady(String imageUrl);
        void onFailure(Exception exception);
    }
    
    public interface DeleteCallback {
        void onSuccess();
        void onFailure(Exception exception);
    }
}