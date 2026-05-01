package com.example.petbuddy;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.ArrayList;

public class EventsAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<EventModel> eventsList;
    private String currentUserId;
    private OnEventRegistrationListener registrationListener;

    public interface OnEventRegistrationListener {
        void onRegisterClicked(EventModel event);
    }

    public EventsAdapter(Context context, ArrayList<EventModel> eventsList, 
                        String currentUserId, OnEventRegistrationListener listener) {
        this.context = context;
        this.eventsList = eventsList;
        this.currentUserId = currentUserId;
        this.registrationListener = listener;
    }

    @Override
    public int getCount() {
        return eventsList.size();
    }

    @Override
    public Object getItem(int position) {
        return eventsList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_event, parent, false);
            holder = new ViewHolder();
            holder.txtTitle = convertView.findViewById(R.id.txtEventTitle);
            holder.txtCategory = convertView.findViewById(R.id.txtEventCategory);
            holder.txtDescription = convertView.findViewById(R.id.txtEventDescription);
            holder.txtDateTime = convertView.findViewById(R.id.txtEventDateTime);
            holder.txtLocation = convertView.findViewById(R.id.txtEventLocation);
            holder.txtOrganizer = convertView.findViewById(R.id.txtEventOrganizer);
            holder.txtParticipants = convertView.findViewById(R.id.txtEventParticipants);
            holder.txtRequirements = convertView.findViewById(R.id.txtEventRequirements);
            holder.btnRegister = convertView.findViewById(R.id.btnRegisterEvent);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        EventModel event = eventsList.get(position);

        holder.txtTitle.setText(event.getTitle());
        holder.txtCategory.setText(event.getCategoryDisplayName());
        holder.txtDescription.setText(event.getDescription());
        holder.txtDateTime.setText("📅 " + event.getDate() + " • ⏰ " + event.getTime());
        holder.txtLocation.setText("📍 " + event.getLocation());
        holder.txtOrganizer.setText("👥 Organized by: " + event.getOrganizer());
        
        int availableSlots = event.getMaxParticipants() - event.getCurrentParticipants();
        holder.txtParticipants.setText("👥 " + event.getCurrentParticipants() + "/" + 
                                     event.getMaxParticipants() + " participants • " + 
                                     availableSlots + " slots available");

        if (event.getRequirements() != null && !event.getRequirements().isEmpty()) {
            holder.txtRequirements.setText("📋 Requirements: " + event.getRequirements());
            holder.txtRequirements.setVisibility(View.VISIBLE);
        } else {
            holder.txtRequirements.setVisibility(View.GONE);
        }

        // Set category color
        int categoryColor;
        switch (event.getCategory()) {
            case "pet_activity":
                categoryColor = 0xFF4CAF50; // Green
                break;
            case "awareness_program":
                categoryColor = 0xFF2196F3; // Blue
                break;
            case "community_event":
                categoryColor = 0xFFFF9800; // Orange
                break;
            default:
                categoryColor = 0xFF9E9E9E; // Gray
                break;
        }
        holder.txtCategory.setTextColor(categoryColor);

        // Configure register button
        if (event.hasAvailableSlots() && event.isRegistrationOpen()) {
            holder.btnRegister.setEnabled(true);
            holder.btnRegister.setText("Register");
            holder.btnRegister.setBackgroundColor(0xFF4CAF50);
        } else if (!event.hasAvailableSlots()) {
            holder.btnRegister.setEnabled(false);
            holder.btnRegister.setText("Full");
            holder.btnRegister.setBackgroundColor(0xFF9E9E9E);
        } else {
            holder.btnRegister.setEnabled(false);
            holder.btnRegister.setText("Closed");
            holder.btnRegister.setBackgroundColor(0xFF9E9E9E);
        }

        holder.btnRegister.setOnClickListener(v -> {
            if (registrationListener != null) {
                registrationListener.onRegisterClicked(event);
            }
        });

        // Make the entire card clickable to view details
        convertView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EventDetailsActivity.class);
            intent.putExtra("eventId", event.getEventId());
            context.startActivity(intent);
        });

        return convertView;
    }

    private static class ViewHolder {
        TextView txtTitle;
        TextView txtCategory;
        TextView txtDescription;
        TextView txtDateTime;
        TextView txtLocation;
        TextView txtOrganizer;
        TextView txtParticipants;
        TextView txtRequirements;
        Button btnRegister;
    }
}