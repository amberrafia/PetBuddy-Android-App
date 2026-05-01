package com.example.petbuddy;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
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
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class EventsActivity extends AppCompatActivity {

    private ListView listViewEvents;
    private ArrayList<EventModel> eventsList;
    private EventsAdapter adapter;
    private DatabaseReference eventsRef, registrationsRef;
    private String currentUserId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_events);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pet Events");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initializeViews();
        initializeFirebase();
        loadEvents();
        addSampleEvents(); // Add sample data
    }

    private void initializeViews() {
        listViewEvents = findViewById(R.id.listViewEvents);
        eventsList = new ArrayList<>();
        
        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        adapter = new EventsAdapter(this, eventsList, currentUserId, this::registerForEvent);
        listViewEvents.setAdapter(adapter);
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        eventsRef = database.getReference("events");
        registrationsRef = database.getReference("eventRegistrations");
    }

    private void loadEvents() {
        showProgressDialog("Loading events...");
        
        eventsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                eventsList.clear();
                
                for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                    EventModel event = eventSnapshot.getValue(EventModel.class);
                    if (event != null) {
                        event.setEventId(eventSnapshot.getKey());
                        eventsList.add(event);
                    }
                }
                
                adapter.notifyDataSetChanged();
                hideProgressDialog();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                hideProgressDialog();
                Toast.makeText(EventsActivity.this, "Failed to load events: " + error.getMessage(), 
                             Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void registerForEvent(EventModel event) {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to register for events", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!event.hasAvailableSlots()) {
            Toast.makeText(this, "Event is full", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!event.isRegistrationOpen()) {
            Toast.makeText(this, "Registration is closed", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if already registered
        registrationsRef.child(event.getEventId()).child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Toast.makeText(EventsActivity.this, "You are already registered for this event", 
                                         Toast.LENGTH_SHORT).show();
                        } else {
                            showRegistrationDialog(event);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(EventsActivity.this, "Error checking registration", 
                                     Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showRegistrationDialog(EventModel event) {
        new AlertDialog.Builder(this)
                .setTitle("Register for Event")
                .setMessage("Do you want to register for:\n\n" + 
                           event.getTitle() + "\n" +
                           "📅 " + event.getDate() + " at " + event.getTime() + "\n" +
                           "📍 " + event.getLocation() + "\n\n" +
                           "Available slots: " + (event.getMaxParticipants() - event.getCurrentParticipants()))
                .setPositiveButton("Register", (dialog, which) -> confirmRegistration(event))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmRegistration(EventModel event) {
        showRegistrationForm(event);
    }

    private void showRegistrationForm(EventModel event) {
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
                .setTitle("Register for " + event.getTitle())
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

                    processRegistration(event, name, email, phone, petName, petType, specialReq);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void processRegistration(EventModel event, String name, String email, String phone, 
                                   String petName, String petType, String specialRequirements) {
        showProgressDialog("Registering...");

        // Create registration object
        EventRegistrationModel registration = new EventRegistrationModel(
                event.getEventId(), currentUserId, name, email, phone, petName, petType, specialRequirements);

        // Save detailed registration
        registrationsRef.child(event.getEventId()).child(currentUserId).setValue(registration)
                .addOnSuccessListener(aVoid -> {
                    // Update participant count
                    eventsRef.child(event.getEventId()).child("currentParticipants")
                            .setValue(event.getCurrentParticipants() + 1)
                            .addOnSuccessListener(aVoid1 -> {
                                hideProgressDialog();
                                Toast.makeText(EventsActivity.this, 
                                             "Successfully registered for " + event.getTitle(), 
                                             Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                hideProgressDialog();
                                Toast.makeText(EventsActivity.this, 
                                             "Registration successful but failed to update count", 
                                             Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(EventsActivity.this, 
                                 "Failed to register: " + e.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                });
    }

    private void addSampleEvents() {
        // Check if events already exist
        eventsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    createSampleEvents();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void createSampleEvents() {
        // Pet Activities
        EventModel dogWalk = new EventModel(
                null, "Community Dog Walk", 
                "Join fellow dog owners for a fun morning walk in the park. Great exercise for both you and your furry friend!",
                "pet_activity", "2024-04-15", "08:00 AM", "Central Park", 
                "PetBuddy Community", "", 50, 
                "contact@petbuddy.com", "Bring your dog on a leash, water bowl, and waste bags"
        );

        EventModel catShow = new EventModel(
                null, "Cat Grooming Videos Workshop", 
                "Learn professional grooming techniques for your cats. Hands-on session with expert groomers.",
                "pet_activity", "2024-04-20", "02:00 PM", "Pet Care Center", 
                "Professional Trainers Association", "", 25,
                "groomers@petcare.com", "Bring your cat in a carrier, vaccination records required"
        );

        // Awareness Programs
        EventModel vaccination = new EventModel(
                null, "Pet Vaccination Awareness", 
                "Free seminar on the importance of pet vaccinations. Learn about vaccination schedules and preventive care.",
                "awareness_program", "2024-04-18", "10:00 AM", "Community Center Hall", 
                "Local Veterinary Clinic", "", 100,
                "info@vetclinic.com", "Open to all pet owners, no pets required for this session"
        );

        EventModel nutrition = new EventModel(
                null, "Pet Nutrition Workshop", 
                "Understanding your pet's nutritional needs. Learn about proper diet, feeding schedules, and healthy treats.",
                "awareness_program", "2024-04-25", "03:00 PM", "Pet Store Conference Room", 
                "Certified Pet Nutritionist", "", 40,
                "nutrition@petstore.com", "Notebook recommended for taking notes"
        );

        // Community Events
        EventModel adoption = new EventModel(
                null, "Pet Adoption Drive", 
                "Help find loving homes for rescued pets. Meet adorable cats and dogs looking for their forever families.",
                "community_event", "2024-04-22", "09:00 AM", "City Square", 
                "Animal Rescue Society", "", 200,
                "adopt@animalrescue.org", "Valid ID required for adoption applications"
        );

        EventModel fundraiser = new EventModel(
                null, "Charity Run for Strays", 
                "5K charity run to raise funds for stray animal care. Registration includes t-shirt and refreshments.",
                "community_event", "2024-04-28", "06:00 AM", "Riverside Park", 
                "Stray Care Foundation", "", 150,
                "run@straycare.org", "Running shoes, comfortable clothing, registration fee: $20"
        );

        // Add events to Firebase
        eventsRef.push().setValue(dogWalk);
        eventsRef.push().setValue(catShow);
        eventsRef.push().setValue(vaccination);
        eventsRef.push().setValue(nutrition);
        eventsRef.push().setValue(adoption);
        eventsRef.push().setValue(fundraiser);
        
        // Add some sample registrations for testing
        addSampleRegistrations();
    }

    private void addSampleRegistrations() {
        // Add sample registrations after a short delay to ensure events are created
        new android.os.Handler().postDelayed(() -> {
            eventsRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot eventSnapshot : snapshot.getChildren()) {
                            String eventId = eventSnapshot.getKey();
                            if (eventId != null) {
                                // Add sample registrations for the first event
                                addSampleRegistrationsForEvent(eventId);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }, 2000);
    }

    private void addSampleRegistrationsForEvent(String eventId) {
        // Sample registration 1
        EventRegistrationModel reg1 = new EventRegistrationModel(
                eventId, "user1", "John Doe", "john.doe@email.com",
                "+1234567890", "Buddy", "Golden Retriever", "Needs water bowl"
        );

        // Sample registration 2
        EventRegistrationModel reg2 = new EventRegistrationModel(
                eventId, "user2", "Jane Smith", "jane.smith@email.com",
                "+0987654321", "Whiskers", "Persian Cat", "Indoor cat, first time outdoors"
        );

        // Sample registration 3
        EventRegistrationModel reg3 = new EventRegistrationModel(
                eventId, "user3", "Mike Johnson", "mike.j@email.com",
                "", "Max", "Labrador", ""
        );

        // Add to Firebase
        registrationsRef.child(eventId).child("user1").setValue(reg1);
        registrationsRef.child(eventId).child("user2").setValue(reg2);
        registrationsRef.child(eventId).child("user3").setValue(reg3);
        
        // Update participant count
        eventsRef.child(eventId).child("currentParticipants").setValue(3);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "🔄 Refresh");
        menu.add(0, 2, 0, "📋 My Registrations");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case 1:
                loadEvents();
                return true;
            case 2:
                showMyRegistrations();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showMyRegistrations() {
        if (currentUserId == null) {
            Toast.makeText(this, "Please login to view registrations", Toast.LENGTH_SHORT).show();
            return;
        }

        // Implementation for showing user's registered events
        Toast.makeText(this, "My Registrations feature coming soon!", Toast.LENGTH_SHORT).show();
    }
}