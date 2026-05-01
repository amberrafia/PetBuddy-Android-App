package com.example.petbuddy;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AdoptablePetsRecyclerAdapter extends RecyclerView.Adapter<AdoptablePetsRecyclerAdapter.ViewHolder> {

    private Context context;
    private ArrayList<AdoptablePetModel> petsList;
    private OnPetClickListener clickListener;
    private OnPetEditListener editListener;
    private SimpleDateFormat dateFormat;
    private boolean isAdmin;

    public interface OnPetClickListener {
        void onPetClicked(AdoptablePetModel pet);
    }

    public interface OnPetEditListener {
        void onPetEdit(AdoptablePetModel pet);
    }

    public AdoptablePetsRecyclerAdapter(Context context, ArrayList<AdoptablePetModel> petsList, OnPetClickListener listener) {
        this.context = context;
        this.petsList = petsList;
        this.clickListener = listener;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        this.isAdmin = AdminHelper.isCurrentUserAdmin(context);
    }

    public void setOnPetEditListener(OnPetEditListener editListener) {
        this.editListener = editListener;
    }

    public void refreshAdminStatus() {
        this.isAdmin = AdminHelper.isCurrentUserAdmin(context);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.list_item_adoptable_pet, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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
                // User is not admin - show admin access button
                holder.btnAdminEdit.setText("🔒 ADMIN");
                holder.btnAdminEdit.setBackgroundTintList(ContextCompat.getColorStateList(context, R.color.warning));
                holder.btnAdminEdit.setTextColor(ContextCompat.getColor(context, android.R.color.white));
                // Add elevation for better visibility
                holder.btnAdminEdit.setElevation(8f);
                holder.btnAdminEdit.setOnClickListener(v -> {
                    Toast.makeText(context, "👑 Admin access required! Go to menu → Admin Panel → Grant Admin", Toast.LENGTH_LONG).show();
                });
            }
            
            // Force button properties for debugging
            holder.btnAdminEdit.setAlpha(1.0f);
            holder.btnAdminEdit.setEnabled(true);
            holder.btnAdminEdit.setClickable(true);
            
            // Debug log
            Log.d("AdoptablePetsRecyclerAdapter", "Admin button configured for " + pet.getName() + 
                  " - Visible: " + (holder.btnAdminEdit.getVisibility() == View.VISIBLE) +
                  " - Admin Status: " + currentAdminStatus);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                clickListener.onPetClicked(pet);
            }
        });
    }

    @Override
    public int getItemCount() {
        return petsList.size();
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
        String imageUrl = pet.getImageUrl();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // If we have a Firebase Storage URL
            if (imageUrl.startsWith("https://")) {
                try {
                    com.bumptech.glide.Glide.with(context)
                        .load(imageUrl)
                        .placeholder(getDefaultDrawableForSpecies(pet.getSpecies()))
                        .error(getDefaultDrawableForSpecies(pet.getSpecies()))
                        .centerCrop()
                        .into(imageView);
                    return;
                } catch (Exception e) {
                    Log.e("AdoptablePetsRecyclerAdapter", "Error loading image with Glide", e);
                }
            }
            // If we have a Base64 encoded image (legacy support)
            else if (imageUrl.startsWith("data:image")) {
                try {
                    String base64String = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    byte[] decodedString = android.util.Base64.decode(base64String, android.util.Base64.DEFAULT);
                    android.graphics.Bitmap decodedByte = android.graphics.BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imageView.setImageBitmap(decodedByte);
                    return;
                } catch (Exception e) {
                    Log.e("AdoptablePetsRecyclerAdapter", "Error loading Base64 image", e);
                }
            }
        }
        
        // Use default image if no custom image or error loading
        setDefaultPetImage(imageView, pet.getSpecies());
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

    public static class ViewHolder extends RecyclerView.ViewHolder {
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

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPetPhoto = itemView.findViewById(R.id.imgPetPhoto);
            txtPetName = itemView.findViewById(R.id.txtPetName);
            txtPetSpecies = itemView.findViewById(R.id.txtPetSpecies);
            txtPetAge = itemView.findViewById(R.id.txtPetAge);
            txtPetGender = itemView.findViewById(R.id.txtPetGender);
            txtPetSize = itemView.findViewById(R.id.txtPetSize);
            txtPetBreed = itemView.findViewById(R.id.txtPetBreed);
            txtPetDescription = itemView.findViewById(R.id.txtPetDescription);
            txtPersonality = itemView.findViewById(R.id.txtPersonality);
            txtShelterInfo = itemView.findViewById(R.id.txtShelterInfo);
            txtAdoptionFee = itemView.findViewById(R.id.txtAdoptionFee);
            txtHealthStatus = itemView.findViewById(R.id.txtHealthStatus);
            txtContactInfo = itemView.findViewById(R.id.txtContactInfo);
            txtSpecialNeeds = itemView.findViewById(R.id.txtSpecialNeeds);
            txtDateAdded = itemView.findViewById(R.id.txtDateAdded);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            btnAdminEdit = itemView.findViewById(R.id.btnAdminEdit);
        }
    }
}