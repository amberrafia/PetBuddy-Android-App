package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CatsFragment extends Fragment implements 
    AnimalDataService.AnimalDataCallback,
    AnimalDataService.NewAnimalCallback,
    RealtimeDataManager.ConnectionStateCallback {

    private RecyclerView recyclerViewCats;
    private TextView txtCatCount;
    private LinearLayout layoutCatEmptyState;
    private Spinner spinnerCatSize, spinnerCatAge;
    
    private ArrayList<AdoptablePetModel> catsList, filteredCatsList;
    private AdoptablePetsRecyclerAdapter adapter;
    
    // Firebase services
    private AnimalDataService animalDataService;
    private RealtimeDataManager realtimeDataManager;
    private ImageCacheManager imageCacheManager;
    
    // Listener management
    private static final String CATS_LISTENER_ID = "cats_fragment_listener";
    private static final String NEW_CATS_LISTENER_ID = "cats_fragment_new_listener";
    private static final String CONNECTION_CALLBACK_ID = "cats_fragment_connection";
    
    private boolean isFragmentVisible = false;
    private boolean hasInitialLoad = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cats, container, false);
        
        initializeServices();
        initializeViews(view);
        setupFilters();
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        isFragmentVisible = true;
        startRealtimeListeners();
        
        // Register connection callback
        if (realtimeDataManager != null) {
            realtimeDataManager.registerConnectionCallback(CONNECTION_CALLBACK_ID, this);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        isFragmentVisible = false;
        stopRealtimeListeners();
        
        // Unregister connection callback
        if (realtimeDataManager != null) {
            realtimeDataManager.unregisterConnectionCallback(CONNECTION_CALLBACK_ID);
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRealtimeListeners();
        if (realtimeDataManager != null) {
            realtimeDataManager.unregisterConnectionCallback(CONNECTION_CALLBACK_ID);
        }
    }
    
    private void initializeServices() {
        animalDataService = AnimalDataService.getInstance();
        realtimeDataManager = RealtimeDataManager.getInstance(getContext());
        imageCacheManager = ImageCacheManager.getInstance(getContext());
    }

    private void initializeViews(View view) {
        recyclerViewCats = view.findViewById(R.id.recyclerViewCats);
        txtCatCount = view.findViewById(R.id.txtCatCount);
        layoutCatEmptyState = view.findViewById(R.id.layoutCatEmptyState);
        spinnerCatSize = view.findViewById(R.id.spinnerCatSize);
        spinnerCatAge = view.findViewById(R.id.spinnerCatAge);

        catsList = new ArrayList<>();
        filteredCatsList = new ArrayList<>();
        adapter = new AdoptablePetsRecyclerAdapter(getContext(), filteredCatsList, this::onCatClicked);
        
        // ALWAYS set admin edit listener - the adapter will handle admin status checking
        adapter.setOnPetEditListener(this::onCatEdit);
        
        recyclerViewCats.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewCats.setAdapter(adapter);

        // Clear filters button
        Button btnClearCatFilters = view.findViewById(R.id.btnClearCatFilters);
        btnClearCatFilters.setOnClickListener(v -> clearFilters());
        
        // Reset filters button (from empty state)
        Button btnResetCatFilters = view.findViewById(R.id.btnResetCatFilters);
        btnResetCatFilters.setOnClickListener(v -> clearFilters());
    }
    
    private void startRealtimeListeners() {
        if (!isFragmentVisible || animalDataService == null || realtimeDataManager == null) {
            return;
        }
        
        // Register listeners with RealtimeDataManager for automatic restoration
        realtimeDataManager.registerListener(
            CATS_LISTENER_ID,
            RealtimeDataManager.ListenerType.ANIMALS_BY_SPECIES,
            "cat",
            null,
            this,
            null,
            null
        );
        
        realtimeDataManager.registerListener(
            NEW_CATS_LISTENER_ID,
            RealtimeDataManager.ListenerType.NEW_ANIMALS,
            null,
            null,
            null,
            null,
            this
        );
        
        // Start listening for cat data
        animalDataService.getAnimalsBySpecies("cat", CATS_LISTENER_ID, this);
        animalDataService.listenForNewAnimals(NEW_CATS_LISTENER_ID, this);
    }
    
    private void stopRealtimeListeners() {
        if (animalDataService != null) {
            animalDataService.removeListener(CATS_LISTENER_ID);
            animalDataService.removeChildListener(NEW_CATS_LISTENER_ID);
        }
        
        if (realtimeDataManager != null) {
            realtimeDataManager.unregisterListener(CATS_LISTENER_ID);
            realtimeDataManager.unregisterListener(NEW_CATS_LISTENER_ID);
        }
    }
    
    private void refreshCatData() {
        if (animalDataService != null && isFragmentVisible) {
            animalDataService.getAnimalsBySpecies("cat", CATS_LISTENER_ID, this);
        }
    }

    private void setupFilters() {
        // Size filter
        String[] sizeOptions = {"All Sizes", "Small", "Medium", "Large"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sizeOptions);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCatSize.setAdapter(sizeAdapter);

        // Age filter
        String[] ageOptions = {"All Ages", "Kitten (0-12 months)", "Young (1-3 years)", "Adult (3-7 years)", "Senior (7+ years)"};
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, ageOptions);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCatAge.setAdapter(ageAdapter);

        // Filter listeners
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerCatSize.setOnItemSelectedListener(filterListener);
        spinnerCatAge.setOnItemSelectedListener(filterListener);
    }

    private void loadCats() {
        // This method is now replaced by startRealtimeListeners()
        // Keep for backwards compatibility but redirect to new method
        startRealtimeListeners();
    }
    
    // AnimalDataService.AnimalDataCallback implementation
    @Override
    public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
        if (!isFragmentVisible) return;
        
        catsList.clear();
        
        // Filter for cats only (extra safety check)
        for (AdoptablePetModel animal : animals) {
            if ("cat".equalsIgnoreCase(animal.getSpecies()) && animal.isAvailable()) {
                catsList.add(animal);
            }
        }
        
        // Sort by date added (newest first)
        Collections.sort(catsList, (p1, p2) -> Long.compare(p2.getDateAdded(), p1.getDateAdded()));
        
        hasInitialLoad = true;
        applyFilters();
        
        // Preload images for better user experience
        preloadCatImages();
    }
    
    @Override
    public void onError(Exception exception) {
        if (!isFragmentVisible) return;
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Failed to load cats: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    // AnimalDataService.NewAnimalCallback implementation
    @Override
    public void onNewAnimal(AdoptablePetModel animal) {
        if (!isFragmentVisible || !"cat".equalsIgnoreCase(animal.getSpecies())) return;
        
        // Check if animal already exists in list
        boolean exists = false;
        for (AdoptablePetModel existingCat : catsList) {
            if (existingCat.getPetId().equals(animal.getPetId())) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            catsList.add(0, animal); // Add to beginning for newest first
            applyFilters();
            
            // Show notification
            if (getView() != null) {
                Snackbar.make(getView(), "New cat available: " + animal.getName(), Snackbar.LENGTH_LONG)
                    .setAction("View", v -> onCatClicked(animal))
                    .show();
            }
        }
    }
    
    @Override
    public void onAnimalUpdated(AdoptablePetModel animal) {
        if (!isFragmentVisible || !"cat".equalsIgnoreCase(animal.getSpecies())) return;
        
        // Update existing animal in list
        for (int i = 0; i < catsList.size(); i++) {
            if (catsList.get(i).getPetId().equals(animal.getPetId())) {
                catsList.set(i, animal);
                applyFilters();
                break;
            }
        }
    }
    
    @Override
    public void onAnimalRemoved(String animalId) {
        if (!isFragmentVisible) return;
        
        // Remove animal from list
        catsList.removeIf(cat -> cat.getPetId().equals(animalId));
        applyFilters();
    }
    
    // RealtimeDataManager.ConnectionStateCallback implementation
    @Override
    public void onNetworkStateChanged(boolean connected) {
        if (!isFragmentVisible) return;
        
        if (connected && !hasInitialLoad) {
            // Retry loading data when network becomes available
            refreshCatData();
        }
    }
    
    @Override
    public void onFirebaseStateChanged(boolean connected) {
        if (!isFragmentVisible) return;
        
        if (connected && !hasInitialLoad) {
            // Retry loading data when Firebase becomes available
            refreshCatData();
        }
    }
    
    private void preloadCatImages() {
        if (imageCacheManager == null || catsList.isEmpty()) return;
        
        List<String> imageUrls = new ArrayList<>();
        for (AdoptablePetModel cat : catsList) {
            if (cat.getPrimaryImageUrl() != null) {
                imageUrls.add(cat.getPrimaryImageUrl());
            }
            
            // Also preload additional images
            if (cat.getImageUrls() != null) {
                imageUrls.addAll(cat.getImageUrls());
            }
        }
        
        imageCacheManager.preloadImages(imageUrls, new ImageCacheManager.PreloadCallback() {
            @Override
            public void onPreloadComplete(int loadedCount, int failedCount) {
                // Optionally show preload results
                if (getContext() != null && failedCount > 0) {
                    Toast.makeText(getContext(), 
                        "Preloaded " + loadedCount + " images (" + failedCount + " failed)", 
                        Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void applyFilters() {
        filteredCatsList.clear();

        String selectedSize = spinnerCatSize.getSelectedItem() != null ? spinnerCatSize.getSelectedItem().toString() : "All Sizes";
        String selectedAge = spinnerCatAge.getSelectedItem() != null ? spinnerCatAge.getSelectedItem().toString() : "All Ages";

        for (AdoptablePetModel cat : catsList) {
            boolean matchesSize = selectedSize.equals("All Sizes") || 
                                 cat.getSize().equalsIgnoreCase(selectedSize);

            boolean matchesAge = selectedAge.equals("All Ages") || matchesAgeFilter(cat, selectedAge);

            if (matchesSize && matchesAge) {
                filteredCatsList.add(cat);
            }
        }

        updateUI();
    }

    private boolean matchesAgeFilter(AdoptablePetModel pet, String ageFilter) {
        int ageInMonths = pet.getAge();
        switch (ageFilter) {
            case "Kitten (0-12 months)":
                return ageInMonths <= 12;
            case "Young (1-3 years)":
                return ageInMonths > 12 && ageInMonths <= 36;
            case "Adult (3-7 years)":
                return ageInMonths > 36 && ageInMonths <= 84;
            case "Senior (7+ years)":
                return ageInMonths > 84;
            default:
                return true;
        }
    }

    private void clearFilters() {
        spinnerCatSize.setSelection(0);
        spinnerCatAge.setSelection(0);
        Toast.makeText(getContext(), "Cat filters cleared", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        String countText = "Available Cats: " + filteredCatsList.size();
        if (hasInitialLoad) {
            countText += " (Live)";
        }
        txtCatCount.setText(countText);
        
        adapter.notifyDataSetChanged();

        if (filteredCatsList.isEmpty()) {
            recyclerViewCats.setVisibility(View.GONE);
            layoutCatEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewCats.setVisibility(View.VISIBLE);
            layoutCatEmptyState.setVisibility(View.GONE);
        }
    }

    private void onCatClicked(AdoptablePetModel cat) {
        Intent intent = new Intent(getContext(), PetDetailsActivity.class);
        intent.putExtra("petId", cat.getPetId());
        startActivity(intent);
    }

    private void onCatEdit(AdoptablePetModel cat) {
        if (getActivity() instanceof AdoptPetActivity) {
            ((AdoptPetActivity) getActivity()).showEditPetDialog(cat);
        }
    }

    public void refreshAdminStatus() {
        if (adapter != null) {
            adapter.refreshAdminStatus();
            // ALWAYS set the edit listener - adapter handles admin status
            adapter.setOnPetEditListener(this::onCatEdit);
        }
    }
}