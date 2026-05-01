package com.example.petbuddy;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditEventActivity extends AppCompatActivity {

    private EventModel event;
    private DatabaseReference eventsRef;
    
    // Event Information
    private EditText editEventTitle, editEventDescription, editEventLocation;
    private EditText editEventDate, editEventTime, editMaxParticipants;
    private EditText editContactPerson, editContactPhone, editContactEmail;
    private Spinner spinnerEventCategory;
    
    private Button btnSave, btnCancel;
    private Calendar selectedDateTime;
    private SimpleDateFormat dateFormat, timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_event);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("👑 Edit Event");
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
        loadEventData();
        setupSpinners();
        setupButtons();
        setupDateTimePickers();
    }

    private void initializeViews() {
        editEventTitle = findViewById(R.id.editEventTitle);
        editEventDescription = findViewById(R.id.editEventDescription);
        editEventLocation = findViewById(R.id.editEventLocation);
        editEventDate = findViewById(R.id.editEventDate);
        editEventTime = findViewById(R.id.editEventTime);
        editMaxParticipants = findViewById(R.id.editMaxParticipants);
        editContactPerson = findViewById(R.id.editContactPerson);
        editContactPhone = findViewById(R.id.editContactPhone);
        editContactEmail = findViewById(R.id.editContactEmail);
        spinnerEventCategory = findViewById(R.id.spinnerEventCategory);
        
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        
        selectedDateTime = Calendar.getInstance();
        dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        eventsRef = database.getReference("events");
    }

    private void loadEventData() {
        String eventId = getIntent().getStringExtra("eventId");
        if (eventId == null) {
            Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        event = (EventModel) getIntent().getSerializableExtra("event");
        if (event == null) {
            Toast.makeText(this, "Event data not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        populateFields();
    }

    private void setupSpinners() {
        String[] categoryOptions = {"Pet Activities", "Awareness Programs", "Community Events"};
        ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categoryOptions);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEventCategory.setAdapter(categoryAdapter);
    }

    private void populateFields() {
        if (event == null) return;

        editEventTitle.setText(event.getTitle());
        editEventDescription.setText(event.getDescription());
        editEventLocation.setText(event.getLocation());
        editMaxParticipants.setText(String.valueOf(event.getMaxParticipants()));
        editContactPerson.setText(event.getContactPerson());
        editContactPhone.setText(event.getContactPhone());
        editContactEmail.setText(event.getContactEmail());
        
        // Set category spinner
        setSpinnerSelection(spinnerEventCategory, event.getCategory());
        
        // Set date and time
        if (event.getDateTime() > 0) {
            selectedDateTime.setTimeInMillis(event.getDateTime());
            editEventDate.setText(dateFormat.format(selectedDateTime.getTime()));
            editEventTime.setText(timeFormat.format(selectedDateTime.getTime()));
        }
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
        btnSave.setOnClickListener(v -> saveEvent());
        btnCancel.setOnClickListener(v -> finish());
    }

    private void setupDateTimePickers() {
        editEventDate.setOnClickListener(v -> showDatePicker());
        editEventTime.setOnClickListener(v -> showTimePicker());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                selectedDateTime.set(Calendar.YEAR, year);
                selectedDateTime.set(Calendar.MONTH, month);
                selectedDateTime.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                editEventDate.setText(dateFormat.format(selectedDateTime.getTime()));
            },
            selectedDateTime.get(Calendar.YEAR),
            selectedDateTime.get(Calendar.MONTH),
            selectedDateTime.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void showTimePicker() {
        TimePickerDialog timePickerDialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedDateTime.set(Calendar.MINUTE, minute);
                editEventTime.setText(timeFormat.format(selectedDateTime.getTime()));
            },
            selectedDateTime.get(Calendar.HOUR_OF_DAY),
            selectedDateTime.get(Calendar.MINUTE),
            false
        );
        timePickerDialog.show();
    }

    private void saveEvent() {
        if (!validateFields()) {
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        
        updates.put("title", editEventTitle.getText().toString().trim());
        updates.put("description", editEventDescription.getText().toString().trim());
        updates.put("location", editEventLocation.getText().toString().trim());
        updates.put("category", spinnerEventCategory.getSelectedItem().toString());
        updates.put("dateTime", selectedDateTime.getTimeInMillis());
        updates.put("maxParticipants", Integer.parseInt(editMaxParticipants.getText().toString().trim()));
        updates.put("contactPerson", editContactPerson.getText().toString().trim());
        updates.put("contactPhone", editContactPhone.getText().toString().trim());
        updates.put("contactEmail", editContactEmail.getText().toString().trim());

        // Save to Firebase
        eventsRef.child(event.getEventId()).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Event updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to update event: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean validateFields() {
        if (editEventTitle.getText().toString().trim().isEmpty()) {
            editEventTitle.setError("Event title is required");
            return false;
        }
        
        if (editEventDescription.getText().toString().trim().isEmpty()) {
            editEventDescription.setError("Event description is required");
            return false;
        }
        
        if (editEventLocation.getText().toString().trim().isEmpty()) {
            editEventLocation.setError("Event location is required");
            return false;
        }
        
        if (editEventDate.getText().toString().trim().isEmpty()) {
            editEventDate.setError("Event date is required");
            return false;
        }
        
        if (editEventTime.getText().toString().trim().isEmpty()) {
            editEventTime.setError("Event time is required");
            return false;
        }
        
        try {
            int maxParticipants = Integer.parseInt(editMaxParticipants.getText().toString().trim());
            if (maxParticipants <= 0) {
                editMaxParticipants.setError("Maximum participants must be positive");
                return false;
            }
        } catch (NumberFormatException e) {
            editMaxParticipants.setError("Invalid number");
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