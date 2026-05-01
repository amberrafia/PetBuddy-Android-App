package com.example.petbuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VeterinarianFragment extends Fragment {

    private DatabaseReference vetsRef;
    private LinearLayout vetsContainer;
    private ArrayList<VeterinarianModel> vetsList = new ArrayList<>();
    private TextView txtClinicName, txtDoctorName, txtExperience, txtTiming, txtContact, txtAddress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_veterinarian, container, false);

        // Initialize UI elements
        txtClinicName = view.findViewById(R.id.txtClinicName);
        txtDoctorName = view.findViewById(R.id.txtDoctorName);
        txtExperience = view.findViewById(R.id.txtExperience);
        txtTiming = view.findViewById(R.id.txtTiming);
        txtContact = view.findViewById(R.id.txtContact);
        txtAddress = view.findViewById(R.id.txtAddress);

        // Add button click listener
        view.findViewById(R.id.btnAddVet).setOnClickListener(v -> showAddVeterinarianDialog());
        
        // View All button click listener
        view.findViewById(R.id.btnViewAll).setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), VeterinarianActivity.class);
            startActivity(intent);
        });

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        vetsRef = database.getReference("veterinarians");

        // Load data from Firebase
        loadVeterinariansFromFirebase();

        // Add click listeners for management
        view.setOnLongClickListener(v -> {
            showManagementMenu();
            return true;
        });

        return view;
    }

    private void loadVeterinariansFromFirebase() {
        vetsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                vetsList.clear();
                
                for (DataSnapshot vetSnapshot : snapshot.getChildren()) {
                    VeterinarianModel vet = vetSnapshot.getValue(VeterinarianModel.class);
                    if (vet != null) {
                        vet.setVetId(vetSnapshot.getKey());
                        vetsList.add(vet);
                    }
                }

                // Display the first veterinarian or show default message
                if (!vetsList.isEmpty()) {
                    displayVeterinarian(vetsList.get(0));
                } else {
                    displayDefaultMessage();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayVeterinarian(VeterinarianModel vet) {
        txtClinicName.setText("Clinic: " + vet.getClinicName());
        txtDoctorName.setText("Doctor: " + vet.getDoctorName());
        txtExperience.setText("Experience: " + vet.getExperience());
        txtTiming.setText("Timing: " + vet.getTiming());
        txtContact.setText("Contact: " + vet.getContact());
        txtAddress.setText("Address: " + vet.getAddress());

        // Make it clickable to show options
        txtClinicName.setOnClickListener(v -> showVeterinarianOptions(vet));
        txtDoctorName.setOnClickListener(v -> showVeterinarianOptions(vet));
        txtExperience.setOnClickListener(v -> showVeterinarianOptions(vet));
        txtTiming.setOnClickListener(v -> showVeterinarianOptions(vet));
        txtContact.setOnClickListener(v -> showVeterinarianOptions(vet));
        txtAddress.setOnClickListener(v -> showVeterinarianOptions(vet));
    }

    private void displayDefaultMessage() {
        txtClinicName.setText("Clinic: No data available");
        txtDoctorName.setText("Doctor: -");
        txtExperience.setText("Experience: -");
        txtTiming.setText("Timing: -");
        txtContact.setText("Contact: -");
        txtAddress.setText("Address: -");
        
        txtClinicName.setOnClickListener(v -> showManagementMenu());
    }

    private void showManagementMenu() {
        new AlertDialog.Builder(getContext())
                .setTitle("Veterinarian Management")
                .setItems(new String[]{
                        "➕ Add Veterinarian",
                        "📋 View All Veterinarians",
                        "✏️ Edit Current",
                        "🗑️ Remove Current"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showAddVeterinarianDialog();
                            break;
                        case 1:
                            showAllVeterinarians();
                            break;
                        case 2:
                            if (!vetsList.isEmpty()) {
                                showEditVeterinarianDialog(vetsList.get(0));
                            } else {
                                Toast.makeText(getContext(), "No veterinarian to edit", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case 3:
                            if (!vetsList.isEmpty()) {
                                confirmRemoveVeterinarian(vetsList.get(0));
                            } else {
                                Toast.makeText(getContext(), "No veterinarian to remove", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                })
                .show();
    }

    private void showVeterinarianOptions(VeterinarianModel vet) {
        new AlertDialog.Builder(getContext())
                .setTitle(vet.getClinicName())
                .setItems(new String[]{
                        "✏️ Edit",
                        "🗑️ Remove",
                        "📋 View All"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showEditVeterinarianDialog(vet);
                            break;
                        case 1:
                            confirmRemoveVeterinarian(vet);
                            break;
                        case 2:
                            showAllVeterinarians();
                            break;
                    }
                })
                .show();
    }

    private void showAddVeterinarianDialog() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.setBackgroundColor(0xFFF6C1CC); // Pink background

        EditText clinicInput = new EditText(getContext());
        clinicInput.setHint("Clinic Name *");
        clinicInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        clinicInput.setTextColor(0xFF333333); // Dark text
        clinicInput.setHintTextColor(0xFF666666); // Gray hint
        clinicInput.setPadding(16, 16, 16, 16);
        layout.addView(clinicInput);

        EditText doctorInput = new EditText(getContext());
        doctorInput.setHint("Doctor Name *");
        doctorInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        doctorInput.setTextColor(0xFF333333); // Dark text
        doctorInput.setHintTextColor(0xFF666666); // Gray hint
        doctorInput.setPadding(16, 16, 16, 16);
        layout.addView(doctorInput);

        EditText experienceInput = new EditText(getContext());
        experienceInput.setHint("Experience (e.g., 8 Years) *");
        experienceInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        experienceInput.setTextColor(0xFF333333); // Dark text
        experienceInput.setHintTextColor(0xFF666666); // Gray hint
        experienceInput.setPadding(16, 16, 16, 16);
        layout.addView(experienceInput);

        EditText timingInput = new EditText(getContext());
        timingInput.setHint("Timing (e.g., 9:00 AM - 6:00 PM) *");
        timingInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        timingInput.setTextColor(0xFF333333); // Dark text
        timingInput.setHintTextColor(0xFF666666); // Gray hint
        timingInput.setPadding(16, 16, 16, 16);
        layout.addView(timingInput);

        EditText contactInput = new EditText(getContext());
        contactInput.setHint("Contact Number *");
        contactInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        contactInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        contactInput.setTextColor(0xFF333333); // Dark text
        contactInput.setHintTextColor(0xFF666666); // Gray hint
        contactInput.setPadding(16, 16, 16, 16);
        layout.addView(contactInput);

        EditText addressInput = new EditText(getContext());
        addressInput.setHint("Address *");
        addressInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        addressInput.setTextColor(0xFF333333); // Dark text
        addressInput.setHintTextColor(0xFF666666); // Gray hint
        addressInput.setPadding(16, 16, 16, 16);
        layout.addView(addressInput);

        EditText specializationInput = new EditText(getContext());
        specializationInput.setHint("Specialization (optional)");
        specializationInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        specializationInput.setTextColor(0xFF333333); // Dark text
        specializationInput.setHintTextColor(0xFF666666); // Gray hint
        specializationInput.setPadding(16, 16, 16, 16);
        layout.addView(specializationInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Veterinarian")
                .setView(layout)
                .setPositiveButton("Add", (dialog, which) -> {
                    String clinic = clinicInput.getText().toString().trim();
                    String doctor = doctorInput.getText().toString().trim();
                    String experience = experienceInput.getText().toString().trim();
                    String timing = timingInput.getText().toString().trim();
                    String contact = contactInput.getText().toString().trim();
                    String address = addressInput.getText().toString().trim();
                    String specialization = specializationInput.getText().toString().trim();

                    if (clinic.isEmpty() || doctor.isEmpty() || experience.isEmpty() || 
                        timing.isEmpty() || contact.isEmpty() || address.isEmpty()) {
                        Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    saveVeterinarianToFirebase(clinic, doctor, experience, timing, contact, address, specialization);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveVeterinarianToFirebase(String clinic, String doctor, String experience, 
                                           String timing, String contact, String address, String specialization) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Adding veterinarian...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String vetId = "vet_" + System.currentTimeMillis();
        VeterinarianModel vet = new VeterinarianModel(clinic, doctor, experience, timing, contact, address, specialization);
        vet.setVetId(vetId);

        vetsRef.child(vetId).setValue(vet)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ Veterinarian added successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to add: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showEditVeterinarianDialog(VeterinarianModel vet) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.setBackgroundColor(0xFFF6C1CC); // Pink background

        EditText clinicInput = new EditText(getContext());
        clinicInput.setHint("Clinic Name *");
        clinicInput.setText(vet.getClinicName());
        clinicInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        clinicInput.setTextColor(0xFF333333); // Dark text
        clinicInput.setHintTextColor(0xFF666666); // Gray hint
        clinicInput.setPadding(16, 16, 16, 16);
        layout.addView(clinicInput);

        EditText doctorInput = new EditText(getContext());
        doctorInput.setHint("Doctor Name *");
        doctorInput.setText(vet.getDoctorName());
        doctorInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        doctorInput.setTextColor(0xFF333333); // Dark text
        doctorInput.setHintTextColor(0xFF666666); // Gray hint
        doctorInput.setPadding(16, 16, 16, 16);
        layout.addView(doctorInput);

        EditText experienceInput = new EditText(getContext());
        experienceInput.setHint("Experience *");
        experienceInput.setText(vet.getExperience());
        experienceInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        experienceInput.setTextColor(0xFF333333); // Dark text
        experienceInput.setHintTextColor(0xFF666666); // Gray hint
        experienceInput.setPadding(16, 16, 16, 16);
        layout.addView(experienceInput);

        EditText timingInput = new EditText(getContext());
        timingInput.setHint("Timing *");
        timingInput.setText(vet.getTiming());
        timingInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        timingInput.setTextColor(0xFF333333); // Dark text
        timingInput.setHintTextColor(0xFF666666); // Gray hint
        timingInput.setPadding(16, 16, 16, 16);
        layout.addView(timingInput);

        EditText contactInput = new EditText(getContext());
        contactInput.setHint("Contact *");
        contactInput.setText(vet.getContact());
        contactInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        contactInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        contactInput.setTextColor(0xFF333333); // Dark text
        contactInput.setHintTextColor(0xFF666666); // Gray hint
        contactInput.setPadding(16, 16, 16, 16);
        layout.addView(contactInput);

        EditText addressInput = new EditText(getContext());
        addressInput.setHint("Address *");
        addressInput.setText(vet.getAddress());
        addressInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        addressInput.setTextColor(0xFF333333); // Dark text
        addressInput.setHintTextColor(0xFF666666); // Gray hint
        addressInput.setPadding(16, 16, 16, 16);
        layout.addView(addressInput);

        EditText specializationInput = new EditText(getContext());
        specializationInput.setHint("Specialization");
        specializationInput.setText(vet.getSpecialization() != null ? vet.getSpecialization() : "");
        specializationInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        specializationInput.setTextColor(0xFF333333); // Dark text
        specializationInput.setHintTextColor(0xFF666666); // Gray hint
        specializationInput.setPadding(16, 16, 16, 16);
        layout.addView(specializationInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Veterinarian")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String clinic = clinicInput.getText().toString().trim();
                    String doctor = doctorInput.getText().toString().trim();
                    String experience = experienceInput.getText().toString().trim();
                    String timing = timingInput.getText().toString().trim();
                    String contact = contactInput.getText().toString().trim();
                    String address = addressInput.getText().toString().trim();
                    String specialization = specializationInput.getText().toString().trim();

                    if (clinic.isEmpty() || doctor.isEmpty() || experience.isEmpty() || 
                        timing.isEmpty() || contact.isEmpty() || address.isEmpty()) {
                        Toast.makeText(getContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateVeterinarianInFirebase(vet.getVetId(), clinic, doctor, experience, timing, contact, address, specialization);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateVeterinarianInFirebase(String vetId, String clinic, String doctor, String experience, 
                                             String timing, String contact, String address, String specialization) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Updating veterinarian...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        Map<String, Object> updates = new HashMap<>();
        updates.put("clinicName", clinic);
        updates.put("doctorName", doctor);
        updates.put("experience", experience);
        updates.put("timing", timing);
        updates.put("contact", contact);
        updates.put("address", address);
        updates.put("specialization", specialization);

        vetsRef.child(vetId).updateChildren(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ Updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRemoveVeterinarian(VeterinarianModel vet) {
        new AlertDialog.Builder(getContext())
                .setTitle("Remove Veterinarian?")
                .setMessage("Are you sure you want to remove " + vet.getClinicName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> removeVeterinarianFromFirebase(vet))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void removeVeterinarianFromFirebase(VeterinarianModel vet) {
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Removing veterinarian...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        vetsRef.child(vet.getVetId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "✅ Removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showAllVeterinarians() {
        if (vetsList.isEmpty()) {
            Toast.makeText(getContext(), "No veterinarians available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] vetNames = new String[vetsList.size()];
        for (int i = 0; i < vetsList.size(); i++) {
            vetNames[i] = vetsList.get(i).getClinicName() + " - Dr. " + vetsList.get(i).getDoctorName();
        }

        new AlertDialog.Builder(getContext())
                .setTitle("All Veterinarians (" + vetsList.size() + ")")
                .setItems(vetNames, (dialog, which) -> {
                    displayVeterinarian(vetsList.get(which));
                    Toast.makeText(getContext(), "Now viewing: " + vetsList.get(which).getClinicName(), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
    }
}
