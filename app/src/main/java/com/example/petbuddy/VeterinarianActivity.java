package com.example.petbuddy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class VeterinarianActivity extends AppCompatActivity {

    private ListView listView;
    private VeterinarianAdapter adapter;
    private ArrayList<VeterinarianModel> veterinarianData;
    private DatabaseReference vetsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_veterinarian);

        listView = findViewById(R.id.listViewVeterinarians);
        veterinarianData = new ArrayList<>();

        adapter = new VeterinarianAdapter(this, veterinarianData);
        listView.setAdapter(adapter);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        vetsRef = database.getReference("veterinarians");

        loadVeterinarians();

        // Item click listener
        listView.setOnItemClickListener((parent, view, position, id) -> {
            showVeterinarianOptions(position);
        });
    }

    private void loadVeterinarians() {
        vetsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                veterinarianData.clear();

                for (DataSnapshot vetSnapshot : snapshot.getChildren()) {
                    VeterinarianModel vet = vetSnapshot.getValue(VeterinarianModel.class);
                    if (vet != null) {
                        vet.setVetId(vetSnapshot.getKey());
                        veterinarianData.add(vet);
                    }
                }
                adapter.notifyDataSetChanged();

                if (veterinarianData.isEmpty()) {
                    Toast.makeText(VeterinarianActivity.this, "No veterinarians found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(VeterinarianActivity.this, "Failed to load: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Custom Adapter
    private class VeterinarianAdapter extends ArrayAdapter<VeterinarianModel> {
        private Context context;
        private ArrayList<VeterinarianModel> vets;

        public VeterinarianAdapter(Context context, ArrayList<VeterinarianModel> vets) {
            super(context, 0, vets);
            this.context = context;
            this.vets = vets;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item_veterinarian, parent, false);
            }

            VeterinarianModel vet = vets.get(position);

            TextView txtClinicName = convertView.findViewById(R.id.txtClinicName);
            TextView txtDoctorName = convertView.findViewById(R.id.txtDoctorName);
            TextView txtExperience = convertView.findViewById(R.id.txtExperience);
            TextView txtContact = convertView.findViewById(R.id.txtContact);
            TextView txtTiming = convertView.findViewById(R.id.txtTiming);
            TextView txtAddress = convertView.findViewById(R.id.txtAddress);
            TextView txtSpecialization = convertView.findViewById(R.id.txtSpecialization);

            txtClinicName.setText("🏥 " + vet.getClinicName());
            txtDoctorName.setText("👨‍⚕️ Dr. " + vet.getDoctorName());
            txtExperience.setText("⭐ " + vet.getExperience() + " Experience");
            txtContact.setText("📞 " + vet.getContact());
            txtTiming.setText("🕐 " + vet.getTiming());
            txtAddress.setText("📍 " + vet.getAddress());

            if (vet.getSpecialization() != null && !vet.getSpecialization().isEmpty()) {
                txtSpecialization.setVisibility(View.VISIBLE);
                txtSpecialization.setText("💊 " + vet.getSpecialization());
            } else {
                txtSpecialization.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

    private void showVeterinarianOptions(int position) {
        VeterinarianModel vet = veterinarianData.get(position);

        new AlertDialog.Builder(this)
                .setTitle(vet.getClinicName())
                .setItems(new String[]{
                        "✏️ Edit",
                        "🗑️ Remove"
                }, (dialog, which) -> {
                    if (which == 0) {
                        showEditDialog(vet);
                    } else {
                        confirmRemove(vet);
                    }
                })
                .show();
    }

    private void showEditDialog(VeterinarianModel vet) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 20);
        layout.setBackgroundColor(0xFFF6C1CC); // Pink background

        EditText clinicInput = new EditText(this);
        clinicInput.setHint("Clinic Name *");
        clinicInput.setText(vet.getClinicName());
        clinicInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        clinicInput.setTextColor(0xFF333333); // Dark text
        clinicInput.setHintTextColor(0xFF666666); // Gray hint
        clinicInput.setPadding(16, 16, 16, 16);
        layout.addView(clinicInput);

        EditText doctorInput = new EditText(this);
        doctorInput.setHint("Doctor Name *");
        doctorInput.setText(vet.getDoctorName());
        doctorInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        doctorInput.setTextColor(0xFF333333); // Dark text
        doctorInput.setHintTextColor(0xFF666666); // Gray hint
        doctorInput.setPadding(16, 16, 16, 16);
        layout.addView(doctorInput);

        EditText experienceInput = new EditText(this);
        experienceInput.setHint("Experience (e.g., 8 Years) *");
        experienceInput.setText(vet.getExperience());
        experienceInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        experienceInput.setTextColor(0xFF333333); // Dark text
        experienceInput.setHintTextColor(0xFF666666); // Gray hint
        experienceInput.setPadding(16, 16, 16, 16);
        layout.addView(experienceInput);

        EditText timingInput = new EditText(this);
        timingInput.setHint("Timing (e.g., 9:00 AM - 6:00 PM) *");
        timingInput.setText(vet.getTiming());
        timingInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        timingInput.setTextColor(0xFF333333); // Dark text
        timingInput.setHintTextColor(0xFF666666); // Gray hint
        timingInput.setPadding(16, 16, 16, 16);
        layout.addView(timingInput);

        EditText contactInput = new EditText(this);
        contactInput.setHint("Contact Number *");
        contactInput.setText(vet.getContact());
        contactInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        contactInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        contactInput.setTextColor(0xFF333333); // Dark text
        contactInput.setHintTextColor(0xFF666666); // Gray hint
        contactInput.setPadding(16, 16, 16, 16);
        layout.addView(contactInput);

        EditText addressInput = new EditText(this);
        addressInput.setHint("Address *");
        addressInput.setText(vet.getAddress());
        addressInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        addressInput.setTextColor(0xFF333333); // Dark text
        addressInput.setHintTextColor(0xFF666666); // Gray hint
        addressInput.setPadding(16, 16, 16, 16);
        layout.addView(addressInput);

        EditText specializationInput = new EditText(this);
        specializationInput.setHint("Specialization (optional)");
        specializationInput.setText(vet.getSpecialization() != null ? vet.getSpecialization() : "");
        specializationInput.setBackgroundColor(0xFFFDECEF); // Light pink background
        specializationInput.setTextColor(0xFF333333); // Dark text
        specializationInput.setHintTextColor(0xFF666666); // Gray hint
        specializationInput.setPadding(16, 16, 16, 16);
        layout.addView(specializationInput);

        new AlertDialog.Builder(this)
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
                        Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateVeterinarian(vet.getVetId(), clinic, doctor, experience, timing, contact, address, specialization);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateVeterinarian(String vetId, String clinic, String doctor, String experience,
                                   String timing, String contact, String address, String specialization) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Updating...");
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
                    Toast.makeText(this, "✅ Updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void confirmRemove(VeterinarianModel vet) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Veterinarian?")
                .setMessage("Are you sure you want to remove " + vet.getClinicName() + "?\n\nThis action cannot be undone.")
                .setPositiveButton("Remove", (dialog, which) -> removeVeterinarian(vet))
                .setNegativeButton("Cancel", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void removeVeterinarian(VeterinarianModel vet) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Removing...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        vetsRef.child(vet.getVetId()).removeValue()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "✅ Removed successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to remove: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
