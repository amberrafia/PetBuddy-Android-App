package com.example.petbuddy;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdoptablePetsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<AdoptablePetModel> petsList;
    private OnPetClickListener clickListener;
    private OnPetEditListener editListener;
    private SimpleDateFormat dateFormat;
    private boolean isAdmin;
    
    // Image management
    private ImageCacheManager imageCacheManager;
    private RequestOptions glideOptions;

    public interface OnPetClickListener {
        void onPetClicked(AdoptablePetModel pet);
    }

    public interface OnPetEditListener {
        void onPetEdit(AdoptablePetModel pet);
    }

    public AdoptablePetsAdapter(Context context, ArrayList<AdoptablePetModel> petsList, OnPetClickListener listener) {
        this.context = context;
        this.petsList = petsList;
        this.clickListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.isAdmin = AdminHelper.isCurrentUserAdmin(context);
        
        // Initialize image management
        this.imageCacheManager = ImageCacheManager.getInstance(context);
        this.glideOptions = new RequestOptions()
            .centerCrop()
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .timeout(10000); // 10 second timeout
    }

    public void setOnPetEditListener(OnPetEditListener editListener) {
        this.editListener = editListener;
    }

    public void refreshAdminStatus() {
        this.isAdmin = AdminHelper.isCurrentUserAdmin(context);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return petsList.size();
    }

    @Override
    public Object getItem(int position) {
        return petsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_adoptable_pet, parent, false);
            holder = new ViewHolder();
            holder.imgPetPhoto = convertView.findViewById(R.id.imgPetPhoto);
            holder.txtPetName = convertView.findViewById(R.id.txtPetName);
            holder.txtPetSpecies = convertView.findViewById(R.id.txtPetSpecies);
            holder.txtPetAge = convertView.findViewById(R.id.txtPetAge);
            holder.txtPetGender = convertView.findViewById(R.id.txtPetGender);
            holder.txtPetSize = convertView.findViewById(R.id.txtPetSize);
            holder.txtPetBreed = convertView.findViewById(R.id.txtPetBreed);
            holder.txtPetDescription = convertView.findViewById(R.id.txtPetDescription);
            holder.txtPersonality = convertView.findViewById(R.id.txtPersonality);
            holder.txtShelterInfo = convertView.findViewById(R.id.txtShelterInfo);
            holder.txtAdoptionFee = convertView.findViewById(R.id.txtAdoptionFee);
            holder.txtHealthStatus = convertView.findViewById(R.id.txtHealthStatus);
            holder.txtContactInfo = convertView.findViewById(R.id.txtContactInfo);
            holder.txtSpecialNeeds = convertView.findViewById(R.id.txtSpecialNeeds);
            holder.txtDateAdded = convertView.findViewById(R.id.txtDateAdded);
            holder.btnViewDetails = convertView.findViewById(R.id.btnViewDetails);
            holder.btnAdminEdit = convertView.findViewById(R.id.btnAdminEdit);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        AdoptablePetModel pet = petsList.get(position);

        // Pet name (clean, no emoji)
        holder.txtPetName.setText(pet.getName());
        
        // Species badge (clean text)
        holder.txtPetSpecies.setText(pet.getSpecies().substring(0, 1).toUpperCase() + pet.getSpecies().substring(1));
        
        // Basic info tags (clean format)
        holder.txtPetAge.setText(pet.getAgeString());
        holder.txtPetGender.setText(pet.getGender().substring(0, 1).toUpperCase() + pet.getGender().substring(1));
        holder.txtPetSize.setText(pet.getSize().substring(0, 1).toUpperCase() + pet.getSize().substring(1));
        holder.txtPetBreed.setText(pet.getBreed());

        // Description
        String description = pet.getDescription();
        if (description.length() > 100) {
            description = description.substring(0, 100) + "...";
        }
        holder.txtPetDescription.setText(description);

        // Personality (clean format)
        String personality = pet.getPersonality() != null ? pet.getPersonality() : pet.getTemperament();
        if (personality != null) {
            holder.txtPersonality.setText(personality);
        } else {
            holder.txtPersonality.setText("Friendly");
        }

        // Shelter info (clean format)
        holder.txtShelterInfo.setText(pet.getShelterName() + " • " + pet.getShelterLocation());

        // Contact info (clean format)
        String contactInfo = "";
        if (pet.getContactPersonName() != null) {
            contactInfo += pet.getContactPersonName();
        }
        if (pet.getContactPhone() != null) {
            if (!contactInfo.isEmpty()) contactInfo += " • ";
            contactInfo += pet.getContactPhone();
        }
        if (contactInfo.isEmpty()) {
            contactInfo = "Contact shelter for details";
        }
        holder.txtContactInfo.setText(contactInfo);

        // Adoption fee (clean format)
        if (pet.getAdoptionFee() > 0) {
            holder.txtAdoptionFee.setText("$" + String.format("%.0f", pet.getAdoptionFee()));
        } else {
            holder.txtAdoptionFee.setText("Free Adoption");
        }

        // Health status (clean format)
        String healthInfo = pet.getHealthStatus();
        if (pet.isVaccinated()) {
            healthInfo += " • Vaccinated";
        }
        if (pet.isNeutered()) {
            healthInfo += " • Spayed/Neutered";
        }
        holder.txtHealthStatus.setText(healthInfo);

        // Special needs (clean format)
        if (pet.getSpecialNeeds() != null && !pet.getSpecialNeeds().isEmpty() && !pet.getSpecialNeeds().equalsIgnoreCase("none")) {
            holder.txtSpecialNeeds.setText("Special Needs: " + pet.getSpecialNeeds());
            holder.txtSpecialNeeds.setVisibility(View.VISIBLE);
        } else {
            holder.txtSpecialNeeds.setVisibility(View.GONE);
        }

        // Date added (clean format)
        Date dateAdded = new Date(pet.getDateAdded());
        holder.txtDateAdded.setText("Added: " + dateFormat.format(dateAdded));

        // Set pet photo (custom or default)
        loadPetImage(holder.imgPetPhoto, pet);

        // Click listeners
        holder.btnViewDetails.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPetClicked(pet);
            }
        });

        // Admin edit button - FORCE VISIBLE for debugging
        if (holder.btnAdminEdit != null) {
            // ALWAYS show the button for debugging
            holder.btnAdminEdit.setVisibility(View.VISIBLE);
            
            // Refresh admin status for each item
            boolean currentAdminStatus = AdminHelper.isCurrentUserAdmin(context);
            
            if (currentAdminStatus) {
                // User is admin - show edit functionality
                holder.btnAdminEdit.setText("👑 EDIT");
                holder.btnAdminEdit.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.admin_primary));
                holder.btnAdminEdit.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                // Add elevation for better visibility
                holder.btnAdminEdit.setElevation(8f);
                holder.btnAdminEdit.setOnClickListener(v -> {
                    if (editListener != null) {
                        editListener.onPetEdit(pet);
                    } else {
                        Toast.makeText(context, "👑 Admin: Edit " + pet.getName(), Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // User is not admin - show admin access button with better visibility
                holder.btnAdminEdit.setText("🔒 ADMIN");
                holder.btnAdminEdit.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.admin_primary));
                holder.btnAdminEdit.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                // Add elevation for better visibility
                holder.btnAdminEdit.setElevation(8f);
                holder.btnAdminEdit.setOnClickListener(v -> {
                    Toast.makeText(context, "👑 Admin access required! Go to menu → Admin Panel → Grant Admin", Toast.LENGTH_LONG).show();
                });
            }
            
            // Force button properties for debugging - Enhanced visibility
            holder.btnAdminEdit.setAlpha(1.0f);
            holder.btnAdminEdit.setEnabled(true);
            holder.btnAdminEdit.setClickable(true);
            holder.btnAdminEdit.setScaleX(1.1f); // Slightly larger
            holder.btnAdminEdit.setScaleY(1.1f); // Slightly larger
            
            // Debug log
            Log.d("AdoptablePetsAdapter", "Admin button configured for " + pet.getName() + 
                  " - Visible: " + (holder.btnAdminEdit.getVisibility() == View.VISIBLE) +
                  " - Admin Status: " + currentAdminStatus);
        }

        convertView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPetClicked(pet);
            }
        });

        return convertView;
    }

    private int getSpeciesColor(String species) {
        switch (species.toLowerCase()) {
            case "dog":
                return ContextCompat.getColor(context, R.color.dog_color);
            case "cat":
                return ContextCompat.getColor(context, R.color.cat_color);
            case "rabbit":
                return ContextCompat.getColor(context, R.color.rabbit_color);
            case "bird":
                return ContextCompat.getColor(context, R.color.bird_color);
            case "hamster":
                return ContextCompat.getColor(context, R.color.hamster_color);
            default:
                return ContextCompat.getColor(context, R.color.primary_light);
        }
    }

    private void setDefaultPetImage(ImageView imageView, String species) {
        // Set default images based on species
        switch (species.toLowerCase()) {
            case "dog":
                imageView.setImageResource(R.drawable.default_dog_image);
                break;
            case "cat":
                imageView.setImageResource(R.drawable.default_cat_image);
                break;
            case "rabbit":
                imageView.setImageResource(R.drawable.default_rabbit_image);
                break;
            case "bird":
                imageView.setImageResource(R.drawable.default_bird_image);
                break;
            default:
                imageView.setImageResource(R.drawable.default_pet_image);
                break;
        }
    }

    private void loadPetImage(ImageView imageView, AdoptablePetModel pet) {
        // Try to get primary image URL (supports both new and legacy formats)
        String primaryImageUrl = pet.getPrimaryImageUrl();
        
        if (primaryImageUrl != null && !primaryImageUrl.isEmpty()) {
            loadImageWithCache(imageView, primaryImageUrl, pet.getSpecies());
        } else {
            // No image available, use default
            setDefaultPetImage(imageView, pet.getSpecies());
        }
    }
    
    /**
     * Load image with caching support and progressive loading
     */
    private void loadImageWithCache(ImageView imageView, String imageUrl, String species) {
        // First, set placeholder
        setDefaultPetImage(imageView, species);
        
        // Check cache first
        imageCacheManager.getImage(imageUrl, new ImageCacheManager.ImageCacheCallback() {
            @Override
            public void onImageLoaded(Bitmap bitmap, ImageCacheManager.CacheSource source) {
                // Image found in cache, display it
                imageView.setImageBitmap(bitmap);
                Log.d("AdoptablePetsAdapter", "Image loaded from " + source + ": " + imageUrl);
            }
            
            @Override
            public void onCacheMiss(String imageUrl) {
                // Not in cache, load with Glide and cache the result
                loadImageWithGlide(imageView, imageUrl, species);
            }
            
            @Override
            public void onError(Exception exception) {
                Log.e("AdoptablePetsAdapter", "Cache error for image: " + imageUrl, exception);
                // Fallback to Glide
                loadImageWithGlide(imageView, imageUrl, species);
            }
        });
    }
    
    /**
     * Load image with Glide and cache the result
     */
    private void loadImageWithGlide(ImageView imageView, String imageUrl, String species) {
        if (imageUrl.startsWith("https://")) {
            // Firebase Storage URL or other web URL
            try {
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .apply(glideOptions)
                    .placeholder(getDefaultDrawableForSpecies(species))
                    .error(getDefaultDrawableForSpecies(species))
                    .into(new com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                        @Override
                        public void onResourceReady(Bitmap resource, com.bumptech.glide.request.transition.Transition<? super Bitmap> transition) {
                            // Display the image
                            imageView.setImageBitmap(resource);
                            
                            // Cache the image for future use
                            imageCacheManager.cacheImage(imageUrl, resource, new ImageCacheManager.CacheCallback() {
                                @Override
                                public void onSuccess() {
                                    Log.d("AdoptablePetsAdapter", "Image cached successfully: " + imageUrl);
                                }
                                
                                @Override
                                public void onError(Exception exception) {
                                    Log.w("AdoptablePetsAdapter", "Failed to cache image: " + imageUrl, exception);
                                }
                            });
                        }
                        
                        @Override
                        public void onLoadCleared(android.graphics.drawable.Drawable placeholder) {
                            // Handle cleanup if needed
                        }
                        
                        @Override
                        public void onLoadFailed(android.graphics.drawable.Drawable errorDrawable) {
                            Log.e("AdoptablePetsAdapter", "Failed to load image: " + imageUrl);
                            setDefaultPetImage(imageView, species);
                        }
                    });
                return;
            } catch (Exception e) {
                Log.e("AdoptablePetsAdapter", "Error loading image with Glide: " + imageUrl, e);
            }
        } else if (imageUrl.startsWith("data:image")) {
            // Base64 encoded image (legacy support)
            try {
                String base64String = imageUrl.substring(imageUrl.indexOf(",") + 1);
                byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                Bitmap decodedBitmap = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                
                if (decodedBitmap != null) {
                    imageView.setImageBitmap(decodedBitmap);
                    
                    // Cache the decoded bitmap
                    imageCacheManager.cacheImage(imageUrl, decodedBitmap, null);
                    return;
                }
            } catch (Exception e) {
                Log.e("AdoptablePetsAdapter", "Error loading Base64 image: " + imageUrl, e);
            }
        }
        
        // Fallback to default image
        setDefaultPetImage(imageView, species);
    }
    
    /**
     * Get all image URLs for a pet (for preloading)
     */
    public List<String> getAllImageUrls(AdoptablePetModel pet) {
        List<String> imageUrls = new ArrayList<>();
        
        // Add primary image
        String primaryUrl = pet.getPrimaryImageUrl();
        if (primaryUrl != null && !primaryUrl.isEmpty()) {
            imageUrls.add(primaryUrl);
        }
        
        // Add additional images
        if (pet.getImageUrls() != null) {
            for (String url : pet.getImageUrls()) {
                if (url != null && !url.isEmpty() && !imageUrls.contains(url)) {
                    imageUrls.add(url);
                }
            }
        }
        
        // Add legacy additional images
        if (pet.getAdditionalImages() != null) {
            for (String url : pet.getAdditionalImages()) {
                if (url != null && !url.isEmpty() && !imageUrls.contains(url)) {
                    imageUrls.add(url);
                }
            }
        }
        
        return imageUrls;
    }
    
    /**
     * Preload images for visible pets
     */
    public void preloadImagesForVisiblePets(int firstVisiblePosition, int visibleItemCount) {
        List<String> imagesToPreload = new ArrayList<>();
        
        int endPosition = Math.min(firstVisiblePosition + visibleItemCount + 5, petsList.size()); // Preload 5 extra
        
        for (int i = firstVisiblePosition; i < endPosition; i++) {
            if (i >= 0 && i < petsList.size()) {
                AdoptablePetModel pet = petsList.get(i);
                imagesToPreload.addAll(getAllImageUrls(pet));
            }
        }
        
        if (!imagesToPreload.isEmpty()) {
            imageCacheManager.preloadImages(imagesToPreload, new ImageCacheManager.PreloadCallback() {
                @Override
                public void onPreloadComplete(int loadedCount, int failedCount) {
                    Log.d("AdoptablePetsAdapter", "Preloaded " + loadedCount + " images (" + failedCount + " failed)");
                }
            });
        }
    }
    
    private int getDefaultDrawableForSpecies(String species) {
        switch (species.toLowerCase()) {
            case "dog":
                return R.drawable.default_dog_image;
            case "cat":
                return R.drawable.default_cat_image;
            case "rabbit":
                return R.drawable.default_rabbit_image;
            case "bird":
                return R.drawable.default_bird_image;
            default:
                return R.drawable.default_pet_image;
        }
    }

    private static class ViewHolder {
        ImageView imgPetPhoto;
        TextView txtPetName;
        TextView txtPetSpecies;
        TextView txtPetAge;
        TextView txtPetGender;
        TextView txtPetSize;
        TextView txtPetBreed;
        TextView txtPetDescription;
        TextView txtPersonality;
        TextView txtShelterInfo;
        TextView txtAdoptionFee;
        TextView txtHealthStatus;
        TextView txtContactInfo;
        TextView txtSpecialNeeds;
        TextView txtDateAdded;
        Button btnViewDetails;
        Button btnAdminEdit;
    }
}