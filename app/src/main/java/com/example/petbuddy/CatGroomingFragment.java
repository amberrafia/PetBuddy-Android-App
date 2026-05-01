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

public class CatGroomingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cat_grooming, container, false);
        
        // Find video components (if they exist in the layout)
        ImageView imgThumb = view.findViewById(R.id.imgThumbnail);
        ImageView imgPlay = view.findViewById(R.id.imgPlay);
        VideoView videoView = view.findViewById(R.id.videoView);

        // Set up video playback if components exist
        if (imgThumb != null && imgPlay != null && videoView != null) {
            videoView.setVisibility(View.INVISIBLE);
            imgThumb.bringToFront();
            imgPlay.bringToFront();

            imgPlay.setOnClickListener(v -> {
                imgThumb.setVisibility(View.GONE);
                imgPlay.setVisibility(View.GONE);
                videoView.setVisibility(View.VISIBLE);

                Uri uri = Uri.parse(
                        "android.resource://" +
                                requireContext().getPackageName() +
                                "/" + R.raw.cat_grooming  // You can change this to cat_grooming.mp4
                );

                videoView.setVideoURI(uri);
                videoView.start();
            });
        }

        return view;
    }
}

