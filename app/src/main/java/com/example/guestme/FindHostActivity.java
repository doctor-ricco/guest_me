package com.example.guestme;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FindHostActivity extends AppCompatActivity {
    private RecyclerView hostsRecyclerView;
    private HostAdapter hostAdapter;
    private ProgressBar progressBar;
    private TextView noHostsText;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<String> visitorPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_host);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        hostsRecyclerView = findViewById(R.id.hostsRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        noHostsText = findViewById(R.id.noHostsText);

        // Setup RecyclerView
        hostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        hostAdapter = new HostAdapter();
        hostsRecyclerView.setAdapter(hostAdapter);

        // Load visitor preferences and then find matching hosts
        loadVisitorPreferences();
    }

    private void loadVisitorPreferences() {
        String visitorId = auth.getCurrentUser().getUid();
        progressBar.setVisibility(View.VISIBLE);

        firestore.collection("users")
                .document(visitorId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists() && document.contains("preferences")) {
                        visitorPreferences = (List<String>) document.get("preferences");
                        findMatchingHosts();
                    } else {
                        progressBar.setVisibility(View.GONE);
                        noHostsText.setText("Please set your preferences first");
                        noHostsText.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error loading preferences: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
    }

    private void findMatchingHosts() {
        if (visitorPreferences == null || visitorPreferences.isEmpty()) {
            progressBar.setVisibility(View.GONE);
            noHostsText.setText("No preferences set");
            noHostsText.setVisibility(View.VISIBLE);
            Log.d("FindHostActivity", "Visitor has no preferences");
            return;
        }

        // Log visitor preferences
        Log.d("FindHostActivity", "Visitor preferences: " + visitorPreferences.toString());

        // First verify that the user is authenticated
        if (auth.getCurrentUser() == null) {
            progressBar.setVisibility(View.GONE);
            noHostsText.setText("Please login to find hosts");
            noHostsText.setVisibility(View.VISIBLE);
            return;
        }

        // Query hosts directly without the initial debug query
        firestore.collection("users")
                .whereEqualTo("type", "Host")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HostModel> matchingHosts = new ArrayList<>();
                    Log.d("FindHostActivity", "Found " + queryDocumentSnapshots.size() + " hosts total");

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        String hostId = document.getId();
                        String hostName = document.getString("fullName");
                        Log.d("FindHostActivity", "Checking host: " + hostId + " - " + hostName);
                        
                        Map<String, Object> hostData = document.getData();
                        Log.d("FindHostActivity", "Host data: " + hostData.toString());
                        
                        List<String> hostPreferences = null;
                        if (document.contains("preferences")) {
                            hostPreferences = (List<String>) document.get("preferences");
                        }
                        
                        Log.d("FindHostActivity", "Host preferences: " + 
                            (hostPreferences != null ? hostPreferences.toString() : "null"));
                        
                        if (hostPreferences != null && !hostPreferences.isEmpty()) {
                            double matchPercentage = calculateMatchPercentage(
                                visitorPreferences, hostPreferences);
                            
                            Log.d("FindHostActivity", "Match percentage for " + hostName + 
                                ": " + matchPercentage + "%");
                            
                            if (matchPercentage >= 70.0) {
                                HostModel host = new HostModel(
                                    document.getId(),
                                    document.getString("fullName"),
                                    document.getString("description"),
                                    document.getString("city"),
                                    document.getString("country"),
                                    document.getString("photoUrl"),
                                    matchPercentage
                                );
                                matchingHosts.add(host);
                                Log.d("FindHostActivity", "Added matching host: " + host.getFullName() + 
                                    " with " + matchPercentage + "% match");
                            }
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    
                    if (matchingHosts.isEmpty()) {
                        noHostsText.setText("No matching hosts found");
                        noHostsText.setVisibility(View.VISIBLE);
                        hostsRecyclerView.setVisibility(View.GONE);
                        Log.d("FindHostActivity", "No matching hosts found after processing");
                    } else {
                        noHostsText.setVisibility(View.GONE);
                        hostsRecyclerView.setVisibility(View.VISIBLE);
                        hostAdapter.setHosts(matchingHosts);
                        Log.d("FindHostActivity", "Found " + matchingHosts.size() + " matching hosts");
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    hostsRecyclerView.setVisibility(View.GONE);
                    noHostsText.setVisibility(View.VISIBLE);
                    
                    String errorMessage = "Error finding hosts. Please try again.";
                    if (e instanceof FirebaseFirestoreException) {
                        FirebaseFirestoreException firestoreException = (FirebaseFirestoreException) e;
                        if (firestoreException.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                            errorMessage = "Access denied. Please try again later.";
                        }
                    }
                    
                    noHostsText.setText(errorMessage);
                    Log.e("FindHostActivity", "Error finding hosts", e);
                });
    }

    private double calculateMatchPercentage(List<String> visitorPrefs, List<String> hostPrefs) {
        if (visitorPrefs == null || hostPrefs == null || 
            visitorPrefs.isEmpty() || hostPrefs.isEmpty()) {
            Log.d("FindHostActivity", "Null or empty preferences found");
            return 0.0;
        }

        int matches = 0;
        Log.d("FindHostActivity", "Comparing preferences:");
        Log.d("FindHostActivity", "Visitor prefs: " + visitorPrefs);
        Log.d("FindHostActivity", "Host prefs: " + hostPrefs);
        
        for (String pref : visitorPrefs) {
            if (hostPrefs.contains(pref)) {
                matches++;
                Log.d("FindHostActivity", "Match found: " + pref);
            }
        }
        
        double percentage = (matches * 100.0) / Math.max(visitorPrefs.size(), hostPrefs.size());
        Log.d("FindHostActivity", String.format(
            "Match calculation: %d matches out of %d preferences = %.2f%%",
            matches, Math.max(visitorPrefs.size(), hostPrefs.size()), percentage));
        
        return percentage;
    }
} 