package com.example.petbuddy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class PetDetailsActivity extends AppCompatActivity {

    private AdoptablePetModel pet;
    private DatabaseReference petsRef;
    
    // UI Components
    private ImageView imgPetPhoto;
    private TextView txtPetName, txtPetSpecies, txtPetBreed, txtPetAge, txtPetGender, txtPetSize, txtPetColor;
    private TextView txtDescription, txtPersonality, txtSpecialTraits;
    private TextView txtHealthStatus, txtMedicalCondition, txtLastCheckup;
    private TextView txtCity, txtShelterInfo;
    private TextView txtAdoptionRequirements, txtContactInfo;
    private TextView txtAdoptionStatus, txtAdoptionFee;
    private Button btnContact, btnAdopt;
    private LinearLayout layoutContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create layout programmatically for comprehensive pet details
        createDetailedLayout();
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pet Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        String petId = getIntent().getStringExtra("petId");
        
        if (petId != null) {
            loadPetDetails(petId);
        } else {
            Toast.makeText(this, "Pet ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void createDetailedLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
            ScrollView.LayoutParams.MATCH_PARENT, 
            ScrollView.LayoutParams.MATCH_PARENT));
        scrollView.setBackgroundColor(getResources().getColor(R.color.background));

        layoutContent = new LinearLayout(this);
        layoutContent.setOrientation(LinearLayout.VERTICAL);
        layoutContent.setPadding(16, 16, 16, 16);

        // Pet Photo Card
        createPetPhotoCard();
        
        // Basic Information Card
        createBasicInfoCard();
        
        // Description Card
        createDescriptionCard();
        
        // Health Information Card
        createHealthInfoCard();
        
        // Location Card
        createLocationCard();
        
        // Adoption Requirements Card
        createAdoptionRequirementsCard();
        
        // Contact Information Card
        createContactInfoCard();
        
        // Action Buttons
        createActionButtons();

        scrollView.addView(layoutContent);
        setContentView(scrollView);
    }

    private void createPetPhotoCard() {
        CardView photoCard = createCard();
        LinearLayout photoLayout = new LinearLayout(this);
        photoLayout.setOrientation(LinearLayout.VERTICAL);
        photoLayout.setPadding(16, 16, 16, 16);

        imgPetPhoto = new ImageView(this);
        imgPetPhoto.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 300));
        imgPetPhoto.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imgPetPhoto.setBackgroundColor(getResources().getColor(R.color.primary_light));

        txtPetName = new TextView(this);
        txtPetName.setTextSize(24);
        txtPetName.setTextColor(getResources().getColor(R.color.primary_text));
        txtPetName.setTypeface(null, android.graphics.Typeface.BOLD);
        txtPetName.setPadding(0, 16, 0, 8);

        txtAdoptionStatus = new TextView(this);
        txtAdoptionStatus.setTextSize(16);
        txtAdoptionStatus.setTextColor(getResources().getColor(R.color.success));
        txtAdoptionStatus.setTypeface(null, android.graphics.Typeface.BOLD);

        photoLayout.addView(imgPetPhoto);
        photoLayout.addView(txtPetName);
        photoLayout.addView(txtAdoptionStatus);
        photoCard.addView(photoLayout);
        layoutContent.addView(photoCard);
    }

    private void createBasicInfoCard() {
        CardView basicCard = createCard();
        LinearLayout basicLayout = new LinearLayout(this);
        basicLayout.setOrientation(LinearLayout.VERTICAL);
        basicLayout.setPadding(16, 16, 16, 16);

        TextView titleBasic = createSectionTitle("🐾 Basic Information");
        basicLayout.addView(titleBasic);

        txtPetSpecies = createInfoText();
        txtPetBreed = createInfoText();
        txtPetAge = createInfoText();
        txtPetGender = createInfoText();
        txtPetSize = createInfoText();
        txtPetColor = createInfoText();

        basicLayout.addView(txtPetSpecies);
        basicLayout.addView(txtPetBreed);
        basicLayout.addView(txtPetAge);
        basicLayout.addView(txtPetGender);
        basicLayout.addView(txtPetSize);
        basicLayout.addView(txtPetColor);

        basicCard.addView(basicLayout);
        layoutContent.addView(basicCard);
    }

    private void createDescriptionCard() {
        CardView descCard = createCard();
        LinearLayout descLayout = new LinearLayout(this);
        descLayout.setOrientation(LinearLayout.VERTICAL);
        descLayout.setPadding(16, 16, 16, 16);

        TextView titleDesc = createSectionTitle("📝 About This Pet");
        descLayout.addView(titleDesc);

        txtDescription = createInfoText();
        txtPersonality = createInfoText();
        txtSpecialTraits = createInfoText();

        descLayout.addView(txtDescription);
        descLayout.addView(txtPersonality);
        descLayout.addView(txtSpecialTraits);

        descCard.addView(descLayout);
        layoutContent.addView(descCard);
    }

    private void createHealthInfoCard() {
        CardView healthCard = createCard();
        LinearLayout healthLayout = new LinearLayout(this);
        healthLayout.setOrientation(LinearLayout.VERTICAL);
        healthLayout.setPadding(16, 16, 16, 16);

        TextView titleHealth = createSectionTitle("🏥 Health Information");
        healthLayout.addView(titleHealth);

        txtHealthStatus = createInfoText();
        txtMedicalCondition = createInfoText();
        txtLastCheckup = createInfoText();

        healthLayout.addView(txtHealthStatus);
        healthLayout.addView(txtMedicalCondition);
        healthLayout.addView(txtLastCheckup);

        healthCard.addView(healthLayout);
        layoutContent.addView(healthCard);
    }

    private void createLocationCard() {
        CardView locationCard = createCard();
        LinearLayout locationLayout = new LinearLayout(this);
        locationLayout.setOrientation(LinearLayout.VERTICAL);
        locationLayout.setPadding(16, 16, 16, 16);

        TextView titleLocation = createSectionTitle("📍 Location");
        locationLayout.addView(titleLocation);

        txtCity = createInfoText();
        txtShelterInfo = createInfoText();

        locationLayout.addView(txtCity);
        locationLayout.addView(txtShelterInfo);

        locationCard.addView(locationLayout);
        layoutContent.addView(locationCard);
    }

    private void createAdoptionRequirementsCard() {
        CardView reqCard = createCard();
        LinearLayout reqLayout = new LinearLayout(this);
        reqLayout.setOrientation(LinearLayout.VERTICAL);
        reqLayout.setPadding(16, 16, 16, 16);

        TextView titleReq = createSectionTitle("📋 Adoption Requirements");
        reqLayout.addView(titleReq);

        txtAdoptionRequirements = createInfoText();
        txtAdoptionFee = createInfoText();

        reqLayout.addView(txtAdoptionRequirements);
        reqLayout.addView(txtAdoptionFee);

        reqCard.addView(reqLayout);
        layoutContent.addView(reqCard);
    }

    private void createContactInfoCard() {
        CardView contactCard = createCard();
        LinearLayout contactLayout = new LinearLayout(this);
        contactLayout.setOrientation(LinearLayout.VERTICAL);
        contactLayout.setPadding(16, 16, 16, 16);

        TextView titleContact = createSectionTitle("📞 Contact Information");
        contactLayout.addView(titleContact);

        txtContactInfo = createInfoText();
        contactLayout.addView(txtContactInfo);

        contactCard.addView(contactLayout);
        layoutContent.addView(contactCard);
    }

    private void createActionButtons() {
        LinearLayout buttonLayout = new LinearLayout(this);
        buttonLayout.setOrientation(LinearLayout.HORIZONTAL);
        buttonLayout.setPadding(16, 16, 16, 16);

        btnContact = new Button(this);
        btnContact.setText("📞 CONTACT");
        btnContact.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btnContact.setBackgroundTintList(getResources().getColorStateList(R.color.info));
        btnContact.setTextColor(getResources().getColor(R.color.white));
        btnContact.setOnClickListener(v -> contactShelter());

        btnAdopt = new Button(this);
        btnAdopt.setText("❤️ ADOPT ME");
        btnAdopt.setLayoutParams(new LinearLayout.LayoutParams(0, 
            LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        btnAdopt.setBackgroundTintList(getResources().getColorStateList(R.color.success));
        btnAdopt.setTextColor(getResources().getColor(R.color.white));
        btnAdopt.setOnClickListener(v -> startAdoptionProcess());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);
        btnContact.setLayoutParams(params);
        btnAdopt.setLayoutParams(params);

        buttonLayout.addView(btnContact);
        buttonLayout.addView(btnAdopt);
        layoutContent.addView(buttonLayout);
    }

    private CardView createCard() {
        CardView card = new CardView(this);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);
        card.setCardElevation(4);
        card.setRadius(12);
        card.setCardBackgroundColor(getResources().getColor(R.color.surface));
        return card;
    }

    private TextView createSectionTitle(String title) {
        TextView titleView = new TextView(this);
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTextColor(getResources().getColor(R.color.primary_text));
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setPadding(0, 0, 0, 12);
        return titleView;
    }

    private TextView createInfoText() {
        TextView textView = new TextView(this);
        textView.setTextSize(14);
        textView.setTextColor(getResources().getColor(R.color.secondary_text));
        textView.setPadding(0, 4, 0, 4);
        return textView;
    }

    private void loadPetDetails(String petId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        petsRef = database.getReference("adoptablePets").child(petId);

        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                pet = snapshot.getValue(AdoptablePetModel.class);
                if (pet != null) {
                    pet.setPetId(petId);
                    displayPetDetails();
                } else {
                    Toast.makeText(PetDetailsActivity.this, "Pet not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(PetDetailsActivity.this, "Failed to load pet details", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayPetDetails() {
        // Pet Photo and Name
        loadPetImage(imgPetPhoto, pet);
        txtPetName.setText(pet.getSpeciesEmoji() + " " + pet.getName());
        txtAdoptionStatus.setText(pet.getAdoptionStatusEmoji() + " " + 
            pet.getAdoptionStatus().substring(0, 1).toUpperCase() + 
            pet.getAdoptionStatus().substring(1));

        // Basic Information
        txtPetSpecies.setText("🐾 Species: " + pet.getSpecies().substring(0, 1).toUpperCase() + pet.getSpecies().substring(1));
        txtPetBreed.setText("🧬 Breed: " + pet.getBreed());
        txtPetAge.setText("🎂 Age: " + pet.getAgeString());
        txtPetGender.setText(pet.getGenderEmoji() + " Gender: " + pet.getGender().substring(0, 1).toUpperCase() + pet.getGender().substring(1));
        txtPetSize.setText("📏 Size: " + pet.getSize().substring(0, 1).toUpperCase() + pet.getSize().substring(1));
        txtPetColor.setText("🎨 Color: " + pet.getColor());

        // Description
        txtDescription.setText("📖 " + pet.getDescription());
        txtPersonality.setText("😊 Personality: " + (pet.getPersonality() != null ? pet.getPersonality() : pet.getTemperament()));
        txtSpecialTraits.setText("⭐ Special Traits: " + (pet.getSpecialTraits() != null ? pet.getSpecialTraits() : "Friendly and well-behaved"));

        // Health Information
        txtHealthStatus.setText(pet.getHealthStatusSummary());
        txtMedicalCondition.setText("🩺 Medical Condition: " + (pet.getMedicalCondition() != null ? pet.getMedicalCondition() : "None"));
        txtLastCheckup.setText("📅 Last Checkup: " + (pet.getLastHealthCheckDate() != null ? pet.getLastHealthCheckDate() : "Recent"));

        // Location
        txtCity.setText("🏙️ City: " + (pet.getCity() != null ? pet.getCity() : "Local Area"));
        txtShelterInfo.setText("🏠 " + pet.getShelterName() + "\n📍 " + pet.getShelterLocation());

        // Adoption Requirements
        txtAdoptionRequirements.setText(pet.getAdoptionRequirementsSummary());
        if (pet.getAdoptionFee() > 0) {
            txtAdoptionFee.setText("💰 Adoption Fee: $" + String.format("%.0f", pet.getAdoptionFee()));
        } else {
            txtAdoptionFee.setText("💰 Free Adoption");
        }

        // Contact Information
        txtContactInfo.setText(pet.getContactInfo());

        // Update button states based on adoption status
        updateButtonStates();
    }

    private void updateButtonStates() {
        if (pet != null && !pet.isAvailable()) {
            btnAdopt.setEnabled(false);
            btnAdopt.setText("❌ NOT AVAILABLE");
            btnAdopt.setBackgroundTintList(getResources().getColorStateList(R.color.hint_text));
        }
    }

    private void setDefaultPetImage(ImageView imageView, String species) {
        switch (species.toLowerCase()) {
            case "dog":
                imageView.setImageResource(R.drawable.default_dog_image);
                break;
            case "cat":
                imageView.setImageResource(R.drawable.default_cat_image);
                break;
            case "rabbit":
                imageView.setImageResource(R.drawable.default_rabbit_image);
                break;
            case "bird":
                imageView.setImageResource(R.drawable.default_bird_image);
                break;
            default:
                imageView.setImageResource(R.drawable.default_pet_image);
                break;
        }
    }

    private void loadPetImage(ImageView imageView, AdoptablePetModel pet) {
        String imageUrl = pet.getImageUrl();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // If we have a Base64 encoded image
            if (imageUrl.startsWith("data:image")) {
                try {
                    String base64String = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imageView.setImageBitmap(decodedByte);
                    return;
                } catch (Exception e) {
                    // Fall back to default image
                }
            }
        }
        
        // Use default image if no custom image or error loading
        setDefaultPetImage(imageView, pet.getSpecies());
    }

    private void contactShelter() {
        if (pet != null && pet.getContactPhone() != null) {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + pet.getContactPhone()));
            startActivity(intent);
        } else {
            Toast.makeText(this, "Contact information not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void startAdoptionProcess() {
        if (pet != null && pet.isAvailable()) {
            String message = "🐾 Adoption Interest\n\n" +
                    "I'm interested in adopting " + pet.getName() + " (" + pet.getSpecies() + ").\n" +
                    "Pet ID: " + pet.getPetId() + "\n\n" +
                    "Please contact me to discuss the adoption process.";

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + pet.getContactEmail()));
            intent.putExtra(Intent.EXTRA_SUBJECT, "Adoption Interest - " + pet.getName());
            intent.putExtra(Intent.EXTRA_TEXT, message);

            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "Email app not found. Please contact: " + pet.getContactEmail(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "This pet is not available for adoption", Toast.LENGTH_SHORT).show();
        }
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