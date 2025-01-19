package com.example.guestme;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class HostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        // Substituir o conteúdo do fragment_container pelo HostHomeFragment
        if (savedInstanceState == null) { // Garantir que o fragmento não seja adicionado duas vezes
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HostHomeFragment())
                    .commit();
        }
    }
}


