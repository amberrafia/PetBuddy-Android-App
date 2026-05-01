package com.example.petbuddy;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ShopFragment extends Fragment {

    private DatabaseHelper db;
    private LinearLayout productContainer;
    private String selectedCategory = "DOG";

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_shop, container, false);

        db = new DatabaseHelper(requireContext());
        productContainer = view.findViewById(R.id.productContainer);

        if (productContainer == null) {
            Toast.makeText(getContext(), "productContainer missing", Toast.LENGTH_LONG).show();
            return view;
        }

        View btnDog = view.findViewById(R.id.btnDogFood);
        View btnCat = view.findViewById(R.id.btnCatFood);
        View btnDry = view.findViewById(R.id.btnDry);
        View btnWet = view.findViewById(R.id.btnWet);
        View btnAdd = view.findViewById(R.id.btnAddProduct);

        // 🐶 DOG
        if (btnDog != null) btnDog.setOnClickListener(v -> loadDogDryFood());

        // 🐱 CAT → NEXT PAGE
        if (btnCat != null) {
            btnCat.setOnClickListener(v ->
                    requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, new CatFoodFragment())
                            .addToBackStack(null)
                            .commit()
            );
        }

        if (btnDry != null) btnDry.setOnClickListener(v -> loadDogDryFood());
        if (btnWet != null) btnWet.setOnClickListener(v -> loadDogWetFood());

        if (btnAdd != null) btnAdd.setOnClickListener(v -> showAddProductDialog());

        // Default
        loadDogDryFood();
        return view;
    }

    // ================= DOG DRY FOOD =================
    private void loadDogDryFood() {
        productContainer.removeAllViews();

        addFoodItem(
                "Pedigree Adult Dry Food",
                "Complete nutrition • Strong bones",
                "₹299 – ₹599",
                R.drawable.dog_food
        );

        addFoodItem(
                "Royal Canin Adult",
                "Digestive & immune support",
                "₹499 – ₹899",
                R.drawable.royal_canin
        );

        addFoodItem(
                "Drools Focus Adult",
                "High protein • Omega 3 & 6",
                "₹349 – ₹699",
                R.drawable.drools
        );

        // ✅ VIDEO AFTER DROOLS
        addDogDryFoodVideoSection();
    }

    // ================= DOG WET FOOD =================
    private void loadDogWetFood() {
        productContainer.removeAllViews();

        addFoodItem(
                "Pedigree Wet Dog Food",
                "Soft chunks • Easy digestion",
                "₹40 – ₹90",
                R.drawable.pedigree_wet
        );

        addFoodItem(
                "Royal Canin Wet Food",
                "Digestive & immune support",
                "₹120 – ₹180",
                R.drawable.royal_canin_wet
        );

        addFoodItem(
                "Drools Wet Dog Food",
                "High protein • Moist gravy",
                "₹70 – ₹140",
                R.drawable.drools_wet
        );
    }

    // ================= FOOD CARD =================
    private void addFoodItem(String name, String desc, String price, int imageRes) {

        View item = LayoutInflater.from(getContext())
                .inflate(R.layout.item_food_with_image, productContainer, false);

        ((TextView) item.findViewById(R.id.txtName)).setText(name);
        ((TextView) item.findViewById(R.id.txtDesc)).setText(desc);
        ((TextView) item.findViewById(R.id.txtPrice)).setText(price);
        ((ImageView) item.findViewById(R.id.imgFood)).setImageResource(imageRes);

        item.setOnClickListener(v ->
                showFoodDetails(name, desc, price, imageRes)
        );

        productContainer.addView(item);
    }

    // ================= VIDEO SECTION (THUMBNAIL + PLAY) =================
    private void addDogDryFoodVideoSection() {

        View videoLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_video, productContainer, false);

        ImageView playIcon = videoLayout.findViewById(R.id.imgPlay);
        ImageView thumbnail = videoLayout.findViewById(R.id.imgThumbnail);

        Uri videoUri = Uri.parse(
                "android.resource://" +
                        requireContext().getPackageName() +
                        "/" + R.raw.dog_dry_food
        );

        // ▶ CLICK → FULLSCREEN VIDEO
        playIcon.setOnClickListener(v -> showFullscreenVideo(videoUri));

        productContainer.addView(videoLayout);
    }

    // ================= FOOD DETAILS =================
    private void showFoodDetails(String name, String desc, String price, int imageRes) {

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.item_food_with_image, null);

        ((ImageView) dialogView.findViewById(R.id.imgFood)).setImageResource(imageRes);
        ((TextView) dialogView.findViewById(R.id.txtName)).setText(name);
        ((TextView) dialogView.findViewById(R.id.txtDesc)).setText(desc);
        ((TextView) dialogView.findViewById(R.id.txtPrice)).setText(price);

        new AlertDialog.Builder(getContext())
                .setTitle("🐶 Product Details")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .show();
    }

    // ================= ADD PRODUCT =================
    private void showAddProductDialog() {

        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_product, null);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etPrice = dialogView.findViewById(R.id.etPrice);

        new AlertDialog.Builder(getContext())
                .setTitle("Add Product")
                .setView(dialogView)
                .setPositiveButton("ADD", (d, w) -> {

                    String name = etName.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();

                    if (name.isEmpty() || price.isEmpty()) {
                        Toast.makeText(getContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.insertProduct(name, price, selectedCategory, "GENERAL");
                    loadDogDryFood();
                })
                .setNegativeButton("CANCEL", null)
                .show();
    }

    // ================= FULLSCREEN VIDEO =================
    private void showFullscreenVideo(Uri videoUri) {

        View fullView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_video_fullscreen, null);

        VideoView fullVideo = fullView.findViewById(R.id.fullVideoView);

        MediaController mediaController = new MediaController(getContext());
        mediaController.setAnchorView(fullVideo);
        fullVideo.setMediaController(mediaController);

        fullVideo.setVideoURI(videoUri);
        fullVideo.start();

        AlertDialog dialog = new AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        ).setView(fullView).create();

        dialog.show();
    }
}
