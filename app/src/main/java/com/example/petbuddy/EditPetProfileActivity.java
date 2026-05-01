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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
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

public class EditPetProfileActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final int REQUEST_STORAGE_PERMISSION = 101;
    private static final int REQUEST_CAMERA = 102;
    private static final int REQUEST_GALLERY = 103;

    private AdoptablePetModel pet;
    private DatabaseReference petsRef;
    
    // Pet Photo
    private ImageView imgPetPhoto;
    private Button btnChangePhoto, btnRemovePhoto;
    private String currentImageUrl = "";
    
    // Basic Information
    private EditText editPetName, editBreed, editColor, editAge;
    private Spinner spinnerSpecies, spinnerGender, spinnerSize;
    
    // Description
    private EditText editDescription, editPersonality, editSpecialTraits;
    
    // Health Information
    private Spinner spinnerHealthStatus;
    private CheckBox checkVaccinated, checkNeutered;
    private EditText editMedicalCondition, editLastCheckup, editSpecialNeeds;
    
    // Location & Shelter
    private EditText editCity, editShelterName, editShelterLocation, editShelterContact;
    
    // Adoption Requirements
    private EditText editMinAge, editHomeEnvironment, editExperience, editAdditionalRequirements;
    
    // Contact Information
    private EditText editContactName, editContactPhone, editContactEmail;
    
    // Adoption Details
    private EditText editAdoptionFee;
    private Spinner spinnerAdoptionStatus;
    
    private Button btnSave, btnCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_pet_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("👑 Edit Pet Profile");
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
        loadPetData();
        setupSpinners();
        setupButtons();
    }

    private void initializeViews() {
        // Pet Photo
        imgPetPhoto = findViewById(R.id.imgPetPhoto);
        btnChangePhoto = findViewById(R.id.btnChangePhoto);
        btnRemovePhoto = findViewById(R.id.btnRemovePhoto);
        
        // Basic Information
        editPetName = findViewById(R.id.editPetName);
        editBreed = findViewById(R.id.editBreed);
        editColor = findViewById(R.id.editColor);
        editAge = findViewById(R.id.editAge);
        spinnerSpecies = findViewById(R.id.spinnerSpecies);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerSize = findViewById(R.id.spinnerSize);
        
        // Description
        editDescription = findViewById(R.id.editDescription);
        editPersonality = findViewById(R.id.editPersonality);
        editSpecialTraits = findViewById(R.id.editSpecialTraits);
        
        // Health Information
        spinnerHealthStatus = findViewById(R.id.spinnerHealthStatus);
        checkVaccinated = findViewById(R.id.checkVaccinated);
        checkNeutered = findViewById(R.id.checkNeutered);
        editMedicalCondition = findViewById(R.id.editMedicalCondition);
        editLastCheckup = findViewById(R.id.editLastCheckup);
        editSpecialNeeds = findViewById(R.id.editSpecialNeeds);
        
        // Location & Shelter
        editCity = findViewById(R.id.editCity);
        editShelterName = findViewById(R.id.editShelterName);
        editShelterLocation = findViewById(R.id.editShelterLocation);
        editShelterContact = findViewById(R.id.editShelterContact);
        
        // Adoption Requirements
        editMinAge = findViewById(R.id.editMinAge);
        editHomeEnvironment = findViewById(R.id.editHomeEnvironment);
        editExperience = findViewById(R.id.editExperience);
        editAdditionalRequirements = findViewById(R.id.editAdditionalRequirements);
        
        // Contact Information
        editContactName = findViewById(R.id.editContactName);
        editContactPhone = findViewById(R.id.editContactPhone);
        editContactEmail = findViewById(R.id.editContactEmail);
        
        // Adoption Details
        editAdoptionFee = findViewById(R.id.editAdoptionFee);
        spinnerAdoptionStatus = findViewById(R.id.spinnerAdoptionStatus);
        
        // Buttons
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        petsRef = database.getReference("adoptablePets");
    }

    private void loadPetData() {
        String petId = getIntent().getStringExtra("petId");
        if (petId == null) {
            Toast.makeText(this, "Pet ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Get pet data from intent or load from Firebase
        pet = (AdoptablePetModel) getIntent().getSerializableExtra("pet");
        if (pet == null) {
            Toast.makeText(this, "Pet data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateFields();
    }

    private void setupSpinners() {
        // Species Spinner
        String[] speciesOptions = {"Dog", "Cat", "Rabbit", "Bird", "Hamster"};
        ArrayAdapter<String> speciesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, speciesOptions);
        speciesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSpecies.setAdapter(speciesAdapter);

        // Gender Spinner
        String[] genderOptions = {"Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);

        // Size Spinner
        String[] sizeOptions = {"Small", "Medium", "Large"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sizeOptions);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSize.setAdapter(sizeAdapter);

        // Health Status Spinner
        String[] healthOptions = {"Excellent", "Good", "Fair", "Needs Medical Attention"};
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, healthOptions);
        healthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHealthStatus.setAdapter(healthAdapter);

        // Adoption Status Spinner
        String[] adoptionOptions = {"Available", "Reserved", "Adopted"};
        ArrayAdapter<String> adoptionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, adoptionOptions);
        adoptionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAdoptionStatus.setAdapter(adoptionAdapter);
    }

    private void populateFields() {
        if (pet == null) return;

        // Pet Photo
        currentImageUrl = pet.getImageUrl();
        loadPetImage();

        // Basic Information
        editPetName.setText(pet.getName());
        editBreed.setText(pet.getBreed());
        editColor.setText(pet.getColor());
        editAge.setText(String.valueOf(pet.getAge()));
        
        setSpinnerSelection(spinnerSpecies, pet.getSpecies());
        setSpinnerSelection(spinnerGender, pet.getGender());
        setSpinnerSelection(spinnerSize, pet.getSize());
        
        // Description
        editDescription.setText(pet.getDescription());
        editPersonality.setText(pet.getPersonality() != null ? pet.getPersonality() : pet.getTemperament());
        editSpecialTraits.setText(pet.getSpecialTraits());
        
        // Health Information
        setSpinnerSelection(spinnerHealthStatus, pet.getHealthStatus());
        checkVaccinated.setChecked(pet.isVaccinated());
        checkNeutered.setChecked(pet.isNeutered());
        editMedicalCondition.setText(pet.getMedicalCondition());
        editLastCheckup.setText(pet.getLastHealthCheckDate());
        editSpecialNeeds.setText(pet.getSpecialNeeds());
        
        // Location & Shelter
        editCity.setText(pet.getCity());
        editShelterName.setText(pet.getShelterName());
        editShelterLocation.setText(pet.getShelterLocation());
        editShelterContact.setText(pet.getShelterContact());
        
        // Adoption Requirements
        editMinAge.setText(String.valueOf(pet.getMinimumAdopterAge()));
        editHomeEnvironment.setText(pet.getHomeEnvironmentRequired());
        editExperience.setText(pet.getExperienceRequired());
        editAdditionalRequirements.setText(pet.getAdditionalRequirements());
        
        // Contact Information
        editContactName.setText(pet.getContactPersonName());
        editContactPhone.setText(pet.getContactPhone());
        editContactEmail.setText(pet.getContactEmail());
        
        // Adoption Details
        editAdoptionFee.setText(String.valueOf(pet.getAdoptionFee()));
        setSpinnerSelection(spinnerAdoptionStatus, pet.getAdoptionStatus());
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
        btnSave.setOnClickListener(v -> savePetProfile());
        btnCancel.setOnClickListener(v -> finish());
        
        // Photo buttons
        btnChangePhoto.setOnClickListener(v -> showPhotoSelectionDialog());
        btnRemovePhoto.setOnClickListener(v -> removePhoto());
    }

    private void savePetProfile() {
        if (!validateFields()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        
        // Basic Information
        updates.put("name", editPetName.getText().toString().trim());
        updates.put("breed", editBreed.getText().toString().trim());
        updates.put("color", editColor.getText().toString().trim());
        updates.put("age", Integer.parseInt(editAge.getText().toString().trim()));
        updates.put("species", spinnerSpecies.getSelectedItem().toString().toLowerCase());
        updates.put("gender", spinnerGender.getSelectedItem().toString().toLowerCase());
        updates.put("size", spinnerSize.getSelectedItem().toString().toLowerCase());
        
        // Description
        updates.put("description", editDescription.getText().toString().trim());
        updates.put("personality", editPersonality.getText().toString().trim());
        updates.put("temperament", editPersonality.getText().toString().trim()); // Keep both for compatibility
        updates.put("specialTraits", editSpecialTraits.getText().toString().trim());
        
        // Health Information
        updates.put("healthStatus", spinnerHealthStatus.getSelectedItem().toString());
        updates.put("vaccinated", checkVaccinated.isChecked());
        updates.put("neutered", checkNeutered.isChecked());
        updates.put("medicalCondition", editMedicalCondition.getText().toString().trim());
        updates.put("lastHealthCheckDate", editLastCheckup.getText().toString().trim());
        updates.put("specialNeeds", editSpecialNeeds.getText().toString().trim());
        
        // Location & Shelter
        updates.put("city", editCity.getText().toString().trim());
        updates.put("shelterName", editShelterName.getText().toString().trim());
        updates.put("shelterLocation", editShelterLocation.getText().toString().trim());
        updates.put("shelterContact", editShelterContact.getText().toString().trim());
        
        // Adoption Requirements
        updates.put("minimumAdopterAge", Integer.parseInt(editMinAge.getText().toString().trim()));
        updates.put("homeEnvironmentRequired", editHomeEnvironment.getText().toString().trim());
        updates.put("experienceRequired", editExperience.getText().toString().trim());
        updates.put("additionalRequirements", editAdditionalRequirements.getText().toString().trim());
        
        // Contact Information
        updates.put("contactPersonName", editContactName.getText().toString().trim());
        updates.put("contactPhone", editContactPhone.getText().toString().trim());
        updates.put("contactEmail", editContactEmail.getText().toString().trim());
        
        // Adoption Details
        updates.put("adoptionFee", Double.parseDouble(editAdoptionFee.getText().toString().trim()));
        String adoptionStatus = spinnerAdoptionStatus.getSelectedItem().toString().toLowerCase();
        updates.put("adoptionStatus", adoptionStatus);
        updates.put("available", "available".equals(adoptionStatus));
        
        // Pet Image
        updates.put("imageUrl", currentImageUrl);

        // Save to Firebase
        petsRef.child(pet.getPetId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Pet profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to update pet profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateFields() {
        // Basic validation
        if (editPetName.getText().toString().trim().isEmpty()) {
            editPetName.setError("Pet name is required");
            return false;
        }
        
        if (editBreed.getText().toString().trim().isEmpty()) {
            editBreed.setError("Breed is required");
            return false;
        }
        
        try {
            int age = Integer.parseInt(editAge.getText().toString().trim());
            if (age <= 0) {
                editAge.setError("Age must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            editAge.setError("Invalid age");
            return false;
        }
        
        if (editDescription.getText().toString().trim().isEmpty()) {
            editDescription.setError("Description is required");
            return false;
        }
        
        try {
            int minAge = Integer.parseInt(editMinAge.getText().toString().trim());
            if (minAge < 16 || minAge > 100) {
                editMinAge.setError("Minimum age should be between 16-100");
                return false;
            }
        } catch (NumberFormatException e) {
            editMinAge.setError("Invalid minimum age");
            return false;
        }
        
        try {
            double fee = Double.parseDouble(editAdoptionFee.getText().toString().trim());
            if (fee < 0) {
                editAdoptionFee.setError("Adoption fee cannot be negative");
                return false;
            }
        } catch (NumberFormatException e) {
            editAdoptionFee.setError("Invalid adoption fee");
            return false;
        }
        
        return true;
    }

    // ================= PHOTO MANAGEMENT METHODS =================
    
    private void loadPetImage() {
        if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
            // If we have a Base64 encoded image
            if (currentImageUrl.startsWith("data:image")) {
                try {
                    String base64String = currentImageUrl.substring(currentImageUrl.indexOf(",") + 1);
                    byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgPetPhoto.setImageBitmap(decodedByte);
                } catch (Exception e) {
                    setDefaultPetImage();
                }
            } else {
                // For now, use default image (Firebase Storage URLs can be loaded with Glide/Picasso)
                setDefaultPetImage();
            }
        } else {
            setDefaultPetImage();
        }
    }

    private void setDefaultPetImage() {
        if (pet != null && pet.getSpecies() != null) {
            switch (pet.getSpecies().toLowerCase()) {
                case "dog":
                    imgPetPhoto.setImageResource(R.drawable.default_dog_image);
                    break;
                case "cat":
                    imgPetPhoto.setImageResource(R.drawable.default_cat_image);
                    break;
                case "rabbit":
                    imgPetPhoto.setImageResource(R.drawable.default_rabbit_image);
                    break;
                case "bird":
                    imgPetPhoto.setImageResource(R.drawable.default_bird_image);
                    break;
                default:
                    imgPetPhoto.setImageResource(R.drawable.default_pet_image);
                    break;
            }
        } else {
            imgPetPhoto.setImageResource(R.drawable.default_pet_image);
        }
    }

    private void showPhotoSelectionDialog() {
        String[] options = {"📷 Take Photo", "🖼️ Choose from Gallery"};
        
        new AlertDialog.Builder(this)
                .setTitle("Select Pet Photo")
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
                .setMessage("Are you sure you want to remove the pet's photo?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    currentImageUrl = "";
                    setDefaultPetImage();
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
            imgPetPhoto.setImageBitmap(resizedBitmap);
            
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

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}