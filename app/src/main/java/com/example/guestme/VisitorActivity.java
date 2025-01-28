package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class VisitorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor);

        if (savedInstanceState == null) {
            boolean isEditing = getIntent().getBooleanExtra("isEditing", false);
            
            VisitorHomeFragment fragment = new VisitorHomeFragment();
            
            if (isEditing) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isEditing", true);
                fragment.setArguments(bundle);
            }

            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, VisitorProfileActivity.class);
        startActivity(intent);
        finish();
    }
}
