package com.example.petbuddy;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.MediaController;
import android.widget.VideoView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    private VideoView videoView;
    private ProgressBar progressBar;
    private View loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        // Set up action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Get video details from intent
        String videoTitle = getIntent().getStringExtra("video_title");
        int videoResource = getIntent().getIntExtra("video_resource", -1);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(videoTitle != null ? videoTitle : "Training Video");
        }

        // Initialize views
        videoView = findViewById(R.id.videoView);
        progressBar = findViewById(R.id.progressBar);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        if (videoResource != -1) {
            playVideo(videoResource, videoTitle);
        } else {
            Toast.makeText(this, "Error: No video specified", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void playVideo(int videoResource, String title) {
        try {
            // Show loading overlay
            loadingOverlay.setVisibility(View.VISIBLE);
            
            // Create URI for the video resource
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResource);
            Log.d(TAG, "Playing video: " + title + " with URI: " + uri.toString());

            // Set up media controller with better styling
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setMediaController(mediaController);

            // Set video URI
            videoView.setVideoURI(uri);

            // Set up listeners
            videoView.setOnPreparedListener(mp -> {
                loadingOverlay.setVisibility(View.GONE);
                Log.d(TAG, "Video prepared, starting playback");
                
                // Auto-start the video
                videoView.start();
                
                // Show media controller briefly
                if (mediaController != null) {
                    mediaController.show(3000); // Show for 3 seconds
                }
                
                Toast.makeText(this, "Playing " + title, Toast.LENGTH_SHORT).show();
            });

            videoView.setOnErrorListener((mp, what, extra) -> {
                loadingOverlay.setVisibility(View.GONE);
                Log.e(TAG, "Video playback error: what=" + what + ", extra=" + extra);
                
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Video Error")
                    .setMessage("Unable to play " + title + ". The video file may be corrupted or in an unsupported format.")
                    .setPositiveButton("OK", (dialog, which) -> finish())
                    .show();
                return true;
            });

            videoView.setOnCompletionListener(mp -> {
                Log.d(TAG, "Video playback completed");
                
                new android.app.AlertDialog.Builder(this)
                    .setTitle("Video Completed")
                    .setMessage(title + " has finished playing.")
                    .setPositiveButton("Watch Again", (dialog, which) -> {
                        videoView.seekTo(0);
                        videoView.start();
                    })
                    .setNegativeButton("Close", (dialog, which) -> finish())
                    .show();
            });

            // Request focus and start preparing
            videoView.requestFocus();

        } catch (Exception e) {
            loadingOverlay.setVisibility(View.GONE);
            Log.e(TAG, "Error setting up video playback", e);
            
            new android.app.AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Failed to load " + title + ": " + e.getMessage())
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null && videoView.isPlaying()) {
            videoView.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoView != null && !videoView.isPlaying()) {
            videoView.resume();
        }
    }
}