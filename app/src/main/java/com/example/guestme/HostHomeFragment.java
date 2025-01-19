package com.example.guestme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class HostHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_host_home, container, false);

        // Referenciar componentes do layout
        TextView welcomeMessage = view.findViewById(R.id.welcomeMessage);
        Button completeProfileButton = view.findViewById(R.id.completeProfileButton);
        Button savePreferencesButton = view.findViewById(R.id.savePreferencesButton);
        CheckBox preferenceCuisine = view.findViewById(R.id.preferenceCuisine);
        CheckBox preferenceHistory = view.findViewById(R.id.preferenceHistory);

        // Personalizar mensagem de boas-vindas
        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName(); // Ajuste isso se não estiver configurado
        welcomeMessage.setText("Bem-vindo(a), " + (userName != null ? userName : "Host") + "!");

        // Configurar botão para completar perfil
        completeProfileButton.setOnClickListener(v -> {
            Toast.makeText(getActivity(), "Complete seu perfil em breve!", Toast.LENGTH_SHORT).show();
            // Redirecionar para outra tela ou fragment para completar perfil
        });

        // Configurar botão para salvar preferências
        savePreferencesButton.setOnClickListener(v -> {
            List<String> preferences = new ArrayList<>();
            if (preferenceCuisine.isChecked()) preferences.add("Culinária");
            if (preferenceHistory.isChecked()) preferences.add("História");

            // Salvar preferências no Firestore
            FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .update("preferences", preferences)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Preferências salvas!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Erro ao salvar preferências.", Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
