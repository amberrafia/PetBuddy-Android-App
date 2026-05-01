package com.example.petbuddy;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;

import android.app.AlarmManager;
import android.app.PendingIntent;
import java.util.Calendar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;



public class HomeFragment extends Fragment implements EmergencyDataManager.EmergencyDataCallback {
    private TextView txtHomeUserName;
    private TextView txtHomeUserEmail;
    
    // Photo management
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;
    private static final int INJURY_CAMERA_REQUEST = 3;
    private static final int INJURY_GALLERY_REQUEST = 4;
    
    // Camera access request codes for different entry points
    private static final int CAMERA_PERMISSION_DIRECT_ACCESS = 100;
    private static final int CAMERA_PERMISSION_DIALOG_ACCESS = 101;
    private static final int STORAGE_PERMISSION_GALLERY = 102;
    
    // Camera access source tracking
    private enum CameraAccessSource {
        DIRECT_ACCESS,  // From injury tracker card click
        DIALOG_ACCESS   // From dialog "Take Photo" option
    }
    private ImageView currentProfileImageView;
    private ImageView mainProfileImageView; // Add reference to main profile image
    
    // Emergency data management
    private EmergencyDataManager emergencyDataManager;
    private java.util.List<EmergencyDataManager.EmergencyVet> nearbyVets = new java.util.ArrayList<>();
    private java.util.List<EmergencyDataManager.EmergencyContact> emergencyContacts = new java.util.ArrayList<>();
    private java.util.List<EmergencyDataManager.EmergencyAlert> activeAlerts = new java.util.ArrayList<>();

    @Override
    public void onResume() {
        super.onResume();
        // Refresh profile photo in case it was changed
        if (mainProfileImageView != null) {
            loadProfilePhoto(mainProfileImageView);
        }
        
        // Start listening for real-time emergency data
        if (emergencyDataManager != null) {
            emergencyDataManager.startListening();
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        
        // Stop listening for real-time emergency data to save resources
        if (emergencyDataManager != null) {
            emergencyDataManager.stopListening();
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Unregister callback and cleanup
        if (emergencyDataManager != null) {
            emergencyDataManager.unregisterCallback(this);
        }
    }
    
    @Override
    public void onEmergencyDataUpdated(java.util.List<EmergencyDataManager.EmergencyVet> vets, 
                                     java.util.List<EmergencyDataManager.EmergencyContact> contacts, 
                                     java.util.List<EmergencyDataManager.EmergencyAlert> alerts) {
        // Update local data when Firebase data changes
        this.nearbyVets = vets;
        this.emergencyContacts = contacts;
        this.activeAlerts = alerts;
        
        Log.d("HomeFragment", "Emergency data updated - Vets: " + vets.size() + 
                             ", Contacts: " + contacts.size() + ", Alerts: " + alerts.size());
        
        // Update UI if needed (e.g., show notification badge for alerts)
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateEmergencyUI();
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case CAMERA_PERMISSION_DIRECT_ACCESS: // Direct camera access from card
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with direct camera access
                    handleCameraAccess(CameraAccessSource.DIRECT_ACCESS);
                } else {
                    handleCameraPermissionDenied("direct access");
                }
                break;
                
            case CAMERA_PERMISSION_DIALOG_ACCESS: // Camera access from dialog
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with dialog camera access
                    handleCameraAccess(CameraAccessSource.DIALOG_ACCESS);
                } else {
                    handleCameraPermissionDenied("dialog option");
                }
                break;
                
            case STORAGE_PERMISSION_GALLERY: // Gallery access
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, open gallery
                    openInjuryGallery();
                } else {
                    Toast.makeText(requireContext(), "Storage permission is required to access gallery", Toast.LENGTH_LONG).show();
                }
                break;
                
            case 200: // Location permission for showing current location
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    showCurrentLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required to show your current location", Toast.LENGTH_LONG).show();
                }
                break;
                
            case 201: // Location permission for finding nearby vets
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    findNearbyVets();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required to find nearby veterinarians", Toast.LENGTH_LONG).show();
                }
                break;
                
            case 202: // Location permission for sharing location
                if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    shareCurrentLocation();
                } else {
                    Toast.makeText(requireContext(), "Location permission is required to share your location", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().requestPermissions(
                    new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                    101
            );
        }

        // ================= USER INFO =================
        SharedPreferencesHelper prefsHelper =
                new SharedPreferencesHelper(requireContext());
        txtHomeUserName = view.findViewById(R.id.txtHomeUserName);
        txtHomeUserEmail = view.findViewById(R.id.txtHomeUserEmail);

        String name = prefsHelper.getUserName();
        String email = prefsHelper.getUserEmail();

        txtHomeUserName.setText(
                (name == null || name.isEmpty()) ? "User Name" : name
        );

        txtHomeUserEmail.setText(
                (email == null || email.isEmpty()) ? "user@email.com" : email
        );

        // ================= LOAD PROFILE PHOTO IN MAIN VIEW =================
        ImageView imgProfile = view.findViewById(R.id.imgProfile);
        mainProfileImageView = imgProfile; // Store reference for updates
        loadProfilePhoto(imgProfile);



        // ================= EDIT PROFILE BUTTON =================
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> showEditProfileDialog());

        // ================= PROFILE IMAGE CLICK =================
        view.findViewById(R.id.imgProfile).setOnClickListener(v -> showProfileDialog());

        // ================= LOGOUT BUTTON =================
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to logout?")
                    .setPositiveButton("Yes", (dialog, which) -> performLogout())
                    .setNegativeButton("No", null)
                    .show();
        });

        // ================= EXISTING CLICKS =================
        view.findViewById(R.id.card_emergency).setOnClickListener(v -> showEmergency());
        view.findViewById(R.id.card_search).setOnClickListener(v -> showSearch());
        view.findViewById(R.id.card_first_aid).setOnClickListener(v -> {
            Log.d("HomeFragment", "Injury Tracker card clicked!");
            Toast.makeText(requireContext(), "Opening Injury Tracker options...", Toast.LENGTH_SHORT).show();
            
            // Show injury tracker dialog with options
            showInjuryTracker();
        });
        view.findViewById(R.id.card_message).setOnClickListener(v -> showMessages());
        view.findViewById(R.id.card_events).setOnClickListener(v -> showEvents());
        view.findViewById(R.id.card_blogs).setOnClickListener(v -> navigateToBlogs());
        // Reviews & Feedback removed due to build issues

        view.findViewById(R.id.btn_shop).setOnClickListener(v -> navigateToShop());

        view.findViewById(R.id.feature_my_pets).setOnClickListener(v -> navigateToMyPets());
        view.findViewById(R.id.feature_community).setOnClickListener(v -> showCommunity());
        view.findViewById(R.id.feature_shop).setOnClickListener(v -> navigateToShop());

        view.findViewById(R.id.card_cat_food).setOnClickListener(v -> showCatFoodPosters());

        view.findViewById(R.id.card_dog_food).setOnClickListener(v -> showDogFoodPosters());

        view.findViewById(R.id.service_vet)
                .setOnClickListener(v ->
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.fragment_container, new VeterinarianFragment())
                                .addToBackStack(null)
                                .commit()
                );

        view.findViewById(R.id.service_grooming)
                .setOnClickListener(v -> showTrainingVideosDialog());

        // ================= INITIALIZE EMERGENCY DATA MANAGER =================
        initializeEmergencyDataManager();

        return view;
    }

    /* ================= PROFILE DIALOG ================= */
    private void showProfileDialog() {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_profile_custom, null);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(view)
                .create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        // Initialize views
        ImageView imgProfilePhoto = view.findViewById(R.id.imgProfilePhoto);
        androidx.cardview.widget.CardView cardProfilePhoto = view.findViewById(R.id.cardProfilePhoto);
        TextView txtProfileName = view.findViewById(R.id.txtProfileName);
        TextView txtProfileEmail = view.findViewById(R.id.txtProfileEmail);
        TextView txtProfilePhone = view.findViewById(R.id.txtProfilePhone);
        
        currentProfileImageView = imgProfilePhoto;

        // Load user data
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
        FirebaseAuth auth = FirebaseAuth.getInstance();
        
        String userName = prefsHelper.getUserName();
        String userEmail = prefsHelper.getUserEmail();
        String userPhone = prefsHelper.getUserPhone();
        
        // If no local data, try Firebase user
        if ((userName == null || userName.isEmpty()) && auth.getCurrentUser() != null) {
            userName = auth.getCurrentUser().getDisplayName();
            userEmail = auth.getCurrentUser().getEmail();
        }
        
        txtProfileName.setText("Name: " + (userName != null ? userName : "Not set"));
        txtProfileEmail.setText("Email: " + (userEmail != null ? userEmail : "Not set"));
        txtProfilePhone.setText("Phone: " + (userPhone != null ? userPhone : "Not set"));

        // Load profile photo
        loadProfilePhoto(imgProfilePhoto);

        // Make profile photo clickable (tap to change) - set on CardView for better touch area
        cardProfilePhoto.setOnClickListener(v -> showPhotoOptions());

        // Button listeners
        view.findViewById(R.id.btnChangePhoto).setOnClickListener(v -> showPhotoOptions());
        view.findViewById(R.id.btnRemovePhoto).setOnClickListener(v -> removeProfilePhoto());
        view.findViewById(R.id.btnEdit).setOnClickListener(v -> {
            dialog.dismiss();
            showEditProfileDialog();
        });
        
        // Sign In/Logout button logic
        View btnSignIn = view.findViewById(R.id.btnSignIn);
        View btnLogout = view.findViewById(R.id.btnLogout);
        
        if (auth.getCurrentUser() != null) {
            btnSignIn.setVisibility(View.GONE);
            btnLogout.setVisibility(View.VISIBLE);
            btnLogout.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                SharedPreferencesHelper prefs = new SharedPreferencesHelper(requireContext());
                prefs.logout();
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            });
        } else {
            btnSignIn.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.GONE);
            btnSignIn.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                startActivity(intent);
            });
        }

        // Close Button
        view.findViewById(R.id.btnClose).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showPhotoOptions() {
        String[] options = {"📷 Take Photo", "🖼️ Choose from Gallery", "❌ Cancel"};
        
        new AlertDialog.Builder(requireContext())
                .setTitle("Change Profile Photo")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Take Photo
                            openCamera();
                            break;
                        case 1: // Choose from Gallery
                            openGallery();
                            break;
                        case 2: // Cancel
                            dialog.dismiss();
                            break;
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST);
        } else {
            Toast.makeText(requireContext(), "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == getActivity().RESULT_OK && data != null) {
            Bitmap bitmap = null;
            
            try {
                if (requestCode == PICK_IMAGE_REQUEST) {
                    // From gallery
                    Uri imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                } else if (requestCode == CAMERA_REQUEST) {
                    // From camera
                    bitmap = (Bitmap) data.getExtras().get("data");
                } else if (requestCode == INJURY_CAMERA_REQUEST) {
                    // Injury photo from camera
                    bitmap = (Bitmap) data.getExtras().get("data");
                    if (bitmap != null) {
                        showInjuryPhotoDialog(bitmap);
                        return;
                    }
                } else if (requestCode == INJURY_GALLERY_REQUEST) {
                    // Injury photo from gallery
                    Uri imageUri = data.getData();
                    bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
                    if (bitmap != null) {
                        showInjuryPhotoDialog(bitmap);
                        return;
                    }
                }
                
                if (bitmap != null) {
                    // Resize bitmap to reasonable size
                    bitmap = resizeBitmap(bitmap, 300, 300);
                    saveProfilePhoto(bitmap);
                    
                    // Create circular bitmap for display
                    Bitmap circularBitmap = createCircularBitmap(bitmap);
                    
                    if (currentProfileImageView != null) {
                        currentProfileImageView.setImageBitmap(circularBitmap);
                        currentProfileImageView.setBackground(null);
                    }
                    // Also update the main profile image
                    if (mainProfileImageView != null) {
                        mainProfileImageView.setImageBitmap(circularBitmap);
                        mainProfileImageView.setBackground(null);
                    }
                    Toast.makeText(requireContext(), "Profile photo updated!", Toast.LENGTH_SHORT).show();
                }
            } catch (IOException e) {
                Toast.makeText(requireContext(), "Error loading image", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        float scaleWidth = ((float) maxWidth) / width;
        float scaleHeight = ((float) maxHeight) / height;
        float scale = Math.min(scaleWidth, scaleHeight);
        
        int newWidth = Math.round(width * scale);
        int newHeight = Math.round(height * scale);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    private void saveProfilePhoto(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
            
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
            prefsHelper.setUserProfilePhoto(encodedImage);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error saving photo", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void loadProfilePhoto(ImageView imageView) {
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
        String encodedImage = prefsHelper.getUserProfilePhoto();
        
        if (encodedImage != null && !encodedImage.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                
                if (bitmap != null) {
                    // Create circular bitmap for better appearance
                    Bitmap circularBitmap = createCircularBitmap(bitmap);
                    imageView.setImageBitmap(circularBitmap);
                    // Remove background when showing actual photo
                    imageView.setBackground(null);
                } else {
                    setDefaultProfileImage(imageView);
                }
            } catch (Exception e) {
                setDefaultProfileImage(imageView);
                e.printStackTrace();
            }
        } else {
            setDefaultProfileImage(imageView);
        }
    }
    
    private void setDefaultProfileImage(ImageView imageView) {
        imageView.setImageResource(R.drawable.ic_profile);
        imageView.setBackgroundResource(R.drawable.profile_circle_bg);
    }
    
    private Bitmap createCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);
        
        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(output);
        
        android.graphics.Paint paint = new android.graphics.Paint();
        android.graphics.Rect rect = new android.graphics.Rect(0, 0, size, size);
        android.graphics.RectF rectF = new android.graphics.RectF(rect);
        
        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawOval(rectF, paint);
        
        paint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
        
        // Center the bitmap
        int x = (size - width) / 2;
        int y = (size - height) / 2;
        canvas.drawBitmap(bitmap, x, y, paint);
        
        return output;
    }

    private void removeProfilePhoto() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Remove Profile Photo")
                .setMessage("Are you sure you want to remove your profile photo?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
                    prefsHelper.removeUserProfilePhoto();
                    if (currentProfileImageView != null) {
                        currentProfileImageView.setImageResource(R.drawable.default_profile_avatar);
                    }
                    // Also update the main profile image
                    if (mainProfileImageView != null) {
                        setDefaultProfileImage(mainProfileImageView);
                    }
                    Toast.makeText(requireContext(), "Profile photo removed", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ================= EXISTING METHODS =================

    /**
     * Initialize Emergency Data Manager for real-time Firebase integration
     */
    private void initializeEmergencyDataManager() {
        try {
            Log.d("HomeFragment", "Initializing Emergency Data Manager");
            
            // Initialize Firebase Manager first
            FirebaseManager firebaseManager = FirebaseManager.getInstance();
            if (!firebaseManager.isInitialized()) {
                firebaseManager.initialize(requireContext());
            }
            
            // Initialize Emergency Data Manager
            emergencyDataManager = EmergencyDataManager.getInstance(requireContext());
            emergencyDataManager.registerCallback(this);
            
            Log.d("HomeFragment", "Emergency Data Manager initialized successfully");
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Failed to initialize Emergency Data Manager", e);
            // Continue without real-time data - basic emergency features will still work
        }
    }
    
    /**
     * Update emergency UI based on real-time data
     */
    private void updateEmergencyUI() {
        try {
            // Update emergency card with real-time information
            // This could show badges, counts, or alerts
            
            if (!activeAlerts.isEmpty()) {
                // Show alert indicator on emergency card
                Log.d("HomeFragment", "Active emergency alerts: " + activeAlerts.size());
                // Could add a red badge or notification indicator here
            }
            
            if (!nearbyVets.isEmpty()) {
                Log.d("HomeFragment", "Nearby vets available: " + nearbyVets.size());
                // Could show vet count or availability indicator
            }
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error updating emergency UI", e);
        }
    }

    private void showEmergency() {
        // Build emergency message with real-time data
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("24/7 Pet Emergency Support\nCall: 1-800-PET-HELP");
        
        // Add real-time information if available
        if (emergencyDataManager != null) {
            if (!nearbyVets.isEmpty()) {
                int availableVets = 0;
                for (EmergencyDataManager.EmergencyVet vet : nearbyVets) {
                    if (vet.isAvailable) availableVets++;
                }
                messageBuilder.append("\n\n🏥 ").append(availableVets).append(" nearby vets available");
            }
            
            if (!activeAlerts.isEmpty()) {
                messageBuilder.append("\n⚠️ ").append(activeAlerts.size()).append(" active alert");
                if (activeAlerts.size() > 1) messageBuilder.append("s");
                
                // Show most critical alert
                EmergencyDataManager.EmergencyAlert criticalAlert = null;
                for (EmergencyDataManager.EmergencyAlert alert : activeAlerts) {
                    if ("critical".equals(alert.severity) || 
                        (criticalAlert == null && "high".equals(alert.severity))) {
                        criticalAlert = alert;
                    }
                }
                
                if (criticalAlert != null) {
                    messageBuilder.append("\n📢 ").append(criticalAlert.title);
                }
            }
            
            if (!emergencyContacts.isEmpty()) {
                messageBuilder.append("\n📞 ").append(emergencyContacts.size()).append(" emergency contacts available");
            }
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("🚨 Pet Emergency")
                .setMessage(messageBuilder.toString())
                .setItems(new String[]{
                        "📞 Call Emergency Hotline",
                        "📍 Show My Location", 
                        "🏥 Find Nearby Vets" + (nearbyVets.isEmpty() ? "" : " (" + nearbyVets.size() + ")"),
                        "📤 Share My Location",
                        "⚠️ View Alerts" + (activeAlerts.isEmpty() ? "" : " (" + activeAlerts.size() + ")"),
                        "🚨 Report Emergency"
                }, (dialog, which) -> {
                    switch (which) {
                        case 0: // Call Emergency Hotline
                            callEmergencyHotline();
                            break;
                        case 1: // Show My Location
                            showCurrentLocation();
                            break;
                        case 2: // Find Nearby Vets
                            findNearbyVets();
                            break;
                        case 3: // Share My Location
                            shareCurrentLocation();
                            break;
                        case 4: // View Alerts
                            showEmergencyAlerts();
                            break;
                        case 5: // Report Emergency
                            showReportEmergencyDialog();
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void callEmergencyHotline() {
        try {
            android.content.Intent callIntent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
            callIntent.setData(android.net.Uri.parse("tel:1-800-PET-HELP"));
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to make call. Please dial 1-800-PET-HELP manually.", Toast.LENGTH_LONG).show();
        }
    }

    private void showCurrentLocation() {
        // Check location permission
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            
            // Request location permission
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 200);
            return;
        }

        try {
            android.location.LocationManager locationManager = 
                (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            
            android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                final android.location.Location finalLocation = location; // Make final for lambda
                String locationText = "📍 Your Current Location:\n" +
                        "Latitude: " + String.format("%.6f", finalLocation.getLatitude()) + "\n" +
                        "Longitude: " + String.format("%.6f", finalLocation.getLongitude()) + "\n\n" +
                        "Accuracy: ±" + Math.round(finalLocation.getAccuracy()) + " meters";
                
                new AlertDialog.Builder(requireContext())
                        .setTitle("📍 Current Location")
                        .setMessage(locationText)
                        .setPositiveButton("📱 Open in Maps", (dialog, which) -> {
                            openLocationInMaps(finalLocation.getLatitude(), finalLocation.getLongitude());
                        })
                        .setNeutralButton("📋 Copy Coordinates", (dialog, which) -> {
                            copyLocationToClipboard(finalLocation.getLatitude(), finalLocation.getLongitude());
                        })
                        .setNegativeButton("Close", null)
                        .show();
            } else {
                Toast.makeText(requireContext(), "Unable to get current location. Please enable GPS and try again.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void findNearbyVets() {
        // Check location permission
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 201);
            return;
        }

        try {
            android.location.LocationManager locationManager = 
                (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            
            android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                // Show Firebase vets if available, otherwise use Google Maps
                if (!nearbyVets.isEmpty()) {
                    showFirebaseVets(location);
                } else {
                    // Fallback to Google Maps search
                    openGoogleMapsVetSearch(location);
                }
            } else {
                Toast.makeText(requireContext(), "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show Firebase real-time veterinarians
     */
    private void showFirebaseVets(android.location.Location userLocation) {
        if (nearbyVets.isEmpty()) {
            Toast.makeText(requireContext(), "No veterinarians available in real-time data", Toast.LENGTH_SHORT).show();
            openGoogleMapsVetSearch(userLocation);
            return;
        }
        
        // Create vet list with distance calculation
        java.util.List<String> vetOptions = new java.util.ArrayList<>();
        java.util.List<EmergencyDataManager.EmergencyVet> sortedVets = new java.util.ArrayList<>(nearbyVets);
        
        // Sort by distance and availability
        sortedVets.sort((v1, v2) -> {
            // Available vets first
            if (v1.isAvailable != v2.isAvailable) {
                return v1.isAvailable ? -1 : 1;
            }
            
            // Then by distance
            double dist1 = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), v1.latitude, v1.longitude);
            double dist2 = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), v2.latitude, v2.longitude);
            return Double.compare(dist1, dist2);
        });
        
        for (EmergencyDataManager.EmergencyVet vet : sortedVets) {
            double distance = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), vet.latitude, vet.longitude);
            String status = vet.isAvailable ? "🟢 Available" : "🔴 Busy";
            String hours = vet.is24Hour ? "24/7" : "Limited hours";
            String responseTime = vet.responseTimeMinutes > 0 ? " • " + vet.responseTimeMinutes + "min response" : "";
            
            vetOptions.add(String.format("%s %s\n📍 %.1f km • %s%s\n⭐ %.1f rating", 
                status, vet.name, distance, hours, responseTime, vet.rating));
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("🏥 Nearby Veterinarians (" + sortedVets.size() + ")")
                .setMessage("Real-time availability from Firebase")
                .setItems(vetOptions.toArray(new String[0]), (dialog, which) -> {
                    EmergencyDataManager.EmergencyVet selectedVet = sortedVets.get(which);
                    showVetDetails(selectedVet, userLocation);
                })
                .setNeutralButton("🗺️ Open Google Maps", (d, w) -> openGoogleMapsVetSearch(userLocation))
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Calculate distance between two points in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c; // Distance in km
    }
    
    /**
     * Show detailed information about a veterinarian
     */
    private void showVetDetails(EmergencyDataManager.EmergencyVet vet, android.location.Location userLocation) {
        double distance = calculateDistance(userLocation.getLatitude(), userLocation.getLongitude(), vet.latitude, vet.longitude);
        
        String details = String.format(
            "🏥 %s\n\n" +
            "📍 %s\n" +
            "📞 %s\n" +
            "📏 Distance: %.1f km\n" +
            "⏰ %s\n" +
            "⭐ Rating: %.1f/5\n" +
            "🩺 Specialties: %s\n" +
            "⚡ Response time: %d minutes\n" +
            "🟢 Status: %s",
            vet.name,
            vet.address,
            vet.phone,
            distance,
            vet.is24Hour ? "24/7 Emergency" : "Limited hours",
            vet.rating,
            vet.specialties != null ? vet.specialties : "General veterinary care",
            vet.responseTimeMinutes,
            vet.isAvailable ? "Available now" : "Currently busy"
        );
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("🏥 Veterinarian Details")
                .setMessage(details)
                .setPositiveButton("📞 Call Now", (d, w) -> {
                    try {
                        Intent callIntent = new Intent(Intent.ACTION_DIAL);
                        callIntent.setData(Uri.parse("tel:" + vet.phone));
                        startActivity(callIntent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "Unable to make call", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNeutralButton("🗺️ Get Directions", (d, w) -> {
                    openDirectionsToVet(vet);
                })
                .setNegativeButton("Close", null);
        
        if (!vet.isAvailable) {
            builder.setNeutralButton("📋 Report Emergency", (d, w) -> {
                reportEmergencyToVet(vet, userLocation);
            });
        }
        
        builder.show();
    }
    
    /**
     * Open directions to veterinarian
     */
    private void openDirectionsToVet(EmergencyDataManager.EmergencyVet vet) {
        try {
            String uri = String.format("google.navigation:q=%f,%f", vet.latitude, vet.longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");
            
            if (intent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(intent);
            } else {
                // Fallback to web maps
                String webUri = String.format("https://www.google.com/maps/dir/?api=1&destination=%f,%f", 
                    vet.latitude, vet.longitude);
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Unable to open directions", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Report emergency to specific veterinarian
     */
    private void reportEmergencyToVet(EmergencyDataManager.EmergencyVet vet, android.location.Location userLocation) {
        if (emergencyDataManager != null) {
            String description = "Emergency reported to " + vet.name;
            String petInfo = "Location: " + userLocation.getLatitude() + ", " + userLocation.getLongitude();
            
            emergencyDataManager.reportEmergency(
                userLocation.getLatitude(), 
                userLocation.getLongitude(), 
                description, 
                petInfo
            );
            
            Toast.makeText(requireContext(), "Emergency reported to " + vet.name, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Fallback to Google Maps vet search
     */
    private void openGoogleMapsVetSearch(android.location.Location location) {
        // Open Google Maps with nearby veterinarians search
        String mapsUri = "geo:" + location.getLatitude() + "," + location.getLongitude() + 
                       "?q=veterinarian+near+me";
        
        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
            android.net.Uri.parse(mapsUri));
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to web browser
            String webUri = "https://www.google.com/maps/search/veterinarian+near+" + 
                          location.getLatitude() + "," + location.getLongitude();
            android.content.Intent webIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
                android.net.Uri.parse(webUri));
            startActivity(webIntent);
        }
    }

    private void shareCurrentLocation() {
        // Check location permission
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 202);
            return;
        }

        try {
            android.location.LocationManager locationManager = 
                (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            
            android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                String shareText = "🚨 PET EMERGENCY - My Current Location:\n\n" +
                                 "📍 Coordinates: " + location.getLatitude() + ", " + location.getLongitude() + "\n" +
                                 "🗺️ Google Maps: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude() + "\n\n" +
                                 "Please send help! - Sent via PetBuddy Emergency";
                
                android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "🚨 Pet Emergency - Location Share");
                shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareText);
                
                startActivity(android.content.Intent.createChooser(shareIntent, "Share Emergency Location"));
            } else {
                Toast.makeText(requireContext(), "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLocationInMaps(double latitude, double longitude) {
        String mapsUri = "geo:" + latitude + "," + longitude + "?z=16";
        android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, 
            android.net.Uri.parse(mapsUri));
        
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(requireContext(), "No maps app available", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyLocationToClipboard(double latitude, double longitude) {
        String coordinates = latitude + ", " + longitude;
        android.content.ClipboardManager clipboard = 
            (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("Location Coordinates", coordinates);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(requireContext(), "📋 Coordinates copied to clipboard", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Show emergency alerts from Firebase real-time data
     */
    private void showEmergencyAlerts() {
        if (activeAlerts.isEmpty()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("⚠️ Emergency Alerts")
                    .setMessage("✅ No active emergency alerts in your area.\n\nYou will be notified automatically if any emergency situations arise.")
                    .setPositiveButton("🚨 Report Emergency", (d, w) -> showReportEmergencyDialog())
                    .setNegativeButton("Close", null)
                    .show();
            return;
        }
        
        // Sort alerts by severity and timestamp
        java.util.List<EmergencyDataManager.EmergencyAlert> sortedAlerts = new java.util.ArrayList<>(activeAlerts);
        sortedAlerts.sort((a1, a2) -> {
            // Sort by severity first (critical > high > medium > low)
            int severityOrder1 = getSeverityOrder(a1.severity);
            int severityOrder2 = getSeverityOrder(a2.severity);
            
            if (severityOrder1 != severityOrder2) {
                return Integer.compare(severityOrder2, severityOrder1); // Higher severity first
            }
            
            // Then by timestamp (newer first)
            return Long.compare(a2.timestamp, a1.timestamp);
        });
        
        String[] alertOptions = new String[sortedAlerts.size()];
        for (int i = 0; i < sortedAlerts.size(); i++) {
            EmergencyDataManager.EmergencyAlert alert = sortedAlerts.get(i);
            String severityIcon = getSeverityIcon(alert.severity);
            String typeIcon = getTypeIcon(alert.type);
            String timeAgo = getTimeAgo(alert.timestamp);
            
            alertOptions[i] = String.format("%s %s %s\n%s • %s ago", 
                severityIcon, typeIcon, alert.title, alert.type.toUpperCase(), timeAgo);
        }
        
        new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Emergency Alerts (" + activeAlerts.size() + ")")
                .setMessage("Real-time emergency information")
                .setItems(alertOptions, (dialog, which) -> {
                    EmergencyDataManager.EmergencyAlert selectedAlert = sortedAlerts.get(which);
                    showAlertDetails(selectedAlert);
                })
                .setPositiveButton("🚨 Report Emergency", (d, w) -> showReportEmergencyDialog())
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Get severity order for sorting (higher number = higher severity)
     */
    private int getSeverityOrder(String severity) {
        switch (severity.toLowerCase()) {
            case "critical": return 4;
            case "high": return 3;
            case "medium": return 2;
            case "low": return 1;
            default: return 0;
        }
    }
    
    /**
     * Get severity icon
     */
    private String getSeverityIcon(String severity) {
        switch (severity.toLowerCase()) {
            case "critical": return "🚨";
            case "high": return "⚠️";
            case "medium": return "⚡";
            case "low": return "ℹ️";
            default: return "📢";
        }
    }
    
    /**
     * Get type icon
     */
    private String getTypeIcon(String type) {
        switch (type.toLowerCase()) {
            case "weather": return "🌪️";
            case "disease_outbreak": return "🦠";
            case "recall": return "📋";
            case "general": return "📢";
            default: return "⚠️";
        }
    }
    
    /**
     * Get human-readable time ago
     */
    private String getTimeAgo(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        if (diff < 60000) return "just now";
        if (diff < 3600000) return (diff / 60000) + "m";
        if (diff < 86400000) return (diff / 3600000) + "h";
        return (diff / 86400000) + "d";
    }
    
    /**
     * Show detailed alert information
     */
    private void showAlertDetails(EmergencyDataManager.EmergencyAlert alert) {
        String severityIcon = getSeverityIcon(alert.severity);
        String typeIcon = getTypeIcon(alert.type);
        String timeAgo = getTimeAgo(alert.timestamp);
        
        String details = String.format(
            "%s %s %s\n\n" +
            "📝 %s\n\n" +
            "🏷️ Type: %s\n" +
            "⚠️ Severity: %s\n" +
            "📍 Region: %s\n" +
            "⏰ %s ago\n" +
            "🆔 Alert ID: %s",
            severityIcon, typeIcon, alert.title,
            alert.message,
            alert.type.replace("_", " ").toUpperCase(),
            alert.severity.toUpperCase(),
            alert.region != null ? alert.region : "General area",
            timeAgo,
            alert.id
        );
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("⚠️ Emergency Alert Details")
                .setMessage(details)
                .setPositiveButton("📤 Share Alert", (d, w) -> shareAlert(alert))
                .setNegativeButton("Close", null);
        
        // Add action button based on alert type
        if ("recall".equals(alert.type)) {
            builder.setNeutralButton("🔍 Check My Pets", (d, w) -> {
                Toast.makeText(requireContext(), "Check if this recall affects your pets", Toast.LENGTH_LONG).show();
                // Could navigate to pet management or show recall checker
            });
        } else if ("weather".equals(alert.type)) {
            builder.setNeutralButton("🏠 Safety Tips", (d, w) -> {
                showWeatherSafetyTips();
            });
        } else if ("disease_outbreak".equals(alert.type)) {
            builder.setNeutralButton("🏥 Find Vet", (d, w) -> {
                findNearbyVets();
            });
        }
        
        builder.show();
    }
    
    /**
     * Share emergency alert
     */
    private void shareAlert(EmergencyDataManager.EmergencyAlert alert) {
        String shareText = String.format(
            "🚨 EMERGENCY ALERT - %s\n\n" +
            "%s\n\n" +
            "Type: %s\n" +
            "Severity: %s\n" +
            "Region: %s\n\n" +
            "Shared via PetBuddy Emergency System",
            alert.title,
            alert.message,
            alert.type.replace("_", " ").toUpperCase(),
            alert.severity.toUpperCase(),
            alert.region != null ? alert.region : "General area"
        );
        
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "🚨 Emergency Alert: " + alert.title);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        
        startActivity(Intent.createChooser(shareIntent, "Share Emergency Alert"));
    }
    
    /**
     * Show weather safety tips
     */
    private void showWeatherSafetyTips() {
        String tips = "🌪️ WEATHER EMERGENCY SAFETY TIPS\n\n" +
                     "🏠 Keep pets indoors during severe weather\n" +
                     "💧 Ensure fresh water is always available\n" +
                     "🆔 Make sure pets have ID tags and microchips\n" +
                     "📦 Prepare emergency kit with food and supplies\n" +
                     "🏥 Know location of nearest emergency vet\n" +
                     "📱 Keep emergency contacts readily available\n\n" +
                     "Stay safe and keep your pets protected!";
        
        new AlertDialog.Builder(requireContext())
                .setTitle("🌪️ Weather Safety Tips")
                .setMessage(tips)
                .setPositiveButton("🏥 Find Emergency Vet", (d, w) -> findNearbyVets())
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Show report emergency dialog
     */
    private void showReportEmergencyDialog() {
        // Check location permission first
        if (android.content.pm.PackageManager.PERMISSION_GRANTED != 
            androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            
            new AlertDialog.Builder(requireContext())
                    .setTitle("🚨 Report Emergency")
                    .setMessage("Location permission is required to report emergencies accurately.\n\nThis helps emergency responders find you quickly.")
                    .setPositiveButton("Grant Permission", (d, w) -> {
                        requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 203);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        
        // Get current location
        try {
            android.location.LocationManager locationManager = 
                (android.location.LocationManager) requireContext().getSystemService(android.content.Context.LOCATION_SERVICE);
            
            android.location.Location location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            
            if (location != null) {
                showEmergencyReportForm(location);
            } else {
                Toast.makeText(requireContext(), "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_LONG).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show emergency report form
     */
    private void showEmergencyReportForm(android.location.Location location) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);
        
        // Emergency type selection
        TextView typeLabel = new TextView(requireContext());
        typeLabel.setText("Emergency Type:");
        typeLabel.setTextSize(16);
        typeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(typeLabel);
        
        android.widget.Spinner typeSpinner = new android.widget.Spinner(requireContext());
        String[] emergencyTypes = {"Pet Injury", "Pet Missing", "Pet Poisoning", "Severe Weather", "Other Emergency"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
            requireContext(), android.R.layout.simple_spinner_item, emergencyTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(adapter);
        layout.addView(typeSpinner);
        
        // Description input
        TextView descLabel = new TextView(requireContext());
        descLabel.setText("\nDescription:");
        descLabel.setTextSize(16);
        descLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(descLabel);
        
        EditText descInput = new EditText(requireContext());
        descInput.setHint("Describe the emergency situation...");
        descInput.setLines(3);
        descInput.setMaxLines(5);
        layout.addView(descInput);
        
        // Pet information input
        TextView petLabel = new TextView(requireContext());
        petLabel.setText("\nPet Information:");
        petLabel.setTextSize(16);
        petLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        layout.addView(petLabel);
        
        EditText petInput = new EditText(requireContext());
        petInput.setHint("Pet name, breed, age, etc...");
        layout.addView(petInput);
        
        // Location info
        TextView locationInfo = new TextView(requireContext());
        locationInfo.setText(String.format("\n📍 Your Location:\n%.6f, %.6f", 
            location.getLatitude(), location.getLongitude()));
        locationInfo.setTextSize(12);
        locationInfo.setTextColor(0xFF666666);
        layout.addView(locationInfo);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("🚨 Report Emergency")
                .setView(layout)
                .setPositiveButton("🚨 REPORT NOW", (dialog, which) -> {
                    String emergencyType = emergencyTypes[typeSpinner.getSelectedItemPosition()];
                    String description = descInput.getText().toString().trim();
                    String petInfo = petInput.getText().toString().trim();
                    
                    if (description.isEmpty()) {
                        Toast.makeText(requireContext(), "Please describe the emergency", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    reportEmergency(location, emergencyType, description, petInfo);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    /**
     * Report emergency to Firebase
     */
    private void reportEmergency(android.location.Location location, String type, String description, String petInfo) {
        if (emergencyDataManager != null) {
            String fullDescription = type + ": " + description;
            emergencyDataManager.reportEmergency(
                location.getLatitude(), 
                location.getLongitude(), 
                fullDescription, 
                petInfo
            );
            
            // Show confirmation
            new AlertDialog.Builder(requireContext())
                    .setTitle("✅ Emergency Reported")
                    .setMessage("Your emergency has been reported successfully.\n\n" +
                              "📍 Location: " + location.getLatitude() + ", " + location.getLongitude() + "\n" +
                              "🚨 Type: " + type + "\n\n" +
                              "Emergency responders have been notified. Please call emergency services if immediate assistance is needed.")
                    .setPositiveButton("📞 Call Emergency", (d, w) -> callEmergencyHotline())
                    .setNegativeButton("Close", null)
                    .show();
        } else {
            Toast.makeText(requireContext(), "Unable to report emergency. Please call emergency services directly.", Toast.LENGTH_LONG).show();
        }
    }

    private void showSearch() {
        try {
            EditText input = new EditText(requireContext());
            input.setHint("Search pets, services, blogs, events...");
            input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
            
            // Style the EditText with error handling
            input.setPadding(20, 20, 20, 20);
            try {
                input.setBackgroundResource(R.drawable.edit_text_background);
            } catch (Exception e) {
                // Fallback styling if drawable not found
                input.setBackgroundColor(0xFFFFFFFF);
            }
            
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("🔍 Search PetBuddy")
                    .setMessage("Search for pets, training videos, services, blogs, events, and more!")
                    .setView(input)
                    .setPositiveButton("SEARCH", (dialogInterface, which) -> {
                        String searchQuery = input.getText().toString().trim();
                        if (!searchQuery.isEmpty()) {
                            Toast.makeText(requireContext(), "Searching for: " + searchQuery, Toast.LENGTH_SHORT).show();
                            performSearch(searchQuery);
                        } else {
                            Toast.makeText(requireContext(), "Please enter something to search", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("CANCEL", null)
                    .create();
            
            // Show keyboard automatically
            dialog.setOnShowListener(dialogInterface -> {
                try {
                    input.requestFocus();
                    android.view.inputmethod.InputMethodManager imm = 
                        (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.showSoftInput(input, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                    }
                } catch (Exception e) {
                    // Keyboard showing failed, but dialog still works
                }
            });
            
            dialog.show();
            
        } catch (Exception e) {
            // Fallback if dialog creation fails
            Toast.makeText(requireContext(), "Search feature is loading... Please try again.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void performSearch(String query) {
        try {
            String queryLower = query.toLowerCase().trim();
            
            // Show immediate feedback
            Toast.makeText(requireContext(), "🔍 Searching for: " + query, Toast.LENGTH_SHORT).show();
            
            // Smart search - automatically detect what user is searching for and go directly
            
            // Training Videos Keywords
            if (queryLower.contains("cross") || queryLower.contains("legs")) {
                Toast.makeText(requireContext(), "🎓 Opening Cross Legs Training Video", Toast.LENGTH_SHORT).show();
                playSpecificVideo("Cross Legs Training", R.raw.dog_training_cross_legs);
                return;
            } else if (queryLower.contains("down") || queryLower.contains("command")) {
                Toast.makeText(requireContext(), "🎓 Opening Down Command Training Video", Toast.LENGTH_SHORT).show();
                playSpecificVideo("Down Command Training", R.raw.dog_training_down);
                return;
            } else if (queryLower.contains("paw") || queryLower.contains("shake") || queryLower.contains("hand")) {
                Toast.makeText(requireContext(), "🎓 Opening Paw Shake Training Video", Toast.LENGTH_SHORT).show();
                playSpecificVideo("Paw Shake Training", R.raw.dog_training_paw_shake);
                return;
            } else if (queryLower.contains("training") || queryLower.contains("video") || queryLower.contains("teach")) {
                Toast.makeText(requireContext(), "🎓 Opening Training Videos Section", Toast.LENGTH_SHORT).show();
                searchTrainingVideos(query);
                return;
            }
            
            // Pet/Animal Keywords - redirect to community instead
            if (queryLower.contains("pet") || queryLower.contains("dog") || queryLower.contains("cat") || 
                queryLower.contains("puppy") || queryLower.contains("kitten") || queryLower.contains("adopt") || 
                queryLower.contains("animal") || queryLower.contains("breed")) {
                Toast.makeText(requireContext(), "🐾 Opening Community Section", Toast.LENGTH_SHORT).show();
                searchCommunity(query);
                return;
            }
            
            // Veterinarian Keywords
            if (queryLower.contains("vet") || queryLower.contains("doctor") || queryLower.contains("clinic") || 
                queryLower.contains("medical") || queryLower.contains("health") || queryLower.contains("treatment")) {
                Toast.makeText(requireContext(), "🏥 Opening Veterinarian Section", Toast.LENGTH_SHORT).show();
                searchVeterinarians(query);
                return;
            }
            
            // Blog Keywords
            if (queryLower.contains("blog") || queryLower.contains("article") || queryLower.contains("tip") || 
                queryLower.contains("guide") || queryLower.contains("advice") || queryLower.contains("care")) {
                Toast.makeText(requireContext(), "📖 Opening Blogs Section", Toast.LENGTH_SHORT).show();
                searchBlogs(query);
                return;
            }
            
            // Event Keywords
            if (queryLower.contains("event") || queryLower.contains("activity") || queryLower.contains("meet") || 
                queryLower.contains("gathering") || queryLower.contains("workshop") || queryLower.contains("class")) {
                Toast.makeText(requireContext(), "📅 Opening Events Section", Toast.LENGTH_SHORT).show();
                searchEvents(query);
                return;
            }
            
            // Community Keywords
            if (queryLower.contains("community") || queryLower.contains("member") || queryLower.contains("people") || 
                queryLower.contains("friend") || queryLower.contains("social") || queryLower.contains("chat")) {
                Toast.makeText(requireContext(), "👥 Opening Community Section", Toast.LENGTH_SHORT).show();
                searchCommunity(query);
                return;
            }
            
            // Reviews & Feedback Keywords - removed due to build issues
            
            // Service Keywords
            if (queryLower.contains("service") || queryLower.contains("grooming") || 
                queryLower.contains("boarding") || queryLower.contains("emergency")) {
                Toast.makeText(requireContext(), "🛠️ Opening Services Section", Toast.LENGTH_SHORT).show();
                showServices();
                return;
            }
            
            // Food Keywords
            if (queryLower.contains("food") || queryLower.contains("feed") || queryLower.contains("nutrition") || 
                queryLower.contains("diet") || queryLower.contains("meal")) {
                Toast.makeText(requireContext(), "🍽️ Showing pet food options", Toast.LENGTH_SHORT).show();
                // Navigate to pet food section
                return;
            }
            
            // Default: If no specific keywords found, search in community (most common search)
            Toast.makeText(requireContext(), "👥 Searching in Community (default)", Toast.LENGTH_SHORT).show();
            searchCommunity(query);
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Search encountered an error. Please try again.", Toast.LENGTH_LONG).show();
        }
    }
    
    
    private void searchVeterinarians(String query) {
        try {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new VeterinarianFragment())
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Veterinarian section is currently unavailable", Toast.LENGTH_LONG).show();
        }
    }
    
    private void searchBlogs(String query) {
        try {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new BlogsFragment())
                    .addToBackStack(null)
                    .commit();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Blogs section is currently unavailable", Toast.LENGTH_LONG).show();
        }
    }
    
    private void searchEvents(String query) {
        try {
            Intent intent = new Intent(requireContext(), EventsActivity.class);
            intent.putExtra("search_query", query);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Events section is currently unavailable", Toast.LENGTH_LONG).show();
        }
    }
    
    private void searchCommunity(String query) {
        try {
            Intent intent = new Intent(requireContext(), CommunityActivity.class);
            intent.putExtra("search_query", query);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Community section is currently unavailable", Toast.LENGTH_LONG).show();
        }
    }
    
    private void searchTrainingVideos(String query) {
        try {
            // Search through training videos
            String queryLower = query.toLowerCase();
            if (queryLower.contains("cross") || queryLower.contains("legs")) {
                playSpecificVideo("Cross Legs Training", R.raw.dog_training_cross_legs);
            } else if (queryLower.contains("down") || queryLower.contains("command")) {
                playSpecificVideo("Down Command Training", R.raw.dog_training_down);
            } else if (queryLower.contains("paw") || queryLower.contains("shake") || queryLower.contains("hand")) {
                playSpecificVideo("Paw Shake Training", R.raw.dog_training_paw_shake);
            } else {
                // Show all training videos
                Intent intent = new Intent(requireContext(), TrainingVideosActivity.class);
                startActivity(intent);
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Training videos section is currently unavailable", Toast.LENGTH_LONG).show();
        }
    }
    
    // searchReviewsFeedback method removed due to build issues

    private void showServices() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Services")
                .setItems(new String[]{
                        "Veterinarian", "Training Videos"
                }, (d, i) -> bookService(new String[]{
                        "Veterinarian", "Training Videos"
                }[i]))
                .show();
    }

    // ================= UNIFIED CAMERA ACCESS LOGIC =================
    
    /**
     * Unified camera access method that handles all camera access scenarios
     * with consistent permission checking and error handling.
     * 
     * @param source The source of the camera access request (direct or dialog)
     */
    private void handleCameraAccess(CameraAccessSource source) {
        Log.d("HomeFragment", "handleCameraAccess() called from: " + source);
        
        try {
            // Step 1: Check camera permissions
            if (!checkCameraPermissions()) {
                requestCameraPermissions(source);
                return;
            }
            
            // Step 2: Create and validate camera intent
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            if (cameraIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                // Step 3: Launch camera
                Log.d("HomeFragment", "Camera app found, starting camera for: " + source);
                String message = source == CameraAccessSource.DIRECT_ACCESS ? 
                    "📷 Opening camera directly..." : 
                    "📷 Opening camera for injury tracking...";
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                
                startActivityForResult(cameraIntent, INJURY_CAMERA_REQUEST);
            } else {
                // Step 4: Handle camera unavailable
                handleCameraUnavailable(source);
            }
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error in camera access", e);
            handleCameraError(source, e);
        }
    }
    
    /**
     * Check if camera permissions are granted
     */
    private boolean checkCameraPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return requireActivity().checkSelfPermission(android.Manifest.permission.CAMERA) == 
                   android.content.pm.PackageManager.PERMISSION_GRANTED;
        }
        return true; // Pre-M devices don't need runtime permissions
    }
    
    /**
     * Request camera permissions with proper context tracking
     */
    private void requestCameraPermissions(CameraAccessSource source) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d("HomeFragment", "Requesting camera permission for: " + source);
            
            // Check if we should show permission rationale
            if (requireActivity().shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
                showCameraPermissionRationale(source);
            } else {
                // Request permission directly
                int requestCode = source == CameraAccessSource.DIRECT_ACCESS ? 
                    CAMERA_PERMISSION_DIRECT_ACCESS : CAMERA_PERMISSION_DIALOG_ACCESS;
                
                requireActivity().requestPermissions(
                    new String[]{android.Manifest.permission.CAMERA}, 
                    requestCode
                );
            }
        }
    }
    
    /**
     * Show camera permission rationale dialog
     */
    private void showCameraPermissionRationale(CameraAccessSource source) {
        new AlertDialog.Builder(requireContext())
                .setTitle("📷 Camera Permission Required")
                .setMessage("PetBuddy needs camera access to take injury photos for tracking your pet's health.\n\n" +
                           "• Document injuries and healing progress\n" +
                           "• Share photos with veterinarians\n" +
                           "• Keep visual records for better care")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    int requestCode = source == CameraAccessSource.DIRECT_ACCESS ? 
                        CAMERA_PERMISSION_DIRECT_ACCESS : CAMERA_PERMISSION_DIALOG_ACCESS;
                    
                    requireActivity().requestPermissions(
                        new String[]{android.Manifest.permission.CAMERA}, 
                        requestCode
                    );
                })
                .setNegativeButton("Not Now", (dialog, which) -> {
                    Toast.makeText(requireContext(), "Camera permission is needed to take injury photos", Toast.LENGTH_LONG).show();
                })
                .show();
    }
    
    /**
     * Handle camera permission denial with clear guidance
     */
    private void handleCameraPermissionDenied(String accessType) {
        // Check if permission was permanently denied
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            !requireActivity().shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)) {
            
            // Permission permanently denied - guide user to settings
            new AlertDialog.Builder(requireContext())
                    .setTitle("📷 Camera Permission Needed")
                    .setMessage("Camera permission was denied. To take injury photos, please:\n\n" +
                               "1. Go to Settings → Apps → PetBuddy\n" +
                               "2. Tap Permissions\n" +
                               "3. Enable Camera permission\n" +
                               "4. Return to PetBuddy and try again")
                    .setPositiveButton("Open Settings", (dialog, which) -> {
                        try {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + requireContext().getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(requireContext(), "Please enable camera permission in device settings", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            // Permission denied but not permanently
            Toast.makeText(requireContext(), 
                "Camera permission is required to take injury photos for " + accessType, 
                Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Handle camera app unavailable scenario
     */
    private void handleCameraUnavailable(CameraAccessSource source) {
        Log.e("HomeFragment", "No camera app available for: " + source);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("📷 Camera Not Available")
                .setMessage("No camera app is available on this device.\n\nAlternative options:")
                .setPositiveButton("📁 View Saved Photos", (dialog, which) -> showSavedInjuryPhotos())
                .setNeutralButton("🖼️ Choose from Gallery", (dialog, which) -> openInjuryGallery())
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Handle camera access errors with proper guidance
     */
    private void handleCameraError(CameraAccessSource source, Exception e) {
        Log.e("HomeFragment", "Camera error for " + source + ": " + e.getMessage(), e);
        
        new AlertDialog.Builder(requireContext())
                .setTitle("📷 Camera Error")
                .setMessage("Unable to access camera: " + e.getMessage() + "\n\nTry these alternatives:")
                .setPositiveButton("🖼️ Use Gallery", (dialog, which) -> openInjuryGallery())
                .setNeutralButton("Retry Camera", (dialog, which) -> handleCameraAccess(source))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void testDirectCameraAccess() {
        Log.d("HomeFragment", "Direct camera access requested from Injury Tracker card");
        handleCameraAccess(CameraAccessSource.DIRECT_ACCESS);
    }

    private void showInjuryTracker() {
        Log.d("HomeFragment", "showInjuryTracker() method called");
        
        try {
            // Check if there are saved photos to make the option more informative
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
            String savedPhotos = prefsHelper.getString("injury_photos", "");
            String firebasePhotos = prefsHelper.getString("injury_photos_firebase", "");
            
            // Count total saved photos
            int totalPhotos = 0;
            if (!savedPhotos.isEmpty()) {
                totalPhotos += savedPhotos.split(";").length;
            }
            if (!firebasePhotos.isEmpty()) {
                // Count unique Firebase photos (avoid double counting)
                String[] firebaseEntries = firebasePhotos.split(";");
                for (String entry : firebaseEntries) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 3) {
                        String filename = parts[0];
                        // Check if this photo is not already counted in local photos
                        if (savedPhotos.isEmpty() || !savedPhotos.contains(filename + ":")) {
                            totalPhotos++;
                        }
                    }
                }
            }
            
            String message = "Document pet injuries with photos for tracking and veterinary consultation.\n\n" +
                           "• Take photos of injuries\n" +
                           "• Track healing progress\n" +
                           "• Share with veterinarians\n" +
                           "• Cloud backup available";
            
            if (totalPhotos > 0) {
                message += "\n\n📁 You have " + totalPhotos + " saved injury photo" + (totalPhotos == 1 ? "" : "s");
            }
            
            // Create dialog with three main buttons
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                    .setTitle("📷🩹 Injury Tracker Camera")
                    .setMessage(message)
                    .setPositiveButton("📷 Take Photo", (dialog, which) -> {
                        Log.d("HomeFragment", "Take Photo selected");
                        Toast.makeText(requireContext(), "Opening camera...", Toast.LENGTH_SHORT).show();
                        openInjuryCamera();
                    })
                    .setNeutralButton("📁 View Photos (" + totalPhotos + ")", (dialog, which) -> {
                        Log.d("HomeFragment", "View Saved Photos selected");
                        Toast.makeText(requireContext(), "Loading saved photos...", Toast.LENGTH_SHORT).show();
                        showSavedInjuryPhotos();
                    })
                    .setNegativeButton("🖼️ Gallery", (dialog, which) -> {
                        Log.d("HomeFragment", "View Gallery selected");
                        Toast.makeText(requireContext(), "Opening gallery...", Toast.LENGTH_SHORT).show();
                        openInjuryGallery();
                    });
            
            // Show the dialog
            AlertDialog dialog = builder.create();
            dialog.show();
            
            Log.d("HomeFragment", "Injury tracker dialog shown successfully");
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error creating injury tracker dialog", e);
            Toast.makeText(requireContext(), "❌ Error opening Injury Tracker: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openInjuryCamera() {
        Log.d("HomeFragment", "Camera access requested from dialog option");
        handleCameraAccess(CameraAccessSource.DIALOG_ACCESS);
    }

    private void openInjuryGallery() {
        // Check for storage permission on Android 6.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (requireActivity().checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != 
                android.content.pm.PackageManager.PERMISSION_GRANTED) {
                
                // Request storage permission
                requireActivity().requestPermissions(
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 
                    STORAGE_PERMISSION_GALLERY
                );
                return;
            }
        }
        
        Toast.makeText(requireContext(), "🖼️ Opening gallery for injury photos...", Toast.LENGTH_SHORT).show();
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryIntent.setType("image/*");
        try {
            startActivityForResult(galleryIntent, INJURY_GALLERY_REQUEST);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error opening gallery: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showInjuryPhotoDialog(Bitmap bitmap) {
        // Create a dialog to show the injury photo with options
        ImageView imageView = new ImageView(requireContext());
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(20, 20, 20, 20);
        
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("📷🩹 Injury Photo Captured")
                .setView(imageView)
                .setMessage("Photo captured successfully!\n\nThis injury photo will be saved for tracking. You can share this with your veterinarian for consultation.")
                .setPositiveButton("Save & Share", (d, w) -> {
                    Log.d("HomeFragment", "User clicked Save & Share");
                    saveInjuryPhoto(bitmap);
                    shareInjuryPhoto(bitmap);
                })
                .setNeutralButton("Save Only", (d, w) -> {
                    Log.d("HomeFragment", "User clicked Save Only");
                    saveInjuryPhoto(bitmap);
                })
                .setNegativeButton("Discard", (d, w) -> {
                    Log.d("HomeFragment", "User clicked Discard");
                    Toast.makeText(requireContext(), "Photo discarded", Toast.LENGTH_SHORT).show();
                })
                .create();
        
        // Apply pink theme to dialog
        dialog.setOnShowListener(dialogInterface -> {
            try {
                // Set pink colors for all dialog buttons
                if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFE91E63); // Pink color
                }
                if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFE91E63); // Pink color
                }
                if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFFE91E63); // Pink color
                }
            } catch (Exception e) {
                // Fallback if theming fails
                Log.w("HomeFragment", "Failed to apply dialog theme", e);
            }
        });
        
        // Auto-save the photo (so users don't have to click Save)
        Log.d("HomeFragment", "Auto-saving injury photo");
        saveInjuryPhoto(bitmap);
        
        dialog.show();
    }

    private void saveInjuryPhoto(Bitmap bitmap) {
        try {
            // Save to app's private storage with timestamp
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "injury_" + timestamp + ".jpg";
            
            Log.d("HomeFragment", "=== DEBUG: Saving Injury Photo ===");
            Log.d("HomeFragment", "Filename: " + filename);
            Log.d("HomeFragment", "Timestamp: " + timestamp);
            
            // Save to internal storage (for offline access)
            java.io.FileOutputStream fos = requireContext().openFileOutput(filename, Context.MODE_PRIVATE);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            Log.d("HomeFragment", "Photo saved to internal storage successfully");
            
            // FIXED: Improved SharedPreferences tracking with proper error handling
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
            String existingPhotos = prefsHelper.getString("injury_photos", "");
            String newPhotoEntry = filename + ":" + timestamp + ":" + new java.util.Date().toString();
            String updatedPhotos = existingPhotos.isEmpty() ? newPhotoEntry : existingPhotos + ";" + newPhotoEntry;
            
            Log.d("HomeFragment", "Existing photos: " + existingPhotos);
            Log.d("HomeFragment", "New photo entry: " + newPhotoEntry);
            Log.d("HomeFragment", "Updated photos: " + updatedPhotos);
            
            // Use SharedPreferences directly for more reliable saving
            SharedPreferences prefs = requireContext().getSharedPreferences("PetBuddyPrefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("injury_photos", updatedPhotos);
            boolean saveSuccess = editor.commit(); // Use commit() for immediate save
            
            Log.d("HomeFragment", "SharedPreferences save success: " + saveSuccess);
            
            // IMMEDIATE VERIFICATION: Test if photo can be retrieved right after saving
            boolean canRetrievePhoto = verifyPhotoCanBeRetrieved(filename);
            Log.d("HomeFragment", "Immediate photo retrieval test: " + canRetrievePhoto);
            
            if (!canRetrievePhoto) {
                Log.e("HomeFragment", "ERROR: Photo cannot be retrieved immediately after save!");
                Toast.makeText(requireContext(), "⚠️ Warning: Photo saved but may not appear in gallery", Toast.LENGTH_LONG).show();
            }
            
            // Save to Firebase Storage for cloud backup and sharing (don't let this block local viewing)
            try {
                saveInjuryPhotoToFirebase(bitmap, filename, timestamp);
            } catch (Exception firebaseError) {
                Log.w("HomeFragment", "Firebase backup failed, but local save succeeded", firebaseError);
                // Don't show error to user - local save is what matters for viewing
            }
            
            Toast.makeText(requireContext(), "📷 Injury photo saved successfully!\nFile: " + filename, Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error saving injury photo", e);
            Toast.makeText(requireContext(), "❌ Error saving injury photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    /**
     * Verify that a saved photo can be retrieved immediately after saving
     */
    private boolean verifyPhotoCanBeRetrieved(String filename) {
        try {
            // Test 1: Check if file exists in internal storage
            String[] fileList = requireContext().fileList();
            boolean fileExists = false;
            for (String file : fileList) {
                if (file.equals(filename)) {
                    fileExists = true;
                    break;
                }
            }
            
            if (!fileExists) {
                Log.e("HomeFragment", "File does not exist in internal storage: " + filename);
                return false;
            }
            
            // Test 2: Try to load the bitmap
            java.io.FileInputStream fis = requireContext().openFileInput(filename);
            Bitmap testBitmap = BitmapFactory.decodeStream(fis);
            fis.close();
            
            if (testBitmap == null) {
                Log.e("HomeFragment", "Cannot decode bitmap from file: " + filename);
                return false;
            }
            
            // Test 3: Check SharedPreferences tracking
            SharedPreferences prefs = requireContext().getSharedPreferences("PetBuddyPrefs", Context.MODE_PRIVATE);
            String savedPhotos = prefs.getString("injury_photos", "");
            boolean inSharedPrefs = savedPhotos.contains(filename);
            
            Log.d("HomeFragment", "Photo verification - File exists: " + fileExists + 
                                 ", Bitmap loadable: " + (testBitmap != null) + 
                                 ", In SharedPrefs: " + inSharedPrefs);
            
            return fileExists && testBitmap != null; // Don't require SharedPrefs for success
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error verifying photo retrieval for: " + filename, e);
            return false;
        }
    }
    
    /**
     * Save injury photo to Firebase Storage for cloud backup and sharing
     */
    private void saveInjuryPhotoToFirebase(Bitmap bitmap, String filename, String timestamp) {
        try {
            // Get current user ID for organizing photos
            FirebaseAuth auth = FirebaseAuth.getInstance();
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
            
            // Use ImageStorageService to upload to Firebase
            ImageStorageService imageService = ImageStorageService.getInstance(requireContext());
            
            // Create unique injury photo ID
            String injuryPhotoId = "injury_" + userId + "_" + timestamp;
            
            imageService.uploadInjuryPhoto(injuryPhotoId, bitmap, new ImageStorageService.UploadCallback() {
                @Override
                public void onSuccess(String imageUrl) {
                    Log.d("HomeFragment", "Injury photo uploaded to Firebase: " + imageUrl);
                    
                    // Save Firebase URL to SharedPreferences for future access
                    SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
                    String firebaseUrls = prefsHelper.getString("injury_photos_firebase", "");
                    String newUrlEntry = filename + ":" + imageUrl + ":" + timestamp;
                    String updatedUrls = firebaseUrls.isEmpty() ? newUrlEntry : firebaseUrls + ";" + newUrlEntry;
                    prefsHelper.edit().putString("injury_photos_firebase", updatedUrls).apply();
                    
                    // Save to Firebase Realtime Database for injury tracking
                    saveInjuryRecordToDatabase(injuryPhotoId, imageUrl, timestamp, filename);
                    
                    // Show success message
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "☁️ Photo backed up to cloud successfully!", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                
                @Override
                public void onFailure(Exception exception) {
                    Log.e("HomeFragment", "Failed to upload injury photo to Firebase", exception);
                    
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "⚠️ Photo saved locally, cloud backup failed", Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error uploading injury photo to Firebase", e);
            Toast.makeText(requireContext(), "⚠️ Photo saved locally, cloud backup failed", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Save injury record to Firebase Realtime Database for tracking and veterinary access
     */
    private void saveInjuryRecordToDatabase(String injuryPhotoId, String imageUrl, String timestamp, String filename) {
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "anonymous";
            
            // Create injury record
            java.util.Map<String, Object> injuryRecord = new java.util.HashMap<>();
            injuryRecord.put("photoId", injuryPhotoId);
            injuryRecord.put("imageUrl", imageUrl);
            injuryRecord.put("filename", filename);
            injuryRecord.put("timestamp", timestamp);
            injuryRecord.put("dateCreated", new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()));
            injuryRecord.put("userId", userId);
            injuryRecord.put("status", "active"); // active, resolved, archived
            injuryRecord.put("notes", ""); // For veterinary notes
            injuryRecord.put("severity", "unknown"); // unknown, mild, moderate, severe
            
            // Save to Firebase Database under injury_photos node
            FirebaseManager firebaseManager = FirebaseManager.getInstance();
            com.google.firebase.database.DatabaseReference injuryRef = firebaseManager.getDatabase()
                .getReference("injury_photos")
                .child(userId)
                .child(injuryPhotoId);
            
            injuryRef.setValue(injuryRecord)
                .addOnSuccessListener(aVoid -> {
                    Log.d("HomeFragment", "Injury record saved to database: " + injuryPhotoId);
                })
                .addOnFailureListener(exception -> {
                    Log.e("HomeFragment", "Failed to save injury record to database", exception);
                });
                
        } catch (Exception e) {
            Log.e("HomeFragment", "Error saving injury record to database", e);
        }
    }

    private void showSavedInjuryPhotos() {
        Log.d("HomeFragment", "=== SHOWING SAVED INJURY PHOTOS ===");
        
        // STEP 1: ALWAYS scan internal storage first (most reliable)
        String[] fileList = requireContext().fileList();
        java.util.List<String> injuryFiles = new java.util.ArrayList<>();
        for (String file : fileList) {
            if (file.startsWith("injury_") && file.endsWith(".jpg")) {
                injuryFiles.add(file);
                Log.d("HomeFragment", "Found injury file: " + file);
            }
        }
        
        Log.d("HomeFragment", "Direct file scan found " + injuryFiles.size() + " injury photos");
        
        // STEP 2: Get SharedPreferences data (for metadata, but not required)
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
        String savedPhotos = prefsHelper.getString("injury_photos", "");
        String firebasePhotos = prefsHelper.getString("injury_photos_firebase", "");
        
        Log.d("HomeFragment", "SharedPreferences - Local: " + savedPhotos);
        Log.d("HomeFragment", "SharedPreferences - Firebase: " + firebasePhotos);
        
        // STEP 3: If we have files, show them (prioritize direct file access)
        if (!injuryFiles.isEmpty()) {
            Log.d("HomeFragment", "Using direct file access for " + injuryFiles.size() + " photos");
            showPhotosFromFiles(injuryFiles, savedPhotos, firebasePhotos);
            return;
        }
        
        // STEP 4: Fallback to SharedPreferences-only data (if files are missing but metadata exists)
        if (!savedPhotos.isEmpty() || !firebasePhotos.isEmpty()) {
            Log.d("HomeFragment", "No files found, but SharedPreferences has data - showing metadata-based view");
            showPhotosFromMetadata(savedPhotos, firebasePhotos);
            return;
        }
        
        // STEP 5: No photos found anywhere
        Log.d("HomeFragment", "No photos found anywhere - showing empty state");
        showEmptyPhotoState();
    }
    
    /**
     * Show photos using direct file access (most reliable method)
     */
    private void showPhotosFromFiles(java.util.List<String> injuryFiles, String savedPhotos, String firebasePhotos) {
        Log.d("HomeFragment", "Showing photos from direct file access");
        
        // Create enhanced photo entries by combining file data with metadata
        java.util.List<PhotoEntry> allPhotos = new java.util.ArrayList<>();
        
        for (String filename : injuryFiles) {
            // Extract timestamp from filename (injury_TIMESTAMP.jpg)
            String timestamp = "0";
            String date = "Unknown date";
            boolean hasFirebaseBackup = false;
            String firebaseUrl = null;
            
            try {
                if (filename.startsWith("injury_") && filename.endsWith(".jpg")) {
                    String timestampStr = filename.substring(7, filename.length() - 4); // Remove "injury_" and ".jpg"
                    timestamp = timestampStr;
                    long timestampLong = Long.parseLong(timestampStr);
                    date = new java.util.Date(timestampLong).toString();
                }
            } catch (Exception e) {
                Log.w("HomeFragment", "Could not parse timestamp from filename: " + filename);
            }
            
            // Check if this file has Firebase backup info in SharedPreferences
            if (!firebasePhotos.isEmpty()) {
                String[] firebaseEntries = firebasePhotos.split(";");
                for (String entry : firebaseEntries) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 2 && parts[0].equals(filename)) {
                        hasFirebaseBackup = true;
                        firebaseUrl = parts[1];
                        Log.d("HomeFragment", "Found Firebase backup for: " + filename);
                        break;
                    }
                }
            }
            
            // Check SharedPreferences for better date info
            if (!savedPhotos.isEmpty()) {
                String[] photoEntries = savedPhotos.split(";");
                for (String entry : photoEntries) {
                    String[] parts = entry.split(":");
                    if (parts.length >= 3 && parts[0].equals(filename)) {
                        date = parts[2]; // Use the saved date string
                        Log.d("HomeFragment", "Found metadata for: " + filename + " -> " + date);
                        break;
                    }
                }
            }
            
            PhotoEntry photoEntry = new PhotoEntry(filename, timestamp, date, firebaseUrl, hasFirebaseBackup);
            allPhotos.add(photoEntry);
            Log.d("HomeFragment", "Created photo entry: " + filename + " (Firebase: " + hasFirebaseBackup + ")");
        }
        
        // Sort by timestamp (newest first)
        allPhotos.sort((a, b) -> {
            try {
                return Long.compare(Long.parseLong(b.timestamp), Long.parseLong(a.timestamp));
            } catch (NumberFormatException e) {
                return b.timestamp.compareTo(a.timestamp);
            }
        });
        
        // Show the photo gallery
        showPhotoGallery(allPhotos);
    }
    
    /**
     * Show photos from SharedPreferences metadata only (fallback method)
     */
    private void showPhotosFromMetadata(String savedPhotos, String firebasePhotos) {
        Log.d("HomeFragment", "Showing photos from metadata only (files may be missing)");
        
        java.util.List<PhotoEntry> allPhotos = new java.util.ArrayList<>();
        
        // Add local photos from metadata
        if (!savedPhotos.isEmpty()) {
            String[] photoEntries = savedPhotos.split(";");
            for (String entry : photoEntries) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    PhotoEntry photoEntry = new PhotoEntry(parts[0], parts[1], parts[2], null, false);
                    allPhotos.add(photoEntry);
                }
            }
        }
        
        // Add Firebase photos from metadata
        if (!firebasePhotos.isEmpty()) {
            String[] firebaseEntries = firebasePhotos.split(";");
            for (String entry : firebaseEntries) {
                String[] parts = entry.split(":");
                if (parts.length >= 3) {
                    // Check if already exists in local list
                    boolean exists = false;
                    for (PhotoEntry existing : allPhotos) {
                        if (existing.filename.equals(parts[0])) {
                            existing.firebaseUrl = parts[1];
                            existing.hasFirebaseBackup = true;
                            exists = true;
                            break;
                        }
                    }
                    
                    if (!exists) {
                        PhotoEntry photoEntry = new PhotoEntry(parts[0], parts[2], "Cloud backup", parts[1], true);
                        allPhotos.add(photoEntry);
                    }
                }
            }
        }
        
        if (allPhotos.isEmpty()) {
            showEmptyPhotoState();
            return;
        }
        
        // Sort and show
        allPhotos.sort((a, b) -> {
            try {
                return Long.compare(Long.parseLong(b.timestamp), Long.parseLong(a.timestamp));
            } catch (NumberFormatException e) {
                return b.timestamp.compareTo(a.timestamp);
            }
        });
        
        showPhotoGallery(allPhotos);
    }
    
    /**
     * Show the photo gallery dialog
     */
    private void showPhotoGallery(java.util.List<PhotoEntry> allPhotos) {
        String[] photoList = new String[allPhotos.size()];
        for (int i = 0; i < allPhotos.size(); i++) {
            PhotoEntry photo = allPhotos.get(i);
            String cloudIcon = photo.hasFirebaseBackup ? "☁️" : "📱";
            String statusText = photo.hasFirebaseBackup ? "Cloud backed up" : "Local only";
            photoList[i] = cloudIcon + " " + photo.filename + "\n📅 " + photo.date + " • " + statusText;
        }
        
        String title = "📁 Injury Photos Gallery (" + photoList.length + " photo" + (photoList.length == 1 ? "" : "s") + ")";
        String message = "📱 = Local device storage\n☁️ = Cloud backed up & shareable\n\n💡 Tap any photo to view, share, or manage";
        
        Log.d("HomeFragment", "Showing photo gallery with " + photoList.length + " photos");
        
        new AlertDialog.Builder(requireContext())
                .setTitle(title)
                .setMessage(message)
                .setItems(photoList, (dialog, which) -> {
                    PhotoEntry selectedPhoto = allPhotos.get(which);
                    Log.d("HomeFragment", "Selected photo: " + selectedPhoto.filename);
                    loadAndDisplaySavedPhoto(selectedPhoto);
                })
                .setPositiveButton("📷 Take New Photo", (d, w) -> handleCameraAccess(CameraAccessSource.DIALOG_ACCESS))
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Show empty state when no photos are found
     */
    private void showEmptyPhotoState() {
        new AlertDialog.Builder(requireContext())
                .setTitle("📁 No Saved Photos Yet")
                .setMessage("🔍 No injury photos have been saved yet.\n\n" +
                          "💡 Start documenting your pet's health:\n" +
                          "• Take photos of any injuries\n" +
                          "• Track healing progress over time\n" +
                          "• Share with veterinarians for consultation\n" +
                          "• Automatic cloud backup available")
                .setPositiveButton("📷 Take Photo Now", (d, w) -> handleCameraAccess(CameraAccessSource.DIALOG_ACCESS))
                .setNeutralButton("🖼️ Choose from Gallery", (d, w) -> openInjuryGallery())
                .setNegativeButton("🔧 Debug Info", (d, w) -> showDebugInfo())
                .show();
    }
    
    /**
     * Helper class to manage photo entries with Firebase integration
     */
    private static class PhotoEntry {
        String filename;
        String timestamp;
        String date;
        String firebaseUrl;
        boolean hasFirebaseBackup;
        
        PhotoEntry(String filename, String timestamp, String date, String firebaseUrl, boolean hasFirebaseBackup) {
            this.filename = filename;
            this.timestamp = timestamp;
            this.date = date;
            this.firebaseUrl = firebaseUrl;
            this.hasFirebaseBackup = hasFirebaseBackup;
        }
    }
    
    private void loadAndDisplaySavedPhoto(PhotoEntry photoEntry) {
        Log.d("HomeFragment", "Loading photo: " + photoEntry.filename);
        
        try {
            Bitmap bitmap = null;
            
            // STEP 1: Try to load from local storage first (most reliable)
            try {
                java.io.FileInputStream fis = requireContext().openFileInput(photoEntry.filename);
                bitmap = BitmapFactory.decodeStream(fis);
                fis.close();
                Log.d("HomeFragment", "Successfully loaded photo from local storage: " + photoEntry.filename);
            } catch (Exception e) {
                Log.d("HomeFragment", "Local photo not found: " + photoEntry.filename + " - " + e.getMessage());
            }
            
            // STEP 2: If local load successful, show the photo
            if (bitmap != null) {
                showPhotoDialog(bitmap, photoEntry);
                return;
            }
            
            // STEP 3: If local failed but we have Firebase backup, try cloud
            if (photoEntry.hasFirebaseBackup && photoEntry.firebaseUrl != null) {
                Log.d("HomeFragment", "Local photo not available, trying Firebase: " + photoEntry.filename);
                loadPhotoFromFirebase(photoEntry);
                return;
            }
            
            // STEP 4: Photo not found anywhere - show error with recovery options
            Log.e("HomeFragment", "Photo not found anywhere: " + photoEntry.filename);
            showPhotoNotFoundDialog(photoEntry);
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error loading photo: " + photoEntry.filename, e);
            Toast.makeText(requireContext(), "❌ Error loading photo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show dialog when photo file is not found
     */
    private void showPhotoNotFoundDialog(PhotoEntry photoEntry) {
        new AlertDialog.Builder(requireContext())
                .setTitle("📷 Photo Not Found")
                .setMessage("The photo file could not be loaded:\n\n" +
                          "📁 File: " + photoEntry.filename + "\n" +
                          "📅 Date: " + photoEntry.date + "\n\n" +
                          "This may happen if:\n" +
                          "• The file was deleted or corrupted\n" +
                          "• App data was cleared\n" +
                          "• Storage issues occurred\n\n" +
                          "💡 Try taking a new photo to continue tracking.")
                .setPositiveButton("📷 Take New Photo", (d, w) -> handleCameraAccess(CameraAccessSource.DIALOG_ACCESS))
                .setNeutralButton("🔧 Debug Info", (d, w) -> showDebugInfo())
                .setNegativeButton("Close", null)
                .show();
    }
    
    /**
     * Load photo from Firebase Storage
     */
    private void loadPhotoFromFirebase(PhotoEntry photoEntry) {
        // Show loading message
        Toast.makeText(requireContext(), "☁️ Loading photo from cloud...", Toast.LENGTH_SHORT).show();
        
        try {
            // Use Glide or similar library to load from Firebase URL
            // For now, we'll show a placeholder and the Firebase URL
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("☁️ Cloud Photo")
                    .setMessage("Photo: " + photoEntry.filename + "\n\nThis photo is stored in the cloud.\nFirebase URL: " + photoEntry.firebaseUrl)
                    .setPositiveButton("Share Cloud Link", (d, w) -> shareFirebasePhotoUrl(photoEntry))
                    .setNeutralButton("View in Browser", (d, w) -> openFirebasePhotoInBrowser(photoEntry))
                    .setNegativeButton("Close", null)
                    .create();
            
            // Apply pink theme
            dialog.setOnShowListener(dialogInterface -> {
                try {
                    if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFE91E63);
                    }
                    if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFE91E63);
                    }
                    if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFFE91E63);
                    }
                } catch (Exception e) {
                    // Fallback if theming fails
                }
            });
            
            dialog.show();
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "❌ Error loading cloud photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    /**
     * Show photo dialog with enhanced Firebase integration options
     */
    private void showPhotoDialog(Bitmap bitmap, PhotoEntry photoEntry) {
        ImageView imageView = new ImageView(requireContext());
        imageView.setImageBitmap(bitmap);
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setAdjustViewBounds(true);
        imageView.setPadding(20, 20, 20, 20);
        
        String cloudStatus = photoEntry.hasFirebaseBackup ? "☁️ Cloud backed up" : "📱 Local only";
        String message = "File: " + photoEntry.filename + "\n" + cloudStatus;
        
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle("📷 Injury Photo")
                .setView(imageView)
                .setMessage(message)
                .setNegativeButton("Close", null);
        
        // Add different sharing options based on backup status
        if (photoEntry.hasFirebaseBackup) {
            builder.setPositiveButton("Share Local", (d, w) -> shareInjuryPhoto(bitmap))
                   .setNeutralButton("Share Cloud Link", (d, w) -> shareFirebasePhotoUrl(photoEntry));
        } else {
            builder.setPositiveButton("Share", (d, w) -> shareInjuryPhoto(bitmap))
                   .setNeutralButton("Backup to Cloud", (d, w) -> backupPhotoToFirebase(bitmap, photoEntry));
        }
        
        AlertDialog dialog = builder.create();
        
        // Apply pink theme
        dialog.setOnShowListener(dialogInterface -> {
            try {
                if (dialog.getButton(AlertDialog.BUTTON_POSITIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFE91E63);
                }
                if (dialog.getButton(AlertDialog.BUTTON_NEGATIVE) != null) {
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(0xFFE91E63);
                }
                if (dialog.getButton(AlertDialog.BUTTON_NEUTRAL) != null) {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(0xFFE91E63);
                }
            } catch (Exception e) {
                // Fallback if theming fails
            }
        });
        
        dialog.show();
    }
    
    /**
     * Share Firebase photo URL for veterinary consultation
     */
    private void shareFirebasePhotoUrl(PhotoEntry photoEntry) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Pet Injury Photo - " + photoEntry.filename);
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                "Pet injury photo for veterinary consultation:\n\n" +
                "📷 Photo: " + photoEntry.filename + "\n" +
                "📅 Date: " + photoEntry.date + "\n" +
                "☁️ Cloud Link: " + photoEntry.firebaseUrl + "\n\n" +
                "Generated by PetBuddy Injury Tracker 🩹");
            
            if (shareIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "Share cloud photo link"));
                Toast.makeText(requireContext(), "☁️ Sharing cloud photo link for veterinary consultation", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No apps available to share the link", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "❌ Error sharing cloud photo link: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    /**
     * Open Firebase photo in browser
     */
    private void openFirebasePhotoInBrowser(PhotoEntry photoEntry) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(photoEntry.firebaseUrl));
            if (browserIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(browserIntent);
                Toast.makeText(requireContext(), "🌐 Opening cloud photo in browser", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No browser available to view the photo", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "❌ Error opening photo in browser: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }
    
    /**
     * Backup existing local photo to Firebase
     */
    private void backupPhotoToFirebase(Bitmap bitmap, PhotoEntry photoEntry) {
        Toast.makeText(requireContext(), "☁️ Backing up photo to cloud...", Toast.LENGTH_SHORT).show();
        saveInjuryPhotoToFirebase(bitmap, photoEntry.filename, photoEntry.timestamp);
    }
    
    private void deleteSavedPhoto(String filename) {
        new AlertDialog.Builder(requireContext())
                .setTitle("🗑️ Delete Photo")
                .setMessage("Are you sure you want to delete this injury photo?\n\nFile: " + filename + "\n\nThis action cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {
                    try {
                        // Delete the file
                        requireContext().deleteFile(filename);
                        
                        // Remove from SharedPreferences
                        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
                        String savedPhotos = prefsHelper.getString("injury_photos", "");
                        String[] photoEntries = savedPhotos.split(";");
                        StringBuilder newPhotos = new StringBuilder();
                        
                        for (String entry : photoEntries) {
                            if (!entry.startsWith(filename + ":")) {
                                if (newPhotos.length() > 0) {
                                    newPhotos.append(";");
                                }
                                newPhotos.append(entry);
                            }
                        }
                        
                        prefsHelper.edit().putString("injury_photos", newPhotos.toString()).apply();
                        Toast.makeText(requireContext(), "🗑️ Photo deleted successfully", Toast.LENGTH_SHORT).show();
                        
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "❌ Error deleting photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        e.printStackTrace();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareInjuryPhoto(Bitmap bitmap) {
        try {
            // Create a temporary file in cache directory for sharing
            String timestamp = String.valueOf(System.currentTimeMillis());
            String filename = "injury_photo_" + timestamp + ".jpg";
            
            // Save to cache directory for sharing
            java.io.File cacheDir = requireContext().getCacheDir();
            java.io.File imageFile = new java.io.File(cacheDir, filename);
            
            java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            
            // Create URI for the file
            Uri imageUri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                requireContext().getPackageName() + ".fileprovider",
                imageFile
            );
            
            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/jpeg");
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Pet Injury Photo - " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()));
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Pet injury photo captured on " + new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(new java.util.Date()) + " for veterinary consultation.\n\nGenerated by PetBuddy Injury Tracker 📷🩹");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (shareIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(Intent.createChooser(shareIntent, "Share injury photo with veterinarian"));
                Toast.makeText(requireContext(), "📤 Sharing injury photo for veterinary consultation", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(requireContext(), "No apps available to share the photo", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "❌ Error sharing injury photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void showCommunity() {

        new AlertDialog.Builder(requireContext())
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
                            Intent intent = new Intent(requireContext(), CommunityActivity.class);
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

    private void showManualMemberRegistration() {
        // Create form layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Full Name *");
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(requireContext());
        emailInput.setHint("Email Address *");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        layout.addView(emailInput);

        // Phone input
        EditText phoneInput = new EditText(requireContext());
        phoneInput.setHint("Phone Number *");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        layout.addView(phoneInput);

        // Info text
        TextView infoText = new TextView(requireContext());
        infoText.setText("\n* All fields are required\n\nThis will add a new member to the community.");
        infoText.setTextSize(12);
        infoText.setTextColor(0xFF666666);
        layout.addView(infoText);

        // Show form dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Add New Community Member")
                .setView(layout)
                .setPositiveButton("Add Member", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    // Validate inputs
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        showManualMemberRegistration(); // Show form again
                        return;
                    }

                    if (email.isEmpty()) {
                        Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show();
                        showManualMemberRegistration(); // Show form again
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
                        showManualMemberRegistration(); // Show form again
                        return;
                    }

                    if (phone.isEmpty()) {
                        Toast.makeText(requireContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
                        showManualMemberRegistration(); // Show form again
                        return;
                    }

                    // Save the new member
                    saveManualMember(name, email, phone);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveManualMember(String name, String email, String phone) {
        // Show progress dialog
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                .setTitle("Adding Member...")
                .setMessage("Saving member to community...\n\nName: " + name + "\nEmail: " + email + "\nPhone: " + phone)
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Get Firebase reference
        FirebaseDatabase database = FirebaseDatabase.getInstance(
                "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
        );
        DatabaseReference communityRef = database.getReference("communityMembers");

        // Generate unique ID for this member (using timestamp + random)
        String memberId = "member_" + System.currentTimeMillis();

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
                    progressDialog.dismiss();

                    new AlertDialog.Builder(requireContext())
                            .setTitle("✅ Success!")
                            .setMessage("Member added successfully!\n\n" +
                                    "Name: " + name + "\n" +
                                    "Email: " + email + "\n" +
                                    "Phone: " + phone + "\n\n" +
                                    "Would you like to add another member?")
                            .setPositiveButton("Add Another", (dialog, which) -> showManualMemberRegistration())
                            .setNeutralButton("View Members", (dialog, which) -> {
                                Intent intent = new Intent(requireContext(), CommunityActivity.class);
                                startActivity(intent);
                            })
                            .setNegativeButton("Done", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(requireContext())
                            .setTitle("❌ Failed")
                            .setMessage("Failed to add member!\n\n" +
                                    "Error: " + e.getMessage() + "\n\n" +
                                    "Please check:\n" +
                                    "1. Internet connection\n" +
                                    "2. Firebase Rules\n" +
                                    "3. Try again")
                            .setPositiveButton("Retry", (dialog, which) -> saveManualMember(name, email, phone))
                            .setNegativeButton("Cancel", null)
                            .show();
                    e.printStackTrace();
                });
    }

    private void addTestMembers() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Add Test Members")
                .setMessage("This will add 5 sample members to the community database for testing.\n\nContinue?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Show progress
                    AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
                            .setTitle("Adding Members...")
                            .setMessage("Please wait...")
                            .setCancelable(false)
                            .create();
                    progressDialog.show();

                    // Get Firebase reference
                    FirebaseDatabase database = FirebaseDatabase.getInstance(
                            "https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/"
                    );
                    DatabaseReference communityRef = database.getReference("communityMembers");

                    // Sample members data
                    String[][] members = {
                            {"John Smith", "john.smith@email.com", "+1234567890"},
                            {"Sarah Johnson", "sarah.j@email.com", "+1234567891"},
                            {"Mike Brown", "mike.brown@email.com", "+1234567892"},
                            {"Emily Davis", "emily.d@email.com", "+1234567893"},
                            {"David Wilson", "david.w@email.com", "+1234567894"}
                    };

                    int[] addedCount = {0};
                    
                    for (int i = 0; i < members.length; i++) {
                        String userId = "testUser" + (i + 1);
                        CommunityModel member = new CommunityModel(
                                members[i][0],
                                members[i][1],
                                members[i][2],
                                System.currentTimeMillis() - (i * 86400000) // Different join dates
                        );

                        communityRef.child(userId).setValue(member)
                                .addOnSuccessListener(aVoid -> {
                                    addedCount[0]++;
                                    if (addedCount[0] == members.length) {
                                        progressDialog.dismiss();
                                        new AlertDialog.Builder(requireContext())
                                                .setTitle("✅ Success!")
                                                .setMessage(members.length + " test members added successfully!\n\n" +
                                                        "Click 'View All Members' to see them.")
                                                .setPositiveButton("OK", null)
                                                .show();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    new AlertDialog.Builder(requireContext())
                                            .setTitle("❌ Error")
                                            .setMessage("Failed to add members: " + e.getMessage())
                                            .setPositiveButton("OK", null)
                                            .show();
                                });
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void showCommunityRegistrationForm() {
        FirebaseAuth auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Login Required")
                    .setMessage("You must be logged in to join the community")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }

        // Get current user data
        SharedPreferencesHelper prefs = new SharedPreferencesHelper(requireContext());
        String currentName = prefs.getUserName();
        String currentEmail = auth.getCurrentUser().getEmail();
        String currentPhone = prefs.getString("user_phone", "");

        // Create form layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        // Name input
        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Full Name *");
        nameInput.setText(currentName != null ? currentName : "");
        layout.addView(nameInput);

        // Email input
        EditText emailInput = new EditText(requireContext());
        emailInput.setHint("Email Address *");
        emailInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailInput.setText(currentEmail != null ? currentEmail : "");
        layout.addView(emailInput);

        // Phone input
        EditText phoneInput = new EditText(requireContext());
        phoneInput.setHint("Phone Number *");
        phoneInput.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        phoneInput.setText(currentPhone);
        layout.addView(phoneInput);

        // Info text
        TextView infoText = new TextView(requireContext());
        infoText.setText("\n* Required fields\n\nYour information will be visible to other community members.");
        infoText.setTextSize(12);
        infoText.setTextColor(0xFF666666);
        layout.addView(infoText);

        // Show form dialog
        new AlertDialog.Builder(requireContext())
                .setTitle("Community Registration")
                .setView(layout)
                .setPositiveButton("Save & Join", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String email = emailInput.getText().toString().trim();
                    String phone = phoneInput.getText().toString().trim();

                    // Validate inputs
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (email.isEmpty()) {
                        Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(requireContext(), "Invalid email format", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (phone.isEmpty()) {
                        Toast.makeText(requireContext(), "Phone number is required", Toast.LENGTH_SHORT).show();
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
        AlertDialog progressDialog = new AlertDialog.Builder(requireContext())
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
                    SharedPreferencesHelper prefs = new SharedPreferencesHelper(requireContext());
                    prefs.setUserName(name);
                    prefs.setUserEmail(email);
                    prefs.edit().putString("user_phone", phone).apply();
                    
                    // Update UI
                    txtHomeUserName.setText(name);
                    txtHomeUserEmail.setText(email);

                    new AlertDialog.Builder(requireContext())
                            .setTitle("✅ Success!")
                            .setMessage("You have successfully joined the community!\n\n" +
                                    "Your profile has been saved.\n\n" +
                                    "Click 'View Members' to see all community members.")
                            .setPositiveButton("OK", null)
                            .show();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    new AlertDialog.Builder(requireContext())
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

    private void showMessages() {
        // Open AI Chat Activity
        Intent intent = new Intent(requireContext(), AIChatActivity.class);
        startActivity(intent);
    }


    private void showEvents() {
        Intent intent = new Intent(getContext(), EventsActivity.class);
        startActivity(intent);
    }

    // Reviews & Feedback methods removed due to build issues

    private void bookService(String service) {
        Toast.makeText(requireContext(),
                service + " booked", Toast.LENGTH_SHORT).show();
    }

    private void navigateToShop() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new ShopFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToMyPets() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new MyPetsFragment())
                .addToBackStack(null)
                .commit();
    }

    private void navigateToBlogs() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new BlogsFragment())
                .addToBackStack(null)
                .commit();
    }

    /**
     * Legacy reminder method - kept for backward compatibility
     * For new reminders, use the enhanced system in NotificationFragment
     */
    private void scheduleReminder(String title, String message, int hour, int minute) {
        Log.d("HomeFragment", "Legacy reminder method called - consider using enhanced notification system");
        
        AlarmManager alarmManager = (AlarmManager) requireContext().getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(requireContext(), ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);

        // If time already passed, schedule for next day
        if (calendar.before(Calendar.getInstance())) {
            calendar.add(Calendar.DATE, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                Toast.makeText(requireContext(),
                        "Exact alarm permission not granted",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }

    private void performLogout() {
        // Sign out from Firebase
        FirebaseAuth.getInstance().signOut();
        
        // Clear SharedPreferences
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
        prefsHelper.logout();
        
        // Show success message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to LoginActivity
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }

    private void showEditProfileDialog() {
        SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
        
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        EditText nameInput = new EditText(requireContext());
        nameInput.setHint("Your Name");
        nameInput.setText(prefsHelper.getUserName());
        layout.addView(nameInput);

        EditText emailInput = new EditText(requireContext());
        emailInput.setHint("Email");
        emailInput.setText(prefsHelper.getUserEmail());
        layout.addView(emailInput);

        EditText phoneInput = new EditText(requireContext());
        phoneInput.setHint("Phone Number");
        phoneInput.setText(prefsHelper.getUserPhone());
        layout.addView(phoneInput);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Profile")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    String newEmail = emailInput.getText().toString().trim();
                    String newPhone = phoneInput.getText().toString().trim();
                    
                    if (!newName.isEmpty()) {
                        prefsHelper.setUserName(newName);
                        if (txtHomeUserName != null) {
                            txtHomeUserName.setText(newName);
                        }
                    }
                    
                    if (!newEmail.isEmpty()) {
                        prefsHelper.setUserEmail(newEmail);
                        if (txtHomeUserEmail != null) {
                            txtHomeUserEmail.setText(newEmail);
                        }
                    }
                    
                    if (!newPhone.isEmpty()) {
                        prefsHelper.setUserPhone(newPhone);
                    }
                    
                    Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTrainingVideosDialog() {
        String[] videoOptions = {
                "🎓 Cross Legs Training",
                "⬇️ Down Command Training",
                "🤝 Paw Shake Training",
                "📱 All Training Videos"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("🎓 Training Videos")
                .setItems(videoOptions, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            playSpecificVideo("Cross Legs Training", R.raw.dog_training_cross_legs);
                            break;
                        case 1:
                            playSpecificVideo("Down Command Training", R.raw.dog_training_down);
                            break;
                        case 2:
                            playSpecificVideo("Paw Shake Training", R.raw.dog_training_paw_shake);
                            break;
                        case 3:
                            try {
                                Intent intent = new Intent(requireContext(), TrainingVideosActivity.class);
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(requireContext(), "Opening training videos...", Toast.LENGTH_SHORT).show();
                            }
                            break;
                    }
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void playSpecificVideo(String title, int videoResource) {
        try {
            // Directly open in-app video player
            android.content.Intent intent = new android.content.Intent(requireContext(), VideoPlayerActivity.class);
            intent.putExtra("video_title", title);
            intent.putExtra("video_resource", videoResource);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error playing " + title + ": " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void openVideoInSystemPlayer(String title, int videoResource) {
        try {
            android.net.Uri uri = android.net.Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + videoResource);
            
            // Try multiple approaches to open the video
            boolean videoOpened = false;
            
            // Approach 1: Try with specific video/mp4 MIME type
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/mp4");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                    Toast.makeText(requireContext(), "Opening " + title + "...", Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                    videoOpened = true;
                }
            } catch (Exception e) {
                android.util.Log.w("HomeFragment", "Failed to open with video/mp4 MIME type", e);
            }
            
            // Approach 2: Try with generic video/* MIME type
            if (!videoOpened) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        Toast.makeText(requireContext(), "Opening " + title + "...", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        videoOpened = true;
                    }
                } catch (Exception e) {
                    android.util.Log.w("HomeFragment", "Failed to open with video/* MIME type", e);
                }
            }
            
            // Approach 3: Try with ACTION_SEND to share the video
            if (!videoOpened) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("video/mp4");
                    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    android.content.Intent chooser = android.content.Intent.createChooser(intent, "Open " + title + " with:");
                    if (chooser.resolveActivity(requireContext().getPackageManager()) != null) {
                        Toast.makeText(requireContext(), "Choose app to play " + title, Toast.LENGTH_SHORT).show();
                        startActivity(chooser);
                        videoOpened = true;
                    }
                } catch (Exception e) {
                    android.util.Log.w("HomeFragment", "Failed to open with ACTION_SEND", e);
                }
            }
            
            // If all approaches failed
            if (!videoOpened) {
                showVideoPlayerInstallDialog(title);
            }
            
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showVideoPlayerInstallDialog(String title) {
        new AlertDialog.Builder(requireContext())
            .setTitle("Choose Video Player")
            .setMessage("How would you like to play " + title + "?")
            .setPositiveButton("In-App Player", (dialog, which) -> {
                try {
                    android.content.Intent intent = new android.content.Intent(requireContext(), VideoPlayerActivity.class);
                    intent.putExtra("video_title", title);
                    // We need to get the video resource ID - let's add a method for this
                    int videoResource = getVideoResourceByTitle(title);
                    if (videoResource != -1) {
                        intent.putExtra("video_resource", videoResource);
                        startActivity(intent);
                    } else {
                        Toast.makeText(requireContext(), "Error: Video not found", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Error opening in-app player: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            })
            .setNeutralButton("Install Player", (dialog, which) -> {
                showInstallPlayerDialog(title);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showInstallPlayerDialog(String title) {
        new AlertDialog.Builder(requireContext())
            .setTitle("No Video Player Found")
            .setMessage("To play " + title + ", please install a video player app like:\n\n" +
                       "• VLC Media Player\n" +
                       "• MX Player\n" +
                       "• Google Photos\n" +
                       "• Any other video player\n\n" +
                       "You can download these from Google Play Store.")
            .setPositiveButton("Open Play Store", (dialog, which) -> {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse("market://search?q=video%20player"));
                    if (intent.resolveActivity(requireContext().getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        // Fallback to web browser
                        intent.setData(android.net.Uri.parse("https://play.google.com/store/search?q=video%20player"));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(requireContext(), "Please install a video player from Play Store", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private int getVideoResourceByTitle(String title) {
        switch (title) {
            case "Cross Legs Training":
                return R.raw.dog_training_cross_legs;
            case "Down Command Training":
                return R.raw.dog_training_down;
            case "Paw Shake Training":
                return R.raw.dog_training_paw_shake;
            default:
                return -1;
        }
    }
    
    private void showVideoFileInfo(String title, int videoResource) {
        try {
            java.io.InputStream inputStream = requireContext().getResources().openRawResource(videoResource);
            int available = inputStream.available();
            inputStream.close();
            
            android.net.Uri uri = android.net.Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + videoResource);
            
            String info = "Video: " + title + "\n\n" +
                         "Size: " + (available / 1024 / 1024) + " MB\n" +
                         "URI: " + uri.toString() + "\n\n" +
                         "Try copying this URI to a video player app.";
            
            new AlertDialog.Builder(requireContext())
                .setTitle("Video Information")
                .setMessage(info)
                .setPositiveButton("Copy URI", (dialog, which) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Video URI", uri.toString());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(requireContext(), "URI copied to clipboard", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Close", null)
                .show();
                
        } catch (Exception e) {
            Toast.makeText(requireContext(), "Error getting video info: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openTrainingFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private void showCatFoodPosters() {
        // Create a custom dialog layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);
        
        // Title
        TextView title = new TextView(requireContext());
        title.setText("🐱 Cat Food Safety Guide");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        title.setTextColor(0xFF333333);
        layout.addView(title);
        
        // Create a scroll view for the food posters
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        LinearLayout posterContainer = new LinearLayout(requireContext());
        posterContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Add cat food posters
        addFoodPoster(posterContainer, "Safe & Unsafe Foods for Cats", "Essential guide to what cats can and cannot eat", R.drawable.cat_food_safety_guide);
        
        scrollView.addView(posterContainer);
        layout.addView(scrollView);
        
        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setPositiveButton("Close", null)
                .create();
        
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
    
    private void addFoodPoster(LinearLayout container, String name, String description, int imageResource) {
        // Create card-like layout for each food poster
        androidx.cardview.widget.CardView cardView = new androidx.cardview.widget.CardView(requireContext());
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        cardView.setLayoutParams(cardParams);
        cardView.setCardElevation(8);
        cardView.setRadius(12);
        cardView.setCardBackgroundColor(0xFFFFFFFF);
        
        // Inner layout
        LinearLayout innerLayout = new LinearLayout(requireContext());
        innerLayout.setOrientation(LinearLayout.HORIZONTAL);
        innerLayout.setPadding(16, 16, 16, 16);
        
        // Food image
        ImageView imageView = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(120, 120);
        imageParams.setMargins(0, 0, 16, 0);
        imageView.setLayoutParams(imageParams);
        imageView.setImageResource(imageResource);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        try {
            imageView.setBackground(requireContext().getDrawable(R.drawable.circle_bg));
        } catch (Exception e) {
            // Fallback if circle_bg drawable not found
            imageView.setBackgroundColor(0xFFE0E0E0);
        }
        
        // Make image clickable - opens full screen view
        imageView.setOnClickListener(v -> showFullScreenPoster(imageResource, name));
        
        innerLayout.addView(imageView);
        
        // Text container
        LinearLayout textContainer = new LinearLayout(requireContext());
        textContainer.setOrientation(LinearLayout.VERTICAL);
        textContainer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        
        // Food name
        TextView nameText = new TextView(requireContext());
        nameText.setText(name);
        nameText.setTextSize(16);
        nameText.setTypeface(null, android.graphics.Typeface.BOLD);
        nameText.setTextColor(0xFF333333);
        nameText.setPadding(0, 0, 0, 8);
        textContainer.addView(nameText);
        
        // Food description
        TextView descText = new TextView(requireContext());
        descText.setText(description);
        descText.setTextSize(14);
        descText.setTextColor(0xFF666666);
        textContainer.addView(descText);
        
        innerLayout.addView(textContainer);
        cardView.addView(innerLayout);
        container.addView(cardView);
    }

    private void showFullScreenPoster(int imageResource, String title) {
        // Create full screen dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        
        // Create layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(0xFFF6C1CC); // Pink theme background
        layout.setPadding(20, 20, 20, 20);
        
        // Title
        TextView titleText = new TextView(requireContext());
        titleText.setText(title);
        titleText.setTextSize(18);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);
        titleText.setTextColor(0xFF333333); // Dark text for better contrast on pink
        titleText.setGravity(android.view.Gravity.CENTER);
        titleText.setPadding(0, 0, 0, 20);
        layout.addView(titleText);
        
        // Full screen image
        ImageView fullScreenImage = new ImageView(requireContext());
        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.MATCH_PARENT
        );
        imageParams.weight = 1;
        fullScreenImage.setLayoutParams(imageParams);
        fullScreenImage.setImageResource(imageResource);
        fullScreenImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
        fullScreenImage.setAdjustViewBounds(true);
        layout.addView(fullScreenImage);
        
        // Close button
        TextView closeButton = new TextView(requireContext());
        closeButton.setText("✕ Close");
        closeButton.setTextSize(16);
        closeButton.setTextColor(0xFFFFFFFF); // White text
        closeButton.setGravity(android.view.Gravity.CENTER);
        closeButton.setPadding(20, 20, 20, 20);
        closeButton.setBackgroundColor(0xFFE8A7B4); // Darker pink for button
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        closeParams.setMargins(0, 20, 0, 0);
        closeButton.setLayoutParams(closeParams);
        layout.addView(closeButton);
        
        AlertDialog dialog = builder.setView(layout).create();
        
        // Close button click
        closeButton.setOnClickListener(v -> dialog.dismiss());
        
        // Click anywhere on image to close
        fullScreenImage.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showDogFoodPosters() {
        // Create a custom dialog layout
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 30, 40, 30);
        
        // Title
        TextView title = new TextView(requireContext());
        title.setText("🐶 Dog Diet Safety Guide");
        title.setTextSize(20);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.CENTER);
        title.setPadding(0, 0, 0, 20);
        title.setTextColor(0xFF333333);
        layout.addView(title);
        
        // Create a scroll view for the food posters
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        LinearLayout posterContainer = new LinearLayout(requireContext());
        posterContainer.setOrientation(LinearLayout.VERTICAL);
        
        // Add dog food safety guide poster
        addFoodPoster(posterContainer, "Safe & Unsafe Foods for Dogs", "Essential guide to what dogs can and cannot eat", R.drawable.dog_food);
        
        scrollView.addView(posterContainer);
        layout.addView(scrollView);
        
        // Create and show dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(layout)
                .setPositiveButton("Close", null)
                .create();
        
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.show();
    }
    
    /**
     * Add a test photo for debugging purposes
     */
    private void addTestPhoto() {
        try {
            // Create a simple test bitmap (red square)
            Bitmap testBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            testBitmap.eraseColor(android.graphics.Color.RED);
            
            // Add some text to make it identifiable
            android.graphics.Canvas canvas = new android.graphics.Canvas(testBitmap);
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setColor(android.graphics.Color.WHITE);
            paint.setTextSize(20);
            paint.setAntiAlias(true);
            canvas.drawText("TEST", 70, 100, paint);
            canvas.drawText("PHOTO", 60, 130, paint);
            
            // Save the test photo
            saveInjuryPhoto(testBitmap);
            
            Toast.makeText(requireContext(), "🔧 Test photo added for debugging", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error creating test photo", e);
            Toast.makeText(requireContext(), "❌ Error creating test photo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Show debug information about saved photos
     */
    private void showDebugInfo() {
        try {
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
            String savedPhotos = prefsHelper.getString("injury_photos", "");
            String firebasePhotos = prefsHelper.getString("injury_photos_firebase", "");
            
            // Also check internal storage files
            String[] fileList = requireContext().fileList();
            StringBuilder injuryFiles = new StringBuilder();
            for (String file : fileList) {
                if (file.startsWith("injury_")) {
                    injuryFiles.append("• ").append(file).append("\n");
                }
            }
            
            String debugInfo = "=== SAVED PHOTOS DEBUG ===\n\n" +
                             "SharedPreferences 'injury_photos':\n" + 
                             (savedPhotos.isEmpty() ? "(empty)" : savedPhotos) + "\n\n" +
                             "SharedPreferences 'injury_photos_firebase':\n" + 
                             (firebasePhotos.isEmpty() ? "(empty)" : firebasePhotos) + "\n\n" +
                             "Internal Storage Files:\n" + 
                             (injuryFiles.length() == 0 ? "(no injury files found)" : injuryFiles.toString());
            
            new AlertDialog.Builder(requireContext())
                    .setTitle("🔧 Debug Information")
                    .setMessage(debugInfo)
                    .setPositiveButton("Add Test Photo", (d, w) -> addTestPhoto())
                    .setNeutralButton("Clear All Data", (d, w) -> clearAllPhotoData())
                    .setNegativeButton("Close", null)
                    .show();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error showing debug info", e);
            Toast.makeText(requireContext(), "❌ Error showing debug info: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Clear all photo data for debugging
     */
    private void clearAllPhotoData() {
        try {
            SharedPreferencesHelper prefsHelper = new SharedPreferencesHelper(requireContext());
            prefsHelper.edit().remove("injury_photos").apply();
            prefsHelper.edit().remove("injury_photos_firebase").apply();
            
            // Also delete injury files from internal storage
            String[] fileList = requireContext().fileList();
            int deletedCount = 0;
            for (String file : fileList) {
                if (file.startsWith("injury_")) {
                    if (requireContext().deleteFile(file)) {
                        deletedCount++;
                    }
                }
            }
            
            Toast.makeText(requireContext(), "🗑️ Cleared all photo data. Deleted " + deletedCount + " files.", Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e("HomeFragment", "Error clearing photo data", e);
            Toast.makeText(requireContext(), "❌ Error clearing data: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}


