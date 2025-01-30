package com.example.guestme;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import com.example.guestme.utils.LocationData;
import io.michaelrocks.libphonenumber.android.NumberParseException;
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil;
import io.michaelrocks.libphonenumber.android.Phonenumber;
import com.google.android.material.textfield.TextInputLayout;
import com.example.guestme.utils.CountryUtils;
import android.widget.AutoCompleteTextView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.example.guestme.utils.PhoneInputFormatter;
import com.google.android.material.textfield.TextInputEditText;

public class HostHomeFragment extends Fragment {

    private Uri selectedImageUri; // URI da imagem selecionada
    private String uploadedImageUrl; // URL da imagem após upload no Cloudinary
    private CircleImageView profileImage; // Referência ao CircleImageView para atualizar a imagem
    private EditText fullNameInput, addressInput, descriptionInput;
    private TextInputEditText phoneInput;
    private MaterialAutoCompleteTextView countrySpinner;
    private MaterialAutoCompleteTextView citySpinner;

    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    private boolean isEditing = false; // Indica se o usuário está editando o perfil
    private boolean isProfileComplete = false; // Indica se o perfil está completo

    private PhoneNumberUtil phoneNumberUtil;
    private TextInputLayout phoneInputLayout;
    private PhoneInputFormatter phoneFormatter;

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
        
        // Initialize location data with callback
        LocationData.initialize(requireContext(), success -> {
            if (isAdded() && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded()) {
                        if (success) {
                            // Remove this line since we'll set up spinners in initializeViews
                            // setupCountrySpinner();
                        } else {
                            Toast.makeText(requireContext(), 
                                "Failed to load countries. Please check your internet connection.", 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        });
        
        // Verificar se estamos editando o perfil
        if (getArguments() != null) {
            isEditing = getArguments().getBoolean("isEditing", false);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host_home, container, false);

        // Initialize Firebase first
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Initialize views
        initializeViews(view);
        
        // Check profile completion status
        if (!isEditing) {
            checkProfileCompletion();
        } else {
            loadProfileData();
        }

        return view;
    }

    private void initializeViews(View view) {
        // Referências aos elementos da UI
        profileImage = view.findViewById(R.id.profileImage);
        fullNameInput = view.findViewById(R.id.fullNameInput);
        addressInput = view.findViewById(R.id.addressInput);
        phoneInput = view.findViewById(R.id.phoneInput);
        descriptionInput = view.findViewById(R.id.hostDescription);
        countrySpinner = view.findViewById(R.id.country_spinner);
        citySpinner = view.findViewById(R.id.city_spinner);
        phoneInputLayout = view.findViewById(R.id.phoneInputLayout);

        Button uploadPhotoButton = view.findViewById(R.id.uploadPhotoButton);
        Button saveProfileButton = view.findViewById(R.id.saveProfileButton);
        //Button preferencesButton = view.findViewById(R.id.preferencesButton);

        // Initialize PhoneNumberUtil
        phoneNumberUtil = PhoneNumberUtil.createInstance(requireContext());

        // Setup click listeners
        uploadPhotoButton.setOnClickListener(v -> openImagePicker());
        saveProfileButton.setOnClickListener(v -> saveProfile());
//        preferencesButton.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), PreferencesActivity.class);
//            startActivity(intent);
//        });

        // Setup phone number formatting
        phoneFormatter = new PhoneInputFormatter(requireContext(), phoneInputLayout, phoneInput);
        
        // Update phone formatting when country is selected
        countrySpinner.setOnItemClickListener((parent, view1, position, id) -> {
            String country = (String) parent.getItemAtPosition(position);
            String countryCode = CountryUtils.getCountryCode(country);
            phoneFormatter.setCountryCode(countryCode);
        });

        phoneFormatter.setupPhoneNumberFormatting();

        // If using MaterialAutoCompleteTextView in layout, change to:
        MaterialAutoCompleteTextView countryInput = view.findViewById(R.id.country_spinner);
        MaterialAutoCompleteTextView cityInput = view.findViewById(R.id.city_spinner);

        // Update the setupCountrySpinner method to work with AutoCompleteTextView
        setupCountrySpinner(countryInput, cityInput);
    }

    private void checkProfileCompletion() {
        String userId = auth.getCurrentUser().getUid();

        firestore.collection("users")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded() || getActivity() == null) return;

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
                                getActivity().finish();
                            } else {
                                // If profile is not complete, load existing data (if any)
                                loadProfileData();
                            }
                        } else {
                            // New user, just stay on the current screen to fill out profile
                            Log.d("HostHomeFragment", "New user, creating profile");
                        }
                    } else {
                        Log.e("HostHomeFragment", "Error checking profile completion", task.getException());
                        Toast.makeText(getActivity(), 
                            "Error checking profile. Please try again.", 
                            Toast.LENGTH_SHORT).show();
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
                            // Store the document for later use
                            final DocumentSnapshot finalDoc = document;

                            // Initialize location data first
                            LocationData.initialize(requireContext(), success -> {
                                if (isAdded() && getActivity() != null) {
                                    requireActivity().runOnUiThread(() -> {
                                        if (isAdded()) {
                                            // Setup spinners using the class members
                                            setupCountrySpinner(countrySpinner, citySpinner);
                                            
                                            // Then load the rest of the profile data
                                            fullNameInput.setText(finalDoc.getString("fullName"));
                                            addressInput.setText(finalDoc.getString("address"));
                                            phoneInput.setText(finalDoc.getString("phone"));
                                            descriptionInput.setText(finalDoc.getString("description"));

                                            // Load existing location after spinners are set up
                                            loadExistingLocation(finalDoc);

                                            // Load existing phone number
                                            loadExistingPhoneNumber(finalDoc.getString("phone"));

                                            // Load profile image
                                            String photoUrl = finalDoc.getString("photoUrl");
                                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                                Picasso.get()
                                                        .load(photoUrl)
                                                        .placeholder(R.drawable.profile)
                                                        .error(R.drawable.profile)
                                                        .into(profileImage);
                                            }
                                        }
                                    });
                                }
                            });
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
        String selectedCountry = countrySpinner.getText().toString();
        String selectedCity = citySpinner.getText().toString();

        // Validate all fields
        if (fullName.isEmpty() || address.isEmpty() || description.isEmpty()) {
            Toast.makeText(getActivity(), "Please fill out all fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate country and city
        if (selectedCountry.isEmpty() || selectedCity.isEmpty()) {
            Toast.makeText(getActivity(), "Please select both country and city.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate phone number
        if (!phoneFormatter.isValidPhoneNumber()) {
            Toast.makeText(getActivity(), "Please enter a valid phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullPhoneNumber = phoneFormatter.getFullPhoneNumber();

        // Format the phone number to E.164 format for storage
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(fullPhoneNumber, null);
            String formattedNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
            saveProfileToFirestore(fullName, address, formattedNumber, description, uploadedImageUrl, selectedCountry, selectedCity);
        } catch (NumberParseException e) {
            Toast.makeText(getActivity(), "Error formatting phone number.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProfileToFirestore(String fullName, String address, String phone, String description, String photoUrl, String country, String city) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> profileData = new HashMap<>();
        profileData.put("fullName", fullName);
        profileData.put("address", address);
        profileData.put("phone", phone);
        profileData.put("description", description);
        if (photoUrl != null) profileData.put("photoUrl", photoUrl);
        profileData.put("country", country);
        profileData.put("city", city);

        firestore.collection("users")
                .document(userId)
                .set(profileData, SetOptions.merge()) // Mescla os dados existentes
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getActivity(), "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    isProfileComplete = true; // Marcar o perfil como completo

                    // Navigate to preferences screen
                    Intent intent = new Intent(getActivity(), PreferencesActivity.class);
                    startActivity(intent);
                    getActivity().finish(); // Optional: finish current activity if you don't want users to come back
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

    private void loadExistingLocation(DocumentSnapshot document) {
        if (!isAdded() || countrySpinner == null) return;

        String savedCountry = document.getString("country");
        String savedCity = document.getString("city");
        
        if (savedCountry != null && !savedCountry.isEmpty()) {
            // Set the text directly for MaterialAutoCompleteTextView
            countrySpinner.setText(savedCountry, false);
            
            // Update city spinner
            if (savedCity != null && !savedCity.isEmpty()) {
                // Update the cities list for the selected country
                updateCitySpinner(savedCountry, citySpinner);
                
                // Set the city text after a short delay to ensure adapter is ready
                countrySpinner.post(() -> {
                    if (isAdded() && citySpinner != null) {
                        citySpinner.setText(savedCity, false);
                    }
                });
            }
        }
    }

    private void updateCitySpinner(String country, MaterialAutoCompleteTextView cityInput) {
        if (!isAdded()) return;
        
        List<String> cities = LocationData.getCitiesForCountry(country);
        ArrayAdapter<String> cityAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            cities
        );
        cityInput.setAdapter(cityAdapter);
    }

    private void loadExistingPhoneNumber(String phoneNumber) {
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            updatePhoneNumberWithFlag(phoneNumber);
        }
    }

    private void updatePhoneNumberWithFlag(String phoneNumber) {
        try {
            Phonenumber.PhoneNumber number = phoneNumberUtil.parse(phoneNumber, null);
            String regionCode = phoneNumberUtil.getRegionCodeForNumber(number);
            if (regionCode != null) {
                String flag = CountryUtils.getCountryFlag(regionCode);
                String formattedNumber = phoneNumberUtil.format(number, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL);
                phoneInput.setText(flag + " " + formattedNumber);
            }
        } catch (NumberParseException e) {
            // If parsing fails, just show the number without flag
            phoneInput.setText(phoneNumber);
        }
    }

    private void setupCountrySpinner(MaterialAutoCompleteTextView countryInput, MaterialAutoCompleteTextView cityInput) {
        if (!isAdded()) return;
        
        List<String> countries = LocationData.getCountries();
        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            countries
        );
        
        countryInput.setAdapter(countryAdapter);
        
        countryInput.setOnItemClickListener((parent, view, position, id) -> {
            if (isAdded()) {
                String selectedCountry = (String) parent.getItemAtPosition(position);
                updateCitySpinner(selectedCountry, cityInput);
            }
        });
    }
}