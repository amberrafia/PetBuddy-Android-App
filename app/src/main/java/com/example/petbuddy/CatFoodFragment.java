package com.example.petbuddy;

import android.app.AlertDialog;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class CatFoodFragment extends Fragment {

    private DatabaseHelper db;
    private LinearLayout productContainer;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cat_food, container, false);

        db = new DatabaseHelper(requireContext());
        productContainer = view.findViewById(R.id.productContainer);

        // Buttons
        View btnWetCat = view.findViewById(R.id.btnWetCat);
        View btnDryCat = view.findViewById(R.id.btnDryCat);

        if (btnDryCat != null) {
            btnDryCat.setOnClickListener(v -> loadDryCatFood());
        }

        if (btnWetCat != null) {
            btnWetCat.setOnClickListener(v -> loadWetCatFood());
        }

        // Default
        loadDryCatFood();

        return view;
    }

    // ================= DRY CAT FOOD =================
    private void loadDryCatFood() {

        productContainer.removeAllViews();

        addDryCatFood(
                "Whiskas Adult Dry Food",
                "Healthy coat • Strong bones",
                R.drawable.cat_whiskas
        );

        addDryCatFood(
                "Royal Canin Dry Cat Food",
                "Digestive health • Hairball control",
                R.drawable.cat_royal_canin
        );

        addDryCatFood(
                "Me-O Dry Cat Food",
                "High protein • Taurine for eyes",
                R.drawable.cat_meo
        );

        addDryCatFood(
                "Purepet Dry Cat Food",
                "Balanced nutrition • Affordable",
                R.drawable.cat_purepet
        );

        // ✅ Dry food video only
        addCatDryFoodVideoTips();
    }

    // ================= WET CAT FOOD =================
    private void loadWetCatFood() {

        productContainer.removeAllViews();

        addWetCatFood(
                "Whiskas Wet Cat Food",
                "Soft chunks in gravy • Easy digestion",
                R.drawable.cat_whiskas
        );

        addWetCatFood(
                "Royal Canin Wet Cat Food",
                "Digestive care • Immune support",
                R.drawable.cat_royal_canin
        );

        addWetCatFood(
                "Me-O Wet Cat Food",
                "High moisture • Taurine rich",
                R.drawable.cat_meo
        );

        // ✅ Wet food video only
        addCatWetFoodVideo();
    }

    // ================= DRY CAT FOOD ITEM =================
    private void addDryCatFood(String name, String desc, int imageRes) {

        View item = LayoutInflater.from(getContext())
                .inflate(R.layout.item_cat_food, productContainer, false);

        ImageView img = item.findViewById(R.id.imgCatFood);
        TextView txtName = item.findViewById(R.id.txtCatFoodName);
        TextView txtDesc = item.findViewById(R.id.txtCatFoodDesc);

        img.setImageResource(imageRes);
        txtName.setText(name);
        txtDesc.setText(desc);

        item.setOnClickListener(v -> showImageFullScreen(imageRes));

        productContainer.addView(item);
    }

    // ================= WET CAT FOOD ITEM =================
    private void addWetCatFood(String name, String desc, int imageRes) {

        View item = LayoutInflater.from(getContext())
                .inflate(R.layout.item_cat_food, productContainer, false);

        ImageView img = item.findViewById(R.id.imgCatFood);
        TextView txtName = item.findViewById(R.id.txtCatFoodName);
        TextView txtDesc = item.findViewById(R.id.txtCatFoodDesc);

        img.setImageResource(imageRes);
        txtName.setText(name);
        txtDesc.setText(desc);

        item.setOnClickListener(v -> showImageFullScreen(imageRes));

        productContainer.addView(item);
    }

    // ================= DRY CAT FOOD VIDEO =================
    private void addCatDryFoodVideoTips() {

        View videoLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.item_cat_dry_food_video, productContainer, false);

        ImageView thumbnail = videoLayout.findViewById(R.id.imgThumbnail);
        ImageView playBtn = videoLayout.findViewById(R.id.imgPlay);
        VideoView videoView = videoLayout.findViewById(R.id.videoView);

        Uri videoUri = Uri.parse(
                "android.resource://" +
                        requireContext().getPackageName() +
                        "/" + R.raw.cat_dry_food
        );

        playBtn.setOnClickListener(v -> {
            thumbnail.setVisibility(View.GONE);
            playBtn.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);
            videoView.setVideoURI(videoUri);
            videoView.start();
        });
        videoView.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                playBtn.setVisibility(View.VISIBLE);
                playBtn.setImageResource(android.R.drawable.ic_media_play);
            } else {
                videoView.start();
                playBtn.setVisibility(View.GONE);
            }
        });

        productContainer.addView(videoLayout);
    }

    // ================= WET CAT FOOD VIDEO =================
    private void addCatWetFoodVideo() {

        View videoLayout = LayoutInflater.from(getContext())
                .inflate(R.layout.item_cat_wet_food_video, productContainer, false);

        ImageView thumbnail = videoLayout.findViewById(R.id.imgThumbnail);
        ImageView playBtn = videoLayout.findViewById(R.id.imgPlay);
        VideoView videoView = videoLayout.findViewById(R.id.videoView);

        Uri videoUri = Uri.parse(
                "android.resource://" +
                        requireContext().getPackageName() +
                        "/" + R.raw.cat_wet_food
        );

        playBtn.setOnClickListener(v -> {

            if (videoView.isPlaying()) {
                videoView.pause();
                playBtn.setImageResource(android.R.drawable.ic_media_play);
                playBtn.setVisibility(View.VISIBLE);
            } else {
                // Open fullscreen
                playVideoFullscreen(videoUri);
            }
        });

        productContainer.addView(videoLayout);
    }

    // ================= LOAD CAT FOOD FROM DATABASE =================
    private void loadCatFoodFromDB() {

        Cursor cursor = db.getProductsByCategory("CAT");
        if (cursor == null || cursor.getCount() == 0) return;

        while (cursor.moveToNext()) {
            TextView tv = new TextView(getContext());
            tv.setText(cursor.getString(0) + " ₹" + cursor.getString(1));
            tv.setTextSize(16);
            tv.setPadding(20, 20, 20, 20);
            productContainer.addView(tv);
        }

        cursor.close();
    }

    // ================= FULLSCREEN IMAGE =================
    private void showImageFullScreen(int imageRes) {

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_image_fullscreen, null);

        ImageView img = view.findViewById(R.id.fullImage);
        img.setImageResource(imageRes);

        AlertDialog dialog = new AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        ).setView(view).create();

        view.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void playVideoFullscreen(Uri videoUri) {

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_video_fullscreen, null);

        VideoView fullVideo = view.findViewById(R.id.fullVideoView);

        fullVideo.setVideoURI(videoUri);
        fullVideo.start();

        AlertDialog dialog = new AlertDialog.Builder(
                getContext(),
                android.R.style.Theme_Black_NoTitleBar_Fullscreen
        )
                .setView(view)
                .create();

        view.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

}
