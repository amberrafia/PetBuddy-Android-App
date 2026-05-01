package com.example.petbuddy;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing realtime animal data operations with Firebase Realtime Database
 */
public class AnimalDataService {
    private static final String TAG = "AnimalDataService";
    
    private static AnimalDataService instance;
    private final FirebaseManager firebaseManager;
    private final Map<String, ValueEventListener> activeListeners;
    private final Map<String, ChildEventListener> activeChildListeners;
    private final Map<String, AnimalFilter> activeFilters;
    
    private AnimalDataService() {
        this.firebaseManager = FirebaseManager.getInstance();
        this.activeListeners = new HashMap<>();
        this.activeChildListeners = new HashMap<>();
        this.activeFilters = new ConcurrentHashMap<>();
    }
    
    public static synchronized AnimalDataService getInstance() {
        if (instance == null) {
            instance = new AnimalDataService();
        }
        return instance;
    }
    
    /**
     * Get all available animals with realtime updates
     */
    public void getAvailableAnimals(String listenerId, AnimalDataCallback callback) {
        DatabaseReference petsRef = firebaseManager.getAdoptablePetsRef();
        
        // Remove existing listener if any
        removeListener(listenerId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<AdoptablePetModel> animals = new ArrayList<>();
                
                for (DataSnapshot petSnapshot : dataSnapshot.getChildren()) {
                    try {
                        AdoptablePetModel animal = parseAnimalFromSnapshot(petSnapshot);
                        if (animal != null && isAnimalAvailable(animal)) {
                            animals.add(animal);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing animal data", e);
                    }
                }
                
                Log.d(TAG, "Loaded " + animals.size() + " available animals");
                callback.onAnimalsLoaded(animals);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load animals", databaseError.toException());
                callback.onError(databaseError.toException());
            }
        };
        
        activeListeners.put(listenerId, listener);
        petsRef.addValueEventListener(listener);
    }
    
    /**
     * Get animals by species with realtime updates
     */
    public void getAnimalsBySpecies(String species, String listenerId, AnimalDataCallback callback) {
        DatabaseReference petsRef = firebaseManager.getAdoptablePetsRef();
        Query speciesQuery = petsRef.orderByChild("basicInfo/species").equalTo(species.toLowerCase());
        
        // Remove existing listener if any
        removeListener(listenerId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<AdoptablePetModel> animals = new ArrayList<>();
                
                for (DataSnapshot petSnapshot : dataSnapshot.getChildren()) {
                    try {
                        AdoptablePetModel animal = parseAnimalFromSnapshot(petSnapshot);
                        if (animal != null && isAnimalAvailable(animal)) {
                            animals.add(animal);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing animal data for species: " + species, e);
                    }
                }
                
                Log.d(TAG, "Loaded " + animals.size() + " " + species + "s");
                callback.onAnimalsLoaded(animals);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load " + species + "s", databaseError.toException());
                callback.onError(databaseError.toException());
            }
        };
        
        activeListeners.put(listenerId, listener);
        speciesQuery.addValueEventListener(listener);
    }
    
    /**
     * Get single animal by ID with realtime updates
     */
    public void getAnimalById(String animalId, String listenerId, SingleAnimalCallback callback) {
        DatabaseReference animalRef = firebaseManager.getAdoptablePetsRef().child(animalId);
        
        // Remove existing listener if any
        removeListener(listenerId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        AdoptablePetModel animal = parseAnimalFromSnapshot(dataSnapshot);
                        if (animal != null) {
                            callback.onAnimalLoaded(animal);
                        } else {
                            callback.onError(new Exception("Failed to parse animal data"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing animal data for ID: " + animalId, e);
                        callback.onError(e);
                    }
                } else {
                    callback.onError(new Exception("Animal not found: " + animalId));
                }
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load animal: " + animalId, databaseError.toException());
                callback.onError(databaseError.toException());
            }
        };
        
        activeListeners.put(listenerId, listener);
        animalRef.addValueEventListener(listener);
    }
    
    /**
     * Listen for new animals being added
     */
    public void listenForNewAnimals(String listenerId, NewAnimalCallback callback) {
        DatabaseReference petsRef = firebaseManager.getAdoptablePetsRef();
        
        // Remove existing child listener if any
        removeChildListener(listenerId);
        
        ChildEventListener childListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    AdoptablePetModel animal = parseAnimalFromSnapshot(dataSnapshot);
                    if (animal != null && isAnimalAvailable(animal)) {
                        Log.d(TAG, "New animal added: " + animal.getName());
                        callback.onNewAnimal(animal);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing new animal data", e);
                }
            }
            
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                try {
                    AdoptablePetModel animal = parseAnimalFromSnapshot(dataSnapshot);
                    if (animal != null) {
                        Log.d(TAG, "Animal updated: " + animal.getName());
                        callback.onAnimalUpdated(animal);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing updated animal data", e);
                }
            }
            
            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                try {
                    String animalId = dataSnapshot.getKey();
                    if (animalId != null) {
                        Log.d(TAG, "Animal removed: " + animalId);
                        callback.onAnimalRemoved(animalId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error handling animal removal", e);
                }
            }
            
            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                // Not used for this implementation
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Child listener cancelled", databaseError.toException());
                callback.onError(databaseError.toException());
            }
        };
        
        activeChildListeners.put(listenerId, childListener);
        petsRef.addChildEventListener(childListener);
    }
    
    /**
     * Update animal adoption status
     */
    public void updateAdoptionStatus(String animalId, String newStatus, StatusUpdateCallback callback) {
        DatabaseReference statusRef = firebaseManager.getAdoptablePetsRef()
            .child(animalId)
            .child("adoption")
            .child("status");
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("lastUpdated", System.currentTimeMillis());
        
        DatabaseReference animalRef = firebaseManager.getAdoptablePetsRef().child(animalId);
        animalRef.child("adoption").updateChildren(updates)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Adoption status updated for animal: " + animalId + " to " + newStatus);
                callback.onSuccess();
            })
            .addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to update adoption status for animal: " + animalId, exception);
                callback.onError(exception);
            });
    }
    
    /**
     * Save new animal to database
     */
    public void saveAnimal(AdoptablePetModel animal, SaveCallback callback) {
        DatabaseReference animalRef = firebaseManager.getAdoptablePetsRef().child(animal.getPetId());
        
        Map<String, Object> animalData = convertAnimalToMap(animal);
        
        animalRef.setValue(animalData)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Animal saved successfully: " + animal.getName());
                callback.onSuccess();
            })
            .addOnFailureListener(exception -> {
                Log.e(TAG, "Failed to save animal: " + animal.getName(), exception);
                callback.onError(exception);
            });
    }
    
    /**
     * Remove listener by ID
     */
    public void removeListener(String listenerId) {
        ValueEventListener listener = activeListeners.remove(listenerId);
        if (listener != null) {
            firebaseManager.getAdoptablePetsRef().removeEventListener(listener);
            Log.d(TAG, "Removed listener: " + listenerId);
        }
        
        // Also remove associated filter
        activeFilters.remove(listenerId);
    }
    
    /**
     * Remove child listener by ID
     */
    public void removeChildListener(String listenerId) {
        ChildEventListener childListener = activeChildListeners.remove(listenerId);
        if (childListener != null) {
            firebaseManager.getAdoptablePetsRef().removeEventListener(childListener);
            Log.d(TAG, "Removed child listener: " + listenerId);
        }
    }
    
    /**
     * Remove all active listeners
     */
    public void removeAllListeners() {
        for (Map.Entry<String, ValueEventListener> entry : activeListeners.entrySet()) {
            firebaseManager.getAdoptablePetsRef().removeEventListener(entry.getValue());
        }
        activeListeners.clear();
        
        for (Map.Entry<String, ChildEventListener> entry : activeChildListeners.entrySet()) {
            firebaseManager.getAdoptablePetsRef().removeEventListener(entry.getValue());
        }
        activeChildListeners.clear();
        
        // Clear all filters
        activeFilters.clear();
        
        Log.d(TAG, "All listeners removed");
    }
    
    /**
     * Check if animal is available for adoption
     */
    private boolean isAnimalAvailable(AdoptablePetModel animal) {
        // Add logic to check availability based on adoption status
        return animal != null && animal.getName() != null && !animal.getName().isEmpty();
    }
    
    /**
     * Parse animal data from Firebase snapshot
     */
    private AdoptablePetModel parseAnimalFromSnapshot(DataSnapshot snapshot) {
        try {
            AdoptablePetModel animal = new AdoptablePetModel();
            
            // Set pet ID from key
            animal.setPetId(snapshot.getKey());
            
            // Parse basic info
            DataSnapshot basicInfo = snapshot.child("basicInfo");
            if (basicInfo.exists()) {
                animal.setName(basicInfo.child("name").getValue(String.class));
                animal.setSpecies(basicInfo.child("species").getValue(String.class));
                animal.setBreed(basicInfo.child("breed").getValue(String.class));
                animal.setAge(basicInfo.child("age").getValue(Integer.class));
                animal.setGender(basicInfo.child("gender").getValue(String.class));
                animal.setSize(basicInfo.child("size").getValue(String.class));
                animal.setColor(basicInfo.child("color").getValue(String.class));
            }
            
            // Parse description
            DataSnapshot description = snapshot.child("description");
            if (description.exists()) {
                animal.setDescription(description.child("description").getValue(String.class));
                animal.setPersonality(description.child("personality").getValue(String.class));
            }
            
            // Parse health info
            DataSnapshot health = snapshot.child("health");
            if (health.exists()) {
                animal.setHealthStatus(health.child("healthStatus").getValue(String.class));
                animal.setVaccinated(Boolean.TRUE.equals(health.child("isVaccinated").getValue(Boolean.class)));
                animal.setNeutered(Boolean.TRUE.equals(health.child("isNeutered").getValue(Boolean.class)));
                animal.setSpecialNeeds(health.child("specialNeeds").getValue(String.class));
            }
            
            // Parse images
            DataSnapshot images = snapshot.child("images");
            if (images.exists()) {
                animal.setImageUrl(images.child("primary").getValue(String.class));
            }
            
            // Parse location
            DataSnapshot location = snapshot.child("location");
            if (location.exists()) {
                animal.setShelterName(location.child("shelterName").getValue(String.class));
                animal.setShelterLocation(location.child("shelterLocation").getValue(String.class));
            }
            
            // Parse adoption info
            DataSnapshot adoption = snapshot.child("adoption");
            if (adoption.exists()) {
                Double fee = adoption.child("fee").getValue(Double.class);
                animal.setAdoptionFee(fee != null ? fee : 0.0);
                
                DataSnapshot contact = adoption.child("contact");
                if (contact.exists()) {
                    animal.setContactPersonName(contact.child("personName").getValue(String.class));
                    animal.setContactPhone(contact.child("phone").getValue(String.class));
                }
            }
            
            // Parse metadata
            DataSnapshot metadata = snapshot.child("metadata");
            if (metadata.exists()) {
                Long dateAdded = metadata.child("dateAdded").getValue(Long.class);
                animal.setDateAdded(dateAdded != null ? dateAdded : System.currentTimeMillis());
            }
            
            return animal;
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing animal from snapshot", e);
            return null;
        }
    }
    
    /**
     * Convert AdoptablePetModel to Firebase-compatible map
     */
    private Map<String, Object> convertAnimalToMap(AdoptablePetModel animal) {
        Map<String, Object> animalData = new HashMap<>();
        
        // Basic info
        Map<String, Object> basicInfo = new HashMap<>();
        basicInfo.put("name", animal.getName());
        basicInfo.put("species", animal.getSpecies());
        basicInfo.put("breed", animal.getBreed());
        basicInfo.put("age", animal.getAge());
        basicInfo.put("gender", animal.getGender());
        basicInfo.put("size", animal.getSize());
        basicInfo.put("color", animal.getColor());
        animalData.put("basicInfo", basicInfo);
        
        // Description
        Map<String, Object> description = new HashMap<>();
        description.put("description", animal.getDescription());
        description.put("personality", animal.getPersonality());
        animalData.put("description", description);
        
        // Health
        Map<String, Object> health = new HashMap<>();
        health.put("healthStatus", animal.getHealthStatus());
        health.put("isVaccinated", animal.isVaccinated());
        health.put("isNeutered", animal.isNeutered());
        health.put("specialNeeds", animal.getSpecialNeeds());
        animalData.put("health", health);
        
        // Images
        Map<String, Object> images = new HashMap<>();
        images.put("primary", animal.getImageUrl());
        animalData.put("images", images);
        
        // Location
        Map<String, Object> location = new HashMap<>();
        location.put("shelterName", animal.getShelterName());
        location.put("shelterLocation", animal.getShelterLocation());
        animalData.put("location", location);
        
        // Adoption
        Map<String, Object> adoption = new HashMap<>();
        adoption.put("status", "available");
        adoption.put("fee", animal.getAdoptionFee());
        
        Map<String, Object> contact = new HashMap<>();
        contact.put("personName", animal.getContactPersonName());
        contact.put("phone", animal.getContactPhone());
        adoption.put("contact", contact);
        animalData.put("adoption", adoption);
        
        // Metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("dateAdded", animal.getDateAdded());
        metadata.put("lastUpdated", System.currentTimeMillis());
        metadata.put("version", 1);
        metadata.put("isAvailable", true);
        animalData.put("metadata", metadata);
        
        return animalData;
    }
    
    /**
     * Animal filter class for multi-criteria filtering
     */
    public static class AnimalFilter {
        private String species;
        private String size;
        private String gender;
        private Integer minAge;
        private Integer maxAge;
        private String location;
        private String adoptionStatus;
        private Boolean isVaccinated;
        private Boolean isNeutered;
        private String searchText;
        
        public AnimalFilter() {}
        
        // Getters and setters
        public String getSpecies() { return species; }
        public void setSpecies(String species) { this.species = species; }
        
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        
        public String getGender() { return gender; }
        public void setGender(String gender) { this.gender = gender; }
        
        public Integer getMinAge() { return minAge; }
        public void setMinAge(Integer minAge) { this.minAge = minAge; }
        
        public Integer getMaxAge() { return maxAge; }
        public void setMaxAge(Integer maxAge) { this.maxAge = maxAge; }
        
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        
        public String getAdoptionStatus() { return adoptionStatus; }
        public void setAdoptionStatus(String adoptionStatus) { this.adoptionStatus = adoptionStatus; }
        
        public Boolean getIsVaccinated() { return isVaccinated; }
        public void setIsVaccinated(Boolean isVaccinated) { this.isVaccinated = isVaccinated; }
        
        public Boolean getIsNeutered() { return isNeutered; }
        public void setIsNeutered(Boolean isNeutered) { this.isNeutered = isNeutered; }
        
        public String getSearchText() { return searchText; }
        public void setSearchText(String searchText) { this.searchText = searchText; }
        
        /**
         * Check if animal matches this filter
         */
        public boolean matches(AdoptablePetModel animal) {
            if (animal == null) return false;
            
            // Species filter
            if (species != null && !species.isEmpty() && 
                !species.equalsIgnoreCase(animal.getSpecies())) {
                return false;
            }
            
            // Size filter
            if (size != null && !size.isEmpty() && 
                !size.equalsIgnoreCase(animal.getSize())) {
                return false;
            }
            
            // Gender filter
            if (gender != null && !gender.isEmpty() && 
                !gender.equalsIgnoreCase(animal.getGender())) {
                return false;
            }
            
            // Age range filter
            if (minAge != null && animal.getAge() < minAge) {
                return false;
            }
            if (maxAge != null && animal.getAge() > maxAge) {
                return false;
            }
            
            // Location filter
            if (location != null && !location.isEmpty()) {
                String animalLocation = animal.getShelterLocation();
                if (animalLocation == null || 
                    !animalLocation.toLowerCase().contains(location.toLowerCase())) {
                    return false;
                }
            }
            
            // Adoption status filter
            if (adoptionStatus != null && !adoptionStatus.isEmpty() && 
                !adoptionStatus.equalsIgnoreCase(animal.getAdoptionStatus())) {
                return false;
            }
            
            // Vaccination filter
            if (isVaccinated != null && animal.isVaccinated() != isVaccinated) {
                return false;
            }
            
            // Neutered filter
            if (isNeutered != null && animal.isNeutered() != isNeutered) {
                return false;
            }
            
            // Text search filter
            if (searchText != null && !searchText.isEmpty()) {
                String searchLower = searchText.toLowerCase();
                boolean matchesText = false;
                
                // Search in name
                if (animal.getName() != null && 
                    animal.getName().toLowerCase().contains(searchLower)) {
                    matchesText = true;
                }
                
                // Search in breed
                if (!matchesText && animal.getBreed() != null && 
                    animal.getBreed().toLowerCase().contains(searchLower)) {
                    matchesText = true;
                }
                
                // Search in description
                if (!matchesText && animal.getDescription() != null && 
                    animal.getDescription().toLowerCase().contains(searchLower)) {
                    matchesText = true;
                }
                
                // Search in personality
                if (!matchesText && animal.getPersonality() != null && 
                    animal.getPersonality().toLowerCase().contains(searchLower)) {
                    matchesText = true;
                }
                
                if (!matchesText) {
                    return false;
                }
            }
            
            return true;
        }
        
        /**
         * Check if filter has any criteria set
         */
        public boolean isEmpty() {
            return (species == null || species.isEmpty()) &&
                   (size == null || size.isEmpty()) &&
                   (gender == null || gender.isEmpty()) &&
                   minAge == null && maxAge == null &&
                   (location == null || location.isEmpty()) &&
                   (adoptionStatus == null || adoptionStatus.isEmpty()) &&
                   isVaccinated == null && isNeutered == null &&
                   (searchText == null || searchText.isEmpty());
        }
    }
    
    /**
     * Get filtered animals with realtime updates
     */
    public void getFilteredAnimals(AnimalFilter filter, String listenerId, AnimalDataCallback callback) {
        // Store filter for reapplying on data updates
        activeFilters.put(listenerId, filter);
        
        DatabaseReference petsRef = firebaseManager.getAdoptablePetsRef();
        Query query = petsRef;
        
        // Apply Firebase-level filtering where possible for better performance
        if (filter.getSpecies() != null && !filter.getSpecies().isEmpty()) {
            query = petsRef.orderByChild("basicInfo/species").equalTo(filter.getSpecies().toLowerCase());
        }
        
        // Remove existing listener if any
        removeListener(listenerId);
        
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List<AdoptablePetModel> animals = new ArrayList<>();
                
                for (DataSnapshot petSnapshot : dataSnapshot.getChildren()) {
                    try {
                        AdoptablePetModel animal = parseAnimalFromSnapshot(petSnapshot);
                        if (animal != null && isAnimalAvailable(animal) && filter.matches(animal)) {
                            animals.add(animal);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing filtered animal data", e);
                    }
                }
                
                Log.d(TAG, "Loaded " + animals.size() + " filtered animals");
                callback.onAnimalsLoaded(animals);
            }
            
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e(TAG, "Failed to load filtered animals", databaseError.toException());
                callback.onError(databaseError.toException());
            }
        };
        
        activeListeners.put(listenerId, listener);
        query.addValueEventListener(listener);
    }
    
    /**
     * Update filter for existing listener
     */
    public void updateFilter(String listenerId, AnimalFilter newFilter) {
        AnimalFilter oldFilter = activeFilters.get(listenerId);
        if (oldFilter != null) {
            activeFilters.put(listenerId, newFilter);
            
            // If the listener exists, we need to restart it with new filter
            ValueEventListener listener = activeListeners.get(listenerId);
            if (listener != null) {
                // Find the callback - this is a limitation, we'd need to store callbacks too
                // For now, log that filter was updated
                Log.d(TAG, "Filter updated for listener: " + listenerId);
            }
        }
    }
    
    /**
     * Get current filter for a listener
     */
    public AnimalFilter getFilter(String listenerId) {
        return activeFilters.get(listenerId);
    }
    
    /**
     * Clear filter for a listener
     */
    public void clearFilter(String listenerId) {
        activeFilters.remove(listenerId);
    }
    
    /**
     * Search animals by text across multiple fields
     */
    public void searchAnimals(String searchText, String listenerId, AnimalDataCallback callback) {
        AnimalFilter searchFilter = new AnimalFilter();
        searchFilter.setSearchText(searchText);
        getFilteredAnimals(searchFilter, listenerId, callback);
    }
    
    /**
     * Advanced search with multiple text fields
     */
    public void advancedSearch(String searchText, String species, String listenerId, AnimalDataCallback callback) {
        AnimalFilter searchFilter = new AnimalFilter();
        searchFilter.setSearchText(searchText);
        searchFilter.setSpecies(species);
        searchFilter.setAdoptionStatus("available");
        getFilteredAnimals(searchFilter, listenerId, callback);
    }
    
    /**
     * Search animals with automatic refresh on data changes
     */
    public void searchAnimalsWithAutoRefresh(String searchText, String listenerId, AnimalDataCallback callback) {
        // Store the search text for auto-refresh
        AnimalFilter searchFilter = new AnimalFilter();
        searchFilter.setSearchText(searchText);
        searchFilter.setAdoptionStatus("available");
        
        // Use filtered animals method which automatically refreshes on data changes
        getFilteredAnimals(searchFilter, listenerId, new AnimalDataCallback() {
            @Override
            public void onAnimalsLoaded(List<AdoptablePetModel> animals) {
                // Sort by relevance (name matches first, then breed, then description)
                animals.sort((a1, a2) -> {
                    if (searchText == null || searchText.isEmpty()) {
                        return 0;
                    }
                    
                    String searchLower = searchText.toLowerCase();
                    
                    // Calculate relevance scores
                    int score1 = calculateSearchRelevance(a1, searchLower);
                    int score2 = calculateSearchRelevance(a2, searchLower);
                    
                    return Integer.compare(score2, score1); // Higher score first
                });
                
                callback.onAnimalsLoaded(animals);
            }
            
            @Override
            public void onError(Exception exception) {
                callback.onError(exception);
            }
        });
    }
    
    /**
     * Calculate search relevance score for sorting
     */
    private int calculateSearchRelevance(AdoptablePetModel animal, String searchLower) {
        int score = 0;
        
        // Name match (highest priority)
        if (animal.getName() != null && animal.getName().toLowerCase().contains(searchLower)) {
            score += 100;
            if (animal.getName().toLowerCase().startsWith(searchLower)) {
                score += 50; // Bonus for starting with search term
            }
        }
        
        // Breed match (medium priority)
        if (animal.getBreed() != null && animal.getBreed().toLowerCase().contains(searchLower)) {
            score += 50;
            if (animal.getBreed().toLowerCase().startsWith(searchLower)) {
                score += 25;
            }
        }
        
        // Description match (lower priority)
        if (animal.getDescription() != null && animal.getDescription().toLowerCase().contains(searchLower)) {
            score += 20;
        }
        
        // Personality match (lower priority)
        if (animal.getPersonality() != null && animal.getPersonality().toLowerCase().contains(searchLower)) {
            score += 15;
        }
        
        // Special needs match
        if (animal.getSpecialNeeds() != null && animal.getSpecialNeeds().toLowerCase().contains(searchLower)) {
            score += 10;
        }
        
        return score;
    }
    
    /**
     * Get search suggestions based on partial text
     */
    public void getSearchSuggestions(String partialText, SearchSuggestionsCallback callback) {
        if (partialText == null || partialText.length() < 2) {
            callback.onSuggestions(new ArrayList<>());
            return;
        }
        
        // Get all available animals to generate suggestions
        DatabaseReference petsRef = firebaseManager.getAdoptablePetsRef();
        
        petsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Map<String, Integer> suggestions = new HashMap<>();
                String partialLower = partialText.toLowerCase();
                
                for (DataSnapshot petSnapshot : dataSnapshot.getChildren()) {
                    try {
                        AdoptablePetModel animal = parseAnimalFromSnapshot(petSnapshot);
                        if (animal != null && isAnimalAvailable(animal)) {
                            
                            // Add name suggestions
                            if (animal.getName() != null) {
                                String name = animal.getName().toLowerCase();
                                if (name.contains(partialLower)) {
                                    suggestions.put(animal.getName(), suggestions.getOrDefault(animal.getName(), 0) + 3);
                                }
                            }
                            
                            // Add breed suggestions
                            if (animal.getBreed() != null) {
                                String breed = animal.getBreed().toLowerCase();
                                if (breed.contains(partialLower)) {
                                    suggestions.put(animal.getBreed(), suggestions.getOrDefault(animal.getBreed(), 0) + 2);
                                }
                            }
                            
                            // Add personality trait suggestions
                            if (animal.getPersonality() != null) {
                                String[] traits = animal.getPersonality().split("[,\\s]+");
                                for (String trait : traits) {
                                    String traitLower = trait.toLowerCase().trim();
                                    if (traitLower.contains(partialLower) && traitLower.length() > 2) {
                                        suggestions.put(trait.trim(), suggestions.getOrDefault(trait.trim(), 0) + 1);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing animal for suggestions", e);
                    }
                }
                
                // Convert to sorted list
                List<String> sortedSuggestions = new ArrayList<>();
                suggestions.entrySet().stream()
                    .sorted((e1, e2) -> Integer.compare(e2.getValue(), e1.getValue()))
                    .limit(10)
                    .forEach(entry -> sortedSuggestions.add(entry.getKey()));
                
                callback.onSuggestions(sortedSuggestions);
            }
            
            @Override
            public void onCancelled(DatabaseError error) {
                Log.e(TAG, "Failed to get search suggestions", error.toException());
                callback.onSuggestions(new ArrayList<>());
            }
        });
    }
    
    /**
     * Get animals by multiple criteria
     */
    public void getAnimalsByCriteria(String species, String size, String gender, 
                                   Integer minAge, Integer maxAge, String location,
                                   String listenerId, AnimalDataCallback callback) {
        AnimalFilter filter = new AnimalFilter();
        filter.setSpecies(species);
        filter.setSize(size);
        filter.setGender(gender);
        filter.setMinAge(minAge);
        filter.setMaxAge(maxAge);
        filter.setLocation(location);
        filter.setAdoptionStatus("available");
        
        getFilteredAnimals(filter, listenerId, callback);
    }
    
    /**
     * Get available animals by species with additional filters
     */
    public void getAvailableAnimalsBySpeciesWithFilters(String species, String size, String gender,
                                                       String listenerId, AnimalDataCallback callback) {
        AnimalFilter filter = new AnimalFilter();
        filter.setSpecies(species);
        filter.setSize(size);
        filter.setGender(gender);
        filter.setAdoptionStatus("available");
        
        getFilteredAnimals(filter, listenerId, callback);
    }
    
    // Callback interfaces
    public interface AnimalDataCallback {
        void onAnimalsLoaded(List<AdoptablePetModel> animals);
        void onError(Exception exception);
    }
    
    public interface SingleAnimalCallback {
        void onAnimalLoaded(AdoptablePetModel animal);
        void onError(Exception exception);
    }
    
    public interface NewAnimalCallback {
        void onNewAnimal(AdoptablePetModel animal);
        void onAnimalUpdated(AdoptablePetModel animal);
        void onAnimalRemoved(String animalId);
        void onError(Exception exception);
    }
    
    public interface StatusUpdateCallback {
        void onSuccess();
        void onError(Exception exception);
    }
    
    public interface SaveCallback {
        void onSuccess();
        void onError(Exception exception);
    }
    
    public interface SearchSuggestionsCallback {
        void onSuggestions(List<String> suggestions);
    }
}