package com.example.guestme;

import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class PreferencesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        CheckBox cuisineCheckBox = findViewById(R.id.preferenceCuisine);
        CheckBox historyCheckBox = findViewById(R.id.preferenceHistory);
        CheckBox natureCheckBox = findViewById(R.id.preferenceNature);
        findViewById(R.id.savePreferencesButton).setOnClickListener(view -> {
            List<String> preferences = new ArrayList<>();

            if (cuisineCheckBox.isChecked()) preferences.add("Cuisine");
            if (historyCheckBox.isChecked()) preferences.add("History");
            if (natureCheckBox.isChecked()) preferences.add("Nature Walks");

            if (preferences.isEmpty()) {
                Toast.makeText(this, "No preferences selected!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Preferences saved: " + preferences, Toast.LENGTH_SHORT).show();
                // Salvar as preferências no Firestore ou fazer outro processamento
            }
        });
    }
}
