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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.snackbar.Snackbar;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DogsFragment extends Fragment implements 
    AnimalDataService.AnimalDataCallback,
    AnimalDataService.NewAnimalCallback,
    RealtimeDataManager.ConnectionStateCallback {

    private RecyclerView recyclerViewDogs;
    private TextView txtDogCount, txtConnectionStatus;
    private LinearLayout layoutDogEmptyState;
    private Spinner spinnerDogSize, spinnerDogAge;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private ArrayList<AdoptablePetModel> dogsList, filteredDogsList;
    private AdoptablePetsRecyclerAdapter adapter;
    
    // Firebase services
    private AnimalDataService animalDataService;
    private RealtimeDataManager realtimeDataManager;
    private ImageCacheManager imageCacheManager;
    
    // Listener management
    private static final String DOGS_LISTENER_ID = "dogs_fragment_listener";
    private static final String NEW_DOGS_LISTENER_ID = "dogs_fragment_new_listener";
    private static final String CONNECTION_CALLBACK_ID = "dogs_fragment_connection";
    
    private boolean isFragmentVisible = false;
    private boolean hasInitialLoad = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dogs, container, false);
        
        initializeServices();
        initializeViews(view);
        setupFilters();
        setupSwipeRefresh();
        
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
        recyclerViewDogs = view.findViewById(R.id.recyclerViewDogs);
        txtDogCount = view.findViewById(R.id.txtDogCount);
        txtConnectionStatus = view.findViewById(R.id.txtConnectionStatus);
        layoutDogEmptyState = view.findViewById(R.id.layoutDogEmptyState);
        spinnerDogSize = view.findViewById(R.id.spinnerDogSize);
        spinnerDogAge = view.findViewById(R.id.spinnerDogAge);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);

        dogsList = new ArrayList<>();
        filteredDogsList = new ArrayList<>();
        adapter = new AdoptablePetsRecyclerAdapter(getContext(), filteredDogsList, this::onDogClicked);
        
        // ALWAYS set admin edit listener - the adapter will handle admin status checking
        adapter.setOnPetEditListener(this::onDogEdit);
        
        recyclerViewDogs.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewDogs.setAdapter(adapter);

        // Clear filters button
        Button btnClearDogFilters = view.findViewById(R.id.btnClearDogFilters);
        btnClearDogFilters.setOnClickListener(v -> clearFilters());
        
        // Reset filters button (from empty state)
        Button btnResetDogFilters = view.findViewById(R.id.btnResetDogFilters);
        btnResetDogFilters.setOnClickListener(v -> clearFilters());
        
        // Initialize connection status
        updateConnectionStatus(false, false);
    }
    
    private void setupSwipeRefresh() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setColorSchemeResources(
                R.color.primary,
                R.color.accent,
                R.color.primary_dark
            );
            
            swipeRefreshLayout.setOnRefreshListener(() -> {
                refreshDogData();
            });
        }
    }
    
    private void startRealtimeListeners() {
        if (!isFragmentVisible || animalDataService == null || realtimeDataManager == null) {
            return;
        }
        
        // Register listeners with RealtimeDataManager for automatic restoration
        realtimeDataManager.registerListener(
            DOGS_LISTENER_ID,
            RealtimeDataManager.ListenerType.ANIMALS_BY_SPECIES,
            "dog",
            null,
            this,
            null,
            null
        );
        
        realtimeDataManager.registerListener(
            NEW_DOGS_LISTENER_ID,
            RealtimeDataManager.ListenerType.NEW_ANIMALS,
            null,
            null,
            null,
            null,
            this
        );
        
        // Start listening for dog data
        animalDataService.getAnimalsBySpecies("dog", DOGS_LISTENER_ID, this);
        animalDataService.listenForNewAnimals(NEW_DOGS_LISTENER_ID, this);
    }
    
    private void stopRealtimeListeners() {
        if (animalDataService != null) {
            animalDataService.removeListener(DOGS_LISTENER_ID);
            animalDataService.removeChildListener(NEW_DOGS_LISTENER_ID);
        }
        
        if (realtimeDataManager != null) {
            realtimeDataManager.unregisterListener(DOGS_LISTENER_ID);
            realtimeDataManager.unregisterListener(NEW_DOGS_LISTENER_ID);
        }
    }
    
    private void refreshDogData() {
        if (animalDataService != null && isFragmentVisible) {
            animalDataService.getAnimalsBySpecies("dog", DOGS_LISTENER_ID, this);
        }
    }

    private void setupFilters() {
        // Size filter
        String[] sizeOptions = {"All Sizes", "Small", "Medium", "Large"};
        ArrayAdapter<String> sizeAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, sizeOptions);
        sizeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDogSize.setAdapter(sizeAdapter);

        // Age filter
        String[] ageOptions = {"All Ages", "Puppy (0-12 months)", "Young (1-3 years)", "Adult (3-7 years)", "Senior (7+ years)"};
        ArrayAdapter<String> ageAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, ageOptions);
        ageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDogAge.setAdapter(ageAdapter);

        // Filter listeners
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        };

        spinnerDogSize.setOnItemSelectedListener(filterListener);
        spinnerDogAge.setOnItemSelectedListener(filterListener);
    }

    private void loadDogs() {
        // This method is now replaced by startRealtimeListeners()
        // Keep for backwards compatibility but redirect to new method
        startRealtimeListeners();
    }
    
    // AnimalDataService.AnimalDataCallback implementation
    @Override
    public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
        if (!isFragmentVisible) return;
        
        dogsList.clear();
        
        // Filter for dogs only (extra safety check)
        for (AdoptablePetModel animal : animals) {
            if ("dog".equalsIgnoreCase(animal.getSpecies()) && animal.isAvailable()) {
                dogsList.add(animal);
            }
        }
        
        // Sort by date added (newest first)
        Collections.sort(dogsList, (p1, p2) -> Long.compare(p2.getDateAdded(), p1.getDateAdded()));
        
        hasInitialLoad = true;
        applyFilters();
        
        // Stop refresh animation
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
        
        // Preload images for better user experience
        preloadDogImages();
    }
    
    @Override
    public void onError(Exception exception) {
        if (!isFragmentVisible) return;
        
        if (getContext() != null) {
            Toast.makeText(getContext(), "Failed to load dogs: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
        }
        
        // Stop refresh animation
        if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }
    
    // AnimalDataService.NewAnimalCallback implementation
    @Override
    public void onNewAnimal(AdoptablePetModel animal) {
        if (!isFragmentVisible || !"dog".equalsIgnoreCase(animal.getSpecies())) return;
        
        // Check if animal already exists in list
        boolean exists = false;
        for (AdoptablePetModel existingDog : dogsList) {
            if (existingDog.getPetId().equals(animal.getPetId())) {
                exists = true;
                break;
            }
        }
        
        if (!exists) {
            dogsList.add(0, animal); // Add to beginning for newest first
            applyFilters();
            
            // Show notification
            if (getView() != null) {
                Snackbar.make(getView(), "New dog available: " + animal.getName(), Snackbar.LENGTH_LONG)
                    .setAction("View", v -> onDogClicked(animal))
                    .show();
            }
        }
    }
    
    @Override
    public void onAnimalUpdated(AdoptablePetModel animal) {
        if (!isFragmentVisible || !"dog".equalsIgnoreCase(animal.getSpecies())) return;
        
        // Update existing animal in list
        for (int i = 0; i < dogsList.size(); i++) {
            if (dogsList.get(i).getPetId().equals(animal.getPetId())) {
                dogsList.set(i, animal);
                applyFilters();
                break;
            }
        }
    }
    
    @Override
    public void onAnimalRemoved(String animalId) {
        if (!isFragmentVisible) return;
        
        // Remove animal from list
        dogsList.removeIf(dog -> dog.getPetId().equals(animalId));
        applyFilters();
    }
    
    // RealtimeDataManager.ConnectionStateCallback implementation
    @Override
    public void onNetworkStateChanged(boolean connected) {
        if (!isFragmentVisible) return;
        
        updateConnectionStatus(connected, realtimeDataManager.isFirebaseConnected());
        
        if (connected && !hasInitialLoad) {
            // Retry loading data when network becomes available
            refreshDogData();
        }
    }
    
    @Override
    public void onFirebaseStateChanged(boolean connected) {
        if (!isFragmentVisible) return;
        
        updateConnectionStatus(realtimeDataManager.isNetworkConnected(), connected);
        
        if (connected && !hasInitialLoad) {
            // Retry loading data when Firebase becomes available
            refreshDogData();
        }
    }
    
    private void updateConnectionStatus(boolean networkConnected, boolean firebaseConnected) {
        if (txtConnectionStatus == null) return;
        
        if (networkConnected && firebaseConnected) {
            txtConnectionStatus.setText("🟢 Live data");
            txtConnectionStatus.setTextColor(getResources().getColor(R.color.success));
            txtConnectionStatus.setVisibility(View.VISIBLE);
        } else if (networkConnected && !firebaseConnected) {
            txtConnectionStatus.setText("🟡 Connecting...");
            txtConnectionStatus.setTextColor(getResources().getColor(R.color.warning));
            txtConnectionStatus.setVisibility(View.VISIBLE);
        } else {
            txtConnectionStatus.setText("🔴 Offline");
            txtConnectionStatus.setTextColor(getResources().getColor(R.color.error));
            txtConnectionStatus.setVisibility(View.VISIBLE);
        }
    }
    
    private void preloadDogImages() {
        if (imageCacheManager == null || dogsList.isEmpty()) return;
        
        List<String> imageUrls = new ArrayList<>();
        for (AdoptablePetModel dog : dogsList) {
            if (dog.getPrimaryImageUrl() != null) {
                imageUrls.add(dog.getPrimaryImageUrl());
            }
            
            // Also preload additional images
            if (dog.getImageUrls() != null) {
                imageUrls.addAll(dog.getImageUrls());
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
        filteredDogsList.clear();

        String selectedSize = spinnerDogSize.getSelectedItem() != null ? spinnerDogSize.getSelectedItem().toString() : "All Sizes";
        String selectedAge = spinnerDogAge.getSelectedItem() != null ? spinnerDogAge.getSelectedItem().toString() : "All Ages";

        for (AdoptablePetModel dog : dogsList) {
            boolean matchesSize = selectedSize.equals("All Sizes") || 
                                 dog.getSize().equalsIgnoreCase(selectedSize);

            boolean matchesAge = selectedAge.equals("All Ages") || matchesAgeFilter(dog, selectedAge);

            if (matchesSize && matchesAge) {
                filteredDogsList.add(dog);
            }
        }

        updateUI();
    }

    private boolean matchesAgeFilter(AdoptablePetModel pet, String ageFilter) {
        int ageInMonths = pet.getAge();
        switch (ageFilter) {
            case "Puppy (0-12 months)":
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
        spinnerDogSize.setSelection(0);
        spinnerDogAge.setSelection(0);
        Toast.makeText(getContext(), "Dog filters cleared", Toast.LENGTH_SHORT).show();
    }

    private void updateUI() {
        String countText = "Available Dogs: " + filteredDogsList.size();
        if (hasInitialLoad) {
            countText += " (Live)";
        }
        txtDogCount.setText(countText);
        
        adapter.notifyDataSetChanged();

        if (filteredDogsList.isEmpty()) {
            recyclerViewDogs.setVisibility(View.GONE);
            layoutDogEmptyState.setVisibility(View.VISIBLE);
        } else {
            recyclerViewDogs.setVisibility(View.VISIBLE);
            layoutDogEmptyState.setVisibility(View.GONE);
        }
    }

    private void onDogClicked(AdoptablePetModel dog) {
        Intent intent = new Intent(getContext(), PetDetailsActivity.class);
        intent.putExtra("petId", dog.getPetId());
        startActivity(intent);
    }

    private void onDogEdit(AdoptablePetModel dog) {
        if (getActivity() instanceof AdoptPetActivity) {
            ((AdoptPetActivity) getActivity()).showEditPetDialog(dog);
        }
    }

    public void refreshAdminStatus() {
        if (adapter != null) {
            adapter.refreshAdminStatus();
            // ALWAYS set the edit listener - adapter handles admin status
            adapter.setOnPetEditListener(this::onDogEdit);
        }
    }
}