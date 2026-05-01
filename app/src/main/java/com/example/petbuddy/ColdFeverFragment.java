package com.example.petbuddy;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ColdFeverFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_cold_fever, container, false);

        ImageView imgCat = view.findViewById(R.id.imgCatColdFever);
        ImageView imgDog = view.findViewById(R.id.imgDogColdFever);


        imgCat.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Cat Cold & Fever Tips",
                        Toast.LENGTH_SHORT).show()
        );


        imgDog.setOnClickListener(v ->
                Toast.makeText(getContext(),
                        "Dog Cold & Fever Tips",
                        Toast.LENGTH_SHORT).show()
        );

        return view;
    }
}
