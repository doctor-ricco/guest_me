package com.example.guestme;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HostHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar o layout do fragmento
        View view = inflater.inflate(R.layout.fragment_host_home, container, false);

        // Referenciar componentes do layout
        EditText fullNameInput = view.findViewById(R.id.fullNameInput);
        EditText addressInput = view.findViewById(R.id.addressInput);
        EditText phoneInput = view.findViewById(R.id.phoneInput);
        Button saveProfileButton = view.findViewById(R.id.saveProfileButton);
        Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);

        // Configurar botão "Save and Continue"
        saveProfileButton.setOnClickListener(v -> {
            String fullName = fullNameInput.getText().toString();
            String address = addressInput.getText().toString();
            String phone = phoneInput.getText().toString();

            // Verificar se todos os campos foram preenchidos
            if (fullName.isEmpty() || address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill out all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            // Salvar informações no Firestore
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            Map<String, Object> userProfile = new HashMap<>();
            userProfile.put("fullName", fullName);
            userProfile.put("address", address);
            userProfile.put("phone", phone);

            FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .update(userProfile)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getActivity(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();
                        // Redirecionar para a próxima etapa (ex.: preferências)
                        Navigation.findNavController(view).navigate(R.id.action_hostHomeFragment_to_preferencesFragment);
                    })
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        return view;
    }
}
