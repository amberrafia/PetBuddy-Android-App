package com.example.petbuddy;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ClinicAdapter extends RecyclerView.Adapter<ClinicAdapter.ClinicViewHolder> {

    private List<Clinic> clinicList;

    public ClinicAdapter(List<Clinic> clinicList) {
        this.clinicList = clinicList;
    }

    @NonNull
    @Override
    public ClinicViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_clinic, parent, false);
        return new ClinicViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ClinicViewHolder holder, int position) {
        Clinic clinic = clinicList.get(position);
        holder.name.setText(clinic.name);
        holder.location.setText(clinic.location);
        holder.phone.setText(clinic.phone);
    }

    @Override
    public int getItemCount() {
        return clinicList.size();
    }

    static class ClinicViewHolder extends RecyclerView.ViewHolder {

        TextView name, location, phone;

        public ClinicViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.txtClinicName);
            location = itemView.findViewById(R.id.txtClinicLocation);
            phone = itemView.findViewById(R.id.txtClinicPhone);
        }
    }
}
