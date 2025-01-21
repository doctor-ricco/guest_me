package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_welcome, container, false);

        // Referenciar os elementos do layout do fragmento
        TextView welcomeText = view.findViewById(R.id.welcomeText);
        Button logoutButton = view.findViewById(R.id.logoutButton);

        // Obter o email do usuário logado
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        welcomeText.setText("Bem-vindo, " + userEmail + "!");

        // Configurar o botão de logout
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut(); // Logout do Firebase
            Toast.makeText(getActivity(), "Você saiu!", Toast.LENGTH_SHORT).show();

            // Redirecionar para a tela de login
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }
}
