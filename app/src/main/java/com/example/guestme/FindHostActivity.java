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

import com.google.android.material.slider.Slider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;

public class FindHostActivity extends AppCompatActivity {
    private RecyclerView hostsRecyclerView;
    private HostAdapter hostAdapter;
    private ProgressBar progressBar;
    private TextView noHostsText;
    private TextView percentageText;
    private Slider matchSlider;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private List<String> visitorPreferences;
    private List<HostModel> allHosts; // Store all hosts
    private float currentMatchThreshold = 70f; // Default threshold
    private AutoCompleteTextView cityFilter;
    private String selectedCity = null;
    private Set<String> availableCities = new HashSet<>();

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
        matchSlider = findViewById(R.id.matchSlider);
        percentageText = findViewById(R.id.percentageText);

        // Setup RecyclerView
        hostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        hostAdapter = new HostAdapter();
        hostsRecyclerView.setAdapter(hostAdapter);

        // Setup slider
        setupMatchSlider();

        // Initialize city filter
        cityFilter = findViewById(R.id.cityFilter);
        setupCityFilter();

        // Load visitor preferences and then find matching hosts
        loadVisitorPreferences();
    }

    private void setupMatchSlider() {
        matchSlider.addOnChangeListener((slider, value, fromUser) -> {
            currentMatchThreshold = value;
            percentageText.setText(String.format(Locale.getDefault(), "%.0f%%", value));
            filterHosts();
        });
    }

    private void setupCityFilter() {
        cityFilter.setOnItemClickListener((parent, view, position, id) -> {
            selectedCity = (String) parent.getItemAtPosition(position);
            filterHosts();
        });

        // Add clear button (X) functionality
        cityFilter.setOnClickListener(v -> {
            if (!cityFilter.getText().toString().isEmpty()) {
                cityFilter.setText("");
                selectedCity = null;
                filterHosts();
            }
        });
    }

    private void updateCityAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            new ArrayList<>(availableCities)
        );
        cityFilter.setAdapter(adapter);
    }

    private void filterHosts() {
        if (allHosts == null) return;

        List<HostModel> filteredHosts = allHosts.stream()
                .filter(host -> host.getMatchPercentage() >= currentMatchThreshold)
                .filter(host -> selectedCity == null || 
                        selectedCity.equals(host.getCity()))
                .collect(Collectors.toList());

        updateHostsList(filteredHosts);
    }

    private void updateHostsList(List<HostModel> hosts) {
        if (hosts.isEmpty()) {
            String message = "No matching hosts found";
            if (selectedCity != null) {
                message += " in " + selectedCity;
            }
            noHostsText.setText(message);
            noHostsText.setVisibility(View.VISIBLE);
            hostsRecyclerView.setVisibility(View.GONE);
        } else {
            noHostsText.setVisibility(View.GONE);
            hostsRecyclerView.setVisibility(View.VISIBLE);
            hostAdapter.setHosts(hosts);
        }
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
                    allHosts = new ArrayList<>();
                    availableCities.clear(); // Clear existing cities

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
                            
                            String city = document.getString("city");
                            if (city != null && !city.isEmpty()) {
                                availableCities.add(city); // Add city to available cities
                            }

                            HostModel host = new HostModel(
                                document.getId(),
                                document.getString("fullName"),
                                document.getString("description"),
                                city,
                                document.getString("country"),
                                document.getString("photoUrl"),
                                matchPercentage
                            );
                            allHosts.add(host);
                        }
                    }

                    progressBar.setVisibility(View.GONE);
                    updateCityAdapter(); // Update city dropdown
                    filterHosts();
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