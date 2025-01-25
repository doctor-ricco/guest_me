package com.example.guestme;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class HostHomeFragment extends Fragment {

    private Uri selectedImageUri; // URI da imagem selecionada
    private String uploadedImageUrl; // URL da imagem após upload no Cloudinary
    private CircleImageView profileImage; // Referência ao CircleImageView para atualizar a imagem
    private EditText fullNameInput, addressInput, phoneInput, descriptionInput;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private boolean isEditing = false; // Indica se o usuário está editando o perfil
    private boolean isProfileComplete = false; // Indica se o perfil está completo

    // ActivityResultLauncher para selecionar imagens
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        // Exibir a imagem selecionada no CircleImageView
                        profileImage.setImageURI(selectedImageUri);
                        // Fazer o upload da imagem ao Cloudinary
                        uploadPhotoToCloudinary();
                    } else {
                        Toast.makeText(getActivity(), "Failed to select image.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Verificar se estamos editando o perfil
        if (getArguments() != null) {
            isEditing = getArguments().getBoolean("isEditing", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host_home, container, false);

        // Inicializar Firebase
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Referências aos elementos da UI
        profileImage = view.findViewById(R.id.profileImage);
        fullNameInput = view.findViewById(R.id.fullNameInput);
        addressInput = view.findViewById(R.id.addressInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        descriptionInput = view.findViewById(R.id.hostDescription);

        Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);
        Button saveProfileButton = view.findViewById(R.id.saveProfileButton);

        // Verificar se o perfil está completo (apenas se não estiver editando)
        if (!isEditing) {
            checkProfileCompletion();
        } else {
            // Se estiver editando, carregar os dados do perfil
            loadProfileData();
        }

        // Configurar o botão de upload de foto
        uploadPhotoButton.setOnClickListener(v -> openImagePicker());

        // Configurar o botão de salvar perfil
        saveProfileButton.setOnClickListener(v -> saveProfile());

        return view;
    }

    private void checkProfileCompletion() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Verificar se todos os campos necessários estão preenchidos
                            isProfileComplete = document.contains("fullName") &&
                                    document.contains("address") &&
                                    document.contains("phone") &&
                                    document.contains("description");

                            if (isProfileComplete) {
                                // Se o perfil estiver completo, redirecionar para a HostProfileActivity
                                Intent intent = new Intent(getActivity(), HostProfileActivity.class);
                                startActivity(intent);
                                getActivity().finish(); // Finalizar a atividade atual para evitar voltar ao fragmento
                            }
                        } else {
                            Toast.makeText(getActivity(), "User document does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                        Log.e("HostHomeFragment", "Error loading profile data", task.getException());
                    }
                });
    }

    private void loadProfileData() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            // Preencher os campos com os dados existentes
                            fullNameInput.setText(document.getString("fullName"));
                            addressInput.setText(document.getString("address"));
                            phoneInput.setText(document.getString("phone"));
                            descriptionInput.setText(document.getString("description"));

                            // Carregar a imagem do perfil (se existir)
                            String photoUrl = document.getString("photoUrl");
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                Picasso.get()
                                        .load(photoUrl)
                                        .placeholder(R.drawable.profile)
                                        .error(R.drawable.profile)
                                        .into(profileImage);
                            }
                        } else {
                            Toast.makeText(getActivity(), "User document does not exist.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getActivity(), "Failed to load profile data.", Toast.LENGTH_SHORT).show();
                        Log.e("HostHomeFragment", "Error loading profile data", task.getException());
                    }
                });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent); // Usar o ActivityResultLauncher para selecionar a imagem
    }

    private void saveProfile() {
        String fullName = fullNameInput.getText().toString();
        String address = addressInput.getText().toString();
        String phone = phoneInput.getText().toString();
        String description = descriptionInput.getText().toString();

        if (fullName.isEmpty() || address.isEmpty() || phone.isEmpty() || description.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        saveProfileToFirestore(fullName, address, phone, description, uploadedImageUrl);

        // Navegar para a tela de preferências após salvar
        Intent intent = new Intent(getActivity(), PreferencesActivity.class);
        startActivity(intent);
    }

    private void saveProfileToFirestore(String fullName, String address, String phone, String description, String photoUrl) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", fullName);
        profileData.put("address", address);
        profileData.put("phone", phone);
        profileData.put("description", description);
        if (photoUrl != null) profileData.put("photoUrl", photoUrl);

        firestore.collection("users")
                .document(userId)
                .set(profileData, SetOptions.merge()) // Mescla os dados existentes
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    isProfileComplete = true; // Marcar o perfil como completo
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getActivity(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("HostHomeFragment", "Error updating profile", e);
                });
    }

    private void uploadPhotoToCloudinary() {
        if (selectedImageUri != null) {
            try {
                String filePath = getRealPathFromURI(selectedImageUri);
                if (filePath != null) {
                    File imageFile = new File(filePath);
                    CloudinaryUploader.uploadImage(imageFile, new okhttp3.Callback() {
                        @Override
                        public void onFailure(okhttp3.Call call, IOException e) {
                            getActivity().runOnUiThread(() ->
                                    Toast.makeText(getActivity(), "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                uploadedImageUrl = extractImageUrlFromResponse(responseBody);
                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getActivity(), "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();
                                    updateProfileImage(uploadedImageUrl);
                                });
                            } else {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getActivity(), "Failed to upload image.", Toast.LENGTH_SHORT).show());
                            }
                        }
                    });
                }
            } catch (Exception e) {
                Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfileImage(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.profile)
                    .error(R.drawable.profile)
                    .into(profileImage);
        }
    }

    private String getRealPathFromURI(Uri uri) {
        if (uri == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                    cursor.moveToFirst();
                    String fileName = cursor.getString(nameIndex);
                    File file = new File(getActivity().getFilesDir(), fileName);
                    try (InputStream inputStream = getActivity().getContentResolver().openInputStream(uri);
                         OutputStream outputStream = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, length);
                        }
                    }
                    cursor.close();
                    return file.getAbsolutePath();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getActivity(), "Failed to process file", Toast.LENGTH_SHORT).show();
            }
        } else {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                String path = cursor.getString(columnIndex);
                cursor.close();
                return path;
            }
        }
        return null;
    }

    private String extractImageUrlFromResponse(String responseBody) {
        try {
            JSONObject json = new JSONObject(responseBody);
            return json.getString("secure_url");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}