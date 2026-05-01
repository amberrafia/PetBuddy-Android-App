package com.example.petbuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class AdoptPetActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AdoptionPagerAdapter pagerAdapter;
    private DatabaseReference petsRef;
    private ProgressDialog progressDialog;
    private com.google.android.material.floatingactionbutton.FloatingActionButton fabAddPet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adopt_pet);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Adopt a Pet");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        initializeFirebase();
        addSamplePets(); // Add sample data
    }

    private void initializeViews() {
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);
        fabAddPet = findViewById(R.id.fabAddPet);

        // Setup ViewPager2 with adapter
        pagerAdapter = new AdoptionPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // Connect TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Dogs");
                    break;
                case 1:
                    tab.setText("Cats");
                    break;
            }
        }).attach();
        
        // Initialize search functionality
        initializeSearch();
        
        // Initialize admin functionality
        initializeAdminFeatures();
    }
    
    private void initializeSearch() {
        // Find the search card view and make it clickable
        androidx.cardview.widget.CardView searchCard = findViewById(R.id.searchCard);
        if (searchCard != null) {
            searchCard.setOnClickListener(v -> showSearchDialog());
        }
    }
    
    private void showSearchDialog() {
        EditText searchInput = new EditText(this);
        searchInput.setHint("Search by name, breed, or personality...");
        searchInput.setPadding(40, 30, 40, 30);
        
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("🔍 Search Pets")
            .setView(searchInput)
            .setPositiveButton("Search", (d, w) -> {
                String searchText = searchInput.getText().toString().trim();
                if (!searchText.isEmpty()) {
                    performSearch(searchText);
                }
            })
            .setNegativeButton("Cancel", null)
            .create();
            
        dialog.show();
        
        // Focus on input and show keyboard
        searchInput.requestFocus();
    }
    
    private void initializeAdminFeatures() {
        // Show/hide admin FAB based on admin status
        boolean isAdmin = AdminHelper.isCurrentUserAdmin(this);
        fabAddPet.setVisibility(isAdmin ? android.view.View.VISIBLE : android.view.View.GONE);
        
        // Set up FAB click listener
        fabAddPet.setOnClickListener(v -> openAddPetActivity());
    }
    
    private void performSearch(String searchText) {
        Toast.makeText(this, "Searching for: " + searchText, Toast.LENGTH_SHORT).show();
        
        // Get the current tab and perform search accordingly
        int currentTab = viewPager.getCurrentItem();
        
        if (currentTab == 0) {
            // Dogs tab - search for dogs
            searchForDogs(searchText);
        } else if (currentTab == 1) {
            // Cats tab - search for cats
            searchForCats(searchText);
        }
    }
    
    private void searchForDogs(String searchText) {
        AnimalDataService animalService = AnimalDataService.getInstance();
        
        // Create a filter for dogs with search text
        AnimalDataService.AnimalFilter filter = new AnimalDataService.AnimalFilter();
        filter.setSpecies("dog");
        filter.setSearchText(searchText);
        filter.setAdoptionStatus("available");
        
        animalService.getFilteredAnimals(filter, "search_dogs", new AnimalDataService.AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                runOnUiThread(() -> {
                    if (animals.isEmpty()) {
                        Toast.makeText(AdoptPetActivity.this, "No dogs found matching '" + searchText + "'", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdoptPetActivity.this, "Found " + animals.size() + " dogs matching '" + searchText + "'", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(AdoptPetActivity.this, "Search failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void searchForCats(String searchText) {
        AnimalDataService animalService = AnimalDataService.getInstance();
        
        // Create a filter for cats with search text
        AnimalDataService.AnimalFilter filter = new AnimalDataService.AnimalFilter();
        filter.setSpecies("cat");
        filter.setSearchText(searchText);
        filter.setAdoptionStatus("available");
        
        animalService.getFilteredAnimals(filter, "search_cats", new AnimalDataService.AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                runOnUiThread(() -> {
                    if (animals.isEmpty()) {
                        Toast.makeText(AdoptPetActivity.this, "No cats found matching '" + searchText + "'", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AdoptPetActivity.this, "Found " + animals.size() + " cats matching '" + searchText + "'", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onError(Exception exception) {
                runOnUiThread(() -> {
                    Toast.makeText(AdoptPetActivity.this, "Search failed: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        petsRef = database.getReference("adoptablePets");
    }

    private void addSamplePets() {
        // Check if pets already exist
        petsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    createSamplePets();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void createSamplePets() {
        // Sample Dogs with comprehensive information
        AdoptablePetModel dog1 = new AdoptablePetModel(
                "Bella", "dog", "Golden Retriever", 18, "female", "large", "Golden",
                "Meet Bella! This gorgeous Golden Retriever is the perfect family companion. She loves children, enjoys long walks, and has the sweetest temperament. Bella is house-trained, knows basic commands, and gets along wonderfully with other pets. She's looking for a loving family to share her endless affection with.",
                "Gentle, Loyal, Playful", "Loves children, knows basic commands, house-trained",
                "Excellent", true, true, "None", "March 2026",
                "", "Happy Valley", "Sunshine Animal Rescue", "456 Rainbow Ave, Happy Valley", "contact@sunshinerescue.org",
                21, "House with yard preferred", "Beginner friendly", "Good with children and other pets",
                "Sarah Johnson", "(555) 123-4567", "sarah@sunshinerescue.org",
                "None", 180.0
        );

        AdoptablePetModel dog2 = new AdoptablePetModel(
                "Max", "dog", "German Shepherd Mix", 36, "male", "large", "Black & Tan",
                "Max is a handsome and intelligent German Shepherd mix who would make an excellent guard dog and loyal companion. He's well-trained, protective yet gentle, and loves outdoor activities. Max is perfect for an active family who enjoys hiking and adventures.",
                "Intelligent, Protective, Active", "Well-trained, loves outdoor activities, protective instincts",
                "Excellent", true, true, "None", "February 2026",
                "", "Downtown", "City Animal Haven", "789 Oak Street, Downtown", "info@cityhaven.org",
                25, "Active household with yard", "Intermediate", "Daily exercise required, experienced with large dogs preferred",
                "Mike Rodriguez", "(555) 234-5678", "mike@cityhaven.org",
                "Needs daily exercise", 200.0
        );

        AdoptablePetModel dog3 = new AdoptablePetModel(
                "Luna", "dog", "Husky", 24, "female", "large", "White & Grey",
                "Luna is a stunning Siberian Husky with piercing blue eyes and a playful spirit. She's energetic, loves snow, and would be perfect for an active owner who enjoys outdoor adventures. Luna is friendly with other dogs and loves to play.",
                "Energetic, Playful, Social", "Loves snow, friendly with other dogs, high energy",
                "Excellent", true, true, "None", "January 2026",
                "", "Mountain View", "Mountain Pet Rescue", "321 Pine Ridge, Mountain View", "help@mountainpets.org",
                23, "Active household, preferably with yard", "Experienced", "High energy breed, needs lots of exercise and mental stimulation",
                "Emma Thompson", "(555) 345-6789", "emma@mountainpets.org",
                "High energy, needs lots of exercise", 220.0
        );

        // Sample Cats with comprehensive information
        AdoptablePetModel cat1 = new AdoptablePetModel(
                "Whiskers", "cat", "Persian", 30, "male", "medium", "Pure White",
                "Whiskers is a majestic Persian cat with the fluffiest coat you've ever seen! He's calm, dignified, and loves to be pampered. Perfect for someone who wants a regal companion who enjoys quiet moments and gentle pets. His beautiful blue eyes will melt your heart.",
                "Calm, Affectionate, Dignified", "Loves being pampered, enjoys quiet moments, beautiful blue eyes",
                "Excellent", true, true, "None", "February 2026",
                "", "Purr City", "Feline Paradise Sanctuary", "567 Cat Lane, Purr City", "meow@felineparadise.org",
                18, "Quiet indoor environment", "Beginner friendly", "Indoor only, daily brushing required",
                "Lisa Chen", "(555) 456-7890", "lisa@felineparadise.org",
                "Daily brushing required", 150.0
        );

        AdoptablePetModel cat2 = new AdoptablePetModel(
                "Mittens", "cat", "Maine Coon", 12, "female", "large", "Tabby & White",
                "Mittens is a gorgeous Maine Coon kitten with the most adorable white paws that look like mittens! She's playful, curious, and loves to explore. Maine Coons are known for their gentle giant personality and Mittens is no exception - she'll grow into a beautiful, loving companion.",
                "Playful, Curious, Gentle", "Adorable white paws, loves to explore, gentle giant personality",
                "Excellent", true, false, "None", "March 2026",
                "", "Cat Town", "Whiskers & Tails Rescue", "890 Meow Street, Cat Town", "adopt@whiskerstails.org",
                18, "Space to grow and play", "Beginner friendly", "Will grow large, needs space to play",
                "David Park", "(555) 567-8901", "david@whiskerstails.org",
                "Will grow large, needs space", 120.0
        );

        // Add to Firebase with better organization
        petsRef.push().setValue(dog1);
        petsRef.push().setValue(dog2);
        petsRef.push().setValue(dog3);
        petsRef.push().setValue(cat1);
        petsRef.push().setValue(cat2);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            boolean petAdded = data.getBooleanExtra("pet_added", false);
            String petName = data.getStringExtra("pet_name");
            
            if (petAdded) {
                Toast.makeText(this, "🎉 " + petName + " has been added successfully!", Toast.LENGTH_LONG).show();
                // The fragments will automatically refresh via Firebase listeners
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "🔄 Refresh");
        menu.add(0, 2, 0, "📋 Adoption Guide");
        
        // Add admin options if user is admin
        if (AdminHelper.isCurrentUserAdmin(this)) {
            menu.add(0, 3, 0, "👑 Add New Pet");
            menu.add(0, 4, 0, "⚙️ Admin Panel");
        }
        
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 1:
                // Refresh will be handled by fragments automatically via Firebase listeners
                Toast.makeText(this, "Refreshing pets...", Toast.LENGTH_SHORT).show();
                return true;
            case 2:
                showAdoptionGuide();
                return true;
            case 3:
                if (AdminHelper.isCurrentUserAdmin(this)) {
                    showAddNewPetDialog();
                }
                return true;
            case 4:
                if (AdminHelper.isCurrentUserAdmin(this)) {
                    showAdminPanel();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showAdoptionGuide() {
        String guideText = "🏠 Pet Adoption Guide:\n\n" +
                "✅ Before Adopting:\n" +
                "• Research the pet's needs\n" +
                "• Prepare your home\n" +
                "• Budget for expenses\n" +
                "• Consider your lifestyle\n\n" +
                "📋 Adoption Process:\n" +
                "• Fill out application\n" +
                "• Meet the pet\n" +
                "• Home visit (if required)\n" +
                "• Adoption fee payment\n\n" +
                "💡 Tips:\n" +
                "• Be patient during transition\n" +
                "• Schedule vet checkup\n" +
                "• Establish routine\n" +
                "• Provide love and care";

        new android.app.AlertDialog.Builder(this)
                .setTitle("Adoption Guide")
                .setMessage(guideText)
                .setPositiveButton("Got it!", null)
                .show();
    }

    // ================= ADMIN METHODS =================
    
    private void showAddNewPetDialog() {
        String[] options = {
                "📝 Add New Pet (Form)",
                "🔄 Populate Sample Data",
                "📊 Admin Data Manager"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Admin: Add Pets")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openAddPetActivity();
                            break;
                        case 1:
                            showPopulateSampleDataDialog();
                            break;
                        case 2:
                            openAdminDataPopulatorActivity();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void openAddPetActivity() {
        Intent intent = new Intent(this, AddPetActivity.class);
        startActivityForResult(intent, 100);
    }
    
    private void showPopulateSampleDataDialog() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("🔄 Populate Sample Data")
                .setMessage("This will add sample dogs and cats to the database for testing. Continue?")
                .setPositiveButton("Yes, Add Sample Data", (dialog, which) -> {
                    createSamplePets();
                    Toast.makeText(this, "✅ Sample pets added successfully!", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void openAdminDataPopulatorActivity() {
        Intent intent = new Intent(this, AdminDataPopulatorActivity.class);
        startActivity(intent);
    }

    private void showAdminPanel() {
        String adminStatusInfo = AdminHelper.getAdminStatusInfo(this);
        boolean isAdmin = AdminHelper.isCurrentUserAdmin(this);
        
        String adminInfo = "👑 Admin Panel\n\n" +
                "Debug Information:\n" + adminStatusInfo + "\n\n" +
                "Available Actions:\n" +
                "• Add new pets\n" +
                "• Edit pet information\n" +
                "• Manage adoption status\n" +
                "• View adoption statistics\n\n" +
                "Admin Emails:\n" +
                "• admin@petbuddy.com\n" +
                "• manager@petbuddy.com\n" +
                "• petbuddy.admin@gmail.com";

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("👑 Admin Panel");
        builder.setMessage(adminInfo);
        
        if (!isAdmin) {
            builder.setNeutralButton("🔓 Grant Admin (Test)", (dialog, which) -> {
                AdminHelper.setAdminStatus(this, true);
                Toast.makeText(this, "👑 Admin access granted for testing!", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu(); // Refresh menu
                refreshFragmentAdminStatus();
            });
        } else {
            builder.setNeutralButton("🔒 Remove Admin (Test)", (dialog, which) -> {
                AdminHelper.setAdminStatus(this, false);
                Toast.makeText(this, "🔒 Admin access removed for testing!", Toast.LENGTH_SHORT).show();
                invalidateOptionsMenu(); // Refresh menu
                refreshFragmentAdminStatus();
            });
        }
        
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    private void refreshFragmentAdminStatus() {
        // Refresh admin status in both fragments
        if (pagerAdapter != null) {
            // Get current fragments from ViewPager2
            getSupportFragmentManager().getFragments().forEach(fragment -> {
                if (fragment instanceof DogsFragment) {
                    ((DogsFragment) fragment).refreshAdminStatus();
                } else if (fragment instanceof CatsFragment) {
                    ((CatsFragment) fragment).refreshAdminStatus();
                }
            });
        }
        
        // Refresh FAB visibility
        initializeAdminFeatures();
    }

    public void showEditPetDialog(AdoptablePetModel pet) {
        String[] options = {
                "✏️ Edit Full Profile",
                "🏥 Update Health Status", 
                "💰 Change Adoption Fee",
                "📝 Edit Description",
                "❌ Mark as Adopted",
                "🗑️ Remove Pet"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Edit: " + pet.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openEditPetProfileActivity(pet);
                            break;
                        case 1:
                            showEditHealthStatusDialog(pet);
                            break;
                        case 2:
                            showEditAdoptionFeeDialog(pet);
                            break;
                        case 3:
                            showEditDescriptionDialog(pet);
                            break;
                        case 4:
                            markPetAsAdopted(pet);
                            break;
                        case 5:
                            removePet(pet);
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openEditPetProfileActivity(AdoptablePetModel pet) {
        Intent intent = new Intent(this, EditPetProfileActivity.class);
        intent.putExtra("petId", pet.getPetId());
        intent.putExtra("pet", pet);
        startActivity(intent);
    }

    private void showEditHealthStatusDialog(AdoptablePetModel pet) {
        String[] healthOptions = {"Excellent", "Good", "Fair", "Needs Medical Attention"};
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Update Health Status: " + pet.getName())
                .setSingleChoiceItems(healthOptions, 0, null)
                .setPositiveButton("Update", (dialog, which) -> {
                    int selectedIndex = ((android.app.AlertDialog) dialog).getListView().getCheckedItemPosition();
                    if (selectedIndex >= 0) {
                        String newHealthStatus = healthOptions[selectedIndex];
                        updatePetHealthStatus(pet, newHealthStatus);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditAdoptionFeeDialog(AdoptablePetModel pet) {
        android.widget.EditText feeInput = new android.widget.EditText(this);
        feeInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        feeInput.setText(String.valueOf(pet.getAdoptionFee()));
        feeInput.setHint("Enter adoption fee");

        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Change Adoption Fee: " + pet.getName())
                .setView(feeInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    try {
                        double newFee = Double.parseDouble(feeInput.getText().toString());
                        updatePetAdoptionFee(pet, newFee);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Invalid fee amount", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDescriptionDialog(AdoptablePetModel pet) {
        android.widget.EditText descInput = new android.widget.EditText(this);
        descInput.setText(pet.getDescription());
        descInput.setHint("Enter pet description");
        descInput.setMinLines(3);

        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Edit Description: " + pet.getName())
                .setView(descInput)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newDescription = descInput.getText().toString().trim();
                    if (!newDescription.isEmpty()) {
                        updatePetDescription(pet, newDescription);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updatePetHealthStatus(AdoptablePetModel pet, String newHealthStatus) {
        if (pet.getPetId() != null) {
            petsRef.child(pet.getPetId()).child("healthStatus").setValue(newHealthStatus)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Health status updated for " + pet.getName(), Toast.LENGTH_SHORT).show();
                        // Fragments will automatically refresh via Firebase listeners
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Failed to update health status", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updatePetAdoptionFee(AdoptablePetModel pet, double newFee) {
        if (pet.getPetId() != null) {
            petsRef.child(pet.getPetId()).child("adoptionFee").setValue(newFee)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Adoption fee updated for " + pet.getName(), Toast.LENGTH_SHORT).show();
                        // Fragments will automatically refresh via Firebase listeners
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Failed to update adoption fee", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void updatePetDescription(AdoptablePetModel pet, String newDescription) {
        if (pet.getPetId() != null) {
            petsRef.child(pet.getPetId()).child("description").setValue(newDescription)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "✅ Description updated for " + pet.getName(), Toast.LENGTH_SHORT).show();
                        // Fragments will automatically refresh via Firebase listeners
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "❌ Failed to update description", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void markPetAsAdopted(AdoptablePetModel pet) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Mark as Adopted")
                .setMessage("Are you sure you want to mark " + pet.getName() + " as adopted?")
                .setPositiveButton("Yes, Adopted", (dialog, which) -> {
                    if (pet.getPetId() != null) {
                        petsRef.child(pet.getPetId()).child("available").setValue(false)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "🎉 " + pet.getName() + " marked as adopted!", Toast.LENGTH_SHORT).show();
                                    // Fragments will automatically refresh via Firebase listeners
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "❌ Failed to update adoption status", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removePet(AdoptablePetModel pet) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("👑 Remove Pet")
                .setMessage("⚠️ WARNING: This will permanently delete " + pet.getName() + " from the database.\n\nThis action cannot be undone!")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (pet.getPetId() != null) {
                        petsRef.child(pet.getPetId()).removeValue()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(this, "🗑️ " + pet.getName() + " removed from database", Toast.LENGTH_SHORT).show();
                                    // Fragments will automatically refresh via Firebase listeners
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "❌ Failed to remove pet", Toast.LENGTH_SHORT).show();
                                });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}