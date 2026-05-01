package com.example.petbuddy;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class PetBuddyMainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("PetCarePrefs", MODE_PRIVATE);

        setupActionBar();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int id = item.getItemId();

            if (id == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (id == R.id.nav_blogs) {
                selectedFragment = new BlogsFragment();
            } else if (id == R.id.nav_pets) {
                selectedFragment = new MyPetsFragment();
            } else if (id == R.id.nav_shop) {
                selectedFragment = new ShopFragment();
            } else if (id == R.id.nav_more) {
                selectedFragment = new MoreFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
            }
            return true;
        });


        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        }
    }

    private void setupActionBar() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setLogo(R.drawable.logo_petbuddy);
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setTitle("  PetBuddy");
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.profile_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            showProfile();
            return true;
        } else if (item.getItemId() == R.id.action_admin) {
            showAdminPanel();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProfile() {
        String name = prefs.getString("user_name", "Guest");
        String email = prefs.getString("user_email", "Not set");
        String phone = prefs.getString("user_phone", "Not set");

        String profileInfo = "Name: " + name + "\nEmail: " + email + "\nPhone: " + phone;

        new AlertDialog.Builder(this)
                .setTitle("👤 My Profile")
                .setMessage(profileInfo)
                .setPositiveButton("Edit", (d, w) -> editProfile())
                .setNegativeButton("Close", null)
                .setNeutralButton("Logout", (d, w) -> logout())
                .show();
    }

    private void logout() {
        prefs.edit().clear().apply();
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void editProfile() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 10);

        EditText name = new EditText(this);
        name.setHint("Name");
        name.setText(prefs.getString("user_name", ""));
        layout.addView(name);

        EditText email = new EditText(this);
        email.setHint("Email");
        email.setText(prefs.getString("user_email", ""));
        layout.addView(email);

        EditText phone = new EditText(this);
        phone.setHint("Phone");
        phone.setText(prefs.getString("user_phone", ""));
        layout.addView(phone);

        new AlertDialog.Builder(this)
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    prefs.edit()
                            .putString("user_name", name.getText().toString())
                            .putString("user_email", email.getText().toString())
                            .putString("user_phone", phone.getText().toString())
                            .apply();
                    Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void showAdminPanel() {
        // Check if user is admin
        if (!AdminHelper.isCurrentUserAdmin(this)) {
            new AlertDialog.Builder(this)
                .setTitle("🔒 Admin Access Required")
                .setMessage("You need admin privileges to access this panel.\n\nAdmin Status Info:\n" + 
                           AdminHelper.getAdminStatusInfo(this))
                .setPositiveButton("Grant Admin (Testing)", (d, w) -> {
                    AdminHelper.setAdminStatus(this, true);
                    Toast.makeText(this, "👑 Admin access granted for testing!", Toast.LENGTH_SHORT).show();
                    showAdminPanel(); // Show panel after granting access
                })
                .setNegativeButton("Cancel", null)
                .show();
            return;
        }
        
        // Show admin options
        String[] adminOptions = {
                "📝 Add New Pet",
                "📊 Populate Real Animal Data", 
                "🔥 Test Firebase Integration",
                "🐾 Manage Adoption Pets"
        };
        
        new AlertDialog.Builder(this)
            .setTitle("👑 Admin Panel")
            .setMessage("Choose an admin action:")
            .setItems(adminOptions, (dialog, which) -> {
                switch (which) {
                    case 0:
                        // Add New Pet
                        Intent addPetIntent = new Intent(this, AddPetActivity.class);
                        startActivity(addPetIntent);
                        break;
                    case 1:
                        // Populate Real Animal Data
                        Intent populateIntent = new Intent(this, AdminDataPopulatorActivity.class);
                        startActivity(populateIntent);
                        break;
                    case 2:
                        // Test Firebase Integration
                        testFirebaseIntegration();
                        break;
                    case 3:
                        // Manage Adoption Pets
                        Intent adoptPetIntent = new Intent(this, AdoptPetActivity.class);
                        startActivity(adoptPetIntent);
                        break;
                }
            })
            .setNeutralButton("❌ Revoke Admin", (d, w) -> {
                AdminHelper.setAdminStatus(this, false);
                Toast.makeText(this, "Admin access revoked", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void testFirebaseIntegration() {
        Toast.makeText(this, "🔥 Starting Firebase integration test...", Toast.LENGTH_SHORT).show();
        
        FirebaseIntegrationTest.runQuickTest(this, new FirebaseIntegrationTest.TestCallback() {
            @Override
            public void onTestCompleted(boolean success, String message) {
                runOnUiThread(() -> {
                    new AlertDialog.Builder(PetBuddyMainActivity.this)
                        .setTitle(success ? "✅ Test Passed" : "❌ Test Failed")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
                });
            }
        });
    }
}
