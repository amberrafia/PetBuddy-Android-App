package com.example.petbuddy;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class DogGroomingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_dog_grooming,
                container,
                false
        );

        ImageView imgThumb = view.findViewById(R.id.imgThumbnail);
        ImageView imgPlay = view.findViewById(R.id.imgPlay);
        VideoView videoView = view.findViewById(R.id.videoView);

        /* ✅ FIX */
        videoView.setVisibility(View.INVISIBLE);
        imgThumb.bringToFront();
        imgPlay.bringToFront();
        /* ✅ FIX */

        imgPlay.setOnClickListener(v -> {
            imgThumb.setVisibility(View.GONE);
            imgPlay.setVisibility(View.GONE);
            videoView.setVisibility(View.VISIBLE);

            Uri uri = Uri.parse(
                    "android.resource://" +
                            requireContext().getPackageName() +
                            "/" + R.raw.dog_grooming  // You can change this to dog_training.mp4
            );

            videoView.setVideoURI(uri);
            videoView.start();
        });



        return view;
    }
}
