package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class PreferencesActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // References to CheckBoxes
        CheckBox cuisineCheckBox = findViewById(R.id.preferenceCuisine);
        CheckBox historyCheckBox = findViewById(R.id.preferenceHistory);
        CheckBox natureCheckBox = findViewById(R.id.preferenceNature);
        CheckBox beachCheckBox = findViewById(R.id.preferenceBeach);
        CheckBox surfCheckBox = findViewById(R.id.preferenceSurf);
        CheckBox nocturneLifeCheckBox = findViewById(R.id.preferenceNocturneLife);
        CheckBox monumentsCheckBox = findViewById(R.id.preferenceMonuments);
        CheckBox shoppingCheckBox = findViewById(R.id.preferenceShoppingSale);

        findViewById(R.id.savePreferencesButton).setOnClickListener(view -> {
            List<String> preferences = new ArrayList<>();

            if (cuisineCheckBox.isChecked()) preferences.add("Cuisine");
            if (historyCheckBox.isChecked()) preferences.add("History");
            if (natureCheckBox.isChecked()) preferences.add("Nature Walks");
            if (beachCheckBox.isChecked()) preferences.add("Peaceful Beach");
            if (surfCheckBox.isChecked()) preferences.add("Beach for surfing");
            if (nocturneLifeCheckBox.isChecked()) preferences.add("Nocturne Lifestyle");
            if (monumentsCheckBox.isChecked()) preferences.add("Churches and Monuments");
            if (shoppingCheckBox.isChecked()) preferences.add("Shopping and Sale");

            if (preferences.isEmpty()) {
                Toast.makeText(this, "No preferences selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            savePreferences(preferences);
        });
    }

    private void savePreferences(List<String> preferences) {
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference userDocRef = db.collection("users").document(userId);

        // First check user type before saving preferences
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                String userType = task.getResult().getString("type");
                
                // Update preferences
                userDocRef.update("preferences", preferences)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();
                            
                            // Navigate based on user type
                            Intent intent;
                            if ("Host".equals(userType)) {
                                intent = new Intent(this, HostProfileActivity.class);
                            } else {
                                intent = new Intent(this, VisitorProfileActivity.class);
                            }
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Error saving preferences: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        });
            } else {
                Toast.makeText(this, "Error retrieving user type", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Classe auxiliar para salvar preferências no Firestore
    public static class UserPreferences {
        private List<String> preferences;

        public UserPreferences() {
        }

        public UserPreferences(List<String> preferences) {
            this.preferences = preferences;
        }

        public List<String> getPreferences() {
            return preferences;
        }

        public void setPreferences(List<String> preferences) {
            this.preferences = preferences;
        }
    }
}
