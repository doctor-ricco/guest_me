package com.example.guestme;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class VisitorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor);

        // Exemplo de mensagem para debug
        setTitle("Bem-vindo, Visitor!");
    }
}
