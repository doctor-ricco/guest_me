package com.example.guestme;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import de.hdodenhof.circleimageview.CircleImageView;

public class HostProfileActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private DatabaseReference databaseRef;

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

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance();
        databaseRef = FirebaseDatabase.getInstance().getReference("users");

        // Obter o UID do usuário atual
        String userId = auth.getCurrentUser().getUid();

        // Carregar dados do Firebase
        databaseRef.child(userId).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DataSnapshot snapshot = task.getResult();
                if (snapshot.exists()) {
                    String firstName = snapshot.child("firstName").getValue(String.class);
                    String lastName = snapshot.child("lastName").getValue(String.class);
                    String birthDate = snapshot.child("birthDate").getValue(String.class);
                    String location = snapshot.child("location").getValue(String.class);
                    String description = snapshot.child("description").getValue(String.class);
                    String experiences = snapshot.child("experiences").getValue(String.class);
                    String profileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);

                    // Configurar a UI com os dados
                    welcomeMessage.setText("Bem-vindo, Host " + firstName + "!");
                    userInfo.setText("Nome Completo: " + firstName + " " + lastName + "\nLocalização: " + location + "\nData de Nascimento: " + birthDate);
                    hostDescription.setText("Descrição do Host: " + description);
                    hostExperiences.setText("Experiências Oferecidas: " + experiences);

                    // Carregar imagem do Cloudinary usando Glide no CircleImageView
                    Glide.with(this).load(profileImageUrl).into(profileImage);
                }
            }
        });

        // Listener para editar perfil
        editProfileButton.setOnClickListener(v -> {
            // Navegar para a tela de edição de perfil
        });
    }
}
