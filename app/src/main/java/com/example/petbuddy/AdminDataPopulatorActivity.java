package com.example.petbuddy;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Admin activity to populate the database with real animal data and photos
 */
public class AdminDataPopulatorActivity extends AppCompatActivity {
    private static final String TAG = "AdminDataPopulator";
    
    private Button btnPopulateDogs, btnPopulateCats, btnPopulateAll;
    private TextView txtStatus;
    private ProgressBar progressBar;
    private RealAnimalDataPopulator dataPopulator;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_data_populator);
        
        initializeViews();
        setupClickListeners();
        
        dataPopulator = new RealAnimalDataPopulator(this);
    }
    
    private void initializeViews() {
        btnPopulateDogs = findViewById(R.id.btnPopulateDogs);
        btnPopulateCats = findViewById(R.id.btnPopulateCats);
        btnPopulateAll = findViewById(R.id.btnPopulateAll);
        txtStatus = findViewById(R.id.txtStatus);
        progressBar = findViewById(R.id.progressBar);
        
        // Set initial status
        txtStatus.setText("Ready to populate database with real animal data");
        progressBar.setVisibility(ProgressBar.GONE);
    }
    
    private void setupClickListeners() {
        btnPopulateDogs.setOnClickListener(v -> populateDogData());
        btnPopulateCats.setOnClickListener(v -> populateCatData());
        btnPopulateAll.setOnClickListener(v -> populateAllData());
    }
    
    private void populateDogData() {
        setLoadingState(true, "Populating dog data...");
        
        dataPopulator.populateRealDogData(new RealAnimalDataPopulator.PopulationCallback() {
            @Override
            public void onSuccess(int count) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Successfully added " + count + " dogs!");
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Added " + count + " dogs successfully!", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onPartialSuccess(int successCount, int errorCount) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Added " + successCount + " dogs, " + errorCount + " failed");
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Partial success: " + successCount + " added, " + errorCount + " failed", 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onFailure(Exception exception) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Failed to populate dog data: " + exception.getMessage());
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Failed to populate dog data", Toast.LENGTH_LONG).show();
                });
                Log.e(TAG, "Failed to populate dog data", exception);
            }
        });
    }
    
    private void populateCatData() {
        setLoadingState(true, "Populating cat data...");
        
        dataPopulator.populateRealCatData(new RealAnimalDataPopulator.PopulationCallback() {
            @Override
            public void onSuccess(int count) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Successfully added " + count + " cats!");
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Added " + count + " cats successfully!", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onPartialSuccess(int successCount, int errorCount) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Added " + successCount + " cats, " + errorCount + " failed");
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Partial success: " + successCount + " added, " + errorCount + " failed", 
                        Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onFailure(Exception exception) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Failed to populate cat data: " + exception.getMessage());
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Failed to populate cat data", Toast.LENGTH_LONG).show();
                });
                Log.e(TAG, "Failed to populate cat data", exception);
            }
        });
    }
    
    private void populateAllData() {
        setLoadingState(true, "Populating all animal data...");
        
        // First populate dogs, then cats
        dataPopulator.populateRealDogData(new RealAnimalDataPopulator.PopulationCallback() {
            @Override
            public void onSuccess(int dogCount) {
                txtStatus.setText("Dogs added successfully, now adding cats...");
                
                dataPopulator.populateRealCatData(new RealAnimalDataPopulator.PopulationCallback() {
                    @Override
                    public void onSuccess(int catCount) {
                        runOnUiThread(() -> {
                            setLoadingState(false, "Successfully added " + dogCount + " dogs and " + catCount + " cats!");
                            Toast.makeText(AdminDataPopulatorActivity.this, 
                                "All data populated successfully!", Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onPartialSuccess(int successCount, int errorCount) {
                        runOnUiThread(() -> {
                            setLoadingState(false, "Dogs: " + dogCount + ", Cats: " + successCount + " added, " + errorCount + " failed");
                            Toast.makeText(AdminDataPopulatorActivity.this, 
                                "Partial success with cats", Toast.LENGTH_LONG).show();
                        });
                    }
                    
                    @Override
                    public void onFailure(Exception exception) {
                        runOnUiThread(() -> {
                            setLoadingState(false, "Dogs added, but cats failed: " + exception.getMessage());
                            Toast.makeText(AdminDataPopulatorActivity.this, 
                                "Dogs added, but cats failed", Toast.LENGTH_LONG).show();
                        });
                    }
                });
            }
            
            @Override
            public void onPartialSuccess(int successCount, int errorCount) {
                // Continue with cats even if some dogs failed
                txtStatus.setText("Some dogs added, now adding cats...");
                populateCatData();
            }
            
            @Override
            public void onFailure(Exception exception) {
                runOnUiThread(() -> {
                    setLoadingState(false, "Failed to populate dog data: " + exception.getMessage());
                    Toast.makeText(AdminDataPopulatorActivity.this, 
                        "Failed to populate data", Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void setLoadingState(boolean isLoading, String statusText) {
        btnPopulateDogs.setEnabled(!isLoading);
        btnPopulateCats.setEnabled(!isLoading);
        btnPopulateAll.setEnabled(!isLoading);
        
        if (isLoading) {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        } else {
            progressBar.setVisibility(ProgressBar.GONE);
        }
        
        txtStatus.setText(statusText);
    }
}