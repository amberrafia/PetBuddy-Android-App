package com.example.petbuddy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.util.UUID;

/**
 * Activity for adding new pets to the adoption database
 */
public class AddPetActivity extends AppCompatActivity {
    private static final String TAG = "AddPetActivity";
    private static final int PICK_IMAGE_REQUEST = 1;
    
    // UI Components
    private TextInputEditText etPetName, etBreed, etAge, etColor;
    private TextInputEditText etDescription, etPersonality, etSpecialNeeds;
    private TextInputEditText etShelterName, etShelterLocation;
    private TextInputEditText etContactPerson, etContactPhone, etAdoptionFee;
    private RadioGroup rgSpecies;
    private RadioButton rbDog, rbCat;
    private Spinner spinnerGender, spinnerSize, spinnerHealthStatus;
    private CheckBox cbVaccinated, cbNeutered;
    private ImageView ivPetPhoto;
    private Button btnSelectPhoto, btnSavePet, btnCancel;
    
    // Services
    private AnimalDataService animalDataService;
    private ImageStorageService imageStorageService;
    
    // Data
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;
    private ProgressDialog progressDialog;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_pet);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("👑 Add New Pet");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        
        initializeServices();
        initializeViews();
        setupSpinners();
        setupClickListeners();
    }
    
    private void initializeServices() {
        animalDataService = AnimalDataService.getInstance();
        imageStorageService = ImageStorageService.getInstance(this);
    }
    
    private void initializeViews() {
        // Basic Information
        etPetName = findViewById(R.id.etPetName);
        etBreed = findViewById(R.id.etBreed);
        etAge = findViewById(R.id.etAge);
        etColor = findViewById(R.id.etColor);
        rgSpecies = findViewById(R.id.rgSpecies);
        rbDog = findViewById(R.id.rbDog);
        rbCat = findViewById(R.id.rbCat);
        spinnerGender = findViewById(R.id.spinnerGender);
        spinnerSize = findViewById(R.id.spinnerSize);
        
        // Description
        etDescription = findViewById(R.id.etDescription);
        etPersonality = findViewById(R.id.etPersonality);
        
        // Health Information
        spinnerHealthStatus = findViewById(R.id.spinnerHealthStatus);
        cbVaccinated = findViewById(R.id.cbVaccinated);
        cbNeutered = findViewById(R.id.cbNeutered);
        etSpecialNeeds = findViewById(R.id.etSpecialNeeds);
        
        // Shelter Information
        etShelterName = findViewById(R.id.etShelterName);
        etShelterLocation = findViewById(R.id.etShelterLocation);
        
        // Contact Information
        etContactPerson = findViewById(R.id.etContactPerson);
        etContactPhone = findViewById(R.id.etContactPhone);
        etAdoptionFee = findViewById(R.id.etAdoptionFee);
        
        // Image
        ivPetPhoto = findViewById(R.id.ivPetPhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        
        // Action Buttons
        btnSavePet = findViewById(R.id.btnSavePet);
        btnCancel = findViewById(R.id.btnCancel);
        
        // Set default values
        rbDog.setChecked(true);
        cbVaccinated.setChecked(true);
        etAdoptionFee.setText("0");
    }
    
    private void setupSpinners() {
        // Gender Spinner
        String[] genders = {"Select Gender", "Male", "Female"};
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, genders);
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerGender.setAdapter(genderAdapter);
        
        // Size Spinner
        String[] sizes = {"Select Size", "Small", "Medium", "Large"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, sizes);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSize.setAdapter(sizeAdapter);
        
        // Health Status Spinner
        String[] healthStatuses = {"Select Health Status", "Excellent", "Good", "Fair", "Needs Medical Attention"};
        ArrayAdapter<String> healthAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, healthStatuses);
        healthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHealthStatus.setAdapter(healthAdapter);
    }
    
    private void setupClickListeners() {
        btnSelectPhoto.setOnClickListener(v -> selectImage());
        btnSavePet.setOnClickListener(v -> savePet());
        btnCancel.setOnClickListener(v -> finish());
    }
    
    private void selectImage() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Pet Photo"), PICK_IMAGE_REQUEST);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ivPetPhoto.setImageBitmap(selectedImageBitmap);
                Toast.makeText(this, "Photo selected successfully!", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                Log.e(TAG, "Error loading selected image", e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void savePet() {
        if (!validateInput()) {
            return;
        }
        
        showProgressDialog("Saving pet information...");
        
        // Create pet model
        AdoptablePetModel pet = createPetFromInput();
        
        if (selectedImageBitmap != null) {
            // Upload image first, then save pet data
            String imageId = "pet_" + UUID.randomUUID().toString();
            imageStorageService.uploadImage(imageId, selectedImageBitmap, new ImageStorageService.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    pet.setImageUrl(imageUrl);
                    savePetToDatabase(pet);
                }
                
                @Override
                public void onFailure(Exception exception) {
                    hideProgressDialog();
                    Log.e(TAG, "Failed to upload image", exception);
                    Toast.makeText(AddPetActivity.this, "Failed to upload image: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                    
                    // Save without image
                    savePetToDatabase(pet);
                }
            });
        } else {
            // Save without image
            savePetToDatabase(pet);
        }
    }
    
    private boolean validateInput() {
        // Check required fields
        if (TextUtils.isEmpty(etPetName.getText())) {
            etPetName.setError("Pet name is required");
            etPetName.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etBreed.getText())) {
            etBreed.setError("Breed is required");
            etBreed.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etAge.getText())) {
            etAge.setError("Age is required");
            etAge.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etColor.getText())) {
            etColor.setError("Color is required");
            etColor.requestFocus();
            return false;
        }
        
        if (spinnerGender.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (spinnerSize.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select size", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (TextUtils.isEmpty(etDescription.getText())) {
            etDescription.setError("Description is required");
            etDescription.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etPersonality.getText())) {
            etPersonality.setError("Personality traits are required");
            etPersonality.requestFocus();
            return false;
        }
        
        if (spinnerHealthStatus.getSelectedItemPosition() == 0) {
            Toast.makeText(this, "Please select health status", Toast.LENGTH_SHORT).show();
            return false;
        }
        
        if (TextUtils.isEmpty(etShelterName.getText())) {
            etShelterName.setError("Shelter name is required");
            etShelterName.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etShelterLocation.getText())) {
            etShelterLocation.setError("Shelter location is required");
            etShelterLocation.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etContactPerson.getText())) {
            etContactPerson.setError("Contact person is required");
            etContactPerson.requestFocus();
            return false;
        }
        
        if (TextUtils.isEmpty(etContactPhone.getText())) {
            etContactPhone.setError("Contact phone is required");
            etContactPhone.requestFocus();
            return false;
        }
        
        // Validate age is a number
        try {
            int age = Integer.parseInt(etAge.getText().toString());
            if (age <= 0) {
                etAge.setError("Age must be greater than 0");
                etAge.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etAge.setError("Please enter a valid age");
            etAge.requestFocus();
            return false;
        }
        
        // Validate adoption fee is a number
        try {
            double fee = Double.parseDouble(etAdoptionFee.getText().toString());
            if (fee < 0) {
                etAdoptionFee.setError("Adoption fee cannot be negative");
                etAdoptionFee.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            etAdoptionFee.setError("Please enter a valid adoption fee");
            etAdoptionFee.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private AdoptablePetModel createPetFromInput() {
        AdoptablePetModel pet = new AdoptablePetModel();
        
        // Generate unique ID
        pet.setPetId("pet_" + UUID.randomUUID().toString());
        
        // Basic Information
        pet.setName(etPetName.getText().toString().trim());
        pet.setSpecies(rbDog.isChecked() ? "dog" : "cat");
        pet.setBreed(etBreed.getText().toString().trim());
        pet.setAge(Integer.parseInt(etAge.getText().toString()));
        pet.setGender(spinnerGender.getSelectedItem().toString().toLowerCase());
        pet.setSize(spinnerSize.getSelectedItem().toString().toLowerCase());
        pet.setColor(etColor.getText().toString().trim());
        
        // Description
        pet.setDescription(etDescription.getText().toString().trim());
        pet.setPersonality(etPersonality.getText().toString().trim());
        
        // Health Information
        pet.setHealthStatus(spinnerHealthStatus.getSelectedItem().toString());
        pet.setVaccinated(cbVaccinated.isChecked());
        pet.setNeutered(cbNeutered.isChecked());
        pet.setSpecialNeeds(etSpecialNeeds.getText().toString().trim());
        
        // Shelter Information
        pet.setShelterName(etShelterName.getText().toString().trim());
        pet.setShelterLocation(etShelterLocation.getText().toString().trim());
        
        // Contact Information
        pet.setContactPersonName(etContactPerson.getText().toString().trim());
        pet.setContactPhone(etContactPhone.getText().toString().trim());
        pet.setAdoptionFee(Double.parseDouble(etAdoptionFee.getText().toString()));
        
        // Metadata
        pet.setDateAdded(System.currentTimeMillis());
        pet.setAdoptionStatus("available");
        
        return pet;
    }
    
    private void savePetToDatabase(AdoptablePetModel pet) {
        animalDataService.saveAnimal(pet, new AnimalDataService.SaveCallback() {
            @Override
            public void onSuccess() {
                hideProgressDialog();
                Toast.makeText(AddPetActivity.this, "✅ Pet added successfully!", Toast.LENGTH_LONG).show();
                
                // Return to previous activity
                Intent resultIntent = new Intent();
                resultIntent.putExtra("pet_added", true);
                resultIntent.putExtra("pet_name", pet.getName());
                setResult(RESULT_OK, resultIntent);
                finish();
            }
            
            @Override
            public void onError(Exception exception) {
                hideProgressDialog();
                Log.e(TAG, "Failed to save pet", exception);
                Toast.makeText(AddPetActivity.this, "❌ Failed to save pet: " + exception.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
    
    private void showProgressDialog(String message) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCancelable(false);
        }
        progressDialog.setMessage(message);
        progressDialog.show();
    }
    
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideProgressDialog();
    }
}