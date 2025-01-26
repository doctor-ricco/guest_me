package com.example.guestme;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class HostActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host);

        // Only create new fragment if savedInstanceState is null
        if (savedInstanceState == null) {
            // Get intent extras
            boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
            String openFragment = getIntent().getStringExtra("openFragment");

            // Create the fragment
            HostHomeFragment fragment = new HostHomeFragment();
            
            // If we're editing, pass the bundle
            if (isEditing) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isEditing", true);
                fragment.setArguments(bundle);
            }

            // Replace container with fragment
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }
}


