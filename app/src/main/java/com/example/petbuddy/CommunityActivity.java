package com.example.petbuddy;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

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

public class CommunityActivity extends AppCompatActivity {

    private ListView listViewMembers;
    private ArrayList<String> membersList;
    private ArrayList<String> memberIdsList; // Store member IDs
    private ArrayList<CommunityModel> membersDataList; // Store full member data
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Community Members");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listViewMembers = findViewById(R.id.listViewMembers);
        membersList = new ArrayList<>();
        memberIdsList = new ArrayList<>();
        membersDataList = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, R.layout.list_item_community, membersList);
        listViewMembers.setAdapter(adapter);

        // Add click listener for editing members
        listViewMembers.setOnItemClickListener((parent, view, position, id) -> {
            if (position < membersDataList.size()) {
                showMemberOptions(memberIdsList.get(position), membersDataList.get(position));
            }
        });

        loadCommunityMembers();
    }

    private void loadCommunityMembers() {
        // Use explicit database URL
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");
        
        communityRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                membersList.clear();
                memberIdsList.clear();
                membersDataList.clear();

                if (!dataSnapshot.exists()) {
                    membersList.add("No community members yet\n\nTo join:\n1. Go to Home screen\n2. Click Community card\n3. Click Join button");
                    adapter.notifyDataSetChanged();
                    Toast.makeText(CommunityActivity.this,
                            "No members found in database",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    CommunityModel member = snapshot.getValue(CommunityModel.class);

                    if (member != null) {
                        String memberId = snapshot.getKey();
                        String joinedDate = formatDate(member.getJoinedAt());
                        String phone = member.getPhone() != null ? member.getPhone() : "Not provided";
                        String memberInfo = "👤 " + member.getName() + "\n" +
                                "📧 " + member.getEmail() + "\n" +
                                "📱 " + phone + "\n" +
                                "📅 Joined: " + joinedDate;
                        
                        membersList.add(memberInfo);
                        memberIdsList.add(memberId);
                        membersDataList.add(member);
                    }
                }

                adapter.notifyDataSetChanged();
                Toast.makeText(CommunityActivity.this,
                        "✅ Loaded " + membersList.size() + " members\nClick any member to edit/remove",
                        Toast.LENGTH_LONG).show();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                membersList.clear();
                membersList.add("❌ Error loading members:\n" + databaseError.getMessage() + 
                        "\n\nCheck:\n1. Internet connection\n2. Firebase Rules\n3. Database URL");
                adapter.notifyDataSetChanged();
                
                Toast.makeText(CommunityActivity.this,
                        "❌ Failed: " + databaseError.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showMemberOptions(String memberId, CommunityModel member) {
        new AlertDialog.Builder(this)
                .setTitle("Member Options")
                .setMessage("👤 " + member.getName() + "\n📧 " + member.getEmail())
                .setPositiveButton("✏️ Edit", (dialog, which) -> showEditMemberDialog(memberId, member))
                .setNegativeButton("🗑️ Remove", (dialog, which) -> confirmRemoveMember(memberId, member))
                .setNeutralButton("Cancel", null)
                .show();
    }

    private void showEditMemberDialog(String memberId, CommunityModel member) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        EditText nameInput = new EditText(this);
        nameInput.setHint("Full Name *");
        nameInput.setText(member.getName());
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(this);
        emailInput.setHint("Email Address *");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(member.getEmail());
        layout.addView(emailInput);

        // Phone input
        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone Number *");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(member.getPhone() != null ? member.getPhone() : "");
        layout.addView(phoneInput);

        new AlertDialog.Builder(this)
                .setTitle("Edit Member")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    updateMember(memberId, name, email, phone, member.getJoinedAt());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateMember(String memberId, String name, String email, String phone, long joinedAt) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        CommunityModel updatedMember = new CommunityModel(name, email, phone, joinedAt);

        communityRef.child(memberId).setValue(updatedMember)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ Member updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to update: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void confirmRemoveMember(String memberId, CommunityModel member) {
        new AlertDialog.Builder(this)
                .setTitle("⚠️ Remove Member")
                .setMessage("Are you sure you want to remove this member?\n\n" +
                        "👤 " + member.getName() + "\n" +
                        "📧 " + member.getEmail() + "\n\n" +
                        "This action cannot be undone!")
                .setPositiveButton("Yes, Remove", (dialog, which) -> removeMember(memberId, member.getName()))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeMember(String memberId, String memberName) {
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        communityRef.child(memberId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "✅ " + memberName + " removed successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Failed to remove: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private String formatDate(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
