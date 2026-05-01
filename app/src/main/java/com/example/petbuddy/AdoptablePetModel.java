package com.example.petbuddy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdoptablePetModel implements Serializable {
    private String petId;
    
    // 1. Pet Basic Information
    private String name;
    private String species; // "dog", "cat", "rabbit", etc.
    private String breed;
    private String gender; // "male", "female"
    private int age; // in months
    private String size; // "small", "medium", "large"
    private String color;
    
    // 2. Pet Description
    private String description;
    private String personality; // friendly, playful, calm, active
    private String specialTraits; // special habits or traits
    
    // 3. Health Information
    private String healthStatus;
    private boolean isVaccinated;
    private boolean isNeutered;
    private String medicalCondition; // any medical conditions
    private String lastHealthCheckDate;
    private String temperament;
    
    // 4. Pet Images
    private String imageUrl;
    private String[] additionalImages; // multiple images
    private List<String> imageUrls; // Firebase Storage URLs for multiple images
    
    // 5. Location
    private String city;
    private String shelterName;
    private String shelterLocation;
    private String shelterContact;
    
    // 6. Adoption Requirements
    private int minimumAdopterAge;
    private String homeEnvironmentRequired; // house, apartment, yard
    private String experienceRequired; // beginner, intermediate, experienced
    private String additionalRequirements;
    
    // 7. Contact Information
    private String contactPersonName;
    private String contactPhone;
    private String contactEmail;
    
    // 8. Adoption Status
    private String adoptionStatus; // "available", "adopted", "reserved"
    private boolean isAvailable;
    private long dateAdded;
    private String specialNeeds;
    private double adoptionFee;
    
    // 9. Realtime Functionality Fields
    private long lastUpdated; // Timestamp of last update
    private String lastUpdatedBy; // User ID who made the last update
    private int version; // Version number for conflict resolution
    private String syncStatus; // "synced", "pending", "conflict"
    private boolean isLocalOnly; // True if this is a local-only record
    private Map<String, Object> metadata; // Extensible metadata map
    
    // 10. Offline Support Fields
    private boolean needsSync; // True if changes need to be synced to server
    private long localModificationTime; // When this record was last modified locally
    private String conflictResolutionStrategy; // "server_wins", "client_wins", "manual"

    public AdoptablePetModel() {
        // Default constructor required for Firebase
        this.imageUrls = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
        this.version = 1;
        this.syncStatus = "synced";
        this.isLocalOnly = false;
        this.needsSync = false;
        this.localModificationTime = System.currentTimeMillis();
        this.conflictResolutionStrategy = "server_wins";
    }

    public AdoptablePetModel(String name, String species, String breed, int age, String gender, 
                           String size, String color, String description, String healthStatus, 
                           String temperament, boolean isVaccinated, boolean isNeutered, 
                           String imageUrl, String shelterName, String shelterContact, 
                           String shelterLocation, String specialNeeds, double adoptionFee) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.size = size;
        this.color = color;
        this.description = description;
        this.healthStatus = healthStatus;
        this.temperament = temperament;
        this.isVaccinated = isVaccinated;
        this.isNeutered = isNeutered;
        this.imageUrl = imageUrl;
        this.shelterName = shelterName;
        this.shelterContact = shelterContact;
        this.shelterLocation = shelterLocation;
        this.specialNeeds = specialNeeds;
        this.adoptionFee = adoptionFee;
        this.isAvailable = true;
        this.adoptionStatus = "available";
        this.dateAdded = System.currentTimeMillis();
        
        // Initialize realtime fields
        this.imageUrls = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
        this.version = 1;
        this.syncStatus = "synced";
        this.isLocalOnly = false;
        this.needsSync = false;
        this.localModificationTime = System.currentTimeMillis();
        this.conflictResolutionStrategy = "server_wins";
        
        // Set default values for new fields
        this.personality = temperament;
        this.specialTraits = "Friendly and well-behaved";
        this.medicalCondition = "None";
        this.lastHealthCheckDate = "Recent";
        this.city = "Local Area";
        this.minimumAdopterAge = 18;
        this.homeEnvironmentRequired = "Any loving home";
        this.experienceRequired = "Beginner friendly";
        this.additionalRequirements = "None";
        this.contactPersonName = shelterName;
        this.contactPhone = shelterContact;
        this.contactEmail = shelterContact;
    }

    // Enhanced constructor with all fields
    public AdoptablePetModel(String name, String species, String breed, int age, String gender, 
                           String size, String color, String description, String personality,
                           String specialTraits, String healthStatus, boolean isVaccinated, 
                           boolean isNeutered, String medicalCondition, String lastHealthCheckDate,
                           String imageUrl, String city, String shelterName, String shelterLocation, 
                           String shelterContact, int minimumAdopterAge, String homeEnvironmentRequired,
                           String experienceRequired, String additionalRequirements,
                           String contactPersonName, String contactPhone, String contactEmail,
                           String specialNeeds, double adoptionFee) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.size = size;
        this.color = color;
        this.description = description;
        this.personality = personality;
        this.specialTraits = specialTraits;
        this.healthStatus = healthStatus;
        this.isVaccinated = isVaccinated;
        this.isNeutered = isNeutered;
        this.medicalCondition = medicalCondition;
        this.lastHealthCheckDate = lastHealthCheckDate;
        this.imageUrl = imageUrl;
        this.city = city;
        this.shelterName = shelterName;
        this.shelterLocation = shelterLocation;
        this.shelterContact = shelterContact;
        this.minimumAdopterAge = minimumAdopterAge;
        this.homeEnvironmentRequired = homeEnvironmentRequired;
        this.experienceRequired = experienceRequired;
        this.additionalRequirements = additionalRequirements;
        this.contactPersonName = contactPersonName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.specialNeeds = specialNeeds;
        this.adoptionFee = adoptionFee;
        this.isAvailable = true;
        this.adoptionStatus = "available";
        this.dateAdded = System.currentTimeMillis();
        this.temperament = personality;
        
        // Initialize realtime fields
        this.imageUrls = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
        this.version = 1;
        this.syncStatus = "synced";
        this.isLocalOnly = false;
        this.needsSync = false;
        this.localModificationTime = System.currentTimeMillis();
        this.conflictResolutionStrategy = "server_wins";
    }

    // Getters and Setters
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecies() { return species; }
    public void setSpecies(String species) { this.species = species; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPersonality() { return personality; }
    public void setPersonality(String personality) { this.personality = personality; }

    public String getSpecialTraits() { return specialTraits; }
    public void setSpecialTraits(String specialTraits) { this.specialTraits = specialTraits; }

    public String getHealthStatus() { return healthStatus; }
    public void setHealthStatus(String healthStatus) { this.healthStatus = healthStatus; }

    public String getTemperament() { return temperament; }
    public void setTemperament(String temperament) { this.temperament = temperament; }

    public boolean isVaccinated() { return isVaccinated; }
    public void setVaccinated(boolean vaccinated) { isVaccinated = vaccinated; }

    public boolean isNeutered() { return isNeutered; }
    public void setNeutered(boolean neutered) { isNeutered = neutered; }

    public String getMedicalCondition() { return medicalCondition; }
    public void setMedicalCondition(String medicalCondition) { this.medicalCondition = medicalCondition; }

    public String getLastHealthCheckDate() { return lastHealthCheckDate; }
    public void setLastHealthCheckDate(String lastHealthCheckDate) { this.lastHealthCheckDate = lastHealthCheckDate; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String[] getAdditionalImages() { return additionalImages; }
    public void setAdditionalImages(String[] additionalImages) { this.additionalImages = additionalImages; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getShelterName() { return shelterName; }
    public void setShelterName(String shelterName) { this.shelterName = shelterName; }

    public String getShelterContact() { return shelterContact; }
    public void setShelterContact(String shelterContact) { this.shelterContact = shelterContact; }

    public String getShelterLocation() { return shelterLocation; }
    public void setShelterLocation(String shelterLocation) { this.shelterLocation = shelterLocation; }

    public int getMinimumAdopterAge() { return minimumAdopterAge; }
    public void setMinimumAdopterAge(int minimumAdopterAge) { this.minimumAdopterAge = minimumAdopterAge; }

    public String getHomeEnvironmentRequired() { return homeEnvironmentRequired; }
    public void setHomeEnvironmentRequired(String homeEnvironmentRequired) { this.homeEnvironmentRequired = homeEnvironmentRequired; }

    public String getExperienceRequired() { return experienceRequired; }
    public void setExperienceRequired(String experienceRequired) { this.experienceRequired = experienceRequired; }

    public String getAdditionalRequirements() { return additionalRequirements; }
    public void setAdditionalRequirements(String additionalRequirements) { this.additionalRequirements = additionalRequirements; }

    public String getContactPersonName() { return contactPersonName; }
    public void setContactPersonName(String contactPersonName) { this.contactPersonName = contactPersonName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }

    public String getAdoptionStatus() { return adoptionStatus; }
    public void setAdoptionStatus(String adoptionStatus) { 
        this.adoptionStatus = adoptionStatus;
        this.isAvailable = "available".equals(adoptionStatus);
    }

    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { 
        isAvailable = available;
        this.adoptionStatus = available ? "available" : "adopted";
    }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public String getSpecialNeeds() { return specialNeeds; }
    public void setSpecialNeeds(String specialNeeds) { this.specialNeeds = specialNeeds; }

    public double getAdoptionFee() { return adoptionFee; }
    public void setAdoptionFee(double adoptionFee) { this.adoptionFee = adoptionFee; }

    // Realtime functionality getters and setters
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    
    public void addImageUrl(String imageUrl) {
        if (this.imageUrls == null) {
            this.imageUrls = new ArrayList<>();
        }
        this.imageUrls.add(imageUrl);
        markAsModified();
    }
    
    public void removeImageUrl(String imageUrl) {
        if (this.imageUrls != null) {
            this.imageUrls.remove(imageUrl);
            markAsModified();
        }
    }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getLastUpdatedBy() { return lastUpdatedBy; }
    public void setLastUpdatedBy(String lastUpdatedBy) { this.lastUpdatedBy = lastUpdatedBy; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }

    public boolean isLocalOnly() { return isLocalOnly; }
    public void setLocalOnly(boolean localOnly) { 
        isLocalOnly = localOnly;
        if (localOnly) {
            this.syncStatus = "pending";
        }
    }

    public Map<String, Object> getMetadata() { 
        if (metadata == null) {
            metadata = new HashMap<>();
        }
        return metadata; 
    }
    
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
        markAsModified();
    }
    
    public Object getMetadata(String key) {
        return this.metadata != null ? this.metadata.get(key) : null;
    }

    public boolean needsSync() { return needsSync; }
    public void setNeedsSync(boolean needsSync) { this.needsSync = needsSync; }

    public long getLocalModificationTime() { return localModificationTime; }
    public void setLocalModificationTime(long localModificationTime) { this.localModificationTime = localModificationTime; }

    public String getConflictResolutionStrategy() { return conflictResolutionStrategy; }
    public void setConflictResolutionStrategy(String conflictResolutionStrategy) { this.conflictResolutionStrategy = conflictResolutionStrategy; }
    
    /**
     * Mark this model as modified for sync purposes
     */
    public void markAsModified() {
        this.localModificationTime = System.currentTimeMillis();
        this.needsSync = true;
        this.syncStatus = "pending";
    }
    
    /**
     * Mark this model as synced
     */
    public void markAsSynced() {
        this.needsSync = false;
        this.syncStatus = "synced";
        this.lastUpdated = System.currentTimeMillis();
    }
    
    /**
     * Check if this model has conflicts with another version
     */
    public boolean hasConflictWith(AdoptablePetModel other) {
        if (other == null) return false;
        
        // Check if versions are different and both have been modified
        return this.version != other.version && 
               this.lastUpdated != other.lastUpdated &&
               this.needsSync && other.needsSync;
    }
    
    /**
     * Get primary image URL (backwards compatibility)
     */
    public String getPrimaryImageUrl() {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            return imageUrls.get(0);
        }
        return imageUrl;
    }
    
    /**
     * Set primary image URL (backwards compatibility)
     */
    public void setPrimaryImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        if (this.imageUrls == null) {
            this.imageUrls = new ArrayList<>();
        }
        if (this.imageUrls.isEmpty()) {
            this.imageUrls.add(imageUrl);
        } else {
            this.imageUrls.set(0, imageUrl);
        }
        markAsModified();
    }

    // Helper methods
    public String getAgeString() {
        if (age < 12) {
            return age + " months";
        } else {
            int years = age / 12;
            int months = age % 12;
            if (months == 0) {
                return years + " year" + (years > 1 ? "s" : "");
            } else {
                return years + " year" + (years > 1 ? "s" : "") + " " + months + " month" + (months > 1 ? "s" : "");
            }
        }
    }

    public String getSpeciesEmoji() {
        switch (species.toLowerCase()) {
            case "dog": return "🐕";
            case "cat": return "🐱";
            case "rabbit": return "🐰";
            case "bird": return "🐦";
            case "hamster": return "🐹";
            default: return "🐾";
        }
    }

    public String getGenderEmoji() {
        return gender.equalsIgnoreCase("male") ? "♂️" : "♀️";
    }

    public String getAdoptionStatusEmoji() {
        switch (adoptionStatus.toLowerCase()) {
            case "available": return "✅";
            case "adopted": return "❤️";
            case "reserved": return "⏳";
            default: return "❓";
        }
    }

    public String getHealthStatusSummary() {
        StringBuilder health = new StringBuilder();
        health.append("🏥 ").append(healthStatus);
        if (isVaccinated) health.append(" • ✅ Vaccinated");
        if (isNeutered) health.append(" • ✅ Spayed/Neutered");
        if (medicalCondition != null && !medicalCondition.equalsIgnoreCase("none")) {
            health.append(" • ⚠️ ").append(medicalCondition);
        }
        return health.toString();
    }

    public String getContactInfo() {
        StringBuilder contact = new StringBuilder();
        if (contactPersonName != null) contact.append("👤 ").append(contactPersonName).append("\n");
        if (contactPhone != null) contact.append("📞 ").append(contactPhone).append("\n");
        if (contactEmail != null) contact.append("📧 ").append(contactEmail);
        return contact.toString();
    }

    public String getAdoptionRequirementsSummary() {
        StringBuilder requirements = new StringBuilder();
        requirements.append("👥 Min Age: ").append(minimumAdopterAge).append(" years\n");
        if (homeEnvironmentRequired != null) {
            requirements.append("🏠 Home: ").append(homeEnvironmentRequired).append("\n");
        }
        if (experienceRequired != null) {
            requirements.append("🎓 Experience: ").append(experienceRequired);
        }
        return requirements.toString();
    }
    
    // Additional methods for Firebase integration
    
    public String getAdopterId() {
        return (String) getMetadata("adopterId");
    }
    
    public void setAdopterId(String adopterId) {
        addMetadata("adopterId", adopterId);
    }
    
    public String getAdopterName() {
        return (String) getMetadata("adopterName");
    }
    
    public void setAdopterName(String adopterName) {
        addMetadata("adopterName", adopterName);
    }
    
    public String getLocation() {
        return getShelterLocation(); // Use existing shelter location
    }
    
    public void setLocation(String location) {
        setShelterLocation(location);
    }
    
    public void setContactInfo(String contactInfo) {
        // Parse contact info and set appropriate fields
        if (contactInfo != null && contactInfo.contains("@")) {
            setContactEmail(contactInfo);
        } else if (contactInfo != null) {
            setContactPhone(contactInfo);
        }
    }
}