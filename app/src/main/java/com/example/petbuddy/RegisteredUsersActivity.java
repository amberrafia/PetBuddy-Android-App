package com.example.petbuddy;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RegisteredUsersActivity extends AppCompatActivity {

    private TextView txtEventTitle, txtTotalRegistrations, txtEmptyState;
    private ListView listViewRegistrations;
    private ArrayList<EventRegistrationModel> registrationsList;
    private RegisteredUsersAdapter adapter;
    private DatabaseReference registrationsRef, eventsRef;
    private String eventId;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_registered_users);

            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Registered Users");
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            initializeViews();
            initializeFirebase();
            
            eventId = getIntent().getStringExtra("eventId");
            String eventTitle = getIntent().getStringExtra("eventTitle");
            
            if (eventId != null && !eventId.isEmpty()) {
                txtEventTitle.setText(eventTitle != null ? eventTitle : "Event Registrations");
                loadRegistrations();
            } else {
                Toast.makeText(this, "Event ID not found", Toast.LENGTH_SHORT).show();
                finish();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Error loading page: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
            finish();
        }
    }

    private void initializeViews() {
        try {
            txtEventTitle = findViewById(R.id.txtEventTitle);
            txtTotalRegistrations = findViewById(R.id.txtTotalRegistrations);
            listViewRegistrations = findViewById(R.id.listViewRegistrations);
            txtEmptyState = findViewById(R.id.txtEmptyState);
            
            registrationsList = new ArrayList<>();
            adapter = new RegisteredUsersAdapter(this, registrationsList);
            listViewRegistrations.setAdapter(adapter);
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void initializeFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        registrationsRef = database.getReference("eventRegistrations");
        eventsRef = database.getReference("events");
    }

    private void loadRegistrations() {
        try {
            showProgressDialog("Loading registrations...");
            
            registrationsRef.child(eventId).addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    try {
                        registrationsList.clear();
                        
                        if (snapshot.exists()) {
                            for (DataSnapshot regSnapshot : snapshot.getChildren()) {
                                try {
                                    EventRegistrationModel registration = regSnapshot.getValue(EventRegistrationModel.class);
                                    if (registration != null) {
                                        registration.setRegistrationId(regSnapshot.getKey());
                                        registrationsList.add(registration);
                                    }
                                } catch (Exception e) {
                                    // Skip this registration if there's an error parsing it
                                    e.printStackTrace();
                                }
                            }
                        }
                        
                        txtTotalRegistrations.setText("Total Registrations: " + registrationsList.size());
                        adapter.notifyDataSetChanged();
                        hideProgressDialog();
                        
                        if (registrationsList.isEmpty()) {
                            listViewRegistrations.setVisibility(View.GONE);
                            txtEmptyState.setVisibility(View.VISIBLE);
                        } else {
                            listViewRegistrations.setVisibility(View.VISIBLE);
                            txtEmptyState.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        hideProgressDialog();
                        Toast.makeText(RegisteredUsersActivity.this, 
                                     "Error processing registrations: " + e.getMessage(), 
                                     Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    hideProgressDialog();
                    Toast.makeText(RegisteredUsersActivity.this, 
                                 "Failed to load registrations: " + error.getMessage(), 
                                 Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            hideProgressDialog();
            Toast.makeText(this, "Error setting up registration listener: " + e.getMessage(), 
                         Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
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