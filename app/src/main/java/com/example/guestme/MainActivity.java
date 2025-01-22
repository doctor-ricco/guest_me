package com.example.guestme;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment navHostFragment = fragmentManager.findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            Log.d("MainActivity", "NavHostFragment encontrado: " + navHostFragment.getClass().getSimpleName());
        } else {
            Log.e("MainActivity", "NavHostFragment não encontrado!");
        }
    }

}

        // O MainActivity agora apenas hospeda o NavHostFragment.
        // Toda a lógica do "Bem-vindo!" e do botão Logout será movida para um fragmento.

