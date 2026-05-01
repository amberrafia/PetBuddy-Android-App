package com.example.petbuddy;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class EventDetailsActivity extends AppCompatActivity {

    private TextView txtTitle, txtCategory, txtDescription, txtDateTime, txtLocation, 
                     txtOrganizer, txtParticipants, txtRequirements, txtContact;
    private Button btnRegister, btnShare, btnContact, btnViewRegistrations;
    
    private EventModel event;
    private String eventId, currentUserId;
    private DatabaseReference eventsRef, registrationsRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Event Details");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        initializeFirebase();
        
        eventId = getIntent().getStringExtra("eventId");
        if (eventId != null) {
            loadEventDetails();
        } else {
            Toast.makeText(this, "Event not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initializeViews() {
        txtTitle = findViewById(R.id.txtEventTitle);
        txtCategory = findViewById(R.id.txtEventCategory);
        txtDescription = findViewById(R.id.txtEventDescription);
        txtDateTime = findViewById(R.id.txtEventDateTime);
        txtLocation = findViewById(R.id.txtEventLocation);
        txtOrganizer = findViewById(R.id.txtEventOrganizer);
        txtParticipants = findViewById(R.id.txtEventParticipants);
        txtRequirements = findViewById(R.id.txtEventRequirements);
        txtContact = findViewById(R.id.txtEventContact);
        
        btnRegister = findViewById(R.id.btnRegisterEvent);
        btnShare = findViewById(R.id.btnShareEvent);
        btnContact = findViewById(R.id.btnContactOrganizer);
        btnViewRegistrations = findViewById(R.id.btnViewRegistrations);

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        eventsRef = database.getReference("events");
        registrationsRef = database.getReference("eventRegistrations");
    }

    private void loadEventDetails() {
        showProgressDialog("Loading event details...");
        
        eventsRef.child(eventId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                event = snapshot.getValue(EventModel.class);
                if (event != null) {
                    event.setEventId(eventId);
                    displayEventDetails();
                    setupButtons();
                } else {
                    Toast.makeText(EventDetailsActivity.this, "Event not found", Toast.LENGTH_SHORT).show();
                    finish();
                }
                hideProgressDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                Toast.makeText(EventDetailsActivity.this, "Failed to load event", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void displayEventDetails() {
        txtTitle.setText(event.getTitle());
        txtCategory.setText(event.getCategoryDisplayName());
        txtDescription.setText(event.getDescription());
        txtDateTime.setText("📅 " + event.getDate() + "\n⏰ " + event.getTime());
        txtLocation.setText("📍 " + event.getLocation());
        txtOrganizer.setText("👥 Organized by: " + event.getOrganizer());
        
        int availableSlots = event.getMaxParticipants() - event.getCurrentParticipants();
        txtParticipants.setText("👥 Participants: " + event.getCurrentParticipants() + "/" + 
                               event.getMaxParticipants() + "\n✅ Available slots: " + availableSlots);

        if (event.getRequirements() != null && !event.getRequirements().isEmpty()) {
            txtRequirements.setText("📋 Requirements:\n" + event.getRequirements());
        } else {
            txtRequirements.setText("📋 No special requirements");
        }

        if (event.getContactInfo() != null && !event.getContactInfo().isEmpty()) {
            txtContact.setText("📞 Contact: " + event.getContactInfo());
        } else {
            txtContact.setText("📞 Contact information not available");
        }

        // Set category color
        int categoryColor;
        switch (event.getCategory()) {
            case "pet_activity":
                categoryColor = 0xFF4CAF50;
                break;
            case "awareness_program":
                categoryColor = 0xFF2196F3;
                break;
            case "community_event":
                categoryColor = 0xFFFF9800;
                break;
            default:
                categoryColor = 0xFF9E9E9E;
                break;
        }
        txtCategory.setTextColor(categoryColor);
    }

    private void setupButtons() {
        // Register button
        if (event.hasAvailableSlots() && event.isRegistrationOpen()) {
            btnRegister.setEnabled(true);
            btnRegister.setText("Register for Event");
            btnRegister.setBackgroundColor(0xFF4CAF50);
        } else if (!event.hasAvailableSlots()) {
            btnRegister.setEnabled(false);
            btnRegister.setText("Event Full");
            btnRegister.setBackgroundColor(0xFF9E9E9E);
        } else {
            btnRegister.setEnabled(false);
            btnRegister.setText("Registration Closed");
            btnRegister.setBackgroundColor(0xFF9E9E9E);
        }

        btnRegister.setOnClickListener(v -> registerForEvent());
        btnShare.setOnClickListener(v -> shareEvent());
        btnContact.setOnClickListener(v -> contactOrganizer());
        btnViewRegistrations.setOnClickListener(v -> viewRegisteredUsers());
    }

    private void registerForEvent() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to register", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Confirm Registration")
                .setMessage("Are you sure you want to register for this event?")
                .setPositiveButton("Yes, Register", (dialog, which) -> confirmRegistration())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRegistration() {
        showRegistrationForm();
    }

    private void showRegistrationForm() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_event_registration, null);
        
        EditText editName = dialogView.findViewById(R.id.editUserName);
        EditText editEmail = dialogView.findViewById(R.id.editUserEmail);
        EditText editPhone = dialogView.findViewById(R.id.editUserPhone);
        EditText editPetName = dialogView.findViewById(R.id.editPetName);
        EditText editPetType = dialogView.findViewById(R.id.editPetType);
        EditText editSpecialRequirements = dialogView.findViewById(R.id.editSpecialRequirements);

        // Pre-fill user information if available
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            if (auth.getCurrentUser().getDisplayName() != null) {
                editName.setText(auth.getCurrentUser().getDisplayName());
            }
            if (auth.getCurrentUser().getEmail() != null) {
                editEmail.setText(auth.getCurrentUser().getEmail());
            }
        }

        new AlertDialog.Builder(this)
                .setTitle("Event Registration")
                .setView(dialogView)
                .setPositiveButton("Register", (dialog, which) -> {
                    String name = editName.getText().toString().trim();
                    String email = editEmail.getText().toString().trim();
                    String phone = editPhone.getText().toString().trim();
                    String petName = editPetName.getText().toString().trim();
                    String petType = editPetType.getText().toString().trim();
                    String specialReq = editSpecialRequirements.getText().toString().trim();

                    if (name.isEmpty()) {
                        Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    processRegistration(name, email, phone, petName, petType, specialReq);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processRegistration(String name, String email, String phone, 
                                   String petName, String petType, String specialRequirements) {
        showProgressDialog("Registering...");

        // Create registration object
        EventRegistrationModel registration = new EventRegistrationModel(
                eventId, currentUserId, name, email, phone, petName, petType, specialRequirements);

        // Save detailed registration
        registrationsRef.child(eventId).child(currentUserId).setValue(registration)
                .addOnSuccessListener(aVoid -> {
                    // Update participant count
                    eventsRef.child(eventId).child("currentParticipants")
                            .setValue(event.getCurrentParticipants() + 1)
                            .addOnSuccessListener(aVoid1 -> {
                                hideProgressDialog();
                                Toast.makeText(this, "Successfully registered!", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(this, "Registration failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void shareEvent() {
        String shareText = "Check out this event: " + event.getTitle() + "\n" +
                          "📅 " + event.getDate() + " at " + event.getTime() + "\n" +
                          "📍 " + event.getLocation() + "\n\n" +
                          event.getDescription() + "\n\n" +
                          "Join me at this amazing pet event!";

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share Event"));
    }

    private void contactOrganizer() {
        if (event.getContactInfo() == null || event.getContactInfo().isEmpty()) {
            Toast.makeText(this, "Contact information not available", Toast.LENGTH_SHORT).show();
            return;
        }

        String contact = event.getContactInfo();
        
        if (contact.contains("@")) {
            // Email
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:" + contact));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about: " + event.getTitle());
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } else {
            // Phone or general contact
            new AlertDialog.Builder(this)
                    .setTitle("Contact Organizer")
                    .setMessage("Contact: " + contact)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    private void viewRegisteredUsers() {
        Intent intent = new Intent(this, RegisteredUsersActivity.class);
        intent.putExtra("eventId", eventId);
        intent.putExtra("eventTitle", event.getTitle());
        startActivity(intent);
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}