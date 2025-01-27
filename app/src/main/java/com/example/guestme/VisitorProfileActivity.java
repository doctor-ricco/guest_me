package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.guestme.utils.CountryUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;

public class VisitorProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private PhoneNumberUtil phoneNumberUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitor_profile);

        CircleImageView profileImage = findViewById(R.id.profileImage);
        TextView fullNameText = findViewById(R.id.fullNameText);
        TextView descriptionText = findViewById(R.id.descriptionText);
        TextView preferencesText = findViewById(R.id.preferencesText);
        TextView locationText = findViewById(R.id.locationText);
        TextView phoneText = findViewById(R.id.phoneText);
        Button editProfileButton = findViewById(R.id.editProfileButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        phoneNumberUtil = PhoneNumberUtil.createInstance(this);

        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "No user data found.", Toast.LENGTH_SHORT).show();
            return;
        }

        loadProfileData(userId, profileImage, fullNameText, descriptionText, 
            preferencesText, locationText, phoneText);

        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, VisitorActivity.class);
            intent.putExtra("isEditing", true);
            startActivity(intent);
        });

        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileData(String userId, CircleImageView profileImage, 
        TextView fullNameText, TextView descriptionText, TextView preferencesText, 
        TextView locationText, TextView phoneText) {
        
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        updateUI(document, profileImage, fullNameText, descriptionText, 
                            preferencesText, locationText, phoneText);
                    }
                })
                .addOnFailureListener(e -> 
                    Toast.makeText(this, "Error loading profile: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show());
    }

    private void updateUI(DocumentSnapshot document, CircleImageView profileImage, 
        TextView fullNameText, TextView descriptionText, TextView preferencesText, 
        TextView locationText, TextView phoneText) {
        
        String fullName = document.getString("fullName");
        String description = document.getString("description");
        String phone = document.getString("phone");
        String profileImageUrl = document.getString("photoUrl");
        String country = document.getString("country");
        String city = document.getString("city");
        List<String> preferences = (List<String>) document.get("preferences");

        fullNameText.setText(fullName);
        descriptionText.setText(description);
        locationText.setText(String.format("%s, %s", city, country));
        
        if (preferences != null && !preferences.isEmpty()) {
            preferencesText.setText(String.join(", ", preferences));
        } else {
            preferencesText.setText("No preferences set");
        }

        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            Picasso.get()
                    .load(profileImageUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
        }

        // Format phone number with flag
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phone, null);
            String regionCode = phoneNumberUtil.getRegionCodeForNumber(number);
            if (regionCode != null) {
                String flag = CountryUtils.getCountryFlag(regionCode);
                String formattedNumber = phoneNumberUtil.format(number, 
                    PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                phoneText.setText(flag + " " + formattedNumber);
            } else {
                phoneText.setText(phone);
            }
        } catch (NumberParseException e) {
            phoneText.setText(phone);
        }
    }
} 