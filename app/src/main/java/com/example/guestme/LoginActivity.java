package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText emailInput = findViewById(R.id.emailInput);
        EditText passwordInput = findViewById(R.id.passwordInput);
        Button loginButton = findViewById(R.id.loginButton);

        loginButton.setOnClickListener(view -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();

            if (!email.isEmpty() && !password.isEmpty()) {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Obter o UID do usuário autenticado
                                String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                                // Ler o tipo de usuário no Firestore
                                FirebaseFirestore.getInstance().collection("users")
                                        .document(userId)
                                        .get()
                                        .addOnSuccessListener(document -> {
                                            if (document.exists()) {
                                                String userType = document.getString("type");

                                                // Redirecionar com base no tipo de usuário
                                                navigateBasedOnUserType(userId);
                                            } else {
                                                Toast.makeText(LoginActivity.this, "Usuário não encontrado no Firestore.", Toast.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(e -> Toast.makeText(LoginActivity.this, "Erro ao buscar dados: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                            } else {
                                Toast.makeText(this, "Erro: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            } else {
                Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show();
            }
        });

        Button registerButton = findViewById(R.id.registerButton);
        registerButton.setOnClickListener(view -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void navigateBasedOnUserType(String userId) {
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        firestore.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String userType = document.getString("type");
                        Intent intent;
                        if ("Host".equals(userType)) {
                            intent = new Intent(LoginActivity.this, HostProfileActivity.class);
                        } else {
                            // Always use VisitorProfileActivity for visitors
                            intent = new Intent(LoginActivity.this, VisitorProfileActivity.class);
                        }
                        startActivity(intent);
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    // ... error handling ...
                });
    }
}
