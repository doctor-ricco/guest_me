package com.example.guestme;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // O MainActivity agora apenas hospeda o NavHostFragment.
        // Toda a lógica do "Bem-vindo!" e do botão Logout será movida para um fragmento.
    }
}
