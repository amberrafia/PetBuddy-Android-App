package com.example.petbuddy;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ChatUsersAdapter extends ArrayAdapter<UserModel> {
    private Context context;
    private ArrayList<UserModel> users;

    public ChatUsersAdapter(Context context, ArrayList<UserModel> users) {
        super(context, 0, users);
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_chat_user, parent, false);
        }

        UserModel user = users.get(position);

        TextView txtName = convertView.findViewById(R.id.txtUserName);
        TextView txtStatus = convertView.findViewById(R.id.txtUserStatus);
        View statusIndicator = convertView.findViewById(R.id.statusIndicator);

        txtName.setText(user.getName());

        if (user.isOnline()) {
            txtStatus.setText("Online");
            txtStatus.setTextColor(0xFF4CAF50); // Green
            statusIndicator.setBackgroundResource(R.drawable.status_online);
        } else {
            String lastSeen = getLastSeenTime(user.getLastSeen());
            txtStatus.setText("Last seen " + lastSeen);
            txtStatus.setTextColor(0xFF999999); // Gray
            statusIndicator.setBackgroundResource(R.drawable.status_offline);
        }

        return convertView;
    }

    private String getLastSeenTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + " min ago";
        } else if (hours < 24) {
            return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        } else if (days < 7) {
            return days + " day" + (days > 1 ? "s" : "") + " ago";
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        }
    }
}
