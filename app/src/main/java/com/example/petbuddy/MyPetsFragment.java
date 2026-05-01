package com.example.petbuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyPetsFragment extends Fragment {

    private LinearLayout petsContainer;
    private DatabaseReference petsRef;
    private String userId;
    private ArrayList<PetModel> petsList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_pets, container, false);

        petsContainer = view.findViewById(R.id.pets_container);

        // Initialize Firebase
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            userId = auth.getCurrentUser().getUid();
            FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
            petsRef = database.getReference("pets").child(userId);
            loadPetsFromFirebase();
        }

        view.findViewById(R.id.btn_add_pet).setOnClickListener(v -> showAddPetDialog());

        view.findViewById(R.id.pet_1).setOnClickListener(v -> showPetMenu("Max", "Golden Retriever"));
        view.findViewById(R.id.pet_2).setOnClickListener(v -> showPetMenu("Luna", "Persian Cat"));

        return view;
    }

    private void loadPetsFromFirebase() {
        petsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                petsList.clear();
                petsContainer.removeAllViews();
                
                for (DataSnapshot petSnapshot : snapshot.getChildren()) {
                    PetModel pet = petSnapshot.getValue(PetModel.class);
                    if (pet != null) {
                        pet.setPetId(petSnapshot.getKey());
                        petsList.add(pet);
                        
                        String emoji = getEmojiForSpecies(pet.getSpecies());
                        String details = buildPetDetails(pet);
                        addPetCardFromModel(pet, emoji, details);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load pets: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getEmojiForSpecies(String species) {
        if (species == null) return "🐾";
        switch (species.toLowerCase()) {
            case "dog": return "🐕";
            case "cat": return "🐱";
            case "bird": return "🐦";
            case "fish": return "🐠";
            case "rabbit": return "🐰";
            case "hamster": return "🐹";
            case "turtle": return "🐢";
            case "reptile": return "🦎";
            default: return "🐾";
        }
    }

    private String buildPetDetails(PetModel pet) {
        StringBuilder details = new StringBuilder();
        if (pet.getBreed() != null && !pet.getBreed().isEmpty()) {
            details.append(pet.getBreed());
        }
        if (pet.getAge() > 0) {
            if (details.length() > 0) details.append(" • ");
            details.append(pet.getAge()).append(" years");
        }
        if (pet.getGender() != null && !pet.getGender().isEmpty()) {
            if (details.length() > 0) details.append(" • ");
            details.append(pet.getGender());
        }
        return details.length() > 0 ? details.toString() : pet.getSpecies();
    }

    private void showAddPetDialog() {
        // First, select pet type
        new AlertDialog.Builder(getContext())
                .setTitle("Select Pet Type")
                .setItems(new String[]{
                        "🐕 Dog",
                        "🐱 Cat",
                        "🐦 Bird",
                        "🐠 Fish",
                        "🐰 Rabbit",
                        "🐹 Hamster",
                        "🐢 Turtle",
                        "🦎 Reptile"
                }, (dialog, which) -> {
                    String[] emojis = {"🐕", "🐱", "🐦", "🐠", "🐰", "🐹", "🐢", "🦎"};
                    String[] types = {"Dog", "Cat", "Bird", "Fish", "Rabbit", "Hamster", "Turtle", "Reptile"};
                    showPetDetailsDialog(emojis[which], types[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showPetDetailsDialog(String emoji, String petType) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        TextView typeLabel = new TextView(getContext());
        typeLabel.setText("Pet Type: " + emoji + " " + petType);
        typeLabel.setTextSize(16);
        typeLabel.setPadding(0, 0, 0, 20);
        layout.addView(typeLabel);

        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Pet name *");
        layout.addView(nameInput);

        EditText breedInput = new EditText(getContext());
        breedInput.setHint("Breed (e.g., Golden Retriever)");
        layout.addView(breedInput);

        EditText ageInput = new EditText(getContext());
        ageInput.setHint("Age (years) *");
        ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(ageInput);

        EditText genderInput = new EditText(getContext());
        genderInput.setHint("Gender (Male/Female) *");
        layout.addView(genderInput);

        EditText weightInput = new EditText(getContext());
        weightInput.setHint("Weight (kg) *");
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(weightInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Add " + petType + " Details")
                .setView(layout)
                .setPositiveButton("Add Pet", (dialog, which) -> {
                    String petName = nameInput.getText().toString().trim();
                    String breed = breedInput.getText().toString().trim();
                    String ageStr = ageInput.getText().toString().trim();
                    String gender = genderInput.getText().toString().trim();
                    String weightStr = weightInput.getText().toString().trim();

                    if (petName.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter pet name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (ageStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter age", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (gender.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter gender", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (weightStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please enter weight", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int age = Integer.parseInt(ageStr);
                    double weight = Double.parseDouble(weightStr);

                    savePetToFirebase(petName, petType, breed, age, gender, weight);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void savePetToFirebase(String petName, String species, String breed, int age, String gender, double weight) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Adding pet...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String petId = "pet_" + System.currentTimeMillis();
        PetModel pet = new PetModel(petName, species, breed, age, gender, weight, "");
        pet.setPetId(petId);

        petsRef.child(petId).setValue(pet)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ " + petName + " added successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to add pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addPetCard(String name, String details, String emoji) {
        CardView cardView = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 32);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(32f);
        cardView.setCardElevation(12f);

        LinearLayout cardContent = new LinearLayout(getContext());
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(48, 48, 48, 48);

        TextView emojiView = new TextView(getContext());
        emojiView.setText(emoji);
        emojiView.setTextSize(40);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(160, 160);
        emojiParams.setMarginEnd(48);
        emojiView.setLayoutParams(emojiParams);
        emojiView.setGravity(android.view.Gravity.CENTER);
        cardContent.addView(emojiView);

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textLayout.setLayoutParams(textParams);

        TextView nameText = new TextView(getContext());
        nameText.setText(name);
        nameText.setTextSize(18);
        nameText.setTextColor(0xFF2D5F5D);
        textLayout.addView(nameText);

        TextView detailsText = new TextView(getContext());
        detailsText.setText(details);
        detailsText.setTextSize(14);
        detailsText.setTextColor(0xFF666666);
        textLayout.addView(detailsText);

        cardContent.addView(textLayout);

        TextView arrow = new TextView(getContext());
        arrow.setText("→");
        arrow.setTextSize(20);
        arrow.setTextColor(0xFF4A8886);
        arrow.setGravity(android.view.Gravity.CENTER);
        cardContent.addView(arrow);

        cardView.addView(cardContent);
        cardView.setOnClickListener(v -> showPetMenu(name, details));

        petsContainer.addView(cardView);
    }

    private void addPetCardFromModel(PetModel pet, String emoji, String details) {
        CardView cardView = new CardView(getContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 32);
        cardView.setLayoutParams(cardParams);
        cardView.setRadius(32f);
        cardView.setCardElevation(12f);

        LinearLayout cardContent = new LinearLayout(getContext());
        cardContent.setOrientation(LinearLayout.HORIZONTAL);
        cardContent.setPadding(48, 48, 48, 48);

        TextView emojiView = new TextView(getContext());
        emojiView.setText(emoji);
        emojiView.setTextSize(40);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(160, 160);
        emojiParams.setMarginEnd(48);
        emojiView.setLayoutParams(emojiParams);
        emojiView.setGravity(android.view.Gravity.CENTER);
        cardContent.addView(emojiView);

        LinearLayout textLayout = new LinearLayout(getContext());
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
        );
        textLayout.setLayoutParams(textParams);

        TextView nameText = new TextView(getContext());
        nameText.setText(pet.getPetName());
        nameText.setTextSize(18);
        nameText.setTextColor(0xFF2D5F5D);
        textLayout.addView(nameText);

        TextView detailsText = new TextView(getContext());
        detailsText.setText(details);
        detailsText.setTextSize(14);
        detailsText.setTextColor(0xFF666666);
        textLayout.addView(detailsText);

        cardContent.addView(textLayout);

        TextView arrow = new TextView(getContext());
        arrow.setText("→");
        arrow.setTextSize(20);
        arrow.setTextColor(0xFF4A8886);
        arrow.setGravity(android.view.Gravity.CENTER);
        cardContent.addView(arrow);

        cardView.addView(cardContent);
        cardView.setOnClickListener(v -> showPetMenuWithModel(pet));

        petsContainer.addView(cardView);
    }

    private void showPetMenuWithModel(PetModel pet) {
        new AlertDialog.Builder(getContext())
                .setTitle(pet.getPetName() + " - " + pet.getSpecies())
                .setItems(new String[]{
                        "📋 Pet Profile",
                        "✏️ Edit Pet",
                        "🛂 Pet Passport",
                        "📅 Appointments",
                        "🏥 Medical Records",
                        "📸 Photo Gallery",
                        "🎂 Birthday & Reminders",
                        "📊 Health Tracker",
                        "🍖 Feeding Schedule",
                        "🏃 Activity Log",
                        "🗑️ Remove Pet"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showPetProfileWithModel(pet);
                            break;
                        case 1:
                            showEditPetDialog(pet);
                            break;
                        case 2:
                            showPetPassport(pet.getPetName());
                            break;
                        case 3:
                            showAppointments(pet.getPetName());
                            break;
                        case 4:
                            showMedicalRecords(pet.getPetName());
                            break;
                        case 5:
                            showPhotoGallery(pet.getPetName());
                            break;
                        case 6:
                            showBirthdayReminders(pet.getPetName());
                            break;
                        case 7:
                            showHealthTracker(pet.getPetName());
                            break;
                        case 8:
                            showFeedingSchedule(pet.getPetName());
                            break;
                        case 9:
                            showActivityLog(pet.getPetName());
                            break;
                        case 10:
                            confirmRemovePetWithModel(pet);
                            break;
                    }
                })
                .show();
    }

    private void showPetProfileWithModel(PetModel pet) {
        String profile = "Name: " + pet.getPetName() + "\n" +
                "Species: " + pet.getSpecies() + "\n" +
                "Breed: " + (pet.getBreed() != null && !pet.getBreed().isEmpty() ? pet.getBreed() : "N/A") + "\n" +
                "Age: " + pet.getAge() + " years\n" +
                "Gender: " + pet.getGender() + "\n" +
                "Weight: " + pet.getWeight() + " kg\n";

        new AlertDialog.Builder(getContext())
                .setTitle("📋 " + pet.getPetName() + "'s Profile")
                .setMessage(profile)
                .setPositiveButton("Edit Profile", (dialog, which) -> showEditPetDialog(pet))
                .setNegativeButton("Close", null)
                .show();
    }

    private void showEditPetDialog(PetModel pet) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);

        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Pet name *");
        nameInput.setText(pet.getPetName());
        layout.addView(nameInput);

        EditText breedInput = new EditText(getContext());
        breedInput.setHint("Breed");
        breedInput.setText(pet.getBreed());
        layout.addView(breedInput);

        EditText ageInput = new EditText(getContext());
        ageInput.setHint("Age (years) *");
        ageInput.setText(String.valueOf(pet.getAge()));
        ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        layout.addView(ageInput);

        EditText genderInput = new EditText(getContext());
        genderInput.setHint("Gender *");
        genderInput.setText(pet.getGender());
        layout.addView(genderInput);

        EditText weightInput = new EditText(getContext());
        weightInput.setHint("Weight (kg) *");
        weightInput.setText(String.valueOf(pet.getWeight()));
        weightInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        layout.addView(weightInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit " + pet.getPetName())
                .setView(layout)
                .setPositiveButton("Save Changes", (dialog, which) -> {
                    String petName = nameInput.getText().toString().trim();
                    String breed = breedInput.getText().toString().trim();
                    String ageStr = ageInput.getText().toString().trim();
                    String gender = genderInput.getText().toString().trim();
                    String weightStr = weightInput.getText().toString().trim();

                    if (petName.isEmpty() || ageStr.isEmpty() || gender.isEmpty() || weightStr.isEmpty()) {
                        Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    int age = Integer.parseInt(ageStr);
                    double weight = Double.parseDouble(weightStr);

                    updatePetInFirebase(pet.getPetId(), petName, breed, age, gender, weight);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePetInFirebase(String petId, String petName, String breed, int age, String gender, double weight) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Updating pet...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("petName", petName);
        updates.put("breed", breed);
        updates.put("age", age);
        updates.put("gender", gender);
        updates.put("weight", weight);

        petsRef.child(petId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ Pet updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to update pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRemovePetWithModel(PetModel pet) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove " + pet.getPetName() + "?")
                .setMessage("Are you sure you want to remove " + pet.getPetName() + " from your pets?\n\n" +
                        "This will delete all pet data.\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> removePetFromFirebase(pet))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void removePetFromFirebase(PetModel pet) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Removing pet...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        petsRef.child(pet.getPetId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ " + pet.getPetName() + " removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to remove pet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showPetMenu(String name, String breed) {
        new AlertDialog.Builder(getContext())
                .setTitle(name + " - " + breed)
                .setItems(new String[]{
                        "📋 Pet Profile",
                        "🛂 Pet Passport",
                        "📅 Appointments",
                        "🏥 Medical Records",
                        "📸 Photo Gallery",
                        "🎂 Birthday & Reminders",
                        "📊 Health Tracker",
                        "🍖 Feeding Schedule",
                        "🏃 Activity Log",
                        "🗑️ Remove Pet"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showPetProfile(name, breed);
                            break;
                        case 1:
                            showPetPassport(name);
                            break;
                        case 2:
                            showAppointments(name);
                            break;
                        case 3:
                            showMedicalRecords(name);
                            break;
                        case 4:
                            showPhotoGallery(name);
                            break;
                        case 5:
                            showBirthdayReminders(name);
                            break;
                        case 6:
                            showHealthTracker(name);
                            break;
                        case 7:
                            showFeedingSchedule(name);
                            break;
                        case 8:
                            showActivityLog(name);
                            break;
                        case 9:
                            confirmRemovePet(name);
                            break;
                    }
                })
                .show();
    }

    private void showPhotoGallery(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("📸 " + name + "'s Photo Gallery")
                .setMessage("Photo Gallery:\n\n" +
                        "📷 Profile Photo\n" +
                        "📷 First Day Home (Jan 2021)\n" +
                        "📷 Birthday Party (Mar 2024)\n" +
                        "📷 At the Park (Nov 2024)\n" +
                        "📷 With Family (Dec 2024)\n\n" +
                        "Total: 5 photos")
                .setPositiveButton("Add Photo", (dialog, which) ->
                        Toast.makeText(getContext(), "📸 Opening camera...", Toast.LENGTH_SHORT).show())
                .setNeutralButton("View All", (dialog, which) ->
                        Toast.makeText(getContext(), "Opening gallery...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showBirthdayReminders(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("🎂 " + name + "'s Birthday & Reminders")
                .setMessage("Birthday: March 15, 2021\n" +
                        "Age: 3 years old\n" +
                        "Next Birthday: March 15, 2025\n\n" +
                        "Upcoming Reminders:\n\n" +
                        "💉 Vaccination Due - Dec 25, 2024\n" +
                        "💊 Heartworm Medicine - Dec 20, 2024\n" +
                        "🎓 Training Videos Appointment - Dec 22, 2024\n" +
                        "🦷 Dental Checkup - Jan 10, 2025\n" +
                        "🎂 Birthday Party - Mar 15, 2025")
                .setPositiveButton("Add Reminder", (dialog, which) ->
                        Toast.makeText(getContext(), "Adding new reminder...", Toast.LENGTH_SHORT).show())
                .setNeutralButton("Edit Birthday", (dialog, which) ->
                        Toast.makeText(getContext(), "Editing birthday...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showHealthTracker(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("📊 " + name + "'s Health Tracker")
                .setMessage("Current Health Status: ✅ Excellent\n\n" +
                        "Weight Tracking:\n" +
                        "• Current: 30 kg\n" +
                        "• Last Month: 29.5 kg\n" +
                        "• Trend: ↗️ Gaining (Healthy)\n\n" +
                        "Activity Level: 🏃 High\n" +
                        "• Daily walks: 2 times\n" +
                        "• Playtime: 3 hours\n\n" +
                        "Appetite: 😋 Normal\n" +
                        "Energy Level: ⚡ High\n" +
                        "Mood: 😊 Happy\n\n" +
                        "Last Vet Visit: Nov 15, 2024\n" +
                        "Next Checkup: May 15, 2025")
                .setPositiveButton("Log Health Data", (dialog, which) ->
                        Toast.makeText(getContext(), "Logging health data...", Toast.LENGTH_SHORT).show())
                .setNeutralButton("View Charts", (dialog, which) ->
                        Toast.makeText(getContext(), "Opening health charts...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showFeedingSchedule(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("🍖 " + name + "'s Feeding Schedule")
                .setMessage("Daily Feeding Plan:\n\n" +
                        "🌅 Breakfast - 7:00 AM\n" +
                        "• Premium Dog Food - 2 cups\n" +
                        "• Fresh Water\n" +
                        "Status: ✅ Fed today\n\n" +
                        "🌆 Dinner - 6:00 PM\n" +
                        "• Premium Dog Food - 2 cups\n" +
                        "• Fresh Water\n" +
                        "Status: ⏰ Pending\n\n" +
                        "🦴 Treats:\n" +
                        "• Training treats (as needed)\n" +
                        "• Dental chews (1 per day)\n\n" +
                        "💧 Water: Refill 3 times daily\n\n" +
                        "Special Diet Notes:\n" +
                        "• No chicken (allergy)\n" +
                        "• Grain-free formula\n" +
                        "• Sensitive stomach")
                .setPositiveButton("Mark as Fed", (dialog, which) ->
                        Toast.makeText(getContext(), "✅ Marked as fed", Toast.LENGTH_SHORT).show())
                .setNeutralButton("Edit Schedule", (dialog, which) ->
                        Toast.makeText(getContext(), "Editing feeding schedule...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showActivityLog(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("🏃 " + name + "'s Activity Log")
                .setMessage("Today's Activities:\n\n" +
                        "🌅 7:30 AM - Morning Walk\n" +
                        "Duration: 30 minutes\n" +
                        "Distance: 2.5 km\n\n" +
                        "🎾 10:00 AM - Playtime\n" +
                        "Activity: Fetch\n" +
                        "Duration: 45 minutes\n\n" +
                        "🏃 6:00 PM - Evening Walk\n" +
                        "Duration: 45 minutes\n" +
                        "Distance: 3.2 km\n\n" +
                        "📊 Daily Summary:\n" +
                        "• Total Exercise: 2 hours\n" +
                        "• Distance: 5.7 km\n" +
                        "• Calories Burned: ~450 kcal\n" +
                        "• Goal Progress: 95% ✅\n\n" +
                        "Weekly Stats:\n" +
                        "• Active Days: 7/7\n" +
                        "• Total Distance: 38.5 km\n" +
                        "• Average Daily: 5.5 km")
                .setPositiveButton("Log Activity", (dialog, which) ->
                        Toast.makeText(getContext(), "Logging new activity...", Toast.LENGTH_SHORT).show())
                .setNeutralButton("View History", (dialog, which) ->
                        Toast.makeText(getContext(), "Opening activity history...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void confirmRemovePet(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove " + name + "?")
                .setMessage("Are you sure you want to remove " + name + " from your pets?\n\n" +
                        "This will delete:\n" +
                        "• Pet profile\n" +
                        "• Medical records\n" +
                        "• Appointments\n" +
                        "• Photos\n" +
                        "• All activity logs\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) ->
                        Toast.makeText(getContext(), name + " removed from your pets", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void showPetProfile(String name, String breed) {
        String profile = "Name: " + name + "\n" +
                "Breed: " + breed + "\n" +
                "Age: 3 years\n" +
                "Gender: Male\n" +
                "Weight: 30 kg\n" +
                "Color: Golden\n" +
                "Microchip: #123456789\n\n" +
                "Personality:\n" +
                "Friendly, energetic, loves playing fetch";

        new AlertDialog.Builder(getContext())
                .setTitle("📋 " + name + "'s Profile")
                .setMessage(profile)
                .setPositiveButton("Edit Profile", (dialog, which) ->
                        Toast.makeText(getContext(), "Edit profile", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showPetPassport(String name) {
        String passport = "🛂 PET PASSPORT\n\n" +
                "Pet Name: " + name + "\n" +
                "Owner: John Doe\n" +
                "Passport ID: PP-2024-001\n" +
                "Issue Date: Jan 1, 2024\n\n" +
                "Vaccinations:\n" +
                "✓ Rabies - Valid until Dec 2025\n" +
                "✓ Distemper - Valid until Dec 2025\n" +
                "✓ Parvovirus - Valid until Dec 2025\n\n" +
                "Travel History:\n" +
                "• USA (2024)\n" +
                "• Canada (2023)";

        new AlertDialog.Builder(getContext())
                .setTitle("🛂 Pet Passport")
                .setMessage(passport)
                .setPositiveButton("Download PDF", (dialog, which) ->
                        Toast.makeText(getContext(), "Downloading passport...", Toast.LENGTH_SHORT).show())
                .setNeutralButton("Share", (dialog, which) ->
                        Toast.makeText(getContext(), "Sharing passport...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void showAppointments(String name) {
        String appointments = "Upcoming Appointments:\n\n" +
                "📅 Dec 20, 2024 - 10:00 AM\n" +
                "Veterinary Checkup\n" +
                "Dr. Smith - Pet Clinic\n\n" +
                "📅 Dec 25, 2024 - 2:00 PM\n" +
                "Training Videos Session\n" +
                "Happy Paws Salon\n\n" +
                "Past Appointments:\n\n" +
                "✓ Nov 15, 2024 - Vaccination\n" +
                "✓ Oct 10, 2024 - Dental Cleaning";

        new AlertDialog.Builder(getContext())
                .setTitle("📅 " + name + "'s Appointments")
                .setMessage(appointments)
                .setPositiveButton("Book New", (dialog, which) -> bookAppointment(name))
                .setNegativeButton("Close", null)
                .show();
    }

    private void bookAppointment(String name) {
        new AlertDialog.Builder(getContext())
                .setTitle("Book Appointment for " + name)
                .setItems(new String[]{
                        "🏥 Veterinary Checkup",
                        "🎓 Training Videos",
                        "💉 Vaccination",
                        "🦷 Dental Care",
                        "🏃 Training Session"
                }, (dialog, which) -> {
                    String[] services = {"Veterinary Checkup", "Training Videos", "Vaccination", "Dental Care", "Training Session"};
                    Toast.makeText(getContext(), "Booking " + services[which] + " for " + name, Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showMedicalRecords(String name) {
        String records = "🏥 MEDICAL RECORDS\n\n" +
                "Recent Visits:\n\n" +
                "Nov 15, 2024 - Annual Checkup\n" +
                "• Weight: 30 kg\n" +
                "• Temperature: Normal\n" +
                "• Heart Rate: Normal\n" +
                "• Vaccinations updated\n\n" +
                "Oct 10, 2024 - Dental Cleaning\n" +
                "• Teeth cleaned\n" +
                "• No cavities found\n" +
                "• Gums healthy\n\n" +
                "Medications:\n" +
                "• Heartworm Prevention (Monthly)\n" +
                "• Flea & Tick Treatment (Monthly)\n\n" +
                "Allergies:\n" +
                "• None reported\n\n" +
                "Blood Type: DEA 1.1 Positive";

        new AlertDialog.Builder(getContext())
                .setTitle("🏥 " + name + "'s Medical Records")
                .setMessage(records)
                .setPositiveButton("Add Record", (dialog, which) ->
                        Toast.makeText(getContext(), "Adding new medical record", Toast.LENGTH_SHORT).show())
                .setNeutralButton("Download", (dialog, which) ->
                        Toast.makeText(getContext(), "Downloading records...", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }
}
