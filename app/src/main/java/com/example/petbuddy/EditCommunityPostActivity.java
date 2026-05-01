package com.example.petbuddy;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditCommunityPostActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_GALLERY = 103;

    private BlogPostModel post;
    private DatabaseReference postsRef;
    
    // Post Information
    private EditText editPostTitle, editPostContent, editAuthorName;
    private Spinner spinnerPostCategory;
    private ImageView imgPostPhoto;
    private Button btnChangePhoto, btnRemovePhoto;
    private Button btnSave, btnCancel;
    
    private String currentImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_community_post);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("👑 Edit Community Post");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Check admin access
        if (!AdminHelper.isCurrentUserAdmin(this)) {
            Toast.makeText(this, "❌ Admin access required!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        initializeFirebase();
        loadPostData();
        setupSpinners();
        setupButtons();
    }

    private void initializeViews() {
        editPostTitle = findViewById(R.id.editPostTitle);
        editPostContent = findViewById(R.id.editPostContent);
        editAuthorName = findViewById(R.id.editAuthorName);
        spinnerPostCategory = findViewById(R.id.spinnerPostCategory);
        imgPostPhoto = findViewById(R.id.imgPostPhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        postsRef = database.getReference("communityPosts");
    }

    private void loadPostData() {
        String postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            Toast.makeText(this, "Post ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        post = (BlogPostModel) getIntent().getSerializableExtra("post");
        if (post == null) {
            Toast.makeText(this, "Post data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateFields();
    }

    private void setupSpinners() {
        String[] categoryOptions = {"Pet Care Tips", "Pet Stories", "Community News", "Pet Health", "Training Tips", "General Discussion"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPostCategory.setAdapter(categoryAdapter);
    }

    private void populateFields() {
        if (post == null) return;

        editPostTitle.setText(post.getTitle());
        editPostContent.setText(post.getContent());
        editAuthorName.setText(post.getAuthor());
        
        // Set category spinner
        setSpinnerSelection(spinnerPostCategory, post.getCategory());
        
        // Load post image
        currentImageUrl = post.getImageUrl();
        loadPostImage();
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (value == null) return;
        
        ArrayAdapter<String> adapter = (ArrayAdapter<String>) spinner.getAdapter();
        for (int i = 0; i < adapter.getCount(); i++) {
            if (adapter.getItem(i).equalsIgnoreCase(value)) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setupButtons() {
        btnSave.setOnClickListener(v -> savePost());
        btnCancel.setOnClickListener(v -> finish());
        btnChangePhoto.setOnClickListener(v -> showPhotoSelectionDialog());
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }

    private void loadPostImage() {
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            // If we have a Base64 encoded image
            if (currentImageUrl.startsWith("data:image")) {
                try {
                    String base64String = currentImageUrl.substring(currentImageUrl.indexOf(",") + 1);
                    byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgPostPhoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    setDefaultPostImage();
                }
            } else {
                setDefaultPostImage();
            }
        } else {
            setDefaultPostImage();
        }
    }

    private void setDefaultPostImage() {
        imgPostPhoto.setImageResource(R.drawable.default_pet_image);
    }

    private void showPhotoSelectionDialog() {
        String[] options = {"📷 Take Photo", "🖼️ Choose from Gallery"};
        
        new AlertDialog.Builder(this)
                .setTitle("Select Post Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            checkCameraPermissionAndTakePhoto();
                            break;
                        case 1:
                            checkStoragePermissionAndSelectFromGallery();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkCameraPermissionAndTakePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            takePhoto();
        }
    }

    private void checkStoragePermissionAndSelectFromGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
        } else {
            selectFromGallery();
        }
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void selectFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void removePhoto() {
        new AlertDialog.Builder(this)
                .setTitle("Remove Photo")
                .setMessage("Are you sure you want to remove the post photo?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    currentImageUrl = "";
                    setDefaultPostImage();
                    Toast.makeText(this, "Photo removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhoto();
                } else {
                    Toast.makeText(this, "Camera permission required to take photos", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    selectFromGallery();
                } else {
                    Toast.makeText(this, "Storage permission required to select photos", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CAMERA:
                    if (data != null && data.getExtras() != null) {
                        Bitmap bitmap = (Bitmap) data.getExtras().get("data");
                        if (bitmap != null) {
                            processSelectedImage(bitmap);
                        }
                    }
                    break;
                case REQUEST_GALLERY:
                    if (data != null && data.getData() != null) {
                        Uri selectedImageUri = data.getData();
                        try {
                            InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
                            if (bitmap != null) {
                                processSelectedImage(bitmap);
                            }
                        } catch (FileNotFoundException e) {
                            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
            }
        }
    }

    private void processSelectedImage(Bitmap bitmap) {
        try {
            // Resize image to reasonable size
            Bitmap resizedBitmap = resizeBitmap(bitmap, 500, 500);
            
            // Convert to Base64
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64String = Base64.encodeToString(byteArray, Base64.DEFAULT);
            
            // Store as data URL
            currentImageUrl = "data:image/jpeg;base64," + base64String;
            
            // Display the image
            imgPostPhoto.setImageBitmap(resizedBitmap);
            
            Toast.makeText(this, "Photo updated successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Error processing image", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void savePost() {
        if (!validateFields()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        
        updates.put("title", editPostTitle.getText().toString().trim());
        updates.put("content", editPostContent.getText().toString().trim());
        updates.put("author", editAuthorName.getText().toString().trim());
        updates.put("category", spinnerPostCategory.getSelectedItem().toString());
        updates.put("imageUrl", currentImageUrl);
        updates.put("lastModified", System.currentTimeMillis());

        // Save to Firebase
        postsRef.child(post.getPostId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Post updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to update post: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateFields() {
        if (editPostTitle.getText().toString().trim().isEmpty()) {
            editPostTitle.setError("Post title is required");
            return false;
        }
        
        if (editPostContent.getText().toString().trim().isEmpty()) {
            editPostContent.setError("Post content is required");
            return false;
        }
        
        if (editAuthorName.getText().toString().trim().isEmpty()) {
            editAuthorName.setError("Author name is required");
            return false;
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}