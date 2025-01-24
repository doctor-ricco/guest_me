package com.example.guestme;

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

import de.hdodenhof.circleimageview.CircleImageView;

public class HostProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_host_profile);

        // Referências aos elementos da UI
        CircleImageView profileImage = findViewById(R.id.profile_image);
        TextView welcomeMessage = findViewById(R.id.welcome_message);
        TextView userInfo = findViewById(R.id.user_info);
        TextView hostDescription = findViewById(R.id.host_description);
        TextView hostExperiences = findViewById(R.id.host_experiences);
        Button editProfileButton = findViewById(R.id.edit_profile_button);

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

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
                            String fullName = document.getString("fullName");
                            String location = document.getString("address");
                            String phone = document.getString("phone");
                            String description = document.getString("description");
                            String experiences = document.getString("experiences");
                            String profileImageUrl = document.getString("photoUrl");

                            // Configurar a UI com os dados
                            welcomeMessage.setText("Welcome, Host " + fullName + "!");
                            userInfo.setText("Address: " + location + "\nPhone: " + phone);
                            hostDescription.setText("About: " + description);
                            hostExperiences.setText("Experiences: " + experiences);

                            // Carregar imagem do Cloudinary usando Glide
                            if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(profileImageUrl)
                                        .placeholder(R.drawable.profile) // Placeholder
                                        .error(R.drawable.profile) // Imagem de erro
                                        .into(profileImage);
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
            Toast.makeText(this, "Funcionalidade de edição em desenvolvimento.", Toast.LENGTH_SHORT).show();
        });
    }
}
