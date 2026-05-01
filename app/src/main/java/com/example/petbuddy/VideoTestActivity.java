package com.example.petbuddy;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

public class VideoTestActivity extends Activity {
    private static final String TAG = "VideoTestActivity";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Test all training videos
        testVideo("Cross Legs Training", R.raw.dog_training_cross_legs);
        testVideo("Down Command Training", R.raw.dog_training_down);
        testVideo("Paw Shake Training", R.raw.dog_training_paw_shake);
        
        finish();
    }
    
    private void testVideo(String title, int videoResource) {
        try {
            // Test if resource exists
            java.io.InputStream inputStream = getResources().openRawResource(videoResource);
            int available = inputStream.available();
            inputStream.close();
            
            Log.d(TAG, "Video " + title + " - Size: " + available + " bytes");
            
            // Test URI creation
            Uri uri = Uri.parse("android.resource://" + getPackageName() + "/" + videoResource);
            Log.d(TAG, "Video " + title + " - URI: " + uri.toString());
            
            // Test intent creation
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/mp4");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                Log.d(TAG, "Video " + title + " - System player available");
            } else {
                Log.w(TAG, "Video " + title + " - No system player found");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing video " + title, e);
        }
    }
}