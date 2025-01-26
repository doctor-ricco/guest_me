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

import de.hdodenhof.circleimageview.CircleImageView;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import com.example.guestme.utils.CountryUtils;

public class HostProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;
    private PhoneNumberUtil phoneNumberUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_profile);

        // Referências aos elementos da UI
        CircleImageView profileImage = findViewById(R.id.profile_image);
        TextView welcomeMessage = findViewById(R.id.welcome_message);
        TextView userAddress = findViewById(R.id.user_address);
        TextView userPhone = findViewById(R.id.user_phone);
        TextView hostDescription = findViewById(R.id.host_description);
        TextView hostPreferences = findViewById(R.id.host_preferences);
        Button editProfileButton = findViewById(R.id.edit_profile_button);
        Button logoutButton = findViewById(R.id.logout_button);
        TextView userCountry = findViewById(R.id.user_country);
        TextView userCity = findViewById(R.id.user_city);

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
                            welcomeMessage.setText("Welcome, Host " + firstName + "!");
                            userAddress.setText(location != null ? location : "N/A");
                            userPhone.setText(phone != null ? phone : "N/A");
                            hostDescription.setText(description != null ? description : "N/A");
                            hostPreferences.setText(preferences);

                            // Set location data
                            String country = document.getString("country");
                            String city = document.getString("city");
                            userCountry.setText(country != null ? country : "Not specified");
                            userCity.setText(city != null ? city : "Not specified");

                            // Carregar imagem do perfil usando Glide
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Log.d("HostProfileActivity", "Loading profile image from URL: " + profileImageUrl);

                                if (!profileImageUrl.startsWith("http://") && !profileImageUrl.startsWith("https://")) {
                                    Log.e("HostProfileActivity", "Invalid image URL: " + profileImageUrl);
                                    Toast.makeText(this, "Invalid image URL.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Glide.with(this)
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
                                    userPhone.setText(flag + " " + formattedNumber);
                                } else {
                                    userPhone.setText(phone != null ? phone : "N/A");
                                }
                            } catch (NumberParseException e) {
                                userPhone.setText(phone != null ? phone : "N/A");
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

        // Listener para editar perfil
        editProfileButton.setOnClickListener(v -> {
            Intent intent = new Intent(HostProfileActivity.this, HostHomeActivity.class);
            intent.putExtra("isEditing", true);
            startActivity(intent);
        });

        // Add logout button click listener
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(HostProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }
}