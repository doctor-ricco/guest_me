package com.example.guestme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class PreferencesFragment extends Fragment {

    private CheckBox preferenceCuisine;
    private CheckBox preferenceHistory;
    private CheckBox preferenceNature;
    private Button savePreferencesButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar o layout do fragmento
        View view = inflater.inflate(R.layout.fragment_preferences, container, false);

        // Conectar os elementos do layout ao código Java
        preferenceCuisine = view.findViewById(R.id.preferenceCuisine);
        preferenceHistory = view.findViewById(R.id.preferenceHistory);
        preferenceNature = view.findViewById(R.id.preferenceNature);
        savePreferencesButton = view.findViewById(R.id.savePreferencesButton);

        // Configurar o clique do botão
        savePreferencesButton.setOnClickListener(v -> savePreferences());

        return view;
    }

    private void savePreferences() {
        // Lista para armazenar as preferências selecionadas
        List<String> selectedPreferences = new ArrayList<>();

        if (preferenceCuisine.isChecked()) {
            selectedPreferences.add("Cuisine");
        }
        if (preferenceHistory.isChecked()) {
            selectedPreferences.add("History");
        }
        if (preferenceNature.isChecked()) {
            selectedPreferences.add("Nature Walks");
        }

        // Mostrar as preferências selecionadas (apenas para teste, pode ser salvo no Firestore)
        if (selectedPreferences.isEmpty()) {
            Toast.makeText(getActivity(), "No preferences selected!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), "Selected Preferences: " + selectedPreferences, Toast.LENGTH_SHORT).show();
        }
    }
}
