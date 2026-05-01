package com.example.petbuddy;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BlogsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_blogs, container, false);

        // ===== CAT VIDEO =====
        VideoView catVideo = view.findViewById(R.id.catVideo);
        ImageView imgCatThumb = view.findViewById(R.id.imgCatThumb);
        ImageView imgCatPlay = view.findViewById(R.id.imgCatPlay);

        if (catVideo != null && imgCatPlay != null) {
            Uri catUri = Uri.parse("android.resource://" +
                    requireActivity().getPackageName() + "/" + R.raw.cat_grooming);

            catVideo.setVideoURI(catUri);

            MediaController catController = new MediaController(getContext());
            catController.setAnchorView(catVideo);
            catVideo.setMediaController(catController);

            imgCatPlay.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), FullScreenVideoActivity.class);
                intent.putExtra("video", R.raw.cat_grooming);
                startActivity(intent);
            });
        }

        // ===== DOG VIDEO =====
        VideoView dogVideo = view.findViewById(R.id.dogVideo);
        ImageView imgDogThumb = view.findViewById(R.id.imgDogThumb);
        ImageView imgDogPlay = view.findViewById(R.id.imgDogPlay);

        if (dogVideo != null && imgDogPlay != null) {
            Uri dogUri = Uri.parse("android.resource://" +
                    requireActivity().getPackageName() + "/" + R.raw.dog_grooming);

            dogVideo.setVideoURI(dogUri);

            MediaController dogController = new MediaController(getContext());
            dogController.setAnchorView(dogVideo);
            dogVideo.setMediaController(dogController);

            imgDogPlay.setOnClickListener(v -> {
                Intent intent = new Intent(getContext(), FullScreenVideoActivity.class);
                intent.putExtra("video", R.raw.dog_grooming);
                startActivity(intent);
            });
        }

        // ===== Other Clickable Images =====
        ImageView imgCat = view.findViewById(R.id.imgCatGrooming);
        ImageView imgDog = view.findViewById(R.id.imgDogGrooming);
        ImageView imgColdFever = view.findViewById(R.id.imgColdFever);

        if (imgCat != null) {
            imgCat.setOnClickListener(v -> openFragment(new CatGroomingFragment()));
        }

        if (imgDog != null) {
            imgDog.setOnClickListener(v -> openFragment(new DogGroomingFragment()));
        }

        if (imgColdFever != null) {
            imgColdFever.setOnClickListener(v -> openFragment(new ColdFeverFragment()));
        }

        return view;
    }

    private void openFragment(Fragment fragment) {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }
}
