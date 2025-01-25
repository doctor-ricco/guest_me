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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
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

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        uploadPhotoToCloudinary(); // Faz o upload da imagem ao Cloudinary
                    } else {
                        Toast.makeText(getActivity(), "Image selection failed.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host_home, container, false);

        profileImage = view.findViewById(R.id.profileImage); // Inicializa o CircleImageView
        EditText fullNameInput = view.findViewById(R.id.fullNameInput);
        EditText addressInput = view.findViewById(R.id.addressInput);
        EditText phoneInput = view.findViewById(R.id.phoneInput);
        EditText descriptionInput = view.findViewById(R.id.hostDescription);

        Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);
        Button saveProfileButton = view.findViewById(R.id.saveProfileButton);

        uploadPhotoButton.setOnClickListener(v -> openImagePicker());
        saveProfileButton.setOnClickListener(v -> {
            String fullName = fullNameInput.getText().toString();
            String address = addressInput.getText().toString();
            String phone = phoneInput.getText().toString();
            String description = descriptionInput.getText().toString();

            if (fullName.isEmpty() || address.isEmpty() || phone.isEmpty() || description.isEmpty()) {
                Toast.makeText(getActivity(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
                return;
            }

            saveProfileToFirestore(fullName, address, phone, description, uploadedImageUrl);

            Intent intent = new Intent(getActivity(), PreferencesActivity.class);
            startActivity(intent);

        });

        return view;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(intent); // Este método chama o imagePickerLauncher para selecionar uma imagem
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

    private void saveProfileToFirestore(String fullName, String address, String phone, String description, String photoUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> existingData = documentSnapshot.getData(); // Dados existentes
                        if (existingData != null) {
                            existingData.put("fullName", fullName);
                            existingData.put("address", address);
                            existingData.put("phone", phone);
                            existingData.put("description", description);
                            if (photoUrl != null) existingData.put("photoUrl", photoUrl);

                            FirebaseFirestore.getInstance().collection("users")
                                    .document(userId)
                                    .set(existingData, SetOptions.merge()) // Mescla os dados existentes
                                    .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Failed to update profile: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    } else {
                        Toast.makeText(getActivity(), "User document does not exist.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Error fetching user data: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
}
