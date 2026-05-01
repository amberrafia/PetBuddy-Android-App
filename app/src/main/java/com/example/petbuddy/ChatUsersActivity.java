package com.example.petbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
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

public class ChatUsersActivity extends AppCompatActivity {

    private ListView listViewUsers;
    private ArrayList<UserModel> usersList;
    private ChatUsersAdapter adapter;
    private DatabaseReference usersRef;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_users);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Messages");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        listViewUsers = findViewById(R.id.listViewChatUsers);
        usersList = new ArrayList<>();
        adapter = new ChatUsersAdapter(this, usersList);
        listViewUsers.setAdapter(adapter);

        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
        }

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        usersRef = database.getReference("users");

        // Set current user online
        setUserOnlineStatus(true);

        // Load users
        loadUsers();

        // Item click listener
        listViewUsers.setOnItemClickListener((parent, view, position, id) -> {
            UserModel selectedUser = usersList.get(position);
            openChatWith(selectedUser);
        });
    }

    private void loadUsers() {
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();

                for (DataSnapshot userSnapshot : snapshot.getChildren()) {
                    UserModel user = userSnapshot.getValue(UserModel.class);
                    if (user != null && !user.getUserId().equals(currentUserId)) {
                        usersList.add(user);
                    }
                }
                adapter.notifyDataSetChanged();

                if (usersList.isEmpty()) {
                    Toast.makeText(ChatUsersActivity.this, "No users available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatUsersActivity.this, "Failed to load users: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setUserOnlineStatus(boolean isOnline) {
        if (currentUserId != null) {
            usersRef.child(currentUserId).child("online").setValue(isOnline);
            usersRef.child(currentUserId).child("lastSeen").setValue(System.currentTimeMillis());
        }
    }

    private void openChatWith(UserModel user) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("userId", user.getUserId());
        intent.putExtra("userName", user.getName());
        intent.putExtra("isOnline", user.isOnline());
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUserOnlineStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setUserOnlineStatus(false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        setUserOnlineStatus(false);
    }
}
