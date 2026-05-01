package com.example.petbuddy;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.VideoView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class TrainingVideosActivity extends AppCompatActivity {
    private static final String TAG = "TrainingVideosActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training_videos);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Training Videos");
        }

        // Test video resources
        testVideoResources();

        // Check if specific video was requested
        String videoTitle = getIntent().getStringExtra("video_title");
        int videoResource = getIntent().getIntExtra("video_resource", -1);
        
        Log.d(TAG, "Video title: " + videoTitle + ", Resource: " + videoResource);
        
        if (videoTitle != null && videoResource != -1) {
            // Play specific video directly
            playSpecificVideo(videoTitle, videoResource);
            return;
        }

        try {
            // Setup new training video cards
            setupVideoCard(R.id.cardCrossLegsTraining, R.id.videoCrossLegs, R.id.imgCrossLegsThumb, 
                          R.id.imgCrossLegsPlay, R.raw.dog_training_cross_legs, "Cross Legs Training");
            
            setupVideoCard(R.id.cardDownTraining, R.id.videoDown, R.id.imgDownThumb, 
                          R.id.imgDownPlay, R.raw.dog_training_down, "Down Command Training");
            
            setupVideoCard(R.id.cardPawShakeTraining, R.id.videoPawShake, R.id.imgPawShakeThumb, 
                          R.id.imgPawShakePlay, R.raw.dog_training_paw_shake, "Paw Shake Training");
                          
        } catch (Exception e) {
            Log.e(TAG, "Error setting up video cards", e);
            Toast.makeText(this, "Loading training videos...", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void testVideoResources() {
        try {
            // Test if video resources are accessible
            int[] videoResources = {
                R.raw.dog_training_cross_legs,
                R.raw.dog_training_down,
                R.raw.dog_training_paw_shake
            };
            
            String[] videoNames = {
                "Cross Legs Training",
                "Down Command Training", 
                "Paw Shake Training"
            };
            
            for (int i = 0; i < videoResources.length; i++) {
                try {
                    Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResources[i]);
                    Log.d(TAG, "Video resource " + videoNames[i] + " URI: " + uri.toString());
                    
                    // Try to open input stream to verify resource exists
                    java.io.InputStream inputStream = getResources().openRawResource(videoResources[i]);
                    int available = inputStream.available();
                    inputStream.close();
                    Log.d(TAG, "Video resource " + videoNames[i] + " size: " + available + " bytes");
                } catch (Exception e) {
                    Log.e(TAG, "Error accessing video resource " + videoNames[i], e);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error testing video resources", e);
        }
    }

    private void playSpecificVideo(String title, int videoResource) {
        try {
            Log.d(TAG, "Playing specific video: " + title + " with resource: " + videoResource);
            openInAppVideoPlayer(title, videoResource);
        } catch (Exception e) {
            Log.e(TAG, "Error playing video: " + title, e);
            Toast.makeText(this, "Error loading video: " + title, Toast.LENGTH_SHORT).show();
        }
    }

    private void setupVideoCard(int cardId, int videoId, int thumbId, int playId, int videoResource, String title) {
        try {
            VideoView videoView = findViewById(videoId);
            ImageView thumbnail = findViewById(thumbId);
            ImageView playButton = findViewById(playId);

            Log.d(TAG, "Setting up video card for: " + title);

            if (videoView != null && thumbnail != null && playButton != null) {
                videoView.setVisibility(View.INVISIBLE);
                
                // Direct click handler that opens in-app video player immediately
                playButton.setOnClickListener(v -> {
                    try {
                        Log.d(TAG, "Play button clicked for: " + title);
                        openInAppVideoPlayer(title, videoResource);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in play button click for: " + title, e);
                        Toast.makeText(this, "Error playing video: " + title, Toast.LENGTH_SHORT).show();
                    }
                });
                
                // Also make the entire card clickable
                findViewById(cardId).setOnClickListener(v -> {
                    try {
                        Log.d(TAG, "Video card clicked for: " + title);
                        openInAppVideoPlayer(title, videoResource);
                    } catch (Exception e) {
                        Log.e(TAG, "Error in card click for: " + title, e);
                        Toast.makeText(this, "Error playing video: " + title, Toast.LENGTH_SHORT).show();
                    }
                });
                
            } else {
                Log.w(TAG, "Some views not found for video card: " + title);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up video card for: " + title, e);
        }
    }
    
    private void openInAppVideoPlayer(String title, int videoResource) {
        try {
            Log.d(TAG, "Opening in-app video player for: " + title);
            android.content.Intent intent = new android.content.Intent(this, VideoPlayerActivity.class);
            intent.putExtra("video_title", title);
            intent.putExtra("video_resource", videoResource);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening in-app video player: " + title, e);
            Toast.makeText(this, "Error playing video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void openVideoInSystemPlayer(String title, int videoResource) {
        try {
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResource);
            Log.d(TAG, "Opening video in system player: " + uri.toString());
            
            // Try multiple approaches to open the video
            boolean videoOpened = false;
            
            // Approach 1: Try with specific video/mp4 MIME type
            try {
                android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "video/mp4");
                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                
                if (intent.resolveActivity(getPackageManager()) != null) {
                    Toast.makeText(this, "Opening " + title + "...", Toast.LENGTH_SHORT).show();
                    startActivity(intent);
                    videoOpened = true;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to open with video/mp4 MIME type", e);
            }
            
            // Approach 2: Try with generic video/* MIME type
            if (!videoOpened) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "video/*");
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        Toast.makeText(this, "Opening " + title + "...", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        videoOpened = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to open with video/* MIME type", e);
                }
            }
            
            // Approach 3: Try with ACTION_SEND to share the video
            if (!videoOpened) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_SEND);
                    intent.setType("video/mp4");
                    intent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    
                    android.content.Intent chooser = android.content.Intent.createChooser(intent, "Open " + title + " with:");
                    if (chooser.resolveActivity(getPackageManager()) != null) {
                        Toast.makeText(this, "Choose app to play " + title, Toast.LENGTH_SHORT).show();
                        startActivity(chooser);
                        videoOpened = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to open with ACTION_SEND", e);
                }
            }
            
            // Approach 4: Try opening with browser
            if (!videoOpened) {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(uri);
                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        Toast.makeText(this, "Opening " + title + " in browser...", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                        videoOpened = true;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Failed to open with browser", e);
                }
            }
            
            // If all approaches failed, show options dialog
            if (!videoOpened) {
                showVideoPlayerOptions(title, videoResource);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening video in system player: " + title, e);
            Toast.makeText(this, "Error opening video: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showVideoPlayerOptions(String title, int videoResource) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Choose Video Player")
            .setMessage("No external video player found. How would you like to play " + title + "?")
            .setPositiveButton("In-App Player", (dialog, which) -> {
                try {
                    android.content.Intent intent = new android.content.Intent(this, VideoPlayerActivity.class);
                    intent.putExtra("video_title", title);
                    intent.putExtra("video_resource", videoResource);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening in-app player: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            })
            .setNeutralButton("Install Player", (dialog, which) -> {
                showVideoPlayerInstallDialog(title);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void showVideoPlayerInstallDialog(String title) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("No Video Player Found")
            .setMessage("To play " + title + ", please install a video player app like:\n\n" +
                       "• VLC Media Player\n" +
                       "• MX Player\n" +
                       "• Google Photos\n" +
                       "• Any other video player\n\n" +
                       "You can download these from Google Play Store.")
            .setPositiveButton("Open Play Store", (dialog, which) -> {
                try {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse("market://search?q=video%20player"));
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    } else {
                        // Fallback to web browser
                        intent.setData(android.net.Uri.parse("https://play.google.com/store/search?q=video%20player"));
                        startActivity(intent);
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Please install a video player from Play Store", Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}