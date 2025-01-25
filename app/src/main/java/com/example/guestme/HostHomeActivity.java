package com.example.guestme;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class HostHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_home);

        // Verificar se estamos editando o perfil
        boolean isEditing = getIntent().getBooleanExtra("isEditing", false);

        // Passar o parâmetro para o fragmento
        Bundle bundle = new Bundle();
        bundle.putBoolean("isEditing", isEditing);

        HostHomeFragment fragment = new HostHomeFragment();
        fragment.setArguments(bundle);

        // Carregar o fragmento
        loadFragment(fragment);
    }

    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, fragment); // R.id.fragment_container é o ID do container no layout
        transaction.commit();
    }
}