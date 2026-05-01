package com.example.petbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class RegisteredUsersAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<EventRegistrationModel> registrationsList;
    private SimpleDateFormat dateFormat;

    public RegisteredUsersAdapter(Context context, ArrayList<EventRegistrationModel> registrationsList) {
        this.context = context;
        this.registrationsList = registrationsList;
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault());
    }

    @Override
    public int getCount() {
        return registrationsList.size();
    }

    @Override
    public Object getItem(int position) {
        return registrationsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_registered_user, parent, false);
            holder = new ViewHolder();
            holder.txtUserName = convertView.findViewById(R.id.txtUserName);
            holder.txtUserEmail = convertView.findViewById(R.id.txtUserEmail);
            holder.txtUserPhone = convertView.findViewById(R.id.txtUserPhone);
            holder.txtPetInfo = convertView.findViewById(R.id.txtPetInfo);
            holder.txtSpecialRequirements = convertView.findViewById(R.id.txtSpecialRequirements);
            holder.txtRegistrationDate = convertView.findViewById(R.id.txtRegistrationDate);
            holder.txtStatus = convertView.findViewById(R.id.txtStatus);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        EventRegistrationModel registration = registrationsList.get(position);

        holder.txtUserName.setText("👤 " + (registration.getUserName() != null ? registration.getUserName() : "Unknown User"));
        holder.txtUserEmail.setText("📧 " + (registration.getUserEmail() != null ? registration.getUserEmail() : "No email"));
        
        if (registration.getUserPhone() != null && !registration.getUserPhone().isEmpty()) {
            holder.txtUserPhone.setText("📱 " + registration.getUserPhone());
            holder.txtUserPhone.setVisibility(View.VISIBLE);
        } else {
            holder.txtUserPhone.setVisibility(View.GONE);
        }

        // Pet information
        if (registration.getPetName() != null && !registration.getPetName().isEmpty()) {
            String petInfo = "🐾 " + registration.getPetName();
            if (registration.getPetType() != null && !registration.getPetType().isEmpty()) {
                petInfo += " (" + registration.getPetType() + ")";
            }
            holder.txtPetInfo.setText(petInfo);
            holder.txtPetInfo.setVisibility(View.VISIBLE);
        } else {
            holder.txtPetInfo.setVisibility(View.GONE);
        }

        // Special requirements
        if (registration.getSpecialRequirements() != null && !registration.getSpecialRequirements().isEmpty()) {
            holder.txtSpecialRequirements.setText("📝 Special Requirements: " + registration.getSpecialRequirements());
            holder.txtSpecialRequirements.setVisibility(View.VISIBLE);
        } else {
            holder.txtSpecialRequirements.setVisibility(View.GONE);
        }

        // Registration date
        Date regDate = new Date(registration.getRegistrationTimestamp());
        holder.txtRegistrationDate.setText("📅 Registered: " + dateFormat.format(regDate));

        // Status
        String status = registration.getStatus() != null ? registration.getStatus() : "registered";
        switch (status) {
            case "registered":
                holder.txtStatus.setText("✅ Registered");
                holder.txtStatus.setTextColor(0xFF4CAF50);
                break;
            case "cancelled":
                holder.txtStatus.setText("❌ Cancelled");
                holder.txtStatus.setTextColor(0xFFF44336);
                break;
            case "attended":
                holder.txtStatus.setText("🎉 Attended");
                holder.txtStatus.setTextColor(0xFF2196F3);
                break;
            default:
                holder.txtStatus.setText("📋 " + status);
                holder.txtStatus.setTextColor(0xFF9E9E9E);
                break;
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView txtUserName;
        TextView txtUserEmail;
        TextView txtUserPhone;
        TextView txtPetInfo;
        TextView txtSpecialRequirements;
        TextView txtRegistrationDate;
        TextView txtStatus;
    }
}