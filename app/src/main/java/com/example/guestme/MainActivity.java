package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Referência aos elementos do layout
        TextView welcomeText = findViewById(R.id.welcomeText);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Obter o email do usuário logado
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        welcomeText.setText("Bem-vindo, " + userEmail + "!");

        // Botão de Logout
        logoutButton.setOnClickListener(view -> {
            FirebaseAuth.getInstance().signOut(); // Logout do Firebase
            Toast.makeText(MainActivity.this, "Você saiu!", Toast.LENGTH_SHORT).show();

            // Redirecionar para a tela de login
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }
}
