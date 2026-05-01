package com.example.petbuddy;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class MoreFragment extends Fragment {

    private SharedPreferences prefs;
    private DatabaseReference databaseReference;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_more, container, false);


        prefs = getActivity().getSharedPreferences("PetCarePrefs", Context.MODE_PRIVATE);
        databaseReference = FirebaseDatabase.getInstance()
                .getReference("users")
                        .child("user1");



        view.findViewById(R.id.menu_profile).setOnClickListener(v -> showProfile());
        view.findViewById(R.id.menu_settings).setOnClickListener(v -> showSettings());
        view.findViewById(R.id.menu_events).setOnClickListener(v -> showEvents());
        view.findViewById(R.id.menu_community).setOnClickListener(v -> showCommunity());
        view.findViewById(R.id.menu_help).setOnClickListener(v -> showHelp());
        // Reviews & Feedback removed due to build issues
        view.findViewById(R.id.menu_notification).setOnClickListener(v -> openNotifications());


        return view;
    }

    private void showProfile() {

        databaseReference.get().addOnSuccessListener(snapshot -> {

            String name = snapshot.child("name").getValue(String.class);
            String email = snapshot.child("email").getValue(String.class);
            String phone = snapshot.child("phone").getValue(String.class);

            if (name == null) name = "Guest User";
            if (email == null) email = "Not set";
            if (phone == null) phone = "Not set";

            String profile = "👤 OWNER PROFILE\n\n" +
                    "Name: " + name + "\n" +
                    "Email: " + email + "\n" +
                    "Phone: " + phone;

            new AlertDialog.Builder(getContext())
                    .setTitle("Owner Profile")
                    .setMessage(profile)
                    .setPositiveButton("Edit Profile", (dialog, which) -> editProfile())
                    .setNegativeButton("Close", null)
                    .show();

        });
    }

    private void editProfile() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Your Name");
        nameInput.setText(prefs.getString("user_name", ""));
        layout.addView(nameInput);

        EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email");
        emailInput.setText(prefs.getString("user_email", ""));
        layout.addView(emailInput);

        EditText phoneInput = new EditText(getContext());
        phoneInput.setHint("Phone Number");
        phoneInput.setText(prefs.getString("user_phone", ""));
        layout.addView(phoneInput);

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    prefs.edit()
                            .putString("user_name", nameInput.getText().toString())
                            .putString("user_email", emailInput.getText().toString())
                            .putString("user_phone", phoneInput.getText().toString())
                            .apply();
                    Toast.makeText(getContext(), "Profile updated!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettings() {
        new AlertDialog.Builder(getContext())
                .setTitle("⚙️ Account Settings")
                .setItems(new String[]{
                        "🔔 Notifications",
                        "🔒 Privacy & Security",
                        "🌐 Language",
                        "🎨 Theme",
                        "📱 App Preferences",
                        "🚪 Logout"
                }, (dialog, which) -> {
                    String[] options = {"Notifications", "Privacy & Security", "Language", "Theme", "App Preferences", "Logout"};
                    handleSettings(options[which]);
                })
                .show();
    }

    private void handleSettings(String option) {
        switch (option) {
            case "Notifications":
                new AlertDialog.Builder(getContext())
                        .setTitle("Notification Settings")
                        .setMultiChoiceItems(
                                new String[]{"Push Notifications", "Email Alerts", "SMS Alerts", "Event Reminders"},
                                new boolean[]{true, true, false, true},
                                null)
                        .setPositiveButton("Save", (dialog, which) ->
                                Toast.makeText(getContext(), "Settings saved", Toast.LENGTH_SHORT).show())
                        .show();
                break;
            case "Logout":
                new AlertDialog.Builder(getContext())
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (dialog, which) -> performLogout())
                        .setNegativeButton("No", null)
                        .show();
                break;
            default:
                Toast.makeText(getContext(), option + " settings", Toast.LENGTH_SHORT).show();
        }
    }

    private void showEvents() {
        new AlertDialog.Builder(getContext())
                .setTitle("📅 Upcoming Events")
                .setMessage("🎉 Pet Adoption Fair\n" +
                        "Date: Dec 15, 2024\n" +
                        "Time: 10:00 AM - 4:00 PM\n" +
                        "Location: City Park\n" +
                        "Registered: 150 people\n\n" +
                        "🏃 Dog Walking Marathon\n" +
                        "Date: Dec 20, 2024\n" +
                        "Time: 8:00 AM\n" +
                        "Location: Beach Trail\n" +
                        "Registered: 85 people\n\n" +
                        "🎓 Pet Training Workshop\n" +
                        "Date: Dec 25, 2024\n" +
                        "Time: 2:00 PM\n" +
                        "Location: Training Center\n" +
                        "Registered: 45 people")
                .setPositiveButton("Register", (dialog, which) -> registerEvent())
                .setNeutralButton("My Events", (dialog, which) ->
                        Toast.makeText(getContext(), "You're registered for 2 events", Toast.LENGTH_SHORT).show())
                .setNegativeButton("Close", null)
                .show();
    }

    private void registerEvent() {
        new AlertDialog.Builder(getContext())
                .setTitle("Register for Event")
                .setItems(new String[]{
                        "🎉 Pet Adoption Fair",
                        "🏃 Dog Walking Marathon",
                        "🎓 Pet Training Workshop"
                }, (dialog, which) -> {
                    String[] events = {"Pet Adoption Fair", "Dog Walking Marathon", "Pet Training Workshop"};
                    Toast.makeText(getContext(), "Registered for " + events[which] + "!", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void showCommunity() {
        new AlertDialog.Builder(getContext())
                .setTitle("Community Options")
                .setItems(new String[]{
                        "📝 Register/Edit My Profile",
                        "👥 View All Members",
                        "➕ Add New Member Manually",
                        "🤖 Add Test Members (Demo)"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            showCommunityRegistrationForm();
                            break;
                        case 1:
                            Intent intent = new Intent(getContext(), CommunityActivity.class);
                            startActivity(intent);
                            break;
                        case 2:
                            showManualMemberRegistration();
                            break;
                        case 3:
                            addTestMembers();
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showHelp() {
        new AlertDialog.Builder(getContext())
                .setTitle("❓ Help Center")
                .setItems(new String[]{
                        "📚 FAQs",
                        "📞 Contact Support",
                        "🐛 Report an Issue",
                        "📖 App Tutorial",
                        "📧 Email Us",
                        "💬 Live Chat"
                }, (dialog, which) -> {
                    String[] options = {"FAQs", "Contact Support", "Report Issue", "Tutorial", "Email", "Live Chat"};
                    handleHelp(options[which]);
                })
                .show();
    }

    private void handleHelp(String option) {
        switch (option) {
            case "Contact Support":
                new AlertDialog.Builder(getContext())
                        .setTitle("Contact Support")
                        .setMessage("📞 Phone: 1-800-PET-CARE\n" +
                                "📧 Email: support@petcare.com\n" +
                                "⏰ Hours: 24/7\n\n" +
                                "Average response time: 2 hours")
                        .setPositiveButton("Call Now", (dialog, which) ->
                                Toast.makeText(getContext(), "Calling support...", Toast.LENGTH_SHORT).show())
                        .setNegativeButton("Close", null)
                        .show();
                break;
            case "Report Issue":
                Toast.makeText(getContext(), "Opening issue report form", Toast.LENGTH_SHORT).show();
                break;
            default:
                Toast.makeText(getContext(), "Opening " + option, Toast.LENGTH_SHORT).show();
        }
    }

    private void openNotifications() {
        // Enhanced connection message showing Notifications integration
        Toast.makeText(getContext(), "🔗 Opening Notifications - Enhanced Notification System", Toast.LENGTH_LONG).show();

        // Create NotificationFragment with enhanced connection indicator
        NotificationFragment notificationFragment = new NotificationFragment();
        Bundle args = new Bundle();
        args.putBoolean("from_more", true);
        args.putBoolean("enhanced_system", true);
        notificationFragment.setArguments(args);

        requireActivity()
                .getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, notificationFragment)
                .addToBackStack("notification_from_more")
                .commit();
    }
    
    /**
     * Public method to demonstrate connection with Home Notifications
     */
    public static void showConnectionDemo(android.content.Context context) {
        Toast.makeText(context, 
            "🔗 CONNECTION CONFIRMED!\n\n" +
            "Home Notifications ↔️ More → Notifications\n\n" +
            "✅ Same data source (PetPrefs)\n" +
            "✅ Same enhanced notification system\n" +
            "✅ Real-time synchronization\n" +
            "✅ Background popup notifications", 
            Toast.LENGTH_LONG).show();
    }

    // Reviews & Feedback methods removed due to build issues

    private void performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Clear SharedPreferences
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(getContext());
        prefsHelper.logout();
        
        // Show success message
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to LoginActivity
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private void showCommunityRegistrationForm() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Login Required")
                    .setMessage("You must be logged in to join the community")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Get current user data
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(getContext());
        String currentName = prefs.getUserName();
        String currentEmail = auth.getCurrentUser().getEmail();
        String currentPhone = prefs.getString("user_phone", "");

        // Create form layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Full Name *");
        nameInput.setText(currentName != null ? currentName : "");
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email Address *");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(currentEmail != null ? currentEmail : "");
        layout.addView(emailInput);

        // Phone input
        EditText phoneInput = new EditText(getContext());
        phoneInput.setHint("Phone Number *");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(currentPhone);
        layout.addView(phoneInput);

        // Info text
        TextView infoText = new TextView(getContext());
        infoText.setText("\n* Required fields\n\nYour information will be visible to other community members.");
        infoText.setTextSize(12);
        infoText.setTextColor(0xFF666666);
        layout.addView(infoText);

        // Show form dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Community Registration")
                .setView(layout)
                .setPositiveButton("Save & Join", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    // Validate inputs
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (email.isEmpty()) {
                        Toast.makeText(getContext(), "Email is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(getContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (phone.isEmpty()) {
                        Toast.makeText(getContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Save to community
                    saveToCommunity(name, email, phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveToCommunity(String name, String email, String phone) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        String uid = auth.getCurrentUser().getUid();

        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(getContext())
                .setTitle("Saving...")
                .setMessage("Registering you in the community...\n\nName: " + name + "\nEmail: " + email + "\nPhone: " + phone)
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Get Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        // Create community member object
        CommunityModel communityMember = new CommunityModel(
                name,
                email,
                phone,
                System.currentTimeMillis()
        );

        // Save to Firebase
        communityRef.child(uid).setValue(communityMember)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    
                    // Save to SharedPreferences
                    SharedPreferencesHelper prefs = new SharedPreferencesHelper(getContext());
                    prefs.setUserName(name);
                    prefs.setUserEmail(email);
                    prefs.edit().putString("user_phone", phone).apply();

                    new AlertDialog.Builder(getContext())
                            .setTitle("✅ Success!")
                            .setMessage("You have successfully joined the community!\n\n" +
                                    "Your profile has been saved.\n\n" +
                                    "Click 'View Members' to see all community members.")
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(getContext())
                            .setTitle("❌ Failed")
                            .setMessage("Failed to join community!\n\n" +
                                    "Error: " + e.getMessage() + "\n\n" +
                                    "Please check:\n" +
                                    "1. Internet connection\n" +
                                    "2. Firebase Rules\n" +
                                    "3. Try again later")
                            .setPositiveButton("OK", null)
                            .show();
                    e.printStackTrace();
                });
    }

    private void showManualMemberRegistration() {
        // Create form layout
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        EditText nameInput = new EditText(getContext());
        nameInput.setHint("Member Name *");
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(getContext());
        emailInput.setHint("Email Address *");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        // Phone input
        EditText phoneInput = new EditText(getContext());
        phoneInput.setHint("Phone Number *");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(phoneInput);

        // Show form dialog
        new AlertDialog.Builder(getContext())
                .setTitle("Add Community Member")
                .setView(layout)
                .setPositiveButton("Add Member", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
                        Toast.makeText(getContext(), "All fields are required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    addManualMember(name, email, phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addManualMember(String name, String email, String phone) {
        // Get Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        // Generate unique ID
        String memberId = "manual_" + System.currentTimeMillis();

        // Create community member object
        CommunityModel communityMember = new CommunityModel(
                name,
                email,
                phone,
                System.currentTimeMillis()
        );

        // Save to Firebase
        communityRef.child(memberId).setValue(communityMember)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "✅ Member added successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "❌ Failed to add member: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void addTestMembers() {
        // Get Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        // Test members data
        String[][] testMembers = {
                {"John Smith", "john.smith@email.com", "+1-555-0101"},
                {"Sarah Johnson", "sarah.j@email.com", "+1-555-0102"},
                {"Mike Brown", "mike.brown@email.com", "+1-555-0103"},
                {"Lisa Davis", "lisa.davis@email.com", "+1-555-0104"},
                {"Tom Wilson", "tom.wilson@email.com", "+1-555-0105"}
        };

        // Add each test member
        for (int i = 0; i < testMembers.length; i++) {
            String memberId = "test_" + System.currentTimeMillis() + "_" + i;
            CommunityModel member = new CommunityModel(
                    testMembers[i][0],
                    testMembers[i][1],
                    testMembers[i][2],
                    System.currentTimeMillis()
            );

            communityRef.child(memberId).setValue(member);
        }

        Toast.makeText(getContext(), "✅ Added " + testMembers.length + " test members!", Toast.LENGTH_SHORT).show();
    }

}


