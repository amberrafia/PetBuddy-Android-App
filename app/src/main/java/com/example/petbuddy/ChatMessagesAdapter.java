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

public class ChatMessagesAdapter extends ArrayAdapter<MessageModel> {
    private Context context;
    private ArrayList<MessageModel> messages;
    private String currentUserId;

    public ChatMessagesAdapter(Context context, ArrayList<MessageModel> messages, String currentUserId) {
        super(context, 0, messages);
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        MessageModel message = messages.get(position);

        // Determine if message is sent or received
        boolean isSentByMe = message.getSenderId().equals(currentUserId);

        if (isSentByMe) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_message_sent, parent, false);
        } else {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_item_message_received, parent, false);
        }

        TextView txtMessage = convertView.findViewById(R.id.txtMessage);
        TextView txtTime = convertView.findViewById(R.id.txtMessageTime);

        txtMessage.setText(message.getText());
        txtTime.setText(formatTime(message.getTimestamp()));

        return convertView;
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}
