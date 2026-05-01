package com.example.petbuddy;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class AIChatActivity extends AppCompatActivity {

    private ListView listViewMessages;
    private EditText editMessage;
    private ImageButton btnSend, btnVoice;
    private Button btnHelp, btnRefresh, btnDelete;
    private TextView txtBotName, txtBotStatus;
    private ArrayList<MessageModel> messagesList;
    private ChatMessagesAdapter adapter;
    private DatabaseReference messagesRef;
    private String currentUserId, currentUserName;
    private String botId = "ai_bot";
    private String botName = "PetBuddy AI Assistant";
    
    // Voice recognition
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final int SPEECH_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("AI Chat");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize views
        listViewMessages = findViewById(R.id.listViewMessages);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        btnVoice = findViewById(R.id.btnVoice);
        btnHelp = findViewById(R.id.btnHelp);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnDelete = findViewById(R.id.btnDelete);
        txtBotName = findViewById(R.id.txtChatUserName);
        txtBotStatus = findViewById(R.id.txtChatUserStatus);

        txtBotName.setText(botName);
        txtBotStatus.setText("🤖 Always Online");
        txtBotStatus.setTextColor(0xFF4CAF50);

        // Button click listeners
        btnHelp.setOnClickListener(v -> showHelpMenu());
        btnRefresh.setOnClickListener(v -> refreshChats());
        btnDelete.setOnClickListener(v -> confirmDeleteChats());
        btnVoice.setOnClickListener(v -> startVoiceRecognition());

        messagesList = new ArrayList<>();
        
        // Get current user
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();
            currentUserName = auth.getCurrentUser().getDisplayName();
            if (currentUserName == null || currentUserName.isEmpty()) {
                currentUserName = auth.getCurrentUser().getEmail();
            }
        }

        adapter = new ChatMessagesAdapter(this, messagesList, currentUserId);
        listViewMessages.setAdapter(adapter);

        // Initialize Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://petbuddy2-b5ac4-default-rtdb.asia-southeast1.firebasedatabase.app/");
        messagesRef = database.getReference("aiChats").child(currentUserId).child("messages");

        // Load messages
        loadMessages();

        // Send button click
        btnSend.setOnClickListener(v -> sendMessage());

        // Send welcome message if first time
        checkAndSendWelcomeMessage();
    }

    private void startVoiceRecognition() {
        // Check if speech recognition is available
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check for audio permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_RECORD_AUDIO_PERMISSION);
            return;
        }

        // Create speech recognition intent
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "🎤 Speak your message to PetBuddy AI...");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);

        try {
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        } catch (Exception e) {
            Toast.makeText(this, "Speech recognition not supported", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startVoiceRecognition();
            } else {
                Toast.makeText(this, "Audio permission is required for voice search", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenText = results.get(0);
                editMessage.setText(spokenText);
                // Optionally send the message automatically
                // sendMessage();
                Toast.makeText(this, "Voice input: " + spokenText, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showOptionsMenu() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Chat Options")
                .setItems(new String[]{
                        "❓ What can I ask?",
                        "🗑️ Delete all chats"
                }, (dialog, which) -> {
                    if (which == 0) {
                        showHelpMenu();
                    } else if (which == 1) {
                        confirmDeleteChats();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showHelpMenu() {
        String helpText = "🤖 I can help you with:\n\n" +
                "📅 Pet Age Calculator\n" +
                "• Ask: 'how old is my pet' or 'age calculator'\n\n" +
                "🍖 Diet Planner\n" +
                "• Ask: 'diet plan' or 'feeding guide'\n\n" +
                "🏥 Symptom Checker\n" +
                "• Ask: 'symptom checker' or 'is my pet sick'\n\n" +
                "💉 Vaccination Reminder\n" +
                "• Ask: 'vaccination schedule' or 'vaccine reminder'\n\n" +
                "🏃 Exercise Plans\n" +
                "• Ask: 'exercise plan' or 'how much exercise'\n\n" +
                "🎾 Training Tips\n" +
                "• Ask: 'training tips' or 'how to train'\n\n" +
                "🍖 Nutrition Guide\n" +
                "• Ask: 'what to feed' or 'pet food'\n\n" +
                "🎓 Training Videos Tips\n" +
                "• Ask: 'training videos' or 'how to train'\n\n" +
                "🏠 Adoption Guide\n" +
                "• Ask: 'adopt a pet' or 'new pet'\n\n" +
                "Just type your question naturally!";

        new android.app.AlertDialog.Builder(this)
                .setTitle("What Can I Ask?")
                .setMessage(helpText)
                .setPositiveButton("Got it!", null)
                .show();
    }

    private void confirmDeleteChats() {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Delete All Chats")
                .setMessage("Are you sure you want to delete all chat history? This cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllChats())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllChats() {
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Deleting chats...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        messagesRef.removeValue()
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    messagesList.clear();
                    adapter.notifyDataSetChanged();
                    Toast.makeText(this, "All chats deleted successfully", Toast.LENGTH_SHORT).show();
                    // Send welcome message again
                    checkAndSendWelcomeMessage();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Failed to delete chats: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
                Toast.makeText(AIChatActivity.this, "Failed to load messages", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void checkAndSendWelcomeMessage() {
        messagesRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Send welcome message
                    sendBotMessage("Hello! 👋 I'm your PetBuddy AI Assistant. I'm here to help you with:\n\n" +
                            "🐕 Pet care & nutrition\n" +
                            "🏥 Health & symptom checker\n" +
                            "💉 Vaccination reminders\n" +
                            "🎾 Training tips\n" +
                            "🏃 Exercise plans\n" +
                            "🍖 Diet planner\n" +
                            "📅 Age calculator\n\n" +
                            "Ask me anything about your pets!");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void sendMessage() {
        String messageText = editMessage.getText().toString().trim();
        
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save user message
        long timestamp = System.currentTimeMillis();
        MessageModel userMessage = new MessageModel(currentUserId, currentUserName, botId, messageText, timestamp);

        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            messagesRef.child(messageId).setValue(userMessage)
                    .addOnSuccessListener(aVoid -> {
                        editMessage.setText("");
                        // Generate AI response
                        generateAIResponse(messageText);
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(AIChatActivity.this, "Failed to send message", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void generateAIResponse(String userMessage) {
        // Simulate AI thinking delay
        new android.os.Handler().postDelayed(() -> {
            String response = getAIResponse(userMessage.toLowerCase());
            sendBotMessage(response);
        }, 1000);
    }

    private String getAIResponse(String message) {
        // Age Calculator
        if (message.contains("age") && (message.contains("calculate") || message.contains("calculator") || message.contains("how old"))) {
            return "📅 Pet Age Calculator:\n\n" +
                    "Dog years to human years:\n" +
                    "• 1 year = 15 human years\n" +
                    "• 2 years = 24 human years\n" +
                    "• 3+ years = Add 4-5 years per year\n\n" +
                    "Cat years to human years:\n" +
                    "• 1 year = 15 human years\n" +
                    "• 2 years = 24 human years\n" +
                    "• 3+ years = Add 4 years per year\n\n" +
                    "Example: A 5-year-old dog is about 36-40 human years old!";
        }
        
        // Diet Planner
        if (message.contains("diet") && (message.contains("plan") || message.contains("planner") || message.contains("weight"))) {
            return "🍖 Pet Diet Planner:\n\n" +
                    "Dogs:\n" +
                    "• Small (< 20 lbs): 1-1.5 cups/day\n" +
                    "• Medium (20-50 lbs): 2-3 cups/day\n" +
                    "• Large (> 50 lbs): 3-4 cups/day\n\n" +
                    "Cats:\n" +
                    "• Average cat: 1/3 to 1/2 cup/day\n" +
                    "• Adjust based on activity level\n\n" +
                    "Tips:\n" +
                    "• Split into 2-3 meals\n" +
                    "• Monitor weight weekly\n" +
                    "• Adjust portions as needed\n" +
                    "• Consult vet for specific needs";
        }
        
        // Symptom Checker
        if (message.contains("symptom") || message.contains("check") && (message.contains("sick") || message.contains("ill"))) {
            return "🏥 Pet Symptom Checker:\n\n" +
                    "⚠️ URGENT - See vet immediately:\n" +
                    "• Difficulty breathing\n" +
                    "• Seizures or collapse\n" +
                    "• Severe bleeding\n" +
                    "• Unconsciousness\n" +
                    "• Bloated abdomen\n\n" +
                    "⚡ See vet within 24 hours:\n" +
                    "• Vomiting/diarrhea (multiple times)\n" +
                    "• Not eating for 24+ hours\n" +
                    "• Lethargy or weakness\n" +
                    "• Limping or pain\n\n" +
                    "📞 Call your vet for guidance!";
        }
        
        // Vaccination Reminder
        if (message.contains("vaccine") || message.contains("vaccination") || message.contains("shot") || message.contains("reminder")) {
            return "💉 Vaccination Reminder:\n\n" +
                    "Dogs:\n" +
                    "• 6-8 weeks: Distemper, Parvovirus\n" +
                    "• 10-12 weeks: DHPP booster\n" +
                    "• 16-18 weeks: Rabies\n" +
                    "• Annual: DHPP, Rabies (every 1-3 years)\n\n" +
                    "Cats:\n" +
                    "• 6-8 weeks: FVRCP\n" +
                    "• 12 weeks: FVRCP booster\n" +
                    "• 16 weeks: Rabies\n" +
                    "• Annual: FVRCP, Rabies (every 1-3 years)\n\n" +
                    "💡 Set reminders in your calendar!\n" +
                    "Consult your vet for personalized schedule!";
        }
        
        // Exercise Plans
        if (message.contains("exercise") || message.contains("walk") || message.contains("play") || message.contains("activity")) {
            return "🏃 Exercise Plans:\n\n" +
                    "Dogs by size:\n" +
                    "• Small breeds: 30 min daily\n" +
                    "  - 2 short walks + playtime\n" +
                    "• Medium breeds: 1 hour daily\n" +
                    "  - 2 walks + active play\n" +
                    "• Large breeds: 1-2 hours daily\n" +
                    "  - 2-3 walks + running/fetch\n\n" +
                    "Cats:\n" +
                    "• 15-30 min play sessions (2x daily)\n" +
                    "• Interactive toys (laser, feathers)\n" +
                    "• Climbing structures\n" +
                    "• Window perches\n\n" +
                    "💡 Adjust based on age, health & weather!";
        }
        
        // Training Tips (Enhanced)
        if (message.contains("train") || message.contains("behavior") || message.contains("obedience") || message.contains("command")) {
            return "🎾 Training Tips:\n\n" +
                    "Basic Commands:\n" +
                    "• Sit, Stay, Come, Down, Leave it\n" +
                    "• Start with one command at a time\n\n" +
                    "Training Methods:\n" +
                    "• Use positive reinforcement\n" +
                    "• Reward immediately (treats/praise)\n" +
                    "• Be consistent with commands\n" +
                    "• Keep sessions short (10-15 min)\n" +
                    "• Practice daily\n\n" +
                    "Tips:\n" +
                    "• Start training at 8-12 weeks\n" +
                    "• Be patient and persistent\n" +
                    "• Never use punishment\n" +
                    "• End on a positive note\n\n" +
                    "Consider professional training classes!";
        }
        
        // Pet care responses
        if (message.contains("food") || message.contains("feed") || message.contains("eat") || message.contains("nutrition")) {
            return "🍖 Pet Nutrition Guide:\n\n" +
                    "Dogs:\n" +
                    "• Feed 2-3 times daily\n" +
                    "• Quality dog food (age-appropriate)\n" +
                    "• Avoid: chocolate, grapes, onions, garlic\n\n" +
                    "Cats:\n" +
                    "• Feed 2 times daily\n" +
                    "• High-protein cat food\n" +
                    "• Avoid: milk, onions, garlic, raw fish\n\n" +
                    "General:\n" +
                    "• Fresh water always available\n" +
                    "• Treats < 10% of daily calories\n" +
                    "• Monitor weight regularly\n\n" +
                    "Consult vet for specific dietary needs!";
        }
        
        if (message.contains("sick") || message.contains("ill") || message.contains("disease") || message.contains("health")) {
            return "🏥 Health Concerns:\n\n" +
                    "Common signs of illness:\n" +
                    "• Loss of appetite\n" +
                    "• Lethargy or weakness\n" +
                    "• Vomiting or diarrhea\n" +
                    "• Difficulty breathing\n" +
                    "• Excessive thirst/urination\n" +
                    "• Behavioral changes\n\n" +
                    "⚠️ Please consult a veterinarian immediately if you notice these symptoms!\n\n" +
                    "For symptom checker, ask: 'symptom checker'";
        }
        
        if (message.contains("train") || message.contains("training") || message.contains("videos")) {
            return "🎓 Training Videos Guide:\n\n" +
                    "Regular training:\n" +
                    "• Practice commands daily (sit, stay, come)\n" +
                    "• Use positive reinforcement\n" +
                    "• Trim nails monthly\n" +
                    "• Clean ears weekly\n" +
                    "• Brush teeth 2-3 times per week\n" +
                    "• Check for fleas/ticks regularly\n\n" +
                    "Benefits:\n" +
                    "• Reduces shedding\n" +
                    "• Prevents matting\n" +
                    "• Early detection of health issues\n" +
                    "• Bonding time with your pet";
        }
        
        if (message.contains("adopt") || message.contains("new pet") || message.contains("puppy") || message.contains("kitten")) {
            return "🏠 Adopting a New Pet:\n\n" +
                    "Before adoption:\n" +
                    "• Research breed characteristics\n" +
                    "• Prepare your home (pet-proof)\n" +
                    "• Budget for expenses (food, vet, supplies)\n" +
                    "• Consider your lifestyle\n\n" +
                    "First week:\n" +
                    "• Schedule vet checkup\n" +
                    "• Establish routine (feeding, walks)\n" +
                    "• Start basic training\n" +
                    "• Provide safe space\n" +
                    "• Introduce slowly to family\n\n" +
                    "Welcome to pet parenthood! 🎉";
        }
        
        if (message.contains("hello") || message.contains("hi") || message.contains("hey")) {
            return "Hello! 👋 How can I help you with your pet today?\n\n" +
                    "I can help with:\n" +
                    "• Pet care & nutrition\n" +
                    "• Health & symptom checker\n" +
                    "• Vaccination reminders\n" +
                    "• Training tips\n" +
                    "• Exercise plans\n" +
                    "• Diet planner\n" +
                    "• Age calculator\n\n" +
                    "Just ask me anything!";
        }
        
        if (message.contains("thank") || message.contains("thanks")) {
            return "You're welcome! 😊 I'm always here to help with your pet care questions. Feel free to ask anything else!";
        }
        
        // Default response
        String[] defaultResponses = {
            "That's an interesting question! For specific concerns, I recommend consulting with a veterinarian. Is there anything else I can help you with?",
            "I'd be happy to help! Could you provide more details about your pet's situation?",
            "Great question! For the best advice, please consult your veterinarian. Meanwhile, is there anything general I can help with?",
            "I'm here to provide general pet care guidance. For specific medical concerns, please contact a vet. What else can I assist you with?"
        };
        
        return defaultResponses[new Random().nextInt(defaultResponses.length)];
    }

    private void sendBotMessage(String messageText) {
        long timestamp = System.currentTimeMillis();
        MessageModel botMessage = new MessageModel(botId, botName, currentUserId, messageText, timestamp);

        String messageId = messagesRef.push().getKey();
        if (messageId != null) {
            messagesRef.child(messageId).setValue(botMessage);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "❓ What can I ask?");
        menu.add(0, 2, 0, "🗑️ Delete all chats");
        menu.add(0, 3, 0, "🔄 Refresh");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                showHelpMenu();
                return true;
            case 2:
                confirmDeleteChats();
                return true;
            case 3:
                refreshChats();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshChats() {
        Toast.makeText(this, "Refreshing chats...", Toast.LENGTH_SHORT).show();
        loadMessages();
    }
}
