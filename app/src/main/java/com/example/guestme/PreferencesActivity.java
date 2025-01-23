package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        // Inicializar Firebase Firestore
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();

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
            } else {
                // Obter o ID do usuário autenticado
                String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

                if (userId == null) {
                    Toast.makeText(this, "User not authenticated!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Atualizar os dados existentes no Firestore
                db.collection("users")
                        .document(userId)
                        .update("preferences", preferences, "timestamp", System.currentTimeMillis())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "Preferences updated successfully!", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            // Se falhar, tentar criar o documento com merge
                            Map<String, Object> userPreferences = new HashMap<>();
                            userPreferences.put("preferences", preferences);
                            userPreferences.put("timestamp", System.currentTimeMillis());

                            db.collection("users")
                                    .document(userId)
                                    .set(userPreferences)
                                    .addOnSuccessListener(aVoid2 -> {
                                        Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show();

                                      
                                        Intent intent = new Intent(PreferencesActivity.this, HostProfileActivity.class);
                                        startActivity(intent);
                                        finish(); // Opcional, para evitar que o usuário volte para esta tela


                                    })
                                    .addOnFailureListener(e2 -> {
                                        Toast.makeText(this, "Error saving preferences: " + e2.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });
            }
        });
    }
}
