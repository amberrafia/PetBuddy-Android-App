package com.example.petbuddy;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private ListView listViewMessages;
    private EditText editMessage;
    private ImageButton btnSend;
    private TextView txtUserName, txtUserStatus;
    private ArrayList<MessageModel> messagesList;
    private ChatMessagesAdapter adapter;
    private DatabaseReference messagesRef, usersRef;
    private String currentUserId, currentUserName;
    private String otherUserId, otherUserName;
    private boolean otherUserOnline;
    private String chatId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get intent data
        otherUserId = getIntent().getStringExtra("userId");
        otherUserName = getIntent().getStringExtra("userName");
        otherUserOnline = getIntent().getBooleanExtra("isOnline", false);

        // Initialize views
        listViewMessages = findViewById(R.id.listViewMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        txtUserName = findViewById(R.id.txtChatUserName);
        txtUserStatus = findViewById(R.id.txtChatUserStatus);

        txtUserName.setText(otherUserName);
        updateUserStatus(otherUserOnline);

        messagesList = new ArrayList<>();
        adapter = new ChatMessagesAdapter(this, messagesList, currentUserId);
        listViewMessages.setAdapter(adapter);

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            currentUserName = auth.getCurrentUser().getDisplayName();
            if (currentUserName == null || currentUserName.isEmpty()) {
                currentUserName = auth.getCurrentUser().getEmail();
            }
        }

        // Create chat ID (consistent for both users)
        chatId = createChatId(currentUserId, otherUserId);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        messagesRef = database.getReference("chats").child(chatId).child("messages");
        usersRef = database.getReference("users");

        // Load messages
        loadMessages();

        // Monitor other user's online status
        monitorUserStatus();

        // Send button click
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private String createChatId(String userId1, String userId2) {
        // Create consistent chat ID regardless of who initiates
        return userId1.compareTo(userId2) < 0 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }

    private void loadMessages() {
        messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesList.clear();

                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    MessageModel message = msgSnapshot.getValue(MessageModel.class);
                    if (message != null) {
                        message.setMessageId(msgSnapshot.getKey());
                        messagesList.add(message);
                    }
                }
                adapter.notifyDataSetChanged();
                
                // Scroll to bottom
                if (!messagesList.isEmpty()) {
                    listViewMessages.setSelection(messagesList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void monitorUserStatus() {
        usersRef.child(otherUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel user = snapshot.getValue(UserModel.class);
                if (user != null) {
                    updateUserStatus(user.isOnline());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void updateUserStatus(boolean isOnline) {
        if (isOnline) {
            txtUserStatus.setText("Online");
            txtUserStatus.setTextColor(0xFF4CAF50); // Green
        } else {
            txtUserStatus.setText("Offline");
            txtUserStatus.setTextColor(0xFF999999); // Gray
        }
    }

    private void sendMessage() {
        String messageText = editMessage.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        long timestamp = System.currentTimeMillis();
        MessageModel message = new MessageModel(currentUserId, currentUserName, otherUserId, messageText, timestamp);

        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            messagesRef.child(messageId).setValue(message)
                    .addOnSuccessListener(aVoid -> {
                        editMessage.setText("");
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(ChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
