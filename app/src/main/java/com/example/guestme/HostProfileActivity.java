package com.example.guestme;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import com.example.guestme.utils.CountryUtils;
import com.squareup.picasso.Picasso;

public class HostProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private PhoneNumberUtil phoneNumberUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_profile);

        // Update view IDs to match the new layout
        CircleImageView profileImage = findViewById(R.id.profileImage);
        TextView fullNameText = findViewById(R.id.fullNameText);
        TextView descriptionText = findViewById(R.id.descriptionText);
        TextView preferencesText = findViewById(R.id.preferencesText);
        TextView locationText = findViewById(R.id.locationText);
        TextView addressText = findViewById(R.id.addressText);
        TextView phoneText = findViewById(R.id.phoneText);
        Button editProfileButton = findViewById(R.id.editProfileButton);
        Button logoutButton = findViewById(R.id.logoutButton);

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize PhoneNumberUtil
        phoneNumberUtil = PhoneNumberUtil.createInstance(this);

        // Obter o UID do usuário atual
        String userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "No user data has been found.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Carregar dados do Firestore
        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Obter os dados do documento
                            String fullName = document.getString("fullName");
                            String firstName = fullName != null ? fullName.split(" ")[0] : "Host";
                            String location = document.getString("address");
                            String phone = document.getString("phone");
                            String description = document.getString("description");
                            String profileImageUrl = document.getString("photoUrl");

                            // Obter a lista de preferências
                            List<String> preferencesList = (List<String>) document.get("preferences");
                            String preferences = preferencesList != null ? String.join(", ", preferencesList) : "No preferences set.";

                            // Configurar a UI com os dados
                            fullNameText.setText(fullName);
                            descriptionText.setText(description);
                            locationText.setText(String.format("%s, %s", document.getString("city"), document.getString("country")));
                            addressText.setText(location != null ? location : "N/A");
                            phoneText.setText(phone != null ? phone : "N/A");
                            preferencesText.setText(preferences);

                            // Carregar imagem do perfil usando Glide
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Log.d("HostProfileActivity", "Loading profile image from URL: " + profileImageUrl);

                                if (!profileImageUrl.startsWith("http://") && !profileImageUrl.startsWith("https://")) {
                                    Log.e("HostProfileActivity", "Invalid image URL: " + profileImageUrl);
                                    Toast.makeText(this, "Invalid image URL.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Picasso.get()
                                            .load(profileImageUrl)
                                            .placeholder(R.drawable.profile)
                                            .error(R.drawable.profile)
                                            .into(profileImage);
                                }
                            } else {
                                Log.d("HostProfileActivity", "Profile image URL is null or empty.");
                                Toast.makeText(this, "No profile image found.", Toast.LENGTH_SHORT).show();
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
                                    phoneText.setText(phone != null ? phone : "N/A");
                                }
                            } catch (NumberParseException e) {
                                phoneText.setText(phone != null ? phone : "N/A");
                            }
                        } else {
                            Log.e("HostProfileActivity", "Usuário não encontrado no Firestore.");
                            Toast.makeText(this, "User Not Found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e("HostProfileActivity", "Erro ao carregar dados do Firestore: ", task.getException());
                        Toast.makeText(this, "No data has been found.", Toast.LENGTH_SHORT).show();
                    }
                });

        // Setup button click listeners
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, HostActivity.class);
            intent.putExtra("isEditing", true);
            intent.putExtra("openFragment", "hostHome");
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

    private void loadPreferences(String userId, TextView preferencesText) {
        FirebaseFirestore.getInstance()
            .collection("preferences")
            .document(userId)
            .get()
            .addOnSuccessListener(document -> {
                if (document.exists()) {
                    // Format preferences text
                    StringBuilder preferences = new StringBuilder();
                    Map<String, Object> data = document.getData();
                    for (Map.Entry<String, Object> entry : data.entrySet()) {
                        preferences.append(entry.getKey())
                                 .append(": ")
                                 .append(entry.getValue())
                                 .append("\n");
                    }
                    preferencesText.setText(preferences.toString().trim());
                }
            });
    }
}