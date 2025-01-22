package com.example.guestme;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
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

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    Log.d("HostHomeFragment", "Permission granted, launching image picker");
                    launchImagePicker();
                } else {
                    Log.d("HostHomeFragment", "Permission denied");
                    Toast.makeText(getActivity(), "Permission Denied. Please allow access to select photos.", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Log.d("HostHomeFragment", "Image picker result: " + result.getResultCode());
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    Log.d("HostHomeFragment", "Image URI received: " + imageUri);

                    if (imageUri != null) {
                        selectedImageUri = imageUri;
                        Toast.makeText(getActivity(), "Image selected successfully!", Toast.LENGTH_SHORT).show();
                        uploadPhotoToCloudinary(); // Chamar o upload após a seleção da imagem
                    } else {
                        Toast.makeText(getActivity(), "Failed to retrieve image URI.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Log.d("HostHomeFragment", "No image selected or result cancelled.");
                    Toast.makeText(getActivity(), "No image selected.", Toast.LENGTH_SHORT).show();
                }
            });


@Nullable
@Override
public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_host_home, container, false);

    // Adiciona logs para verificar o FragmentManager e a hierarquia
    FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
    for (Fragment fragment : fragmentManager.getFragments()) {
        Log.d("HostHomeFragment", "Fragment encontrado: " + fragment.getClass().getSimpleName());
    }

    // O restante do código existente deve permanecer
    EditText fullNameInput = view.findViewById(R.id.fullNameInput);
    EditText addressInput = view.findViewById(R.id.addressInput);
    EditText phoneInput = view.findViewById(R.id.phoneInput);
    Button saveProfileButton = view.findViewById(R.id.saveProfileButton);
    Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);

    uploadPhotoButton.setOnClickListener(v -> openImagePicker());
    saveProfileButton.setOnClickListener(v -> {
        String fullName = fullNameInput.getText().toString();
        String address = addressInput.getText().toString();
        String phone = phoneInput.getText().toString();

        if (fullName.isEmpty() || address.isEmpty() || phone.isEmpty() || uploadedImageUrl == null) {
            Toast.makeText(getActivity(), "Please complete all fields and upload a photo", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("fullName", fullName);
        userProfile.put("address", address);
        userProfile.put("phone", phone);
        userProfile.put("photoUrl", uploadedImageUrl);

        FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update(userProfile)
                .addOnSuccessListener(aVoid -> {
                    Log.d("HostHomeFragment", "Profile saved successfully!");
                    Toast.makeText(getActivity(), "Profile saved successfully!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), PreferencesActivity.class);
                    startActivity(intent);
                    requireActivity().finish(); // Opcional, para evitar que o usuário volte para esta tela
                })

                .addOnFailureListener(e -> {
                    Log.e("HostHomeFragment", "Error saving profile: " + e.getMessage(), e);
                    Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    });

    return view;
}

    private void openImagePicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES);
            } else {
                launchImagePicker();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0+
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE);
            } else {
                launchImagePicker();
            }
        } else {
            launchImagePicker(); // Android 5 ou inferior
        }
    }

    private void launchImagePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*"); // Apenas imagens
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getActivity(), "Failed to open image picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
                            Log.e("HostHomeFragment", "Cloudinary upload failed", e);
                        }

                        @Override
                        public void onResponse(okhttp3.Call call, okhttp3.Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body().string();
                                uploadedImageUrl = extractImageUrlFromResponse(responseBody);

                                getActivity().runOnUiThread(() -> {
                                    Toast.makeText(getActivity(), "Photo uploaded successfully!", Toast.LENGTH_SHORT).show();

                                    // Atualiza o CircleImageView com a URL da imagem carregada
                                    CircleImageView profileImage = getView().findViewById(R.id.profileImage);
                                    if (profileImage != null) {
                                        Picasso.get()
                                                .load(uploadedImageUrl)
                                                .placeholder(R.drawable.profile) // Substitua pelo seu drawable de placeholder
                                                .into(profileImage);
                                    } else {
                                        Log.e("HostHomeFragment", "profileImage view is null");
                                    }
                                });
                            } else {
                                getActivity().runOnUiThread(() ->
                                        Toast.makeText(getActivity(), "Upload failed", Toast.LENGTH_SHORT).show());
                                Log.e("HostHomeFragment", "Cloudinary response unsuccessful");
                            }
                        }
                    });
                } else {
                    getActivity().runOnUiThread(() ->
                            Toast.makeText(getActivity(), "Failed to get file path", Toast.LENGTH_SHORT).show());
                    Log.e("HostHomeFragment", "File path is null");
                }
            } catch (Exception e) {
                getActivity().runOnUiThread(() ->
                        Toast.makeText(getActivity(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                Log.e("HostHomeFragment", "Error during upload", e);
            }
        } else {
            getActivity().runOnUiThread(() ->
                    Toast.makeText(getActivity(), "No image selected", Toast.LENGTH_SHORT).show());
            Log.e("HostHomeFragment", "Selected image URI is null");
        }
    }

    private String extractImageUrlFromResponse(String responseBody) {
        try {
            JSONObject jsonResponse = new JSONObject(responseBody);
            return jsonResponse.getString("secure_url");
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getRealPathFromURI(Uri uri) {
        if (uri == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // Android 10+
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
        } else { // Android 9 (API 28) e anteriores
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
